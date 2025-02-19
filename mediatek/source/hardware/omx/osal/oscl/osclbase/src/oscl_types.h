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

//       O S C L _ T Y P E S   ( B A S I C   T Y P E D E F S )

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

/*! \addtogroup osclbase OSCL Base
 *
 * @{
 */


/*! \file oscl_types.h
 *  \brief This file contains basic type definitions for common use across platforms.
 *
 */

#ifndef OSCL_TYPES_H_INCLUDED
#define OSCL_TYPES_H_INCLUDED


// include the config header for the platform
#ifndef OSCLCONFIG_H_INCLUDED
#include "osclconfig.h"
#endif

//! The c_bool type is mapped to an integer to provide a bool type for C interfaces
typedef int c_bool;


//! The OsclAny is meant to be used the context of a generic pointer (i.e., no specific type).
typedef void OsclAny;

//! mbchar is multi-byte char (e.g., UTF-8) with null termination.
typedef char mbchar;

//! The uint type is a convenient abbreviation for unsigned int.
#if !defined(__USE_MISC)
// uint is defined in some Linux platform sys\types.h
typedef unsigned int uint;
#endif

//! The octet type is meant to be used for referring to a byte or collection bytes without suggesting anything about the underlying meaning of the bytes.
typedef uint8 octet;

//! The Float type defined as OsclFloat
typedef float OsclFloat;

#ifndef OSCL_INT64_TYPES_DEFINED
//use native type
typedef OSCL_NATIVE_INT64_TYPE int64;
//use native type
typedef OSCL_NATIVE_UINT64_TYPE uint64;
#define OSCL_INT64_TYPES_DEFINED
#endif

// define OSCL_WCHAR
typedef OSCL_NATIVE_WCHAR_TYPE oscl_wchar;

//! define OSCL_TCHAR
typedef oscl_wchar OSCL_TCHAR;

// The definition of the MemoryFragment will probably
// be OS-dependant since the goal is to allow this data
// structure to be passed directly to I/O routines that take
// scatter/gather arrays.
#if ( OSCL_MEMFRAG_PTR_BEFORE_LEN )

struct OsclMemoryFragment
{
    void *ptr;
    uint32 len;
};

#else
struct OsclMemoryFragment
{
    uint32 len;
    void *ptr;
};
#endif


/*! @} */


#endif  // OSCL_TYPES_H_INCLUDED
