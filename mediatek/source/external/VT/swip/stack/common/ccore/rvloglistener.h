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

#if 0
/***********************************************************************
Filename   : rvloglistener.h
Description: rvloglistener header file - message log listener
************************************************************************
        Copyright (c) 2001 RADVISION Inc. and RADVISION Ltd.
************************************************************************
NOTICE:
This document contains information that is confidential and proprietary
to RADVISION Inc. and RADVISION Ltd.. No part of this document may be
reproduced in any form whatsoever without written prior approval by
RADVISION Inc. or RADVISION Ltd..

RADVISION Inc. and RADVISION Ltd. reserve the right to revise this
publication and make changes without obligation to notify any person of
such revisions or changes.
***********************************************************************/

#ifndef RV_LOG_LISTENER_H
#define RV_LOG_LISTENER_H


#include "rvccore.h"
#include "rvstdio.h"
#include "rvlog.h"
#if (RV_OS_TYPE == RV_OS_TYPE_SYMBIAN) && (RV_THREADNESS_TYPE == RV_THREADNESS_MULTI)
#include "rvsemaphore.h"
#endif

/* Error checks to make sure configuration has been done properly */
#if !defined(RV_LOGLISTENER_TYPE) || ((RV_LOGLISTENER_TYPE != RV_LOGLISTENER_WIN32) && \
    (RV_LOGLISTENER_TYPE != RV_LOGLISTENER_FILE_AND_TERMINAL) && \
    (RV_LOGLISTENER_TYPE != RV_LOGLISTENER_TERMINAL) && \
    (RV_LOGLISTENER_TYPE != RV_LOGLISTENER_UDP) && \
    (RV_LOGLISTENER_TYPE != RV_LOGLISTENER_NONE))
#error RV_LOGLISTENER_TYPE not set properly
#endif
/* End of configuration error checks */



#if ((RV_LOGLISTENER_TYPE == RV_LOGLISTENER_WIN32) || \
    (RV_LOGLISTENER_TYPE == RV_LOGLISTENER_FILE_AND_TERMINAL))
typedef struct
{
#if (RV_LOGLISTENER_TYPE == RV_LOGLISTENER_WIN32)
    HANDLE      openedFile; /* Handle of the currently opened file */
#else
    RvFILE*     openedFile; /* Handle of the currently opened file */
#endif
    RvChar      baseFilename[256]; /* Base name of files */
    RvUint32    numFiles; /* Number of files in cycle */
    RvUint32    curFileNum; /* Current file number being used */
    RvUint32    maxFileSize; /* Maximum size of file in cycle */
    RvUint32    curSize; /* Current size of file */
    RvBool      flushLines; /* If RV_TRUE, then on each message, the logfile is flushed */
    RvBool      openError; /* Indication of errors - on error, we stop logging to the log files */
#if (RV_OS_TYPE == RV_OS_TYPE_SYMBIAN) && (RV_THREADNESS_TYPE == RV_THREADNESS_MULTI)
    RvChar* logMessage;
    RvUint32 logMessageSize;
    void* logThread;        /* RvThread*   */
    RvSemaphore message;    /* RvSemaphore */
    RvSemaphore logIsReady; /* RvSemaphore */
#endif
} LogFileListener;
#endif

#if (RV_LOGLISTENER_TYPE == RV_LOGLISTENER_UDP)

#if (RV_OS_TYPE == RV_OS_TYPE_WIN32)
#include <winsock2.h>
#define CLOSE_SOCKET closesocket

#elif (RV_OS_TYPE == RV_OS_TYPE_OSE)
#include <inet.h>
#define CLOSE_SOCKET close

#elif (RV_OS_TYPE == RV_OS_TYPE_NUCLEUS)
#include <inc/nu_net.h>

/* redefine structure names */
#define sockaddr_in addr_struct
#define sin_family family
#define sin_port port
#define AF_INET NU_FAMILY_IP                /* translate nucleus IP definition */
#define SOCK_DGRAM  NU_TYPE_DGRAM           /* translate nucleus UDP definition */
//#define errno 777

#else

