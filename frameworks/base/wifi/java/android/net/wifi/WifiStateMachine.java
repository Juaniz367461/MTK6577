/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.wifi;

import static android.net.wifi.WifiManager.WIFI_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_DISABLING;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLING;
import static android.net.wifi.WifiManager.WIFI_STATE_UNKNOWN;

/**
 * TODO: Add soft AP states as part of WIFI_STATE_XXX
 * Retain WIFI_STATE_ENABLING that indicates driver is loading
 * Add WIFI_STATE_AP_ENABLED to indicate soft AP has started
 * and WIFI_STATE_FAILED for failure
 * Deprecate WIFI_STATE_UNKNOWN
 *
 * Doing this will simplify the logic for sending broadcasts
 */
import static android.net.wifi.WifiManager.WIFI_AP_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_DISABLING;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLED;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLING;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_FAILED;

import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.backup.IBackupManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.DhcpInfoInternal;
import android.net.DhcpStateMachine;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.wifi.WpsResult.Status;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pService;
import android.net.wifi.StateChangeResult;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.provider.Settings;
import static android.provider.Settings.System.GPRS_CONNECTION_SETTING;
import static android.provider.Settings.System.GPRS_CONNECTION_SETTING_DEFAULT;
import android.provider.Telephony.SIMInfo;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.util.LruCache;

import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.Protocol;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.mediatek.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

/**
 * Track the state of Wifi connectivity. All event handling is done here,
 * and all changes in connectivity state are initiated here.
 *
 * Wi-Fi now supports three modes of operation: Client, Soft Ap and Direct
 * In the current implementation, we do not support any concurrency and thus only
 * one of Client, Soft Ap or Direct operation is supported at any time.
 *
 * The WifiStateMachine supports Soft Ap and Client operations while WifiP2pService
 * handles Direct. WifiP2pService and WifiStateMachine co-ordinate to ensure only
 * one exists at a certain time.
 *
 * @hide
 */
public class WifiStateMachine extends StateMachine {

    private static final String TAG = "WifiStateMachine";
    private static final String NETWORKTYPE = "WIFI";
    private static final boolean DBG = true;

    /* TODO: This is no more used with the hostapd code. Clean up */
    private static final String SOFTAP_IFACE = "wl0.1";

    private WifiMonitor mWifiMonitor;
    private INetworkManagementService mNwService;
    private ConnectivityManager mCm;

    /* Scan results handling */
    private List<ScanResult> mScanResults;
    private static final Pattern scanResultPattern = Pattern.compile("\t+");
    private static final int SCAN_RESULT_CACHE_SIZE = 80;
    private final LruCache<String, ScanResult> mScanResultCache;

    private String mInterfaceName;
    /* Tethering interface could be seperate from wlan interface */
    private String mTetherInterfaceName;

    private int mLastSignalLevel = -1;
    private String mLastBssid;
    private int mLastNetworkId;
    private boolean mEnableRssiPolling = false;
    private boolean mEnableBackgroundScan = false;
    private int mRssiPollToken = 0;
    private int mReconnectCount = 0;
    private boolean mIsScanMode = false;
    private boolean mScanResultIsPending = false;

    private boolean mBluetoothConnectionActive = false;

    /**
     * Interval in milliseconds between polling for RSSI
     * and linkspeed information
     */
    private static final int POLL_RSSI_INTERVAL_MSECS = 3000;

    /**
     * Delay between supplicant restarts upon failure to establish connection
     */
    private static final int SUPPLICANT_RESTART_INTERVAL_MSECS = 5000;

    /**
     * Number of times we attempt to restart supplicant
     */
    private static final int SUPPLICANT_RESTART_TRIES = 5;

    private int mSupplicantRestartCount = 0;
    /* Tracks sequence number on stop failure message */
    private int mSupplicantStopFailureToken = 0;

    /**
     * Tether state change notification time out
     */
    private static final int TETHER_NOTIFICATION_TIME_OUT_MSECS = 5000;

    /* Tracks sequence number on a tether notification time out */
    private int mTetherToken = 0;

    private LinkProperties mLinkProperties;

    // Wakelock held during wifi start/stop and driver load/unload
    private PowerManager.WakeLock mWakeLock;

    private Context mContext;

    private DhcpInfoInternal mDhcpInfoInternal;
    private WifiInfo mWifiInfo;
    private NetworkInfo mNetworkInfo;
    private SupplicantStateTracker mSupplicantStateTracker;
    private WpsStateMachine mWpsStateMachine;
    private DhcpStateMachine mDhcpStateMachine;

    private AlarmManager mAlarmManager;
    private PendingIntent mScanIntent;
    /* Tracks current frequency mode */
    private AtomicInteger mFrequencyBand = new AtomicInteger(WifiManager.WIFI_FREQUENCY_BAND_AUTO);

    /* Tracks if we are filtering Multicast v4 packets. Default is to filter. */
    private AtomicBoolean mFilteringMulticastV4Packets = new AtomicBoolean(true);

    // Channel for sending replies.
    private AsyncChannel mReplyChannel = new AsyncChannel();

    private WifiP2pManager mWifiP2pManager;
    //Used to initiate a connection with WifiP2pService
    private AsyncChannel mWifiP2pChannel = new AsyncChannel();
    private AsyncChannel mWifiApConfigChannel = new AsyncChannel();
    
    //add for 231810
    private WifiManager mWifiManager;

    // Event log tags (must be in sync with event-log-tags)
    private static final int EVENTLOG_WIFI_STATE_CHANGED        = 50021;
    private static final int EVENTLOG_WIFI_EVENT_HANDLED        = 50022;
    private static final int EVENTLOG_SUPPLICANT_STATE_CHANGED  = 50023;

    /* The base for wifi message types */
    static final int BASE = Protocol.BASE_WIFI;
    /* Load the driver */
    static final int CMD_LOAD_DRIVER                      = BASE + 1;
    /* Unload the driver */
    static final int CMD_UNLOAD_DRIVER                    = BASE + 2;
    /* Indicates driver load succeeded */
    static final int CMD_LOAD_DRIVER_SUCCESS              = BASE + 3;
    /* Indicates driver load failed */
    static final int CMD_LOAD_DRIVER_FAILURE              = BASE + 4;
    /* Indicates driver unload succeeded */
    static final int CMD_UNLOAD_DRIVER_SUCCESS            = BASE + 5;
    /* Indicates driver unload failed */
    static final int CMD_UNLOAD_DRIVER_FAILURE            = BASE + 6;

    /* Start the supplicant */
    static final int CMD_START_SUPPLICANT                 = BASE + 11;
    /* Stop the supplicant */
    static final int CMD_STOP_SUPPLICANT                  = BASE + 12;
    /* Start the driver */
    static final int CMD_START_DRIVER                     = BASE + 13;
    /* Stop the driver */
    static final int CMD_STOP_DRIVER                      = BASE + 14;
    /* Indicates Static IP succeded */
    static final int CMD_STATIC_IP_SUCCESS                = BASE + 15;
    /* Indicates Static IP failed */
    static final int CMD_STATIC_IP_FAILURE                = BASE + 16;
    /* Indicates supplicant stop failed */
    static final int CMD_STOP_SUPPLICANT_FAILED           = BASE + 17;
    /* Delayed stop to avoid shutting down driver too quick*/
    static final int CMD_DELAYED_STOP_DRIVER              = BASE + 18;


    /* Start the soft access point */
    static final int CMD_START_AP                         = BASE + 21;
    /* Indicates soft ap start succeded */
    static final int CMD_START_AP_SUCCESS                 = BASE + 22;
    /* Indicates soft ap start failed */
    static final int CMD_START_AP_FAILURE                 = BASE + 23;
    /* Stop the soft access point */
    static final int CMD_STOP_AP                          = BASE + 24;
    /* Set the soft access point configuration */
    static final int CMD_SET_AP_CONFIG                    = BASE + 25;
    /* Soft access point configuration set completed */
    static final int CMD_SET_AP_CONFIG_COMPLETED          = BASE + 26;
    /* Request the soft access point configuration */
    static final int CMD_REQUEST_AP_CONFIG                = BASE + 27;
    /* Response to access point configuration request */
    static final int CMD_RESPONSE_AP_CONFIG               = BASE + 28;
    /* Invoked when getting a tether state change notification */
    static final int CMD_TETHER_STATE_CHANGE              = BASE + 29;
    /* A delayed message sent to indicate tether state change failed to arrive */
    static final int CMD_TETHER_NOTIFICATION_TIMED_OUT    = BASE + 30;

    static final int CMD_BLUETOOTH_ADAPTER_STATE_CHANGE   = BASE + 31;

    /* Supplicant commands */
    /* Is supplicant alive ? */
    static final int CMD_PING_SUPPLICANT                  = BASE + 51;
    /* Add/update a network configuration */
    static final int CMD_ADD_OR_UPDATE_NETWORK            = BASE + 52;
    /* Delete a network */
    static final int CMD_REMOVE_NETWORK                   = BASE + 53;
    /* Enable a network. The device will attempt a connection to the given network. */
    static final int CMD_ENABLE_NETWORK                   = BASE + 54;
    /* Enable all networks */
    static final int CMD_ENABLE_ALL_NETWORKS              = BASE + 55;
    /* Disable a network. The device does not attempt a connection to the given network. */
    static final int CMD_DISABLE_NETWORK                  = BASE + 56;
    /* Blacklist network. De-prioritizes the given BSSID for connection. */
    static final int CMD_BLACKLIST_NETWORK                = BASE + 57;
    /* Clear the blacklist network list */
    static final int CMD_CLEAR_BLACKLIST                  = BASE + 58;
    /* Save configuration */
    static final int CMD_SAVE_CONFIG                      = BASE + 59;

    /* Supplicant commands after driver start*/
    /* Initiate a scan */
    static final int CMD_START_SCAN                       = BASE + 71;
    /* Set scan mode. CONNECT_MODE or SCAN_ONLY_MODE */
    static final int CMD_SET_SCAN_MODE                    = BASE + 72;
    /* Set scan type. SCAN_ACTIVE or SCAN_PASSIVE */
    static final int CMD_SET_SCAN_TYPE                    = BASE + 73;
    /* Disconnect from a network */
    static final int CMD_DISCONNECT                       = BASE + 74;
    /* Reconnect to a network */
    static final int CMD_RECONNECT                        = BASE + 75;
    /* Reassociate to a network */
    static final int CMD_REASSOCIATE                      = BASE + 76;
    /* Controls power mode and suspend mode optimizations
     *
     * When high perf mode is enabled, power mode is set to
     * POWER_MODE_ACTIVE and suspend mode optimizations are disabled
     *
     * When high perf mode is disabled, power mode is set to
     * POWER_MODE_AUTO and suspend mode optimizations are enabled
     *
     * Suspend mode optimizations include:
     * - packet filtering
     * - turn off roaming
     * - DTIM wake up settings
     */
    static final int CMD_SET_HIGH_PERF_MODE               = BASE + 77;
    /* Set the country code */
    static final int CMD_SET_COUNTRY_CODE                 = BASE + 80;
    /* Enables RSSI poll */
    static final int CMD_ENABLE_RSSI_POLL                 = BASE + 82;
    /* RSSI poll */
    static final int CMD_RSSI_POLL                        = BASE + 83;
    /* Set up packet filtering */
    static final int CMD_START_PACKET_FILTERING           = BASE + 84;
    /* Clear packet filter */
    static final int CMD_STOP_PACKET_FILTERING            = BASE + 85;

    /* arg1 values to CMD_STOP_PACKET_FILTERING and CMD_START_PACKET_FILTERING */
    static final int MULTICAST_V6  = 1;
    static final int MULTICAST_V4  = 0;

    /* Connect to a specified network (network id
     * or WifiConfiguration) This involves increasing
     * the priority of the network, enabling the network
     * (while disabling others) and issuing a reconnect.
     * Note that CMD_RECONNECT just does a reconnect to
     * an existing network. All the networks get enabled
     * upon a successful connection or a failure.
     */
    static final int CMD_CONNECT_NETWORK                  = BASE + 86;
    /* Save the specified network. This involves adding
     * an enabled network (if new) and updating the
     * config and issuing a save on supplicant config.
     */
    static final int CMD_SAVE_NETWORK                     = BASE + 87;
    /* Delete the specified network. This involves
     * removing the network and issuing a save on
     * supplicant config.
     */
    static final int CMD_FORGET_NETWORK                   = BASE + 88;
    /* Start Wi-Fi protected setup */
    static final int CMD_START_WPS                        = BASE + 89;
    /* Set the frequency band */
    static final int CMD_SET_FREQUENCY_BAND               = BASE + 90;
    /* Enable background scan for configured networks */
    static final int CMD_ENABLE_BACKGROUND_SCAN           = BASE + 91;

    /* Commands from/to the SupplicantStateTracker */
    /* Reset the supplicant state tracker */
    static final int CMD_RESET_SUPPLICANT_STATE           = BASE + 111;

    /* Commands/events reported by WpsStateMachine */
    /* Indicates the completion of WPS activity */
    static final int WPS_COMPLETED_EVENT                  = BASE + 121;
    /* Reset the WPS state machine */
    static final int CMD_RESET_WPS_STATE                  = BASE + 122;

    /* Interaction with WifiP2pService */
    public static final int WIFI_ENABLE_PENDING           = BASE + 131;
    public static final int P2P_ENABLE_PROCEED            = BASE + 132;
    
    //MTK_OP01_PROTECT_START
    public static final int CMD_SAVE_AP_PRIORITY          = BASE + 140;
    public static final int CMD_SET_CONNECT_POLICY        = BASE + 141;
    public static final int CMD_UPDATE_SETTINGS           = BASE + 142;
    public static final int CMD_UPDATE_SCAN_INTERVAL      = BASE + 143;
    //MTK_OP01_PROTECT_END
    
    public static final int CMD_DO_WPS_PBC                = BASE + 160;
    public static final int CMD_DO_WPS_PIN                = BASE + 161;
    public static final int CMD_DO_WPS_ABORT              = BASE + 162;
    public static final int CMD_DO_CTIA_TEST_ON           = BASE + 163;
    public static final int CMD_DO_CTIA_TEST_OFF          = BASE + 164;
    public static final int CMD_DO_CTIA_TEST_RATE         = BASE + 165;
    public static final int CMD_DO_CTIA_TEST_POWER        = BASE + 166;
    public static final int CMD_DO_CTIA_TEST_SET          = BASE + 167;
    public static final int CMD_DO_CTIA_TEST_GET          = BASE + 168;
    public static final int CMD_GET_CONNECTING_NETWORK_ID = BASE + 169;
    public static final int CMD_UPDATE_RSSI               = BASE + 170;
    public static final int CMD_SET_TX_POWER_ENABLED      = BASE + 171;
    public static final int CMD_SET_TX_POWER              = BASE + 172;

    private static final int CONNECT_MODE   = 1;
    private static final int SCAN_ONLY_MODE = 2;

    private static final int SCAN_ACTIVE = 1;
    private static final int SCAN_PASSIVE = 2;

    private static final int SUCCESS = 1;
    private static final int FAILURE = -1;

    /* Phone in emergency call back mode */
    private static final int IN_ECM_STATE = 1;
    private static final int NOT_IN_ECM_STATE = 0;

    /**
     * The maximum number of times we will retry a connection to an access point
     * for which we have failed in acquiring an IP address from DHCP. A value of
     * N means that we will make N+1 connection attempts in all.
     * <p>
     * See {@link Settings.Secure#WIFI_MAX_DHCP_RETRY_COUNT}. This is the default
     * value if a Settings value is not present.
     */
    private static final int DEFAULT_MAX_DHCP_RETRIES = 3;

    static final int POWER_MODE_ACTIVE = 1;
    static final int POWER_MODE_AUTO = 0;

    /* Tracks the power mode for restoration after a DHCP request/renewal goes through */
    private int mPowerMode = POWER_MODE_AUTO;

    /**
     * Default framework scan interval in milliseconds. This is used in the scenario in which
     * wifi chipset does not support background scanning to set up a
     * periodic wake up scan so that the device can connect to a new access
     * point on the move. {@link Settings.Secure#WIFI_FRAMEWORK_SCAN_INTERVAL_MS} can
     * override this.
     */
    private final int mDefaultFrameworkScanIntervalMs;

    /**
     * Default supplicant scan interval in milliseconds.
     * {@link Settings.Secure#WIFI_SUPPLICANT_SCAN_INTERVAL_MS} can override this.
     */
    private final int mDefaultSupplicantScanIntervalMs;

    /**
     * Minimum time interval between enabling all networks.
     * A device can end up repeatedly connecting to a bad network on screen on/off toggle
     * due to enabling every time. We add a threshold to avoid this.
     */
    private static final int MIN_INTERVAL_ENABLE_ALL_NETWORKS_MS = 10 * 60 * 1000; /* 10 minutes */
    private long mLastEnableAllNetworksTime;

    /**
     * Starting and shutting down driver too quick causes problems leading to driver
     * being in a bad state. Delay driver stop.
     */
    private static final int DELAYED_DRIVER_STOP_MS = 2 * 60 * 1000; /* 2 minutes */
    private int mDelayedStopCounter;
    private boolean mInDelayedStop = false;

    private static final int MIN_RSSI = -200;
    private static final int MAX_RSSI = 256;

    /* Default parent state */
    private State mDefaultState = new DefaultState();
    /* Temporary initial state */
    private State mInitialState = new InitialState();
    /* Unloading the driver */
    private State mDriverUnloadingState = new DriverUnloadingState();
    /* Loading the driver */
    private State mDriverUnloadedState = new DriverUnloadedState();
    /* Driver load/unload failed */
    private State mDriverFailedState = new DriverFailedState();
    /* Driver loading */
    private State mDriverLoadingState = new DriverLoadingState();
    /* Driver loaded */
    private State mDriverLoadedState = new DriverLoadedState();
    /* Driver loaded, waiting for supplicant to start */
    private State mSupplicantStartingState = new SupplicantStartingState();
    /* Driver loaded and supplicant ready */
    private State mSupplicantStartedState = new SupplicantStartedState();
    /* Waiting for supplicant to stop and monitor to exit */
    private State mSupplicantStoppingState = new SupplicantStoppingState();
    /* Driver start issued, waiting for completed event */
    private State mDriverStartingState = new DriverStartingState();
    /* Driver started */
    private State mDriverStartedState = new DriverStartedState();
    /* Driver stopping */
    private State mDriverStoppingState = new DriverStoppingState();
    /* Driver stopped */
    private State mDriverStoppedState = new DriverStoppedState();
    /* Scan for networks, no connection will be established */
    private State mScanModeState = new ScanModeState();
    /* Connecting to an access point */
    private State mConnectModeState = new ConnectModeState();
    /* Fetching IP after network connection (assoc+auth complete) */
    private State mConnectingState = new ConnectingState();
    /* Connected with IP addr */
    private State mConnectedState = new ConnectedState();
    /* disconnect issued, waiting for network disconnect confirmation */
    private State mDisconnectingState = new DisconnectingState();
    /* Network is not connected, supplicant assoc+auth is not complete */
    private State mDisconnectedState = new DisconnectedState();
    /* Waiting for WPS to be completed*/
    private State mWaitForWpsCompletionState = new WaitForWpsCompletionState();

