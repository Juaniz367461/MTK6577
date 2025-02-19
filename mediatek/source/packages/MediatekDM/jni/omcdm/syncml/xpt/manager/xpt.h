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
 *      Name:				xpt.h
 *
 *      Derived From:		Original
 *
 *      Created On:			May 2004
 *
 *      Version:			$Id: //depot/main/base/syncml/xpt/manager/xpt.h#4 $
 *
 *      Coding Standards:	3.0
 *
 *      Purpose:            SyncML core code
 *
 *      (c) Copyright Insignia Solutions plc, 2004 - 2006
 *
]*/

/**
 * @file
 * SyncML Communication Protocol Header file
 *
 * @target_system   all
 * @target_os       all
 * @description This include file contains the API specifications
 * for the SyncML Communication Services
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


#ifndef XPT_H
#define XPT_H

#include <syncml/xpt/manager/xptdef.h>


#include <syncml/sml/smldef.h>
#include <syncml/sml/smlerr.h>


/** Generic XPT error */
#define SML_ERR_A_XPT_ERROR                 OMC_ERR_COMMS

/** Server authentication failed */
#define SML_ERR_A_XPT_SERVER_AUTH           (OMC_ERR_TYPE_COMMS + 0x01)

/** Server rejected request */
#define SML_ERR_A_XPT_ACCESS_DENIED         (OMC_ERR_TYPE_COMMS + 0x02)

/** HTTP error */
#define SML_ERR_A_XPT_HTTP_ERROR            (OMC_ERR_TYPE_COMMS + 0x03)

/** communication error */
#define SML_ERR_A_XPT_COMMUNICATION         (OMC_ERR_TYPE_COMMS + 0x04)


/** Protocol not supported */
#define SML_ERR_A_XPT_INVALID_PROTOCOL      (OMC_ERR_TYPE_COMMS + 0x10)

/** Parameter error */
#define SML_ERR_A_XPT_INVALID_PARM          (OMC_ERR_TYPE_COMMS + 0x11)

/** Invalid Communication/Service ID */
#define SML_ERR_A_XPT_INVALID_ID            (OMC_ERR_TYPE_COMMS + 0x12)

/** Memory allocation error */
#define SML_ERR_A_XPT_MEMORY                (OMC_ERR_TYPE_COMMS + 0x13)

/** Too many open communication instances */
#define SML_ERR_A_XPT_IN_USE                (OMC_ERR_TYPE_COMMS + 0x14)

/** Invalid communication state */
#define SML_ERR_A_XPT_INVALID_STATE         (OMC_ERR_TYPE_COMMS + 0x15)

/** No transports are available */
#define SML_ERR_A_XPT_NO_TRANSPORTS         (OMC_ERR_TYPE_COMMS + 0x16)


/* Communication info, used by XptOpenCommunication */
#define AUTH_BUFFER_SIZE  256       /**< maximum length of authorization data
                                     * buffer (User ID plus password) */
#define XPT_DOC_NAME_SIZE 255       /**< maximum length of a document name */
#define XPT_DOC_TYPE_SIZE 63        /**< maximum length of document MIME type */
#define XPT_CLIENT_ADDRESS_SIZE 80  /**< maximum length of originator */
#define XPT_DEFAULT_MIME_TYPE "application/vnd.syncml+xml"

