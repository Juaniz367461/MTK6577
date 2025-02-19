package com.android.phone;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.provider.Settings;
//import com.mediatek.CellConnService.CellConnMgr;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.cdma.TtyIntent;
import com.android.internal.telephony.Phone;
import com.mediatek.xlog.Xlog;

import com.mediatek.featureoption.FeatureOption;

public class OthersSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener
{
    private static final String BUTTON_OTHERS_FDN_KEY     = "button_fdn_key";
    private static final String BUTTON_OTHERS_MINUTE_REMINDER_KEY    = "minute_reminder_key";
    private static final String BUTTON_OTHERS_DUAL_MIC_KEY = "dual_mic_key";
    private static final String BUTTON_TTY_KEY    = "button_tty_mode_key";
    
    // preferred TTY mode
    // Phone.TTY_MODE_xxx
    static final int preferredTtyMode = Phone.TTY_MODE_OFF;
    
    private static final String LOG_TAG = "Settings/OthersSettings";
    private Preference mButtonFdn;
    private CheckBoxPreference mButtonMr;
    private CheckBoxPreference mButtonDualMic;
    private ListPreference mButtonTTY;
    
    private int mSimId = 0;
    private boolean isOnlyOneSim = false;
    PreCheckForRunning preCfr = null;
    
    /*private CellConnMgr mCellConnMgr;
    private ServiceComplete mServiceComplete;
    
    class ServiceComplete implements Runnable {
        public void run() {
            int result = mCellConnMgr.getResult();
            Xlog.d(LOG_TAG, "ServiceComplete with the result = " + CellConnMgr.resultToString(result));
            if (CellConnMgr.RESULT_OK == result) {
                startActivity(intent);
            }
        }
        
        public void setIntent(Intent it)
        {
            intent = it;
        }
        private Intent intent;
    }*/
    
