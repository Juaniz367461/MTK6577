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

/* ------------------------------------------------------------------
 * Copyright (C) 1998-2009 PacketVideo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 * -------------------------------------------------------------------
 */
// -*- c++ -*-
// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

//     O S C L C O N F I G _ I O

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =


/*! \file osclconfig_io.h
 *  \brief This file contains common typedefs based on the ANSI C limits.h header
 *
 *  This header file should work for any ANSI C compiler to determine the
 *  proper native C types to use for OSCL integer types.
 */


#ifndef OSCLCONFIG_IO_H_INCLUDED
#define OSCLCONFIG_IO_H_INCLUDED

#ifndef OSCLCONFIG_H_INCLUDED
#include "osclconfig.h"
#endif

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <fcntl.h>
#include <signal.h>
#include <netdb.h>
#include <sys/types.h>
#include <sys/vfs.h>
#include <glob.h>


//For File I/O
#define OSCL_HAS_GLOB 1
#define OSCL_HAS_ANSI_FILE_IO_SUPPORT 1
#define OSCL_HAS_SYMBIAN_COMPATIBLE_IO_FUNCTION 0
#define OSCL_HAS_NATIVE_FILE_CACHE_ENABLE 1
#define OSCL_FILE_BUFFER_MAX_SIZE   32768
#define OSCL_HAS_PV_FILE_CACHE  1
#define OSCL_HAS_LARGE_FILE_SUPPORT 1

//For Sockets
#define OSCL_HAS_SYMBIAN_SOCKET_SERVER 0
#define OSCL_HAS_SYMBIAN_DNS_SERVER 0
#define OSCL_HAS_BERKELEY_SOCKETS 1
#define OSCL_HAS_SOCKET_SUPPORT 1
#define OSCL_HAS_SELECTABLE_PIPES 1

//basic socket types
typedef int TOsclSocket;
typedef struct sockaddr_in TOsclSockAddr;
typedef socklen_t TOsclSockAddrLen;

//Init addr macro
#define OsclValidInetAddr(addr) (inet_addr(addr)!=((in_addr_t)(-1)))

//address conversion macro-- from string to network address.
#define OsclMakeSockAddr(sockaddr,port,addrstr,ok)\
    sockaddr.sin_family=OSCL_AF_INET;\
    sockaddr.sin_port=htons(port);\
    int32 result=inet_aton((const char*)addrstr,&sockaddr.sin_addr);\
    ok=(result!=0);

//address conversion macro-- from network address to string
#define OsclUnMakeSockAddr(sockaddr,addrstr)\
    addrstr=inet_ntoa(sockaddr.sin_addr);

//wrappers for berkeley socket calls
#define OsclSetRecvBufferSize(s,val,ok,err) \
    ok=(setsockopt(s,SOL_SOCKET,SO_RCVBUF,(char*)&val, sizeof(int)) !=-1);\
    if (!ok)err=errno

#define OsclBind(s,addr,ok,err)\
    TOsclSockAddr* tmpadr = &addr;\
    sockaddr* sadr = OSCL_STATIC_CAST(sockaddr*, tmpadr);\
    ok=(bind(s,sadr,sizeof(addr))!=(-1));\
        if (!ok)err=errno

#define OsclJoin(s,addr,ok,err)\
        struct ip_mreq mreq; \
        ok=(bind(s,(sockaddr*)&addr,sizeof(addr))!=(-1));\
        mreq.imr_multiaddr.s_addr = addr.sin_addr.s_addr ; \
        mreq.imr_interface.s_addr = htonl(INADDR_ANY); \
        ok=(setsockopt(s, IPPROTO_IP, IP_ADD_MEMBERSHIP, &mreq, sizeof(struct ip_mreq))!=(-1)); \
        if (!ok)err=errno

#define OsclListen(s,size,ok,err)\
    ok=(listen(iSocket,qSize)!=(-1));\
    if (!ok)err=errno

#define OsclAccept(s,accept_s,ok,err,wouldblock)\
    accept_s=accept(s,NULL,NULL);\
    ok=(accept_s!=(-1));\
    if (!ok){err=errno;wouldblock=(err==EAGAIN||err==EWOULDBLOCK);}

#define OsclSetNonBlocking(s,ok,err)\
    ok=(fcntl(s,F_SETFL,O_NONBLOCK)!=(-1));\
    if (!ok)err=errno

#define OsclShutdown(s,how,ok,err)\
    ok=(shutdown(iSocket,how)!=(-1));\
    if (!ok)err=errno

#define OsclSocket(s,fam,type,prot,ok,err)\
    s=socket(fam,type,prot);\
    ok=(s!=(-1));\
    if (!ok)err=errno

#define OsclSendTo(s,buf,len,addr,ok,err,nbytes,wouldblock)\
    TOsclSockAddr* tmpadr = &addr;\
    sockaddr* sadr = OSCL_STATIC_CAST(sockaddr*, tmpadr);\
    nbytes=sendto(s,buf,(size_t)(len),0,sadr,(socklen_t)sizeof(addr));\
    ok=(nbytes!=(-1));\
    if (!ok){err=errno;wouldblock=(err==EAGAIN||err==EWOULDBLOCK);}

