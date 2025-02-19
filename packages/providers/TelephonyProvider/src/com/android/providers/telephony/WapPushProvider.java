/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.providers.telephony;


import android.content.ContentProvider;
import android.content.ContentResolver;
import android.provider.BaseColumns;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.provider.Telephony.CanonicalAddressesColumns;
import android.provider.Telephony.WapPush;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Threads;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

public class WapPushProvider extends ContentProvider {
    
    private static final String TAG = "WapPush/Provider";
 
    static final String TABLE_WAPPUSH = "wappush";
    
    private static boolean mUseStrictPhoneNumberComparation;
    
    private static final Uri NOTIFICATION_URI = Uri.parse("content://wappush");
    
    private SQLiteOpenHelper mWapPushOpenHelper;
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
    	
        //if wap push is not support, should return 0;
        if(!FeatureOption.MTK_WAPPUSH_SUPPORT){
        	return 0;
        }
        
        SQLiteDatabase db = mWapPushOpenHelper.getWritableDatabase();
        int count = 0;
        switch (sUriMatcher.match(uri)) {
        case WAPPUSH_ALL:
            count = db.delete(TABLE_WAPPUSH, selection, selectionArgs);
            if(count!=0){
                updatePushAllThread(db, null, null);
            }
            break;

        case WAPPUSH_ALL_ID:
            int messageId;
            try{
                messageId = Integer.parseInt(uri.getPathSegments().get(0));
                
            }catch(Exception e){
                Xlog.e(TAG,"Delete: Bad Message ID");
                return 0;
            }
            
            count = deleteOnePushMsg(db, messageId);
            break;
            
        case WAPPUSH_SI:
            count = db.delete(TABLE_WAPPUSH, WapPush.TYPE + "=" + WapPush.TYPE_SI
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            if(count!=0){
                updatePushAllThread(db, WapPush.TYPE + "=" + WapPush.TYPE_SI, null);
            }
            break;
            
        case WAPPUSH_SL:
            count = db.delete(TABLE_WAPPUSH, WapPush.TYPE + "=" + WapPush.TYPE_SL
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            if(count!=0){
                updatePushAllThread(db, WapPush.TYPE + "=" + WapPush.TYPE_SL, null);
            }

            break;
            
        //delete a thread
        case WAPPUSH_THREAD_ID:
            int threadId;
            try{
                threadId = Integer.parseInt(uri.getPathSegments().get(1));
            }catch(Exception e){
                Xlog.e(TAG,"Delete: Bad conversation ID");
                return 0;
            }
            
            count = db.delete(TABLE_WAPPUSH, WapPush.THREAD_ID + "=" + threadId
                    + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            updatePushThread(db, threadId);
            
            break;
            
        default:
            Xlog.e(TAG,"Unknown URI " + uri);
            return 0;
        }
        if(count>0){
        	notifyChange(uri);
        }
        return count;

    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        switch (sUriMatcher.match(uri)) {
        case WAPPUSH_ALL:
        case WAPPUSH_SI:
        case WAPPUSH_SL:
        case WAPPUSH_THREAD_ID:
            return VND_ANDROID_DIR_PUSH;
        case WAPPUSH_ALL_ID:    
            return VND_ANDROID_PUSH; 
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
    	
        //if wap push is not support, should return null;
        if(!FeatureOption.MTK_WAPPUSH_SUPPORT){
        	return null;
        }
    	
        if(values == null){
            return null;
        }

        switch (sUriMatcher.match(uri)) {
        case WAPPUSH_ALL:
            break;   
        case WAPPUSH_SI:
            values.put(WapPush.TYPE, WapPush.TYPE_SI);
            break;
        case WAPPUSH_SL:
            values.put(WapPush.TYPE, WapPush.TYPE_SL);
            break;

        default:
            Xlog.e("TAG","Unknown URI " + uri);
            return null;
        }
        
        SQLiteDatabase db = mWapPushOpenHelper.getWritableDatabase();
        
        //get thread_id and puts in values
        String address = values.getAsString(WapPush.ADDR);
        long threadId = -1;
        if(address != null){
            threadId = getOrCreatePushThreadId(db, address);
            values.put(WapPush.THREAD_ID, threadId);
        }
        
        //get date
        if(values.getAsLong(WapPush.DATE) == null){
            values.put(WapPush.DATE, System.currentTimeMillis());
        }
        
        //insert into database
        long rowId = db.insert(TABLE_WAPPUSH, null, values);
        
        if (rowId > 0) {
            Uri insertUri = ContentUris.withAppendedId(WapPush.CONTENT_URI, rowId);
            
            //update thread
            if(threadId > 0){
                updatePushThread(db, threadId);
            }
            
            notifyChange(uri);
            return insertUri;
        }else{
            Xlog.e("TAG","Failed to insert! "+ values.toString());
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        mWapPushOpenHelper = MmsSmsDatabaseHelper.getInstance(getContext());
        
        mUseStrictPhoneNumberComparation = getContext().getResources().getBoolean(
                    com.android.internal.R.bool.config_use_strict_phone_number_comparation);
        
        //if wap push is not support, should return false;
        if(!FeatureOption.MTK_WAPPUSH_SUPPORT){
        	return false;
        }else{
        	return true;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        
        //if wap push is not support, should return null;
        if(!FeatureOption.MTK_WAPPUSH_SUPPORT){
        	return null;
        }
    	
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_WAPPUSH);
        
        switch (sUriMatcher.match(uri)) {
        case WAPPUSH_ALL:
            break;
        case WAPPUSH_ALL_ID:
            qb.appendWhere(WapPush._ID + "=" + uri.getPathSegments().get(0));
            break;
        case WAPPUSH_SI:
            qb.appendWhere(WapPush.TYPE + "=" + WapPush.TYPE_SI);
            break;
        case WAPPUSH_SL:
            qb.appendWhere(WapPush.TYPE + "=" + WapPush.TYPE_SL);
            break;
        case WAPPUSH_THREAD_ID:
            qb.appendWhere(WapPush.THREAD_ID + "=" + uri.getPathSegments().get(1));
            break;    

        default:
            Xlog.e("TAG","Unknown URI " + uri);
            return null;
        }

        String finalSortOrder = TextUtils.isEmpty(sortOrder) ? WapPush.DEFAULT_SORT_ORDER : sortOrder;

        SQLiteDatabase db = mWapPushOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, finalSortOrder);
        if(c != null){
        	c.setNotificationUri(getContext().getContentResolver(), NOTIFICATION_URI);
        }
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
 
        //if wap push is not support, should return 0;
        if(!FeatureOption.MTK_WAPPUSH_SUPPORT){
        	return 0;
        }
    	
        SQLiteDatabase db = mWapPushOpenHelper.getWritableDatabase();
        int count = 0;
        switch (sUriMatcher.match(uri)) {
        case WAPPUSH_ALL:
            count = db.update(TABLE_WAPPUSH, values, selection, selectionArgs);
            if(count > 0){
                updatePushAllThread(db, selection, selectionArgs);
            }
            break;
            
        case WAPPUSH_ALL_ID:
        	String newIdSelection = WapPush._ID + "=" + uri.getPathSegments().get(0) + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
            count = db.update(TABLE_WAPPUSH, values, newIdSelection, selectionArgs);
            if(count > 0){
                updatePushAllThread(db, newIdSelection, selectionArgs);
            }
            break;
            
        case WAPPUSH_SI:
        	String newSiSelection = WapPush.TYPE + "=" + WapPush.TYPE_SI + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
            count = db.update(TABLE_WAPPUSH, values , newSiSelection , selectionArgs);
            if(count > 0){
                updatePushAllThread(db, newSiSelection, selectionArgs);
            }
            break;       
            
        case WAPPUSH_SL:
        	String newSlSelection = WapPush.TYPE + "=" + WapPush.TYPE_SL + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
            count = db.update(TABLE_WAPPUSH, values ,newSlSelection, selectionArgs);
            if(count > 0){
                updatePushAllThread(db, newSlSelection, selectionArgs);
            }
            break;
        case WAPPUSH_THREAD_ID:
            int threadId;
            try{
                threadId = Integer.parseInt(uri.getPathSegments().get(1));
            }catch(Exception e){
                Xlog.e(TAG,"Update: Bad conversation ID");
                return 0;
            }
        	String newThreadSelection = WapPush.THREAD_ID + "=" + threadId + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
        	count = db.update(TABLE_WAPPUSH, values ,newThreadSelection, selectionArgs);
            if(count > 0){
                updatePushThread(db,threadId);
            }
        	break;
        default:
            Xlog.e("TAG","Unknown URI " + uri);
            return count;
        }
        if(count>0){
        	notifyChange(uri);
        }
        return count;
    }
    
    
    /*
     * Query the threads table to find thread_id
     * if not found , create a new push thread;
     */
    
    private long getOrCreatePushThreadId(SQLiteDatabase db, String address){
        
    	Xlog.i(TAG,"getOrCreatePushThreadId address:" + address);
    	
        //long threadId = Threads.getOrCreateThreadId(getContext(), address);
    	long threadId = -1;
        
    	Uri threadUri = Uri.parse("content://mms-sms/threadID");
        Uri.Builder uriBuilder = threadUri.buildUpon();
        uriBuilder.appendQueryParameter("recipient", address);
        uriBuilder.appendQueryParameter("wappush", address);
        Uri uri = uriBuilder.build();
        
        Xlog.i(TAG, "getOrCreatePushThreadId uri: " + uri);
        
        Cursor cursor = getContext().getContentResolver().query(uri, new String[] {"_id"}, null, null, null);

        if (cursor != null) {
            Xlog.v(TAG, "getOrCreateThreadId cursor cnt: " + cursor.getCount());
            try {
                if (cursor.moveToFirst()) {
                    threadId = cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }
    	
        if(threadId < 0){
        	Xlog.w(TAG,"getOrCreatePushThreadId: Failed to create an ThreadId");
        	return -1;
        }
        
        //set the thread type
        ContentValues values = new ContentValues();
        values.put(Threads.TYPE, Threads.WAPPUSH_THREAD);
        db.update("threads",values,Threads._ID + "=" + threadId,null);
        
        return threadId;     
    }
    
    /*
     * update Push thread with thread id
     */
    private void updatePushThread(SQLiteDatabase db, long threadId){
        
        if(threadId < 0){
            updatePushAllThread(db,null,null);
        }
        
		Xlog.i(TAG, "updatePushThread thread: " + threadId);

		// if it is not a wappush thread, it doesn't need to be updated here;
		Cursor pushCursor = db.rawQuery("select * from threads where type <>"
				+ Threads.WAPPUSH_THREAD + " AND _id=" + threadId, null);
		if (pushCursor != null) {
			try {
				if (pushCursor.getCount() != 0) {
					return;
				}
			} finally {
				pushCursor.close();
			}
		}
        
        /*
         * Delete the row for this thread if no push message exists
         */
        int rows = db.delete("threads",
                "_id = ? AND _id NOT IN" 
                + " (SELECT thread_id FROM " 
                + TABLE_WAPPUSH	+ ")", 
                new String[] { String.valueOf(threadId) }); 
        if (rows > 0) { 
            // If this deleted a row, we have no more work to do. 244 
            return; 
            }
        
     /*
      * Update the message count in the threads table as the sum 
      * of all messages in push tables. 
      */
        db.execSQL(
                "UPDATE threads SET message_count = " 
                + "(SELECT COUNT(" + WapPush._ID + ") FROM " + TABLE_WAPPUSH + " WHERE " + WapPush.THREAD_ID + "=" + threadId + ")" 
                + "  WHERE threads._id = " + threadId + ";");                 
                
                
    /*
     * Update the date and the snippet (and its character set)
     * the threads table to be that of the most recent message in the thread.    
     */    
        
        /*
         * get the latest message
         */
        Cursor c = db.query(TABLE_WAPPUSH, 
                new String[]{WapPush.DATE,WapPush.TEXT,WapPush.URL}, 
                WapPush.THREAD_ID + " = " + threadId, null, null, null, "date DESC LIMIT 1");
        long date = 0;
        String snippet="";
        if(c!= null){
            try{
                if(c.moveToFirst()){
                    date = c.getLong(0);
                    snippet = c.getString(1);
                    // if text is null, snippet is set to url
                    if(snippet == null || snippet.equals("")){
                        snippet = c.getString(2);
                    }
                }
            }finally{
                c.close();
            }
        }
        //update date snippet
        ContentValues values = new ContentValues();
        values.put(Threads.DATE, date);
        values.put(Threads.SNIPPET, snippet);
        //db.update("threads", values, Threads._ID + " = " + threadId, null);
        
        
        /*
        * update read status
        */
        //query the unread
        c = db.query(TABLE_WAPPUSH, 
                new String[]{WapPush.READ}
                , WapPush.THREAD_ID + " = " + threadId + " AND " + WapPush.READ + " = " + WapPush.STATUS_UNREAD
                , null, null, null, null);
        //values.clear();
        if(c!= null){
            try{
                if(c.moveToFirst()){  //has a unread wappush message
                    values.put(Threads.READ, WapPush.STATUS_UNREAD);
                    //db.update("threads", values, Threads._ID + " = " + threadId, null);
                }else{
                    values.put(Threads.READ, WapPush.STATUS_READ);
                    //db.update("threads", values, Threads._ID + " = " + threadId, null);
                }
            }finally{
                c.close();
            }
        }

		// Update the error column of the thread to indicate if there
		// are any messages in it that have failed to send.
		// First check to see if there are any messages with errors in this
		// thread
		String query = "SELECT thread_id FROM wappush WHERE error=1"
				+ " AND thread_id = " + threadId + " LIMIT 1";
		int setError = 0;
		c = db.rawQuery(query, null);
		if (c != null) {
			try {
				setError = c.getCount(); // Because of the LIMIT 1, count will be 1 or 0.
			} finally {
				c.close();
			}
		}
		
        // Update the current thread's error field
		values.put(Threads.ERROR, setError);
		
		//Update all change
        //db.execSQL("UPDATE threads SET error=" + setError + " WHERE _id = " + threadId);
		db.update("threads", values, Threads._ID + " = " + threadId, null);
    }
       
    /*
     * update all push thread
     */
    private void updatePushAllThread(SQLiteDatabase db, String where, String[] whereArgs){
        
    	Xlog.i(TAG,"updatePushAllThread");
    	
        if(where == null){
            where = "";  
        }else{
            where = "WHERE (" + where +")";
        }
        
        //update the thread which need to be updated
        String query = "SELECT _id FROM threads WHERE " + Threads.TYPE + " = " + Threads.WAPPUSH_THREAD + "  AND _id IN " 
                        + "(SELECT DISTINCT thread_id FROM " + TABLE_WAPPUSH + " " + where + ")";
        Cursor c = db.rawQuery(query, whereArgs);
        
        if( c != null){
            try{
                while(c.moveToNext()){
                    updatePushThread(db, c.getInt(0));
                }
            }finally{
                c.close();
            }
        }
        
        //delete the thread which has not push messages
        db.delete("threads", "_id NOT IN (SELECT DISTINCT thread_id FROM " + TABLE_WAPPUSH + ")" 
        					+ " AND " + Threads.TYPE + " = " + Threads.WAPPUSH_THREAD, null);
    }


    /*
     * 
     */
    private int deleteOnePushMsg(SQLiteDatabase db, int messageId){
        
    	Xlog.i(TAG,"deleteOnePushMsg messageId:" + messageId);
    	
        int threadId = -1;
        //Find the thread ID 
        Cursor c = db.query(TABLE_WAPPUSH,new String[]{WapPush.THREAD_ID},"_id=" + messageId, null,null,null,null);
        
        if(c !=null){
            if(c.moveToFirst()){
                threadId = c.getInt(0);
            }
            c.close();
        }
        
        //Delete the message and update the thread
        int rows = db.delete(TABLE_WAPPUSH, "_id=" + messageId, null);
        if(threadId > 0){
            updatePushThread(db,threadId);
        }   
        return rows;
    }
    
    private void notifyChange(Uri uri) {
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(uri, null);
        cr.notifyChange(MmsSms.CONTENT_URI, null);
        cr.notifyChange(Uri.parse("content://mms-sms/conversations/"), null);
        Xlog.i(TAG,"WapPushProvider notifyChange: " + uri);
        MmsSmsProvider.notifyUnreadMessageNumberChanged(getContext());
    }
     
    private static final String VND_ANDROID_PUSH = "vnd.android.cursor.item/wappush";
    private static final String VND_ANDROID_DIR_PUSH = "vnd.android.cursor.dir/wappush";
    
    
    private static final int WAPPUSH_ALL = 0;
    private static final int WAPPUSH_ALL_ID = 1;
    private static final int WAPPUSH_SI = 2;
    private static final int WAPPUSH_SL = 3;
    private static final int WAPPUSH_THREAD_ID = 4;
    
    private static final UriMatcher sUriMatcher = 
        new UriMatcher(UriMatcher.NO_MATCH);
    static{
        sUriMatcher.addURI("wappush", null , WAPPUSH_ALL);
        sUriMatcher.addURI("wappush", "#" , WAPPUSH_ALL_ID);
        sUriMatcher.addURI("wappush", "thread_id/#" , WAPPUSH_THREAD_ID);
        sUriMatcher.addURI("wappush", "si" , WAPPUSH_SI);
        sUriMatcher.addURI("wappush", "sl" , WAPPUSH_SL);
 
    } 
    
}
