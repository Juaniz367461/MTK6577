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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;

import  com.android.pqtuningtool.common.Utils;
import  com.android.pqtuningtool.data.ContentListener;
import  com.android.pqtuningtool.data.MediaItem;
import  com.android.pqtuningtool.data.MediaObject;
import  com.android.pqtuningtool.data.MediaSet;
import  com.android.pqtuningtool.data.Path;
import  com.android.pqtuningtool.ui.GLCanvas;
import  com.android.pqtuningtool.ui.GLView;
import  com.android.pqtuningtool.ui.SlideshowView;
import  com.android.pqtuningtool.ui.SynchronizedHandler;
import  com.android.pqtuningtool.util.Future;
import  com.android.pqtuningtool.util.FutureListener;

import java.util.ArrayList;
import java.util.Random;

public class SlideshowPage extends ActivityState {
    private static final String TAG = "SlideshowPage";

    public static final String KEY_SET_PATH = "media-set-path";
    public static final String KEY_ITEM_PATH = "media-item-path";
    public static final String KEY_PHOTO_INDEX = "photo-index";
    public static final String KEY_RANDOM_ORDER = "random-order";
    public static final String KEY_REPEAT = "repeat";

    private static final long SLIDESHOW_DELAY = 3000; // 3 seconds

    private static final int MSG_LOAD_NEXT_BITMAP = 1;
    private static final int MSG_SHOW_PENDING_BITMAP = 2;

    public static interface Model {
        public void pause();

        public void resume();

        public Future<Slide> nextSlide(FutureListener<Slide> listener);
    }

    public static class Slide {
        public Bitmap bitmap;
        public MediaItem item;
        public int index;

        public Slide(MediaItem item, int index, Bitmap bitmap) {
            this.bitmap = bitmap;
            this.item = item;
            this.index = index;
        }
    }

    private Handler mHandler;
    private Model mModel;
    private SlideshowView mSlideshowView;

    private Slide mPendingSlide = null;
    private boolean mIsActive = false;
    private final Intent mResultIntent = new Intent();

    private final GLView mRootPane = new GLView() {
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            mSlideshowView.layout(0, 0, right - left, bottom - top);
        }

