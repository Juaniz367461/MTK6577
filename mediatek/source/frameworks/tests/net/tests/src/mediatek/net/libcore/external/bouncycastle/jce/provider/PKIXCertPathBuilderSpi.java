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

package org.bouncycastle.jce.provider;

import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathBuilderSpi;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.jce.exception.ExtCertPathBuilderException;
import org.bouncycastle.util.Selector;
import org.bouncycastle.x509.ExtendedPKIXBuilderParameters;
import org.bouncycastle.x509.X509CertStoreSelector;

/**
 * Implements the PKIX CertPathBuilding algorithm for BouncyCastle.
 * 
 * @see CertPathBuilderSpi
 */
public class PKIXCertPathBuilderSpi
    extends CertPathBuilderSpi
{
    /**
     * Build and validate a CertPath using the given parameter.
     * 
     * @param params PKIXBuilderParameters object containing all information to
     *            build the CertPath
     */
    public CertPathBuilderResult engineBuild(CertPathParameters params)
        throws CertPathBuilderException, InvalidAlgorithmParameterException
    {
        if (!(params instanceof PKIXBuilderParameters)
            && !(params instanceof ExtendedPKIXBuilderParameters))
        {
            throw new InvalidAlgorithmParameterException(
                "Parameters must be an instance of "
                    + PKIXBuilderParameters.class.getName() + " or "
                    + ExtendedPKIXBuilderParameters.class.getName() + ".");
        }

        ExtendedPKIXBuilderParameters pkixParams = null;
        if (params instanceof ExtendedPKIXBuilderParameters)
        {
            pkixParams = (ExtendedPKIXBuilderParameters) params;
        }
        else
        {
            pkixParams = (ExtendedPKIXBuilderParameters) ExtendedPKIXBuilderParameters
                .getInstance((PKIXBuilderParameters) params);
        }

        Collection targets;
        Iterator targetIter;
        List certPathList = new ArrayList();
        X509Certificate cert;

        // search target certificates

        Selector certSelect = pkixParams.getTargetConstraints();
        if (!(certSelect instanceof X509CertStoreSelector))
        {
            throw new CertPathBuilderException(
                "TargetConstraints must be an instance of "
                    + X509CertStoreSelector.class.getName() + " for "
                    + this.getClass().getName() + " class.");
        }

        try
        {
            targets = CertPathValidatorUtilities.findCertificates((X509CertStoreSelector)certSelect, pkixParams.getStores());
            targets.addAll(CertPathValidatorUtilities.findCertificates((X509CertStoreSelector)certSelect, pkixParams.getCertStores()));
        }
        catch (AnnotatedException e)
        {
            throw new ExtCertPathBuilderException(
                "Error finding target certificate.", e);
        }

        if (targets.isEmpty())
        {

            throw new CertPathBuilderException(
                "No certificate found matching targetContraints.");
        }

        CertPathBuilderResult result = null;

        // check all potential target certificates
        targetIter = targets.iterator();
        while (targetIter.hasNext() && result == null)
        {
            cert = (X509Certificate) targetIter.next();
            result = build(cert, pkixParams, certPathList);
        }

        if (result == null && certPathException != null)
        {
            if (certPathException instanceof AnnotatedException)
            {
                throw new CertPathBuilderException(certPathException.getMessage(), certPathException.getCause());
            }
            throw new CertPathBuilderException(
                "Possible certificate chain could not be validated.",
                certPathException);
        }

        if (result == null && certPathException == null)
        {
            throw new CertPathBuilderException(
                "Unable to find certificate chain.");
        }

        return result;
    }

    private Exception certPathException;

    protected CertPathBuilderResult build(X509Certificate tbvCert,
        ExtendedPKIXBuilderParameters pkixParams, List tbvPath)
    {
        // If tbvCert is readily present in tbvPath, it indicates having run
        // into a cycle in the
        // PKI graph.
        if (tbvPath.contains(tbvCert))
        {
            return null;
        }
        // step out, the certificate is not allowed to appear in a certification
        // chain.
        if (pkixParams.getExcludedCerts().contains(tbvCert))
        {
            return null;
        }
        // test if certificate path exceeds maximum length
        if (pkixParams.getMaxPathLength() != -1)
        {
            if (tbvPath.size() - 1 > pkixParams.getMaxPathLength())
            {
                return null;
            }
        }

        tbvPath.add(tbvCert);

        CertificateFactory cFact;
        CertPathValidator validator;
        CertPathBuilderResult builderResult = null;

        try
        {
            cFact = CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
            validator = CertPathValidator.getInstance("PKIX", BouncyCastleProvider.PROVIDER_NAME);
        }
        catch (Exception e)
        {
            // cannot happen
            throw new RuntimeException("Exception creating support classes.");
        }

        try
        {
            // check whether the issuer of <tbvCert> is a TrustAnchor
            if (CertPathValidatorUtilities.findTrustAnchor(tbvCert, pkixParams.getTrustAnchors(),
                pkixParams.getSigProvider()) != null)
            {
                // exception message from possibly later tried certification
                // chains
                CertPath certPath = null;
                PKIXCertPathValidatorResult result = null;
                try
                {
                    certPath = cFact.generateCertPath(tbvPath);
                }
                catch (Exception e)
                {
                    throw new AnnotatedException(
                        "Certification path could not be constructed from certificate list.",
                        e);
                }

                try
                {
                    result = (PKIXCertPathValidatorResult) validator.validate(
                        certPath, pkixParams);
                }
                catch (Exception e)
                {
                    throw new AnnotatedException(
                        "Certification path could not be validated.", e);
                }

                return new PKIXCertPathBuilderResult(certPath, result
                    .getTrustAnchor(), result.getPolicyTree(), result
                    .getPublicKey());

            }
            else
            {
                // add additional X.509 stores from locations in certificate
                try
                {
                    CertPathValidatorUtilities.addAdditionalStoresFromAltNames(
                        tbvCert, pkixParams);
                }
                catch (CertificateParsingException e)
                {
                    throw new AnnotatedException(
                        "No additiontal X.509 stores can be added from certificate locations.",
                        e);
                }
                Collection issuers = new HashSet();
                // try to get the issuer certificate from one
                // of the stores
                try
                {
                    issuers.addAll(CertPathValidatorUtilities.findIssuerCerts(tbvCert, pkixParams));
                }
                catch (AnnotatedException e)
                {
                    throw new AnnotatedException(
                        "Cannot find issuer certificate for certificate in certification path.",
                        e);
                }
                if (issuers.isEmpty())
                {
                    throw new AnnotatedException(
                        "No issuer certificate for certificate in certification path found.");
                }
                Iterator it = issuers.iterator();

                while (it.hasNext() && builderResult == null)
                {
                    X509Certificate issuer = (X509Certificate) it.next();
                    builderResult = build(issuer, pkixParams, tbvPath);
                }
            }
        }
        catch (AnnotatedException e)
        {
            certPathException = e;
        }
        if (builderResult == null)
        {
            tbvPath.remove(tbvCert);
        }
        return builderResult;
    }

}
