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

package  com.android.pqtuningtool.data;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.mtp.MtpDevice;
import android.mtp.MtpDeviceInfo;
import android.mtp.MtpObjectInfo;
import android.mtp.MtpStorageInfo;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class helps an application manage a list of connected MTP or PTP devices.
 * It listens for MTP devices being attached and removed from the USB host bus
 * and notifies the application when the MTP device list changes.
 */
public class MtpClient {

    private static final String TAG = "MtpClient";

    private static final String ACTION_USB_PERMISSION =
            "android.mtp.MtpClient.action.USB_PERMISSION";

    private final Context mContext;
    private final UsbManager mUsbManager;
    private final ArrayList<Listener> mListeners = new ArrayList<Listener>();
    // mDevices contains all MtpDevices that have been seen by our client,
    // so we can inform when the device has been detached.
    // mDevices is also used for synchronization in this class.
    private final HashMap<String, MtpDevice> mDevices = new HashMap<String, MtpDevice>();
    // List of MTP devices we should not try to open for which we are currently
    // asking for permission to open.
    private final ArrayList<String> mRequestPermissionDevices = new ArrayList<String>();
    // List of MTP devices we should not try to open.
    // We add devices to this list if the user canceled a permission request or we were
    // unable to open the device.
    private final ArrayList<String> mIgnoredDevices = new ArrayList<String>();

    private final PendingIntent mPermissionIntent;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            String deviceName = usbDevice.getDeviceName();

