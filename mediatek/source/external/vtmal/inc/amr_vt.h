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

#ifndef AMR_CODEER_DOT_H
#define AMR_CODEER_DOT_H

#ifdef __cplusplus
extern "C"{
#endif 
/*
********************************************************************************
*                         INCLUDE FILES
********************************************************************************
*/
 
/*
********************************************************************************
*                         DEFINITION OF DATA TYPES
********************************************************************************
*/

typedef enum { BR475 = 0,
               BR515,            
               BR59,
               BR67,
               BR74,
               BR795,
               BR102,
               BR122,            
	           BRDTX	    
             }AMR_BitRate;

/*****************************************************************************
 * STRUCTURE
 *  AMR_ENC_HANDLE
 *
 * DESCRIPTION
 *  AMR encoder handler
 *
 * MEMBERS
 *  amr_enc_data   Encoder internal data pointer (these data can't be release during encoding process)
 *  BitRate         Encoder bitrate
 *  dtx_enable      DTX enable or not (1:enable, 0:disable) 
 *
 *****************************************************************************
 */
typedef struct {
   void *amr_enc_data;
   int dtx_enable;    
   AMR_BitRate BitRate;                	
}AMR_ENC_HANDLE;

/*****************************************************************************
 * STRUCTURE
 *  AMR_DEC_HANDLE
 *
 * DESCRIPTION
 *  AMR decoder handler
 *
 * MEMBERS
 *  amr_dec_data    Decoder internal data pointer (these data can't be release during decoding process)
 *
 *****************************************************************************
 */
typedef struct {
   void *amr_dec_data;
}AMR_DEC_HANDLE;
 
 
/*
********************************************************************************
*                         DECLARATION OF PROTOTYPES
********************************************************************************
*/
 
/*****************************************************************************
 * FUNCTION
 *  AMREnc_GetBufferSize
 *
 * DESCRIPTION
 *  This function gets the buffers size for encoder.
 *
 * PARAMETERS
 *  int_buf_size    [o] Internal buffer size
 *  tmp_buf_size    [o] Temporary buffer size
 *  bits_buf_size   [o] Bitstream buffer size
 *  pcm_buf_size    [o] Speech PCM buffer size 
 *
 * RETURNS
 *  None
 *
 *****************************************************************************
 */
void AMREnc_GetBufferSize(
   unsigned int *int_buf_size,
   unsigned int *tmp_buf_size,
   unsigned int *bs_buf_size,
   unsigned int *pcm_buf_size   
);
/*****************************************************************************
 * FUNCTION
 *  AMRDec_GetBufferSize
 *
 * DESCRIPTION
 *  This function gets the buffers size for decoder.
 *
 * PARAMETERS
 *  int_buf_size    [o] Internal buffer size
 *  tmp_buf_size    [o] Temporary buffer size
 *  bits_buf_size   [o] Bitstream buffer size 
 *  pcm_buf_size    [o] Speech PCM buffer size
 *
 * RETURNS
 *  None
 *
 *****************************************************************************
 */
void AMRDec_GetBufferSize(
   unsigned int *int_buf_size,
   unsigned int *tmp_buf_size,
   unsigned int *bs_buf_size,   
   unsigned int *pcm_buf_size
);


/*****************************************************************************
 * FUNCTION
 *  AMREnc_Init
 *
 * DESCRIPTION
 *  This function initialize the buffer for encoder.
 *
 * PARAMETERS
 *  int_buffer  [i] Encoder internal buffer pointer. This buffer can��t be re-used by other application.
 *  dtx         [i] Supported DTX or not (1:enable, 0:disable).
 *  BitRate     [i] Bitrate of AMR codec
 * 	 
 * RETURNS
 *  the pointer of AMR encoder handler
 *
 *****************************************************************************
 */
AMR_ENC_HANDLE *AMREnc_Init(
   void *int_buffer,
   AMR_BitRate BitRate,
   int dtx   
);

/*****************************************************************************
 * FUNCTION
 *  AMRDec_Init
 *
 * DESCRIPTION
 *  This function initialize the buffer for decoder.
 *
 * PARAMETERS
 *  int_buffer    [i] Decoder internal buffer pointer. This buffer can��t be re-used by other application.
 *
 * RETURNS
 *  the pointer of AMR decoder handler
 *
 *****************************************************************************
 */
AMR_DEC_HANDLE *AMRDec_Init(
   void *int_buffer
);


/*****************************************************************************
 * FUNCTION
 *  AMR_Encode
 *
 * DESCRIPTION
 *  This function encodes one frame pcm data (160 samples/frame) and output AMR bitstream.
 *
 * PARAMETERS
 *  amr_enc_handle  [i] the pointer of AMR encoder handle
 *  tmp_buffer      [i] Temporary buffer pointer. Temporary buffer could be used by other application after encoding a frame.  
 *  pcm_buffer      [i] Input PCM sample buffer pointer (input 160 samples pcm data)   
 *  bs_buffer       [o] Output bitstream buffer pointer
 *  bitrate         [i] AMR Encoder bitrate 
 *
 * RETURNS
 *  Bitstream length in byte
 *
 *
 *****************************************************************************
 */
 
int AMR_Encode(
   AMR_ENC_HANDLE *amr_enc_handle,
   void *tmp_buffer,
   short *pcm_buffer,
   unsigned char *bs_buffer, 
	AMR_BitRate bitrate						
);


/*****************************************************************************
 * FUNCTION
 *  AMR_Decode
 *
 * DESCRIPTION
 *  This function decodes AMR bitstream and output 160 samples pcm data.
 *
 * PARAMETERS
 *  amr_dec_handle  [i] The pointer of AMR decoder handle  
 *  tmp_buffer      [i] Temporary buffer pointer. Temporary buffer could be used by other application after encoding a frame.
 *  pcm_buffer      [o] Output PCM sample buffer pointer (output 160 samples pcm data)   
 *  bs_buffer       [i] Input bitstream buffer pointer 
 *
 * RETURNS
 *  None  
 *
 *****************************************************************************
 */
 
void AMR_Decode(
   AMR_DEC_HANDLE *amr_dec_handle,
   void *tmp_buffer,
   short *pcm_buffer,	
   unsigned char *bs_buffer,
   unsigned char frame_types   
); 
 
 
 
#ifdef __cplusplus
}
#endif 
#endif


