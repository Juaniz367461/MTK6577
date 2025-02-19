/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.contacts.vcard;

import com.android.contacts.R;
import android.util.Log;
import com.android.vcard.VCardEntry;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.RawContacts;
import android.widget.RemoteViews;
import android.widget.Toast;

public class NotificationImportExportListener implements VCardImportExportListener,
        Handler.Callback {
    /** The tag used by vCard-related notifications. */
    /* package */ static final String DEFAULT_NOTIFICATION_TAG = "VCardServiceProgress";
    /**
     * The tag used by vCard-related failure notifications.
     * <p>
     * Use a different tag from {@link #DEFAULT_NOTIFICATION_TAG} so that failures do not get
     * replaced by other notifications and vice-versa.
     */
    /* package */ static final String FAILURE_NOTIFICATION_TAG = "VCardServiceFailure";

    private final NotificationManager mNotificationManager;
    private final Activity mContext;
    private final Handler mHandler;

    public NotificationImportExportListener(Activity activity) {
        mContext = activity;
        mNotificationManager = (NotificationManager) activity.getSystemService(
                Context.NOTIFICATION_SERVICE);
        mHandler = new Handler(this);
    }

    @Override
    public boolean handleMessage(Message msg) {
        String text = (String) msg.obj;
        Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    public void onImportProcessed(ImportRequest request, int jobId, int sequence) {
        // Show a notification about the status
        final String displayName;
        final String message;
        if (request.displayName != null) {
            displayName = request.displayName;
            message = mContext.getString(R.string.vcard_import_will_start_message, displayName);
        } else {
            displayName = mContext.getString(R.string.vcard_unknown_filename);
            message = mContext.getString(
                    R.string.vcard_import_will_start_message_with_default_name);
        }

        // We just want to show notification for the first vCard.
        if (sequence == 0) {
            // TODO: Ideally we should detect the current status of import/export and
            // show "started" when we can import right now and show "will start" when
            // we cannot.
            mHandler.obtainMessage(0, message).sendToTarget();
        }

        final Notification notification = constructProgressNotification(mContext,
                VCardService.TYPE_IMPORT, message, message, jobId, displayName, -1, 0);
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
    }

    @Override
    public void onImportParsed(ImportRequest request, int jobId, VCardEntry entry, int currentCount,
            int totalCount) {
        if (entry.isIgnorable()) {
            return;
        }

        final String totalCountString = String.valueOf(totalCount);
        final String tickerText =
                mContext.getString(R.string.progress_notifier_message,
                        String.valueOf(currentCount),
                        totalCountString,
                        entry.getDisplayName());
        final String description = mContext.getString(R.string.importing_vcard_description,
                entry.getDisplayName());

        final Notification notification = constructProgressNotification(
                mContext.getApplicationContext(), VCardService.TYPE_IMPORT, description, tickerText,
                jobId, request.displayName, totalCount, currentCount);
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
    }

    @Override
    public void onImportFinished(ImportRequest request, int jobId, Uri createdUri) {
        final String description = mContext.getString(R.string.importing_vcard_finished_title,
                request.displayName);
        final Intent intent;
        if (createdUri != null) {
            final long rawContactId = ContentUris.parseId(createdUri);
            final Uri contactUri = RawContacts.getContactLookupUri(
                    mContext.getContentResolver(), ContentUris.withAppendedId(
                            RawContacts.CONTENT_URI, rawContactId));
            intent = new Intent(Intent.ACTION_VIEW, contactUri);
        } else {
            intent = null;
        }
        final Notification notification =
                NotificationImportExportListener.constructFinishNotification(VCardService.TYPE_IMPORT,mContext,
                description, null, intent);
        mNotificationManager.notify(NotificationImportExportListener.DEFAULT_NOTIFICATION_TAG,
                jobId, notification);
    }

    @Override
    public void onImportFailed(ImportRequest request) {
        // TODO: a little unkind to show Toast in this case, which is shown just a moment.
        // Ideally we should show some persistent something users can notice more easily.
        mHandler.obtainMessage(0,
                mContext.getString(R.string.vcard_import_request_rejected_message)).sendToTarget();
    }

    @Override
    public void onImportCanceled(ImportRequest request, int jobId) {
        final String description = mContext.getString(R.string.importing_vcard_canceled_title,
                request.displayName);
        final Notification notification =
                NotificationImportExportListener.constructCancelNotification(mContext, description);
        mNotificationManager.notify(NotificationImportExportListener.DEFAULT_NOTIFICATION_TAG,
                jobId, notification);
    }

    @Override
    public void onExportProcessed(ExportRequest request, int jobId) {
        final String displayName = request.destUri.getLastPathSegment();
        final String message = mContext.getString(R.string.vcard_export_will_start_message,
                displayName);

        mHandler.obtainMessage(0, message).sendToTarget();
        final Notification notification =
                NotificationImportExportListener.constructProgressNotification(mContext,
                        VCardService.TYPE_EXPORT, message, message, jobId, displayName, -1, 0);
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, jobId, notification);
    }

    @Override
    public void onExportFailed(ExportRequest request) {
        mHandler.obtainMessage(0,
                mContext.getString(R.string.vcard_export_request_rejected_message)).sendToTarget();
    }

    @Override
    public void onCancelRequest(CancelRequest request, int type) {
        final String description = type == VCardService.TYPE_IMPORT ?
                mContext.getString(R.string.importing_vcard_canceled_title, request.displayName) :
                mContext.getString(R.string.exporting_vcard_canceled_title, request.displayName);
        final Notification notification = constructCancelNotification(mContext, description);
        mNotificationManager.notify(DEFAULT_NOTIFICATION_TAG, request.jobId, notification);
    }

    /**
     * Constructs a {@link Notification} showing the current status of import/export.
     * Users can cancel the process with the Notification.
     *
     * @param context
     * @param type import/export
     * @param description Content of the Notification.
     * @param tickerText
     * @param jobId
     * @param displayName Name to be shown to the Notification (e.g. "finished importing XXXX").
     * Typycally a file name.
     * @param totalCount The number of vCard entries to be imported. Used to show progress bar.
     * -1 lets the system show the progress bar with "indeterminate" state.
     * @param currentCount The index of current vCard. Used to show progress bar.
     */
    /* package */ static Notification constructProgressNotification(
            Context context, int type, String description, String tickerText,
            int jobId, String displayName, int totalCount, int currentCount) {
        // Note: We cannot use extra values here (like setIntExtra()), as PendingIntent doesn't
        // preserve them across multiple Notifications. PendingIntent preserves the first extras
        // (when flag is not set), or update them when PendingIntent#getActivity() is called
        // (See PendingIntent#FLAG_UPDATE_CURRENT). In either case, we cannot preserve extras as we
        // expect (for each vCard import/export request).
        //
        // We use query parameter in Uri instead.
        // Scheme and Authority is arbitorary, assuming CancelActivity never refers them.
        final Intent intent = new Intent(context, CancelActivity.class);
        final Uri uri = (new Uri.Builder())
                .scheme("invalidscheme")
                .authority("invalidauthority")
                .appendQueryParameter(CancelActivity.JOB_ID, String.valueOf(jobId))
                .appendQueryParameter(CancelActivity.DISPLAY_NAME, displayName)
                .appendQueryParameter(CancelActivity.TYPE, String.valueOf(type)).build();
        intent.setData(uri);

        final Notification.Builder builder = new Notification.Builder(context);
        builder.setOngoing(true)
                .setProgress(totalCount, currentCount, totalCount == - 1)
                /*.setTicker(tickerText)*/
                .setContentTitle(description)
                .setSmallIcon(type == VCardService.TYPE_IMPORT ? android.R.drawable.stat_sys_download_done : R.drawable.stat_sys_upload_done_hold_dark)
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
        if (totalCount > 0) {
            builder.setContentText(context.getString(R.string.percentage,
                    String.valueOf(currentCount * 100 / totalCount)));
        }
        return builder.getNotification();
    }

    /**
     * Constructs a Notification telling users the process is canceled.
     *
     * @param context
     * @param description Content of the Notification
     */
    /* package */ static Notification constructCancelNotification(
            Context context, String description) {
        return new Notification.Builder(context)
                .setAutoCancel(true)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(description)
                .setContentText(description)
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0))
                .getNotification();
    }

    /**
     * Constructs a Notification telling users the process is finished.
     *
     * @param context
     * @param description Content of the Notification
     * @param intent Intent to be launched when the Notification is clicked. Can be null.
     */
    /* package */ static Notification constructFinishNotification(int type,
            Context context, String title, String description, Intent intent) {
        return new Notification.Builder(context)
                .setAutoCancel(true)
                .setSmallIcon(type == VCardService.TYPE_IMPORT ? android.R.drawable.stat_sys_download_done : R.drawable.stat_sys_upload_done_hold_dark)
                .setContentTitle(title)
                .setContentText(description)
                .setContentIntent(PendingIntent.getActivity(context, 0,
                        (intent != null ? intent : new Intent()), 0))
                .getNotification();
    }

    /**
     * Constructs a Notification telling the vCard import has failed.
     *
     * @param context
     * @param reason The reason why the import has failed. Shown in description field.
     */
    /* package */ static Notification constructImportFailureNotification(
            Context context, String reason) {
        return new Notification.Builder(context)
                .setAutoCancel(true)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(context.getString(R.string.vcard_import_failed))
                .setContentText(reason)
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0))
                .getNotification();
    }

    @Override
    public void onComplete() {
        mContext.finish();
    }
}
