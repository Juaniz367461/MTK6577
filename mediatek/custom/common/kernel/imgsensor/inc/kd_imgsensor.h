#ifndef _KD_IMGSENSOR_H
#define _KD_IMGSENSOR_H

#include <linux/ioctl.h>

#ifndef ASSERT
    #define ASSERT(expr)        BUG_ON(!(expr))
#endif

#define IMGSENSORMAGIC 'i'
//IOCTRL(inode * ,file * ,cmd ,arg )
//S means "set through a ptr"
//T means "tell by a arg value"
//G means "get by a ptr"
//Q means "get by return a value"
//X means "switch G and S atomically"
//H means "switch T and Q atomically"

/*******************************************************************************
*
********************************************************************************/

/*******************************************************************************
*
********************************************************************************/

//sensorOpen
//This command will TBD
#define KDIMGSENSORIOC_T_OPEN            _IO(IMGSENSORMAGIC,0)
//sensorGetInfo
//This command will TBD
#define KDIMGSENSORIOC_X_GETINFO            _IOWR(IMGSENSORMAGIC,5,ACDK_SENSOR_GETINFO_STRUCT)
//sensorGetResolution
//This command will TBD
#define KDIMGSENSORIOC_X_GETRESOLUTION      _IOWR(IMGSENSORMAGIC,10,ACDK_SENSOR_RESOLUTION_INFO_STRUCT)
//sensorFeatureControl
//This command will TBD
#define KDIMGSENSORIOC_X_FEATURECONCTROL    _IOWR(IMGSENSORMAGIC,15,ACDK_SENSOR_FEATURECONTROL_STRUCT)
//sensorControl
//This command will TBD
#define KDIMGSENSORIOC_X_CONTROL            _IOWR(IMGSENSORMAGIC,20,ACDK_SENSOR_CONTROL_STRUCT)
//sensorClose
//This command will TBD
#define KDIMGSENSORIOC_T_CLOSE            _IO(IMGSENSORMAGIC,25)
//set sensor driver
#define KDIMGSENSORIOC_X_SET_DRIVER         _IOWR(IMGSENSORMAGIC,40,u32)
//sensorSearch 
#define KDIMGSENSORIOC_T_CHECK_IS_ALIVE     _IO(IMGSENSORMAGIC, 30) 
//Set Shutter Gain Wait Done
#define KDIMGSENSORIOC_X_SET_SHUTTER_GAIN_WAIT_DONE   _IOWR(IMGSENSORMAGIC,50,u32)


/*******************************************************************************
*
********************************************************************************/
/* SENSOR CHIP VERSION */
#define MC501CB_SENSOR_ID                       0x0062
#define MC501CC_SENSOR_ID                       0x0074
#define MC501CA_SENSOR_ID                       0x0011
#define HI542MIPI_SENSOR_ID                         0x00B1

#define MT9D011_SENSOR_ID                       0x1511
#define MT9D111_SENSOR_ID                       0x1511
#define MT9D112_SENSOR_ID                       0x1580
#define MT9M011_SENSOR_ID                       0x1433
#define MT9M111_SENSOR_ID                       0x143A
#define MT9M112_SENSOR_ID                       0x148C
#define MT9M113_SENSOR_ID                       0x2480
#define MT9P012_SENSOR_ID						0x2800
#define MT9P012_SENSOR_ID_REV7					0x2801
#define MT9T012_SENSOR_ID                       0x1600
#define MT9T013_SENSOR_ID                       0x2600
#define MT9T113_SENSOR_ID                       0x4680
#define MT9V112_SENSOR_ID                       0x1229
#define MT9DX11_SENSOR_ID                       0x1519
#define MT9D113_SENSOR_ID                       0x2580

#define NOON200PC11_SENSOR_ID                   0x0013
#define NOON200PC20_SENSOR_ID                   0x0063
#define NOON200PC40_SENSOR_ID                   0x0063
#define NOON200PC51_SENSOR_ID                   0x006C
#define NOON130PC51_SENSOR_ID                   0x0076

