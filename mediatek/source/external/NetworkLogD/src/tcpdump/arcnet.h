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
 * Copyright (c) 1982, 1986, 1993
 *	The Regents of the University of California.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *	This product includes software developed by the University of
 *	California, Berkeley and its contributors.
 * 4. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * @(#) $Id: arcnet.h,v 1.3 2003/01/23 09:05:37 guy Exp $ (LBL)
 *
 * from: NetBSD: if_arc.h,v 1.13 1999/11/19 20:41:19 thorpej Exp
 */

/*
 * Structure of a 2.5MB/s Arcnet header on the BSDs,
 * as given to interface code.
 */
struct	arc_header {
	u_int8_t  arc_shost;
	u_int8_t  arc_dhost;
	u_int8_t  arc_type;
	/*
	 * only present for newstyle encoding with LL fragmentation.
	 * Don't use sizeof(anything), use ARC_HDR{,NEW}LEN instead.
	 */
	u_int8_t  arc_flag;
	u_int16_t arc_seqid;

	/*
	 * only present in exception packets (arc_flag == 0xff)
	 */
	u_int8_t  arc_type2;	/* same as arc_type */
	u_int8_t  arc_flag2;	/* real flag value */
	u_int16_t arc_seqid2;	/* real seqid value */
};

#define	ARC_HDRLEN		3
#define	ARC_HDRNEWLEN		6
#define	ARC_HDRNEWLEN_EXC	10

/* RFC 1051 */
#define	ARCTYPE_IP_OLD		240	/* IP protocol */
#define	ARCTYPE_ARP_OLD		241	/* address resolution protocol */

/* RFC 1201 */
#define	ARCTYPE_IP		212	/* IP protocol */
#define	ARCTYPE_ARP		213	/* address resolution protocol */
#define	ARCTYPE_REVARP		214	/* reverse addr resolution protocol */

#define	ARCTYPE_ATALK		221	/* Appletalk */
#define	ARCTYPE_BANIAN		247	/* Banyan Vines */
#define	ARCTYPE_IPX		250	/* Novell IPX */

#define ARCTYPE_INET6		0xc4	/* IPng */
#define ARCTYPE_DIAGNOSE	0x80	/* as per ANSI/ATA 878.1 */

/*
 * Structure of a 2.5MB/s Arcnet header on Linux.  Linux has
 * an extra "offset" field when given to interface code, and
 * never presents packets that look like exception frames.
 */
struct	arc_linux_header {
	u_int8_t  arc_shost;
	u_int8_t  arc_dhost;
	u_int16_t arc_offset;
	u_int8_t  arc_type;
	/*
	 * only present for newstyle encoding with LL fragmentation.
	 * Don't use sizeof(anything), use ARC_LINUX_HDR{,NEW}LEN
	 * instead.
	 */
	u_int8_t  arc_flag;
	u_int16_t arc_seqid;
};

#define	ARC_LINUX_HDRLEN	5
#define	ARC_LINUX_HDRNEWLEN	8
