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

package  com.android.pqtuningtool.photoeditor;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

/**
 * View that holds a single child and could be recreated/restored after orientation changes.
 */
public abstract class RestorableView extends FrameLayout {

    private static final float ENABLED_ALPHA = 1;
    private static final float DISABLED_ALPHA = 0.47f;

    private final HashMap<Integer, Runnable> clickRunnables = new HashMap<Integer, Runnable>();
    private final HashSet<Integer> changedViews = new HashSet<Integer>();
    private final LayoutInflater inflater;

    public RestorableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    protected abstract int childLayoutId();

    private void recreateChildView() {
        if (getChildCount() != 0) {
            removeAllViews();
        }
        inflater.inflate(childLayoutId(), this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        recreateChildView();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Remember the removing child before recreating the child.
        View view = getChildAt(0);
        recreateChildView();

        // Restore its runnables and status of views that have been changed.
        for (Entry<Integer, Runnable> entry : clickRunnables.entrySet()) {
            setClickRunnable(entry.getKey(), entry.getValue());
        }
        for (int id : changedViews) {
            View changed = view.findViewById(id);
            setViewEnabled(id, changed.isEnabled());
            setViewSelected(id, changed.isSelected());
        }
    }

    public void setClickRunnable(int id, final Runnable r) {
        findViewById(id).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isEnabled()) {
                    r.run();
                }
            }
        });
        clickRunnables.put(id, r);
    }

    public void setViewEnabled(int id, boolean enabled) {
        View view = findViewById(id);
        view.setEnabled(enabled);
        view.setAlpha(enabled ? ENABLED_ALPHA : DISABLED_ALPHA);
        // Track views whose enabled status has been updated.
        changedViews.add(id);
    }

    public void setViewSelected(int id, boolean selected) {
        findViewById(id).setSelected(selected);
        // Track views whose selected status has been updated.
        changedViews.add(id);
    }
}