#define HM3451_SENSOR_ID						0x3451

#define OV2630_SENSOR_ID                        0x2633
#define OV2640_SENSOR_ID                        0x2642
#define OV2650_SENSOR_ID                        0x2652
#define OV3640_SENSOR_ID                        0x364C
#define OV6680_SENSOR_ID                        0x6681
#define OV7660_SENSOR_ID                        0x7660
#define OV7670_SENSOR_ID                        0x7673
#define OV7680_SENSOR_ID                        0x7680
#define OV9650_SENSOR_ID                        0x9652
#define OV9655_SENSOR_ID                        0x9657
#define OV9660_SENSOR_ID                        0x9663
#define OV3647_SENSOR_ID                        0x364A
#define OV2655_SENSOR_ID					    0x2656
#define OV2659_SENSOR_ID					    0x2656
#define OV2650_SENSOR_ID_1                      0x2651
#define OV2650_SENSOR_ID_2                      0x2652
#define OV2650_SENSOR_ID_3			            0x2655
#define OV5640_SENSOR_ID						0x5640
#define OV5642_SENSOR_ID            0x5642
#define OV5650_SENSOR_ID                        0x5651
#define OV5650MIPI_SENSOR_ID                    0x5651
#define OV9665_SENSOR_ID                        0x9663
#define OV5630_SENSOR_ID                        0x5634
#define OV7675_SENSOR_ID                        0x7673
#define OV5647_SENSOR_ID                        0x5647
#define HI542_SENSOR_ID                         0x00B1
#define OV5647MIPI_SENSOR_ID                        0x5647

#define PO6030K_SENSOR_ID                       0x0060
#define PO4010K_SENSOR_ID                       0x0040

#define SID020A_SENSOR_ID                       0x12B4
#define SIV100B_SENSOR_ID                       0x0C11
#define SIV100A_SENSOR_ID                       0x0C10
#define SIV120A_SENSOR_ID                       0x1210
#define SIV120B_SENSOR_ID                       0x0012
#define SIV121D_SENSOR_ID			0xDE
#define SIM101B_SENSOR_ID                       0x09A0
#define SIM120C_SENSOR_ID                       0x0012
#define SID130B_SENSOR_ID                       0x001b
#define SIC110A_SENSOR_ID                       0x000D

#define S5K8AAYX_SENSOR_ID                      0x08AA		//SW4-L1-HL-Camera-S1UMainCamFrontCamBringUp-00+_20120705


#define S5KA3DFX_SENSOR_ID                      0x00AB
#define S5K4B2FX_SENSOR_ID                      0x5080
#define S5K3AAEA_SENSOR_ID                      0x07AC
#define S5K3BAFB_SENSOR_ID                      0x7070
#define S5K53BEX_SENSOR_ID                      0x45A8
#define S5K53BEB_SENSOR_ID                      0x87A8
#define S5K83AFX_SENSOR_ID                      0x01C4
#define S5K5BAFX_SENSOR_ID                      0x05BA
#define S5K3E2FX_SENSOR_ID                      0x3E2F
#define S5K4E1GA_SENSOR_ID                      0x4E10
#define S5K5CAGX_SENSOR_ID                      0x05ca
#define S5K4ECGX_SENSOR_ID                        0x4EC0
#define S5K4E1FX_SENSOR_ID                      0x4E10
#define S5K5CAGXMIPI_SENSOR_ID                      0x05ca
#define S5K3H2YX_SENSOR_ID                      0x382b

#define PAS105_SENSOR_ID                        0x0065
#define PAS302_SENSOR_ID                        0x0064
#define PAS5101_SENSOR_ID                       0x0067
#define PAS6180_SENSOR_ID                       0x6179

#define ET8EE6_SENSOR_ID                        0x0034
#define ET8EF2_SENSOR_ID                        0x1048

#define OM6802_SENSOR_ID                        0x1705

#define HV7131_SENSOR_ID                        0x0042

#define RJ53S1BA0C_SENSOR_ID                    0x0129

