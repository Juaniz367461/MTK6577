/* ------------------------------------------------------------------
 * Copyright (C) 1998-2009 PacketVideo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 * -------------------------------------------------------------------
 */
/*

 Filename: ps_hybrid_synthesis.c

------------------------------------------------------------------------------
 REVISION HISTORY


 Who:                                   Date: MM/DD/YYYY
 Description:

------------------------------------------------------------------------------
 INPUT AND OUTPUT DEFINITIONS



------------------------------------------------------------------------------
 FUNCTION DESCRIPTION

        Hybrid synthesis

     _______                                              ________
    |       |                                  _______   |        |
  ->|Hybrid | LF ----                         |       |->| Hybrid |-->
    | Anal. |        |                        |       |  | Synth  |   QMF -> L
     -------         o----------------------->|       |   --------    Synth
QMF                  |                s_k(n)  |Stereo |-------------->
Anal.              -------------------------->|       |
     _______       | |                        |       |   ________
    |       | HF --o |   -----------          |Process|  |        |
  ->| Delay |      |  ->|           |-------->|       |->| Hybrid |-->
     -------       |    |decorrelate| d_k(n)  |       |  | Synth  |   QMF -> R
                   ---->|           |-------->|       |   --------    Synth
                         -----------          |_______|-------------->


------------------------------------------------------------------------------
 REQUIREMENTS


------------------------------------------------------------------------------
 REFERENCES

SC 29 Software Copyright Licencing Disclaimer:

This software module was originally developed by
  Coding Technologies

and edited by
  -

in the course of development of the ISO/IEC 13818-7 and ISO/IEC 14496-3
standards for reference purposes and its performance may not have been
optimized. This software module is an implementation of one or more tools as
specified by the ISO/IEC 13818-7 and ISO/IEC 14496-3 standards.
ISO/IEC gives users free license to this software module or modifications
thereof for use in products claiming conformance to audiovisual and
image-coding related ITU Recommendations and/or ISO/IEC International
Standards. ISO/IEC gives users the same free license to this software module or
modifications thereof for research purposes and further ISO/IEC standardisation.
Those intending to use this software module in products are advised that its
use may infringe existing patents. ISO/IEC have no liability for use of this
software module or modifications thereof. Copyright is not released for
products that do not conform to audiovisual and image-coding related ITU
Recommendations and/or ISO/IEC International Standards.
The original developer retains full right to modify and use the code for its
own purpose, assign or donate the code to a third party and to inhibit third
parties from using the code for products that do not conform to audiovisual and
image-coding related ITU Recommendations and/or ISO/IEC International Standards.
This copyright notice must be included in all copies or derivative works.
Copyright (c) ISO/IEC 2003.

------------------------------------------------------------------------------
 PSEUDO-CODE

------------------------------------------------------------------------------
*/


/*----------------------------------------------------------------------------
; INCLUDES
----------------------------------------------------------------------------*/

#ifdef AAC_PLUS

#ifdef PARAMETRICSTEREO

#include "s_hybrid.h"
#include "ps_hybrid_synthesis.h"
#include "fxp_mul32.h"

/*----------------------------------------------------------------------------
; MACROS
; Define module specific macros here
----------------------------------------------------------------------------*/


/*----------------------------------------------------------------------------
; DEFINES
; Include all pre-processor statements here. Include conditional
; compile variables also.
----------------------------------------------------------------------------*/
#ifndef min
#define min(a, b) ((a) < (b) ? (a) : (b))
#endif

/*----------------------------------------------------------------------------
; LOCAL FUNCTION DEFINITIONS
; Function Prototype declaration
----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------
; LOCAL STORE/BUFFER/POINTER DEFINITIONS
; Variable declaration - defined here and used outside this module
----------------------------------------------------------------------------*/


/*----------------------------------------------------------------------------
; EXTERNAL FUNCTION REFERENCES
; Declare functions defined elsewhere and referenced in this module
----------------------------------------------------------------------------*/
/*----------------------------------------------------------------------------
; FUNCTION CODE
----------------------------------------------------------------------------*/
void ps_hybrid_synthesis(const Int32 *mHybridReal,
                         const Int32 *mHybridImag,
                         Int32 *mQmfReal,
                         Int32 *mQmfImag,
                         HYBRID *hHybrid)
{
    Int32  k;
    Int32  band;
    HYBRID_RES hybridRes;

    Int32 real;
    Int32 imag;
    Int32 *ptr_mQmfReal = mQmfReal;
    Int32 *ptr_mQmfImag = mQmfImag;
    const Int32 *ptr_mHybrid_Re = mHybridReal;
    const Int32 *ptr_mHybrid_Im = mHybridImag;

    for (band = 0; band < hHybrid->nQmfBands; band++)
    {
        hybridRes = (HYBRID_RES)(min(hHybrid->pResolution[band], 6) - 2);

#ifndef ANDROID_DEFAULT_CODE
        
        real  = *(ptr_mHybrid_Re++);
        real  = qadd(real, *(ptr_mHybrid_Re++));

        imag  = *(ptr_mHybrid_Im++);
        imag  = qadd(imag, *(ptr_mHybrid_Im++));
#else
        real  = *(ptr_mHybrid_Re++);
        real += *(ptr_mHybrid_Re++);
        imag  = *(ptr_mHybrid_Im++);
        imag += *(ptr_mHybrid_Im++);
#endif        

        for (k = (hybridRes >> 1); k != 0; k--)    /*  hybridRes = { 2,4,6 }  */
        {
#ifndef ANDROID_DEFAULT_CODE
            real  = qadd(real, *(ptr_mHybrid_Re++));
            real  = qadd(real, *(ptr_mHybrid_Re++));
            imag  = qadd(imag, *(ptr_mHybrid_Im++));
            imag  = qadd(imag, *(ptr_mHybrid_Im++));
#else
            real += *(ptr_mHybrid_Re++);
            real += *(ptr_mHybrid_Re++);
            imag += *(ptr_mHybrid_Im++);
            imag += *(ptr_mHybrid_Im++);
#endif            
        }

        *(ptr_mQmfReal++) = real;
        *(ptr_mQmfImag++) = imag;
    }
}

#endif


#endif

