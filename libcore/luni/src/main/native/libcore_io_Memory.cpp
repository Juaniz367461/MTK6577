/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "Memory"

#include "JNIHelp.h"
#include "JniConstants.h"
#include "ScopedBytes.h"
#include "ScopedPrimitiveArray.h"
#include "UniquePtr.h"

#include <byteswap.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <signal.h>

#if defined(__arm__)
// 32-bit ARM has load/store alignment restrictions for longs.
#define LONG_ALIGNMENT_MASK 0x3
#elif defined(__i386__)
// x86 can load anything at any alignment.
#define LONG_ALIGNMENT_MASK 0x0
#else
#error unknown load/store alignment restrictions for this architecture
#endif

template <typename T> static T cast(jint address) {
    return reinterpret_cast<T>(static_cast<uintptr_t>(address));
}

static inline void swapShorts(jshort* dstShorts, const jshort* srcShorts, size_t count) {
    // Do 32-bit swaps as long as possible...
    jint* dst = reinterpret_cast<jint*>(dstShorts);
    const jint* src = reinterpret_cast<const jint*>(srcShorts);
    for (size_t i = 0; i < count / 2; ++i) {
        jint v = *src++;                            // v=ABCD
        v = bswap_32(v);                            // v=DCBA
        jint v2 = (v << 16) | ((v >> 16) & 0xffff); // v=BADC
        *dst++ = v2;
    }
    // ...with one last 16-bit swap if necessary.
    if ((count % 2) != 0) {
        jshort v = *reinterpret_cast<const jshort*>(src);
        *reinterpret_cast<jshort*>(dst) = bswap_16(v);
    }
}

static inline void swapInts(jint* dstInts, const jint* srcInts, size_t count) {
    for (size_t i = 0; i < count; ++i) {
        jint v = *srcInts++;
        *dstInts++ = bswap_32(v);
    }
}

static inline void swapLongs(jlong* dstLongs, const jlong* srcLongs, size_t count) {
    jint* dst = reinterpret_cast<jint*>(dstLongs);
    const jint* src = reinterpret_cast<const jint*>(srcLongs);
    for (size_t i = 0; i < count; ++i) {
        jint v1 = *src++;
        jint v2 = *src++;
        *dst++ = bswap_32(v2);
        *dst++ = bswap_32(v1);
    }
}

static void Memory_memmove(JNIEnv* env, jclass, jobject dstObject, jint dstOffset, jobject srcObject, jint srcOffset, jlong length) {
    ScopedBytesRW dstBytes(env, dstObject);
    if (dstBytes.get() == NULL) {
        return;
    }
    ScopedBytesRO srcBytes(env, srcObject);
    if (srcBytes.get() == NULL) {
        return;
    }
    memmove(dstBytes.get() + dstOffset, srcBytes.get() + srcOffset, length);
}

static jbyte Memory_peekByte(JNIEnv*, jclass, jint srcAddress) {
    return *cast<const jbyte*>(srcAddress);
}

static void Memory_peekByteArray(JNIEnv* env, jclass, jint srcAddress, jbyteArray dst, jint dstOffset, jint byteCount) {
    env->SetByteArrayRegion(dst, dstOffset, byteCount, cast<const jbyte*>(srcAddress));
}

// Implements the peekXArray methods:
// - For unswapped access, we just use the JNI SetXArrayRegion functions.
// - For swapped access, we use GetXArrayElements and our own copy-and-swap routines.
//   GetXArrayElements is disproportionately cheap on Dalvik because it doesn't copy (as opposed
//   to Hotspot, which always copies). The SWAP_FN copies and swaps in one pass, which is cheaper
//   than copying and then swapping in a second pass. Depending on future VM/GC changes, the
//   swapped case might need to be revisited.
#define PEEKER(SCALAR_TYPE, JNI_NAME, SWAP_TYPE, SWAP_FN) { \
    if (swap) { \
        Scoped ## JNI_NAME ## ArrayRW elements(env, dst); \
        if (elements.get() == NULL) { \
            return; \
        } \
        const SWAP_TYPE* src = cast<const SWAP_TYPE*>(srcAddress); \
        SWAP_FN(reinterpret_cast<SWAP_TYPE*>(elements.get()) + dstOffset, src, count); \
    } else { \
        const SCALAR_TYPE* src = cast<const SCALAR_TYPE*>(srcAddress); \
        env->Set ## JNI_NAME ## ArrayRegion(dst, dstOffset, count, src); \
    } \
}

