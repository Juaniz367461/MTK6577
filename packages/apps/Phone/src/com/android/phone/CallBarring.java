package com.android.phone;

import java.util.ArrayList;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.mediatek.xlog.Xlog;
import com.android.internal.telephony.CommandsInterface;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

public class CallBarring extends TimeConsumingPreferenceActivity implements
        CallBarringInterface {
    private static final String LOG_TAG = "Settings/CallBarring";
    private static final boolean DBG = true; //(PhoneApp.DBG_LEVEL >= 2);

    private CallBarringBasePreference mCallAllOutButton;
    private CallBarringBasePreference mCallInternationalOutButton;
    private CallBarringBasePreference mCallInternationalOutButton2;
    private CallBarringBasePreference mCallInButton;
    private CallBarringBasePreference mCallInButton2;
    private CallBarringResetPreference mCallCancel;
    private CallBarringChangePassword mCallChangePassword;

    private static final String BUTTON_CALL_BARRING_KEY = "all_outing_key";
    private static final String BUTTON_ALL_OUTING_KEY = "all_outing_international_key";
    private static final String BUTTON_OUT_INTERNATIONAL_EXCEPT = "all_outing_except_key";
    private static final String BUTTON_ALL_INCOMING_KEY = "all_incoming_key";
    private static final String BUTTON_ALL_INCOMING_EXCEPT = "all_incoming_except_key";
    private static final String BUTTON_DEACTIVATE_KEY = "deactivate_all_key";
    private static final String BUTTON_CHANGE_PASSWORD_KEY = "change_password_key";

    private ArrayList<Preference> mPreferences = new ArrayList<Preference>();
    private ArrayList<Preference> mCheckedPreferences = new ArrayList<Preference>();
    private int mInitIndex = 0;
    private int mResetIndex = 0;
    private String mPassword = null;
    private int mErrorState = 0;
    private boolean bFirstResume = false;

/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */

    private int mSimId = DEFAULT_SIM;
/* Fion add end */

    private boolean isVtSetting = false;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
            PhoneApp app = PhoneApp.getInstance();
            mSimId = getIntent().getIntExtra(app.phone.GEMINI_SIM_ID_KEY, -1);
        }
        isVtSetting = getIntent().getBooleanExtra("ISVT", false);
        Xlog.d("CallBarring", "Sim Id : " + mSimId + " ISVT = " + isVtSetting);		
/* Fion add end */

        addPreferencesFromResource(R.xml.call_barring_options);

        PreferenceScreen prefSet = getPreferenceScreen();
        mCallAllOutButton = (CallBarringBasePreference) prefSet
                .findPreference(BUTTON_CALL_BARRING_KEY);
        mCallInternationalOutButton = (CallBarringBasePreference) prefSet
                .findPreference(BUTTON_ALL_OUTING_KEY);
        mCallInternationalOutButton2 = (CallBarringBasePreference) prefSet
                .findPreference(BUTTON_OUT_INTERNATIONAL_EXCEPT);
        mCallInButton = (CallBarringBasePreference) prefSet
                .findPreference(BUTTON_ALL_INCOMING_KEY);
        mCallInButton2 = (CallBarringBasePreference) prefSet
                .findPreference(BUTTON_ALL_INCOMING_EXCEPT);

        mCallCancel = (CallBarringResetPreference) prefSet
                .findPreference(BUTTON_DEACTIVATE_KEY);
        mCallChangePassword = (CallBarringChangePassword) prefSet
                .findPreference(BUTTON_CHANGE_PASSWORD_KEY);

        initial();
        mPreferences.add(mCallAllOutButton);
        mPreferences.add(mCallInternationalOutButton);
        mPreferences.add(mCallInternationalOutButton2);
        mPreferences.add(mCallInButton);
        mPreferences.add(mCallInButton2);

        mCallAllOutButton.setRefreshInterface(this);
        mCallInternationalOutButton.setRefreshInterface(this);
        mCallInternationalOutButton2.setRefreshInterface(this);
        mCallInButton.setRefreshInterface(this);
        mCallInButton2.setRefreshInterface(this);
/* Fion add start */		
        mCallCancel.setCallBarringInterface(this, mSimId);
        mCallChangePassword.setTimeConsumingListener(this, mSimId);
