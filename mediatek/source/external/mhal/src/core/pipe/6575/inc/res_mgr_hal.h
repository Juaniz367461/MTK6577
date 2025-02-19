/********************************************************************************************
 *     LEGAL DISCLAIMER
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED,
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION,
TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#ifndef RES_MGR_HAL_H
#define RES_MGR_HAL_H
//----------------------------------------------------------------------------
#include "pipe_types.h"
//----------------------------------------------------------------------------
typedef enum
{
    RES_MGR_HAL_MODE_NONE,
    RES_MGR_HAL_MODE_PREVIEW_OFF,
    RES_MGR_HAL_MODE_PREVIEW_ON,
    RES_MGR_HAL_MODE_CAPTURE_OFF,
    RES_MGR_HAL_MODE_CAPTURE_ON,
    RES_MGR_HAL_MODE_VIDEO_REC_OFF,
    RES_MGR_HAL_MODE_VIDEO_REC_ON
}RES_MGR_HAL_MODE_ENUM;

typedef enum
{
    RES_MGR_HAL_MODE_SUB_NONE,
    RES_MGR_HAL_MODE_SUB_ISP2MEM
}RES_MGR_HAL_MODE_SUB_ENUM;

typedef enum
{
    RES_MGR_HAL_DEV_NONE,
    RES_MGR_HAL_DEV_CAM,
    RES_MGR_HAL_DEV_ATV,
    RES_MGR_HAL_DEV_VT
}RES_MGR_HAL_DEV_ENUM;

typedef struct
{
    RES_MGR_HAL_MODE_ENUM       Mode;
    RES_MGR_HAL_MODE_SUB_ENUM   ModeSub;
    RES_MGR_HAL_DEV_ENUM        Dev;
}RES_MGR_HAL_MODE_STRUCT;
//----------------------------------------------------------------------------
class ResMgrHal 
{
    protected:
        virtual ~ResMgrHal() {};
    //
    public:
        static ResMgrHal* CreateInstance(void);
        virtual void    DestroyInstance(void) = 0;
        virtual MBOOL   Init(void) = 0;
        virtual MBOOL   Uninit(void) = 0;
        virtual MBOOL   SetMode(RES_MGR_HAL_MODE_STRUCT* pModeInfo) = 0;
        virtual MBOOL   LockMdpBrz(MBOOL Lock) = 0;
};
//----------------------------------------------------------------------------
#endif


