/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.R;
import com.android.settings.Utils;
import static android.provider.Telephony.Intents.EXTRA_PLMN;
import static android.provider.Telephony.Intents.EXTRA_SHOW_PLMN;
import static android.provider.Telephony.Intents.EXTRA_SHOW_SPN;
import static android.provider.Telephony.Intents.EXTRA_SPN;
import static android.provider.Telephony.Intents.SPN_STRINGS_UPDATED_ACTION;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;

import com.mediatek.xlog.Xlog;

/**
 * Display the following information
 * # Phone Number
 * # Network
 * # Roaming
 * # Device Id (IMEI in GSM and MEID in CDMA)
 * # Network type
 * # Signal Strength
 * # Battery Strength  : TODO
 * # Uptime
 * # Awake Time
 * # XMPP/buzz/tickle status : TODO
 *
 */
public class Status extends PreferenceActivity {

    private static final String TAG = "DeviceInfoStatus";
    private static final String KEY_DATA_STATE = "data_state";
    private static final String KEY_SERVICE_STATE = "service_state";
    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_ROAMING_STATE = "roaming_state";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_PHONE_NUMBER = "number";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_PRL_VERSION = "prl_version";
    private static final String KEY_MIN_NUMBER = "min_number";
    private static final String KEY_MEID_NUMBER = "meid_number";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private static final String KEY_BATTERY_STATUS = "battery_status";
    private static final String KEY_BATTERY_LEVEL = "battery_level";
    private static final String KEY_IP_ADDRESS = "wifi_ip_address";
    private static final String KEY_WIMAX_MAC_ADDRESS = "wimax_mac_address";
    private static final String KEY_WIFI_MAC_ADDRESS = "wifi_mac_address";
    private static final String KEY_BT_ADDRESS = "bt_address";
    private static final String KEY_SERIAL_NUMBER = "serial_number";
    private static final String KEY_ICC_ID = "icc_id";

    private static final String[] PHONE_RELATED_ENTRIES = {
        KEY_DATA_STATE,
        KEY_SERVICE_STATE,
        KEY_OPERATOR_NAME,
        KEY_ROAMING_STATE,
        KEY_NETWORK_TYPE,
        KEY_PHONE_NUMBER,
        KEY_IMEI,
        KEY_IMEI_SV,
        KEY_PRL_VERSION,
        KEY_MIN_NUMBER,
        KEY_MEID_NUMBER,
        KEY_SIGNAL_STRENGTH,
        KEY_ICC_ID
    };

    private static final int EVENT_SIGNAL_STRENGTH_CHANGED = 200;
    private static final int EVENT_SERVICE_STATE_CHANGED = 300;

    private static final int EVENT_UPDATE_STATS = 500;

    private TelephonyManager mTelephonyManager;
    private Phone mPhone = null;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private Resources mRes;
    private Preference mSignalStrength;
    private Preference mUptime;

    private static String sUnknown;
    private static String mOperatorName;
    private Preference mBatteryStatus;
    private Preference mBatteryLevel;

    private Handler mHandler;

    private int mServiceState;
    
    private static class MyHandler extends Handler {
        private WeakReference<Status> mStatus;

        public MyHandler(Status activity) {
            mStatus = new WeakReference<Status>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Status status = mStatus.get();
            if (status == null) {
                return;
            }

            switch (msg.what) {
                case EVENT_SIGNAL_STRENGTH_CHANGED:
                    SignalStrength signalStrength = status.mPhone.getSignalStrength();
                    status.updateSignalStrength(signalStrength);
                    break;

                case EVENT_SERVICE_STATE_CHANGED:
                    ServiceState serviceState = status.mPhoneStateReceiver.getServiceState();
                    status.updateServiceState(serviceState);
                    break;

                case EVENT_UPDATE_STATS:
                    status.updateTimes();
                    sendEmptyMessageDelayed(EVENT_UPDATE_STATS, 1000);
                    break;
            }
        }
    }
    
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel.setSummary(Utils.getBatteryPercentage(intent));
                mBatteryStatus.setSummary(Utils.getBatteryStatus(getResources(), intent));
            }
