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

/*[
 *      Project:    	    OMC
 *
 *      Name:				libstr.h
 *
 *      Derived From:		Original
 *
 *      Created On:			May 2004
 *
 *      Version:			$Id: //depot/main/base/syncml/sml/lib/libstr.h#3 $
 *
 *      Coding Standards:	3.0
 *
 *      Purpose:            SyncML core code
 *
 *      (c) Copyright Insignia Solutions plc, 2004
 *
]*/

/**
 * @file
 * Library for String Functions
 *
 * @target_system   ALL
 * @target_os       ALL
 * @description Header for the implementation of common string-handling functions
 */



/*
 * Copyright Notice
 * Copyright (c) Ericsson, IBM, Lotus, Matsushita Communication
 * Industrial Co., Ltd., Motorola, Nokia, Openwave Systems, Inc.,
 * Palm, Inc., Psion, Starfish Software, Symbian, Ltd. (2001).
 * All Rights Reserved.
 * Implementation of all or part of any Specification may require
 * licenses under third party intellectual property rights,
 * including without limitation, patent rights (such a third party
 * may or may not be a Supporter). The Sponsors of the Specification
 * are not responsible and shall not be held responsible in any
 * manner for identifying or failing to identify any or all such
 * third party intellectual property rights.
 *
 * THIS DOCUMENT AND THE INFORMATION CONTAINED HEREIN ARE PROVIDED
 * ON AN "AS IS" BASIS WITHOUT WARRANTY OF ANY KIND AND ERICSSON, IBM,
 * LOTUS, MATSUSHITA COMMUNICATION INDUSTRIAL CO. LTD, MOTOROLA,
 * NOKIA, PALM INC., PSION, STARFISH SOFTWARE AND ALL OTHER SYNCML
 * SPONSORS DISCLAIM ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION
 * HEREIN WILL NOT INFRINGE ANY RIGHTS OR ANY IMPLIED WARRANTIES OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT
 * SHALL ERICSSON, IBM, LOTUS, MATSUSHITA COMMUNICATION INDUSTRIAL CO.,
 * LTD, MOTOROLA, NOKIA, PALM INC., PSION, STARFISH SOFTWARE OR ANY
 * OTHER SYNCML SPONSOR BE LIABLE TO ANY PARTY FOR ANY LOSS OF
 * PROFITS, LOSS OF BUSINESS, LOSS OF USE OF DATA, INTERRUPTION OF
 * BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR EXEMPLARY, INCIDENTAL,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN CONNECTION WITH
 * THIS DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH LOSS OR DAMAGE.
 *
 * The above notice and this paragraph must be included on all copies
 * of this document that are made.
 *
 */


#ifndef __LIB_STR_H
#define __LIB_STR_H

/*************************************************************************
 *  Definitions
 *************************************************************************/

#include <syncml/sml/smldef.h>
#include <syncml/sml/lib/libmem.h>
#ifdef __ANSI_C__
#include <string.h>
#endif
#ifdef __PALM_OS__
#include <StringMgr.h>
#endif


/*************************************************************************
 *  External Functions for all Toolkit versions
 *************************************************************************/


#ifdef __PALM_OS__  /* we use #define to reduce heap usage */
  String_t   smlLibStrdup (const char* constStringP);
  #define		smlLibStrcpy(pTarget,pSource)		 	(char*)StrCopy((char*)pTarget,(char*)pSource)
  #define		smlLibStrncpy(pTarget,pSource,count)	  (char*)StrNCopy((char*)pTarget,(char*)pSource,count)
  #define		smlLibStrcat(pTarget,pSource)	  (char*)StrCat((char*)pTarget,(char*)pSource)
  #define		smlLibStrcmp(pTarget,pSource)	  StrCompare((char*)pTarget,(char*)pSource)
  #define		smlLibStrncmp(pTarget,pSource,count)	 StrNCompare((char*)pTarget,(char*)pSource,count)
  #define		smlLibStrchr(pString,character)		(char*)StrChr((String_t)pString,character)
  #define		smlLibStrlen(pString)	  StrLen((char*)pString)
#else               /* we use functions, to make the library exportable */
  SML_API_DEF String_t 	smlLibStrdup (const char *constStringP);
  SML_API_DEF String_t	smlLibStrcpy(const char *pTarget, const char *pSource);
  SML_API_DEF String_t	smlLibStrncpy(const char *pTarget, const char *pSource, int count);
  SML_API_DEF String_t	smlLibStrcat(const char *pTarget, const char *pSource);
  SML_API_DEF int		smlLibStrcmp(const char *pTarget, const char *pSource);
  SML_API_DEF int		smlLibStrncmp(const char *pTarget, const char *pSource, int count);
  SML_API_DEF String_t	smlLibStrchr(const char *pString, char character);
  SML_API_DEF int		smlLibStrlen(const char *pString);
#endif




/*************************************************************************
 *  Additional External Functions for Full Sized Toolkit Only
 *************************************************************************/

#ifndef __SML_LITE__  /* these API calls are NOT included in the Toolkit lite version */
#ifdef __PALM_OS__  /* we use define to reduce heap usage */
  #define		smlLibStrncat(pTarget,pSource,count)	 (char*)StrNCat((char*)pTarget,(char*)pSource,count)
  #define		smlLibStrstr(pString,pSubstring)	 (char*)StrStr((char*)pString,(char*)pSubstring)
#else               /* we use functions, to make the library exportable */
  SML_API_DEF String_t	smlLibStrncat(const char *pTarget, const char *pSource, int count);
  SML_API_DEF String_t	smlLibStrstr(const char *pString, const char *pSubString);
#endif
#endif


#endif
