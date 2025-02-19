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

package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

/**
 * <pre>
 * IssuingDistributionPoint ::= SEQUENCE { 
 *   distributionPoint          [0] DistributionPointName OPTIONAL, 
 *   onlyContainsUserCerts      [1] BOOLEAN DEFAULT FALSE, 
 *   onlyContainsCACerts        [2] BOOLEAN DEFAULT FALSE, 
 *   onlySomeReasons            [3] ReasonFlags OPTIONAL, 
 *   indirectCRL                [4] BOOLEAN DEFAULT FALSE,
 *   onlyContainsAttributeCerts [5] BOOLEAN DEFAULT FALSE }
 * </pre>
 */
public class IssuingDistributionPoint
    extends ASN1Encodable
{
    private DistributionPointName distributionPoint;

    private boolean onlyContainsUserCerts;

    private boolean onlyContainsCACerts;

    private ReasonFlags onlySomeReasons;

    private boolean indirectCRL;

    private boolean onlyContainsAttributeCerts;

    private ASN1Sequence seq;

    public static IssuingDistributionPoint getInstance(
        ASN1TaggedObject obj,
        boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static IssuingDistributionPoint getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof IssuingDistributionPoint)
        {
            return (IssuingDistributionPoint)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new IssuingDistributionPoint((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    /**
     * Constructor from given details.
     * 
     * @param distributionPoint
     *            May contain an URI as pointer to most current CRL.
     * @param onlyContainsUserCerts Covers revocation information for end certificates.
     * @param onlyContainsCACerts Covers revocation information for CA certificates.
     * 
     * @param onlySomeReasons
     *            Which revocation reasons does this point cover.
     * @param indirectCRL
     *            If <code>true</code> then the CRL contains revocation
     *            information about certificates ssued by other CAs.
     * @param onlyContainsAttributeCerts Covers revocation information for attribute certificates.
     */
    public IssuingDistributionPoint(
        DistributionPointName distributionPoint,
        boolean onlyContainsUserCerts,
        boolean onlyContainsCACerts,
        ReasonFlags onlySomeReasons,
        boolean indirectCRL,
        boolean onlyContainsAttributeCerts)
    {
        this.distributionPoint = distributionPoint;
        this.indirectCRL = indirectCRL;
        this.onlyContainsAttributeCerts = onlyContainsAttributeCerts;
        this.onlyContainsCACerts = onlyContainsCACerts;
        this.onlyContainsUserCerts = onlyContainsUserCerts;
        this.onlySomeReasons = onlySomeReasons;

        ASN1EncodableVector vec = new ASN1EncodableVector();
        if (distributionPoint != null)
        {                                    // CHOICE item so explicitly tagged
            vec.add(new DERTaggedObject(true, 0, distributionPoint));
        }
        if (onlyContainsUserCerts)
        {
            // BEGIN android-changed
            vec.add(new DERTaggedObject(false, 1, DERBoolean.TRUE));
            // END android-changed
        }
        if (onlyContainsCACerts)
        {
            // BEGIN android-changed
            vec.add(new DERTaggedObject(false, 2, DERBoolean.TRUE));
            // END android-changed
        }
        if (onlySomeReasons != null)
        {
            vec.add(new DERTaggedObject(false, 3, onlySomeReasons));
        }
        if (indirectCRL)
        {
            // BEGIN android-changed
            vec.add(new DERTaggedObject(false, 4, DERBoolean.TRUE));
            // END android-changed
        }
        if (onlyContainsAttributeCerts)
        {
            // BEGIN android-changed
            vec.add(new DERTaggedObject(false, 5, DERBoolean.TRUE));
            // END android-changed
        }

        seq = new DERSequence(vec);
    }

    /**
     * Constructor from ASN1Sequence
     */
    public IssuingDistributionPoint(
        ASN1Sequence seq)
    {
        this.seq = seq;

        for (int i = 0; i != seq.size(); i++)
        {
            ASN1TaggedObject o = ASN1TaggedObject.getInstance(seq.getObjectAt(i));

            switch (o.getTagNo())
            {
            case 0:
                                                    // CHOICE so explicit
                distributionPoint = DistributionPointName.getInstance(o, true);
                break;
            case 1:
                onlyContainsUserCerts = DERBoolean.getInstance(o, false).isTrue();
                break;
            case 2:
                onlyContainsCACerts = DERBoolean.getInstance(o, false).isTrue();
                break;
            case 3:
                onlySomeReasons = new ReasonFlags(ReasonFlags.getInstance(o, false));
                break;
            case 4:
                indirectCRL = DERBoolean.getInstance(o, false).isTrue();
                break;
            case 5:
                onlyContainsAttributeCerts = DERBoolean.getInstance(o, false).isTrue();
                break;
            default:
                throw new IllegalArgumentException(
                        "unknown tag in IssuingDistributionPoint");
            }
        }
    }

    public boolean onlyContainsUserCerts()
    {
        return onlyContainsUserCerts;
    }

    public boolean onlyContainsCACerts()
    {
        return onlyContainsCACerts;
    }

    public boolean isIndirectCRL()
    {
        return indirectCRL;
    }

    public boolean onlyContainsAttributeCerts()
    {
        return onlyContainsAttributeCerts;
    }

    /**
     * @return Returns the distributionPoint.
     */
    public DistributionPointName getDistributionPoint()
    {
        return distributionPoint;
    }

    /**
     * @return Returns the onlySomeReasons.
     */
    public ReasonFlags getOnlySomeReasons()
    {
        return onlySomeReasons;
    }

    public DERObject toASN1Object()
    {
        return seq;
    }

    public String toString()
    {
        String       sep = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();

        buf.append("IssuingDistributionPoint: [");
        buf.append(sep);
        if (distributionPoint != null)
        {
            appendObject(buf, sep, "distributionPoint", distributionPoint.toString());
        }
        if (onlyContainsUserCerts)
        {
            appendObject(buf, sep, "onlyContainsUserCerts", booleanToString(onlyContainsUserCerts));
        }
        if (onlyContainsCACerts)
        {
            appendObject(buf, sep, "onlyContainsCACerts", booleanToString(onlyContainsCACerts));
        }
        if (onlySomeReasons != null)
        {
            appendObject(buf, sep, "onlySomeReasons", onlySomeReasons.toString());
        }
        if (onlyContainsAttributeCerts)
        {
            appendObject(buf, sep, "onlyContainsAttributeCerts", booleanToString(onlyContainsAttributeCerts));
        }
        if (indirectCRL)
        {
            appendObject(buf, sep, "indirectCRL", booleanToString(indirectCRL));
        }
        buf.append("]");
        buf.append(sep);
        return buf.toString();
    }

    private void appendObject(StringBuffer buf, String sep, String name, String value)
    {
        String       indent = "    ";

        buf.append(indent);
        buf.append(name);
        buf.append(":");
        buf.append(sep);
        buf.append(indent);
        buf.append(indent);
        buf.append(value);
        buf.append(sep);
    }

    private String booleanToString(boolean value)
    {
        return value ? "true" : "false";
    }
}
