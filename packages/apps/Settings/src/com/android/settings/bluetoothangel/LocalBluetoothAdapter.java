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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDevicePicker;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Set;

/**
 * LocalBluetoothAdapter provides an interface between the Settings app
 * and the functionality of the local {@link BluetoothAdapter}, specifically
 * those related to state transitions of the adapter itself.
 *
 * <p>Connection and bonding state changes affecting specific devices
 * are handled by {@link CachedBluetoothDeviceManager},
 * {@link BluetoothEventManager}, and {@link LocalBluetoothProfileManager}.
 */
public final class LocalBluetoothAdapter {
    private static final String TAG = "LocalBluetoothAdapter";

    /** This class does not allow direct access to the BluetoothAdapter. */
    private final BluetoothAdapter mAdapter;

    private LocalBluetoothProfileManager mProfileManager;

    private static LocalBluetoothAdapter sInstance;

    private int mState = BluetoothAdapter.ERROR;

    private static final int SCAN_EXPIRATION_MS = 5 * 60 * 1000; // 5 mins

    private long mLastScan;

    private LocalBluetoothAdapter(BluetoothAdapter adapter) {
        mAdapter = adapter;
    }

    void setProfileManager(LocalBluetoothProfileManager manager) {
        mProfileManager = manager;
    }

    /**
     * Get the singleton instance of the LocalBluetoothAdapter. If this device
     * doesn't support Bluetooth, then null will be returned. Callers must be
     * prepared to handle a null return value.
     * @return the LocalBluetoothAdapter object, or null if not supported
     */
    static synchronized LocalBluetoothAdapter getInstance() {
        if (sInstance == null) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                sInstance = new LocalBluetoothAdapter(adapter);
            }
        }

        return sInstance;
    }

    // Pass-through BluetoothAdapter methods that we can intercept if necessary

    void cancelDiscovery() {
        mAdapter.cancelDiscovery();
    }

    boolean enable() {
        return mAdapter.enable();
    }

    boolean disable() {
        return mAdapter.disable();
    }

    void getProfileProxy(Context context,
            BluetoothProfile.ServiceListener listener, int profile) {
        mAdapter.getProfileProxy(context, listener, profile);
    }

    Set<BluetoothDevice> getBondedDevices() {
        return mAdapter.getBondedDevices();
    }

    String getName() {
        return mAdapter.getName();
    }

    int getScanMode() {
        return mAdapter.getScanMode();
    }

    int getState() {
        return mAdapter.getState();
    }

    ParcelUuid[] getUuids() {
        return mAdapter.getUuids();
    }

    boolean isDiscovering() {
        return mAdapter.isDiscovering();
    }

    boolean isEnabled() {
        return mAdapter.isEnabled();
    }

    void setDiscoverableTimeout(int timeout) {
        mAdapter.setDiscoverableTimeout(timeout);
    }

    void setName(String name) {
        mAdapter.setName(name);
    }

    void setScanMode(int mode) {
        mAdapter.setScanMode(mode);
    }

    boolean setScanMode(int mode, int duration) {
        return mAdapter.setScanMode(mode, duration);
    }

	int getScanType(int filterType)
	{
		if (BluetoothDevicePicker.FILTER_TYPE_PRX == filterType)
		{
			return BluetoothAdapter.TYPE_DUAL_MODE;
		}
		else
		{
			return BluetoothAdapter.TYPE_BR_EDR_ONLY;
		}
	}
	int getScanType(BluetoothDeviceFilter.Filter filter)
	{
		BluetoothDeviceFilter.Filter prxfilter = BluetoothDeviceFilter.getFilter(BluetoothDevicePicker.FILTER_TYPE_PRX);
		if (prxfilter != null && prxfilter.equals(filter))
		{
			return BluetoothAdapter.TYPE_DUAL_MODE;
		}
		else
		{
			return BluetoothAdapter.TYPE_BR_EDR_ONLY;
		}
	}
    void startScanning(boolean force) {
		startScanning(force, BluetoothAdapter.TYPE_BR_EDR_ONLY);
	}

    void startScanning(boolean force, int type) {
        // Only start if we're not already scanning
        if (!mAdapter.isDiscovering()) {
            if (!force) {
                // Don't scan more than frequently than SCAN_EXPIRATION_MS,
                // unless forced
                if (mLastScan + SCAN_EXPIRATION_MS > System.currentTimeMillis()) {
                    return;
                }

                // If we are playing music, don't scan unless forced.
                A2dpProfile a2dp = mProfileManager.getA2dpProfile();
                if (a2dp != null && a2dp.isA2dpPlaying()) {
                    return;
                }
            }

            if (mAdapter.startDiscovery(type)) {
                mLastScan = System.currentTimeMillis();
            }
        }
    }

    void stopScanning() {
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
    }

    public synchronized int getBluetoothState() {
        // Always sync state, in case it changed while paused
        syncBluetoothState();
        return mState;
    }

    synchronized void setBluetoothStateInt(int state) {
        mState = state;

        if (state == BluetoothAdapter.STATE_ON) {
            // if mProfileManager hasn't been constructed yet, it will
            // get the adapter UUIDs in its constructor when it is.
            if (mProfileManager != null) {
                mProfileManager.setBluetoothStateOn();
            }
        }
    }

    // Returns true if the state changed; false otherwise.
    boolean syncBluetoothState() {
        int currentState = mAdapter.getState();
        if (currentState != mState) {
            setBluetoothStateInt(mAdapter.getState());
            return true;
        }
        return false;
    }

    public void setBluetoothEnabled(boolean enabled) {
        boolean success = enabled
                ? mAdapter.enable()
                : mAdapter.disable();

        if (success) {
            setBluetoothStateInt(enabled
                ? BluetoothAdapter.STATE_TURNING_ON
                : BluetoothAdapter.STATE_TURNING_OFF);
        } else {
            if (Utils.V) {
                Log.v(TAG, "setBluetoothEnabled call, manager didn't return " +
                        "success for enabled: " + enabled);
            }

            syncBluetoothState();
        }
    }
}
