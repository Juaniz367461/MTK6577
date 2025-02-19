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

package com.mediatek.apst.target.data.provider.contacts;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import android.database.Cursor;
import android.net.Uri;

import com.mediatek.apst.target.data.proxy.IRawBufferWritable;
import com.mediatek.apst.target.util.Debugger;
import com.mediatek.apst.target.util.Global;
import com.mediatek.apst.util.entity.DataStoreLocations;
import com.mediatek.apst.util.entity.DatabaseRecordEntity;
import com.mediatek.apst.util.entity.RawTransUtil;
import com.mediatek.apst.util.entity.contacts.RawContact;

public abstract class SimContactsContent {
    //==============================================================
    // Constants                                                    
    //==============================================================
    /**
     * SIM Content URI. 
     */
    public static final Uri CONTENT_URI = Uri.parse("content://icc/adn");
    
    public static final Uri CONTENT_URI_SIM1 = Uri.parse("content://icc/adn1");
    
    public static final Uri CONTENT_URI_SIM2 = Uri.parse("content://icc/adn2");
    
    /*
     * USIM URI  added by mtk54043 2012.4.19
     */
    public static final Uri mIccUsimUri = Uri.parse("content://icc/pbr");
    
    public static final Uri mIccUsim1Uri = Uri.parse("content://icc/pbr1/");
    
    public static final Uri mIccUsim2Uri = Uri.parse("content://icc/pbr2/");
    
    public static final String COLUMN_ID = "_id";
    
    /**
     * Column name of contact name. Used as:
     * <p>1. Name of contact queried out.</p>
     */
    public static final String COLUMN_NAME = "name";
    
    /**
     * Column name of contact name. Used as:
     * <p>1. Name of contact to insert.</p>
     * <p>2. Name of contact to delete.</p>
     * <p>3. Old name of contact to update.</p>
     */
    public static final String COLUMN_TAG = "tag";
    
    /**
     * Column name of contact phone number. Used as:
     * <p>1. Phone number of contact to insert.</p>
     * <p>2. Phone number of contact to delete.</p>
     * <p>3. Old phone number of contact to update.</p>
     */
    public static final String COLUMN_NUMBER = "number";
    
    /**
     * Column name of contact name. Used as:
     * <p>1. New name of contact to update.</p>
     */
    public static final String COLUMN_NEW_TAG = "newTag";
    
    /**
     * Column name of contact phone number. Used as:
     * <p>1. New phone number of contact to update.</p>
     */
    public static final String COLUMN_NEW_NUMBER = "newNumber";
    
    public static final String COLUMN_EMAILS = "emails";
    
    public static final String COLUMN_NEW_EMAILS = "newEmails";
    
    // These error codes are defined in IccProvider
    public static final int ERR_ICC_NO_ERROR = 1;
    public static final int ERR_ICC_UNKNOWN = 0;
    public static final int ERR_ICC_NUMBER_TOO_LONG = -1;
    public static final int ERR_ICC_TEXT_TOO_LONG = -2;
    public static final int ERR_ICC_STORAGE_FULL = -3;
    public static final int ERR_ICC_NOT_READY = -4;
    public static final int ERR_ICC_PASSWORD_ERROR = -5;
    // Generic failure. It seems that delete and update will return this value 
    // when no row fulfills the 'where' clause was found.
    public static final int ERR_ICC_GENERIC_FAILURE = -10;
    
    //==============================================================
    // Fields                                                       
    //==============================================================
    
    //==============================================================
    // Constructors                                                 
    //==============================================================
    
    //==============================================================
    // Getters                                                      
    //==============================================================
    
    //==============================================================
    // Setters                                                      
    //==============================================================
    
    //==============================================================
    // Methods                                                      
    //==============================================================
    public static RawContact cursorToRawContact(Cursor c, int storeLocation){
        if (null == c || c.getPosition() == -1 || 
                c.getPosition() == c.getCount()){
            return null;
        }
        
        // Create a new raw contact object
        RawContact contact = new RawContact();
        contact.setStoreLocation(storeLocation);
        // sim name
        contact.setSimName(Global.getSimName(storeLocation));
        
        try {
            // Read basic info fields of the raw contact -----------------------
            int colId;
            // id
            colId = c.getColumnIndex(COLUMN_ID);
            if (-1 != colId){
                contact.setId(c.getLong(colId));
            }
            // name
            colId = c.getColumnIndex(COLUMN_NAME);
            if (-1 != colId){
                contact.setDisplayName(c.getString(colId));
            }
            // number
            colId = c.getColumnIndex(COLUMN_NUMBER);
            if (-1 != colId){
                contact.setPrimaryNumber(c.getString(colId));
            }
            // Modify Time. Added by Shaoying Han
            colId = c.getColumnIndex(RawContactsContent.COLUMN_MODIFY_TIME);
            if (-1 != colId){
                contact.setModifyTime(c.getLong(colId));
            }
            // -----------------------------------------------------------------
        } catch (IllegalArgumentException e) {
            Debugger.logE(new Object[]{c, storeLocation}, null, e);
        }
        
        return contact;
    }
    
