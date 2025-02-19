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

package com.mediatek.backuprestore;

import android.database.Cursor;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.mediatek.backuprestore.BackupRestoreUtils.ModuleType;
import com.mediatek.backuprestore.BackupRestoreUtils.LogTag;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class CalendarRestoreComposer extends Composer{
    private static final String CALENDARTAG = "Calendar:";
    private static final String action = "com.mtk.intent.action.RESTORE";
    private static final String VCS_CONTENT = "vcs_content";
    private static final Uri calanderEventURI = CalendarContract.Events.CONTENT_URI;
    private static final Uri calanderEventURI2 = Uri.parse("content://com.mediatek.calendarimporter/");
    private int mIdx;

    private ArrayList<String> mFileNameList;

    public CalendarRestoreComposer(Context context) {
        super(context);
    }

    @Override
    public int getModuleType() {
		return ModuleType.TYPE_CALENDAR;
	}


    @Override
    public int getCount() {
        int count = 0;
        if (mFileNameList != null) {
            count = mFileNameList.size();
        }

        Log.d(LogTag.RESTORE, CALENDARTAG + "getCount():" + count);
        return count;
    }

    public boolean init() {
        boolean result = false;
        mFileNameList = new ArrayList<String>();
        try {
            // List<String> fileList = BackupZip.GetFileList(mZipFileName, true, true, "calendar/calendar[0-9]+\\.vcs");
            // for (String file : fileList) {
            //     mFileNameList.add(file);
            // }
            mFileNameList = (ArrayList<String>)BackupZip.GetFileList(
                                                                     mZipFileName,
                                                                     true,
                                                                     true,
                                                                     "calendar/calendar[0-9]+\\.vcs");
            
            result = true;
        } catch (IOException e) {
        }

        Log.d(LogTag.RESTORE, CALENDARTAG + "init():" + result + ",count:" + mFileNameList.size());
        return result;
    }

    @Override
    public boolean isAfterLast() {
        boolean result = true;
        if (mFileNameList != null) {
            result = (mIdx >= mFileNameList.size()) ? true : false;
        }

        Log.d(LogTag.RESTORE, CALENDARTAG + "isAfterLast():" + result);
        return result;
    }

    @Override
    public boolean implementComposeOneEntity() {
        boolean result = false;
        String tmpFileName = mFileNameList.get(mIdx++);
        byte[] vcsByte = BackupZip.readFileContent(mZipFileName, tmpFileName);
        if (vcsByte != null) {
            Intent intent = new Intent(action);
            intent.putExtra(VCS_CONTENT, vcsByte);
            mContext.sendBroadcast(intent, null);
            result = true;
        } else {
            if (super.mReporter != null) {
                super.mReporter.onErr(new IOException());
            }
        }

        Log.d(LogTag.RESTORE, CALENDARTAG + "implementComposeOneEntity():" + mIdx + ",result:" + result);
        return result;
    }

	private boolean deleteAllCalendarEvents() {
        boolean result = false;
        int count = 0;
        Cursor cur = mContext.getContentResolver().query(calanderEventURI, null, null, null, null);
        if (cur != null && cur.moveToFirst()) {
            int[] cId = new int[cur.getCount()];
            int i = 0;
            while (!cur.isAfterLast()) {
                cId[i++] = cur.getInt(cur.getColumnIndex("_id"));
                cur.moveToNext();
            }

            for (int j = 0; j < cId.length; ++j) {
                Uri uri = ContentUris.withAppendedId(calanderEventURI, cId[j]);
                count +=  mContext.getContentResolver().delete(uri, null, null);
            }

            cur.close();
            result = true;
        }

        Log.d(LogTag.RESTORE, CALENDARTAG + "deleteAllCalendarEvents()result:" + result + ", " + count + " events deleted!");
		return result;
	}

    @Override
    public void onStart() {
        super.onStart();
        deleteAllCalendarEvents();

        Log.d(LogTag.RESTORE, CALENDARTAG + " onStart()");
    }

    @Override
    public void onEnd() {
        super.onEnd();
        if (mFileNameList != null) {
            mFileNameList.clear();
        }

        Log.d(LogTag.RESTORE, CALENDARTAG + " onEnd()");
    }
}