static void Memory_peekCharArray(JNIEnv* env, jclass, jint srcAddress, jcharArray dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(jchar, Char, jshort, swapShorts);
}

static void Memory_peekDoubleArray(JNIEnv* env, jclass, jint srcAddress, jdoubleArray dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(jdouble, Double, jlong, swapLongs);
}

static void Memory_peekFloatArray(JNIEnv* env, jclass, jint srcAddress, jfloatArray dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(jfloat, Float, jint, swapInts);
}

static void Memory_peekIntArray(JNIEnv* env, jclass, jint srcAddress, jintArray dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(jint, Int, jint, swapInts);
}

static void Memory_peekLongArray(JNIEnv* env, jclass, jint srcAddress, jlongArray dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(jlong, Long, jlong, swapLongs);
}

static void Memory_peekShortArray(JNIEnv* env, jclass, jint srcAddress, jshortArray dst, jint dstOffset, jint count, jboolean swap) {
    PEEKER(jshort, Short, jshort, swapShorts);
}

static void Memory_pokeByte(JNIEnv*, jclass, jint dstAddress, jbyte value) {
    *cast<jbyte*>(dstAddress) = value;
}

static void Memory_pokeByteArray(JNIEnv* env, jclass, jint dstAddress, jbyteArray src, jint offset, jint length) {
    env->GetByteArrayRegion(src, offset, length, cast<jbyte*>(dstAddress));
}

// Implements the pokeXArray methods:
// - For unswapped access, we just use the JNI GetXArrayRegion functions.
// - For swapped access, we use GetXArrayElements and our own copy-and-swap routines.
//   GetXArrayElements is disproportionately cheap on Dalvik because it doesn't copy (as opposed
//   to Hotspot, which always copies). The SWAP_FN copies and swaps in one pass, which is cheaper
//   than copying and then swapping in a second pass. Depending on future VM/GC changes, the
//   swapped case might need to be revisited.
#define POKER(SCALAR_TYPE, JNI_NAME, SWAP_TYPE, SWAP_FN) { \
    if (swap) { \
        Scoped ## JNI_NAME ## ArrayRO elements(env, src); \
        if (elements.get() == NULL) { \
            return; \
        } \
        const SWAP_TYPE* src = reinterpret_cast<const SWAP_TYPE*>(elements.get()) + srcOffset; \
        SWAP_FN(cast<SWAP_TYPE*>(dstAddress), src, count); \
    } else { \
        env->Get ## JNI_NAME ## ArrayRegion(src, srcOffset, count, cast<SCALAR_TYPE*>(dstAddress)); \
    } \
}

static void Memory_pokeCharArray(JNIEnv* env, jclass, jint dstAddress, jcharArray src, jint srcOffset, jint count, jboolean swap) {
    POKER(jchar, Char, jshort, swapShorts);
}

static void Memory_pokeDoubleArray(JNIEnv* env, jclass, jint dstAddress, jdoubleArray src, jint srcOffset, jint count, jboolean swap) {
    POKER(jdouble, Double, jlong, swapLongs);
}

static void Memory_pokeFloatArray(JNIEnv* env, jclass, jint dstAddress, jfloatArray src, jint srcOffset, jint count, jboolean swap) {
    POKER(jfloat, Float, jint, swapInts);
}

static void Memory_pokeIntArray(JNIEnv* env, jclass, jint dstAddress, jintArray src, jint srcOffset, jint count, jboolean swap) {
    POKER(jint, Int, jint, swapInts);
}

static void Memory_pokeLongArray(JNIEnv* env, jclass, jint dstAddress, jlongArray src, jint srcOffset, jint count, jboolean swap) {
    POKER(jlong, Long, jlong, swapLongs);
}

static void Memory_pokeShortArray(JNIEnv* env, jclass, jint dstAddress, jshortArray src, jint srcOffset, jint count, jboolean swap) {
    POKER(jshort, Short, jshort, swapShorts);
}

static jshort Memory_peekShort(JNIEnv*, jclass, jint srcAddress, jboolean swap) {
    jshort result = *cast<const jshort*>(srcAddress);
    if (swap) {
        result = bswap_16(result);
    }
    return result;
}