    public static RawContact cursorToRawContact(Cursor c){
        return cursorToRawContact(c, DataStoreLocations.SIM);
    };
    
    public static int cursorToRaw(Cursor c, ByteBuffer buffer, 
            int storeLocation){
        if (null == c){
            Debugger.logW(new Object[]{c, buffer, storeLocation}, 
                    "Cursor is null.");
            return IRawBufferWritable.RESULT_FAIL;
        } else if (c.getPosition() == -1 || c.getPosition() == c.getCount()){
            Debugger.logW(new Object[]{c, buffer, storeLocation}, 
                    "Cursor has moved to the end.");
            return IRawBufferWritable.RESULT_FAIL;
        } else if (null == buffer) {
            Debugger.logW(new Object[]{c, buffer, storeLocation}, 
                    "Buffer is null.");
            return IRawBufferWritable.RESULT_FAIL;
        }
        // Mark the current start position of byte buffer in order to reset 
        // later when there is not enough space left in buffer
        buffer.mark();
        try {
            int colId;
            // id
            colId = c.getColumnIndex(COLUMN_ID);
            if (-1 != colId){
                buffer.putLong(c.getLong(colId));
            } else {
                buffer.putLong(DatabaseRecordEntity.ID_NULL);
            }
            // store location
            buffer.putInt(storeLocation);
            
            // display name
            colId = c.getColumnIndex(COLUMN_NAME);
            if (-1 != colId){
                RawTransUtil.putString(buffer, c.getString(colId));
            } else {
                RawTransUtil.putString(buffer, null);
            }
            // primary number
            colId = c.getColumnIndex(COLUMN_NUMBER);
            if (-1 != colId){
                RawTransUtil.putString(buffer, c.getString(colId));
            } else {
                RawTransUtil.putString(buffer, null);
            }
            // group memberships
            buffer.putInt(0); // Only put a int to tell that list size is 0
            // sim name. Added by Shaoying Han
            RawTransUtil.putString(buffer, Global.getSimName(storeLocation));
            // Modify time. Added by Shaoying Han
            colId = c.getColumnIndex(RawContactsContent.COLUMN_MODIFY_TIME);
            if (-1 != colId){
            	buffer.putLong(c.getLong(colId)); 
            } else {
            	buffer.putLong(DatabaseRecordEntity.ID_NULL);
            }
            
            // starred
            RawTransUtil.putBoolean(buffer, false); // false by default
            // send to voicemail
            RawTransUtil.putBoolean(buffer, false); // false by default
            // times contacted
            // last time contacted
            // custom ringtone
            // version
            buffer.putInt(-1);
            // dirty
            // names
            buffer.putInt(0); // Only put a int to tell that list size is 0
            // phones
            buffer.putInt(0); // Only put a int to tell that list size is 0
            // photos
            buffer.putInt(0); // Only put a int to tell that list size is 0
            // emails
            buffer.putInt(0); // Only put a int to tell that list size is 0
            // ims
            buffer.putInt(0); // Only put a int to tell that list size is 0
            // postals
            buffer.putInt(0); // Only put a int to tell that list size is 0
            // organizations
            buffer.putInt(0); // Only put a int to tell that list size is 0
            // notes
            buffer.putInt(0); // Only put a int to tell that list size is 0
            // nicknames
            buffer.putInt(0); // Only put a int to tell that list size is 0
            // websites
            buffer.putInt(0); // Only put a int to tell that list size is 0
        } catch (IllegalArgumentException e) {
            Debugger.logE(new Object[]{c, buffer, storeLocation}, null, e);
            buffer.reset();
            return IRawBufferWritable.RESULT_FAIL;
        } catch (BufferOverflowException e) {
            /*DebugHelper.logW("[SimContactsContent] " +
                    "cursorToRaw(" + c + "): Not enough space left in " +
                    "buffer. ", e);*/
            buffer.reset();
            return IRawBufferWritable.RESULT_NOT_ENOUGH_BUFFER;
        }
        return IRawBufferWritable.RESULT_SUCCESS;
    }
    
    public static int cursorToRaw(Cursor c, ByteBuffer buffer){
        return cursorToRaw(c, buffer, DataStoreLocations.SIM);
    }
    
    //==============================================================
    // Inner & Nested classes                                               
    //==============================================================
}
