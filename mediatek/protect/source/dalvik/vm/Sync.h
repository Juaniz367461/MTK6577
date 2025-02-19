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
 * Object synchronization functions.
 */
#ifndef DALVIK_SYNC_H_
#define DALVIK_SYNC_H_

/*
 * Monitor shape field.  Used to distinguish immediate thin locks from
 * indirecting fat locks.
 */
#define LW_SHAPE_THIN 0
#define LW_SHAPE_FAT 1
#define LW_SHAPE_MASK 0x1
#define LW_SHAPE(x) ((x) & LW_SHAPE_MASK)

/*
 * Hash state field.  Used to signify that an object has had its
 * identity hash code exposed or relocated.
 */
#define LW_HASH_STATE_UNHASHED 0
#define LW_HASH_STATE_HASHED 1
#define LW_HASH_STATE_HASHED_AND_MOVED 3
#define LW_HASH_STATE_MASK 0x3
#define LW_HASH_STATE_SHIFT 1
#define LW_HASH_STATE(x) (((x) >> LW_HASH_STATE_SHIFT) & LW_HASH_STATE_MASK)

/*
 * Monitor accessor.  Extracts a monitor structure pointer from a fat
 * lock.  Performs no error checking.
 */
#define LW_MONITOR(x) \
  ((Monitor*)((x) & ~((LW_HASH_STATE_MASK << LW_HASH_STATE_SHIFT) | \
                      LW_SHAPE_MASK)))

/*
 * Lock owner field.  Contains the thread id of the thread currently
 * holding the lock.
 */
#define LW_LOCK_OWNER_MASK 0xffff
#define LW_LOCK_OWNER_SHIFT 3
#define LW_LOCK_OWNER(x) (((x) >> LW_LOCK_OWNER_SHIFT) & LW_LOCK_OWNER_MASK)

/*
 * Lock recursion count field.  Contains a count of the numer of times
 * a lock has been recursively acquired.
 */
#define LW_LOCK_COUNT_MASK 0x1fff
#define LW_LOCK_COUNT_SHIFT 19
#define LW_LOCK_COUNT(x) (((x) >> LW_LOCK_COUNT_SHIFT) & LW_LOCK_COUNT_MASK)

struct Object;
struct Monitor;
struct Thread;

/*
 * Returns true if the lock has been fattened.
 */
#define IS_LOCK_FAT(lock)   (LW_SHAPE(*(lock)) == LW_SHAPE_FAT)

/*
 * Acquire the object's monitor.
 */
extern "C" void dvmLockObject(Thread* self, Object* obj);

/* Returns true if the unlock succeeded.
 * If the unlock failed, an exception will be pending.
 */
extern "C" bool dvmUnlockObject(Thread* self, Object* obj);

/*
 * Implementations of some java/lang/Object calls.
 */
void dvmObjectWait(Thread* self, Object* obj,
    s8 timeout, s4 nanos, bool interruptShouldThrow);
void dvmObjectNotify(Thread* self, Object* obj);
void dvmObjectNotifyAll(Thread* self, Object* obj);

/*
 * Implementation of System.identityHashCode().
 */
u4 dvmIdentityHashCode(Object* obj);

/*
 * Implementation of Thread.sleep().
 */
void dvmThreadSleep(u8 msec, u4 nsec);

/*
 * Implementation of Thread.interrupt().
 *
 * Interrupt a thread.  If it's waiting on a monitor, wake it up.
 */
void dvmThreadInterrupt(Thread* thread);

/* create a new Monitor struct */
Monitor* dvmCreateMonitor(Object* obj);

/*
 * Frees unmarked monitors from the monitor list.  The given callback
 * routine should return a non-zero value when passed a pointer to an
 * unmarked object.
 */
void dvmSweepMonitorList(Monitor** mon, int (*isUnmarkedObject)(void*));

/* free monitor list */
void dvmFreeMonitorList(void);

/*
 * Get the object a monitor is part of.
 *
 * Returns NULL if "mon" is NULL or the monitor is not part of an object
 * (which should only happen for Thread.sleep() in the current implementation).
 */
Object* dvmGetMonitorObject(Monitor* mon);

/*
 * Get the thread that holds the lock on the specified object.  The
 * object may be unlocked, thin-locked, or fat-locked.
 *
 * The caller must lock the thread list before calling here.
 */
Thread* dvmGetObjectLockHolder(Object* obj);

/*
 * Checks whether the object is held by the specified thread.
 */
bool dvmHoldsLock(Thread* thread, Object* obj);

/*
 * Relative timed wait on condition
 */
int dvmRelativeCondWait(pthread_cond_t* cond, pthread_mutex_t* mutex,
                         s8 msec, s4 nsec);

#endif  // DALVIK_SYNC_H_
