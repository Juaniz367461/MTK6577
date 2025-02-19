/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2009
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
*****************************************************************************/
/** 
 * @file 
 *   jpeg_dec_hal.h 
 *
 * @par Project:
 *   Yusu 
 *
 * @par Description::
 *   JPEG Decoder Driver definitions and function prototypes
 *
 * @par Author:
 *   Tzu-Meng Chung (mtk02529)
 *
 * @par $Revision$
 * @par $Modtime:$
 * @par $Log:$
 *
 */
 
#ifndef __MTK_JPEGDEC_API_H__
#define __MTK_JPEGDEC_API_H__

/*=============================================================================
 *                              Type definition
 *===========================================================================*/
typedef unsigned int    UINT32;
typedef unsigned short  UINT16;
typedef unsigned char   UINT8;
#define INT8  char

/* 
#define UINT32  unsigned int 
#define UINT16  unsigned short
#define UINT8  unsigned char
#define INT8  char
*/
/**
 * @par Enumeration
 *   JPEG_DEC_STATUS_ENUM
 * @par Description
 *   This is the return status of each jpeg decoder function 
 */
typedef enum {
   JPEG_DEC_STATUS_OK = 0,                      ///< The function work successfully
   JPEG_DEC_STATUS_ERROR_CODEC_UNAVAILABLE,     ///< Error due to unavailable hardare codec
   JPEG_DEC_STATUS_ERROR_INVALID_PARAMETER,     ///< Error due to invalid parameter
   JPEG_DEC_STATUS_ERROR_PARSE_INCOMPLETE,      ///< Error due to invalid parser
   JPEG_DEC_STATUS_ERROR_INVALID_FILE,          ///< Error due to invalid source file
   JPEG_DEC_STATUS_ERROR_UNSUPPORTED_FORMAT,    ///< Error due to unsupported format of hardware
   JPEG_DEC_STATUS_ERROR_INVALID_WIDTH,         ///< Error due to unsupported dimension of hardware
   JPEG_DEC_STATUS_ERROR_INVALID_HEIGHT,        ///< Error due to unsupported dimension of hardware
   JPEG_DEC_STATUS_ERROR_INSUFFICIENT_MEMORY,   ///< Error due to insufficient memory

   JPEG_DEC_STATUS_ALL = 0xFFFFFFFF
} JPEG_DEC_STATUS_ENUM;

/**
 * @par Enumeration
 *   JPEG_DEC_RESULT_ENUM
 * @par Description
 *   This is the result after trigger jpeg decoder
 */
typedef enum {
   JPEG_DEC_RESULT_DONE = 0,                ///< The result of decoder is successful
   JPEG_DEC_RESULT_PAUSE,                   ///< The result of decoder is pause, need resume again
   JPEG_DEC_RESULT_OVERFLOW,                ///< The result of decoder is overflow
   JPEG_DEC_RESULT_HALT,                    ///< The result of decoder is timeout

   JPEG_DEC_RESULT_ALL = 0xFFFFFFFF
} JPEG_DEC_RESULT_ENUM;

/**
 * @par Structure
 *   JPEG_FILE_INFO_IN
 * @par Description
 *   This is a structure which store jpeg information for IDP configuration
 */
typedef struct JPEG_FILE_INFO_T {
   UINT32 width;                    ///< the width of jpeg file
   UINT32 height;                   ///< the height of jpeg file

   UINT32 padded_width;             ///< the padded width of jpeg file
   UINT32 padded_height;            ///< the padded height of jpeg file

   UINT32 dst_width;                ///< the destination width of jpeg file
   UINT32 dst_height;               ///< the destination height of jpeg file
   UINT32 dst_buffer_pa;            ///< the physical address of destinaion buffer
   UINT32 temp_buffer_pa;           ///< the physical address of working buffer

   UINT32 total_mcu;                ///< the total mcu number
   UINT32 samplingFormat;           ///< format (400 422 420 411)
   
   UINT8  componentNum;             ///< the number of components
   UINT8  y_hSamplingFactor;        ///< the horizontal sampling factor of y component
   UINT8  u_hSamplingFactor;        ///< the horizontal sampling factor of u component
   UINT8  v_hSamplingFactor;        ///< the horizontal sampling factor of v component
   UINT8  y_vSamplingFactor;        ///< the vertical sampling factor of y component
   UINT8  u_vSamplingFactor;        ///< the vertical sampling factor of u component
   UINT8  v_vSamplingFactor;        ///< the vertical sampling factor of v component
   UINT8  doDithering;                    ///
} JPEG_FILE_INFO_IN;


