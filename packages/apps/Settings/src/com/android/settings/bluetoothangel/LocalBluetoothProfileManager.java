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

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
/* MTK Added : Begin */
import android.bluetooth.BluetoothFtp;
/* MTK Added : End */
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * LocalBluetoothProfileManager provides access to the LocalBluetoothProfile
 * objects for the available Bluetooth profiles.
 */
final class LocalBluetoothProfileManager {
    private static final String TAG = "LocalBluetoothProfileManager";
    
    static final ParcelUuid[] BPP_PROFILE_UUIDS = new ParcelUuid[] {
        BluetoothUuid.BppReceiver
    };
    static final ParcelUuid[] BIP_PROFILE_UUIDS = new ParcelUuid[] {
        BluetoothUuid.BipResponder
    };
    static final ParcelUuid[] HID_PROFILE_UUIDS = new ParcelUuid[] {
		BluetoothUuid.Hid
	};
    static final ParcelUuid[] PRX_PROFILE_UUIDS = new ParcelUuid[] {
		BluetoothUuid.Proximity
    };
    
    /** Singleton instance. */
    private static LocalBluetoothProfileManager sInstance;

    /**
     * An interface for notifying BluetoothHeadset IPC clients when they have
     * been connected to the BluetoothHeadset service.
     * Only used by {@link DockService}.
     */
    public interface ServiceListener {
        /**
         * Called to notify the client when this proxy object has been
         * connected to the BluetoothHeadset service. Clients must wait for
         * this callback before making IPC calls on the BluetoothHeadset
         * service.
         */
        void onServiceConnected();

        /**
         * Called to notify the client that this proxy object has been
         * disconnected from the BluetoothHeadset service. Clients must not
         * make IPC calls on the BluetoothHeadset service after this callback.
         * This callback will currently only occur if the application hosting
         * the BluetoothHeadset service, but may be called more often in future.
         */
        void onServiceDisconnected();
    }
    
    private final Context mContext;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private final BluetoothEventManager mEventManager;

    private A2dpProfile mA2dpProfile;
    private HeadsetProfile mHeadsetProfile;
    private final HidProfile mHidProfile;
    private OppProfile mOppProfile;
    private final PanProfile mPanProfile;
    private FtpProfile mFtpProfile;

    /**
     * Mapping from profile name, e.g. "HEADSET" to profile object.
     */
    private final Map<String, LocalBluetoothProfile>
            mProfileNameMap = new HashMap<String, LocalBluetoothProfile>();

    LocalBluetoothProfileManager(Context context,
            LocalBluetoothAdapter adapter,
            CachedBluetoothDeviceManager deviceManager,
            BluetoothEventManager eventManager) {
        mContext = context;

        mLocalAdapter = adapter;
        mDeviceManager = deviceManager;
        mEventManager = eventManager;
        // pass this reference to adapter and event manager (circular dependency)
        mLocalAdapter.setProfileManager(this);
        mEventManager.setProfileManager(this);

        ParcelUuid[] uuids = adapter.getUuids();

        // uuids may be null if Bluetooth is turned off
        if (uuids != null) {
            Log.d(TAG, "adapter uuid is not null");
            for (ParcelUuid uuid : uuids) {
                Log.v(TAG, "  " + uuid);
            }
        	updateLocalProfiles(uuids);
        }

        // Always add HID and PAN profiles
        mHidProfile = new HidProfile(context, mLocalAdapter);
        addProfile(mHidProfile, HidProfile.NAME,
                BluetoothInputDevice.ACTION_CONNECTION_STATE_CHANGED);

        mPanProfile = new PanProfile(context);
        addPanProfile(mPanProfile, PanProfile.NAME,
                BluetoothPan.ACTION_CONNECTION_STATE_CHANGED);

		// Always add FTP profile for FTP client.
		// If FTP client is removed in the future, this requirement can be done by
		// setting the return value of FtpProfile.isConnectable() to false.
		mFtpProfile = new FtpProfile(mContext);
		addProfile(mFtpProfile, FtpProfile.NAME,
				BluetoothFtp.ACTION_STATE_CHANGED);

        Log.d(TAG, "LocalBluetoothProfileManager construction complete");
    }

