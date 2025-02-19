/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.providers.drm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.drm.DrmManagerClient;
import android.drm.DrmUtils;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.featureoption.FeatureOption;

public class DrmService extends Service {
    private static final String TAG = "DrmService";

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, "onStart");

        Resources res = this.getResources();
        String text = null;

        String cid = (String)intent.getExtra("cid");
        if (cid == null || cid.isEmpty()) {
            Log.e(TAG, "onStart: cid: [" + cid + "] invalid.");
            // show error notification
            text = res.getString(com.mediatek.internal.R.string.drm_license_install_fail);
        } else {
            Log.d(TAG, "cid: [" + cid + "] valid.");
            // scan related dcf files
            int count = DrmUtils.rescanDrmMediaFiles(this, cid, null);
            Log.d(TAG, "onStart: rights object installed, scan [" + count + "] dcf files for content id: [" + cid + "].");
            // show success notification
            text = res.getString(com.mediatek.internal.R.string.drm_license_install_success);
        }

        NotificationManager nm =
            (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification =
            new Notification(com.mediatek.internal.R.drawable.drm_stat_notify_wappush,
                             text, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        Intent i = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i, 0);
        notification.setLatestEventInfo(this, text, text, pendingIntent);

        nm.notify(0, notification);

        stopSelf();
    }
}

