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
package  com.android.pqtuningtool.app;

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
import android.widget.Toast;

import  com.android.pqtuningtool.R;
import  com.android.pqtuningtool.util.MtkLog;
/**
 * Copied from com.android.setting.wifi.WifiProxyDialog.java
 * 
 */
public class LimitDialog extends AlertDialog implements TextWatcher,
        DialogInterface.OnClickListener {
    private static final String TAG = "LimitDialog";
    private static final boolean LOG = true;
    
    private static final int ERROR_EMPTY = 0;
    private static final int ERROR_INVALID = 1;
    
    private static final int BTN_OK = DialogInterface.BUTTON_POSITIVE;
    private static final int BTN_CANCEL = DialogInterface.BUTTON_NEGATIVE;

    private Context mContext;
    private ContentResolver mCr;
    
    private View mView;
    private EditText mBufferField;
    private TextView mBufferTip;
    private int mBufferSize;
    
    private final String mKey;
    private final int mMinValue;
    private final int mMaxValue;
    private final int mDefaultValue;
    private final int mType;

    private static final String KEY_HTTP_BUFFER_SIZE = "MTK-HTTP-CACHE-SIZE";
    private static final String KEY_RTSP_BUFFER_SIZE = "MTK-RTSP-CACHE-SIZE";
    private static final int DEFAULT_HTTP_BUFFER_SIZE_MIN = 5;//seconds
    private static final int DEFAULT_RTSP_BUFFER_SIZE_MIN = 4;//seconds
    private static final int DEFAULT_HTTP_BUFFER_SIZE_MAX = 30;//seconds
    private static final int DEFAULT_RTSP_BUFFER_SIZE_MAX = 12;//seconds
    private static final int DEFAULT_HTTP_BUFFER_SIZE = 10;//seconds
    private static final int DEFAULT_RTSP_BUFFER_SIZE = 4;//seconds
    
    public static final int TYPE_RTSP = 1;
    public static final int TYPE_HTTP = 2;
    
    public LimitDialog(Context context, int type) {
        super(context);
        mContext = context;
        mCr = mContext.getContentResolver();
        mType = type;
        if (mType == TYPE_HTTP) {
            mKey = KEY_HTTP_BUFFER_SIZE;
            mMinValue = DEFAULT_HTTP_BUFFER_SIZE_MIN;
            mMaxValue = DEFAULT_HTTP_BUFFER_SIZE_MAX;
            mDefaultValue = DEFAULT_HTTP_BUFFER_SIZE;
        } else {
            mKey = KEY_RTSP_BUFFER_SIZE;
            mMinValue = DEFAULT_RTSP_BUFFER_SIZE_MIN;
            mMaxValue = DEFAULT_RTSP_BUFFER_SIZE_MAX;
            mDefaultValue = DEFAULT_RTSP_BUFFER_SIZE;
        }
        mBufferSize = Settings.System.getInt(mCr, mKey, mDefaultValue);
        if (LOG) MtkLog.v(TAG, "LimitDialog(" + type + ") mBufferSize=" + mBufferSize);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mType == TYPE_HTTP) {
            setTitle(R.string.http_buffer_size);
        } else {
            setTitle(R.string.rtsp_buffer_size);
        }
        mView = getLayoutInflater().inflate(R.layout.limit_dialog, null);
        if (mView != null) {
            setView(mView);
        }
        setInverseBackgroundForced(true);
        mBufferField = (EditText)mView.findViewById(R.id.buffer_size);
        mBufferTip = (TextView)mView.findViewById(R.id.buffer_tip);
        if (mBufferField != null) {
            mBufferField.setText(String.valueOf(mBufferSize));
            mBufferField.addTextChangedListener(this);
        }
        mBufferTip.setText(mContext.getString(R.string.buffer_size_tip, mMinValue, mMaxValue));
        setButton(BTN_OK, mContext.getString(android.R.string.ok), this);
        setButton(BTN_CANCEL, mContext.getString(android.R.string.cancel), this);
        super.onCreate(savedInstanceState);
        validate();

    }
    
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == BTN_OK) {
            enableCacheSize();
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
        String bufferSize = mBufferField.getText().toString().trim();
        boolean isValid = true;
        if (bufferSize == null || bufferSize.trim().equals("")) {
            isValid = false;
        } else {
            try {
                mBufferSize = Integer.parseInt(bufferSize);
                if (mBufferSize < mMinValue || mBufferSize > mMaxValue) {
                    isValid = false;
                }
            } catch (NumberFormatException ex) {
                Log.w(TAG, ex.toString());
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
    
    private void enableCacheSize() {
        if (LOG) MtkLog.v(TAG, "enableCacheSize() mBufferSize=" + mBufferSize);
        try {
            Settings.System.putInt(mCr, mKey, mBufferSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}