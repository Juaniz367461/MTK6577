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

package com.mediatek.bluetooth.avrcp;

import android.util.Log;
import android.os.*;
import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;

/**
* @brief Avrcp-Music Adpater for the native Android Music player
*/
public class BTAvrcpProfile {
	public final static int MAX_ATTRIBUTE_NUM = 0x04; // max attribute number
	public final static int MAX_ATTRVALUE_NUM = 0x07; // max attribute number
	
    public final static byte OK = 0;
    public final static short UTF8_CHARSET = 0x6a;
    public final static byte STATUS_OK = 0x04; /* AVRCP status definition */
    public final static byte FAIL = 1;
    	
	/* AVRCP Error/Status code AVRCP spec 6.15.3 status and error codes */
	public final static byte AVRCP_ERRCODE_INVALID_CMD            = 0x00; // All cmds
	public final static byte AVRCP_ERRCODE_INVALID_PARAM          = 0x01; // All cmds
	public final static byte AVRCP_ERRCODE_NOT_FOUND              = 0x02; // All cmds
	public final static byte AVRCP_ERRCODE_INTERNAL_ERROR         = 0x03; // All cmds
	public final static byte AVRCP_ERRCODE_OPERATION_COMPLETE     = 0x04; // All cmds except CType is reject
	public final static byte AVRCP_ERRCODE_UID_CHANGED            = 0x05; // All cmds
	public final static byte AVRCP_ERRCODE_RESERVED               = 0x06; // All cmds
	public final static byte AVRCP_ERRCODE_INVALID_DIRECTION      = 0x07; // change path cmd
	public final static byte AVRCP_ERRCODE_NOT_A_DIRECTORY        = 0x08; // change path cmd
	public final static byte AVRCP_ERRCODE_NOT_EXIST              = 0x09; // change path, playitem, addtonowplaying, getitemattributes
	public final static byte AVRCP_ERRCODE_INVALID_SCOPE          = 0x0a; // get folder itmes, play items, addtonowplaying, getitemattributes
	public final static byte AVRCP_ERRCODE_RANGE_OUT_OF_BOUNDS    = 0x0b; // getfolderitems
	public final static byte AVRCP_ERRCODE_UID_IS_DIRECTORY       = 0x0c; // play items, addtonowplaying
	public final static byte AVRCP_ERRCODE_MEDIA_IN_USE           = 0x0d; // play items, addtonowplaying
	public final static byte AVRCP_ERRCODE_NOW_PLAYING_FULL       = 0x0e; // addtonowplaying
	public final static byte AVRCP_ERRCODE_SEARCH_NOT_SUPPORTED   = 0x0f; // search
	public final static byte AVRCP_ERRCODE_SEARCH_IN_PROGRESS     = 0x10; // search
	public final static byte AVRCP_ERRCODE_INVALID_PLAYER_ID      = 0x11; // setaddressedplayer, setbrowsedplayer
	public final static byte AVRCP_ERRCODE_PLAYER_NOT_BROWSABLE   = 0x12; // setbrowsedplayer
	public final static byte AVRCP_ERRCODE_PLAYER_NOT_ADDRESSED   = 0x13; // search, setbrowsedplayer
	public final static byte AVRCP_ERRCODE_INVALID_SEARCH_RESULT  = 0x14; // get folder itmes
	public final static byte AVRCP_ERRCODE_NO_AVAILABLE_PLAYER    = 0x15; // All cmds
	public final static byte AVRCP_ERRCODE_PLAYER_CHANGED         = 0x16; // register notification
	
