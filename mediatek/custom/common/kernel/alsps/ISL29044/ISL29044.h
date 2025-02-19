/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

/* 
 *
 * This software is licensed under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation, and
 * may be copied, distributed, and modified under those terms.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */
/*
 * Definitions for ISL29044 als/ps sensor chip.
 */
#ifndef __ISL29044_H__
#define __ISL29044_H__

#include <linux/ioctl.h>

#define ISL29044_I2C_SLAVE_ADDR 0x88 //0x3A<-->SEL to VDD  0x38<-->SEL to GND

#define	POWER_IR_LED			MT65XX_POWER_LDO_VMC

/* registers */
#define ISL29044_REG_VENDOR_REV                 0x06
#define ISL29044_VENDOR                         1
#define ISL29044_VENDOR_MASK                    0x0F
#define ISL29044_REV                            4
#define ISL29044_REV_SHIFT                      4
#define ISL29044_REG_DEVICE                     0x22
#define ISL29044_DEVICE                        	22 

// Table 1: all i2c registers and bits per register
#define REG_CMD_1		0x01 // configure, range is reg 1 bit 1
#define REG_CMD_2		0x02 // interrupt control

#define REG_INT_LOW_PROX	0x03 // 8 bits intr low thresh for prox
#define REG_INT_HIGH_PROX	0x04 // 8 bits intr high thresh for prox
#define REG_INT_LOW_ALS 	0x05 // 8 bits intr low thresh for ALS-IR
#define REG_INT_LOW_HIGH_ALS	0x06 // 8 bits(0-3,4-7) intr high/low thresh for ALS-IR
#define REG_INT_HIGH_ALS	0x07 // 8 bits intr high thresh for ALS-IR

#define REG_DATA_PROX		0x08 // 8 bits of PROX data
#define REG_DATA_LSB_ALS	0x09 // 8 bits of ALS data
#define REG_DATA_MSB_ALS	0x0A // 4 bits of ALS MSB data

#define ISL_TEST1 		0x0E // test write 0x00
#define ISL_TEST2 		0x0F // test write 0x00

#define ISL_MOD_MASK		0xE0
#define ISL_MOD_POWERDOWN	0

#define ISL_MOD_ALS_ONCE	1
#define ISL_MOD_IR_ONCE		2
#define ISL_MOD_PS_ONCE		3
#define ISL_MOD_RESERVED	4
#define ISL_MOD_ALS_CONT	5
#define ISL_MOD_IR_CONT		6
#define ISL_MOD_PS_CONT		7
#define ISL_MOD_DEFAULT		8

#define PROX_EN_MASK          0x80 // prox sense on mask, 1=on, 0=off
#define PROX_CONT_MASK        0x70 // prox sense contimnuous mask
//IR_CURRENT_MASK is now PROX_DR_MASK with just 0 or 1 settings
#define PROX_DR_MASK          0x08 // prox drive pulse 220ma sink mask def=0 110ma
#define ALS_EN_MASK           0x04 // prox sense enabled contimnuous mask
#define ALS_RANGE_HIGH_MASK   0x02 // ALS range high LUX mask
#define ALSIR_MODE_SPECT_MASK 0x01 // prox sense contimnuous mask

#define IR_CURRENT_MASK		0xC0
#define IR_FREQ_MASK		0x30
#define SENSOR_RANGE_MASK	0x03
#define ISL_RES_MASK		0x0C






#endif

