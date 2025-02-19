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
#ifndef _MDP_HAL_IMP_H_
#define _MDP_HAL_IMP_H_

#include "mdp_hal.h"
#include "mdp_pipe.h"   //For using MDP Pipe Object



/*******************************************************************************
*
********************************************************************************/
class MdpHalImp : public MdpHal {
public:
    static MdpHal* getInstance();
    virtual void destroyInstance();
//
public:
    MdpHalImp();
    virtual ~MdpHalImp();
//
public:
    MINT32 init();
    //
    MINT32 uninit();
    //
    MINT32 start();
    //
    MINT32 stop();
    //
    MINT32 setPrv(halIDPParam_t *phalIDPParam);
    //
    MINT32 setCapJpg(halIDPParam_t *phalIDPParam);
    //
    MINT32 setConf(halIDPParam_t *phalIDPParam);
    //
    MINT32 getConf(halIDPParam_t *phalIDPParam);
    //
    MINT32 dequeueBuff(halMdpOutputPort_e e_Port , halMdpBufInfo_t * a_pstBuffInfo);
    //
    MINT32 enqueueBuff(halMdpOutputPort_e e_Port);
    //
    MINT32 calCropRect(rect_t rSrc, rect_t rDst, rect_t *prCrop, MUINT32 zoomRatio);
    //
    MINT32 waitDone(MINT32 mode);
    //
    MINT32 dumpReg();
    //
    MINT32 sendCommand(int cmd, int parg1 = NULL, int parg2 = NULL, int parg3 = NULL);
//
private:
    //
    halIDPParam_t mMDPParam;
    //
    MdpPipeCameraPreviewParameter   m_mdp_pipe_camera_preview_param;
    MdpPipeCameraPreview            m_mdp_pipe_camera_preview;

    MdpPathDummyBrz                 m_mdp_path_dummy_brz;   //Use to lock brz to prevent jpg decode when preview

    MdpPipeCameraCapture            m_mdp_pipe_camera_capture;
    //

private:
    void        _MapHalportToMdpId( halMdpOutputPort_e e_Port, unsigned long *p_mdp_id, unsigned long *p_mdp_id_ex );
    const char* _MapHalportToStr( halMdpOutputPort_e e_Port );


private:
    
    

private:
    int SaveBufferImage( int port_num , int frame_skip );

    


    
};

#endif // _MDP_HAL_IMP_H_

