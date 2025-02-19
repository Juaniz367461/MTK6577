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

/*
 * Maintain a table of references.  Used for internal local references,
 * JNI monitor references, and JNI pinned array references.
 *
 * None of the table functions are synchronized.
 */
#ifndef DALVIK_REFERENCETABLE_H_
#define DALVIK_REFERENCETABLE_H_

/*
 * Table definition.
 *
 * The expected common operations are adding a new entry and removing a
 * recently-added entry (usually the most-recently-added entry).
 *
 * If "allocEntries" is not equal to "maxEntries", the table may expand when
 * entries are added, which means the memory may move.  If you want to keep
 * pointers into "table" rather than offsets, use a fixed-size table.
 *
 * (This structure is still somewhat transparent; direct access to
 * table/nextEntry is allowed.)
 */
struct ReferenceTable {
    Object**        nextEntry;          /* top of the list */
    Object**        table;              /* bottom of the list */

    int             allocEntries;       /* #of entries we have space for */
    int             maxEntries;         /* max #of entries allowed */
};

/*
 * Initialize a ReferenceTable.
 *
 * If "initialCount" != "maxCount", the table will expand as required.
 *
 * Returns "false" if table allocation fails.
 */
bool dvmInitReferenceTable(ReferenceTable* pRef, int initialCount,
    int maxCount);

/*
 * Clears out the contents of a ReferenceTable, freeing allocated storage.
 * Does not free "pRef".
 *
 * You must call dvmInitReferenceTable() before you can re-use this table.
 */
void dvmClearReferenceTable(ReferenceTable* pRef);

/*
 * Return the #of entries currently stored in the ReferenceTable.
 */
INLINE size_t dvmReferenceTableEntries(const ReferenceTable* pRef)
{
    return pRef->nextEntry - pRef->table;
}

/*
 * Returns "true" if the table is full.  The table is considered full if
 * we would need to expand it to add another entry.
 */
INLINE size_t dvmIsReferenceTableFull(const ReferenceTable* pRef)
{
    return dvmReferenceTableEntries(pRef) == (size_t)pRef->allocEntries;
}

/*
 * Add a new entry.  "obj" must be a valid non-NULL object reference
 * (though it's okay if it's not fully-formed, e.g. the result from
 * dvmMalloc doesn't have obj->clazz set).
 *
 * Returns "false" if the table is full.
 */
bool dvmAddToReferenceTable(ReferenceTable* pRef, Object* obj);

/*
 * Determine if "obj" is present in "pRef".  Stops searching when we hit
 * "bottom".  To include the entire table, pass in "pRef->table" as the
 * bottom.
 *
 * Returns NULL if "obj" was not found.
 */
Object** dvmFindInReferenceTable(const ReferenceTable* pRef, Object** bottom,
    Object* obj);

/*
 * Remove an existing entry.
 *
 * We stop searching for a match after examining the element at "bottom".
 * This is useful when entries are associated with a stack frame.
 *
 * Returns "false" if the entry was not found.
 */
bool dvmRemoveFromReferenceTable(ReferenceTable* pRef, Object** bottom,
    Object* obj);

/*
 * Dump the contents of a reference table to the log file.
 *
 * The caller should lock any external sync before calling.
 */
void dvmDumpReferenceTable(const ReferenceTable* pRef, const char* descr);

/*
 * Internal function, shared with IndirectRefTable.
 */
void dvmDumpReferenceTableContents(Object* const* refs, size_t count,
    const char* descr);

#endif  // DALVIK_REFERENCETABLE_H_
