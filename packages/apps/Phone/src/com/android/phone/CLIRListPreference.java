package com.android.phone;

import static com.android.phone.TimeConsumingPreferenceActivity.EXCEPTION_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;
import static com.android.phone.TimeConsumingPreferenceActivity.FDN_FAIL;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.util.AttributeSet;
import com.mediatek.xlog.Xlog;

/* Fion add start */
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.featureoption.FeatureOption;
/* Fion add end */


public class CLIRListPreference extends ListPreference {
    private static final String LOG_TAG = "Settings/CLIRListPreference";
    private final boolean DBG = true; //(PhoneApp.DBG_LEVEL >= 2);

    private MyHandler mHandler = new MyHandler();
    Phone phone;
    TimeConsumingPreferenceListener tcpListener;

/* Fion add start */
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */
    private int mSimId = DEFAULT_SIM;
/* Fion add end */

    int clirArray[];

    public CLIRListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        phone = PhoneApp.getPhone();
    }

    public CLIRListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

/* Fion add start */
            if (CallSettings.isMultipleSim())
            {
                ((GeminiPhone)phone).setOutgoingCallerIdDisplayGemini(findIndexOfValue(getValue()),
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_CLIR), mSimId);
            }
            else
            {
        phone.setOutgoingCallerIdDisplay(findIndexOfValue(getValue()),
                mHandler.obtainMessage(MyHandler.MESSAGE_SET_CLIR));
            }
/* Fion add end */
        if (tcpListener != null) {
            tcpListener.onStarted(this, false);
        }
    }

/* Fion add start */
    void init(TimeConsumingPreferenceListener listener, boolean skipReading, int simId) {
        Xlog.d(LOG_TAG,"init, simId = "+simId);

        tcpListener = listener;
        mSimId = simId;
		
        if (!skipReading) {
/* Fion add start */
            if (CallSettings.isMultipleSim())
            {
                ((GeminiPhone)phone).getOutgoingCallerIdDisplayGemini(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CLIR,
                        MyHandler.MESSAGE_GET_CLIR, MyHandler.MESSAGE_GET_CLIR), mSimId);
            }
            else
            {
            phone.getOutgoingCallerIdDisplay(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CLIR,
                    MyHandler.MESSAGE_GET_CLIR, MyHandler.MESSAGE_GET_CLIR));
            }
/* Fion add end */
            if (tcpListener != null) {
                tcpListener.onStarted(this, true);
            }
        }
    }
/* Fion add end */

    void handleGetCLIRResult(int tmpClirArray[]) {
        clirArray = tmpClirArray;
        final boolean enabled = tmpClirArray[1] == 1 || tmpClirArray[1] == 3 || tmpClirArray[1] == 4;
        setEnabled(enabled);

        // set the value of the preference based upon the clirArgs.
        int value = CommandsInterface.CLIR_DEFAULT;
        switch (tmpClirArray[1]) {
            case 1: // Permanently provisioned
            case 3: // Temporary presentation disallowed
            case 4: // Temporary presentation allowed
                switch (tmpClirArray[0]) {
                    case 1: // CLIR invoked
                        value = CommandsInterface.CLIR_INVOCATION;
                        break;
                    case 2: // CLIR suppressed
                        value = CommandsInterface.CLIR_SUPPRESSION;
                        break;
                    case 0: // Network default
                    default:
                        value = CommandsInterface.CLIR_DEFAULT;
                        break;
                }
                break;
            case 0: // Not Provisioned
            case 2: // Unknown (network error, etc)
            default:
                value = CommandsInterface.CLIR_DEFAULT;
                break;
        }
        setValueIndex(value);

        // set the string summary to reflect the value
        int summary = R.string.sum_default_caller_id;
        switch (value) {
            case CommandsInterface.CLIR_SUPPRESSION:
                summary = R.string.sum_show_caller_id;
                break;
            case CommandsInterface.CLIR_INVOCATION:
                summary = R.string.sum_hide_caller_id;
                break;
            case CommandsInterface.CLIR_DEFAULT:
                summary = R.string.sum_default_caller_id;
                break;
        }
        setSummary(summary);
    }

    private class MyHandler extends Handler {
        private static final int MESSAGE_GET_CLIR = 0;
        private static final int MESSAGE_SET_CLIR = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_CLIR:
                    handleGetCLIRResponse(msg);
                    break;
                case MESSAGE_SET_CLIR:
                    handleSetCLIRResponse(msg);
                    break;
            }
        }

        private void handleGetCLIRResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (msg.arg2 == MESSAGE_SET_CLIR) {
                tcpListener.onFinished(CLIRListPreference.this, false);
            } else {
                tcpListener.onFinished(CLIRListPreference.this, true);
            }
            clirArray = null;
            if (ar.exception != null) {
                if (DBG) Xlog.d(LOG_TAG, "handleGetCLIRResponse: ar.exception="+ar.exception);
                setEnabled(false);
                tcpListener.onException(CLIRListPreference.this, (CommandException) ar.exception);
            } else if (ar.userObj instanceof Throwable) {
                tcpListener.onError(CLIRListPreference.this, RESPONSE_ERROR);
            } else {
                int clirArray[] = (int[]) ar.result;
                if (clirArray.length != 2) {
                    tcpListener.onError(CLIRListPreference.this, RESPONSE_ERROR);
                } else {
                    if (DBG) Xlog.d(LOG_TAG, "handleGetCLIRResponse: CLIR successfully queried, clirArray[0]="
                            + clirArray[0] + ", clirArray[1]=" + clirArray[1]);
                    handleGetCLIRResult(clirArray);
                }
            }
        }

        private void handleSetCLIRResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception != null) {
                if (DBG) Xlog.d(LOG_TAG, "handleSetCallWaitingResponse: ar.exception="+ar.exception);
                //setEnabled(false);
            }
            if (DBG) Xlog.d(LOG_TAG, "handleSetCallWaitingResponse: re get");

/* Fion add start */
            if (CallSettings.isMultipleSim())
            {
                ((GeminiPhone)phone).getOutgoingCallerIdDisplayGemini(obtainMessage(MESSAGE_GET_CLIR,
                        MESSAGE_SET_CLIR, MESSAGE_SET_CLIR, ar.exception), mSimId);
            }
            else
            {
            phone.getOutgoingCallerIdDisplay(obtainMessage(MESSAGE_GET_CLIR,
                    MESSAGE_SET_CLIR, MESSAGE_SET_CLIR, ar.exception));
        }
/* Fion add end */
        }
    }
}