//MTK_OP01_PROTECT_START
            else if ((mOperatorName.equals("OP01"))
                    && (SPN_STRINGS_UPDATED_ACTION.equals(action))) {
                String operator_name = null;
                String plmn = null;
                String spn = null;
                if (intent.getBooleanExtra(EXTRA_SHOW_PLMN, false)) {
                    plmn = intent.getStringExtra(EXTRA_PLMN);
                }

                if (intent.getBooleanExtra(EXTRA_SHOW_SPN, false)) {
                    spn = intent.getStringExtra(EXTRA_SPN);
                }
                if (plmn != null) {
                    operator_name = plmn;
                } else if (spn != null) {
                    operator_name = spn;
                }
                setSummaryText("operator_name", operator_name);
            }
//MTK_OP01_PROTECT_END
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataConnectionStateChanged(int state) {
            updateDataState();
            updateNetworkType();
        }
    };
    
    private PhoneStateListener mSignalStrengthListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength){
        	updateSignalStrength(signalStrength);
        }
    };

    private PhoneStateListener mPhoneServiceListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState){
        	mServiceState = serviceState.getState();
        	updateServiceState(serviceState);
        }
    };
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Preference removablePref;

        mHandler = new MyHandler(this);

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.device_info_status);
        mBatteryLevel = findPreference(KEY_BATTERY_LEVEL);
        mBatteryStatus = findPreference(KEY_BATTERY_STATUS);
        Preference p = findPreference("serial_number");
        if(p!=null){
            p.setSummary(getDeviceSerialNumber());
        }
        
        mRes = getResources();
        if (sUnknown == null) {
            sUnknown = mRes.getString(R.string.device_info_default);
        }
        mOperatorName = SystemProperties.get("ro.operator.optr", "");
        mPhone = PhoneFactory.getDefaultPhone();
        // Note - missing in zaku build, be careful later...
        mSignalStrength = findPreference(KEY_SIGNAL_STRENGTH);
        mUptime = findPreference("up_time");

        if (Utils.isWifiOnly(getApplicationContext())) {
            for (String key : PHONE_RELATED_ENTRIES) {
                removePreferenceFromScreen(key);
            }
        } else {
            // NOTE "imei" is the "Device ID" since it represents
            //  the IMEI in GSM and the MEID in CDMA
            if (mPhone.getPhoneName().equals("CDMA")) {
                setSummaryText(KEY_MEID_NUMBER, mPhone.getMeid());
                setSummaryText(KEY_MIN_NUMBER, mPhone.getCdmaMin());
                if (getResources().getBoolean(R.bool.config_msid_enable)) {
                    findPreference(KEY_MIN_NUMBER).setTitle(R.string.status_msid_number);
                }
                setSummaryText(KEY_PRL_VERSION, mPhone.getCdmaPrlVersion());
                removePreferenceFromScreen(KEY_IMEI_SV);

                if (mPhone.getLteOnCdmaMode() == Phone.LTE_ON_CDMA_TRUE) {
                    // Show ICC ID and IMEI for LTE device
                    setSummaryText(KEY_ICC_ID, mPhone.getIccSerialNumber());
                    setSummaryText(KEY_IMEI, mPhone.getImei());
                } else {
                    // device is not GSM/UMTS, do not display GSM/UMTS features
                    // check Null in case no specified preference in overlay xml
                    removePreferenceFromScreen(KEY_IMEI);
                    removePreferenceFromScreen(KEY_ICC_ID);
                }
            } else {
                setSummaryText(KEY_IMEI, mPhone.getDeviceId());

                setSummaryText(KEY_IMEI_SV,
                        ((TelephonyManager) getSystemService(TELEPHONY_SERVICE))
                            .getDeviceSoftwareVersion());

                // device is not CDMA, do not display CDMA features
                // check Null in case no specified preference in overlay xml
                removePreferenceFromScreen(KEY_PRL_VERSION);
                removePreferenceFromScreen(KEY_MEID_NUMBER);
                removePreferenceFromScreen(KEY_MIN_NUMBER);
                removePreferenceFromScreen(KEY_ICC_ID);
            }

            String rawNumber = mPhone.getLine1Number();  // may be null or empty
            String formattedNumber = null;
            if (!TextUtils.isEmpty(rawNumber)) {
                formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
            }
            // If formattedNumber is null or empty, it'll display as "Unknown".
            setSummaryText(KEY_PHONE_NUMBER, formattedNumber);

            mPhoneStateReceiver = new PhoneStateIntentReceiver(this, mHandler);
            mPhoneStateReceiver.notifySignalStrength(EVENT_SIGNAL_STRENGTH_CHANGED);
            mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);
        }
        setWimaxStatus();
        setWifiStatus();
        setBtStatus();
        setIpAddressStatus();

        String serial = Build.SERIAL;
        if (serial != null && !serial.equals("")) {
            setSummaryText(KEY_SERIAL_NUMBER, serial);
        } else {
            removePreferenceFromScreen(KEY_SERIAL_NUMBER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Utils.isWifiOnly(getApplicationContext())) {
            mPhoneStateReceiver.registerIntent();

            ServiceState serviceState = mPhone.getServiceState();
            updateServiceState(serviceState);
            mServiceState = serviceState.getState();
            SignalStrength signalStrength = mPhone.getSignalStrength();
            updateSignalStrength(signalStrength);
            updateServiceState(serviceState);
            updateDataState();

            mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
            mTelephonyManager.listen(mSignalStrengthListener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            mTelephonyManager.listen(mPhoneServiceListener,
                PhoneStateListener.LISTEN_SERVICE_STATE);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
//MTK_OP01_PROTECT_START
        if (mOperatorName.equals("OP01")) {
            filter.addAction(SPN_STRINGS_UPDATED_ACTION);
        }
//MTK_OP01_PROTECT_END
        registerReceiver(mBatteryInfoReceiver, filter);
        mHandler.sendEmptyMessage(EVENT_UPDATE_STATS);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (!Utils.isWifiOnly(getApplicationContext())) {
            mPhoneStateReceiver.unregisterIntent();
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            mTelephonyManager.listen(mSignalStrengthListener,PhoneStateListener.LISTEN_NONE);
        }
        unregisterReceiver(mBatteryInfoReceiver);
        mHandler.removeMessages(EVENT_UPDATE_STATS);
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    /**
     * @param preference The key for the Preference item
     * @param property The system property to fetch
     * @param alt The default value, if the property doesn't exist
     */
    private void setSummary(String preference, String property, String alt) {
        try {
        	String strSummary = SystemProperties.get(property, alt);
        	
            findPreference(preference).setSummary(
            		(strSummary.equals("unknown") == true)?sUnknown:strSummary);
  
        } catch (RuntimeException e) {

        }
    }

    private void setSummaryText(String preference, String text) {
            if (TextUtils.isEmpty(text)) {
            	text = this.getResources().getString(R.string.device_info_default);
             }
             // some preferences may be missing
            Preference p = findPreference(preference);
             if (p != null) {
                 p.setSummary(text);
             }
    }

    private void updateNetworkType() {
        // Whether EDGE, UMTS, etc...
    	sUnknown = this.getResources().getString(R.string.device_info_default);
        setSummary(KEY_NETWORK_TYPE, TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE, sUnknown);
    }

    private void updateDataState() {
        int state = mTelephonyManager.getDataState();
        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
                display = mRes.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = mRes.getString(R.string.radioInfo_data_suspended);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = mRes.getString(R.string.radioInfo_data_disconnected);
                break;
        }

        setSummaryText(KEY_DATA_STATE, display);
    }

    private void updateServiceState(ServiceState serviceState) {
        int state = serviceState.getState();
        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                display = mRes.getString(R.string.radioInfo_service_in);
                break;
            case ServiceState.STATE_OUT_OF_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                display = mRes.getString(R.string.radioInfo_service_out);
                break;
            case ServiceState.STATE_POWER_OFF:
                display = mRes.getString(R.string.radioInfo_service_off);
                break;
        }

        setSummaryText(KEY_SERVICE_STATE, display);

        if (serviceState.getRoaming()) {
            setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_not));
        }
