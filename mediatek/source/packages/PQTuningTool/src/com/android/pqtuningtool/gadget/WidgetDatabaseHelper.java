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

package  com.android.pqtuningtool.gadget;

import  com.android.pqtuningtool.common.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class WidgetDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "PhotoDatabaseHelper";
    private static final String DATABASE_NAME = "launcher.db";

    private static final int DATABASE_VERSION = 4;

    private static final String TABLE_WIDGETS = "widgets";

    private static final String FIELD_APPWIDGET_ID = "appWidgetId";
    private static final String FIELD_IMAGE_URI = "imageUri";
    private static final String FIELD_PHOTO_BLOB = "photoBlob";
    private static final String FIELD_WIDGET_TYPE = "widgetType";
    private static final String FIELD_ALBUM_PATH = "albumPath";

    public static final int TYPE_SINGLE_PHOTO = 0;
    public static final int TYPE_SHUFFLE = 1;
    public static final int TYPE_ALBUM = 2;

    private static final String[] PROJECTION = {
            FIELD_WIDGET_TYPE, FIELD_IMAGE_URI, FIELD_PHOTO_BLOB, FIELD_ALBUM_PATH};
    private static final int INDEX_WIDGET_TYPE = 0;
    private static final int INDEX_IMAGE_URI = 1;
    private static final int INDEX_PHOTO_BLOB = 2;
    private static final int INDEX_ALBUM_PATH = 3;
    private static final String WHERE_CLAUSE = FIELD_APPWIDGET_ID + " = ?";

    public static class Entry {
        public int widgetId;
        public int type;
        public String imageUri;
        public byte imageData[];
        public String albumPath;

        private Entry() {}

        private Entry(int id, Cursor cursor) {
            widgetId = id;
            type = cursor.getInt(INDEX_WIDGET_TYPE);
            if (type == TYPE_SINGLE_PHOTO) {
                imageUri = cursor.getString(INDEX_IMAGE_URI);
                imageData = cursor.getBlob(INDEX_PHOTO_BLOB);
            } else if (type == TYPE_ALBUM) {
                albumPath = cursor.getString(INDEX_ALBUM_PATH);
            }
        }
    }

    public WidgetDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_WIDGETS + " ("
                + FIELD_APPWIDGET_ID + " INTEGER PRIMARY KEY, "
                + FIELD_WIDGET_TYPE + " INTEGER DEFAULT 0, "
                + FIELD_IMAGE_URI + " TEXT, "
                + FIELD_ALBUM_PATH + " TEXT, "
                + FIELD_PHOTO_BLOB + " BLOB)");
    }

    private void saveData(SQLiteDatabase db, int oldVersion, ArrayList<Entry> data) {
        if (oldVersion <= 2) {
            Cursor cursor = db.query("photos",
                    new String[] {FIELD_APPWIDGET_ID, FIELD_PHOTO_BLOB},
                    null, null, null, null, null);
            if (cursor == null) return;
            try {
                while (cursor.moveToNext()) {
                    Entry entry = new Entry();
                    entry.type = TYPE_SINGLE_PHOTO;
                    entry.widgetId = cursor.getInt(0);
                    entry.imageData = cursor.getBlob(1);
                    data.add(entry);
                }
            } finally {
                cursor.close();
            }
        } else if (oldVersion == 3) {
            Cursor cursor = db.query("photos",
                    new String[] {FIELD_APPWIDGET_ID, FIELD_PHOTO_BLOB, FIELD_IMAGE_URI},
                    null, null, null, null, null);
            if (cursor == null) return;
            try {
                while (cursor.moveToNext()) {
                    Entry entry = new Entry();
                    entry.type = TYPE_SINGLE_PHOTO;
                    entry.widgetId = cursor.getInt(0);
                    entry.imageData = cursor.getBlob(1);
                    entry.imageUri = cursor.getString(2);
                    data.add(entry);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void restoreData(SQLiteDatabase db, ArrayList<Entry> data) {
        db.beginTransaction();
        try {
            for (Entry entry : data) {
                ContentValues values = new ContentValues();
                values.put(FIELD_APPWIDGET_ID, entry.widgetId);
                values.put(FIELD_WIDGET_TYPE, entry.type);
                values.put(FIELD_IMAGE_URI, entry.imageUri);
                values.put(FIELD_PHOTO_BLOB, entry.imageData);
                values.put(FIELD_ALBUM_PATH, entry.albumPath);
                db.insert(TABLE_WIDGETS, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int version = oldVersion;

        if (version != DATABASE_VERSION) {
            ArrayList<Entry> data = new ArrayList<Entry>();
            saveData(db, oldVersion, data);

            Log.w(TAG, "destroying all old data.");
            // Table "photos" is renamed to "widget" in version 4
            db.execSQL("DROP TABLE IF EXISTS photos");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIDGETS);
            onCreate(db);

            restoreData(db, data);
        }
    }

    /**
     * Store the given bitmap in this database for the given appWidgetId.
     */
    public boolean setPhoto(int appWidgetId, Uri imageUri, Bitmap bitmap) {
        try {
            // Try go guesstimate how much space the icon will take when
            // serialized to avoid unnecessary allocations/copies during
            // the write.
            int size = bitmap.getWidth() * bitmap.getHeight() * 4;
            ByteArrayOutputStream out = new ByteArrayOutputStream(size);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();

            ContentValues values = new ContentValues();
            values.put(FIELD_APPWIDGET_ID, appWidgetId);
            values.put(FIELD_WIDGET_TYPE, TYPE_SINGLE_PHOTO);
            values.put(FIELD_IMAGE_URI, imageUri.toString());
            values.put(FIELD_PHOTO_BLOB, out.toByteArray());

            SQLiteDatabase db = getWritableDatabase();
            db.replaceOrThrow(TABLE_WIDGETS, null, values);
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "set widget photo fail", e);
            return false;
        }
    }

    public boolean setWidget(int id, int type, String albumPath) {
        try {
            ContentValues values = new ContentValues();
            values.put(FIELD_APPWIDGET_ID, id);
            values.put(FIELD_WIDGET_TYPE, type);
            values.put(FIELD_ALBUM_PATH, Utils.ensureNotNull(albumPath));
            getWritableDatabase().replaceOrThrow(TABLE_WIDGETS, null, values);
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "set widget fail", e);
            return false;
        }
    }

    public Entry getEntry(int appWidgetId) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(TABLE_WIDGETS, PROJECTION,
                    WHERE_CLAUSE, new String[] {String.valueOf(appWidgetId)},
                    null, null, null);
            if (cursor == null || !cursor.moveToNext()) {
                Log.e(TAG, "query fail: empty cursor: " + cursor);
                return null;
            }
            return new Entry(appWidgetId, cursor);
        } catch (Throwable e) {
            Log.e(TAG, "Could not load photo from database", e);
            return null;
        } finally {
            Utils.closeSilently(cursor);
        }
    }

    /**
     * Remove any bitmap associated with the given appWidgetId.
     */
    public void deleteEntry(int appWidgetId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(TABLE_WIDGETS, WHERE_CLAUSE,
                    new String[] {String.valueOf(appWidgetId)});
        } catch (SQLiteException e) {
            Log.e(TAG, "Could not delete photo from database", e);
        }
    }
}