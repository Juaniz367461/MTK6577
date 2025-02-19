/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.phone;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.LocalPowerManager;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.provider.CallLog.Calls;
import android.provider.Settings.System;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.gemini.GeminiNetworkSubUtil;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.OtaUtils.CdmaOtaScreenState;
import com.android.internal.telephony.cdma.TtyIntent;
import com.android.phone.OtaUtils.CdmaOtaScreenState;
import com.android.server.sip.SipService;
import android.os.Bundle;
import com.android.internal.telephony.gemini.*;
import com.mediatek.vt.VTManager;
import android.os.AsyncResult;
import com.android.internal.telephony.IccCard;
import android.content.ComponentName;
import java.util.List;
import android.os.RemoteException;
import android.os.SystemService;
import com.android.phone.PhoneFeatureConstants.FeatureOption;

import com.mediatek.CellConnService.CellConnMgr;

/**
 * Top-level Application class for the Phone app.
 */
public class PhoneApp extends Application implements AccelerometerListener.OrientationListener {
    /* package */ static final String LOG_TAG = "PhoneApp";

    /**
     * Phone app-wide debug level:
     *   0 - no debug logging
     *   1 - normal debug logging if ro.debuggable is set (which is true in
     *       "eng" and "userdebug" builds but not "user" builds)
     *   2 - ultra-verbose debug logging
     *
     * Most individual classes in the phone app have a local DBG constant,
     * typically set to
     *   (PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1)
     * or else
     *   (PhoneApp.DBG_LEVEL >= 2)
     * depending on the desired verbosity.
     *
     * ***** DO NOT SUBMIT WITH DBG_LEVEL > 0 *************
     */
    /* package */ static final int DBG_LEVEL = 1;

    //private static final boolean DBG =
    //         (PhoneApp.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    // private static final boolean VDBG = (PhoneApp.DBG_LEVEL >= 2);
    
    private static final int USE_CM = 1;  // use call manager or not

    private static final boolean DBG = true ;
    private static final boolean VDBG = true;

    // Message codes; see mHandler below.
    private static final int EVENT_SIM_NETWORK_LOCKED = 3;
    private static final int EVENT_SIM1_NETWORK_LOCKED = 19;
    private static final int EVENT_SIM2_NETWORK_LOCKED = 21;
    private static final int EVENT_WIRED_HEADSET_PLUG = 7;
    private static final int EVENT_SIM_STATE_CHANGED = 8;
    private static final int EVENT_UPDATE_INCALL_NOTIFICATION = 9;
    private static final int EVENT_DATA_ROAMING_DISCONNECTED = 10;
    private static final int EVENT_DATA_ROAMING_OK = 11;
    private static final int EVENT_UNSOL_CDMA_INFO_RECORD = 12;
    private static final int EVENT_DOCK_STATE_CHANGED = 13;
    private static final int EVENT_TTY_PREFERRED_MODE_CHANGED = 14;
    private static final int EVENT_TTY_MODE_GET = 15;
    private static final int EVENT_TTY_MODE_SET = 16;
    private static final int EVENT_START_SIP_SERVICE = 17;
    private static final int EVENT_TIMEOUT = 18;
    private static final int EVENT_TOUCH_ANSWER_VT = 30;

  //Msg event for SIM Lock
    private static final int SIM1QUERY = 120;
    private static final int SIM2QUERY = 122; 
    
    

    // The MMI codes are also used by the InCallScreen.
    public static final int MMI_INITIATE = 51;
    public static final int MMI_COMPLETE = 52;
    public static final int MMI_CANCEL = 53;

    public static final int MMI_INITIATE2 = 54; 
    public static final int MMI_COMPLETE2 = 55;
    public static final int MMI_CANCEL2 = 56;
    public static final int EVENT_SHOW_INCALL_SCREEN_FOR_STK_SETUP_CALL = 57;
    public static final int DELAY_SHOW_INCALL_SCREEN_FOR_STK_SETUP_CALL = 160;

    private static final String PERMISSION = android.Manifest.permission.PROCESS_OUTGOING_CALLS;
    private static final String STKCALL_REGISTER_SPEECH_INFO = "com.android.stk.STKCALL_REGISTER_SPEECH_INFO";
    public static final String MISSEDCALL_DELETE_INTENT = "com.android.phone.MISSEDCALL_DELETE_INTENT";
    // Don't use message codes larger than 99 here; those are reserved for
    // the individual Activities of the Phone UI.
    public static final String OLD_NETWORK_MODE = "com.android.phone.OLD_NETWORK_MODE";
    public static final String NETWORK_MODE_CHANGE = "com.android.phone.NETWORK_MODE_CHANGE";
    public static final String NETWORK_MODE_CHANGE_RESPONSE = "com.android.phone.NETWORK_MODE_CHANGE_RESPONSE";
    public static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 10011;

    public static final boolean sGemini = FeatureOption.MTK_GEMINI_SUPPORT;
    public static final boolean sVideoCallSupport = true;

    private static final String ACTION_MODEM_STATE = "com.mtk.ACTION_MODEM_STATE";
    private static final int CCCI_MD_BROADCAST_EXCEPTION = 1;
    private static final int CCCI_MD_BROADCAST_RESET = 2;
    private static final int CCCI_MD_BROADCAST_READY = 3;
    
    /**
     * Allowable values for the poke lock code (timeout between a user activity and the
     * going to sleep), please refer to {@link com.android.server.PowerManagerService}
     * for additional reference.
     *   SHORT uses the short delay for the timeout (SHORT_KEYLIGHT_DELAY, 6 sec)
     *   MEDIUM uses the medium delay for the timeout (MEDIUM_KEYLIGHT_DELAY, 15 sec)
     *   DEFAULT is the system-wide default delay for the timeout (1 min)
     */
    public enum ScreenTimeoutDuration {
        SHORT,
        MEDIUM,
        DEFAULT
    }

    /**
     * Allowable values for the wake lock code.
     *   SLEEP means the device can be put to sleep.
     *   PARTIAL means wake the processor, but we display can be kept off.
     *   FULL means wake both the processor and the display.
     */
    public enum WakeState {
        SLEEP,
        PARTIAL,
        FULL
    }

    private static PhoneApp sMe;

    // A few important fields we expose to the rest of the package
    // directly (rather than thru set/get methods) for efficiency.
    Phone phone;
    CallController callController;
    InCallUiState inCallUiState;
    CallNotifier notifier;
    NotificationMgr notificationMgr;
    Ringer ringer;
    BluetoothHandsfree mBtHandsfree;
    PhoneInterfaceManager phoneMgr;
    CallManager mCM;
    MTKCallManager mCMGemini;
    int mBluetoothHeadsetState = BluetoothProfile.STATE_DISCONNECTED;
    int mBluetoothHeadsetAudioState = BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
    boolean mShowBluetoothIndication = false;
    static int mDockState = Intent.EXTRA_DOCK_STATE_UNDOCKED;
    static boolean sVoiceCapable = true;

    // Internal PhoneApp Call state tracker
    CdmaPhoneCallState cdmaPhoneCallState;

    // The InCallScreen instance (or null if the InCallScreen hasn't been
    // created yet.)
    private InCallScreen mInCallScreen;

    // The currently-active PUK entry activity and progress dialog.
    // Normally, these are the Emergency Dialer and the subsequent
    // progress dialog.  null if there is are no such objects in
    // the foreground.
    private Activity mPUKEntryActivity;
    private ProgressDialog mPUKEntryProgressDialog;

    private boolean mIsSimPinEnabled;
    private String mCachedSimPin;

    // True if a wired headset is currently plugged in, based on the state
    // from the latest Intent.ACTION_HEADSET_PLUG broadcast we received in
    // mReceiver.onReceive().
    private boolean mIsHeadsetPlugged;

    // True if the keyboard is currently *not* hidden
    // Gets updated whenever there is a Configuration change
    private boolean mIsHardKeyboardOpen;

    // True if we are beginning a call, but the phone state has not changed yet
    private boolean mBeginningCall;

    // Last phone state seen by updatePhoneState()
    Phone.State mLastPhoneState = Phone.State.IDLE;

    private WakeState mWakeState = WakeState.SLEEP;
    private ScreenTimeoutDuration mScreenTimeoutDuration = ScreenTimeoutDuration.DEFAULT;
    private boolean mIgnoreTouchUserActivity = false;
    private IBinder mPokeLockToken = new Binder();
    private IPowerManager mPowerManagerService;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager.WakeLock mWakeLockForDisconnect;
    private int mWakelockSequence = 0; 
    private PowerManager.WakeLock mPartialWakeLock;
    private PowerManager.WakeLock mProximityWakeLock;
    private KeyguardManager mKeyguardManager;
    private AccelerometerListener mAccelerometerListener;
    private int mOrientation = AccelerometerListener.ORIENTATION_UNKNOWN;

    // Broadcast receiver for various intent broadcasts (see onCreate())
    private final BroadcastReceiver mReceiver = new PhoneAppBroadcastReceiver();

    // Broadcast receiver purely for ACTION_MEDIA_BUTTON broadcasts
    private final BroadcastReceiver mMediaButtonReceiver = new MediaButtonBroadcastReceiver();

    /** boolean indicating restoring mute state on InCallScreen.onResume() */
    private boolean mShouldRestoreMuteOnInCallResume;

    /**
     * The singleton OtaUtils instance used for OTASP calls.
     *
     * The OtaUtils instance is created lazily the first time we need to
     * make an OTASP call, regardless of whether it's an interactive or
     * non-interactive OTASP call.
     */
    public OtaUtils otaUtils;

    // Following are the CDMA OTA information Objects used during OTA Call.
    // cdmaOtaProvisionData object store static OTA information that needs
    // to be maintained even during Slider open/close scenarios.
    // cdmaOtaConfigData object stores configuration info to control visiblity
    // of each OTA Screens.
    // cdmaOtaScreenState object store OTA Screen State information.
    public OtaUtils.CdmaOtaProvisionData cdmaOtaProvisionData;
    public OtaUtils.CdmaOtaConfigData cdmaOtaConfigData;
    public OtaUtils.CdmaOtaScreenState cdmaOtaScreenState;
    public OtaUtils.CdmaOtaInCallScreenUiState cdmaOtaInCallScreenUiState;

    // TTY feature enabled on this platform
    private boolean mTtyEnabled;
    
    AudioManager mAudioManager = null;
    
    public boolean isEnableTTY() {
        return mTtyEnabled;
    }
    // Current TTY operating mode selected by user
    private int mPreferredTtyMode = Phone.TTY_MODE_OFF;

    
    public int ihandledEventSIM2SIMLocked = 0;//whether handled EVENT_SIM2_NETWORK_LOCKED message, 0--not handled,1--already handled
    public int ihandledEventSIM1SIMLocked = 0;//whether handled EVENT_SIM1_NETWORK_LOCKED message, 0--not handled,1--already handled

    public static int[] arySIMLockStatus = {3,3}; //the SIM Lock deal with status
    
    /**
     * Set the restore mute state flag. Used when we are setting the mute state
     * OUTSIDE of user interaction {@link PhoneUtils#startNewCall(Phone)}
     */
    /*package*/void setRestoreMuteOnInCallResume (boolean mode) {
        PhoneLog.d(LOG_TAG, "setRestoreMuteOnInCallResume, mode = "+mode);
        mShouldRestoreMuteOnInCallResume = mode;
    }

