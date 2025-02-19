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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.mediatek.xlog.Xlog;

import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/**
 * "Change ICC PIN" UI for the Phone app.
 */
public class ChangeIccPinScreen extends Activity {
    private static final String LOG_TAG = "Settings/"+PhoneApp.LOG_TAG;
    private static final boolean DBG = true;

    private static final int EVENT_PIN_CHANGED = 100;
    
    private enum EntryState {
        ES_PIN,
        ES_PUK
    }
    
    private EntryState mState;

    private static final int NO_ERROR = 0;
    private static final int PIN_MISMATCH = 1;
    private static final int PIN_INVALID_LENGTH = 2;

    private static final int MIN_PIN_LENGTH = 4;
    private static final int MAX_PIN_LENGTH = 8;
    private static final int GET_SIM_RETRY_EMPTY = -1;
    private final BroadcastReceiver mReceiver = new ChangeIccPinScreenBroadcastReceiver();
    private Phone mPhone;
    private boolean mChangePin2;
    private TextView mOldPinLabel;
    private TextView mPinRetryLabel;
    private TextView mPukRetryLabel;
    private TextView mBadPukError;
    private TextView mPuk2Label;
    private TextView mNewPin1Label;
    private TextView mNewPin2Label;
    private TextView mBadPinError;
    private TextView mMismatchError;
    private EditText mOldPin;
    private EditText mNewPin1;
    private EditText mNewPin2;
    private EditText mPUKCode;
    private Button mButton;
    private ScrollView mScrollView;

    private LinearLayout mOldPINPanel;
    private LinearLayout mIccPUKPanel;

