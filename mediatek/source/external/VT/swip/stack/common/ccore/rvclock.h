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
Filename   : rvclock.h
Description: This module provides functions for accessing the wall clock (time of day).
************************************************************************
      Copyright (c) 2001,2002 RADVISION Inc. and RADVISION Ltd.
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

#ifndef RV_CLOCK_H
#define RV_CLOCK_H

#include "rvccore.h"
#include "rvtime.h"
#include "rvlog.h"

#if !defined(RV_CLOCK_TYPE) || \
    ((RV_CLOCK_TYPE != RV_CLOCK_LINUX)    && (RV_CLOCK_TYPE != RV_CLOCK_VXWORKS) && \
     (RV_CLOCK_TYPE != RV_CLOCK_PSOS)     && (RV_CLOCK_TYPE != RV_CLOCK_OSE)     && \
     (RV_CLOCK_TYPE != RV_CLOCK_NUCLEUS)  && (RV_CLOCK_TYPE != RV_CLOCK_SOLARIS) && \
     (RV_CLOCK_TYPE != RV_CLOCK_WIN32)    && (RV_CLOCK_TYPE != RV_CLOCK_TRU64)   && \
     (RV_CLOCK_TYPE != RV_CLOCK_UNIXWARE) && (RV_CLOCK_TYPE != RV_CLOCK_SYMBIAN) && \
     (RV_CLOCK_TYPE != RV_CLOCK_MOPI)	  && (RV_CLOCK_TYPE != RV_CLOCK_OSA))
#error RV_CLOCK_TYPE not set properly
#endif


#include "rvadclock.h"


#if defined(__cplusplus)
extern "C" {
#endif


/********************************************************************************************
 * RvClockInit
 * Initializes the Clock module. Must be called once (and
 * only once) before any other functions in the module are called.
 * INPUT   : None
 * OUTPUT  : None
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvClockInit(void);


/********************************************************************************************
 * RvClockEnd
 * Shuts down the Clock module. Must be called once (and
 * only once) when no further calls to this module will be made.
 * INPUT   : None
 * OUTPUT  : None
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvClockEnd(void);

/********************************************************************************************
 * RvClockSourceConstruct -Constructs clock module log source
 *
 * Constructs clock module log source for printing log per specific
 * log manager instance
 *
 * INPUT   : logMgr	- log manager instance
 * OUTPUT  : None.
 * RETURN  : RV_OK if successful otherwise an error code.
 */
RvStatus RvClockSourceConstruct(
	IN RvLogMgr* logMgr);

/********************************************************************************************
 * RvClockGet
 * Get time since epoch -- can vary due to system time adjustments
 * returns time in seconds and fills result with seconds and nanoseconds
 * INPUT   : logMgr - log manager instance
 * OUTPUT  : Pointer to time structure where time will be stored.
 *           If NULL, the seconds will still be returned.
 * RETURN  : if result is not NULL. Epoch is January 1, 1970
 */
RVCOREAPI 
RvInt32 RVCALLCONV RvClockGet(
    IN  RvLogMgr  *logMgr,
    OUT RvTime    *t);


/********************************************************************************************
 * RvClockSet
 * Sets time of day in seconds and nanoseconds since January 1, 1970.
 * INPUT   : t - Pointer to time structure with the time to be set.
 *           logMgr - log manager instance
 * OUTPUT  : None
 * RETURN  : RV_OK if successful otherwise an error code.
 * Note    : On some systems the nanoseconds portion of the time will be ignored.
 *           On some systems setting the clock requires special priviledges.
 */
RVCOREAPI
RvStatus RVCALLCONV RvClockSet(
    IN const RvTime *t,
	IN RvLogMgr     *logMgr);


/********************************************************************************************
 * RvClockResolution
 * Gets the resolution of the wall clock in seconds and nanoseconds params.
 * INPUT   : logMgr - log manager instance
 * OUTPUT  : t - Pointer to time structure where resolution will be stored.
 *               If NULL, the nanoseconds will still be returned.
 * RETURN  : nanoseconds portion of the clock resolution.
 */
RVCOREAPI 
RvInt32 RVCALLCONV RvClockResolution(
    IN  RvLogMgr *logMgr,
    OUT RvTime   *t);


#if defined(__cplusplus)
}
#endif


#endif
