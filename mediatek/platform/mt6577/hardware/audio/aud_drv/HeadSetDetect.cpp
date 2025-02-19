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

/*
** this module is used to detect headset/headphone.
** CMCC criteria claims that sound must output from earphone when system power on/off if 
** earphone is connected. BootAnimation starts ahead of java, so audio should
** detect earphone event.if earphone is conneced,speaker will be turned off.
*/
#define LOG_TAG "HeadSetDetect"
#include "HeadSetDetect.h"
#include <cutils/xlog.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <poll.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <linux/netlink.h>
namespace {

const char HEADSET_STATE_PATH[]= "/sys/class/switch/h2w/state";
const char  headName[]={"SWITCH_NAME=h2w"};
const char  headState[]={"SWITCH_STATE="};

}

namespace android {

HeadSetDetect::HeadSetDetect(void* observer,callback_t cblk)
	    :Thread(false),
	     mActive(false),
	     mOn(false),
	     mObserver(observer),
	     mCblk(cblk),
	     mFd(-1)
{
}

HeadSetDetect::~HeadSetDetect()
{
    SXLOGV("+deconstruct");
    stop();
    requestExitAndWait();
    SXLOGV("-deconstruct");
}

status_t HeadSetDetect::init()
{
    int state = readStateFromFile();
    if(state == -1)
        return NO_INIT;
    bool on=  headsetConnect(state);
    if(mOn != on)
    {
        mCblk(mObserver,on);
        mOn = on;
    }
    if(!socketInit())
        return NO_INIT;
    return NO_ERROR;
}

int HeadSetDetect::readStateFromFile()
{
    SXLOGV("readStateFromFile");
    int fd = open(HEADSET_STATE_PATH, O_RDONLY, 0);
    if(fd < 0)
    {
        SXLOGD("Fail to open file %s", HEADSET_STATE_PATH);
        return -1;
    }
    char buf[1];
    if (read(fd, buf, 1) == -1){
        SXLOGD("Can't read %s", HEADSET_STATE_PATH);
        return -1;
    }
    close(fd);
    return buf[0];
}
int  HeadSetDetect::detect()
{
    char buffer[1024];
    int state =-1;
    int count = socketNextEvent(buffer,sizeof(buffer));
    if(count != 0)
    {   
        state = parseState(buffer,count);
    }
    return state;
}

bool HeadSetDetect::isPlaying()
{
    Mutex::Autolock _l(mLock);
    return mActive;
}
void HeadSetDetect::stop()
{
    SXLOGV("HeadSetDetect::stop");

    Mutex::Autolock _l(mLock);
    mActive = false;
    requestExit();
}

bool HeadSetDetect::threadLoop()
{ 
    int state = detect(); //do not hold lock;
    Mutex::Autolock _l(mLock);
    if(mActive == false)
    {
       if(mFd >=0)
       {
           close(mFd); //close socket
           mFd = -1;
       }
       LOGD("thread to exit");
       return false;
    }
    //SXLOGV("threadLoop:state=%d",state);
    if(state != -1)
    {
        bool on= headsetConnect(state);
        if(mOn != on )
        {  
            mCblk(mObserver,on);
            mOn = on; 
        }
    }
    LOGD_IF(state == -1,"detect other event");
  return true;
}

int HeadSetDetect::parseState(char *buffer, int len)
{
   char *beg = buffer;
   char *end = buffer + len;
   char *p = beg;

   bool find=false;
   char value[1]={-1};
   while(p<end)
   {
      // SXLOGV("%s",p);
       int len= strlen(p);
       char *namePos=strstr(p, headName);
       if(namePos)
       {
           find=true;
       }
       char *statePos=strstr(p, headState);
       if(statePos)
       {
           value[0]= p[len-1];
           //SXLOGV("%c",value[0]);
       }
       p= p + len + 1;
   }
   if(find)
   {
        return value[0];
   }
    return -1;
}

bool  HeadSetDetect::headsetConnect(int stateVal)
{
     if(WIRE_HEADSET==stateVal){
        SXLOGV("detect headset pluged in");
        return true;
    }else if(BIT_HEADSET_NO_MIC==stateVal) {
        SXLOGV("detect headphone pluged in");
        return true;
    }else{
        SXLOGV("default headset pluged out");
        return false;
    }
}
    
status_t HeadSetDetect::readyToRun()
{

    Mutex::Autolock _l(mLock);
    if(init()!=NO_INIT)
    {
        mActive =true;
    }
    return NO_ERROR;
}
void HeadSetDetect::onFirstRef()
{
    const size_t SIZE = 256;
    char buffer[SIZE];

    snprintf(buffer, SIZE, "HeadSetDetect %p", this);

    run(buffer, PRIORITY_NORMAL);
}

int HeadSetDetect::socketInit()
{
    struct sockaddr_nl addr;
    int sz = 64*1024;
    int s;

    memset(&addr, 0, sizeof(addr));
    addr.nl_family = AF_NETLINK;
    addr.nl_pid = getpid();
    addr.nl_groups = 0xffffffff;

    s = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
    if(s < 0)
        return 0;

    setsockopt(s, SOL_SOCKET, SO_RCVBUFFORCE, &sz, sizeof(sz));

    if(bind(s, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
        close(s);
        return 0;
    }

    mFd = s;
	
    return (mFd >= 0);
}

int HeadSetDetect::socketNextEvent(char* buffer, int buffer_length)
{
    while (1) {
        struct pollfd fds;
        int nr;
    
        fds.fd = mFd;
        fds.events = POLLIN;
        fds.revents = 0;
        nr = poll(&fds, 1, -1);
     
        if(nr > 0 && fds.revents == POLLIN) {
            int count = recv(mFd, buffer, buffer_length, 0);
            if (count > 0) {
                return count;
            } 
        }
    }
    
    // won't get here
    return 0;
}

}