#define HI251_SENSOR_ID                         0x0084
#define HIVICF_SENSOR_ID                        0x0081
#define HI253_SENSOR_ID                         0x0092
#define HI704_SENSOR_ID                         0x0096

#define IMX058_SENSOR_ID                        0x0058
#define A8141_SENSOR_ID                        0x4B00
#define IMX073_SENSOR_ID                        0x0046
#define SIV120B_SENSOR_ID                       0x0012
#define MT9V113_SENSOR_ID                         0x2280
#define MT9V114_SENSOR_ID                         0x2283
#define MT9V115_SENSOR_ID						  0x2284
#define MT9M114_SENSOR_ID                       0x2481
#define MT9P015_SENSOR_ID                       0x2803
#define MT9P017_SENSOR_ID                       0x4800
#define MT9P017MIPI_SENSOR_ID                       0x4800
#define SHARP3D_SENSOR_ID                       0x003d
#define MT9T113MIPI_SENSOR_ID                       0x4680
#define IMX105_SENSOR_ID                        0x0105
#define OV8830_SENSOR_ID			  0x8830
#define OV9726_SENSOR_ID                          0x9726
#define A5141MIPI_SENSOR_ID                     0x4800
#define A5142MIPI_SENSOR_ID                     0x4800
#define OV7692_SENSOR_ID            0x7692
#define OV7690_SENSOR_ID                        0x7691


// Added by chu, zewei on 2012/11/23
#define GC0308_SENSOR_ID						  0x009B
#define GC0309_SENSOR_ID                        0x00A0
#define GC0311_SENSOR_ID                        0x00BB
#define GC0329_SENSOR_ID                        0x00C0
#define GC2035_SENSOR_ID						  0x2035
#define SP2518_SENSOR_ID						  0x0053
#define SP0828_SENSOR_ID						  0x000c

/* CAMERA DRIVER NAME */
#define CAMERA_HW_DEVNAME            "kd_camera_hw"

/* SENSOR DEVICE DRIVER NAME */
#define SENSOR_DRVNAME_MT9P012_RAW  "mt9p012"
#define SENSOR_DRVNAME_MT9P015_RAW  "mt9p015"
#define SENSOR_DRVNAME_MT9P017_RAW  "mt9p017"
#define SENSOR_DRVNAME_MT9P017_MIPI_RAW  "mt9p017mipi"
#define SENSOR_DRVNAME_OV2650_RAW   "ov265x"
#define SENSOR_DRVNAME_OV2655_YUV   "ov2655yuv"
#define SENSOR_DRVNAME_OV2659_YUV   "ov2659yuv"
#define SENSOR_DRVNAME_OV5650_RAW   	"ov5650raw"
#define SENSOR_DRVNAME_OV3640_RAW   "ov3640"
#define SENSOR_DRVNAME_OV3640_YUV    "ov3640yuv"
#define SENSOR_DRVNAME_OV5642_RAW   "ov5642raw"
#define SENSOR_DRVNAME_HI542_RAW   "hi542raw"
#define SENSOR_DRVNAME_HI542MIPI_RAW   "hi542mipiraw"

#define SENSOR_DRVNAME_OV5647MIPI_RAW   	"ov5647mip"
#define SENSOR_DRVNAME_HM3451_RAW				"hm3451raw"

