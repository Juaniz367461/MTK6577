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

package com.android.email.activity.setup;

import com.android.email.R;
import com.android.email.activity.ActivityHelper;
import com.android.email.activity.UiUtilities;
import com.android.emailcommon.provider.Account;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Provides setup flow for IMAP/POP accounts.
 *
 * Uses AccountSetupIncomingFragment for primary UI.  Uses AccountCheckSettingsFragment to validate
 * the settings as entered.  If the account is OK, proceeds to AccountSetupOutgoing.
 */
public class AccountSetupIncoming extends AccountSetupActivity
        implements AccountSetupIncomingFragment.Callback, OnClickListener {

    /* package */ AccountSetupIncomingFragment mFragment;
    private Button mNextButton;
    /* package */ boolean mNextButtonEnabled;

    public static void actionIncomingSettings(Activity fromActivity, int mode, Account account) {
        SetupData.setFlowMode(mode);
        SetupData.setAccount(account);
        fromActivity.startActivity(new Intent(fromActivity, AccountSetupIncoming.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHelper.debugSetWindowFlags(this);
        setContentView(R.layout.account_setup_incoming);

        mFragment = (AccountSetupIncomingFragment)
                getFragmentManager().findFragmentById(R.id.setup_fragment);

        // Configure fragment
        mFragment.setCallback(this);

        mNextButton = (Button) UiUtilities.getView(this, R.id.next);
        mNextButton.setOnClickListener(this);
        UiUtilities.getView(this, R.id.previous).setOnClickListener(this);
   }

    /**
     * Implements View.OnClickListener
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.next:
                mFragment.onNext();
                break;
            case R.id.previous:
                onBackPressed();
                break;
        }
    }

    /**
     * Implements AccountServerBaseFragment.Callback
     *
     * Launches the account checker.  Positive results are reported to onCheckSettingsOk().
     */
    public void onProceedNext(int checkMode, AccountServerBaseFragment target) {
        FragmentManager fm = getFragmentManager();
        // Just return because another FragmentTransaction has been invoked just now
        if (fm.getBackStackEntryCount() > 0 && fm.findFragmentByTag(AccountCheckSettingsFragment.TAG)!= null) {
            return;
        }
        AccountCheckSettingsFragment checkerFragment =
            AccountCheckSettingsFragment.newInstance(checkMode, target);
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.add(checkerFragment, AccountCheckSettingsFragment.TAG);
        transaction.addToBackStack("back");
        transaction.commit();
        fm.executePendingTransactions();
    }

    /**
     * Implements AccountServerBaseFragment.Callback
     */
    public void onEnableProceedButtons(boolean enable) {
        mNextButtonEnabled = enable;
        mNextButton.setEnabled(enable);
    }

    /**
     * Implements AccountServerBaseFragment.Callback
     *
     * If the checked settings are OK, proceed to outgoing settings screen
     */
    public void onCheckSettingsComplete(int result, int setupMode) {
        if (result == AccountCheckSettingsFragment.CHECK_SETTINGS_OK) {
            AccountSetupOutgoing.actionOutgoingSettings(this, SetupData.getFlowMode(),
                    SetupData.getAccount());
            finish();
        }
    }
}
