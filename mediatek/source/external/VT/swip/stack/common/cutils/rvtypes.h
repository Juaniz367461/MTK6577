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
Filename   : rvtypes.h
Description: type definitions for all RADVISION modules
************************************************************************
        Copyright (c) 2001 RADVISION Inc. and RADVISION Ltd.
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
#ifndef RV_TYPES_H
#define RV_TYPES_H

#include <stddef.h>
#include "rvconfig.h"

/* A number of constants need to be defined in the configuration headers
   in order for this file to work properly. The values should be set
   properly for the architecture, os, compiler, memory model, etc. The
   definitions required are:

   RV_SIZET_TYPE          size_t type (should always be size_t)
   RV_PTRDIFFT_TYPE       ptrdiff_t type (should always be ptrdiff_t)
   RV_CHAR_TYPE           standard character type (for char and char * only)

   RV_VAR_INT_TYPE        standard variable (non-fixed) size signed variable
   RV_VAR_UINT_TYPE       standard variable (non-fixed) size unsigned variable
   RV_SIGNED_INT8_TYPE    8 bit signed variable
   RV_UNSIGNED_INT8_TYPE  8 bit unsigned variable
   RV_SIGNED_INT16_TYPE   16 bit signed variable
   RV_UNSIGNED_INT16_TYPE 16 bit unsigned variable
   RV_SIGNED_INT32_TYPE   32 bit signed variable
   RV_UNSIGNED_INT32_TYPE 32 bit unsigned variable
   RV_SIGNED_INT64_TYPE   64 bit signed variable
   RV_UNSIGNED_INT64_TYPE 64 bit unsigned variable

   RV_VAR_INT_SUFFIX(n)        standard variable (non-fixed) size signed constant suffix
   RV_VAR_UINT_SUFFIX(n)       standard variable (non-fixed) size unsigned constant suffix
   RV_SIGNED_INT8_SUFFIX(n)    8 bit signed constant suffix
   RV_UNSIGNED_INT8_SUFFIX(n)  8 bit ubsigned constant suffix
   RV_SIGNED_INT16_SUFFIX(n)   16 bit signed constant suffix
   RV_UNSIGNED_INT16_SUFFIX(n) 16 bit unsigned constant suffix
   RV_SIGNED_INT32_SUFFIX(n)   32 bit signed constant suffix
   RV_UNSIGNED_INT32_SUFFIX(n) 32 bit unsigned constant suffix
   RV_SIGNED_INT64_SUFFIX(n)   64 bit signed constant suffix
   RV_UNSIGNED_INT64_SUFFIX(n) 64 bit unsigned constant suffix

   RV_VAR_INT_MAX  maximum value of the standard variable (non-fixed) size signed variable
   RV_VAR_INT_MIN  minimum value of the standard variable (non-fixed) size signed variable
   RV_VAR_UINT_MAX maximum value of the standard variable (non-fixed) size unsigned variable
*/

