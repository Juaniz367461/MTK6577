/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.gallery3d.ui;

import com.android.gallery3d.R;

import android.content.Context;
import android.graphics.Rect;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

// This is copied from android.widget.EdgeEffect with some small modifications:
// (1) Copy the images (overscroll_{edge|glow}.png) to local resources.
// (2) Use "GLCanvas" instead of "Canvas" for draw()'s parameter.
// (3) Use a private Drawable class (which inherits from ResourceTexture)
//     instead of android.graphics.drawable.Drawable to hold the images.
//     The private Drawable class is used to translate original Canvas calls to
//     corresponding GLCanvas calls.

/**
 * This class performs the graphical effect used at the edges of scrollable widgets
 * when the user scrolls beyond the content bounds in 2D space.
 *
 * <p>EdgeEffect is stateful. Custom widgets using EdgeEffect should create an
 * instance for each edge that should show the effect, feed it input data using
 * the methods {@link #onAbsorb(int)}, {@link #onPull(float)}, and {@link #onRelease()},
 * and draw the effect using {@link #draw(Canvas)} in the widget's overridden
 * {@link android.view.View#draw(Canvas)} method. If {@link #isFinished()} returns
 * false after drawing, the edge effect's animation is not yet complete and the widget
 * should schedule another drawing pass to continue the animation.</p>
 *
 * <p>When drawing, widgets should draw their main content and child views first,
 * usually by invoking <code>super.draw(canvas)</code> from an overridden <code>draw</code>
 * method. (This will invoke onDraw and dispatch drawing to child views as needed.)
 * The edge effect may then be drawn on top of the view's content using the
 * {@link #draw(Canvas)} method.</p>
 */
public class EdgeEffect {
    private static final String TAG = "EdgeEffect";

    // Time it will take the effect to fully recede in ms
    private static final int RECEDE_TIME = 1000;

    // Time it will take before a pulled glow begins receding in ms
    private static final int PULL_TIME = 167;

    // Time it will take in ms for a pulled glow to decay to partial strength before release
    private static final int PULL_DECAY_TIME = 1000;

    private static final float MAX_ALPHA = 0.8f;
    private static final float HELD_EDGE_ALPHA = 0.7f;
    private static final float HELD_EDGE_SCALE_Y = 0.5f;
    private static final float HELD_GLOW_ALPHA = 0.5f;
    private static final float HELD_GLOW_SCALE_Y = 0.5f;

    private static final float MAX_GLOW_HEIGHT = 4.f;

    private static final float PULL_GLOW_BEGIN = 1.f;
    private static final float PULL_EDGE_BEGIN = 0.6f;

    // Minimum velocity that will be absorbed
    private static final int MIN_VELOCITY = 100;

    private static final float EPSILON = 0.001f;

    private static Drawable mEdge;
    private static Drawable mGlow;
    //Mediatek bug fix: change the above two variable from none-static
    //to static to save memory usage. mEdge & mGlow are used for edge
    //effect which tells user that limit has reached.
    //By Google default design, Gallery tends to create many PhotoPage
    //activity state when viewing picture, hence creates many edge effect
    //object, hence create many mEdge & mGlow. What was worse, the two
    //drawables occupies considerable memory, making Gallery is prone
    //to OutOfMemoryError. As all edget effect objects can share the
    //same drawables, it is possible to create only one set of drawables
    //and shared with others. So the type is changed to static.
    //Is there any risk? should we void so many PhotoPage intances?

    private int mWidth;
    private int mHeight;
    private final int MIN_WIDTH = 300;
    private final int mMinWidth;

    private float mEdgeAlpha;
    private float mEdgeScaleY;
    private float mGlowAlpha;
    private float mGlowScaleY;

    private float mEdgeAlphaStart;
    private float mEdgeAlphaFinish;
    private float mEdgeScaleYStart;
    private float mEdgeScaleYFinish;
    private float mGlowAlphaStart;
    private float mGlowAlphaFinish;
    private float mGlowScaleYStart;
    private float mGlowScaleYFinish;

    private long mStartTime;
    private float mDuration;

    private final Interpolator mInterpolator;

    private static final int STATE_IDLE = 0;
    private static final int STATE_PULL = 1;
    private static final int STATE_ABSORB = 2;
    private static final int STATE_RECEDE = 3;
    private static final int STATE_PULL_DECAY = 4;

