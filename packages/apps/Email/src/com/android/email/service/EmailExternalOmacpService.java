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

package com.android.email.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.android.email.Email;
import com.android.email.EmailAddressValidator;
import com.android.email.activity.setup.AccountSettingsUtils;
import com.android.email.mail.Sender;
import com.android.email.mail.Store;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent.AccountColumns;
import com.android.emailcommon.provider.HostAuth;
import com.android.emailcommon.provider.EmailContent.HostAuthColumns;
import com.android.emailcommon.service.EmailExternalConstants;
import com.android.emailcommon.Configuration;
import com.android.emailcommon.Logging;;

public class EmailExternalOmacpService extends Service {

    private static final String TAG = "OmacpAddAccountService";
    private static final String APPID_KEY = "25";
    private static final String SMTP_APPID = String.valueOf(Configuration.SMTP_DEFAULT_PORT);
    private static final String SMTP_DEAULT_PORT_NUM = String.valueOf(Configuration.SMTP_DEFAULT_PORT);
    private static final String SMTP_DEFAULT_SERVICE = String.valueOf(Configuration.SMTP_DEFAULT_PORT);
    private static final String SMTP_SSL_SERVICE = String.valueOf(Configuration.SMTP_DEFAULT_SSL_PORT);
    private static final String POP_DEFAULT_PORT_NUM = String.valueOf(Configuration.POP3_DEFAULT_PORT);
    private static final String IMAP_DEFAULT_PORT_NUM = String.valueOf(Configuration.IMAP_DEFAULT_PORT);
    private static final String POP_DEFAULT_SERVICE = String.valueOf(Configuration.POP3_DEFAULT_PORT);
    private static final String IMAP_DEFAULT_SERVICE = String.valueOf(Configuration.IMAP_DEFAULT_PORT);
    private static final String POP_SSL_SERVICE = String.valueOf(Configuration.POP3_DEFAULT_SSL_PORT);
    private static final String IMAP_SSL_SERVICE = String.valueOf(Configuration.IMAP_DEFAULT_SSL_PORT);
    private static final String POP_APPID = String.valueOf(Configuration.POP3_DEFAULT_PORT);
    private static final String IMAP_APPID = String.valueOf(Configuration.IMAP_DEFAULT_PORT);

    private static final String STR_SSL = "ssl";
    private static final String STR_TLS = "tls";

    private static final int SYNC_INTERVAL = 15;
    private static final int SMTP_SERVER_TYPE = 1;
    private static final int POP_SERVER_TYPE = 2;
    private static final int IMAP_SERVER_TYPE = 3;
    private final int CONNECT_SUCCESS = 1;
    private final int CONNECT_FAIL = -1;
    private final static String HOSTAUTH_WHERE_CREDENTIALS = HostAuthColumns.ADDRESS + " like ?"
            + " and " + HostAuthColumns.LOGIN + " like ?" + " and " + HostAuthColumns.PROTOCOL
            + " not like \"smtp\"";

    private final static String ACCOUNT_WHERE_HOSTAUTH = AccountColumns.HOST_AUTH_KEY_RECV + "=?";

