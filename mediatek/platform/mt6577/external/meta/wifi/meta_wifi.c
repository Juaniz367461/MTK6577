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

#include "meta_wifi.h"

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#include <net/if_arp.h>		    /* For ARPHRD_ETHER */
#include <sys/socket.h>		    /* For AF_INET & struct sockaddr */
#include <netinet/in.h>         /* For struct sockaddr_in */
#include <netinet/if_ether.h>
#include <linux/wireless.h>

#include "cutils/misc.h"
#include "iwlibstub.h"

#include "libnvram.h"
#include "Custom_NvRam_LID.h"

#ifndef WIFI_DRV_MOD_PATH
#define WIFI_DRV_MOD_PATH         "/system/lib/modules/wlan.ko"
#endif
#ifndef WIFI_DRV_MOD_NAME
#define WIFI_DRV_MOD_NAME         "wlan"
#endif
#ifndef WIFI_DRV_MOD_ARG
#define WIFI_DRV_MOD_ARG          ""
#endif
#ifndef WIFI_TYPE_NAME
#define WIFI_TYPE_NAME            "wlan"
#endif

#define FREEIF(p)   do { if(p) free(p); p = NULL; } while(0)

static const char DRIVER_MODULE_NAME[]  = WIFI_DRV_MOD_NAME;
static const char DRIVER_MODULE_TAG[]   = WIFI_DRV_MOD_NAME " ";
static const char DRIVER_MODULE_ARG[]   = WIFI_DRV_MOD_ARG;
static const char DRIVER_MODULE_PATH[]  = WIFI_DRV_MOD_PATH;
static const char MODULE_FILE[]         = "/proc/modules";

static int   wifi_init = 0;
static int   wifi_skfd = -1;
static int   wifi_rfkill_id = -1;
static char *wifi_rfkill_state_path = NULL;
static WIFI_CNF_CB cnf_cb = NULL;

extern int init_module(void *, unsigned long, const char *);
extern int delete_module(const char *, unsigned int);
extern int sched_yield(void);
extern int ifc_init();
extern int ifc_up(const char *name);
extern int ifc_down(const char *name);
extern void ifc_close();

static void wifi_send_resp(FT_WM_WIFI_CNF *cnf, void *buf, unsigned int size)
{
    if (cnf_cb)
        cnf_cb(cnf, buf, size);
    else
        WriteDataToPC(cnf, sizeof(FT_WM_WIFI_CNF), buf, size);
}

static int wifi_init_rfkill(void) 
{
    char path[128];
    char buf[32];
    int fd, id;
    ssize_t sz;

    for (id = 0; id < 10 ; id++) {
        snprintf(path, sizeof(path), "/sys/class/rfkill/rfkill%d/type", id);
        fd = open(path, O_RDONLY);
        if (fd < 0) {
            printf("open(%s) failed: %s (%d)\n", path, strerror(errno), errno);
            return -1;
        }
        sz = read(fd, &buf, sizeof(buf));
        close(fd);
        if (sz >= (ssize_t)strlen(WIFI_TYPE_NAME) && 
            memcmp(buf, WIFI_TYPE_NAME, strlen(WIFI_TYPE_NAME)) == 0) {
            wifi_rfkill_id = id;
            break;
        }
    }

    if (id == 10)
        return -1;

    asprintf(&wifi_rfkill_state_path, "/sys/class/rfkill/rfkill%d/state", 
        wifi_rfkill_id);

    return 0;
}

static int wifi_check_power(void) {
    int sz;
    int fd = -1;
    int ret = -1;
    char buf;
    char *path = wifi_rfkill_state_path;

    if ((wifi_rfkill_id == -1) && wifi_init_rfkill())
        goto out;

    fd = open(path, O_RDONLY);
    if (fd < 0) {
        printf("open(%s) failed: %s (%d)", path, strerror(errno),
            errno);
        goto out;
    }
    sz = read(fd, &buf, 1);
    if (sz != 1) {
        printf("read(%s) failed: %s (%d)", path, strerror(errno),
            errno);
        goto out;
    }

    switch (buf) {
    case '1':
        ret = 1;
        break;
    case '0':
        ret = 0;
        break;
    }

out:
    if (fd >= 0) 
        close(fd);
    return ret;
}

