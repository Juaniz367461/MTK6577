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

/* rvtls.h - Common Core TLS functionality support module */

/************************************************************************
                Copyright (c) 2001 RADVISION Inc.
************************************************************************
NOTICE:
This document contains information that is proprietary to RADVISION LTD.
No part of this publication may be reproduced in any form whatsoever
without written prior approval by RADVISION LTD..

RADVISION LTD. reserves the right to revise this publication and make
changes without obligation to notify any person of such revisions or
changes.
************************************************************************/


#ifndef _RV_TLS_H
#define _RV_TLS_H


#if (RV_TLS_TYPE != RV_TLS_NONE)
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <openssl/conf.h>
#include <openssl/x509.h>
#include <openssl/x509v3.h>
#endif
#include "rvccore.h"
#include "rvlog.h"
#include "rvsocket.h"
#include "rvselect.h"
#include "rvmutex.h"


#if defined(__cplusplus)
extern "C" {
#endif

/* Module specific error codes (-512..-1023). See rverror.h for more details */
#define RV_TLS_ERROR_GEN		-512		/* general TLS error */
#define	RV_TLS_ERROR_WILL_BLOCK	-513		/* Will block error, specifies that 
											operation should be complete later */
#define	RV_TLS_ERROR_INCOMPLETE	-514		/* Specifies that operation remained incomplete
											but not because it may block caller. Currently
											used only in shutdown procedure */
#define RV_TLS_ERROR_SHUTDOWN	-515		/* Specifies that shutdown request
											was received */

	

#if (RV_TLS_TYPE != RV_TLS_NONE)


/*
 * RvCompareCertificateCB - defines stack callback that will be applied
 * when peer certificate will be received. The callback return code
 * specifies if stack finds received certificate valid or not.
 */
typedef RvInt ( * RvCompareCertificateCB )(
	IN int  prevErrro, 
	IN void *certCtx);

/*
 * RvTLSMethod - defines supported SSL versions:
 *----------------------------------------------------------------------
 * SSL_V1	- SSL version 1
 * SSL_V2	- SSL version 2
 * TLS_V1	- SSL version 3
 */
typedef enum {
	RV_TLS_SSL_V2 = 1,
	RV_TLS_SSL_V3,
	RV_TLS_TLS_V1
} RvTLSMethod;



#define RV_TLS_DEFAULT_CERT_DEPTH	(-1) /* Used to specify that default
										  certificate chain lenght should
										  be used */


/* TLS module event type - bitmask of defined below TLS events */
typedef RvUint16 RvTLSEvents;
    
#define RV_TLS_NONE_EV      0x0

#define	RV_TLS_HANDSHAKE_EV 0x1 /*	TLS handshake event, notifies that 
									handshake procedure may be continue */
#define	RV_TLS_READ_EV		0x2	/* TLS read event, notifies that TLS read 
									may be applied */
#define	RV_TLS_WRITE_EV		0x4	/* TLS write event, notifies that TLS write 
									may be applied */
#define	RV_TLS_SHUTDOWN_EV	0x8	/* TLS shutdown event, notifies that TLS 
									shutdown procedure can be applied */

/*
 * RvPrivKeyType - defines supported private keys types:
 *----------------------------------------------------------------------
 * RSA	- RSA key type
 */
typedef enum {
	RV_TLS_RSA_KEY = 1
} RvPrivKeyType;

/*
 * RvTLSEngine - defines TLS module instance
 *----------------------------------------------------------------------
 * ctx				- OpenSSL context instance
 */
typedef struct {
	SSL_CTX						*ctx;
} RvTLSEngine;


/*
 *	RvTLSRenegState - renegotiation state of this session
 */
typedef enum {
    RV_TLS_RENEG_NONE,         /* No renegotiation process is in progress */
    RV_TLS_RENEG_NEEDED,       /* Session needs renegotiation */
    RV_TLS_RENEG_ACCEPTED,     /* Renegotiation is accepted from the peer */
    RV_TLS_RENEG_STARTED       /* Renegotiation is started on this session 
                                *  at least from RV wrappers point of view.
                                *  Actual renegotiation may be started later
                                */
} RvTLSRenegState;


/*
 * RvTLSSession - defines TLS/SSL session
 *----------------------------------------------------------------------
 * SSL					- OpenSSL session
 * BIO					- OpenSSL session BIO (based on TCP socket)
 * requiredForHandshake	- lists RvSelect events required to continue handshake
 * requiredForTLSRead	- lists RvSelect events required to continue read
 * requiredForTLSWrite	- lists RvSelect events required to continue write
 * tlsEvents			- list of TLS events that stack is interest to receive
 * mtx					- mutex to protect session structure instance
 */


typedef struct {
	SSL							*sslSession;
	BIO							*bio;
	RvSelectEvents				requiredForHandshake;
	RvSelectEvents				requiredForTLSRead;
	RvSelectEvents				requiredForTLSWrite;
	RvSelectEvents				requiredForTLSShutdown;
	RvTLSEvents					tlsEvents;
    RvTLSRenegState             renegState;
    RvTLSEvents                 stickyEvent;
} RvTLSSession;

#endif


/**********************************************************************************
 * RvTLSInit - initiates OpenSSL library
 *
 * This function should be called only once in specific process. It initiates 
 * OpenSSL library.
 *
 * INPUT : none
 * OUTPUT: none
 * RETURN: RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSInit(void);

/**********************************************************************************
 * RvTLSEnd - finalize OpenSSL library usage
 *
 * This function should be called only once in specific process.
 *
 * INPUT : none
 * OUTPUT: none
 * RETURN: RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSEnd(void);


/**********************************************************************************
 * RvTLSSourceConstruct - initiates TLS module log source
 *
 * This function should be called only once in specific process. It initiates 
 * OpenSSL library.
 *
 * INPUT : none
 * OUTPUT: none
 * RETURN: RvStatus	- Success or failure
 */
RvStatus RvTLSSourceConstruct(
	IN RvLogMgr* logMgr);


#if (RV_TLS_TYPE != RV_TLS_NONE)
/**********************************************************************************
 * RvTLSEngineConstruct - constructs TLS engine
 *
 * Constructs SSL context according to the input parameters and TLS module log source.
 * It fills output TLS engine structure with results.
 *
 * INPUT: 
 *	method		- SSL version. Can be SSL v1, SSL v2 or TLS v1
 *	logMgr		- log instance
 *	privKey		- private key
 *	privKeyType	- private key type
 *	privKeyLen	- private key length
 *  cert		- local side certificate
 *  certLen		- local side certificate length
 *	certDepth	- maximum allowed depth of certificates
 *  mtx			- mutex that protects this TLS 
 *				  engine memory
 * OUTPUT: tlsEngine	- TLS engine memory
 * RETURN: RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSEngineConstruct(
	IN  RvTLSMethod				method,
	IN  RvChar					*privKey,
	IN  RvPrivKeyType			privKeyType,
	IN  RvInt					privKeyLen,
	IN  RvChar					*cert,
	IN  RvInt					certLen,
	IN	RvInt					certDepth,
	IN  RvMutex					*mtx,
	IN  RvLogMgr				*logMgr,
	OUT RvTLSEngine				*tlsEngine);



/**********************************************************************************
 * RvTLSEngineDestruct - destructs TLS engine
 *
 * Destructs SSL context.
 *
 * INPUT: 
 *	tlsEngine	- engine to be destructed
 *  mtx			- mutex that protects this TLS 
 *				  engine memory
 *	logMgr		- log instance
 * OUTPUT: none
 * RETURN: RvStatus	- Success or failure
 */
RVCOREAPI RvStatus RvTLSEngineDestruct(
	IN RvTLSEngine	*tlsEngine,
	IN RvMutex		*mtx,
	IN RvLogMgr		*logMgr);



/**********************************************************************************
 * RvTLSSessionConstruct - Initiates TLS/SSL session
 *
 * Creates uninitialized SSL/TLS session that matches context defined by 
 * input TLS engine.
 *
 * INPUT: 
 *	tlsEngine	- TLS engine
 *  mtxEngine	- mutex that protects TLS 
 *				  engine memory
 *  mtxSession	- mutex that protects Session 
 *				  engine memory
 *  logMgr		- log instance
 * OUTPUT: tlsSession	- new TLS session
 * RETURN: RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSSessionConstruct(
	IN  RvTLSEngine		*tlsEngine,
	IN  RvMutex			*mtxEngine,
	IN  RvMutex			*mtxSession,
	IN  RvLogMgr		*logMgr,
	OUT RvTLSSession	*tlsSession);


/**********************************************************************************
 * RvTLSSessionDestruct - Destructs TLS/SSL session
 *
 * INPUT: 
 *	tlsSession	- new TLS session
 *	mtx			- Mutex that protects session 
 *				  structure
 *  logMgr		- log instance
 * OUTPUT: none
 * RETURN: RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSSessionDestruct(
	IN RvTLSSession		*tlsSession,
	IN RvMutex			*mtx,
	IN RvLogMgr			*logMgr);

/**********************************************************************************
 * RvTLSSessionClientHandshake - client side SSL/TLS handshake
 *
 * Initiates client connection to a remote TLS/SSL server.
 *
 * INPUT: 
 *	tlsSession	- created earlier TLS session
 *  certCB		- certificate callback. If is not NULL enables
 *				  certificate check by client. 
 *	tcpSock		- already connected to server TCP socket.
 *	mtx			- Mutex that protects session 
 *				  structure
 *  logMgr		- log instance
 * OUTPUT:
 * RETURN: RvStatus	- Success, WilllBlock or failure
 */
RVCOREAPI 
RvStatus RvTLSSessionClientHandshake(
	IN  RvTLSSession			*tlsSession,
	IN  RvCompareCertificateCB	certCB,
	IN  RvSocket				tcpSock,
	IN  RvMutex					*mtx,
	IN  RvLogMgr				*logMgr);

/**********************************************************************************
 * RvTLSSessionServerHandshake - server side SSL/TLS handshake
 *
 * Responces to client handshake request.
 *
 * INPUT: 
 *	tlsSession			- created earlier TLS session
 *	tcpSock				- already accepted server TCP socket.
 *  certCB				- certification callback. If not NULL 
 *						  server requests client certification
 *	mtx			- Mutex that protects session 
 *				  structure
 *  logMgr		- log instance
 * OUTPUT:
 * RETURN: RvStatus	- Success, WilllBlock or failure
 */
RVCOREAPI 
RvStatus RvTLSSessionServerHandshake(
	IN  RvTLSSession			*tlsSession,
	IN  RvCompareCertificateCB	certCB,
	IN  RvSocket				tcpSock,
	IN  RvMutex					*mtx,
	IN  RvLogMgr				*logMgr);


/**********************************************************************************
 * RvTLSSessionReceiveBuffer - retrieves SSL/TLS message
 *
 * Reads & decrypt SSL/TLS message. Writes retrieved message or it's part output 
 * buffer
 *
 * INPUT: 
 *	tlsSession		- Connected TLS session
 *	receiveBuf		- receive buffer
 *	mtx				- Mutex that protects session 
 *					  structure
 *  logMgr			- log instance
 *  receiveBufLen	- receive buffer maximum length
 * OUTPUT:
 *  receiveBufLen	- length of received data
 * RETURN: RvStatus	- Success, WilllBlock, Pending or failure
 */
RVCOREAPI 
RvStatus RvTLSSessionReceiveBuffer(
	IN    RvTLSSession	*tlsSession,
	IN    RvChar		*receiveBuf,
	IN    RvMutex		*mtx,
	IN    RvLogMgr		*logMgr,
	INOUT RvInt			*receiveBufLen);


/**********************************************************************************
 * RvTLSSessionSendBuffer - sends stack message via SSL session
 *
 * Enecrypts stack message and sends SSL/TLS message with encrypted stack data to
 * the SSL session peer.
 *
 * INPUT: 
 *	tlsSession		- Connected TLS session
 *	sendBuf			- buffer to send
 *  sendBufLen		- length of stack data in the sendBuf
 *	mtx				- Mutex that protects session 
 *					  structure
 *  logMgr			- log instance
 * OUTPUT: none
 * RETURN: 
 *	RvStatus	- Success, WilllBlock or failure
 */
RVCOREAPI 
RvStatus RvTLSSessionSendBuffer(
	IN  RvTLSSession	*tlsSession,
	IN  RvChar			*sendBuf,
	IN  RvInt			sendBufLen,
	IN  RvMutex			*mtx,
	IN  RvLogMgr		*logMgr);

/**********************************************************************************
 * RvTLSShutdown - shutdown SSL/TLS session
 *
 * Sends shutdown request to the SSL/TLS connection peer
 *
 * INPUT: 
 *	tlsSession		- Connected TLS session
 *	mtx				- Mutex that protects session 
 *					  structure
 *  logMgr			- log instance
 * OUTPUT: none
 * RETURN: RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSSessionShutdown(
	IN  RvTLSSession	*tlsSession,
	IN  RvMutex			*mtx,
	IN  RvLogMgr		*logMgr);


/**********************************************************************************
 * RvTLSTranslateSelectEvents - Translates received select events to appropriate 
 * TLS event
 *
 * INPUT: 
 *	tlsSession		- Connected TLS session
 *  selEvents		- select events to be translated
 *	mtx				- Mutex that protects session 
 *					  structure
 *	logMgr			- log instance
 * OUTPUT:
 *	tlsEvents		- translated TLS events
 * RETURN: RvStatus	- Success, WilllBlock or failure
 */
RVCOREAPI 
RvStatus RvTLSTranslateSelectEvents(
	IN  RvTLSSession	*tlsSession,
	IN  RvSelectEvents	selEvents,
	IN  RvMutex			*mtx,
	IN  RvLogMgr		*logMgr,
	OUT RvTLSEvents		*tlsEvents);

/**********************************************************************************
 * RvTLSTranslateTLSEvents - Translates received TLS events to appropriate 
 * select event
 *
 * INPUT: 
 *	tlsSession		- Connected TLS session
 *  tlsEvents		- TLS event
 *	mtx				- Mutex that protects session 
 *					  structure
 *	logMgr			- log instance
 * OUTPUT: selEvents		- select events memory
 * RETURN: RvStatus	- Success, WilllBlock or failure
 */
RVCOREAPI 
RvStatus RvTLSTranslateTLSEvents(
	IN  RvTLSSession	*tlsSession,
	IN  RvTLSEvents		tlsEvents,
	IN  RvMutex			*mtx,
	IN  RvLogMgr		*logMgr,
	OUT RvSelectEvents	*selEvents);


/**********************************************************************************
 * RvTLSGetCertificateLength - retrieves ASN1 format certificate length 
 * from a certificate context received by a certificate verification callback.
 * This function used to specify stack how much memory should be allocated for 
 * the certificate.
 *
 * INPUT: 
 *	certCtx		- Certificate context
 * OUTPUT:
 *	certLen		- lenght of ASN1 certificate
 * RETURN: 
 *	RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSGetCertificateLength(
	IN	 void  *certCtx,
	OUT  RvInt *certLen);


/**********************************************************************************
 * RvTLSGetCertificate - retrieves ASN1 format certificate from a certificate 
 * context received by a certificate verification callback.
 *
 * INPUT: 
 *	certCtx		- Certificate context
 * OUTPUT:
 *  cert		- retrieved certificate. Note that user should provide 
 *                allocated memory space enought for keeping the ASN1
 *				  certificate
 * RETURN: 
 *	RvStatus	- Success or failure
 */
RVCOREAPI
RvStatus RvTLSGetCertificate(
	IN  void   *certCtx,
	OUT RvChar *cert);


/**********************************************************************************
 * RvTLSSessionGetCertificateLength - retrieves ASN1 format certificate length 
 * from a session that complete handshake
 * This function used to specify stack how much memory should be allocated for 
 * the certificate.
 *
 * INPUT: 
 *	tlsSession		- Connected TLS session
 *	mtx			- Mutex that protects session 
 *				  structure
 *  logMgr		- log instance
 * OUTPUT:
 *  certLen		- lenght of ASN1 certificate
 * RETURN: 
 *	RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSSessionGetCertificateLength(
	IN  RvTLSSession	*tlsSession,
	IN  RvMutex			*mtx,
	IN  RvLogMgr		*logMgr,
	OUT RvInt			*certLen);

/**********************************************************************************
 * RvTLSSessionGetCertificate - retrieves ASN1 format certificate from a session 
 * after handshake is complete
 *
 * INPUT: 
 *	tlsSession	- Connected TLS session
 *	mtx			- Mutex that protects session 
 *				  structure
 *  logMgr		- log instance
 * OUTPUT:
 *  cert		- retrieved certificate. Note that user should provide 
 *                allocated memory space enought for keeping the ASN1
 *				  certificate
 * RETURN: 
 *	RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSSessionGetCertificate(
	IN  RvTLSSession	*tlsSession,
	IN  RvMutex			*mtx,
	IN  RvLogMgr		*logMgr,
	OUT RvChar			*cert);

/**********************************************************************************
 * RvTLSSessionExpose - exposes session's underlying SSL* pointer
 *
 * INPUT:
 *  tlsSession  - Connected TLS session
 *  mtx         - Mutex that protects session
 *                structure
 *  logMgr      - log instance
 * OUTPUT:
 *  underlying  - session's underlying SSL* pointer
 * RETURN:
 *  RvStatus    - Success or failure
 */
RVCOREAPI
RvStatus RvTLSSessionExpose(
    IN  RvTLSSession    *tlsSession,
    IN  RvMutex         *mtx,
    IN  RvLogMgr        *logMgr,
    OUT SSL            **underlying);

/**********************************************************************************
 * RvTLSEngineAddCertificate - adds certificate to the engine certificate chain
 *
 * INPUT: 
 *	tlsEngine	- TLS engine where certificate will be added
 *  cert		- local side certificate
 *  certLen		- local side certificate length
 *  mtx			- mutex that protects this TLS 
 *				  engine memory
 *	logMgr		- log instance
 * OUTPUT: none
 * RETURN: RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSEngineAddCertificate(
	IN  RvTLSEngine				*tlsEngine,
	IN  RvChar					*cert,
	IN  RvInt					certLen,
	IN  RvMutex					*mtx,
	IN  RvLogMgr				*logMgr);



/**********************************************************************************
 * RvTLSEngineAddAutorityCertificate - adds certificate authority to the engine
 *
 * INPUT: 
 *	tlsEngine	- TLS engine where certificate will be added
 *  cert		- CA certificate
 *  certLen		- CA certificate length
 *  mtx			- mutex that protects this TLS 
 *				  engine memory
 *	logMgr		- log instance
 * OUTPUT: none
 * RETURN: RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSEngineAddAutorityCertificate(
	IN  RvTLSEngine				*tlsEngine,
	IN  RvChar					*cert,
	IN  RvInt					certLen,
	IN  RvMutex					*mtx,
	IN  RvLogMgr				*logMgr);

/**********************************************************************************
 * RvTLSEngineExpose - exposes underlying engine's SSL_CTX*
 *
 * INPUT:
 *  tlsEngine   - TLS engine where certificate will be added
 *  mtx         - mutex that protects this TLS
 *                engine memory
 *  logMgr      - log instance
 * OUTPUT:     
 *  underlying  - underlyingctx engine's underlying SSL_CTX* pointer
 * RETURN: RvStatus - Success or failure
 */
RVCOREAPI
RvStatus RvTLSEngineExpose(
    IN  RvTLSEngine             *tlsEngine,
    IN  RvMutex                 *mtx,
    IN  RvLogMgr                *logMgr,
    OUT SSL_CTX                **underlying);

/**********************************************************************************
 * RvTLSGetCertificateVerificationError - retrieves certificate verification error
 *
 * INPUT: 
 *	cert		- certificate context, received by vverification callback
 * OUTPUT:
 *  errString	- retrieved error string
 * RETURN: none
 */
RVCOREAPI 
void RvTLSGetCertificateVerificationError(
	IN  void	*cert,
	OUT RvChar	**errString);


/**********************************************************************************
 * RvTLSEngineCheckPrivateKey - checks the consistency of a private key with the 
 *                             corresponding certificate loaded into engine
 *
 * INPUT: 
 *	logMgr		- log instance
 *  cert		- local side certificate
 *  certLen		- local side certificate length
 *  mtx			- mutex that protects this TLS 
 *				  engine memory
 *	tlsEngine	- TLS engine where certificate will be added
 * OUTPUT: none
 * RETURN: _RvStatus	- Success or failure
 */
RVCOREAPI 
RvStatus RvTLSEngineCheckPrivateKey(
	IN  RvTLSEngine				*tlsEngine,
	IN  RvMutex					*mtx,
	IN  RvLogMgr			    *logMgr);


/**********************************************************************************
 * RvTLSSessionGetCertificate - retrieves ASN1 format certificate from a session 
 * after handshake is complete
 *
 * INPUT: 
 *	tlsSession	- Connected TLS session
 *	name		- domain name string
 *	mtx			- Mutex that protects session structure
 *  logMgr		- log instance
 * RETURN: 
 *	RvStatus	- Success or failure
 */
RVCOREAPI
RvStatus RvTLSSessionCheckCertAgainstName(
	IN  RvTLSSession	*tlsSession,
    IN  RvChar          *name,
	IN  RvMutex			*mtx,
	IN	RvChar			*memBuf,
	IN	RvInt32			memBufLen,
	IN  RvLogMgr		*logMgr);


/**********************************************************************************
 * RvTLSSessionGetSubjectAltDNS - retrieves dns names of the subject
 *
 * INPUT: 
 *	IN    sess	   - Connected TLS session
 *  IN    buf      - buffer that will contain zero-separated list of dns names
 *  INOUT pBufSize - pointer to the size of buffer. On return contains requested buffer size
 *  OUT   pNitems  - pointer to the size of the list
 *  logMgr		-    log instance
 * RETURN: 
 *	RV_OK - for success
 *  RV_ERROR_OUTOFRESOURCES - not enough buffer space. In this case *pbufSize will hold the requested
 *                            buffer size.
 *  
 */
RVCOREAPI
RvStatus RvTLSSessionGetSubjectAltDNS(RvTLSSession *sess, RvChar *buf, RvSize_t *pbufSize, RvSize_t *pNitems, RvLogMgr *logMgr);

RVCOREAPI
RvStatus RvX509GetSubjectAltDNS(X509 *cert, RvChar *buf, RvSize_t *pbufSize, RvSize_t *pNitems);

#if RV_TLS_ENABLE_RENEGOTIATION

/**********************************************************************************
 * RvTLSSessionRenegotiate - renegotiate TLS session
 *
 * INPUT: 
 *	tlsSession	- Connected TLS session
 *	forced		- if RV_FALSE - renegotiation process will be completed during 
 *                I/O operatation. Currently unused
 *                if RV_TRUE - no applicationI/O is possible during renegotiation process,
 *                e.g. next message from the peer should be handshake protocol message
 *	mtx			- Mutex that protects session structure
 *  logMgr		- log instance
 * RETURN: 
 *	RvStatus	- Success, WilllBlock or failure
 *                  
 */ 
       
RVCOREAPI
RvStatus RvTLSSessionRenegotiate(RvTLSSession *tlsSession, RvBool forced, RvMutex *mtx, RvLogMgr *logMgr);

#endif

#if defined(__cplusplus)
}
#endif /*(RV_TLS_TYPE != RV_TLS_NONE) */

#endif
#endif