    @Override
    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.others_settings);
        
        mButtonFdn = findPreference(BUTTON_OTHERS_FDN_KEY);
        mButtonMr = (CheckBoxPreference)findPreference(BUTTON_OTHERS_MINUTE_REMINDER_KEY);
        mButtonDualMic = (CheckBoxPreference)findPreference(BUTTON_OTHERS_DUAL_MIC_KEY);
        if (!PhoneUtils.isSupportFeature("DUAL_MIC")) {
            this.getPreferenceScreen().removePreference(mButtonDualMic);
        }
	/** this commen is by lemon mtk54102	
		if (true == FeatureOption.MTK_VT3G324M_SUPPORT) {
			// MTK_OP01_PROTECT_START
			if (!"OP01".equals(PhoneUtils.getOptrProperties()))
				// MTK_OP01_PROTECT_END
				this.getPreferenceScreen().removePreference(
						findPreference("auto_reject_setting_key"));
		} else {
			this.getPreferenceScreen().removePreference(
					findPreference("auto_reject_setting_key"));
		}
		*/

		if(true){
			this.getPreferenceScreen().removePreference(findPreference("auto_reject_setting_key"));
		}
        
		// MTK_OP01_PROTECT_START
		if (!"OP01".equals(PhoneUtils.getOptrProperties())){
			this.getPreferenceScreen().removePreference(findPreference("call_reject"));
   		}
		// MTK_OP01_PROTECT_END
        
        if (mButtonMr != null)
        {
            mButtonMr.setOnPreferenceChangeListener(this);
        }
        
        if (mButtonDualMic != null)
        {
            mButtonDualMic.setOnPreferenceChangeListener(this);
        }
        mButtonTTY = (ListPreference) findPreference(BUTTON_TTY_KEY);

        if (mButtonTTY != null) {
            if (PhoneUtils.isSupportFeature("TTY")) {
                mButtonTTY.setOnPreferenceChangeListener(this);
            } else {
            	getPreferenceScreen().removePreference(mButtonTTY);
                mButtonTTY = null;
            }
        }
        /*mServiceComplete = new ServiceComplete();
        mCellConnMgr = new CellConnMgr(mServiceComplete);
        mCellConnMgr.register(getApplicationContext());*/
        preCfr = new PreCheckForRunning(this);
        
        if (CallSettings.isMultipleSim()) {
            List<SIMInfo> list = SIMInfo.getInsertedSIMList(this);
            if (list.size() == 1) {
                this.isOnlyOneSim = true;
                this.mSimId = list.get(0).mSlot;
            }
        } else {
            this.isOnlyOneSim = true;
            this.mSimId = 0;
        }
        
        preCfr.byPass = !isOnlyOneSim;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (!CallSettings.isMultipleSim()) {
            if (preference == mButtonFdn) {
                Intent intent = new Intent(this, FdnSetting2.class);
                preCfr.checkToRun(intent, this.mSimId, 302);
                return true;
            } else if (preference == mButtonTTY) {
                return true;
            }
            return false;
        }
        
        if (preference == mButtonFdn) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            //intent.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.FdnSetting2");
            //this.startActivity(intent);
            preCfr.checkToRun(intent, this.mSimId, 302);
            return true;
        }
        
        return false;
    }
    
     public boolean onPreferenceChange(Preference preference, Object objValue) {
         if (preference == mButtonMr) {
             if (mButtonMr.isChecked()){
                 Xlog.d("OthersSettings", "onPreferenceChange mButtonReminder turn on"); 
                 mButtonMr.setSummary(getString(R.string.minutereminder_turnon));
             }else{
                 Xlog.d("OthersSettings", "onPreferenceChange mButtonReminder turn off"); 
                 mButtonMr.setSummary(getString(R.string.minutereminder_turnoff));
             }
         } else if (preference == mButtonDualMic) {
             if (mButtonDualMic.isChecked()){
                 Xlog.d(LOG_TAG, "onPreferenceChange mButtonDualmic turn on"); 
                 //mButtonDualMic.setSummary(getString(R.string.dual_mic_turnoff));
                 PhoneUtils.setDualMicMode("0");
             }else{
                 Xlog.d(LOG_TAG, "onPreferenceChange mButtonDualmic turn off"); 
                 //mButtonDualMic.setSummary(getString(R.string.dual_mic_turnon));
                 PhoneUtils.setDualMicMode("1");
             }
         } else if (preference == mButtonTTY) {
             handleTTYChange(preference, objValue);
         }
         
         return true;
     }
     
     public void onResume() {
         super.onResume();
         boolean isRadioOn = CallSettings.isRadioOn(mSimId);
         if (isOnlyOneSim && !isRadioOn) { 
             mButtonFdn.setEnabled(false);
         }
         
         if (CallSettings.isMultipleSim()) {
             List<SIMInfo> insertSim = SIMInfo.getInsertedSIMList(this);
             if (insertSim.size() == 0) {
                 mButtonFdn.setEnabled(false);
             }
         } else {
             boolean bIccExist = TelephonyManager.getDefault().hasIccCard();
             if (!bIccExist) {
                 if (mButtonFdn != null)  mButtonFdn.setEnabled(false);
             }
         }

         if (mButtonTTY != null) {
             int settingsTtyMode = Settings.Secure.getInt(getContentResolver(),
                     Settings.Secure.PREFERRED_TTY_MODE,
                     Phone.TTY_MODE_OFF);
             mButtonTTY.setValue(Integer.toString(settingsTtyMode));
             updatePreferredTtyModeSummary(settingsTtyMode);
         }
     }
     
     
     protected void onDestroy() {
         super.onDestroy();
         //mCellConnMgr.unregister();
         if (preCfr != null) {
             preCfr.deRegister();
         }
     }
    public static void goUpToTopLevelSetting(Activity activity) {
        Intent intent = new Intent(activity, OthersSettings.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
    
    private void handleTTYChange(Preference preference, Object objValue) {
        int buttonTtyMode;
        buttonTtyMode = Integer.valueOf((String) objValue).intValue();
        int settingsTtyMode = android.provider.Settings.Secure.getInt(
                getContentResolver(),
                android.provider.Settings.Secure.PREFERRED_TTY_MODE, preferredTtyMode);
        Xlog.d(LOG_TAG, "handleTTYChange: requesting set TTY mode enable (TTY) to" +
                Integer.toString(buttonTtyMode));

        if (buttonTtyMode != settingsTtyMode) {
            switch(buttonTtyMode) {
            case Phone.TTY_MODE_OFF:
            case Phone.TTY_MODE_FULL:
            case Phone.TTY_MODE_HCO:
            case Phone.TTY_MODE_VCO:
                android.provider.Settings.Secure.putInt(getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_TTY_MODE, buttonTtyMode);
                break;
            default:
                buttonTtyMode = Phone.TTY_MODE_OFF;
            }

            mButtonTTY.setValue(Integer.toString(buttonTtyMode));
            updatePreferredTtyModeSummary(buttonTtyMode);
            Intent ttyModeChanged = new Intent(TtyIntent.TTY_PREFERRED_MODE_CHANGE_ACTION);
            ttyModeChanged.putExtra(TtyIntent.TTY_PREFFERED_MODE, buttonTtyMode);
            sendBroadcast(ttyModeChanged);
        }
    }
    
    private void updatePreferredTtyModeSummary(int TtyMode) {
        String [] txts = getResources().getStringArray(R.array.tty_mode_entries);
        switch(TtyMode) {
            case Phone.TTY_MODE_OFF:
            case Phone.TTY_MODE_HCO:
            case Phone.TTY_MODE_VCO:
            case Phone.TTY_MODE_FULL:
                mButtonTTY.setSummary(txts[TtyMode]);
                break;
            default:
                mButtonTTY.setEnabled(false);
                mButtonTTY.setSummary(txts[Phone.TTY_MODE_OFF]);
        }
    }
}