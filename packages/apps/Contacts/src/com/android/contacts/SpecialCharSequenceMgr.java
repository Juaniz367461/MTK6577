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

package com.android.contacts;

import com.android.internal.telephony.ITelephony;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony.Intents;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import com.mediatek.contacts.util.TelephonyUtils;

/**
 * Helper class to listen for some magic character sequences
 * that are handled specially by the dialer.
 *
 * Note the Phone app also handles these sequences too (in a couple of
 * relativly obscure places in the UI), so there's a separate version of
 * this class under apps/Phone.
 *
 * TODO: there's lots of duplicated code between this class and the
 * corresponding class under apps/Phone.  Let's figure out a way to
 * unify these two classes (in the framework? in a common shared library?)
 */
public class SpecialCharSequenceMgr {
    private static final String TAG = "SpecialCharSequenceMgr";
    private static final String MMI_IMEI_DISPLAY = "*#06#";

    /** This class is never instantiated. */
    private SpecialCharSequenceMgr() {
    }

    public static boolean handleChars(Context context, String input, EditText textField) {
        return handleChars(context, input, false, textField);
    }

    public static boolean handleChars(Context context, String input) {
        return handleChars(context, input, false, null);
    }

    public static boolean handleChars(Context context, String input, boolean useSystemWindow,
            EditText textField) {

        //get rid of the separators so that the string gets parsed correctly
        String dialString = PhoneNumberUtils.stripSeparators(input);

        if (handleIMEIDisplay(context, dialString, useSystemWindow)
                || handlePinEntry(context, dialString)
                || handleAdnEntry(context, dialString, textField)
                || handleSecretCode(context, dialString)) {
            return true;
        }

        return false;
    }

    /**
     * Handles secret codes to launch arbitrary activities in the form of *#*#<code>#*#*.
     * If a secret code is encountered an Intent is started with the android_secret_code://<code>
     * URI.
     *
     * @param context the context to use
     * @param input the text to check for a secret code in
     * @return true if a secret code was encountered
     */
    public static boolean handleSecretCode(Context context, String input) {
        // Secret codes are in the form *#*#<code>#*#*
        int len = input.length();
        if (len > 8 && input.startsWith("*#*#") && input.endsWith("#*#*")) {
            Intent intent = new Intent(Intents.SECRET_CODE_ACTION,
                    Uri.parse("android_secret_code://" + input.substring(4, len - 4)));
            context.sendBroadcast(intent);
            return true;
        }

        return false;
    }

    /**
     * Handle ADN requests by filling in the SIM contact number into the requested
     * EditText.
     *
     * This code works alongside the Asynchronous query handler {@link QueryHandler}
     * and query cancel handler implemented in {@link SimContactQueryCookie}.
     */
    public static boolean handleAdnEntry(Context context, String input, EditText textField) {
        /* ADN entries are of the form "N(N)(N)#" */

        // if the phone is keyguard-restricted, then just ignore this
        // input.  We want to make sure that sim card contacts are NOT
        // exposed unless the phone is unlocked, and this code can be
        // accessed from the emergency dialer.
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.inKeyguardRestrictedInputMode()) {
            return false;
        }

