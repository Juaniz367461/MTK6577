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


package android.filterpacks.imageproc;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.KeyValueMap;
import android.filterfw.core.NativeProgram;
import android.filterfw.core.NativeFrame;
import android.filterfw.core.Program;
import android.filterfw.core.ShaderProgram;
import android.filterfw.format.ImageFormat;

import android.util.Log;

import java.lang.Math;
import java.util.Set;

/**
 * @hide
 */
public class FisheyeFilter extends Filter {
    private static final String TAG = "FisheyeFilter";

    // This parameter has range between 0 and 1. It controls the effect of radial distortion.
    // The larger the value, the more prominent the distortion effect becomes (a straight line
    // becomes a curve).
    @GenerateFieldPort(name = "scale", hasDefault = true)
    private float mScale = 0f;

    @GenerateFieldPort(name = "tile_size", hasDefault = true)
    private int mTileSize = 640;

    private Program mProgram;

    private int mWidth = 0;
    private int mHeight = 0;
    private int mTarget = FrameFormat.TARGET_UNSPECIFIED;

    private static final String mFisheyeShader =
            "precision mediump float;\n" +
            "uniform sampler2D tex_sampler_0;\n" +
            "uniform vec2 center;\n" +
            "uniform float alpha;\n" +
            "uniform float bound;\n" +
            "uniform highp float radius2;\n" +
            "uniform float factor;\n" +
            "uniform float inv_height;\n" +
            "uniform float inv_width;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            "  const float m_pi_2 = 1.570963;\n" +
            "  float dist = distance(gl_FragCoord.xy, center);\n" +
            "  float radian = m_pi_2 - atan(alpha * sqrt(radius2 - dist * dist), dist);\n" +
            "  float scale = radian * factor / dist;\n" +
            "  vec2 new_coord = gl_FragCoord.xy * scale + (1.0 - scale) * center;\n" +
            "  new_coord.x *= inv_width;\n" +
            "  new_coord.y *= inv_height;\n" +
            "  vec4 color = texture2D(tex_sampler_0, new_coord);\n" +
            "  gl_FragColor = color;\n" +
            "}\n";

    public FisheyeFilter(String name) {
        super(name);
    }

    @Override
    public void setupPorts() {
        addMaskedInputPort("image", ImageFormat.create(ImageFormat.COLORSPACE_RGBA));
        addOutputBasedOnInput("image", "image");
    }

    @Override
    public FrameFormat getOutputFormat(String portName, FrameFormat inputFormat) {
        return inputFormat;
    }

    public void initProgram(FilterContext context, int target) {
        switch (target) {
            case FrameFormat.TARGET_GPU:
                ShaderProgram shaderProgram = new ShaderProgram(context, mFisheyeShader);
                shaderProgram.setMaximumTileSize(mTileSize);
                mProgram = shaderProgram;
                break;

            default:
                throw new RuntimeException("Filter FisheyeFilter does not support frames of " +
                    "target " + target + "!");
        }
        mTarget = target;
    }

    @Override
    public void process(FilterContext context) {
        // Get input frame
        Frame input = pullInput("image");
        FrameFormat inputFormat = input.getFormat();

        // Create output frame
        Frame output = context.getFrameManager().newFrame(inputFormat);

        // Create program if not created already
        if (mProgram == null || inputFormat.getTarget() != mTarget) {
            initProgram(context, inputFormat.getTarget());
        }

        // Check if the frame size has changed
        if (inputFormat.getWidth() != mWidth || inputFormat.getHeight() != mHeight) {
            updateFrameSize(inputFormat.getWidth(), inputFormat.getHeight());
        }

        // Process
        mProgram.process(input, output);

        // Push output
        pushOutput("image", output);

        // Release pushed frame
        output.release();
    }

    @Override
    public void fieldPortValueUpdated(String name, FilterContext context) {
        if (mProgram != null) {
            updateProgramParams();
        }
    }

    private void updateFrameSize(int width, int height) {
        float center[] = {0.5f * width, 0.5f * height};

        mProgram.setHostValue("center", center);
        mProgram.setHostValue("inv_width", 1.0f / width);
        mProgram.setHostValue("inv_height", 1.0f / height);

        mWidth = width;
        mHeight = height;

        updateProgramParams();
    }

    private void updateProgramParams() {
        final float pi = 3.14159265f;

        float alpha = mScale * 2.0f + 0.75f;
        float bound2 = 0.25f * (mWidth * mWidth  + mHeight * mHeight);
        float bound = (float) Math.sqrt(bound2);
        float radius = 1.15f * bound;
        float radius2 = radius * radius;
        float max_radian = 0.5f * pi -
            (float) Math.atan(alpha / bound * (float) Math.sqrt(radius2 - bound2));
        float factor = bound / max_radian;

        mProgram.setHostValue("radius2",radius2);
        mProgram.setHostValue("factor", factor);
        mProgram.setHostValue("alpha", (float) (mScale * 2.0 + 0.75));
    }


}
