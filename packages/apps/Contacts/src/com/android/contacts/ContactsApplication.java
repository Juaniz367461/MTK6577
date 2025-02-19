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

package com.android.contacts;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.test.InjectedServices;
import com.android.contacts.util.Constants;
import com.google.common.annotations.VisibleForTesting;

import android.app.Application;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mediatek.contacts.HyphonManager;
import com.mediatek.contacts.SimAssociateHandler;
import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.simcontact.SIMInfoWrapper;

public final class ContactsApplication extends Application {
    private static final boolean ENABLE_LOADER_LOG = false; // Don't submit with true
    private static final boolean ENABLE_FRAGMENT_LOG = false; // Don't submit with true

    private static InjectedServices sInjectedServices;
    private AccountTypeManager mAccountTypeManager;
    private ContactPhotoManager mContactPhotoManager;
    private ContactListFilterController mContactListFilterController;

    static ContactsApplication sMe;

    public static final boolean sGemini = FeatureOption.MTK_GEMINI_SUPPORT;
    public static final boolean sDialerSearchSupport = FeatureOption.MTK_DIALER_SEARCH_SUPPORT;
    public static final boolean sSpeedDial = true;

    /**
     * Overrides the system services with mocks for testing.
     */
    @VisibleForTesting
    public static void injectServices(InjectedServices services) {
        sInjectedServices = services;
    }

    public static InjectedServices getInjectedServices() {
        return sInjectedServices;
    }

    @Override
    public ContentResolver getContentResolver() {
        if (sInjectedServices != null) {
            ContentResolver resolver = sInjectedServices.getContentResolver();
            if (resolver != null) {
                return resolver;
            }
        }
        return super.getContentResolver();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (sInjectedServices != null) {
            SharedPreferences prefs = sInjectedServices.getSharedPreferences();
            if (prefs != null) {
                return prefs;
            }
        }

        return super.getSharedPreferences(name, mode);
    }

    @Override
    public Object getSystemService(String name) {
        if (sInjectedServices != null) {
            Object service = sInjectedServices.getSystemService(name);
            if (service != null) {
                return service;
            }
        }

        if (AccountTypeManager.ACCOUNT_TYPE_SERVICE.equals(name)) {
            if (mAccountTypeManager == null) {
                mAccountTypeManager = AccountTypeManager.createAccountTypeManager(this);
            }
            return mAccountTypeManager;
        }

        if (ContactPhotoManager.CONTACT_PHOTO_SERVICE.equals(name)) {
            if (mContactPhotoManager == null) {
                mContactPhotoManager = ContactPhotoManager.createContactPhotoManager(this);
                registerComponentCallbacks(mContactPhotoManager);
                mContactPhotoManager.preloadPhotosInBackground();
            }
            return mContactPhotoManager;
        }

        if (ContactListFilterController.CONTACT_LIST_FILTER_SERVICE.equals(name)) {
            if (mContactListFilterController == null) {
                mContactListFilterController =
                        ContactListFilterController.createContactListFilterController(this);
            }
            return mContactListFilterController;
        }

        return super.getSystemService(name);
    }

    public static ContactsApplication getInstance() {
        return sMe;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "ContactsApplication.onCreate start");
        }

        // Priming caches to placate the StrictMode police
        Context context = getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context);
        AccountTypeManager.getInstance(context);
        if (ENABLE_FRAGMENT_LOG) FragmentManager.enableDebugLogging(true);
        if (ENABLE_LOADER_LOG) LoaderManager.enableDebugLogging(true);

        if (Log.isLoggable(Constants.STRICT_MODE_TAG, Log.DEBUG)) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
        }

        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "ContactsApplication.onCreate finish");
        }

        sMe = this;

        /**
         * added by mediatek .inc
         * description : initialize CellConnService and SIM associate handler
         */
        SimAssociateHandler.getInstance().prepair();
        SimAssociateHandler.getInstance().load();
        
        cellConnMgr = new CellConnMgr();
        cellConnMgr.register(getApplicationContext());
        SIMInfoWrapper.getDefault();
        HyphonManager.getInstance();
        new Thread(new Runnable() {
            public void run() {
            	long lStart = System.currentTimeMillis();
                HyphonManager.getInstance().formatNumber(TEST_NUMBER);
                Log.i(Constants.PERFORMANCE_TAG," Thread HyphonManager formatNumber() use time :" + (System.currentTimeMillis() - lStart));
            }
        }).start();
        /**
         * added by mediatek .inc end
         */
        /*
         * Bug Fix by Mediatek Begin.
         *   CR ID: ALPS00286964
         *   Descriptions: Remove all of notifications which owns to Contacts application.
         */
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
        /*
         * Bug Fix by Mediatek End.
         */
    }

    /* below are added by mediatek .inc */
    public CellConnMgr cellConnMgr;

    protected String TEST_NUMBER = "10086";

    // Should be single thread, as we don't want to simultaneously handle contacts 
    // copy-delete-import-export request.
    public final ExecutorService singleTaskService = Executors.newSingleThreadExecutor();
}
