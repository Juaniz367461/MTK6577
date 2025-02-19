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

#ifndef OSCLCONFIG_CHECK_H_INCLUDED
#define OSCLCONFIG_CHECK_H_INCLUDED

/*! \addtogroup osclconfig OSCL config
 *
 * @{
 */

/**
\def Make sure the basic types are defined,
either in osclconfig_limits_typedefs.h or elsewhere.
*/
typedef int8 __int8__check__;
typedef uint8 __uint8__check__;
typedef int16 __int16__check__;
typedef uint16 __uint16__check__;
typedef int32 __int32__check__;
typedef uint32 __uint32__check__;

/**
\def OSCL_ASSERT_ALWAYS macro should be set to 0 or 1.
When set to 1, OSCL_ASSERT will be compiled in release mode as well
as debug mode.
*/
#ifndef OSCL_ASSERT_ALWAYS
#error "ERROR: OSCL_ASSERT_ALWAYS has to be defined to either 1 or 0."
#endif


/**
\def OSCL_DISABLE_INLINES macro should be set to 1 if
the target compiler supports 'inline' function definitions.
Otherwise it should be set to 0.
*/
#ifndef OSCL_DISABLE_INLINES
#error "ERROR: OSCL_DISABLE_INLINES has to be defined to either 1 or 0."
#endif

/**
\def OSCL_HAS_ANSI_STDLIB_SUPPORT macro should be set to 1 if
the target compiler supports ANSI C standard lib functions.
Otherwise it should be set to 0.
*/
#ifndef OSCL_HAS_ANSI_STDLIB_SUPPORT
#error "ERROR: OSCL_HAS_ANSI_STDLIB_SUPPORT has to be defined to either 1 or 0."
#endif

/**
\def OSCL_HAS_ANSI_STDIO_SUPPORT macro should be set to 1 if
the target compiler supports ANSI C standard I/O functions.
Otherwise it should be set to 0.
*/
#ifndef OSCL_HAS_ANSI_STDIO_SUPPORT
#error "ERROR: OSCL_HAS_ANSI_STDIO_SUPPORT has to be defined to either 1 or 0."
#endif

/**
\def OSCL_HAS_ANSI_STRING_SUPPORT macro should be set to 1 if
the target compiler supports ANSI C standard string functions.
Otherwise it should be set to 0.
*/
#ifndef OSCL_HAS_ANSI_STRING_SUPPORT
#error "ERROR: OSCL_HAS_ANSI_STRING_SUPPORT has to be defined to either 1 or 0."
#endif

/**
\def OSCL_HAS_UNICODE_SUPPORT macro should be set to 1 if
the target platform has a native 16-bit (wide) character type.
Otherwise it should be set to 0.
*/
#ifndef OSCL_HAS_UNICODE_SUPPORT
#error "ERROR: OSCL_HAS_UNICODE_SUPPORT has to be defined to either 1 or 0."
#endif

/**
\def _STRLIT macro should be set to an expression to convert
a constant character string into a string literal type
appropriate for the platform.
Otherwise it should be set to 0.
*/
#ifndef _STRLIT
#error "ERROR: _STRLIT has to be defined."
#endif

/**
\def _STRLIT_CHAR macro should be set to an expression to convert
a constant character string into a char string literal type
appropriate for the platform.
Otherwise it should be set to 0.
*/
#ifndef _STRLIT_CHAR
#error "ERROR: _STRLIT_CHAR has to be defined."
#endif

/**
When OSCL_HAS_UNICODE_SUPPORT==1,
\def _STRLIT_WCHAR macro should be set to an expression to convert
a constant character string into a wchar string literal type
appropriate for the platform.
Otherwise it should be set to 0.
*/
#if (OSCL_HAS_UNICODE_SUPPORT) && !defined(_STRLIT_WCHAR)
#error "ERROR: _STRLIT_WCHAR has to be defined"
#endif

/**
When OSCL_HAS_UNICODE_SUPPORT==1,
\def OSCL_NATIVE_WCHAR_TYPE macro should be set to
the native wide character type for the platform.
Otherwise it should be set to 0.
*/
#if (OSCL_HAS_UNICODE_SUPPORT) && !defined(OSCL_NATIVE_WCHAR_TYPE)
#error "ERROR: OSCL_NATIVE_WCHAR_TYPE has to be defined."
#endif

