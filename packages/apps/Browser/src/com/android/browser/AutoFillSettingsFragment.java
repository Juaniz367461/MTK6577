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

package com.android.browser;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings.AutoFillProfile;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AutoFillSettingsFragment extends Fragment {

    private static final String LOGTAG = "AutoFillSettingsFragment";

    private EditText mFullNameEdit;
    private EditText mEmailEdit;
    private EditText mCompanyEdit;
    private EditText mAddressLine1Edit;
    private EditText mAddressLine2Edit;
    private EditText mCityEdit;
    private EditText mStateEdit;
    private EditText mZipEdit;
    private EditText mCountryEdit;
    private EditText mPhoneEdit;

    private Button mSaveButton;

    // Used to display toast after DB interactions complete.
    private Handler mHandler;
    private BrowserSettings mSettings;

    private final static int PROFILE_SAVED_MSG = 100;
    private final static int PROFILE_DELETED_MSG = 101;

    // For now we support just one profile so it's safe to hardcode the
    // id to 1 here. In the future this unique identifier will be set
    // dynamically.
    private int mUniqueId = 1;

    private class PhoneNumberValidator implements TextWatcher {
        // Keep in sync with kPhoneNumberLength in chrome/browser/autofill/phone_number.cc
        private static final int PHONE_NUMBER_LENGTH = 7;
        private static final String PHONE_NUMBER_SEPARATORS_REGEX = "[\\s\\.\\(\\)-]";

        public void afterTextChanged(Editable s) {
            String phoneNumber = s.toString();
            int phoneNumberLength = phoneNumber.length();

            // Strip out any phone number separators.
            phoneNumber = phoneNumber.replaceAll(PHONE_NUMBER_SEPARATORS_REGEX, "");

            int strippedPhoneNumberLength = phoneNumber.length();

            if (phoneNumberLength > 0 && strippedPhoneNumberLength < PHONE_NUMBER_LENGTH) {
                mPhoneEdit.setError(getResources().getText(
                        R.string.autofill_profile_editor_phone_number_invalid));
            } else {
                mPhoneEdit.setError(null);
            }

            updateButtonState();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    private class FieldChangedListener implements TextWatcher {
        public void afterTextChanged(Editable s) {
            updateButtonState();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

    }

    private TextWatcher mFieldChangedListener = new FieldChangedListener();

    public AutoFillSettingsFragment() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Context c = getActivity();
                switch (msg.what) {
                case PROFILE_SAVED_MSG:
                    if (c != null) {
                        Toast.makeText(c, R.string.autofill_profile_successful_save,
                                Toast.LENGTH_SHORT).show();
                        closeEditor();
                    }
                    break;

                case PROFILE_DELETED_MSG:
                    if (c != null) {
                        Toast.makeText(c, R.string.autofill_profile_successful_delete,
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        };
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setHasOptionsMenu(true);
        mSettings = BrowserSettings.getInstance();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.autofill_profile_editor, menu);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        boolean autoHide = getResources().getBoolean(R.bool.auto_hide_keyboard_when_onpause);
        if ( autoHide )
        {
            Context tContext = getActivity();
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(tContext.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.autofill_profile_editor_delete_profile_menu_id) {
            // Clear the UI.
            mFullNameEdit.setText("");
            mEmailEdit.setText("");
            mCompanyEdit.setText("");
            mAddressLine1Edit.setText("");
            mAddressLine2Edit.setText("");
            mCityEdit.setText("");
            mStateEdit.setText("");
            mZipEdit.setText("");
            mCountryEdit.setText("");
            mPhoneEdit.setText("");

            // Update browser settings and native with a null profile. This will
            // trigger the current profile to get deleted from the DB.
            mSettings.setAutoFillProfile(null,
                    mHandler.obtainMessage(PROFILE_DELETED_MSG));
            updateButtonState();
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.autofill_settings_fragment, container, false);

        mFullNameEdit = (EditText)v.findViewById(R.id.autofill_profile_editor_name_edit);
        mEmailEdit = (EditText)v.findViewById(R.id.autofill_profile_editor_email_address_edit);
        mCompanyEdit = (EditText)v.findViewById(R.id.autofill_profile_editor_company_name_edit);
        mAddressLine1Edit = (EditText)v.findViewById(
                R.id.autofill_profile_editor_address_line_1_edit);
        mAddressLine2Edit = (EditText)v.findViewById(
                R.id.autofill_profile_editor_address_line_2_edit);
        mCityEdit = (EditText)v.findViewById(R.id.autofill_profile_editor_city_edit);
        mStateEdit = (EditText)v.findViewById(R.id.autofill_profile_editor_state_edit);
        mZipEdit = (EditText)v.findViewById(R.id.autofill_profile_editor_zip_code_edit);
        mCountryEdit = (EditText)v.findViewById(R.id.autofill_profile_editor_country_edit);
        mPhoneEdit = (EditText)v.findViewById(R.id.autofill_profile_editor_phone_number_edit);

        mFullNameEdit.addTextChangedListener(mFieldChangedListener);
        mEmailEdit.addTextChangedListener(mFieldChangedListener);
        mCompanyEdit.addTextChangedListener(mFieldChangedListener);
        mAddressLine1Edit.addTextChangedListener(mFieldChangedListener);
        mAddressLine2Edit.addTextChangedListener(mFieldChangedListener);
        mCityEdit.addTextChangedListener(mFieldChangedListener);
        mStateEdit.addTextChangedListener(mFieldChangedListener);
        mZipEdit.addTextChangedListener(mFieldChangedListener);
        mCountryEdit.addTextChangedListener(mFieldChangedListener);
        mPhoneEdit.addTextChangedListener(new PhoneNumberValidator());

        mSaveButton = (Button)v.findViewById(R.id.autofill_profile_editor_save_button);
        mSaveButton.setOnClickListener(new OnClickListener() {
            public void onClick(View button) {
                AutoFillProfile newProfile = new AutoFillProfile(
                        mUniqueId,
                        mFullNameEdit.getText().toString(),
                        mEmailEdit.getText().toString(),
                        mCompanyEdit.getText().toString(),
                        mAddressLine1Edit.getText().toString(),
                        mAddressLine2Edit.getText().toString(),
                        mCityEdit.getText().toString(),
                        mStateEdit.getText().toString(),
                        mZipEdit.getText().toString(),
                        mCountryEdit.getText().toString(),
                        mPhoneEdit.getText().toString());

                mSettings.setAutoFillProfile(newProfile,
                        mHandler.obtainMessage(PROFILE_SAVED_MSG));
            }
        });

        // Populate the text boxes with any pre existing AutoFill data.
        AutoFillProfile activeProfile = mSettings.getAutoFillProfile();
        if (activeProfile != null) {
            mFullNameEdit.setText(activeProfile.getFullName());
            mEmailEdit.setText(activeProfile.getEmailAddress());
            mCompanyEdit.setText(activeProfile.getCompanyName());
            mAddressLine1Edit.setText(activeProfile.getAddressLine1());
            mAddressLine2Edit.setText(activeProfile.getAddressLine2());
            mCityEdit.setText(activeProfile.getCity());
            mStateEdit.setText(activeProfile.getState());
            mZipEdit.setText(activeProfile.getZipCode());
            mCountryEdit.setText(activeProfile.getCountry());
            mPhoneEdit.setText(activeProfile.getPhoneNumber());
        }

        updateButtonState();

        return v;
    }

    public void updateButtonState() {

        boolean valid = (mFullNameEdit.getText().toString().length() > 0 ||
            mEmailEdit.getText().toString().length() > 0 ||
            mCompanyEdit.getText().toString().length() > 0 ||
            mAddressLine1Edit.getText().toString().length() > 0 ||
            mAddressLine2Edit.getText().toString().length() > 0 ||
            mCityEdit.getText().toString().length() > 0 ||
            mStateEdit.getText().toString().length() > 0 ||
            mZipEdit.getText().toString().length() > 0 ||
            mCountryEdit.getText().toString().length() > 0) &&
            mPhoneEdit.getError() == null;

        // Only enable the save buttons if we have at least one field completed
        // and the phone number (if present is valid).
        mSaveButton.setEnabled(valid);
    }

    private void closeEditor() {
        // Hide the IME if the user wants to close while an EditText has focus
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            getActivity().finish();
        }
    }
}
