#include "SkBlitRow.h"
#include "SkColorPriv.h"
#include "SkUtils.h"

#define UNROLL

static void S32_Opaque_BlitRow32(SkPMColor* SK_RESTRICT dst,
                                 const SkPMColor* SK_RESTRICT src,
                                 int count, U8CPU alpha) {
    SkASSERT(255 == alpha);
    memcpy(dst, src, count * sizeof(SkPMColor));
}

static void S32_Blend_BlitRow32(SkPMColor* SK_RESTRICT dst,
                                const SkPMColor* SK_RESTRICT src,
                                int count, U8CPU alpha) {
    SkASSERT(alpha <= 255);
    if (count > 0) {
        unsigned src_scale = SkAlpha255To256(alpha);
        unsigned dst_scale = 256 - src_scale;

#ifdef UNROLL
        if (count & 1) {
            *dst = SkAlphaMulQ(*(src++), src_scale) + SkAlphaMulQ(*dst, dst_scale);
            dst += 1;
            count -= 1;
        }

        const SkPMColor* SK_RESTRICT srcEnd = src + count;
        while (src != srcEnd) {
            *dst = SkAlphaMulQ(*(src++), src_scale) + SkAlphaMulQ(*dst, dst_scale);
            dst += 1;
            *dst = SkAlphaMulQ(*(src++), src_scale) + SkAlphaMulQ(*dst, dst_scale);
            dst += 1;
        }
#else
        do {
            *dst = SkAlphaMulQ(*src, src_scale) + SkAlphaMulQ(*dst, dst_scale);
            src += 1;
            dst += 1;
        } while (--count > 0);
#endif
    }
}

//#define TEST_SRC_ALPHA

static void S32A_Opaque_BlitRow32(SkPMColor* SK_RESTRICT dst,
                                  const SkPMColor* SK_RESTRICT src,
                                  int count, U8CPU alpha) {
    SkASSERT(255 == alpha);
    if (count > 0) {
#ifdef UNROLL
        if (count & 1) {
            *dst = SkPMSrcOver(*(src++), *dst);
            dst += 1;
            count -= 1;
        }

        const SkPMColor* SK_RESTRICT srcEnd = src + count;
        while (src != srcEnd) {
            *dst = SkPMSrcOver(*(src++), *dst);
            dst += 1;
            *dst = SkPMSrcOver(*(src++), *dst);
            dst += 1;
        }
#else
        do {
#ifdef TEST_SRC_ALPHA
            SkPMColor sc = *src;
            if (sc) {
                unsigned srcA = SkGetPackedA32(sc);
                SkPMColor result = sc;
                if (srcA != 255) {
                    result = SkPMSrcOver(sc, *dst);
                }
                *dst = result;
            }
#else
            *dst = SkPMSrcOver(*src, *dst);
#endif
            src += 1;
            dst += 1;
        } while (--count > 0);
#endif
    }
}

static void S32A_Blend_BlitRow32(SkPMColor* SK_RESTRICT dst,
                                 const SkPMColor* SK_RESTRICT src,
                                 int count, U8CPU alpha) {
    SkASSERT(alpha <= 255);
    if (count > 0) {
#ifdef UNROLL
        if (count & 1) {
            *dst = SkBlendARGB32(*(src++), *dst, alpha);
            dst += 1;
            count -= 1;
        }

        const SkPMColor* SK_RESTRICT srcEnd = src + count;
        while (src != srcEnd) {
            *dst = SkBlendARGB32(*(src++), *dst, alpha);
            dst += 1;
            *dst = SkBlendARGB32(*(src++), *dst, alpha);
            dst += 1;
        }
#else
        do {
            *dst = SkBlendARGB32(*src, *dst, alpha);
            src += 1;
            dst += 1;
        } while (--count > 0);
#endif
    }
}

///////////////////////////////////////////////////////////////////////////////

static const SkBlitRow::Proc32 gDefault_Procs32[] = {
    S32_Opaque_BlitRow32,
    S32_Blend_BlitRow32,
    S32A_Opaque_BlitRow32,
    S32A_Blend_BlitRow32
};

