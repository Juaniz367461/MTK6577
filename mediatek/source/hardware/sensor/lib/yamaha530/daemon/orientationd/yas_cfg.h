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
 * Copyright (c) 2010 Yamaha Corporation
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

#ifndef __YAS_CFG_H__
#define __YAS_CFG_H__

#define YAS_MAG_DRIVER_YAS529               (1)
#define YAS_MAG_DRIVER_YAS530               (2)

#define YAS_ACC_DRIVER_ADXL345              (0)
#define YAS_ACC_DRIVER_ADXL346              (1)
#define YAS_ACC_DRIVER_BMA150               (2)
#define YAS_ACC_DRIVER_BMA222               (3)
#define YAS_ACC_DRIVER_BMA250               (4)
#define YAS_ACC_DRIVER_KXSD9                (5)
#define YAS_ACC_DRIVER_KXTE9                (6)
#define YAS_ACC_DRIVER_KXTF9                (7)
#define YAS_ACC_DRIVER_KXUD9                (8)
#define YAS_ACC_DRIVER_LIS331DL             (9)
#define YAS_ACC_DRIVER_LIS331DLH            (10)
#define YAS_ACC_DRIVER_LIS331DLM            (11)
#define YAS_ACC_DRIVER_LIS3DH               (12)
#define YAS_ACC_DRIVER_MMA8452Q             (13)
#define YAS_ACC_DRIVER_MMA8453Q             (14)

/*----------------------------------------------------------------------------*/
/*                               Configuration                                */
/*----------------------------------------------------------------------------*/

#define YAS_ACC_DRIVER                      (YAS_ACC_DRIVER_BMA150)
#define YAS_MAG_DRIVER                      (YAS_MAG_DRIVER_YAS530)

/*----------------------------------------------------------------------------*/
/*                   Acceleration Calibration Configuration                   */
/*----------------------------------------------------------------------------*/

#define YAS_DEFAULT_ACCCALIB_LENGTH         (20)

