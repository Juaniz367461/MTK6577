/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

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

package com.mediatek.connectivity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.Log;
import com.mediatek.xlog.Xlog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.content.Context;

import com.android.internal.telephony.DataConnection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.gsm.GsmDataConnection;
import com.android.internal.telephony.DataCallState;

import android.telephony.gsm.GsmCellLocation;
import android.telephony.NeighboringCellInfo;

import com.mediatek.featureoption.FeatureOption;
import com.android.internal.telephony.ITelephony;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import java.lang.Throwable;

import android.util.Log;

public class CdsRadioInfoActivity extends Activity {
    private final String TAG = "cds_phone";

    private static final int EVENT_PHONE_STATE_CHANGED = 100;
    private static final int EVENT_SIGNAL_STRENGTH_CHANGED = 200;
    private static final int EVENT_SERVICE_STATE_CHANGED = 300;

    private static final int EVENT_QUERY_NEIGHBORING_CIDS_DONE = 1001;
    private static final int EVENT_QUERY_DATACALL_LIST_DONE    = 1002;
    private static final int EVENT_AT_CMD_DONE                 = 1003;

        
    private static final String SIMID = "simId";
    private static final String NA = "N/A";
    private static final String UNKNOWN = "unknown";

    private static final String[] CMDLINES = new String[] {
        "AT+EGMR=1,7,\"\"",
        "AT+EGMR=1,10,\"\"", 
        "AT+CGEQREQ=1,2,128,128",
        "AT+CGEQREQ=2,2,128,128"
        };
        
    private TextView mDeviceId; //DeviceId is the IMEI in GSM and the MEID in CDMA
    private TextView mImsi;
    private TextView radioState;
    private TextView simState;
    private TextView number;
    private TextView callState;
    private TextView operatorName;
    private TextView roamingState;
    private TextView gsmState;
    private TextView gprsAttachState;
    private TextView gprsState;
    private TextView network;
    private TextView dBm;
    private TextView mServiceState;
    private TextView mLocation;
    private TextView mNeighboringCids;
    private TextView resets;
    private TextView attempts;
    private TextView successes;
    private TextView disconnects;
    private TextView sentSinceReceived;
    private TextView sent;
    private TextView received;
    private TextView mPingIpAddr;
    private TextView mPingHostname;
    private TextView mHttpClientTest;
    private TextView mSystemProperties;
    private Button pingTestButton;
    private Button mAtBtnCmd;
    

    private ArrayAdapter<String> mAutoCompleteAdapter;
    private AutoCompleteTextView cmdLineList;

    private TelephonyManager mTelephonyManager;
    private Phone mGsmPhone = null;
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private GeminiPhoneStateListener mPhoneStateListener;

    private String mPingIpAddrResult;
    private String mPingHostnameResult;
    private String mHttpClientTestResult;

    private Context mConext;
    private int mSimId = 0;

    private static final String[] RADIO_SYSTEM_PROPERTY = new String[]{
        "gsm.operator.alpha",
        "gsm.sim.operator.default-name",
        "gsm.sim.operator.iso-country",
        "gsm.sim.operator.alpha",
        "gsm.sim.operator.numeric",
        "gsm.network.type",
        "gsm.cs.network.type",
        "gsm.ril.uicctype",        
        "gsm.baseband.capability",
        "gsm.gcf.testmode",
        "gsm.sim.state",
        "gsm.sim.ril.phbready",
        "gsm.sim.retry.pin1",
        "gsm.sim.retry.pin2",
        "gsm.sim.retry.puk1",
        "gsm.sim.retry.puk2",
        "gsm.operator.alpha.2",
        "gsm.sim.operator.default-name.2",
        "gsm.sim.operator.iso-country.2",
        "gsm.sim.operator.alpha.2",
        "gsm.sim.operator.numeric.2",
        "gsm.network.type.2",
        "gsm.cs.network.type.2",
        "gsm.ril.uicctype.2",
        "gsm.baseband.capability2",
        "gsm.gcf.testmode2",
        "gsm.sim.retry.pin1.2",
        "gsm.sim.retry.pin2.2",
        "gsm.sim.retry.puk1.2",
        "gsm.sim.retry.puk2.2",
        "gsm.sim.state.2",
        "gsm.sim.ril.phbready.2",
        "gsm.3gswitch",
        "gsm.current.phone-type",
        "gsm.defaultpdpcontext.active",
        "gsm.nitz.time",
        "gsm.phone.created",
        "gsm.sim.inserted",
        "gsm.siminfo.ready",
        "gsm.version.baseband",
        "init.svc.ril-daemon",
        "init.svc.gsm0710muxd",
        "persist.radio.default_sim",
        "persist.radio.default_sim_mode",
        "persist.radio.fd.counter",
        "persist.radio.fd.off.counter",
        "persist.radio.gprs.attach.type",
        "ro.mediatek.gemini_support",
        };


