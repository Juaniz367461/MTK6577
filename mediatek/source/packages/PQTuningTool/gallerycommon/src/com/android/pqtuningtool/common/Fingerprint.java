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

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package  com.android.pqtuningtool.common;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * MD5-based digest Wrapper.
 */
public class Fingerprint {
    // Instance of the MessageDigest using our specified digest algorithm.
    private static final MessageDigest DIGESTER;

    /**
     * Name of the digest algorithm we use in {@link java.security.MessageDigest}
     */
    private static final String DIGEST_MD5 = "md5";

    // Version 1 streamId prefix.
    // Hard coded stream id length limit is 40-chars. Don't ask!
    private static final String STREAM_ID_CS_PREFIX = "cs_01_";

    // 16 bytes for 128-bit fingerprint
    private static final int FINGERPRINT_BYTE_LENGTH;

    // length of prefix + 32 hex chars for 128-bit fingerprint
    private static final int STREAM_ID_CS_01_LENGTH;

    static {
        try {
            DIGESTER = MessageDigest.getInstance(DIGEST_MD5);
            FINGERPRINT_BYTE_LENGTH = DIGESTER.getDigestLength();
            STREAM_ID_CS_01_LENGTH = STREAM_ID_CS_PREFIX.length()
                    + (FINGERPRINT_BYTE_LENGTH * 2);
        } catch (NoSuchAlgorithmException e) {
            // can't continue, but really shouldn't happen
            throw new IllegalStateException(e);
        }
    }

    // md5 digest bytes.
    private final byte[] mMd5Digest;

    /**
     * Creates a new Fingerprint.
     */
    public Fingerprint(byte[] bytes) {
        if ((bytes == null) || (bytes.length != FINGERPRINT_BYTE_LENGTH)) {
            throw new IllegalArgumentException();
        }
        mMd5Digest = bytes;
    }

    /**
     * Creates a Fingerprint based on the contents of a file.
     *
     * Note that this will close() stream after calculating the digest.
     * @param byteCount length of original data will be stored at byteCount[0] as a side product
     *        of the fingerprint calculation
     */
    public static Fingerprint fromInputStream(InputStream stream, long[] byteCount)
            throws IOException {
        DigestInputStream in = null;
        long count = 0;
        try {
            in = new DigestInputStream(stream, DIGESTER);
            byte[] bytes = new byte[8192];
            while (true) {
                // scan through file to compute a fingerprint.
                int n = in.read(bytes);
                if (n < 0) break;
                count += n;
            }
        } finally {
            if (in != null) in.close();
        }
        if ((byteCount != null) && (byteCount.length > 0)) byteCount[0] = count;
        return new Fingerprint(in.getMessageDigest().digest());
    }

    /**
     * Decodes a string stream id to a 128-bit fingerprint.
     */
    public static Fingerprint fromStreamId(String streamId) {
        if ((streamId == null)
                || !streamId.startsWith(STREAM_ID_CS_PREFIX)
                || (streamId.length() != STREAM_ID_CS_01_LENGTH)) {
            throw new IllegalArgumentException("bad streamId: " + streamId);
        }

        // decode the hex bytes of the fingerprint portion
        byte[] bytes = new byte[FINGERPRINT_BYTE_LENGTH];
        int byteIdx = 0;
        for (int idx = STREAM_ID_CS_PREFIX.length(); idx < STREAM_ID_CS_01_LENGTH;
                idx += 2) {
            int value = (toDigit(streamId, idx) << 4) | toDigit(streamId, idx + 1);
            bytes[byteIdx++] = (byte) (value & 0xff);
        }
        return new Fingerprint(bytes);
    }

    /**
     * Scans a list of strings for a valid streamId.
     *
     * @param streamIdList list of stream id's to be scanned
     * @return valid fingerprint or null if it can't be found
     */
    public static Fingerprint extractFingerprint(List<String> streamIdList) {
        for (String streamId : streamIdList) {
            if (streamId.startsWith(STREAM_ID_CS_PREFIX)) {
                return fromStreamId(streamId);
            }
        }
        return null;
    }

    /**
     * Encodes a 128-bit fingerprint as a string stream id.
     *
     * Stream id string is limited to 40 characters, which could be digits, lower case ASCII and
     * underscores.
     */
    public String toStreamId() {
        StringBuilder streamId = new StringBuilder(STREAM_ID_CS_PREFIX);
        appendHexFingerprint(streamId, mMd5Digest);
        return streamId.toString();
    }

    public byte[] getBytes() {
        return mMd5Digest;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Fingerprint)) return false;
        Fingerprint other = (Fingerprint) obj;
        return Arrays.equals(mMd5Digest, other.mMd5Digest);
    }

    public boolean equals(byte[] md5Digest) {
        return Arrays.equals(mMd5Digest, md5Digest);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mMd5Digest);
    }

    // Utility methods.

    private static int toDigit(String streamId, int index) {
        int digit = Character.digit(streamId.charAt(index), 16);
        if (digit < 0) {
            throw new IllegalArgumentException("illegal hex digit in " + streamId);
        }
        return digit;
    }

    private static void appendHexFingerprint(StringBuilder sb, byte[] bytes) {
        for (int idx = 0; idx < FINGERPRINT_BYTE_LENGTH; idx++) {
            int value = bytes[idx];
            sb.append(Integer.toHexString((value >> 4) & 0x0f));
            sb.append(Integer.toHexString(value& 0x0f));
        }
    }
}