#if defined(__cplusplus)
extern "C" {
#endif

/* Our types for size_t and ptrdiff_t, should never be anything */
/* different than ANSI standard but we define it just in case. */
typedef RV_SIZET_TYPE RvSize_t;
typedef RV_PTRDIFFT_TYPE RvPtrdiff_t;

/* Character type specifically for characters, not values */
typedef RV_CHAR_TYPE RvChar;

/* Variable size types, set to optimum variable size */
typedef RV_VAR_INT_TYPE RvInt;
#define RvIntConst(_n) (RV_VAR_INT_SUFFIX(_n))
#define RV_INT_MAX RV_VAR_INT_MAX
#define RV_INT_MIN RV_VAR_INT_MIN

typedef RV_VAR_UINT_TYPE RvUint;
#define RvUintConst(_n) (RV_VAR_UINT_SUFFIX(_n))
#define RV_UINT_MAX RV_VAR_UINT_MAX
#define RV_UINT_MIN RvUintConst(0)

typedef RV_VAR_LONG_TYPE RvLong;
typedef RV_VAR_ULONG_TYPE RvUlong;
/* Fixed size types */
typedef RV_SIGNED_INT8_TYPE RvInt8;
#define RvInt8Const(_n) (RV_SIGNED_INT8_SUFFIX(_n))
#define RV_INT8_MAX RvInt8Const(127)
#define RV_INT8_MIN (RvInt8Const(-127) - RvInt8Const(1))

typedef RV_UNSIGNED_INT8_TYPE RvUint8;
#define RvUint8Const(_n) (RV_UNSIGNED_INT8_SUFFIX(_n))
#define RV_UINT8_MAX RvUint8Const(255)
#define RV_UINT8_MIN RvUint8Const(0)

typedef RV_SIGNED_INT16_TYPE RvInt16;
#define RvInt16Const(_n) (RV_SIGNED_INT16_SUFFIX(_n))
#define RV_INT16_MAX RvInt16Const(32767)
#define RV_INT16_MIN (RvInt16Const(-32767) - RvInt16Const(1))

typedef RV_UNSIGNED_INT16_TYPE RvUint16;
#define RvUint16Const(_n) (RV_UNSIGNED_INT16_SUFFIX(_n))
#define RV_UINT16_MAX RvUint16Const(65535)
#define RV_UINT16_MIN RvUint16Const(0)

typedef RV_SIGNED_INT32_TYPE RvInt32;
#define RvInt32Const(_n) (RV_SIGNED_INT32_SUFFIX(_n))
#define RV_INT32_MAX RvInt32Const(2147483647)
#define RV_INT32_MIN (RvInt32Const(-2147483647) - RvInt32Const(-1))

typedef RV_UNSIGNED_INT32_TYPE RvUint32;
#define RvUint32Const(_n) (RV_UNSIGNED_INT32_SUFFIX(_n))
#define RV_UINT32_MAX RvUint32Const(4294967295)
#define RV_UINT32_MIN RvUint32Const(0)

#if (RV_64BITS_TYPE == RV_64BITS_STANDARD)
/* standard 64 bit usage */
typedef RV_SIGNED_INT64_TYPE RvInt64;
#define RvInt64ShortConst(_n) (RV_SIGNED_INT64_SUFFIX(_n))
#define RvInt64Const2(_n)     (RV_SIGNED_INT64_SUFFIX(_n))
#define RvInt64Const(_s, _m, _l) ((RvInt64)(_s) * (((RvInt64)(_m) * RV_SIGNED_INT64_SUFFIX(0x100000000)) + (RvInt64)(_l)))
#define RV_INT64_MAX RvInt64Const(1,0x7FFFFFFF,0xFFFFFFFF)
#define RV_INT64_MIN RvInt64Const(-1,0x80000000,0)
typedef RV_UNSIGNED_INT64_TYPE RvUint64;
#define RvUint64ShortConst(_n) (RV_UNSIGNED_INT64_SUFFIX(_n))
#define RvUint64Const2(_n)     (RV_UNSIGNED_INT64_SUFFIX(_n))
#define RvUint64Const(_m, _l) (((RvUint64)(_m) * RV_UNSIGNED_INT64_SUFFIX(0x100000000)) + (RvUint64)(_l))
#define RV_UINT64_MAX RvUint64Const(0xFFFFFFFF,0xFFFFFFFF)
#define RV_UINT64_MIN RvUint64Const(0,0)

#else
/* manual 64 bit usage */
typedef struct {
    RvUint16 words[4];
} Rv64BitsBase;

typedef Rv64BitsBase RvInt64;
#define RvInt64ShortConst(_n) (RvInt64ShortAssign((_n)))
#define RvInt64Const2(_n)     (RvInt64ShortAssign((_n)))
#define RvInt64Const(_s, _m, _l) (RvInt64Assign((_s), (_m), (_l)))
#define RV_INT64_MAX RvInt64Const(1,0x7FFFFFFF,0xFFFFFFFF)
#define RV_INT64_MIN RvInt64Const(-1,0x80000000,0)

typedef Rv64BitsBase RvUint64;
#define RvUint64ShortConst(_n) (RvUint64Assign(0, (_n)))
#define RvUint64Const2(_n)     (RvUint64Assign(0, (_n)))
#define RvUint64Const(_m, _l) (RvUint64Assign((_m), (_l)))
#define RV_UINT64_MAX RvUint64Const(0xFFFFFFFF,0xFFFFFFFF)
#define RV_UINT64_MIN RvUint64Const(0,0)
#endif

/* RvIntPtr, RvUintPtr is defined as integer types capable to accommodate
 *  pointer data type without loosing 
 */
#if (RV_ARCH_BITS == RV_ARCH_BITS_32)
typedef RvInt32 RvIntPtr;
typedef RvUint32 RvUintPtr;
#elif (RV_ARCH_BITS == RV_ARCH_BITS_64)
typedef RvInt64 RvIntPtr;
typedef RvUint64 RvUintPtr;
#endif

typedef RvIntPtr RvOffset;

/* Other standard types */
typedef RvUint RvBool;
#if (!defined(RV_FALSE))
#define RV_FALSE  RvUintConst(0)
#endif
#if (!defined(RV_TRUE))
#define RV_TRUE RvUintConst(1)
#endif

/* Misc Macros. Perhaps these should go elsewhere. */
#define RvMin(_a, _b) (((_a) < (_b)) ? (_a) : (_b))
#define RvMax(_a, _b)   (((_a) > (_b)) ? (_a) : (_b))

#if (RV_OS_TYPE == RV_OS_TYPE_WIN32)
#pragma warning(push)
#pragma warning(disable : 4121)	/* alignment of a member was sensitive to packing */
#endif
/* Alignment macros. Perhaps these should go elsewhere. */
struct RvAlign_s { /* used only for natural data alignment macro */
	/* DO NOT CHANGE THE ORDER OF THE FIELDS */
    char x;
    double y;
};
#if (RV_OS_TYPE == RV_OS_TYPE_WIN32)
#pragma warning(pop)
#endif

/* Not pretty, but allows numbytes to be any valid size_t value which is */
/* a power of 2 and works no matter what the relationship between pointer */
/* and data sizes. Pay attention: argument to these macros may be evaluated */
/* multiple times, so no side effects, please */
 
#define RvAlignTo(_ptr, _numbytes) ((void *)((RvInt8 *)(_ptr) + (((RvSize_t)(_numbytes) - ((RvSize_t)(_ptr) & (RvSize_t)((_numbytes) - 1))) & (RvSize_t)((_numbytes) - 1))))

#define RvAlignValueTo(x, p2) ((x) + ( ((p2) - 1) - (((x) - 1) & ((p2) - 1)) ))


/* Natural alignment sizes and macros */
#define RV_ALIGN_DATASIZE (offsetof(struct RvAlign_s, y))
#define RV_ALIGN_PTRSIZE (sizeof(void *))
#define RV_ALIGN_SIZE (RvMax(RV_ALIGN_DATASIZE, RV_ALIGN_PTRSIZE))
#define RvAlignPtr(_x) (RvAlignTo((_x), RV_ALIGN_PTRSIZE))
#define RvAlignData(_x) (RvAlignTo((_x), RV_ALIGN_DATASIZE))
#define RvAlign(_x) (RvAlignTo((_x), RV_ALIGN_SIZE))
#define RvAlignValue(x) RvAlignValueTo(x, RV_ALIGN_SIZE)

/* Fixed width alignment macros */
#define RvAlign16(_x) (RvAlignTo((_x), 2))
#define RvAlign32(_x) (RvAlignTo((_x), 4))
#define RvAlign64(_x) (RvAlignTo((_x), 8))

/* Round up size_t size to a multiple of size_t numbytes (size must be > 0 and <= maxsize - numbytes) */
#define RvRoundToSize(_size, _numbytes) ((RvSize_t)((_size) + (_numbytes) - 1 - ((RvSize_t)((_size) - 1) % (RvSize_t)(_numbytes))))


/*******************/
/* Unused arguments macro */
#define RV_UNUSED_ARG(_arg) if (_arg) {}

/* For more than 1 argument in argList it should be placed between parenthesis, e.g.
 * RV_UNUSED_ARGS((a, b, c, d));
 *
 */
#define RV_UNUSED_ARGS(argList)   ((void)(argList))

/* to get an offset of the field within the structure */
#define RV_OFFSETOF(_type,_field) /*lint -e(413) */ ((char*)&(((_type*)0)->_field)-(char*)0)



/* Some "empty" definitons that we can use for readability of the code */
#define IN
#define OUT
#define INOUT

/************************************************************************
 * RV_DECLARE_HANDLE
 * Definition used to create a general purpose handle. It is declared
 * in this weird way to allow better type checking with C compilers.
 ************************************************************************/
#define RV_DECLARE_HANDLE(_handleName) \
                                      typedef struct { int unused; } _handleName##__ ; \
                                      typedef const _handleName##__ * _handleName; \
                                      typedef _handleName*  LP##_handleName


/* Macro used to calculate the pointer position of a struct, given one of its
   internal fields */
#define RV_GET_STRUCT(_structTypedef, _fieldName, _fieldPtr) \
    ((_structTypedef *)( ((char *)(_fieldPtr)) - offsetof(_structTypedef, _fieldName) ))


/*****************************************/
/* Macro used for useful pragma messages */
#define Value2String(m)     #m
#define Macro2String(m)     Value2String(m)

#define RvReminder          __FILE__ "(" Macro2String(__LINE__) ") : Reminder: "

/* Usage:
#pragma message(RvReminder "Remove these definitions before release")
*/

#include "rv64bits.h"

#if defined(__cplusplus)
}
#endif

typedef enum 
{
   SA_stop = 0,
   SA_start = 1
} SA_ACTION_T;

#endif /* RV_TYPES_H */
