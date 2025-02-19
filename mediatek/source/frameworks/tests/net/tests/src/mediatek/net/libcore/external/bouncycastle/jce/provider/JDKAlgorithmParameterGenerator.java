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

import java.security.AlgorithmParameterGeneratorSpi;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.DSAParameterSpec;

import javax.crypto.spec.DHGenParameterSpec;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
// BEGIN android-removed
// import javax.crypto.spec.RC2ParameterSpec;
// END android-removed

import org.bouncycastle.crypto.generators.DHParametersGenerator;
import org.bouncycastle.crypto.generators.DSAParametersGenerator;
// BEGIN android-removed
// import org.bouncycastle.crypto.generators.ElGamalParametersGenerator;
// import org.bouncycastle.crypto.generators.GOST3410ParametersGenerator;
// END android-removed
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DSAParameters;
// BEGIN android-removed
// import org.bouncycastle.crypto.params.ElGamalParameters;
// import org.bouncycastle.crypto.params.GOST3410Parameters;
// import org.bouncycastle.jce.spec.GOST3410ParameterSpec;
// import org.bouncycastle.jce.spec.GOST3410PublicKeyParameterSetSpec;
// END android-removed

public abstract class JDKAlgorithmParameterGenerator
    extends AlgorithmParameterGeneratorSpi
{
    protected SecureRandom  random;
    protected int           strength = 1024;

    protected void engineInit(
        int             strength,
        SecureRandom    random)
    {
        this.strength = strength;
        this.random = random;
    }

    public static class DH
        extends JDKAlgorithmParameterGenerator
    {
        private int l = 0;

        protected void engineInit(
            AlgorithmParameterSpec  genParamSpec,
            SecureRandom            random)
            throws InvalidAlgorithmParameterException
        {
            if (!(genParamSpec instanceof DHGenParameterSpec))
            {
                throw new InvalidAlgorithmParameterException("DH parameter generator requires a DHGenParameterSpec for initialisation");
            }
            DHGenParameterSpec  spec = (DHGenParameterSpec)genParamSpec;

            this.strength = spec.getPrimeSize();
            this.l = spec.getExponentSize();
            this.random = random;
        }

        protected AlgorithmParameters engineGenerateParameters()
        {
            DHParametersGenerator        pGen = new DHParametersGenerator();

            if (random != null)
            {
                pGen.init(strength, 20, random);
            }
            else
            {
                pGen.init(strength, 20, new SecureRandom());
            }

            DHParameters                p = pGen.generateParameters();

            AlgorithmParameters params;

            try
            {
                params = AlgorithmParameters.getInstance("DH", BouncyCastleProvider.PROVIDER_NAME);
                params.init(new DHParameterSpec(p.getP(), p.getG(), l));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e.getMessage());
            }

            return params;
        }
    }

    public static class DSA
        extends JDKAlgorithmParameterGenerator
    {
        protected void engineInit(
            int             strength,
            SecureRandom    random)
        {
            if (strength < 512 || strength > 1024 || strength % 64 != 0)
            {
                throw new InvalidParameterException("strength must be from 512 - 1024 and a multiple of 64");
            }

            this.strength = strength;
            this.random = random;
        }

        protected void engineInit(
            AlgorithmParameterSpec  genParamSpec,
            SecureRandom            random)
            throws InvalidAlgorithmParameterException
        {
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for DSA parameter generation.");
        }

        protected AlgorithmParameters engineGenerateParameters()
        {
            DSAParametersGenerator pGen = new DSAParametersGenerator();

            if (random != null)
            {
                pGen.init(strength, 20, random);
            }
            else
            {
                pGen.init(strength, 20, new SecureRandom());
            }

            DSAParameters p = pGen.generateParameters();

            AlgorithmParameters params;

            try
            {
                params = AlgorithmParameters.getInstance("DSA", BouncyCastleProvider.PROVIDER_NAME);
                params.init(new DSAParameterSpec(p.getP(), p.getQ(), p.getG()));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e.getMessage());
            }

            return params;
        }
    }

    // BEGIN android-removed
    // public static class GOST3410
    //     extends JDKAlgorithmParameterGenerator
    // {
    //     protected void engineInit(
    //             AlgorithmParameterSpec  genParamSpec,
    //             SecureRandom            random)
    //     throws InvalidAlgorithmParameterException
    //     {
    //         throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for GOST3410 parameter generation.");
    //     }
    //    
    //     protected AlgorithmParameters engineGenerateParameters()
    //     {
    //         GOST3410ParametersGenerator pGen = new GOST3410ParametersGenerator();
    //        
    //         if (random != null)
    //         {
    //             pGen.init(strength, 2, random);
    //         }
    //         else
    //         {
    //             pGen.init(strength, 2, new SecureRandom());
    //         }
    //        
    //         GOST3410Parameters p = pGen.generateParameters();
    //        
    //         AlgorithmParameters params;
    //
    //         try
    //         {
    //             params = AlgorithmParameters.getInstance("GOST3410", BouncyCastleProvider.PROVIDER_NAME);
    //             params.init(new GOST3410ParameterSpec(new GOST3410PublicKeyParameterSetSpec(p.getP(), p.getQ(), p.getA())));
    //         }
    //         catch (Exception e)
    //         {
    //             throw new RuntimeException(e.getMessage());
    //         }
    //
    //         return params;
    //     }
    // }
    //
    // public static class ElGamal
    //     extends JDKAlgorithmParameterGenerator
    // {
    //     private int l = 0;
    //
    //     protected void engineInit(
    //         AlgorithmParameterSpec  genParamSpec,
    //         SecureRandom            random)
    //         throws InvalidAlgorithmParameterException
    //     {
    //         if (!(genParamSpec instanceof DHGenParameterSpec))
    //         {
    //             throw new InvalidAlgorithmParameterException("DH parameter generator requires a DHGenParameterSpec for initialisation");
    //         }
    //         DHGenParameterSpec  spec = (DHGenParameterSpec)genParamSpec;
    //
    //         this.strength = spec.getPrimeSize();
    //         this.l = spec.getExponentSize();
    //         this.random = random;
    //     }
    //
    //     protected AlgorithmParameters engineGenerateParameters()
    //     {
    //         ElGamalParametersGenerator pGen = new ElGamalParametersGenerator();
    //
    //         if (random != null)
    //         {
    //             pGen.init(strength, 20, random);
    //         }
    //         else
    //         {
    //             pGen.init(strength, 20, new SecureRandom());
    //         }
    //
    //         ElGamalParameters p = pGen.generateParameters();
    //
    //         AlgorithmParameters params;
    //
    //         try
    //         {
    //             params = AlgorithmParameters.getInstance("ElGamal", BouncyCastleProvider.PROVIDER_NAME);
    //             params.init(new DHParameterSpec(p.getP(), p.getG(), l));
    //         }
    //         catch (Exception e)
    //         {
    //             throw new RuntimeException(e.getMessage());
    //         }
    //
    //         return params;
    //     }
    // }
    //
    // public static class DES
    //     extends JDKAlgorithmParameterGenerator
    // {
    //     protected void engineInit(
    //         AlgorithmParameterSpec  genParamSpec,
    //         SecureRandom            random)
    //         throws InvalidAlgorithmParameterException
    //     {
    //         throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for DES parameter generation.");
    //     }
    //
    //     protected AlgorithmParameters engineGenerateParameters()
    //     {
    //         byte[]  iv = new byte[8];
    //
    //         if (random == null)
    //         {
    //             random = new SecureRandom();
    //         }
    //
    //         random.nextBytes(iv);
    //
    //         AlgorithmParameters params;
    //
    //         try
    //         {
    //             params = AlgorithmParameters.getInstance("DES", BouncyCastleProvider.PROVIDER_NAME);
    //             params.init(new IvParameterSpec(iv));
    //         }
    //         catch (Exception e)
    //         {
    //             throw new RuntimeException(e.getMessage());
    //         }
    //
    //         return params;
    //     }
    // }
    //
    // public static class RC2
    //     extends JDKAlgorithmParameterGenerator
    // {
    //     RC2ParameterSpec    spec = null;
    //
    //     protected void engineInit(
    //         AlgorithmParameterSpec  genParamSpec,
    //         SecureRandom            random)
    //         throws InvalidAlgorithmParameterException
    //     {
    //         if (genParamSpec instanceof RC2ParameterSpec)
    //         {
    //             spec = (RC2ParameterSpec)genParamSpec;
    //             return;
    //         }
    //
    //         throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for RC2 parameter generation.");
    //     }
    //
    //     protected AlgorithmParameters engineGenerateParameters()
    //     {
    //         AlgorithmParameters params;
    //
    //         if (spec == null)
    //         {
    //             byte[]  iv = new byte[8];
    //
    //             if (random == null)
    //             {
    //                 random = new SecureRandom();
    //             }
    //
    //             random.nextBytes(iv);
    //
    //             try
    //             {
    //                 params = AlgorithmParameters.getInstance("RC2", BouncyCastleProvider.PROVIDER_NAME);
    //                 params.init(new IvParameterSpec(iv));
    //             }
    //             catch (Exception e)
    //             {
    //                 throw new RuntimeException(e.getMessage());
    //             }
    //         }
    //         else
    //         {
    //             try
    //             {
    //                 params = AlgorithmParameters.getInstance("RC2", BouncyCastleProvider.PROVIDER_NAME);
    //                 params.init(spec);
    //             }
    //             catch (Exception e)
    //             {
    //                 throw new RuntimeException(e.getMessage());
    //             }
    //         }
    //
    //         return params;
    //     }
    // }
    // END android-removed
}