static void Memory_pokeShort(JNIEnv*, jclass, jint dstAddress, jshort value, jboolean swap) {
    if (swap) {
        value = bswap_16(value);
    }
    *cast<jshort*>(dstAddress) = value;
}

static jint Memory_peekInt(JNIEnv*, jclass, jint srcAddress, jboolean swap) {
    jint result = *cast<const jint*>(srcAddress);
    if (swap) {
        result = bswap_32(result);
    }
    return result;
}

#if 1
static bool sigBusCatched = false;

static void sigBusCatcher(int signum, siginfo_t* info, void* context)
{
    void* addr = info->si_addr;

    context = context;

    LOGE("Signal (%d) : %p\n", signum, addr);

    sigBusCatched = true;

    signal(SIGBUS, SIG_IGN);
}

static bool isReadableAddr(int p)
{
    int  cc;
    volatile int x;
    struct sigaction sa;
    struct sigaction old_sa;

    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = sigBusCatcher;
    sa.sa_flags = SA_SIGINFO;
    cc = sigaction(SIGBUS, &sa, &old_sa);
    if (cc != 0)
        return false;

    sigBusCatched = false;

    x = * ((int *) p);

    cc = sigaction(SIGBUS, &old_sa, NULL);

    if (sigBusCatched)
        return false;

    return true;
}
#endif

static void Memory_pokeInt(JNIEnv*, jclass, jint dstAddress, jint value, jboolean swap) {
    if (swap) {
        value = bswap_32(value);
    }
    *cast<jint*>(dstAddress) = value;
}

static jlong Memory_peekLong(JNIEnv*, jclass, jint srcAddress, jboolean swap) {
    jlong result;

    if ((srcAddress & LONG_ALIGNMENT_MASK) == 0) {
        if (isReadableAddr(srcAddress))  {
            result = *cast<const jlong*>(srcAddress);
        }
        else  {
            LOGD("Memory_peekLong : 0x%X", (int) srcAddress);
            result = 0;
        }
    } else {
        // Handle unaligned memory access one byte at a time
        const jbyte* src = cast<const jbyte*>(srcAddress);
        jbyte* dst = reinterpret_cast<jbyte*>(&result);
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
        dst[3] = src[3];
        dst[4] = src[4];
        dst[5] = src[5];
        dst[6] = src[6];
        dst[7] = src[7];
    }
    if (swap) {
        result = bswap_64(result);
    }
    return result;
}

static void Memory_pokeLong(JNIEnv*, jclass, jint dstAddress, jlong value, jboolean swap) {
    if (swap) {
        value = bswap_64(value);
    }
    if ((dstAddress & LONG_ALIGNMENT_MASK) == 0) {
        *cast<jlong*>(dstAddress) = value;
    } else {
        // Handle unaligned memory access one byte at a time
        const jbyte* src = reinterpret_cast<const jbyte*>(&value);
        jbyte* dst = cast<jbyte*>(dstAddress);
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
        dst[3] = src[3];
        dst[4] = src[4];
        dst[5] = src[5];
        dst[6] = src[6];
        dst[7] = src[7];
    }
}

static void unsafeBulkCopy(jbyte* dst, const jbyte* src, jint byteCount,
        jint sizeofElement, jboolean swap) {
    if (!swap) {
        memcpy(dst, src, byteCount);
        return;
    }

    if (sizeofElement == 2) {
        jshort* dstShorts = reinterpret_cast<jshort*>(dst);
        const jshort* srcShorts = reinterpret_cast<const jshort*>(src);
        swapShorts(dstShorts, srcShorts, byteCount / 2);
    } else if (sizeofElement == 4) {
        jint* dstInts = reinterpret_cast<jint*>(dst);
        const jint* srcInts = reinterpret_cast<const jint*>(src);
        swapInts(dstInts, srcInts, byteCount / 4);
    } else if (sizeofElement == 8) {
        jlong* dstLongs = reinterpret_cast<jlong*>(dst);
        const jlong* srcLongs = reinterpret_cast<const jlong*>(src);
        swapLongs(dstLongs, srcLongs, byteCount / 8);
    }
}