    /* Soft ap is starting up */
    private State mSoftApStartingState = new SoftApStartingState();
    /* Soft ap is running */
    private State mSoftApStartedState = new SoftApStartedState();
    /* Soft ap is running and we are waiting for tether notification */
    private State mTetheringState = new TetheringState();
    /* Soft ap is running and we are tethered through connectivity service */
    private State mTetheredState = new TetheredState();
    /* Waiting for untether confirmation to stop soft Ap */
    private State mSoftApStoppingState = new SoftApStoppingState();

    /* Wait till p2p is disabled */
    private State mWaitForP2pDisableState = new WaitForP2pDisableState();

    private class TetherStateChange {
        ArrayList<String> available;
        ArrayList<String> active;
        TetherStateChange(ArrayList<String> av, ArrayList<String> ac) {
            available = av;
            active = ac;
        }
    }


    /**
     * One of  {@link WifiManager#WIFI_STATE_DISABLED},
     *         {@link WifiManager#WIFI_STATE_DISABLING},
     *         {@link WifiManager#WIFI_STATE_ENABLED},
     *         {@link WifiManager#WIFI_STATE_ENABLING},
     *         {@link WifiManager#WIFI_STATE_UNKNOWN}
     *
     */
    private final AtomicInteger mWifiState = new AtomicInteger(WIFI_STATE_DISABLED);

    /**
     * One of  {@link WifiManager#WIFI_AP_STATE_DISABLED},
     *         {@link WifiManager#WIFI_AP_STATE_DISABLING},
     *         {@link WifiManager#WIFI_AP_STATE_ENABLED},
     *         {@link WifiManager#WIFI_AP_STATE_ENABLING},
     *         {@link WifiManager#WIFI_AP_STATE_FAILED}
     *
     */
    private final AtomicInteger mWifiApState = new AtomicInteger(WIFI_AP_STATE_DISABLED);

    private final AtomicInteger mLastEnableUid = new AtomicInteger(Process.myUid());
    private final AtomicInteger mLastApEnableUid = new AtomicInteger(Process.myUid());

    private static final int SCAN_REQUEST = 0;
    private static final String ACTION_START_SCAN =
        "com.android.server.WifiManager.action.START_SCAN";

    /**
     * Keep track of whether WIFI is running.
     */
    private boolean mIsRunning = false;

    /**
     * Keep track of whether we last told the battery stats we had started.
     */
    private boolean mReportedRunning = false;

    /**
     * Most recently set source of starting WIFI.
     */
    private final WorkSource mRunningWifiUids = new WorkSource();

    /**
     * The last reported UIDs that were responsible for starting WIFI.
     */
    private final WorkSource mLastRunningWifiUids = new WorkSource();

    private final IBatteryStats mBatteryStats;
    private boolean mNextWifiActionExplicit = false;
    private int mLastExplicitNetworkId;
    private long mLastNetworkChoiceTime;
    private static final long EXPLICIT_CONNECT_ALLOWED_DELAY_MS = 2 * 60 * 1000;

    //MTK_OP01_PROTECT_START
    private String OPTR;
    private int mConnectPolicy = Settings.System.WIFI_CONNECT_AP_TYPE_AUTO;
    private int mCellToWiFiPolicy = Settings.System.WIFI_CONNECT_TYPE_AUTO;
    private boolean mScreenOn;
    //MTK_OP01_PROTECT_END
    
    private boolean mIsInterfaceConfigFail = false;

    private PendingIntent mIntentStopDriver;
    private static final int STOP_DRIVER_REQUEST = 1;
    private static final String ACTION_STOP_DRIVER = "com.android.server.WifiManager.action.STOP_DRIVER";

    private PendingIntent mIntentStopHotspot;
    private static final int STOP_HOTSPOT_REQUEST = 2;
    private static final String ACTION_STOP_HOTSPOT = "com.android.server.WifiManager.action.STOP_HOTSPOT";
    private static final long HOTSPOT_DISABLE_MS = 5 * 60 * 1000;
    private int mDuration = Settings.System.WIFI_HOTSPOT_AUTO_DISABLE_FOR_FIVE_MINS;
    private int mClientNum = 0;
    private HotspotAutoDisableObserver mHotspotAutoDisableObserver;
    
    //Add for WiFi to 3G begin
    private List<WifiConfiguration> mRssiWeakNetworks = null;
    private int mRssiWeakCounter = 0;
    private final int RSSI_WEAK_NUM = 3;
    public static final int RSSI_THRESHOLD = -85;
    boolean mIsSimAbsent = false;
    boolean mIsGPRSDisabled = false;
    private GPRSObserver mGPRSObserver;
    //Add for WiFi to 3G end

    private boolean mDeviceIdle;
    private PowerManager.WakeLock mDhcpWakeLock;
    
    public WifiStateMachine(Context context, String wlanInterface) {
        super(TAG);

        mContext = context;
        mInterfaceName = wlanInterface;

        mNetworkInfo = new NetworkInfo(ConnectivityManager.TYPE_WIFI, 0, NETWORKTYPE, "");
        mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batteryinfo"));

        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNwService = INetworkManagementService.Stub.asInterface(b);

        mWifiMonitor = new WifiMonitor(this);
        mDhcpInfoInternal = new DhcpInfoInternal();
        mWifiInfo = new WifiInfo();
        mSupplicantStateTracker = new SupplicantStateTracker(context, this, getHandler());
        mWpsStateMachine = new WpsStateMachine(context, this, getHandler());
        mLinkProperties = new LinkProperties();

        WifiApConfigStore wifiApConfigStore = WifiApConfigStore.makeWifiApConfigStore(
                context, getHandler());
        wifiApConfigStore.loadApConfiguration();
        mWifiApConfigChannel.connectSync(mContext, getHandler(), wifiApConfigStore.getMessenger());

        mNetworkInfo.setIsAvailable(false);
        mLinkProperties.clear();
        mLastBssid = null;
        mLastNetworkId = WifiConfiguration.INVALID_NETWORK_ID;
        mLastSignalLevel = -1;

        mAlarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        Intent scanIntent = new Intent(ACTION_START_SCAN, null);
        mScanIntent = PendingIntent.getBroadcast(mContext, SCAN_REQUEST, scanIntent, 0);

        mDefaultFrameworkScanIntervalMs = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_wifi_framework_scan_interval);

