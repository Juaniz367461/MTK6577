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

#ifdef __cplusplus
extern "C" {
#endif



/*
***********************************************************************************

NOTICE:
This document contains information that is proprietary to RADVISION LTD..
No part of this publication may be reproduced in any form whatsoever without
written prior approval by RADVISION LTD..

RADVISION LTD. reserves the right to revise this publication and make changes
without obligation to notify any person of such revisions or changes.

***********************************************************************************
*/

/*
  perInternal.h

  for PER internal usage.

  Ron S.
  14 May 1996

  */

#ifndef _PERINTERNAL_
#define _PERINTERNAL_

#include "per.h"
#include "persimpl.h"
#include "psyntreeStackApi.h"
#include "coderbitbuffer.h"

#include "rvlog.h"

/* Log sources used by the PER encoder/decoder module */
extern RvLogSource rvPerLogSource; /* PER log source */
extern RvLogSource rvPerErrLogSource; /* PERERR log source */



#define MAX_SPECIAL_NODES 20
#define MAX_INT_SIZE  4


#define encDecErrorsObjectWasNotFound   0x1 /* 001 */
#define encDecErrorsMessageIsInvalid    0x2 /* 010 */
#define encDecErrorsResourcesProblem    0x4 /* 100 */



/************************************************************************
 * THREAD_CoderLocalStorage
 * Thread specific information for the coder.
 * bufferSize   - Size of allocated buffer
 * buffer       - Encode/decode buffer to use for the given thread
 *                We've got a single buffer for each thread - this allows us to
 *                work will less memory resources, and dynamic size of buffers.
 ************************************************************************/
typedef struct
{
    RvUint32    bufferSize;
    RvUint8*    buffer;
} THREAD_CoderLocalStorage;



typedef struct
{
    HBB           hBB;   /* encoded buffer */

#if (RV_ASN1_CODER_USE_H450 == RV_YES)
    int           arrayOfSpecialNodes[MAX_SPECIAL_NODES]; /* list for "special" fields */
    unsigned int  currentPositionInArrayOfSpecialNodes;
#endif /* (RV_ASN1_CODER_USE_H450 == RV_YES) */

    /* --- Encoding parameters --- */

    /* --- decoding parameters --- */
    unsigned int  encodingDecodingErrorBitMask;
    RvUint32      decodingPosition; /* currently decoding... */
    /* last decoded node */
    int           synParent;
    int           valParent;
    RvInt32       fieldId; /* reference from last parent. Debug only */

    /* We open some of the emanagStruct fields here for easier and faster access */
    HPST          hSyn; /* syntax tree */
    HPVT          hVal; /* value tree */
    RvBool        isTolerant; /* RV_TRUE if we're allowing non-valid encodings */

    THREAD_CoderLocalStorage* buf; /* Buffer for encode/decode */
} perStruct;



int perEncNode(IN  HPER hPer,
           IN  int synParent,
           IN  int valParent,
           IN  RvInt32 fieldId,
           IN  RvBool wasTypeResolvedInRunTime);

int perDecNode(
    IN  HPER         hPer,
    IN  pstChildExt  *synInfo,
    IN  int         valParent);

int
perEncodeOpenTypeBegin(
	IN  HPER hPer,
    IN  RvInt32 fieldId,
    OUT RvInt32 *offset);  /* beginning of open type encoding */

int
perEncodeOpenTypeEnd(
	IN  HPER hPer,
    IN  RvInt32 offset,  /* beginning of open type encoding */
    IN  RvInt32 fieldId);


#endif
#ifdef __cplusplus
}
#endif



