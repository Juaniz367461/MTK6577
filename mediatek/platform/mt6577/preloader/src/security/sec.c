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

/* for general operations */
#include "sec_platform.h"

/* import customer configuration */
#include "sec_cust.h"

/* import sec cfg partition info */
#include "sec_rom_info.h"

/* import secro image info */
#include "sec_secroimg.h"

/* customer key */
#include "cust_sec_ctrl.h"
#include "KEY_IMAGE_AUTH.h"
#include "KEY_SML_ENCODE.h"

/* for crypto operations */
#include "sec.h"
#include "sec_error.h"

/* for storage device operations */
#include "cust_bldr.h"

/**************************************************************************
*  MACRO
**************************************************************************/ 
#define MOD                             "SEC"

/**************************************************************************
 * DEBUG
 **************************************************************************/ 
#define SEC_DEBUG                       (FALSE)
#define SMSG                            print
#if SEC_DEBUG
#define DMSG                            print
#else
#define DMSG 
#endif

/**************************************************************************
 *  GLOBAL VARIABLES
 **************************************************************************/

/**************************************************************************
 *  LOCAL VARIABLES
 **************************************************************************/
SECURE_CFG_INFO                         sec_cfg_info;

/**************************************************************************
 *  EXTERNAL VARIABLES
 **************************************************************************/
extern AND_ROMINFO_T                    g_ROM_INFO;

/**************************************************************************
 *  EXTERNAL FUNCTIONS
 **************************************************************************/
 
U8* sec_cfg_load (void)
{
    U32 i       = 0;
    U8 *buf     = (U8*)SEC_WORKING_BUFFER_START;

    blkdev_t    *bootdev = NULL;


    /* --------------------- */
    /* initialize buffer     */
    /* --------------------- */

    memset(buf, 0x0, SEC_WORKING_BUFFER_LENGTH);

    /* --------------------- */
    /* read sec cfg          */
    /* --------------------- */

    SMSG("\n\n[%s] read '0x%x'\n",MOD,sec_cfg_info.addr);

    if (NULL == (bootdev = blkdev_get(CFG_BOOT_DEV))) 
    {
        SMSG("[%s] can't find boot device(%d)\n", MOD, CFG_BOOT_DEV);
        ASSERT(0);
    }

    blkdev_read(bootdev, sec_cfg_info.addr, SEC_CFG_READ_SIZE, (u8*)buf);

    /* dump first 8 bytes for debugging */
    for(i=0;i<8;i++)
        SMSG("0x%x,",buf[i]);
    SMSG("\n");
    
    return buf;
}

/**************************************************************************
 * [SECURE LIBRARY INITIALIZATION] 
 **************************************************************************/
void sec_lib_init (void)
{

#if SEC_ENV_ENABLE

    part_t *part;
    U32 err;
    CUSTOM_SEC_CFG cust_cfg;
    BOOL bAC = g_ROM_INFO.m_SEC_CTRL.m_seccfg_ac_en;

    /* ---------------------- */
    /* check status           */
    /* ---------------------- */

    /* check customer configuration data structure */
    COMPILE_ASSERT(CUSTOM_SEC_CFG_SIZE == sizeof(CUSTOM_SEC_CFG));


    /* ---------------------- */
    /* initialize variables   */
    /* ---------------------- */
    
    /* initialize customer configuration buffer */
    memset (&cust_cfg, 0x0, sizeof(cust_cfg));

    /* initialize customer configuration for security library */
    cust_cfg.sec_usb_dl = SEC_USBDL_CFG;
    cust_cfg.sec_boot = SEC_BOOT_CFG;
    memcpy (cust_cfg.img_auth_rsa_n, IMG_CUSTOM_RSA_N, sizeof(cust_cfg.img_auth_rsa_n));    
    memcpy (cust_cfg.img_auth_rsa_e, IMG_CUSTOM_RSA_E, sizeof(cust_cfg.img_auth_rsa_e));   
    memcpy (cust_cfg.crypto_seed, CUSTOM_CRYPTO_SEED, sizeof(cust_cfg.crypto_seed));        

    /* ---------------------- */
    /* check data structure   */
    /* ---------------------- */
    
    sec_rom_info_init();
    sec_key_init();
    sec_ctrl_init();   
    sec_flashtool_cfg_init();
    
    /* ---------------------- */
    /* find sec cfg part info */
    /* ---------------------- */

    /* check if sec cfg is defined in partition table */
    part = part_get (PART_SECURE);

    if (!part)
    {
        SMSG ("[%s] part not found\n", MOD);
        ASSERT (0);
    }

    /* apply the rom info's sec cfg part info since tool also refer to this setting*/
    sec_cfg_info.addr = (unsigned int) g_ROM_INFO.m_sec_cfg_offset;
    sec_cfg_info.len = (unsigned int) g_ROM_INFO.m_sec_cfg_length;

    /* ---------------------- */
    /* initialize library     */
    /* ---------------------- */

    SMSG ("[%s] AES Legacy : %d\n", MOD,g_ROM_INFO.m_SEC_CTRL.m_sec_aes_legacy);
    SMSG ("[%s] SECCFG AC : %d\n", MOD,bAC);
#if !CFG_FPGA_PLATFORM
    /* starting to initialze security library */
    if(SEC_OK != (err = seclib_init (&cust_cfg, sec_cfg_load(), SEC_CFG_READ_SIZE, TRUE, bAC)))
    {
        SMSG("[%s] init fail '0x%x'\n",MOD,err);
        ASSERT (0);
    }
#endif

#else
    /* ROM_INFO must be linked even though SEC_ENV_ENABLE=0. 
     * Therefore, we refer to ROM_INFO to make sure it's linked.
     */
    g_ROM_INFO.m_SEC_CTRL.reserve[0] = 0;
#endif

}

BOOL is_BR_cmd_disabled(void)
{
    U32 addr = 0;
    u8 b_disable = 0;

    addr = &g_ROM_INFO;
    addr = addr & 0xFFFFF000;
    addr = addr - 0x300;
       
    if ((TRUE == seclib_sec_usbdl_enabled(TRUE))
        && (SEC_OK == seclib_read_sec_cmd_cfg(addr, 0x300 ,&b_disable)))
    {                                               
        if (b_disable)
        {
            SMSG("[%s] BR cmd is disabled\n", MOD); 
            return TRUE;                 
        }
    }

    return FALSE;
}


