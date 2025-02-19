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

package  com.android.pqtuningtool.app;

import  com.android.pqtuningtool.common.Utils;
import  com.android.pqtuningtool.util.GalleryUtils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class EyePosition {
    private static final String TAG = "EyePosition";

    public interface EyePositionListener {
        public void onEyePositionChanged(float x, float y, float z);
    }

    private static final float GYROSCOPE_THRESHOLD = 0.15f;
    private static final float GYROSCOPE_LIMIT = 10f;
    private static final int GYROSCOPE_SETTLE_DOWN = 15;
    private static final float GYROSCOPE_RESTORE_FACTOR = 0.995f;

    private static final double USER_ANGEL = Math.toRadians(10);
    private static final float USER_ANGEL_COS = (float) Math.cos(USER_ANGEL);
    private static final float USER_ANGEL_SIN = (float) Math.sin(USER_ANGEL);
    private static final float MAX_VIEW_RANGE = (float) 0.5;
    private static final int NOT_STARTED = -1;

    private static final float USER_DISTANCE_METER = 0.3f;

    private Context mContext;
    private EyePositionListener mListener;
    private Display mDisplay;
    // The eyes' position of the user, the origin is at the center of the
    // device and the unit is in pixels.
    private float mX;
    private float mY;
    private float mZ;

    private final float mUserDistance; // in pixel
    private final float mLimit;
    private long mStartTime = NOT_STARTED;
    private Sensor mSensor;
    private PositionListener mPositionListener = new PositionListener();

    private int mGyroscopeCountdown = 0;

    public EyePosition(Context context, EyePositionListener listener) {
        mContext = context;
        mListener = listener;
        mUserDistance = GalleryUtils.meterToPixel(USER_DISTANCE_METER);
        mLimit = mUserDistance * MAX_VIEW_RANGE;

        WindowManager wManager = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        mDisplay = wManager.getDefaultDisplay();

        // The 3D effect where the photo albums fan out in 3D based on angle
        // of device tilt is currently disabled.
/*
        SensorManager sManager = (SensorManager) mContext
                .getSystemService(Context.SENSOR_SERVICE);
        mSensor = sManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (mSensor == null) {
            Log.w(TAG, "no gyroscope, use accelerometer instead");
            mSensor = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (mSensor == null) {
            Log.w(TAG, "no sensor available");
        }
*/
    }

    public void resetPosition() {
        mStartTime = NOT_STARTED;
        mX = mY = 0;
        mZ = -mUserDistance;
        mListener.onEyePositionChanged(mX, mY, mZ);
    }

    /*
     * We assume the user is at the following position
     *
     *              /|\  user's eye
     *               |   /
     *   -G(gravity) |  /
     *               |_/
     *             / |/_____\ -Y (-y direction of device)
     *     user angel
     */
    private void onAccelerometerChanged(float gx, float gy, float gz) {

        float x = gx, y = gy, z = gz;

        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_90: x = -gy; y= gx; break;
            case Surface.ROTATION_180: x = -gx; y = -gy; break;
            case Surface.ROTATION_270: x = gy; y = -gx; break;
        }

        float temp = x * x + y * y + z * z;
        float t = -y /temp;

        float tx = t * x;
        float ty = -1 + t * y;
        float tz = t * z;

        float length = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
        float glength = (float) Math.sqrt(temp);

        mX = Utils.clamp((x * USER_ANGEL_COS / glength
                + tx * USER_ANGEL_SIN / length) * mUserDistance,
                -mLimit, mLimit);
        mY = -Utils.clamp((y * USER_ANGEL_COS / glength
                + ty * USER_ANGEL_SIN / length) * mUserDistance,
                -mLimit, mLimit);
        mZ = (float) -Math.sqrt(
                mUserDistance * mUserDistance - mX * mX - mY * mY);
        mListener.onEyePositionChanged(mX, mY, mZ);
    }

    private void onGyroscopeChanged(float gx, float gy, float gz) {
        long now = SystemClock.elapsedRealtime();
        float distance = (gx > 0 ? gx : -gx) + (gy > 0 ? gy : - gy);
        if (distance < GYROSCOPE_THRESHOLD
                || distance > GYROSCOPE_LIMIT || mGyroscopeCountdown > 0) {
            --mGyroscopeCountdown;
            mStartTime = now;
            float limit = mUserDistance / 20f;
            if (mX > limit || mX < -limit || mY > limit || mY < -limit) {
                mX *= GYROSCOPE_RESTORE_FACTOR;
                mY *= GYROSCOPE_RESTORE_FACTOR;
                mZ = (float) -Math.sqrt(
                        mUserDistance * mUserDistance - mX * mX - mY * mY);
                mListener.onEyePositionChanged(mX, mY, mZ);
            }
            return;
        }

        float t = (now - mStartTime) / 1000f * mUserDistance * (-mZ);
        mStartTime = now;

        float x = -gy, y = -gx;
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_90: x = -gx; y= gy; break;
            case Surface.ROTATION_180: x = gy; y = gx; break;
            case Surface.ROTATION_270: x = gx; y = -gy; break;
        }

        mX = Utils.clamp((float) (mX + x * t / Math.hypot(mZ, mX)),
                -mLimit, mLimit) * GYROSCOPE_RESTORE_FACTOR;
        mY = Utils.clamp((float) (mY + y * t / Math.hypot(mZ, mY)),
                -mLimit, mLimit) * GYROSCOPE_RESTORE_FACTOR;

        mZ = (float) -Math.sqrt(
                mUserDistance * mUserDistance - mX * mX - mY * mY);
        mListener.onEyePositionChanged(mX, mY, mZ);
    }

    private class PositionListener implements SensorEventListener {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE: {
                    onGyroscopeChanged(
                            event.values[0], event.values[1], event.values[2]);
                    break;
                }
                case Sensor.TYPE_ACCELEROMETER: {
                    onAccelerometerChanged(
                            event.values[0], event.values[1], event.values[2]);
                }
            }
        }
    }

    public void pause() {
        if (mSensor != null) {
            SensorManager sManager = (SensorManager) mContext
                    .getSystemService(Context.SENSOR_SERVICE);
            sManager.unregisterListener(mPositionListener);
        }
    }

    public void resume() {
        if (mSensor != null) {
            SensorManager sManager = (SensorManager) mContext
                    .getSystemService(Context.SENSOR_SERVICE);
            sManager.registerListener(mPositionListener,
                    mSensor, SensorManager.SENSOR_DELAY_GAME);
        }

        mStartTime = NOT_STARTED;
        mGyroscopeCountdown = GYROSCOPE_SETTLE_DOWN;
        mX = mY = 0;
        mZ = -mUserDistance;
        mListener.onEyePositionChanged(mX, mY, mZ);
    }
}
