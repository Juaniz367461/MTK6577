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

#ifndef MTK_OSAL_UTILS
#define MTK_OSAL_UTILS

#include <OMX_Core.h>

#define CPP_STL_SUPPORT 0
#define ANDROID 1

#if CPP_STL_SUPPORT
#include <vector>
using namespace std;
#endif

#if ANDROID
#include <sys/prctl.h>
#include <utils/Vector.h>
using namespace android;
#endif

#include <semaphore.h>
#include <unistd.h>
#include <pthread.h>
#include <signal.h>
#include <semaphore.h>


#if 1
#include <cutils/xlog.h>
#define LOG_TAG "MtkOmx"
#define MTK_OMX_LOGD(...) xlog_printf(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define MTK_OMX_LOGE(...) xlog_printf(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define MTK_OMX_LOGV(...) xlog_printf(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define MTK_OMX_LOGI(...) xlog_printf(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#else
#define MTK_OMX_LOGD(x,...) 
#define MTK_OMX_LOGE(x,...) 
#endif


// TODO:
void* MTK_OMX_ALLOC (unsigned int size);
void MTK_OMX_FREE (void* ptr);
void* MTK_OMX_MEMSET (void * ptr, int value, unsigned int num);


// pthread mutex macro
#define INIT_MUTEX(X) pthread_mutex_init(&X, NULL);
#define LOCK(X) pthread_mutex_lock(&X)
#define UNLOCK(X) pthread_mutex_unlock(&X)
#define DESTROY_MUTEX(X) pthread_mutex_destroy(&X)

// sem macro
#define INIT_SEMAPHORE(X) sem_init(&X, 0, 0);
#define WAIT(X) sem_wait(&X)
#define SIGNAL(X) sem_post(&X)
#define DESTROY_SEMAPHORE(X) sem_destroy(&X)

#define SLEEP_MS(t) \
    sched_yield();  \
    usleep(1000*t);


// write X to pipe P
#define WRITE_PIPE(X, P)   \
    ret = write(P[MTK_OMX_PIPE_ID_WRITE], &X, sizeof(X));   \
    if (-1 == ret) {    \
        LOGE ("WRITE_PIPE error, LINE:%d, errno=%d", __LINE__, errno); \
        goto EXIT;  \
    }

// read X from pipe P
#define READ_PIPE(X, P)  \
    ret = read(P[MTK_OMX_PIPE_ID_READ], &X, sizeof(X));  \
    if (-1 == ret) {    \
        LOGE ("READ_PIPE error, LINE:%d, errno=%d", __LINE__, errno);  \
        goto EXIT; \
    }

int get_sem_value (sem_t* sem);


// TODO: move to OMX utils
const char* CommandToString(OMX_U32 cmd);
const char* StateToString(OMX_U32 state);
	
#endif
