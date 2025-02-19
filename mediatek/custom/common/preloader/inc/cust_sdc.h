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

#ifndef __CUST_SDC_H__
#define __CUST_SDC_H__

#include <mt65xx_typedefs.h>

#define CONFIG_MMC

/* storage boot type definitions */
#define NON_BOOTABLE        (0)
#define RAW_BOOT            (1)

#define MSDC_CD_PIN_EN      (1 << 0)  /* card detection pin is wired   */
#define MSDC_WP_PIN_EN      (1 << 1)  /* write protection pin is wired */
#define MSDC_RST_PIN_EN     (1 << 2)  /* emmc reset pin is wired       */
#define MSDC_SDIO_IRQ       (1 << 3)  /* use internal sdio irq (bus)   */
#define MSDC_EXT_SDIO_IRQ   (1 << 4)  /* use external sdio irq         */
#define MSDC_REMOVABLE      (1 << 5)  /* removable slot                */
#define MSDC_SYS_SUSPEND    (1 << 6)  /* suspended by system           */
#define MSDC_HIGHSPEED      (1 << 7)  /* high-speed mode support       */

/* configure the output driving capacity and slew rate */
#define MSDC_ODC_4MA        (0x0)
#define MSDC_ODC_8MA        (0x2)
#define MSDC_ODC_12MA       (0x4)
#define MSDC_ODC_16MA       (0x6)
#define MSDC_ODC_SLEW_FAST  (0)
#define MSDC_ODC_SLEW_SLOW  (1)

#define MSDC_CMD_PIN        (0)
#define MSDC_DAT_PIN        (1)
#define MSDC_CD_PIN         (2)
#define MSDC_WP_PIN         (3)
#define MSDC_RST_PIN        (4)

enum {
    MSDC_CLKSRC_98MHZ = 0,  /* 98.3MHz */
    MSDC_CLKSRC_81MHZ = 1,  /* 81.9MHz */
    MSDC_CLKSRC_70MHZ = 2,  /* 70.2MHz */
    MSDC_CLKSRC_61MHZ = 3   /* 61.4MHz */
};

enum {
    MSDC_PULL_RES_NONE = 0, /* No pull up/down resister */
    MSDC_PULL_RES_47K  = 2, /* 47k pull up/down resister */
    MSDC_PULL_RES_23K  = 3, /* 23.5k pull up/down resister */
};

enum {
    EDGE_RISING  = 0,
    EDGE_FALLING = 1
};

struct msdc_hw {
    unsigned char  clk_src;          /* host clock source */
    unsigned char  cmd_edge;         /* command latch edge */
    unsigned char  data_edge;        /* data latch edge */
    unsigned char  cmd_odc;          /* command driving capability */
    unsigned char  data_odc;         /* data driving capability */
    unsigned char  cmd_slew_rate;    /* command slew rate */
    unsigned char  data_slew_rate;   /* data slew rate */
    unsigned char  cmd_pull_res;     /* command pin pull resistor */
    unsigned char  dat_pull_res;     /* data pin pull resistor */
    unsigned char  clk_pull_res;     /* clk pin pull resistor */
    unsigned char  rst_wp_pull_res;  /* reset/wp pin pull resistor */
    unsigned char  padding;          /* unused padding */
    unsigned int   flags;            /* hardware capability flags */
    unsigned int   data_pins;        /* data pins */
    unsigned int   data_offset;      /* data address offset */
    unsigned int   boot_type;        /* boot type */

    /* config gpio pull mode */
    void (*config_gpio_pin)(int type, int pull);

    /* external power control for card */
    void (*ext_power_on)(void);
    void (*ext_power_off)(void);

    /* extern cd irq */
    void (*enable_cd_eirq)(void);
    void (*disable_cd_eirq)(void);
    int  (*get_cd_status)(void);
};

extern struct msdc_hw *cust_msdc_hw[];

#endif /* __CUST_SDC_H__ */