/**
\def OSCL_HAS_MSWIN_SUPPORT macro should be set to 1 if
the target platform supports the Win32 API.
Otherwise it should be set to 0.
*/
#ifndef OSCL_HAS_MSWIN_SUPPORT
#error "ERROR: OSCL_HAS_MSWIN_SUPPORT has to be defined to either 1 or 0"
#endif

/**
\def OSCL_HAS_MSWIN_SUPPORT macro should be set to 1 if
the target platform supports the Unix API.
Otherwise it should be set to 0.
*/
#ifndef OSCL_HAS_UNIX_SUPPORT
#error "ERROR: OSCL_HAS_UNIX_SUPPORT has to be defined to either 1 or 0."
#endif

/**
\def OSCL_HAS_SYMBIAN_SUPPORT macro should be set to 1 if
the target platform supports the Symbian API.
Otherwise it should be set to 0.
*/
#ifndef OSCL_HAS_SYMBIAN_SUPPORT
#error "ERROR: OSCL_HAS_SYMBIAN_SUPPORT has to be defined to either 1 or 0"
#endif

/**
\def OSCL_INTEGERS_WORD_ALIGNED macro should be set to 1 if
the target platform requires integers to be word-aligned in memory.
Otherwise it should be set to 0.
*/
#ifndef OSCL_INTEGERS_WORD_ALIGNED
#error "ERROR: OSCL_INTEGERS_WORD_ALIGNED has to be defined to either 1 or 0."
#endif

/**
\def OSCL_BYTE_ORDER_BIG_ENDIAN macro should be set to 1 if
the target platform uses big-endian byte order in memory.
Otherwise it should be set to 0.
*/
#ifndef OSCL_BYTE_ORDER_BIG_ENDIAN
#error "ERROR: OSCL_BYTE_ORDER_BIG_ENDIAN has to be defined to either 1 or 0."
#endif

/**
\def OSCL_BYTE_ORDER_LITTLE_ENDIAN macro should be set to 1 if
the target platform uses little-endian byte order in memory.
Otherwise it should be set to 0.
*/
#ifndef OSCL_BYTE_ORDER_LITTLE_ENDIAN
#error "ERROR: OSCL_BYTE_ORDER_LITTLE_ENDIAN has to be defined to either 1 or 0."
#endif

/**
\def Either OSCL_BYTE_ORDER_BIG_ENDIAN must be set to 1
or else OSCL_BYTE_ORDER_LITTLE_ENDIAN must be set to 1.
*/
#if !(OSCL_BYTE_ORDER_BIG_ENDIAN) && !(OSCL_BYTE_ORDER_LITTLE_ENDIAN)
#error "ERROR: either OSCL_BYTE_ORDER_LITTLE_ENDIAN or else OSCL_BYTE_ORDER_BIG_ENDIAN must be 1."
#endif
#if (OSCL_BYTE_ORDER_BIG_ENDIAN) && (OSCL_BYTE_ORDER_LITTLE_ENDIAN)
#error "ERROR: either OSCL_BYTE_ORDER_LITTLE_ENDIAN or else OSCL_BYTE_ORDER_BIG_ENDIAN must be 1."
#endif

/**
\def OSCL_HAS_GLOBAL_VARIABLE_SUPPORT macro should be set to 1 if
the target platform allows global variable definitions.
Otherwise it should be set to 0.
*/
#ifndef OSCL_HAS_GLOBAL_VARIABLE_SUPPORT
#error "ERROR: OSCL_HAS_GLOBAL_VARIABLE_SUPPORT has to be defined to either 1 or 0."
#endif

/**
\def When OSCL_HAS_GLOBAL_VARIABLE_SUPPORT is 0, OSCL_HAS_PARTIAL_GLOBAL_VARIABLE_SUPPORT
macro should be set to 1 if the target platform allows global variable definitions within
the Oscl base library.
Otherwise it should be set to 0.
*/
#if !(OSCL_HAS_GLOBAL_VARIABLE_SUPPORT)
#ifndef OSCL_HAS_PARTIAL_GLOBAL_VARIABLE_SUPPORT
#error "ERROR: OSCL_HAS_PARTIAL_GLOBAL_VARIABLE_SUPPORT has to be defined to either 1 or 0."
#endif
#endif

