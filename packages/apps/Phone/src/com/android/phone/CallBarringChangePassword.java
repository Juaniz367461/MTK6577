package com.android.phone;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.PhoneApp;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.Toast;
import com.mediatek.xlog.Xlog;

import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.FDN_FAIL;
import static com.android.phone.TimeConsumingPreferenceActivity.PASSWORD_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;


/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */

public class CallBarringChangePassword extends EditPinPreference implements
        EditPinPreference.OnPinEnteredListener {
    private static final String LOG_TAG = "Settings/CallChangePassword";
    // private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);
    private static final boolean DBG = true;

    // size limits for the password.
    private static final int PASSWORD_LENGTH = 4;

    private static final int PASSWORD_CHANGE_OLD = 0;
    private static final int PASSWORD_CHANGE_NEW = 1;
    private static final int PASSWORD_CHANGE_REENTER = 2;

    private MyHandler mHandler = new MyHandler();
    private TimeConsumingPreferenceListener tcpListener = null;
    private int mPasswordChangeState;
    private String mOldPassword;
    private String mNewPassword;
    private Context mContext = null;
    private Phone phone;
    private CallBarringInterface mCallBarringInterface = null;

/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */

    private int mSimId = DEFAULT_SIM;
/* Fion add end */


    public CallBarringChangePassword(Context context) {
        this(context, null);
        init(context);
        phone = PhoneApp.getPhone();
    }

    public CallBarringChangePassword(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        phone = PhoneApp.getPhone();
    }

    private final void displayPasswordChangeDialog(int strId,
            boolean shouldDisplay) {
        int msgId = 0;
        switch (mPasswordChangeState) {
        case PASSWORD_CHANGE_OLD:
            msgId = R.string.old_password_label;
            break;
        case PASSWORD_CHANGE_NEW:
            msgId = R.string.new_password_label;
            break;
        case PASSWORD_CHANGE_REENTER:
            msgId = R.string.confirm_password_label;
            break;
        default:
            break;
        }
        if (strId != 0) {
            this.setDialogMessage(this.getContext().getText(msgId) + "\n"
                    + this.getContext().getText(strId));
        } else {
            this.setDialogMessage(msgId);
        }
        // only display if requested.
        if (shouldDisplay) {
            this.showPinDialog();
        }
    }

    private final void resetPasswordChangeState() {
        mPasswordChangeState = PASSWORD_CHANGE_OLD;
        displayPasswordChangeDialog(0, false);
        mOldPassword = mNewPassword = "";
    }

    public void onPinEntered(EditPinPreference preference,
            boolean positiveResult) {
        if (!positiveResult) {
            resetPasswordChangeState();
            return;
        }
        switch (mPasswordChangeState) {
        case PASSWORD_CHANGE_OLD:
            mOldPassword = this.getText();
            this.setText("");
            if (validatePassword(mOldPassword)) {
                mPasswordChangeState = PASSWORD_CHANGE_NEW;
                displayPasswordChangeDialog(0, true);
            } else {
                displayPasswordChangeDialog(R.string.invalid_password, true);
            }
            break;
        case PASSWORD_CHANGE_NEW:
            mNewPassword = this.getText();
            this.setText("");
            if (validatePassword(mNewPassword)) {
                mPasswordChangeState = PASSWORD_CHANGE_REENTER;
                displayPasswordChangeDialog(0, true);
            } else {
                displayPasswordChangeDialog(R.string.invalid_password, true);
            }
            break;
        case PASSWORD_CHANGE_REENTER:
            if (!mNewPassword.equals(this.getText())) {
                mPasswordChangeState = PASSWORD_CHANGE_NEW;
                this.setText("");
                displayPasswordChangeDialog(R.string.mismatch_password, true);
            } else {
                if (tcpListener != null) {
                    tcpListener.onStarted(this, false);
                }
                doChangePassword(mOldPassword, mNewPassword);
                this.setText("");
                resetPasswordChangeState();
            }
            break;
        default:
            break;
        }
        return;
    }

    private void doChangePassword(String oldPassword, String newPassword) {
        if (DBG)
            Xlog.d(LOG_TAG, "doChangePassword() is called with oldPassword is "
                    + oldPassword + "newPassword is " + newPassword);
        Message m = mHandler.obtainMessage(MyHandler.EVENT_PASSWORD_CHANGE, 0,
                MyHandler.EVENT_PASSWORD_CHANGE, null);

/* Fion add start */
        if (CallSettings.isMultipleSim())
        {
            ((GeminiPhone)phone).changeBarringPasswordGemini(CommandsInterface.CB_FACILITY_BA_ALL,
                    oldPassword, newPassword, m, mSimId);
        }
        else
        {
            phone.changeBarringPassword(CommandsInterface.CB_FACILITY_BA_ALL,
                    oldPassword, newPassword, m);
        }
/* Fion add end */
    }

    private boolean validatePassword(String password) {
        if (password == null || password.length() != PASSWORD_LENGTH) 
            return false;
        return true;
    }

    private void init(Context context) {
        mContext = context;
        setEnabled(true);
        setOnPinEnteredListener(this);
        resetPasswordChangeState();
    }

/* Fion add start */
    public void setTimeConsumingListener(
            TimeConsumingPreferenceListener listener, int simId) {
        tcpListener = listener;
        mCallBarringInterface = (CallBarringInterface)listener;
        mSimId = simId;
    }
/* Fion add end */

    /**
     * Display a toast for message, like the rest of the settings.
     */
    private final void displayMessage(int strId) {
        Toast.makeText(mContext, mContext.getString(strId), Toast.LENGTH_SHORT)
                .show();
    }

    private class MyHandler extends Handler {
        private static final int EVENT_PASSWORD_CHANGE = 0;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_PASSWORD_CHANGE:
                handlePasswordChanged(msg);
                break;
            }
        }

        private void handlePasswordChanged(Message msg) {
        	int errorid;
            if (msg.arg2 == EVENT_PASSWORD_CHANGE) {
                if (tcpListener != null) {
                    tcpListener.onFinished(CallBarringChangePassword.this,
                            false);
                }
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                if (DBG)
                    Xlog.i(LOG_TAG, "handlePasswordChanged: ar.exception="
                            + ar.exception);
                if (tcpListener != null) {
                	CommandException ce = (CommandException) ar.exception;
                    if (ce.getCommandError() == CommandException.Error.PASSWORD_INCORRECT){
                    	errorid =  PASSWORD_ERROR;
                    }else if (ce.getCommandError() == CommandException.Error.FDN_CHECK_FAILURE){
                    	errorid = FDN_FAIL;
                    }else{
                    	errorid= EXCEPTION_ERROR;
                    }
                    mCallBarringInterface.setErrorState(errorid);
                    tcpListener.onError(CallBarringChangePassword.this,
                    		errorid);
                }
            } else {
                if (ar.userObj instanceof Throwable) {
                    if (tcpListener != null) {
                        tcpListener.onError(CallBarringChangePassword.this,
                                RESPONSE_ERROR);
                    }
                } else {
                    if (DBG)
                        Xlog.i(LOG_TAG,
                                "handlePasswordChanged is called without exception");
                    displayMessage(R.string.password_changed);
                }
            }
        }
    }
}
