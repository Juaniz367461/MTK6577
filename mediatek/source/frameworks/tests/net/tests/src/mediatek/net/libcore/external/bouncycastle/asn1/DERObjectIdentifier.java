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

package org.bouncycastle.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

public class DERObjectIdentifier
    extends ASN1Object
{
    String      identifier;

    /**
     * return an OID from the passed in object
     *
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static DERObjectIdentifier getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof DERObjectIdentifier)
        {
            return (DERObjectIdentifier)obj;
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * return an Object Identifier from a tagged object.
     *
     * @param obj the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *               be converted.
     */
    public static DERObjectIdentifier getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        DERObject o = obj.getObject();

        if (explicit || o instanceof DERObjectIdentifier)
        {
            return getInstance(o);
        }
        else
        {
            return new ASN1ObjectIdentifier(ASN1OctetString.getInstance(obj.getObject()).getOctets());
        }
    }
    

    DERObjectIdentifier(
        byte[]  bytes)
    {
        StringBuffer    objId = new StringBuffer();
        long            value = 0;
        BigInteger      bigValue = null;
        boolean         first = true;

        for (int i = 0; i != bytes.length; i++)
        {
            int b = bytes[i] & 0xff;

            if (value < 0x80000000000000L) 
            {
                value = value * 128 + (b & 0x7f);
                if ((b & 0x80) == 0)             // end of number reached
                {
                    if (first)
                    {
                        switch ((int)value / 40)
                        {
                        case 0:
                            objId.append('0');
                            break;
                        case 1:
                            objId.append('1');
                            value -= 40;
                            break;
                        default:
                            objId.append('2');
                            value -= 80;
                        }
                        first = false;
                    }

                    objId.append('.');
                    objId.append(value);
                    value = 0;
                }
            } 
            else 
            {
                if (bigValue == null)
                {
                    bigValue = BigInteger.valueOf(value);
                }
                bigValue = bigValue.shiftLeft(7);
                bigValue = bigValue.or(BigInteger.valueOf(b & 0x7f));
                if ((b & 0x80) == 0) 
                {
                    objId.append('.');
                    objId.append(bigValue);
                    bigValue = null;
                    value = 0;
                }
            }
        }

        // BEGIN android-changed
        /*
         * Intern the identifier so there aren't hundreds of duplicates
         * (in practice).
         */
        this.identifier = objId.toString().intern();
        // END android-changed
    }

    public DERObjectIdentifier(
        String  identifier)
    {
        if (!isValidIdentifier(identifier))
        {
            throw new IllegalArgumentException("string " + identifier + " not an OID");
        }

        // BEGIN android-changed
        /*
         * Intern the identifier so there aren't hundreds of duplicates
         * (in practice).
         */
        this.identifier = identifier.intern();
        // END android-changed
    }

    public String getId()
    {
        return identifier;
    }

    private void writeField(
        OutputStream    out,
        long            fieldValue)
        throws IOException
    {
        byte[] result = new byte[9];
        int pos = 8;
        result[pos] = (byte)((int)fieldValue & 0x7f);
        while (fieldValue >= (1L << 7))
        {
            fieldValue >>= 7;
            result[--pos] = (byte)((int)fieldValue & 0x7f | 0x80);
        }
        out.write(result, pos, 9 - pos);
    }

    private void writeField(
        OutputStream    out,
        BigInteger      fieldValue)
        throws IOException
    {
        int byteCount = (fieldValue.bitLength()+6)/7;
        if (byteCount == 0) 
        {
            out.write(0);
        }  
        else 
        {
            BigInteger tmpValue = fieldValue;
            byte[] tmp = new byte[byteCount];
            for (int i = byteCount-1; i >= 0; i--) 
            {
                tmp[i] = (byte) ((tmpValue.intValue() & 0x7f) | 0x80);
                tmpValue = tmpValue.shiftRight(7); 
            }
            tmp[byteCount-1] &= 0x7f;
            out.write(tmp);
        }

    }

    void encode(
        DEROutputStream out)
        throws IOException
    {
        OIDTokenizer            tok = new OIDTokenizer(identifier);
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        DEROutputStream         dOut = new DEROutputStream(bOut);

        writeField(bOut, 
                    Integer.parseInt(tok.nextToken()) * 40
                    + Integer.parseInt(tok.nextToken()));

        while (tok.hasMoreTokens())
        {
            String token = tok.nextToken();
            if (token.length() < 18) 
            {
                writeField(bOut, Long.parseLong(token));
            }
            else
            {
                writeField(bOut, new BigInteger(token));
            }
        }

        dOut.close();

        byte[]  bytes = bOut.toByteArray();

        out.writeEncoded(OBJECT_IDENTIFIER, bytes);
    }

    public int hashCode()
    {
        return identifier.hashCode();
    }

    boolean asn1Equals(
        DERObject  o)
    {
        if (!(o instanceof DERObjectIdentifier))
        {
            return false;
        }

        return identifier.equals(((DERObjectIdentifier)o).identifier);
    }

    public String toString()
    {
        return getId();
    }

    private static boolean isValidIdentifier(
        String identifier)
    {
        if (identifier.length() < 3
            || identifier.charAt(1) != '.')
        {
            return false;
        }

        char first = identifier.charAt(0);
        if (first < '0' || first > '2')
        {
            return false;
        }

        boolean periodAllowed = false;
        for (int i = identifier.length() - 1; i >= 2; i--)
        {
            char ch = identifier.charAt(i);

            if ('0' <= ch && ch <= '9')
            {
                periodAllowed = true;
                continue;
            }

            if (ch == '.')
            {
                if (!periodAllowed)
                {
                    return false;
                }

                periodAllowed = false;
                continue;
            }

            return false;
        }

        return periodAllowed;
    }
}
