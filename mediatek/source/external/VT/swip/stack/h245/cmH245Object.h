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
        Copyright (c) 2002 RADVISION Ltd.
************************************************************************
NOTICE:
This document contains information that is confidential and proprietary
to RADVISION Ltd.. No part of this document may be reproduced in any
form whatsoever without written prior approval by RADVISION Ltd..

RADVISION Ltd. reserve the right to revise this publication and make
changes without obligation to notify any person of such revisions or
changes.
***********************************************************************/

#ifndef _RV_CM_H245_OBJECT_H
#define _RV_CM_H245_OBJECT_H

#include "rvlog.h"
#include "rvrandomgenerator.h"
#include "rvtimer.h"
#include "cmH245.h"
#include "ema.h"


#ifdef __cplusplus
extern "C" {
#endif


/* H.245 object declarations */
typedef struct
{
    /* CallBacks */
    SCMSESSIONEVENT         cmMySessionEvent;
    SCMCHANEVENT            cmMyChannelEvent;

    /* Log */
    RvLogMgr*               logMgr; /* Log manager used for this H.245 object */
    RvLogSource             log; /* H245 log source */

    /* General H.245 Callbacks */
    H245EvHandlers          evHandlers; /* Callbacks between H.245 and the above application */

    /* Control Offset */
    int                     h225CtrlOffset; /* Offset of the control inside an H.323 call object */
    int                     h223CtrlOffset; /* Offset of the control inside a 3G-324M call object */

    /* Object Identifier for H.245 messages */
    char                    h245protocolID[12]; /* Protocol Identifier of outgoing TCS messages */
    int                     h245protocolIDLen;  /* Protocol Identifier length */


    /* -- parameters that are used in H.245 and outside H.245 -- */

    RvBool                  bIsPropertyModeNotUsed;  /* Mode used for the property database of
                                                        the stack */
    HEMA                    hChannels; /* H.245 channel objects */

    /* PVT/PST parameters */
    HPVT                    hVal; /* PVT handle to use for all messages */
    HPST                    hSynProtH245; /* MultimediaSystemControlMessage ASN.1 syntax */
    HPST                    synOLC; /* OpenLogicalChannel ASN.1 syntax */
#if (RV_H245_LEAN_H223 == RV_NO)
    HPST                    hAddrSynH245; /* TransportAddress ASN.1 syntax */
    HPST                    h245TransCap;
    HPST                    h245RedEnc;
#endif
    HPST                    h245DataType; /* DataType ASN.1 syntax */
    RvPvtNodeId             h245Conf; /* H.245 configuration node id */
    int                     encodeBufferSize;

#if (RV_H245_SUPPORT_H225_PARAMS == RV_YES)
    /* MIB related parameters */
    MibEventT               mibEvent;       /* MIB Callbacks to call */
    HMibHandleT             mibHandle;      /* Handle of MIB to use */

    /* Fast Start */
    int*                     fastStartBuff;  /* Array of proposals for all calls in the stack.
                                              The size of this array is:
                                              maxCalls*(maxFsProposed+maxFsAccepted) */
    RvUint8*                 fastStartBuff2; /* Array of indexes to match acked channels to proposed ones */
    int                      maxFsProposed;  /* Maximum number of proposed channels in a single call */
    int                      maxFsAccepted;  /* Maximum number of accepted channels in a single call */
    RvBool                   bSupportEFC;    /* true if EFC is supported */
    H245SupportH225CallEvent h225CallEvent;
    RvLogSource              logFastStart;
    
    RvUint8                 dynamicPayloadNumber; /* 0-31 dynamic payload number */
#endif  /* (RV_H245_SUPPORT_H225_PARAMS == 1) */

    RvRandomGenerator       randomGenerator; /* Random numbers generator */
    RvLock                  lock; /* Lock for random generator and dynamic payload number */

    HAPP                    pAppObject;
}H245Object;




#ifdef __cplusplus
}
#endif

#endif  /* _RV_CM_H255_PARAMS_H */