    private EmailAddressValidator mEmailValidator = new EmailAddressValidator();
    private String from;
    private String providerId;
    private String rtAddr;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == CONNECT_SUCCESS) {
                Account account = (Account) msg.obj;
                addAccount(account);
                sendResultToOmacp(true);
            } else if (msg.what == CONNECT_FAIL) {
                sendResultToOmacp(false);
            }
        }
    };

    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Intent omacpIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        if (omacpIntent != null) {
            Account account = parserOmacpRequest(omacpIntent);
            Log.i(TAG, "account is " + account);
            if (account != null) {
                checkEmailServerConnect(this, account);
            } else {
                sendResultToOmacp(false);
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * Obtaining Account Object by parsering OMACP request
     * 
     * @param intent OMACP intent
     * @return Account
     */
    @SuppressWarnings("unchecked")
    private Account parserOmacpRequest(Intent intent) {
        Log.i(TAG, "parser Omacp Request is begin");
        List<Intent> omacpList = intent.getParcelableArrayListExtra("email_setting_intent");
        if (null == omacpList || omacpList.isEmpty()) {
            Logging.e(TAG, "OMACP email_setting_intent is " + omacpList);
            return null;
        }
        Account account = new Account();
        String addr = null;
        String portNbr = null;
        String service = null;
        String scheme = null;
        ArrayList appAddr = null;
        ArrayList appAuth = null;
        URI uri = null;
        URI smtpUri = null;
        String appId = null;
        String authType = null;
        try {
            for (Intent emailIntent : omacpList) {
                appId = emailIntent.getStringExtra("APPID");
                appAddr = emailIntent.getParcelableArrayListExtra("APPADDR");
                if (null != appAddr && !appAddr.isEmpty()) {
                    for (int i = 0; i < appAddr.size(); i++) {
                        if (appAddr.get(i) instanceof Map) {
                            Map<String, String> addrMap = (Map<String, String>) appAddr.get(i);
                            addr = addrMap.get("ADDR");
                            portNbr = addrMap.get("PORTNBR");
                            service = addrMap.get("SERVICE");
                        }
                    }
                } else {
                    addr = emailIntent.getStringExtra("ADDR");
                }
                if (isEmpty(addr)) {
                    Logging.e(TAG, "addr is empty");
                    return null;
                }
                appAuth = emailIntent.getParcelableArrayListExtra("APPAUTH");
                String aAuthName = null;
                String aAuthSecret = null;
                if (null != appAuth && !appAuth.isEmpty()) {
                    for (int i = 0; i < appAuth.size(); i++) {
                        if (appAuth.get(i) instanceof Map) {
                            Map<String, String> authMap = (Map<String, String>) appAuth.get(i);
                            authType = authMap.get("AAUTHTYPE");
                            aAuthName = authMap.get("AAUTHNAME");
                            aAuthSecret = authMap.get("AAUTHSECRET");
                        }
                    }
                }
                if (SMTP_APPID.equals(appId)) {
                    providerId = emailIntent.getStringExtra("PROVIDER-ID");
                    from = emailIntent.getStringExtra("FROM");
                    rtAddr = emailIntent.getStringExtra("RT-ADDR");

                    Log.i(TAG, "[OMACP] smtp param: PROVIDER-ID=" + providerId + ";ADDR=" + addr
                            + ";FROM=" + from + ";RT-ADDR=" + rtAddr + ";PORT=" + portNbr
                            + ";SERVICE=" + service + ";AAUTHTYPE=" + authType + ";AAUTHNAME="
                            + aAuthName + ";AAUTHSECRET=" + aAuthSecret);
                    if (isInvalidEmailAddress(from)) {
                        return null;
                    }

                    // delete "< >"
                    if (from.contains("<")) {
                        from = from.split("<")[1].replace(">", "");
                    }
                    if (rtAddr != null) {
                        if (isInvalidEmailAddress(rtAddr)) {
                            return null;
                        }
                        if (rtAddr.contains("<")) {
                            rtAddr = rtAddr.split("<")[0].trim().replace("\"", "");
                        } else {

                            // if the rtaddr doesn't contain nickname,then set
                            // email address to be reply address
                            rtAddr = null;
                        }
                    }
                    String userInfo = null;

                    // if authType is "LOGIN", get set userinfo ,otherwise,set
                    // userinfo is null
                    if ("LOGIN".equalsIgnoreCase(authType)) {
                        if (isEmpty(aAuthName) || isEmpty(aAuthSecret)) {
                            return null;
                        }
                        userInfo = aAuthName + ":" + aAuthSecret;
                    }
                    portNbr = portNbr == null ? SMTP_DEAULT_PORT_NUM : portNbr;
                    service = service == null ? SMTP_DEFAULT_SERVICE : service;
                    scheme = getScheme(service, SMTP_SERVER_TYPE);
                    if (null == scheme) {
                        return null;
                    }
                    smtpUri = new URI(scheme, userInfo, addr, Integer.parseInt(portNbr), null,
                            null, null);
                    HostAuth sendAuth = account.getOrCreateHostAuthSend(this);
                    HostAuth.setHostAuthFromString(sendAuth, smtpUri.toString());
                } else if (POP_APPID.equals(appId)) {
                    Log.i(TAG, "[OMACP] pop param: ADDR=" + addr + ";PORT=" + portNbr + ";SERVICE="
                            + service + ";AAUTHNAME=" + aAuthName + ";AAUTHSECRET=" + aAuthSecret);
                    if (isEmpty(aAuthName) || isEmpty(aAuthSecret)) {
                        return null;
                    }
                    Account tempAccount = findDuplicateAccount(this, account.mId, addr, aAuthName);
                    if (null != tempAccount) {

                        // for updating account,obtain account according mid
                        account.mId = tempAccount.mId;
                        account = Account.restoreAccountWithId(this, account.mId);
                    }
                    portNbr = portNbr == null ? POP_DEFAULT_PORT_NUM : portNbr;
                    service = service == null ? POP_DEFAULT_SERVICE : service;
                    scheme = getScheme(service, POP_SERVER_TYPE);
                    if (null == scheme) {
                        return null;
                    }
                    uri = new URI(scheme, aAuthName + ":" + aAuthSecret, addr, Integer
                            .parseInt(portNbr), null, null, null);
                    HostAuth recAuth = account.getOrCreateHostAuthRecv(this);
                    HostAuth.setHostAuthFromString(recAuth, uri.toString());
//                    account.setStoreUri(this, uri.toString());
                } else if (IMAP_APPID.equals(appId)) {
                    Log.i(TAG, "[OMACP] imap param: ADDR=" + addr + ";PORT=" + portNbr
                            + ";SERVICE=" + service + ";AAUTHNAME=" + aAuthName + ";AAUTHSECRET="
                            + aAuthSecret);
                    if (isEmpty(aAuthName) || isEmpty(aAuthSecret)) {
                        return null;
                    }
                    Account tempAccount = findDuplicateAccount(this, account.mId, addr, aAuthName);
                    if (null != tempAccount) {

                        // for updating account,obtain account according mid
                        account.mId = tempAccount.mId;
                        account = Account.restoreAccountWithId(this, account.mId);
                    }
                    portNbr = portNbr == null ? IMAP_DEFAULT_PORT_NUM : portNbr;
                    service = service == null ? IMAP_DEFAULT_SERVICE : service;
                    scheme = getScheme(service, IMAP_SERVER_TYPE);
                    if (null == scheme) {
                        return null;
                    }
                    uri = new URI(scheme, aAuthName + ":" + aAuthSecret, addr, Integer
                            .parseInt(portNbr), null, null, null);
                    account.setDeletePolicy(Account.DELETE_POLICY_ON_DELETE);
                    HostAuth recAuth = account.getOrCreateHostAuthRecv(this);
                    HostAuth.setHostAuthFromString(recAuth, uri.toString());
//                    account.setStoreUri(this, uri.toString());
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            Logging.e(TAG, "" + e, e);
            return null;
        }

        // if the account is already saved, reset smtp sender
        try {
            if (account.isSaved()) {
                HostAuth sendHostAuth = account.getOrCreateHostAuthSend(this);
                HostAuth.setHostAuthFromString(sendHostAuth, smtpUri.toString());
                // account.setSenderUri(this, smtpUri.toString());
            }
        } catch (Exception e) {
            Logging.e(TAG, "" + e, e);
            return null;
        }

        return account;
    }

    /**
     * get scheme of email uri
     * 
     * @param service service from omacp
     * @param serverType Email server type
     * @return
     */
    private String getScheme(String service, int serverType) {
        String scheme = null;
        if (SMTP_SERVER_TYPE == serverType) {
            if (SMTP_DEFAULT_SERVICE.equalsIgnoreCase(service)) {
                scheme = "smtp";
            } else if ("STARTTLS".equalsIgnoreCase(service) || STR_TLS.equalsIgnoreCase(service)) {
                scheme = "smtp+tls+";
            } else if (SMTP_SSL_SERVICE.equalsIgnoreCase(service) || STR_SSL.equalsIgnoreCase(service)) {
                scheme = "smtp+ssl+";
            }
        } else if (POP_SERVER_TYPE == serverType) {
            if (POP_DEFAULT_SERVICE.equalsIgnoreCase(service)) {
                scheme = "pop3";
            } else if ("STARTTLS".equalsIgnoreCase(service) || STR_TLS.equalsIgnoreCase(service)) {
                scheme = "pop3+tls+";
            } else if (POP_SSL_SERVICE.equalsIgnoreCase(service) || STR_SSL.equalsIgnoreCase(service)) {
                scheme = "pop3+ssl+";
            }
        } else if (IMAP_SERVER_TYPE == serverType) {
            if (IMAP_DEFAULT_SERVICE.equalsIgnoreCase(service)) {
                scheme = "imap";
            } else if ("STARTTLS".equalsIgnoreCase(service) || STR_TLS.equalsIgnoreCase(service)) {
                scheme = "imap+tls+";
            } else if (IMAP_SSL_SERVICE.equalsIgnoreCase(service) || STR_SSL.equalsIgnoreCase(service)) {
                scheme = "imap+ssl+";
            }
        }

        return scheme;
    }

    /**
     * check the email address is invalid or not
     * 
     * @param email email address
     * @return
     */
    private boolean isInvalidEmailAddress(String email) {
        return isEmpty(email) || !mEmailValidator.isValid(email);
    }

    /**
     * check the string is empty or not
     * 
     * @param str
     * @return
     */
    private boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * Look for an existing account with the same username & server
     * 
     * @param context a system context
     * @param allowAccountId this account Id will not trigger (when editing an
     *            existing account)
     * @param hostName the server
     * @param userLogin the user login string
     * @result null = no dupes found. non-null = dupe account's display name
     */
    public Account findDuplicateAccount(Context context, long allowAccountId, String hostName,
            String userLogin) {
        Account account = null;
        ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(HostAuth.CONTENT_URI, HostAuth.ID_PROJECTION,
                HOSTAUTH_WHERE_CREDENTIALS, new String[] {
                        hostName, userLogin
                }, null);
        try {
            while (c.moveToNext()) {
                long hostAuthId = c.getLong(HostAuth.ID_PROJECTION_COLUMN);

                // Find account with matching hostauthrecv key, and return its
                // display name
                Cursor c2 = resolver.query(Account.CONTENT_URI, Account.ID_PROJECTION,
                        ACCOUNT_WHERE_HOSTAUTH, new String[] {
                            Long.toString(hostAuthId)
                        }, null);
                try {
                    while (c2.moveToNext()) {
                        long accountId = c2.getLong(Account.ID_PROJECTION_COLUMN);
                        if (accountId != allowAccountId) {
                            account = Account.restoreAccountWithId(context, accountId);
                        }
                    }
                } finally {
                    c2.close();
                }
            }
        } finally {
            c.close();
        }

        return account;
    }

    /**
     * check email server is connect or not,including sender server and receiver
     * server
     * 
     * @param context
     * @param account
     */
    private void checkEmailServerConnect(final Context context, final Account account) {
        Runnable runnable = new Runnable() {

            public void run() {
                try {

//                    Store store = Store.getInstance(account.getStoreUri(context), context, null);
                    Store store = Store.getInstance(account, context);
                    if (store == null) {
                        sendMessage(CONNECT_FAIL, null);
                        return;
                    }
                    store.checkSettings();

                    Sender sender = Sender.getInstance(getApplication(), account);
                    if (sender == null) {
                        sendMessage(CONNECT_FAIL, null);
                        return;
                    }
                    sender.close();
                    sender.open();
                    sender.close();
                    Log.i(TAG, "email server check finish.");
                    sendMessage(CONNECT_SUCCESS, account);
                } catch (Exception e) {
                    Logging.e(TAG, "" + e, e);
                    sendMessage(CONNECT_FAIL, null);
                }
            }
        };
        new Thread(runnable).start();
    }

    private void sendMessage(int isConnect, Account account) {
        if (CONNECT_FAIL == isConnect) {
            mHandler.sendEmptyMessage(isConnect);
        } else if (CONNECT_SUCCESS == isConnect) {
            Message message = mHandler.obtainMessage();
            message.what = isConnect;
            message.obj = account;
            mHandler.sendMessage(message);
        }
    }

    /**
     * add account to email
     * 
     * @param account
     */
    private void addAccount(Account account) {
        Log.i(TAG, "add Account is beginning");
        int newFlags = account.getFlags() & ~(Account.FLAGS_NOTIFY_NEW_MAIL);
        newFlags |= Account.FLAGS_NOTIFY_NEW_MAIL;
        account.setFlags(newFlags);
        account.setEmailAddress(from);
        account.setSenderName(rtAddr);
        account.setDisplayName(providerId);
        account.setSyncInterval(SYNC_INTERVAL);
        account.setDefaultAccount(true);
        account.mFlags &= ~Account.FLAGS_INCOMPLETE;
        if (account.isSaved()) {
            Log.i(TAG, "update Account send & receive information");
            account.mHostAuthSend.update(this, account.mHostAuthSend.toContentValues());
            account.mHostAuthRecv.update(this, account.mHostAuthRecv.toContentValues());
        }
        //update account flag.
        String protocal = account.mHostAuthRecv == null ? null : account.mHostAuthRecv.mProtocol;
        if (null != protocal) {
            setFlagsForProtocol(account, protocal);
        }
        Log.i(TAG, "add Account with flag : " + account.mFlags);
        //Save email account.
        AccountSettingsUtils.commitSettings(this, account);
        Log.i(TAG, "AccountSettingsUtils.commitSettings save email Account ");
        //Save system account.
        MailService.setupAccountManagerAccount(this, account, true, false, false, null);
        Log.i(TAG, "MailService.setupAccountManagerAccount save system Account ");
        Email.setServicesEnabledSync(this);
    }

    private void sendResultToOmacp(boolean isSucceed) {
        Intent intent = new Intent();
        intent.setAction(EmailExternalConstants.OMACP_SETTING_RESULT_ACTION);
        intent.putExtra("appId", APPID_KEY);
        intent.putExtra("result", isSucceed);
        this.sendBroadcast(intent);
    }

    private static final String APPID_VALUE = "25";
    static void buildCapabilityResultToOmacp(Context context) {
        Intent intent = new Intent(EmailExternalConstants.OMAPCP_CAPABILITY_RESULT_ACTION);
        intent.putExtra(EmailCapability.APPID, APPID_VALUE);
        intent.putExtra(EmailCapability.EMAIL, true);
        intent.putExtra(EmailCapability.EMAIL_PROVIDER_ID, true);
        intent.putExtra(EmailCapability.EMAIL_OUTBOUND_ADDR, true);
        intent.putExtra(EmailCapability.EMAIL_OUTBOUND_PORT_NUMBER, true);
        intent.putExtra(EmailCapability.EMAIL_OUTBOUND_SECURE, true);
        intent.putExtra(EmailCapability.EMAIL_OUTBOUND_AUTH_TYPE, true);
        intent.putExtra(EmailCapability.EMAIL_OUTBOUND_USER_NAME, true);
        intent.putExtra(EmailCapability.EMAIL_OUTBOUND_PASSWORD, true);
        intent.putExtra(EmailCapability.EMAIL_FROM, true);
        intent.putExtra(EmailCapability.EMAIL_RT_ADDR, true);
        intent.putExtra(EmailCapability.EMAIL_INBOUND_ADDR, true);
        intent.putExtra(EmailCapability.EMAIL_INBOUND_PORT_NUMBER, true);
        intent.putExtra(EmailCapability.EMAIL_INBOUND_SECURE, true);
        intent.putExtra(EmailCapability.EMAIL_INBOUND_USER_NAME, true);
        intent.putExtra(EmailCapability.EMAIL_INBOUND_PASSWORD, true);
        Log.i("EmailExternalReceiver", "return OMACP capability result intent:" + intent);
        context.sendBroadcast(intent);
    }

    interface EmailCapability {

        String APPID = "appId";

        String EMAIL = "email";

        String EMAIL_PROVIDER_ID = "email_provider_id";

        String EMAIL_OUTBOUND_ADDR = "email_outbound_addr";

        String EMAIL_OUTBOUND_PORT_NUMBER = "email_outbound_port_number";

        String EMAIL_OUTBOUND_SECURE = "email_outbound_secure";

        String EMAIL_OUTBOUND_AUTH_TYPE = "email_outbound_auth_type";

        String EMAIL_OUTBOUND_USER_NAME = "email_outbound_user_name";

        String EMAIL_OUTBOUND_PASSWORD = "email_outbound_password";

        String EMAIL_FROM = "email_from";

        String EMAIL_RT_ADDR = "email_rt_addr";

        String EMAIL_INBOUND_ADDR = "email_inbound_addr";

        String EMAIL_INBOUND_PORT_NUMBER = "email_inbound_port_number";

        String EMAIL_INBOUND_SECURE = "email_inbound_secure";

        String EMAIL_INBOUND_USER_NAME = "email_inbound_user_name";

        String EMAIL_INBOUND_PASSWORD = "email_inbound_password";
    }    

    /**
     * Sets the account sync, delete, and other misc flags not captured in {@code HostAuth}
     * information for the specified account based on the protocol type.
     */
    static void setFlagsForProtocol(Account account, String protocol) {
        if (HostAuth.SCHEME_IMAP.equals(protocol)) {
            // Delete policy must be set explicitly, because IMAP does not provide a UI selection
            // for it.
            account.setDeletePolicy(Account.DELETE_POLICY_ON_DELETE);
            account.mFlags |= Account.FLAGS_SUPPORTS_SEARCH;
        }
    }
}
