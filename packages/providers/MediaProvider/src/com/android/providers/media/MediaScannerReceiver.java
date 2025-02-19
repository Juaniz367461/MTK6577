/* //device/content/providers/media/src/com/android/providers/media/MediaScannerReceiver.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.android.providers.media;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaScannerReceiver extends BroadcastReceiver
{
    private final static String TAG = "MediaScannerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // scan internal storage
            scan(context, MediaProvider.INTERNAL_VOLUME);
        } else {
            Uri uri = intent.getData();
            if (uri != null && uri.getScheme().equals("file")) {
                // handle intents related to external storage
                String path = uri.getPath();
                Log.d(TAG, "action: " + action + " path: " + path);
                if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    boolean mountAll = intent.getBooleanExtra("mount_unmount_all", false);
                    requestScan(context, path, mountAll);
                } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE) && 
                        inScanDirectory(context, path)) {
                    scanFile(context, path);
                }
            }
        }
    }

    private void scan(Context context, String volume) {
        Bundle args = new Bundle();
        args.putString("volume", volume);
        context.startService(
                new Intent(context, MediaScannerService.class).putExtras(args));
    }

    private void scanFile(Context context, String path) {
        Bundle args = new Bundle();
        args.putString("filepath", path);
        context.startService(
                new Intent(context, MediaScannerService.class).putExtras(args));
    }

    private boolean inScanDirectory(Context context, String path) {
        StorageManager storageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        String[] directories = storageManager.getVolumePaths();
        if (path == null || directories == null) {
            return false;
        }
        
        for (int i = 0; i < directories.length; i++) {
            String directory = directories[i];
            if (path.startsWith(directory)) {
                return true;
            }
        }
        return false;
    }

    /// M: Delays the scan to all sdcards are mounted. @{
    private void requestScan(Context context, String path, boolean mountAll) {
        if (LOG) MtkLog.v(TAG, "requestScan: path=" + path + ",mountAll=" + mountAll);
        if (mountAll) {
            if (getMountedStorage().contains(path)) {
                if (LOG) MtkLog.v(TAG, "requestScan: New scan.");
                // Next mount occurs, remove last time checker
                clear();
            }
            getMountedStorage().add(path);
            if (isAllMounted(context)) {
                if (LOG) MtkLog.v(TAG, "requestScan: All are ready and starts to scan.");
                getHandler().removeMessages(MSG_CHECK_TIMEOUT);
                scan(context, MediaProvider.EXTERNAL_VOLUME);
                clear();
            } else {
                if (!getHandler().hasMessages(MSG_CHECK_TIMEOUT)) {
                    Message msg = getHandler().obtainMessage(MSG_CHECK_TIMEOUT);
                    msg.obj = context;
                    msg.arg1 = CHECK_INTERVAL;
                    getHandler().sendMessageDelayed(msg, CHECK_INTERVAL);
                }
            }
        } else {
            if (LOG) MtkLog.v(TAG, "requestScan: Starts to scan the single sdcard.");
            //if mount only 1 sdcard, scan it directly.
            scan(context, MediaProvider.EXTERNAL_VOLUME);
            clear();
        }
    }

    private ArrayList<String> getAllExternalStorage(Context context) {
        if (sAllExternalStorage == null) {
            sAllExternalStorage = new ArrayList<String>();
            StorageManager storageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
            String[] directories = storageManager.getVolumePaths();
            int size = (directories != null) ? directories.length : 0;
            for(int i = 0; i < size; i++) {
                sAllExternalStorage.add(directories[i]);
            }
            if (LOG) MtkLog.v(TAG, "allStorage: " + dumpList(sAllExternalStorage));
        }
        return sAllExternalStorage;
    }

    private ArrayList<String> getMountedStorage() {
        if (sMountedStorage == null) {
            sMountedStorage = new ArrayList<String>();
        }
        return sMountedStorage;
    }

    private boolean isAllMounted(Context context) {
        ArrayList<String> allStorage = getAllExternalStorage(context);
        ArrayList<String> mountedStorage = getMountedStorage();
        int total = allStorage.size();
        int mounted = mountedStorage.size();
        if (mounted < total) {
            return false;
        }
        
        int ready = 0;
        for(int i = 0; i < mounted; i++) {
            if(allStorage.contains(mountedStorage.get(i))) {
                ready++;
            } else {
                MtkLog.e(TAG, "isAllMounted: invalidStorage:" + mountedStorage.get(i));
            }
        }
        return total == ready;
    }

    private String dumpList(List<String> list) {
        if (list != null) {
            int size = list.size();
            StringBuilder sb = new StringBuilder("size:");
            sb.append(size);
            if (size > 0) {
                sb.append(", values:");
                for(int i = 0; i < size - 1; i++) {
                    sb.append(list.get(i)).append(",");
                }
                sb.append(list.get(size - 1));
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    private Handler getHandler() {
        if (sHandler == null) {
            sHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (MSG_CHECK_TIMEOUT == msg.what) {
                        if (msg.arg1 >= TIMEOUT_VALUE) {
                            MtkLog.w(TAG, "handler: Timeout and starts to scan. mountedStorage:"
                                    + dumpList(sMountedStorage));
                            // Timeout occurs, but not all sdcard were mounted.
                            // Keeps mounted list for later check.
                            scan((Context)msg.obj, MediaProvider.EXTERNAL_VOLUME);
                        } else {
                            Message copy = new Message();
                            copy.copyFrom(msg);
                            copy.arg1 += CHECK_INTERVAL;
                            sendMessageDelayed(copy, CHECK_INTERVAL);
                        }
                    }
                }
            };
        }
        return sHandler;
    }

    private void clear() {
        if (sHandler != null) {
            sHandler.removeMessages(MSG_CHECK_TIMEOUT);
            sHandler = null;
        }
        sMountedStorage = null;
    }

    private static final boolean LOG = true;
    private static final int MSG_CHECK_TIMEOUT = 1;
    private static final int CHECK_INTERVAL = 100;
    private static final int TIMEOUT_VALUE = 1000;
    private static Handler sHandler;
    private static ArrayList<String> sAllExternalStorage;
    private static ArrayList<String> sMountedStorage;
    /// M: @}
}