    // How much dragging should effect the height of the edge image.
    // Number determined by user testing.
    private static final int PULL_DISTANCE_EDGE_FACTOR = 7;

    // How much dragging should effect the height of the glow image.
    // Number determined by user testing.
    private static final int PULL_DISTANCE_GLOW_FACTOR = 7;
    private static final float PULL_DISTANCE_ALPHA_GLOW_FACTOR = 1.1f;

    private static final int VELOCITY_EDGE_FACTOR = 8;
    private static final int VELOCITY_GLOW_FACTOR = 16;

    private int mState = STATE_IDLE;

    private float mPullDistance;

    /**
     * Construct a new EdgeEffect with a theme appropriate for the provided context.
     * @param context Context used to provide theming and resource information for the EdgeEffect
     */
    public EdgeEffect(Context context) {
        if (null == mEdge) {
            Log.d(TAG,"EdgeEffect:load overscroll_edge");
            mEdge = new Drawable(context, R.drawable.overscroll_edge);
        }
        if (null == mGlow) {
            Log.d(TAG,"EdgeEffect:load overscroll_glow");
            mGlow = new Drawable(context, R.drawable.overscroll_glow);
        }

        mMinWidth = (int) (context.getResources().getDisplayMetrics().density * MIN_WIDTH + 0.5f);
        mInterpolator = new DecelerateInterpolator();
    }