        mDefaultSupplicantScanIntervalMs = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_wifi_supplicant_scan_interval);

        mContext.registerReceiver(
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    ArrayList<String> available = intent.getStringArrayListExtra(
                            ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                    ArrayList<String> active = intent.getStringArrayListExtra(
                            ConnectivityManager.EXTRA_ACTIVE_TETHER);
                    sendMessage(CMD_TETHER_STATE_CHANGE, new TetherStateChange(available, active));
                }
            },new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED));

        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        startScan(false);
                    }
                },
                new IntentFilter(ACTION_START_SCAN));

        mScanResultCache = new LruCache<String, ScanResult>(SCAN_RESULT_CACHE_SIZE);

        PowerManager powerManager = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        addState(mDefaultState);
            addState(mInitialState, mDefaultState);
            addState(mDriverUnloadingState, mDefaultState);
            addState(mDriverUnloadedState, mDefaultState);
                addState(mDriverFailedState, mDriverUnloadedState);
            addState(mDriverLoadingState, mDefaultState);
            addState(mDriverLoadedState, mDefaultState);
            addState(mSupplicantStartingState, mDefaultState);
            addState(mSupplicantStartedState, mDefaultState);
                addState(mDriverStartingState, mSupplicantStartedState);
                addState(mDriverStartedState, mSupplicantStartedState);
                    addState(mScanModeState, mDriverStartedState);
                    addState(mConnectModeState, mDriverStartedState);
                        addState(mConnectingState, mConnectModeState);
                        addState(mConnectedState, mConnectModeState);
                        addState(mDisconnectingState, mConnectModeState);
                        addState(mDisconnectedState, mConnectModeState);
                        addState(mWaitForWpsCompletionState, mConnectModeState);
                addState(mDriverStoppingState, mSupplicantStartedState);
                addState(mDriverStoppedState, mSupplicantStartedState);
            addState(mSupplicantStoppingState, mDefaultState);
            addState(mSoftApStartingState, mDefaultState);
            addState(mSoftApStartedState, mDefaultState);
                addState(mTetheringState, mSoftApStartedState);
                addState(mTetheredState, mSoftApStartedState);
            addState(mSoftApStoppingState, mDefaultState);
            addState(mWaitForP2pDisableState, mDefaultState);

        setInitialState(mInitialState);

        mDhcpWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DHCP_WAKELOCK");
        mDhcpWakeLock.setReferenceCounted(false);
        WifiNative.setWifiStateMachine(this);
        Intent stopDriverIntent = new Intent(ACTION_STOP_DRIVER);
        mIntentStopDriver = PendingIntent.getBroadcast(mContext, STOP_DRIVER_REQUEST, stopDriverIntent, 0);
        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        sendMessage(obtainMessage(CMD_DELAYED_STOP_DRIVER, mDelayedStopCounter, 0));
                    }
                },
                new IntentFilter(ACTION_STOP_DRIVER));

        mHotspotAutoDisableObserver = new HotspotAutoDisableObserver(new Handler());
        Intent stopHotspotIntent = new Intent(ACTION_STOP_HOTSPOT);
        mIntentStopHotspot = PendingIntent.getBroadcast(mContext, STOP_HOTSPOT_REQUEST, stopHotspotIntent, 0);
        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        setWifiApEnabled(null, false);
                        int wifiSavedState = 0;
                        try {
                            wifiSavedState = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.WIFI_SAVED_STATE);
                        } catch (Settings.SettingNotFoundException e) {
                            Xlog.e(TAG, "SettingNotFoundException:" + e);
                        }
                        if (wifiSavedState == 1) {
                            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                            mWifiManager.setWifiEnabled(true);
                            Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.WIFI_SAVED_STATE, 0);
                        }
                    }
                },
                new IntentFilter(ACTION_STOP_HOTSPOT));
        mDuration = Settings.System.getInt(mContext.getContentResolver(), Settings.System.WIFI_HOTSPOT_AUTO_DISABLE,
                    Settings.System.WIFI_HOTSPOT_AUTO_DISABLE_FOR_FIVE_MINS);
                
        //MTK_OP01_PROTECT_START
        OPTR = SystemProperties.get("ro.operator.optr", "");
        mConnectPolicy = Settings.System.getInt(context.getContentResolver(), Settings.System.WIFI_CONNECT_AP_TYPE, Settings.System.WIFI_CONNECT_AP_TYPE_AUTO);
        mCellToWiFiPolicy = Settings.System.getInt(context.getContentResolver(), Settings.System.WIFI_CONNECT_TYPE, Settings.System.WIFI_CONNECT_TYPE_AUTO);
        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Xlog.d(TAG, "Received CONNECTIVITY_ACTION");
                        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo info = cm.getActiveNetworkInfo();
                        if (null == info) {
                            Xlog.d(TAG, "No active network");
                        } else {
                            Xlog.d(TAG, "Active network type:" + info.getTypeName());
                        }
                        if (info != null && info.getType() == ConnectivityManager.TYPE_MOBILE) {
                            sendMessage(CMD_UPDATE_SETTINGS);
                        }
                    }
                },
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        //MTK_OP01_PROTECT_END
        
        //Add for WiFi to 3G begin
        mRssiWeakNetworks = new ArrayList<WifiConfiguration>();
        mGPRSObserver = new GPRSObserver(new Handler());
        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Xlog.d(TAG, "Received ACTION_SIM_INFO_UPDATE");
                        if (FeatureOption.MTK_GEMINI_SUPPORT) {
                            int isSimInserted = SystemProperties.getInt("gsm.sim.inserted", -1);
                            Xlog.d(TAG, "isSimInserted=" + isSimInserted);
                            if (isSimInserted == 0) {
                                mIsSimAbsent = true;
                            }                                
                            int slot = -1;
                            if (FeatureOption.MTK_GEMINI_ENHANCEMENT) {
                                long currentDataConnectionMultiSimId = Settings.System.getLong(mContext.getContentResolver(), Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
                                slot = SIMInfo.getSlotById(mContext, currentDataConnectionMultiSimId);
                            } else {
                                slot = Settings.System.getInt(mContext.getContentResolver(), GPRS_CONNECTION_SETTING, GPRS_CONNECTION_SETTING_DEFAULT) - 1;
                            }
                            mIsGPRSDisabled = (slot != 1 && slot != 0);
                        } else {
                            TelephonyManager tm = TelephonyManager.getDefault();
                            int state = tm.getSimState();
                            Xlog.d(TAG, "simState=" + state);
                            if (state == TelephonyManager.SIM_STATE_ABSENT) {
                                mIsSimAbsent = true;
                            }                                
                            mIsGPRSDisabled = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.MOBILE_DATA, 1) != 1;
                        }
                    }
                },
                new IntentFilter(TelephonyIntents.ACTION_SIM_INFO_UPDATE));
        //Add for WiFi to 3G end
                
        if (DBG) setDbg(true);

        //start the state machine
        start();
    }

    /*********************************************************
     * Methods exposed for public use
     ********************************************************/

    /**
     * TODO: doc
     */
    public boolean syncPingSupplicant(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_PING_SUPPLICANT);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }

    /**
     * TODO: doc
     */
    public void startScan(boolean forceActive) {
        sendMessage(obtainMessage(CMD_START_SCAN, forceActive ?
                SCAN_ACTIVE : SCAN_PASSIVE, 0));
    }

    /**
     * TODO: doc
     */
    public void setWifiEnabled(boolean enable) {
        mLastEnableUid.set(Binder.getCallingUid());
        if (enable) {
            /* Argument is the state that is entered prior to load */
            sendMessage(obtainMessage(CMD_LOAD_DRIVER, WIFI_STATE_ENABLING, 0));
            sendMessage(CMD_START_SUPPLICANT);
        } else {
            sendMessage(CMD_STOP_SUPPLICANT);
            /* Argument is the state that is entered upon success */
            sendMessage(obtainMessage(CMD_UNLOAD_DRIVER, WIFI_STATE_DISABLED, 0));
        }
    }

    /**
     * TODO: doc
     */
    public void setWifiApEnabled(WifiConfiguration wifiConfig, boolean enable) {
        mLastApEnableUid.set(Binder.getCallingUid());
        if (enable) {
            /* Argument is the state that is entered prior to load */
            sendMessage(obtainMessage(CMD_LOAD_DRIVER, WIFI_AP_STATE_ENABLING, 0));
            sendMessage(obtainMessage(CMD_START_AP, wifiConfig));
        } else {
            sendMessage(CMD_STOP_AP);
            /* Argument is the state that is entered upon success */
            sendMessage(obtainMessage(CMD_UNLOAD_DRIVER, WIFI_AP_STATE_DISABLED, 0));
        }
    }

    public void setWifiApConfiguration(WifiConfiguration config) {
        mWifiApConfigChannel.sendMessage(CMD_SET_AP_CONFIG, config);
    }

    public WifiConfiguration syncGetWifiApConfiguration() {
        Message resultMsg = mWifiApConfigChannel.sendMessageSynchronously(CMD_REQUEST_AP_CONFIG);
        WifiConfiguration ret = (WifiConfiguration) resultMsg.obj;
        resultMsg.recycle();
        return ret;
    }

    /**
     * TODO: doc
     */
    public int syncGetWifiState() {
        return mWifiState.get();
    }

    /**
     * TODO: doc
     */
    public String syncGetWifiStateByName() {
        switch (mWifiState.get()) {
            case WIFI_STATE_DISABLING:
                return "disabling";
            case WIFI_STATE_DISABLED:
                return "disabled";
            case WIFI_STATE_ENABLING:
                return "enabling";
            case WIFI_STATE_ENABLED:
                return "enabled";
            case WIFI_STATE_UNKNOWN:
                return "unknown state";
            default:
                return "[invalid state]";
        }
    }

    /**
     * TODO: doc
     */
    public int syncGetWifiApState() {
        return mWifiApState.get();
    }

    /**
     * TODO: doc
     */
    public String syncGetWifiApStateByName() {
        switch (mWifiApState.get()) {
            case WIFI_AP_STATE_DISABLING:
                return "disabling";
            case WIFI_AP_STATE_DISABLED:
                return "disabled";
            case WIFI_AP_STATE_ENABLING:
                return "enabling";
            case WIFI_AP_STATE_ENABLED:
                return "enabled";
            case WIFI_AP_STATE_FAILED:
                return "failed";
            default:
                return "[invalid state]";
        }
    }

    /**
     * Get status information for the current connection, if any.
     * @return a {@link WifiInfo} object containing information about the current connection
     *
     */
    public WifiInfo syncRequestConnectionInfo() {
        return mWifiInfo;
    }

    public DhcpInfo syncGetDhcpInfo() {
        synchronized (mDhcpInfoInternal) {
            return mDhcpInfoInternal.makeDhcpInfo();
        }
    }

    /**
     * TODO: doc
     */
    public void setDriverStart(boolean enable, boolean ecm) {
        if (enable) {
            sendMessage(CMD_START_DRIVER);
        } else {
            sendMessage(obtainMessage(CMD_STOP_DRIVER, ecm ? IN_ECM_STATE : NOT_IN_ECM_STATE, 0));
        }
    }

    /**
     * TODO: doc
     */
    public void setScanOnlyMode(boolean enable) {
      if (enable) {
          sendMessage(obtainMessage(CMD_SET_SCAN_MODE, SCAN_ONLY_MODE, 0));
      } else {
          sendMessage(obtainMessage(CMD_SET_SCAN_MODE, CONNECT_MODE, 0));
      }
    }

    /**
     * TODO: doc
     */
    public void setScanType(boolean active) {
      if (active) {
          sendMessage(obtainMessage(CMD_SET_SCAN_TYPE, SCAN_ACTIVE, 0));
      } else {
          sendMessage(obtainMessage(CMD_SET_SCAN_TYPE, SCAN_PASSIVE, 0));
      }
    }

    /**
     * TODO: doc
     */
    public List<ScanResult> syncGetScanResultsList() {
        return mScanResults;
    }

    /**
     * Disconnect from Access Point
     */
    public void disconnectCommand() {
        sendMessage(CMD_DISCONNECT);
    }

    /**
     * Initiate a reconnection to AP
     */
    public void reconnectCommand() {
        sendMessage(CMD_RECONNECT);
    }

    /**
     * Initiate a re-association to AP
     */
    public void reassociateCommand() {
        sendMessage(CMD_REASSOCIATE);
    }

    /**
     * Add a network synchronously
     *
     * @return network id of the new network
     */
    public int syncAddOrUpdateNetwork(AsyncChannel channel, WifiConfiguration config) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_ADD_OR_UPDATE_NETWORK, config);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }

    public List<WifiConfiguration> syncGetConfiguredNetworks() {
        return WifiConfigStore.getConfiguredNetworks();
    }

    /**
     * Delete a network
     *
     * @param networkId id of the network to be removed
     */
    public boolean syncRemoveNetwork(AsyncChannel channel, int networkId) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_REMOVE_NETWORK, networkId);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }

    /**
     * Enable a network
     *
     * @param netId network id of the network
     * @param disableOthers true, if all other networks have to be disabled
     * @return {@code true} if the operation succeeds, {@code false} otherwise
     */
    public boolean syncEnableNetwork(AsyncChannel channel, int netId, boolean disableOthers) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_ENABLE_NETWORK, netId,
                disableOthers ? 1 : 0);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }

    /**
     * Disable a network
     *
     * @param netId network id of the network
     * @return {@code true} if the operation succeeds, {@code false} otherwise
     */
    public boolean syncDisableNetwork(AsyncChannel channel, int netId) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_DISABLE_NETWORK, netId,
                WifiConfiguration.DISABLED_UNKNOWN_REASON);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }

    /**
     * Blacklist a BSSID. This will avoid the AP if there are
     * alternate APs to connect
     *
     * @param bssid BSSID of the network
     */
    public void addToBlacklist(String bssid) {
        sendMessage(obtainMessage(CMD_BLACKLIST_NETWORK, bssid));
    }

    /**
     * Clear the blacklist list
     *
     */
    public void clearBlacklist() {
        sendMessage(obtainMessage(CMD_CLEAR_BLACKLIST));
    }

    public void connectNetwork(int netId) {
        sendMessage(obtainMessage(CMD_CONNECT_NETWORK, netId, 0));
    }

    public void connectNetwork(WifiConfiguration wifiConfig) {
        /* arg1 is used to indicate netId, force a netId value of
         * WifiConfiguration.INVALID_NETWORK_ID when we are passing
         * a configuration since the default value of 0 is a valid netId
         */
        sendMessage(obtainMessage(CMD_CONNECT_NETWORK, WifiConfiguration.INVALID_NETWORK_ID,
                0, wifiConfig));
    }

    public void saveNetwork(WifiConfiguration wifiConfig) {
        sendMessage(obtainMessage(CMD_SAVE_NETWORK, wifiConfig));
    }

    public void forgetNetwork(int netId) {
        sendMessage(obtainMessage(CMD_FORGET_NETWORK, netId, 0));
    }

    public void disableNetwork(Messenger replyTo, int netId, int reason) {
        Message message = obtainMessage(CMD_DISABLE_NETWORK, netId, reason);
        message.replyTo = replyTo;
        sendMessage(message);
    }

    public void startWps(Messenger replyTo, WpsInfo config) {
        Message msg = obtainMessage(CMD_START_WPS, config);
        msg.replyTo = replyTo;
        sendMessage(msg);
    }

    public void enableRssiPolling(boolean enabled) {
       sendMessage(obtainMessage(CMD_ENABLE_RSSI_POLL, enabled ? 1 : 0, 0));
    }

    public void enableBackgroundScanCommand(boolean enabled) {
       sendMessage(obtainMessage(CMD_ENABLE_BACKGROUND_SCAN, enabled ? 1 : 0, 0));
    }

    public void enableAllNetworks() {
        sendMessage(CMD_ENABLE_ALL_NETWORKS);
    }

    /**
     * Start filtering Multicast v4 packets
     */
    public void startFilteringMulticastV4Packets() {
        mFilteringMulticastV4Packets.set(true);
        sendMessage(obtainMessage(CMD_START_PACKET_FILTERING, MULTICAST_V4, 0));
    }

    /**
     * Stop filtering Multicast v4 packets
     */
    public void stopFilteringMulticastV4Packets() {
        mFilteringMulticastV4Packets.set(false);
        sendMessage(obtainMessage(CMD_STOP_PACKET_FILTERING, MULTICAST_V4, 0));
    }

    /**
     * Start filtering Multicast v4 packets
     */
    public void startFilteringMulticastV6Packets() {
        sendMessage(obtainMessage(CMD_START_PACKET_FILTERING, MULTICAST_V6, 0));
    }

    /**
     * Stop filtering Multicast v4 packets
     */
    public void stopFilteringMulticastV6Packets() {
        sendMessage(obtainMessage(CMD_STOP_PACKET_FILTERING, MULTICAST_V6, 0));
    }

    /**
     * Set high performance mode of operation.
     * Enabling would set active power mode and disable suspend optimizations;
     * disabling would set auto power mode and enable suspend optimizations
     * @param enable true if enable, false otherwise
     */
    public void setHighPerfModeEnabled(boolean enable) {
        sendMessage(obtainMessage(CMD_SET_HIGH_PERF_MODE, enable ? 1 : 0, 0));
    }

    /**
     * Set the country code
     * @param countryCode following ISO 3166 format
     * @param persist {@code true} if the setting should be remembered.
     */
    public void setCountryCode(String countryCode, boolean persist) {
        if (persist) {
            Settings.Secure.putString(mContext.getContentResolver(),
                    Settings.Secure.WIFI_COUNTRY_CODE,
                    countryCode);
        }
        sendMessage(obtainMessage(CMD_SET_COUNTRY_CODE, countryCode));
    }

    /**
     * Set the operational frequency band
     * @param band
     * @param persist {@code true} if the setting should be remembered.
     */
    public void setFrequencyBand(int band, boolean persist) {
        if (persist) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.WIFI_FREQUENCY_BAND,
                    band);
        }
        sendMessage(obtainMessage(CMD_SET_FREQUENCY_BAND, band, 0));
    }

    /**
     * Returns the operational frequency band
     */
    public int getFrequencyBand() {
        return mFrequencyBand.get();
    }

    /**
     * Returns the wifi configuration file
     */
    public String getConfigFile() {
        return WifiConfigStore.getConfigFile();
    }

    /**
     * Send a message indicating bluetooth adapter connection state changed
     */
    public void sendBluetoothAdapterStateChange(int state) {
        sendMessage(obtainMessage(CMD_BLUETOOTH_ADAPTER_STATE_CHANGE, state, 0));
    }

    /**
     * Save configuration on supplicant
     *
     * @return {@code true} if the operation succeeds, {@code false} otherwise
     *
     * TODO: deprecate this
     */
    public boolean syncSaveConfig(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_SAVE_CONFIG);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }

    public void updateBatteryWorkSource(WorkSource newSource) {
        synchronized (mRunningWifiUids) {
            try {
                if (newSource != null) {
                    mRunningWifiUids.set(newSource);
                }
                if (mIsRunning) {
                    if (mReportedRunning) {
                        // If the work source has changed since last time, need
                        // to remove old work from battery stats.
                        if (mLastRunningWifiUids.diff(mRunningWifiUids)) {
                            mBatteryStats.noteWifiRunningChanged(mLastRunningWifiUids,
                                    mRunningWifiUids);
                            mLastRunningWifiUids.set(mRunningWifiUids);
                        }
                    } else {
                        // Now being started, report it.
                        mBatteryStats.noteWifiRunning(mRunningWifiUids);
                        mLastRunningWifiUids.set(mRunningWifiUids);
                        mReportedRunning = true;
                    }
                } else {
                    if (mReportedRunning) {
                        // Last reported we were running, time to stop.
                        mBatteryStats.noteWifiStopped(mLastRunningWifiUids);
                        mLastRunningWifiUids.clear();
                        mReportedRunning = false;
                    }
                }
                mWakeLock.setWorkSource(newSource);
            } catch (RemoteException ignore) {
            }
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String LS = System.getProperty("line.separator");
        sb.append("current HSM state: ").append(getCurrentState().getName()).append(LS);
        sb.append("mLinkProperties ").append(mLinkProperties).append(LS);
        sb.append("mWifiInfo ").append(mWifiInfo).append(LS);
        sb.append("mDhcpInfoInternal ").append(mDhcpInfoInternal).append(LS);
        sb.append("mNetworkInfo ").append(mNetworkInfo).append(LS);
        sb.append("mLastSignalLevel ").append(mLastSignalLevel).append(LS);
        sb.append("mLastBssid ").append(mLastBssid).append(LS);
        sb.append("mLastNetworkId ").append(mLastNetworkId).append(LS);
        sb.append("mReconnectCount ").append(mReconnectCount).append(LS);
        sb.append("mIsScanMode ").append(mIsScanMode).append(LS);
        sb.append("Supplicant status").append(LS)
                .append(WifiNative.statusCommand()).append(LS).append(LS);

        sb.append(WifiConfigStore.dump());
        return sb.toString();
    }

    /*********************************************************
     * Internal private functions
     ********************************************************/

    private void checkAndSetConnectivityInstance() {
        if (mCm == null) {
            mCm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
    }

    private boolean startTethering(ArrayList<String> available) {

        boolean wifiAvailable = false;

        checkAndSetConnectivityInstance();

        String[] wifiRegexs = mCm.getTetherableWifiRegexs();

        for (String intf : available) {
            for (String regex : wifiRegexs) {
                if (intf.matches(regex)) {

                    InterfaceConfiguration ifcg = null;
                    try {
                        ifcg = mNwService.getInterfaceConfig(intf);
                        if (ifcg != null) {
                            /* IP/netmask: 192.168.43.1/255.255.255.0 */
                            ifcg.addr = new LinkAddress(NetworkUtils.numericToInetAddress(
                                    "192.168.43.1"), 24);
                            ifcg.interfaceFlags = "[up]";

                            mNwService.setInterfaceConfig(intf, ifcg);
                        }
                    } catch (Exception e) {
                        loge("Error configuring interface " + intf + ", :" + e);
                        return false;
                    }

                    if(mCm.tether(intf) != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        loge("Error tethering on " + intf);
                        return false;
                    }
                    mTetherInterfaceName = intf;
                    return true;
                }
            }
        }
        // We found no interfaces to tether
        return false;
    }

    private void stopTethering() {

        checkAndSetConnectivityInstance();

        /* Clear the interface config to allow dhcp correctly configure new
           ip settings */
        InterfaceConfiguration ifcg = null;
        try {
            ifcg = mNwService.getInterfaceConfig(mInterfaceName);
            if (ifcg != null) {
                ifcg.addr = new LinkAddress(NetworkUtils.numericToInetAddress(
                            "0.0.0.0"), 0);
                mNwService.setInterfaceConfig(mInterfaceName, ifcg);
            }
        } catch (Exception e) {
            loge("Error resetting interface " + mInterfaceName + ", :" + e);
        }

        if (mCm.untether(mTetherInterfaceName) != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
            loge("Untether initiate failed!");
        }
    }

    private boolean isWifiTethered(ArrayList<String> active) {

        checkAndSetConnectivityInstance();

        String[] wifiRegexs = mCm.getTetherableWifiRegexs();
        for (String intf : active) {
            for (String regex : wifiRegexs) {
                if (intf.matches(regex)) {
                    return true;
                }
            }
        }
        // We found no interfaces that are tethered
        return false;
    }

    /**
     * Set the country code from the system setting value, if any.
     */
    private void setCountryCode() {
        String countryCode = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.WIFI_COUNTRY_CODE);
        if (countryCode != null && !countryCode.isEmpty()) {
            setCountryCode(countryCode, false);
        } else {
            //use driver default
        }
    }

    /**
     * Set the frequency band from the system setting value, if any.
     */
    private void setFrequencyBand() {
        int band = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.WIFI_FREQUENCY_BAND, WifiManager.WIFI_FREQUENCY_BAND_AUTO);
        setFrequencyBand(band, false);
    }

    private void setWifiState(int wifiState) {
        final int previousWifiState = mWifiState.get();

        try {
            if (wifiState == WIFI_STATE_ENABLED) {
                mBatteryStats.noteWifiOn();
            } else if (wifiState == WIFI_STATE_DISABLED) {
                mBatteryStats.noteWifiOff();
            }
        } catch (RemoteException e) {
            loge("Failed to note battery stats in wifi");
        }

        mWifiState.set(wifiState);

        if (DBG) log("setWifiState: " + syncGetWifiStateByName());

        final Intent intent = new Intent(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        intent.putExtra(WifiManager.EXTRA_WIFI_STATE, wifiState);
        intent.putExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, previousWifiState);
        mContext.sendStickyBroadcast(intent);
    }

    private void setWifiApState(int wifiApState) {
        final int previousWifiApState = mWifiApState.get();

        try {
            if (wifiApState == WIFI_AP_STATE_ENABLED) {
                mBatteryStats.noteWifiOn();
            } else if (wifiApState == WIFI_AP_STATE_DISABLED) {
                mBatteryStats.noteWifiOff();
            }
        } catch (RemoteException e) {
            loge("Failed to note battery stats in wifi");
        }

        // Update state
        mWifiApState.set(wifiApState);

        if (DBG) log("setWifiApState: " + syncGetWifiApStateByName());

        final Intent intent = new Intent(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        intent.putExtra(WifiManager.EXTRA_WIFI_AP_STATE, wifiApState);
        intent.putExtra(WifiManager.EXTRA_PREVIOUS_WIFI_AP_STATE, previousWifiApState);
        mContext.sendStickyBroadcast(intent);
    }

    /**
     * Parse the scan result line passed to us by wpa_supplicant (helper).
     * @param line the line to parse
     * @return the {@link ScanResult} object
     */
    private ScanResult parseScanResult(String line) {
        ScanResult scanResult = null;
        if (line != null) {
            /*
             * Cache implementation (LinkedHashMap) is not synchronized, thus,
             * must synchronized here!
             */
            synchronized (mScanResultCache) {
                String[] result = scanResultPattern.split(line);
                if (3 <= result.length && result.length <= 5) {
                    String bssid = result[0];
                    // bssid | frequency | level | flags | ssid
                    int frequency;
                    int level;
                    try {
                        frequency = Integer.parseInt(result[1]);
                        level = Integer.parseInt(result[2]);
                        /* some implementations avoid negative values by adding 256
                         * so we need to adjust for that here.
                         */
                        if (level > 0) level -= 256;
                    } catch (NumberFormatException e) {
                        frequency = 0;
                        level = 0;
                    }

                    /*
                     * The formatting of the results returned by
                     * wpa_supplicant is intended to make the fields
                     * line up nicely when printed,
                     * not to make them easy to parse. So we have to
                     * apply some heuristics to figure out which field
                     * is the SSID and which field is the flags.
                     */
                    String ssid;
                    String flags;
                    if (result.length == 4) {
                        if (result[3].charAt(0) == '[') {
                            flags = result[3];
                            ssid = "";
                        } else {
                            flags = "";
                            ssid = result[3];
                        }
                    } else if (result.length == 5) {
                        flags = result[3];
                        ssid = result[4];
                    } else {
                        // Here, we must have 3 fields: no flags and ssid
                        // set
                        flags = "";
                        ssid = "";
                    }

                    // bssid + ssid is the hash key
                    String key = bssid + ssid;
                    scanResult = mScanResultCache.get(key);
                    if (scanResult != null) {
                        scanResult.level = level;
                        scanResult.SSID = ssid;
                        scanResult.capabilities = flags;
                        scanResult.frequency = frequency;
                    } else {
                        // Do not add scan results that have no SSID set
                        if (0 < ssid.trim().length()) {
                            scanResult =
                                new ScanResult(
                                    ssid, bssid, flags, level, frequency);
                            mScanResultCache.put(key, scanResult);
                        }
                    }
                } else {
                    loge("Misformatted scan result text with " +
                          result.length + " fields: " + line);
                }
            }
        }

        return scanResult;
    }

    /**
     * scanResults input format
     * 00:bb:cc:dd:cc:ee       2427    166     [WPA-EAP-TKIP][WPA2-EAP-CCMP]   Net1
     * 00:bb:cc:dd:cc:ff       2412    165     [WPA-EAP-TKIP][WPA2-EAP-CCMP]   Net2
     */
    private void setScanResults(String scanResults) {
        if (scanResults == null) {
            return;
        }

        List<ScanResult> scanList = new ArrayList<ScanResult>();

        int lineCount = 0;

        int scanResultsLen = scanResults.length();
        // Parse the result string, keeping in mind that the last line does
        // not end with a newline.
        for (int lineBeg = 0, lineEnd = 0; lineEnd <= scanResultsLen; ++lineEnd) {
            if (lineEnd == scanResultsLen || scanResults.charAt(lineEnd) == '\n') {
                ++lineCount;

                if (lineCount == 1) {
                    lineBeg = lineEnd + 1;
                    continue;
                }
                if (lineEnd > lineBeg) {
                    String line = scanResults.substring(lineBeg, lineEnd);
                    ScanResult scanResult = parseScanResult(line);
                    if (scanResult != null) {
                        scanList.add(scanResult);
                    } else {
                        //TODO: hidden network handling
                    }
                }
                lineBeg = lineEnd + 1;
            }
        }

        mScanResults = scanList;
    }

    private String fetchSSID() {
        String status = WifiNative.statusCommand();
        if (status == null) {
            return null;
        }
        // extract ssid from a series of "name=value"
        String[] lines = status.split("\n");
        for (String line : lines) {
            String[] prop = line.split(" *= *");
            if (prop.length < 2) continue;
            String name = prop[0];
            String value = prop[1];
            if (name.equalsIgnoreCase("ssid")) return value;
        }
        return null;
    }

    /*
     * Fetch RSSI and linkspeed on current connection
     */
    private void fetchRssiAndLinkSpeedNative() {
        int newRssi = -1;
        int newLinkSpeed = -1;

        String signalPoll = WifiNative.signalPoll();

        if (signalPoll != null) {
            String[] lines = signalPoll.split("\n");
            for (String line : lines) {
                String[] prop = line.split("=");
                if (prop.length < 2) continue;
                try {
                    if (prop[0].equals("RSSI")) {
                        newRssi = Integer.parseInt(prop[1]);
                    } else if (prop[0].equals("LINKSPEED")) {
                        newLinkSpeed = Integer.parseInt(prop[1]);
                    }
                } catch (NumberFormatException e) {
                    //Ignore, defaults on rssi and linkspeed are assigned
                }
            }
        }

        Xlog.i(TAG, "fetchRssiAndLinkSpeedNative, newRssi=" + newRssi);
        if (newRssi != -1 && MIN_RSSI < newRssi && newRssi < MAX_RSSI) { // screen out invalid values
            /* some implementations avoid negative values by adding 256
             * so we need to adjust for that here.
             */
            if (newRssi > 0) newRssi -= 256;
            mWifiInfo.setRssi(newRssi);
            /*
             * Rather then sending the raw RSSI out every time it
             * changes, we precalculate the signal level that would
             * be displayed in the status bar, and only send the
             * broadcast if that much more coarse-grained number
             * changes. This cuts down greatly on the number of
             * broadcasts, at the cost of not mWifiInforming others
             * interested in RSSI of all the changes in signal
             * level.
             */
            // TODO: The second arg to the call below needs to be a symbol somewhere, but
            // it's actually the size of an array of icons that's private
            // to StatusBar Policy.
            if (getCurrentState() == mConnectedState) {
                int newSignalLevel = WifiManager.calculateSignalLevel(newRssi, 4);
                if (newSignalLevel != mLastSignalLevel) {
                    sendRssiChangeBroadcast(newRssi);
                }
                mLastSignalLevel = newSignalLevel;
            }
        } else {
            mWifiInfo.setRssi(MIN_RSSI);
        }

        if (newLinkSpeed != -1) {
            mWifiInfo.setLinkSpeed(newLinkSpeed);
        }
    }

    private void setHighPerfModeEnabledNative(boolean enable) {
        if(!WifiNative.setSuspendOptimizationsCommand(!enable)) {
            loge("set suspend optimizations failed!");
        }
        if (enable) {
            if (!WifiNative.setPowerModeCommand(POWER_MODE_ACTIVE)) {
                loge("set power mode active failed!");
            }
        } else {
            if (!WifiNative.setPowerModeCommand(POWER_MODE_AUTO)) {
                loge("set power mode auto failed!");
            }
        }
    }

    private void configureLinkProperties() {
        if (WifiConfigStore.isUsingStaticIp(mLastNetworkId)) {
            mLinkProperties = WifiConfigStore.getLinkProperties(mLastNetworkId);
        } else {
            synchronized (mDhcpInfoInternal) {
                mLinkProperties = mDhcpInfoInternal.makeLinkProperties();
            }
            mLinkProperties.setHttpProxy(WifiConfigStore.getProxyProperties(mLastNetworkId));
        }
        mLinkProperties.setInterfaceName(mInterfaceName);
        if (DBG) {
            log("netId=" + mLastNetworkId  + " Link configured: " +
                    mLinkProperties.toString());
        }
    }

    private int getMaxDhcpRetries() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                                      Settings.Secure.WIFI_MAX_DHCP_RETRY_COUNT,
                                      DEFAULT_MAX_DHCP_RETRIES);
    }

    private void sendScanResultsAvailableBroadcast() {
        Intent intent = new Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mContext.sendBroadcast(intent);
    }

    private void sendRssiChangeBroadcast(final int newRssi) {
        Intent intent = new Intent(WifiManager.RSSI_CHANGED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        intent.putExtra(WifiManager.EXTRA_NEW_RSSI, newRssi);
        mContext.sendBroadcast(intent);
    }

    private void sendNetworkStateChangeBroadcast(String bssid) {
        Intent intent = new Intent(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                | Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(WifiManager.EXTRA_NETWORK_INFO, new NetworkInfo(mNetworkInfo));
        intent.putExtra(WifiManager.EXTRA_LINK_PROPERTIES, new LinkProperties (mLinkProperties));
        if (bssid != null)
            intent.putExtra(WifiManager.EXTRA_BSSID, bssid);
        if (mNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
            fetchRssiAndLinkSpeedNative();
            intent.putExtra(WifiManager.EXTRA_WIFI_INFO, new WifiInfo(mWifiInfo));
        }
        mContext.sendStickyBroadcast(intent);
    }

    private void sendErrorBroadcast(int errorCode) {
        Intent intent = new Intent(WifiManager.ERROR_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        intent.putExtra(WifiManager.EXTRA_ERROR_CODE, errorCode);
        mContext.sendBroadcast(intent);
    }

    private void sendLinkConfigurationChangedBroadcast() {
        Intent intent = new Intent(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        intent.putExtra(WifiManager.EXTRA_LINK_PROPERTIES, new LinkProperties(mLinkProperties));
        mContext.sendBroadcast(intent);
    }

    private void sendSupplicantConnectionChangedBroadcast(boolean connected) {
        Intent intent = new Intent(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        intent.putExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, connected);
        mContext.sendBroadcast(intent);
    }

    /**
     * Record the detailed state of a network.
     * @param state the new @{code DetailedState}
     */
    private void setNetworkDetailedState(NetworkInfo.DetailedState state) {
        if (DBG) {
            log("setDetailed state, old ="
                    + mNetworkInfo.getDetailedState() + " and new state=" + state);
        }

        if (state != mNetworkInfo.getDetailedState()) {
            mNetworkInfo.setDetailedState(state, null, null);
        }
    }

    private DetailedState getNetworkDetailedState() {
        return mNetworkInfo.getDetailedState();
    }


    private SupplicantState handleSupplicantStateChange(Message message) {
        StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
        SupplicantState state = stateChangeResult.state;
        // Supplicant state change
        // [31-13] Reserved for future use
        // [8 - 0] Supplicant state (as defined in SupplicantState.java)
        // 50023 supplicant_state_changed (custom|1|5)
        EventLog.writeEvent(EVENTLOG_SUPPLICANT_STATE_CHANGED, state.ordinal());
        mWifiInfo.setSupplicantState(state);
        // Network id is only valid when we start connecting
        if (SupplicantState.isConnecting(state)) {
            mWifiInfo.setNetworkId(stateChangeResult.networkId);
        } else {
            mWifiInfo.setNetworkId(WifiConfiguration.INVALID_NETWORK_ID);
        }

        if (state == SupplicantState.ASSOCIATING) {
            /* BSSID is valid only in ASSOCIATING state */
            mWifiInfo.setBSSID(stateChangeResult.BSSID);
        }

        mSupplicantStateTracker.sendMessage(Message.obtain(message));
        mWpsStateMachine.sendMessage(Message.obtain(message));

        return state;
    }

    /**
     * Resets the Wi-Fi Connections by clearing any state, resetting any sockets
     * using the interface, stopping DHCP & disabling interface
     */
    private void handleNetworkDisconnect() {
        if (DBG) log("Stopping DHCP and clearing IP");
        
        //MTK_OP01_PROTECT_START
        disableLastNetwork();
        //MTK_OP01_PROTECT_END

        /*
         * stop DHCP
         */
        if (mDhcpStateMachine != null) {
            mDhcpStateMachine.sendMessage(DhcpStateMachine.CMD_STOP_DHCP);
            mDhcpStateMachine.quit();
            mDhcpStateMachine = null;
        }

        try {
            mNwService.clearInterfaceAddresses(mInterfaceName);
            mNwService.disableIpv6(mInterfaceName);
        } catch (Exception e) {
            loge("Failed to clear addresses or disable ipv6" + e);
        }

        /* Reset data structures */
        mWifiInfo.setInetAddress(null);
        mWifiInfo.setBSSID(null);
        mWifiInfo.setSSID(null);
        mWifiInfo.setNetworkId(WifiConfiguration.INVALID_NETWORK_ID);
        mWifiInfo.setRssi(MIN_RSSI);
        mWifiInfo.setLinkSpeed(-1);
        mWifiInfo.setExplicitConnect(false);

        /* send event to CM & network change broadcast */
        if (mIsInterfaceConfigFail) {
            setNetworkDetailedState(DetailedState.FAILED);
            mIsInterfaceConfigFail = false;
        } else {
            setNetworkDetailedState(DetailedState.DISCONNECTED);
        }
        sendNetworkStateChangeBroadcast(mLastBssid);

        /* Clear network properties */
        mLinkProperties.clear();
        /* Clear IP settings if the network used DHCP */
        if (!WifiConfigStore.isUsingStaticIp(mLastNetworkId)) {
            WifiConfigStore.clearIpConfiguration(mLastNetworkId);
        }

        mLastBssid = null;
        mLastNetworkId = WifiConfiguration.INVALID_NETWORK_ID;
    }

    void handlePreDhcpSetup() {
    	mDhcpWakeLock.acquire(40000);
        if (!mBluetoothConnectionActive) {
            /*
             * There are problems setting the Wi-Fi driver's power
             * mode to active when bluetooth coexistence mode is
             * enabled or sense.
             * <p>
             * We set Wi-Fi to active mode when
             * obtaining an IP address because we've found
             * compatibility issues with some routers with low power
             * mode.
             * <p>
             * In order for this active power mode to properly be set,
             * we disable coexistence mode until we're done with
             * obtaining an IP address.  One exception is if we
             * are currently connected to a headset, since disabling
             * coexistence would interrupt that connection.
             */
            // Disable the coexistence mode
            WifiNative.setBluetoothCoexistenceModeCommand(
                    WifiNative.BLUETOOTH_COEXISTENCE_MODE_DISABLED);
        }

        mPowerMode =  WifiNative.getPowerModeCommand();
        if (mPowerMode < 0) {
            // Handle the case where supplicant driver does not support
            // getPowerModeCommand.
            mPowerMode = WifiStateMachine.POWER_MODE_AUTO;
        }
        if (mPowerMode != WifiStateMachine.POWER_MODE_ACTIVE) {
            WifiNative.setPowerModeCommand(WifiStateMachine.POWER_MODE_ACTIVE);
        }
    }


    void handlePostDhcpSetup() {
        /* restore power mode */
        WifiNative.setPowerModeCommand(mPowerMode);

        // Set the coexistence mode back to its default value
        WifiNative.setBluetoothCoexistenceModeCommand(
                WifiNative.BLUETOOTH_COEXISTENCE_MODE_SENSE);
       	mDhcpWakeLock.release();
    }

    private void handleSuccessfulIpConfiguration(DhcpInfoInternal dhcpInfoInternal) {
        mIsInterfaceConfigFail = false;
        synchronized (mDhcpInfoInternal) {
            mDhcpInfoInternal = dhcpInfoInternal;
        }
        mLastSignalLevel = -1; // force update of signal strength
        mReconnectCount = 0; //Reset IP failure tracking
        WifiConfigStore.setIpConfiguration(mLastNetworkId, dhcpInfoInternal);
        InetAddress addr = NetworkUtils.numericToInetAddress(dhcpInfoInternal.ipAddress);
        mWifiInfo.setInetAddress(addr);
        if (getNetworkDetailedState() == DetailedState.CONNECTED) {
            //DHCP renewal in connected state
            LinkProperties linkProperties = dhcpInfoInternal.makeLinkProperties();
            linkProperties.setHttpProxy(WifiConfigStore.getProxyProperties(mLastNetworkId));
            linkProperties.setInterfaceName(mInterfaceName);
            if (!linkProperties.equals(mLinkProperties)) {
                if (DBG) {
                    log("Link configuration changed for netId: " + mLastNetworkId
                            + " old: " + mLinkProperties + "new: " + linkProperties);
                }
                mLinkProperties = linkProperties;
                sendLinkConfigurationChangedBroadcast();
            }
        } else {
            configureLinkProperties();
            setNetworkDetailedState(DetailedState.CONNECTED);
            sendNetworkStateChangeBroadcast(mLastBssid);
        }
    }

    private void handleFailedIpConfiguration() {
        loge("IP configuration failed");
        mIsInterfaceConfigFail = true;
        mWifiInfo.setInetAddress(null);
        /**
         * If we've exceeded the maximum number of retries for DHCP
         * to a given network, disable the network
         */
        if (++mReconnectCount > getMaxDhcpRetries()) {
            loge("Failed " +
                    mReconnectCount + " times, Disabling " + mLastNetworkId);
            WifiConfigStore.disableNetwork(mLastNetworkId,
                    WifiConfiguration.DISABLED_DHCP_FAILURE);
            mReconnectCount = 0;
        }

        /* DHCP times out after about 30 seconds, we do a
         * disconnect and an immediate reconnect to try again
         */
        WifiNative.disconnectCommand();
        WifiNative.reconnectCommand();
    }

    /* Current design is to not set the config on a running hostapd but instead
     * stop and start tethering when user changes config on a running access point
     *
     * TODO: Add control channel setup through hostapd that allows changing config
     * on a running daemon
     */
    private void startSoftApWithConfig(final WifiConfiguration config) {
        // start hostapd on a seperate thread
        new Thread(new Runnable() {
            public void run() {
                if (DBG) Xlog.d(TAG, "startSoftApWithConfig, config:" + config);
                try {
                    mNwService.startAccessPoint(config, mInterfaceName, SOFTAP_IFACE);
                } catch (Exception e) {
                    loge("Exception in softap start " + e);
                    try {
                        mNwService.stopAccessPoint(mInterfaceName);
                        mNwService.startAccessPoint(config, mInterfaceName, SOFTAP_IFACE);
                    } catch (Exception e1) {
                        loge("Exception in softap re-start " + e1);
                        sendMessage(CMD_START_AP_FAILURE);
                        return;
                    }
                }
                if (DBG) log("Soft AP start successful");
                sendMessage(CMD_START_AP_SUCCESS);
            }
        }, "startSoftApThread").start();
    }

    /********************************************************
     * HSM states
     *******************************************************/

    class DefaultState extends State {
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch (message.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED:
                    if (message.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
                        mWifiP2pChannel.sendMessage(AsyncChannel.CMD_CHANNEL_FULL_CONNECTION);
                    } else {
                        loge("WifiP2pService connection failure, error=" + message.arg1);
                    }
                    break;
                case AsyncChannel.CMD_CHANNEL_DISCONNECTED:
                    loge("WifiP2pService channel lost, message.arg1 =" + message.arg1);
                    //TODO: Re-establish connection to state machine after a delay
                    //mWifiP2pChannel.connect(mContext, getHandler(), mWifiP2pManager.getMessenger());
                    break;
                case CMD_BLUETOOTH_ADAPTER_STATE_CHANGE:
                    mBluetoothConnectionActive = (message.arg1 !=
                            BluetoothAdapter.STATE_DISCONNECTED);
                    break;
                    /* Synchronous call returns */
                case CMD_PING_SUPPLICANT:
                case CMD_ENABLE_NETWORK:
                case CMD_DISABLE_NETWORK:
                case CMD_ADD_OR_UPDATE_NETWORK:
                case CMD_REMOVE_NETWORK:
                case CMD_SAVE_CONFIG:
                //MTK_OP01_PROTECT_START
                case CMD_SAVE_AP_PRIORITY:
                //MTK_OP01_PROTECT_END
                case CMD_DO_WPS_PBC:                
                case CMD_DO_WPS_ABORT:
                case CMD_DO_CTIA_TEST_ON:
                case CMD_DO_CTIA_TEST_OFF:
                case CMD_DO_CTIA_TEST_RATE:
                case CMD_DO_CTIA_TEST_POWER:
                case CMD_DO_CTIA_TEST_SET:
                case CMD_UPDATE_RSSI:
                    mReplyChannel.replyToMessage(message, message.what, FAILURE);
                    break;
                case CMD_DO_WPS_PIN:
                case CMD_DO_CTIA_TEST_GET:
                    mReplyChannel.replyToMessage(message, message.what, null);
                    break;
                case CMD_GET_CONNECTING_NETWORK_ID:
                    mReplyChannel.replyToMessage(message, message.what, WifiConfiguration.INVALID_NETWORK_ID);
                    break;
                case CMD_ENABLE_RSSI_POLL:
                    mEnableRssiPolling = (message.arg1 == 1);
                    break;
                case CMD_ENABLE_BACKGROUND_SCAN:
                    mEnableBackgroundScan = (message.arg1 == 1);
                    break;
                    /* Discard */
                case CMD_LOAD_DRIVER:
                case CMD_UNLOAD_DRIVER:
                case CMD_START_SUPPLICANT:
                case CMD_STOP_SUPPLICANT:
                case CMD_STOP_SUPPLICANT_FAILED:
                case CMD_START_DRIVER:
                case CMD_STOP_DRIVER:
                case CMD_DELAYED_STOP_DRIVER:
                case CMD_START_AP:
                case CMD_START_AP_SUCCESS:
                case CMD_START_AP_FAILURE:
                case CMD_STOP_AP:
                case CMD_TETHER_STATE_CHANGE:
                case CMD_TETHER_NOTIFICATION_TIMED_OUT:
                case CMD_START_SCAN:
                case CMD_DISCONNECT:
                case CMD_RECONNECT:
                case CMD_REASSOCIATE:
                case WifiMonitor.SUP_CONNECTION_EVENT:
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                case WifiMonitor.SCAN_RESULTS_EVENT:
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                case WifiMonitor.WPS_OVERLAP_EVENT:
                case CMD_BLACKLIST_NETWORK:
                case CMD_CLEAR_BLACKLIST:
                case CMD_SET_SCAN_MODE:
                case CMD_SET_SCAN_TYPE:
                case CMD_SET_HIGH_PERF_MODE:
                case CMD_SET_COUNTRY_CODE:
                case CMD_SET_FREQUENCY_BAND:
                case CMD_CONNECT_NETWORK:
                case CMD_SAVE_NETWORK:
                case CMD_FORGET_NETWORK:
                case CMD_RSSI_POLL:
                case CMD_ENABLE_ALL_NETWORKS:
                case DhcpStateMachine.CMD_PRE_DHCP_ACTION:
                case DhcpStateMachine.CMD_POST_DHCP_ACTION:
                /* Handled by WifiApConfigStore */
                case CMD_SET_AP_CONFIG:
                case CMD_SET_AP_CONFIG_COMPLETED:
                case CMD_REQUEST_AP_CONFIG:
                case CMD_RESPONSE_AP_CONFIG:
                //MTK_OP01_PROTECT_START
                case CMD_UPDATE_SETTINGS:
                //MTK_OP01_PROTECT_END
                    break;
                case WifiMonitor.SUPPLICANT_HUNG_EVENT:
                    Xlog.e(TAG, "Supplicant hung");
                    WifiNative.killSupplicant();
                    WifiNative.closeSupplicantConnection();
                case WifiMonitor.DRIVER_HUNG_EVENT:
                    setWifiEnabled(false);
                    setWifiEnabled(true);
                    break;
                case CMD_START_WPS:
                    /* Return failure when the state machine cannot handle WPS initiation*/
                    mReplyChannel.replyToMessage(message, WifiManager.CMD_WPS_COMPLETED,
                                new WpsResult(Status.FAILURE));
                    break;
                case WifiP2pService.P2P_ENABLE_PENDING:
                    // turn off wifi and defer to be handled in DriverUnloadedState
                    mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                    mWifiManager.setWifiEnabled(false);
                    deferMessage(message);
                    break;
                case CMD_SET_TX_POWER_ENABLED:
                    boolean ok = WifiNative.setTxPowerEnabledCommand(message.arg1 == 1 ? true : false);
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                case CMD_SET_TX_POWER:
                    ok = WifiNative.setTxPowerCommand(message.arg1);
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                //MTK_OP01_PROTECT_START
                case CMD_SET_CONNECT_POLICY:
                    mConnectPolicy = message.arg1;
                    mCellToWiFiPolicy = message.arg2;
                    mReplyChannel.replyToMessage(message, message.what, SUCCESS);
                    break;
                case CMD_UPDATE_SCAN_INTERVAL:
                    mScreenOn = (message.arg1 == 1);
                    break;
                //MTK_OP01_PROTECT_END
                default:
                    loge("Error! unhandled message" + message);
                    break;
            }
            return HANDLED;
        }
    }

    class InitialState extends State {
        @Override
        //TODO: could move logging into a common class
        public void enter() {
            if (DBG) log(getName() + "\n");
            // [31-8] Reserved for future use
            // [7 - 0] HSM state change
            // 50021 wifi_state_changed (custom|1|5)
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());

            if (WifiNative.isDriverLoaded()) {
                transitionTo(mDriverLoadedState);
            }
            else {
                transitionTo(mDriverUnloadedState);
            }

            //Connect to WifiP2pService
            mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
            mWifiP2pChannel.connect(mContext, getHandler(), mWifiP2pManager.getMessenger());

            /* IPv6 is disabled at boot time and is controlled by framework
             * to be enabled only as long as we are connected to an access point
             *
             * This fixes issues, a few being:
             * - IPv6 addresses and routes stick around after disconnection
             * - When connected, the kernel is unaware and can fail to start IPv6 negotiation
             * - The kernel sometimes starts autoconfiguration when 802.1x is not complete
             */
            try {
                mNwService.disableIpv6(mInterfaceName);
            } catch (RemoteException re) {
                loge("Failed to disable IPv6: " + re);
            } catch (IllegalStateException e) {
                loge("Failed to disable IPv6: " + e);
            }
        }
    }

    class DriverLoadingState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());

            final Message message = new Message();
            message.copyFrom(getCurrentMessage());
            
            //Add for WiFi to 3G begin
            synchronized (mRssiWeakNetworks) {
                mRssiWeakNetworks.clear();
            }
            //Add for WiFi to 3G end
            
            /* TODO: add a timeout to fail when driver load is hung.
             * Similarly for driver unload.
             */
            new Thread(new Runnable() {
                public void run() {
                    if (DBG) Xlog.d(TAG, getName() + message.toString() + "\n");
                    mWakeLock.acquire();
                    //enabling state
                    switch(message.arg1) {
                        case WIFI_STATE_ENABLING:
                            setWifiState(WIFI_STATE_ENABLING);
                            break;
                        case WIFI_AP_STATE_ENABLING:
                            setWifiApState(WIFI_AP_STATE_ENABLING);
                            break;
                    }

                    if(WifiNative.loadDriver()) {
                        if (DBG) log("Driver load successful");
                        sendMessage(CMD_LOAD_DRIVER_SUCCESS);
                    } else {
                        loge("Failed to load driver!");
                        switch(message.arg1) {
                            case WIFI_STATE_ENABLING:
                                setWifiState(WIFI_STATE_UNKNOWN);
                                break;
                            case WIFI_AP_STATE_ENABLING:
                                setWifiApState(WIFI_AP_STATE_FAILED);
                                break;
                        }
                        sendMessage(CMD_LOAD_DRIVER_FAILURE);
                    }
                    mWakeLock.release();
                }
            }, "loadDriverThread").start();
        }

        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_LOAD_DRIVER_SUCCESS:
                    transitionTo(mDriverLoadedState);
                    break;
                case CMD_LOAD_DRIVER_FAILURE:
                    transitionTo(mDriverFailedState);
                    break;
                case CMD_LOAD_DRIVER:
                case CMD_UNLOAD_DRIVER:
                case CMD_START_SUPPLICANT:
                case CMD_STOP_SUPPLICANT:
                case CMD_START_AP:
                case CMD_STOP_AP:
                case CMD_START_DRIVER:
                case CMD_STOP_DRIVER:
                case CMD_SET_SCAN_MODE:
                case CMD_SET_SCAN_TYPE:
                case CMD_SET_HIGH_PERF_MODE:
                case CMD_SET_COUNTRY_CODE:
                case CMD_SET_FREQUENCY_BAND:
                case CMD_START_PACKET_FILTERING:
                case CMD_STOP_PACKET_FILTERING:
                    deferMessage(message);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class DriverLoadedState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
            mWifiInfo.setSupplicantState(SupplicantState.UNINITIALIZED);
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
                case CMD_UNLOAD_DRIVER:
                    transitionTo(mDriverUnloadingState);
                    break;
                case CMD_START_SUPPLICANT:
                    try {
                        mNwService.wifiFirmwareReload(mInterfaceName, "STA");
                    } catch (Exception e) {
                        loge("Failed to reload STA firmware " + e);
                        // continue
                    }
                   try {
                       //A runtime crash can leave the interface up and
                       //this affects connectivity when supplicant starts up.
                       //Ensure interface is down before a supplicant start.
                        mNwService.setInterfaceDown(mInterfaceName);
                        //Set privacy extensions
                        mNwService.setInterfaceIpv6PrivacyExtensions(mInterfaceName, true);
                    } catch (RemoteException re) {
                        loge("Unable to change interface settings: " + re);
                    } catch (IllegalStateException ie) {
                        loge("Unable to change interface settings: " + ie);
                    }
                    
                    //A runtime crash can leave the supplicant running
                    //Ensure supplicant is not running before a supplicant start
                    WifiNative.killSupplicant();
                    WifiNative.closeSupplicantConnection();
                    
                    if(WifiNative.startSupplicant()) {
                        if (DBG) log("Supplicant start successful");
                        mWifiMonitor.startMonitoring(false);
                        transitionTo(mSupplicantStartingState);
                    } else {
                        loge("Failed to start supplicant!");
                        sendMessage(obtainMessage(CMD_UNLOAD_DRIVER, WIFI_STATE_UNKNOWN, 0));
                    }
                    break;
                case CMD_START_AP:
                    transitionTo(mSoftApStartingState);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class DriverUnloadingState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());

            final Message message = new Message();
            message.copyFrom(getCurrentMessage());
            new Thread(new Runnable() {
                public void run() {
                    if (DBG) log(getName() + message.toString() + "\n");
                    mWakeLock.acquire();
                    if(WifiNative.unloadDriver()) {
                        if (DBG) log("Driver unload successful");
                        sendMessage(CMD_UNLOAD_DRIVER_SUCCESS);

                        switch(message.arg1) {
                            case WIFI_STATE_DISABLED:
                            case WIFI_STATE_UNKNOWN:
                                setWifiState(message.arg1);
                                break;
                            case WIFI_AP_STATE_DISABLED:
                            case WIFI_AP_STATE_FAILED:
                                setWifiApState(message.arg1);
                                break;
                        }
                    } else {
                        loge("Failed to unload driver!");
                        sendMessage(CMD_UNLOAD_DRIVER_FAILURE);

                        switch(message.arg1) {
                            case WIFI_STATE_DISABLED:
                            case WIFI_STATE_UNKNOWN:
                                setWifiState(WIFI_STATE_UNKNOWN);
                                break;
                            case WIFI_AP_STATE_DISABLED:
                            case WIFI_AP_STATE_FAILED:
                                setWifiApState(WIFI_AP_STATE_FAILED);
                                break;
                        }
                    }
                    mWakeLock.release();
                }
            }, "unloadDriverThread").start();
        }

        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_UNLOAD_DRIVER_SUCCESS:
                    transitionTo(mDriverUnloadedState);
                    break;
                case CMD_UNLOAD_DRIVER_FAILURE:
                    transitionTo(mDriverFailedState);
                    break;
                case CMD_LOAD_DRIVER:
                case CMD_UNLOAD_DRIVER:
                case CMD_START_SUPPLICANT:
                case CMD_STOP_SUPPLICANT:
                case CMD_START_AP:
                case CMD_STOP_AP:
                case CMD_START_DRIVER:
                case CMD_STOP_DRIVER:
                case CMD_SET_SCAN_MODE:
                case CMD_SET_SCAN_TYPE:
                case CMD_SET_HIGH_PERF_MODE:
                case CMD_SET_COUNTRY_CODE:
                case CMD_SET_FREQUENCY_BAND:
                case CMD_START_PACKET_FILTERING:
                case CMD_STOP_PACKET_FILTERING:
                    deferMessage(message);
                    break;
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                    Xlog.d(TAG, "DriverUnloadingState, SUP_DISCONNECTION_EVENT for hotspot");
                    WifiNative.closeSupplicantConnection();
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class DriverUnloadedState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_LOAD_DRIVER:
                    mWifiP2pChannel.sendMessage(WIFI_ENABLE_PENDING);
                    transitionTo(mWaitForP2pDisableState);
                    break;
                case WifiP2pService.P2P_ENABLE_PENDING:
                    mReplyChannel.replyToMessage(message, P2P_ENABLE_PROCEED);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class DriverFailedState extends State {
        @Override
        public void enter() {
            loge(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            return NOT_HANDLED;
        }
    }


    class SupplicantStartingState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
                case WifiMonitor.SUP_CONNECTION_EVENT:
                    if (DBG) log("Supplicant connection established");
                    setWifiState(WIFI_STATE_ENABLED);
                    mSupplicantRestartCount = 0;
                    /* Reset the supplicant state to indicate the supplicant
                     * state is not known at this time */
                    mSupplicantStateTracker.sendMessage(CMD_RESET_SUPPLICANT_STATE);
                    mWpsStateMachine.sendMessage(CMD_RESET_WPS_STATE);
                    /* Initialize data structures */
                    mLastBssid = null;
                    mLastNetworkId = WifiConfiguration.INVALID_NETWORK_ID;
                    mLastSignalLevel = -1;

                    mWifiInfo.setMacAddress(WifiNative.getMacAddressCommand());

                    WifiConfigStore.initialize(mContext);

                    //MTK_OP01_PROTECT_START
                    disableAllNetworks(false);
                    //MTK_OP01_PROTECT_END
                    
                    if (FeatureOption.MTK_EAP_SIM_AKA) {
                        if (isAirplaneModeOn()) {
                            List<WifiConfiguration> networks = syncGetConfiguredNetworks();
                            if (null != networks) {
                                for (WifiConfiguration network : networks) {
                                    String value = network.enterpriseFields[0].value();
                                    Log.d(TAG, "eap value:" + value);
                                    if (value != null && (value.equals("SIM") || value.equals("AKA"))) {
                                        WifiConfigStore.disableNetwork(network.networkId, WifiConfiguration.DISABLED_UNKNOWN_REASON);
                                    }
                                }
                            }
                        }
                    }

                    sendSupplicantConnectionChangedBroadcast(true);
                    transitionTo(mDriverStartedState);
                    break;
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                    if (++mSupplicantRestartCount <= SUPPLICANT_RESTART_TRIES) {
                        loge("Failed to setup control channel, restart supplicant");
                        WifiNative.killSupplicant();
                        transitionTo(mDriverLoadedState);
                        sendMessageDelayed(CMD_START_SUPPLICANT, SUPPLICANT_RESTART_INTERVAL_MSECS);
                    } else {
                        loge("Failed " + mSupplicantRestartCount +
                                " times to start supplicant, unload driver");
                        mSupplicantRestartCount = 0;
                        transitionTo(mDriverLoadedState);
                        sendMessage(obtainMessage(CMD_UNLOAD_DRIVER, WIFI_STATE_UNKNOWN, 0));
                    }
                    break;
                case CMD_LOAD_DRIVER:
                case CMD_UNLOAD_DRIVER:
                case CMD_START_SUPPLICANT:
                case CMD_STOP_SUPPLICANT:
                case CMD_START_AP:
                case CMD_STOP_AP:
                case CMD_START_DRIVER:
                case CMD_STOP_DRIVER:
                case CMD_SET_SCAN_MODE:
                case CMD_SET_SCAN_TYPE:
                case CMD_SET_HIGH_PERF_MODE:
                case CMD_SET_COUNTRY_CODE:
                case CMD_SET_FREQUENCY_BAND:
                case CMD_START_PACKET_FILTERING:
                case CMD_STOP_PACKET_FILTERING:
                    deferMessage(message);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class SupplicantStartedState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
            /* Initialize for connect mode operation at start */
            mIsScanMode = false;
            /* Wifi is available as long as we have a connection to supplicant */
            mNetworkInfo.setIsAvailable(true);
            /* Set scan interval */
            long supplicantScanIntervalMs = 0;
            //MTK_OP01_PROTECT_START
            if (OPTR.equals("OP01")) {
                supplicantScanIntervalMs = Settings.Secure.getLong(mContext.getContentResolver(),
                        Settings.Secure.WIFI_SUPPLICANT_SCAN_INTERVAL_MS,
                        mScreenOn ? mDefaultSupplicantScanIntervalMs : mDefaultFrameworkScanIntervalMs);
            } else
            //MTK_OP01_PROTECT_END
            {
                supplicantScanIntervalMs = Settings.Secure.getLong(mContext.getContentResolver(),
                    Settings.Secure.WIFI_SUPPLICANT_SCAN_INTERVAL_MS,
                    mDefaultSupplicantScanIntervalMs);
            }
            WifiNative.setScanIntervalCommand((int)supplicantScanIntervalMs / 1000);
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            WifiConfiguration config;
            boolean eventLoggingEnabled = true;
            switch(message.what) {
                case CMD_STOP_SUPPLICANT:   /* Supplicant stopped by user */
                    transitionTo(mSupplicantStoppingState);
                    break;
                case WifiMonitor.SUP_DISCONNECTION_EVENT:  /* Supplicant connection lost */
                    loge("Connection lost, restart supplicant");
                    WifiNative.killSupplicant();
                    WifiNative.closeSupplicantConnection();
                    mNetworkInfo.setIsAvailable(false);
                    handleNetworkDisconnect();
                    sendSupplicantConnectionChangedBroadcast(false);
                    mSupplicantStateTracker.sendMessage(CMD_RESET_SUPPLICANT_STATE);
                    mWpsStateMachine.sendMessage(CMD_RESET_WPS_STATE);
                    transitionTo(mDriverLoadedState);
                    sendMessageDelayed(CMD_START_SUPPLICANT, SUPPLICANT_RESTART_INTERVAL_MSECS);
                    break;
                case WifiMonitor.SCAN_RESULTS_EVENT:
                    eventLoggingEnabled = false;
                    setScanResults(WifiNative.scanResultsCommand());
                    sendScanResultsAvailableBroadcast();
                    mScanResultIsPending = false;
                    break;
                case CMD_PING_SUPPLICANT:
                    boolean ok = WifiNative.pingCommand();
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                case CMD_ADD_OR_UPDATE_NETWORK:
                    config = (WifiConfiguration) message.obj;
                    mReplyChannel.replyToMessage(message, CMD_ADD_OR_UPDATE_NETWORK,
                            WifiConfigStore.addOrUpdateNetwork(config));
                    break;
                case CMD_REMOVE_NETWORK:
                    ok = WifiConfigStore.removeNetwork(message.arg1);
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                case CMD_ENABLE_NETWORK:
                    ok = WifiConfigStore.enableNetwork(message.arg1, message.arg2 == 1);
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                case CMD_ENABLE_ALL_NETWORKS:
                    long time =  android.os.SystemClock.elapsedRealtime();
                    if (time - mLastEnableAllNetworksTime > MIN_INTERVAL_ENABLE_ALL_NETWORKS_MS) {
                        WifiConfigStore.enableAllNetworks();
                        mLastEnableAllNetworksTime = time;
                    }
                    break;
                case CMD_DISABLE_NETWORK:
                    ok = WifiConfigStore.disableNetwork(message.arg1, message.arg2);
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                case CMD_BLACKLIST_NETWORK:
                    WifiNative.addToBlacklistCommand((String)message.obj);
                    break;
                case CMD_CLEAR_BLACKLIST:
                    WifiNative.clearBlacklistCommand();
                    break;
                case CMD_SAVE_CONFIG:
                    ok = WifiConfigStore.saveConfig();
                    mReplyChannel.replyToMessage(message, CMD_SAVE_CONFIG, ok ? SUCCESS : FAILURE);

                    // Inform the backup manager about a data change
                    IBackupManager ibm = IBackupManager.Stub.asInterface(
                            ServiceManager.getService(Context.BACKUP_SERVICE));
                    if (ibm != null) {
                        try {
                            ibm.dataChanged("com.android.providers.settings");
                        } catch (Exception e) {
                            // Try again later
                        }
                    }
                    break;
                    /* Cannot start soft AP while in client mode */
                case CMD_START_AP:
                    loge("Failed to start soft AP with a running supplicant");
                    setWifiApState(WIFI_AP_STATE_FAILED);
                    break;
                case CMD_SET_SCAN_MODE:
                    mIsScanMode = (message.arg1 == SCAN_ONLY_MODE);
                    break;
                case CMD_SAVE_NETWORK:
                    config = (WifiConfiguration) message.obj;
                    WifiConfigStore.saveNetwork(config);
                    break;
                case CMD_FORGET_NETWORK:
                    WifiConfigStore.forgetNetwork(message.arg1);
                    mSupplicantStateTracker.sendMessage(CMD_FORGET_NETWORK, message.arg1);
                    break;
                //MTK_OP01_PROTECT_START
                case CMD_SAVE_AP_PRIORITY:
                    ok = WifiConfigStore.saveAPPriority();
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);

                    // Inform the backup manager about a data change
                    ibm = IBackupManager.Stub.asInterface(
                            ServiceManager.getService(Context.BACKUP_SERVICE));
                    if (ibm != null) {
                        try {
                            ibm.dataChanged("com.android.providers.settings");
                        } catch (Exception e) {
                            // Try again later
                        }
                    }
                    break;
                case CMD_SET_CONNECT_POLICY:
                    setConnectPolicy(message.arg1, message.arg2);
                    mReplyChannel.replyToMessage(message, message.what, SUCCESS);
                    break;
                case CMD_UPDATE_SETTINGS:
                    updateAutoConnectSettings();
                    break;
                case CMD_UPDATE_SCAN_INTERVAL:
                    mScreenOn = (message.arg1 == 1);
                    long supplicantScanIntervalMs = Settings.Secure.getLong(mContext.getContentResolver(),
                        Settings.Secure.WIFI_SUPPLICANT_SCAN_INTERVAL_MS,
                        mScreenOn ? mDefaultSupplicantScanIntervalMs : mDefaultFrameworkScanIntervalMs);
                    WifiNative.setScanIntervalCommand((int)supplicantScanIntervalMs / 1000);
                    break;
                //MTK_OP01_PROTECT_END
                case CMD_DO_WPS_PBC:
                    ok = WifiNative.doWpsPbcCommand((String)message.obj);
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                case CMD_DO_WPS_PIN:
                    String result = WifiNative.doWpsPinCommand((String)message.obj);
                    mReplyChannel.replyToMessage(message, message.what, result);
                    break;
                case CMD_DO_WPS_ABORT:
                    ok = WifiNative.doWpsAbortCommand();
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    //MTK_OP01_PROTECT_START
                    disableAllNetworks(false);
                    //MTK_OP01_PROTECT_END
                    break;
                case CMD_DO_CTIA_TEST_ON:
                    ok = WifiNative.doCTIATestOnCommand();
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                case CMD_DO_CTIA_TEST_OFF:
                    ok = WifiNative.doCTIATestOffCommand();
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                case CMD_DO_CTIA_TEST_RATE:
                    ok = WifiNative.doCTIATestRateCommand(message.arg1);
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                case CMD_DO_CTIA_TEST_POWER:
                    ok = WifiNative.doCTIATestPowerCommand(message.arg1);
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                case CMD_DO_CTIA_TEST_SET:
                    ok = WifiNative.doCTIATestSetCommand(message.arg1, message.arg2);
                    mReplyChannel.replyToMessage(message, message.what, ok ? SUCCESS : FAILURE);
                    break;
                case CMD_DO_CTIA_TEST_GET:
                    result = WifiNative.doCTIATestGetCommand(message.arg1);
                    mReplyChannel.replyToMessage(message, message.what, result);
                    break;
                case CMD_GET_CONNECTING_NETWORK_ID:
                    int networkId = WifiConfiguration.INVALID_NETWORK_ID;
                    String listStr = WifiNative.listNetworksCommand();
                    if (listStr != null) {
                        String[] lines = listStr.split("\n");
                        // Skip the first line, which is a header
                        for (int i = 1; i < lines.length; i++) {
                            if (lines[i].indexOf("[CURRENT]") != -1) {
                                String[] items = lines[i].split("\t");
                                try {
                                    networkId = Integer.parseInt(items[0]);
                                } catch(NumberFormatException e) {
                                    Xlog.e(TAG, "Exception: " + e.toString());
                                }
                            }
                        }
                    }
                    mReplyChannel.replyToMessage(message, message.what, networkId);
                    break;
                case CMD_UPDATE_RSSI:
                    fetchRssiAndLinkSpeedNative();
                    mReplyChannel.replyToMessage(message, message.what);
                    break;
                case WifiMonitor.WPS_SUCCESS_EVENT:
                    if (ActivityManagerNative.isSystemReady()) {
                        mContext.sendBroadcast(new Intent(WifiManager.WPS_SUCCESS_ACTION));
                    }
                    break;
                case WifiMonitor.WPS_ERROR_EVENT:
                    if (ActivityManagerNative.isSystemReady()) {
                        mContext.sendBroadcast(new Intent(WifiManager.WPS_ERROR_ACTION));
                    }
                    break;
                case WifiMonitor.MTK_WPS_OEVELAP_EVENT:
                    if (ActivityManagerNative.isSystemReady()) {
                        mContext.sendBroadcast(new Intent(WifiManager.WPS_OVERLAP_ACTION));
                    }
                    break;
                case WifiMonitor.MTK_WPS_NO_OEVELAP_EVENT:
                    if (ActivityManagerNative.isSystemReady()) {
                        mContext.sendBroadcast(new Intent(WifiManager.WPS_NO_OVERLAP_ACTION));
                    }
                    break;
                case WifiMonitor.WPS_AP_PIN_EVENT:
                    if (ActivityManagerNative.isSystemReady()) {
                        mContext.sendBroadcast(new Intent(WifiManager.WPS_AP_PIN_ACTION));
                    }
                    break;
                case WifiMonitor.WPS_AP_PBC_EVENT:
                    if (ActivityManagerNative.isSystemReady()) {
                        mContext.sendBroadcast(new Intent(WifiManager.WPS_AP_PBC_ACTION));
                    }
                    break;
                case WifiMonitor.WPS_CRED_RECEIVED_EVENT:
                    if (ActivityManagerNative.isSystemReady()) {
                        mContext.sendBroadcast(new Intent(WifiManager.WPS_CRED_ACTION));
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            if (eventLoggingEnabled) {
                EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            }
            return HANDLED;
        }

        @Override
        public void exit() {
            mNetworkInfo.setIsAvailable(false);
        }
    }

    class SupplicantStoppingState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
            if (DBG) log("stopping supplicant");
            if (!WifiNative.stopSupplicant()) {
                loge("Failed to stop supplicant");
            }

            /* Send ourselves a delayed message to indicate failure after a wait time */
            sendMessageDelayed(obtainMessage(CMD_STOP_SUPPLICANT_FAILED,
                    ++mSupplicantStopFailureToken, 0), SUPPLICANT_RESTART_INTERVAL_MSECS);

            mNetworkInfo.setIsAvailable(false);
            handleNetworkDisconnect();
            setWifiState(WIFI_STATE_DISABLING);
            sendSupplicantConnectionChangedBroadcast(false);
            mSupplicantStateTracker.sendMessage(CMD_RESET_SUPPLICANT_STATE);
            mWpsStateMachine.sendMessage(CMD_RESET_WPS_STATE);
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
                case WifiMonitor.SUP_CONNECTION_EVENT:
                    loge("Supplicant connection received while stopping");
                    break;
                case WifiMonitor.SUP_DISCONNECTION_EVENT:
                    if (DBG) log("Supplicant connection lost");
                    /* Socket connection can be lost when we do a graceful shutdown
                     * or when the driver is hung. Ensure supplicant is stopped here.
                     */
                    WifiNative.killSupplicant();
                    WifiNative.closeSupplicantConnection();
                    transitionTo(mDriverLoadedState);
                    break;
                case CMD_STOP_SUPPLICANT_FAILED:
                    if (message.arg1 == mSupplicantStopFailureToken) {
                        loge("Timed out on a supplicant stop, kill and proceed");
                        WifiNative.killSupplicant();
                        WifiNative.closeSupplicantConnection();
                        transitionTo(mDriverLoadedState);
                    }
                    break;
                case CMD_LOAD_DRIVER:
                case CMD_UNLOAD_DRIVER:
                case CMD_START_SUPPLICANT:
                case CMD_STOP_SUPPLICANT:
                case CMD_START_AP:
                case CMD_STOP_AP:
                case CMD_START_DRIVER:
                case CMD_STOP_DRIVER:
                case CMD_SET_SCAN_MODE:
                case CMD_SET_SCAN_TYPE:
                case CMD_SET_HIGH_PERF_MODE:
                case CMD_SET_COUNTRY_CODE:
                case CMD_SET_FREQUENCY_BAND:
                case CMD_START_PACKET_FILTERING:
                case CMD_STOP_PACKET_FILTERING:
                    deferMessage(message);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class DriverStartingState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
               case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    SupplicantState state = handleSupplicantStateChange(message);
                    /* If suplicant is exiting out of INTERFACE_DISABLED state into
                     * a state that indicates driver has started, it is ready to
                     * receive driver commands
                     */
                    if (SupplicantState.isDriverActive(state)) {
                        transitionTo(mDriverStartedState);
                    }
                    break;
                    /* Queue driver commands & connection events */
                case CMD_START_DRIVER:
                case CMD_STOP_DRIVER:
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                case WifiMonitor.WPS_OVERLAP_EVENT:
                case CMD_SET_SCAN_TYPE:
                case CMD_SET_HIGH_PERF_MODE:
                case CMD_SET_COUNTRY_CODE:
                case CMD_SET_FREQUENCY_BAND:
                case CMD_START_PACKET_FILTERING:
                case CMD_STOP_PACKET_FILTERING:
                case CMD_START_SCAN:
                case CMD_DISCONNECT:
                case CMD_REASSOCIATE:
                case CMD_RECONNECT:
                    deferMessage(message);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class DriverStartedState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());

            mIsRunning = true;
            mInDelayedStop = false;
            updateBatteryWorkSource(null);

            /**
             * Enable bluetooth coexistence scan mode when bluetooth connection is active.
             * When this mode is on, some of the low-level scan parameters used by the
             * driver are changed to reduce interference with bluetooth
             */
            WifiNative.setBluetoothCoexistenceScanModeCommand(mBluetoothConnectionActive);
            /* set country code */
            setCountryCode();
            /* set frequency band of operation */
            setFrequencyBand();
            /* initialize network state */
            setNetworkDetailedState(DetailedState.DISCONNECTED);

            /* Remove any filtering on Multicast v6 at start */
            WifiNative.stopFilteringMulticastV6Packets();

            /* Reset Multicast v4 filtering state */
            if (mFilteringMulticastV4Packets.get()) {
                WifiNative.startFilteringMulticastV4Packets();
            } else {
                WifiNative.stopFilteringMulticastV4Packets();
            }

            if (mIsScanMode) {
                WifiNative.setScanResultHandlingCommand(SCAN_ONLY_MODE);
                WifiNative.disconnectCommand();
                transitionTo(mScanModeState);
            } else {
                WifiNative.setScanResultHandlingCommand(CONNECT_MODE);
                WifiNative.reconnectCommand();
                transitionTo(mDisconnectedState);
            }
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            boolean eventLoggingEnabled = true;
            switch(message.what) {
                case CMD_SET_SCAN_TYPE:
                    if (message.arg1 == SCAN_ACTIVE) {
                        WifiNative.setScanModeCommand(true);
                    } else {
                        WifiNative.setScanModeCommand(false);
                    }
                    break;
                case CMD_START_SCAN:
                    eventLoggingEnabled = false;
                    WifiNative.scanCommand(message.arg1 == SCAN_ACTIVE);
                    mScanResultIsPending = true;
                    break;
                case CMD_SET_HIGH_PERF_MODE:
                    setHighPerfModeEnabledNative(message.arg1 == 1);
                    break;
                case CMD_SET_COUNTRY_CODE:
                    String country = (String) message.obj;
                    if (DBG) log("set country code " + country);
                    if (!WifiNative.setCountryCodeCommand(country.toUpperCase())) {
                        loge("Failed to set country code " + country);
                    }
                    break;
                case CMD_SET_FREQUENCY_BAND:
                    int band =  message.arg1;
                    if (DBG) log("set frequency band " + band);
                    if (WifiNative.setBandCommand(band)) {
                        mFrequencyBand.set(band);
                        //Fetch the latest scan results when frequency band is set
                        startScan(true);
                    } else {
                        loge("Failed to set frequency band " + band);
                    }
                    break;
                case CMD_BLUETOOTH_ADAPTER_STATE_CHANGE:
                    mBluetoothConnectionActive = (message.arg1 !=
                            BluetoothAdapter.STATE_DISCONNECTED);
                    WifiNative.setBluetoothCoexistenceScanModeCommand(mBluetoothConnectionActive);
                    break;
                case CMD_STOP_DRIVER:
                    int mode = message.arg1;

                    /* Already doing a delayed stop && not in ecm state */
                    if (mInDelayedStop && mode != IN_ECM_STATE) {
                        if (DBG) log("Already in delayed stop");
                        break;
                    }
                    mInDelayedStop = true;
                    mDelayedStopCounter++;
                    if (DBG) log("Delayed stop message " + mDelayedStopCounter);

                    if (mode == IN_ECM_STATE) {
                        /* send a shut down immediately */
                        sendMessage(obtainMessage(CMD_DELAYED_STOP_DRIVER, mDelayedStopCounter, 0));
                    } else {
                        /* send regular delayed shut down */
                        mAlarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + DELAYED_DRIVER_STOP_MS, mIntentStopDriver);      
                    }
                    break;
                case CMD_START_DRIVER:
                    if (mInDelayedStop) {
                        mAlarmManager.cancel(mIntentStopDriver);
                        mInDelayedStop = false;
                        mDelayedStopCounter++;
                        if (DBG) log("Delayed stop ignored due to start");
                    }
                    break;
                case CMD_DELAYED_STOP_DRIVER:
                    if (message.arg1 != mDelayedStopCounter) break;
                    if (getCurrentState() != mDisconnectedState) {
                        WifiNative.disconnectCommand();
                        handleNetworkDisconnect();
                    }
                    mWakeLock.acquire();
                    WifiNative.stopDriverCommand();
                    transitionTo(mDriverStoppingState);
                    mWakeLock.release();
                    break;
                case CMD_START_PACKET_FILTERING:
                    if (message.arg1 == MULTICAST_V6) {
                        WifiNative.startFilteringMulticastV6Packets();
                    } else if (message.arg1 == MULTICAST_V4) {
                        WifiNative.startFilteringMulticastV4Packets();
                    } else {
                        loge("Illegal arugments to CMD_START_PACKET_FILTERING");
                    }
                    break;
                case CMD_STOP_PACKET_FILTERING:
                    if (message.arg1 == MULTICAST_V6) {
                        WifiNative.stopFilteringMulticastV6Packets();
                    } else if (message.arg1 == MULTICAST_V4) {
                        WifiNative.stopFilteringMulticastV4Packets();
                    } else {
                        loge("Illegal arugments to CMD_STOP_PACKET_FILTERING");
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            if (eventLoggingEnabled) {
                EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            }
            return HANDLED;
        }
        @Override
        public void exit() {
            if (DBG) log(getName() + "\n");
            mIsRunning = false;
            updateBatteryWorkSource(null);
            mScanResults = null;
        }
    }

    class DriverStoppingState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    SupplicantState state = handleSupplicantStateChange(message);
                    if (state == SupplicantState.INTERFACE_DISABLED) {
                        transitionTo(mDriverStoppedState);
                    }
                    break;
                    /* Queue driver commands */
                case CMD_START_DRIVER:
                case CMD_STOP_DRIVER:
                case CMD_SET_SCAN_TYPE:
                case CMD_SET_HIGH_PERF_MODE:
                case CMD_SET_COUNTRY_CODE:
                case CMD_SET_FREQUENCY_BAND:
                case CMD_START_PACKET_FILTERING:
                case CMD_STOP_PACKET_FILTERING:
                case CMD_START_SCAN:
                case CMD_DISCONNECT:
                case CMD_REASSOCIATE:
                case CMD_RECONNECT:
                    deferMessage(message);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class DriverStoppedState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch (message.what) {
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    SupplicantState state = stateChangeResult.state;
                    // A WEXT bug means that we can be back to driver started state
                    // unexpectedly
                    if (SupplicantState.isDriverActive(state)) {
                        transitionTo(mDriverStartedState);
                    }
                    break;
                case CMD_START_DRIVER:
                    mWakeLock.acquire();
                    WifiNative.startDriverCommand();
                    mWakeLock.release();
                    transitionTo(mDriverStartingState);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class ScanModeState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
                case CMD_SET_SCAN_MODE:
                    if (message.arg1 == SCAN_ONLY_MODE) {
                        /* Ignore */
                        return HANDLED;
                    } else {
                        WifiNative.setScanResultHandlingCommand(message.arg1);
                        WifiNative.reconnectCommand();
                        mIsScanMode = false;
                        transitionTo(mDisconnectedState);
                    }
                    break;
                    /* Ignore */
                case CMD_DISCONNECT:
                case CMD_RECONNECT:
                case CMD_REASSOCIATE:
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class ConnectModeState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            StateChangeResult stateChangeResult;
            switch(message.what) {
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                    mSupplicantStateTracker.sendMessage(WifiMonitor.AUTHENTICATION_FAILURE_EVENT);
                    break;
                case WifiMonitor.WPS_OVERLAP_EVENT:
                    /* We just need to broadcast the error */
                    sendErrorBroadcast(WifiManager.WPS_OVERLAP_ERROR);
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    SupplicantState state = handleSupplicantStateChange(message);
                    // Due to a WEXT bug, during the time of driver start/stop
                    // we can go into a driver stopped state in an unexpected way.
                    // The sequence eventually puts interface
                    // up and we should be back to a connected state
                    if (!SupplicantState.isDriverActive(state)) {
                        if (mNetworkInfo.getState() != NetworkInfo.State.DISCONNECTED) {
                            handleNetworkDisconnect();
                        }
                        transitionTo(mDriverStoppedState);
                        break;
                    }

                    // Supplicant can fail to report a NETWORK_DISCONNECTION_EVENT
                    // when authentication times out after a successful connection,
                    // we can figure this from the supplicant state. If supplicant
                    // state is DISCONNECTED, but the mNetworkInfo says we are not
                    // disconnected, we need to handle a disconnection
                    if (state == SupplicantState.DISCONNECTED &&
                            mNetworkInfo.getState() != NetworkInfo.State.DISCONNECTED) {
                        if (DBG) log("Missed CTRL-EVENT-DISCONNECTED, disconnect");
                        handleNetworkDisconnect();
                        transitionTo(mDisconnectedState);
                    }
                    break;
                    /* Do a redundant disconnect without transition */
                case CMD_DISCONNECT:
                    WifiNative.disconnectCommand();
                    break;
                case CMD_RECONNECT:
                    WifiNative.reconnectCommand();
                    break;
                case CMD_REASSOCIATE:
                    WifiNative.reassociateCommand();
                    break;
                case CMD_CONNECT_NETWORK:
                    int netId = message.arg1;
                    WifiConfiguration config = (WifiConfiguration) message.obj;

                    /* We connect to a specific network by issuing a select
                     * to the WifiConfigStore. This enables the network,
                     * while disabling all other networks in the supplicant.
                     * Disabling a connected network will cause a disconnection
                     * from the network. A reconnectCommand() will then initiate
                     * a connection to the enabled network.
                     */
                    if (config != null) {
                        netId = WifiConfigStore.selectNetwork(config);
                    } else {
                        WifiConfigStore.selectNetwork(netId);
                    }

                    /* The state tracker handles enabling networks upon completion/failure */
                    mSupplicantStateTracker.sendMessage(CMD_CONNECT_NETWORK, netId);

                    WifiNative.reconnectCommand();
                    mLastExplicitNetworkId = netId;
                    mLastNetworkChoiceTime  = SystemClock.elapsedRealtime();
                    mNextWifiActionExplicit = true;
                    if (DBG) log("Setting wifi connect explicit for netid " + netId);
                    /* Expect a disconnection from the old connection */
                    transitionTo(mDisconnectingState);
                    break;
                case CMD_START_WPS:
                    mWpsStateMachine.sendMessage(Message.obtain(message));
                    transitionTo(mWaitForWpsCompletionState);
                    break;
                case WifiMonitor.SCAN_RESULTS_EVENT:
                    /* Set the scan setting back to "connect" mode */
                    WifiNative.setScanResultHandlingCommand(CONNECT_MODE);
                    /* Handle scan results */
                    return NOT_HANDLED;
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    if (DBG) log("Network connection established");
                    mLastNetworkId = message.arg1;
                    mLastBssid = (String) message.obj;

                    //TODO: make supplicant modification to push this in events
                    mWifiInfo.setSSID(fetchSSID());
                    mWifiInfo.setBSSID(mLastBssid);
                    mWifiInfo.setNetworkId(mLastNetworkId);
                    if (mNextWifiActionExplicit &&
                        mWifiInfo.getNetworkId() == mLastExplicitNetworkId &&
                        SystemClock.elapsedRealtime() < mLastNetworkChoiceTime +
                                                            EXPLICIT_CONNECT_ALLOWED_DELAY_MS) {
                        mWifiInfo.setExplicitConnect(true);
                    }
                    mNextWifiActionExplicit = false;
                    /* send event to CM & network change broadcast */
                    setNetworkDetailedState(DetailedState.OBTAINING_IPADDR);
                    sendNetworkStateChangeBroadcast(mLastBssid);
                    transitionTo(mConnectingState);
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    if (DBG) log("Network connection lost");
                    handleNetworkDisconnect();
                    transitionTo(mDisconnectedState);
                    break;
                case WifiMonitor.WAPI_NO_CERTIFICATION_EVENT:
                    if (DBG) Xlog.d(TAG, "WAPI no certification");
                    mContext.sendBroadcast(new Intent(WifiManager.NO_CERTIFICATION_ACTION));
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class ConnectingState extends State {

        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());

            try {
                mNwService.enableIpv6(mInterfaceName);
            } catch (RemoteException re) {
                loge("Failed to enable IPv6: " + re);
            } catch (IllegalStateException e) {
                loge("Failed to enable IPv6: " + e);
            }

            if (!WifiConfigStore.isUsingStaticIp(mLastNetworkId)) {
                Xlog.d(TAG, "Supplicant state is " + mWifiInfo.getSupplicantState() + " when before start DHCP");
                if (mWifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                    //start DHCP
                    mDhcpStateMachine = DhcpStateMachine.makeDhcpStateMachine(
                            mContext, WifiStateMachine.this, mInterfaceName);
                    mDhcpStateMachine.registerForPreDhcpNotification();
                    mDhcpStateMachine.sendMessage(DhcpStateMachine.CMD_START_DHCP);
                }
            } else {
                if (mDhcpStateMachine != null) {
                    mDhcpStateMachine.sendMessage(DhcpStateMachine.CMD_STOP_DHCP);
                    mDhcpStateMachine.quit();
                    mDhcpStateMachine = null;
                }
                DhcpInfoInternal dhcpInfoInternal = WifiConfigStore.getIpConfiguration(
                        mLastNetworkId);
                Collection<RouteInfo> routeInfos = dhcpInfoInternal.getRoutes();
                InterfaceConfiguration ifcg = new InterfaceConfiguration();
                ifcg.addr = dhcpInfoInternal.makeLinkAddress();
                ifcg.interfaceFlags = "[up]";
                try {
                    mNwService.setInterfaceConfig(mInterfaceName, ifcg);
                    for (RouteInfo routeInfo : routeInfos) {
                        mNwService.addRoute(mInterfaceName, routeInfo);
                    }
                    if (DBG) log("Static IP configuration succeeded");
                    sendMessage(CMD_STATIC_IP_SUCCESS, dhcpInfoInternal);
                } catch (RemoteException re) {
                    loge("Static IP configuration failed: " + re);
                    sendMessage(CMD_STATIC_IP_FAILURE);
                } catch (IllegalStateException e) {
                    loge("Static IP configuration failed: " + e);
                    sendMessage(CMD_STATIC_IP_FAILURE);
                }
            }
        }
      @Override
      public boolean processMessage(Message message) {
          if (DBG) log(getName() + message.toString() + "\n");

          switch(message.what) {
              case DhcpStateMachine.CMD_PRE_DHCP_ACTION:
                  handlePreDhcpSetup();
                  mDhcpStateMachine.sendMessage(DhcpStateMachine.CMD_PRE_DHCP_ACTION_COMPLETE);
                  break;
              case DhcpStateMachine.CMD_POST_DHCP_ACTION:
                  handlePostDhcpSetup();
                  if (message.arg1 == DhcpStateMachine.DHCP_SUCCESS) {
                      handleSuccessfulIpConfiguration((DhcpInfoInternal) message.obj);
                      transitionTo(mConnectedState);
                  } else if (message.arg1 == DhcpStateMachine.DHCP_FAILURE) {
                      handleFailedIpConfiguration();
                      transitionTo(mDisconnectingState);
                  }
                  break;
              case CMD_STATIC_IP_SUCCESS:
                  handleSuccessfulIpConfiguration((DhcpInfoInternal) message.obj);
                  transitionTo(mConnectedState);
                  break;
              case CMD_STATIC_IP_FAILURE:
                  handleFailedIpConfiguration();
                  transitionTo(mDisconnectingState);
                  break;
              case CMD_DISCONNECT:
                  WifiNative.disconnectCommand();
                  transitionTo(mDisconnectingState);
                  break;
                  /* Ignore connection to same network */
              case CMD_CONNECT_NETWORK:
                  int netId = message.arg1;
                  if (mWifiInfo.getNetworkId() == netId) {
                      break;
                  }
                  return NOT_HANDLED;
              case CMD_SAVE_NETWORK:
                  deferMessage(message);
                  break;
                  /* Ignore */
              case WifiMonitor.NETWORK_CONNECTION_EVENT:
                  break;
              case CMD_SET_SCAN_MODE:
                  if (message.arg1 == SCAN_ONLY_MODE) {
                      sendMessage(CMD_DISCONNECT);
                      deferMessage(message);
                  }
                  break;
                  /* Defer scan when IP is being fetched */
              case CMD_START_SCAN:
                  deferMessage(message);
                  break;
                  /* Defer any power mode changes since we must keep active power mode at DHCP */
              case CMD_SET_HIGH_PERF_MODE:
                  deferMessage(message);
                  break;
              default:
                return NOT_HANDLED;
          }
          EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
          return HANDLED;
      }
      
        @Override
        public void exit() {
            handlePostDhcpSetup();
        }
    }

    class ConnectedState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
            mRssiPollToken++;
            if (mEnableRssiPolling) {
                sendMessage(obtainMessage(WifiStateMachine.CMD_RSSI_POLL, mRssiPollToken, 0));
            }
            //MTK_OP01_PROTECT_START
            disableAllNetworks(true);
            //MTK_OP01_PROTECT_END
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            boolean eventLoggingEnabled = true;
            switch (message.what) {
              case DhcpStateMachine.CMD_PRE_DHCP_ACTION:
                  handlePreDhcpSetup();
                  mDhcpStateMachine.sendMessage(DhcpStateMachine.CMD_PRE_DHCP_ACTION_COMPLETE);
                  break;
              case DhcpStateMachine.CMD_POST_DHCP_ACTION:
                  handlePostDhcpSetup();
                  if (message.arg1 == DhcpStateMachine.DHCP_SUCCESS) {
                      handleSuccessfulIpConfiguration((DhcpInfoInternal) message.obj);
                  } else if (message.arg1 == DhcpStateMachine.DHCP_FAILURE) {
                      handleFailedIpConfiguration();
                      transitionTo(mDisconnectingState);
                  }
                  break;
                case CMD_DISCONNECT:
                    WifiNative.disconnectCommand();
                    transitionTo(mDisconnectingState);
                    break;
                case CMD_SET_SCAN_MODE:
                    if (message.arg1 == SCAN_ONLY_MODE) {
                        sendMessage(CMD_DISCONNECT);
                        deferMessage(message);
                    }
                    break;
                case CMD_START_SCAN:
                    eventLoggingEnabled = false;
                    /* When the network is connected, re-scanning can trigger
                     * a reconnection. Put it in scan-only mode during scan.
                     * When scan results are received, the mode is switched
                     * back to CONNECT_MODE.
                     */
                    WifiNative.setScanResultHandlingCommand(SCAN_ONLY_MODE);
                    /* Have the parent state handle the rest */
                    return NOT_HANDLED;
                    /* Ignore connection to same network */
                case CMD_CONNECT_NETWORK:
                    int netId = message.arg1;
                    if (mWifiInfo.getNetworkId() == netId) {
                        break;
                    }
                    return NOT_HANDLED;
                case CMD_SAVE_NETWORK:
                    WifiConfiguration config = (WifiConfiguration) message.obj;
                    NetworkUpdateResult result = WifiConfigStore.saveNetwork(config);
                    if (mWifiInfo.getNetworkId() == result.getNetworkId()) {
                        if (result.hasIpChanged()) {
                            log("Reconfiguring IP on connection");
                            transitionTo(mConnectingState);
                        }
                        if (result.hasProxyChanged()) {
                            log("Reconfiguring proxy on connection");
                            configureLinkProperties();
                            sendLinkConfigurationChangedBroadcast();
                        }
                    }
                    break;
                    /* Ignore */
                case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    break;
                case CMD_RSSI_POLL:
                    eventLoggingEnabled = false;
                    if (message.arg1 == mRssiPollToken) {
                        // Get Info and continue polling
                        fetchRssiAndLinkSpeedNative();
                        
                        //Add for WiFi to 3G begin
                        //MTK_OP01_PROTECT_START
                        if (!OPTR.equals("OP01")) {
                        //MTK_OP01_PROTECT_END
                            if (mWifiInfo.getRssi() <= RSSI_THRESHOLD) {
                                mRssiWeakCounter++;
                                Xlog.d(TAG, "mRssiWeakCounter:" + mRssiWeakCounter);
                                if (mRssiWeakCounter >= RSSI_WEAK_NUM) {
                                    Xlog.d(TAG, "mIsSimAbsent=" + mIsSimAbsent + ", mIsGPRSDisabled=" + mIsGPRSDisabled);
                                    if (!(mIsSimAbsent || mIsGPRSDisabled)) {
                                        Xlog.i(TAG, "Rssi <= " + RSSI_THRESHOLD + " for three times, disabling network " + mLastNetworkId);
                                        List<WifiConfiguration> networks = syncGetConfiguredNetworks();
                                        if (networks != null) {
                                            synchronized (mRssiWeakNetworks) {
                                                for (WifiConfiguration network : networks) {
                                                    if (network.networkId == mLastNetworkId) {
                                                        mRssiWeakNetworks.add(network);
                                                    }
                                                }
                                            }
                                        }
                                        WifiConfigStore.disableNetwork(mLastNetworkId, WifiConfiguration.DISABLED_UNKNOWN_REASON);
                                    }
                                    mRssiWeakCounter = 0;
                                }
                            } else {
                                if (mRssiWeakCounter > 0) {
                                    Xlog.i(TAG, "Rssi > " + RSSI_THRESHOLD + ", reset counter");
                                    mRssiWeakCounter = 0;
                                }
                            }
                        //MTK_OP01_PROTECT_START
                        }
                        //MTK_OP01_PROTECT_END
                        //Add for WiFi to 3G end

                        sendMessageDelayed(obtainMessage(WifiStateMachine.CMD_RSSI_POLL,
                                mRssiPollToken, 0), POLL_RSSI_INTERVAL_MSECS);
                    } else {
                        // Polling has completed
                    }
                    break;
                case CMD_ENABLE_RSSI_POLL:
                    mEnableRssiPolling = (message.arg1 == 1);
                    mRssiPollToken++;
                    if (mEnableRssiPolling) {
                        // first poll
                        fetchRssiAndLinkSpeedNative();
                        sendMessageDelayed(obtainMessage(WifiStateMachine.CMD_RSSI_POLL,
                                mRssiPollToken, 0), POLL_RSSI_INTERVAL_MSECS);
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            if (eventLoggingEnabled) {
                EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            }
            return HANDLED;
        }
        @Override
        public void exit() {

            /* Request a CS wakelock during transition to mobile */
            checkAndSetConnectivityInstance();
            mCm.requestNetworkTransitionWakelock(TAG);

            /* If a scan result is pending in connected state, the supplicant
             * is in SCAN_ONLY_MODE. Restore CONNECT_MODE on exit
             */
            if (mScanResultIsPending) {
                WifiNative.setScanResultHandlingCommand(CONNECT_MODE);
            }
        }
    }

    class DisconnectingState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_SET_SCAN_MODE:
                    if (message.arg1 == SCAN_ONLY_MODE) {
                        deferMessage(message);
                    }
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    /* If we get a SUPPLICANT_STATE_CHANGE_EVENT before NETWORK_DISCONNECTION_EVENT
                     * we have missed the network disconnection, transition to mDisconnectedState
                     * and handle the rest of the events there
                     */
                    deferMessage(message);
                    handleNetworkDisconnect();
                    transitionTo(mDisconnectedState);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class DisconnectedState extends State {
        private boolean mAlarmEnabled = false;
        /* This is set from the overlay config file or from a secure setting.
         * A value of 0 disables scanning in the framework.
         */
        private long mFrameworkScanIntervalMs;

        private void setScanAlarm(boolean enabled) {
            if (enabled == mAlarmEnabled) return;
            if (enabled) {
                if (mFrameworkScanIntervalMs > 0) {
                    mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis() + mFrameworkScanIntervalMs,
                            mFrameworkScanIntervalMs,
                            mScanIntent);
                    mAlarmEnabled = true;
                }
            } else {
                mAlarmManager.cancel(mScanIntent);
                mAlarmEnabled = false;
            }
        }

        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());

            //MTK_OP01_PROTECT_START
            if (OPTR.equals("OP01")) {
                mFrameworkScanIntervalMs = Settings.Secure.getLong(mContext.getContentResolver(),
                    Settings.Secure.WIFI_FRAMEWORK_SCAN_INTERVAL_MS,
                    mScreenOn ? 15000 : mDefaultFrameworkScanIntervalMs);
            } else
            //MTK_OP01_PROTECT_END
            {
                mFrameworkScanIntervalMs = Settings.Secure.getLong(mContext.getContentResolver(),
                    Settings.Secure.WIFI_FRAMEWORK_SCAN_INTERVAL_MS,
                    mDefaultFrameworkScanIntervalMs);
            }
            /*
             * We initiate background scanning if it is enabled, otherwise we
             * initiate an infrequent scan that wakes up the device to ensure
             * a user connects to an access point on the move
             */
            if (mEnableBackgroundScan) {
                /* If a regular scan result is pending, do not initiate background
                 * scan until the scan results are returned. This is needed because
                 * initiating a background scan will cancel the regular scan and
                 * scan results will not be returned until background scanning is
                 * cleared
                 */
                if (!mScanResultIsPending) {
                    WifiNative.enableBackgroundScanCommand(true);
                }
            } else {
                setScanAlarm(true);
            }
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch (message.what) {
                case CMD_SET_SCAN_MODE:
                    if (message.arg1 == SCAN_ONLY_MODE) {
                        WifiNative.setScanResultHandlingCommand(message.arg1);
                        //Supplicant disconnect to prevent further connects
                        WifiNative.disconnectCommand();
                        mIsScanMode = true;
                        transitionTo(mScanModeState);
                    }
                    break;
                case CMD_ENABLE_BACKGROUND_SCAN:
                    mEnableBackgroundScan = (message.arg1 == 1);
                    if (mEnableBackgroundScan) {
                        WifiNative.enableBackgroundScanCommand(true);
                        setScanAlarm(false);
                    } else {
                        WifiNative.enableBackgroundScanCommand(false);
                        setScanAlarm(true);
                    }
                    break;
                    /* Ignore network disconnect */
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    setNetworkDetailedState(WifiInfo.getDetailedStateOf(stateChangeResult.state));
                    /* ConnectModeState does the rest of the handling */
                    return NOT_HANDLED;
                case CMD_START_SCAN:
                    /* Disable background scan temporarily during a regular scan */
                    if (mEnableBackgroundScan) {
                        WifiNative.enableBackgroundScanCommand(false);
                    }
                    /* Handled in parent state */
                    return NOT_HANDLED;
                case WifiMonitor.SCAN_RESULTS_EVENT:
                    /* Re-enable background scan when a pending scan result is received */
                    if (mEnableBackgroundScan && mScanResultIsPending) {
                        WifiNative.enableBackgroundScanCommand(true);
                    }
                    /* Handled in parent state */
                    return NOT_HANDLED;
                //MTK_OP01_PROTECT_START
                case CMD_UPDATE_SCAN_INTERVAL:
                    mScreenOn = (message.arg1 == 1);
                    mFrameworkScanIntervalMs = Settings.Secure.getLong(mContext.getContentResolver(),
                        Settings.Secure.WIFI_FRAMEWORK_SCAN_INTERVAL_MS,
                        mScreenOn ? 15000 : mDefaultFrameworkScanIntervalMs);
                    if (!mEnableBackgroundScan) {
                        setScanAlarm(false);
                        setScanAlarm(true);
                    }
                    return NOT_HANDLED;
                //MTK_OP01_PROTECT_END
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }

        @Override
        public void exit() {
            /* No need for a background scan upon exit from a disconnected state */
            if (mEnableBackgroundScan) {
                WifiNative.enableBackgroundScanCommand(false);
            }
            setScanAlarm(false);
        }
    }

    class WaitForWpsCompletionState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch (message.what) {
                /* Defer all commands that can cause connections to a different network
                 * or put the state machine out of connect mode
                 */
                case CMD_STOP_DRIVER:
                case CMD_SET_SCAN_MODE:
                case CMD_CONNECT_NETWORK:
                case CMD_ENABLE_NETWORK:
                case CMD_RECONNECT:
                case CMD_REASSOCIATE:
                case WifiMonitor.NETWORK_CONNECTION_EVENT: /* Handled after IP & proxy update */
                    deferMessage(message);
                    break;
                case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    if (DBG) log("Network connection lost");
                    handleNetworkDisconnect();
                    break;
                case WPS_COMPLETED_EVENT:
                    /* we are still disconnected until we see a network connection
                     * notification */
                    transitionTo(mDisconnectedState);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class SoftApStartingState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());

            final Message message = getCurrentMessage();
            if (message.what == CMD_START_AP) {
                final WifiConfiguration config = (WifiConfiguration) message.obj;

                if (config == null) {
                    mWifiApConfigChannel.sendMessage(CMD_REQUEST_AP_CONFIG);
                } else {
                    mWifiApConfigChannel.sendMessage(CMD_SET_AP_CONFIG, config);
                    startSoftApWithConfig(config);
                }
            } else {
                throw new RuntimeException("Illegal transition to SoftApStartingState: " + message);
            }
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
                case CMD_LOAD_DRIVER:
                case CMD_UNLOAD_DRIVER:
                case CMD_START_SUPPLICANT:
                case CMD_STOP_SUPPLICANT:
                case CMD_START_AP:
                case CMD_STOP_AP:
                case CMD_START_DRIVER:
                case CMD_STOP_DRIVER:
                case CMD_SET_SCAN_MODE:
                case CMD_SET_SCAN_TYPE:
                case CMD_SET_HIGH_PERF_MODE:
                case CMD_SET_COUNTRY_CODE:
                case CMD_SET_FREQUENCY_BAND:
                case CMD_START_PACKET_FILTERING:
                case CMD_STOP_PACKET_FILTERING:
                case CMD_TETHER_STATE_CHANGE:
                case WifiP2pService.P2P_ENABLE_PENDING:
                    deferMessage(message);
                    break;
                case WifiStateMachine.CMD_RESPONSE_AP_CONFIG:
                    WifiConfiguration config = (WifiConfiguration) message.obj;
                    if (config != null) {
                        startSoftApWithConfig(config);
                    } else {
                        loge("Softap config is null!");
                        sendMessage(CMD_START_AP_FAILURE);
                    }
                    break;
                case CMD_START_AP_SUCCESS:
                    setWifiApState(WIFI_AP_STATE_ENABLED);
                    mWifiMonitor.startMonitoring(true);
                    transitionTo(mSoftApStartedState);
                    break;
                case CMD_START_AP_FAILURE:
                    // initiate driver unload
                    sendMessage(obtainMessage(CMD_UNLOAD_DRIVER, WIFI_AP_STATE_FAILED, 0));
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class SoftApStartedState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
                case CMD_STOP_AP:
                    if (DBG) log("Stopping Soft AP");
                    setWifiApState(WIFI_AP_STATE_DISABLING);

                    /* We have not tethered at this point, so we just shutdown soft Ap */
                    try {
                        mNwService.stopAccessPoint(mInterfaceName);
                    } catch(Exception e) {
                        loge("Exception in stopAccessPoint()");
                    }
                    transitionTo(mDriverLoadedState);
                    break;
                case CMD_START_AP:
                    // Ignore a start on a running access point
                    break;
                    /* Fail client mode operation when soft AP is enabled */
                case CMD_START_SUPPLICANT:
                    loge("Cannot start supplicant with a running soft AP");
                    setWifiState(WIFI_STATE_UNKNOWN);
                    break;
                case CMD_TETHER_STATE_CHANGE:
                    TetherStateChange stateChange = (TetherStateChange) message.obj;
                    if (startTethering(stateChange.available)) {
                        transitionTo(mTetheringState);
                    }
                    break;
                case WifiP2pService.P2P_ENABLE_PENDING:
                    // turn of soft Ap and defer to be handled in DriverUnloadedState
                    setWifiApEnabled(null, false);
                    deferMessage(message);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class WaitForP2pDisableState extends State {
        private int mSavedArg;
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());

            //Preserve the argument arg1 that has information used in DriverLoadingState
            mSavedArg = getCurrentMessage().arg1;
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
                case WifiP2pService.WIFI_ENABLE_PROCEED:
                    //restore argument from original message (CMD_LOAD_DRIVER)
                    message.arg1 = mSavedArg;
                    transitionTo(mDriverLoadingState);
                    break;
                case CMD_LOAD_DRIVER:
                case CMD_UNLOAD_DRIVER:
                case CMD_START_SUPPLICANT:
                case CMD_STOP_SUPPLICANT:
                case CMD_START_AP:
                case CMD_STOP_AP:
                case CMD_START_DRIVER:
                case CMD_STOP_DRIVER:
                case CMD_SET_SCAN_MODE:
                case CMD_SET_SCAN_TYPE:
                case CMD_SET_HIGH_PERF_MODE:
                case CMD_SET_COUNTRY_CODE:
                case CMD_SET_FREQUENCY_BAND:
                case CMD_START_PACKET_FILTERING:
                case CMD_STOP_PACKET_FILTERING:
                    deferMessage(message);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class TetheringState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());

            /* Send ourselves a delayed message to shut down if tethering fails to notify */
            sendMessageDelayed(obtainMessage(CMD_TETHER_NOTIFICATION_TIMED_OUT,
                    ++mTetherToken, 0), TETHER_NOTIFICATION_TIME_OUT_MSECS);
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
                case CMD_TETHER_STATE_CHANGE:
                    TetherStateChange stateChange = (TetherStateChange) message.obj;
                    if (isWifiTethered(stateChange.active)) {
                        transitionTo(mTetheredState);
                    }
                    return HANDLED;
                case CMD_TETHER_NOTIFICATION_TIMED_OUT:
                    if (message.arg1 == mTetherToken) {
                        loge("Failed to get tether update, shutdown soft access point");
                        setWifiApEnabled(null, false);
                    }
                    break;
                case CMD_LOAD_DRIVER:
                case CMD_UNLOAD_DRIVER:
                case CMD_START_SUPPLICANT:
                case CMD_STOP_SUPPLICANT:
                case CMD_START_AP:
                case CMD_STOP_AP:
                case CMD_START_DRIVER:
                case CMD_STOP_DRIVER:
                case CMD_SET_SCAN_MODE:
                case CMD_SET_SCAN_TYPE:
                case CMD_SET_HIGH_PERF_MODE:
                case CMD_SET_COUNTRY_CODE:
                case CMD_SET_FREQUENCY_BAND:
                case CMD_START_PACKET_FILTERING:
                case CMD_STOP_PACKET_FILTERING:
                case WifiP2pService.P2P_ENABLE_PENDING:
                    deferMessage(message);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    class TetheredState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            mClientNum = 0;
            if (mDuration != Settings.System.WIFI_HOTSPOT_AUTO_DISABLE_OFF) {
                mAlarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + mDuration * HOTSPOT_DISABLE_MS, mIntentStopHotspot);
            }
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
                case CMD_TETHER_STATE_CHANGE:
                    TetherStateChange stateChange = (TetherStateChange) message.obj;
                    if (!isWifiTethered(stateChange.active)) {
                        loge("Tethering reports wifi as untethered!, shut down soft Ap");
                        setWifiApEnabled(null, false);
                    }
                    return HANDLED;
                case CMD_STOP_AP:
                    if (DBG) log("Untethering before stopping AP");
                    setWifiApState(WIFI_AP_STATE_DISABLING);
                    stopTethering();
                    transitionTo(mSoftApStoppingState);
                    break;
                case WifiMonitor.AP_STA_CONNECTED_EVENT:
                    Xlog.d(TAG, "AP STA CONNECTED: " + message.obj);
                    ++mClientNum;
                    if (mDuration != Settings.System.WIFI_HOTSPOT_AUTO_DISABLE_OFF && mClientNum == 1) {
                        mAlarmManager.cancel(mIntentStopHotspot);
                    }
                    break;
                case WifiMonitor.AP_STA_DISCONNECTED_EVENT:
                    Xlog.d(TAG, "AP STA DISCONNECTED: " + message.obj);
                    --mClientNum;
                    if (mDuration != Settings.System.WIFI_HOTSPOT_AUTO_DISABLE_OFF && mClientNum == 0) {
                        mAlarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + mDuration * HOTSPOT_DISABLE_MS, mIntentStopHotspot);
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }

        @Override
        public void exit() {
            if (mDuration != Settings.System.WIFI_HOTSPOT_AUTO_DISABLE_OFF) {
                mAlarmManager.cancel(mIntentStopHotspot);
            }
        }
    }

    class SoftApStoppingState extends State {
        @Override
        public void enter() {
            if (DBG) log(getName() + "\n");
            EventLog.writeEvent(EVENTLOG_WIFI_STATE_CHANGED, getName());

            /* Send ourselves a delayed message to shut down if tethering fails to notify */
            sendMessageDelayed(obtainMessage(CMD_TETHER_NOTIFICATION_TIMED_OUT,
                    ++mTetherToken, 0), TETHER_NOTIFICATION_TIME_OUT_MSECS);

        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) log(getName() + message.toString() + "\n");
            switch(message.what) {
                case CMD_TETHER_STATE_CHANGE:
                    TetherStateChange stateChange = (TetherStateChange) message.obj;

                    /* Wait till wifi is untethered */
                    if (isWifiTethered(stateChange.active)) break;

                    try {
                        mNwService.stopAccessPoint(mInterfaceName);
                    } catch(Exception e) {
                        loge("Exception in stopAccessPoint()");
                    }
                    transitionTo(mDriverLoadedState);
                    break;
                case CMD_TETHER_NOTIFICATION_TIMED_OUT:
                    if (message.arg1 == mTetherToken) {
                        loge("Failed to get tether update, force stop access point");
                        try {
                            mNwService.stopAccessPoint(mInterfaceName);
                        } catch(Exception e) {
                            loge("Exception in stopAccessPoint()");
                        }
                        transitionTo(mDriverLoadedState);
                    }
                    break;
                case CMD_LOAD_DRIVER:
                case CMD_UNLOAD_DRIVER:
                case CMD_START_SUPPLICANT:
                case CMD_STOP_SUPPLICANT:
                case CMD_START_AP:
                case CMD_STOP_AP:
                case CMD_START_DRIVER:
                case CMD_STOP_DRIVER:
                case CMD_SET_SCAN_MODE:
                case CMD_SET_SCAN_TYPE:
                case CMD_SET_HIGH_PERF_MODE:
                case CMD_SET_COUNTRY_CODE:
                case CMD_SET_FREQUENCY_BAND:
                case CMD_START_PACKET_FILTERING:
                case CMD_STOP_PACKET_FILTERING:
                case WifiP2pService.P2P_ENABLE_PENDING:
                    deferMessage(message);
                    break;
                default:
                    return NOT_HANDLED;
            }
            EventLog.writeEvent(EVENTLOG_WIFI_EVENT_HANDLED, message.what);
            return HANDLED;
        }
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    private void loge(String s) {
        Log.e(TAG, s);
    }
    
    public boolean syncDoWpsPbc(AsyncChannel channel, String bssid) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_DO_WPS_PBC, bssid);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }
    
    public String syncDoWpsPin(AsyncChannel channel, String bssid) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_DO_WPS_PIN, bssid);
        String result = (String)resultMsg.obj;
        resultMsg.recycle();
        return result;
    }
    
    public boolean syncDoWpsAbort(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_DO_WPS_ABORT);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }
    
    public boolean syncDoCTIATestOn(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_DO_CTIA_TEST_ON);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }
    
    public boolean syncDoCTIATestOff(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_DO_CTIA_TEST_OFF);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }
    
    public boolean syncDoCTIATestRate(AsyncChannel channel, int rate) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_DO_CTIA_TEST_RATE, rate);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }
    
    public boolean syncDoCTIATestPower(AsyncChannel channel, int powerMode) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_DO_CTIA_TEST_POWER, powerMode);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }
    
    public boolean syncDoCTIATestSet(AsyncChannel channel, int id, int value) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_DO_CTIA_TEST_SET, id, value);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }
    
    public String syncDoCTIATestGet(AsyncChannel channel, int id) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_DO_CTIA_TEST_GET, id);
        String result = (String)resultMsg.obj;
        resultMsg.recycle();
        return result;
    }
    
    //MTK_OP01_PROTECT_START
    public boolean syncSaveAPPriority(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_SAVE_AP_PRIORITY);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }
    
    public boolean syncSetConnectPolicy(AsyncChannel channel, int connectType, int cellToWlan) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_SET_CONNECT_POLICY, connectType, cellToWlan);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }
    
    public void setConnectPolicy(int connectType, int cellToWlan) {
        if (OPTR.equals("OP01")) {
            Xlog.d(TAG, "setConnectPolicy, connectType=" + connectType + ", cellToWlan=" + cellToWlan);
            if (mConnectPolicy == connectType && mCellToWiFiPolicy == cellToWlan) {
                return;
            }
            mConnectPolicy = connectType;
            mCellToWiFiPolicy = cellToWlan;
            updateAutoConnectSettings();
        }
    }
    
    private boolean shouldAutoConnect() {
        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (null == info) {
            Xlog.d(TAG, "No active network");
        } else {
            Xlog.d(TAG, "Active network type:" + info.getTypeName());
        }
        if (mConnectPolicy == Settings.System.WIFI_CONNECT_AP_TYPE_AUTO 
            && (info == null /*|| (info != null && info.getType() == ConnectivityManager.TYPE_WIFI)*/
                || (info != null && info.getType() == ConnectivityManager.TYPE_MOBILE && mCellToWiFiPolicy == Settings.System.WIFI_CONNECT_TYPE_AUTO))) {
            Xlog.d(TAG, "Should auto connect");
            return true;
        } else {
            Xlog.d(TAG, "Shouldn't auto connect");
            return false;
        }
    }
    
    private void disableLastNetwork() {
    	if (OPTR.equals("OP01")) {
            Xlog.d(TAG, "disableLastNetwork, getCurrentState=" + getCurrentState() + ", mLastBssid=" + mLastBssid);
            if (getCurrentState() != mSupplicantStoppingState) {
                WifiConfigStore.disableNetwork(mLastNetworkId, WifiConfiguration.DISABLED_UNKNOWN_REASON);
            }
        }
    }
    
    private void disableAllNetworks(boolean except) {
        if (OPTR.equals("OP01")) {
            Xlog.d(TAG, "disableAllNetworks, except=" + except + ", mConnectPolicy=" + mConnectPolicy + ", mCellToWiFiPolicy=" + mCellToWiFiPolicy);
            List<WifiConfiguration> networks = syncGetConfiguredNetworks();
            if (except) {
                if (null != networks) {
                    for (WifiConfiguration network : networks) {
                        if (network.networkId != mLastNetworkId && network.status != WifiConfiguration.Status.DISABLED) {
                            WifiConfigStore.disableNetwork(network.networkId, WifiConfiguration.DISABLED_UNKNOWN_REASON);
                        }
                    }
                }
                return;
            }
            
            if (shouldAutoConnect()) {
                return;
            }
            
            if (null != networks) {
                for (WifiConfiguration network : networks) {
                    if (network.status != WifiConfiguration.Status.DISABLED) {
                        WifiConfigStore.disableNetwork(network.networkId, WifiConfiguration.DISABLED_UNKNOWN_REASON);
                    }
                }
            }
        }
    }

    private void updateAutoConnectSettings() {
        if (OPTR.equals("OP01")) {
            boolean isConnecting = isNetworksDisabledDuringConnect();
            Xlog.d(TAG, "updateAutoConnectSettings, isConnecting=" + isConnecting);
            List<WifiConfiguration> networks = syncGetConfiguredNetworks();
            if (null != networks) {
                if (shouldAutoConnect()) {                   
                    if (!isConnecting) {
                        for (WifiConfiguration network : networks) {
                            if (network.networkId != mLastNetworkId && network.disableReason == WifiConfiguration.DISABLED_UNKNOWN_REASON) {
                                WifiConfigStore.enableNetwork(network.networkId, false);
                            }
                        }
                    }
                } else {
                    if (!isConnecting) {
                        for (WifiConfiguration network : networks) {
                            if (network.networkId != mLastNetworkId && network.status != WifiConfiguration.Status.DISABLED) {
                                WifiConfigStore.disableNetwork(network.networkId, WifiConfiguration.DISABLED_UNKNOWN_REASON);
                            }
                        }
                    }
                }
            }
        }
    }
    
    public boolean isNetworksDisabledDuringConnect() {
        return mSupplicantStateTracker.isNetworksDisabledDuringConnect() || (getCurrentState() == mWaitForWpsCompletionState);
    }

    public void updateScanInterval(boolean screenOn) {
        sendMessage(obtainMessage(CMD_UPDATE_SCAN_INTERVAL, screenOn ? 1 : 0, 0));
    }
    //MTK_OP01_PROTECT_END
    
    public int syncGetConnectingNetworkId(AsyncChannel channel) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_GET_CONNECTING_NETWORK_ID);
        int result = resultMsg.arg1;
        resultMsg.recycle();
        return result;
    }
    
    public void syncUpdateRssi(AsyncChannel channel) {
        if (getCurrentState() == mConnectingState || SupplicantState.isHandshakeState(mWifiInfo.getSupplicantState())) {
            Message resultMsg = channel.sendMessageSynchronously(CMD_UPDATE_RSSI);
            resultMsg.recycle();
        }
    }
    
    public boolean syncSetTxPowerEnabled(AsyncChannel channel, boolean enable) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_SET_TX_POWER_ENABLED, enable ? 1 : 0);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }
    
    public boolean syncSetTxPower(AsyncChannel channel, int offset) {
        Message resultMsg = channel.sendMessageSynchronously(CMD_SET_TX_POWER, offset);
        boolean result = (resultMsg.arg1 != FAILURE);
        resultMsg.recycle();
        return result;
    }
    
    private boolean isAirplaneSensitive() {
        String airplaneModeRadios = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_RADIOS);
        return airplaneModeRadios == null
            || airplaneModeRadios.contains(Settings.System.RADIO_WIFI);
    }

    private boolean isAirplaneModeOn() {
        return isAirplaneSensitive() && Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }

    private class HotspotAutoDisableObserver extends ContentObserver {
        public HotspotAutoDisableObserver(Handler handler) {
            super(handler);
            ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(Settings.System.getUriFor(
                Settings.System.WIFI_HOTSPOT_AUTO_DISABLE), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mDuration = Settings.System.getInt(mContext.getContentResolver(), Settings.System.WIFI_HOTSPOT_AUTO_DISABLE,
                    Settings.System.WIFI_HOTSPOT_AUTO_DISABLE_FOR_FIVE_MINS);
            if (mDuration != Settings.System.WIFI_HOTSPOT_AUTO_DISABLE_OFF) {
                if (mClientNum == 0) {
                    mAlarmManager.cancel(mIntentStopHotspot);
                    mAlarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + mDuration * HOTSPOT_DISABLE_MS, mIntentStopHotspot);
                }
            } else {
                mAlarmManager.cancel(mIntentStopHotspot);
            }
        }
    }
    
    public List<WifiConfiguration> syncGetRssiWeakNetworks() {
        List<WifiConfiguration> networks = new ArrayList<WifiConfiguration>();
        synchronized (mRssiWeakNetworks) {
            for(WifiConfiguration config : mRssiWeakNetworks) {
                networks.add(new WifiConfiguration(config));
            }
        }
        return networks;
    }
    
    public void syncSetRssiWeakNetwork(List<WifiConfiguration> rssiWeakNetworks) {
        synchronized (mRssiWeakNetworks) {
            mRssiWeakNetworks.clear();
            for(WifiConfiguration config : rssiWeakNetworks) {
                mRssiWeakNetworks.add(new WifiConfiguration(config));
            }
        }
    }

    private class GPRSObserver extends ContentObserver {

        public GPRSObserver(Handler handler) {
            super(handler);
            ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(Settings.System.getUriFor(
                Settings.System.GPRS_CONNECTION_SIM_SETTING), false, this);
            cr.registerContentObserver(Settings.System.getUriFor(
                Settings.System.GPRS_CONNECTION_SETTING), false, this);
            cr.registerContentObserver(Settings.Secure.getUriFor(
                Settings.Secure.MOBILE_DATA), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (ActivityManagerNative.isSystemReady()) {
                if (FeatureOption.MTK_GEMINI_SUPPORT) {                           
                    int slot = -1;
                    if (FeatureOption.MTK_GEMINI_ENHANCEMENT) {
                        long currentDataConnectionMultiSimId = Settings.System.getLong(mContext.getContentResolver(), Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
                        slot = SIMInfo.getSlotById(mContext, currentDataConnectionMultiSimId);
                    } else {
                        slot = Settings.System.getInt(mContext.getContentResolver(), GPRS_CONNECTION_SETTING, GPRS_CONNECTION_SETTING_DEFAULT) - 1;
                    }
                    mIsGPRSDisabled = (slot != 1 && slot != 0);
                } else {                               
                    mIsGPRSDisabled = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.MOBILE_DATA, 1) != 1;
                }
            }
        }
    }

    public void setDeviceIdle(boolean deviceIdle) {
        mDeviceIdle = deviceIdle;
    }

    public boolean shouldStartWifi() {
        Xlog.d(TAG, "shouldStartWifi, mDeviceIdle=" + mDeviceIdle + ", currentState=" + getCurrentState());
        if (mDeviceIdle && getCurrentState() == mDriverStoppedState) {
            return false;
        } else {
            return true;
        }
    }
}
