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
#ifndef _SENSOR_DRV_H
#define _SENSOR_DRV_H


#include "kd_imgsensor_define.h"

/*******************************************************************************
*
********************************************************************************/
typedef unsigned int    MUINT32;
typedef signed int      MINT32;

//////////////////////////////////////////////////////////////////////////
//
//  Sensor Feature 
//
//////////////////////////////////////////////////////////////////////////
typedef enum {
    CMD_SENSOR_SET_EXP_TIME = 0x1000, 
    CMD_SENSOR_SET_EXP_LINE,
    CMD_SENSOR_SET_GAIN,
    CMD_SENSOR_SET_FRAME_RATE,
    CMD_SENSOR_SET_YUV_CMD,
    CMD_SENSOR_SET_VIDEO_MODE,
    CMD_SENSOR_SET_SENSOR_SYNC,
    CMD_SENSOR_CCT_FEATURE_CONTROL,
    CMD_SENSOR_SET_CALIBRATION_DATA,
    CMD_SENSOR_GET_PIXEL_CLOCK_FREQ,
    CMD_SENSOR_GET_PIXEL_PERIOD,
    CMD_SENSOR_SET_FLICKER_FRAME_RATE,
    CMD_SENSOR_SET_ESHUTTER_GAIN,
    CMD_SENSOR_GET_INDATA_FORMAT = 0x2000,
    CMD_SENSOR_GET_ATV_DISP_DELAY,
    CMD_SENSOR_GET_DELAY_FRAME_CNT, 
    CMD_SENSOR_GET_PAD_PCLK_INV, 
    CMD_SENSOR_SINGLE_FOCUS_MODE = 0x3000,
    CMD_SENSOR_CANCEL_AF,
    CMD_SENSOR_GET_AF_STATUS,
    CMD_SENSOR_CONSTANT_AF,
    CMD_SENSOR_SET_AF_WINDOW,       
    CMD_SENSOR_GET_EV_AWB_REF,
    CMD_SENSOR_GET_SHUTTER_GAIN_AWB_GAIN,
    CMD_SENSOR_GET_AF_MAX_NUM_FOCUS_AREAS,
    CMD_SENSOR_GET_AE_MAX_NUM_METERING_AREAS,
    CMD_SENSOR_SET_AE_WINDOW,
    CMD_SENSOR_GET_EXIF_INFO,
    CMD_SENSOR_CHECK_ALIVE,
    CMD_SENSOR_MAX = 0xFFFF
} CMD_SENSOR_ENUM;

typedef enum {
	WAIT_SENSOR_SET_SHUTTER_GAIN_DONE = 0x0,
	WAIT_SENSOR_EVENT_MAX = 0xFFFF
}WAIT_SENSOR_EVENT_ENUM;

/*******************************************************************************
*
********************************************************************************/
typedef enum {
    IMAGE_SENSOR_TYPE_RAW, 
    IMAGE_SENSOR_TYPE_YUV, 
    IMAGE_SENSOR_TYPE_YCBCR, 
    IMAGE_SENSOR_TYPE_RGB565, 
    IMAGE_SENSOR_TYPE_UNKNOWN = 0xFFFF,
} IMAGE_SENSOR_TYPE; 

typedef enum {
    SENSOR_NONE = 0x00,
    SENSOR_MAIN = 0x01,
    SENSOR_SUB  = 0x02,
    SENSOR_ATV  = 0x04,
} SENSOR_DEV_ENUM;

typedef enum {
    SENSOR_NO_ERROR         = 0,            ///< The function work successfully
    SENSOR_UNKNOWN_ERROR    = 0x80000000,   ///< Unknown error    
    SENSOR_INVALID_DRIVER   = 0x80000001,
    SENSOR_NO_SENSOR        = 0x80000002,
    SENSOR_INVALID_SENSOR   = 0x80000003, 
    SENSOR_INVALID_PARA     = 0x80000004, 
} SENSOR_ERROR_ENUM;

/*******************************************************************************
*
********************************************************************************/
namespace NSFeature
{
    class SensorInfoBase;
};  //NSFeature

typedef MINT32 (*pfExIdChk)(void);

class SensorDrv {
public:
    //
    static SensorDrv* createInstance(MINT32 sensorDev);
    virtual void      destroyInstance() = 0;

protected:
    virtual ~SensorDrv() {};

public:
    virtual MINT32 init(MINT32 sensorIdx) = 0;
    virtual MINT32 uninit() = 0;
    
    static MINT32 searchSensor(pfExIdChk pExIdChkCbf);
    virtual MINT32 open() = 0;
    virtual MINT32 close()= 0;
    
    virtual MINT32 setScenario(ACDK_SCENARIO_ID_ENUM sId) = 0;
    
    virtual MINT32 start() = 0;
    virtual MINT32 stop() = 0;

	virtual MINT32 waitSensorEventDone( MUINT32 EventType, MUINT32 Timeout)= 0;

    virtual MINT32 getInfo(ACDK_SCENARIO_ID_ENUM ScenarioId,ACDK_SENSOR_INFO_STRUCT *pSensorInfo,ACDK_SENSOR_CONFIG_STRUCT *pSensorConfigData) = 0;
    virtual MINT32 getResolution(ACDK_SENSOR_RESOLUTION_INFO_STRUCT *pSensorResolution) = 0;

    virtual MINT32 sendCommand(MUINT32 cmd, MUINT32 *parg1 = NULL, MUINT32 *parg2 = NULL, MUINT32 *parg3 = NULL) = 0;

    virtual MUINT32 getMainSensorID() const = 0;
    virtual MUINT32 getSubSensorID() const = 0;
    virtual IMAGE_SENSOR_TYPE getCurrentSensorType() = 0; 
    virtual NSFeature::SensorInfoBase*  getMainSensorInfo() const = 0;
    virtual NSFeature::SensorInfoBase*  getSubSensorInfo()  const = 0;

private:
    virtual MINT32 impSearchSensor(pfExIdChk pExIdChkCbf) = 0;
}; 

/*******************************************************************************
*
********************************************************************************/

#endif // _SENSOR_DRV_H

