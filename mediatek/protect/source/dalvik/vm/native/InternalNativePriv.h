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

/*
 * Copyright (C) 2008 The Android Open Source Project
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

/*
 * Declarations and definitions common to internal native code.
 */
#ifndef DALVIK_NATIVE_INTERNALNATIVEPRIV_H_
#define DALVIK_NATIVE_INTERNALNATIVEPRIV_H_

/*
 * Return macros.  Note we use "->i" instead of "->z" for boolean; this
 * is because the interpreter expects everything to be a 32-bit value.
 */
#ifdef NDEBUG
# define RETURN_VOID()           do { (void)(pResult); return; } while(0)
#else
# define RETURN_VOID()           do { pResult->i = 0xfefeabab; return; }while(0)
#endif
#define RETURN_BOOLEAN(_val)    do { pResult->i = (_val); return; } while(0)
#define RETURN_INT(_val)        do { pResult->i = (_val); return; } while(0)
#define RETURN_LONG(_val)       do { pResult->j = (_val); return; } while(0)
#define RETURN_FLOAT(_val)      do { pResult->f = (_val); return; } while(0)
#define RETURN_DOUBLE(_val)     do { pResult->d = (_val); return; } while(0)
#define RETURN_PTR(_val)        do { pResult->l = (Object*)(_val); return; } while(0)

/*
 * Normally a method that has an "inline native" will be invoked using
 * execute-inline. If the method is invoked via reflection, JNI, or by
 * virtual dispatch (in the case of String.equals, which we may arrive
 * at via Object.equals), we need a non-"inline native" implementation.
 *
 * This macro is used to implement the native methods that bridge this gap.
 */
#define MAKE_INTRINSIC_TRAMPOLINE(INTRINSIC_FN) \
    extern bool INTRINSIC_FN(u4 arg0, u4 arg1, u4 arg2, u4 arg3, \
            JValue* pResult); \
    INTRINSIC_FN(args[0], args[1], args[2], args[3], pResult);

/*
 * Verify that "obj" is non-null and is an instance of "clazz".
 *
 * Returns "false" and throws an exception if not.
 */
bool dvmVerifyObjectInClass(Object* obj, ClassObject* clazz);

/*
 * Find a class by name, initializing it if requested.
 */
ClassObject* dvmFindClassByName(StringObject* nameObj, Object* loader,
    bool doInit);

/*
 * We insert native method stubs for abstract methods so we don't have to
 * check the access flags at the time of the method call.  This results in
 * "native abstract" methods, which can't exist.  If we see the "abstract"
 * flag set, clear the "native" flag.
 *
 * We also move the DECLARED_SYNCHRONIZED flag into the SYNCHRONIZED
 * position, because the callers of this function are trying to convey
 * the "traditional" meaning of the flags to their callers.
 */
u4 dvmFixMethodFlags(u4 flags);

/*
 * dvmHashTableFree callback for some DexFile operations.
 */
void dvmFreeDexOrJar(void* vptr);

/*
 * Tables of methods.
 */
extern const DalvikNativeMethod dvm_java_lang_Object[];
extern const DalvikNativeMethod dvm_java_lang_Class[];
extern const DalvikNativeMethod dvm_java_lang_Double[];
extern const DalvikNativeMethod dvm_java_lang_Float[];
extern const DalvikNativeMethod dvm_java_lang_Math[];
extern const DalvikNativeMethod dvm_java_lang_Runtime[];
extern const DalvikNativeMethod dvm_java_lang_String[];
extern const DalvikNativeMethod dvm_java_lang_System[];
extern const DalvikNativeMethod dvm_java_lang_Throwable[];
extern const DalvikNativeMethod dvm_java_lang_VMClassLoader[];
extern const DalvikNativeMethod dvm_java_lang_VMThread[];
extern const DalvikNativeMethod dvm_java_lang_reflect_AccessibleObject[];
extern const DalvikNativeMethod dvm_java_lang_reflect_Array[];
extern const DalvikNativeMethod dvm_java_lang_reflect_Constructor[];
extern const DalvikNativeMethod dvm_java_lang_reflect_Field[];
extern const DalvikNativeMethod dvm_java_lang_reflect_Method[];
extern const DalvikNativeMethod dvm_java_lang_reflect_Proxy[];
extern const DalvikNativeMethod dvm_java_util_concurrent_atomic_AtomicLong[];
extern const DalvikNativeMethod dvm_dalvik_bytecode_OpcodeInfo[];
extern const DalvikNativeMethod dvm_dalvik_system_SamplingProfiler[];
extern const DalvikNativeMethod dvm_dalvik_system_VMDebug[];
extern const DalvikNativeMethod dvm_dalvik_system_DexFile[];
extern const DalvikNativeMethod dvm_dalvik_system_VMRuntime[];
extern const DalvikNativeMethod dvm_dalvik_system_Zygote[];
extern const DalvikNativeMethod dvm_dalvik_system_VMStack[];
extern const DalvikNativeMethod dvm_org_apache_harmony_dalvik_ddmc_DdmServer[];
extern const DalvikNativeMethod dvm_org_apache_harmony_dalvik_ddmc_DdmVmInternal[];
extern const DalvikNativeMethod dvm_org_apache_harmony_dalvik_NativeTestTarget[];
extern const DalvikNativeMethod dvm_sun_misc_Unsafe[];

#endif  // DALVIK_NATIVE_INTERNALNATIVEPRIV_H_
