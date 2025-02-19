/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.apst.target.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import com.mediatek.apst.target.data.proxy.IRawBufferWritable;
import com.mediatek.apst.target.data.proxy.sysinfo.SystemInfoProxy;
import com.mediatek.apst.target.event.Event;
import com.mediatek.apst.target.event.EventDispatcher;
import com.mediatek.apst.target.event.IBatteryListener;
import com.mediatek.apst.target.event.IPackageListener;
import com.mediatek.apst.target.event.ISdStateListener;
import com.mediatek.apst.target.event.ISimStateListener;
import com.mediatek.apst.target.event.ISmsListener;
import com.mediatek.apst.target.service.MainService;
import com.mediatek.apst.target.service.NotifyService;
import com.mediatek.apst.target.service.SmsSender;
import com.mediatek.apst.target.util.Config;
import com.mediatek.apst.target.util.Debugger;
import com.mediatek.apst.target.util.Global;
import com.mediatek.apst.util.command.sysinfo.SimDetailInfo;
import com.mediatek.apst.util.entity.message.Message;

public class InternalReceiver extends BroadcastReceiver {
    //==============================================================
    // Constants                                                    
    //==============================================================
    
    public static final String ACTION_SMS_RECEIVED = 
        "android.provider.Telephony.SMS_RECEIVED";
    
    public static final String ACTION_SIM_STATE_CHANGED = 
        "android.intent.action.SIM_STATE_CHANGED";
    
    public static final String ACTION_USB_CONNECT = 
        "android.intent.action.UMS_CONNECTED";
    
    public static final String ACTION_USB_DISCONNECT = 
        "android.intent.action.UMS_DISCONNECTED";
    
    public static final String SIM_ID = 
        "simid";
    
    //==============================================================
    // Fields                                                       
    //==============================================================
    //private IEventListener mActionHandler;
    
    private Context mContext;
    
    private boolean mRegistered;
    
    private int mBatteryLevel;
    private int mBatteryScale;
    private Boolean mSimOK;
    private Boolean mSim1OK;
    private Boolean mSim2OK;
    
    //==============================================================
    // Constructors                                                 
    //==============================================================
    public InternalReceiver(Context context){
        super();
        this.mContext = context;
        this.mRegistered = false;
        this.mBatteryLevel = 0;
        this.mSimOK = null;
        this.mSim1OK = null;
        this.mSim2OK = null;
    }
    
    //==============================================================
    // Getters                                                      
    //==============================================================
    public boolean isRegistered(){
        return this.mRegistered;
    }
    
    public int getBatteryLevel(){
        return this.mBatteryLevel;
    }
    
    public int getBatteryScale(){
        return this.mBatteryScale;
    }
    
    //==============================================================
    // Setters                                                      
    //==============================================================
    public void setRegistered(boolean registered){
        this.mRegistered = registered;
    }
    
    //==============================================================
    // Methods                                                      
    //==============================================================
    public void registerAll(){
        IntentFilter intentFilter = new IntentFilter();
        //intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(ACTION_SIM_STATE_CHANGED);
        // Added by Shaoying Han 2011-04-08 
        intentFilter.addAction(Intent.SIM_SETTINGS_INFO_CHANGED);
        intentFilter.addAction(ACTION_SMS_RECEIVED);
        intentFilter.addAction(SmsSender.ACTION_SMS_SENT);
        intentFilter.addAction(SmsSender.ACTION_SMS_DELIVERED);
        mContext.registerReceiver(this, intentFilter);
        
        // Media mount/unmount/remove
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        intentFilter.addDataScheme("file");
        mContext.registerReceiver(this, intentFilter);
        
        // Package add/remove/data clear
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        intentFilter.addDataScheme("package");
        mContext.registerReceiver(this, intentFilter);
        
//        intentFilter = new IntentFilter();
//        intentFilter.addAction(ACTION_USB_CONNECT);
//        intentFilter.addAction(ACTION_USB_DISCONNECT);
//        mContext.registerReceiver(this, intentFilter);
        
        setRegistered(true);
    }
    
    public void unregisterAll(){
        if (isRegistered()){
            setRegistered(false);
            mContext.unregisterReceiver(this);
        }
    }
    
