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
Filename   : rvosconfig.h
Description: OS specific configuration
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
#ifndef RV_OSCONFIG_H
#define RV_OSCONFIG_H

/* Include proper files for the OS to be used. */
/* Supported OS's are listed in rvosdefs.h and RV_OS_TYPE */
/* should be set in rvbuildconfig.h (which is generated by */
/* the makefiles. */
#if (RV_OS_TYPE == RV_OS_TYPE_SOLARIS)
#include "rvsolaris.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_LINUX)
#include "rvlinux.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_HPUX)
#include "rvhpux.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_TRU64)
#include "rvtru64.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_UNIXWARE)
#include "rvunixware.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_WIN32)
#include "rvwin32.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_WINCE)
#include "rvwince.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_VXWORKS)
#include "rvvxworks.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_PSOS)
#include "rvpsos.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_OSE)
#include "rvose.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_NUCLEUS)
#include "rvnucleus.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_SYMBIAN)
#include "rvsymbian.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_MOPI)
#include "rvmopi.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_INTEGRITY)
#include "rvintegrity.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_OSA)
#include "rvosa.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_FREEBSD)
#include "rvfreebsd.h"
#elif (RV_OS_TYPE == RV_OS_TYPE_MAC)
#include "rvmac.h"
#else
#error RV_OS_TYPE not set properly
#endif

#endif /* RV_OSCONFIG_H */

