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
//MTK_OP01_PROTECT_START
package com.android.gallery3d.app;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.gallery3d.R;
import com.android.gallery3d.util.MtkLog;
/**
 * Copied from com.android.setting.wifi.WifiProxyDialog.java
 * 
 */
public class ProxyDialog extends AlertDialog implements TextWatcher,
        DialogInterface.OnClickListener {
    private static final String TAG = "Gallery3D/ProxyDialog";
    private static final boolean LOG = true;
    
    private static final int UNKNOWN_PORT = -1;
    private static final int ERROR_NONE = -1;
    private static final int ERROR_HOST_EMPTY = 0;
    private static final int ERROR_PORT_EMPTY = 1;
    private static final int ERROR_HOST_INVALID = 2;
    private static final int ERROR_PORT_INVALID = 3;

    private static String SETTING_KEY_PROXY_HOST;
    private static String SETTING_KEY_PROXY_PORT;
    
    private static final int BTN_OK = DialogInterface.BUTTON_POSITIVE;
    private static final int BTN_CANCEL = DialogInterface.BUTTON_NEGATIVE;

    private Context mContext;
    private ContentResolver mCr;
    private int mType;
    
    private View mView;
    private EditText mHostField;
    private EditText mPortField;
    private TextView mHostErrMsg;
    private TextView mPortErrMsg;
    
    private String mHost;
    private String mPort;

    public static final int TYPE_RTSP = 1;
    public static final int TYPE_HTTP = 2;
    
    public ProxyDialog(Context context, int type) {
        super(context);
        mContext = context;
        mCr = mContext.getContentResolver();
        mType = type;
        if (mType == TYPE_RTSP) {
            SETTING_KEY_PROXY_HOST = MediaStore.Streaming.Setting.RTSP_PROXY_HOST;
            SETTING_KEY_PROXY_PORT = MediaStore.Streaming.Setting.RTSP_PROXY_PORT;
        } else if (mType == TYPE_HTTP) {
            SETTING_KEY_PROXY_HOST = MediaStore.Streaming.Setting.HTTP_PROXY_HOST;
            SETTING_KEY_PROXY_PORT = MediaStore.Streaming.Setting.HTTP_PROXY_PORT;
        } else {
            throw new RuntimeException("Error type = " + type 
                    + ". type should be one of ProxyDialog.TYPE_RTSP and ProxyDialog.TYPE_HTTP");
        }
        if (LOG) MtkLog.v(TAG, "ProxyDialog(" + mType + ")");
        if (LOG) MtkLog.v(TAG, "SETTING_KEY_PROXY_HOST=" + SETTING_KEY_PROXY_HOST);
        if (LOG) MtkLog.v(TAG, "SETTING_KEY_PROXY_PORT=" + SETTING_KEY_PROXY_PORT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mType == TYPE_RTSP) {
            setTitle(R.string.rtsp_proxy_settings);
        } else {
            setTitle(R.string.http_proxy_settings);
        }
        mView = getLayoutInflater().inflate(R.layout.proxy_dialog, null);
        if (mView != null) {
            setView(mView);
        }
        //setInverseBackgroundForced(true);

        mHostField = (EditText)mView.findViewById(R.id.proxy_host);
        mPortField = (EditText)mView.findViewById(R.id.proxy_port);

        if (mHostField != null && mPortField != null) {
            mHost = Settings.System.getString(mCr, SETTING_KEY_PROXY_HOST);
            mHostField.setText(mHost != null ? mHost : "");
            mHostField.addTextChangedListener(this);
            
            int port = Settings.System.getInt(mCr, SETTING_KEY_PROXY_PORT, UNKNOWN_PORT);
            if (port == UNKNOWN_PORT) {
                mPort = "";
            } else {
                try {
                    mPort = Integer.toString(port);
                } catch (NumberFormatException ex) {
                    MtkLog.e(TAG, ex.toString());
                    mPort = "";
                }
            }
            mPortField.setText(mPort != null ? mPort : "");
            mPortField.addTextChangedListener(this);
        }

        mHostErrMsg = (TextView)mView.findViewById(R.id.proxy_host_err_msg);
        if (mHostErrMsg != null) {
            mHostErrMsg.setText("");
        }

        mPortErrMsg = (TextView)mView.findViewById(R.id.proxy_port_err_msg);
        if (mPortErrMsg != null) {
            mPortErrMsg.setText("");
        }
        
        setButton(BTN_OK, mContext.getString(android.R.string.ok), this);
        setButton(BTN_CANCEL, mContext.getString(android.R.string.cancel), this);
           
        super.onCreate(savedInstanceState);

        validate();

    }

    /**
     * @return current proxy type
     */
    public int getType() {
        return mType;
    }
    
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == BTN_OK) {
            enableProxy();
        } else if (button == BTN_CANCEL) {
            //do nothing
        }
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }
    
    public void afterTextChanged(Editable editable) {
        validate();
    }

    private void validate() {
        mHost = mHostField.getText().toString().trim();
        mPort = mPortField.getText().toString().trim();
        boolean isValid = true;
    
        mHostField.setHint("");
        mPortField.setHint("");
        mHostErrMsg.setText("");
        mPortErrMsg.setText("");

        if (mHost != null && mPort != null) {
            if (mHost.length() == 0 && mPort.length() != 0) {
                showError(ERROR_HOST_EMPTY);
                isValid = false;
            } 
            if (mHost.length() != 0 && mPort.length() == 0) {
                showError(ERROR_PORT_EMPTY);
                isValid = false;
            } 
            if (mPort.length() != 0) {
                try {
                    int port = Integer.parseInt(mPort);
                    if (port <= 0 || port > 0xFFFF) {
                        showError(ERROR_PORT_INVALID);
                        isValid = false;
                    }
                } catch (NumberFormatException ex) {
                    MtkLog.w(TAG, ex.toString());
                    showError(ERROR_PORT_INVALID);
                    isValid = false;
                }        
            }

            if (getButton(BTN_OK) != null) {
                if (isValid) {
                    getButton(BTN_OK).setEnabled(true);
                } else {
                    getButton(BTN_OK).setEnabled(false);
                }
            }
            
        }
    }

    private void showError(int errCode) {
        String[] errMsg = mContext.getResources().getStringArray(R.array.proxy_error);
    
        switch (errCode) {
        case ERROR_HOST_EMPTY:
            mHostField.setHint(errMsg[ERROR_HOST_EMPTY]);
            break;

        case ERROR_PORT_EMPTY:
            mPortField.setHint(errMsg[ERROR_PORT_EMPTY]);
            break;

        case ERROR_HOST_INVALID:
            mHostErrMsg.setText(errMsg[ERROR_HOST_INVALID]);
            break;

        case ERROR_PORT_INVALID:
            mPortErrMsg.setText(errMsg[ERROR_PORT_INVALID]);
            break;
        default:
            break;
        }

        if (getButton(BTN_OK) != null) {
            getButton(BTN_OK).setEnabled(false);
        }
        
    }
    
    private void enableProxy() {
        boolean save = false;
        if (mHost != null && mPort != null) {
            if (mHost.length() != 0 && mPort.length() != 0) {
                try {
                    Settings.System.putString(mCr, SETTING_KEY_PROXY_HOST, mHost);
                    Settings.System.putString(mCr, SETTING_KEY_PROXY_PORT, mPort);
                    save = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (!save) {
            Settings.System.putString(mCr, SETTING_KEY_PROXY_HOST, "");
            Settings.System.putString(mCr, SETTING_KEY_PROXY_PORT, String.valueOf(UNKNOWN_PORT));
        }
    }
}
//MTK_OP01_PROTECT_END