/*=============================================================================
 *                             Function Declaration
 *===========================================================================*/
 
 /**
 * @par Function       
 *   jpegDecLockDecoder
 * @par Description    
 *   Lock hardware controller  
 * @param              
 *   drvID          [OUT]       identify for hardware
 * @par Returns        
 *   JPEG_DEC_STATUS_ENUM
 */
JPEG_DEC_STATUS_ENUM jpegDecLockDecoder(int* drvID);

 /**
 * @par Function
 *   jpegDecUnlockDecoder
 * @par Description:
 *   Unlock hardware controller  
 * @param 
 *   drvID          [IN]        identify for hardware
 * @par Returns:
 *   JPEG_DEC_STATUS_ENUM
 */
JPEG_DEC_STATUS_ENUM jpegDecUnlockDecoder(int drvID);

 /**
 * @par Function
 *   jpegDecSetSourceFile
 * @par Description:
 *   Set JPEG file source stream  
 * @param 
 *   drvID          [IN]        identify for hardware
 * @param
 *   srcVA          [IN]        source file virtual address
 * @param
 *   srcPA          [IN]        source file physical address
 * @param
 *   srcSize        [IN]        source file size
 * @par Returns:
 *   JPEG_DEC_STATUS_ENUM
 */
JPEG_DEC_STATUS_ENUM jpegDecSetSourceFile(int drvID, unsigned char *srcVA, unsigned int srcSize);

 /**
 * @par Function
 *   jpegDecSetSourceSize
 * @par Description:
 *   Set JPEG file source stream  
 * @param 
 *   drvID          [IN]        identify for hardware
 * @param
 *   srcSize        [IN]        source file size
 * @par Returns:
 *   JPEG_DEC_STATUS_ENUM
 */
JPEG_DEC_STATUS_ENUM jpegDecSetSourceSize(int drvID, unsigned int srcVA, unsigned int srcSize);

 /**
 * @par Function
 *   jpegDecRange
 * @par Description:
 *   Set the decode range
 * @param 
 *   drvID          [IN]        identify for hardware
 * @param
 *   top            [IN]        the top of an image
 * @param
 *   bottom         [IN]        the bottom of an image  
 * @par Returns:
 *   JPEG_DEC_STATUS_ENUM
 */
//JPEG_DEC_STATUS_ENUM jpegDecSetRange(int drvID, int* top, int* bottom);

 /**
 * @par Function
 *   jpegDecStart
 * @par Description:
 *   Trigger the jpeg decode hardware 
 * @param 
 *   drvID          [IN]        identify for hardware
 * @param
 *   timeout        [IN]        the timeout for jpeg decoder
 * @param
 *   result         [IN]        the result for jpeg decoder  
 * @par Returns:
 *   JPEG_DEC_STATUS_ENUM
 */
JPEG_DEC_STATUS_ENUM jpegDecStart(int drvID, long timeout, JPEG_DEC_RESULT_ENUM *result);

 /**
 * @par Function
 *   jpegDecResume
 * @par Description:
 *   Resume the jpeg decode hardware 
 * @param 
 *   drvID          [IN]        identify for hardware
 * @param
 *   srcFileAddr    [IN]        source file address at this round
 * @param
 *   srcFileSize    [IN]        source file size at this round
 * @param
 *   isPhyAddr      [IN]        the flag to record the file address is physical or not
 * @param
 *   timeout        [IN]        the timeout for jpeg decoder
 * @param
 *   result         [IN]        the result for jpeg decoder  
 * @par Returns:
 *   JPEG_DEC_STATUS_ENUM
 */
//JPEG_DEC_STATUS_ENUM jpegDecResume(int drvID, unsigned int srcFileAddr, unsigned int srcFileSize, 
//                                   unsigned char isPhyAddr, long timeout, JPEG_DEC_RESULT_ENUM *result);

 /**
 * @par Function
 *   jpegDecGetFileInfo
 * @par Description:
 *   Get the information of jpeg file 
 * @param 
 *   drvID          [IN]        identify for hardware
 * @param
 *   fileInfo       [OUT]       record the jpeg file information
 * @par Returns:
 *   JPEG_DEC_STATUS_ENUM
 */
JPEG_DEC_STATUS_ENUM jpegDecGetFileInfo(int drvID, JPEG_FILE_INFO_IN *fileInfo);

 /**
 * @par Function
 *   jpegDecGetData
 * @par Description:
 *   Get the decoded data from kernel space 
 * @param 
 *   drvID          [IN]        identify for hardware
 * @par Returns:
 *   JPEG_DEC_STATUS_ENUM
 */
//JPEG_DEC_STATUS_ENUM jpegDecGetData(int drvID);


#endif
