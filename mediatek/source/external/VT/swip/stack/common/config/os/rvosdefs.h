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
Filename   : rvosdefs.h
Description: definitions used by OS configuration headers
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
#ifndef RV_OSDEFS_H
#define RV_OSDEFS_H

/* Note: Adding an OS requires changing the makefiles too since */
/* these definitions are set in rvbuildconfig.h, which is generated */
/* by the makefiles. */

/* Supported Target OS's - list specifics versions below */
#define RV_OS_TYPE_SOLARIS 0
#define RV_OS_TYPE_LINUX 1
#define RV_OS_TYPE_WIN32 2
#define RV_OS_TYPE_VXWORKS 3
#define RV_OS_TYPE_PSOS 4
#define RV_OS_TYPE_OSE 5
#define RV_OS_TYPE_NUCLEUS 6
#define RV_OS_TYPE_HPUX 7
#define RV_OS_TYPE_TRU64 8
#define RV_OS_TYPE_UNIXWARE 9
#define RV_OS_TYPE_WINCE 10 /* WinCE is different enough from Win32 that it has its own OS type */
                      /* 11 */
#define RV_OS_TYPE_SYMBIAN 12
#define RV_OS_TYPE_MOPI 13  /* MOPI is a proprietary (non RV) core abstraction layer */
#define RV_OS_TYPE_INTEGRITY 14
#define RV_OS_TYPE_OSA 15
#define RV_OS_TYPE_FREEBSD 16
#define RV_OS_TYPE_MAC 17
#define RV_OS_TYPE_KAL RV_OS_TYPE_NUCLEUS 

/***** Specific OS Versions *****/

/* RV_OS_TYPE_SOLARIS */
#define RV_OS_SOLARIS_2_6 0
#define RV_OS_SOLARIS_7 1
#define RV_OS_SOLARIS_8 2
#define RV_OS_SOLARIS_9 3
#define RV_OS_SOLARIS_10 4

/* RV_OS_TYPE_LINUX */
#define RV_OS_LINUX_REDHAT      0x800
#define RV_OS_LINUX_MVISTA      0x400
#define RV_OS_LINUX_UCLINUX     0x200
#define RV_OS_LINUX_SUSE        0x100

#define RV_OS_LINUX_REDHAT_6_0  RV_OS_LINUX_REDHAT + 0
#define RV_OS_LINUX_REDHAT_6_1  RV_OS_LINUX_REDHAT + 1
#define RV_OS_LINUX_REDHAT_6_2  RV_OS_LINUX_REDHAT + 2
#define RV_OS_LINUX_REDHAT_7_0  RV_OS_LINUX_REDHAT + 3
#define RV_OS_LINUX_REDHAT_7_1  RV_OS_LINUX_REDHAT + 4
#define RV_OS_LINUX_REDHAT_7_2  RV_OS_LINUX_REDHAT + 5
#define RV_OS_LINUX_REDHAT_7_3  RV_OS_LINUX_REDHAT + 6
#define RV_OS_LINUX_REDHAT_9    RV_OS_LINUX_REDHAT + 7
#define RV_OS_LINUX_REDHAT_WS   RV_OS_LINUX_REDHAT + 8
#define RV_OS_LINUX_REDHAT_AS   RV_OS_LINUX_REDHAT + 9
#define RV_OS_LINUX_REDHAT_ES   RV_OS_LINUX_REDHAT + 10

#define RV_OS_LINUX_MVISTA_2_1  RV_OS_LINUX_MVISTA + 0
#define RV_OS_LINUX_MVISTA_3_0  RV_OS_LINUX_MVISTA + 1
#define RV_OS_LINUX_MVISTA_3_1  RV_OS_LINUX_MVISTA + 2

#define RV_OS_LINUX_UCLINUX_2_4 RV_OS_LINUX_UCLINUX + 0
#define RV_OS_LINUX_SUSE_8      RV_OS_LINUX_SUSE + 0
#define RV_OS_LINUX_SUSE_9      RV_OS_LINUX_SUSE + 1

/* RV_OS_TYPE_WIN32 */
#define RV_OS_WIN32_GENERIC 0
#define RV_OS_WIN32_95   1
#define RV_OS_WIN32_98   2
#define RV_OS_WIN32_NT4  3
#define RV_OS_WIN32_2000 4
#define RV_OS_WIN32_XP   5
#define RV_OS_WIN32_2003 6

/* RV_OS_TYPE_VXWORKS (uses Tornado version number) */
#define RV_OS_VXWORKS_2_0 0
#define RV_OS_VXWORKS_2_1 1
#define RV_OS_VXWORKS_2_2 2
#define RV_OS_VXWORKS_3_1 3

/* RV_OS_TYPE_PSOS */
#define RV_OS_PSOS_2_0 0
#define RV_OS_PSOS_2_5 1
#define RV_OS_PSOS_3_0 2

/* RV_OS_TYPE_OSE */
#define RV_OS_OSE_4_2 0
#define RV_OS_OSE_4_4 1

/* RV_OS_TYPE_NUCLEUS */
#define RV_OS_NUCLEUS_4_4 1

/* RV_OS_TYPE_HPUX */
#define RV_OS_HPUX_10_20 0
#define RV_OS_HPUX_11 1

/* RV_OS_TYPE_TRU64 */
#define RV_OS_TYPE_TRU64_4_0 0
#define RV_OS_TYPE_TRU64_5_1 1

/* RV_OS_TYPE_WINCE */
#define RV_OS_WINCE_2_11 0
#define RV_OS_WINCE_3_0  1
#define RV_OS_WINCE_4_0  2


/* RV_OS_TYPE_SYMBIAN */
#define RV_OS_SYMBIAN_7_0  0

/* RV_OS_TYPE_MOPI */
#define RV_OS_MOPI_1_4 0

/* RV_OS_TYPE_INTEGRITY */
#define RV_OS_INTEGRITY_4_0 0

/* RV_OS_TYPE_OSA */
#define RV_OS_OSA_4_1 0

/* RV_OS_TYPE_FREEBSD */
#define RV_OS_FREEBSD_4_1 0

/* RV_OS_TYPE_MAC */
#define RV_OS_MAC_DARWIN_8_0_0 0
#endif /* RV_OSDEFS_H */