#if YAS_ACC_DRIVER == YAS_ACC_DRIVER_ADXL345
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (8000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_ADXL346
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (4000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_BMA150
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (4000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_BMA222
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (25000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_BMA250
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (20000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXSD9
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (80000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXTE9
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (400000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXTF9
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (2000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXUD9
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (20000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS331DL
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (17000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS331DLH
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (6000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS331DLM
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (28000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS3DH
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (18000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_MMA8452Q
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (1000)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_MMA8453Q
#define YAS_DEFAULT_ACCCALIB_DISTORTION     (1000)
#else
#error "unknown accelerometer"
#endif

#if YAS_ACC_DRIVER == YAS_ACC_DRIVER_ADXL345
#define YAS_ACC_I2C_SLAVEADDR               (0x53)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_ADXL346
#define YAS_ACC_I2C_SLAVEADDR               (0x53)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_BMA150
#define YAS_ACC_I2C_SLAVEADDR               (0x38)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_BMA222
#define YAS_ACC_I2C_SLAVEADDR               (0x08)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_BMA250
#define YAS_ACC_I2C_SLAVEADDR               (0x18)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXSD9
#define YAS_ACC_I2C_SLAVEADDR               (0x18)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXTE9
#define YAS_ACC_I2C_SLAVEADDR               (0x0f)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXTF9
#define YAS_ACC_I2C_SLAVEADDR               (0x0f)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXUD9
#define YAS_ACC_I2C_SLAVEADDR               (0x18)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS331DL
#define YAS_ACC_I2C_SLAVEADDR               (0x1c)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS331DLH
#define YAS_ACC_I2C_SLAVEADDR               (0x18)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS331DLM
#define YAS_ACC_I2C_SLAVEADDR               (0x08)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS3DH
#define YAS_ACC_I2C_SLAVEADDR               (0x18)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_MMA8452Q
#define YAS_ACC_I2C_SLAVEADDR               (0x1c)
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_MMA8453Q
#define YAS_ACC_I2C_SLAVEADDR               (0x1c)
#else
#error "unknown accelerometer"
#endif

/*----------------------------------------------------------------------------*/
/*                     Accelerometer Filter Configuration                     */
/*----------------------------------------------------------------------------*/
#if YAS_ACC_DRIVER == YAS_ACC_DRIVER_ADXL345
#define YAS_ACC_DEFAULT_FILTER_THRESH       (76612)  /* ((38,306 um/s^2)/count) * 2  */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_ADXL346
#define YAS_ACC_DEFAULT_FILTER_THRESH       (76612)  /* ((38,306 um/s^2)/count) * 2  */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_BMA150
#define YAS_ACC_DEFAULT_FILTER_THRESH       (76612)  /* ((38,306 um/s^2)/count) * 2  */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_BMA222
#define YAS_ACC_DEFAULT_FILTER_THRESH       (153227) /* ((153,227 um/s^2)/count) * 1 */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_BMA250
#define YAS_ACC_DEFAULT_FILTER_THRESH       (76612)  /* ((38,306 um/s^2)/count) * 2  */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXSD9
#define YAS_ACC_DEFAULT_FILTER_THRESH       (239460) /* ((11,973 um/s^2)/count) * 20 */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXTE9
#define YAS_ACC_DEFAULT_FILTER_THRESH       (612909) /* ((612,909 um/s^2)/count) * 1 */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXTF9
#define YAS_ACC_DEFAULT_FILTER_THRESH       (19152)  /* ((9,576 um/s^2)/count) * 2   */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_KXUD9
#define YAS_ACC_DEFAULT_FILTER_THRESH       (215514) /* ((11.973 um/s^2)/count * 18  */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS331DL
#define YAS_ACC_DEFAULT_FILTER_THRESH       (176518) /* ((176.518 um/s^2)/count * 1  */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS331DLH
#define YAS_ACC_DEFAULT_FILTER_THRESH       (95760)  /* ((9.576 um/s^2)/count * 10   */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS331DLM
#define YAS_ACC_DEFAULT_FILTER_THRESH       (306454) /* ((153,227 um/s^2)/count * 2  */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_LIS3DH
#define YAS_ACC_DEFAULT_FILTER_THRESH       (76608)  /* ((9,576 um/s^2)/count * 8    */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_MMA8452Q
#define YAS_ACC_DEFAULT_FILTER_THRESH       (19152)  /* ((9.576 um/s^2)/count * 2    */
#elif YAS_ACC_DRIVER == YAS_ACC_DRIVER_MMA8453Q
#define YAS_ACC_DEFAULT_FILTER_THRESH       (38306)  /* ((38,306 um/s^2)/count * 1   */
#else
#error "unknown accelerometer"
#endif

/*----------------------------------------------------------------------------*/
/*                    Geomagnetic Calibration Configuration                   */
/*----------------------------------------------------------------------------*/

#define YAS_DEFAULT_MAGCALIB_THRESHOLD      (1)
#define YAS_DEFAULT_MAGCALIB_DISTORTION     (15)
#define YAS_DEFAULT_MAGCALIB_SHAPE          (0)
#define YAS_MAGCALIB_SHAPE_NUM              (2)

/*----------------------------------------------------------------------------*/
/*                      Geomagnetic Filter Configuration                      */
/*----------------------------------------------------------------------------*/

#define YAS_MAG_MAX_FILTER_LEN              (30)
#define YAS_MAG_I2C_SLAVEADDR               (0x2e)
#define YAS_MAG_DEFAULT_FILTER_NOISE_X      (144) /* sd: 1200 nT */
#define YAS_MAG_DEFAULT_FILTER_NOISE_Y      (144) /* sd: 1200 nT */
#define YAS_MAG_DEFAULT_FILTER_NOISE_Z      (144) /* sd: 1200 nT */
#define YAS_MAG_DEFAULT_FILTER_LEN          (20)

#if YAS_MAG_DRIVER == YAS_MAG_DRIVER_YAS529
#define YAS_MAG_DEFAULT_FILTER_THRESH       (300)
#elif YAS_MAG_DRIVER == YAS_MAG_DRIVER_YAS530
#define YAS_MAG_DEFAULT_FILTER_THRESH       (100)
#else
#error "unknown magnetometer"
#endif

#endif