    private int mSimId;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_PIN_CHANGED:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    handleResult(ar);
                    break;
            }

            return;
        }
    };

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPhone = PhoneApp.getPhone();

        resolveIntent();

        setContentView(R.layout.change_sim_pin_screen);
        int id;

        
        mOldPinLabel = (TextView) findViewById(R.id.old_pin_label);
        if (mOldPinLabel != null){
        id = mChangePin2 ? R.string.oldPin2Label : R.string.oldPinLabel;
        mOldPinLabel.setText(id);
        mOldPinLabel.append(getResources().getText(R.string.pin_length_indicate));
        }

        mPinRetryLabel = (TextView) findViewById(R.id.pin_retry_label);
        mOldPINPanel = (LinearLayout) findViewById(R.id.old_pin_panel);

        
        mNewPin1Label = (TextView) findViewById(R.id.new_pin1_label);
        if (mNewPin1Label != null){
        id = mChangePin2 ? R.string.newPin2Label : R.string.newPinLabel;
        mNewPin1Label.setText(id);
        }

        
        mNewPin2Label = (TextView) findViewById(R.id.new_pin2_label);
        if (mNewPin2Label != null){
        id = mChangePin2 ? R.string.confirmPin2Label : R.string.confirmPinLabel;
        mNewPin2Label.setText(id);
        }
        
        mPuk2Label = (TextView) findViewById(R.id.puk2_label);
        if (mPuk2Label != null){
        mPuk2Label.append(getResources().getText(R.string.puk_length_indicate));
        }
        mPukRetryLabel = (TextView) findViewById(R.id.puk_retry_label);
        mBadPukError = (TextView) findViewById(R.id.bad_puk);
        mOldPin = (EditText) findViewById(R.id.old_pin);
        mNewPin1 = (EditText) findViewById(R.id.new_pin1);
        mNewPin2 = (EditText) findViewById(R.id.new_pin2);

        mBadPinError = (TextView) findViewById(R.id.bad_pin);
        mMismatchError = (TextView) findViewById(R.id.mismatch);

        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(mClicked);

        mScrollView = (ScrollView) findViewById(R.id.scroll);
        
        mPUKCode = (EditText) findViewById(R.id.puk_code);

        mIccPUKPanel = (LinearLayout) findViewById(R.id.puk_panel);

        
        mState = EntryState.ES_PIN;
        IntentFilter intentFilter =
            new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        
        if(FeatureOption.MTK_GEMINI_SUPPORT) {
            intentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }

        registerReceiver(mReceiver, intentFilter);
    }

    private void resolveIntent() {
        Intent intent = getIntent();
        mChangePin2 = intent.getBooleanExtra("pin2", mChangePin2);
        mSimId = intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, -1);
    }

    private void reset() {
        mScrollView.scrollTo(0, 0);
        mBadPinError.setVisibility(View.GONE);
        mBadPukError.setVisibility(View.GONE);
        mMismatchError.setVisibility(View.GONE);
    }

    private int validateNewPin(String p1, String p2) {
        if (p1 == null) {
            return PIN_INVALID_LENGTH;
        }

        if (!p1.equals(p2)) {
            return PIN_MISMATCH;
        }

        int len1 = p1.length();

        if (len1 < MIN_PIN_LENGTH || len1 > MAX_PIN_LENGTH) {
            return PIN_INVALID_LENGTH;
        }

        return NO_ERROR;
    }

    private View.OnClickListener mClicked = new View.OnClickListener() {
        public void onClick(View v) {
            if (v == mButton) {
                IccCard iccCardInterface;
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    iccCardInterface = ((GeminiPhone)mPhone).getIccCardGemini(mSimId);
                } else {
                    iccCardInterface = mPhone.getIccCard();
                }
                if (iccCardInterface != null) {
                    String oldPin = mOldPin.getText().toString();
                    String puk = mPUKCode.getText().toString();
                    if (mState == EntryState.ES_PUK) {
                        if (puk == null || puk.length() != MAX_PIN_LENGTH) {
                            mPUKCode.getText().clear();
                            mBadPukError.setText(R.string.invalidPuk2);
                            mBadPukError.setVisibility(View.VISIBLE);
                            mMismatchError.setVisibility(View.GONE);
                            mPUKCode.requestFocus();
                            return;
                        } else {
                            mBadPukError.setVisibility(View.GONE);
                        }
                    } else {
                        if (oldPin == null || oldPin.length() < MIN_PIN_LENGTH || oldPin.length() > MAX_PIN_LENGTH) {
                            int id = mChangePin2 ? R.string.invalidPin2 : R.string.invalidPin;
                            mOldPin.getText().clear();
                            mBadPinError.setText(id);
                            mBadPinError.setVisibility(View.VISIBLE);
                            mMismatchError.setVisibility(View.GONE);
                            mOldPin.requestFocus();
                            return;
                        } else {
                            mBadPinError.setVisibility(View.GONE);
                        }   
                    }
                    String newPin1 = mNewPin1.getText().toString();
                    String newPin2 = mNewPin2.getText().toString();

                    int error = validateNewPin(newPin1, newPin2);

                    switch (error) {
                        case PIN_INVALID_LENGTH:
                        case PIN_MISMATCH:
                            mNewPin1.getText().clear();
                            mNewPin2.getText().clear();
                            mMismatchError.setVisibility(View.VISIBLE);
                            mNewPin1.requestFocus();
                            Resources r = getResources();
                            CharSequence text;

                            if (error == PIN_MISMATCH) {
                            	// modified by mtk80909 for mismatch pin2, 2010-9-16
                            	int id = mChangePin2 ? R.string.mismatchPin2 : R.string.mismatchPin;
                                text = r.getString(id);
                            } else {
                                int id = mChangePin2 ? R.string.invalidPin2 : R.string.invalidPin;
                                text = r.getString(id);
                            }

                            mMismatchError.setText(text);
                            break;

                        default:
                            Message callBack = Message.obtain(mHandler,
                                    EVENT_PIN_CHANGED);

                            if (DBG) log("change pin attempt: old=" + oldPin +
                                    ", newPin=" + newPin1);

                            reset();

                            if (mChangePin2) {
                                if (mState == EntryState.ES_PUK) {
//                                    mPhone.getIccCard().supplyPuk2(puk, 
//                                            mNewPin1.getText().toString(), 
//                                            Message.obtain(mHandler, EVENT_PIN_CHANGED));
                                    iccCardInterface.supplyPuk2(puk, 
                                            mNewPin1.getText().toString(), 
                                            Message.obtain(mHandler, EVENT_PIN_CHANGED));
                                } else {
                                    iccCardInterface.changeIccFdnPassword(oldPin,
                                            newPin1, callBack);
                                }
                            } else {
                                iccCardInterface.changeIccLockPassword(oldPin,
                                        newPin1, callBack);
                            }

                            // TODO: show progress panel
                    }
                }
            }
        }
    };

    private void handleResult(AsyncResult ar) {
        if (ar.exception == null) {
            if (DBG) log("handleResult: success!");

            // TODO: show success feedback
            showConfirmation();

            finish();


        } else {
            if (mState == EntryState.ES_PIN) {
                if (DBG) log("handleResult: pin failed!");
                mOldPin.getText().clear();
                int id = mChangePin2 ? R.string.badPin2 : R.string.badPin;
                mBadPinError.setText(id);
                mBadPinError.setVisibility(View.VISIBLE);
                if (getRetryPinCount() == 0) {
                    if (DBG) log("handleResult: puk requested!");
                    if (mChangePin2) {
                    mState = EntryState.ES_PUK;
                    displayPUKAlert();
                        showPukPanel();
                    } else {
                        finish();
                    }
                } else {
                    mPinRetryLabel.setText(getRetryPin());
                    mOldPin.requestFocus();
                }
            } else if (mState == EntryState.ES_PUK) {
                //should really check to see if the error is CommandException.PASSWORD_INCORRECT...
                if (DBG) log("handleResult: puk2 failed!");
                if (getRetryPuk2Count() == 0) {
                    Toast.makeText(this, R.string.sim_permanently_locked, Toast.LENGTH_SHORT).show();
                    finish();
                }
                mBadPukError.setText(R.string.badPuk2);
                mBadPukError.setVisibility(View.VISIBLE);
                mPukRetryLabel.setText(getRetryPuk2());
                mPUKCode.getText().clear();
                mPUKCode.requestFocus();
            }
        }
    }
    
    private void displayPUKAlert () {
        new AlertDialog.Builder(this)
            .setMessage (R.string.puk_requested)
            .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

        }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateScreenPanel();
        reset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private int getRetryPinCount() {
        if (mSimId == Phone.GEMINI_SIM_2) {
            return mChangePin2 ? SystemProperties.getInt("gsm.sim.retry.pin2.2",GET_SIM_RETRY_EMPTY) 
                    : SystemProperties.getInt("gsm.sim.retry.pin1.2", GET_SIM_RETRY_EMPTY);
        }
        return mChangePin2 ? SystemProperties.getInt("gsm.sim.retry.pin2",GET_SIM_RETRY_EMPTY) 
                : SystemProperties.getInt("gsm.sim.retry.pin1", GET_SIM_RETRY_EMPTY);
    }

    private String getRetryPin() {
        int retryCount = getRetryPinCount();
        switch (retryCount) {
        case GET_SIM_RETRY_EMPTY:
            return " ";
        case 1:
            return getString(R.string.one_retry_left);
        default:
            return getString(R.string.retries_left,retryCount) ;
        }
    }

    private int getRetryPuk2Count() {
        if (mSimId == Phone.GEMINI_SIM_2) {
            return SystemProperties.getInt("gsm.sim.retry.puk2.2",GET_SIM_RETRY_EMPTY);
        }
        return SystemProperties.getInt("gsm.sim.retry.puk2",GET_SIM_RETRY_EMPTY);
    }

    private String getRetryPuk2() {
        int retryCount = getRetryPuk2Count();
        switch (retryCount) {
        case GET_SIM_RETRY_EMPTY:
            return " ";
        case 1:
            return getString(R.string.one_retry_left);
        default:
            return getString(R.string.retries_left,retryCount) ;
        }
    }

    private void showPukPanel() {
        setTitle(getResources().getText(R.string.unblock_pin2));
        mPukRetryLabel.setText(getRetryPuk2());
        mIccPUKPanel.setVisibility(View.VISIBLE);
        mOldPINPanel.setVisibility(View.GONE);
        mPUKCode.requestFocus();
    }

    private void showPinPanel() {
        int id = mChangePin2 ? R.string.change_pin2 : R.string.change_pin;
        setTitle(getResources().getText(id));
        mPinRetryLabel.setText(getRetryPin());
        mIccPUKPanel.setVisibility(View.GONE);
        mOldPINPanel.setVisibility(View.VISIBLE);
        mOldPin.requestFocus();
    }

    private void updateScreenPanel() {
        if (mChangePin2) {
            if (getRetryPinCount() == 0) {
                if (getRetryPuk2Count() == 0) {
                    finish();
                }
                mState = EntryState.ES_PUK;
                showPukPanel();
           } else {
               mState = EntryState.ES_PIN;
               showPinPanel();
           }
        } else {
            showPinPanel();
        }
    }

    private void showConfirmation() {
        int id;
        if (mState == EntryState.ES_PUK) {
            id = R.string.pin2_unblocked;   
        } else {
            id = mChangePin2 ? R.string.pin2_changed : R.string.pin_changed;
        }
        Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
    }

    private void log(String msg) {
        String prefix = mChangePin2 ? "[ChgPin2]" : "[ChgPin]";
        Xlog.d(LOG_TAG, prefix + msg);
    }

    private class ChangeIccPinScreenBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ((action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            				&&intent.getBooleanExtra("state", false))
            		||(action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)
            				&&(intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1) == 0))){
                finish();
            }
        }
    }
}