    /**
     * Get the restore mute state flag.
     * This is used by the InCallScreen {@link InCallScreen#onResume()} to figure
     * out if we need to restore the mute state for the current active call.
     */
    /*package*/boolean getRestoreMuteOnInCallResume () {
        return mShouldRestoreMuteOnInCallResume;
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Phone.State phoneState;
            switch (msg.what) {
                // Starts the SIP service. It's a no-op if SIP API is not supported
                // on the deivce.
                // TODO: Having the phone process host the SIP service is only
                // temporary. Will move it to a persistent communication process
                // later.
                case EVENT_START_SIP_SERVICE:
                    SipService.start(getApplicationContext());
                    break;

                // TODO: This event should be handled by the lock screen, just
                // like the "SIM missing" and "Sim locked" cases (bug 1804111).
                case EVENT_SIM_NETWORK_LOCKED:
//                    if (getResources().getBoolean(R.bool.ignore_sim_network_locked_events)) {
//                        // Some products don't have the concept of a "SIM network lock"
//                        Log.i(LOG_TAG, "Ignoring EVENT_SIM_NETWORK_LOCKED event; "
//                              + "not showing 'SIM network unlock' PIN entry screen");
//                    } else {
//                        // Normal case: show the "SIM network unlock" PIN entry screen.
//                        // The user won't be able to do anything else until
//                        // they enter a valid SIM network PIN.
//                        Log.i(LOG_TAG, "show sim depersonal panel");
//                        IccNetworkDepersonalizationPanel ndpPanel =
//                                new IccNetworkDepersonalizationPanel(PhoneApp.getInstance());
//                        ndpPanel.show();
//                    }
					Log.d(LOG_TAG, "handle EVENT_SIM_NETWORK_LOCKED +");
                	Intent intent3 = new Intent(PhoneApp.this, PowerOnSetupUnlockSIMLock.class);  
                    intent3.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            		startActivity(intent3); 
                	Log.d(LOG_TAG, "handle EVENT_SIM_NETWORK_LOCKED -");
                    break;
				 case EVENT_SIM1_NETWORK_LOCKED: //SIM 1 Network locked 
//                	try{
//                		Thread.sleep(1000);
//                	}catch(InterruptedException er){
//                		er.printStackTrace();
//                	}
//                	if(bNeedUnlockSIMLock(Phone.GEMINI_SIM_2) == false){//wait for SIM2 call PowerOnSetupUnlockSIMLock
//                		
//                	}else{
//                    	ihandledEventSIM1SIMLocked = 1;//deal with EVENT_SIM2_NETWORK_LOCKED
                	Log.d(LOG_TAG, "[Received][EVENT_SIM1_NETWORK_LOCKED]");	
//                	if (arySIMLockStatus[0] == 0)//not deal with EVENT_SIM1_NETWORK_LOCKED
//                    	{
	                		Log.d(LOG_TAG, "handle EVENT_SIM1_NETWORK_LOCKED +");
	                    	Intent intent = new Intent(PhoneApp.this, PowerOnSetupUnlockSIMLock.class);  
	                		Bundle bundle = new Bundle();
	                        bundle.putInt("Phone.GEMINI_SIM_ID_KEY",0);//To unlock which card  default:-1, Slot1: 0, Slot2:1
	                		intent.putExtras(bundle);
	                    	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                		startActivity(intent); 
	                    	Log.d(LOG_TAG, "handle EVENT_SIM1_NETWORK_LOCKED -");
//                    	}
//                	}


                  break;    
                case EVENT_SIM2_NETWORK_LOCKED: //SIM 2 Network locked
//                	try{
//                		Thread.sleep(1000);
//                	}catch(InterruptedException er){
//                		er.printStackTrace();
//                	}
//                	if(bNeedUnlockSIMLock(Phone.GEMINI_SIM_1) == false){//wait for SIM2 call PowerOnSetupUnlockSIMLock
//                		
//                	}else{
//                    	ihandledEventSIM2SIMLocked = 1;//deal with EVENT_SIM2_NETWORK_LOCKED
                	Log.d(LOG_TAG, "[Received][EVENT_SIM2_NETWORK_LOCKED]");		
//                	if (arySIMLockStatus[1] == 0)//not deal with EVENT_SIM1_NETWORK_LOCKED
//                    	{
                    		Log.d(LOG_TAG, "handle EVENT_SIM2_NETWORK_LOCKED +");
                            
                        	Intent intent2 = new Intent(PhoneApp.this, PowerOnSetupUnlockSIMLock.class); 
	                		Bundle bundle2 = new Bundle();
	                        bundle2.putInt("Phone.GEMINI_SIM_ID_KEY",1);//To unlock which card  default:-1, Slot1: 0, Slot2:1
	                		intent2.putExtras(bundle2);
	                    	intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                    	startActivity(intent2);
                    		Log.d(LOG_TAG, "handle EVENT_SIM2_NETWORK_LOCKED -");
//                    	}
//
//                	}


                    break;	
                case EVENT_UPDATE_INCALL_NOTIFICATION:
                    // Tell the NotificationMgr to update the "ongoing
                    // call" icon in the status bar, if necessary.
                    // Currently, this is triggered by a bluetooth headset
                    // state change (since the status bar icon needs to
                    // turn blue when bluetooth is active.)
                    if (DBG) Log.d (LOG_TAG, "- updating in-call notification from handler...");
                    notificationMgr.updateInCallNotification();
                    break;

                case EVENT_DATA_ROAMING_DISCONNECTED:
                    notificationMgr.showDataDisconnectedRoaming(msg.arg1);
                    break;

                case EVENT_DATA_ROAMING_OK:
                    notificationMgr.hideDataDisconnectedRoaming();
                    break;
                    
                case MMI_INITIATE:
                    if (mInCallScreen == null) {
                        inCallUiState.setPendingUssdMessage(
                                Message.obtain(mHandler, Phone.GEMINI_SIM_1, (AsyncResult) msg.obj));
                        //mInCallScreen.onMMIInitiate((AsyncResult) msg.obj, Phone.GEMINI_SIM_1);
                    }
                    break;
                    
                case MMI_INITIATE2:
                    if (mInCallScreen == null) {
                        inCallUiState.setPendingUssdMessage(
                                Message.obtain(mHandler, Phone.GEMINI_SIM_1, (AsyncResult) msg.obj));
                        //mInCallScreen.onMMIInitiate((AsyncResult) msg.obj, Phone.GEMINI_SIM_2);
                    }
                    break;

                case MMI_COMPLETE:
                    inCallUiState.setPendingUssdMessage(null);
                    onMMIComplete((AsyncResult) msg.obj);
                    break;

                case MMI_COMPLETE2:
                    inCallUiState.setPendingUssdMessage(null);
                    onMMIComplete2((AsyncResult) msg.obj);
                    break;

                case MMI_CANCEL:
                    PhoneUtils.cancelMmiCodeExt(phone, Phone.GEMINI_SIM_1);
                    break;

                case MMI_CANCEL2:					
                    PhoneUtils.cancelMmiCodeExt(phone, Phone.GEMINI_SIM_2);
                    break;		

                case EVENT_WIRED_HEADSET_PLUG:
                    // Since the presence of a wired headset or bluetooth affects the
                    // speakerphone, update the "speaker" state.  We ONLY want to do
                    // this on the wired headset connect / disconnect events for now
                    // though, so we're only triggering on EVENT_WIRED_HEADSET_PLUG.

                    phoneState = mCM.getState();
                    // Do not change speaker state if phone is not off hook
                    if (phoneState == Phone.State.OFFHOOK) {
                        if (!isShowingCallScreen() &&
                            (mBtHandsfree == null || !mBtHandsfree.isAudioOn())) {
                            if (!isHeadsetPlugged()) {
                                // if the state is "not connected", restore the speaker state.
                                PhoneUtils.restoreSpeakerMode(getApplicationContext());
                            } else {
                                // if the state is "connected", force the speaker off without
                                // storing the state.
                                PhoneUtils.turnOnSpeaker(getApplicationContext(), false, false);
                            }
                        }
                    }
                    // Update the Proximity sensor based on headset state
                    updateProximitySensorMode(phoneState);

                    // Force TTY state update according to new headset state
                    if (mTtyEnabled) {
                        sendMessage(obtainMessage(EVENT_TTY_PREFERRED_MODE_CHANGED, 0));
                    }
                    break;

                case EVENT_SIM_STATE_CHANGED:
                    // Marks the event where the SIM goes into ready state.
                    // Right now, this is only used for the PUK-unlocking
                    // process.
                    if (msg.obj.equals(IccCard.INTENT_VALUE_ICC_READY)) {
                        // when the right event is triggered and there
                        // are UI objects in the foreground, we close
                        // them to display the lock panel.
                        if (mPUKEntryActivity != null) {
                            mPUKEntryActivity.finish();
                            mPUKEntryActivity = null;
                        }
                        if (mPUKEntryProgressDialog != null) {
                            mPUKEntryProgressDialog.dismiss();
                            mPUKEntryProgressDialog = null;
                        }
                    }
                    break;

                case EVENT_UNSOL_CDMA_INFO_RECORD:
                    //TODO: handle message here;
                    break;

                case EVENT_DOCK_STATE_CHANGED:
                    // If the phone is docked/undocked during a call, and no wired or BT headset
                    // is connected: turn on/off the speaker accordingly.
                    boolean inDockMode = false;
                    if (mDockState != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                        inDockMode = true;
                    }
                    if (VDBG) Log.d(LOG_TAG, "received EVENT_DOCK_STATE_CHANGED. Phone inDock = "
                            + inDockMode);

                    phoneState = mCM.getState();
                    if (phoneState == Phone.State.OFFHOOK &&
                            !isHeadsetPlugged() &&
                            !(mBtHandsfree != null && mBtHandsfree.isAudioOn())) {
                        PhoneUtils.turnOnSpeaker(getApplicationContext(), inDockMode, true);
                        updateInCallScreen();  // Has no effect if the InCallScreen isn't visible
                    }
                    break;

                case EVENT_TTY_PREFERRED_MODE_CHANGED:
                    // TTY mode is only applied if a headset is connected
                    int ttyMode;
                    if (isHeadsetPlugged()) {
                        ttyMode = mPreferredTtyMode;
                    } else {
                        ttyMode = Phone.TTY_MODE_OFF;
                    }
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        ((GeminiPhone)phone).setTTYModeGemini(convertTTYmodeToRadio(ttyMode), 
                                mHandler.obtainMessage(EVENT_TTY_MODE_SET), Phone.GEMINI_SIM_1);
                        ((GeminiPhone)phone).setTTYModeGemini(convertTTYmodeToRadio(ttyMode), 
                                mHandler.obtainMessage(EVENT_TTY_MODE_SET), Phone.GEMINI_SIM_2);
                    } else {
                        phone.setTTYMode(convertTTYmodeToRadio(ttyMode), mHandler.obtainMessage(EVENT_TTY_MODE_SET));
                    }
                    break;

                case EVENT_TTY_MODE_GET:
                    handleQueryTTYModeResponse(msg);
                    break;

                case EVENT_TTY_MODE_SET:
                    handleSetTTYModeResponse(msg);
                    break;

                case EVENT_TIMEOUT:
                    handleTimeout(msg.arg1);
                    break;
                    
                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Intent it = new Intent(NETWORK_MODE_CHANGE_RESPONSE);
                    if (ar.exception == null) {
                        it.putExtra(NETWORK_MODE_CHANGE_RESPONSE, true);
                        it.putExtra("NEW_NETWORK_MODE", msg.arg2);
                    }else {
                        it.putExtra(NETWORK_MODE_CHANGE_RESPONSE, false);
                        it.putExtra(OLD_NETWORK_MODE, msg.arg1);
                    }
                    sendBroadcast(it);
                    break;
                   
                    
                case EVENT_TOUCH_ANSWER_VT:
                	if (DBG) Log.d (LOG_TAG, "mHandler.handleMessage() : EVENT_TOUCH_ANSWER_VT");
                	try{
                		getInCallScreenInstance().getInCallTouchUi().touchAnswerCall();
                	}catch(Exception e){
                		if (DBG) Log.d (LOG_TAG, "mHandler.handleMessage() : the InCallScreen Instance is null , so cannot answer incoming VT call");
                	}
                	break;

                case EVENT_SHOW_INCALL_SCREEN_FOR_STK_SETUP_CALL:
                    PhoneUtils.showIncomingCallUi();
                	break;
            }
        }
    };

    public PhoneApp() {
        sMe = this;
    }

    @Override
    public void onCreate() {
        if (VDBG) Log.v(LOG_TAG, "onCreate()...");

        String state = SystemProperties.get("vold.decrypt");

        if (!SystemProperties.getBoolean("gsm.phone.created", false)&& ("".equals(state) || "trigger_restart_framework".equals(state))) {
            Log.d(LOG_TAG, "set System Property gsm.phone.created = true");
            SystemProperties.set("gsm.phone.created", "true");
            Settings.System.putLong(getApplicationContext().getContentResolver(),
                    Settings.System.SIM_LOCK_STATE_SETTING, 0x0L);
        }

        ContentResolver resolver = getContentResolver();

        // Cache the "voice capable" flag.
        // This flag currently comes from a resource (which is
        // overrideable on a per-product basis):
        sVoiceCapable =
                getResources().getBoolean(com.android.internal.R.bool.config_voice_capable);
        // ...but this might eventually become a PackageManager "system
        // feature" instead, in which case we'd do something like:
        // sVoiceCapable =
        //   getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_VOICE_CALLS);

        if (phone == null) {
            Log.v(LOG_TAG, "onCreate(), start to make default phone");	
            // Initialize the telephony framework
            PhoneFactory.makeDefaultPhones(this);
            Log.v(LOG_TAG, "onCreate(), make default phone complete");
            // Get the default phone
            phone = PhoneFactory.getDefaultPhone();

            mCM = CallManager.getInstance();
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mCMGemini = MTKCallManager.getInstance();
                mCMGemini.registerPhoneGemini(phone); 
            } else {
                mCM.registerPhone(phone);
            }

            // Create the NotificationMgr singleton, which is used to display
            // status bar icons and control other status bar behavior.
            notificationMgr = NotificationMgr.init(this);

            Log.v(LOG_TAG, "onCreate(), start to new phone interface");

            phoneMgr = PhoneInterfaceManager.init(this, phone);

            mHandler.sendEmptyMessage(EVENT_START_SIP_SERVICE);

            int phoneType = phone.getPhoneType();

            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                // Create an instance of CdmaPhoneCallState and initialize it to IDLE
                cdmaPhoneCallState = new CdmaPhoneCallState();
                cdmaPhoneCallState.CdmaPhoneCallStateInit();
            }

            Log.v(LOG_TAG, "onCreate(), start to get BT default adapter");			

            if (BluetoothAdapter.getDefaultAdapter() != null) {
                // Start BluetoothHandsree even if device is not voice capable.
                // The device can still support VOIP.
                mBtHandsfree = BluetoothHandsfree.init(this, mCM);
                startService(new Intent(this, BluetoothHeadsetService.class));
            } else {
                // Device is not bluetooth capable
                mBtHandsfree = null;
            }

            ringer = Ringer.init(this);

            // before registering for phone state changes
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, LOG_TAG);

            mWakeLockForDisconnect = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
   
            Log.v(LOG_TAG, "onCreate(), new partial wakelock");			

            // lock used to keep the processor awake, when we don't care for the display.
            mPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
            // Wake lock used to control proximity sensor behavior.
            if ((pm.getSupportedWakeLockFlags()
                 & PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) != 0x0) {
                mProximityWakeLock =
                        pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, LOG_TAG);
            }
            if (DBG) Log.d(LOG_TAG, "onCreate: mProximityWakeLock: " + mProximityWakeLock);

            // create mAccelerometerListener only if we are using the proximity sensor
            if (proximitySensorModeEnabled()) {
                mAccelerometerListener = new AccelerometerListener(this, this);
            }

            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

            // get a handle to the service so that we can use it later when we
            // want to set the poke lock.
            mPowerManagerService = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));

            // Create the CallController singleton, which is the interface
            // to the telephony layer for user-initiated telephony functionality
            // (like making outgoing calls.)
            callController = CallController.init(this);
            // ...and also the InCallUiState instance, used by the CallController to
            // keep track of some "persistent state" of the in-call UI.
            inCallUiState = InCallUiState.init(this);

            // Create the CallNotifer singleton, which handles
            // asynchronous events from the telephony layer (like
            // launching the incoming-call UI when an incoming call comes
            // in.)
            Log.v(LOG_TAG, "onCreate(), new callnotifier");
            notifier = CallNotifier.init(this, phone, ringer, mBtHandsfree, new CallLogAsync());

            // register for ICC status
