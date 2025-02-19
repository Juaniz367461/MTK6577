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

#include "meta_gpio.h"
#include "funk.h"
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/mtgpio.h>


/*****************************************************************************/
struct meta_gpio_object {
    GPIO_CNF cnf;
    int      fd;
    bool     init;
};
/*****************************************************************************/
static struct meta_gpio_object gpio_obj = {.init = false};
static const char *dev = "/dev/mtgpio";
static const int op_map[] = {
    GPIO_IOCQMODE,        /*GET_MODE_STA*/
    GPIO_IOCTMODE0,       /*SET_MODE_0*/
    GPIO_IOCTMODE1,       /*SET_MODE_1*/
    GPIO_IOCTMODE2,       /*SET_MODE_2*/
    GPIO_IOCTMODE3,       /*SET_MODE_3*/

    GPIO_IOCQDIR,         /*GET_DIR_STA*/  
    GPIO_IOCSDIRIN,       /*SET_DIR_IN*/
    GPIO_IOCSDIROUT,      /*SET_DIR_OUT*/

    GPIO_IOCQPULLEN,      /*GET_PULLEN_STA*/
    GPIO_IOCSPULLDISABLE, /*SET_PULLEN_DISABLE*/
    GPIO_IOCSPULLENABLE,  /*SET_PULLEN_ENABLE*/

    GPIO_IOCQPULL,        /*GET_PULL_STA*/
    GPIO_IOCSPULLDOWN,    /*SET_PULL_DOWN*/
    GPIO_IOCSPULLUP,      /*SET_PULL_UP*/

    GPIO_IOCQINV,         /*GET_INV_STA*/
    GPIO_IOCSINVENABLE,   /*SET_INV_ENABLE*/
    GPIO_IOCSINVDISABLE,  /*SET_INV_DISABLE*/

    GPIO_IOCQDATAIN,      /*GET_DATA_IN*/
    GPIO_IOCQDATAOUT,     /*GET_DATA_OUT*/
    GPIO_IOCSDATALOW,     /*SET_DATA_LOW*/
    GPIO_IOCSDATAHIGH,    /*SET_DATA_HIGH*/
};
static bool __DEBUG__ = true;
#define MGP_LOG(fmt, args...)                           \
    do {                                                \
        if (__DEBUG__)                                  \
            META_LOG("GPIO: %s: "fmt, __func__,##args); \
    } while (0)        
/*****************************************************************************/
void Meta_GPIO_Debug(bool enable)
{
    __DEBUG__ = enable;
    printf("debug: %s\n", (enable) ? "enable" : "disable");
}
/*****************************************************************************/
bool Meta_GPIO_Init(void) 
{
    struct meta_gpio_object *obj = &gpio_obj;
    if (obj->init) {
        //MGP_LOG("initialized!!\n");
        return true;
    }

    obj->fd = open(dev, O_RDONLY);

    if (obj->fd == -1) {
        //MGP_LOG("open %s fail, %s\n", dev, strerror(errno));
        return false;
    } else {
        obj->init = true;
        //MGP_LOG("okay\n");
        return true;
    }
}
/*****************************************************************************/
GPIO_CNF Meta_GPIO_OP(GPIO_REQ req, unsigned char* peer_buf, unsigned short peer_len)
{
    struct meta_gpio_object *obj = &gpio_obj;
    int res, op = -1;

    /*reference to WinMO*/
    obj->cnf.header.id = req.header.id + 1;
    obj->cnf.header.token = req.header.token;
    obj->cnf.status = 0;
    switch (req.op)
    {
        case GET_MODE_STA:
        case GET_DIR_STA:
        case GET_PULLEN_STA:
        case GET_PULL_STA:
        case GET_INV_STA:
        case GET_DATA_IN:
        case GET_DATA_OUT:
            op = op_map[req.op];
            res = ioctl(obj->fd, op, req.pin);
            obj->cnf.data   = (res < 0) ? (0) : (res);
            obj->cnf.status = (res < 0) ? (META_FAILED) : (META_SUCCESS);
            break;
        
        case SET_MODE_0:
        case SET_MODE_1:
        case SET_MODE_2:
        case SET_MODE_3:
        case SET_DIR_IN:
        case SET_DIR_OUT:
        case SET_PULLEN_DISABLE:
        case SET_PULLEN_ENABLE:
        case SET_PULL_DOWN:
        case SET_PULL_UP:
        case SET_INV_ENABLE:
        case SET_INV_DISABLE:
        case SET_DATA_LOW:
        case SET_DATA_HIGH:
            op = op_map[req.op];
            res = ioctl(obj->fd, op, req.pin);
            obj->cnf.data = 0;
            obj->cnf.status = (res < 0) ? (META_FAILED) : (META_SUCCESS);
            break;
            
        default:
            res = -EFAULT;
            obj->cnf.status = META_FAILED;
            obj->cnf.data = 0;
            break;
    }
    //MGP_LOG("(%2d,%3d) -> 0x%08x : %d, %d, %d\n", req.op, req.pin, op, res, obj->cnf.data, obj->cnf.status);
    return obj->cnf;
}
/*****************************************************************************/
bool Meta_GPIO_Deinit(void)
{
    int res;
    struct meta_gpio_object *obj = &gpio_obj;
    if (!obj->init)
        return true;
    
    res = close(obj->fd);
   // if (res)
	    
        //MGP_LOG("close %s fail, %s\n", dev, strerror(errno));

    obj->init = false;    
    //MGP_LOG("okay\n");
    return true;
}
