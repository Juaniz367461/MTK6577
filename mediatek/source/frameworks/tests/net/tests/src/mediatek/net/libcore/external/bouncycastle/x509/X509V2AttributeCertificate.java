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

package org.bouncycastle.x509;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.x509.AttributeCertificate;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.util.Arrays;

/**
 * An implementation of a version 2 X.509 Attribute Certificate.
 * @deprecated use org.bouncycastle.cert.X509AttributeCertificateHolder
 */
public class X509V2AttributeCertificate
    implements X509AttributeCertificate
{
    private AttributeCertificate    cert;
    private Date                    notBefore;
    private Date                    notAfter;

    private static AttributeCertificate getObject(InputStream in)
        throws IOException
    {
        try
        {
            return AttributeCertificate.getInstance(new ASN1InputStream(in).readObject());
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new IOException("exception decoding certificate structure: " + e.toString());
        }
    }

    public X509V2AttributeCertificate(
        InputStream encIn)
        throws IOException
    {
        this(getObject(encIn));
    }
    
    public X509V2AttributeCertificate(
        byte[]  encoded)
        throws IOException
    {
        this(new ByteArrayInputStream(encoded));
    }
    
    X509V2AttributeCertificate(
        AttributeCertificate    cert)
        throws IOException
    {
        this.cert = cert;
        
        try
        {
            this.notAfter = cert.getAcinfo().getAttrCertValidityPeriod().getNotAfterTime().getDate();
            this.notBefore = cert.getAcinfo().getAttrCertValidityPeriod().getNotBeforeTime().getDate();
        }
        catch (ParseException e)
        {
            throw new IOException("invalid data structure in certificate!");
        }
    }
    
    public int getVersion()
    {
        return cert.getAcinfo().getVersion().getValue().intValue() + 1;
    }
    
    public BigInteger getSerialNumber()
    {
        return cert.getAcinfo().getSerialNumber().getValue();
    }
    
    public AttributeCertificateHolder getHolder()
    {
        return new AttributeCertificateHolder((ASN1Sequence)cert.getAcinfo().getHolder().toASN1Object());
    }
    
    public AttributeCertificateIssuer getIssuer()
    {
        return new AttributeCertificateIssuer(cert.getAcinfo().getIssuer());
    }
    
    public Date getNotBefore()
    {
        return notBefore;
    }
    
    public Date getNotAfter()
    {
        return notAfter;
    }
    
    public boolean[] getIssuerUniqueID()
    {
        DERBitString    id = cert.getAcinfo().getIssuerUniqueID();

        if (id != null)
        {
            byte[]          bytes = id.getBytes();
            boolean[]       boolId = new boolean[bytes.length * 8 - id.getPadBits()];

            for (int i = 0; i != boolId.length; i++)
            {
                boolId[i] = (bytes[i / 8] & (0x80 >>> (i % 8))) != 0;
            }

            return boolId;
        }
            
        return null;
    }
    
    public void checkValidity() 
        throws CertificateExpiredException, CertificateNotYetValidException
    {
        this.checkValidity(new Date());
    }
    
    public void checkValidity(
        Date    date)
        throws CertificateExpiredException, CertificateNotYetValidException
    {
        if (date.after(this.getNotAfter()))
        {
            throw new CertificateExpiredException("certificate expired on " + this.getNotAfter());
        }

        if (date.before(this.getNotBefore()))
        {
            throw new CertificateNotYetValidException("certificate not valid till " + this.getNotBefore());
        }
    }
    
    public byte[] getSignature()
    {
        return cert.getSignatureValue().getBytes();
    }
    
    public final void verify(
            PublicKey   key,
            String      provider)
            throws CertificateException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchProviderException, SignatureException
    {
        Signature   signature = null;

        if (!cert.getSignatureAlgorithm().equals(cert.getAcinfo().getSignature()))
        {
            throw new CertificateException("Signature algorithm in certificate info not same as outer certificate");
        }

        signature = Signature.getInstance(cert.getSignatureAlgorithm().getObjectId().getId(), provider);

        signature.initVerify(key);

        try
        {
            signature.update(cert.getAcinfo().getEncoded());
        }
        catch (IOException e)
        {
            throw new SignatureException("Exception encoding certificate info object");
        }

        if (!signature.verify(this.getSignature()))
        {
            throw new InvalidKeyException("Public key presented not for certificate signature");
        }
    }
    
    public byte[] getEncoded()
        throws IOException
    {
        return cert.getEncoded();
    }

    public byte[] getExtensionValue(String oid) 
    {
        X509Extensions  extensions = cert.getAcinfo().getExtensions();

        if (extensions != null)
        {
            X509Extension   ext = extensions.getExtension(new DERObjectIdentifier(oid));

            if (ext != null)
            {
                try
                {
                    return ext.getValue().getEncoded(ASN1Encodable.DER);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("error encoding " + e.toString());
                }
            }
        }

        return null;
    }

    private Set getExtensionOIDs(
        boolean critical) 
    {
        X509Extensions  extensions = cert.getAcinfo().getExtensions();

        if (extensions != null)
        {
            Set             set = new HashSet();
            Enumeration     e = extensions.oids();

            while (e.hasMoreElements())
            {
                DERObjectIdentifier oid = (DERObjectIdentifier)e.nextElement();
                X509Extension       ext = extensions.getExtension(oid);

                if (ext.isCritical() == critical)
                {
                    set.add(oid.getId());
                }
            }

            return set;
        }

        return null;
    }
    
    public Set getNonCriticalExtensionOIDs() 
    {
        return getExtensionOIDs(false);
    }

    public Set getCriticalExtensionOIDs() 
    {
        return getExtensionOIDs(true);
    }
    
    public boolean hasUnsupportedCriticalExtension()
    {
        Set  extensions = getCriticalExtensionOIDs();

        return extensions != null && !extensions.isEmpty();
    }

    public X509Attribute[] getAttributes()
    {
        ASN1Sequence    seq = cert.getAcinfo().getAttributes();
        X509Attribute[] attrs = new X509Attribute[seq.size()];
        
        for (int i = 0; i != seq.size(); i++)
        {
            attrs[i] = new X509Attribute((ASN1Encodable)seq.getObjectAt(i));
        }
        
        return attrs;
    }
    
    public X509Attribute[] getAttributes(String oid)
    {
        ASN1Sequence    seq = cert.getAcinfo().getAttributes();
        List            list = new ArrayList();
        
        for (int i = 0; i != seq.size(); i++)
        {
            X509Attribute attr = new X509Attribute((ASN1Encodable)seq.getObjectAt(i));
            if (attr.getOID().equals(oid))
            {
                list.add(attr);
            }
        }
        
        if (list.size() == 0)
        {
            return null;
        }
        
        return (X509Attribute[])list.toArray(new X509Attribute[list.size()]);
    }

    public boolean equals(
        Object o)
    {
        if (o == this)
        {
            return true;
        }

        if (!(o instanceof X509AttributeCertificate))
        {
            return false;
        }

        X509AttributeCertificate other = (X509AttributeCertificate)o;

        try
        {
            byte[] b1 = this.getEncoded();
            byte[] b2 = other.getEncoded();

            return Arrays.areEqual(b1, b2);
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public int hashCode()
    {
        try
        {
            return Arrays.hashCode(this.getEncoded());
        }
        catch (IOException e)
        {
            return 0;
        }
    }
}
