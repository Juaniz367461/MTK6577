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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ASN1StreamParser
{
    private final InputStream _in;
    private final int         _limit;

    public ASN1StreamParser(
        InputStream in)
    {
        this(in, ASN1InputStream.findLimit(in));
    }

    public ASN1StreamParser(
        InputStream in,
        int         limit)
    {
        this._in = in;
        this._limit = limit;
    }

    public ASN1StreamParser(
        byte[] encoding)
    {
        this(new ByteArrayInputStream(encoding), encoding.length);
    }

    DEREncodable readIndef(int tagValue) throws IOException
    {
        // Note: INDEF => CONSTRUCTED

        // TODO There are other tags that may be constructed (e.g. BIT_STRING)
        switch (tagValue)
        {
            case DERTags.EXTERNAL:
                return new DERExternalParser(this);
            case DERTags.OCTET_STRING:
                return new BEROctetStringParser(this);
            case DERTags.SEQUENCE:
                return new BERSequenceParser(this);
            case DERTags.SET:
                return new BERSetParser(this);
            default:
                throw new ASN1Exception("unknown BER object encountered: 0x" + Integer.toHexString(tagValue));
        }
    }

    DEREncodable readImplicit(boolean constructed, int tag) throws IOException
    {
        if (_in instanceof IndefiniteLengthInputStream)
        {
            if (!constructed)
            {
                throw new IOException("indefinite length primitive encoding encountered");
            }
            
            return readIndef(tag);
        }

        if (constructed)
        {
            switch (tag)
            {
                case DERTags.SET:
                    return new DERSetParser(this);
                case DERTags.SEQUENCE:
                    return new DERSequenceParser(this);
                case DERTags.OCTET_STRING:
                    return new BEROctetStringParser(this);
            }
        }
        else
        {
            switch (tag)
            {
                case DERTags.SET:
                    throw new ASN1Exception("sequences must use constructed encoding (see X.690 8.9.1/8.10.1)");
                case DERTags.SEQUENCE:
                    throw new ASN1Exception("sets must use constructed encoding (see X.690 8.11.1/8.12.1)");
                case DERTags.OCTET_STRING:
                    return new DEROctetStringParser((DefiniteLengthInputStream)_in);
            }
        }

        // TODO ASN1Exception
        throw new RuntimeException("implicit tagging not implemented");
    }

    DERObject readTaggedObject(boolean constructed, int tag) throws IOException
    {
        if (!constructed)
        {
            // Note: !CONSTRUCTED => IMPLICIT
            DefiniteLengthInputStream defIn = (DefiniteLengthInputStream)_in;
            return new DERTaggedObject(false, tag, new DEROctetString(defIn.toByteArray()));
        }

        ASN1EncodableVector v = readVector();

        if (_in instanceof IndefiniteLengthInputStream)
        {
            return v.size() == 1
                ?   new BERTaggedObject(true, tag, v.get(0))
                :   new BERTaggedObject(false, tag, BERFactory.createSequence(v));
        }

        return v.size() == 1
            ?   new DERTaggedObject(true, tag, v.get(0))
            :   new DERTaggedObject(false, tag, DERFactory.createSequence(v));
    }

    public DEREncodable readObject()
        throws IOException
    {
        int tag = _in.read();
        if (tag == -1)
        {
            return null;
        }

        //
        // turn of looking for "00" while we resolve the tag
        //
        set00Check(false);

        //
        // calculate tag number
        //
        int tagNo = ASN1InputStream.readTagNumber(_in, tag);

        boolean isConstructed = (tag & DERTags.CONSTRUCTED) != 0;

        //
        // calculate length
        //
        int length = ASN1InputStream.readLength(_in, _limit);

        if (length < 0) // indefinite length method
        {
            if (!isConstructed)
            {
                throw new IOException("indefinite length primitive encoding encountered");
            }

            IndefiniteLengthInputStream indIn = new IndefiniteLengthInputStream(_in, _limit);
            ASN1StreamParser sp = new ASN1StreamParser(indIn, _limit);

            if ((tag & DERTags.APPLICATION) != 0)
            {
                return new BERApplicationSpecificParser(tagNo, sp);
            }

            if ((tag & DERTags.TAGGED) != 0)
            {
                return new BERTaggedObjectParser(true, tagNo, sp);
            }

            return sp.readIndef(tagNo);
        }
        else
        {
            DefiniteLengthInputStream defIn = new DefiniteLengthInputStream(_in, length);

            if ((tag & DERTags.APPLICATION) != 0)
            {
                return new DERApplicationSpecific(isConstructed, tagNo, defIn.toByteArray());
            }

            if ((tag & DERTags.TAGGED) != 0)
            {
                return new BERTaggedObjectParser(isConstructed, tagNo, new ASN1StreamParser(defIn));
            }

            if (isConstructed)
            {
                // TODO There are other tags that may be constructed (e.g. BIT_STRING)
                switch (tagNo)
                {
                    case DERTags.OCTET_STRING:
                        //
                        // yes, people actually do this...
                        //
                        return new BEROctetStringParser(new ASN1StreamParser(defIn));
                    case DERTags.SEQUENCE:
                        return new DERSequenceParser(new ASN1StreamParser(defIn));
                    case DERTags.SET:
                        return new DERSetParser(new ASN1StreamParser(defIn));
                    case DERTags.EXTERNAL:
                        return new DERExternalParser(new ASN1StreamParser(defIn));
                    default:
                        // TODO Add DERUnknownTagParser class?
                        return new DERUnknownTag(true, tagNo, defIn.toByteArray());
                }
            }

            // Some primitive encodings can be handled by parsers too...
            switch (tagNo)
            {
                case DERTags.OCTET_STRING:
                    return new DEROctetStringParser(defIn);
            }

            try
            {
                return ASN1InputStream.createPrimitiveDERObject(tagNo, defIn.toByteArray());
            }
            catch (IllegalArgumentException e)
            {
                throw new ASN1Exception("corrupted stream detected", e);
            }
        }
    }

    private void set00Check(boolean enabled)
    {
        if (_in instanceof IndefiniteLengthInputStream)
        {
            ((IndefiniteLengthInputStream)_in).setEofOn00(enabled);
        }
    }

    ASN1EncodableVector readVector() throws IOException
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        DEREncodable obj;
        while ((obj = readObject()) != null)
        {
            if (obj instanceof InMemoryRepresentable)
            {
                v.add(((InMemoryRepresentable)obj).getLoadedObject());
            }
            else
            {
                v.add(obj.getDERObject());
            }
        }

        return v;
    }
}
