/*
    Copyright 2011 Google Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
/*
 * Original file from Android Open source project, modified by Code Aurora Forum
 * Copyright (c) 2010, Code Aurora Forum. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


#define SCALE_FILTER_NAME       MAKENAME(_filter_DX_shaderproc)

static void SCALE_FILTER_NAME(const SkBitmapProcState& s, int x, int y,
                              DSTTYPE* SK_RESTRICT colors, int count) {
    SkASSERT((s.fInvType & ~(SkMatrix::kTranslate_Mask |
                             SkMatrix::kScale_Mask)) == 0);
    SkASSERT(s.fInvKy == 0);
    SkASSERT(count > 0 && colors != NULL);
    SkASSERT(s.fDoFilter);
    SkDEBUGCODE(CHECKSTATE(s);)

    const unsigned maxX = s.fBitmap->width() - 1;
    const SkFixed oneX = s.fFilterOneX;
    const SkFixed dx = s.fInvSx;
    SkFixed fx;
    const SRCTYPE* SK_RESTRICT row0;
    const SRCTYPE* SK_RESTRICT row1;
    unsigned subY;

    {
        SkPoint pt;
        s.fInvProc(*s.fInvMatrix, SkIntToScalar(x) + SK_ScalarHalf,
                   SkIntToScalar(y) + SK_ScalarHalf, &pt);
        SkFixed fy = SkScalarToFixed(pt.fY) - (s.fFilterOneY >> 1);
        const unsigned maxY = s.fBitmap->height() - 1;
        // compute our two Y values up front
        subY = TILEY_LOW_BITS(fy, maxY);
        int y0 = TILEY_PROCF(fy, maxY);
        int y1 = TILEY_PROCF((fy + s.fFilterOneY), maxY);

        const char* SK_RESTRICT srcAddr = (const char*)s.fBitmap->getPixels();
        unsigned rb = s.fBitmap->rowBytes();
        row0 = (const SRCTYPE*)(srcAddr + y0 * rb);
        row1 = (const SRCTYPE*)(srcAddr + y1 * rb);
        // now initialize fx
        fx = SkScalarToFixed(pt.fX) - (oneX >> 1);
    }

#ifdef PREAMBLE
    PREAMBLE(s);
#endif
    
#ifdef S32_OPAQUE_D32_FILTER_DX_NEON
    int post_count;
    SkFixed post_fx;
    DSTTYPE* SK_RESTRICT post_colors;
    int num;
    post_count = count;
    post_fx = fx;
    post_colors = colors;


    if (dx>=0)
    {
        int end = ((int)maxX-1)<<16;
        num = (end-fx)/dx;
        if (num < 0) num = 0;

        if (num<count)
        {
            count = num;
            post_count = post_count - count;
            post_fx = fx + count*dx;
            post_colors = post_colors + count;
        }
        else
            post_count = 0;

        while (fx<0 && count) {
            unsigned subX = TILEX_LOW_BITS(fx, maxX);
            unsigned x0 = TILEX_PROCF(fx, maxX);
            unsigned x1 = TILEX_PROCF((fx + oneX), maxX);

            FILTER_PROC(subX, subY,
                SRC_TO_FILTER(row0[x0]),
                SRC_TO_FILTER(row0[x1]),
                SRC_TO_FILTER(row1[x0]),
                SRC_TO_FILTER(row1[x1]),
                colors);
            colors += 1;

            fx += dx;
            count--;
        }
    }
    else
    {
        int end = 0;
        int maxXFix = ((int)maxX-1)<<16;
        num = (end-fx)/dx;
        if (num < 0) num = 0;


        if (num<count)
        {
            count = num;
            post_count = post_count - count;
            post_fx = fx + count*dx;
            post_colors = post_colors + count;
        }
        else
            post_count = 0;

        while (fx>=maxXFix && count) {
            unsigned subX = TILEX_LOW_BITS(fx, maxX);
            unsigned x0 = TILEX_PROCF(fx, maxX);
            unsigned x1 = TILEX_PROCF((fx + oneX), maxX);

            FILTER_PROC(subX, subY,
                SRC_TO_FILTER(row0[x0]),
                SRC_TO_FILTER(row0[x1]),
                SRC_TO_FILTER(row1[x0]),
                SRC_TO_FILTER(row1[x1]),
                colors);
            colors += 1;

            fx += dx;
            count--;
        }

    }

    S32_Opaque_D32_filter_DX_shaderproc_neon(row0, row1, fx, maxX, subY, colors, dx, count);

    fx = post_fx;
    colors = post_colors;
    while (post_count) {
        unsigned subX = TILEX_LOW_BITS(fx, maxX);
        unsigned x0 = TILEX_PROCF(fx, maxX);
        unsigned x1 = TILEX_PROCF((fx + oneX), maxX);

        FILTER_PROC(subX, subY,
            SRC_TO_FILTER(row0[x0]),
            SRC_TO_FILTER(row0[x1]),
            SRC_TO_FILTER(row1[x0]),
            SRC_TO_FILTER(row1[x1]),
            colors);
        colors += 1;

        fx += dx;
        post_count--;
    }


#else //S32_OPAQUE_D32_FILTER_DX_NEON

    do {
        unsigned subX = TILEX_LOW_BITS(fx, maxX);
        unsigned x0 = TILEX_PROCF(fx, maxX);
        unsigned x1 = TILEX_PROCF((fx + oneX), maxX);

        FILTER_PROC(subX, subY,
                    SRC_TO_FILTER(row0[x0]),
                    SRC_TO_FILTER(row0[x1]),
                    SRC_TO_FILTER(row1[x0]),
                    SRC_TO_FILTER(row1[x1]),
                    colors);
        colors += 1;

        fx += dx;
    } while (--count != 0);
#endif //S32_OPAQUE_D32_FILTER_DX_NEON

#ifdef POSTAMBLE
    POSTAMBLE(s);
#endif
}

///////////////////////////////////////////////////////////////////////////////

#undef TILEX_PROCF
#undef TILEY_PROCF
#undef TILEX_LOW_BITS
#undef TILEY_LOW_BITS
#undef MAKENAME
#undef SRCTYPE
#undef DSTTYPE
#undef CHECKSTATE
#undef SRC_TO_FILTER
#undef FILTER_TO_DST
#undef PREAMBLE
#undef POSTAMBLE

#undef SCALE_FILTER_NAME
