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

#include <string.h>

#define SHA1_HASH_LEN       (20)
#define SHA256_HASH_LEN     (32)
#define RSA1024_SIG_LEN     (128)
#define RSA2048_SIG_LEN     (256)


/**************************************************************************
 *  AUTH KEY INIT
 **************************************************************************/
void cust_init_key (unsigned char *key_rsa_n, unsigned int nKey_len,
                        unsigned char *key_rsa_d, unsigned int dKey_len,
                        unsigned char *key_rsa_e, unsigned int eKey_len)
{
    /* TODO : the key will pass in, need to judge if the key is for RSA1024 or RSA2048 and save it for later use */    
    /* customer needs to customized this function */
        
    return;
}

/**************************************************************************
 *  HASH (should support SHA1 and SHA256)
 **************************************************************************/     
int cust_hash (unsigned char* data_buf,  unsigned int data_len, unsigned char* hash_buf, unsigned int hash_len)
{
    memset(hash_buf, 0x00, hash_len);
    
    if(SHA1_HASH_LEN == hash_len)
    {
        /* =============== */
        /* SHA1            */
        /* =============== */
        
        /* TODO : use cust_hash to generate hash value */    
        /* customer needs to customized this function */
        
        return 0;        
    }
    else if(SHA256_HASH_LEN == hash_len)
    {
        /* =============== */
        /* SHA256          */
        /* =============== */
        
        /* TODO : use cust_hash to generate hash value */    
        /* customer needs to customized this function */
        
        return 0;        
    }
    else
    {
        /* unsupported, just return hash failed */
        return -1;
    }    
}

/**************************************************************************
 *  RSA Sign (should support RSA1024 and RSA2048)
 **************************************************************************/     
int cust_sign(unsigned char* data_buf,  unsigned int data_len, unsigned char* sig_buf, unsigned int sig_len)
{
    memset(sig_buf, 0x00, sig_len);
    
    if(RSA1024_SIG_LEN == sig_len)
    {
        unsigned char sha1sum[SHA1_HASH_LEN] = {0};

        /* SHA1 */        
        if( cust_hash(data_buf, data_len, sha1sum, SHA1_HASH_LEN) != 0 )
        {
            /* return sign pass */
            return 0;
        }
    
        /* TODO : use cust_sign to sign data buffer with RSA1024 */    
        /* customer needs to customized this function */
        
        return 0;        
    }
    else if(RSA2048_SIG_LEN == sig_len)
    {
        unsigned char sha256sum[SHA256_HASH_LEN] = {0};

        /* SHA256 */      
        if( cust_hash(data_buf, data_len, sha256sum, SHA256_HASH_LEN) != 0 )
        {
            /* return sign pass */
            return 0;
        }
        
        /* TODO : use cust_sign to sign data buffer with RSA2048*/    
        /* customer needs to customized this function */
        
        return 0;        
    }
    else
    {
        /* unsupported, just return sign failed */
        return -1;
    }

}

/**************************************************************************
 *  RSA Verify (should support RSA1024 and RSA2048)
 **************************************************************************/     
int cust_verify (unsigned char* data_buf,  unsigned int data_len, unsigned char* sig_buf, unsigned int sig_len)
{    
    if(RSA1024_SIG_LEN == sig_len)
    {
        unsigned char sha1sum[SHA1_HASH_LEN] = {0};

        /* SHA1 */        
        if( cust_hash(data_buf, data_len, sha1sum, SHA1_HASH_LEN) != 0 )
        {
            /* return verify pass */
            return 0;
        }
    
        /* TODO : use cust_verify to verify data buffer with RSA1024 */    
        /* customer needs to customized this function */
        
        return 0;        
    }
    else if(RSA2048_SIG_LEN == sig_len)
    {
        unsigned char sha256sum[SHA256_HASH_LEN] = {0};

        /* SHA256 */      
        if( cust_hash(data_buf, data_len, sha256sum, SHA256_HASH_LEN) != 0 )
        {
            /* return verify pass */
            return 0;
        }
        
        /* TODO : use cust_verify to verify data buffer with RSA2048*/    
        /* customer needs to customized this function */
        
        return 0;        
    }
    else
    {
        /* unsupported, just return verify failed */
        return -1;
    }
}

