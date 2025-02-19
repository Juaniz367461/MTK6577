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
/* -*- c++ -*- */
// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

//           O S C L _ M E M _ B A S I C _ F U N C T I O N S

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

/*! \addtogroup osclmemory OSCL Memory
 *
 * @{
 */


/*! \file oscl_mem_basic_functions.h
    \brief This file contains prototypes for the basic memory functions.
*/

#ifndef OSCL_MEM_BASIC_FUNCTIONS_H
#define OSCL_MEM_BASIC_FUNCTIONS_H

#ifndef OSCL_BASE_H_INCLUDED
#include "oscl_base.h"
#endif

/*
 * Note: the public oscl_malloc call has been deprecated.  This
 * function is for internal use by Oscl code only.  Higher level
 * code should include "oscl_mem.h" and use OSCL_MALLOC.
 *
 * Allocates a memory block.
 *
 * @param count  number of bytes to allocate
 *
 * @return a void pointer to the allocated space, or NULL if there is insufficient
 *         memory available.
 */
OSCL_COND_IMPORT_REF void* _oscl_malloc(int32 count);

/*
 * Note: the public oscl_calloc call has been deprecated.  This
 * function is for internal use by Oscl code only.  Higher level
 * code should include "oscl_mem.h" and use OSCL_CALLOC.
 *
 *
 * Allocates a memory block and fills with zeros.
 *
 *
 * @param nelems  number of elements of size bytes to allocate.
 * @param size    the size in bytes of each element
 *
 * the total space allocated = nelems * size  bytes.
 *
 * @return a void pointer to the allocated space, or NULL if there is insufficient
 *         memory available.
 */
OSCL_COND_IMPORT_REF void* _oscl_calloc(int32 nelems, int32 size);


/*
 * Note: the public oscl_realloc call has been deprecated.  This
 * function is for internal use by Oscl code only.  Higher level
 * code should include "oscl_mem.h" and use OSCL_REALLOC.
 *
 *
 * Reallocates a memory block
 * If src is NULL, realloc behaves the same way as oscl_malloc and allocates a new
 * block of size count.
 *
 * @param src    pointer to previously allocated memory block
 * @param count  number of bytes to allocate
 *
 * @return a void pointer to the reallocated (and possibly moved) memory block. The
 *         return value is NULL if the size is zero and the buffer argument is not NULL,
 *         or if there is not enough available memory to expand the block to the given
 *         size.
 */
OSCL_COND_IMPORT_REF void* _oscl_realloc(void* src, int32 count);

/*
 * Note: the public oscl_free call has been deprecated.  This
 * function is for internal use by Oscl code only.  Higher level
 * code should include "oscl_mem.h" and use OSCL_FREE.
 *
 *
 * Deallocates or frees a memory block
 *
 * @param src    pointer to previously allocated memory block
 *
 * @return
 */
OSCL_COND_IMPORT_REF void  _oscl_free(void* src);

/**
 * Copies characters between buffers
 * The oscl_memcpy function copies count bytes of src to dest. If the source and
 * destination overlap, this function does not ensure that the original source
 * bytes in the overlapping region are copied before being overwritten. Use
 * oscl_memmove to handle overlapping regions
 *
 * @param dest   new buffer
 * @param src    buffer to copy
 * @param count  number of bytes to copy
 *
 * @return the value of dest
 */
OSCL_COND_IMPORT_REF void* oscl_memcpy(void* dest, const void* src, uint32 count);

/**
 * Moves chars from one buffer to another
 * The memmove function copies count bytes of characters from src to dest. If
 * some regions of the source area and the destination overlap, memmove ensures
 * that the original source bytes in the overlapping region are copied before
 * being overwritten.
 *
 * @param dest   new buffer
 * @param src    buffer to copy
 * @param count  number of bytes to copy
 *
 * @return the value of dest
 */
OSCL_COND_IMPORT_REF void* oscl_memmove(void* dest, const void* src, uint32 count);

/**
 * Same functionality as oscl_memmove, yet optimized for memory alligned on
 * 32-bit boundary
 *
 * @param dest   new buffer
 * @param src    buffer to copy
 * @param count  number of bytes to copy
 *
 * @return the value of dest
 */
OSCL_COND_IMPORT_REF void* oscl_memmove32(void* dest, const void* src, uint32 count);

/**
 * Sets the bytes of a buffer to a specified character
 *
 * @param dest   buffer to modify
 * @param val    character to set
 * @param count  number of bytes to set
 *
 * @return the value of dest
 */
OSCL_COND_IMPORT_REF void* oscl_memset(void* dest, uint8 val, uint32 count);

/**
 * Compare characters in two buffers
 *
 * @param buf1   first buffer
 * @param buf2   second buffer
 * @param count  number of bytes to compare
 *
 * @return <0 buf1 less than buf2
 *         0  buf1 equal to buf2
 *         >0 buf1 greater than buf2
 */
OSCL_COND_IMPORT_REF int oscl_memcmp(const void* buf1, const void* buf2, uint32 count);


#if (!OSCL_DISABLE_INLINES)
#include "oscl_mem_basic_functions.inl"
#endif

#endif

/*! @} */
