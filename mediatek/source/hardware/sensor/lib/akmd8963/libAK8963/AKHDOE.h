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

/******************************************************************************
 *
 *  $Id: AKHDOE.h 361 2011-07-27 09:27:24Z yamada.rj $
 *
 * -- Copyright Notice --
 *
 * Copyright (c) 2004 Asahi Kasei Microdevices Corporation, Japan
 * All Rights Reserved.
 *
 * This software program is proprietary program of Asahi Kasei Microdevices
 * Corporation("AKM") licensed to authorized Licensee under Software License
 * Agreement (SLA) executed between the Licensee and AKM.
 *
 * Use of the software by unauthorized third party, or use of the software
 * beyond the scope of the SLA is strictly prohibited.
 *
 * -- End Asahi Kasei Microdevices Copyright Notice --
 *
 ******************************************************************************/
#ifndef AKSC_INC_AKHDOE_H
#define AKSC_INC_AKHDOE_H

#include "AKMDevice.h"

//========================= Constant definition =========================//
#define	AKSC_NUM_HCOND			2		// The number of HCOND. e.g. HCOND[0];for small magnetism size, HCOND[1];for normal magnetism size
#define AKSC_MAX_HDOE_LEVEL		3		// The maximum DOE level
#define AKSC_HBUF_SIZE			32		// Buffer size for DOE
#define AKSC_HOBUF_SIZE			16		// Offset buffer size for DOE

//========================= Macro definition =========================//
#define AKSC_InitHDOEProcPrmsS3(hdoev, idxhcond, ho, hdst) zzAKSC_InitHDOEProcPrmsS3((hdoev), 0, 0, (idxhcond), (ho), (hdst))
#define AKSC_HDOEProcessS3(licenser, licensee, key, hdoev, hdata, hn, ho, hdst) zzAKSC_HDOEProcessS3((licenser), (licensee), (key), (hdoev), (hdata), (hn), 0, 1, (ho), (hdst))

//========================= Type declaration  ===========================//
typedef enum _AKSC_HDFI{
	AKSC_HDFI_SMA		= 0,	// Low magnetic intensity
	AKSC_HDFI_NOR		= 1		// Normal magnetic intensity
} AKSC_HDFI;

typedef enum _AKSC_HDST{
	AKSC_HDST_UNSOLVED	= 0,	// Offset is not determined.
	AKSC_HDST_L0		= 1,	// Offset has been determined once or more with Level0 parameter
	AKSC_HDST_L1		= 2,	// Offset has been determined once or more with Level1 parameter
	AKSC_HDST_L2		= 3		// Offset has been determined once or more with Level2 parameter
} AKSC_HDST;

typedef struct _AKSC_HDOEVAR{					// Variables necessary for DOE calculation
	void**			HTH;						// [AKSC_NUM_HCOND][AKSC_MAX_HDOE_LEVEL]
	int16vec		hbuf[AKSC_HBUF_SIZE];		// Buffer for measurement values
	int16vec		hobuf[AKSC_HOBUF_SIZE];		// Buffer for offsets
	int16vec		hvobuf[AKSC_HOBUF_SIZE];	// Buffer for successful offsets
	int16			hdoeCnt;					// DOE counter
	int16			hdoeLv;						// HDOE level
	int16			hrdoe;						// Geomagnetic vector size
	int16			hthIdx;						// Index of HTH. This value can be from 0 to AKSC_NUM_HCOND-1

	void**			HTHHR;						// [AKSC_NUM_HCOND or 1][AKSC_MAX_HDOE_LEVEL or 1]
	int16vec		hobufHR[AKSC_HOBUF_SIZE];	// Buffer for offsets to check the size of geomagnetism
	int16			hrdoeHR;

	int16			reserved;					// Reserve
} AKSC_HDOEVAR;

//========================= Prototype of Function =======================//
AKLIB_C_API_START
void zzAKSC_InitHDOEProcPrmsS3(
			AKSC_HDOEVAR*	hdoev,		//(i/o)	: A set of variables
			void**			HTH,		//(i)	: Only 0 is acceptable
			void**			HTHHR,		//(i)	: Only 0 is acceptable
	const	int16			idxhcond,	//(i)	: Initial index of criteria
	const	int16vec*		ho,			//(i)	: Initial offset
	const	AKSC_HDST		hdst		//(i)	: Initial DOE level
);

int16 zzAKSC_HDOEProcessS3(				//(o)   : Estimation success(1 ~ hn), failure(0)
	const	uint8			licenser[],	//(i)	: Licenser
	const	uint8			licensee[],	//(i)	: Licensee
	const	int16			key[],		//(i)	: Key
			AKSC_HDOEVAR*	hdoev,		//(i/o)	: A set of variables
	const	int16vec		hdata[],	//(i)	: Vectors of data
	const	int16			hn,			//(i)	: The number of data (the value must be less than the size of hdata array)
	const	int16			linkHthHRIdxToHth,//(i)	: (1)link hthHR[idx][] index to hth[idx][]. (0) use only hthHR[0][].
	const	int16			linkHthHRLvlToHth,//(i)	: (1)link hthHR[][lvl] level to hth[][lvl]. (0) use only hthHR[][0].
			int16vec*		ho,			//(i/o)	: Offset
			AKSC_HDST*		hdst		//(o)	: HDOE status
);

void AKSC_SetHDOELevel(
			AKSC_HDOEVAR*	hdoev,		//(i/o)	: A set of variables
	const	int16vec*		ho,			//(i)	: Initial offset
	const	AKSC_HDST		hdst,		//(i)	: DOE level
	const	int16			initBuffer	//(i)	: If this flag is 0, don't clear buffer
);
AKLIB_C_API_END

#endif