//MTK_OP02_PROTECT_START
//MTK_OP03_PROTECT_START
        if(!(mOperatorName.equals("OP01"))) {
            setSummaryText(KEY_OPERATOR_NAME, serviceState.getOperatorAlphaLong());
        }
//MTK_OP03_PROTECT_END
//MTK_OP02_PROTECT_END
    }
    
    void updateSignalStrength(SignalStrength signalStrength) {
        // TODO PhoneStateIntentReceiver is deprecated and PhoneStateListener
        // should probably used instead.

        // not loaded in some versions of the code (e.g., zaku)
        if (mSignalStrength != null) {
            int state =
                    mPhoneStateReceiver.getServiceState().getState();
            Resources r = getResources();

            if ((ServiceState.STATE_OUT_OF_SERVICE == mServiceState) ||
                    (ServiceState.STATE_POWER_OFF == mServiceState)) {
                mSignalStrength.setSummary("0");
            }

            int signalDbm = signalStrength.getGsmSignalStrengthDbm();
            int signalAsu = signalStrength.getGsmSignalStrength();
            if (-1 == signalDbm) signalDbm = 0;
            if (99 == signalAsu) signalAsu = 0;

            mSignalStrength.setSummary(String.valueOf(signalDbm) + " "
                        + r.getString(R.string.radioInfo_display_dbm) + "   "
                        + String.valueOf(signalAsu) + " "
                        + r.getString(R.string.radioInfo_display_asu));
        }
    }

    private void setWimaxStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);

        if (ni == null) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = (Preference) findPreference(KEY_WIMAX_MAC_ADDRESS);
            if (ps != null) root.removePreference(ps);
        } else {
            Preference wimaxMacAddressPref = findPreference(KEY_WIMAX_MAC_ADDRESS);
            String macAddress = SystemProperties.get("net.wimax.mac.address",
                    getString(R.string.status_unavailable));
            wimaxMacAddressPref.setSummary(macAddress);
        }

        ni = null;
        if (cm != null) {
        	ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);
        }
        
        if (ni == null) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = (Preference) findPreference(KEY_WIMAX_MAC_ADDRESS);
            if (ps != null)
                root.removePreference(ps);
        } else {
            Preference wimaxMacAddressPref = findPreference(KEY_WIMAX_MAC_ADDRESS);
            String macAddress = SystemProperties.get("net.wimax.mac.address",
                    getString(R.string.status_unavailable));
            wimaxMacAddressPref.setSummary(macAddress);
        }
    }

    private void setWifiStatus() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        Preference wifiMacAddressPref = findPreference(KEY_WIFI_MAC_ADDRESS);
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        wifiMacAddressPref.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress
                : getString(R.string.status_unavailable));
    }

    private void setIpAddressStatus() {
        Preference ipAddressPref = findPreference(KEY_IP_ADDRESS);
        String ipAddress = Utils.getDefaultIpAddresses(this);
        if (ipAddress != null) {
            ipAddressPref.setSummary(ipAddress);
        } else {
            ipAddressPref.setSummary(getString(R.string.status_unavailable));
        }
    }

    private void setBtStatus() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        Preference btAddressPref = findPreference(KEY_BT_ADDRESS);

        if (bluetooth == null) {
            // device not BT capable
            getPreferenceScreen().removePreference(btAddressPref);
        } else {
            String address = bluetooth.isEnabled() ? bluetooth.getAddress() : null;
            btAddressPref.setSummary(!TextUtils.isEmpty(address) ? address
                    : getString(R.string.status_unavailable));
        }
    }

    void updateTimes() {
        long at = SystemClock.uptimeMillis() / 1000;
        long ut = SystemClock.elapsedRealtime() / 1000;

        if (ut == 0) {
            ut = 1;
        }

        mUptime.setSummary(convert(ut));
    }

    private String pad(int n) {
        if (n >= 10) {
            return String.valueOf(n);
        } else {
            return "0" + String.valueOf(n);
        }
    }

    private String convert(long t) {
        int s = (int)(t % 60);
        int m = (int)((t / 60) % 60);
        int h = (int)((t / 3600));

        return h + ":" + pad(m) + ":" + pad(s);
    }

    private String getDeviceSerialNumber() {
        String devSerialNumStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/sys/SystemInfo/SerialNumber"), 64);
            try {
                devSerialNumStr = reader.readLine();
            } finally {
                reader.close();
            }

            return devSerialNumStr;
        } catch (IOException e) {  
            Xlog.e(TAG,
                "IO Exception when getting device serial number",
                e);

            return "Unavailable";
        }
    }
}