#ifdef __cplusplus
extern "C" {
#endif



typedef int XptProtocolId_t;     /**< A protocol identifier */

/** Error information structure, returned by XptGetLastError */
typedef struct {                 // Communication status
   XptProtocolId_t protocolId;   /**< selected communication protocol */
   Ret_t status;                 /**< General status from failing function */
   const char *shortProtocolName;/**< Short name of the selected protocol */
   const char *failingFunction;  /**< The name of the failing function */
   long protocolErrorCode;       /**< Protocol-specific error code */
   const char *errorMessage;     /**< Error message */
} XptErrorInformation_t, *XptErrorInformationPtr_t;


typedef struct {
   const char* algorithm; /**< opt. is missing MD5 is assumed */
   const char* username;  /**< identity of the sender of the message */
   const char* mac;       /**< digest values */
} XptHmacInfo_t, *XptHmacInfoPtr_t;

/** Communication info */
typedef struct {
   short cbSize;                       /**< size of this data structure */
   size_t cbLength;                    /**< Document Length. Use (size_t) -1 to
                                        * indicate that the length is unknown.
                                        */
   char mimeType[XPT_DOC_TYPE_SIZE+1]; /**< document MIME type */
   char docName[XPT_DOC_NAME_SIZE+1];  /**< document name */
   XptHmacInfoPtr_t hmacInfo;          /**< opt. Information for the HMAC
                                        * Transport Header field */
   // %%% luz:2002-05-23: Auth support added
   //     Note: auth is a HttpAuthenticationPtr_t, which IS a void *
   //     and because including xpt-auth.h causes problems we just define
   //     void * here
   void * auth;       /**< opt. auth structure (must be initialized with
                       * authInit() and user info set with authSetUserData()) */
} XptCommunicationInfo_t, *XptCommunicationInfoPtr_t;

/** Protocol Service ID */
typedef void *XptServiceID_t, **XptServiceIDPtr_t;

/** Communication Service ID */
typedef void *XptCommunicationID_t, **XptCommunicationIDPtr_t;

/** Incomplete declaration to satisfy ptr ref */
struct xptInternalTransportInfo;

// Flags for use in the XptProtocolInfo.flags field and on the flags parm
// of the xptSelectProtocol() function call.
#define XPT_CLIENT   0x1      /**< Protocol supports client mode */
#define XPT_SERVER   0x2      /**< Protocol supports server mode */

// Flags for use in the xptOpenCommunication() function call.
#define XPT_REQUEST_SENDER   0x1 /**< Caller will act as a client
                                  * in the exchange */
#define XPT_REQUEST_RECEIVER 0x2 /**< Caller will act as a server
                                  * in the exchange */

struct XptProtocolInfo {
   /** Short name of protocol. E.g., "HTTP" or "OBEX/IR" */
   const char *shortName;

   /** Summary of protocol, suitable for display E.g., "OBEX over infrared" */
   const char *description;

   /** Flags. Any of XPT_CLIENT and/or XPT_SERVER */
   unsigned int flags;

   /** Numeric identifier for use in xptSelectProtocol() */
   XptProtocolId_t id;

   /** Internal protocol information */
   struct xptInternalTransportInfo *internal;
};



// %%% luz 2003-06-26:
/**
 * Clean up after using xpt
 */
void xptCleanUp(void);


/**
 * Obtain information on given protocol
 *
 * @post The application can call this function to look up information
 *       on a specific protocol.
 * @param protocolName (IN)
 *        protocol name to trasport i.e. HTTP
 * @param pProtocolInfo (OUT)
 *        pointer to an XptProtocolInfo protocol information structure.
 *        The memory is owned by xpt the service, and should therefore not be
 *        released. Also, do not change the contents of the returned structure.
 * @return Return code. Refer to the return code list above for details.
 * @note See the NOTES section of the xptGetProtocols() function
 *       for a description of the contents of the returned structure.
 */
XPTDECLEXP1 Ret_t XPTAPI xptGetProtocol(const char *protocolName,
                    const struct XptProtocolInfo **pProtocolInfo) XPT_SECTION;

/**
 * Obtain a list of all available protocols
 *
 * @post The application can call this function to get the available protocols.
 * @param pProtocolList (OUT)
 *        pointer to an array of XptProtocolInfo protocol information
 *        structures. The memory is owned by xpt the service, and should
 *        therefore not be released. Also, do not change the contents of
 *        the returned array.
 * @param pProtocolCount (OUT)
 *        the number of entries in the returned array.
 * @return Return code. Refer to the return code list above for details.
 *
 * @note Each element of the returned array describes one available transport
 *       protocol. Each XptProtocolInfo struct contains the following useful
 *       fields:
 *       - shortName - The short name of the protocol. Each available protocol
 *                   registers itself with a unique short name such as
 *                   "HTTP", "OBEX/IR", "OBEX/IP", "WSP", etc..
 *       - description - A one-sentence description of the protocol,
 *                       suitable for displaying in a list of available
 *                       protocols from which the user may choose.
 *       - flags - Flags that provide additional information about the
 *                 protocol. At least one of XPT_CLIENT or XPT_SERVER
 *                 will be set, but not necessarily both. These are:
 *         - XPT_CLIENT  - Indicates the protocol can support
 *                         client-type connections (outgoing requests).
 *         - XPT_SERVER  - Indicates the protocol can support
 *                         server-type connections (incoming requests).
 *       - id - A numeric id assigned by the xpt service to the protocol.
 *              This id is the official identification of the protocol and
 *              is therefore passed to functions such as xptSelectProtocol()
 *              when selecting a protocol.
 */
XPTDECLEXP1 Ret_t XPTAPI xptGetProtocols(
                              const struct XptProtocolInfo **pProtocolList,
                              int *pProtocolCount) XPT_SECTION;


/**
 * Select the communication protocol to start the communication
 *
 * @post The desired protocol is selected. Multiple connections can be opened
 *       using this protocol service ID.
 * @param protocolId (IN)
 *        The id of the protocol service to use
 * @param metaInformation (IN)
 *        Protocol settings. The protocol settings depend on the
 *        protocol that has been implemented. An example setting is the
 *        string: "TIMEOUT=20 HOST=192.168.5.1". A null pointer may be
 *        specified to indicate that no meta information is being provided.
 * @param flags (IN)
 *        One of the flags XPT_CLIENT or XPT_SERVER, indicating whether
 *        this service will act as a client or a server. A client makes
 *        a request and receives a response. A server receives a request
 *        and replies with a response.
 * @param pId (OUT)
 *        Reference to a protocol service instance
 * @return Return code. Refer to the return code list above for details.
 */
XPTDECLEXP1 Ret_t XPTAPI xptSelectProtocol(XptProtocolId_t protocolId,
                                           const char *metaInformation,
                                           unsigned int flags,
                                           XptServiceIDPtr_t pId) XPT_SECTION;


/**
 * Deselect the communication protocol to stop the communication
 *
 * @pre The specified protocol service ID must have been created by a call
 *      of the service xptSelectProtocol().
 * @post The desired protocol is deselected. The resources that are allocated
 *       by the function are freed. The service ID and all connections that
 *       are assigned to the service ID are invalidated.
 * @param id (IN)
 *        Protocol service ID
 * @return Return code. Refer to the return code list above for details.
 */
XPTDECLEXP1 Ret_t XPTAPI xptDeselectProtocol(XptServiceID_t id) XPT_SECTION;



/**
 * Establishes a communication using the protocol service with the given id.
 *
 * @pre A protocol service id has been obtained by calling xptSelectProtocol().
 * @post The communication role can be specified as either REQUEST_SENDER or
 *       REQUEST_RECEIVER. A request sender is expected subsequently to send
 *       a request, then receive a response. A request receiver is expected
 *       to receive a request, then send a response. See the prologue
 *       comments for more information on the expected sequence of calls.
 * @param id (IN)
 *        Protocol Service ID
 * @param role (IN)
 *        Communication mode. One of:
 *        - XPT_REQUEST_SENDER   indicates that the caller will send a
 *                               document, and then receive one.
 *        - XPT_REQUEST_RECEIVER indicates that the caller will receive
 *                               a document, and then send one.
 * @param pConn (OUT)
 *        pointer to a connection instance handle
 * @return Return code. Refer to the return code list above for details.
 */
XPTDECLEXP1 Ret_t XPTAPI xptOpenCommunication(XptServiceID_t id,
                    int role, XptCommunicationIDPtr_t pConn) XPT_SECTION;

/**
 * Closes a previously opened communication
 *
 * @pre An open connection exists, the transmission of the data has been
 *      either completed, or it is desired to cancel the (incomplete)
 *      transmission
 * @post The connection instance is closed. It cannot be used any more.
 * @param conn (IN)
 *        connection instance handle
 * @return Return code. Refer to the return code list above for details.
 */
XPTDECLEXP1 Ret_t XPTAPI xptCloseCommunication(
                    XptCommunicationID_t conn) XPT_SECTION;


/**
 * Prepares to begin a new exchange of documents.
 *
 * @pre An active connection exists.
 * @post If the connection was opened with the XPT_REQUEST_SENDER flag,
 *       then this call allows the transport binding implementation to
 *       prepare for a sending a document and then receiving one. If the
 *       connection was opened with the XPT_REQUEST_RECEIVER flag, then
 *       this call allows the transport binding implementation to prepare
 *       for receiving a document and then sending one.
 * @param conn (IN)
 *        connection instance handle
 * @return Return code. Refer to the return code list above for details.
 */
XPTDECLEXP1 Ret_t XPTAPI xptBeginExchange(
                    XptCommunicationID_t conn) XPT_SECTION;


/**
 * Completes an exchange of documents.
 *
 * @pre An active connection exists
 * @post This call allows the transport binding implementation
 *       to clean up after a document exchange.
 * @param conn (IN)
 *        connection instance handle
 * @return Return code. Refer to the return code list above for details.
 */
XPTDECLEXP1 Ret_t XPTAPI xptEndExchange(XptCommunicationID_t conn) XPT_SECTION;


/**
 * Read data across a connection
 *
 * @pre The connection has been opened with a call of xptOpenCommunication
 * @post This call blocks until at least one byte of data is available,
 *       or until no more bytes will become available. It returns when one
 *       or more bytes are read, or when the end of the document is reached.
 *       It may return before bufferLen bytes are read. If the earlier call
 *       to xptGetDocumentInfo() returned a document length, the caller
 *       can either read just that many bytes or read until the end
 *       of the document is reached.
 * @param connection (IN)
 *        connection instance handle.
 * @param buffer (IN)
 *        pointer to a buffer to hold the data.
 * @param bufferLen (IN)
 *        size of the data buffer, in bytes.
 * @param dataLen (IN)
 *        pointer to a field that on return will contain the
 *        length of the received data.
 * @param dataLen (OUT)
 *        set to the length of the received data. If the length is
 *        zero, the end of the document has been reached.
 * @return Return code. Refer to the return code list above for details.
 */
XPTDECLEXP1 Ret_t XPTAPI xptReceiveData(XptCommunicationID_t connection,
                                        void *buffer, size_t bufferLen,
                                        size_t *dataLen) XPT_SECTION;


/**
 * Send data across a connection
 *
 * @pre The connection has been opened with a call of xptOpenCommunication
 * @post This call blocks until at least one byte of data is sent.
 *       It returns when one or more bytes are sent, but it may return
 *       before bufferLen bytes are sent. If the caller previously provided
 *       a document length in the call to xptSetDocumentInfo(), then it must
 *       not attempt to write more than that many bytes. Doing so will result
 *       in an error. After calling xptSendData() one or more times to send
 *       the document body, the application must call xptSendComplete()
 *       to indicate that sending is finished.
 * @param connection (IN)
 *        connection instance handle.
 * @param buffer (IN)
 *        pointer to the data to be sent.
 * @param bufferLen (IN)
 *        size of the data block to be transmitted, in bytes.
 * @param bytesSent (IN/OUT)
 *        set to the actual number of bytes sent
 * @return Return code. Refer to the return code list above for details.
 */
XPTDECLEXP1 Ret_t XPTAPI xptSendData(XptCommunicationID_t connection,
                                     const void *buffer, size_t bufferLen,
                                     size_t *bytesSent) XPT_SECTION;


/**
 * Indicate that sending of a document is complete.
 *
 * @pre The document body was sent using one or more calls to xptSendData().
 * @post This call is used to indicate that the complete document body has been
 *       written. If the application previously supplied a document length in
 *       its call to xptSetDocumentInfo(), then the number of bytes written
 *       before calling xptSendComplete() must match that length.
 *       Otherwise an error will be returned from xptSendComplete().
 * @param connection (IN)
 *        connection instance handle.
 * @return Return code. Refer to the return code list above for details.
 */
XPTDECLEXP1 Ret_t XPTAPI xptSendComplete(
                    XptCommunicationID_t connection) XPT_SECTION;



/**
 * Provide document information for an outgoing document.
 *
 * @pre The connection has been opened with a call of xptOpenCommunication
 * @post The transport protocol uses the provided document information in a
 *       protocol-dependent way. Generally, it is used to set document name and
 *       length headers. The caller is expected to subsequently call
 *       xptSendData() to send the document body.\n
 *       All fields of the XptCommunicationInfo_t structure must be filled in.
 *       If the caller doesn't know the document length in advance, it is
 *       permitted to set the cbLength field to (size_t) -1 to indicate that
 *       the document length is unknown.
 * @param conn (IN)
 *        connection instance handle
 * @param pDoc (IN)
 *        Pointer to a structure that contains some information
 *        about the data to be transmitted.
 * @return Return code. Refer to the return code list above for details.
 */
XPTDECLEXP1 Ret_t XPTAPI xptSetDocumentInfo(XptCommunicationID_t conn,
                    const XptCommunicationInfo_t *pDoc) XPT_SECTION;

/**
 * Retrieve the document information associated with an incoming document.
 *
 * @pre The connection has been opened with a call of xptOpenCommunication
 * @post The transport protocol provides the document information for the
 *       incoming request. The caller is expected to subsequently call
 *       xptReceiveData() to receive the document body.
 *       All fields of the XptCommunicationInfo_t structure will be filled in.
 *       If the transport doesn't know the document length at this time, it
 *       will set the cbLength field to (size_t) -1 to indicate that the
 *       document length is unknown. In that case, the application must
 *       allocate storage dynamically and make xptReceiveData() calls until
 *       an end-of-document indication is returned.
 * @param conn (IN)
 *        connection instance handle
 * @param pDoc (IN)
 *        Pointer to a structure that is filled in with information
 *        about the data being transmitted.
 * @return Return code. Refer to the return code list above for details.
 */
XPTDECLEXP1 Ret_t XPTAPI xptGetDocumentInfo(XptCommunicationID_t conn,
                    XptCommunicationInfo_t *pDoc) XPT_SECTION;

/**
 * Get information about the last error
 *
 * @pre A call to some other xpt function failed. If the platform is a
 *      multithreaded platform, the earlier call must have taken place on the
 *      same thread making the call to xptGetLastError(). The application
 *      calls this function to get information about the cause of the error,
 *      including an error message that might be useful to display.\n
 *      If the platform is multithreaded, error information is maintained
 *      separately for each thread.
 * @post Information about the last communication error is returned in a
 *       structure. The caller should treat this structure as read-only
 *       storage, and not attempt the free it. The storage will be maintained
 *       by xptGetLastError().
 * @return A structure that describes the last error encountered.
 *         For multithreaded platforms, it describes the last error encountered
 *         on this thread.
 */
XPTDECLEXP1 const XptErrorInformation_t * XPTAPI xptGetLastError(
                    void) XPT_SECTION;

/* For EPOC OS */
#ifdef __EPOC_OS__

XPTDECLEXP1 int xptOpenTLS();

XPTDECLEXP1 void xptCloseTLS();

#endif

#ifdef __cplusplus
}
#endif

#endif
