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

/** 
 * @file 
 *   val_log.h 
 *
 * @par Project:
 *   MFlexVideo 
 *
 * @par Description:
 *   Log System
 *
 * @par Author:
 *   Jackal Chen (mtk02532)
 *
 * @par $Revision: #5 $
 * @par $Modtime:$
 * @par $Log:$
 *
 */

#ifndef _VAL_LOG_H_
#define _VAL_LOG_H_

#ifdef LOG_TAG
#undef LOG_TAG
#endif
#define LOG_TAG "MFV_COMMON"
#include <utils/Log.h>
#include <cutils/xlog.h>

#ifdef __cplusplus
extern "C" {
#endif

#define MFV_LOG_ERROR   //error
#ifdef MFV_LOG_ERROR
#define MFV_LOGE(...) xlog_printf(ANDROID_LOG_ERROR, "VDO_LOG", __VA_ARGS__);
#define VDO_LOGE(...) xlog_printf(ANDROID_LOG_ERROR, "VDO_LOG", __VA_ARGS__);
#else
#define MFV_LOGE(...)
#define VDO_LOGE(...)
#endif

#define MFV_LOG_WARNING //warning
#ifdef MFV_LOG_WARNING
#define MFV_LOGW(...) xlog_printf(ANDROID_LOG_WARN, "VDO_LOG", __VA_ARGS__);
#define VDO_LOGW(...) xlog_printf(ANDROID_LOG_WARN, "VDO_LOG", __VA_ARGS__);
#else
#define MFV_LOGW(...)
#define VDO_LOGW(...)
#endif

//#define MFV_LOG_DEBUG   //debug information
#ifdef MFV_LOG_DEBUG
#define MFV_LOGD(...) xlog_printf(ANDROID_LOG_DEBUG, "VDO_LOG", __VA_ARGS__);
#define VDO_LOGD(...) xlog_printf(ANDROID_LOG_DEBUG, "VDO_LOG", __VA_ARGS__);
#else
#define MFV_LOGD(...)
#define VDO_LOGD(...)
#endif

#define MFV_LOG_INFO   //information
#ifdef MFV_LOG_INFO
#define MFV_LOGI(...) xlog_printf(ANDROID_LOG_INFO, "VDO_LOG", __VA_ARGS__);
#define VDO_LOGI(...) xlog_printf(ANDROID_LOG_INFO, "VDO_LOG", __VA_ARGS__);
#else
#define MFV_LOGI(...)
#define VDO_LOGI(...)
#endif

#ifdef __cplusplus
}
#endif

#endif // #ifndef _VAL_LOG_H_
