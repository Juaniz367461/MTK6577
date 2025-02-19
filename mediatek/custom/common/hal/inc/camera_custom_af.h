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

#ifndef _AF_COEF_H
#define _AF_COEF_H

#include "MediaTypes.h"

    typedef struct
    {
        MINT32  i4AFS_STEP_MIN_ENABLE;
        MINT32  i4AFS_STEP_MIN_NORMAL;
        MINT32  i4AFS_STEP_MIN_MACRO;
        MINT32  i4AFS_NOISE_LV1;
        MINT32  i4AFS_NOISE_POS_OFFSET1;
        MINT32  i4AFS_NOISE_GAIN1;
        MINT32  i4AFS_NOISE_LV2;
        MINT32  i4AFS_NOISE_POS_OFFSET2;
        MINT32  i4AFS_NOISE_GAIN2;
        MINT32  i4AFS_NOISE_POS_OFFSET3;
        MINT32  i4AFS_NOISE_GAIN3;
        MINT32  i4AFC_FAIL_STOP;
        MINT32  i4AFC_FRAME_MODE;      // 0: for 1 frame, 1: for 2 frame

        MINT32 i4AFC_RESTART_ENABLE;     // restart AFC if AE non stable                   
        MINT32 i4AFC_INIT_STABLE_ENABLE; // for waiting stable in AFC init               
        MINT32 i4FV_1ST_STABLE_ENABLE;   // for get 1st FV             
        MINT32 i4AFC_WAIT;               // AFC wait frame when i4AFC_INIT_STABLE_ENABLE = 0
        
    } AF_COEF_T;

	AF_COEF_T get_AF_Coef();
	
#endif /* _AF_COEF_H */

