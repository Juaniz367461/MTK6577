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
#ifndef _ATVSENSOR_DRV_H
#define _ATVSENSOR_DRV_H

#include "sensor_drv.h"

/*******************************************************************************
*
********************************************************************************/
#define ISP_HAL_ATV_PREVIEW_WIDTH (312)
#define ISP_HAL_ATV_PREVIEW_HEIGHT (238)
#define ISP_HAL_ATV_FULL_WIDTH ISP_HAL_ATV_PREVIEW_WIDTH
#define ISP_HAL_ATV_FULLW_HEIGHT ISP_HAL_ATV_PREVIEW_HEIGHT
/*******************************************************************************
*
********************************************************************************/
class AtvSensorDrv : public SensorDrv {
public:
    static SensorDrv* getInstance();

private:
    AtvSensorDrv();
    virtual ~AtvSensorDrv();

public:
    virtual void destroyInstance();

public:

    virtual MINT32 init(MINT32 sensorIdx);
    virtual MINT32 uninit();
    
    virtual MINT32 open();
    virtual MINT32 close();
    
    virtual MINT32 setScenario(ACDK_SCENARIO_ID_ENUM sId);

    virtual MINT32 start();
    virtual MINT32 stop();

	virtual MINT32 waitSensorEventDone( MUINT32 EventType, MUINT32 Timeout) {return SENSOR_ATV;}

    virtual MINT32 getInfo(ACDK_SCENARIO_ID_ENUM ScenarioId,ACDK_SENSOR_INFO_STRUCT *pSensorInfo,ACDK_SENSOR_CONFIG_STRUCT *pSensorConfigData);
    virtual MINT32 getResolution(ACDK_SENSOR_RESOLUTION_INFO_STRUCT *pSensorResolution);

    virtual MINT32 sendCommand(MUINT32 cmd, MUINT32 *parg1 = NULL, MUINT32 *parg2 = NULL, MUINT32 *parg3 = NULL);

    virtual MUINT32 getMainSensorID() const { return 0xF1; }  // Just pick a unique value
    virtual MUINT32 getSubSensorID() const { return 0xFF; }
    virtual IMAGE_SENSOR_TYPE getCurrentSensorType() { return IMAGE_SENSOR_TYPE_YUV; } 
    virtual NSFeature::SensorInfoBase*  getMainSensorInfo() const { return NULL; }
    virtual NSFeature::SensorInfoBase*  getSubSensorInfo()  const { return NULL; }

private:
    virtual MINT32 impSearchSensor(pfExIdChk pExIdChkCbf) { return SENSOR_ATV; }
//js_tst
#if defined (ATVCHIP_MTK_ENABLE)
    MINT32 atvGetDispDelay(); 
#endif 
    
    ACDK_SENSOR_RESOLUTION_INFO_STRUCT  m_SenosrResInfo;
}; 

/*******************************************************************************
*
********************************************************************************/

#endif // _IMGSENSOR_DRV_H

