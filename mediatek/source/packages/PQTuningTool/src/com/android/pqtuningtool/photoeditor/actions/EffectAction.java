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
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import  com.android.pqtuningtool.R;
import  com.android.pqtuningtool.photoeditor.FilterStack;
import  com.android.pqtuningtool.photoeditor.OnDoneCallback;
import  com.android.pqtuningtool.photoeditor.filters.Filter;

/**
 * An action binding UI controls and effect operation for editing photo.
 */
public abstract class EffectAction extends LinearLayout {

    /**
     * Listener of effect action.
     */
    public interface Listener {

        void onClick();

        void onDone();
    }

    protected EffectToolFactory factory;

    private Listener listener;
    private Toast tooltip;
    private FilterStack filterStack;
    private boolean pushedFilter;
    private FilterChangedCallback lastFilterChangedCallback;

    public EffectAction(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setListener(Listener l) {
        listener = l;
        findViewById(R.id.effect_button).setOnClickListener(
                (listener == null) ? null : new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onClick();
            }
        });
    }

    public CharSequence name() {
        return ((TextView) findViewById(R.id.effect_label)).getText();
    }

    public void begin(FilterStack filterStack, EffectToolFactory factory) {
        // This view is already detached from UI view hierarchy by reaching here; findViewById()
        // could only access its own child views from here.
        this.filterStack = filterStack;
        this.factory = factory;

        // Shows the tooltip if it's available.
        if (getTag() != null) {
            tooltip = Toast.makeText(getContext(), (String) getTag(), Toast.LENGTH_SHORT);
            tooltip.setGravity(Gravity.CENTER, 0, 0);
            tooltip.show();
        }
        doBegin();
    }

    /**
     * Ends the effect and then executes the runnable after the effect is finished.
     */
    public void end(final Runnable runnableOnODone) {
        doEnd();

        // Wait till last output callback is done before finishing.
        if ((lastFilterChangedCallback == null) || lastFilterChangedCallback.done) {
            finish(runnableOnODone);
        } else {
            lastFilterChangedCallback.runnableOnReady = new Runnable() {

                @Override
                public void run() {
                    finish(runnableOnODone);
                }
            };
        }
    }

    private void finish(Runnable runnableOnDone) {
        // Close the tooltip if it's still showing.
        if ((tooltip != null) && (tooltip.getView().getParent() != null)) {
            tooltip.cancel();
            tooltip = null;
        }
        pushedFilter = false;
        lastFilterChangedCallback = null;

        runnableOnDone.run();
    }

    protected void notifyDone() {
        if (listener != null) {
            listener.onDone();
        }
    }

    protected void notifyFilterChanged(Filter filter, boolean output) {
        if (!pushedFilter && filter.isValid()) {
            filterStack.pushFilter(filter);
            pushedFilter = true;
        }
        if (pushedFilter && output) {
            // Notify the stack to execute the changed top filter and output the results.
            lastFilterChangedCallback = new FilterChangedCallback();
            filterStack.topFilterChanged(lastFilterChangedCallback);
        }
    }

    /**
     * Subclasses should creates a specific filter and binds the filter to necessary UI controls
     * here when the action is about to begin.
     */
    protected abstract void doBegin();

    /**
     * Subclasses could do specific ending operations here when the action is about to end.
     */
    protected abstract void doEnd();

    /**
     * Done callback for executing top filter changes.
     */
    private class FilterChangedCallback implements OnDoneCallback {

        private boolean done;
        private Runnable runnableOnReady;

        @Override
        public void onDone() {
            done = true;

            if (runnableOnReady != null) {
                runnableOnReady.run();
            }
        }
    }
}
