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

//         O S C L _ S H A R E D _ P T R

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

/*! \addtogroup osclbase OSCL Base
 *
 * @{
 */


/*! \file oscl_shared_ptr.h
    \brief This file defines a template class OsclSharedPtr which is a "smart pointer" to the parameterized type.
*/


#ifndef OSCL_SHARED_PTR_H_INCLUDED
#define OSCL_SHARED_PTR_H_INCLUDED

#ifndef OSCL_BASE_H_INCLUDED
#include "oscl_base.h"
#endif

#ifndef OSCL_REFCOUNTER_H_INCLUDED
#include "oscl_refcounter.h"
#endif

#define OSCL_DISABLE_WARNING_RETURN_TYPE_NOT_UDT
#include "osclconfig_compiler_warnings.h"

//! A parameterized smart pointer class.
template <class TheClass>
class OsclSharedPtr
{
    public:
        //! Constructor
        OsclSharedPtr() :
                mpRep(NULL), refcnt(NULL) {}

        //! Constructor
        /*!
          \param inClassPtr A pointer to an instance of the parameterized type that the new OsclSharedPtr will wrap.
        */
        OsclSharedPtr(TheClass* inClassPtr, OsclRefCounter* in_refcnt) :
                mpRep(inClassPtr), refcnt(in_refcnt) {};


        //! Copy constructor
        OsclSharedPtr(const OsclSharedPtr& inSharedPtr) :
                mpRep(inSharedPtr.mpRep), refcnt(inSharedPtr.refcnt)
        {
            if (refcnt)
            {
                refcnt->addRef();
            }
        }


        //! Destructor.
        virtual ~OsclSharedPtr()
        {
            if (refcnt != NULL)
            {
                refcnt->removeRef();
            }
        } // end destructor


        //! The dereferencing operator returns a pointer to the parameterized type and can be used to access member
        //! elements of TheClass.
        TheClass* operator->()
        {
            return mpRep;
        }

        //! The indirection operator returns a reference to an object of the parameterized type.
        TheClass& operator*()
        {
            return *mpRep;
        }

        //! Casting operator
        operator TheClass*()
        {
            return mpRep;
        }

        //! Use this function to get a pointer to the wrapped object.
        TheClass* GetRep()
        {
            return mpRep;
        }

        //! Get the refcount pointer.  This should primarily be used for conversion operations
        OsclRefCounter* GetRefCounter()
        {
            return refcnt;
        }

        //! Get a count of how many references to the object exist.
        int get_count()
        {
            return (refcnt == NULL) ? 0 : refcnt->getCount();
        }

        //! Use this function to bind an existing OsclSharedPtr to a already-wrapped object.
        void Bind(const OsclSharedPtr& inHandle);

        //! Use this function to bind an existing OsclSharedPtr to a new (unwrapped) object.
        void Bind(TheClass* ptr, OsclRefCounter* in_refcnt);

        //! Use this function of unbind an existing OsclSharedPtr.
        void Unbind()
        {
            Bind(NULL, NULL);
        };

        //! Assignment operator.
        OsclSharedPtr& operator=(const OsclSharedPtr& inSharedPtr)
        {
            Bind(inSharedPtr);
            return *this;
        }

        //! Test for equality to see if two PVHandles wrap the same object.
        bool operator==(const OsclSharedPtr& b) const;

    private:

        TheClass* mpRep;
        OsclRefCounter* refcnt;

};


template <class TheClass> inline bool OsclSharedPtr<TheClass>::operator==(const OsclSharedPtr<TheClass>& b) const
{
    if ((this->mpRep == b.mpRep) &&
            (this->refcnt == b.refcnt))
    {
        return true;
    }
    return false;
}


template <class TheClass> inline void OsclSharedPtr<TheClass>::Bind(const OsclSharedPtr& inSharedPtr)
{
    if (mpRep == inSharedPtr.mpRep) return;

    if (refcnt != NULL)
    {
        refcnt->removeRef();
    }

    refcnt = inSharedPtr.refcnt;
    mpRep = inSharedPtr.mpRep;

    if (refcnt != NULL)
    {
        refcnt->addRef();
    }

}

template <class TheClass> inline void OsclSharedPtr<TheClass>::Bind(TheClass* ptr,
        OsclRefCounter* in_refcnt)
{
    if (refcnt != NULL)
    {
        refcnt->removeRef();
    }

    mpRep = ptr;
    refcnt = in_refcnt;

}

#endif  // OSCL_SHARED_PTR_H_INCLUDED
