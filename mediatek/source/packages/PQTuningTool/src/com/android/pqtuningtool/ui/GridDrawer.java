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

import  com.android.pqtuningtool.R;
import  com.android.pqtuningtool.data.Path;

import android.content.Context;
import android.graphics.Color;
import android.text.Layout;

public class GridDrawer extends IconDrawer {
    private Texture mImportLabel;
    private int mGridWidth;
    private final SelectionManager mSelectionManager;
    private final Context mContext;
    private final int IMPORT_FONT_SIZE = 14;
    private final int IMPORT_FONT_COLOR = Color.WHITE;
    private final int IMPORT_LABEL_MARGIN = 10;
    private boolean mSelectionMode;

    public GridDrawer(Context context, SelectionManager selectionManager) {
        super(context);
        mContext = context;
        mSelectionManager = selectionManager;
    }

    @Override
    public void prepareDrawing() {
        mSelectionMode = mSelectionManager.inSelectionMode();
    }

    @Override
    public void draw(GLCanvas canvas, Texture content, int width,
            int height, int rotation, Path path,
            int dataSourceType, int mediaType, /* boolean isPanorama */ int subType,
            int labelBackgroundHeight, boolean wantCache, boolean isCaching) {

        int x = -width / 2;
        int y = -height / 2;

        drawWithRotation(canvas, content, x, y, width, height, rotation);

        if (((rotation / 90) & 0x01) == 1) {
            int temp = width;
            width = height;
            height = temp;
            x = -width / 2;
            y = -height / 2;
        }

        drawMediaTypeOverlay(canvas, mediaType, subType, x, y, width, height);
        drawLabelBackground(canvas, width, height, labelBackgroundHeight);
        drawIcon(canvas, width, height, dataSourceType);
        if (dataSourceType == DATASOURCE_TYPE_MTP) {
            drawImportLabel(canvas, width, height);
        }

        if (mSelectionManager.isPressedPath(path)) {
            drawPressedFrame(canvas, x, y, width, height);
        } else if (mSelectionMode && mSelectionManager.isItemSelected(path)) {
            drawSelectedFrame(canvas, x, y, width, height);
        }
    }

    // Draws the "click to import" label at the center of the frame
    private void drawImportLabel(GLCanvas canvas, int width, int height) {
        if (mImportLabel == null || mGridWidth != width) {
            mGridWidth = width;
            mImportLabel = MultiLineTexture.newInstance(
                    mContext.getString(R.string.click_import),
                    width - 2 * IMPORT_LABEL_MARGIN,
                    IMPORT_FONT_SIZE, IMPORT_FONT_COLOR,
                    Layout.Alignment.ALIGN_CENTER);
        }
        int w = mImportLabel.getWidth();
        int h = mImportLabel.getHeight();
        mImportLabel.draw(canvas, -w / 2, -h / 2);
    }

    @Override
    public void drawFocus(GLCanvas canvas, int width, int height) {
    }
}