    /**
     * Initialize or update the local profile objects. If a UUID was previously
     * present but has been removed, we print a warning but don't remove the
     * profile object as it might be referenced elsewhere, or the UUID might
     * come back and we don't want multiple copies of the profile objects.
     * @param uuids
     */
    void updateLocalProfiles(ParcelUuid[] uuids) {
        // A2DP
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.AudioSource)) {
            if (mA2dpProfile == null) {
                Log.d(TAG, "Adding local A2DP profile");
                mA2dpProfile = new A2dpProfile(mContext);
                addProfile(mA2dpProfile, A2dpProfile.NAME,
                        BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
           }
        } else if (mA2dpProfile != null) {
            Log.w(TAG, "Warning: A2DP profile was previously added but the UUID is now missing.");
        }

        // Headset / Handsfree
        /*if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree_AG) ||
            BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP_AG)) { */
            if (mHeadsetProfile == null) {
                Log.d(TAG, "Adding local HEADSET profile");
                mHeadsetProfile = new HeadsetProfile(mContext, mLocalAdapter,
                        mDeviceManager, this);
                addProfile(mHeadsetProfile, HeadsetProfile.NAME,
                        BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
            }
        
        /*} else if (mHeadsetProfile != null) {
            Log.w(TAG, "Warning: HEADSET profile was previously added but the UUID is now missing.");
        }*/

        // OPP
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.ObexObjectPush)) {
            if (mOppProfile == null) {
                Log.d(TAG, "Adding local OPP profile");
                mOppProfile = new OppProfile();
                // Note: no event handler for OPP, only name map.
                mProfileNameMap.put(OppProfile.NAME, mOppProfile);
            }
        } else if (mOppProfile != null) {
            Log.w(TAG, "Warning: OPP profile was previously added but the UUID is now missing.");
        }
        
        mEventManager.registerProfileIntentReceiver();

        // There is no local SDP record for HID and Settings app doesn't control PBAP
    }

    private final Collection<ServiceListener> mServiceListeners =
            new ArrayList<ServiceListener>();

    private void addProfile(LocalBluetoothProfile profile,
            String profileName, String stateChangedAction) {
        mEventManager.addProfileHandler(stateChangedAction, new StateChangedHandler(profile));
        mProfileNameMap.put(profileName, profile);
    }

    private void addPanProfile(LocalBluetoothProfile profile,
            String profileName, String stateChangedAction) {
        mEventManager.addProfileHandler(stateChangedAction,
                new PanStateChangedHandler(profile));
        mProfileNameMap.put(profileName, profile);
    }

    LocalBluetoothProfile getProfileByName(String name) {
        return mProfileNameMap.get(name);
    }

    // Called from LocalBluetoothAdapter when state changes to ON
    void setBluetoothStateOn() {
        ParcelUuid[] uuids = mLocalAdapter.getUuids();
        if (uuids != null) {
            updateLocalProfiles(uuids);
        }
        mEventManager.readPairedDevices();
    }

    /**
     * Generic handler for connection state change events for the specified profile.
     */
    private class StateChangedHandler implements BluetoothEventManager.Handler {
        final LocalBluetoothProfile mProfile;

        StateChangedHandler(LocalBluetoothProfile profile) {
            mProfile = profile;
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w(TAG, "StateChangedHandler found new device: " + device);
                cachedDevice = mDeviceManager.addDevice(mLocalAdapter,
                        LocalBluetoothProfileManager.this, device);
            }
            int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
            int oldState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);
            if (newState == BluetoothProfile.STATE_DISCONNECTED &&
                    oldState == BluetoothProfile.STATE_CONNECTING) {
                Log.i(TAG, "Failed to connect " + mProfile + " device");
            }

            cachedDevice.onProfileStateChanged(mProfile, newState);
            cachedDevice.refresh();
        }
    }

    /** State change handler for NAP and PANU profiles. */
    private class PanStateChangedHandler extends StateChangedHandler {

        PanStateChangedHandler(LocalBluetoothProfile profile) {
            super(profile);
        }

        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            PanProfile panProfile = (PanProfile) mProfile;
            int role = intent.getIntExtra(BluetoothPan.EXTRA_LOCAL_ROLE, 0);
            panProfile.setLocalRole(device, role);
            super.onReceive(context, intent, device);
        }
    }

    // called from DockService
    void addServiceListener(ServiceListener l) {
        mServiceListeners.add(l);
    }

    // called from DockService
    void removeServiceListener(ServiceListener l) {
        mServiceListeners.remove(l);
    }

    // not synchronized: use only from UI thread! (TODO: verify)
    void callServiceConnectedListeners() {
        for (ServiceListener l : mServiceListeners) {
            l.onServiceConnected();
        }
    }

    // not synchronized: use only from UI thread! (TODO: verify)
    void callServiceDisconnectedListeners() {
        for (ServiceListener listener : mServiceListeners) {
            listener.onServiceDisconnected();
        }
    }

    // This is called by DockService, so check Headset and A2DP.
    public synchronized boolean isManagerReady() {
        // Getting just the headset profile is fine for now. Will need to deal with A2DP
        // and others if they aren't always in a ready state.
        LocalBluetoothProfile profile = mHeadsetProfile;
        if (profile != null) {
            return profile.isProfileReady();
        }
        profile = mA2dpProfile;
        if (profile != null) {
            return profile.isProfileReady();
        }
        return false;
    }

    A2dpProfile getA2dpProfile() {
        return mA2dpProfile;
    }

    HeadsetProfile getHeadsetProfile() {
        return mHeadsetProfile;
    }

    /**
     * Fill in a list of LocalBluetoothProfile objects that are supported by
     * the local device and the remote device.
     *
     * @param uuids of the remote device
     * @param localUuids UUIDs of the local device
     * @param profiles The list of profiles to fill
     * @param removedProfiles list of profiles that were removed
     */
    synchronized void updateProfiles(ParcelUuid[] uuids, ParcelUuid[] localUuids,
            Collection<LocalBluetoothProfile> profiles,
            Collection<LocalBluetoothProfile> removedProfiles) {
        // Copy previous profile list into removedProfiles
        removedProfiles.clear();
        removedProfiles.addAll(profiles);
        profiles.clear();
        Log.d(TAG, "update profiles");
        if (uuids == null) {
            Log.d(TAG, "uuid is null");
            return;
        }
        
        Log.d(TAG, "Before update mprofiles, updateLocalProfiles");
        if(localUuids != null) {
        	updateLocalProfiles(localUuids);
        }
        
        if (mHeadsetProfile != null) {
        	Log.d(TAG, "mHeadsetProfile is not null");
            if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP) ||
                BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree)) {
                    profiles.add(mHeadsetProfile);
                    removedProfiles.remove(mHeadsetProfile);
            }
        }
        
        if (BluetoothUuid.containsAnyUuid(uuids, A2dpProfile.SINK_UUIDS) &&
            mA2dpProfile != null) {
        	Log.d(TAG, "A2dpProfile is not null");
            profiles.add(mA2dpProfile);
            removedProfiles.remove(mA2dpProfile);
        }

        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.ObexObjectPush) &&
            mOppProfile != null) {
            profiles.add(mOppProfile);
            removedProfiles.remove(mOppProfile);
        }

        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Hid) &&
            mHidProfile != null) {
            profiles.add(mHidProfile);
            removedProfiles.remove(mHidProfile);
        }

        if (mPanProfile != null) {
            if(BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.NAP) && 
                    BluetoothUuid.isUuidPresent(localUuids, BluetoothUuid.PANU)) {
                profiles.add(mPanProfile);
                removedProfiles.remove(mPanProfile);
            }
        }
        
        /* MTK Added : Begin */
    	if (BluetoothUuid.containsAnyUuid(uuids, FtpProfile.UUIDS) && 
    		mFtpProfile != null) {
    		profiles.add(mFtpProfile);
            removedProfiles.remove(mFtpProfile);
    	}
    	/* MTK Added : End */
    }
}
