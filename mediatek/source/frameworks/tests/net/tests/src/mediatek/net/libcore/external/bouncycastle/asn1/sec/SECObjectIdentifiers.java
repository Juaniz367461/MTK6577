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

package org.bouncycastle.asn1.sec;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;

public interface SECObjectIdentifiers
{
    /**
     *  ellipticCurve OBJECT IDENTIFIER ::= {
     *        iso(1) identified-organization(3) certicom(132) curve(0)
     *  }
     */
    static final ASN1ObjectIdentifier ellipticCurve = new ASN1ObjectIdentifier("1.3.132.0");

    static final ASN1ObjectIdentifier sect163k1 = ellipticCurve.branch("1");
    static final ASN1ObjectIdentifier sect163r1 = ellipticCurve.branch("2");
    static final ASN1ObjectIdentifier sect239k1 = ellipticCurve.branch("3");
    static final ASN1ObjectIdentifier sect113r1 = ellipticCurve.branch("4");
    static final ASN1ObjectIdentifier sect113r2 = ellipticCurve.branch("5");
    static final ASN1ObjectIdentifier secp112r1 = ellipticCurve.branch("6");
    static final ASN1ObjectIdentifier secp112r2 = ellipticCurve.branch("7");
    static final ASN1ObjectIdentifier secp160r1 = ellipticCurve.branch("8");
    static final ASN1ObjectIdentifier secp160k1 = ellipticCurve.branch("9");
    static final ASN1ObjectIdentifier secp256k1 = ellipticCurve.branch("10");
    static final ASN1ObjectIdentifier sect163r2 = ellipticCurve.branch("15");
    static final ASN1ObjectIdentifier sect283k1 = ellipticCurve.branch("16");
    static final ASN1ObjectIdentifier sect283r1 = ellipticCurve.branch("17");
    static final ASN1ObjectIdentifier sect131r1 = ellipticCurve.branch("22");
    static final ASN1ObjectIdentifier sect131r2 = ellipticCurve.branch("23");
    static final ASN1ObjectIdentifier sect193r1 = ellipticCurve.branch("24");
    static final ASN1ObjectIdentifier sect193r2 = ellipticCurve.branch("25");
    static final ASN1ObjectIdentifier sect233k1 = ellipticCurve.branch("26");
    static final ASN1ObjectIdentifier sect233r1 = ellipticCurve.branch("27");
    static final ASN1ObjectIdentifier secp128r1 = ellipticCurve.branch("28");
    static final ASN1ObjectIdentifier secp128r2 = ellipticCurve.branch("29");
    static final ASN1ObjectIdentifier secp160r2 = ellipticCurve.branch("30");
    static final ASN1ObjectIdentifier secp192k1 = ellipticCurve.branch("31");
    static final ASN1ObjectIdentifier secp224k1 = ellipticCurve.branch("32");
    static final ASN1ObjectIdentifier secp224r1 = ellipticCurve.branch("33");
    static final ASN1ObjectIdentifier secp384r1 = ellipticCurve.branch("34");
    static final ASN1ObjectIdentifier secp521r1 = ellipticCurve.branch("35");
    static final ASN1ObjectIdentifier sect409k1 = ellipticCurve.branch("36");
    static final ASN1ObjectIdentifier sect409r1 = ellipticCurve.branch("37");
    static final ASN1ObjectIdentifier sect571k1 = ellipticCurve.branch("38");
    static final ASN1ObjectIdentifier sect571r1 = ellipticCurve.branch("39");

    static final ASN1ObjectIdentifier secp192r1 = X9ObjectIdentifiers.prime192v1;
    static final ASN1ObjectIdentifier secp256r1 = X9ObjectIdentifiers.prime256v1;

}