	public final static byte AVRCP_OPERATION_ID_PLAY              = 0x44;
	public final static byte AVRCP_OPERATION_ID_STOP              = 0x45;
	public final static byte AVRCP_OPERATION_ID_PAUSE             = 0x46;
	public final static byte AVRCP_OPERATION_ID_FORWARD           = 0x4B;
	public final static byte AVRCP_OPERATION_ID_BACKWARD          = 0x4C;
	public final static byte AVRCP_OPERATION_ID_FASTFORWARD       = 0x49;
	public final static byte AVRCP_OPERATION_ID_REWIND            = 0x48;
	public final static byte AVRCP_OPERATION_ID_VOLUME_UP         = 0x41;
	public final static byte AVRCP_OPERATION_ID_VOLUME_DOWN       = 0x42;

	public final static byte AVRCP_OPERATION_ID_VENDOR_UNIQUE     = 0x7E;
	public final static short AVRCP_OPERATION_ID_VENDOR_NEXT_GROUP       = 0x0000;
	public final static short AVRCP_OPERATION_ID_VENDOR_PREVIOUS_GROUP   = 0x0001;
	
	public final static byte AVRCP_MAX_ATTRIBUTE_NUM			    = 4;			// Max number of player application setting attributes
	public final static byte AVRCP_MAX_ATTRIBUTE_VALUE_NUM	    = 4;			// Max number of player application setting attibute value
	public final static byte AVRCP_MAX_EVENT_NUM			        = 20;			// Max number of event supported (TG)
	public final static byte AVRCP_MAX_MEDIA_ATTRIBUTE_ID        = 7;			// Max number of media attibute ID supported (TG)
	public final static byte AVRCP_MAX_PLAYERS_NUM		        = 20;			// Max number of Available players
	public final static byte AVRCP_MAX_FILE_ATTRIBUTE   			= 7;			// Max number of a file's attributes
	public final static byte AVRCP_MAX_ATTRIBUTE_STRING_SIZE     = 80;          // Max string leng for text
	public final static byte AVRCP_MAX_VALUE_STRING_SIZE         = 80;          // Max string leng for text
	public final static byte AVRCP_MAX_GET_ELEMENT_ATTR_NUM      = 10;          // Max number of media elements which be querying in once
	public final static byte AVRCP_MAX_GET_ELEMENT_ITEM_SIZE     = 8;           // Max number of media elements which be querying in once
	public final static byte AVRCP_MAX_PLAYER_NAME_SIZE          = 60;          // Max string leng for text
	public final static byte AVRCP_MAX_FOLDER_DEPTH_NUM          = 60;          // Max folder depth
	
	public final static short AVRCP_MAX_ELEMENET_BUFFER_SIZE       = 512;
	public final static short AVRCP_MAX_FOLDER_BUFFER_SIZE         = 512;
	public final static short AVRCP_MAX_SEARCH_TEXT_SIZE           = 128;
	
	public final static byte AVRCP_SCOPE_PLAYER_LIST                 = 0x00;
	public final static byte AVRCP_SCOPE_FILE_SYSTEM                 = 0x01;
	public final static byte AVRCP_SCOPE_SEARCH                      = 0x02;
	public final static byte AVRCP_SCOPE_NOW_PLAYING                 = 0x03;
	
	/* event */
	public static final int EVENT_PLAYBACK_STATUS_CHANGED = 0x01;
	public static final int EVENT_TRACK_CHANGED = 0x02;
	public static final int EVENT_TRACK_REACHED_END = 0x03;
	public static final int EVENT_TRACK_REACHED_START = 0x04;
	public static final int EVENT_PLAYBACK_POS_CHANGED = 0x05;
	public static final int EVENT_BATT_STATUS_CHANGED = 0x06;
	public static final int EVENT_SYSTEM_STATUS_CHANGED = 0x07;
	public static final int EVENT_PLAYER_APPLICATION_SETTING_CHANGED = 0x08;
	public static final int EVENT_NOW_PLAYING_CONTENT_CHANGED = 0x09;
	public static final int EVENT_AVAILABLE_PLAYERS_CHANGED = 0x0a;
	public static final int EVENT_ADDRESSED_PLAYER_CHANGED = 0x0b;
	public static final int EVENT_UIDS_CHANGED = 0x0c;
	public static final int EVENT_VOLUME_CHANGED = 0x0d;
	
	
	/* application settings */
	public static final int APP_SETTING_MAX_NUM     = 0x04;
	public static final int APP_SETTING_EQUALIZER   = 0x01;
	public static final int APP_SETTING_REPEAT_MODE = 0x02;
	public static final int APP_SETTING_SHUFFLE     = 0x03;
	public static final int APP_SETTING_SCAN        = 0x04;
	