static void Memory_unsafeBulkGet(JNIEnv* env, jclass, jobject dstObject, jint dstOffset,
        jint byteCount, jbyteArray srcArray, jint srcOffset, jint sizeofElement, jboolean swap) {
    ScopedByteArrayRO srcBytes(env, srcArray);
    if (srcBytes.get() == NULL) {
        return;
    }
    jarray dstArray = reinterpret_cast<jarray>(dstObject);
    jbyte* dstBytes = reinterpret_cast<jbyte*>(env->GetPrimitiveArrayCritical(dstArray, NULL));
    if (dstBytes == NULL) {
        return;
    }
    jbyte* dst = dstBytes + dstOffset*sizeofElement;
    const jbyte* src = srcBytes.get() + srcOffset;
    unsafeBulkCopy(dst, src, byteCount, sizeofElement, swap);
    env->ReleasePrimitiveArrayCritical(dstArray, dstBytes, 0);
}

static void Memory_unsafeBulkPut(JNIEnv* env, jclass, jbyteArray dstArray, jint dstOffset,
        jint byteCount, jobject srcObject, jint srcOffset, jint sizeofElement, jboolean swap) {
    ScopedByteArrayRW dstBytes(env, dstArray);
    if (dstBytes.get() == NULL) {
        return;
    }
    jarray srcArray = reinterpret_cast<jarray>(srcObject);
    jbyte* srcBytes = reinterpret_cast<jbyte*>(env->GetPrimitiveArrayCritical(srcArray, NULL));
    if (srcBytes == NULL) {
        return;
    }
    jbyte* dst = dstBytes.get() + dstOffset;
    const jbyte* src = srcBytes + srcOffset*sizeofElement;
    unsafeBulkCopy(dst, src, byteCount, sizeofElement, swap);
    env->ReleasePrimitiveArrayCritical(srcArray, srcBytes, 0);
}

static JNINativeMethod gMethods[] = {
    NATIVE_METHOD(Memory, memmove, "(Ljava/lang/Object;ILjava/lang/Object;IJ)V"),
    NATIVE_METHOD(Memory, peekByte, "!(I)B"),
    NATIVE_METHOD(Memory, peekByteArray, "(I[BII)V"),
    NATIVE_METHOD(Memory, peekCharArray, "(I[CIIZ)V"),
    NATIVE_METHOD(Memory, peekDoubleArray, "(I[DIIZ)V"),
    NATIVE_METHOD(Memory, peekFloatArray, "(I[FIIZ)V"),
    NATIVE_METHOD(Memory, peekInt, "!(IZ)I"),
    NATIVE_METHOD(Memory, peekIntArray, "(I[IIIZ)V"),
    NATIVE_METHOD(Memory, peekLong, "!(IZ)J"),
    NATIVE_METHOD(Memory, peekLongArray, "(I[JIIZ)V"),
    NATIVE_METHOD(Memory, peekShort, "!(IZ)S"),
    NATIVE_METHOD(Memory, peekShortArray, "(I[SIIZ)V"),
    NATIVE_METHOD(Memory, pokeByte, "!(IB)V"),
    NATIVE_METHOD(Memory, pokeByteArray, "(I[BII)V"),
    NATIVE_METHOD(Memory, pokeCharArray, "(I[CIIZ)V"),
    NATIVE_METHOD(Memory, pokeDoubleArray, "(I[DIIZ)V"),
    NATIVE_METHOD(Memory, pokeFloatArray, "(I[FIIZ)V"),
    NATIVE_METHOD(Memory, pokeInt, "!(IIZ)V"),
    NATIVE_METHOD(Memory, pokeIntArray, "(I[IIIZ)V"),
    NATIVE_METHOD(Memory, pokeLong, "!(IJZ)V"),
    NATIVE_METHOD(Memory, pokeLongArray, "(I[JIIZ)V"),
    NATIVE_METHOD(Memory, pokeShort, "!(ISZ)V"),
    NATIVE_METHOD(Memory, pokeShortArray, "(I[SIIZ)V"),
    NATIVE_METHOD(Memory, unsafeBulkGet, "(Ljava/lang/Object;II[BIIZ)V"),
    NATIVE_METHOD(Memory, unsafeBulkPut, "([BIILjava/lang/Object;IIZ)V"),
};
int register_libcore_io_Memory(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "libcore/io/Memory", gMethods, NELEM(gMethods));
}
