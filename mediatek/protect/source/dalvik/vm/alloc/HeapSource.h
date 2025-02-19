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

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef DALVIK_HEAP_SOURCE_H_
#define DALVIK_HEAP_SOURCE_H_

#include "alloc/Heap.h"
#include "alloc/HeapInternal.h" // for GcHeap

/* dlmalloc uses one size_t per allocated chunk.
 */
#define HEAP_SOURCE_CHUNK_OVERHEAD         (1 * sizeof (size_t))
#define HEAP_SOURCE_WORST_CHUNK_OVERHEAD   (32 * sizeof (size_t))

/* The largest number of separate heaps we can handle.
 */
#define HEAP_SOURCE_MAX_HEAP_COUNT 2

enum HeapSourceValueSpec {
    HS_FOOTPRINT,
    HS_ALLOWED_FOOTPRINT,
    HS_BYTES_ALLOCATED,
    HS_OBJECTS_ALLOCATED
};

/*
 * Initializes the heap source; must be called before any other
 * dvmHeapSource*() functions.
 */
GcHeap *dvmHeapSourceStartup(size_t startingSize,
                             size_t maximumSize,
                             size_t growthLimit);

/*
 * If the HeapSource was created while in zygote mode, this
 * will create a new heap for post-zygote allocations.
 * Having a separate heap should maximize the number of pages
 * that a given app_process shares with the zygote process.
 */
bool dvmHeapSourceStartupAfterZygote(void);

/*
 * If the HeapSource was created while in zygote mode, this
 * will create an additional zygote heap before the first fork().
 * Having a separate heap should reduce the number of shared
 * pages subsequently touched by the zygote process.
 */
bool dvmHeapSourceStartupBeforeFork(void);

/*
 * Shutdown any threads internal to the heap source.  This should be
 * called before the heap source itself is shutdown.
 */
void dvmHeapSourceThreadShutdown(void);

/*
 * Tears down the heap source and frees any resources associated with it.
 */
void dvmHeapSourceShutdown(GcHeap **gcHeap);

/*
 * Returns the base and inclusive max addresses of the heap source
 * heaps.  The base and max values are suitable for passing directly
 * to the bitmap sweeping routine.
 */
void dvmHeapSourceGetRegions(uintptr_t *base, uintptr_t *max, size_t numHeaps);

/*
 * Get the bitmap representing all live objects.
 */
HeapBitmap *dvmHeapSourceGetLiveBits(void);

/*
 * Get the bitmap representing all marked objects.
 */
HeapBitmap *dvmHeapSourceGetMarkBits(void);

/*
 * Gets the begining of the allocation for the HeapSource.
 */
void *dvmHeapSourceGetBase(void);

/*
 * Returns the requested value. If the per-heap stats are requested, fill
 * them as well.
 */
size_t dvmHeapSourceGetValue(HeapSourceValueSpec spec,
                             size_t perHeapStats[], size_t arrayLen);

/*
 * Allocates <n> bytes of zeroed data.
 */
void *dvmHeapSourceAlloc(size_t n);

/*
 * Allocates <n> bytes of zeroed data, growing up to absoluteMaxSize
 * if necessary.
 */
void *dvmHeapSourceAllocAndGrow(size_t n);

/*
 * Frees the first numPtrs objects in the ptrs list and returns the
 * amount of reclaimed storage.  The list must contain addresses all
 * in the same mspace, and must be in increasing order. This implies
 * that there are no duplicates, and no entries are NULL.
 */
size_t dvmHeapSourceFreeList(size_t numPtrs, void **ptrs);

/*
 * Returns true iff <ptr> was allocated from the heap source.
 */
bool dvmHeapSourceContains(const void *ptr);

/*
 * Returns true iff <ptr> is within the address space managed by heap source.
 */
bool dvmHeapSourceContainsAddress(const void *ptr);

/*
 * Returns the number of usable bytes in an allocated chunk; the size
 * may be larger than the size passed to dvmHeapSourceAlloc().
 */
size_t dvmHeapSourceChunkSize(const void *ptr);

/*
 * Returns the number of bytes that the heap source has allocated
 * from the system using sbrk/mmap, etc.
 */
size_t dvmHeapSourceFootprint(void);

/*
 * Gets the maximum number of bytes that the heap source is allowed
 * to allocate from the system.
 */
size_t dvmHeapSourceGetIdealFootprint(void);

/*
 * Given the current contents of the heap, increase the allowed
 * heap footprint to match the target utilization ratio.  This
 * should only be called immediately after a full mark/sweep.
 */
void dvmHeapSourceGrowForUtilization(void);

/*
 * Walks over the heap source and passes every allocated and
 * free chunk to the callback.
 */
void dvmHeapSourceWalk(void(*callback)(const void *chunkptr, size_t chunklen,
                                      const void *userptr, size_t userlen,
                                      void *arg),
                       void *arg);
/*
 * Gets the number of heaps available in the heap source.
 */
size_t dvmHeapSourceGetNumHeaps(void);

/*
 * Exchanges the mark and object bitmaps.
 */
void dvmHeapSourceSwapBitmaps(void);

/*
 * Zeroes the mark bitmap.
 */
void dvmHeapSourceZeroMarkBitmap(void);

/*
 * Marks all objects inside the immune region of the heap. Addresses
 * at or above this pointer are threatened, addresses below this
 * pointer are immune.
 */
void dvmMarkImmuneObjects(const char *immuneLimit);

/*
 * Returns a pointer that demarcates the threatened region of the
 * heap.  Addresses at or above this pointer are threatened, addresses
 * below this pointer are immune.
 */
void *dvmHeapSourceGetImmuneLimit(bool isPartial);

/*
 * Returns the maximum size of the heap.  This value will be either
 * the value of -Xmx or a user supplied growth limit.
 */
size_t dvmHeapSourceGetMaximumSize(void);

#ifdef DALVIK_ROBUST_GC
bool dvmObjectInHeap(const void *ptr);
#endif

#endif  // DALVIK_HEAP_SOURCE_H_
