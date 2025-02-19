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
Filename   : rvlockinternal.h
Description: rvlockinternal header file
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

#ifndef RV_LOCK_INTERNAL_H
#define RV_LOCK_INTERNAL_H

#include "rvconfig.h"

#if (RV_LOCK_TYPE == RV_LOCK_NONE)
/* In case, lock was disabled, we need this include
for RvInt type definition */
#include "rvtypes.h"
#endif

#if defined(__cplusplus)
extern "C" {
#endif


/* Get include files and define RvLock and RvLockAttr types for each OS */
#if (RV_LOCK_TYPE == RV_LOCK_SOLARIS) || (RV_LOCK_TYPE == RV_LOCK_LINUX)
/* They're both posix, but the attributes are not the same, so we need 2 settings */
#include <pthread.h>
typedef pthread_mutex_t RvLock;
typedef struct {
        /* These correspond to attributes in the pthread_mutexattr struct that we let users set */
        int kind; /* solaris calls it type, never set it to recursive */
#if (RV_LOCK_TYPE == RV_LOCK_SOLARIS)
        /* Solaris specific options */
        int pshared;
        int protocol;
#endif
} RvLockAttr;

#elif (RV_LOCK_TYPE == RV_LOCK_VXWORKS)
#include <vxWorks.h>
#include <semLib.h>
typedef SEM_ID RvLock;
typedef int RvLockAttr; /* options to semBCreate */

#elif (RV_LOCK_TYPE == RV_LOCK_PSOS)
#include <psos.h>
typedef unsigned long RvLock;
typedef unsigned long RvLockAttr; /* flags to mu_create (Don't set RECURSIVE/NORECURSIVE) */

#elif (RV_LOCK_TYPE == RV_LOCK_WIN32_MUTEX)
typedef HANDLE RvLock;
typedef int RvLockAttr; /* not used */

#elif (RV_LOCK_TYPE == RV_LOCK_WIN32_CRITICAL)
#if defined(RV_LOCK_WIN32_DEBUG)
typedef struct
{
    int                 isLocked;
    CRITICAL_SECTION    lock;
} RvLock;
#else
typedef CRITICAL_SECTION RvLock;
#endif
typedef DWORD RvLockAttr; /* spin count (use only on Win2000 and newer) */

#elif (RV_LOCK_TYPE == RV_LOCK_SYMBIAN)
#include "rvtypes.h"
#include "rvsymbianinf.h"
typedef struct 
{
	RvInt	      *ServerLock;
	void          *lock;
} RvLock;
typedef int RvLockAttr;

#elif (RV_LOCK_TYPE == RV_LOCK_MOPI)
#include "mmb_rv.h"

typedef T_Mmb_Lock      RvLock;
typedef T_Mmb_LockAttr  RvLockAttr;

#elif (RV_LOCK_TYPE == RV_LOCK_OSA)
typedef OSAMutexRef RvLock;
typedef UINT32 RvLockAttr; /* not used */

#elif (RV_LOCK_TYPE == RV_LOCK_MANUAL)
#if (RV_SEMAPHORE_TYPE == RV_SEMAPHORE_NUCLEUS)
#include <nucleus.h>
typedef NU_SEMAPHORE RvLock;
typedef int RvLockAttr; /* not used, any semaphore attributes will apply */
#elif (RV_SEMAPHORE_TYPE == RV_SEMAPHORE_POSIX)
#include <semaphore.h>
typedef sem_t RvLock;
typedef int RvLockAttr; /* not used, any semaphore attributes will apply */
#elif (RV_SEMAPHORE_TYPE == RV_SEMAPHORE_OSE)
typedef SEMAPHORE *RvLock;
typedef int RvLockAttr;
#elif (RV_SEMAPHORE_TYPE == RV_SEMAPHORE_SYMBIAN)
typedef void* RvLock;
typedef int RvLockAttr; /* not used, any semaphore attributes will apply */
#elif (RV_SEMAPHORE_TYPE == RV_SEMAPHORE_MAC)
#include <semaphore.h>
typedef sem_t RvLock;
typedef int RvLockAttr; /* not used, any semaphore attributes will apply */
#endif

#elif (RV_LOCK_TYPE == RV_LOCK_NONE)
typedef RvInt RvLock;    /* Dummy types, used to prevent warnings. */
typedef RvInt RvLockAttr; /* not used */
#endif



#if defined(__cplusplus)
}
#endif

#endif /* RV_LOCK_INTERNAL_H */
