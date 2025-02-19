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

/***********************************************************************
Filename   : rvlock.h
Description: rvlock header file
************************************************************************
      Copyright (c) 2001,2002 RADVISION Inc. and RADVISION Ltd.
************************************************************************
NOTICE:
This document contains information that is confidential and proprietary
to RADVISION Inc. and RADVISION Ltd.. No part of this document may be
reproduced in any form whatsoever without written prior approval by
RADVISION Inc. or RADVISION Ltd..

RADVISION Inc. and RADVISION Ltd. reserve the right to revise this
publication and make changes without obligation to notify any person of
such revisions or changes.
***********************************************************************/

/**********************************************************************
 *
 * DESCRIPTION:
 *			This module provides non-recursive locking functions 
 *			to use specifically for locking code sections.
 *
 **********************************************************************/

#ifndef RV_LOCK_H
#define RV_LOCK_H

#include "rvccore.h"
#include "rvadlock.h"
#include "rvlog.h"

#if !defined(RV_LOCK_TYPE) || \
   ((RV_LOCK_TYPE != RV_LOCK_SOLARIS)     && (RV_LOCK_TYPE != RV_LOCK_POSIX)          && \
    (RV_LOCK_TYPE != RV_LOCK_VXWORKS)     && (RV_LOCK_TYPE != RV_LOCK_PSOS)           && \
    (RV_LOCK_TYPE != RV_LOCK_WIN32_MUTEX) && (RV_LOCK_TYPE != RV_LOCK_WIN32_CRITICAL) && \
    (RV_LOCK_TYPE != RV_LOCK_SYMBIAN)     && (RV_LOCK_TYPE != RV_LOCK_MANUAL)         && \
    (RV_LOCK_TYPE != RV_LOCK_MOPI)        && (RV_LOCK_TYPE != RV_LOCK_OSA)			  && \
	(RV_LOCK_TYPE != RV_LOCK_NONE))
#error RV_LOCK_TYPE not set properly
#endif

#if !defined(RV_LOCK_ATTRIBUTE_DEFAULT)
#error RV_LOCK_ATTRIBUTE_DEFAULT not set properly
#endif


#if defined(__cplusplus)
extern "C" {
#endif

/**********************************************************************
 *
 *								PROTOTYPES
 *
 **********************************************************************/

/********************************************************************************************
 * RvLockInit - Initializes the Lock module.
 *
 * Must be called once (and only once) before any other functions in the module are called.
 * 
 * INPUT   : none
 * OUTPUT  : none
 * RETURN  : RV_OK if successful otherwise an error code. 
 */
RvStatus RvLockInit(void);

/********************************************************************************************
 * RvLockEnd - Shuts down the Lock module.
 *
 * Must be called once (and only once) when no further calls to this module will be made.
 * 
 * INPUT   : none
 * OUTPUT  : none
 * RETURN  : RV_OK if successful otherwise an error code. 
 */
RvStatus RvLockEnd(void);

/********************************************************************************************
 * RvLockSourceConstruct - Constructs lock module log source.
 *
 * Constructs log source to be used by common core when printing log from the 
 * lock module. This function is applied per instance of log manager.
 * 
 * INPUT   : logMgr - log manager instance
 * OUTPUT  : none
 * RETURN  : RV_OK if successful otherwise an error code. 
 */
RvStatus RvLockSourceConstruct(
	IN RvLogMgr	*logMgr);


#if (RV_LOCK_TYPE != RV_LOCK_NONE)

/********************************************************************************************
 * RvLockConstruct - Creates a locking object.
 * 
 * INPUT   : logMgr - log manager instance.
 * OUTPUT  : lock	- Pointer to lock object to be constructed.
 * RETURN  : RV_OK if successful otherwise an error code. 
 */
RVCOREAPI
RvStatus RVCALLCONV RvLockConstruct(
	IN  RvLogMgr	*logMgr, 
	OUT	RvLock		*lock);


/********************************************************************************************
 * RvLockDestruct - Destroys a locking object.
 *
 * Never destroy a lock object which has a thread suspended on it.
 * 
 * INPUT   : lock	- Pointer to lock object to be constructed.
 *			 logMgr - log manager instance.
 * OUTPUT  : none
 * RETURN  : RV_OK if successful otherwise an error code. 
 */
RVCOREAPI
RvStatus RVCALLCONV RvLockDestruct( 
	IN  RvLock *lock,
	IN  RvLogMgr* logMgr);



/********************************************************************************************
 * RvLockGet - Aquires a lock.
 *
 * Will suspend the calling task until the lock is available.
 * 
 * INPUT   : lock	- Pointer to lock object to be aquired.
 *			 logMgr - log manager instance.
 * OUTPUT  : none
 * RETURN  : RV_OK if successful otherwise an error code. 
 */
RVCOREAPI
RvStatus RVCALLCONV RvLockGet(
	IN RvLock	*lock,
	IN RvLogMgr	*logMgr);


/********************************************************************************************
 * RvLockRelease - Releases a lock.
 *
 * Do not release a lock more times than it has been aquired.
 * 
 * INPUT   : lock	- Pointer to lock object to be released.
 *			 logMgr - log manager instance.
 * OUTPUT  : none
 * RETURN  : RV_OK if successful otherwise an error code. 
 */
RVCOREAPI
RvStatus RVCALLCONV RvLockRelease(
	IN RvLock	*lock, 
	IN RvLogMgr	*logMgr);


/********************************************************************************************
 * RvLockSetAttr - Sets the options and attributes to be used when creating and using lock objects.
 *
 * Do not release a lock more times than it has been aquired.
 * note: Non-reentrant function. Do not call when other threads may be calling rvlock functions.
 * note: These attributes are global and will effect all lock functions called thereafter.
 * note: The default values for these attributes are set in rvccoreconfig.h.
 * Not thread-safe
 *
 * INPUT   : attr	- Pointer to OS speicific lock attributes to begin using.
 *           logMgr - log manager instance
 * OUTPUT  : none
 * RETURN  : Always returns RV_OK
 */
RvStatus RvLockSetAttr(
	IN RvLockAttr *attr,
	IN RvLogMgr   *logMgr);

#else
/* If none is set then none of these functions do anything */

/* prevents warnings when compiled without thread support */
#define EMPTY_LOCK_OPERATION(lock, log) ((void)(lock), (void)(log), *(lock) = RV_OK)

#define RvLockConstruct(_lg,_l) EMPTY_LOCK_OPERATION(_l, _lg)
#define RvLockDestruct(_l,_lg)  EMPTY_LOCK_OPERATION(_l, _lg)
#define RvLockGet(_l,_lg)       EMPTY_LOCK_OPERATION(_l, _lg)
#define RvLockRelease(_l,_lg)   EMPTY_LOCK_OPERATION(_l, _lg)
#define RvLockSetAttr(_l,_lg)   EMPTY_LOCK_OPERATION(_l, _lg)
#endif

#if defined(__cplusplus)
}
#endif


#endif
