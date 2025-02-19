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
 *      Name:				xltdec.h
 *
 *      Derived From:		Original
 *
 *      Created On:			May 2004
 *
 *      Version:			$Id: //depot/main/base/syncml/sml/xlt/xltdec.h#4 $
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
 * Interface for the XLT Decoder component.
 *
 * @target_system  all
 * @target_os      all
 * @description Interface for the WBXML and XML decoder component.
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

/*************************************************************************/
/* Definitions                                                           */
/*************************************************************************/
#ifndef _XLT_DEC_H
#define _XLT_DEC_H

#include <syncml/sml/smldef.h>
#include <syncml/sml/smldtd.h>
#include <syncml/sml/smlerr.h>

#include <syncml/sml/xlt/xltdeccom.h>
#include <syncml/sml/xlt/xltutilstack.h>

#ifdef _cplusplus
extern "C" {
#endif

/**
 * The XLT Decoder Interface consists of a single XltDecoder "object"
 * (struct) and an creation/initialization function. The XltDecoder
 * object contains all "public" methods and data structures. The first
 * parameter for any public method is the object of which the method is
 * called.
 */
typedef struct XltDecoder_s
{
    /**
     * Character set used in the document - this is the MIBEnum value assigned
     * by the IANA for the character encoding, e.g. "3" for US-ASCII.
     */
    Long_t charset;

    /**
     * Name of the character set, e.g. "US-ASCII" - valid
     * only when charset == 0.
     */
    String_t charsetStr;

    /**
     * Indicates whether the decoder has reached the end of the buffer during
     * the last call to xltDecNext.
     */
    Flag_t finished;

    Boolean_t final;

    /**
     * Pointer to the scanner status object used by this decoder. The scanner
     * will be created during the initialization of the decoder as either a XML
     * or WBXML scanner.
     */
    XltDecScannerPtr_t scanner;

    /**
     * The decoder uses an internal stack to check that for every start tag
     * there is a corresponding end tag.
     */
    XltUtilStackPtr_t tagstack;

} XltDecoder_t, *XltDecoderPtr_t;

/**
 * Initializes a new decoder object. This function allocates memory for the
 * decoder structure which has to be freed by a call to the decoder's
 * terminate method when the decoder is not needed anymore. As part of the
 * initialization the decoder begins decoding the SyncML document to find
 * the SyncHdr element.
 *
 * @pre ppDecoder is NULL
 *      ppBufPos
 * @post ppDecoder points to an initialized decoder status object
 * @param enc (IN)
 *        the document encoding (WBXML or XML)
 * @param pBufEnd (IN)
 *        pointer to the end of the buffer which contains the document
 * @param ppBufPos (IN/OUT)
 *        pointer to the current position within the buffer
 * @param ppDecoder (OUT)
 *        the decoder status object
 * @param ppSyncHdr (OUT)
 *        the SyncHdr element
 * @return
 *         - SML_ERR_OK, if the decoder could be created and the
 *           SmlSyncHdr was found
 *         - else error code
 */
Ret_t xltDecInit(const SmlEncoding_t enc,
        const MemPtr_t pBufEnd,
        MemPtr_t *ppBufPos,
        XltDecoderPtr_t *ppDecoder,
        SmlSyncHdrPtr_t *ppSyncHdr);

/**
 * Decodes the next protocol element of the given SyncML document. This
 * function creates the data structures detailed in the SMLDtd header file.
 * It is the responsibility of the SyncML client application to free the
 * allocated memory after it is done processing the data.
 * This function sets the decoder's finished flag if no protocol element was
 * found. In that case pPE is set to SML_PE_UNDEF and pContent is NULL.
 *
 * @pre pDecoder points to a decoder status object initialized by xltDecInit
 * @post pPE and pContent describe the next valid protocol
 *       element within the SyncML document OR
 *       the finished flag of the decoder status object is set
 * @param pBufEnd (IN)
 *        pointer to the end of the buffer
 * @param pDecoder (IN/OUT)
 *        the decoder status object
 * @param ppBufPos (IN/OUT)
 *        pointer to the current position within the
 *        buffer before and after the call to xltDecNext
 * @param pPE (OUT)
 *        the type of the protocol element (e.g. SML_PE_ADD)
 * @param pContent (OUT)
 *        the data structure for the p.e. cast
 *        (e.g. AddPtr_t) to a void pointer
 * @return
 *         - SML_ERR_OK, if a valid protocol element was found
 *           or if decoder reached the end of the buffer
 *         - else error code showing where the parsing failed
 */
Ret_t xltDecNext(XltDecoderPtr_t pDecoder,
        const MemPtr_t pBufEnd,
        MemPtr_t *ppBufPos,
        SmlProtoElement_t *pPE,
        VoidPtr_t *pContent);

/**
 * Frees the memory allocated by the decoder.
 *
 * @pre pDecoder points to a decoder status object initialized by xltDecInit
 * @post all memory allocated by the decoder status object is freed
 * @param pDecoder (IN)
 *        the decoder
 * @return
 *         - SML_ERR_OK, if the memory could be freed
 *         - else error code
 */
Ret_t xltDecTerminate(XltDecoderPtr_t pDecoder);


Ret_t xltDecReset(XltDecoderPtr_t pDecoder);

/* T.K. moved here from xltdec.c for use in sub-DTD parsing */
#define IS_START(tok) ((tok)->type == TOK_TAG_START)
#define IS_END(tok) ((tok)->type == TOK_TAG_END)
#define IS_EMPTY(tok) ((tok)->type == TOK_TAG_EMPTY)
#define IS_TAG(tok) (IS_START(tok) || IS_EMPTY(tok) || IS_END(tok))
#define IS_START_OR_EMPTY(tok) (IS_START(tok) || IS_EMPTY(tok))
#define IS_CONTENT(tok) ((tok)->type == TOK_CONT)
/**
 * just wrapper around the scanner's
 * nextTok methods that do some error checking.
 */
Ret_t nextToken(XltDecoderPtr_t pDecoder);
/**
 * just wrapper around the scanner's
 * pushTok methods that do some error checking.
 */
Ret_t discardToken(XltDecoderPtr_t pDecoder);
/* eof xltdec.c stuff */

/*
 * Allow flag to be expressed as, for example, <NoResp></NoResp>
 * rather than only <NoResp/>
 */
Ret_t xltSkipFlag(XltDecoderPtr_t pDecoder);

#ifdef _cplusplus
}
#endif

#endif
