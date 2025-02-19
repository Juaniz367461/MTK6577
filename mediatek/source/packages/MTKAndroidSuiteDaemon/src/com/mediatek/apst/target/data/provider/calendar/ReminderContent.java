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

package com.mediatek.apst.target.data.provider.calendar;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import android.database.Cursor;
import android.net.Uri;

import com.mediatek.apst.target.data.proxy.IRawBufferWritable;
import com.mediatek.apst.target.util.Debugger;
import com.mediatek.apst.util.entity.calendar.Reminder;

public abstract class ReminderContent {

	public static final Uri CONTENT_URI = Uri.parse("content://com.android.calendar/reminders");

	/** Id. INTEGER(long). */
	public static final String COLUMN_ID = "_id";
	/**
	 * The event the reminder belongs to
	 * <P>
	 * Type: INTEGER (foreign key to the Events table)
	 * </P>
	 */
	public static final String COLUMN_EVENT_ID = "event_id";

	/**
	 * The minutes prior to the event that the alarm should ring. -1 specifies
	 * that we should use the default value for the system.
	 * <P>
	 * Type: INTEGER
	 * </P>
	 */
	public static final String COLUMN_MINUTES = "minutes";

	public static final int MINUTES_DEFAULT = -1;

	/**
	 * The alarm method, as set on the server. DEFAULT, ALERT, EMAIL, and SMS
	 * are possible values; the device will only process DEFAULT and ALERT
	 * reminders (the other types are simply stored so we can send the same
	 * reminder info back to the server when we make changes).
	 */
	public static final String COLUMN_METHOD = "method";

	public static final int METHOD_DEFAULT = 0;
	public static final int METHOD_ALERT = 1;
	public static final int METHOD_EMAIL = 2;
	public static final int METHOD_SMS = 3;

	public static Reminder cursorToReminder(Cursor c) {
		if (null == c || c.getPosition() == -1 || c.getPosition() == c.getCount()) {
			return null;
		}

		Reminder reminder = new Reminder();
		try {
			int colId;
			// id
			colId = c.getColumnIndex(COLUMN_ID);
			if (-1 != colId) {
				reminder.setId(c.getLong(colId));
			}
			// event id
			colId = c.getColumnIndex(COLUMN_EVENT_ID);
			if (-1 != colId) {
				reminder.setEventId(c.getLong(colId));
			}
			// minutes
			colId = c.getColumnIndex(COLUMN_MINUTES);
			if (-1 != colId) {
				reminder.setMinutes(c.getLong(colId));
			}
			// method
			colId = c.getColumnIndex(COLUMN_METHOD);
			if (-1 != colId) {
				reminder.setMethod(c.getLong(colId));
			}
		} catch (IllegalArgumentException e) {
			Debugger.logE(new Object[] { c }, null, e);
		}
		return reminder;
	}
	
	public static int cursorToRaw(Cursor c, ByteBuffer buffer) {
		if (null == c) {
			Debugger.logW(new Object[] { c, buffer }, "Cursor is null.");
			return IRawBufferWritable.RESULT_FAIL;
		} else if (c.getPosition() == -1 || c.getPosition() == c.getCount()) {
			Debugger.logW(new Object[] { c, buffer }, "Cursor has moved to the end.");
			return IRawBufferWritable.RESULT_FAIL;
		} else if (null == buffer) {
			Debugger.logW(new Object[] { c, buffer }, "Buffer is null.");
			return IRawBufferWritable.RESULT_FAIL;
		}
		// Mark the current start position of byte buffer in order to reset
		// later when there is not enough space left in buffer
		buffer.mark();
		try {
			int colId;
			// id
			colId = c.getColumnIndex(COLUMN_ID);
			if (-1 != colId) {
				buffer.putLong(c.getLong(colId));
				Debugger.logI(new Object[] { }, "get Reminder: _id = " + c.getLong(colId));
			} else {
				buffer.putLong(0);
			}
			// event id
			colId = c.getColumnIndex(COLUMN_EVENT_ID);
			if (-1 != colId) {
				buffer.putLong(c.getLong(colId));
				Debugger.logI(new Object[] { }, "get Reminder: event_id = " + c.getLong(colId));
			} else {
				buffer.putLong(0);
			}
			// minutes
			colId = c.getColumnIndex(COLUMN_MINUTES);
			if (-1 != colId) {
				buffer.putLong(c.getLong(colId));
				Debugger.logI(new Object[] { }, "get Reminder: MINUTES = " + c.getLong(colId));
			} else {
				buffer.putLong(0);
			}
			// method
			colId = c.getColumnIndex(COLUMN_METHOD);
			if (-1 != colId) {
				buffer.putLong(c.getLong(colId));
				Debugger.logI(new Object[] { }, "get Reminder: method = " + c.getLong(colId));
			} else {
				buffer.putLong(0);
			}
		} catch (IllegalArgumentException e) {
			Debugger.logE(new Object[] { c, buffer }, null, e);
			buffer.reset();
			return IRawBufferWritable.RESULT_FAIL;
		} catch (BufferOverflowException e) {
			/*
			 * Debugger.logW(new Object[]{c, buffer},
			 * "Not enough space left in buffer. ", e);
			 */
			buffer.reset();
			return IRawBufferWritable.RESULT_NOT_ENOUGH_BUFFER;
		}
		return IRawBufferWritable.RESULT_SUCCESS;
	}
}