/* Fion add end */
        /*if (icicle == null) {
            if (DBG)
                Xlog.d(LOG_TAG, "start to init ");
            Preference p = mPreferences.get(0);
            doGetCallState(p);
        } else {
            // TODO
        }*/
        

        if (null != getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME))
        {
            setTitle(getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME));
        }
        
        if (isVtSetting)
        {
	        mCallAllOutButton.setServiceClass(CommandsInterface.SERVICE_CLASS_VIDEO);
	        mCallInternationalOutButton.setServiceClass(CommandsInterface.SERVICE_CLASS_VIDEO);
	        mCallInternationalOutButton2.setServiceClass(CommandsInterface.SERVICE_CLASS_VIDEO);
	        mCallInButton.setServiceClass(CommandsInterface.SERVICE_CLASS_VIDEO);
	        mCallInButton2.setServiceClass(CommandsInterface.SERVICE_CLASS_VIDEO);
	        mCallCancel.setServiceClass(CommandsInterface.SERVICE_CLASS_VIDEO);
        }
        
        bFirstResume = true;
        }
    
    public void onResume()
    {
        super.onResume();

        if (bFirstResume == true){

            bFirstResume = false;
            startUpdate();
        } else if(PhoneUtils.getMmiFinished() == true) {
        	startUpdate();
        }
    }
    
    private void startUpdate() {
        mInitIndex = 0;
        Preference p = mPreferences.get(mInitIndex);
        if(p != null) {
            doGetCallState(p);
    		PhoneUtils.setMmiFinished(false);
        }
    }
    
    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        if(EXCEPTION_ERROR == getErrorState() ){
            this.finish();
        }
    }
    private void initial() {
        mCallAllOutButton.setmFacility(CommandsInterface.CB_FACILITY_BAOC);
        mCallAllOutButton.setmTitle(R.string.lable_all_outgoing);

        mCallInternationalOutButton
                .setmFacility(CommandsInterface.CB_FACILITY_BAOIC);
        mCallInternationalOutButton.setmTitle(R.string.lable_all_outgoing_international);

        mCallInternationalOutButton2
                .setmFacility(CommandsInterface.CB_FACILITY_BAOICxH);
        mCallInternationalOutButton2
                .setmTitle(R.string.lable_all_out_except);

        mCallInButton.setmFacility(CommandsInterface.CB_FACILITY_BAIC);
        mCallInButton.setmTitle(R.string.lable_all_incoming);

        mCallInButton2.setmFacility(CommandsInterface.CB_FACILITY_BAICr);
        mCallInButton2.setmTitle(R.string.lable_incoming_calls_except);
        mCallCancel.setListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO
    }

    private void doGetCallState(Preference p) {
        if (p instanceof CallBarringBasePreference) {
/* Fion add start */
            ((CallBarringBasePreference) p).init(this, false, mSimId);
/* Fion add end */
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        // Init process.
        if (mInitIndex < mPreferences.size() - 1 && !isFinishing()) {
            CallBarringBasePreference cb = (CallBarringBasePreference)mPreferences.get(mInitIndex);
            if (cb.isSuccess())
            {
            mInitIndex++;
            if (DBG)
                Xlog.i(LOG_TAG, "onFinished() is called (init part) mInitIndex is "
                        + mInitIndex + "is reading?  " + reading);
            Preference p = mPreferences.get(mInitIndex);
            doGetCallState(p);
        }
            else
            {
                for (int i = mInitIndex; i < mPreferences.size(); ++i)
                {
                    mPreferences.get(i).setEnabled(false);
                }
                mInitIndex = mPreferences.size();
            }
        }
        super.onFinished(preference, reading);
    }


    private void doSetCallState(Preference p, String password) {
        if (p instanceof CallBarringBasePreference) {
            CallBarringBasePreference cp = (CallBarringBasePreference) p;
            if (DBG)
                Xlog.i(LOG_TAG, "doSetCallState() is called");
            cp.setCallState(password);
        }
    }

    public void doCancelAllState(){
        String summary;
        summary = mCallAllOutButton.getContext().getString(R.string.lable_disable);
        mCallAllOutButton.setSummary(summary);
        mCallAllOutButton.setChecked(false);
        mCallInternationalOutButton.setSummary(summary);
        mCallInternationalOutButton.setChecked(false);
        mCallInternationalOutButton2.setSummary(summary);
        mCallInternationalOutButton2.setChecked(false);
        mCallInButton.setSummary(summary);
        mCallInButton.setChecked(false);
        mCallInButton2.setSummary(summary);
        mCallInButton2.setChecked(false);
    }
    public void doCallBarringRefresh(String state){
        String summary;
        summary = mCallAllOutButton.getContext().getString(R.string.lable_disable);
        if (state.equals(CommandsInterface.CB_FACILITY_BAOC)){
            mCallInternationalOutButton.setSummary(summary);
            mCallInternationalOutButton.setChecked(false);
            mCallInternationalOutButton2.setSummary(summary);
            mCallInternationalOutButton2.setChecked(false);
        }
        
        if (state.equals(CommandsInterface.CB_FACILITY_BAOIC)){
            mCallAllOutButton.setSummary(summary);
            mCallAllOutButton.setChecked(false);
            mCallInternationalOutButton2.setSummary(summary);
            mCallInternationalOutButton2.setChecked(false);
        }
        
        if (state.equals(CommandsInterface.CB_FACILITY_BAOICxH)){
            mCallAllOutButton.setSummary(summary);
            mCallAllOutButton.setChecked(false);
            mCallInternationalOutButton.setSummary(summary);
            mCallInternationalOutButton.setChecked(false);
        }
        
        if (state.equals(CommandsInterface.CB_FACILITY_BAIC)){
            mCallInButton2.setSummary(summary);
            mCallInButton2.setChecked(false);
        }
        
        if (state.equals(CommandsInterface.CB_FACILITY_BAICr)){
            mCallInButton.setSummary(summary);
            mCallInButton.setChecked(false);
        }
    }
    
    public int getErrorState(){
    	return mErrorState;
    }
    
    public void setErrorState(int state){
    	mErrorState = state;
    }
    
    public void resetIndex(int i)
    {
        mInitIndex = i;   
    }
}
