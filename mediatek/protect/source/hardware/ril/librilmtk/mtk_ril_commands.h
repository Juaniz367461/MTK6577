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

{RIL_REQUEST_HANGUP_ALL, dispatchVoid, responseVoid, RIL_CMD_PROXY_4},      //0 in alps/hardware/ril/include/telephony/ril.h
{RIL_REQUEST_GET_COLP, dispatchVoid, responseInts, RIL_CMD_PROXY_2},
{RIL_REQUEST_SET_COLP, dispatchInts, responseVoid, RIL_CMD_PROXY_2},
{RIL_REQUEST_GET_COLR, dispatchVoid, responseInts, RIL_CMD_PROXY_2},
{RIL_REQUEST_GET_CCM, dispatchVoid, responseString, RIL_CMD_PROXY_2},
{RIL_REQUEST_GET_ACM, dispatchVoid, responseString, RIL_CMD_PROXY_2},
{RIL_REQUEST_GET_ACMMAX, dispatchVoid, responseString, RIL_CMD_PROXY_2},
{RIL_REQUEST_GET_PPU_AND_CURRENCY, dispatchVoid, responseStrings, RIL_CMD_PROXY_2},
{RIL_REQUEST_SET_ACMMAX, dispatchStrings, responseVoid, RIL_CMD_PROXY_2},
{RIL_REQUEST_RESET_ACM, dispatchString, responseVoid, RIL_CMD_PROXY_2},
{RIL_REQUEST_SET_PPU_AND_CURRENCY, dispatchStrings, responseVoid, RIL_CMD_PROXY_2}, //10
{RIL_REQUEST_RADIO_POWEROFF, dispatchVoid, responseVoid, RIL_CMD_PROXY_1},          
{RIL_REQUEST_DUAL_SIM_MODE_SWITCH, dispatchInts, responseVoid, RIL_CMD_PROXY_1},
{RIL_REQUEST_QUERY_PHB_STORAGE_INFO, dispatchInts, responseInts, RIL_CMD_PROXY_1},
{RIL_REQUEST_WRITE_PHB_ENTRY, dispatchPhbEntry, responseVoid, RIL_CMD_PROXY_1},
{RIL_REQUEST_READ_PHB_ENTRY, dispatchInts, responsePhbEntries, RIL_CMD_PROXY_1},
{RIL_REQUEST_SET_GPRS_CONNECT_TYPE, dispatchInts, responseVoid, RIL_CMD_PROXY_3},
{RIL_REQUEST_SET_GPRS_TRANSFER_TYPE, dispatchInts, responseVoid, RIL_CMD_PROXY_3},
{RIL_REQUEST_MOBILEREVISION_AND_IMEI, dispatchInts, responseString, RIL_CMD_PROXY_3},
{RIL_REQUEST_QUERY_SIM_NETWORK_LOCK, dispatchInts, responseInts, RIL_CMD_PROXY_1},
{RIL_REQUEST_SET_SIM_NETWORK_LOCK, dispatchStrings, responseVoid, RIL_CMD_PROXY_1}, //20
{RIL_REQUEST_SET_SCRI, dispatchInts, responseVoid, RIL_CMD_PROXY_1},
{RIL_REQUEST_VT_DIAL, dispatchDial, responseVoid, RIL_CMD_PROXY_2},
{RIL_REQUEST_BTSIM_CONNECT, dispatchVoid, responseString, RIL_CMD_PROXY_1},
{RIL_REQUEST_BTSIM_DISCONNECT_OR_POWEROFF, dispatchString, responseVoid, RIL_CMD_PROXY_1},
{RIL_REQUEST_BTSIM_POWERON_OR_RESETSIM, dispatchStrings, responseString, RIL_CMD_PROXY_1},
{RIL_REQUEST_BTSIM_TRANSFERAPDU, dispatchStrings, responseString, RIL_CMD_PROXY_1},
{RIL_REQUEST_EMERGENCY_DIAL, dispatchDial, responseVoid, RIL_CMD_PROXY_2},
{RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_WITH_ACT, dispatchStrings, responseVoid,RIL_CMD_PROXY_1},
{RIL_REQUEST_QUERY_ICCID,dispatchVoid, responseString,RIL_CMD_PROXY_1},
{RIL_REQUEST_SIM_AUTHENTICATION, dispatchString, responseString, RIL_CMD_PROXY_1},
{RIL_REQUEST_USIM_AUTHENTICATION, dispatchStrings, responseString, RIL_CMD_PROXY_1},
{RIL_REQUEST_VOICE_ACCEPT, dispatchInts, responseVoid, RIL_CMD_PROXY_2},
{RIL_REQUEST_RADIO_POWERON,dispatchVoid, responseVoid,RIL_CMD_PROXY_1},
{RIL_REQUEST_GET_SMS_SIM_MEM_STATUS, dispatchVoid, responseGetSmsSimMemStatusCnf, RIL_CMD_PROXY_1},
{RIL_REQUEST_FORCE_RELEASE_CALL, dispatchInts, responseVoid, RIL_CMD_PROXY_2},
{RIL_REQUEST_SET_CALL_INDICATION, dispatchInts, responseVoid, RIL_CMD_PROXY_2},
{RIL_REQUEST_REPLACE_VT_CALL, dispatchInts, responseVoid, RIL_CMD_PROXY_2},
{RIL_REQUEST_GET_3G_CAPABILITY, dispatchVoid, responseInts, RIL_CMD_PROXY_2},
{RIL_REQUEST_SET_3G_CAPABILITY, dispatchInts, responseVoid, RIL_CMD_PROXY_2},
{RIL_REQUEST_GET_POL_CAPABILITY, dispatchVoid, responseInts, RIL_CMD_PROXY_1},
{RIL_REQUEST_GET_POL_LIST, dispatchVoid, responseStrings, RIL_CMD_PROXY_1},
{RIL_REQUEST_SET_POL_ENTRY, dispatchStrings, responseVoid, RIL_CMD_PROXY_1},
{RIL_REQUEST_QUERY_UPB_CAPABILITY, dispatchVoid, responseInts, RIL_CMD_PROXY_1},
{RIL_REQUEST_EDIT_UPB_ENTRY, dispatchStrings, responseVoid, RIL_CMD_PROXY_1},
{RIL_REQUEST_DELETE_UPB_ENTRY, dispatchInts, responseVoid, RIL_CMD_PROXY_1},
{RIL_REQUEST_READ_UPB_GAS_LIST, dispatchInts, responseStrings, RIL_CMD_PROXY_1},
{RIL_REQUEST_READ_UPB_GRP, dispatchInts, responseInts, RIL_CMD_PROXY_1},
{RIL_REQUEST_WRITE_UPB_GRP, dispatchInts, responseVoid, RIL_CMD_PROXY_1},
{RIL_REQUEST_DISABLE_VT_CAPABILITY, dispatchVoid, responseVoid, RIL_CMD_PROXY_2},
{RIL_REQUEST_HANGUP_ALL_EX, dispatchVoid, responseVoid, RIL_CMD_PROXY_4},
{RIL_REQUEST_SET_SIM_RECOVERY_ON, dispatchInts, responseVoid, RIL_CMD_PROXY_3},
{RIL_REQUEST_GET_SIM_RECOVERY_ON, dispatchVoid, responseInts, RIL_CMD_PROXY_3},
{RIL_REQUEST_SET_TRM, dispatchInts, responseVoid, RIL_CMD_PROXY_3}, 
{RIL_REQUEST_DETECT_SIM_MISSING, dispatchVoid, responseInts, RIL_CMD_PROXY_4},   // ALPS00294581
{RIL_REQUEST_GET_CALIBRATION_DATA, dispatchVoid, responseString,RIL_CMD_PROXY_3}, //add by mtk80950 for calibration data    
{RIL_REQUEST_SIM_TRANSMIT_BASIC, dispatchSIM_IO, responseSIM_IO, RIL_CMD_PROXY_1}, // NFC SEEK start
{RIL_REQUEST_SIM_OPEN_CHANNEL, dispatchString, responseInts, RIL_CMD_PROXY_1},
{RIL_REQUEST_SIM_CLOSE_CHANNEL, dispatchInts, responseVoid, RIL_CMD_PROXY_1},
{RIL_REQUEST_SIM_TRANSMIT_CHANNEL, dispatchSIM_IO, responseSIM_IO, RIL_CMD_PROXY_1},
{RIL_REQUEST_SIM_GET_ATR, dispatchVoid, responseString, RIL_CMD_PROXY_1}, // NFC SEEK end
{RIL_REQUEST_SET_NEW_MESSAGE_INDICATION, dispatchInts, responseVoid, RIL_CMD_PROXY_1},
{RIL_REQUEST_SET_SIM_SMS_READ, dispatchInts, responseVoid, RIL_CMD_PROXY_1},
{RIL_REQUEST_SET_UNSOL_OEM_HOOW_RAW_PREFIX, dispatchStrings, responseVoid, RIL_CMD_PROXY_4}, // [mt04258][121002] OEM unsol support
{RIL_REQUEST_SIM_INTERFACE_SWITCH, dispatchInts, responseVoid, RIL_CMD_PROXY_1}, // [mtk04258][20120806][International card support]
{RIL_REQUEST_2SATGES_POWER_ON_CONTINUE, dispatchVoid, responseVoid, RIL_CMD_PROXY_3}, // [mtk04258][20120806][International card support]
{RIL_REQUEST_OPERATOR_LIMIT_SERVICE, dispatchVoid, responseStrings, RIL_CMD_PROXY_3}, // [mtk04258][20120806][International card support]
