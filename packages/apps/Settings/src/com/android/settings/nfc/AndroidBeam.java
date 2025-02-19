/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.nfc;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import com.android.settings.R;
import android.util.Log;

public class AndroidBeam extends Fragment
        implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "AndroidBeam";
    private View mView;
    private ImageView mImageView;
    private NfcAdapter mNfcAdapter;
    private Switch mActionBarSwitch;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();

        mActionBarSwitch = new Switch(activity);

        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                mActionBarSwitch.setPadding(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(mActionBarSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.RIGHT));
                activity.getActionBar().setTitle(R.string.android_beam_settings_title);
            }
        }

        mActionBarSwitch.setOnCheckedChangeListener(this);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        setSwitchEnable(mNfcAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.android_beam, container, false);
        initView(mView);
        return mView;
    }

    private void initView(View view) {
        mActionBarSwitch.setOnCheckedChangeListener(this);
        setSwitchEnable(mNfcAdapter);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean desiredState) {
        boolean success = false;
        mActionBarSwitch.setEnabled(false);
        if (desiredState) {
            success = mNfcAdapter == null ? false :mNfcAdapter.enableNdefPush();
        } else {
            success = mNfcAdapter == null ? false :mNfcAdapter.disableNdefPush();
        }
        if (success) {
            mActionBarSwitch.setChecked(desiredState);
        }
        mActionBarSwitch.setEnabled(true);
    }
    
    private void setSwitchEnable(NfcAdapter nfcAdapter){
        if (nfcAdapter == null){
       	    Log.d(TAG, "NfcAdapter is null");
            mActionBarSwitch.setChecked(false);
        }else{
            mActionBarSwitch.setChecked(nfcAdapter.isNdefPushEnabled());
        }
    }
}
