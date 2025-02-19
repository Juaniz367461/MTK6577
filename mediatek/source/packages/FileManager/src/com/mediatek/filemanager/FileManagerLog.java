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

package com.mediatek.filemanager;

import android.util.Log;

import com.mediatek.xlog.Xlog;


public final class FileManagerLog {
    private static final boolean XLOG_ENABLED = true;
    private static final String MODULE_NAME = "FileManager";
    private static final FileManagerLog INSTANCE = new FileManagerLog();

    /**
     * private constructor here, It is a singleton class.
     */
    private FileManagerLog() {
    }

    /**
     * The FileManagerLog is a singleton class, this static method can be used to obtain the unique
     * instance of this class.
     * @return The global unique instance of FileManagerLog.
     */
    public static FileManagerLog getInstance() {
        return INSTANCE;
    }

    /**
     * The method prints the log, level error
     * @param tag the tag of the class
     * @param msg the message to print
     */
    public static void e(String tag, String msg) {
        if (XLOG_ENABLED) {
            Xlog.e(MODULE_NAME, tag + ", " + msg);
        } else {
            Log.e(MODULE_NAME, tag + ", " + msg);
        }
    }

    /**
     * The method prints the log, level error
     * @param tag the tag of the class
     * @param msg the message to print
     * @param t An exception to log
     */
    public static void e(String tag, String msg, Throwable t) {
        if (XLOG_ENABLED) {
            Xlog.e(MODULE_NAME, tag + ", " + msg, t);
        } else {
            Log.e(MODULE_NAME, tag + ", " + msg, t);
        }
    }

    /**
     * The method prints the log, level warning
     * @param tag the tag of the class
     * @param msg the message to print
     */
    public static void w(String tag, String msg) {
        if (XLOG_ENABLED) {
            Xlog.w(MODULE_NAME, tag + ", " + msg);
        } else {
            Log.w(MODULE_NAME, tag + ", " + msg);
        }
    }

    /**
     * The method prints the log, level warning
     * @param tag the tag of the class
     * @param msg the message to print
     * @param t An exception to log
     */
    public static void w(String tag, String msg, Throwable t) {
        if (XLOG_ENABLED) {
            Xlog.w(MODULE_NAME, tag + ", " + msg, t);
        } else {
            Log.w(MODULE_NAME, tag + ", " + msg, t);
        }
    }

    /**
     * The method prints the log, level debug
     * @param tag the tag of the class
     * @param msg the message to print
     */
    public static void i(String tag, String msg) {
        if (XLOG_ENABLED) {
            Xlog.i(MODULE_NAME, tag + ", " + msg);
        } else {
            Log.i(MODULE_NAME, tag + ", " + msg);
        }
    }

    /**
     * The method prints the log, level debug
     * @param tag the tag of the class
     * @param msg the message to print
     * @param t An exception to log
     */
    public static void i(String tag, String msg, Throwable t) {
        if (XLOG_ENABLED) {
            Xlog.i(MODULE_NAME, tag + ", " + msg, t);
        } else {
            Log.i(MODULE_NAME, tag + ", " + msg, t);
        }
    }

    /**
     * The method prints the log, level debug
     * @param tag the tag of the class
     * @param msg the message to print
     */
    public static void d(String tag, String msg) {
        if (XLOG_ENABLED) {
            Xlog.d(MODULE_NAME, tag + ", " + msg);
        } else {
            Log.d(MODULE_NAME, tag + ", " + msg);
        }
    }

    /**
     * The method prints the log, level debug
     * @param tag the tag of the class
     * @param msg the message to print
     * @param t An exception to log
     */
    public static void d(String tag, String msg, Throwable t) {
        if (XLOG_ENABLED) {
            Xlog.d(MODULE_NAME, tag + ", " + msg, t);
        } else {
            Log.d(MODULE_NAME, tag + ", " + msg, t);
        }
    }

    /**
     * The method prints the log, level debug
     * @param tag the tag of the class
     * @param msg the message to print
     */
    public static void v(String tag, String msg) {
        if (XLOG_ENABLED) {
            Xlog.v(MODULE_NAME, tag + ", " + msg);
        } else {
            Log.v(MODULE_NAME, tag + ", " + msg);
        }
    }

    /**
     * The method prints the log, level debug
     * @param tag the tag of the class
     * @param msg the message to print
     * @param t An exception to log
     */
    public static void v(String tag, String msg, Throwable t) {
        if (XLOG_ENABLED) {
            Xlog.v(MODULE_NAME, tag + ", " + msg, t);
        } else {
            Log.v(MODULE_NAME, tag + ", " + msg, t);
        }
    }
}