//            IccCard sim = phone.getIccCard();
//           if (sim != null) {
//                if (VDBG) Log.v(LOG_TAG, "register for ICC status");
//                sim.registerForNetworkLocked(mHandler, EVENT_SIM_NETWORK_LOCKED, null);
//            }

//            // register for SIM Lock
//            if (phoneType == Phone.PHONE_TYPE_GSM) {
//                if (FeatureOption.MTK_GEMINI_SUPPORT) {
//                	GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
//                	IccCard sim1Gemini = mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_1);
//                	IccCard sim2Gemini = mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_2);
//                	sim1Gemini.registerForNetworkLocked(mHandler, EVENT_SIM1_NETWORK_LOCKED, null);
//                	sim2Gemini.registerForNetworkLocked(mHandler, EVENT_SIM2_NETWORK_LOCKED, null);
//                } else {
//                    IccCard sim = phone.getIccCard();
//                    if (sim != null) {
//                        if (VDBG) Log.v(LOG_TAG, "register for ICC status");
//                        sim.registerForNetworkLocked(mHandler, EVENT_SIM_NETWORK_LOCKED, null);
//                    }
//                }
//            }

            // register for MMI/USSD
            //For c+g dualtalk case, we must register these notifications
            //if (phoneType == Phone.PHONE_TYPE_GSM) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mCMGemini.registerForMmiCompleteGemini(mHandler, PhoneApp.MMI_COMPLETE, null, Phone.GEMINI_SIM_1);                
                mCMGemini.registerForMmiCompleteGemini(mHandler, PhoneApp.MMI_COMPLETE2, null, Phone.GEMINI_SIM_2);  
                mCMGemini.registerForMmiInitiateGemini(mHandler, PhoneApp.MMI_INITIATE, null, Phone.GEMINI_SIM_1);
                mCMGemini.registerForMmiInitiateGemini(mHandler, PhoneApp.MMI_INITIATE2, null, Phone.GEMINI_SIM_2);
            } else {
                mCM.registerForMmiComplete(mHandler, MMI_INITIATE, null);
                mCM.registerForMmiComplete(mHandler, MMI_COMPLETE, null);
            }
            //}

            Log.v(LOG_TAG, "onCreate(), initialize connection handler");			

            // register connection tracking to PhoneUtils
            PhoneUtils.initializeConnectionHandler(mCM);

            // Read platform settings for TTY feature
            if (PhoneUtils.isSupportFeature("TTY")) {
                mTtyEnabled = getResources().getBoolean(R.bool.tty_enabled);
            } else {
                mTtyEnabled = false;
            }

            Log.v(LOG_TAG, "onCreate(), new intentfilter");			

            // Register for misc other intent broadcasts.
            IntentFilter intentFilter =
                    new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
            intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
            intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
