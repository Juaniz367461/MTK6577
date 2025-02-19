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
 * Copyright (C) 2010 The Android Open Source Project
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

package  com.android.pqtuningtool.ui;

import java.nio.Buffer;
import java.util.HashMap;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class GLMock extends GLStub {
    @SuppressWarnings("unused")
    private static final String TAG = "GLMock";

    // glClear
    int mGLClearCalled;
    int mGLClearMask;
    // glBlendFunc
    int mGLBlendFuncCalled;
    int mGLBlendFuncSFactor;
    int mGLBlendFuncDFactor;
    // glColor4[fx]
    int mGLColorCalled;
    int mGLColor;
    // glEnable, glDisable
    boolean mGLBlendEnabled;
    boolean mGLStencilEnabled;
    // glEnableClientState
    boolean mGLVertexArrayEnabled;
    // glVertexPointer
    PointerInfo mGLVertexPointer;
    // glMatrixMode
    int mGLMatrixMode = GL10.GL_MODELVIEW;
    // glLoadMatrixf
    float[] mGLModelViewMatrix = new float[16];
    float[] mGLProjectionMatrix = new float[16];
    // glBindTexture
    int mGLBindTextureId;
    // glTexEnvf
    HashMap<Integer, Float> mGLTexEnv0 = new HashMap<Integer, Float>();
    HashMap<Integer, Float> mGLTexEnv1 = new HashMap<Integer, Float>();
    // glActiveTexture
    int mGLActiveTexture = GL11.GL_TEXTURE0;

    @Override
    public void glClear(int mask) {
        mGLClearCalled++;
        mGLClearMask = mask;
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        mGLBlendFuncSFactor = sfactor;
        mGLBlendFuncDFactor = dfactor;
        mGLBlendFuncCalled++;
    }

    @Override
    public void glColor4f(float red, float green, float blue,
        float alpha) {
        mGLColorCalled++;
        mGLColor = makeColor4f(red, green, blue, alpha);
    }

    @Override
    public void glColor4x(int red, int green, int blue, int alpha) {
        mGLColorCalled++;
        mGLColor = makeColor4x(red, green, blue, alpha);
    }

    @Override
    public void glEnable(int cap) {
        if (cap == GL11.GL_BLEND) {
            mGLBlendEnabled = true;
        } else if (cap == GL11.GL_STENCIL_TEST) {
            mGLStencilEnabled = true;
        }
    }

    @Override
    public void glDisable(int cap) {
        if (cap == GL11.GL_BLEND) {
            mGLBlendEnabled = false;
        } else if (cap == GL11.GL_STENCIL_TEST) {
            mGLStencilEnabled = false;
        }
    }

    @Override
    public void glEnableClientState(int array) {
        if (array == GL10.GL_VERTEX_ARRAY) {
           mGLVertexArrayEnabled = true;
        }
    }

    @Override
    public void glVertexPointer(int size, int type, int stride, Buffer pointer) {
        mGLVertexPointer = new PointerInfo(size, type, stride, pointer);
    }

    @Override
    public void glMatrixMode(int mode) {
        mGLMatrixMode = mode;
    }

    @Override
    public void glLoadMatrixf(float[] m, int offset) {
        if (mGLMatrixMode == GL10.GL_MODELVIEW) {
            System.arraycopy(m, offset, mGLModelViewMatrix, 0, 16);
        } else if (mGLMatrixMode == GL10.GL_PROJECTION) {
            System.arraycopy(m, offset, mGLProjectionMatrix, 0, 16);
        }
    }

    @Override
    public void glOrthof(
        float left, float right, float bottom, float top,
        float zNear, float zFar) {
        float tx = -(right + left) / (right - left);
        float ty = -(top + bottom) / (top - bottom);
            float tz = - (zFar + zNear) / (zFar - zNear);
            float[] m = new float[] {
                    2 / (right - left), 0, 0,  0,
                    0, 2 / (top - bottom), 0,  0,
                    0, 0, -2 / (zFar - zNear), 0,
                    tx, ty, tz, 1
            };
            glLoadMatrixf(m, 0);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        if (target == GL11.GL_TEXTURE_2D) {
            mGLBindTextureId = texture;
        }
    }

    @Override
    public void glTexEnvf(int target, int pname, float param) {
        if (target == GL11.GL_TEXTURE_ENV) {
            if (mGLActiveTexture == GL11.GL_TEXTURE0) {
                mGLTexEnv0.put(pname, param);
            } else if (mGLActiveTexture == GL11.GL_TEXTURE1) {
                mGLTexEnv1.put(pname, param);
            } else {
                throw new AssertionError();
            }
        }
    }

    public int getTexEnvi(int pname) {
        return getTexEnvi(mGLActiveTexture, pname);
    }

    public int getTexEnvi(int activeTexture, int pname) {
        if (activeTexture == GL11.GL_TEXTURE0) {
            return (int) mGLTexEnv0.get(pname).floatValue();
        } else if (activeTexture == GL11.GL_TEXTURE1) {
            return (int) mGLTexEnv1.get(pname).floatValue();
        } else {
            throw new AssertionError();
        }
    }

    @Override
    public void glActiveTexture(int texture) {
        mGLActiveTexture = texture;
    }

    public static int makeColor4f(float red, float green, float blue,
            float alpha) {
        return (Math.round(alpha * 255) << 24) |
                (Math.round(red * 255) << 16) |
                (Math.round(green * 255) << 8) |
                Math.round(blue * 255);
    }

    public static int makeColor4x(int red, int green, int blue, int alpha) {
        final float X = 65536f;
        return makeColor4f(red / X, green / X, blue / X, alpha / X);
    }
}
