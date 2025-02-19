/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.wappush;

import com.mediatek.pushparser.ParsedMessage;
import android.provider.Telephony.WapPush;
import com.mediatek.pushparser.si.SiMessage;
import com.android.mms.transaction.WapPushMessagingNotification;
import com.android.mms.ui.MessageUtils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.mediatek.xlog.Xlog;

public class SiManager extends WapPushManager {

    public final static String TAG = "Mms/WapPush";
    
    public SiManager(Context context) {
        super(context);
    }

    @Override
    public void handleIncoming(ParsedMessage message) {

        if(message == null){
            Xlog.e(TAG,"SiManager handleIncoming: null message");
            return;
        }
        
        
        SiMessage siMsg = null;
        try{
             siMsg = (SiMessage)message;
        }catch (Exception e) {
            Xlog.e(TAG,"SiManager SiMessage error");
        }
        
        //Prepare for query
        ContentResolver resolver = m_context.getContentResolver();
        Cursor cursor = null;
        

        int currentTime = (int)(System.currentTimeMillis()/1000);
        
        //If there is no Created time, use the local device time instead (convert to UTC).
        if(siMsg.create == 0){
            siMsg.create = (int) (System.currentTimeMillis()/1000);
        }
        /*
         * Handle SI Message
         * Reference:WAP-167-ServiceInd-20010731-a
         */
        
        //if si-id is null , si-id will be set the url
        if(siMsg.siid == null){
            siMsg.siid = siMsg.url;
        }
        
        //1,Dicard Expired Message
        
        if(siMsg.expiration > 0 && siMsg.expiration < currentTime){
            Xlog.i(TAG,"SiManager:Expired Message! "+ siMsg.url );
            return;
        }
        
        /*
         * Query to find Message with the same si-id
         * 
         */
        String []projection = {WapPush._ID,WapPush.SIID,WapPush.URL,WapPush.CREATE, WapPush.ADDR, WapPush.TEXT};
        String selection = WapPush.SIID + "=?";
        String []selectionArgs ={siMsg.siid};
        
        //url && siid will be the same; if siid is null, no need to query database 
        if(siMsg.siid != null){
            cursor = resolver.query(WapPush.CONTENT_URI_SI, projection, selection, selectionArgs, null);
        }

        if(cursor != null){
            try {
                if (cursor.moveToFirst()) {
                    do{
                        long messageId = cursor.getLong(0);
                        String siid = cursor.getString(1);
                        int createdTime = cursor.getInt(3);
                        String address = cursor.getString(4);
                        String text = cursor.getString(5);
                        
                        if(siid.equals(siMsg.siid)){
                          //2,Received SI older than other SI with identical si-id, discard the older one
                            if(siMsg.create > 0 && siMsg.create < createdTime ){
                                Xlog.i(TAG,"SiManager:Out of order Message! " + siMsg.url );
                                return;
                            }else if(siMsg.create >= createdTime){
                                if (siMsg.getSenderAddr().equals(address) && siMsg.text.equals(text)) {
                                    Xlog.d(TAG, "Discard duplicate message!");
                                    return;
                                    /*//3,Received SI newer (or of the same age)than other SI with identical si-id, delete the older one
                                    m_context.getContentResolver().delete(ContentUris.withAppendedId(WapPush.CONTENT_URI,messageId), null, null);
                                    Xlog.i(TAG,"SiManager:Delete older Message! " + messageId);
                                    //we may need to cancel the notification and called off of the UI thread so ok to block
                                    WapPushMessagingNotification.blockingUpdateNewMessageIndicator(m_context, false);*/
                                } else {                                    
                                    // Insert new message!
                                }
                            }
                        }           
                        
                    }while(cursor.moveToNext());

                }
            } finally {
                cursor.close();
            }
        } 
        
        //4,SI has action=delete, discard it
        if(siMsg.action == SiMessage.ACTION_DELETE){
            Xlog.i(TAG,"SiManager:Discard delete Message! " + siMsg.url );
            return;
        }
        
        //5,Discard SI has action = none
        if(siMsg.action == SiMessage.ACTION_NONE){
            Xlog.i(TAG,"SiManager:Discard None Message! " + siMsg.url );
            return;
        }
        
        //store in db
        ContentValues values = new ContentValues();
        values.put(WapPush.ADDR,siMsg.getSenderAddr());
        values.put(WapPush.SERVICE_ADDR, siMsg.getServiceCenterAddr());
        values.put(WapPush.SIM_ID,siMsg.getSimId());
        values.put(WapPush.URL, siMsg.url);
        values.put(WapPush.SIID, siMsg.siid);
        values.put(WapPush.ACTION,siMsg.action);
        values.put(WapPush.CREATE,siMsg.create);
        values.put(WapPush.EXPIRATION,siMsg.expiration);
        values.put(WapPush.TEXT, siMsg.text);
        Uri uri = m_context.getContentResolver().insert(WapPush.CONTENT_URI_SI, values);
          
        //notification
        if(uri != null){
            Xlog.i(TAG,"SiManager:Store msg! " + siMsg.url );
            //called off of the UI thread so ok to block
            WapPushMessagingNotification.blockingUpdateNewMessageIndicator(m_context, true);
        }
    }

}
