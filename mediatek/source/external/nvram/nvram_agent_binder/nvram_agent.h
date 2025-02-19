/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */


#include <utils/KeyedVector.h>
#include <utils/RefBase.h>
#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include <utils/String16.h>
#include <utils/threads.h>

#include <sys/socket.h>
#include <sys/un.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include <pthread.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <utils/Log.h>

#include<sys/mount.h>
//#include "../../../external/mediatek/meta/include/FT_Public.h"
//#include "../../../external/mediatek/meta/include/WM2Linux.h"
#include "../../../../kernel/include/mtd/mtd-abi.h"
#include "../../../../kernel/include/mtd/mtd-abi.h"
//#include "../../../mediatek/source/external/nvram/libfile_op/libfile_op.h"

#include <cutils/atomic.h>
#include <utils/Errors.h>
#include <binder/IServiceManager.h>
#include <utils/String16.h>
#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <utils/Vector.h>
#include "libnvram.h"

using namespace android;
enum {
    TRANSACTION_readFile = IBinder::FIRST_CALL_TRANSACTION,
    TRANSACTION_writeFile,
};

class INvRAMAgent:public IInterface {
public:
    DECLARE_META_INTERFACE(NvRAMAgent)
    virtual char* readFile(int file_lid,int & size)=0;
    virtual int writeFile(int file_lid,int size,char *buff)=0;
};

class BpNvRAMAgent: public android::BpInterface<INvRAMAgent>
{
public:
    BpNvRAMAgent(const android::sp<android::IBinder>& impl)
	: android::BpInterface<INvRAMAgent>(impl)
        {
        }
    char* readFile(int file_lid,int & size) {return 0;}
    int writeFile(int file_lid,int size,char *buff) {return 1;}
};

class BnNvRAMAgent : public BnInterface<INvRAMAgent>
{
public:
    status_t onTransact(uint32_t code,
			const Parcel &data,
			Parcel *reply,
			uint32_t flags);
    
};

class NvRAMAgent : public BnNvRAMAgent
{

public:
    static  void instantiate();
    NvRAMAgent();
    ~NvRAMAgent() {}
    virtual char* readFile(int file_lid,int & size);
    virtual int writeFile(int file_lid,int size,char *buff);
};


IMPLEMENT_META_INTERFACE(NvRAMAgent, "NvRAMAgent")

