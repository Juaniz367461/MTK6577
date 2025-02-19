package com.android.phone;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.EditPhoneNumberPreference;
import com.android.phone.PhoneApp;

import com.android.internal.telephony.Phone;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import com.android.internal.telephony.CommandsInterface;
import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.FDN_FAIL;
import static com.android.phone.TimeConsumingPreferenceActivity.PASSWORD_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

	
public class CallBarringResetPreference extends Preference implements
        OnPreferenceClickListener {
    private static final String LOG_TAG = "CallBarringResetPreference";
    private static final boolean DBG = true; //(PhoneApp.DBG_LEVEL >= 2);

    private Context mContext;
    private CallBarringInterface mCallBarringInterface = null;
    private static final int PASSWORD_LENGTH = 4;
    private MyHandler mHandler = new MyHandler();
    private TimeConsumingPreferenceListener tcpListener;
    private Phone phone;
    
/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */
    private int mSimId = DEFAULT_SIM;
/* Fion add end */
    
    private int mServiceClass = CommandsInterface.SERVICE_CLASS_VOICE;

    public CallBarringResetPreference(Context context) {
        this(context, null);
        init(context);
        phone = PhoneApp.getPhone();
    }

    public CallBarringResetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        phone = PhoneApp.getPhone();
    }

    public void setListener(TimeConsumingPreferenceListener listener){
        tcpListener = listener;
    }

/* Fion add start */	
    public void setCallBarringInterface(CallBarringInterface i, int simId) {
        mCallBarringInterface = i;
        mSimId = simId;
    }
/* Fion add end */

    public boolean onPreferenceClick(Preference preference) {
        doPreferenceClick(mContext.getString(R.string.deactivate_all));
        return true;
    }

    public void doPreferenceClick(final String title) {
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        final View textEntryView = inflater.inflate(
                R.layout.callbarring_option, null);
        TextView content = (TextView) textEntryView.findViewById(R.id.ViewTop);
        content.setText(mContext
                .getString(R.string.enter_callbarring_password));

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setView(textEntryView);
        builder.setTitle(title);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText passwordText = (EditText) textEntryView
                                .findViewById(R.id.EditPassword);
                        String password = passwordText.getText().toString();
                        if (!validatePassword(password)) {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(
                                    mContext);
                            builder1.setTitle(title);
                            builder1.setMessage(mContext
                                    .getText(R.string.wrong_password));
                            builder1.setCancelable(false);
                            builder1.setPositiveButton(R.string.ok, null);
                            builder1.create().show();
                            return;
                        }
                        // need notify parent the click event.
                        if (mCallBarringInterface != null)
                            setCallState(password);
                    }
                });
        
        AlertDialog dlg = builder.create();
        
        if(dlg != null) {
            Window window = dlg.getWindow();
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dlg.show();
        }
        return;
    }
    
    private boolean validatePassword(String password) {
        if (password == null || password.length() != PASSWORD_LENGTH) 
            return false;
        return true;
    }
    private void init(Context context) {
        mContext = context;
        setEnabled(true);
        setOnPreferenceClickListener(this);
    }
    public void setCallState(String password){
        setCallState(false, password);    
    }    
    private void setCallState(boolean enable, String password) {
        if (DBG)
            Log.i(LOG_TAG, "setCallState() is called " + "password is " + password + "enable is "
                    + enable);
        Message m = mHandler.obtainMessage(
                MyHandler.MESSAGE_SET_CALLBARRING_STATE, 0,
                0, null);

        if (CallSettings.isMultipleSim()) {
            if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                ((GeminiPhone)phone).setVtFacilityLockGemini(CommandsInterface.CB_FACILITY_BA_ALL, enable, password, m, mSimId);
            } else {
                ((GeminiPhone)phone).setFacilityLockGemini(CommandsInterface.CB_FACILITY_BA_ALL, enable, password, m, mSimId);
            }
        } else {
            if (mServiceClass == CommandsInterface.SERVICE_CLASS_VIDEO) {
                phone.setVtFacilityLock(CommandsInterface.CB_FACILITY_BA_ALL, enable, password, m);
            } else {
                phone.setFacilityLock(CommandsInterface.CB_FACILITY_BA_ALL, enable, password, m);
            }
        }

        if (tcpListener != null) {
            tcpListener.onStarted(this, false);
        }
    }
    
    private class MyHandler extends Handler {
        private static final int MESSAGE_SET_CALLBARRING_STATE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_SET_CALLBARRING_STATE:
                tcpListener.onFinished(CallBarringResetPreference.this, false);
                handleSetCallBarringResponse(msg);
                break;
            }
        }
        
        private void handleSetCallBarringResponse(Message msg) {
            int errorid;
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                if (DBG)
                    Log.i(LOG_TAG, "handleSetCallBarringResponse: ar.exception="
                            + ar.exception);
                // TODO password error   
                CommandException ce = (CommandException) ar.exception;
                if (ce.getCommandError() == CommandException.Error.PASSWORD_INCORRECT){
                	errorid =  PASSWORD_ERROR;
                }else if (ce.getCommandError() == CommandException.Error.FDN_CHECK_FAILURE){
                	errorid = FDN_FAIL;
                }else{
                	errorid = EXCEPTION_ERROR;
                }
                mCallBarringInterface.setErrorState(errorid);
                tcpListener.onError(CallBarringResetPreference.this, errorid);
                // TODO Other error
            } else {
                // TODO Maybe this password should be the input password.
                if (DBG)
                    Log.i(LOG_TAG,
                            "handleSetCallBarringResponse is called without exception");
                //mCallBarringInterface.doCancelAllState();
                if (mCallBarringInterface instanceof CallBarring)
                {
                    CallBarring cb = (CallBarring)mCallBarringInterface;
                    PreferenceScreen prefSet = cb.getPreferenceScreen();
                    CallBarringBasePreference mCallAllOutButton = (CallBarringBasePreference) prefSet
                    .findPreference("all_outing_key");
                    
                    if (mCallAllOutButton != null)
                    {
                        cb.resetIndex(0);
                        mCallAllOutButton.init(cb, false, mSimId);
                    }
                }
                else
                {
                mCallBarringInterface.doCancelAllState();
            }
        }

       }
    }
    
    public void setServiceClass(int serviceClass)
    {
    	mServiceClass = serviceClass;
    }
}