/**
Note: only one byte order mode can be defined per platform.
*/
#if (OSCL_BYTE_ORDER_LITTLE_ENDIAN) && (OSCL_BYTE_ORDER_BIG_ENDIAN)
#error "ERROR: Multiple selection for OSCL_BYTE_ORDER."
#endif

/**
\def OSCL_HAS_ANSI_STRING_SUPPORT macro should be set to 1 if
the target platform supports C standard string functions (string.h).
Otherwise it should be set to 0.
*/
#ifndef OSCL_HAS_ANSI_STRING_SUPPORT
#error "ERROR: OSCL_HAS_ANSI_STRING_SUPPORT has to be defined to either 1 or 0."
#endif

/**
\def OSCL_HAS_NATIVE_INT64_TYPE has to be defined to either 1 or 0.
*/
#ifndef OSCL_HAS_NATIVE_INT64_TYPE
#error "ERROR: OSCL_HAS_NATIVE_INT64_TYPE has to be defined to either 1 or 0."
#endif

/**
\def OSCL_HAS_NATIVE_UINT64_TYPE has to be defined to either 1 or 0.
*/
#ifndef OSCL_HAS_NATIVE_UINT64_TYPE
#error "ERROR: OSCL_HAS_NATIVE_UINT64_TYPE has to be defined to either 1 or 0."
#endif

/**
\def When OSCL_HAS_NATIVE_INT64_TYPE is 1,
OSCL_NATIVE_INT64_TYPE has to be defined to the native
signed 64-bit integer type.
*/
#if OSCL_HAS_NATIVE_INT64_TYPE
#ifndef OSCL_NATIVE_INT64_TYPE
#error "ERROR: OSCL_NATIVE_INT64_TYPE has to be defined."
#endif
#endif

/**
\def When OSCL_HAS_NATIVE_UINT64_TYPE is 1,
OSCL_NATIVE_UINT64_TYPE has to be defined to the native
unsigned 64-bit integer type.
*/
#if OSCL_HAS_NATIVE_UINT64_TYPE
#ifndef OSCL_NATIVE_UINT64_TYPE
#error "ERROR: OSCL_NATIVE_UINT64_TYPE has to be defined."
#endif
#endif

/**
\def When OSCL_HAS_NATIVE_INT64_TYPE is 1,
INT64(x) has to be defined to the expression for a signed
64-bit literal.
*/
#if OSCL_HAS_NATIVE_INT64_TYPE
#ifndef INT64
#error "ERROR: INT64(x) has to be defined."
#endif
#endif

/**
\def When OSCL_HAS_NATIVE_UINT64_TYPE is 1,
INT64(x) has to be defined to the expression for a signed
64-bit literal.
*/
#if OSCL_HAS_NATIVE_UINT64_TYPE
#ifndef UINT64
#error "ERROR: UINT64(x) has to be defined."
#endif
#endif

/**
\def When OSCL_HAS_NATIVE_INT64_TYPE is 1,
INT64_HILO(high,low) has to be defined to an expression
to create a signed 64-bit integer from 2 32-bit integers.
*/
#if OSCL_HAS_NATIVE_INT64_TYPE
#ifndef INT64_HILO
#error "ERROR: INT64_HILO(high,low) has to be defined."
#endif
#endif

/**
\def When OSCL_HAS_NATIVE_UINT64_TYPE is 1,
UINT64_HILO(high,low) has to be defined to an expression
to create an unsigned 64-bit integer from 2 32-bit integers.
*/
#if OSCL_HAS_NATIVE_UINT64_TYPE
#ifndef UINT64_HILO
#error "ERROR: UINT64_HILO(high,low) has to be defined."
#endif
#endif

/**
\def OSCL_MEMFRAG_PTR_BEFORE_LEN macro should be set to 1 if
memory fragements data structures, such as used by sendmsg
(i.e., the iovec data structures), should use ptr before length.
Otherwise it should be set to 0.
*/
#ifndef OSCL_MEMFRAG_PTR_BEFORE_LEN
#error "ERROR: OSCL_MEMFRAG_PTR_BEFORE_LEN has to be defined to either 0 or 1"
#endif