        @Override
        protected boolean onTouch(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                onBackPressed();
            }
            return true;
        }

        @Override
        protected void renderBackground(GLCanvas canvas) {
            canvas.clearBuffer();
        }
    };

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        mFlags |= (FLAG_HIDE_ACTION_BAR | FLAG_HIDE_STATUS_BAR | FLAG_SCREEN_ON);

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_SHOW_PENDING_BITMAP:
                        showPendingBitmap();
                        break;
                    case MSG_LOAD_NEXT_BITMAP:
                        loadNextBitmap();
                        break;
                    default: throw new AssertionError();
                }
            }
        };
        initializeViews();
        initializeData(data);
    }

    private void loadNextBitmap() {
        mModel.nextSlide(new FutureListener<Slide>() {
            public void onFutureDone(Future<Slide> future) {
                mPendingSlide = future.get();
                mHandler.sendEmptyMessage(MSG_SHOW_PENDING_BITMAP);
            }
        });
    }

    private void showPendingBitmap() {
        // mPendingBitmap could be null, if
        // 1.) there is no more items
        // 2.) mModel is paused
        Slide slide = mPendingSlide;
        if (slide == null) {
            if (mIsActive) {
                mActivity.getStateManager().finishState(SlideshowPage.this);
            }
            return;
        }

        mSlideshowView.next(slide.bitmap, slide.item.getRotation());

        setStateResult(Activity.RESULT_OK, mResultIntent
                .putExtra(KEY_ITEM_PATH, slide.item.getPath().toString())
                .putExtra(KEY_PHOTO_INDEX, slide.index));
        mHandler.sendEmptyMessageDelayed(MSG_LOAD_NEXT_BITMAP, SLIDESHOW_DELAY);
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsActive = false;
        mModel.pause();
        mSlideshowView.release();

        mHandler.removeMessages(MSG_LOAD_NEXT_BITMAP);
        mHandler.removeMessages(MSG_SHOW_PENDING_BITMAP);
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsActive = true;
        mModel.resume();

        if (mPendingSlide != null) {
            showPendingBitmap();
        } else {
            loadNextBitmap();
        }
    }

    private void initializeData(Bundle data) {
        boolean random = data.getBoolean(KEY_RANDOM_ORDER, false);

        // We only want to show slideshow for images only, not videos.
        String mediaPath = data.getString(KEY_SET_PATH);
        mediaPath = FilterUtils.newFilterPath(mediaPath, FilterUtils.FILTER_IMAGE_ONLY);
        MediaSet mediaSet = mActivity.getDataManager().getMediaSet(mediaPath);

        if (random) {
            boolean repeat = data.getBoolean(KEY_REPEAT);
            mModel = new SlideshowDataAdapter(mActivity,
                    new ShuffleSource(mediaSet, repeat), 0, null);
            setStateResult(Activity.RESULT_OK, mResultIntent.putExtra(KEY_PHOTO_INDEX, 0));
        } else {
            int index = data.getInt(KEY_PHOTO_INDEX);
            String itemPath = data.getString(KEY_ITEM_PATH);
            Path path = itemPath != null ? Path.fromString(itemPath) : null;
            boolean repeat = data.getBoolean(KEY_REPEAT);
            mModel = new SlideshowDataAdapter(mActivity, new SequentialSource(mediaSet, repeat),
                    index, path);
            setStateResult(Activity.RESULT_OK, mResultIntent.putExtra(KEY_PHOTO_INDEX, index));
        }
    }

    private void initializeViews() {
        mSlideshowView = new SlideshowView();
        mRootPane.addComponent(mSlideshowView);
        setContentPane(mRootPane);
    }

    private static MediaItem findMediaItem(MediaSet mediaSet, int index) {
        for (int i = 0, n = mediaSet.getSubMediaSetCount(); i < n; ++i) {
            MediaSet subset = mediaSet.getSubMediaSet(i);
            int count = subset.getTotalMediaItemCount();
            if (index < count) {
                return findMediaItem(subset, index);
            }
            index -= count;
        }
        ArrayList<MediaItem> list = mediaSet.getMediaItem(index, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    private static class ShuffleSource implements SlideshowDataAdapter.SlideshowSource {
        private static final int RETRY_COUNT = 5;
        private final MediaSet mMediaSet;
        private final Random mRandom = new Random();
        private int mOrder[] = new int[0];
        private final boolean mRepeat;
        private long mSourceVersion = MediaSet.INVALID_DATA_VERSION;
        private int mLastIndex = -1;

        public ShuffleSource(MediaSet mediaSet, boolean repeat) {
            mMediaSet = Utils.checkNotNull(mediaSet);
            mRepeat = repeat;
        }

        public int findItemIndex(Path path, int hint) {
            return hint;
        }

        public MediaItem getMediaItem(int index) {
            if (!mRepeat && index >= mOrder.length) return null;
            if (mOrder.length == 0) return null;
            mLastIndex = mOrder[index % mOrder.length];
            MediaItem item = findMediaItem(mMediaSet, mLastIndex);
            for (int i = 0; i < RETRY_COUNT && item == null; ++i) {
                Log.w(TAG, "fail to find image: " + mLastIndex);
                mLastIndex = mRandom.nextInt(mOrder.length);
                item = findMediaItem(mMediaSet, mLastIndex);
            }
            return item;
        }

        public long reload() {
            long version = mMediaSet.reload();
            if (version != mSourceVersion) {
                mSourceVersion = version;
                int count = mMediaSet.getTotalMediaItemCount();
                if (count != mOrder.length) generateOrderArray(count);
            }
            return version;
        }

        private void generateOrderArray(int totalCount) {
            if (mOrder.length != totalCount) {
                mOrder = new int[totalCount];
                for (int i = 0; i < totalCount; ++i) {
                    mOrder[i] = i;
                }
            }
            for (int i = totalCount - 1; i > 0; --i) {
                Utils.swap(mOrder, i, mRandom.nextInt(i + 1));
            }
            if (mOrder[0] == mLastIndex && totalCount > 1) {
                Utils.swap(mOrder, 0, mRandom.nextInt(totalCount - 1) + 1);
            }
        }

        public void addContentListener(ContentListener listener) {
            mMediaSet.addContentListener(listener);
        }

        public void removeContentListener(ContentListener listener) {
            mMediaSet.removeContentListener(listener);
        }
    }

    private static class SequentialSource implements SlideshowDataAdapter.SlideshowSource {
        private static final int DATA_SIZE = 32;

        private ArrayList<MediaItem> mData = new ArrayList<MediaItem>();
        private int mDataStart = 0;
        private long mDataVersion = MediaObject.INVALID_DATA_VERSION;
        private final MediaSet mMediaSet;
        private final boolean mRepeat;

        public SequentialSource(MediaSet mediaSet, boolean repeat) {
            mMediaSet = mediaSet;
            mRepeat = repeat;
        }

        public int findItemIndex(Path path, int hint) {
            return mMediaSet.getIndexOfItem(path, hint);
        }

        public MediaItem getMediaItem(int index) {
            int dataEnd = mDataStart + mData.size();

            if (mRepeat) {
                int count = mMediaSet.getMediaItemCount();
                if (count == 0) return null;
                index = index % count;
            }
            if (index < mDataStart || index >= dataEnd) {
                mData = mMediaSet.getMediaItem(index, DATA_SIZE);
                mDataStart = index;
                dataEnd = index + mData.size();
            }

            return (index < mDataStart || index >= dataEnd) ? null : mData.get(index - mDataStart);
        }

        public long reload() {
            long version = mMediaSet.reload();
            if (version != mDataVersion) {
                mDataVersion = version;
                mData.clear();
            }
            return mDataVersion;
        }

        public void addContentListener(ContentListener listener) {
            mMediaSet.addContentListener(listener);
        }

        public void removeContentListener(ContentListener listener) {
            mMediaSet.removeContentListener(listener);
        }
    }
}