    //@Override
    public void onReceive(Context context, Intent intent) {
        Debugger.logI(new Object[]{context, intent}, "Intent received.");
        String strAction = intent.getAction();
        
        if (null == strAction) {
            Debugger.logW(new Object[]{context, intent}, 
                    "intent.getAction() returns null.");
        } else if (Intent.ACTION_BATTERY_CHANGED.equals(strAction)) {
            // Record battery level
            int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            int batteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,0);
            if (batteryLevel != mBatteryLevel || batteryScale != mBatteryScale){
                EventDispatcher.dispatchBatteryStateChangedEvent(
                            new Event()
                            .put(IBatteryListener.LEVEL, batteryLevel)
                            .put(IBatteryListener.SCALE, batteryScale));
            }
            mBatteryLevel = batteryLevel;
            mBatteryScale = batteryScale;
        } else if (Intent.ACTION_MEDIA_MOUNTED.equals(strAction)) {
            boolean readOnly = intent.getBooleanExtra("read-only", false);
            EventDispatcher.dispatchSdStateChangedEvent(
                    new Event()
                    .put(ISdStateListener.PRESENT, true)
                    .put(ISdStateListener.MOUNTED, true)
                    .put(ISdStateListener.WRITEABLE, !readOnly));
        } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(strAction)) {
            EventDispatcher.dispatchSdStateChangedEvent(
                    new Event()
                    .put(ISdStateListener.PRESENT, true)
                    .put(ISdStateListener.MOUNTED, false)
                    .put(ISdStateListener.WRITEABLE, false));
        } else if (Intent.ACTION_MEDIA_REMOVED.equals(strAction)) {
            EventDispatcher.dispatchSdStateChangedEvent(
                    new Event()
                    .put(ISdStateListener.PRESENT, false)
                    .put(ISdStateListener.MOUNTED, false)
                    .put(ISdStateListener.WRITEABLE, false));
        } else if (Intent.ACTION_MEDIA_BAD_REMOVAL.equals(strAction)) {
            EventDispatcher.dispatchSdStateChangedEvent(
                    new Event()
                    .put(ISdStateListener.PRESENT, false)
                    .put(ISdStateListener.MOUNTED, false)
                    .put(ISdStateListener.WRITEABLE, false));
        } else if (ACTION_SMS_RECEIVED.equals(strAction)) {
            SmsMessage msg = null;
            Bundle extras = intent.getExtras();
            if (null != extras) {
                Object[] pdusObj = (Object[]) extras.get("pdus");
                if (null != pdusObj && pdusObj.length > 0) {
                    long timestamp = System.currentTimeMillis();
                    String address = null;
                    String body = null;
                    for (int i = 0; i< pdusObj.length; i++) {
                        msg = SmsMessage.createFromPdu ((byte[]) pdusObj[i]);
                        //timestamp = msg.getTimestampMillis();
                        if (null == address) {
                            address = msg.getDisplayOriginatingAddress();
                        } else if (!address.equals(
                                msg.getDisplayOriginatingAddress())) {
                            Debugger.logE(new Object[]{context, intent}, 
                                    "Pdus array contains different addresses!");
                        }
                        if (null == body){
                            body = msg.getMessageBody();
                        } else {
                            body += msg.getMessageBody();
                        }
                    }
                    EventDispatcher.dispatchSmsReceivedEvent(
                            new Event()
                            .put(ISmsListener.AFTER_TIME_OF, timestamp - 300)
                            .put(ISmsListener.ADDRESS, address)
                            .put(ISmsListener.BODY, body));
                }
            }
        } else if (SmsSender.ACTION_SMS_SENT.equals(strAction)) {
            long id = intent.getLongExtra(SmsSender.EXTRA_ID, -1);
            long date = intent.getLongExtra(SmsSender.EXTRA_DATE, -1);
            Debugger.logD(SmsSender.EXTRA_ID + "=" + id + ", " + 
                    SmsSender.EXTRA_DATE + "=" + date);
            boolean result;
            
            switch(getResultCode()){
            case Activity.RESULT_OK:
                result = true;
                break;
                
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                result = false;
                break;
                
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                result = false;
                break;
                
            case SmsManager.RESULT_ERROR_NULL_PDU:
                result = false;
                break;
                
            default:
                result = false;
                break;
            }

            EventDispatcher.dispatchSmsSentEvent(
                    new Event()
                    .put(ISmsListener.SMS_ID, id)
                    .put(ISmsListener.DATE, date)
                    .put(ISmsListener.SENT, result));
        } else if (SmsSender.ACTION_SMS_DELIVERED.equals(strAction)) {
            // Do nothing
        } else if (Intent.ACTION_PACKAGE_ADDED.equals(strAction)) {
            int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
            if (uid != -1){
                EventDispatcher.dispatchPackageAddedEvent(
                        new Event()
                        .put(IPackageListener.UID, uid));
            }
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(strAction)) {
            // Do nothing
        } else if (Intent.ACTION_PACKAGE_DATA_CLEARED.equals(strAction)) {
            int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
            if (uid != -1){
                EventDispatcher.dispatchPackageDataClearedEvent(
                        new Event()
                        .put(IPackageListener.UID, uid));
            }
        } else if (ACTION_SIM_STATE_CHANGED.equals(strAction)
        		|| Intent.SIM_SETTINGS_INFO_CHANGED.equals(strAction)) {// Modified by Shaoying Han

              Intent intentService = new Intent(context, NotifyService.class);
              intentService.putExtras(intent.getExtras());
              intentService.putExtra("Action", strAction);
              context.startService(intentService);
/*
            boolean isSimInfoChanged = false;
            boolean isSim1InfoChanged = false;
            boolean isSim2InfoChanged = false;
            if (Intent.SIM_SETTINGS_INFO_CHANGED.equals(strAction)) {
            	if (Config.MTK_GEMINI_SUPPORT){
            		long simId = intent.getLongExtra(SIM_ID, -1);
            		SimDetailInfo simInfo = Global.getSimInfoById((int) simId);
            		if (simInfo != null) {
            			int slotId = simInfo.getSlotId();
            			if (0 == slotId) {
            				isSim1InfoChanged = true;
            			} else if (1 == slotId) {
            				isSim2InfoChanged = true;
            			}
            		}
            		
            	} else {
            		isSimInfoChanged = true;
            	}
            }
        	if (Config.MTK_GEMINI_SUPPORT){
                int sim1State = SystemInfoProxy.getSimState(Message.SIM1_ID);
                int sim2State = SystemInfoProxy.getSimState(Message.SIM2_ID);
                boolean sim1OK = SystemInfoProxy.isSimAccessible(sim1State);
                boolean sim2OK = SystemInfoProxy.isSimAccessible(sim2State);
                SimDetailInfo sim1Info = Global.getSimInfoBySlot(Message.SIM1_ID);
                SimDetailInfo sim2Info = Global.getSimInfoBySlot(Message.SIM2_ID);
                if (null == mSim1OK || mSim1OK != sim1OK || isSim1InfoChanged == true){
                    EventDispatcher.dispatchSimStateChangedEvent(
                            new Event()
                            .put(ISimStateListener.STATE, sim1State)
                            .put(ISimStateListener.SIM_ID, Message.SIM1_ID)
                            // Modified by Shaoying Han
                            .put(ISimStateListener.SIM_INFO, sim1Info)
                            .put(ISimStateListener.SIM_INFO_FLAG, isSimInfoChanged));
                }
                if (null == mSim2OK || mSim2OK != sim2OK || isSim2InfoChanged == true){
                    EventDispatcher.dispatchSimStateChangedEvent(
                            new Event()
                            .put(ISimStateListener.STATE, sim2State)
                            .put(ISimStateListener.SIM_ID, Message.SIM2_ID)
                            // Modified by Shaoying Han
                            .put(ISimStateListener.SIM_INFO, sim2Info)
                            .put(ISimStateListener.SIM_INFO_FLAG, isSimInfoChanged));
                }
                mSim1OK = sim1OK;
                mSim2OK = sim2OK;
            } else {
                int simState = SystemInfoProxy.getSimState(Message.SIM_ID);
                boolean simOK = SystemInfoProxy.isSimAccessible(simState);
                SimDetailInfo simInfo = Global.getSimInfoBySlot(Message.SIM_ID);
                if (null == mSimOK || mSimOK != simOK || isSimInfoChanged == true){
                    EventDispatcher.dispatchSimStateChangedEvent(
                            new Event()
                            .put(ISimStateListener.STATE, simState)
                            .put(ISimStateListener.SIM_ID, Message.SIM_ID)
                            // Modified by Shaoying Han
                            .put(ISimStateListener.SIM_INFO, simInfo)
                            .put(ISimStateListener.SIM_INFO_FLAG, isSimInfoChanged));
                }
                mSimOK = simOK;
            }
*/
        } 
        /*
         * else if (ACTION_USB_DISCONNECT.equals(strAction)) { // usb disconnect
         * Debugger.logI(new Object[] { context }, "Receiver>>>>>>>>>>>>:" +
         * strAction); Intent newIntent = new Intent(context,
         * MainService.class); mContext.stopService(newIntent);
         * 
         * } else if (ACTION_USB_CONNECT.equals(strAction)) { Debugger.logI(new
         * Object[] { context }, "Receiver>>>>>>>>>>>>:" + strAction); }
         */
    }
    
    //==============================================================
    // Inner & Nested classes                                               
    //==============================================================
}
