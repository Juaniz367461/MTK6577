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

package  com.android.pqtuningtool.photoeditor.actions;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A view that tracks touch motions as paths and paints them as doodles.
 */
class DoodleView extends FullscreenToolView {

    /**
     * Listener of doodle paths.
     */
    public interface OnDoodleChangeListener {

        void onDoodleInPhotoBounds();

        void onDoodleFinished(Doodle doodle);
    }

    private final Paint bitmapPaint = new Paint(Paint.DITHER_FLAG);
    private final Paint doodlePaint = Doodle.createPaint();
    private final PointF lastPoint = new PointF();
    private final Path drawingPath = new Path();
    private final Matrix drawingMatrix = new Matrix();
    private final Matrix displayMatrix = new Matrix();

    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private Doodle doodle;
    private int color;
    private OnDoodleChangeListener listener;

    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnDoodleChangeListener(OnDoodleChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        RectF r = new RectF(0, 0, getPhotoWidth(), getPhotoHeight());
        if ((bitmap == null) && !r.isEmpty()) {
            bitmap = Bitmap.createBitmap((int) r.width(), (int) r.height(),
                    Bitmap.Config.ARGB_8888);
            bitmapCanvas = new Canvas(bitmap);

            // Set up a matrix that maps back normalized paths to be drawn on the bitmap or canvas.
            drawingMatrix.setRectToRect(new RectF(0, 0, 1, 1), r, Matrix.ScaleToFit.FILL);
        }
        displayMatrix.setRectToRect(r, displayBounds, Matrix.ScaleToFit.FILL);
    }

    private void drawDoodle(Canvas canvas) {
        if ((canvas != null) && (doodle != null)) {
            doodlePaint.setColor(doodle.getColor());
            doodle.getDrawingPath(drawingMatrix, drawingPath);
            canvas.drawPath(drawingPath, doodlePaint);
        }
    }

    public void setColor(int color) {
        // Restart doodle to draw in a new color.
        this.color = color;
        finishDoodle();
        startDoodle();
    }

    private void startDoodle() {
        doodle = new Doodle(color, new PointF(lastPoint.x, lastPoint.y));
    }

    private void finishDoodle() {
        if ((doodle != null) && !doodle.isEmpty()) {
            // Update the finished non-empty doodle to the bitmap.
            drawDoodle(bitmapCanvas);
            if (listener != null) {
                listener.onDoodleFinished(doodle);
            }
            invalidate();
        }
        doodle = null;
    }

    private void addLastPointIntoDoodle() {
        if ((doodle != null) && doodle.addControlPoint(new PointF(lastPoint.x, lastPoint.y))) {
            if (listener != null) {
                listener.onDoodleInPhotoBounds();
            }
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (isEnabled()) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mapPhotoPoint(x, y, lastPoint);
                    startDoodle();
                    break;

                case MotionEvent.ACTION_MOVE:
                    mapPhotoPoint(x, y, lastPoint);
                    addLastPointIntoDoodle();
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    // Line to last position with offset to draw at least dots for single clicks.
                    mapPhotoPoint(x + 1, y + 1, lastPoint);
                    addLastPointIntoDoodle();
                    finishDoodle();
                    break;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.clipRect(displayBounds);
        canvas.concat(displayMatrix);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
        }
        drawDoodle(canvas);
        canvas.restore();
    }
}
