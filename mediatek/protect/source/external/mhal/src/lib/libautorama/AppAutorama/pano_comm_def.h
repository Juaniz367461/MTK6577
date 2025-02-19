/*******************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2010
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*******************************************************************************/

/*******************************************************************************
 * Filename:
 * ---------
 *  pano_comm_def.h
 *
 * Project:
 * --------
 *   MT6236
 *
 * Description:
 * ------------
 *   This file is intends for Panorama Control Exposed Interface.
 *
 * Author:
 * -------
 *   Eric Liu (mtk03608)
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Log$
 *
 * 04 26 2011 eric.liu
 * [MAUI_xxxxxxxx][HAL PostProc] Rearrange PP interface and 
 * assign the working memory size
 *
 * 02 18 2011 shouchun.liao
 * [MAUI_02871948] [HAL PostProc] Rearrange PP interface
 * .
 *
 * 02 11 2011 shouchun.liao
 * [MAUI_02841005] [Camera HAL] Camera HAL first version check-in
 * Remove PP_FEATURE_SAVE_LOG
 *
 * 01 05 2011 shouchun.liao
 * [MAUI_02841005] [Camera HAL] Camera HAL first version check-in
 * Revise PP save log interface.
 *
 * 12 30 2010 shouchun.liao
 * [MAUI_02841005] [Camera HAL] Camera HAL first version check-in
 * revise pano code
 *
 * 12 28 2010 shouchun.liao
 * [MAUI_02841005] [Camera HAL] Camera HAL first version check-in
 * Check-in PP save log interface
 *
 * 12 08 2010 shouchun.liao
 * [MAUI_02841005] [Camera HAL] Camera HAL first version check-in
 * Revise PPI interface
 *
 * 12 02 2010 shouchun.liao
 * [MAUI_02841005] [Camera HAL] Camera HAL first version check-in
 * .revise pano set env info interface
 *
 * 11 30 2010 shouchun.liao
 * [MAUI_02841005] [Camera HAL] Camera HAL first version check-in
 * .Revise PPI & Panorama control
 *
 * 11 23 2010 shouchun.liao
 * [MAUI_02841005] [Camera HAL] Camera HAL first version check-in
 * [HAL] Post Process Check-in
 *
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/
#ifndef __PANO_COMM_DEF_H__
#define __PANO_COMM_DEF_H__

#include "kal_release.h"
#include "mm_comm_def.h"

/****************************************************************************
 *          Panorama Stitch (__PANORAMA_VIEW_SUPPORT__)
 ****************************************************************************/

//#define PANO_LOG_DEBUG_INFO
#ifdef PANO_LOG_DEBUG_INFO
#define PANO_LOG_BUFFER_SIZE				(25024) //32K BYTES
#else
#define PANO_LOG_BUFFER_SIZE				(0) //BYTES
#endif

#define PANO_IMG_WIDTH              (640)
#define PANO_IMG_HEIGHT             (480)
#define PANO_MAX_OUT_IMG_WIDTH      (3800)
#define PANO_ADD_IMAGE_BUFFER_SIZE  ((((PANO_IMG_WIDTH*PANO_IMG_HEIGHT/2*4)+31)>>5)<<5) //w*h*overlap_ratio*4
#define PANO_STITCH_BUFFER_SIZE	    ((((PANO_MAX_OUT_IMG_WIDTH*PANO_IMG_HEIGHT*3/2)+31)>>5)<<5) //max_w*h*1.5


typedef enum
{   
    PANO_DIR_RIGHT=0,
    PANO_DIR_LEFT,
    PANO_DIR_UP,
    PANO_DIR_DOWN,
    PANO_DIR_NO
} PANO_DIRECTION_ENUM;

/* PANO_FEATURE_SET_INFO (for CAL) */
typedef struct
{
    kal_uint16  SrcImgWidth;
    kal_uint16  SrcImgHeight;
    MM_IMAGE_FORMAT_ENUM SrcImgFormat;
    PANO_DIRECTION_ENUM StitchDirection;
} PANO_SET_INFO_STRUCT, *P_PANO_SET_INFO_STRUCT;

typedef struct
{
    kal_uint32  ImgBufferAddr;                  //output image buffer address
    kal_uint16  ImgWidth;                       //output image width
    kal_uint16  ImgHeight;                      //output image height
    MM_IMAGE_FORMAT_ENUM  ImgFormat;            //output image format, currently only YUV420 is supported
} PANO_RESULT_STRUCT, *P_PANO_RESULT_STRUCT;

#endif /* __PANO_COMM_DEF_H__ */