//            intentFilter.addAction(BluetoothHeadset.ACTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
            intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
            intentFilter.addAction(Intent.ACTION_DOCK_EVENT);
            intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
            intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
            if (mTtyEnabled) {
                intentFilter.addAction(TtyIntent.TTY_PREFERRED_MODE_CHANGE_ACTION);
            }
            intentFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
            intentFilter.addAction(Intent.ACTION_SHUTDOWN);
			intentFilter.addAction(STKCALL_REGISTER_SPEECH_INFO);
            intentFilter.addAction(MISSEDCALL_DELETE_INTENT);
            intentFilter.addAction("out_going_call_to_phone_app");
            //Handle the network mode change for enhancement
            intentFilter.addAction(NETWORK_MODE_CHANGE);
            intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
            intentFilter.addAction("android.intent.action.ACTION_PREBOOT_IPO");
            intentFilter.addAction(GeminiPhone.EVENT_3G_SWITCH_START_MD_RESET);
            intentFilter.addAction(TelephonyIntents.ACTION_RADIO_OFF);
            intentFilter.addAction(ACTION_MODEM_STATE);
            registerReceiver(mReceiver, intentFilter);

            // Use a separate receiver for ACTION_MEDIA_BUTTON broadcasts,
            // since we need to manually adjust its priority (to make sure
            // we get these intents *before* the media player.)
            IntentFilter mediaButtonIntentFilter =
                    new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
            //
            // Make sure we're higher priority than the media player's
            // MediaButtonIntentReceiver (which currently has the default
            // priority of zero; see apps/Music/AndroidManifest.xml.)
            mediaButtonIntentFilter.setPriority(1);
            //
            registerReceiver(mMediaButtonReceiver, mediaButtonIntentFilter);

            //set the default values for the preferences in the phone.
            PreferenceManager.setDefaultValues(this, R.xml.network_setting, false);

            PreferenceManager.setDefaultValues(this, R.xml.call_feature_setting, false);

            // Make sure the audio mode (along with some
            // audio-mode-related state of our own) is initialized
            // correctly, given the current state of the phone.
            PhoneUtils.setAudioMode(mCM);
        }

        if (TelephonyCapabilities.supportsOtasp(phone)) {
            cdmaOtaProvisionData = new OtaUtils.CdmaOtaProvisionData();
            cdmaOtaConfigData = new OtaUtils.CdmaOtaConfigData();
            cdmaOtaScreenState = new OtaUtils.CdmaOtaScreenState();
            cdmaOtaInCallScreenUiState = new OtaUtils.CdmaOtaInCallScreenUiState();
        }

        // XXX pre-load the SimProvider so that it's ready
        resolver.getType(Uri.parse("content://icc/adn"));

        // start with the default value to set the mute state.
        mShouldRestoreMuteOnInCallResume = false;

        // TODO: Register for Cdma Information Records
        // phone.registerCdmaInformationRecord(mHandler, EVENT_UNSOL_CDMA_INFO_RECORD, null);

        // Read TTY settings and store it into BP NV.
        // AP owns (i.e. stores) the TTY setting in AP settings database and pushes the setting
        // to BP at power up (BP does not need to make the TTY setting persistent storage).
        // This way, there is a single owner (i.e AP) for the TTY setting in the phone.
        if (mTtyEnabled) {
            mPreferredTtyMode = android.provider.Settings.Secure.getInt(
                    phone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_TTY_MODE,
                    Phone.TTY_MODE_OFF);
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_TTY_PREFERRED_MODE_CHANGED, 0));
        }
        // Read HAC settings and configure audio hardware
        if (getResources().getBoolean(R.bool.hac_enabled)) {
            int hac = android.provider.Settings.System.getInt(phone.getContext().getContentResolver(),
                                                              android.provider.Settings.System.HEARING_AID,
                                                              0);
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setParameter(CallFeaturesSetting.HAC_KEY, hac != 0 ?
                                      CallFeaturesSetting.HAC_VAL_ON :
                                      CallFeaturesSetting.HAC_VAL_OFF);
        }

        /**
         * Change Feature by mediatek .inc
         * description : initilize SimAssociateHandler
         */
        SimAssociateHandler.getInstance().prepair();
        SimAssociateHandler.getInstance().load();
        cellConnMgr = new CellConnMgr();
        cellConnMgr.register(getApplicationContext());
        /**
         * Change Feature by mediatek .inc end
         */

        /**
         * Change Feature by mediatek .inc
         * description : set the global flag that support dualtalk
         */
        DualTalkUtils.init();
        /**
         * Change Feature by mediatek .inc end
         */


        Log.v(LOG_TAG, "onCreate(), exit.");

   }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            mIsHardKeyboardOpen = true;
        } else {
            mIsHardKeyboardOpen = false;
        }

        // Update the Proximity sensor based on keyboard state
        updateProximitySensorMode(mCM.getState());
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Returns the singleton instance of the PhoneApp.
     */
    public static PhoneApp getInstance() {
        return sMe;
    }

    /**
     * Returns the Phone associated with this instance
     */
    static Phone getPhone() {
        return getInstance().phone;
    }

    Ringer getRinger() {
        return ringer;
    }

    BluetoothHandsfree getBluetoothHandsfree() {
        return mBtHandsfree;
    }

    /**
     * Returns an Intent that can be used to go to the "Call log"
     * UI (aka CallLogActivity) in the Contacts app.
     *
     * Watch out: there's no guarantee that the system has any activity to
     * handle this intent.  (In particular there may be no "Call log" at
     * all on on non-voice-capable devices.)
     */
    /* package */ static Intent createCallLogIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW, null);
        intent.setType("vnd.android.cursor.dir/calls");
        return intent;
    }

    /**
     * Return an Intent that can be used to bring up the in-call screen.
     *
     * This intent can only be used from within the Phone app, since the
     * InCallScreen is not exported from our AndroidManifest.
     */
    /* package */ static Intent createInCallIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.setClassName("com.android.phone", getCallScreenClassName());

        // EXTRA_FORCE_SPEAKER_ON is only appended only at tablet 
        // or while the MTK_TB_APP_CALL_FORCE_SPEAKER_ON is true
        // Since there's no ear piece in tablet, speaker should be ON defaultly while call is placed
        if (FeatureOption.MTK_TB_APP_CALL_FORCE_SPEAKER_ON == true)
        {
          intent.putExtra(InCallScreen.EXTRA_FORCE_SPEAKER_ON, true);
        }
        
        return intent;
    }

    /**
     * Variation of createInCallIntent() that also specifies whether the
     * DTMF dialpad should be initially visible when the InCallScreen
     * comes up.
     */
    /* package */ static Intent createInCallIntent(boolean showDialpad) {
        Intent intent = createInCallIntent();
        intent.putExtra(InCallScreen.SHOW_DIALPAD_EXTRA, showDialpad);
        return intent;
    }

    // TODO(InCallScreen redesign): This should be made private once
    // we fix PhoneInterfaceManager.java to *not* manually launch
    // the InCallScreen from its call() method.
    static String getCallScreenClassName() {
        return InCallScreen.class.getName();
    }

    /**
     * Starts the InCallScreen Activity.
     */
    /* package */ void displayCallScreen(final boolean isVoiceOrVTCall) {
        if (VDBG) Log.d(LOG_TAG, "displayCallScreen()...");

        // On non-voice-capable devices we shouldn't ever be trying to
        // bring up the InCallScreen in the first place.
        if (!sVoiceCapable) {
            Log.w(LOG_TAG, "displayCallScreen() not allowed: non-voice-capable device",
                  new Throwable("stack dump"));  // Include a stack trace since this warning
                                                 // indicates a bug in our caller
            return;
        }

        try {
            Intent intent = isVoiceOrVTCall ? createInCallIntent() : createVTInCallIntent();
             startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // It's possible that the in-call UI might not exist (like on
            // non-voice-capable devices), so don't crash if someone
            // accidentally tries to bring it up...
            Log.w(LOG_TAG, "displayCallScreen: transition to InCallScreen failed: " + e);
        }
        Profiler.callScreenRequested();
    }

    boolean isSimPinEnabled() {
        return mIsSimPinEnabled;
    }

    boolean authenticateAgainstCachedSimPin(String pin) {
        return (mCachedSimPin != null && mCachedSimPin.equals(pin));
    }

    void setCachedSimPin(String pin) {
        mCachedSimPin = pin;
    }

    void setInCallScreenInstance(InCallScreen inCallScreen) {
        mInCallScreen = inCallScreen;
    }

    void clearInCallScreenInstance(InCallScreen inCallScreen) {
        if (DBG) Log.d(LOG_TAG, "clearInCallScreenInstance(), inCallScreen = " + inCallScreen);
        // Here we need judge whether mInCallScreen is same as
        // inCallScreen because there may be 2 InCallScreen instance
        // exiting in some case even if InCallScreen activity is single instance.
        // if mInCallScreen != inCallScreen, that means another InCallScreen
        // is active, no need set mInCallScreen as null
        if (mInCallScreen == inCallScreen) {
            if (DBG) Log.d(LOG_TAG, "same InCallScreen instance");
            mInCallScreen = null;
        }
    }

    InCallScreen getInCallScreenInstance() {
    	return mInCallScreen;
    }

    /**
     * @return true if the in-call UI is running as the foreground
     * activity.  (In other words, from the perspective of the
     * InCallScreen activity, return true between onResume() and
     * onPause().)
     *
     * Note this method will return false if the screen is currently off,
     * even if the InCallScreen *was* in the foreground just before the
     * screen turned off.  (This is because the foreground activity is
     * always "paused" while the screen is off.)
     */
    boolean isShowingCallScreen() {
        if (mInCallScreen == null) 
            return false;
        return mInCallScreen.isForegroundActivity();
    }

    boolean isShowingCallScreenForProximity() {
        if (mInCallScreen == null) return false;
        return mInCallScreen.isForegroundActivityForProximity();
    }

    /**
     * Dismisses the in-call UI.
     *
     * This also ensures that you won't be able to get back to the in-call
     * UI via the BACK button (since this call removes the InCallScreen
     * from the activity history.)
     * For OTA Call, it call InCallScreen api to handle OTA Call End scenario
     * to display OTA Call End screen.
     */
    /* package */ void dismissCallScreen() {
        if (mInCallScreen != null) {
            if ((TelephonyCapabilities.supportsOtasp(phone)) &&
                    (mInCallScreen.isOtaCallInActiveState()
                    || mInCallScreen.isOtaCallInEndState()
                    || ((cdmaOtaScreenState != null)
                    && (cdmaOtaScreenState.otaScreenState
                            != CdmaOtaScreenState.OtaScreenState.OTA_STATUS_UNDEFINED)))) {
                // TODO: During OTA Call, display should not become dark to
                // allow user to see OTA UI update. Phone app needs to hold
                // a SCREEN_DIM_WAKE_LOCK wake lock during the entire OTA call.
                wakeUpScreen();
                // If InCallScreen is not in foreground we resume it to show the OTA call end screen
                // Fire off the InCallScreen intent
                displayCallScreen(true);

                mInCallScreen.handleOtaCallEnd();
                return;
            } else {
                mInCallScreen.finish();
            }
        } else {
            //Tells to finish incallscreen when it be resumed with Phone IDLE
            InCallUiState.mLastInCallScreenStatus = InCallUiState.INCALLSCREEN_NOT_EXIT_NOT_INIT;
        }
    }

    /**
     * Handles OTASP-related events from the telephony layer.
     *
     * While an OTASP call is active, the CallNotifier forwards
     * OTASP-related telephony events to this method.
     */
    void handleOtaspEvent(Message msg) {
        if (DBG) Log.d(LOG_TAG, "handleOtaspEvent(message " + msg + ")...");

        if (otaUtils == null) {
            // We shouldn't be getting OTASP events without ever
            // having started the OTASP call in the first place!
            Log.w(LOG_TAG, "handleOtaEvents: got an event but otaUtils is null! "
                  + "message = " + msg);
            return;
        }

        otaUtils.onOtaProvisionStatusChanged((AsyncResult) msg.obj);
    }

    /**
     * Similarly, handle the disconnect event of an OTASP call
     * by forwarding it to the OtaUtils instance.
     */
    /* package */ void handleOtaspDisconnect() {
        if (DBG) Log.d(LOG_TAG, "handleOtaspDisconnect()...");

        if (otaUtils == null) {
            // We shouldn't be getting OTASP events without ever
            // having started the OTASP call in the first place!
            Log.w(LOG_TAG, "handleOtaspDisconnect: otaUtils is null!");
            return;
        }

        otaUtils.onOtaspDisconnect();
    }

    /**
     * Sets the activity responsible for un-PUK-blocking the device
     * so that we may close it when we receive a positive result.
     * mPUKEntryActivity is also used to indicate to the device that
     * we are trying to un-PUK-lock the phone. In other words, iff
     * it is NOT null, then we are trying to unlock and waiting for
     * the SIM to move to READY state.
     *
     * @param activity is the activity to close when PUK has
     * finished unlocking. Can be set to null to indicate the unlock
     * or SIM READYing process is over.
     */
    void setPukEntryActivity(Activity activity) {
        mPUKEntryActivity = activity;
    }

    Activity getPUKEntryActivity() {
        return mPUKEntryActivity;
    }

    /**
     * Sets the dialog responsible for notifying the user of un-PUK-
     * blocking - SIM READYing progress, so that we may dismiss it
     * when we receive a positive result.
     *
     * @param dialog indicates the progress dialog informing the user
     * of the state of the device.  Dismissed upon completion of
     * READYing process
     */
    void setPukEntryProgressDialog(ProgressDialog dialog) {
        mPUKEntryProgressDialog = dialog;
    }

    ProgressDialog getPUKEntryProgressDialog() {
        return mPUKEntryProgressDialog;
    }

    /**
     * Controls how quickly the screen times out.
     *
     * The poke lock controls how long it takes before the screen powers
     * down, and therefore has no immediate effect when the current
     * WakeState (see {@link PhoneApp#requestWakeState}) is FULL.
     * If we're in a state where the screen *is* allowed to turn off,
     * though, the poke lock will determine the timeout interval (long or
     * short).
     *
     * @param shortPokeLock tells the device the timeout duration to use
     * before going to sleep
     * {@link com.android.server.PowerManagerService#SHORT_KEYLIGHT_DELAY}.
     */
    /* package */ void setScreenTimeout(ScreenTimeoutDuration duration) {
        if (VDBG) Log.d(LOG_TAG, "setScreenTimeout(" + duration + ")...");

        // make sure we don't set the poke lock repeatedly so that we
        // avoid triggering the userActivity calls in
        // PowerManagerService.setPokeLock().
        if (duration == mScreenTimeoutDuration) {
            return;
        }
        // stick with default timeout if we are using the proximity sensor
        if (proximitySensorModeEnabled()) {
            return;
        }
        mScreenTimeoutDuration = duration;
        updatePokeLock();
    }

    /**
     * Update the state of the poke lock held by the phone app,
     * based on the current desired screen timeout and the
     * current "ignore user activity on touch" flag.
     */
    private void updatePokeLock() {
        // This is kind of convoluted, but the basic thing to remember is
        // that the poke lock just sends a message to the screen to tell
        // it to stay on for a while.
        // The default is 0, for a long timeout and should be set that way
        // when we are heading back into a the keyguard / screen off
        // state, and also when we're trying to keep the screen alive
        // while ringing.  We'll also want to ignore the cheek events
        // regardless of the timeout duration.
        // The short timeout is really used whenever we want to give up
        // the screen lock, such as when we're in call.
        int pokeLockSetting = 0;
        switch (mScreenTimeoutDuration) {
            case SHORT:
                // Set the poke lock to timeout the display after a short
                // timeout (5s). This ensures that the screen goes to sleep
                // as soon as acceptably possible after we the wake lock
                // has been released.
                pokeLockSetting |= LocalPowerManager.POKE_LOCK_SHORT_TIMEOUT;
                break;

            case MEDIUM:
                // Set the poke lock to timeout the display after a medium
                // timeout (15s). This ensures that the screen goes to sleep
                // as soon as acceptably possible after we the wake lock
                // has been released.
                pokeLockSetting |= LocalPowerManager.POKE_LOCK_MEDIUM_TIMEOUT;
                break;

            case DEFAULT:
            default:
                // set the poke lock to timeout the display after a long
                // delay by default.
                // TODO: it may be nice to be able to disable cheek presses
                // for long poke locks (emergency dialer, for instance).
                break;
        }

        if (mIgnoreTouchUserActivity) {
            pokeLockSetting |= LocalPowerManager.POKE_LOCK_IGNORE_TOUCH_EVENTS;
        }

        // Send the request
        try {
            mPowerManagerService.setPokeLock(pokeLockSetting, mPokeLockToken, LOG_TAG);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "mPowerManagerService.setPokeLock() failed: " + e);
        }
    }

    /**
     * Controls whether or not the screen is allowed to sleep.
     *
     * Once sleep is allowed (WakeState is SLEEP), it will rely on the
     * settings for the poke lock to determine when to timeout and let
     * the device sleep {@link PhoneApp#setScreenTimeout}.
     *
     * @param ws tells the device to how to wake.
     */
    /* package */ void requestWakeState(WakeState ws) {
        if (VDBG) Log.d(LOG_TAG, "requestWakeState(" + ws + ")...");
        synchronized (this) {
            if (mWakeState != ws 
                    || (DualTalkUtils.isSupportDualTalk && (ws == WakeState.SLEEP))) {
                switch (ws) {
                    case PARTIAL:
                        // acquire the processor wake lock, and release the FULL
                        // lock if it is being held.
                        mPartialWakeLock.acquire();
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        break;
                    case FULL:
                        // acquire the full wake lock, and release the PARTIAL
                        // lock if it is being held.
                        mWakeLock.acquire();
                        if (mPartialWakeLock.isHeld()) {
                            mPartialWakeLock.release();
                        }
                        break;
                    case SLEEP:
                    default:
                        // release both the PARTIAL and FULL locks.
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        
                        //During CDMDA in call, if the CPU sleep will cause the
                        //sound is too low, so make sure if cdma in call, prevent
                        //CPU to sleep
                        if(DualTalkUtils.isSupportDualTalk 
                                && DualTalkUtils.getInstance().isCDMAPhoneActive()) {
                            if (!mPartialWakeLock.isHeld()) {
                                if (DBG) {
                                    Log.d(LOG_TAG, "CDMA non-IDLE, make sure we have the PARTIAL lock!!");
                                }
                                mPartialWakeLock.acquire();
                            }
                        } else if (mPartialWakeLock.isHeld()) {
                            if (DBG) {
                                Log.d(LOG_TAG, "release PARTIAL lock!!");
                            }
                            mPartialWakeLock.release();
                        }
                        
                        break;
                }
                mWakeState = ws;
            }
        }
    }

    /**
     * If we are not currently keeping the screen on, then poke the power
     * manager to wake up the screen for the user activity timeout duration.
     */
    /* package */ void wakeUpScreen() {
        synchronized (this) {
            if (mWakeState == WakeState.SLEEP) {
                if (DBG) Log.d(LOG_TAG, "pulse screen lock");
                try {
                    mPowerManagerService.userActivityWithForce(SystemClock.uptimeMillis(), false, true);
                } catch (RemoteException ex) {
                    // Ignore -- the system process is dead.
                }
            }
        }
    }

    void wakeUpScreenForDisconnect(int holdMs) {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        synchronized (this) {
            if (mWakeState == WakeState.SLEEP && !pm.isScreenOn()) { 
                if (DBG) Log.d(LOG_TAG, "wakeUpScreenForDisconnect(" + holdMs + ")");
                mWakeLockForDisconnect.acquire();
                mHandler.removeMessages(EVENT_TIMEOUT);
                mWakelockSequence++;
                Message msg = mHandler.obtainMessage(EVENT_TIMEOUT, mWakelockSequence, 0);
                mHandler.sendMessageDelayed(msg, holdMs);
            }
        }
    }
    
    void handleTimeout(int seq){
        synchronized (this) {
            if (DBG) Log.d(LOG_TAG, "handleTimeout");
            if (seq == mWakelockSequence) {
                mWakeLockForDisconnect.release();
            }
        }
    }
    /**
     * Sets the wake state and screen timeout based on the current state
     * of the phone, and the current state of the in-call UI.
     *
     * This method is a "UI Policy" wrapper around
     * {@link PhoneApp#requestWakeState} and {@link PhoneApp#setScreenTimeout}.
     *
     * It's safe to call this method regardless of the state of the Phone
     * (e.g. whether or not it's idle), and regardless of the state of the
     * Phone UI (e.g. whether or not the InCallScreen is active.)
     */
    /* package */ void updateWakeState() {
        Phone.State state = mCM.getState();

        // True if the in-call UI is the foreground activity.
        // (Note this will be false if the screen is currently off,
        // since in that case *no* activity is in the foreground.)
        boolean isShowingCallScreen = isShowingCallScreen();

        // True if the InCallScreen's DTMF dialer is currently opened.
        // (Note this does NOT imply whether or not the InCallScreen
        // itself is visible.)
        boolean isDialerOpened = (mInCallScreen != null) && mInCallScreen.isDialerOpened();

        // True if the speakerphone is in use.  (If so, we *always* use
        // the default timeout.  Since the user is obviously not holding
        // the phone up to his/her face, we don't need to worry about
        // false touches, and thus don't need to turn the screen off so
        // aggressively.)
        // Note that we need to make a fresh call to this method any
        // time the speaker state changes.  (That happens in
        // PhoneUtils.turnOnSpeaker().)
        boolean isSpeakerInUse = (state == Phone.State.OFFHOOK) && PhoneUtils.isSpeakerOn(this);

        // TODO (bug 1440854): The screen timeout *might* also need to
        // depend on the bluetooth state, but this isn't as clear-cut as
        // the speaker state (since while using BT it's common for the
        // user to put the phone straight into a pocket, in which case the
        // timeout should probably still be short.)

        if (DBG) Log.d(LOG_TAG, "updateWakeState: callscreen " + isShowingCallScreen
                       + ", dialer " + isDialerOpened
                       + ", speaker " + isSpeakerInUse + "...");

        //
        // (1) Set the screen timeout.
        //
        // Note that the "screen timeout" value we determine here is
        // meaningless if the screen is forced on (see (2) below.)
        //

        // Historical note: In froyo and earlier, we checked here for a special
        // case: the in-call UI being active, the speaker off, and the DTMF dialpad
        // not visible.  In that case, with no touchable UI onscreen at all (for
        // non-prox-sensor devices at least), we could assume the user was probably
        // holding the phone up to their face and *not* actually looking at the
        // screen.  So we'd switch to a special screen timeout value
        // (ScreenTimeoutDuration.MEDIUM), purely to save battery life.
        //
        // On current devices, we can rely on the proximity sensor to turn the
        // screen off in this case, so we use the system-wide default timeout
        // unconditionally.
        setScreenTimeout(ScreenTimeoutDuration.DEFAULT);

        //
        // (2) Decide whether to force the screen on or not.
        //
        // Force the screen to be on if the phone is ringing or dialing,
        // or if we're displaying the "Call ended" UI for a connection in
        // the "disconnected" state.
        //
        boolean isRinging = (state == Phone.State.RINGING);
        boolean isDialing;
        if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
            isDialing = (((GeminiPhone)phone).getForegroundCall().getState() == Call.State.DIALING);
        } else {
            isDialing = (phone.getForegroundCall().getState() == Call.State.DIALING);
        }
        boolean showingDisconnectedConnection =
                PhoneUtils.hasDisconnectedConnections(mCM) && isShowingCallScreen;
        boolean keepScreenOn = isRinging || isDialing || showingDisconnectedConnection;
        if (DBG) Log.d(LOG_TAG, "updateWakeState: keepScreenOn = " + keepScreenOn
                       + " (isRinging " + isRinging
                       + ", isDialing " + isDialing
                       + ", showingDisc " + showingDisconnectedConnection + ")");
        // keepScreenOn == true means we'll hold a full wake lock:
        requestWakeState(keepScreenOn ? WakeState.FULL : WakeState.SLEEP);
    }

    /**
     * Wrapper around the PowerManagerService.preventScreenOn() API.
     * This allows the in-call UI to prevent the screen from turning on
     * even if a subsequent call to updateWakeState() causes us to acquire
     * a full wake lock.
     */
    /* package */ void preventScreenOn(boolean prevent) {
        if (VDBG) Log.d(LOG_TAG, "- preventScreenOn(" + prevent + ")...");
        try {
            mPowerManagerService.preventScreenOn(prevent);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "mPowerManagerService.preventScreenOn() failed: " + e);
        }
    }

    /**
     * Sets or clears the flag that tells the PowerManager that touch
     * (and cheek) events should NOT be considered "user activity".
     *
     * Since the in-call UI is totally insensitive to touch in most
     * states, we set this flag whenever the InCallScreen is in the
     * foreground.  (Otherwise, repeated unintentional touches could
     * prevent the device from going to sleep.)
     *
     * There *are* some some touch events that really do count as user
     * activity, though.  For those, we need to manually poke the
     * PowerManager's userActivity method; see pokeUserActivity().
     */
    /* package */ void setIgnoreTouchUserActivity(boolean ignore) {
        if (VDBG) Log.d(LOG_TAG, "setIgnoreTouchUserActivity(" + ignore + ")...");
        mIgnoreTouchUserActivity = ignore;
        updatePokeLock();
    }

    /**
     * Manually pokes the PowerManager's userActivity method.  Since we
     * hold the POKE_LOCK_IGNORE_TOUCH_EVENTS poke lock while
     * the InCallScreen is active, we need to do this for touch events
     * that really do count as user activity (like pressing any
     * onscreen UI elements.)
     */
    /* package */ void pokeUserActivity() {
        if (VDBG) Log.d(LOG_TAG, "pokeUserActivity()...");
        try {
            mPowerManagerService.userActivity(SystemClock.uptimeMillis(), false);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "mPowerManagerService.userActivity() failed: " + e);
        }
    }

    /**
     * Set when a new outgoing call is beginning, so we can update
     * the proximity sensor state.
     * Cleared when the InCallScreen is no longer in the foreground,
     * in case the call fails without changing the telephony state.
     */
    /* package */ void setBeginningCall(boolean beginning) {
        // Note that we are beginning a new call, for proximity sensor support
        mBeginningCall = beginning;
        // Update the Proximity sensor based on mBeginningCall state
        updateProximitySensorMode(mCM.getState());
    }

    /**
     * Updates the wake lock used to control proximity sensor behavior,
     * based on the current state of the phone.  This method is called
     * from the CallNotifier on any phone state change.
     *
     * On devices that have a proximity sensor, to avoid false touches
     * during a call, we hold a PROXIMITY_SCREEN_OFF_WAKE_LOCK wake lock
     * whenever the phone is off hook.  (When held, that wake lock causes
     * the screen to turn off automatically when the sensor detects an
     * object close to the screen.)
     *
     * This method is a no-op for devices that don't have a proximity
     * sensor.
     *
     * Note this method doesn't care if the InCallScreen is the foreground
     * activity or not.  That's because we want the proximity sensor to be
     * enabled any time the phone is in use, to avoid false cheek events
     * for whatever app you happen to be running.
     *
     * Proximity wake lock will *not* be held if any one of the
     * conditions is true while on a call:
     * 1) If the audio is routed via Bluetooth
     * 2) If a wired headset is connected
     * 3) if the speaker is ON
     * 4) If the slider is open(i.e. the hardkeyboard is *not* hidden)
     *
     * @param state current state of the phone (see {@link Phone#State})
     */
    /* package */ void updateProximitySensorMode(Phone.State state) {
    
        boolean isRingingWhenActive = false;//MTK81281 add isRingingWhenActive for Cr:ALPS00117091
		
        if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: state = " + state);
        
        if (proximitySensorModeEnabled()) {
            synchronized (mProximityWakeLock) {
                // turn proximity sensor off and turn screen on immediately if
                // we are using a headset, the keyboard is open, or the device
                // is being held in a horizontal position.
                boolean screenOnImmediately = (isHeadsetPlugged()
                            || PhoneUtils.isSpeakerOn(this)
                            || ((mBtHandsfree != null) && mBtHandsfree.isAudioOn())
                            || mIsHardKeyboardOpen);

		if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
		    screenOnImmediately = screenOnImmediately || ((!isVTIdle()) && (!isVTRinging()));
		}

                // We do not keep the screen off when the user is outside in-call screen and we are
                // horizontal, but we do not force it on when we become horizontal until the
                // proximity sensor goes negative.
                
                // Currently not support horizontal screen in Phone
                // So just comment google code below
                // boolean horizontal =
                //        (mOrientation == AccelerometerListener.ORIENTATION_HORIZONTAL);
                // screenOnImmediately |= !isShowingCallScreenForProximity() && horizontal;
                if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: mBeginningCall = " + mBeginningCall);
                if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: screenOnImmediately = " + screenOnImmediately);
	       //MTK81281 add isRingingWhenActive for Cr:ALPS00117091 start	
	       //when a call is activeand p-sensor turn off the screen,  
	       //another call or vtcall in we don't release the lock and acquire again(the prowermanagerservice will turn on and off the screen and it's a problem)
	       //instead ,we don't release the lock(prowermanagerservice will not turn on and off the screen)
                isRingingWhenActive =(state == Phone.State.RINGING)
					&& (mCM.getActiveFgCallState() == Call.State.ACTIVE)
					&&(mCM.getFirstActiveRingingCall().getState() == Call.State.WAITING);
		   
                if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: isRingingWhenActive = " + isRingingWhenActive);
	       //MTK81281 add  isRingingWhenActive for Cr:ALPS00117091 end
	       
                if (((state == Phone.State.OFFHOOK) || mBeginningCall || isRingingWhenActive) && //MTK81281 add isRingingWhenActive for Cr:ALPS00117091
                        !screenOnImmediately ) {
                    // Phone is in use!  Arrange for the screen to turn off
                    // automatically when the sensor detects a close object.
                    if (!mProximityWakeLock.isHeld()) {
                        if (DBG) Log.d(LOG_TAG, "updateProximitySensorMode: acquiring...");
                        mProximityWakeLock.acquire();
                    } else {
                        if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: lock already held.");
                    }
                } else {
                    // Phone is either idle, or ringing.  We don't want any
                    // special proximity sensor behavior in either case.
                    if (mProximityWakeLock.isHeld()) {
                        if (DBG) Log.d(LOG_TAG, "updateProximitySensorMode: releasing...");
                        // Wait until user has moved the phone away from his head if we are
                        // releasing due to the phone call ending.
                        // Qtherwise, turn screen on immediately
                        int flags =
                            (screenOnImmediately ? 0 : PowerManager.WAIT_FOR_PROXIMITY_NEGATIVE);
                        mProximityWakeLock.release(flags);
                    } else {
                        if (VDBG) {
                            Log.d(LOG_TAG, "updateProximitySensorMode: lock already released.");
                        }
                    }
                }
            }
        }
    }

    public void orientationChanged(int orientation) {
        mOrientation = orientation;
        updateProximitySensorMode(mCM.getState());
    }

    /**
     * Notifies the phone app when the phone state changes.
     * Currently used only for proximity sensor support.
     */
    /* package */ void updatePhoneState(Phone.State state) {
        if (state != mLastPhoneState) {
            mLastPhoneState = state;
            if (state == Phone.State.IDLE)
                PhoneApp.getInstance().pokeUserActivity();
            updateProximitySensorMode(state);
            if (mAccelerometerListener != null) {
                // use accelerometer to augment proximity sensor when in call
                mOrientation = AccelerometerListener.ORIENTATION_UNKNOWN;
                mAccelerometerListener.enable(state == Phone.State.OFFHOOK);
            }
            // clear our beginning call flag
            mBeginningCall = false;
            // While we are in call, the in-call screen should dismiss the keyguard.
            // This allows the user to press Home to go directly home without going through
            // an insecure lock screen.
            // But we do not want to do this if there is no active call so we do not
            // bypass the keyguard if the call is not answered or declined.
            if (mInCallScreen != null) {
		if (VDBG) Log.d(LOG_TAG, "updatePhoneState: state = " + state);
		if (!PhoneUtils.isDMLocked())
                    mInCallScreen.updateKeyguardPolicy(state == Phone.State.OFFHOOK);
            }
        }
        if (mInCallScreen != null) {
	        mInCallScreen.updateActivityHiberarchy(state != Phone.State.IDLE);
        }
    }

    /* package */ Phone.State getPhoneState() {
        return mLastPhoneState;
    }

    /**
     * @return true if this device supports the "proximity sensor
     * auto-lock" feature while in-call (see updateProximitySensorMode()).
     */
    /* package */ boolean proximitySensorModeEnabled() {
        return (mProximityWakeLock != null);
    }

    KeyguardManager getKeyguardManager() {
        return mKeyguardManager;
    }

    private void onMMIComplete(AsyncResult r) {
        if (VDBG) Log.d(LOG_TAG, "onMMIComplete()...");
        MmiCode mmiCode = (MmiCode) r.result;
        MmiCode.State state = mmiCode.getState();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (state != MmiCode.State.PENDING) {
	        Intent intent = new Intent();
	     	intent.setAction("com.android.phone.mmi");
	     	sendBroadcast(intent);
            }
        }
        PhoneUtils.displayMMIComplete(phone, getInstance(), mmiCode, null, null);
    }

    private void onMMIComplete2(AsyncResult r) {
        if (VDBG) Log.d(LOG_TAG, "onMMIComplete2()...");
        MmiCode mmiCode = (MmiCode) r.result;
        MmiCode.State state = mmiCode.getState();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (state != MmiCode.State.PENDING) {
	        Intent intent = new Intent();
	     	intent.setAction("com.android.phone.mmi");
	     	sendBroadcast(intent);
            }
        }
        PhoneUtils.displayMMICompleteExt(phone, getInstance(), mmiCode, null, null, Phone.GEMINI_SIM_2);
    }

    private void initForNewRadioTechnology() {
        if (DBG) Log.d(LOG_TAG, "initForNewRadioTechnology...");

         if (phone.getPhoneType() == Phone.PHONE_TYPE_CDMA) {
            // Create an instance of CdmaPhoneCallState and initialize it to IDLE
            cdmaPhoneCallState = new CdmaPhoneCallState();
            cdmaPhoneCallState.CdmaPhoneCallStateInit();
        }
        if (TelephonyCapabilities.supportsOtasp(phone)) {
            //create instances of CDMA OTA data classes
            if (cdmaOtaProvisionData == null) {
                cdmaOtaProvisionData = new OtaUtils.CdmaOtaProvisionData();
            }
            if (cdmaOtaConfigData == null) {
                cdmaOtaConfigData = new OtaUtils.CdmaOtaConfigData();
            }
            if (cdmaOtaScreenState == null) {
                cdmaOtaScreenState = new OtaUtils.CdmaOtaScreenState();
            }
            if (cdmaOtaInCallScreenUiState == null) {
                cdmaOtaInCallScreenUiState = new OtaUtils.CdmaOtaInCallScreenUiState();
            }
        } else {
            //Clean up OTA data in GSM/UMTS. It is valid only for CDMA
            clearOtaState();
        }

        ringer.updateRingerContextAfterRadioTechnologyChange(this.phone);
        notifier.updateCallNotifierRegistrationsAfterRadioTechnologyChange();
        if (mBtHandsfree != null) {
            mBtHandsfree.updateBtHandsfreeAfterRadioTechnologyChange();
        }
        if (mInCallScreen != null) {
            //mInCallScreen.updateAfterRadioTechnologyChange();
            mInCallScreen.endInCallScreenSession(true);
        }

        /// M: For solving [ALPS00362556][International card][G+G] both SIM cards are locked, only has unlock screen of SIM2 when boot. @{
        //  Currently, we used intent(sent from IccCard) to receive network locked notification, so remove the following registration.
        // Update registration for ICC status after radio technology change       
        /*       
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
            IccCard sim1Gemini = mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_1);
            IccCard sim2Gemini = mGeminiPhone.getIccCardGemini(Phone.GEMINI_SIM_2);
            sim1Gemini.registerForNetworkLocked(mHandler, EVENT_SIM1_NETWORK_LOCKED, null);
            sim2Gemini.registerForNetworkLocked(mHandler, EVENT_SIM2_NETWORK_LOCKED, null);
        } else {
            IccCard sim = phone.getIccCard();
            if (sim != null) {
                if (VDBG) Log.v(LOG_TAG, "register for ICC status");
                sim.registerForNetworkLocked(mHandler, EVENT_SIM_NETWORK_LOCKED, null);
            }
        }
        */
        /// @}
    }


    /**
     * @return true if a wired headset is currently plugged in.
     *
     * @see Intent.ACTION_HEADSET_PLUG (which we listen for in mReceiver.onReceive())
     */
    boolean isHeadsetPlugged() {
        return mIsHeadsetPlugged;
    }

    /**
     * @return true if the onscreen UI should currently be showing the
     * special "bluetooth is active" indication in a couple of places (in
     * which UI elements turn blue and/or show the bluetooth logo.)
     *
     * This depends on the BluetoothHeadset state *and* the current
     * telephony state; see shouldShowBluetoothIndication().
     *
     * @see CallCard
     * @see NotificationMgr.updateInCallNotification
     */
    /* package */ boolean showBluetoothIndication() {
        return mShowBluetoothIndication;
    }

    /**
     * Recomputes the mShowBluetoothIndication flag based on the current
     * bluetooth state and current telephony state.
     *
     * This needs to be called any time the bluetooth headset state or the
     * telephony state changes.
     *
     * @param forceUiUpdate if true, force the UI elements that care
     *                      about this flag to update themselves.
     */
    /* package */ void updateBluetoothIndication(boolean forceUiUpdate) {
        mShowBluetoothIndication = shouldShowBluetoothIndication(mBluetoothHeadsetState,
                                                                 mBluetoothHeadsetAudioState,
                                                                 mCM);
        if (forceUiUpdate) {
            // Post Handler messages to the various components that might
            // need to be refreshed based on the new state.
            if (isShowingCallScreen()) mInCallScreen.requestUpdateBluetoothIndication();
            if (DBG) Log.d (LOG_TAG, "- updating in-call notification for BT state change...");
            mHandler.sendEmptyMessage(EVENT_UPDATE_INCALL_NOTIFICATION);
        }

        // Update the Proximity sensor based on Bluetooth audio state
        updateProximitySensorMode(mCM.getState());
    }

    /**
     * UI policy helper function for the couple of places in the UI that
     * have some way of indicating that "bluetooth is in use."
     *
     * @return true if the onscreen UI should indicate that "bluetooth is in use",
     *         based on the specified bluetooth headset state, and the
     *         current state of the phone.
     * @see showBluetoothIndication()
     */
    private static boolean shouldShowBluetoothIndication(int bluetoothState,
                                                         int bluetoothAudioState,
                                                         CallManager cm) {
        // We want the UI to indicate that "bluetooth is in use" in two
        // slightly different cases:
        //
        // (a) The obvious case: if a bluetooth headset is currently in
        //     use for an ongoing call.
        //
        // (b) The not-so-obvious case: if an incoming call is ringing,
        //     and we expect that audio *will* be routed to a bluetooth
        //     headset once the call is answered.

        switch (cm.getState()) {
            case OFFHOOK:
                // This covers normal active calls, and also the case if
                // the foreground call is DIALING or ALERTING.  In this
                // case, bluetooth is considered "active" if a headset
                // is connected *and* audio is being routed to it.
                return ((bluetoothState == BluetoothHeadset.STATE_CONNECTED)
                        && (bluetoothAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTED));

            case RINGING:
                // If an incoming call is ringing, we're *not* yet routing
                // audio to the headset (since there's no in-call audio
                // yet!)  In this case, if a bluetooth headset is
                // connected at all, we assume that it'll become active
                // once the user answers the phone.
                return (bluetoothState == BluetoothHeadset.STATE_CONNECTED);

            default:  // Presumably IDLE
                return false;
        }
    }

    public class OutgoingCallPhoneAppReceiver extends BroadcastReceiver {
        private static final String TAG = "OutgoingCallPhoneAppReceiver";

        public void onReceive(Context context, Intent intent) {
            if (DBG) Log.v(TAG, "onReceive: " + intent);
            String resultdata = getResultData();
            if (resultdata == null) {
                Log.v(TAG, "CALL cancelled (null number), returning...");
                return;
            }
            if (null != intent && intent.getBooleanExtra("launch_from_dialer", false)) {
                final String number = intent.getStringExtra("number");
                if (DBG) Log.v(TAG, "onReceive: launch_from_dialer");
                if (null != intent && intent.getBooleanExtra("is_sip_call", false)) {
                    if (DBG) Log.v(TAG, "onReceive: is_sip_call: true");
                    intent.setData(Uri.fromParts(Constants.SCHEME_SIP, number, null));
                    intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.SipCallHandlerEx"));
                } else {
                    if (DBG) Log.v(TAG, "onReceive: is_sip_call: false");
                    intent.setData(Uri.fromParts(Constants.SCHEME_TEL, number, null));
                    intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.InCallScreen"));
                }
                intent.setAction(Intent.ACTION_CALL_PRIVILEGED);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                context.startActivity(intent);
            }
        }
    }

    /**
     * Receiver for misc intent broadcasts the Phone app cares about.
     */
    private class PhoneAppBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (VDBG) Log.d(LOG_TAG, "PhoneAppBroadcastReceiver -----action=" + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            boolean enabled = intent.getBooleanExtra("state", false);
                if (VDBG) Log.d(LOG_TAG, "PhoneAppBroadcastReceiver ------enabled=" + enabled);
                if (enabled == true) {
                    PhoneUtils.DismissMMIDialog();
                }
                if (FeatureOption.MTK_GEMINI_SUPPORT != true)
                    phone.setRadioPower(!enabled);
                else {
                    if (!enabled) {
                        int dualSimModeSetting = System.getInt(getContentResolver(),
                                System.DUAL_SIM_MODE_SETTING, GeminiNetworkSubUtil.MODE_DUAL_SIM);
                        ((GeminiPhone)phone).setRadioMode(dualSimModeSetting);
                    } else {
                        ((GeminiPhone)phone).setRadioMode(GeminiNetworkSubUtil.MODE_FLIGHT_MODE);
                    }
                }
            } else if (action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)) {
                int mode = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, GeminiNetworkSubUtil.MODE_DUAL_SIM);
                ((GeminiPhone)phone).setRadioMode(mode);
            } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                if (FeatureOption.MTK_GEMINI_SUPPORT != true) {
                    phone.refreshSpnDisplay();
                } else {
                    ((GeminiPhone)phone).refreshSpnDisplay();
                }
            } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                mBluetoothHeadsetState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                                                          BluetoothHeadset.STATE_DISCONNECTED);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: HEADSET_STATE_CHANGED_ACTION");
                if (VDBG) Log.d(LOG_TAG, "==> new state: " + mBluetoothHeadsetState);
                updateBluetoothIndication(true);  // Also update any visible UI if necessary
            } else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
                mBluetoothHeadsetAudioState =
                        intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                                           BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: HEADSET_AUDIO_STATE_CHANGED_ACTION");
                if (VDBG) Log.d(LOG_TAG, "==> new state: " + mBluetoothHeadsetAudioState);
                updateBluetoothIndication(true);  // Also update any visible UI if necessary
            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                String state = intent.getStringExtra(Phone.STATE_KEY);
                String reason = intent.getStringExtra(Phone.STATE_CHANGE_REASON_KEY);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: ACTION_ANY_DATA_CONNECTION_STATE_CHANGED state:" + state
                     + " reason:" + reason);
    
                // The "data disconnected due to roaming" notification is shown
                // if (a) you have the "data roaming" feature turned off, and
                // (b) you just lost data connectivity because you're roaming.
                boolean disconnectedDueToRoaming = "DISCONNECTED".equals(state) &&
                    Phone.REASON_ROAMING_ON.equals(reason) && !phone.getDataRoamingEnabled();
                //since getDataRoamingEnabled will access database, put it at last.
    
                mHandler.sendEmptyMessage(disconnectedDueToRoaming
                                          ? EVENT_DATA_ROAMING_DISCONNECTED
                                          : EVENT_DATA_ROAMING_OK);
            } else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                if (VDBG) Log.d(LOG_TAG, "mReceiver: ACTION_HEADSET_PLUG");
                if (VDBG) Log.d(LOG_TAG, "    state: " + intent.getIntExtra("state", 0));
                if (VDBG) Log.d(LOG_TAG, "    name: " + intent.getStringExtra("name"));
                mIsHeadsetPlugged = (intent.getIntExtra("state", 0) == 1);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_WIRED_HEADSET_PLUG, 0));
            } else if (action.equals(Intent.ACTION_BATTERY_LOW)) {
                if (VDBG) Log.d(LOG_TAG, "mReceiver: ACTION_BATTERY_LOW");
                notifier.sendBatteryLow();  // Play a warning tone if in-call
            } else if ((action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED))) {
                // if an attempt to un-PUK-lock the device was made, while we're
                // receiving this state change notification, notify the handler.
                // NOTE: This is ONLY triggered if an attempt to un-PUK-lock has
                // been attempted.
                // !!! Need to check below message name, both MTK and android 4.0 modified it,
                // below is MTK version
                int unlockSIMID = intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY,-1);
                String unlockSIMStatus = intent.getStringExtra(IccCard.INTENT_KEY_ICC_STATE);
                Log.d(LOG_TAG, "[unlock SIM card NO switched. Now] " + unlockSIMID + " is active.");
                Log.d(LOG_TAG, "[unlockSIMStatus] : "  + unlockSIMStatus);
                if ((unlockSIMID == Phone.GEMINI_SIM_1) && ((IccCard.INTENT_VALUE_LOCKED_NETWORK).equals(unlockSIMStatus)) ){
                    Log.d(LOG_TAG, "[unlockSIMID :Phone.GEMINI_SIM_1]");

                    arySIMLockStatus[0] = 2;//need to deal with SIM1 SIM Lock
                    if ((arySIMLockStatus[1] != 1) && (arySIMLockStatus[1] != 4)){
                        arySIMLockStatus[0] = 1;
                        mHandler.sendMessage(mHandler.obtainMessage(EVENT_SIM1_NETWORK_LOCKED,
                        intent.getStringExtra(IccCard.INTENT_KEY_ICC_STATE)));
                    }
                    Log.d(LOG_TAG,"[SIM1][changed][arySIMLockStatus]: ["+ PhoneApp.arySIMLockStatus[0] + " , " + PhoneApp.arySIMLockStatus[1] + " ]");
                }else if((unlockSIMID == Phone.GEMINI_SIM_2) && ((IccCard.INTENT_VALUE_LOCKED_NETWORK).equals(unlockSIMStatus))){
                    Log.d(LOG_TAG, "[unlockSIMID :Phone.GEMINI_SIM_2]");            		
                    arySIMLockStatus[1] = 2;//need to deal with SIM2 SIM Lock
                    if ((arySIMLockStatus[0] != 1) && (arySIMLockStatus[0] != 4)){
                        arySIMLockStatus[1] = 1;
                        mHandler.sendMessage(mHandler.obtainMessage(EVENT_SIM2_NETWORK_LOCKED,
                                intent.getStringExtra(IccCard.INTENT_KEY_ICC_STATE)));
                    }
                    Log.d(LOG_TAG,"[SIM2][changed][arySIMLockStatus]: ["+ PhoneApp.arySIMLockStatus[0] + " , " + PhoneApp.arySIMLockStatus[1] + " ]");
                }else if(unlockSIMStatus.equals(IccCard.INTENT_VALUE_ICC_READY)){
                    int delaySendMessage = 2000;
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_SIM_STATE_CHANGED,IccCard.INTENT_VALUE_ICC_READY), delaySendMessage);
                }else{
                    Log.d(LOG_TAG, "[unlockSIMID : Other information]");
                }
            } else if (action.equals(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED)) {
                String newPhone = intent.getStringExtra(Phone.PHONE_NAME_KEY);
                Log.d(LOG_TAG, "Radio technology switched. Now " + newPhone + " is active.");
                if ("CDMA".equals(newPhone)) {
                    DualTalkUtils.switchDTFeatureOption(true);
                } else if ("GSM".equals(newPhone)) {
                    DualTalkUtils.switchDTFeatureOption(false);
                }
                initForNewRadioTechnology();
            } else if (action.equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
                handleServiceStateChanged(intent);
            } else if (action.equals(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED)) {
                if (TelephonyCapabilities.supportsEcm(phone)) {
                    Log.d(LOG_TAG, "Emergency Callback Mode arrived in PhoneApp.");
                    // Start Emergency Callback Mode service
                    if (intent.getBooleanExtra("phoneinECMState", false)) {
                        context.startService(new Intent(context,
                                EmergencyCallbackModeService.class));
                    }
                } else {
                    // It doesn't make sense to get ACTION_EMERGENCY_CALLBACK_MODE_CHANGED
                    // on a device that doesn't support ECM in the first place.
                    Log.e(LOG_TAG, "Got ACTION_EMERGENCY_CALLBACK_MODE_CHANGED, "
                          + "but ECM isn't supported for phone: " + phone.getPhoneName());
                }
            } else if (action.equals(Intent.ACTION_DOCK_EVENT)) {
                mDockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                        Intent.EXTRA_DOCK_STATE_UNDOCKED);
                if (VDBG) Log.d(LOG_TAG, "ACTION_DOCK_EVENT -> mDockState = " + mDockState);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_DOCK_STATE_CHANGED, 0));
            } else if (action.equals(TtyIntent.TTY_PREFERRED_MODE_CHANGE_ACTION)) {
                mPreferredTtyMode = intent.getIntExtra(TtyIntent.TTY_PREFFERED_MODE,
                                                       Phone.TTY_MODE_OFF);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: TTY_PREFERRED_MODE_CHANGE_ACTION");
                if (VDBG) Log.d(LOG_TAG, "    mode: " + mPreferredTtyMode);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_TTY_PREFERRED_MODE_CHANGED, 0));
            } else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                int ringerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE,
                        AudioManager.RINGER_MODE_NORMAL);
                if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                    notifier.silenceRinger();
                }
            } else if (action.equals(Intent.ACTION_SHUTDOWN)) {
                Log.d(LOG_TAG, "ACTION_SHUTDOWN received");
                // MTK_OP02_PROTECT_START
                if("OP02".equals(PhoneUtils.getOptrProperties())){
                    addCallSync();
                }
                // MTK_OP02_PROTECT_END
                notifier.unregisterCallNotifierRegistrations();
            } else if (action.equals(STKCALL_REGISTER_SPEECH_INFO)) {
                PhoneUtils.placeCallRegister(phone);
                mHandler.sendEmptyMessageDelayed(EVENT_SHOW_INCALL_SCREEN_FOR_STK_SETUP_CALL, DELAY_SHOW_INCALL_SCREEN_FOR_STK_SETUP_CALL);
            } else if (action.equals(MISSEDCALL_DELETE_INTENT)){
                Log.d(LOG_TAG, "MISSEDCALL_DELETE_INTENT");
                notificationMgr.resetMissedCallNumber();
            }else if (action.equals(NETWORK_MODE_CHANGE)) {
            	int modemNetworkMode = intent.getIntExtra(NETWORK_MODE_CHANGE, 0);
            	int simId = intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, 0);
            	int oldmode = intent.getIntExtra(OLD_NETWORK_MODE, -1);
            	if (FeatureOption.MTK_GEMINI_SUPPORT)
            	{
            		GeminiPhone dualPhone = (GeminiPhone)phone;
            		dualPhone.setPreferredNetworkTypeGemini(modemNetworkMode, mHandler
        	                .obtainMessage(MESSAGE_SET_PREFERRED_NETWORK_TYPE, oldmode, modemNetworkMode), simId);
            	}else {
            		phone.setPreferredNetworkType(modemNetworkMode, mHandler
                            .obtainMessage(MESSAGE_SET_PREFERRED_NETWORK_TYPE, oldmode, modemNetworkMode));
                }
            } else if (action.equals("out_going_call_to_phone_app")) {
                if (VDBG) Log.d(LOG_TAG, "PhoneAppBroadcastReceiver ------------------------- action.equals out_going_call_to_phone_app");

                final String number = intent.getStringExtra("number");
                final boolean isSip = intent.getBooleanExtra("is_sip_call", false);
                final boolean isVT = intent.getBooleanExtra("is_vt_call", false);
                Intent broadcastIntent = new Intent(Intent.ACTION_NEW_OUTGOING_CALL);
                if (number != null) {
                    broadcastIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
                }
                if (isSip) {
                	broadcastIntent.putExtra("number", number);
                	broadcastIntent.putExtra("launch_from_dialer", true);
                	broadcastIntent.putExtra("is_sip_call", true);
                    broadcastIntent.putExtra("is_vt_call", isVT);
                } else {
                	broadcastIntent.putExtra("number", number);
                    int slot = intent.getIntExtra(com.android.internal.telephony.Phone.GEMINI_SIM_ID_KEY, -2);
                	broadcastIntent.putExtra(com.android.internal.telephony.Phone.GEMINI_SIM_ID_KEY, slot);
                	broadcastIntent.putExtra("launch_from_dialer", true);
                	broadcastIntent.putExtra("is_sip_call", false);
                    broadcastIntent.putExtra("is_vt_call", isVT);
                }
                //intent.setAction(Intent.ACTION_NEW_OUTGOING_CALL);
                PhoneUtils.checkAndCopyPhoneProviderExtras(intent, broadcastIntent);
                //broadcastIntent.putExtra(OutgoingCallBroadcaster.EXTRA_ALREADY_CALLED, true);
                sendOrderedBroadcast(broadcastIntent, PERMISSION, new OutgoingCallPhoneAppReceiver(),
                        null, Activity.RESULT_OK, number, null);
                if (VDBG) Log.d(LOG_TAG, "PhoneAppBroadcastReceiver ------------------------- sendOrderedBroadcast");
            }else if(action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
                Log.d(LOG_TAG, "ACTION_SHUTDOWN_IPO received");
                phone.setRadioPower(false, true);
                if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
        			if (VTManager.State.CLOSE != VTManager.getInstance().getState()) {
        				if (VDBG) Log.d(LOG_TAG,"- call VTManager onDisconnected ! ");
        				VTManager.getInstance().onDisconnected();
        				if (VDBG) Log.d(LOG_TAG,"- finish call VTManager onDisconnected ! ");
        				if (VDBG) Log.d(LOG_TAG,"- set VTManager close ! ");
        				VTManager.getInstance().setVTClose();
        				if (VDBG) Log.d(LOG_TAG,"- finish set VTManager close ! ");
        				if (VTInCallScreenFlags.getInstance().mVTInControlRes) {
        					sendBroadcast(new Intent(VTCallUtils.VT_CALL_END));
        					VTInCallScreenFlags.getInstance().mVTInControlRes = false;
        				}
        			}
        		}
                if (null != inCallUiState) {
                    inCallUiState.clearState();
                }
                // MTK_OP02_PROTECT_START
                if("OP02".equals(PhoneUtils.getOptrProperties())) {
                    if(PhoneApp.this.mInCallScreen != null)
                        PhoneApp.this.mInCallScreen.internalHangupAllCalls(mCM);
                }
                // MTK_OP02_PROTECT_END
            }else if(action.equals("android.intent.action.ACTION_PREBOOT_IPO")){
                Log.d(LOG_TAG, "ACTION_PREBOOT_IPO received");
                Settings.System.putLong(getApplicationContext().getContentResolver(), Settings.System.SIM_LOCK_STATE_SETTING, 0x0L);
                phone.setRadioPowerOn();
                if (null != inCallUiState) {
                    inCallUiState.clearState();
                }
            }else if(action.equals(GeminiPhone.EVENT_3G_SWITCH_START_MD_RESET)){
                Log.d(LOG_TAG, "EVENT_3G_SWITCH_START_MD_RESET");
                Settings.System.putLong(getApplicationContext().getContentResolver(), Settings.System.SIM_LOCK_STATE_SETTING, 0x0L);
                arySIMLockStatus[0] = 3;
                arySIMLockStatus[1] = 3;
            }else if(action.equals(TelephonyIntents.ACTION_RADIO_OFF)){
                int slot = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_SLOT, 0);
                Log.d(LOG_TAG, "ACTION_RADIO_OFF slot = " + slot);
                clearSimSettingFlag(slot);
                Log.i(LOG_TAG,"[xp Test][MODEM RESET]");
                arySIMLockStatus[0] = 3;
                arySIMLockStatus[1] = 3;
            } else if (action.equals(ACTION_MODEM_STATE)) {
                SystemService.start("md_minilog_util");
                /*int mdState = intent.getIntExtra("state", -1);
                Log.i(LOG_TAG, "Get MODEM STATE [" + mdState + "]");
                switch (mdState) {
                    case CCCI_MD_BROADCAST_EXCEPTION:
                        SystemService.start("md_minilog_util");
                        break;
                    case CCCI_MD_BROADCAST_RESET:
                        SystemService.start("md_minilog_util");
                        break;
                    case CCCI_MD_BROADCAST_READY:
                        SystemService.start("md_minilog_util");
                        break;
                    defaut:
                        SystemService.start("md_minilog_util");
                }*/
            }
        }
    }

    /**
     * Broadcast receiver for the ACTION_MEDIA_BUTTON broadcast intent.
     *
     * This functionality isn't lumped in with the other intents in
     * PhoneAppBroadcastReceiver because we instantiate this as a totally
     * separate BroadcastReceiver instance, since we need to manually
     * adjust its IntentFilter's priority (to make sure we get these
     * intents *before* the media player.)
     */
    private class MediaButtonBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (VDBG) Log.d(LOG_TAG,
                           "MediaButtonBroadcastReceiver.onReceive()...  event = " + event);
            //Not sure why add the ACTION_DOWN condition, but this will not answer the incomig call
            //so change the ACTION_DOWN to ACTION_UP (ALPS00287837)
            if ((event != null)
                && (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK)
                && (event.getAction() == KeyEvent.ACTION_UP)) {

                if (event.getRepeatCount() == 0) {
                    // Mute ONLY on the initial keypress.
                    if (VDBG) Log.d(LOG_TAG, "MediaButtonBroadcastReceiver: HEADSETHOOK down!");
                    boolean consumed = PhoneUtils.handleHeadsetHook(phone, event);
                    if (VDBG) Log.d(LOG_TAG, "==> handleHeadsetHook(): consumed = " + consumed);
                    if (consumed) {
                        // If a headset is attached and the press is consumed, also update
                        // any UI items (such as an InCallScreen mute button) that may need to
                        // be updated if their state changed.
                        updateInCallScreen();  // Has no effect if the InCallScreen isn't visible
                        abortBroadcast();
                    }
                } else {
                    if (mCM.getState() != Phone.State.IDLE) {
                        // If the phone is anything other than completely idle,
                        // then we consume and ignore any media key events,
                        // Otherwise it is too easy to accidentally start
                        // playing music while a phone call is in progress.
                        if (VDBG) Log.d(LOG_TAG, "MediaButtonBroadcastReceiver: consumed");
                        abortBroadcast();
                    }
                }
            }
        }
    }

    private void handleServiceStateChanged(Intent intent) {
        /**
         * This used to handle updating EriTextWidgetProvider this routine
         * and and listening for ACTION_SERVICE_STATE_CHANGED intents could
         * be removed. But leaving just in case it might be needed in the near
         * future.
         */

        // If service just returned, start sending out the queued messages
        ServiceState ss = ServiceState.newFromBundle(intent.getExtras());

        if (ss != null) {
            int state = ss.getState();
            notificationMgr.updateNetworkSelection(state, ss.getMySimId());
        }
    }

    public boolean isOtaCallInActiveState() {
        boolean otaCallActive = false;
        if (mInCallScreen != null) {
            otaCallActive = mInCallScreen.isOtaCallInActiveState();
        }
        if (VDBG) Log.d(LOG_TAG, "- isOtaCallInActiveState " + otaCallActive);
        return otaCallActive;
    }

    public boolean isOtaCallInEndState() {
        boolean otaCallEnded = false;
        if (mInCallScreen != null) {
            otaCallEnded = mInCallScreen.isOtaCallInEndState();
        }
        if (VDBG) Log.d(LOG_TAG, "- isOtaCallInEndState " + otaCallEnded);
        return otaCallEnded;
    }

    // it is safe to call clearOtaState() even if the InCallScreen isn't active
    public void clearOtaState() {
        if (DBG) Log.d(LOG_TAG, "- clearOtaState ...");
        if ((mInCallScreen != null)
                && (otaUtils != null)) {
            otaUtils.cleanOtaScreen(true);
            if (DBG) Log.d(LOG_TAG, "  - clearOtaState clears OTA screen");
        }
    }

    // it is safe to call dismissOtaDialogs() even if the InCallScreen isn't active
    public void dismissOtaDialogs() {
        if (DBG) Log.d(LOG_TAG, "- dismissOtaDialogs ...");
        if ((mInCallScreen != null)
                && (otaUtils != null)) {
            otaUtils.dismissAllOtaDialogs();
            if (DBG) Log.d(LOG_TAG, "  - dismissOtaDialogs clears OTA dialogs");
        }
    }

    // it is safe to call clearInCallScreenMode() even if the InCallScreen isn't active
    public void clearInCallScreenMode() {
        if (DBG) Log.d(LOG_TAG, "- clearInCallScreenMode ...");
        if (mInCallScreen != null) {
            mInCallScreen.resetInCallScreenMode();
        }
    }

    /**
     * Force the in-call UI to refresh itself, if it's currently visible.
     *
     * This method can be used any time there's a state change anywhere in
     * the phone app that needs to be reflected in the onscreen UI.
     *
     * Note that it's *not* necessary to manually refresh the in-call UI
     * (via this method) for regular telephony state changes like
     * DIALING -> ALERTING -> ACTIVE, since the InCallScreen already
     * listens for those state changes itself.
     *
     * This method does *not* force the in-call UI to come up if it's not
     * already visible.  To do that, use displayCallScreen().
     */
    /* package */ void updateInCallScreen() {
        if (DBG) Log.d(LOG_TAG, "- updateInCallScreen()...");
        if (mInCallScreen != null) {
            // Post an updateScreen() request.  Note that the
            // updateScreen() call will end up being a no-op if the
            // InCallScreen isn't the foreground activity.
            mInCallScreen.requestUpdateScreen();
        }
    }

    private void handleQueryTTYModeResponse(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;
        if (ar.exception != null) {
            if (DBG) Log.d(LOG_TAG, "handleQueryTTYModeResponse: Error getting TTY state.");
        } else {
            if (DBG) Log.d(LOG_TAG,
                           "handleQueryTTYModeResponse: TTY enable state successfully queried.");
            //We will get the tty mode from the settings directly
            //int ttymode = ((int[]) ar.result)[0];
            int ttymode = Phone.TTY_MODE_OFF;
            if (isHeadsetPlugged()) {
                ttymode = mPreferredTtyMode;
            }
            if (DBG) Log.d(LOG_TAG, "handleQueryTTYModeResponse:ttymode=" + ttymode);

            Intent ttyModeChanged = new Intent(TtyIntent.TTY_ENABLED_CHANGE_ACTION);
            ttyModeChanged.putExtra("ttyEnabled", ttymode != Phone.TTY_MODE_OFF);
            sendBroadcast(ttyModeChanged);

            String audioTtyMode;
            switch (ttymode) {
            case Phone.TTY_MODE_FULL:
                audioTtyMode = "tty_full";
                break;
            case Phone.TTY_MODE_VCO:
                audioTtyMode = "tty_vco";
                break;
            case Phone.TTY_MODE_HCO:
                audioTtyMode = "tty_hco";
                break;
            case Phone.TTY_MODE_OFF:
            default:
                audioTtyMode = "tty_off";
                break;
            }
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setParameters("tty_mode="+audioTtyMode);
            PhoneUtils.setTtyMode(audioTtyMode);
        }
    }

    private int convertTTYmodeToRadio(int ttyMode) {
        int radioMode = 0;
        
        switch (ttyMode) {
            case Phone.TTY_MODE_FULL:
            case Phone.TTY_MODE_HCO:
            case Phone.TTY_MODE_VCO:
                radioMode = Phone.TTY_MODE_FULL;
                break;
                
            default:
            radioMode = Phone.TTY_MODE_OFF;     
        }
        
        return radioMode;
    }

    private void handleSetTTYModeResponse(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;

        if (ar.exception != null) {
            if (DBG) Log.d (LOG_TAG,
                    "handleSetTTYModeResponse: Error setting TTY mode, ar.exception"
                    + ar.exception);
        }

       //Now Phone doesn't support ttymode query, so we make a fake response to trigger the set to audio
        //phone.queryTTYMode(mHandler.obtainMessage(EVENT_TTY_MODE_GET));
        Message m = mHandler.obtainMessage(EVENT_TTY_MODE_GET);
        m.obj = new AsyncResult(null, null, null);
        m.sendToTarget();
    }

    /* package */ void clearUserActivityTimeout() {
        try {
            mPowerManagerService.clearUserActivityTimeout(SystemClock.uptimeMillis(),
                    10*1000 /* 10 sec */);
        } catch (RemoteException ex) {
            // System process is dead.
        }
    }

    /**
     * "Call origin" may be used by Contacts app to specify where the phone call comes from.
     * Currently, the only permitted value for this extra is {@link #ALLOWED_EXTRA_CALL_ORIGIN}.
     * Any other value will be ignored, to make sure that malicious apps can't trick the in-call
     * UI into launching some random other app after a call ends.
     *
     * TODO: make this more generic. Note that we should let the "origin" specify its package
     * while we are now assuming it is "com.android.contacts"
     */
    public static final String EXTRA_CALL_ORIGIN = "com.android.phone.CALL_ORIGIN";
    private static final String DEFAULT_CALL_ORIGIN_PACKAGE = "com.android.contacts";
    private static final String ALLOWED_EXTRA_CALL_ORIGIN =
            "com.android.contacts.activities.DialtactsActivity";

    public void setLatestActiveCallOrigin(String callOrigin) {
        inCallUiState.latestActiveCallOrigin = callOrigin;
    }

    /**
     * @return Intent which will be used when in-call UI is shown and the phone call is hang up.
     * By default CallLog screen will be introduced, but the destination may change depending on
     * its latest call origin state.
     */
    public Intent createPhoneEndIntentUsingCallOrigin() {
        if (TextUtils.equals(inCallUiState.latestActiveCallOrigin, ALLOWED_EXTRA_CALL_ORIGIN)) {
            if (VDBG) Log.d(LOG_TAG, "Valid latestActiveCallOrigin("
                    + inCallUiState.latestActiveCallOrigin + ") was found. "
                    + "Go back to the previous screen.");
            // Right now we just launch the Activity which launched in-call UI. Note that we're
            // assuming the origin is from "com.android.contacts", which may be incorrect in the
            // future.
            final Intent intent = new Intent();
            intent.setClassName(DEFAULT_CALL_ORIGIN_PACKAGE, inCallUiState.latestActiveCallOrigin);
            return intent;
        } else {
            if (VDBG) Log.d(LOG_TAG, "Current latestActiveCallOrigin ("
                    + inCallUiState.latestActiveCallOrigin + ") is not valid. "
                    + "Just use CallLog as a default destination.");
            return PhoneApp.createCallLogIntent();
        }
    }

    public boolean isQVGA() {
    	boolean retval = false;
    	DisplayMetrics dm = new DisplayMetrics();
    	WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
    	wm.getDefaultDisplay().getMetrics(dm);
    	if((dm.widthPixels == 320 && dm.heightPixels == 240)||(dm.widthPixels == 240 && dm.heightPixels == 320))
    	    retval = true;
        return retval;
    }

    /*void displayVTCallScreen() {
        if (VDBG) Log.d(LOG_TAG, "displayVTCallScreen()...");
        startActivity(createVTInCallIntent());
        Profiler.callScreenRequested();
    }*/

    static Intent createVTInCallIntent() { 
    	Intent intent = createInCallIntent();
        intent.putExtra(Constants.EXTRA_IS_VIDEO_CALL, true);
        return intent;
    }
    
    public boolean isVTIdle() {
    	
    	if (true != FeatureOption.MTK_VT3G324M_SUPPORT) {
    	    return true;
    	}
    	
    	if(Phone.State.IDLE == mCM.getState()) {
    	    return true;
    	}    	
    	
    	if (true == FeatureOption.MTK_GEMINI_SUPPORT) {    		
    	    if (Phone.State.IDLE == ((GeminiPhone)phone).getState()) {
    		return true;
    	    }else if(((GeminiPhone)phone).getForegroundCall().getState().isAlive()) {
    		if(((GeminiPhone)phone).getForegroundCall().getLatestConnection().isVideo()) {
		    return false;
		}
    	    }else if(((GeminiPhone)phone).getRingingCall().getState().isAlive()) {
		if(((GeminiPhone)phone).getRingingCall().getLatestConnection().isVideo())
		    return false;
	    }
    	    return true;
	} else {   		
    	    if (Phone.State.IDLE == phone.getState()){
    		return true;
    	    }else if (phone.getForegroundCall().getState().isAlive()) {
    		if(phone.getForegroundCall().getLatestConnection().isVideo()){
		    return false;
		}
    	    }else if (phone.getRingingCall().getState().isAlive()) {
    		if(phone.getRingingCall().getLatestConnection().isVideo()) {
		    return false;
		}
    	    }
    	    return true;
    	}
    }
    
    public boolean isVTActive() {
    	if (true != FeatureOption.MTK_VT3G324M_SUPPORT) {
    	    return false;
    	}
    	if (true == FeatureOption.MTK_GEMINI_SUPPORT) {    		
    	    if (Call.State.ACTIVE == ((GeminiPhone)phone).getForegroundCall().getState()) {
    		if(((GeminiPhone)phone).getForegroundCall().getLatestConnection().isVideo()) {
		    return true;
		}
    	    }
	} else {   		
    	    if (Call.State.ACTIVE == phone.getForegroundCall().getState()) {
    		if (phone.getForegroundCall().getLatestConnection().isVideo()) {
		    return true;
		}
    	    }
    	}
    	return false;
    }
    
    public boolean isVTRinging() {
    	if (true != FeatureOption.MTK_VT3G324M_SUPPORT) {
    	    return false;
    	}
    	if (Phone.State.RINGING != mCM.getState()) {
    	    return false;
    	}
    	if (true == FeatureOption.MTK_GEMINI_SUPPORT) {    		
    	    if(((GeminiPhone)phone).getRingingCall().getState().isRinging()) {
    		if(((GeminiPhone)phone).getRingingCall().getLatestConnection().isVideo()) {
		    return true;
		}
    	    }
	} else {   		
    	    if (phone.getRingingCall().getState().isRinging()) {
    		if(phone.getRingingCall().getLatestConnection().isVideo()) {
		    return true;
		}
    	    }
    	}
    	
    	return false;
    }
    
    public void touchAnswerVTCall(){
    	
    	if (DBG) Log.d (LOG_TAG, "touchAnswerVTCall()");
    	
    	if(getInCallScreenInstance() == null){
    		if (DBG) Log.d (LOG_TAG, "touchAnswerVTCall() : the InCallScreen Instance is null , so cannot answer incoming VT call");
    		return;
    	}
    	
    	if(!isVTRinging()){
    		if (DBG) Log.d (LOG_TAG, "touchAnswerVTCall() : there is no Ringing VT call , so return");
    		return;
    	}
    	
    	mHandler.sendMessage(Message.obtain(mHandler, EVENT_TOUCH_ANSWER_VT));
    }
    
    //To judge whether current sim card need to unlock sim lock:default false
    public static boolean bNeedUnlockSIMLock(int iSIMNum){
    		GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
    		if( (mGeminiPhone.getIccCardGemini(iSIMNum).getState() == IccCard.State.PIN_REQUIRED) ||
    		    (mGeminiPhone.getIccCardGemini(iSIMNum).getState() == IccCard.State.PUK_REQUIRED) ||
    		    (mGeminiPhone.getIccCardGemini(iSIMNum).getState() == IccCard.State.NOT_READY)){   			
    			
    			Log.d(LOG_TAG, "[bNeedUnlockSIMLock][NO Card/PIN/PUK]: " +  iSIMNum);    			
    			return false;
    		}else{
    			return true;
    		}
    	
    }

    // MTK_OP02_PROTECT_START
    void addCallSync() {
        Call fgCall = mCM.getActiveFgCall();
        Call bgCall = mCM.getFirstActiveBgCall();
        
        List<Connection> connections = null;
        CallerInfo ci = null;
        int callType = Calls.OUTGOING_TYPE;
        int simId = Phone.GEMINI_SIM_1;
        int isVideo = 0;

        if(FeatureOption.MTK_GEMINI_SUPPORT) {
            GeminiPhone phone = (GeminiPhone) PhoneApp.getInstance().phone;
            SIMInfo simInfo = null;
            if(phone.getStateGemini(Phone.GEMINI_SIM_2) != Phone.State.IDLE) {
                simId = Phone.GEMINI_SIM_2;
            } else if(phone.getStateGemini(Phone.GEMINI_SIM_1) != Phone.State.IDLE) {
                simId = Phone.GEMINI_SIM_1;
            }
            if(mInCallScreen != null)
                simInfo = SIMInfo.getSIMInfoBySlot(mInCallScreen, simId);
            if(simInfo != null)
                simId = (int)simInfo.mSimId;
            else
                simId = 0;
        }

        if(fgCall.getState() != Call.State.IDLE) {
            connections = fgCall.getConnections();
            for(Connection c : connections) {
                if(c.isAlive()) {
                    ci = notifier.getCallerInfoFromConnection(c);
                    if (c.isIncoming())
                        callType = Calls.INCOMING_TYPE;
                    if(c.isVideo())
                        isVideo = 1;
                    else 
                        isVideo = 0;
                    Calls.addCall(ci, mInCallScreen, c.getAddress(),
                            notifier.getPresentation(c, ci), callType, c.getCreateTime(), (int)(c.getDurationMillis()/1000), simId, isVideo);//, false);
                }
            }
        }
        
        if(bgCall.getState() != Call.State.IDLE) {
            connections = bgCall.getConnections();
            for(Connection c : connections) {
                if(c.isAlive()) {
                    ci = notifier.getCallerInfoFromConnection(c);
                    if (c.isIncoming())
                        callType = Calls.INCOMING_TYPE;
                    if(c.isVideo())
                        isVideo = 1;
                    else 
                        isVideo = 0;
                    Calls.addCall(ci, mInCallScreen, c.getAddress(),
                            notifier.getPresentation(c, ci), callType, c.getCreateTime(), (int)(c.getDurationMillis()/1000), simId, isVideo);//, false);
                }
            }
        }
    }
    // MTK_OP02_PROTECT_END

    @Override
    public void onTerminate() {
        // TODO Auto-generated method stub
        super.onTerminate();
        Log.d(LOG_TAG, "onTerminate");
        HyphonManager.getInstance().onDestroy();
    }


	public boolean isRejectAllVoiceCall() {
		try {
			return getApplicationContext().getSharedPreferences(
					"com.android.phone_preferences", Context.MODE_PRIVATE)
					.getBoolean(AutoRejectSetting.AUTO_REJECT_VOICE_CALL_KEY,
							false);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isRejectAllVideoCall() {
		try {
			return getApplicationContext().getSharedPreferences(
					"com.android.phone_preferences", Context.MODE_PRIVATE)
					.getBoolean(AutoRejectSetting.AUTO_REJECT_VIDEO_CALL_KEY,
							false);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isRejectAllSIPCall() {
		try {
			return getApplicationContext().getSharedPreferences(
					"com.android.phone_preferences", Context.MODE_PRIVATE)
					.getBoolean(AutoRejectSetting.AUTO_REJECT_SIP_CALL_KEY,
							false);
		} catch (Exception e) {
			return false;
		}
	}
	
    private void clearSimSettingFlag(int slot) {

        Long bitSetMask = (0x3L << (2 * slot));

        Long simLockState = 0x0L;

        try {
            simLockState = Settings.System.getLong(getApplicationContext()
                    .getContentResolver(), Settings.System.SIM_LOCK_STATE_SETTING);

            simLockState = simLockState & (~bitSetMask);

            Settings.System.putLong(getApplicationContext().getContentResolver(),
                    Settings.System.SIM_LOCK_STATE_SETTING, simLockState);
        } catch (SettingNotFoundException e) {
            Log.e(LOG_TAG, "clearSimSettingFlag exception");
            e.printStackTrace();
        }
    }

    /* below are added by mediatek .inc */
    CellConnMgr cellConnMgr;

    public Intent createPhoneEndIntent() {
        Intent intent = null;
        if (FeatureOption.MTK_BRAZIL_CUSTOMIZATION_VIVO) {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory (Intent.CATEGORY_HOME);
            return intent;
        }

        if (TextUtils.equals(inCallUiState.latestActiveCallOrigin, ALLOWED_EXTRA_CALL_ORIGIN)) {
            if (VDBG) Log.d(LOG_TAG, "Valid latestActiveCallOrigin("
                    + inCallUiState.latestActiveCallOrigin + ") was found. "
                    + "Go back to the previous screen.");
            // Right now we just launch the Activity which launched in-call UI. Note that we're
            // assuming the origin is from "com.android.contacts", which may be incorrect in the
            // future.
            intent = new Intent();
            intent.setClassName(DEFAULT_CALL_ORIGIN_PACKAGE, inCallUiState.latestActiveCallOrigin);
            return intent;
        }

        return intent;
    }

    private static final String IN_VOICE_COMM_FOCUS_ID = "AudioFocus_For_Phone_Ring_And_Calls";
    void requestAudioFocus(Phone.State state) {
        if (mAudioManager == null) {
            mAudioManager  = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        int audioMode = mAudioManager.getMode();
        
        if (state == Phone.State.RINGING) {
            Log.d(LOG_TAG, " CALL_STATE_RINGING");
            mAudioManager.requestAudioFocus(
                    mAudioFocusListener,
                    AudioManager.STREAM_RING,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        } else if (state == Phone.State.OFFHOOK) {
            Log.d(LOG_TAG, " CALL_STATE_OFFHOOK");
            mAudioManager.requestAudioFocus(
                    mAudioFocusListener,
                    AudioManager.STREAM_RING,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        } else if (state == Phone.State.IDLE) {
            Log.d(LOG_TAG, " CALL_STATE_IDLE");
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
        }
    }
    
    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            // AudioFocus is a new feature: focus updates are made verbose on purpose
            // delay 1.8 second to avoid noises after call.
            Log.d(LOG_TAG, "receive audio focus change message.");
        }
    };
}
