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

package org.bouncycastle.crypto.digests;

import org.bouncycastle.crypto.util.Pack;


/**
 * FIPS 180-2 implementation of SHA-384.
 *
 * <pre>
 *         block  word  digest
 * SHA-1   512    32    160
 * SHA-256 512    32    256
 * SHA-384 1024   64    384
 * SHA-512 1024   64    512
 * </pre>
 */
public class SHA384Digest
    extends LongDigest
{

    private static final int    DIGEST_LENGTH = 48;

    /**
     * Standard constructor
     */
    public SHA384Digest()
    {
    }

    /**
     * Copy constructor.  This will copy the state of the provided
     * message digest.
     */
    public SHA384Digest(SHA384Digest t)
    {
        super(t);
    }

    public String getAlgorithmName()
    {
        return "SHA-384";
    }

    public int getDigestSize()
    {
        return DIGEST_LENGTH;
    }

    public int doFinal(
        byte[]  out,
        int     outOff)
    {
        finish();

        Pack.longToBigEndian(H1, out, outOff);
        Pack.longToBigEndian(H2, out, outOff + 8);
        Pack.longToBigEndian(H3, out, outOff + 16);
        Pack.longToBigEndian(H4, out, outOff + 24);
        Pack.longToBigEndian(H5, out, outOff + 32);
        Pack.longToBigEndian(H6, out, outOff + 40);

        reset();

        return DIGEST_LENGTH;
    }

    /**
     * reset the chaining variables
     */
    public void reset()
    {
        super.reset();

        /* SHA-384 initial hash value
         * The first 64 bits of the fractional parts of the square roots
         * of the 9th through 16th prime numbers
         */
        H1 = 0xcbbb9d5dc1059ed8l;
        H2 = 0x629a292a367cd507l;
        H3 = 0x9159015a3070dd17l;
        H4 = 0x152fecd8f70e5939l;
        H5 = 0x67332667ffc00b31l;
        H6 = 0x8eb44a8768581511l;
        H7 = 0xdb0c2e0d64f98fa7l;
        H8 = 0x47b5481dbefa4fa4l;
    }
}
