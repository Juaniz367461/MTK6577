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

import android.graphics.Bitmap;


public class BoxBlurFilter {
    private static final int RED_MASK = 0xff0000;
    private static final int RED_MASK_SHIFT = 16;
    private static final int GREEN_MASK = 0x00ff00;
    private static final int GREEN_MASK_SHIFT = 8;
    private static final int BLUE_MASK = 0x0000ff;
    private static final int RADIUS = 4;
    private static final int KERNEL_SIZE = RADIUS * 2 + 1;
    private static final int NUM_COLORS = 256;
    private static final int[] KERNEL_NORM = new int[KERNEL_SIZE * NUM_COLORS];

    public static final int MODE_REPEAT = 1;
    public static final int MODE_CLAMP = 2;

    static {
        int index = 0;
        // Build a lookup table from summed to normalized kernel values.
        // The formula: KERNAL_NORM[value] = value / KERNEL_SIZE
        for (int i = 0; i < NUM_COLORS; ++i) {
            for (int j = 0; j < KERNEL_SIZE; ++j) {
                KERNEL_NORM[index++] = i;
            }
        }
    }

    private BoxBlurFilter() {
    }

    private static int sample(int x, int width, int mode) {
        if (x >= 0 && x < width) return x;
        return mode == MODE_REPEAT
                ? x < 0 ? x + width : x - width
                : x < 0 ? 0 : width - 1;
    }

    public static void apply(
            Bitmap bitmap, int horizontalMode, int verticalMode) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int data[] = new int[width * height];
        bitmap.getPixels(data, 0, width, 0, 0, width, height);
        int temp[] = new int[width * height];
        applyOneDimension(data, temp, width, height, horizontalMode);
        applyOneDimension(temp, data, height, width, verticalMode);
        bitmap.setPixels(data, 0, width, 0, 0, width, height);
    }

    private static void applyOneDimension(
            int[] in, int[] out, int width, int height, int mode) {
        for (int y = 0, read = 0; y < height; ++y, read += width) {
            // Evaluate the kernel for the first pixel in the row.
            int red = 0;
            int green = 0;
            int blue = 0;
            for (int i = -RADIUS; i <= RADIUS; ++i) {
                int argb = in[read + sample(i, width, mode)];
                red += (argb & RED_MASK) >> RED_MASK_SHIFT;
                green += (argb & GREEN_MASK) >> GREEN_MASK_SHIFT;
                blue += argb & BLUE_MASK;
            }
            for (int x = 0, write = y; x < width; ++x, write += height) {
                // Output the current pixel.
                out[write] = 0xFF000000
                        | (KERNEL_NORM[red] << RED_MASK_SHIFT)
                        | (KERNEL_NORM[green] << GREEN_MASK_SHIFT)
                        | KERNEL_NORM[blue];

                // Slide to the next pixel, adding the new rightmost pixel and
                // subtracting the former leftmost.
                int prev = in[read + sample(x - RADIUS, width, mode)];
                int next = in[read + sample(x + RADIUS + 1, width, mode)];
                red += ((next & RED_MASK) - (prev & RED_MASK)) >> RED_MASK_SHIFT;
                green += ((next & GREEN_MASK) - (prev & GREEN_MASK)) >> GREEN_MASK_SHIFT;
                blue += (next & BLUE_MASK) - (prev & BLUE_MASK);
            }
        }
    }
}
