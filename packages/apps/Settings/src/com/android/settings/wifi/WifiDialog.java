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

package com.android.settings.wifi;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import com.mediatek.featureoption.FeatureOption;

import com.mediatek.xlog.Xlog;

class WifiDialog extends AlertDialog implements WifiConfigUiBase {

    static final String TAG = "WifiDialog";
    static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;
    static final int BUTTON_FORGET = DialogInterface.BUTTON_NEUTRAL;

    private final boolean mEdit;
    private final DialogInterface.OnClickListener mListener;
    private final AccessPoint mAccessPoint;

    private View mView;
    private WifiConfigController mController;

    private TelephonyManager mTm;

    public WifiDialog(Context context, DialogInterface.OnClickListener listener,
            AccessPoint accessPoint, boolean edit, TelephonyManager tm) {
        super(context, R.style.Theme_WifiDialog);
        mEdit = edit;
        mListener = listener;
        mAccessPoint = accessPoint;
        mTm = tm;
    }

    @Override
    public WifiConfigController getController() {
        return mController;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(FeatureOption.MTK_EAP_SIM_AKA==true){
	    if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
                mView = getLayoutInflater().inflate(R.layout.wifi_dialog_eap_sim_aka_gemini, null);
            } else {
                mView = getLayoutInflater().inflate(R.layout.wifi_dialog_eap_sim_aka, null);
            }
        }else{
            mView = getLayoutInflater().inflate(R.layout.wifi_dialog, null);
        }
        setView(mView);
        setInverseBackgroundForced(true);
        mController = new WifiConfigController(this, mView, mAccessPoint, mEdit,mTm);
        super.onCreate(savedInstanceState);
        setDialogWidth();
        mController.verifyPassword();
    }

    @Override
    public boolean isEdit() {
        return mEdit;
    }

    @Override
    public Button getSubmitButton() {
        return getButton(BUTTON_SUBMIT);
    }

    @Override
    public Button getForgetButton() {
        return getButton(BUTTON_FORGET);
    }

    @Override
    public Button getCancelButton() {
        return getButton(BUTTON_NEGATIVE);
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        setButton(BUTTON_SUBMIT, text, mListener);
    }

    @Override
    public void setForgetButton(CharSequence text) {
        setButton(BUTTON_FORGET, text, mListener);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        setButton(BUTTON_NEGATIVE, text, mListener);
    }
    public static String makeNAI(String imsi, String eapMethod) {
        return WifiConfigController.makeNAI(imsi, eapMethod);
    }
    public static String  addQuote(String s) {
        return WifiConfigController.addQuote(s);
    }
    public void setDialogWidth(){
        WindowManager m = getWindow().getWindowManager();
        Display d = m.getDefaultDisplay();
        LayoutParams p = getWindow().getAttributes();
        int width = d.getWidth();
        int height = d.getHeight();
        if(height < width){
            p.width = (int)(d.getWidth()*0.65);
        } else {
            p.width = (int)(d.getWidth()*0.95);
        }
        Xlog.d(TAG,"width="+p.width+"height="+p.height);
        getWindow().setAttributes(p);
        if(mController!=null){
            mController.closeSpinnerDialog();
        }
    }
}
