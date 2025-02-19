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

/* //hardware/ril/reference-ril/ril_oem.h
**
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/
#ifndef RIL_OEM_H 
#define RIL_OEM_H 1

// TODO: requestSetMute
#define MTK_REQUEST_SET_MUTE(data,datalen,token) \
        requestSetMute(data,datalen,token)
// TODO: requestGetMute
#define MTK_REQUEST_GET_MUTE(data,datalen,token) \
        requestGetMute(data,datalen,token)
// TODO: requestResetRadio
#define MTK_REQUEST_RESET_RADIO(data,datalen,token) \
        requestResetRadio(data,datalen,token)
// TODO: requestOemHookRaw
#define MTK_REQUEST_OEM_HOOK_RAW(data,datalen,token) \
        requestOemHookRaw(data,datalen,token)
#define MTK_REQUEST_OEM_HOOK_STRINGS(data,datalen,token) \
        requestOemHookStrings(data,datalen,token)
// TODO: requestScreenState
#define MTK_REQUEST_SCREEN_STATE(data,datalen,token) \
        requestScreenState(data,datalen,token)

// TODO: requestSetMute
extern void requestSetMute(void * data, size_t datalen, RIL_Token t);
// TODO: requestGetMute
extern void requestGetMute(void * data, size_t datalen, RIL_Token t);
// TODO: requestResetRadio
extern void requestResetRadio(void * data, size_t datalen, RIL_Token t);
// TODO: requestOemHookRaw
extern void requestOemHookRaw(void * data, size_t datalen, RIL_Token t);
extern void requestOemHookStrings(void * data, size_t datalen, RIL_Token t);
// TODO: requestScreenState
extern void requestScreenState(void * data, size_t datalen, RIL_Token t);

extern void requestGet3GCapability(void * data, size_t datalen, RIL_Token t);
extern int get3GCapabilitySim();
extern void requestSet3GCapability(void * data, size_t datalen, RIL_Token t);

//MTK-START [mt04258][121002] OEM unsol support
extern void requestSetUnsolOemHookRawPrefix(void *data, size_t datalen, RIL_Token t);
//MTK-END [mt04258][121002] OEM unsol support

extern int rilOemMain(int request, void *data, size_t datalen, RIL_Token t);     
extern int rilOemUnsolicited(const char *s, const char *sms_pdu, RILChannelCtx* p_channel);

#endif /* RIL_OEM_H */


