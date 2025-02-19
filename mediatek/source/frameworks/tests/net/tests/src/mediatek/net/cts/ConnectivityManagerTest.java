/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2009 The Android Open Source Project
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

package mediatek.net.cts;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;
import dalvik.annotation.ToBeFixed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@TestTargetClass(ConnectivityManager.class)
public class ConnectivityManagerTest extends AndroidTestCase {

    private static final String TAG = ConnectivityManagerTest.class.getSimpleName();

    private static final String FEATURE_ENABLE_HIPRI = "enableHIPRI";

    public static final int TYPE_MOBILE = ConnectivityManager.TYPE_MOBILE;
    public static final int TYPE_WIFI = ConnectivityManager.TYPE_WIFI;
    private static final int HOST_ADDRESS = 0x7f000001;// represent ip 127.0.0.1

    // device could have only one interface: data, wifi.
    private static final int MIN_NUM_NETWORK_TYPES = 1;

    private ConnectivityManager mCm;
    private WifiManager mWifiManager;
    private PackageManager mPackageManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        mPackageManager = getContext().getPackageManager();
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "getNetworkInfo",
        args = {int.class}
    )
    public void testGetNetworkInfo() {
        assertTrue(mCm.getAllNetworkInfo().length >= MIN_NUM_NETWORK_TYPES);
        NetworkInfo ni = mCm.getNetworkInfo(TYPE_WIFI);
        if (ni != null) {
            State state = ni.getState();
            assertTrue(State.UNKNOWN.ordinal() >= state.ordinal()
                       && state.ordinal() >= State.CONNECTING.ordinal());
            DetailedState ds = ni.getDetailedState();
            assertTrue(DetailedState.FAILED.ordinal() >= ds.ordinal()
                       && ds.ordinal() >= DetailedState.IDLE.ordinal());
        }
        ni = mCm.getNetworkInfo(TYPE_MOBILE);
        if (ni != null) {
            State state = ni.getState();
            assertTrue(State.UNKNOWN.ordinal() >= state.ordinal()
                    && state.ordinal() >= State.CONNECTING.ordinal());
            DetailedState ds = ni.getDetailedState();
            assertTrue(DetailedState.FAILED.ordinal() >= ds.ordinal()
                    && ds.ordinal() >= DetailedState.IDLE.ordinal());
        }
        ni = mCm.getNetworkInfo(-1);
        assertNull(ni);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "isNetworkTypeValid",
            args = {int.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getAllNetworkInfo",
            args = {}
        )
    })
    public void testIsNetworkTypeValid() {

        NetworkInfo[] ni = mCm.getAllNetworkInfo();

        for (NetworkInfo n : ni) {
            assertTrue(ConnectivityManager.isNetworkTypeValid(n.getType()));
        }
        assertFalse(ConnectivityManager.isNetworkTypeValid(-1));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test getAllNetworkInfo().",
        method = "getAllNetworkInfo",
        args = {}
    )
    public void testGetAllNetworkInfo() {
        NetworkInfo[] ni = mCm.getAllNetworkInfo();
        assertTrue(ni.length >= MIN_NUM_NETWORK_TYPES);
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "startUsingNetworkFeature",
            args = {int.class, java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "stopUsingNetworkFeature",
            args = {int.class, java.lang.String.class}
        )
    })
    public void testStartUsingNetworkFeature() {

        final String invalidateFeature = "invalidateFeature";
        final String mmsFeature = "enableMMS";
        final int failureCode = -1;
        final int wifiOnlyStartFailureCode = 3;
        final int wifiOnlyStopFailureCode = 1;

        NetworkInfo ni = mCm.getNetworkInfo(TYPE_MOBILE);
        if (ni != null) {
            assertEquals(failureCode, mCm.startUsingNetworkFeature(TYPE_MOBILE,
                    invalidateFeature));
            assertEquals(failureCode, mCm.stopUsingNetworkFeature(TYPE_MOBILE,
                    invalidateFeature));
        } else {
            assertEquals(wifiOnlyStartFailureCode, mCm.startUsingNetworkFeature(TYPE_MOBILE,
                    invalidateFeature));
            assertEquals(wifiOnlyStopFailureCode, mCm.stopUsingNetworkFeature(TYPE_MOBILE,
                    invalidateFeature));
        }

        // Should return failure(-1) because MMS is not supported on WIFI.
        assertEquals(failureCode, mCm.startUsingNetworkFeature(TYPE_WIFI, mmsFeature));
        assertEquals(failureCode, mCm.stopUsingNetworkFeature(TYPE_WIFI, mmsFeature));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "requestRouteToHost",
        args = {int.class, int.class}
    )
    public void testRequestRouteToHost() {
        Set<Integer> exceptionFreeTypes = new HashSet<Integer>();
        exceptionFreeTypes.add(ConnectivityManager.TYPE_BLUETOOTH);
        exceptionFreeTypes.add(ConnectivityManager.TYPE_ETHERNET);
        exceptionFreeTypes.add(ConnectivityManager.TYPE_MOBILE);
        exceptionFreeTypes.add(ConnectivityManager.TYPE_MOBILE_DUN);
        exceptionFreeTypes.add(ConnectivityManager.TYPE_MOBILE_HIPRI);
        exceptionFreeTypes.add(ConnectivityManager.TYPE_MOBILE_MMS);
        exceptionFreeTypes.add(ConnectivityManager.TYPE_MOBILE_SUPL);

        NetworkInfo[] ni = mCm.getAllNetworkInfo();
        for (NetworkInfo n : ni) {
            if (n.isConnected() && exceptionFreeTypes.contains(n.getType())) {
                assertTrue("Network type: " + n.getType(), mCm.requestRouteToHost(n.getType(),
                        HOST_ADDRESS));
            }
        }

        assertFalse(mCm.requestRouteToHost(-1, HOST_ADDRESS));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        method = "getActiveNetworkInfo",
        args = {}
    )
    @ToBeFixed(bug="1695243", explanation="No Javadoc")
    public void testGetActiveNetworkInfo() {
        NetworkInfo ni = mCm.getActiveNetworkInfo();

        if (ni != null) {
            assertTrue(ni.getType() >= 0);
        }
    }

    @TestTargetNew(
        level = TestLevel.SUFFICIENT,
        method = "getBackgroundDataSetting",
        args = {}
    )
    public void testTest() {
        mCm.getBackgroundDataSetting();
    }

    /** Test that hipri can be brought up when Wifi is enabled. */
    public void testStartUsingNetworkFeature_enableHipri() throws Exception {
        /*if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                || !mPackageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            // This test requires a mobile data connection and WiFi.
            return;
        }

        boolean isWifiConnected = mWifiManager.isWifiEnabled()
                && mWifiManager.getConnectionInfo().getSSID() != null;

        try {
            // Make sure WiFi is connected to an access point.
            if (!isWifiConnected) {
                connectToWifi();
            }

            // Register a receiver that will capture the connectivity change for hipri.
            ConnectivityActionReceiver receiver =
                    new ConnectivityActionReceiver(ConnectivityManager.TYPE_MOBILE_HIPRI);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(receiver, filter);

            // Try to start using the hipri feature...
            int result = mCm.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                    FEATURE_ENABLE_HIPRI);
            assertTrue("Couldn't start using the HIPRI feature.", result != -1);

            // Check that the ConnectivityManager reported that it connected using hipri...
            assertTrue("Couldn't connect using hipri...", receiver.waitForConnection());

            assertTrue("Couldn't requestRouteToHost using HIPRI.",
                    mCm.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_HIPRI, HOST_ADDRESS));

        } catch (InterruptedException e) {
            fail("Broadcast receiver waiting for ConnectivityManager interrupted.");
        } finally {
            mCm.stopUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                    FEATURE_ENABLE_HIPRI);
            if (!isWifiConnected) {
                mWifiManager.setWifiEnabled(false);
            }
        }*/
    }

    private void connectToWifi() throws InterruptedException {
        ConnectivityActionReceiver receiver =
                new ConnectivityActionReceiver(ConnectivityManager.TYPE_WIFI);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(receiver, filter);

        assertTrue(mWifiManager.setWifiEnabled(true));
        assertTrue("Wifi must be configured to connect to an access point for this test.",
                receiver.waitForConnection());

        mContext.unregisterReceiver(receiver);
    }

    /** Receiver that captures the last connectivity change's network type and state. */
    private class ConnectivityActionReceiver extends BroadcastReceiver {

        private final CountDownLatch mReceiveLatch = new CountDownLatch(1);

        private final int mNetworkType;

        ConnectivityActionReceiver(int networkType) {
            mNetworkType = networkType;
        }

        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = intent.getExtras()
                    .getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
            int networkType = networkInfo.getType();
            State networkState = networkInfo.getState();
            Log.i(TAG, "Network type: " + networkType + " state: " + networkState);
            if (networkType == mNetworkType && networkInfo.getState() == State.CONNECTED) {
                mReceiveLatch.countDown();
            }
        }

        public boolean waitForConnection() throws InterruptedException {
            return mReceiveLatch.await(30, TimeUnit.SECONDS);
        }
    }
}
