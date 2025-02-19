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

import android.graphics.RectF;

import javax.microedition.khronos.opengles.GL11;

//
// GLCanvas gives a convenient interface to draw using OpenGL.
//
// When a rectangle is specified in this interface, it means the region
// [x, x+width) * [y, y+height)
//
public interface GLCanvas {
    // Tells GLCanvas the size of the underlying GL surface. This should be
    // called before first drawing and when the size of GL surface is changed.
    // This is called by GLRoot and should not be called by the clients
    // who only want to draw on the GLCanvas. Both width and height must be
    // nonnegative.
    public void setSize(int width, int height);

    // Clear the drawing buffers. This should only be used by GLRoot.
    public void clearBuffer();

    // This is the time value used to calculate the animation in the current
    // frame. The "set" function should only called by GLRoot, and the
    // "time" parameter must be nonnegative.
    public void setCurrentAnimationTimeMillis(long time);
    public long currentAnimationTimeMillis();

    public void setBlendEnabled(boolean enabled);

    // Sets and gets the current alpha, alpha must be in [0, 1].
    public void setAlpha(float alpha);
    public float getAlpha();

    // (current alpha) = (current alpha) * alpha
    public void multiplyAlpha(float alpha);

    // Change the current transform matrix.
    public void translate(float x, float y, float z);
    public void scale(float sx, float sy, float sz);
    public void rotate(float angle, float x, float y, float z);
    public void multiplyMatrix(float[] mMatrix, int offset);

    // Modifies the current clip with the specified rectangle.
    // (current clip) = (current clip) intersect (specified rectangle).
    // Returns true if the result clip is non-empty.
    public boolean clipRect(int left, int top, int right, int bottom);

    // Pushes the configuration state (matrix, alpha, and clip) onto
    // a private stack.
    public int save();

    // Same as save(), but only save those specified in saveFlags.
    public int save(int saveFlags);

    public static final int SAVE_FLAG_ALL = 0xFFFFFFFF;
    public static final int SAVE_FLAG_CLIP = 0x01;
    public static final int SAVE_FLAG_ALPHA = 0x02;
    public static final int SAVE_FLAG_MATRIX = 0x04;

    // Pops from the top of the stack as current configuration state (matrix,
    // alpha, and clip). This call balances a previous call to save(), and is
    // used to remove all modifications to the configuration state since the
    // last save call.
    public void restore();

    // Draws a line using the specified paint from (x1, y1) to (x2, y2).
    // (Both end points are included).
    public void drawLine(float x1, float y1, float x2, float y2, GLPaint paint);

    // Draws a rectangle using the specified paint from (x1, y1) to (x2, y2).
    // (Both end points are included).
    public void drawRect(float x1, float y1, float x2, float y2, GLPaint paint);

    // Fills the specified rectangle with the specified color.
    public void fillRect(float x, float y, float width, float height, int color);

    // Draws a texture to the specified rectangle.
    public void drawTexture(
            BasicTexture texture, int x, int y, int width, int height);
    public void drawMesh(BasicTexture tex, int x, int y, int xyBuffer,
            int uvBuffer, int indexBuffer, int indexCount);

    // Draws a texture to the specified rectangle. The "alpha" parameter
    // overrides the current drawing alpha value.
    public void drawTexture(BasicTexture texture,
            int x, int y, int width, int height, float alpha);

    // Draws a the source rectangle part of the texture to the target rectangle.
    public void drawTexture(BasicTexture texture, RectF source, RectF target);

    // Draw two textures to the specified rectangle. The actual texture used is
    // from * (1 - ratio) + to * ratio
    // The two textures must have the same size.
    public void drawMixed(BasicTexture from, BasicTexture to,
            float ratio, int x, int y, int w, int h);

    public void drawMixed(BasicTexture from, int toColor,
            float ratio, int x, int y, int w, int h);

    // Return a texture copied from the specified rectangle.
    public BasicTexture copyTexture(int x, int y, int width, int height);

    // Gets the underlying GL instance. This is used only when direct access to
    // GL is needed.
    public GL11 getGLInstance();

    // Unloads the specified texture from the canvas. The resource allocated
    // to draw the texture will be released. The specified texture will return
    // to the unloaded state. This function should be called only from
    // BasicTexture or its descendant
    public boolean unloadTexture(BasicTexture texture);

    // Delete the specified buffer object, similar to unloadTexture.
    public void deleteBuffer(int bufferId);

    // Delete the textures and buffers in GL side. This function should only be
    // called in the GL thread.
    public void deleteRecycledResources();

}