/**
\def OSCL_HAS_TLS_SUPPORT macro should be set to 1 if
the target platform has thread-local storage functions.
Otherwise it should be set to 0.
*/
#ifndef OSCL_HAS_TLS_SUPPORT
#error "ERROR: OSCL_HAS_TLS_SUPPORT has to be defined to either 1 or 0"
#endif

/**
\def OSCL_TLS_IS_KEYED macro should be set to 1 if
the target platform's thread local storage function requires an
input key value to uniquely identify the TLS.
If the thread local storage function does not require any key,
or thread local storage is not supported, it should be set to 0.
*/
#ifndef OSCL_TLS_IS_KEYED
#error "ERROR: OSCL_TLS_IS_KEYED has to be defined to either 1 or 0"
#endif


/**
When OSCL_TLS_IS_KEYED==1,
\def OSCL_TLS_STORE_FUNC macro must be set to an expression that will
set the TLS value and evalutes to true on success, false on failure.
The macro takes 2 input parameters (key, ptr).
*/
#if (OSCL_TLS_IS_KEYED) && !defined(OSCL_TLS_STORE_FUNC)
#error "ERROR: OSCL_TLS_STORE_FUNC has to be defined"
#endif

/**
When OSCL_TLS_IS_KEYED==1,
\def OSCL_TLS_GET_FUNC macro should be set to an expression that
returns the TLS value.
The macro takes 1 input parameter (key).
*/
#if (OSCL_TLS_IS_KEYED) && !defined(OSCL_TLS_GET_FUNC)
#error "ERROR: OSCL_TLS_GET_FUNC has to be defined"
#endif

/**
When OSCL_TLS_IS_KEYED==1,
\def OSCL_TLS_GET_FUNC macro should be set to an expression that
creates a TLS entry and evalutes to true on success, false on failure.
The macro takes 1 input parameter (key).
*/
#if (OSCL_TLS_IS_KEYED) && !defined(OSCL_TLS_KEY_CREATE_FUNC)
#error "ERROR: OSCL_TLS_KEY_CREATE_FUNC has to be defined"
#endif

/**
When OSCL_TLS_IS_KEYED==1,
\def OSCL_TLS_GET_FUNC macro should be set to an expression that
deletes a TLS entry.
The macro takes 1 input parameter (key).
*/
#if (OSCL_TLS_IS_KEYED) && !defined(OSCL_TLS_KEY_DELETE_FUNC)
#error "ERROR: OSCL_TLS_KEY_DELETE_FUNC has to be defined"
#endif


/**
When OSCL_TLS_IS_KEYED==0,
\def OSCL_TLS_STORE_FUNC macro must be set to an expression that will
set the TLS value and evalutes to true on success, false on failure.
The macro takes 1 input parameter (ptr).
*/
#if (OSCL_HAS_TLS_SUPPORT) && !(OSCL_TLS_IS_KEYED) && !defined(OSCL_TLS_STORE_FUNC)
#error "ERROR: OSCL_TLS_STORE_FUNC has to be defined"
#endif

/**
When OSCL_TLS_IS_KEYED==0,
\def OSCL_TLS_GET_FUNC macro should be set to an expression that
returns the TLS value.
*/
#if (OSCL_HAS_TLS_SUPPORT) && !(OSCL_TLS_IS_KEYED) && !defined(OSCL_TLS_GET_FUNC)
#error "ERROR: OSCL_TLS_GET_FUNC has to be defined"
#endif

/**
OSCL_HAS_BASIC_LOCK should be set to 1 if the platform has basic lock support.
*/
#if !defined(OSCL_HAS_BASIC_LOCK )
#error "ERROR: OSCL_HAS_BASIC_LOCK must be defined to 0 or 1"
#endif


/**
When OSCL_HAS_BASIC_LOCK is 1,
type TOsclBasicLockObject should be defined as the type used as
a mutex object or handle on the target platform.  It can
be either typedef'd as a C-compilable type or can be #defined.
Examples:
typedef pthread_mutex_t TOsclBasicLockObject;
#define TOsclBasicLockObject RMutex
*/
#if (OSCL_HAS_BASIC_LOCK) && !defined(TOsclBasicLockObject)
typedef TOsclBasicLockObject __verify__TOsclBasicLockObject__defined__;
#endif

/*! @} */

#endif // OSCLCONFIG_CHECK_H_INCLUDED