#if (RV_OS_TYPE == RV_OS_TYPE_PSOS)
#include <pna.h>
#endif

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
//#include <errno.h>
#if (RV_OS_TYPE == RV_OS_TYPE_VXWORKS)
#include <sockLib.h>
#endif
#define CLOSE_SOCKET close
#endif

#endif

/********************************************************************************************
 * RvLogListener
 * Holds information about the listeners supported by the core.
 * This includes only the type of the listener.
 * We allow only one type of each listener to be constructed - this is only the case for the
 * listeners that the core implements. Applications can write their own listeners and use
 * them as desired.
 ********************************************************************************************/
#if (RV_LOGLISTENER_TYPE != RV_LOGLISTENER_NONE)
typedef struct
{
    RvLogMgr*       logMgr; /* Log manager this listener uses */
    int             listenerType; /* Type of listener constructed */
#if ((RV_LOGLISTENER_TYPE == RV_LOGLISTENER_WIN32) || \
     (RV_LOGLISTENER_TYPE == RV_LOGLISTENER_FILE_AND_TERMINAL))
	LogFileListener logFileListener;
#elif (RV_LOGLISTENER_TYPE == RV_LOGLISTENER_UDP)
	void*  logUdpListener;
#endif
} RvLogListener;

#else

/* Dummy declaration for a log listener, since we're not supporting any */
typedef RvInt RvLogListener;

#endif  /* (RV_LOGLISTENER_TYPE != RV_LOGLISTENER_NONE) */



