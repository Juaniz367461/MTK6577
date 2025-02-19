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

#ifndef RV_MQ_H
#define RV_MQ_H

#include "rvtypes.h"
#include "rvlog.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef RvUint32 RvSMQMsgId;
typedef RvUint32 RvSMQMsgParam;
typedef RvUint32 RvSMQMsgTargetId;

#define RvSMQErrorCode(_e) RvErrorCode(RV_ERROR_LIBCODE_CBASE, RV_CBASE_MODULE_SMQ, (_e))

#define RV_SMQ_ERROR_EMPTY             RvSMQErrorCode(1)
#define RV_SMQ_ERROR_FULL              RvSMQErrorCode(2)


/* SMQ - multi-target message queue 
 *
 * Provides ability to post messages to different registerd message targets
 *
 */

#define RV_SMQ_NONEMPTY 0

typedef struct _RvSMQ *HRvSMQ;

/* Target callback
 * Will be called to process messages posted to some target 
 */
typedef void (*RvSMQCb)(RvSMQMsgTargetId id, RvSMQMsgId message, RvSMQMsgParam p1, RvSMQMsgParam p2, void *ctx);

/* Notify callback
 * Will be called to signal various interesting events related to queue
 * Currently, only RV_SMQ_NONEMPTY signal is defined (transition from empty to non-empty state)
 * Mainly used for subclassing message queue (for example, when used to implement select preemption
 *  this function writes 1 byte of data to preemption socket)
 */
typedef void (*RvSMQNotifyCb)(HRvSMQ self, void *ctx, RvUint32 signal);

/********************************************************************************************
 * RvSMQConstruct
 *
 * Allocates and construct a new message queue of given size
 *
 *
 * INPUT   : size       - queue size
 *           notifyCb   - user-supplied notification function. This function 
 *                        will be called to notify user about various interesting 
 *                        events. Currently, the only event defined is RV_SMQ_NONEMPTY
 *                        for transition from empty to non-empty state.
 *           notifyCtx  - user-supplied context. Will be passed to notifyCb as the second 
 *                        argument.
 *                        
 *           logMgr     - log manager
 * OUTPUT  : pSelf      - pointer to SMQ handle. Will contain valid SMQ handle on success.
 * RETURN  : RV_OK on success, other on failure
 */
RvStatus
RvSMQConstruct(HRvSMQ *pSelf,  RvInt size, RvSMQNotifyCb notifyCB, void *notifyCtx, RvLogMgr *logMgr);

/********************************************************************************************
 * RvSMQDestruct
 *
 * Destructs and deallocates a message queue previously constructed by RvSMQDestruct
 *
 *
 * INPUT   : self       - SMQ handle
 *           logMgr     - log manager
 * RETURN  : RV_OK on success, other on failure
 */
RvStatus 
RvSMQDestruct(HRvSMQ self, RvLogMgr *logMgr);

/********************************************************************************************
 * RvSMQRegisterTarget
 *
 * Registers new message target
 *
 * INPUT   : self (in)      - SMQ handle
 *           cb   (in)      - per-target callback
 *           ctx  (in)      - per-target user data
 *           t    (out)     - pointer to target identifier
 *           logMgr (in)    - log manager
 * RETURN  : RV_OK on success, other on failure
 */
RvStatus 
RvSMQRegisterTarget(HRvSMQ self, RvSMQCb cb, void *ctx, RvSMQMsgTargetId *t, RvLogMgr *logMgr);

/********************************************************************************************
 * RvSMQUnRegisterTarget
 *
 * Unregisters message target
 *
 * INPUT   : self (in)      - SMQ handle
 *           t    (out)     - pointer to target identifier (as accepted from the call
 *                            to RvSMQRegisterTarget)
 *           logMgr (in)    - log manager
 * RETURN  : RV_OK on success, other on failure
 */
RvStatus 
RvSMQUnregisterTarget(HRvSMQ self, RvSMQMsgTargetId t, RvLogMgr *logMgr);

/********************************************************************************************
 * RvSMQPost
 *
 * Post message to specified target
 *
 * INPUT   : self (in)      - SMQ handle
 *           t    (out)     - pointer to target identifier (as accepted from the call
 *                            to RvSMQRegisterTarget)
 *           p1, p2 (in)    - user-defined message parameters. Message queue doesn't associate
 *                            any semantic with these parameters
 *           logMgr (in)    - log manager
 * RETURN  : RV_OK on success, other on failure
 */

RvStatus
RvSMQPost(HRvSMQ self, RvSMQMsgTargetId t, RvSMQMsgId msg, RvSMQMsgParam p1, RvSMQMsgParam p2, RvLogMgr *logMgr);

/********************************************************************************************
 * RvSMQPost
 *
 * Reads and dispatches all messages posted to this queue. Should be called from the same thread
 *   that created this message queue
 *
 * INPUT   : self (in)      - SMQ handle
 *           logMgr (in)    - log manager
 * RETURN  : RV_OK on success, other on failure
 */
RvStatus
RvSMQDispatch(HRvSMQ self, RvLogMgr *logMgr);


#ifdef __cplusplus
}
#endif

#endif

