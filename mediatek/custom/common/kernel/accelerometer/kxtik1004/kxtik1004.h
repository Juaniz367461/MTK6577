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

/* linux/drivers/hwmon/adxl345.c
 *
 * (C) Copyright 2008 
 * MediaTek <www.mediatek.com>
 *
 * KXTIK1004 driver for MT6575
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA  KXTIK
 */
#ifndef KXTIK1004_H
#define KXTIK1004_H
	 
#include <linux/ioctl.h>
	 
#define KXTIK1004_I2C_SLAVE_ADDR		0x1E
	 
 /* KXTIK1004 Register Map  (Please refer to KXTIK1004 Specifications) */
#define KXTIK1004_REG_DEVID			0x0F
#define	KXTIK1004_REG_BW_RATE			0x21
#define KXTIK1004_REG_POWER_CTL		0x1B
#define KXTIK1004_REG_CTL_REG3		0x1D
#define KXTIK1004_DCST_RESP			0x0C
#define KXTIK1004_REG_DATA_FORMAT		0x1B
#define KXTIK1004_REG_DATA_RESOLUTION		0x1B
#define KXTIK1004_RANGE_DATA_RESOLUTION_MASK	0x40
#define KXTIK1004_REG_DATAX0			0x06	 
#define KXTIK1004_FIXED_DEVID			0x12	 
#define KXTIK1004_BW_200HZ				0x04
#define KXTIK1004_BW_100HZ				0x03
#define KXTIK1004_BW_50HZ				0x02	 
#define KXTIK1004_MEASURE_MODE		0x80		 
#define KXTIK1004_RANGE_MASK		0x18
#define KXTIK1004_RANGE_2G			0x00
#define KXTIK1004_RANGE_4G			0x08
#define KXTIK1004_RANGE_8G			0x10
#define KXTIK1004_REG_INT_ENABLE	0x1E

#define KXTIK1004_SELF_TEST           0x10
	 	 
	 
#define KXTIK1004_SUCCESS						0
#define KXTIK1004_ERR_I2C						-1
#define KXTIK1004_ERR_STATUS					-3
#define KXTIK1004_ERR_SETUP_FAILURE				-4
#define KXTIK1004_ERR_GETGSENSORDATA			-5
#define KXTIK1004_ERR_IDENTIFICATION			-6
	 
	 
	 
#define KXTIK1004_BUFSIZE				256
	 
#endif