#if defined(__cplusplus)
extern "C" {
#endif

/********************************************************************************************
 * RvLogListenerInit - inits log listener module
 *
 * This function should be called only once in the process
 *
 * INPUT   : none
 * OUTPUT  : None.
 * RETURN  : always RV_OK
 */
RvStatus RvLogListenerInit(void);

/********************************************************************************************
 * RvLogListenerEnd - shut down log listener module
 *
 * This function should be called only once in the process
 *
 * INPUT   : none
 * OUTPUT  : None.
 * RETURN  : always RV_OK
 */
RvStatus RvLogListenerEnd(void);


#if (RV_LOGLISTENER_TYPE != RV_LOGLISTENER_NONE)

/********************************************************************************************
 * RvLogListenerConstructTerminal
 *
 * Construct a log listener that sends log messages to the terminal, using
 * standard output or standard error
 *
 * INPUT   : listener   - Listener to construct
 *           logMgr     - Log manager to listen to
 *           stdOut     - RV_TRUE for stdout, RV_FALSE for stderr
 * OUTPUT  : None.
 * RETURN  : RV_OK on success, other values on failure
 */
RVCOREAPI
RvStatus RVCALLCONV RvLogListenerConstructTerminal(
    IN  RvLogListener*  listener,
    IN  RvLogMgr*       logMgr,
    IN  RvBool          stdOut);


/********************************************************************************************
 * RvLogListenerDestructTerminal - Destruct the terminal listener
 *
 * INPUT   : listener   - Listener to destruct
 * OUTPUT  : None.
 * RETURN  : RV_OK on success, other values on failure
 */
RVCOREAPI
RvStatus RVCALLCONV RvLogListenerDestructTerminal(
    IN  RvLogListener*  listener);


/********************************************************************************************
 * RvLogListenerConstructLogfile
 *
 * Construct a log listener that sends log messages to a file
 *
 * INPUT   : listener           - Listener to construct
 *           logMgr             - Log manager to listen to
 *           fileName           - Name of the logfile
 *           numFiles           - Number of cyclic files to use
 *           fileSize           - Size of each file in cycle in bytes
 *                                This parameter is only applicable if numFiles > 1
 *           flushEachMessage   - RV_TRUE if we want to flush each message written to the
 *                                logfile
 * OUTPUT  : None.
 * RETURN  : RV_OK on success, other values on failure
 */
RVCOREAPI
RvStatus RVCALLCONV RvLogListenerConstructLogfile(
    IN  RvLogListener*  listener,
    IN  RvLogMgr*       logMgr,
    IN  const RvChar*   fileName,
    IN  RvUint32        numFiles,
    IN  RvUint32        fileSize,
    IN  RvBool          flushEachMessage);


/********************************************************************************************
 * RvLogListenerLogfileGetCurrentFilename
 *
 * Get the filename of the current file being written.
 *
 * INPUT   : listener       - Listener to check
 *           fileNameLength - Maximum length of filename
 * OUTPUT  : fileName       - Filename of the current file being written
 * RETURN  : RV_OK on success, other values on failure
 */
RVCOREAPI
RvStatus RVCALLCONV RvLogListenerLogfileGetCurrentFilename(
    IN  RvLogListener*  listener,
    IN  RvUint32        fileNameLength,
    OUT RvChar*         fileName);


/********************************************************************************************
 * RvLogListenerDestructLogfile - Destruct the logfile listener
 *
 * INPUT   : listener   - Listener to destruct
 * OUTPUT  : None.
 * RETURN  : RV_OK on success, other values on failure
 */
RVCOREAPI
RvStatus RVCALLCONV RvLogListenerDestructLogfile(
    IN  RvLogListener*  listener);


/********************************************************************************************
 * RvLogListenerConstructDebug
 *
 * Construct a log listener that sends log messages to the debug window of Visual C.
 * This one is only applicable for Win32 applications.
 *           
 * INPUT   : listener   - Listener to construct
 *           logMgr     - Log manager to listen to
 * OUTPUT  : None.
 * RETURN  : RV_OK on success, other values on failure
 */
RVCOREAPI
RvStatus RVCALLCONV RvLogListenerConstructDebug(
    IN  RvLogListener*  listener,
    IN  RvLogMgr*       logMgr);


/********************************************************************************************
 * RvLogListenerDestructDebug - Destruct the debug listener
 *
 * INPUT   : listener   - Listener to destruct
 * OUTPUT  : None.
 * RETURN  : RV_OK on success, other values on failure
 */
RVCOREAPI
RvStatus RVCALLCONV RvLogListenerDestructDebug(
    IN  RvLogListener*  listener);


/********************************************************************************************
 * RvLogListenerConstructUdp
 *
 * Construct a log listener that sends log messages to server using UDP protocol.
 *           
 * INPUT   : listener   - Listener to construct
 *           logMgr     - Log manager to listen to
 * OUTPUT  : None.
 * RETURN  : RV_OK on success, other values on failure
 */
RVCOREAPI
RvStatus RVCALLCONV RvLogListenerConstructUdp(
    IN  RvLogListener*  listener,
    IN  RvLogMgr*       logMgr,
    IN  RvChar*         fileName,
    IN  RvChar*         serverIpAddr,
    IN  RvUint16        serverPort);


/********************************************************************************************
 * RvLogListenerDestructUdp - Destruct the UDP listener
 *
 * INPUT   : listener   - Listener to destruct
 * OUTPUT  : None.
 * RETURN  : RV_OK on success, other values on failure
 */
RVCOREAPI
RvStatus RVCALLCONV RvLogListenerDestructUdp(
    IN  RvLogListener*  listener);


#else
/* No internal listeners - use macros instead */
#define RvLogListenerConstructTerminal(_listener, _logMgr, _stdOut) (*_listener = RV_OK)
#define RvLogListenerDestructTerminal(_listener) (*_listener = RV_OK)
#define RvLogListenerConstructLogfile(_listener, _logMgr, _fileName, _numFiles, _fileSize, _flushEachMessage) \
    (*_listener = RV_OK)
#define RvLogListenerDestructLogfile(_listener) (*_listener = RV_OK)
#define RvLogListenerConstructDebug(_listener, _logMgr) (*_listener = RV_OK)
#define RvLogListenerDestructDebug(_listener) (*_listener = RV_OK)
#define RvLogListenerConstructUdp(_listener, _logMgr, _fileName, _ipAddr, _port) (*_listener = RV_OK)
#define RvLogListenerDestructUdp(_listener) (*_listener = RV_OK)

#endif  /* (RV_LOGLISTENER_TYPE != RV_LOGLISTENER_NONE) */


#if defined(__cplusplus)
}
#endif /* __cplusplus*/

#endif  /* RV_LOG_LISTENER_H */
#endif
