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
 * Copyright (c) 1992, 1993, 1994, 1995, 1996
 *	The Regents of the University of California.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that: (1) source code distributions
 * retain the above copyright notice and this paragraph in its entirety, (2)
 * distributions including binary code include the above copyright notice and
 * this paragraph in its entirety in the documentation or other materials
 * provided with the distribution, and (3) all advertising materials mentioning
 * features or use of this software display the following acknowledgement:
 * ``This product includes software developed by the University of California,
 * Lawrence Berkeley Laboratory and its contributors.'' Neither the name of
 * the University nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior
 * written permission.
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 *
 * @(#) $Header: /tcpdump/master/tcpdump/fddi.h,v 1.11 2002/12/11 07:13:51 guy Exp $ (LBL)
 */

/*
 * Based on Ultrix if_fddi.h
 */

/*
 * This stuff should come from a system header file, but there's no
 * obviously portable way to do that and it's not really going
 * to change from system to system (except for the padding business).
 */

struct fddi_header {
	u_char  fddi_fc;		/* frame control */
	u_char  fddi_dhost[6];
	u_char  fddi_shost[6];
};

/*
 * Length of an FDDI header; note that some compilers may pad
 * "struct fddi_header" to a multiple of 4 bytes, for example, so
 * "sizeof (struct fddi_header)" may not give the right
 * answer.
 */
#define FDDI_HDRLEN 13

/* Useful values for fddi_fc (frame control) field */

/*
 * FDDI Frame Control bits
 */
#define	FDDIFC_C		0x80		/* Class bit */
#define	FDDIFC_L		0x40		/* Address length bit */
#define	FDDIFC_F		0x30		/* Frame format bits */
#define	FDDIFC_Z		0x0f		/* Control bits */

/*
 * FDDI Frame Control values. (48-bit addressing only).
 */
#define	FDDIFC_VOID		0x40		/* Void frame */
#define	FDDIFC_NRT		0x80		/* Nonrestricted token */
#define	FDDIFC_RT		0xc0		/* Restricted token */
#define	FDDIFC_SMT_INFO		0x41		/* SMT Info */
#define	FDDIFC_SMT_NSA		0x4F		/* SMT Next station adrs */
#define	FDDIFC_MAC_BEACON	0xc2		/* MAC Beacon frame */
#define	FDDIFC_MAC_CLAIM	0xc3		/* MAC Claim frame */
#define	FDDIFC_LLC_ASYNC	0x50		/* Async. LLC frame */
#define	FDDIFC_LLC_SYNC		0xd0		/* Sync. LLC frame */
#define	FDDIFC_IMP_ASYNC	0x60		/* Implementor Async. */
#define	FDDIFC_IMP_SYNC		0xe0		/* Implementor Synch. */
#define FDDIFC_SMT		0x40		/* SMT frame */
#define FDDIFC_MAC		0xc0		/* MAC frame */

#define	FDDIFC_CLFF		0xF0		/* Class/Length/Format bits */
#define	FDDIFC_ZZZZ		0x0F		/* Control bits */