    class GeminiPhoneStateListener extends PhoneStateListener {
        int mListenSimID = Phone.GEMINI_SIM_1;

        public GeminiPhoneStateListener(int simID){
            mListenSimID = simID;
        }
        
        @Override
        public void onDataConnectionStateChanged(int state) {
            updateDataState();
            updateDataStats();
            updatePdpList();
            updateNetworkType();
        }

        @Override
        public void onDataActivity(int direction) {
            updateDataStats2();
        }

        @Override
        public void onCellLocationChanged(CellLocation location) {
            if(location == null) return;
            Xlog.i(TAG, "sim id:" + mListenSimID + " " + location.toString());
            updateLocation(mGsmPhone.getCellLocation());
        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean mwi) {

        }

        @Override
        public void onCallForwardingIndicatorChanged(boolean cfi) {

        }
    };


    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case EVENT_PHONE_STATE_CHANGED:
                updatePhoneState();
                break;

            case EVENT_SIGNAL_STRENGTH_CHANGED:
                updateSignalStrength();
                break;

            case EVENT_SERVICE_STATE_CHANGED:
                updateServiceState();
                updatePowerState();
                break;

            case EVENT_QUERY_DATACALL_LIST_DONE:
                ar= (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    updateDataCallList((ArrayList<DataCallState>)ar.result);
                }
                break;

            case EVENT_QUERY_NEIGHBORING_CIDS_DONE:
                ar= (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    updateNeighboringCids((ArrayList<NeighboringCellInfo>)ar.result);
                } else {
                    mNeighboringCids.setText(UNKNOWN);
                }
                break;

            case EVENT_AT_CMD_DONE:
                ar= (AsyncResult) msg.obj;
                handleAtCmdResponse(ar);
                break;
            default:
                break;

            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        mSimId = intent.getIntExtra(SIMID, 0);
        Xlog.i(TAG, "The SIM ID is " + mSimId);

        setContentView(R.layout.radio_info);

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

        try {
            if(FeatureOption.MTK_GEMINI_SUPPORT) {
                ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                if(iTelephony == null) {
                    Xlog.e(TAG, "clocwork worked...");
                }
                iTelephony.setDefaultPhone(mSimId);
            } else {
                mSimId = 0;
            }            
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            mGsmPhone = PhoneFactory.getDefaultPhone();
        }

        

        mAutoCompleteAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, CMDLINES);

        cmdLineList = (AutoCompleteTextView) findViewById(R.id.AtComLine);
        cmdLineList.setThreshold(3);
        cmdLineList.setAdapter(mAutoCompleteAdapter);
        cmdLineList.setText("AT+");
        
        mDeviceId         = (TextView) findViewById(R.id.imei);
        mImsi             = (TextView) findViewById(R.id.imsi);
        radioState        = (TextView) findViewById(R.id.radioState);
        simState          = (TextView) findViewById(R.id.simState);
        number            = (TextView) findViewById(R.id.number);
        callState         = (TextView) findViewById(R.id.call);
        operatorName      = (TextView) findViewById(R.id.operator);
        roamingState      = (TextView) findViewById(R.id.roaming);
        gsmState          = (TextView) findViewById(R.id.gsm);
        gprsAttachState   = (TextView) findViewById(R.id.gprs_attach);
        gprsState         = (TextView) findViewById(R.id.gprs);
        network           = (TextView) findViewById(R.id.network);
        mServiceState     = (TextView) findViewById(R.id.service_state);
        dBm               = (TextView) findViewById(R.id.dbm);


        mLocation         = (TextView) findViewById(R.id.location);
        mNeighboringCids  = (TextView) findViewById(R.id.neighboring);

        resets            = (TextView) findViewById(R.id.resets);
        attempts          = (TextView) findViewById(R.id.attempts);
        successes         = (TextView) findViewById(R.id.successes);
        disconnects       = (TextView) findViewById(R.id.disconnects);

        resets.setVisibility(View.GONE);
        attempts.setVisibility(View.GONE);
        successes.setVisibility(View.GONE);
        disconnects.setVisibility(View.GONE);
        
        sentSinceReceived = (TextView) findViewById(R.id.sentSinceReceived);
        sent              = (TextView) findViewById(R.id.sent);
        received          = (TextView) findViewById(R.id.received);

        mPingIpAddr       = (TextView) findViewById(R.id.pingIpAddr);
        mPingHostname     = (TextView) findViewById(R.id.pingHostname);
        mHttpClientTest   = (TextView) findViewById(R.id.httpClientTest);


        pingTestButton    = (Button) findViewById(R.id.ping_test);
        pingTestButton.setOnClickListener(mPingButtonHandler);

        mPhoneStateReceiver = new PhoneStateIntentReceiver(this, mHandler);
        mPhoneStateReceiver.notifySignalStrength(EVENT_SIGNAL_STRENGTH_CHANGED);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);
        mPhoneStateReceiver.notifyPhoneCallState(EVENT_PHONE_STATE_CHANGED);

        mGsmPhone.getNeighboringCids(mHandler.obtainMessage(EVENT_QUERY_NEIGHBORING_CIDS_DONE));

        mAtBtnCmd = (Button) findViewById(R.id.Submit);
        mAtBtnCmd.setOnClickListener(mAtButtonHandler);
        
        mDeviceId.requestFocus();
        
        mConext = this.getBaseContext();
        
        mSystemProperties = (TextView) findViewById(R.id.system_property);
        
                
        if(mSimId == Phone.GEMINI_SIM_2){
           mPhoneStateListener = new GeminiPhoneStateListener(Phone.GEMINI_SIM_2);
        }else{
           mPhoneStateListener = new GeminiPhoneStateListener(Phone.GEMINI_SIM_1);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        updatePhoneState();
        updatePowerState();
        updateSignalStrength();
        updateServiceState();
        updateLocation(mGsmPhone.getCellLocation());
        updateDataState();
        updateDataStats();
        updateDataStats2();
        updateProperties();

        Xlog.i(TAG, "[RadioInfo] onResume: register phone & data intents");

        mPhoneStateReceiver.registerIntent();
        
        if(FeatureOption.MTK_GEMINI_SUPPORT){
            if(mSimId == Phone.GEMINI_SIM_2){
                mTelephonyManager.listenGemini((PhoneStateListener) mPhoneStateListener,
                                         PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                                         | PhoneStateListener.LISTEN_DATA_ACTIVITY
                                         | PhoneStateListener.LISTEN_CELL_LOCATION
                                        ,Phone.GEMINI_SIM_2);
            }else{
                mTelephonyManager.listenGemini((PhoneStateListener) mPhoneStateListener,
                                         PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                                         | PhoneStateListener.LISTEN_DATA_ACTIVITY
                                         | PhoneStateListener.LISTEN_CELL_LOCATION
                                        ,Phone.GEMINI_SIM_1);                
            }
        }else{
            mTelephonyManager.listen((PhoneStateListener) mPhoneStateListener,
                                     PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                                     | PhoneStateListener.LISTEN_DATA_ACTIVITY
                                     | PhoneStateListener.LISTEN_CELL_LOCATION
                                    );
        }

        updateSystemProperties();                                
    }

    @Override
    public void onPause() {
        super.onPause();

        Xlog.i(TAG, "[RadioInfo] onPause: unregister phone & data intents");

        mPhoneStateReceiver.unregisterIntent();
        
        if(FeatureOption.MTK_GEMINI_SUPPORT){
            if(mSimId == Phone.GEMINI_SIM_2){
                    mTelephonyManager.listenGemini((PhoneStateListener) mPhoneStateListener,
                                            PhoneStateListener.LISTEN_NONE
                                            ,Phone.GEMINI_SIM_2);
            }else{
                    mTelephonyManager.listenGemini((PhoneStateListener) mPhoneStateListener,
                                            PhoneStateListener.LISTEN_NONE
                                            ,Phone.GEMINI_SIM_1);
            }
        }else{
            mTelephonyManager.listen((PhoneStateListener) mPhoneStateListener,
                                     PhoneStateListener.LISTEN_NONE
                                    );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    private boolean isRadioOn() {
        try{
            return mGsmPhone.getServiceState().getState() != ServiceState.STATE_POWER_OFF;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private void updatePowerState() {
        String buttonText = isRadioOn() ?
                            getString(R.string.radioInfo_service_on) :
                            getString(R.string.radioInfo_service_off);
        radioState.setText(buttonText);
    }


    private final void
    updateSignalStrength() {
        // TODO PhoneStateIntentReceiver is deprecated and PhoneStateListener
        // should probably used instead.
        int state = mGsmPhone.getServiceState().getState();

        Resources r = getResources();

        if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                (ServiceState.STATE_POWER_OFF == state)) {
            dBm.setText("0");
        }

        int signalDbm = mGsmPhone.getSignalStrength().getGsmSignalStrengthDbm();

        if (-1 == signalDbm) signalDbm = 0;

        int signalAsu = mGsmPhone.getSignalStrength().getGsmAsuLevel();

        if (-1 == signalAsu) signalAsu = 0;

        dBm.setText(String.valueOf(signalDbm) + " "
                    + r.getString(R.string.radioInfo_display_dbm) + "   "
                    + String.valueOf(signalAsu) + " "
                    + r.getString(R.string.radioInfo_display_asu));
    }



    private final void
    updateServiceState() {
        ServiceState serviceState = mGsmPhone.getServiceState();

        if(serviceState == null) return;

        int state = serviceState.getState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
        case ServiceState.STATE_IN_SERVICE:
            display = r.getString(R.string.radioInfo_service_in);
            break;
        case ServiceState.STATE_OUT_OF_SERVICE:
            display = r.getString(R.string.radioInfo_service_out);
            break;
        case ServiceState.STATE_EMERGENCY_ONLY:
            display = r.getString(R.string.radioInfo_service_emergency);
            break;
        case ServiceState.STATE_POWER_OFF:
            display = r.getString(R.string.radioInfo_service_off);
            break;
        }

        gsmState.setText(display);

        if (serviceState.getRoaming()) {
            roamingState.setText(R.string.radioInfo_roaming_in);
        } else {
            roamingState.setText(R.string.radioInfo_roaming_not);
        }

        operatorName.setText(serviceState.getOperatorAlphaLong());
        mServiceState.setText(serviceState.toString());

        /*
        ServiceStateTracker sst = null;

        //Get GPRS attach state
        /*
        try {
            if(FeatureOption.MTK_GEMINI_SUPPORT) {
                //GSMPhone tmpPhone = (GSMPhone) (((GeminiPhone) mGsmPhone).getDefaultPhone());
                //sst = tmpPhone.getServiceStateTracker();
            } else {
                sst = ((GSMPhone) mGsmPhone).getServiceStateTracker();
            }
            //state = sst.getCurrentDataConnectionState();
        } catch(Exception e) {
            e.printStackTrace();
        }
        */

        display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
        case ServiceState.STATE_IN_SERVICE:
            display = r.getString(R.string.radioInfo_service_in);
            break;
        case ServiceState.STATE_OUT_OF_SERVICE:
            display = r.getString(R.string.radioInfo_service_out);
            break;
        case ServiceState.STATE_EMERGENCY_ONLY:
            display = r.getString(R.string.radioInfo_service_emergency);
            break;
        case ServiceState.STATE_POWER_OFF:
            display = r.getString(R.string.radioInfo_service_off);
            break;
        }
        gprsAttachState.setText(display);
        gprsAttachState.setVisibility(View.GONE);
    }


    private final void
    updatePhoneState() {
        Phone.State state = mGsmPhone.getState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);

        switch (state) {
        case IDLE:
            display = r.getString(R.string.radioInfo_phone_idle);
            break;
        case RINGING:
            display = r.getString(R.string.radioInfo_phone_ringing);
            break;
        case OFFHOOK:
            display = r.getString(R.string.radioInfo_phone_offhook);
            break;
        }

        callState.setText(display);
    }

    private final void
    updateDataState() {
        Phone.DataState state = mGsmPhone.getDataConnectionState();
        Resources r = getResources();
        String display = r.getString(R.string.radioInfo_unknown);


        switch (state) {
        case CONNECTED:
            display = r.getString(R.string.radioInfo_data_connected);
            break;
        case CONNECTING:
            display = r.getString(R.string.radioInfo_data_connecting);
            break;
        case DISCONNECTED:
            display = r.getString(R.string.radioInfo_data_disconnected);
            break;
        case SUSPENDED:
            display = r.getString(R.string.radioInfo_data_suspended);
            break;
        }

        gprsState.setText(display);
    }

    private final void updateNetworkType() {
        String sysProp = "gsm.network.type";
        Resources r = getResources();
       
        
        if(FeatureOption.MTK_GEMINI_SUPPORT){
            if(mSimId == 1){
               sysProp = "gsm.network.type.2";
            }
        }
        
        String display = SystemProperties.get(sysProp,
                                                r.getString(R.string.radioInfo_unknown)); 
        network.setText(display);
    }

    private final void
    updateProperties() {
        String s;
        Resources r = getResources();

        s = mGsmPhone.getDeviceId();
        if (s == null) s = r.getString(R.string.radioInfo_unknown);
        mDeviceId.setText(s);


        s = mGsmPhone.getLine1Number();
        if (s == null) s = r.getString(R.string.radioInfo_unknown);
        number.setText(s);

        s = mGsmPhone.getSubscriberId();
        if (s == null) s = r.getString(R.string.radioInfo_unknown);
        mImsi.setText(s);
        
        s = mGsmPhone.getIccCard().getIccCardState().toString();
        if (s == null) s = r.getString(R.string.radioInfo_unknown);
        simState.setText(s);
    }

    private final void updateLocation(CellLocation location) {
        Resources r = getResources();
                              
        Xlog.i(TAG, "GsmCellLocation:" + location.toString());                                          
        
        if (location instanceof GsmCellLocation) {
            GsmCellLocation loc = (GsmCellLocation)location;
            int lac = loc.getLac();
            int cid = loc.getCid();
                                    
            mLocation.setText(r.getString(R.string.radioInfo_lac) + " = "
                              + ((lac == -1) ? "unknown" : lac + "[0x" + Integer.toHexString(lac) + "]")
                              + "\n"
                              + r.getString(R.string.radioInfo_cid) + " = "
                              + ((cid == -1) ? "unknown" : cid + "[0x" + Integer.toHexString(cid) + "]"));
        } else {
            mLocation.setText("unknown");
        }

    }

    private final void updateNeighboringCids(ArrayList<NeighboringCellInfo> cids) {
        StringBuilder sb = new StringBuilder();

        if (cids != null) {
            if ( cids.isEmpty() ) {
                sb.append("no neighboring cells");
            } else {
                for (NeighboringCellInfo cell : cids) {
                    sb.append(cell.toString()).append(" ");
                }
            }
        } else {
            sb.append("unknown");
        }
        mNeighboringCids.setText(sb.toString());
    }


    private final void updateDataStats() {
        String s;

        s = SystemProperties.get("net.gsm.radio-reset", "0");
        resets.setText(s);

        s = SystemProperties.get("net.gsm.attempt-gprs", "0");
        attempts.setText(s);

        s = SystemProperties.get("net.gsm.succeed-gprs", "0");
        successes.setText(s);

        s = SystemProperties.get("net.gsm.disconnect", "0");
        disconnects.setText(s);

        s = SystemProperties.get("net.ppp.reset-by-timeout", "0");
        sentSinceReceived.setText(s);
    }

    private final void updateDataStats2() {
        Resources r = getResources();

        long txPackets = TrafficStats.getMobileTxPackets();
        long rxPackets = TrafficStats.getMobileRxPackets();
        long txBytes   = TrafficStats.getMobileTxBytes();
        long rxBytes   = TrafficStats.getMobileRxBytes();

        String packets = r.getString(R.string.radioInfo_display_packets);
        String bytes   = r.getString(R.string.radioInfo_display_bytes);

        sent.setText(txPackets + " " + packets + ", " + txBytes + " " + bytes);
        received.setText(rxPackets + " " + packets + ", " + rxBytes + " " + bytes);
    }

    /**
     * Ping a IP address.
     */
    private final void pingIpAddr() {
        try {
            // This is hardcoded IP addr. This is for testing purposes.
            // We would need to get rid of this before release.
            String ipAddress = "74.125.47.104";
            Process p = Runtime.getRuntime().exec("ping -c 1 " + ipAddress);
            int status = p.waitFor();
            if (status == 0) {
                mPingIpAddrResult = "Pass";
            } else {
                mPingIpAddrResult = "Fail: IP addr not reachable";
            }
        } catch (IOException e) {
            mPingIpAddrResult = "Fail: IOException";
        } catch (InterruptedException e) {
            mPingIpAddrResult = "Fail: InterruptedException";
        }
    }

    /**
     *  Ping a host name
     */
    private final void pingHostname() {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 www.google.com");
            int status = p.waitFor();
            if (status == 0) {
                mPingHostnameResult = "Pass";
            } else {
                mPingHostnameResult = "Fail: Host unreachable";
            }
        } catch (UnknownHostException e) {
            mPingHostnameResult = "Fail: Unknown Host";
        } catch (IOException e) {
            mPingHostnameResult= "Fail: IOException";
        } catch (InterruptedException e) {
            mPingHostnameResult = "Fail: InterruptedException";
        }
    }

    /**
     * This function checks for basic functionality of HTTP Client.
     */
    private void httpClientTest() {
        HttpClient client = new DefaultHttpClient();
        try {
            HttpGet request = new HttpGet("http://www.google.com");
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                mHttpClientTestResult = "Pass";
            } else {
                mHttpClientTestResult = "Fail: Code: " + String.valueOf(response);
            }
            request.abort();
        } catch (IOException e) {
            mHttpClientTestResult = "Fail: IOException";
        }
    }



    private final void updatePingState() {
        final Handler handler = new Handler();
        // Set all to unknown since the threads will take a few secs to update.
        mPingIpAddrResult = getResources().getString(R.string.radioInfo_unknown);
        mPingHostnameResult = getResources().getString(R.string.radioInfo_unknown);
        mHttpClientTestResult = getResources().getString(R.string.radioInfo_unknown);

        mPingIpAddr.setText(mPingIpAddrResult);
        mPingHostname.setText(mPingHostnameResult);
        mHttpClientTest.setText(mHttpClientTestResult);

        final Runnable updatePingResults = new Runnable() {
            public void run() {
                mPingIpAddr.setText(mPingIpAddrResult);
                mPingHostname.setText(mPingHostnameResult);
                mHttpClientTest.setText(mHttpClientTestResult);
            }
        };
        Thread ipAddr = new Thread() {
            @Override
            public void run() {
                pingIpAddr();
                handler.post(updatePingResults);
            }
        };
        ipAddr.start();

        Thread hostname = new Thread() {
            @Override
            public void run() {
                pingHostname();
                handler.post(updatePingResults);
            }
        };
        hostname.start();

        Thread httpClient = new Thread() {
            @Override
            public void run() {
                httpClientTest();
                handler.post(updatePingResults);
            }
        };
        httpClient.start();
    }

    private final void updatePdpList() {
        mGsmPhone.getDataCallList(mHandler.obtainMessage(EVENT_QUERY_DATACALL_LIST_DONE));


        /*
                List<DataConnection> dcs = mGsmPhone.getCurrentDataConnectionList();
            for (DataConnection dc : dcs) {
                        sb.append("    State=").append(dc.getStateAsString()).append("\n");
                        if (dc.isActive()) {
                            long timeElapsed =
                                (System.currentTimeMillis() - dc.getConnectionTime())/1000;
                            sb.append("    connected at ")
                              .append(DateUtils.timeString(dc.getConnectionTime()))
                              .append(" and elapsed ")
                              .append(DateUtils.formatElapsedTime(timeElapsed));

                            if (dc instanceof GsmDataConnection) {
                                GsmDataConnection pdp = (GsmDataConnection)dc;
                                sb.append("\n    to ")
                                  .append(pdp.getApn().toString());
                            }
                            sb.append("\nLinkProperties: ");
                            sb.append(phone.getLinkProperties(phone.getActiveApnTypes()[0]).toString());
                        } else if (dc.isInactive()) {
                            sb.append("    disconnected with last try at ")
                              .append(DateUtils.timeString(dc.getLastFailTime()))
                              .append("\n    fail because ")
                              .append(dc.getLastFailCause().toString());
                        } else {
                            if (dc instanceof GsmDataConnection) {
                                GsmDataConnection pdp = (GsmDataConnection)dc;
                                sb.append("    is connecting to ")
                                  .append(pdp.getApn().toString());
                            } else {
                                sb.append("    is connecting");
                            }
                        }
                        sb.append("\n===================");
                    }
        */
    }

    private final void updateDataCallList(ArrayList<DataCallState> dataCallStates) {
        StringBuilder sb = new StringBuilder("========DATA=======\n");

        if (dataCallStates != null) {
            if ( dataCallStates.isEmpty() ) {
                sb.append("no data call lists");
            } else {
                for (DataCallState dataCallState : dataCallStates) {
                    sb.append(dataCallState.toString()).append("\r\n");
                }
            }
        } else {
            sb.append("unknown");
        }

        disconnects.setText(sb.toString());
    }


    OnClickListener mPingButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            updatePingState();
        }
    };

    OnClickListener mAtButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            String atCmdLine[] = {"", ""};


            atCmdLine[0] = cmdLineList.getText().toString();

            Xlog.v(TAG , "Execute AT command:" + atCmdLine[0]);

            mGsmPhone.invokeOemRilRequestStrings(atCmdLine,
                                                 mHandler.obtainMessage(EVENT_AT_CMD_DONE));

        }
    };

    void handleAtCmdResponse(AsyncResult ar) {
        if (ar.exception != null) {
            Toast.makeText(mConext, "AT command is failed to send", Toast.LENGTH_LONG).show();
        } else {
            String[] str = (String[]) ar.result;
            String txt = "";
            for(int i = 0; i < str.length; i++) {
                txt += str[i] + "\r\n";
            }
            Xlog.i(TAG, "resopnse is " + txt);
            Toast.makeText(mConext, "AT command is sent" , Toast.LENGTH_LONG).show();
        }
    }
    
    private void updateSystemProperties(){
        StringBuilder sb = new StringBuilder();
        
        for(int i = 0; i < RADIO_SYSTEM_PROPERTY.length; i++){
            sb.append("[" + RADIO_SYSTEM_PROPERTY[i] + "]: [" + SystemProperties.get(RADIO_SYSTEM_PROPERTY[i],"") + "]\r\n");
        }
        
        mSystemProperties.setText(sb);
        
    }    
}