	public static final int EQUALIZER_OFF   = 0x01;
	public static final int EQUALIZER_ON   = 0x02;
	
	public static final int REPEAT_MODE_OFF            = 0x01;
	public static final int REPEAT_MODE_SINGLE_TRACK   = 0x02;
	public static final int REPEAT_MODE_ALL_TRACK      = 0x03;
	public static final int REPEAT_MODE_GROUP_TRACK    = 0x04;

	public static final int SHUFFLE_OFF            = 0x01;
	public static final int SHUFFLE_ALL_TRACK      = 0x02;
	public static final int SHUFFLE_GROUP_TRACK    = 0x03;
	
	public static final int SCAN_OFF            = 0x01;
	public static final int SCAN_ALL_TRACK      = 0x02;
	public static final int SCAN_GROUP_TRACK    = 0x03;
	
	/* Avrcp key */
	public static final int AVRCP_POP_POWER             = 0x40;
	public static final int AVRCP_POP_VOLUME_UP         = 0x41;
	public static final int AVRCP_POP_VOLUME_DOWN       = 0x42;
	public static final int AVRCP_POP_MUTE              = 0x43;
	public static final int AVRCP_POP_PLAY              = 0x44;
	public static final int AVRCP_POP_STOP              = 0x45;
	public static final int AVRCP_POP_PAUSE             = 0x46;
	public static final int AVRCP_POP_RECORD            = 0x47;
	public static final int AVRCP_POP_REWIND            = 0x48;
	public static final int AVRCP_POP_FAST_FORWARD      = 0x49;
	public static final int AVRCP_POP_EJECT             = 0x4A;
	public static final int AVRCP_POP_FORWARD           = 0x4B;
	public static final int AVRCP_POP_BACKWARD          = 0x4C;
	
	/* Avrcp playing status */
	public static final int PLAY_STATUS_STOPPED         = 0x00;
	public static final int PLAY_STATUS_PLAYING         = 0x01;
	public static final int PLAY_STATUS_PAUSED          = 0x02;
	public static final int PLAY_STATUS_FWD_SEEK        = 0x03;
	public static final int PLAY_STATUS_REV_SEEK        = 0x04;
	public static final int PLAY_STATUS_ERROR           = 0xFF;
	
	
	/* Avrcp media attribute */
	
	public static final int MEIDA_ATTR_TITLE            = 0x01;
	public static final int MEIDA_ATTR_ARTIST           = 0x02;
	public static final int MEIDA_ATTR_ALBUM            = 0x03;
	public static final int MEIDA_ATTR_NUM_OF_ALBUM     = 0x04;
	public static final int MEIDA_ATTR_TOTAL_NUM        = 0x05;
	public static final int MEIDA_ATTR_GENRE            = 0x06;
	public static final int MEIDA_ATTR_PLAYING_TIME_MS  = 0x07;
	
	
	/* avrcp browsing const values */
	public static final byte DIR_UP = 0;
	public static final byte DIR_DOWN = 1;
	
	/* avrcp element type */
	public static final byte ITEM_TYPE_PLAYER  = 0x01;
	public static final byte ITEM_TYPE_FOLDER  = 0x02;
	public static final byte ITEM_TYPE_ELEMENT = 0x03;
	

	/* avrcp media type - spec page 79 */
	public static final byte MEDIA_TYPE_AUDIO  = 0x00;
	public static final byte MEDIA_TYPE_VIDEO  = 0x01; 
}


 
 
