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

package com.android.emailcommon.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

public class AccountServiceProxy extends ServiceProxy implements IAccountService {

    public static final String ACCOUNT_INTENT = "com.android.email.ACCOUNT_INTENT";
    public static final int DEFAULT_ACCOUNT_COLOR = 0xFF0000FF;

    private IAccountService mService = null;
    private Object mReturn;

    public AccountServiceProxy(Context _context) {
        super(_context, new Intent(ACCOUNT_INTENT));
    }

    @Override
    public void onConnected(IBinder binder) {
        mService = IAccountService.Stub.asInterface(binder);
    }

    @Override
    public IBinder asBinder() {
        return null;
    }

    @Override
    public void notifyLoginFailed(final long accountId) {
        setTask(new ProxyTask() {
            @Override
            public void run() throws RemoteException {
                mService.notifyLoginFailed(accountId);
            }
        }, "notifyLoginFailed");
    }

    @Override
    public void notifyLoginSucceeded(final long accountId) {
        setTask(new ProxyTask() {
            @Override
            public void run() throws RemoteException {
                mService.notifyLoginSucceeded(accountId);
            }
        }, "notifyLoginSucceeded");
    }

    @Override
    public void notifySendFailed(final long accountId) {
        setTask(new ProxyTask() {
            @Override
            public void run() throws RemoteException {
                mService.notifySendFailed(accountId);
            }
        }, "notifySendFailed");
    }

    @Override
    public void notifySendSucceeded(final long accountId) {
        setTask(new ProxyTask() {
            @Override
            public void run() throws RemoteException {
                mService.notifySendSucceeded(accountId);
            }
        }, "notifySendSucceeded");
    }

    // The following call is synchronous, and should not be made from the UI thread
    @Override
    public void reconcileAccounts(final String protocol, final String accountManagerType) {
        setTask(new ProxyTask() {
            @Override
            public void run() throws RemoteException {
                mService.reconcileAccounts(protocol, accountManagerType);
            }
        }, "reconcileAccounts");
        waitForCompletion();
    }

    // The following call is synchronous, and should not be made from the UI thread
    @Override
    public int getAccountColor(final long accountId) {
        setTask(new ProxyTask() {
            @Override
            public void run() throws RemoteException{
                mReturn = mService.getAccountColor(accountId);
            }
        }, "getAccountColor");
        waitForCompletion();
        if (mReturn == null) {
            return DEFAULT_ACCOUNT_COLOR;
        } else {
            return (Integer)mReturn;
        }
    }

    // The following call is synchronous, and should not be made from the UI thread
    @Override
    public Bundle getConfigurationData(final String accountType) {
        setTask(new ProxyTask() {
            @Override
            public void run() throws RemoteException{
                mReturn = mService.getConfigurationData(accountType);
            }
        }, "getConfigurationData");
        waitForCompletion();
        if (mReturn == null) {
            return null;
        } else {
            return (Bundle)mReturn;
        }
    }

    // The following call is synchronous, and should not be made from the UI thread
    @Override
    public String getDeviceId() {
        setTask(new ProxyTask() {
            @Override
            public void run() throws RemoteException{
                mReturn = mService.getDeviceId();
            }
        }, "getDeviceId");
        waitForCompletion();
        if (mReturn == null) {
            return null;
        } else {
            return (String)mReturn;
        }
    }
}

