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

import  com.android.pqtuningtool.R;
import  com.android.pqtuningtool.common.Utils;
import  com.android.pqtuningtool.data.MediaObject;
import  com.android.pqtuningtool.data.MediaSet;
import  com.android.pqtuningtool.data.Path;
import  com.android.pqtuningtool.ui.AlbumSetView;
import  com.android.pqtuningtool.ui.CacheStorageUsageInfo;
import  com.android.pqtuningtool.ui.GLCanvas;
import  com.android.pqtuningtool.ui.GLView;
import  com.android.pqtuningtool.ui.ManageCacheDrawer;
import  com.android.pqtuningtool.ui.MenuExecutor;
import  com.android.pqtuningtool.ui.SelectionDrawer;
import  com.android.pqtuningtool.ui.SelectionManager;
import  com.android.pqtuningtool.ui.SlotView;
import  com.android.pqtuningtool.ui.StaticBackground;
import  com.android.pqtuningtool.ui.SynchronizedHandler;
import  com.android.pqtuningtool.util.Future;
import  com.android.pqtuningtool.util.GalleryUtils;
import  com.android.pqtuningtool.util.ThreadPool.Job;
import  com.android.pqtuningtool.util.ThreadPool.JobContext;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ManageCachePage extends ActivityState implements
        SelectionManager.SelectionListener, MenuExecutor.ProgressListener,
        EyePosition.EyePositionListener, OnClickListener {
    public static final String KEY_MEDIA_PATH = "media-path";

    private static final String TAG = "ManageCachePage";

    private static final float USER_DISTANCE_METER = 0.3f;
    private static final int DATA_CACHE_SIZE = 256;
    private static final int MSG_REFRESH_STORAGE_INFO = 1;
    private static final int MSG_REQUEST_LAYOUT = 2;
    private static final int PROGRESS_BAR_MAX = 10000;

    private StaticBackground mStaticBackground;
    private AlbumSetView mAlbumSetView;

    private MediaSet mMediaSet;

    protected SelectionManager mSelectionManager;
    protected SelectionDrawer mSelectionDrawer;
    private AlbumSetDataAdapter mAlbumSetDataAdapter;
    private float mUserDistance; // in pixel

    private EyePosition mEyePosition;

    // The eyes' position of the user, the origin is at the center of the
    // device and the unit is in pixels.
    private float mX;
    private float mY;
    private float mZ;

    private int mAlbumCountToMakeAvailableOffline;
    private View mFooterContent;
    private CacheStorageUsageInfo mCacheStorageInfo;
    private Future<Void> mUpdateStorageInfo;
    private Handler mHandler;
    private boolean mLayoutReady = false;

    private GLView mRootPane = new GLView() {
        private float mMatrix[] = new float[16];

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            // Hack: our layout depends on other components on the screen.
            // We assume the other components will complete before we get a change
            // to run a message in main thread.
            if (!mLayoutReady) {
                mHandler.sendEmptyMessage(MSG_REQUEST_LAYOUT);
                return;
            }
            mLayoutReady = false;

            mStaticBackground.layout(0, 0, right - left, bottom - top);
            mEyePosition.resetPosition();
            Activity activity = (Activity) mActivity;
            int slotViewTop = GalleryActionBar.getHeight(activity);
            int slotViewBottom = bottom - top;

            View footer = activity.findViewById(R.id.footer);
            if (footer != null) {
                int location[] = {0, 0};
                footer.getLocationOnScreen(location);
                slotViewBottom = location[1];
            }

            mAlbumSetView.layout(0, slotViewTop, right - left, slotViewBottom);
        }

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            GalleryUtils.setViewPointMatrix(mMatrix,
                        getWidth() / 2 + mX, getHeight() / 2 + mY, mZ);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);
            canvas.restore();
        }
    };

    public void onEyePositionChanged(float x, float y, float z) {
        mRootPane.lockRendering();
        mX = x;
        mY = y;
        mZ = z;
        mRootPane.unlockRendering();
        mRootPane.invalidate();
    }

    private void onDown(int index) {
        MediaSet set = mAlbumSetDataAdapter.getMediaSet(index);
        Path path = (set == null) ? null : set.getPath();
        mSelectionManager.setPressedPath(path);
        mAlbumSetView.invalidate();
    }

    private void onUp() {
        mSelectionManager.setPressedPath(null);
        mAlbumSetView.invalidate();
    }

    public void onSingleTapUp(int slotIndex) {
        MediaSet targetSet = mAlbumSetDataAdapter.getMediaSet(slotIndex);
        if (targetSet == null) return; // Content is dirty, we shall reload soon

        // ignore selection action if the target set does not support cache
        // operation (like a local album).
        if ((targetSet.getSupportedOperations()
                & MediaSet.SUPPORT_CACHE) == 0) {
            showToastForLocalAlbum();
            return;
        }

        Path path = targetSet.getPath();
        boolean isFullyCached =
                (targetSet.getCacheFlag() == MediaObject.CACHE_FLAG_FULL);
        boolean isSelected = mSelectionManager.isItemSelected(path);

        if (!isFullyCached) {
            // We only count the media sets that will be made available offline
            // in this session.
            if (isSelected) {
                --mAlbumCountToMakeAvailableOffline;
            } else {
                ++mAlbumCountToMakeAvailableOffline;
            }
        }

        long sizeOfTarget = targetSet.getCacheSize();
        mCacheStorageInfo.increaseTargetCacheSize(
                (isFullyCached ^ isSelected) ? -sizeOfTarget : sizeOfTarget);
        refreshCacheStorageInfo();

        mSelectionManager.toggle(path);
        mAlbumSetView.invalidate();
    }

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        mCacheStorageInfo = new CacheStorageUsageInfo(mActivity);
        initializeViews();
        initializeData(data);
        mEyePosition = new EyePosition(mActivity.getAndroidContext(), this);
        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_REFRESH_STORAGE_INFO:
                        refreshCacheStorageInfo();
                        break;
                    case MSG_REQUEST_LAYOUT: {
                        mLayoutReady = true;
                        removeMessages(MSG_REQUEST_LAYOUT);
                        mRootPane.requestLayout();
                        break;
                    }
                }
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        // We use different layout resources for different configs
        initializeFooterViews();
        FrameLayout layout = (FrameLayout) ((Activity) mActivity).findViewById(R.id.footer);
        if (layout.getVisibility() == View.VISIBLE) {
            layout.removeAllViews();
            layout.addView(mFooterContent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mAlbumSetDataAdapter.pause();
        mAlbumSetView.pause();
        mEyePosition.pause();

        if (mUpdateStorageInfo != null) {
            mUpdateStorageInfo.cancel();
            mUpdateStorageInfo = null;
        }
        mHandler.removeMessages(MSG_REFRESH_STORAGE_INFO);

        FrameLayout layout = (FrameLayout) ((Activity) mActivity).findViewById(R.id.footer);
        layout.removeAllViews();
        layout.setVisibility(View.INVISIBLE);
    }

    private Job<Void> mUpdateStorageInfoJob = new Job<Void>() {
        @Override
        public Void run(JobContext jc) {
            mCacheStorageInfo.loadStorageInfo(jc);
            if (!jc.isCancelled()) {
                mHandler.sendEmptyMessage(MSG_REFRESH_STORAGE_INFO);
            }
            return null;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        setContentPane(mRootPane);
        mAlbumSetDataAdapter.resume();
        mAlbumSetView.resume();
        mEyePosition.resume();
        mUpdateStorageInfo = mActivity.getThreadPool().submit(mUpdateStorageInfoJob);
        FrameLayout layout = (FrameLayout) ((Activity) mActivity).findViewById(R.id.footer);
        layout.addView(mFooterContent);
        layout.setVisibility(View.VISIBLE);
    }

    private void initializeData(Bundle data) {
        mUserDistance = GalleryUtils.meterToPixel(USER_DISTANCE_METER);
        String mediaPath = data.getString(ManageCachePage.KEY_MEDIA_PATH);
        mMediaSet = mActivity.getDataManager().getMediaSet(mediaPath);
        mSelectionManager.setSourceMediaSet(mMediaSet);

        // We will always be in selection mode in this page.
        mSelectionManager.setAutoLeaveSelectionMode(false);
        mSelectionManager.enterSelectionMode();

        mAlbumSetDataAdapter = new AlbumSetDataAdapter(
                mActivity, mMediaSet, DATA_CACHE_SIZE);
        mAlbumSetView.setModel(mAlbumSetDataAdapter);
    }

    private void initializeViews() {
        Activity activity = (Activity) mActivity;

        mSelectionManager = new SelectionManager(mActivity, true);
        mSelectionManager.setSelectionListener(this);
        mStaticBackground = new StaticBackground(activity);
        mRootPane.addComponent(mStaticBackground);

        Config.ManageCachePage config = Config.ManageCachePage.get(activity);
        mSelectionDrawer = new ManageCacheDrawer((Context) mActivity,
                mSelectionManager, config.cachePinSize, config.cachePinMargin);
        mAlbumSetView = new AlbumSetView(mActivity, mSelectionDrawer,
                config.slotViewSpec, config.labelSpec);
        mAlbumSetView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                ManageCachePage.this.onDown(index);
            }

            @Override
            public void onUp() {
                ManageCachePage.this.onUp();
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                ManageCachePage.this.onSingleTapUp(slotIndex);
            }
        });
        mRootPane.addComponent(mAlbumSetView);
        initializeFooterViews();
    }

    private void initializeFooterViews() {
        Activity activity = (Activity) mActivity;

        FrameLayout footer = (FrameLayout) activity.findViewById(R.id.footer);
        LayoutInflater inflater = activity.getLayoutInflater();
        mFooterContent = inflater.inflate(R.layout.manage_offline_bar, null);

        mFooterContent.findViewById(R.id.done).setOnClickListener(this);
        mStaticBackground.setImage(R.drawable.background, R.drawable.background_portrait);
        refreshCacheStorageInfo();
    }

    @Override
    public void onClick(View view) {
        Utils.assertTrue(view.getId() == R.id.done);

        ArrayList<Path> ids = mSelectionManager.getSelected(false);
        if (ids.size() == 0) {
            onBackPressed();
            return;
        }
        showToast();

        MenuExecutor menuExecutor = new MenuExecutor(mActivity, mSelectionManager);
        menuExecutor.startAction(R.id.action_toggle_full_caching,
                R.string.process_caching_requests, this);
    }

    private void showToast() {
        if (mAlbumCountToMakeAvailableOffline > 0) {
            Activity activity = (Activity) mActivity;
            Toast.makeText(activity, activity.getResources().getQuantityString(
                    R.plurals.make_albums_available_offline,
                    mAlbumCountToMakeAvailableOffline),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showToastForLocalAlbum() {
        Activity activity = (Activity) mActivity;
        Toast.makeText(activity, activity.getResources().getString(
            R.string.try_to_set_local_album_available_offline),
            Toast.LENGTH_SHORT).show();
    }

    private void refreshCacheStorageInfo() {
        ProgressBar progressBar = (ProgressBar) mFooterContent.findViewById(R.id.progress);
        TextView status = (TextView) mFooterContent.findViewById(R.id.status);
        progressBar.setMax(PROGRESS_BAR_MAX);
        long totalBytes = mCacheStorageInfo.getTotalBytes();
        long usedBytes = mCacheStorageInfo.getUsedBytes();
        long expectedBytes = mCacheStorageInfo.getExpectedUsedBytes();
        long freeBytes = mCacheStorageInfo.getFreeBytes();

        Activity activity = (Activity) mActivity;
        if (totalBytes == 0) {
            progressBar.setProgress(0);
            progressBar.setSecondaryProgress(0);

            // TODO: get the string translated
            String label = activity.getString(R.string.free_space_format, "-");
            status.setText(label);
        } else {
            progressBar.setProgress((int) (usedBytes * PROGRESS_BAR_MAX / totalBytes));
            progressBar.setSecondaryProgress(
                    (int) (expectedBytes * PROGRESS_BAR_MAX / totalBytes));
            String label = activity.getString(R.string.free_space_format,
                    Formatter.formatFileSize(activity, freeBytes));
            status.setText(label);
        }
    }

    public void onProgressComplete(int result) {
        onBackPressed();
    }

    public void onProgressUpdate(int index) {
    }

    public void onSelectionModeChange(int mode) {
    }

    public void onSelectionChange(Path path, boolean selected) {
    }

}