        int len = input.length();
        if ((len > 1) && (len < 5) && (input.endsWith("#"))) {
            try {
                // get the ordinal number of the sim contact
                int index = Integer.parseInt(input.substring(0, len-1));

                // The original code that navigated to a SIM Contacts list view did not
                // highlight the requested contact correctly, a requirement for PTCRB
                // certification.  This behaviour is consistent with the UI paradigm
                // for touch-enabled lists, so it does not make sense to try to work
                // around it.  Instead we fill in the the requested phone number into
                // the dialer text field.

                // create the async query handler
                QueryHandler handler = new QueryHandler (context.getContentResolver());

                if(!TelephonyUtils.isRadioOnInner() && !TelephonyUtils.isSimReady()) {
                    Log.d("onQueryComplete","TelephonyUtils.isRadioOnInner(0)  " + TelephonyUtils.isRadioOnInner() 
                            + " TelephonyUtils.isSimReadyInner(0) " + TelephonyUtils.isSimReady());
                    return false;
                }

                // create the cookie object
                SimContactQueryCookie sc = new SimContactQueryCookie(index - 1, handler,
                        ADN_QUERY_TOKEN);

                // setup the cookie fields
                sc.contactNum = index;
                sc.setTextField(textField);

                if (null != textField) {
                    sc.text = textField.getText().toString();
                } else {
                    sc.text = null;
                }

                // create the progress dialog
                sc.progressDialog = new ProgressDialog(context);
                sc.progressDialog.setTitle(R.string.simContacts_title);
                sc.progressDialog.setMessage(context.getText(R.string.simContacts_emptyLoading));
                sc.progressDialog.setIndeterminate(true);
                sc.progressDialog.setCancelable(true);
                sc.progressDialog.setOnCancelListener(sc);
                sc.progressDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

                // display the progress dialog
                sc.progressDialog.show();

                // run the query.
                handler.startQuery(ADN_QUERY_TOKEN, sc, Uri.parse("content://icc/adn"),
                        new String[]{ADN_PHONE_NUMBER_COLUMN_NAME,ADN_INDEX_COLUMN_NAME}, null, null, null);
                return true;
            } catch (NumberFormatException ex) {
                // Ignore
            }
        }
        return false;
    }

    public static boolean handlePinEntry(Context context, String input) {
        if ((input.startsWith("**04") || input.startsWith("**05")) && input.endsWith("#")) {
            try {
                return ITelephony.Stub.asInterface(ServiceManager.getService("phone"))
                        .handlePinMmi(input);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to handlePinMmi due to remote exception");
                return false;
            }
        }
        return false;
    }

    public static boolean handleIMEIDisplay(Context context, String input, boolean useSystemWindow) {
        if (input.equals(MMI_IMEI_DISPLAY)) {
            int phoneType = ((TelephonyManager)context.getSystemService(
                    Context.TELEPHONY_SERVICE)).getCurrentPhoneType();

            if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
                showIMEIPanel(context, useSystemWindow);
                return true;
            } else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                showMEIDPanel(context, useSystemWindow);
                return true;
            }
        }

        return false;
    }

    // TODO: Combine showIMEIPanel() and showMEIDPanel() into a single
    // generic "showDeviceIdPanel()" method, like in the apps/Phone
    // version of SpecialCharSequenceMgr.java.  (This will require moving
    // the phone app's TelephonyCapabilities.getDeviceIdLabel() method
    // into the telephony framework, though.)

    public static void showIMEIPanel(Context context, boolean useSystemWindow) {
        String imeiStr = ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE))
                .getDeviceId();

        /**
         * add by mediatek .inc
         * description : set the imeiStr to 'Invalid'
         * when it's empty
         */
        if(TextUtils.isEmpty(imeiStr)) {
            imeiStr = context.getResources().getString(R.string.imei_invalid);
        }
        /**
         * add by mediatek .inc end
         */

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(R.string.imei)
                .setMessage(imeiStr)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
    }

    public static void showMEIDPanel(Context context, boolean useSystemWindow) {
        String meidStr = ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE))
                .getDeviceId();

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(R.string.meid)
                .setMessage(meidStr)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .show();
    }

    /*******
     * This code is used to handle SIM Contact queries
     *******/
    private static final String ADN_PHONE_NUMBER_COLUMN_NAME = "number";
    private static final String ADN_NAME_COLUMN_NAME = "name";
    private static final String ADN_INDEX_COLUMN_NAME = "index";	
    private static final int ADN_QUERY_TOKEN = -1;

    /**
     * Cookie object that contains everything we need to communicate to the
     * handler's onQuery Complete, as well as what we need in order to cancel
     * the query (if requested).
     *
     * Note, access to the textField field is going to be synchronized, because
     * the user can request a cancel at any time through the UI.
     */
    private static class SimContactQueryCookie implements DialogInterface.OnCancelListener{
        public ProgressDialog progressDialog;
        public int contactNum;

        // Used to identify the query request.
        private int mToken;
        private QueryHandler mHandler;

        // The text field we're going to update
        private EditText textField;
        public String text;

        public SimContactQueryCookie(int number, QueryHandler handler, int token) {
            contactNum = number;
            mHandler = handler;
            mToken = token;
        }

        /**
         * Synchronized getter for the EditText.
         */
        public synchronized EditText getTextField() {
            return textField;
        }

        /**
         * Synchronized setter for the EditText.
         */
        public synchronized void setTextField(EditText text) {
            textField = text;
        }

        /**
         * Cancel the ADN query by stopping the operation and signaling
         * the cookie that a cancel request is made.
         */
        public synchronized void onCancel(DialogInterface dialog) {
            // close the progress dialog
            if (progressDialog != null) {
                progressDialog.dismiss();
            }

            // setting the textfield to null ensures that the UI does NOT get
            // updated.
            textField = null;

            // Cancel the operation if possible.
            mHandler.cancelOperation(mToken);
        }
    }

    /**
     * Asynchronous query handler that services requests to look up ADNs
     *
     * Queries originate from {@link handleAdnEntry}.
     */
    private static class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        /**
         * Override basic onQueryComplete to fill in the textfield when
         * we're handed the ADN cursor.
         */
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            SimContactQueryCookie sc = (SimContactQueryCookie) cookie;

            // close the progress dialog.
            sc.progressDialog.dismiss();
            if(fdnRequest()) return;

            // get the EditText to update or see if the request was cancelled.
            EditText text = sc.getTextField();
            String name = null;
            String number = null;

            if (c != null && text != null) {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    if (c.getInt(c.getColumnIndexOrThrow(ADN_INDEX_COLUMN_NAME)) == sc.contactNum) {
                        name = c.getString(c.getColumnIndexOrThrow(ADN_NAME_COLUMN_NAME));
                        number = c.getString(c.getColumnIndexOrThrow(ADN_PHONE_NUMBER_COLUMN_NAME));
                        break;
                    }
                }
                c.close();
            }

            // if the textview is valid, and the cursor is valid and postionable
            // on the Nth number, then we update the text field and display a
            // toast indicating the caller name.
                final Context context = sc.progressDialog.getContext();
                final int len = number != null ? number.length() : 0;
                Log.d("onQueryComplete","number " + number + "sc.text" + sc.text);
                if (sc.text.equals(number)) {
                    Toast .makeText(context, context.getString(R.string.non_phone_caption) + "\n" + number, Toast.LENGTH_LONG).show();
                } else if ((len > 1) && (len < 5) && (number.endsWith("#"))) {
                    Toast.makeText(context, context.getString(R.string.non_phone_caption) + "\n" + number, Toast.LENGTH_LONG).show();
                } else if(number != null){
                    // fill the text in.
                    text.setText(number);
                    text.setSelection(text.getText().length());

                    // display the name as a toast
                    name = context.getString(R.string.menu_callNumber, name);
                    Toast.makeText(context, name, Toast.LENGTH_SHORT)
                        .show();
                }
            
        }
    }

	static boolean fdnRequest() {

		boolean bRet = false;

		final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
				.getService(Context.TELEPHONY_SERVICE));
		if (null == iTel) {
			Log.e(TAG, "fdnRequest iTel is null");
			return false;
		}

		try {
			bRet = iTel.isFDNEnabled();
		} catch (RemoteException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}

		Log.d(TAG, "fdnRequest fdn enable is " + bRet);
		return bRet;
	}	
}