#define OsclSend(s,buf,len,ok,err,nbytes,wouldblock)\
    nbytes=send(s,(const void*)(buf),(size_t)(len),0);\
    ok=(nbytes!=(-1));\
    if (!ok){err=errno;wouldblock=(err==EAGAIN||err==EWOULDBLOCK);}

#define OsclCloseSocket(s,ok,err)\
    ok=(close(s)!=(-1));\
    if (!ok)err=errno

#define OsclConnect(s,addr,ok,err,wouldblock)\
    TOsclSockAddr* tmpadr = &addr;\
    sockaddr* sadr = OSCL_STATIC_CAST(sockaddr*, tmpadr);\
    ok=(connect(s,sadr,sizeof(addr))!=(-1));\
    if (!ok){err=errno;wouldblock=(err==EINPROGRESS);}

#define OsclGetAsyncSockErr(s,ok,err)\
    int opterr;socklen_t optlen=sizeof(opterr);\
    ok=(getsockopt(s,SOL_SOCKET,SO_ERROR,(void *)&opterr,&optlen)!=(-1));\
    if(ok)err=opterr;else err=errno;

#define OsclPipe(x)         pipe(x)
#define OsclReadFD(fd,buf,cnt)  read(fd,buf,cnt)
#define OsclWriteFD(fd,buf,cnt) write(fd,buf,cnt)

//unix reports connect completion in write set in the getsockopt
//error.
#define OsclConnectComplete(s,wset,eset,success,fail,ok,err)\
    success=fail=false;\
    if (FD_ISSET(s,&eset))\
    {fail=true;OsclGetAsyncSockErr(s,ok,err);}\
    else if (FD_ISSET(s,&wset))\
    {OsclGetAsyncSockErr(s,ok,err);if (ok && err==0)success=true;else fail=true;}

#define OsclRecv(s,buf,len,ok,err,nbytes,wouldblock)\
    nbytes=recv(s,(void *)(buf),(size_t)(len),0);\
    ok=(nbytes!=(-1));\
    if (!ok){err=errno;wouldblock=(err==EAGAIN);}

#define OsclRecvFrom(s,buf,len,paddr,paddrlen,ok,err,nbytes,wouldblock)\
    nbytes=recvfrom(s,(void*)(buf),(size_t)(len),0,(struct sockaddr*)paddr,paddrlen);\
    ok=(nbytes!=(-1));\
    if (!ok){err=errno;wouldblock=(err==EAGAIN);}

#define OsclSocketSelect(nfds,rd,wr,ex,timeout,ok,err,nhandles)\
    nhandles=select(nfds,&rd,&wr,&ex,&timeout);\
    ok=(nhandles!=(-1));\
    if (!ok)err=errno

//there's not really any socket startup needed on unix, but
//you need to define a signal handler for SIGPIPE to avoid
//broken pipe crashes.
#define OsclSocketStartup(ok)\
    signal(SIGPIPE,SIG_IGN);\
    ok=true

#define OsclSocketCleanup(ok)\
    signal(SIGPIPE,SIG_DFL);\
    ok=true

//hostent type
typedef struct hostent TOsclHostent;

//wrapper for gethostbyname
#define OsclGethostbyname(name,hostent,ok,err)\
    hostent=gethostbyname((const char*)name);\
    ok=(hostent!=NULL);\
    if (!ok)err=errno;

//extract dotted address from a hostent
#define OsclGetDottedAddr(hostent,dottedaddr,ok)\
    long *_hostaddr=(long*)hostent->h_addr_list[0];\
    struct in_addr _inaddr;\
    _inaddr.s_addr=*_hostaddr;\
    dottedaddr=inet_ntoa(_inaddr);\
    ok=(dottedaddr!=NULL);

//socket shutdown codes
#define OSCL_SD_RECEIVE SHUT_RD
#define OSCL_SD_SEND SHUT_WR
#define OSCL_SD_BOTH SHUT_RDWR

//address family codes
#define OSCL_AF_INET AF_INET

//socket type codes
#define OSCL_SOCK_STREAM SOCK_STREAM
#define OSCL_SOCK_DATAGRAM SOCK_DGRAM

//IP protocol codes
#define OSCL_IPPROTO_TCP IPPROTO_TCP
#define OSCL_IPPROTO_UDP IPPROTO_UDP

//End sockets

// file IO support
#if (OSCL_HAS_LARGE_FILE_SUPPORT)
#define _FILE_OFFSET_BITS 64
typedef off_t TOsclFileOffset;
#else
typedef int32 TOsclFileOffset;
#endif

//For Parser AsyncFile I/O
// Honda add
//#define USES_ASYNC_FILE_IO

#include "osclconfig_io_check.h"

#endif

