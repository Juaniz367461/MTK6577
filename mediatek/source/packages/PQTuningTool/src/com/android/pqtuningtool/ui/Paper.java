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

import  com.android.pqtuningtool.common.Utils;
import  com.android.pqtuningtool.ui.PositionRepository.Position;
import  com.android.pqtuningtool.util.GalleryUtils;

import android.opengl.Matrix;
import android.os.SystemClock;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

// This class does the overscroll effect.
class Paper {
    private static final String TAG = "Paper";
    private static final int ROTATE_FACTOR = 4;
    private EdgeAnimation mAnimationLeft = new EdgeAnimation();
    private EdgeAnimation mAnimationRight = new EdgeAnimation();
    private int mWidth, mHeight;
    private float[] mMatrix = new float[16];

    public void overScroll(float distance) {
        distance /= mWidth;  // make it relative to width
        if (distance < 0) {
            mAnimationLeft.onPull(-distance);
        } else {
            mAnimationRight.onPull(distance);
        }
    }

    public void edgeReached(float velocity) {
        velocity /= mWidth;  // make it relative to width
        if (velocity < 0) {
            mAnimationRight.onAbsorb(-velocity);
        } else {
            mAnimationLeft.onAbsorb(velocity);
        }
    }

    public void onRelease() {
        mAnimationLeft.onRelease();
        mAnimationRight.onRelease();
    }

    public boolean advanceAnimation() {
        // Note that we use "|" because we want both animations get updated.
        return mAnimationLeft.update() | mAnimationRight.update();
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public float[] getTransform(Position target, Position base,
            float scrollX, float scrollY) {
        float left = mAnimationLeft.getValue();
        float right = mAnimationRight.getValue();
        float screenX = target.x - scrollX;
        // We linearly interpolate the value [left, right] for the screenX
        // range int [-1/4, 5/4]*mWidth. So if part of the thumbnail is outside
        // the screen, we still get some transform.
        float x = screenX + mWidth / 4;
        int range = 3 * mWidth / 2;
        float t = ((range - x) * left - x * right) / range;
        // compress t to the range (-1, 1) by the function
        // f(t) = (1 / (1 + e^-t) - 0.5) * 2
        // then multiply by 90 to make the range (-45, 45)
        float degrees =
                (1 / (1 + (float) Math.exp(-t * ROTATE_FACTOR)) - 0.5f) * 2 * -45;
        Matrix.setIdentityM(mMatrix, 0);
        Matrix.translateM(mMatrix, 0, mMatrix, 0, base.x, base.y, base.z);
        Matrix.rotateM(mMatrix, 0, degrees, 0, 1, 0);
        Matrix.translateM(mMatrix, 0, mMatrix, 0,
                target.x - base.x, target.y - base.y, target.z - base.z);
        return mMatrix;
    }
}

// This class follows the structure of frameworks's EdgeEffect class.
class EdgeAnimation {
    private static final String TAG = "EdgeAnimation";

    private static final int STATE_IDLE = 0;
    private static final int STATE_PULL = 1;
    private static final int STATE_ABSORB = 2;
    private static final int STATE_RELEASE = 3;

    // Time it will take the effect to fully done in ms
    private static final int ABSORB_TIME = 200;
    private static final int RELEASE_TIME = 500;

    private static final float VELOCITY_FACTOR = 0.1f;

    private final Interpolator mInterpolator;

    private int mState;
    private long mAnimationStartTime;
    private float mValue;

    private float mValueStart;
    private float mValueFinish;
    private long mStartTime;
    private long mDuration;

    public EdgeAnimation() {
        mInterpolator = new DecelerateInterpolator();
        mState = STATE_IDLE;
    }

    private void startAnimation(float start, float finish, long duration,
            int newState) {
        mValueStart = start;
        mValueFinish = finish;
        mDuration = duration;
        mStartTime = now();
        mState = newState;
    }

    // The deltaDistance's magnitude is in the range of -1 (no change) to 1.
    // The value 1 is the full length of the view. Negative values means the
    // movement is in the opposite direction.
    public void onPull(float deltaDistance) {
        if (mState == STATE_ABSORB) return;
        mValue = Utils.clamp(mValue + deltaDistance, -1.0f, 1.0f);
        mState = STATE_PULL;
    }

    public void onRelease() {
        if (mState == STATE_IDLE || mState == STATE_ABSORB) return;
        startAnimation(mValue, 0, RELEASE_TIME, STATE_RELEASE);
    }

    public void onAbsorb(float velocity) {
        float finish = Utils.clamp(mValue + velocity * VELOCITY_FACTOR,
                -1.0f, 1.0f);
        startAnimation(mValue, finish, ABSORB_TIME, STATE_ABSORB);
    }

    public boolean update() {
        if (mState == STATE_IDLE) return false;
        if (mState == STATE_PULL) return true;

        float t = Utils.clamp((float)(now() - mStartTime) / mDuration, 0.0f, 1.0f);
        /* Use linear interpolation for absorb, quadratic for others */
        float interp = (mState == STATE_ABSORB)
                ? t : mInterpolator.getInterpolation(t);

        mValue = mValueStart + (mValueFinish - mValueStart) * interp;

        if (t >= 1.0f) {
            switch (mState) {
                case STATE_ABSORB:
                    startAnimation(mValue, 0, RELEASE_TIME, STATE_RELEASE);
                    break;
                case STATE_RELEASE:
                    mState = STATE_IDLE;
                    break;
            }
        }

        return true;
    }

    public float getValue() {
        return mValue;
    }

    private long now() {
        return SystemClock.uptimeMillis();
    }
}