static int wifi_set_power(int on) 
{
    int sz;
    int fd = -1;
    int ret = -1;
    const char buf = (on ? '1' : '0');

    if (wifi_rfkill_id == -1) {
        if (wifi_init_rfkill()) goto out;
    }

    fd = open(wifi_rfkill_state_path, O_WRONLY);
    if (fd < 0) {
        printf("open(%s) for write failed: %s (%d)", wifi_rfkill_state_path,
             strerror(errno), errno);
        goto out;
    }
    sz = write(fd, &buf, 1);
    if (sz < 0) {
        printf("write(%s) failed: %s (%d)", wifi_rfkill_state_path, strerror(errno),
             errno);
        goto out;
    }
    ret = 0;

out:
    if (fd >= 0) close(fd);
    return ret;
}

static int wifi_insmod(const char *filename, const char *args)
{
    void *module;
    unsigned int size;
    int ret;

    module = load_file(filename, &size);
    if (!module)
        return -1;

    ret = init_module(module, size, args);

    free(module);

    return ret;
}

static int wifi_rmmod(const char *modname)
{
    int ret = -1;
    int maxtry = 10;

    while (maxtry-- > 0) {
        ret = delete_module(modname, O_NONBLOCK | O_EXCL);
        if (ret < 0 && errno == EAGAIN)
            usleep(500000);
        else
            break;
    }

    if (ret != 0)
        printf("Unable to unload driver \"%s\": %s\n", modname, strerror(errno));

    return ret;
}

static int wifi_is_loaded(void) 
{
    FILE *proc;
    char line[sizeof(DRIVER_MODULE_TAG)+10];

    if ((proc = fopen(MODULE_FILE, "r")) == NULL) {
        printf("Could not open %s: %s", MODULE_FILE, strerror(errno));
        return 0;
    }
    while ((fgets(line, sizeof(line), proc)) != NULL) {
        if (strncmp(line, DRIVER_MODULE_TAG, strlen(DRIVER_MODULE_TAG)) == 0) {
            fclose(proc);
            return 1;
        }
    }
    fclose(proc);
    return 0;
}

void META_WIFI_Register(WIFI_CNF_CB callback)
{
    cnf_cb = callback;
}

int META_WIFI_init(void)
{
    int count = 100;

    wifi_set_power(1);

    if (!wifi_is_loaded()){
        printf("[META_WIFI] loading wifi driver ... ...\n");    	
        if (wifi_insmod(DRIVER_MODULE_PATH, DRIVER_MODULE_ARG) < 0) {
            printf("[META_WIFI] failed to load wifi driver!!!\n");    	        	
            goto error;
        }
    }

    sched_yield();
    
    while (count-- > 0) {
        if (ifc_init() == 0) {
            if (ifc_up("wlan0") == 0) {
                ifc_close();
                break;
            }
            printf("[META_WIFI] ifc_up(wlan0) failed\n");
            ifc_close();
        } else {
            printf("[META_WIFI] ifc_init() failed\n");
        }
        usleep(100000);
    }
    if (count == 0)
        goto error;

    if (wifi_skfd == -1)
        wifi_skfd = openNetHandle();

    if (wifi_skfd < 0) {
        META_WIFI_deinit();
        goto error;
    }

    wifi_init = 1;

    return true;

error:
    wifi_set_power(0);
    return false;
}

void META_WIFI_deinit(void)
{
    int count = 20; /* wait at most 10 seconds for completion */

    if (wifi_skfd > 0) {
        closeNetHandle(wifi_skfd);
        wifi_skfd = -1;
    }

    if (wifi_rmmod(DRIVER_MODULE_NAME) == 0) {
        while (count-- > 0) {
            if (!wifi_is_loaded())
                break;
            usleep(500000);
        }
        sched_yield();
        wifi_set_power(0);
    }
    wifi_init = 0;
    return;
}