#define SENSOR_DRVNAME_OV5647_RAW   	"ov5647"
#define SENSOR_DRVNAME_A8141_MIPI_RAW   "a8141mipiraw"
#define SENSOR_DRVNAME_OV5640_YUV   	"ov5640_yuv"
#define SENSOR_DRVNAME_OV5642_MIPI_YUV    "ov5642mipiyuv"
#define SENSOR_DRVNAME_OV5642_MIPI_RGB    "ov5642mipirgb"
#define SENSOR_DRVNAME_OV5642_MIPI_JPG     "ov5642mipijpg"
#define SENSOR_DRVNAME_OV5642_YUV   "ov5642yuv"
#define SENSOR_DRVNAME_OV5642_YUV_SWI2C   "ov5642yuvswi2c"
#define SENSOR_DRVNAME_OV5650MIPI_RAW   "ov5650mipiraw"
#define SENSOR_DRVNAME_OV7675_YUV   "ov7675yuv"
#define SENSOR_DRVNAME_IMX073_MIPI_RAW   "imx073mipiraw"
#define SENSOR_DRVNAME_S5K5CAGX_YUV     "s5k5cagxyuv"
#define SENSOR_DRVNAME_S5K4ECGX_YUV     "s5k4ecgxyuv"
#define SENSOR_DRVNAME_SIV120B_YUV    "siv120byuv"
#define SENSOR_DRVNAME_MT9V113_YUV    "mt9v113yuv"
#define SENSOR_DRVNAME_HI253_YUV    	"hi253yuv"
#define SENSOR_DRVNAME_SIV121D_YUV    	"siv121dyuv"
#define SENSOR_DRVNAME_HI704_YUV    	"hi704yuv"
#define SENSOR_DRVNAME_MT9V114_YUV    "mt9v114"
#define SENSOR_DRVNAME_MT9V115_YUV	"mt9v115yuv"
#define SENSOR_DRVNAME_MT9M114_YUV		"mt9m114yuv"
#define SENSOR_DRVNAME_MT9T113_YUV    "mt9t113yuv"
#define SENSOR_DRVNAME_PAS6180_SERIAL_YUV    "pas6180serialyuv"
#define SENSOR_DRVNAME_SHARP3D_MIPI_YUV    "sharp3dmipiyuv"
#define SENSOR_DRVNAME_MT9T113_MIPI_YUV    "mt9t113mipiyuv"
#define SENSOR_DRVNAME_IMX105_MIPI_RAW   "imx105mipiraw"
#define SENSOR_DRVNAME_OV8830_RAW   	       "ov8830"
#define SENSOR_DRVNAME_0V9726_RAW		"ov9726raw"
#define SENSOR_DRVNAME_S5K4E1GA_MIPI_RAW   "s5k4e1gamipiraw"
#define SENSOR_DRVNAME_A5141_MIPI_RAW   "a5141mipiraw"
#define SENSOR_DRVNAME_A5142_MIPI_RAW   "a5142mipiraw"
#define SENSOR_DRVNAME_OV7692_YUV    "ov7692yuv"
#define SENSOR_DRVNAME_OV7690_YUV   	"ov7690yuv"
#define SENSOR_DRVNAME_S5K4E1FX_MIPI_RAW   "s5k4e1fxmipiraw"
#define SENSOR_DRVNAME_S5K5CAGX_MIPI_YUV    "s5k5cagxmipiyuv"

#define SENSOR_DRVNAME_S5K3H2YX_MIPI_RAW   "s5k3h2yxmipiraw"
#define SENSOR_DRVNAME_S5K8AAYX_YUV  "s5k8aayxyuv"		//SW4-L1-HL-Camera-S1UMainCamFrontCamBringUp-00+_20120705

// Added by chu, zewei on 2012/11/23
#define SENSOR_DRVNAME_GC0308_YUV   	"gc0308yuv"
#define SENSOR_DRVNAME_GC0309_YUV   	"gc0309yuv"
#define SENSOR_DRVNAME_GC0311_YUV   	"gc0311yuv"
#define SENSOR_DRVNAME_GC0329_YUV   	"gc0329yuv"
#define SENSOR_DRVNAME_GC2035_YUV   	"gc2035yuv"
#define SENSOR_DRVNAME_SP2518_YUV    	"sp2518yuv"
#define SENSOR_DRVNAME_SP0828_YUV    "sp0828yuv"

/*******************************************************************************
*
********************************************************************************/

void KD_IMGSENSOR_PROFILE_INIT(void); 
void KD_IMGSENSOR_PROFILE(char *tag); 

#define mDELAY(ms)     mdelay(ms) 
#define uDELAY(us)       udelay(us) 
#endif //_KD_IMGSENSOR_H


