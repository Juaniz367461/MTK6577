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

/* rvadthread.h - rvadthread header file */
/************************************************************************
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

#ifndef RV_ADTHREAD_H
#define RV_ADTHREAD_H


#include "rvccore.h"


/* thread states */
#define RV_THREAD_STATE_INIT      RvInt32Const(0)
#define RV_THREAD_STATE_CREATED   RvInt32Const(1)
#define RV_THREAD_STATE_STARTING  RvInt32Const(2)
#define RV_THREAD_STATE_RUNNING   RvInt32Const(3)
#define RV_THREAD_STATE_EXITING   RvInt32Const(4)
#define RV_THREAD_STATE_DESTROYED RvInt32Const(5)
#define RV_THREAD_STATE_SPECIAL   RvInt32Const(6) /* Dummy thread attached to application controlled task */


#if (RV_THREAD_TYPE != RV_THREAD_NONE)

#include "rvlog.h"
#include "rvtime.h"
#include "rvadthread_t.h"


#ifdef __cplusplus
extern "C" {
#endif

/********************************************************************************************
 * RvThreadWrapper
 *
 * This is the RvThread wrapper called by the adapter wrapper.
 *
 * INPUT   : arg1       - the 32-bit argument which was provided by RvThread when the
 *                        thread was created
 *           callDelete - tells RvThread wrapper whether to call RvThreadDelete upon
 *                        thread termination
 * OUTPUT  : None.
 * RETURN  : None.
 */
void RvThreadWrapper(
    IN  void*           arg1,
    IN  RvBool          callDelete);


/********************************************************************************************
 * RvThreadExitted
 *
 * Can be registered or called by a thread adapter to destruct a user thread object.
 *
 * INPUT   : ptr  - address of RvThread object
 * OUTPUT  : None.
 * RETURN  : None.
 */
void RvThreadExitted(
    IN void*            ptr);


/********************************************************************************************
 * RvAdThreadInit
 *
 * Called by RvThreadInit.
 * Allows the thread adapter to perform OS specific module initialization.
 *
 * INPUT   : None.
 * OUTPUT  : None.
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvAdThreadInit(void);


/********************************************************************************************
 * RvAdThreadEnd
 *
 * Called by RvThreadEnd.
 * Allows the thread adapter to perform OS specific module clean up.
 *
 * INPUT   : None.
 * OUTPUT  : None.
 * RETURN  : None.
 */
void RvAdThreadEnd(void);


/********************************************************************************************
 * RvAdThreadConstruct
 *
 * Called by RvThreadConstruct.
 * Allows the thread adapter to perform OS specific thread initialization.
 *
 * INPUT   : tcb - address of the thread TCB
 * OUTPUT  : None.
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvAdThreadConstruct(
    IN  RvThreadBlock*  tcb);


/********************************************************************************************
 * RvAdThreadDestruct
 *
 * Called by RvThreadDestruct.
 * Allows the thread adapter to perform OS specific thread clean up.
 *
 * INPUT   : tcb - address of the thread TCB
 * OUTPUT  : None.
 * RETURN  : None.
 */
void RvAdThreadDestruct(
    IN  RvThreadBlock*  tcb);


/********************************************************************************************
 * RvAdThreadCreate
 *
 * Called by RvThreadCreate.
 * This function is called in order to spawn a new thread in the OS.
 * The new thread must be created in suspend mode.
 *
 * INPUT   : tcb           - address of the thread TCB
 *           name          - used by the OS to identify the thread
 *           priority      - the priority level to be set for the new thread
 *           attr          - set of attributes
 *           stackaddr     - address of a memory block to be used as the thread stack
 *           realstacksize - size of the stack
 *           arg1          - a 32-bit argument will be supplied to the RvThread wrapper
 * OUTPUT  : id            - the id value generated by the OS
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvAdThreadCreate(
    IN  RvThreadBlock*  tcb,
    IN  RvChar*         name,
    IN  RvInt32         priority,
    IN  RvThreadAttr*   attr,
    IN  void*           stackaddr,
    IN  RvInt32         realstacksize,
    IN  void*           arg1,
    OUT RvThreadId*     id);


/********************************************************************************************
 * RvAdThreadStart
 *
 * Called by RvThreadStart.
 * This function is called in order to resume (start) a previously created thread.
 *
 * INPUT   : tcb   - address of the thread TCB
 *           id    - the id given by the OS when the thread was created
 *           attr  - set of attributes
 *           arg1  - a 32-bit argument will be supplied to the RvThread wrapper
 * OUTPUT  : None.
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvAdThreadStart(
    IN  RvThreadBlock*  tcb,
    IN  RvThreadId*     id,
    IN  RvThreadAttr*   attr,
    IN  void*           arg1);


/********************************************************************************************
 * RvAdThreadDelete
 *
 * Called by RvThreadDestruct.
 * This function is called in order to terminate a thread.
 *
 * INPUT   : tcb   - address of the thread TCB
 *           id    - the id given by the OS when the thread was created
 * OUTPUT  : None.
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvAdThreadDelete(
    IN  RvThreadBlock*  tcb,
    IN  RvThreadId      id);


/********************************************************************************************
 * RvAdThreadWaitOnExit
 *
 * Called by RvThreadWaitOnExit.
 * This function is called iteratively until a thread is destroyed by the OS.
 *
 * INPUT   : tcb   - address of the thread TCB
 *           id    - the id given by the OS when the thread was created
 * OUTPUT  : None.
 * RETURN  : RV_OK              - if the thread has been terminated
 *           RV_ERROR_TRY_AGAIN - if the thread is still alive
 */
RvStatus RvAdThreadWaitOnExit(
    IN  RvThreadBlock*  tcb,
    IN  RvThreadId      id);


/********************************************************************************************
 * RvAdThreadSetTls
 *
 * Called by RvThreadSetupThreadPtr.
 * Stores a 32-bit data in the thread local storage.
 *
 * INPUT   : tcb     - address of the thread TCB
 *           state   - state of the thread
 *           tlsData - the 32-bit data to be saved
 * OUTPUT  : None.
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvAdThreadSetTls(
    IN  RvThreadId      id,
    IN  RvInt32         state,
    IN  void*           tlsData);


/********************************************************************************************
 * RvAdThreadGetTls
 *
 * Called by RvThreadCurrent.
 * Retrieves a 32-bit data from the thread local storage of the calling thread.
 *
 * INPUT   : None.
 * OUTPUT  : None.
 * RETURN  : the 32-bit data
 */
void* RvAdThreadGetTls(void);


/********************************************************************************************
 * RvAdThreadCurrentId
 *
 * Called by RvThreadCurrentId.
 * Returns the thread OS id of the current thread.
 *
 * INPUT   : None.
 * OUTPUT  : None.
 * RETURN  : the thread OS id
 */
RvThreadId RvAdThreadCurrentId(void);


/********************************************************************************************
 * RvAdThreadIdEqual
 *
 * Called by RvThreadIdEqual.
 * Compares 2 thread OS id's.
 *
 * INPUT   : id1 - id of the first thread
 *           id2 - id of the 2nd thread
 * OUTPUT  : None.
 * RETURN  : RV_TRUE if id's are equal. RV_FALSE otherwise.
 */
RvBool RvAdThreadIdEqual(
    IN  RvThreadId      id1,
    IN  RvThreadId      id2);


/********************************************************************************************
 * RvAdThreadSleep
 *
 * Called by RvThreadSleep.
 * Suspends the current thread for the requested amount of time.
 * Not supported by all of the OS's.
 *
 * INPUT   : t - time to sleep, represented by RvTime
 * OUTPUT  : None.
 * RETURN  : RV_OK if successful. RV_ERROR_NOTSUPPORTED if not supported.
 */
RvStatus RvAdThreadSleep(
    IN  const RvTime*   t);


/********************************************************************************************
 * RvAdThreadNanosleep
 *
 * Called by RvThreadNanosleep.
 * Suspends the current thread for the requested amount of time.
 *
 * INPUT   : nsecs - time to sleep in nanoseconds
 * OUTPUT  : None.
 * RETURN  : None.
 */
void RvAdThreadNanosleep(
    IN  RvInt64         nsecs);


/********************************************************************************************
 * RvAdThreadGetPriority
 *
 * Called by RvThreadGetOsPriority.
 * Returns the priority of a thread, in terms of the OS.
 *
 * INPUT   : id       - the id given by the OS when the thread was created
 * OUTPUT  : priority - address of variable where the priority level will be stored
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvAdThreadGetPriority(
    IN  RvThreadId      id,
    OUT RvInt32*        priority);


/********************************************************************************************
 * RvAdThreadSetPriority
 *
 * Called by RvThreadSetPriority.
 * Sets the priority of a thread.
 *
 * INPUT   : tcb      - address of the thread TCB
 *           id       - the id given by the OS when the thread was created
 *           priority - priority level to be set for the thread
 *           state    - the thread state (whether the thread has been started already or not)
 * OUTPUT  : None.
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvAdThreadSetPriority(
    IN  RvThreadBlock*  tcb,
    IN  RvThreadId      id,
    IN  RvInt32         priority,
    IN  RvInt32         state);


/********************************************************************************************
 * RvAdThreadGetName
 *
 * Called by RvThreadGetOsName.
 * Returns the name of the thread.
 *
 * INPUT   : id    - the id given by the OS when the thread was created
 *           size  - size of the output buffer
 * OUTPUT  : buf   - address of buffer where the name will be stored
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvAdThreadGetName(
    IN  RvThreadId      id,
    IN  RvInt32         size,
    OUT RvChar*         buf);

#ifdef __cplusplus
}
#endif


#else

/* RV_THREAD_TYPE == RV_THREAD_NONE */

typedef RvInt RvThreadBlock;
typedef RvInt RvThreadId;
typedef struct
{
    RvInt unused;
} RvThreadAttr;

#define RV_THREAD_PRIORITY_MAX 0

#define RvAdThreadInit() RV_OK
#define RvAdThreadEnd()
#define RvAdThreadConstruct(tcb) RV_OK
#define RvAdThreadDestruct(tcb)
#define RvAdThreadIdEqual(id1,id2) RV_TRUE
#define RvAdThreadSetPriority(tcb,id,pri,state) RV_OK
#define RvAdThreadGetName(id,size,buf) RV_OK

#endif  /* RV_THREAD_TYPE != RV_THREAD_NONE */


#endif