SkBlitRow::Proc32 SkBlitRow::Factory32(unsigned flags) {
    SkASSERT(flags < SK_ARRAY_COUNT(gDefault_Procs32));
    // just so we don't crash
    flags &= kFlags32_Mask;

    SkBlitRow::Proc32 proc = PlatformProcs32(flags);
    if (NULL == proc) {
        proc = gDefault_Procs32[flags];
    }
    SkASSERT(proc);
    return proc;
}

SkBlitRow::Proc32 SkBlitRow::ColorProcFactory() {
    SkBlitRow::ColorProc proc = PlatformColorProc();
    if (NULL == proc) {
        proc = Color32;
    }
    SkASSERT(proc);
    return proc;
}

void SkBlitRow::Color32(SkPMColor dst[], const SkPMColor src[],
                        int count, SkPMColor color) {
    if (count > 0) {
        if (0 == color) {
            if (src != dst) {
                memcpy(dst, src, count * sizeof(SkPMColor));
            }
        }
        unsigned colorA = SkGetPackedA32(color);
        if (255 == colorA) {
            sk_memset32(dst, color, count);
        } else {
            unsigned scale = 256 - SkAlpha255To256(colorA);
            do {
                *dst = color + SkAlphaMulQ(*src, scale);
                src += 1;
                dst += 1;
            } while (--count);
        }
    }
}

///////////////////////////////////////////////////////////////////////////////

static void D32_Mask_Color(void* dst, size_t dstRB, SkBitmap::Config,
                           const uint8_t* mask, size_t maskRB, SkColor color,
                           int width, int height) {
    SkPMColor pmc = SkPreMultiplyColor(color);
    size_t dstOffset = dstRB - (width << 2);
    size_t maskOffset = maskRB - width;
    SkPMColor *device = (SkPMColor *)dst;
    do {
        int w = width;
        do {
            unsigned aa = *mask++;
            *device = SkBlendARGB32(pmc, *device, aa);
            device += 1;
        } while (--w != 0);
        device = (uint32_t*)((char*)device + dstOffset);
        mask += maskOffset;
    } while (--height != 0);
}

static void D32_Mask_Opaque(void* dst, size_t dstRB, SkBitmap::Config,
                            const uint8_t* mask, size_t maskRB, SkColor color,
                            int width, int height) {
    SkPMColor pmc = SkPreMultiplyColor(color);
    uint32_t* device = (uint32_t*)dst;

    maskRB -= width;
    dstRB -= (width << 2);
    do {
        int w = width;
        do {
            unsigned aa = *mask++;
            *device = SkAlphaMulQ(pmc, SkAlpha255To256(aa)) + SkAlphaMulQ(*device, SkAlpha255To256(255 - aa));
            device += 1;
        } while (--w != 0);
        device = (uint32_t*)((char*)device + dstRB);
        mask += maskRB;
    } while (--height != 0);
}

static void D32_Mask_Black(void* dst, size_t dstRB, SkBitmap::Config,
                           const uint8_t* mask, size_t maskRB, SkColor,
                           int width, int height) {
    uint32_t* device = (uint32_t*)dst;

    maskRB -= width;
    dstRB -= (width << 2);
    do {
        int w = width;
        do {
            unsigned aa = *mask++;
            *device = (aa << SK_A32_SHIFT) + SkAlphaMulQ(*device, SkAlpha255To256(255 - aa));
            device += 1;
        } while (--w != 0);
        device = (uint32_t*)((char*)device + dstRB);
        mask += maskRB;
    } while (--height != 0);
}

SkBlitMask::Proc SkBlitMask::Factory(SkBitmap::Config config, SkColor color) {
    SkBlitMask::Proc proc = PlatformProcs(config, color);
    //proc = NULL;
    if (NULL == proc) {
        switch (config) {
            case SkBitmap::kARGB_8888_Config:
                if (SK_ColorBLACK == color) {
                    proc = D32_Mask_Black;
                } else if (0xFF == SkColorGetA(color)) {
                    proc = D32_Mask_Opaque;
                } else {
                    proc = D32_Mask_Color;
                }
                break;
            default:
                break;
        }
    }
    return proc;
}