void META_WIFI_OP(FT_WM_WIFI_REQ *req, char *peer_buf, unsigned short peer_len)
{
    unsigned int i;
    int ret = -1;
    FT_WM_WIFI_CNF cnf;
    OID_STRUC *poid = NULL;
    unsigned long avail_sz;
    NVRAM_ACCESS_STRUCT *pnvram = NULL;
    F_INFO kFileInfo;
    int iFD;
    void *ret_buf = NULL, *allocated_buf = NULL;
    unsigned int ret_size = 0;

    if (NULL == req || NULL == peer_buf || wifi_skfd < 0 || !wifi_init) {
        printf("[META_WIFI] Invalid arguments or operation\n");
        goto exit;
    }

    // OID operation
    if(WIFI_CMD_SET_OID == req->type
            || WIFI_CMD_QUERY_OID == req->type) {
        if (NULL == (poid = (OID_STRUC *)malloc(peer_len))) {
            printf("[META_WIFI] No memory\n");
            goto exit;
        }

        // for later freeing
        allocated_buf = (void *)poid;
        memcpy(poid, peer_buf, peer_len);

        if (WIFI_CMD_SET_OID == req->type) {
            for (i = 0; i < poid->SetOidPara.dataLen; i++) {
                printf("[META_WIFI] OIDReq : data[%d] = 0x%x\n",
                    i, poid->SetOidPara.data[i]);
            }
            ret = setIWreq(wifi_skfd, "wlan0", poid->SetOidPara.oid, 
                poid->SetOidPara.data, poid->SetOidPara.dataLen, &avail_sz);
            printf("[META_WIFI] SET_OID, OID: 0x%x, len: %d, ret: %d\n", 
                poid->SetOidPara.oid, poid->SetOidPara.dataLen, ret);  
        }
        else if (WIFI_CMD_QUERY_OID == req->type) {
            ret = getIWreq(wifi_skfd, "wlan0", poid->QueryOidPara.oid,
                poid->QueryOidPara.data, poid->QueryOidPara.dataLen, &avail_sz);
            printf("[META_WIFI] QUERY_OID, OID: 0x%x, len: %d, ret: %d\n", 
                poid->QueryOidPara.oid, poid->QueryOidPara.dataLen, ret);
        }

        if (ret == 0 && WIFI_CMD_QUERY_OID == req->type) {
            ret_buf = (void *)poid;
            ret_size = avail_sz+8;
        }
    }
    // NVRAM access
    else if(WIFI_CMD_NVRAM_WRITE_ACCESS == req->type
            || WIFI_CMD_NVRAM_READ_ACCESS == req->type) {
        if (NULL == (pnvram = (NVRAM_ACCESS_STRUCT *)malloc(peer_len))) {
            printf("[META_WIFI] No memory\n");
            goto exit;
        }

        // for later freeing
        allocated_buf = (void *)pnvram;
        memcpy(pnvram, peer_buf, peer_len);

        if(peer_len < (offsetof(NVRAM_ACCESS_STRUCT, data) + pnvram->dataLen)) {
            printf("[META_WIFI] Mimatched NVRAM content length: (%d / %d)\n", peer_len, offsetof(NVRAM_ACCESS_STRUCT, data) + pnvram->dataLen);
            goto exit;
        }

        kFileInfo = NVM_ReadFileVerInfo(AP_CFG_RDEB_FILE_WIFI_LID);

        if(WIFI_CMD_NVRAM_READ_ACCESS == req->type) {

            /* post-check for read access */
            if(NVM_ProtectDataFile(AP_CFG_RDEB_FILE_WIFI_LID, 0) != 1) {
                printf("[META_WIFI] NVM_ProtectDataFile(): get failed\n");
                goto exit;
            }

            iFD = open(kFileInfo.cFileName, O_RDONLY, S_IRUSR);
            if(iFD) {
                lseek(iFD, pnvram->dataOffset, SEEK_SET);
                read(iFD, pnvram->data, pnvram->dataLen);
                close(iFD);

                ret = 0;
            }
        }
        else if(WIFI_CMD_NVRAM_WRITE_ACCESS == req->type) {
            iFD = open(kFileInfo.cFileName, O_WRONLY|O_CREAT, S_IRUSR);
            if(iFD) {
                lseek(iFD, pnvram->dataOffset, SEEK_SET);
                write(iFD, pnvram->data, pnvram->dataLen);
                close(iFD);

                /* invoke protect data file mechanism */
            	if(NVM_ProtectDataFile(AP_CFG_RDEB_FILE_WIFI_LID, 1) != 1) {
		        	printf("[META_WIFI] NVM_ProtectDataFile(): set failed\n");
                    ret = -1;
                }
                else {
                    // invoke auto backup mechanism
                    NVM_AddBackupFileNum(AP_CFG_RDEB_FILE_WIFI_LID);

                    ret = 0;
                }
            }
        }

        if (ret == 0 && WIFI_CMD_NVRAM_READ_ACCESS == req->type) {
            ret_buf = (void *)pnvram;
            ret_size = offsetof(NVRAM_ACCESS_STRUCT, data) + pnvram->dataLen;
        }
    }

exit:
    memset(&cnf, 0, sizeof(FT_WM_WIFI_CNF));
    cnf.header.token = req->header.token;
    cnf.header.id    = FT_WIFI_CNF_ID;
    cnf.type         = req->type;
    cnf.status       = META_SUCCESS;

    /* CHECKME!! Need to confirm the value of drv_status */
    cnf.drv_status   = ret == 0 ? (long)true : (long)false;

    wifi_send_resp(&cnf, ret_buf, ret_size);
    FREEIF(allocated_buf);

    return;
}

