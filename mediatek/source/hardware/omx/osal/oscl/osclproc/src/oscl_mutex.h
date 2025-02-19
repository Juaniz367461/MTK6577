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

/* ------------------------------------------------------------------
 * Copyright (C) 1998-2009 PacketVideo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 * -------------------------------------------------------------------
 */

// -*- c++ -*-
// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

//                     O S C L _ M U T E X

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

/**
 *  @file oscl_mutex.h
 *  @brief This file provides implementation of mutex
 *
 */

#ifndef OSCL_MUTEX_H_INCLUDED
#define OSCL_MUTEX_H_INCLUDED

#ifndef OSCLCONFIG_PROC_H_INCLUDED
#include "osclconfig_proc.h"
#endif
#ifndef OSCL_TYPES_H_INCLUDED
#include "oscl_types.h"
#endif
#ifndef OSCL_BASE_H_INCLUDED
#include "oscl_base.h"
#endif
#ifndef OSCL_THREAD_H_INCLUDED
#include "oscl_thread.h"
#endif
#ifndef OSCL_LOCK_BASE_H_INCLUDED
#include "oscl_lock_base.h"
#endif


/**
 * Class OsclMutex
 */
class OsclMutex : public OsclLockBase
{
    public:

        /**
         * Class constructor
         */
        OSCL_IMPORT_REF OsclMutex();

        /**
         * Class destructor
         */
        OSCL_IMPORT_REF virtual ~OsclMutex();

        /**
         * Creates the Mutex
         *
         * @param No input arguments
         *
         * @return Returns the Error whether it is success or failure.
         * Incase of failure it will return what is the specific error
         */
        OSCL_IMPORT_REF OsclProcStatus::eOsclProcError Create(void);


        /**
         * Locks the Mutex
         *
         * @param It wont take any parameters
         *
         * @return Returns nothing
         */
        OSCL_IMPORT_REF void Lock();

        /**
         * Try to lock the mutex,if the Mutex is already locked calling thread
         * immediately returns with out blocking
         * @param It wont take any parameters
         *
         * @return Returns SUCCESS_ERROR if the mutex was acquired,
         * MUTEX_LOCKED_ERROR if the mutex cannot be acquired without waiting,
         * or an error code if the operation failed.
         * Note: this function may not be supported on all platforms, and
         * may return NOT_IMPLEMENTED.
         */
        OSCL_IMPORT_REF OsclProcStatus::eOsclProcError TryLock();


        /**
         * Releases the Mutex
         *
         * @param It wont take any parameters
         *
         * @return Returns nothing
         */
        OSCL_IMPORT_REF void Unlock();


        /**
         * Closes the Mutex
         *
         * @param It wont take any prameters
         *
         * @return Returns the Error whether it is success or failure.
         * Incase of failure it will return what is the specific error
         */
        OSCL_IMPORT_REF OsclProcStatus::eOsclProcError Close(void);

    private:

        /**
         * Error Mapping
         *
         * @param It will take error returned by OS specific API
         *
         * @return Returns specific error
         */
        OsclProcStatus::eOsclProcError ErrorMapping(int32 Error);

        TOsclMutexObject    ObjMutex;
        bool bCreated;

};

/**
 * Class OsclNoYieldMutex can be used in use cases where there will be
 * no CPU-yielding operation done while the Mutex is locked.
 *
 * CPU-yielding operations include OsclMutex::Lock, OsclSemphore::Wait,
 * OsclThread::Sleep, and OsclBrewThreadUtil::BThreadYield.
 *
 * The behavior of OsclNoYieldMutex depends on whether the threading model
 * is pre-emptive or not.  When threading is pre-emptive, it is identical
 * to OsclMutex.  When threading is non-pre-emptive, it is a NO-OP.
 *
 * An example of this type of use case is for simple data protection.
 *
 */
#if !OSCL_HAS_NON_PREEMPTIVE_THREAD_SUPPORT
//In pre-emptive threading, OsclNoYieldMutex is identical to OsclMutex
typedef OsclMutex OsclNoYieldMutex;
#else
//In non-pre-emptive threading, OsclNoYieldMutex is a NO-OP.
class OsclNoYieldMutex : public OsclLockBase
{
    public:

        /**
         * Class constructor
         */
        OsclNoYieldMutex()
        {
#ifndef NDEBUG
            iNumLock = 0;
            bCreated = false;
#endif
        }

        /**
         * Class destructor
         */
        virtual ~OsclNoYieldMutex()
        {}

        /**
         * Creates the Mutex
         *
         * @param No input arguments
         *
         * @return Returns the Error whether it is success or failure.
         * Incase of failure it will return what is the specific error
         */
        OsclProcStatus::eOsclProcError Create(void)
        {
#ifndef NDEBUG
            if (bCreated)
                return OsclProcStatus::INVALID_OPERATION_ERROR;
            bCreated = true;
#endif
            return OsclProcStatus::SUCCESS_ERROR;
        }


        /**
         * Locks the Mutex
         *
         * @param It wont take any parameters
         *
         * @return Returns nothing
         */
        void Lock()
        {
#ifndef NDEBUG
            OSCL_ASSERT(bCreated);
            OSCL_ASSERT(iNumLock == 0);//detect deadlock condition.
            iNumLock++;
#endif
        }

        /**
         * Try to lock the mutex,if the Mutex is already locked calling thread
         * immediately returns with out blocking
         * @param It wont take any parameters
         *
         * @return Returns SUCCESS_ERROR if the mutex was acquired,
         * MUTEX_LOCKED_ERROR if the mutex cannot be acquired without waiting,
         * or an error code if the operation failed.
         * Note: this function may not be supported on all platforms, and
         * may return NOT_IMPLEMENTED.
         */
        OsclProcStatus::eOsclProcError TryLock()
        {
#ifndef NDEBUG
            if (!bCreated)
                return OsclProcStatus::INVALID_OPERATION_ERROR;
            if (iNumLock)
                return OsclProcStatus::MUTEX_LOCKED_ERROR;
            else
                Lock();
            return OsclProcStatus::SUCCESS_ERROR;
#endif
        }


        /**
         * Releases the Mutex
         *
         * @param It wont take any parameters
         *
         * @return Returns nothing
         */
        void Unlock()
        {
#ifndef NDEBUG
            OSCL_ASSERT(bCreated);
            OSCL_ASSERT(iNumLock);
            if (iNumLock > 0)
                iNumLock--;
#endif
        }


        /**
         * Closes the Mutex
         *
         * @param It wont take any prameters
         *
         * @return Returns the Error whether it is success or failure.
         * Incase of failure it will return what is the specific error
         */
        OsclProcStatus::eOsclProcError Close(void)
        {
#ifndef NDEBUG
            if (!bCreated)
                return OsclProcStatus::INVALID_OPERATION_ERROR;
            bCreated = false;
#endif
            return OsclProcStatus::SUCCESS_ERROR;
        }

    private:

#ifndef NDEBUG
        uint32 iNumLock;
        bool bCreated;
#endif

};
#endif //OSCL_HAS_NON_PREEMPTIVE_THREAD_SUPPORT

/**
** An implementation of OsclLockBase using a mutex
**/
class OsclThreadLock: public OsclLockBase
{
    public:
        OSCL_IMPORT_REF OsclThreadLock();
        OSCL_IMPORT_REF virtual ~OsclThreadLock();
        OSCL_IMPORT_REF void Lock();
        OSCL_IMPORT_REF void Unlock();
    private:
        OsclMutex iMutex;
};

#endif



