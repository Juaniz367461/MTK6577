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

//                     O S C L _ O P A Q U E _ T Y P E

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

/*! \addtogroup osclbase OSCL Base
 * Additional osclbase comment
 * @{
 */

/*! \file oscl_opaque_type.h
    \brief The file oscl_opaque_type.h defines pure virtual classes for working
       with opaque types.
*/

#ifndef OSCL_OPAQUE_TYPE_H_INCLUDED
#define OSCL_OPAQUE_TYPE_H_INCLUDED

#ifndef OSCL_BASE_H_INCLUDED
#include "oscl_base.h"
#endif



/**
 * This class combines opaque type operations with memory allocation operations.
 */
class Oscl_Opaque_Type_Alloc
{
    public:
        virtual ~Oscl_Opaque_Type_Alloc() {}

        /**
         * Construct element at p using element at init_val as the initial value.
         * Both pointers must be non-NULL.
         */
        virtual void construct(OsclAny* p, const OsclAny* init_val) = 0;

        /**
         * Destroy element at p.
         */
        virtual void destroy(OsclAny* p) = 0;

        /**
         * Allocate "size" bytes
         */
        virtual OsclAny* allocate(const uint32 size) = 0;

        /**
         * Deallocate memory previously allocated with "allocate"
         */
        virtual void deallocate(OsclAny* p) = 0;
};

/**
 * Opaque type operations with swap & comparisons.
 */
class Oscl_Opaque_Type_Compare
{
    public:
        virtual ~Oscl_Opaque_Type_Compare() {}

        /**
         * Swap element at "a" with element at "b".
         * Both pointers must be non-NULL.
         */
        virtual void swap(OsclAny* a, const OsclAny* b) = 0;

        /**
         * Return a<b.
         */
        virtual int compare_LT(OsclAny* a, OsclAny* b) const = 0;

        /**
         * Return a==b.
         */
        virtual int compare_EQ(const OsclAny* a, const OsclAny* b) const = 0;

};

/**
 * This class combines opaque type operations with memory allocation operations
 * and linked list support
 */
class Oscl_Opaque_Type_Alloc_LL
{
    public:
        virtual ~Oscl_Opaque_Type_Alloc_LL() {}

        /**
         * Construct element at p using element at init_val as the initial value.
         * Both pointers must be non-NULL.
         */
        virtual void construct(OsclAny* p, const OsclAny* init_val) = 0;

        /**
         * Destroy element at p.
         */
        virtual void destroy(OsclAny* p) = 0;

        /**
         * Allocate "size" bytes
         */
        virtual OsclAny* allocate(const uint32 size) = 0;

        /**
         * Deallocate memory previously allocated with "allocate"
         */
        virtual void deallocate(OsclAny* p) = 0;

        /**
         * Get next element in linked list.
         */
        virtual OsclAny* get_next(const OsclAny* elem)const = 0;

        /**
         * Set next element in linked list.
         */
        virtual void set_next(OsclAny* elem, const OsclAny* nextelem) = 0;

        /**
         * Get data
         */
        virtual void get_data(OsclAny*elem, OsclAny*data_val) = 0;

        /**
         * Compare data.
         */
        virtual bool compare_data(const OsclAny*elem, const OsclAny*data_val)const = 0;
};

/*! @} */


#endif





