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
 *      Name:				xpt-auth.h
 *
 *      Derived From:		Original
 *
 *      Created On:			May 2004
 *
 *      Version:			$Id: //depot/main/base/syncml/xpt/bindings/common/xpt-auth.h#3 $
 *
 *      Coding Standards:	3.0
 *
 *      Purpose:            SyncML core code
 *
 *      (c) Copyright Insignia Solutions plc, 2004
 *
]*/

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
#ifndef XPTAUTH_H
#define XPTAUTH_H

#include <syncml/xpt/bindings/common/tcp/xpttypes.h>

#define MAX_DIGEST_SIZE 80
#define MAX_REALM_SIZE	80
#define MAX_NONCE_SIZE	65
#define MAX_USER_SIZE	32
#define MAX_PWD_SIZE	32
#define MAX_CNONCE_SIZE 10
#define MAX_NC_SIZE	10
#define MAX_QOP_SIZE	10
#define MAX_OPAQUE_SIZE 32
#define MAX_DOMAIN_SIZE 256

/********************************************************/
/* Authentication info, Basic and Digest authentication */
/********************************************************/
/** required authentication */
typedef enum
	 {
	 AUTH_NONE = 0,
	 AUTH_BASIC = 1,
	 AUTH_DIGEST = 2
	 } HttpAuthenticationType_t;
 
/** authentication destination */
typedef enum
	 {
	 DEST_NONE = 0,
	 DEST_SERVER = 1,
	 DEST_PROXY  = 2
	 } HttpAuthenticationDest_t;

typedef struct
   {
   int cbSize;
   CString_t szUser;
   CString_t szPassword;
   CString_t szRealm;
   CString_t szCNonce;
   CString_t szNC;
   } HttpAuthenticationUserData_t, *HttpAuthenticationUserDataPtr_t;


typedef struct
   {
   int cbSize;
   CString_t szNonce;
   CString_t szNC;
   CString_t szQop;
   } HttpAuthenticationInfo_t, *HttpAuthenticationInfoPtr_t;

typedef struct
   {
   int cbSize;
   CString_t szRealm;
   CString_t szNonce;
   CString_t szOpaque;
   CString_t szDomain;
   CString_t szQop;
   Bool_t    bStale;
   } HttpAuthenticationData_t, *HttpAuthenticationDataPtr_t;

typedef void * HttpAuthenticationPtr_t;

HttpAuthenticationPtr_t authInit (void);
void authTerminate (HttpAuthenticationPtr_t auth);

Bool_t authSetDigest (HttpAuthenticationPtr_t auth,
		      HttpAuthenticationDest_t dest,
		      CString_t szDigest);
Bool_t authCalcDigest (HttpAuthenticationPtr_t auth,
		       HttpAuthenticationDest_t dest,
		       CString_t szURI,
		       CString_t szMode);
CString_t authGetDigest (HttpAuthenticationPtr_t auth,
			 HttpAuthenticationDest_t dest);

Bool_t authSetType (HttpAuthenticationPtr_t auth,
		    HttpAuthenticationDest_t dest,
		    HttpAuthenticationType_t type);
HttpAuthenticationType_t authGetType (HttpAuthenticationPtr_t auth,
				      HttpAuthenticationDest_t dest);


Bool_t authSetUserData (HttpAuthenticationPtr_t auth,
			HttpAuthenticationDest_t dest,
			HttpAuthenticationUserDataPtr_t pUserData);

Bool_t authGetUserData (HttpAuthenticationPtr_t auth,
			HttpAuthenticationDest_t dest,
			HttpAuthenticationUserDataPtr_t pUserData);

Bool_t authSetAuthenticationInfo (HttpAuthenticationPtr_t auth,
				  HttpAuthenticationDest_t dest,
				  HttpAuthenticationInfoPtr_t pAuthInfo);


Bool_t authSetAuthenticationData (HttpAuthenticationPtr_t auth,
				  HttpAuthenticationDest_t dest,
				  HttpAuthenticationDataPtr_t pAuthData);

Bool_t authGetAuthenticationData (HttpAuthenticationPtr_t auth,
				  HttpAuthenticationDest_t dest,
				  HttpAuthenticationDataPtr_t pAuthData);

#endif

/* eof */