    /**
     * Set the size of this edge effect in pixels.
     *
     * @param width Effect width in pixels
     * @param height Effect height in pixels
     */
    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * Reports if this EdgeEffect's animation is finished. If this method returns false
     * after a call to {@link #draw(Canvas)} the host widget should schedule another
     * drawing pass to continue the animation.
     *
     * @return true if animation is finished, false if drawing should continue on the next frame.
     */
    public boolean isFinished() {
        return mState == STATE_IDLE;
    }

    /**
     * Immediately finish the current animation.
     * After this call {@link #isFinished()} will return true.
     */
    public void finish() {
        mState = STATE_IDLE;
    }

    /**
     * A view should call this when content is pulled away from an edge by the user.
     * This will update the state of the current visual effect and its associated animation.
     * The host view should always {@link android.view.View#invalidate()} after this
     * and draw the results accordingly.
     *
     * @param deltaDistance Change in distance since the last call. Values may be 0 (no change) to
     *                      1.f (full length of the view) or negative values to express change
     *                      back toward the edge reached to initiate the effect.
     */
    public void onPull(float deltaDistance) {
        final long now = AnimationUtils.currentAnimationTimeMillis();
        if (mState == STATE_PULL_DECAY && now - mStartTime < mDuration) {
            return;
        }
        if (mState != STATE_PULL) {
            mGlowScaleY = PULL_GLOW_BEGIN;
        }
        mState = STATE_PULL;

        mStartTime = now;
        mDuration = PULL_TIME;

        mPullDistance += deltaDistance;
        float distance = Math.abs(mPullDistance);

        mEdgeAlpha = mEdgeAlphaStart = Math.max(PULL_EDGE_BEGIN, Math.min(distance, MAX_ALPHA));
        mEdgeScaleY = mEdgeScaleYStart = Math.max(
                HELD_EDGE_SCALE_Y, Math.min(distance * PULL_DISTANCE_EDGE_FACTOR, 1.f));

        mGlowAlpha = mGlowAlphaStart = Math.min(MAX_ALPHA,
                mGlowAlpha +
                (Math.abs(deltaDistance) * PULL_DISTANCE_ALPHA_GLOW_FACTOR));

        float glowChange = Math.abs(deltaDistance);
        if (deltaDistance > 0 && mPullDistance < 0) {
            glowChange = -glowChange;
        }
        if (mPullDistance == 0) {
            mGlowScaleY = 0;
        }

        // Do not allow glow to get larger than MAX_GLOW_HEIGHT.
        mGlowScaleY = mGlowScaleYStart = Math.min(MAX_GLOW_HEIGHT, Math.max(
                0, mGlowScaleY + glowChange * PULL_DISTANCE_GLOW_FACTOR));

        mEdgeAlphaFinish = mEdgeAlpha;
        mEdgeScaleYFinish = mEdgeScaleY;
        mGlowAlphaFinish = mGlowAlpha;
        mGlowScaleYFinish = mGlowScaleY;
    }

    /**
     * Call when the object is released after being pulled.
     * This will begin the "decay" phase of the effect. After calling this method
     * the host view should {@link android.view.View#invalidate()} and thereby
     * draw the results accordingly.
     */
    public void onRelease() {
        mPullDistance = 0;

        if (mState != STATE_PULL && mState != STATE_PULL_DECAY) {
            return;
        }

        mState = STATE_RECEDE;
        mEdgeAlphaStart = mEdgeAlpha;
        mEdgeScaleYStart = mEdgeScaleY;
        mGlowAlphaStart = mGlowAlpha;
        mGlowScaleYStart = mGlowScaleY;

        mEdgeAlphaFinish = 0.f;
        mEdgeScaleYFinish = 0.f;
        mGlowAlphaFinish = 0.f;
        mGlowScaleYFinish = 0.f;

        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        mDuration = RECEDE_TIME;
    }

    /**
     * Call when the effect absorbs an impact at the given velocity.
     * Used when a fling reaches the scroll boundary.
     *
     * <p>When using a {@link android.widget.Scroller} or {@link android.widget.OverScroller},
     * the method <code>getCurrVelocity</code> will provide a reasonable approximation
     * to use here.</p>
     *
     * @param velocity Velocity at impact in pixels per second.
     */
    public void onAbsorb(int velocity) {
        mState = STATE_ABSORB;
        velocity = Math.max(MIN_VELOCITY, Math.abs(velocity));

        mStartTime = AnimationUtils.currentAnimationTimeMillis();
        mDuration = 0.1f + (velocity * 0.03f);

        // The edge should always be at least partially visible, regardless
        // of velocity.
        mEdgeAlphaStart = 0.f;
        mEdgeScaleY = mEdgeScaleYStart = 0.f;
        // The glow depends more on the velocity, and therefore starts out
        // nearly invisible.
        mGlowAlphaStart = 0.5f;
        mGlowScaleYStart = 0.f;

        // Factor the velocity by 8. Testing on device shows this works best to
        // reflect the strength of the user's scrolling.
        mEdgeAlphaFinish = Math.max(0, Math.min(velocity * VELOCITY_EDGE_FACTOR, 1));
        // Edge should never get larger than the size of its asset.
        mEdgeScaleYFinish = Math.max(
                HELD_EDGE_SCALE_Y, Math.min(velocity * VELOCITY_EDGE_FACTOR, 1.f));

        // Growth for the size of the glow should be quadratic to properly
        // respond
        // to a user's scrolling speed. The faster the scrolling speed, the more
        // intense the effect should be for both the size and the saturation.
        mGlowScaleYFinish = Math.min(0.025f + (velocity * (velocity / 100) * 0.00015f), 1.75f);
        // Alpha should change for the glow as well as size.
        mGlowAlphaFinish = Math.max(
                mGlowAlphaStart, Math.min(velocity * VELOCITY_GLOW_FACTOR * .00001f, MAX_ALPHA));
    }


    /**
     * Draw into the provided canvas. Assumes that the canvas has been rotated
     * accordingly and the size has been set. The effect will be drawn the full
     * width of X=0 to X=width, beginning from Y=0 and extending to some factor <
     * 1.f of height.
     *
     * @param canvas Canvas to draw into
     * @return true if drawing should continue beyond this frame to continue the
     *         animation
     */
    public boolean draw(GLCanvas canvas) {
        update();

        final int edgeHeight = mEdge.getIntrinsicHeight();
        final int edgeWidth = mEdge.getIntrinsicWidth();
        final int glowHeight = mGlow.getIntrinsicHeight();
        final int glowWidth = mGlow.getIntrinsicWidth();

        mGlow.setAlpha((int) (Math.max(0, Math.min(mGlowAlpha, 1)) * 255));

        int glowBottom = (int) Math.min(
                glowHeight * mGlowScaleY * glowHeight/ glowWidth * 0.6f,
                glowHeight * MAX_GLOW_HEIGHT);
        if (mWidth < mMinWidth) {
            // Center the glow and clip it.
            int glowLeft = (mWidth - mMinWidth)/2;
            mGlow.setBounds(glowLeft, 0, mWidth - glowLeft, glowBottom);
        } else {
            // Stretch the glow to fit.
            mGlow.setBounds(0, 0, mWidth, glowBottom);
        }

        mGlow.draw(canvas);

        mEdge.setAlpha((int) (Math.max(0, Math.min(mEdgeAlpha, 1)) * 255));

        int edgeBottom = (int) (edgeHeight * mEdgeScaleY);
        if (mWidth < mMinWidth) {
            // Center the edge and clip it.
            int edgeLeft = (mWidth - mMinWidth)/2;
            mEdge.setBounds(edgeLeft, 0, mWidth - edgeLeft, edgeBottom);
        } else {
            // Stretch the edge to fit.
            mEdge.setBounds(0, 0, mWidth, edgeBottom);
        }
        mEdge.draw(canvas);

        return mState != STATE_IDLE;
    }

    private void update() {
        final long time = AnimationUtils.currentAnimationTimeMillis();
        final float t = Math.min((time - mStartTime) / mDuration, 1.f);

        final float interp = mInterpolator.getInterpolation(t);

        mEdgeAlpha = mEdgeAlphaStart + (mEdgeAlphaFinish - mEdgeAlphaStart) * interp;
        mEdgeScaleY = mEdgeScaleYStart + (mEdgeScaleYFinish - mEdgeScaleYStart) * interp;
        mGlowAlpha = mGlowAlphaStart + (mGlowAlphaFinish - mGlowAlphaStart) * interp;
        mGlowScaleY = mGlowScaleYStart + (mGlowScaleYFinish - mGlowScaleYStart) * interp;

        if (t >= 1.f - EPSILON) {
            switch (mState) {
                case STATE_ABSORB:
                    mState = STATE_RECEDE;
                    mStartTime = AnimationUtils.currentAnimationTimeMillis();
                    mDuration = RECEDE_TIME;

                    mEdgeAlphaStart = mEdgeAlpha;
                    mEdgeScaleYStart = mEdgeScaleY;
                    mGlowAlphaStart = mGlowAlpha;
                    mGlowScaleYStart = mGlowScaleY;

                    // After absorb, the glow and edge should fade to nothing.
                    mEdgeAlphaFinish = 0.f;
                    mEdgeScaleYFinish = 0.f;
                    mGlowAlphaFinish = 0.f;
                    mGlowScaleYFinish = 0.f;
                    break;
                case STATE_PULL:
                    mState = STATE_PULL_DECAY;
                    mStartTime = AnimationUtils.currentAnimationTimeMillis();
                    mDuration = PULL_DECAY_TIME;

                    mEdgeAlphaStart = mEdgeAlpha;
                    mEdgeScaleYStart = mEdgeScaleY;
                    mGlowAlphaStart = mGlowAlpha;
                    mGlowScaleYStart = mGlowScaleY;

                    // After pull, the glow and edge should fade to nothing.
                    mEdgeAlphaFinish = 0.f;
                    mEdgeScaleYFinish = 0.f;
                    mGlowAlphaFinish = 0.f;
                    mGlowScaleYFinish = 0.f;
                    break;
                case STATE_PULL_DECAY:
                    // When receding, we want edge to decrease more slowly
                    // than the glow.
                    float factor = mGlowScaleYFinish != 0 ? 1
                            / (mGlowScaleYFinish * mGlowScaleYFinish)
                            : Float.MAX_VALUE;
                    mEdgeScaleY = mEdgeScaleYStart +
                        (mEdgeScaleYFinish - mEdgeScaleYStart) *
                            interp * factor;
                    mState = STATE_RECEDE;
                    break;
                case STATE_RECEDE:
                    mState = STATE_IDLE;
                    break;
            }
        }
    }

    private static class Drawable extends ResourceTexture {
        private Rect mBounds = new Rect();
        private int mAlpha = 255;

        public Drawable(Context context, int resId) {
            super(context, resId);
        }

        public int getIntrinsicWidth() {
            return getWidth();
        }

        public int getIntrinsicHeight() {
            return getHeight();
        }

        public void setBounds(int left, int top, int right, int bottom) {
            mBounds.set(left, top, right, bottom);
        }

        public void setAlpha(int alpha) {
            mAlpha = alpha;
        }

        public void draw(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_ALPHA);
            canvas.multiplyAlpha(mAlpha / 255.0f);
            Rect b = mBounds;
            draw(canvas, b.left, b.top, b.width(), b.height());
            canvas.restore();
        }
    }
}