            synchronized (mDevices) {
                MtpDevice mtpDevice = mDevices.get(deviceName);

                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    if (mtpDevice == null) {
                        mtpDevice = openDeviceLocked(usbDevice);
                    }
                    if (mtpDevice != null) {
                        for (Listener listener : mListeners) {
                            listener.deviceAdded(mtpDevice);
                        }
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    if (mtpDevice != null) {
                        mDevices.remove(deviceName);
                        mRequestPermissionDevices.remove(deviceName);
                        mIgnoredDevices.remove(deviceName);
                        for (Listener listener : mListeners) {
                            listener.deviceRemoved(mtpDevice);
                        }
                    }
                } else if (ACTION_USB_PERMISSION.equals(action)) {
                    mRequestPermissionDevices.remove(deviceName);
                    boolean permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
                            false);
                    Log.d(TAG, "ACTION_USB_PERMISSION: " + permission);
                    if (permission) {
                        if (mtpDevice == null) {
                            mtpDevice = openDeviceLocked(usbDevice);
                        }
                        if (mtpDevice != null) {
                            for (Listener listener : mListeners) {
                                listener.deviceAdded(mtpDevice);
                            }
                        }
                    } else {
                        // so we don't ask for permission again
                        mIgnoredDevices.add(deviceName);
                    }
                }
            }
        }
    };

    /**
     * An interface for being notified when MTP or PTP devices are attached
     * or removed.  In the current implementation, only PTP devices are supported.
     */
    public interface Listener {
        /**
         * Called when a new device has been added
         *
         * @param device the new device that was added
         */
        public void deviceAdded(MtpDevice device);

        /**
         * Called when a new device has been removed
         *
         * @param device the device that was removed
         */
        public void deviceRemoved(MtpDevice device);
    }

    /**
     * Tests to see if a {@link android.hardware.usb.UsbDevice}
     * supports the PTP protocol (typically used by digital cameras)
     *
     * @param device the device to test
     * @return true if the device is a PTP device.
     */
    static public boolean isCamera(UsbDevice device) {
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == UsbConstants.USB_CLASS_STILL_IMAGE &&
                    intf.getInterfaceSubclass() == 1 &&
                    intf.getInterfaceProtocol() == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * MtpClient constructor
     *
     * @param context the {@link android.content.Context} to use for the MtpClient
     */
    public MtpClient(Context context) {
        mContext = context;
        mUsbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        context.registerReceiver(mUsbReceiver, filter);
    }

    /**
     * Opens the {@link android.hardware.usb.UsbDevice} for an MTP or PTP
     * device and return an {@link android.mtp.MtpDevice} for it.
     *
     * @param device the device to open
     * @return an MtpDevice for the device.
     */
    private MtpDevice openDeviceLocked(UsbDevice usbDevice) {
        String deviceName = usbDevice.getDeviceName();

        // don't try to open devices that we have decided to ignore
        // or are currently asking permission for
        if (isCamera(usbDevice) && !mIgnoredDevices.contains(deviceName)
                && !mRequestPermissionDevices.contains(deviceName)) {
            if (!mUsbManager.hasPermission(usbDevice)) {
                mUsbManager.requestPermission(usbDevice, mPermissionIntent);
                mRequestPermissionDevices.add(deviceName);
            } else {
                UsbDeviceConnection connection = mUsbManager.openDevice(usbDevice);
                if (connection != null) {
                    MtpDevice mtpDevice = new MtpDevice(usbDevice);
                    if (mtpDevice.open(connection)) {
                        mDevices.put(usbDevice.getDeviceName(), mtpDevice);
                        return mtpDevice;
                    } else {
                        // so we don't try to open it again
                        mIgnoredDevices.add(deviceName);
                    }
                } else {
                    // so we don't try to open it again
                    mIgnoredDevices.add(deviceName);
                }
            }
        }
        return null;
    }

    /**
     * Closes all resources related to the MtpClient object
     */
    public void close() {
        mContext.unregisterReceiver(mUsbReceiver);
    }

    /**
     * Registers a {@link android.mtp.MtpClient.Listener} interface to receive
     * notifications when MTP or PTP devices are added or removed.
     *
     * @param listener the listener to register
     */
    public void addListener(Listener listener) {
        synchronized (mDevices) {
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
            }
        }
    }

    /**
     * Unregisters a {@link android.mtp.MtpClient.Listener} interface.
     *
     * @param listener the listener to unregister
     */
    public void removeListener(Listener listener) {
        synchronized (mDevices) {
            mListeners.remove(listener);
        }
    }

    /**
     * Retrieves an {@link android.mtp.MtpDevice} object for the USB device
     * with the given name.
     *
     * @param deviceName the name of the USB device
     * @return the MtpDevice, or null if it does not exist
     */
    public MtpDevice getDevice(String deviceName) {
        synchronized (mDevices) {
            return mDevices.get(deviceName);
        }
    }

    /**
     * Retrieves an {@link android.mtp.MtpDevice} object for the USB device
     * with the given ID.
     *
     * @param id the ID of the USB device
     * @return the MtpDevice, or null if it does not exist
     */
    public MtpDevice getDevice(int id) {
        synchronized (mDevices) {
            return mDevices.get(UsbDevice.getDeviceName(id));
        }
    }

    /**
     * Retrieves a list of all currently connected {@link android.mtp.MtpDevice}.
     *
     * @return the list of MtpDevices
     */
    public List<MtpDevice> getDeviceList() {
        synchronized (mDevices) {
            // Query the USB manager since devices might have attached
            // before we added our listener.
            for (UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
                if (mDevices.get(usbDevice.getDeviceName()) == null) {
                    openDeviceLocked(usbDevice);
                }
            }

            return new ArrayList<MtpDevice>(mDevices.values());
        }
    }

    /**
     * Retrieves a list of all {@link android.mtp.MtpStorageInfo}
     * for the MTP or PTP device with the given USB device name
     *
     * @param deviceName the name of the USB device
     * @return the list of MtpStorageInfo
     */
    public List<MtpStorageInfo> getStorageList(String deviceName) {
        MtpDevice device = getDevice(deviceName);
        if (device == null) {
            return null;
        }
        int[] storageIds = device.getStorageIds();
        if (storageIds == null) {
            return null;
        }

        int length = storageIds.length;
        ArrayList<MtpStorageInfo> storageList = new ArrayList<MtpStorageInfo>(length);
        for (int i = 0; i < length; i++) {
            MtpStorageInfo info = device.getStorageInfo(storageIds[i]);
            if (info == null) {
                Log.w(TAG, "getStorageInfo failed");
            } else {
                storageList.add(info);
            }
        }
        return storageList;
    }

    /**
     * Retrieves the {@link android.mtp.MtpObjectInfo} for an object on
     * the MTP or PTP device with the given USB device name with the given
     * object handle
     *
     * @param deviceName the name of the USB device
     * @param objectHandle handle of the object to query
     * @return the MtpObjectInfo
     */
    public MtpObjectInfo getObjectInfo(String deviceName, int objectHandle) {
        MtpDevice device = getDevice(deviceName);
        if (device == null) {
            return null;
        }
        return device.getObjectInfo(objectHandle);
    }

    /**
     * Deletes an object on the MTP or PTP device with the given USB device name.
     *
     * @param deviceName the name of the USB device
     * @param objectHandle handle of the object to delete
     * @return true if the deletion succeeds
     */
    public boolean deleteObject(String deviceName, int objectHandle) {
        MtpDevice device = getDevice(deviceName);
        if (device == null) {
            return false;
        }
        return device.deleteObject(objectHandle);
    }

    /**
     * Retrieves a list of {@link android.mtp.MtpObjectInfo} for all objects
     * on the MTP or PTP device with the given USB device name and given storage ID
     * and/or object handle.
     * If the object handle is zero, then all objects in the root of the storage unit
     * will be returned. Otherwise, all immediate children of the object will be returned.
     * If the storage ID is also zero, then all objects on all storage units will be returned.
     *
     * @param deviceName the name of the USB device
     * @param storageId the ID of the storage unit to query, or zero for all
     * @param objectHandle the handle of the parent object to query, or zero for the storage root
     * @return the list of MtpObjectInfo
     */
    public List<MtpObjectInfo> getObjectList(String deviceName, int storageId, int objectHandle) {
        MtpDevice device = getDevice(deviceName);
        if (device == null) {
            return null;
        }
        if (objectHandle == 0) {
            // all objects in root of storage
            objectHandle = 0xFFFFFFFF;
        }
        int[] handles = device.getObjectHandles(storageId, 0, objectHandle);
        if (handles == null) {
            return null;
        }

        int length = handles.length;
        ArrayList<MtpObjectInfo> objectList = new ArrayList<MtpObjectInfo>(length);
        for (int i = 0; i < length; i++) {
            MtpObjectInfo info = device.getObjectInfo(handles[i]);
            if (info == null) {
                Log.w(TAG, "getObjectInfo failed");
            } else {
                objectList.add(info);
            }
        }
        return objectList;
    }

    /**
     * Returns the data for an object as a byte array.
     *
     * @param deviceName the name of the USB device containing the object
     * @param objectHandle handle of the object to read
     * @param objectSize the size of the object (this should match
     *      {@link android.mtp.MtpObjectInfo#getCompressedSize}
     * @return the object's data, or null if reading fails
     */
    public byte[] getObject(String deviceName, int objectHandle, int objectSize) {
        MtpDevice device = getDevice(deviceName);
        if (device == null) {
            return null;
        }
        return device.getObject(objectHandle, objectSize);
    }

    /**
     * Returns the thumbnail data for an object as a byte array.
     *
     * @param deviceName the name of the USB device containing the object
     * @param objectHandle handle of the object to read
     * @return the object's thumbnail, or null if reading fails
     */
    public byte[] getThumbnail(String deviceName, int objectHandle) {
        MtpDevice device = getDevice(deviceName);
        if (device == null) {
            return null;
        }
        return device.getThumbnail(objectHandle);
    }

    /**
     * Copies the data for an object to a file in external storage.
     *
     * @param deviceName the name of the USB device containing the object
     * @param objectHandle handle of the object to read
     * @param destPath path to destination for the file transfer.
     *      This path should be in the external storage as defined by
     *      {@link android.os.Environment#getExternalStorageDirectory}
     * @return true if the file transfer succeeds
     */
    public boolean importFile(String deviceName, int objectHandle, String destPath) {
        MtpDevice device = getDevice(deviceName);
        if (device == null) {
            return false;
        }
        return device.importFile(objectHandle, destPath);
    }
}
