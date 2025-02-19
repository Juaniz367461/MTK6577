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
#ifndef _CAMERA_CUSTOM_SENSOR_H_
#define _CAMERA_CUSTOM_SENSOR_H_

#include "camera_custom_types.h"
#include "camera_custom_nvram.h"


namespace NSFeature
{


struct FeatureInfoProvider;
class SensorInfoBase
{
public:     ////    Feature Type.
    typedef enum
    {
        EType_RAW =   0,  //  RAW Sensor
        EType_YUV,        //  YUV Sensor
    }   EType;

    typedef NSFeature::FeatureInfoProvider FeatureInfoProvider_T;

public:
    virtual ~SensorInfoBase(){}

public:     //// Interface.
    virtual EType       GetType() const = 0;
    virtual MUINT32     GetID()   const = 0;
    virtual MBOOL       GetFeatureProvider(
        FeatureInfoProvider_T& rFInfoProvider
    ) = 0;

    virtual MUINT32 impGetDefaultData(CAMERA_DATA_TYPE_ENUM const CameraDataType, MVOID*const pDataBuf, MUINT32 const size) const = 0;
};


template <SensorInfoBase::EType _sensor_type, MUINT32 _sensor_id>
class SensorInfo : public SensorInfoBase
{
public:     //// Interface.
    virtual EType       GetType() const { return _sensor_type; }
    virtual MUINT32     GetID()   const { return _sensor_id;   }
};


template <MUINT32 _sensor_id>
class YUVSensorInfo : public SensorInfo<SensorInfoBase::EType_YUV, _sensor_id>
{
public:
    typedef SensorInfoBase::FeatureInfoProvider_T FeatureInfoProvider_T;
    typedef YUVSensorInfo<_sensor_id> SensorInfo_T;

private:    //// Instanciation from outside is disallowed.
    YUVSensorInfo(){}

public:     //// Interface.
    static SensorInfoBase* GetInstance();
    virtual MBOOL GetFeatureProvider(FeatureInfoProvider_T& rFInfoProvider);

    static MUINT32 GetDefaultData(CAMERA_DATA_TYPE_ENUM const CameraDataType, MVOID*const pDataBuf, MUINT32 const size)
    {
        return  GetInstance()->impGetDefaultData(CameraDataType, pDataBuf, size);
    }

    virtual MUINT32 impGetDefaultData(CAMERA_DATA_TYPE_ENUM const CameraDataType, MVOID*const pDataBuf, MUINT32 const size) const;
};


template <MUINT32 _sensor_id>
class RAWSensorInfo : public SensorInfo<SensorInfoBase::EType_RAW, _sensor_id>
{
public:
    typedef SensorInfoBase::FeatureInfoProvider_T FeatureInfoProvider_T;
    typedef RAWSensorInfo<_sensor_id> SensorInfo_T;

private:    //// Instanciation from outside is disallowed.
    RAWSensorInfo(){}

public:     //// Interface.
    static SensorInfoBase* GetInstance()
    {
        static SensorInfo_T singleton;
        return &singleton;
    }
    virtual MBOOL GetFeatureProvider(FeatureInfoProvider_T& rFInfoProvider)
    {
        return MFALSE;
    }

    static MUINT32 GetDefaultData(CAMERA_DATA_TYPE_ENUM const CameraDataType, MVOID*const pDataBuf, MUINT32 const size)
    {
        return  GetInstance()->impGetDefaultData(CameraDataType, pDataBuf, size);
    }

    virtual MUINT32 impGetDefaultData(CAMERA_DATA_TYPE_ENUM const CameraDataType, MVOID*const pDataBuf, MUINT32 const size) const;
};


};  //  NSFeature


typedef struct
{
    MUINT32 SensorId;
    MUINT8  drvname[32];
    NSFeature::SensorInfoBase* (*pfGetSensorInfoInstance)();
    MUINT32 (*getCameraDefault)(CAMERA_DATA_TYPE_ENUM CameraDataType, MVOID *pDataBuf, MUINT32 size);
    MUINT32 (*getCameraCalData)(MUINT32* pGetCalData);
} MSDK_SENSOR_INIT_FUNCTION_STRUCT, *PMSDK_SENSOR_INIT_FUNCTION_STRUCT;

MUINT32 GetSensorInitFuncList(MSDK_SENSOR_INIT_FUNCTION_STRUCT **ppSensorList);


#endif  //  _CAMERA_CUSTOM_SENSOR_H_

