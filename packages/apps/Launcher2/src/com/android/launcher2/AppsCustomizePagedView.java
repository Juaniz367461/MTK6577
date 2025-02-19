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

package com.android.launcher2;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.TableMaskFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Process;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.launcher.R;
import com.android.launcher2.DropTarget.DragObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A simple callback interface which also provides the results of the task.
 */
interface AsyncTaskCallback {
    void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data);
}

/**
 * The data needed to perform either of the custom AsyncTasks.
 */
class AsyncTaskPageData {
    enum Type {
        LoadWidgetPreviewData,
        LoadHolographicIconsData
    }

    AsyncTaskPageData(int p, ArrayList<Object> l, ArrayList<Bitmap> si, AsyncTaskCallback bgR,
            AsyncTaskCallback postR) {
        page = p;
        items = l;
        sourceImages = si;
        generatedImages = new ArrayList<Bitmap>();
        maxImageWidth = maxImageHeight = -1;
        doInBackgroundCallback = bgR;
        postExecuteCallback = postR;
    }
    AsyncTaskPageData(int p, ArrayList<Object> l, int cw, int ch, AsyncTaskCallback bgR,
            AsyncTaskCallback postR) {
        page = p;
        items = l;
        generatedImages = new ArrayList<Bitmap>();
        maxImageWidth = cw;
        maxImageHeight = ch;
        doInBackgroundCallback = bgR;
        postExecuteCallback = postR;
    }
    void cleanup(boolean cancelled) {
        // Clean up any references to source/generated bitmaps
        if (sourceImages != null) {
            if (cancelled) {
                for (Bitmap b : sourceImages) {
                    b.recycle();
                }
            }
            sourceImages.clear();
        }
        if (generatedImages != null) {
            if (cancelled) {
                for (Bitmap b : generatedImages) {
                    b.recycle();
                }
            }
            generatedImages.clear();
        }
    }
    int page;
    ArrayList<Object> items;
    ArrayList<Bitmap> sourceImages;
    ArrayList<Bitmap> generatedImages;
    int maxImageWidth;
    int maxImageHeight;
    AsyncTaskCallback doInBackgroundCallback;
    AsyncTaskCallback postExecuteCallback;
}

/**
 * A generic template for an async task used in AppsCustomize.
 */
class AppsCustomizeAsyncTask extends AsyncTask<AsyncTaskPageData, Void, AsyncTaskPageData> {
    AppsCustomizeAsyncTask(int p, AsyncTaskPageData.Type ty) {
        page = p;
        threadPriority = Process.THREAD_PRIORITY_DEFAULT;
        dataType = ty;
    }
    @Override
    protected AsyncTaskPageData doInBackground(AsyncTaskPageData... params) {
        if (params.length != 1) return null;
        // Load each of the widget previews in the background
        params[0].doInBackgroundCallback.run(this, params[0]);
        return params[0];
    }
    @Override
    protected void onPostExecute(AsyncTaskPageData result) {
        // All the widget previews are loaded, so we can just callback to inflate the page
        result.postExecuteCallback.run(this, result);
    }

    void setThreadPriority(int p) {
        threadPriority = p;
    }
    void syncThreadPriority() {
        Process.setThreadPriority(threadPriority);
    }

    // The page that this async task is associated with
    AsyncTaskPageData.Type dataType;
    int page;
    int threadPriority;
}

/**
 * The Apps/Customize page that displays all the applications, widgets, and shortcuts.
 */
public class AppsCustomizePagedView extends PagedViewWithDraggableItems implements
        AllAppsView, View.OnClickListener, View.OnKeyListener, DragSource {
    static final String TAG = "Launcher.AppsCustomizePagedView";

    /**
     * The different content types that this paged view can show.
     */
    public enum ContentType {
        Applications,
        Widgets
    }

    // Refs
    private Launcher mLauncher;
    private DragController mDragController;
    private final LayoutInflater mLayoutInflater;
    private final PackageManager mPackageManager;

    // Save and Restore
    private int mSaveInstanceStateItemIndex = -1;

    // Content
    private ArrayList<ApplicationInfo> mApps;
    private ArrayList<Object> mWidgets;

    // Cling
    private boolean mHasShownAllAppsCling;
    private int mClingFocusedX;
    private int mClingFocusedY;

    // Caching
    private Canvas mCanvas;
    private Drawable mDefaultWidgetBackground;
    private IconCache mIconCache;
    private int mDragViewMultiplyColor;

    // Dimens
    private int mContentWidth;
    private int mAppIconSize;
    private int mMaxAppCellCountX, mMaxAppCellCountY;
    private int mWidgetCountX, mWidgetCountY;
    private int mWidgetWidthGap, mWidgetHeightGap;
    private final int mWidgetPreviewIconPaddedDimension;
    private final float sWidgetPreviewIconPaddingPercentage = 0.25f;
    private PagedViewCellLayout mWidgetSpacingLayout;
    private int mNumAppsPages;
    private int mNumWidgetPages;

    // Relating to the scroll and overscroll effects
    Workspace.ZInterpolator mZInterpolator = new Workspace.ZInterpolator(0.5f);
    private static float CAMERA_DISTANCE = 5000;
    private static float TRANSITION_SCALE_FACTOR = 0.74f;
    private static float TRANSITION_PIVOT = 0.65f;
    private static float TRANSITION_MAX_ROTATION = 44;
    private static final boolean PERFORM_OVERSCROLL_ROTATION = true;
    private AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);
    private DecelerateInterpolator mLeftScreenAlphaInterpolator = new DecelerateInterpolator(4);

    // Previews & outlines
    ArrayList<AppsCustomizeAsyncTask> mRunningTasks;
    private HolographicOutlineHelper mHolographicOutlineHelper;
    private static final int sPageSleepDelay = 200;

    public AppsCustomizePagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = LayoutInflater.from(context);
        mPackageManager = context.getPackageManager();
        mApps = new ArrayList<ApplicationInfo>();
        mWidgets = new ArrayList<Object>();
        mIconCache = ((LauncherApplication) context.getApplicationContext()).getIconCache();
        mHolographicOutlineHelper = new HolographicOutlineHelper();
        mCanvas = new Canvas();
        mRunningTasks = new ArrayList<AppsCustomizeAsyncTask>();

        // Save the default widget preview background
        Resources resources = context.getResources();
        mDefaultWidgetBackground = resources.getDrawable(R.drawable.default_widget_preview_holo);
        mAppIconSize = resources.getDimensionPixelSize(R.dimen.app_icon_size);
        mDragViewMultiplyColor = resources.getColor(R.color.drag_view_multiply_color);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppsCustomizePagedView, 0, 0);
        mMaxAppCellCountX = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountX, -1);
        mMaxAppCellCountY = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountY, -1);
        mWidgetWidthGap =
            a.getDimensionPixelSize(R.styleable.AppsCustomizePagedView_widgetCellWidthGap, 0);
        mWidgetHeightGap =
            a.getDimensionPixelSize(R.styleable.AppsCustomizePagedView_widgetCellHeightGap, 0);
        mWidgetCountX = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountX, 2);
        mWidgetCountY = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountY, 2);
        mClingFocusedX = a.getInt(R.styleable.AppsCustomizePagedView_clingFocusedX, 0);
        mClingFocusedY = a.getInt(R.styleable.AppsCustomizePagedView_clingFocusedY, 0);
        a.recycle();
        mWidgetSpacingLayout = new PagedViewCellLayout(getContext());

        // The padding on the non-matched dimension for the default widget preview icons
        // (top + bottom)
        mWidgetPreviewIconPaddedDimension =
            (int) (mAppIconSize * (1 + (2 * sWidgetPreviewIconPaddingPercentage)));
        mFadeInAdjacentScreens = false;
    }

    @Override
    protected void init() {
        super.init();
        mCenterPagesVertically = false;

        Context context = getContext();
        Resources r = context.getResources();
        setDragSlopeThreshold(r.getInteger(R.integer.config_appsCustomizeDragSlopeThreshold)/100f);
    }

    @Override
    protected void onUnhandledTap(MotionEvent ev) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onUnhandledTap: action = " + ev.getAction() + ",ev = " + ev);
        }
        if (LauncherApplication.isScreenLarge()) {
            // Dismiss AppsCustomize if we tap
            mLauncher.showWorkspace(true);
        }
    }

    /** Returns the item index of the center item on this page so that we can restore to this
     *  item index when we rotate. */
    private int getMiddleComponentIndexOnCurrentPage() {
        int i = -1;
        if (getPageCount() > 0) {
            int currentPage = getCurrentPage();
            if (currentPage < mNumAppsPages) {
                PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(currentPage);
                PagedViewCellLayoutChildren childrenLayout = layout.getChildrenLayout();
                int numItemsPerPage = mCellCountX * mCellCountY;
                int childCount = childrenLayout.getChildCount();
                if (childCount > 0) {
                    i = (currentPage * numItemsPerPage) + (childCount / 2);
                }
            } else {
                int numApps = mApps.size();
                PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(currentPage);
                int numItemsPerPage = mWidgetCountX * mWidgetCountY;
                int childCount = layout.getChildCount();
                if (childCount > 0) {
                    i = numApps +
                        ((currentPage - mNumAppsPages) * numItemsPerPage) + (childCount / 2);
                }
            }
        }
        return i;
    }

    /** Get the index of the item to restore to if we need to restore the current page. */
    int getSaveInstanceStateIndex() {
        if (mSaveInstanceStateItemIndex == -1) {
            mSaveInstanceStateItemIndex = getMiddleComponentIndexOnCurrentPage();
        }
        return mSaveInstanceStateItemIndex;
    }
    
    /**
     * Force set mSaveInstanceStateItemIndex to set the current page item,
     * called when user click the tab to change content.
     */
    void forceSetInstanceStateIndex() {
        mSaveInstanceStateItemIndex = getMiddleComponentIndexOnCurrentPage();
    }

    /** Returns the page in the current orientation which is expected to contain the specified
     *  item index. */
    int getPageForComponent(int index) {
        if (index < 0) return 0;

        if (index < mApps.size()) {
            int numItemsPerPage = mCellCountX * mCellCountY;
            return (index / numItemsPerPage);
        } else {
            int numItemsPerPage = mWidgetCountX * mWidgetCountY;
            return mNumAppsPages + ((index - mApps.size()) / numItemsPerPage);
        }
    }

    /**
     * This differs from isDataReady as this is the test done if isDataReady is not set.
     */
    private boolean testDataReady() {
        // We only do this test once, and we default to the Applications page, so we only really
        // have to wait for there to be apps.
        // TODO: What if one of them is validly empty
        return !mApps.isEmpty() && !mWidgets.isEmpty() && mAppsHasSet;
    }

    /** Restores the page for an item at the specified index */
    void restorePageForIndex(int index) {
        if (index < 0) return;
        mSaveInstanceStateItemIndex = index;
    }

    private void updatePageCounts() {
        mNumWidgetPages = (int) Math.ceil(mWidgets.size() /
                (float) (mWidgetCountX * mWidgetCountY));
        mNumAppsPages = (int) Math.ceil((float) mApps.size() / (mCellCountX * mCellCountY));
		if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, " < -------- updatePageCounts end:  mNumWidgetPages = "
                    + mNumWidgetPages + ", mNumAppsPages = " + mNumAppsPages + ", mApps.size() = "
                    + mApps.size() + ",mCellCountX = " + mCellCountX + ",mCellCountY = "
                    + mCellCountY + ",this = " + this);
		}
    }

    protected void onDataReady(int width, int height) {
        // Note that we transpose the counts in portrait so that we get a similar layout
        boolean isLandscape = getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
        int maxCellCountX = Integer.MAX_VALUE;
        int maxCellCountY = Integer.MAX_VALUE;
        if (LauncherApplication.isScreenLarge()) {
            maxCellCountX = (isLandscape ? LauncherModel.getCellCountX() :
                LauncherModel.getCellCountY());
            maxCellCountY = (isLandscape ? LauncherModel.getCellCountY() :
                LauncherModel.getCellCountX());
        }
        if (mMaxAppCellCountX > -1) {
            maxCellCountX = Math.min(maxCellCountX, mMaxAppCellCountX);
        }
        if (mMaxAppCellCountY > -1) {
            maxCellCountY = Math.min(maxCellCountY, mMaxAppCellCountY);
        }

        // Now that the data is ready, we can calculate the content width, the number of cells to
        // use for each page
        mWidgetSpacingLayout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        mWidgetSpacingLayout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);
        mWidgetSpacingLayout.calculateCellCount(width, height, maxCellCountX, maxCellCountY);
        mCellCountX = mWidgetSpacingLayout.getCellCountX();
        mCellCountY = mWidgetSpacingLayout.getCellCountY();
        updatePageCounts();

        // Force a measure to update recalculate the gaps
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        mWidgetSpacingLayout.measure(widthSpec, heightSpec);
        mContentWidth = mWidgetSpacingLayout.getContentWidth();

        AppsCustomizeTabHost host = (AppsCustomizeTabHost) getTabHost();
        final boolean hostIsTransitioning = host.isTransitioning();

        // Restore the page
        int page = getPageForComponent(mSaveInstanceStateItemIndex);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onDataReady: height = " + height + ",width = " + width
                    + ",isLandscape = " + isLandscape + ",page = " + page
                    + ",hostIsTransitioning = " + hostIsTransitioning + ",mContentWidth = "
                    + mContentWidth + ",mNumAppsPages = " + mNumAppsPages + ",mNumWidgetPages = "
                    + mNumWidgetPages + ",this = " + this);
        }
        invalidatePageData(Math.max(0, page), hostIsTransitioning);

        // Show All Apps cling if we are finished transitioning, otherwise, we will try again when
        // the transition completes in AppsCustomizeTabHost (otherwise the wrong offsets will be
        // returned while animating)
        if (!hostIsTransitioning) {
            post(new Runnable() {
                @Override
                public void run() {
                	showAllAppsCling();
                }
            });
        }
    }

    void showAllAppsCling() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "showAllAppsCling: mHasShownAllAppsCling = " + mHasShownAllAppsCling);
        }
        if (!mHasShownAllAppsCling && isDataReady() && testDataReady()) {
            mHasShownAllAppsCling = true;
            // Calculate the position for the cling punch through
            int[] offset = new int[2];
            int[] pos = mWidgetSpacingLayout.estimateCellPosition(mClingFocusedX, mClingFocusedY);
            mLauncher.getDragLayer().getLocationInDragLayer(this, offset);
            // PagedViews are centered horizontally but top aligned
            pos[0] += (getMeasuredWidth() - mWidgetSpacingLayout.getMeasuredWidth()) / 2 +
                    offset[0];
            pos[1] += offset[1];
            mLauncher.showFirstRunAllAppsCling(pos);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (!isDataReady()) {
            if (testDataReady()) {
                setDataIsReady();
                setMeasuredDimension(width, height);
                onDataReady(width, height);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void onPackagesUpdated() {
        // TODO: this isn't ideal, but we actually need to delay here. This call is triggered
        // by a broadcast receiver, and in order for it to work correctly, we need to know that
        // the AppWidgetService has already received and processed the same broadcast. Since there
        // is no guarantee about ordering of broadcast receipt, we just delay here. Ideally,
        // we should have a more precise way of ensuring the AppWidgetService is up to date.
        postDelayed(new Runnable() {
           public void run() {
               updatePackages();
           }
        }, 500);
    }

    public void updatePackages() {
        // Get the list of widgets and shortcuts
        boolean wasEmpty = mWidgets.isEmpty();
        mWidgets.clear();
        List<AppWidgetProviderInfo> widgets =
            AppWidgetManager.getInstance(mLauncher).getInstalledProviders();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "updatePackages: wasEmpty = " + wasEmpty + ",widgets size = "
                    + widgets.size());
        }
        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<ResolveInfo> shortcuts = mPackageManager.queryIntentActivities(shortcutsIntent, 0);
        for (AppWidgetProviderInfo widget : widgets) {
            if (widget.minWidth > 0 && widget.minHeight > 0) {
                mWidgets.add(widget);
            } else {
                Log.e(TAG, "Widget " + widget.provider + " has invalid dimensions (" +
                        widget.minWidth + ", " + widget.minHeight + ")");
            }
        }
        mWidgets.addAll(shortcuts);
        Collections.sort(mWidgets,
                new LauncherModel.WidgetAndShortcutNameComparator(mPackageManager));
        updatePageCounts();

        if (wasEmpty) {
            // The next layout pass will trigger data-ready if both widgets and apps are set, so request
            // a layout to do this test and invalidate the page data when ready.
            if (testDataReady()) requestLayout();
        } else {
            cancelAllTasks();
            invalidatePageData();
        }
    }

    @Override
    public void onClick(View v) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onClick: v = " + v + ", v.getTag() = " + v.getTag());
        }
        // When we have exited all apps or are in transition, disregard clicks
        if (!mLauncher.isAllAppsCustomizeOpen() ||
                mLauncher.getWorkspace().isSwitchingState()) return;

        if (v instanceof MTKAppIcon) {
        	v = ((MTKAppIcon)v).mAppIcon;
        }
        
        if (v instanceof PagedViewIcon) {
            // Animate some feedback to the click
            final ApplicationInfo appInfo = (ApplicationInfo) v.getTag();
            animateClickFeedback(v, new Runnable() {
                @Override
                public void run() {
                    mLauncher.startActivitySafely(appInfo.intent, appInfo);
                }
            });
        } else if (v instanceof PagedViewWidget) {
            // Let the user know that they have to long press to add a widget
            Toast.makeText(getContext(), R.string.long_press_widget_to_add,
                    Toast.LENGTH_SHORT).show();

            // Create a little animation to show that the widget can move
            float offsetY = getResources().getDimensionPixelSize(R.dimen.dragViewOffsetY);
            final ImageView p = (ImageView) v.findViewById(R.id.widget_preview);
            AnimatorSet bounce = new AnimatorSet();
            ValueAnimator tyuAnim = ObjectAnimator.ofFloat(p, "translationY", offsetY);
            tyuAnim.setDuration(125);
            ValueAnimator tydAnim = ObjectAnimator.ofFloat(p, "translationY", 0f);
            tydAnim.setDuration(100);
            bounce.play(tyuAnim).before(tydAnim);
            bounce.setInterpolator(new AccelerateInterpolator());
            bounce.start();
        }
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return FocusHelper.handleAppsCustomizeKeyEvent(v,  keyCode, event);
    }

    /*
     * PagedViewWithDraggableItems implementation
     */
    @Override
    protected void determineDraggingStart(android.view.MotionEvent ev) {
        // Disable dragging by pulling an app down for now.
    }

    private void beginDraggingApplication(View v) {
        mLauncher.getWorkspace().onDragStartedWithItem(v);
        mLauncher.getWorkspace().beginDragShared(v, this);
    }

    private void beginDraggingWidget(View v) {
        // Get the widget preview as the drag representation
        ImageView image = (ImageView) v.findViewById(R.id.widget_preview);
        PendingAddItemInfo createItemInfo = (PendingAddItemInfo) v.getTag();

        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "beginDraggingWidget: createItemInfo = " + createItemInfo + ",v = "
                    + v + ",image = " + image + ",this = " + this);
        }
        // Compose the drag image
        Bitmap preview;
        Bitmap outline;
        if (createItemInfo instanceof PendingAddWidgetInfo) {
            PendingAddWidgetInfo createWidgetInfo = (PendingAddWidgetInfo) createItemInfo;
            int[] spanXY = mLauncher.getSpanForWidget(createWidgetInfo, null);
            createItemInfo.spanX = spanXY[0];
            createItemInfo.spanY = spanXY[1];

            int[] maxSize = mLauncher.getWorkspace().estimateItemSize(spanXY[0], spanXY[1],
                    createWidgetInfo, true);
            preview = getWidgetPreview(createWidgetInfo.componentName, createWidgetInfo.previewImage,
                    createWidgetInfo.icon, spanXY[0], spanXY[1], maxSize[0], maxSize[1]);
        } else {
            // Workaround for the fact that we don't keep the original ResolveInfo associated with
            // the shortcut around.  To get the icon, we just render the preview image (which has
            // the shortcut icon) to a new drag bitmap that clips the non-icon space.
            preview = Bitmap.createBitmap(mWidgetPreviewIconPaddedDimension,
                    mWidgetPreviewIconPaddedDimension, Bitmap.Config.ARGB_8888);
            Drawable d = image.getDrawable();
            mCanvas.setBitmap(preview);
            d.draw(mCanvas);
            mCanvas.setBitmap(null);
            createItemInfo.spanX = createItemInfo.spanY = 1;
        }

        // We use a custom alpha clip table for the default widget previews
        Paint alphaClipPaint = null;
        if (createItemInfo instanceof PendingAddWidgetInfo) {
            if (((PendingAddWidgetInfo) createItemInfo).previewImage != 0) {
                MaskFilter alphaClipTable = TableMaskFilter.CreateClipTable(0, 255);
                alphaClipPaint = new Paint();
                alphaClipPaint.setMaskFilter(alphaClipTable);
            }
        }

        // Save the preview for the outline generation, then dim the preview
        outline = Bitmap.createScaledBitmap(preview, preview.getWidth(), preview.getHeight(),
                false);
        mCanvas.setBitmap(preview);
        mCanvas.drawColor(mDragViewMultiplyColor, PorterDuff.Mode.MULTIPLY);
        mCanvas.setBitmap(null);

        // Start the drag
        alphaClipPaint = null;
        mLauncher.lockScreenOrientationOnLargeUI();
        mLauncher.getWorkspace().onDragStartedWithItem(createItemInfo, outline, alphaClipPaint);
        mDragController.startDrag(image, preview, this, createItemInfo,
                DragController.DRAG_ACTION_COPY, null);
        outline.recycle();
        preview.recycle();
    }
    
    @Override
    protected boolean beginDragging(View v) {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "beginDragging: v = " + v + ",this = " + this);
        }
        // Dismiss the cling
        mLauncher.dismissAllAppsCling(null);

        if (!super.beginDragging(v)) return false;

        // Go into spring loaded mode (must happen before we startDrag())
        mLauncher.enterSpringLoadedDragMode();

        if (v instanceof PagedViewIcon) {
            beginDraggingApplication(v);
        } else if (v instanceof PagedViewWidget) {
            beginDraggingWidget(v);
        }
        return true;
    }
    
    private void endDragging(View target, boolean success) {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "endDragging: target = " + target + ",success = " + success);
        }
        mLauncher.getWorkspace().onDragStopped(success);
        if (!success || (target != mLauncher.getWorkspace() &&
                !(target instanceof DeleteDropTarget))) {
            // Exit spring loaded mode if we have not successfully dropped or have not handled the
            // drop in Workspace
            mLauncher.exitSpringLoadedDragMode();
        }
        mLauncher.unlockScreenOrientationOnLargeUI();

    }

    @Override
    public void onDropCompleted(View target, DragObject d, boolean success) {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d(TAG, "onDropCompleted: target = " + target + ",d = " + d + ",success = " + success);
        }
        endDragging(target, success);

        // Display an error message if the drag failed due to there not being enough space on the
        // target layout we were dropping on.
        if (!success) {
            boolean showOutOfSpaceMessage = false;
            if (target instanceof Workspace) {
                int currentScreen = mLauncher.getCurrentWorkspaceScreen();
                Workspace workspace = (Workspace) target;
                CellLayout layout = (CellLayout) workspace.getChildAt(currentScreen);
                ItemInfo itemInfo = (ItemInfo) d.dragInfo;
                if (layout != null) {
                    layout.calculateSpans(itemInfo);
                    showOutOfSpaceMessage =
                            !layout.findCellForSpan(null, itemInfo.spanX, itemInfo.spanY);
                }
                if (d.dragInfo instanceof PendingAddWidgetInfo) {
                	PendingAddWidgetInfo info = (PendingAddWidgetInfo)d.dragInfo;
                	if(workspace.searchIMTKWidget(workspace, info.componentName.getClassName()) != null) {
                		mLauncher.showOnlyOneWidgetMessage(info);
                	}
                }
            }
            if (showOutOfSpaceMessage) {
                mLauncher.showOutOfSpaceMessage();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAllTasks();
    }

    public void clearAllWidgetPages() {        
        cancelAllTasks();
        int count = getChildCount();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "clearAllWidgetPages: count = " + count);
        }
        for (int i = 0; i < count; i++) {
            View v = getPageAt(i);
            if (v instanceof PagedViewGridLayout) {
                ((PagedViewGridLayout) v).removeAllViewsOnPage();
                mDirtyPageContent.set(i, true);
            }
        }
    }

    private void cancelAllTasks() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "cancelAllTasks: mRunningTasks size = " + mRunningTasks.size());
        }
        // Clean up all the async tasks
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            task.cancel(false);
            iter.remove();
        }
    }

    public void setContentType(ContentType type) {
        if (type == ContentType.Widgets) {
            invalidatePageData(mNumAppsPages, true);
        } else if (type == ContentType.Applications) {
            invalidatePageData(0, true);
        }
    }

    protected void snapToPage(int whichPage, int delta, int duration) {
        super.snapToPage(whichPage, delta, duration);
        updateCurrentTab(whichPage);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "snapToPage: whichPage = " + whichPage + ",delta = "
                    + delta + ",duration = " + duration + ",this = " + this);
        }
        
        // Update the thread priorities given the direction lookahead
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int pageIndex = task.page + mNumAppsPages;
            if ((mNextPage > mCurrentPage && pageIndex >= mCurrentPage) ||
                (mNextPage < mCurrentPage && pageIndex <= mCurrentPage)) {
                task.setThreadPriority(getThreadPriorityForPage(pageIndex));
            } else {
                task.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            }
        }
    }

    private void updateCurrentTab(int currentPage) {
        AppsCustomizeTabHost tabHost = getTabHost();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "updateCurrentTab: currentPage = " + currentPage
                    + ",mCurrentPage = " + mCurrentPage + ",this = " + this);
        }
        if (tabHost != null) {
            String tag = tabHost.getCurrentTabTag();
            if (tag != null) {
                if (currentPage >= mNumAppsPages &&
                        !tag.equals(tabHost.getTabTagForContentType(ContentType.Widgets))) {
                    tabHost.setCurrentTabFromContent(ContentType.Widgets);
                } else if (currentPage < mNumAppsPages &&
                        !tag.equals(tabHost.getTabTagForContentType(ContentType.Applications))) {
                    tabHost.setCurrentTabFromContent(ContentType.Applications);
                }
            }
        }
    }

    /*
     * Apps PagedView implementation
     */
    private void setVisibilityOnChildren(ViewGroup layout, int visibility) {
        int childCount = layout.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            layout.getChildAt(i).setVisibility(visibility);
        }
    }
    
    private void setupPage(PagedViewCellLayout layout) {
        layout.setCellCount(mCellCountX, mCellCountY);
        layout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

        // Note: We force a measure here to get around the fact that when we do layout calculations
        // immediately after syncing, we don't have a proper width.  That said, we already know the
        // expected page width, so we can actually optimize by hiding all the TextView-based
        // children that are expensive to measure, and let that happen naturally later.
        setVisibilityOnChildren(layout, View.GONE);
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        layout.setMinimumWidth(getPageContentWidth());
        layout.measure(widthSpec, heightSpec);
        setVisibilityOnChildren(layout, View.VISIBLE);
    }

    public void syncAppsPageItems(int page, boolean immediate) {
        // ensure that we have the right number of items on the pages
        int numCells = mCellCountX * mCellCountY;
        int startIndex = page * numCells;
        int endIndex = Math.min(startIndex + numCells, mApps.size());
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "syncAppsPageItems: page = " + page + ",immediate = " + immediate
                    + ",numCells = " + numCells + ",startIndex = " + startIndex + ",endIndex = "
                    + endIndex + ",app size = " + mApps.size() + ",child count = "
                    + getChildCount() + ",this = " + this);
        }
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page);  
        layout.removeAllViewsOnPage();
        ArrayList<Object> items = new ArrayList<Object>();
        ArrayList<Bitmap> images = new ArrayList<Bitmap>();
        for (int i = startIndex; i < endIndex; ++i) {
            ApplicationInfo info = mApps.get(i);
            MTKAppIcon icon = (MTKAppIcon) mLayoutInflater.inflate(
                    R.layout.mtk_apps_customize_application, layout, false);
            icon.applyFromApplicationInfo(info, true, mHolographicOutlineHelper);
            //icon.setOnClickListener(this);
            //icon.setOnLongClickListener(this);
            icon.mAppIcon.setOnClickListener(this);
            icon.mAppIcon.setOnLongClickListener(this);
            icon.setOnTouchListener(this);
            icon.mAppIcon.setOnKeyListener(this);

            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayout(icon, -1, i, new PagedViewCellLayout.LayoutParams(x,y, 1,1));

            items.add(info);
            images.add(info.iconBitmap);
        }

        layout.createHardwareLayers();

        /* TEMPORARILY DISABLE HOLOGRAPHIC ICONS
        if (mFadeInAdjacentScreens) {
            prepareGenerateHoloOutlinesTask(page, items, images);
        }
        */
    }

    /**
     * A helper to return the priority for loading of the specified widget page.
     */
    private int getWidgetPageLoadPriority(int page) {
        // If we are snapping to another page, use that index as the target page index
        int toPage = mCurrentPage;
        if (mNextPage > -1) {
            toPage = mNextPage;
        }

        // We use the distance from the target page as an initial guess of priority, but if there
        // are no pages of higher priority than the page specified, then bump up the priority of
        // the specified page.
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        int minPageDiff = Integer.MAX_VALUE;
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            minPageDiff = Math.abs(task.page + mNumAppsPages - toPage);
        }

        int rawPageDiff = Math.abs(page - toPage);
        return rawPageDiff - Math.min(rawPageDiff, minPageDiff);
    }
    /**
     * Return the appropriate thread priority for loading for a given page (we give the current
     * page much higher priority)
     */
    private int getThreadPriorityForPage(int page) {
        // TODO-APPS_CUSTOMIZE: detect number of cores and set thread priorities accordingly below
        int pageDiff = getWidgetPageLoadPriority(page);
        if (pageDiff <= 0) {
            return Process.THREAD_PRIORITY_LESS_FAVORABLE;
        } else if (pageDiff <= 1) {
            return Process.THREAD_PRIORITY_LOWEST;
        } else {
            return Process.THREAD_PRIORITY_LOWEST;
        }
    }
    
    private int getSleepForPage(int page) {
        int pageDiff = getWidgetPageLoadPriority(page);
        return Math.max(0, pageDiff * sPageSleepDelay);
    }
    
    /**
     * Creates and executes a new AsyncTask to load a page of widget previews.
     */
    private void prepareLoadWidgetPreviewsTask(int page, ArrayList<Object> widgets,
            int cellWidth, int cellHeight, int cellCountX) {
        // Prune all tasks that are no longer needed
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int taskPage = task.page + mNumAppsPages;
            if (taskPage < getAssociatedLowerPageBound(mCurrentPage) ||
                    taskPage > getAssociatedUpperPageBound(mCurrentPage)) {
                task.cancel(false);
                iter.remove();
            } else {
                task.setThreadPriority(getThreadPriorityForPage(taskPage));
            }
        }

        // We introduce a slight delay to order the loading of side pages so that we don't thrash
        final int sleepMs = getSleepForPage(page + mNumAppsPages);
        AsyncTaskPageData pageData = new AsyncTaskPageData(page, widgets, cellWidth, cellHeight,
            new AsyncTaskCallback() {
                @Override
                public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                    try {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (Exception e) {}
                        loadWidgetPreviewsInBackground(task, data);
                    } finally {
                        if (task.isCancelled()) {
                            data.cleanup(true);
                        }
                    }
                }
            },
            new AsyncTaskCallback() {
                @Override
                public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                    try {
                        mRunningTasks.remove(task);
                        if (task.isCancelled()) return;
                        onSyncWidgetPageItems(data);
                    } finally {
                        data.cleanup(task.isCancelled());
                    }
                }
            });

        // Ensure that the task is appropriately prioritized and runs in parallel
        AppsCustomizeAsyncTask t = new AppsCustomizeAsyncTask(page,
                AsyncTaskPageData.Type.LoadWidgetPreviewData);
        t.setThreadPriority(getThreadPriorityForPage(page + mNumAppsPages));
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pageData);
        mRunningTasks.add(t);
    }
    
    /**
     * Creates and executes a new AsyncTask to load the outlines for a page of content.
     */
    private void prepareGenerateHoloOutlinesTask(int page, ArrayList<Object> items,
            ArrayList<Bitmap> images) {
        // Prune old tasks for this page
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int taskPage = task.page;
            if ((taskPage == page) &&
                    (task.dataType == AsyncTaskPageData.Type.LoadHolographicIconsData)) {
                task.cancel(false);
                iter.remove();
            }
        }

        AsyncTaskPageData pageData = new AsyncTaskPageData(page, items, images,
            new AsyncTaskCallback() {
                @Override
                public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                    try {
                        // Ensure that this task starts running at the correct priority
                        task.syncThreadPriority();

                        ArrayList<Bitmap> images = data.generatedImages;
                        ArrayList<Bitmap> srcImages = data.sourceImages;
                        int count = srcImages.size();
                        Canvas c = new Canvas();
                        for (int i = 0; i < count && !task.isCancelled(); ++i) {
                            // Before work on each item, ensure that this task is running at the correct
                            // priority
                            task.syncThreadPriority();

                            Bitmap b = srcImages.get(i);
                            Bitmap outline = Bitmap.createBitmap(b.getWidth(), b.getHeight(),
                                    Bitmap.Config.ARGB_8888);

                            c.setBitmap(outline);
                            c.save();
                            c.drawBitmap(b, 0, 0, null);
                            c.restore();
                            c.setBitmap(null);

                            images.add(outline);
                        }
                    } finally {
                        if (task.isCancelled()) {
                            data.cleanup(true);
                        }
                    }
                }
            },
            new AsyncTaskCallback() {
                @Override
                public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                    try {
                        mRunningTasks.remove(task);
                        if (task.isCancelled()) return;
                        onHolographicPageItemsLoaded(data);
                    } finally {
                        data.cleanup(task.isCancelled());
                    }
                }
            });

        // Ensure that the outline task always runs in the background, serially
        AppsCustomizeAsyncTask t =
            new AppsCustomizeAsyncTask(page, AsyncTaskPageData.Type.LoadHolographicIconsData);
        t.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        t.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, pageData);
        mRunningTasks.add(t);
    }

    /*
     * Widgets PagedView implementation
     */
    private void setupPage(PagedViewGridLayout layout) {
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

        // Note: We force a measure here to get around the fact that when we do layout calculations
        // immediately after syncing, we don't have a proper width.
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        layout.setMinimumWidth(getPageContentWidth());
        layout.measure(widthSpec, heightSpec);
    }

    private void renderDrawableToBitmap(Drawable d, Bitmap bitmap, int x, int y, int w, int h) {
        renderDrawableToBitmap(d, bitmap, x, y, w, h, 1f, 0xFFFFFFFF);
    }
    
    private void renderDrawableToBitmap(Drawable d, Bitmap bitmap, int x, int y, int w, int h,
            float scale, int multiplyColor) {
        if (bitmap != null) {
            Canvas c = new Canvas(bitmap);
            c.scale(scale, scale);
            Rect oldBounds = d.copyBounds();
            d.setBounds(x, y, x + w, y + h);
            d.draw(c);
            d.setBounds(oldBounds); // Restore the bounds
            if (multiplyColor != 0xFFFFFFFF) {
                c.drawColor(mDragViewMultiplyColor, PorterDuff.Mode.MULTIPLY);
            }
            c.setBitmap(null);
        }
    }
    private Bitmap getShortcutPreview(ResolveInfo info) {
        // Render the background
        int offset = 0;
        int bitmapSize = mAppIconSize;
        Bitmap preview = Bitmap.createBitmap(bitmapSize, bitmapSize, Config.ARGB_8888);

        // Render the icon
        Drawable icon = mIconCache.getFullResIcon(info);
        renderDrawableToBitmap(icon, preview, offset, offset, mAppIconSize, mAppIconSize);
        return preview;
    }
    
    private Bitmap getWidgetPreview(ComponentName provider, int previewImage, int iconId,
            int cellHSpan, int cellVSpan, int maxWidth, int maxHeight) {
        // Load the preview image if possible
        String packageName = provider.getPackageName();
        if (maxWidth < 0) maxWidth = Integer.MAX_VALUE;
        if (maxHeight < 0) maxHeight = Integer.MAX_VALUE;

        Drawable drawable = null;
        if (previewImage != 0) {
            drawable = mPackageManager.getDrawable(packageName, previewImage, null);
            if (drawable == null) {
                Log.w(TAG, "Can't load widget preview drawable 0x" +
                        Integer.toHexString(previewImage) + " for provider: " + provider);
            }
        }

        int bitmapWidth;
        int bitmapHeight;
        boolean widgetPreviewExists = (drawable != null);
        if (widgetPreviewExists) {
            bitmapWidth = drawable.getIntrinsicWidth();
            bitmapHeight = drawable.getIntrinsicHeight();

            // Cap the size so widget previews don't appear larger than the actual widget
            maxWidth = Math.min(maxWidth, mWidgetSpacingLayout.estimateCellWidth(cellHSpan));
            maxHeight = Math.min(maxHeight, mWidgetSpacingLayout.estimateCellHeight(cellVSpan));
        } else {
            // Determine the size of the bitmap for the preview image we will generate
            // TODO: This actually uses the apps customize cell layout params, where as we make want
            // the Workspace params for more accuracy.
            bitmapWidth = mWidgetSpacingLayout.estimateCellWidth(cellHSpan);
            bitmapHeight = mWidgetSpacingLayout.estimateCellHeight(cellVSpan);
            if (cellHSpan == cellVSpan) {
                // For square widgets, we just have a fixed size for 1x1 and larger-than-1x1
                int minOffset = (int) (mAppIconSize * sWidgetPreviewIconPaddingPercentage);
                if (cellHSpan <= 1) {
                    bitmapWidth = bitmapHeight = mAppIconSize + 2 * minOffset;
                } else {
                    bitmapWidth = bitmapHeight = mAppIconSize + 4 * minOffset;
                }
            }
        }

        float scale = 1f;
        if (bitmapWidth > maxWidth) {
            scale = maxWidth / (float) bitmapWidth;
        }
        if (bitmapHeight * scale > maxHeight) {
            scale = maxHeight / (float) bitmapHeight;
        }
        if (scale != 1f) {
            bitmapWidth = (int) (scale * bitmapWidth);
            bitmapHeight = (int) (scale * bitmapHeight);
        }

        Bitmap preview = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Config.ARGB_8888);

        if (widgetPreviewExists) {
            renderDrawableToBitmap(drawable, preview, 0, 0, bitmapWidth, bitmapHeight);
        } else {
            // Generate a preview image if we couldn't load one
            int minOffset = (int) (mAppIconSize * sWidgetPreviewIconPaddingPercentage);
            int smallestSide = Math.min(bitmapWidth, bitmapHeight);
            float iconScale = Math.min((float) smallestSide / (mAppIconSize + 2 * minOffset), 1f);
            if (cellHSpan != 1 || cellVSpan != 1) {
                renderDrawableToBitmap(mDefaultWidgetBackground, preview, 0, 0, bitmapWidth,
                        bitmapHeight);
            }

            // Draw the icon in the top left corner
            try {
                Drawable icon = null;
                int hoffset = (int) (bitmapWidth / 2 - mAppIconSize * iconScale / 2);
                int yoffset = (int) (bitmapHeight / 2 - mAppIconSize * iconScale / 2);
                if (iconId > 0) icon = mIconCache.getFullResIcon(packageName, iconId);
                Resources resources = mLauncher.getResources();
                if (icon == null) icon = resources.getDrawable(R.drawable.ic_launcher_application);

                renderDrawableToBitmap(icon, preview, hoffset, yoffset,
                        (int) (mAppIconSize * iconScale),
                        (int) (mAppIconSize * iconScale));
            } catch (Resources.NotFoundException e) {}
        }
        return preview;
    }

    public void syncWidgetPageItems(final int page, final boolean immediate) {
        int numItemsPerPage = mWidgetCountX * mWidgetCountY;

        // Calculate the dimensions of each cell we are giving to each widget
        final ArrayList<Object> items = new ArrayList<Object>();
        int contentWidth = mWidgetSpacingLayout.getContentWidth();
        final int cellWidth = ((contentWidth - mPageLayoutPaddingLeft - mPageLayoutPaddingRight
                - ((mWidgetCountX - 1) * mWidgetWidthGap)) / mWidgetCountX);
        int contentHeight = mWidgetSpacingLayout.getContentHeight();
        final int cellHeight = ((contentHeight - mPageLayoutPaddingTop - mPageLayoutPaddingBottom
                - ((mWidgetCountY - 1) * mWidgetHeightGap)) / mWidgetCountY);

        // Prepare the set of widgets to load previews for in the background
        int offset = page * numItemsPerPage;
        for (int i = offset; i < Math.min(offset + numItemsPerPage, mWidgets.size()); ++i) {
            items.add(mWidgets.get(i));
        }
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "syncWidgetPageItems: page = " + page + ",immediate = " + immediate
                    + ",numItemsPerPage = " + numItemsPerPage + ",cellWidth = " + cellWidth
                    + ",contentHeight = " + contentHeight + ",cellHeight = " + cellHeight
                    + ",offset = " + offset + ",this = " + this);
        }
        // Prepopulate the pages with the other widget info, and fill in the previews later
        final PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(page + mNumAppsPages);
        layout.setColumnCount(layout.getCellCountX());
        for (int i = 0; i < items.size(); ++i) {
            Object rawInfo = items.get(i);
            PendingAddItemInfo createItemInfo = null;
            PagedViewWidget widget = (PagedViewWidget) mLayoutInflater.inflate(
                    R.layout.apps_customize_widget, layout, false);
            if (rawInfo instanceof AppWidgetProviderInfo) {
                // Fill in the widget information
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) rawInfo;
                createItemInfo = new PendingAddWidgetInfo(info, null, null);
                int[] cellSpans = mLauncher.getSpanForWidget(info, null);
                widget.applyFromAppWidgetProviderInfo(info, -1, cellSpans,
                        mHolographicOutlineHelper);
                widget.setTag(createItemInfo);
            } else if (rawInfo instanceof ResolveInfo) {
                // Fill in the shortcuts information
                ResolveInfo info = (ResolveInfo) rawInfo;
                createItemInfo = new PendingAddItemInfo();
                createItemInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
                createItemInfo.componentName = new ComponentName(info.activityInfo.packageName,
                        info.activityInfo.name);
                widget.applyFromResolveInfo(mPackageManager, info, mHolographicOutlineHelper);
                widget.setTag(createItemInfo);
            }
            widget.setOnClickListener(this);
            widget.setOnLongClickListener(this);
            widget.setOnTouchListener(this);
            widget.setOnKeyListener(this);

            // Layout each widget
            int ix = i % mWidgetCountX;
            int iy = i / mWidgetCountX;
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(iy, GridLayout.LEFT),
                    GridLayout.spec(ix, GridLayout.TOP));
            lp.width = cellWidth;
            lp.height = cellHeight;
            lp.setGravity(Gravity.TOP | Gravity.LEFT);
            if (ix > 0) lp.leftMargin = mWidgetWidthGap;
            if (iy > 0) lp.topMargin = mWidgetHeightGap;
            layout.addView(widget, lp);
        }

        final Resources res = getContext().getResources();
        final int widgetsPaddingLeft = (int)res.getDimension(R.dimen.app_widget_preview_padding_left);
        final int widgetsPaddingTop = (int)res.getDimension(R.dimen.app_widget_preview_padding_top);
        // wait until a call on onLayout to start loading, because
        // PagedViewWidget.getPreviewSize() will return 0 if it hasn't been laid out
        // TODO: can we do a measure/layout immediately?
        layout.setOnLayoutListener(new Runnable() {
            public void run() {
                // Load the widget previews, minus the widgets padding to avoid image cut issue.
                int maxPreviewWidth = cellWidth - widgetsPaddingLeft;
                int maxPreviewHeight = cellHeight - widgetsPaddingTop;
                if (layout.getChildCount() > 0) {
                    PagedViewWidget w = (PagedViewWidget) layout.getChildAt(0);
                    int[] maxSize = w.getPreviewSize();
                    if (maxSize[0] > 0 && maxSize[1] > 0) {
                        maxPreviewWidth = maxSize[0];
                        maxPreviewHeight = maxSize[1];
                    }               
                }
                if (immediate) {
                    AsyncTaskPageData data = new AsyncTaskPageData(page, items,
                            maxPreviewWidth, maxPreviewHeight, null, null);
                    loadWidgetPreviewsInBackground(null, data);
                    onSyncWidgetPageItems(data);
                } else {
                    prepareLoadWidgetPreviewsTask(page, items,
                            maxPreviewWidth, maxPreviewHeight, mWidgetCountX);
                }
            }
        });
    }
    
    private void loadWidgetPreviewsInBackground(AppsCustomizeAsyncTask task,
            AsyncTaskPageData data) {
        // loadWidgetPreviewsInBackground can be called without a task to load a set of widget
        // previews synchronously
        if (task != null) {
            // Ensure that this task starts running at the correct priority
            task.syncThreadPriority();
        }

        // Load each of the widget/shortcut previews
        ArrayList<Object> items = data.items;
        ArrayList<Bitmap> images = data.generatedImages;
        int count = items.size();
        for (int i = 0; i < count; ++i) {
            if (task != null) {
                // Ensure we haven't been cancelled yet
                if (task.isCancelled()) break;
                // Before work on each item, ensure that this task is running at the correct
                // priority
                task.syncThreadPriority();
            }

            Object rawInfo = items.get(i);
            if (rawInfo instanceof AppWidgetProviderInfo) {
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) rawInfo;
                int[] cellSpans = mLauncher.getSpanForWidget(info, null);
                Bitmap b = getWidgetPreview(info.provider, info.previewImage, info.icon,
                        cellSpans[0], cellSpans[1], data.maxImageWidth, data.maxImageHeight);
                images.add(b);
            } else if (rawInfo instanceof ResolveInfo) {
                // Fill in the shortcuts information
                ResolveInfo info = (ResolveInfo) rawInfo;
                images.add(getShortcutPreview(info));
            }
        }
    }
    
    private void onSyncWidgetPageItems(AsyncTaskPageData data) {
        int page = data.page;
        PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(page + mNumAppsPages);

        ArrayList<Object> items = data.items;
        int count = items.size();
        for (int i = 0; i < count; ++i) {
            PagedViewWidget widget = (PagedViewWidget) layout.getChildAt(i);
            if (widget != null) {
                Bitmap preview = data.generatedImages.get(i);
                widget.applyPreview(new FastBitmapDrawable(preview), i);
            }
        }

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onSyncWidgetPageItems: page = " + page + ",layout = " + 
                    layout + ",count = " + count + ",this = " + this);
        }
        layout.createHardwareLayer();
        invalidate();

        /* TEMPORARILY DISABLE HOLOGRAPHIC ICONS
        if (mFadeInAdjacentScreens) {
            prepareGenerateHoloOutlinesTask(data.page, data.items, data.generatedImages);
        }
        */

        // Update all thread priorities
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int pageIndex = task.page + mNumAppsPages;
            task.setThreadPriority(getThreadPriorityForPage(pageIndex));
        }
    }
    
    private void onHolographicPageItemsLoaded(AsyncTaskPageData data) {
        // Invalidate early to short-circuit children invalidates
        invalidate();

        int page = data.page;
        ViewGroup layout = (ViewGroup) getPageAt(page);
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onSyncWidgetPageItems: page = " + page + ",layout = " + layout
                    + ",this = " + this);
        }
        if (layout instanceof PagedViewCellLayout) {
            PagedViewCellLayout cl = (PagedViewCellLayout) layout;
            int count = cl.getPageChildCount();
            if (count != data.generatedImages.size()) return;
            for (int i = 0; i < count; ++i) {
                MTKAppIcon icon = (MTKAppIcon) cl.getChildOnPageAt(i);
                icon.setHolographicOutline(data.generatedImages.get(i));
            }
        } else {
            int count = layout.getChildCount();
            if (count != data.generatedImages.size()) return;
            for (int i = 0; i < count; ++i) {
                View v = layout.getChildAt(i);
                ((PagedViewWidget) v).setHolographicOutline(data.generatedImages.get(i));
            }
        }
    }

    @Override
    public void syncPages() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "syncPages: mNumWidgetPages = " + mNumWidgetPages + ",mNumAppsPages = "
                    + mNumAppsPages + ",this = " + this);
        }
        removeAllViews();
        cancelAllTasks();

        Context context = getContext();
        for (int j = 0; j < mNumWidgetPages; ++j) {
            PagedViewGridLayout layout = new PagedViewGridLayout(context, mWidgetCountX,
                    mWidgetCountY);
            setupPage(layout);
            addView(layout, new PagedViewGridLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
        }

        for (int i = 0; i < mNumAppsPages; ++i) {
            PagedViewCellLayout layout = new PagedViewCellLayout(context);
            setupPage(layout);
            addView(layout);
			if (LauncherLog.DEBUG) {
				LauncherLog.d(TAG,
						"<---- syncPages: PagedViewCellLayout layout  = "
								+ layout);
			}
        }
    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "syncPageItems: page = " + page + ",immediate = " + immediate
                    + ",mNumAppsPages = " + mNumAppsPages, new Throwable("syncPageItems"));
        }
        if (page < mNumAppsPages) {
            syncAppsPageItems(page, immediate);
        } else {
            syncWidgetPageItems(page - mNumAppsPages, immediate);
        }
    }

    // We want our pages to be z-ordered such that the further a page is to the left, the higher
    // it is in the z-order. This is important to insure touch events are handled correctly.
    View getPageAt(int index) {
        return getChildAt(getChildCount() - index - 1);
    }

    @Override
    protected int indexToPage(int index) {
        return getChildCount() - index - 1;
    }

    // In apps customize, we have a scrolling effect which emulates pulling cards off of a stack.
    @Override
    protected void screenScrolled(int screenCenter) {
        super.screenScrolled(screenCenter);

        if (LauncherLog.DEBUG_LAYOUT) {
            LauncherLog.d(TAG, "screenScrolled: screenCenter = " + screenCenter
                    + ",mOverScrollX = " + mOverScrollX + ",mMaxScrollX = " + mMaxScrollX
                    + ",mScrollX = " + mScrollX + ",this = " + this);
        }
        for (int i = 0; i < getChildCount(); i++) {
            View v = getPageAt(i);
            if (v != null) {
                float scrollProgress = getScrollProgress(screenCenter, v, i);

                float interpolatedProgress =
                        mZInterpolator.getInterpolation(Math.abs(Math.min(scrollProgress, 0)));
                float scale = (1 - interpolatedProgress) +
                        interpolatedProgress * TRANSITION_SCALE_FACTOR;
                float translationX = Math.min(0, scrollProgress) * v.getMeasuredWidth();

                float alpha;

                if (!LauncherApplication.isScreenLarge() || scrollProgress < 0) {
                    alpha = scrollProgress < 0 ? mAlphaInterpolator.getInterpolation(
                        1 - Math.abs(scrollProgress)) : 1.0f;
                } else {
                    // On large screens we need to fade the page as it nears its leftmost position
                    alpha = mLeftScreenAlphaInterpolator.getInterpolation(1 - scrollProgress);
                }

                v.setCameraDistance(mDensity * CAMERA_DISTANCE);
                int pageWidth = v.getMeasuredWidth();
                int pageHeight = v.getMeasuredHeight();

                if (PERFORM_OVERSCROLL_ROTATION) {
                    if (i == 0 && scrollProgress < 0) {
                        // Overscroll to the left
                        v.setPivotX(TRANSITION_PIVOT * pageWidth);
                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
                        scale = 1.0f;
                        alpha = 1.0f;
                        // On the first page, we don't want the page to have any lateral motion
                        translationX = 0;
                    } else if (i == getChildCount() - 1 && scrollProgress > 0) {
                        // Overscroll to the right
                        v.setPivotX((1 - TRANSITION_PIVOT) * pageWidth);
                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
                        scale = 1.0f;
                        alpha = 1.0f;
                        // On the last page, we don't want the page to have any lateral motion.
                        translationX = 0;
                    } else {
                        v.setPivotY(pageHeight / 2.0f);
                        v.setPivotX(pageWidth / 2.0f);
                        v.setRotationY(0f);
                    }
                }

                v.setTranslationX(translationX);
                v.setScaleX(scale);
                v.setScaleY(scale);
                v.setAlpha(alpha);

                // If the view has 0 alpha, we set it to be invisible so as to prevent
                // it from accepting touches
                if (alpha < ViewConfiguration.ALPHA_THRESHOLD) {
                    v.setVisibility(INVISIBLE);
                } else if (v.getVisibility() != VISIBLE) {
                    v.setVisibility(VISIBLE);
                }
            }
        }
    }

    protected void overScroll(float amount) {
        acceleratedOverScroll(amount);
    }

    /**
     * Used by the parent to get the content width to set the tab bar to
     * @return
     */
    public int getPageContentWidth() {
        return mContentWidth;
    }

    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();

        // We reset the save index when we change pages so that it will be recalculated on next
        // rotation
        mSaveInstanceStateItemIndex = -1;
    }

    /*
     * AllAppsView implementation
     */
    @Override
    public void setup(Launcher launcher, DragController dragController) {
        mLauncher = launcher;
        mDragController = dragController;
    }
    
    @Override
    public void zoom(float zoom, boolean animate) {
        // TODO-APPS_CUSTOMIZE: Call back to mLauncher.zoomed()
    }
    
    @Override
    public boolean isVisible() {
        return (getVisibility() == VISIBLE);
    }
    
    @Override
    public boolean isAnimating() {
        return false;
    }
    
    @Override
    public void setApps(ArrayList<ApplicationInfo> list) {
        mApps = list;
		if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, " < ------- setApps : mApps = " + mApps.size() + ",mAppsHasSet = "
                    + mAppsHasSet + ", this = " + this);
        }
        mAppsHasSet = true;
        Collections.sort(mApps, LauncherModel.APP_NAME_COMPARATOR);
        reorderApps();
        updatePageCounts();

        // The next layout pass will trigger data-ready if both widgets and apps are set, so 
        // request a layout to do this test and invalidate the page data when ready.
        if (testDataReady()) requestLayout();
    }
    
    private void addAppsWithoutInvalidate(ArrayList<ApplicationInfo> list) {
        // We add it in place, in alphabetical order
        int count = list.size();
        for (int i = 0; i < count; ++i) {
            ApplicationInfo info = list.get(i);
            int index = Collections.binarySearch(mApps, info, LauncherModel.APP_NAME_COMPARATOR);
            if (index < 0) {
                mApps.add(-(index + 1), info);
				if (LauncherLog.DEBUG) {
					LauncherLog.d(TAG,
							"< ----- addAppsWithoutInvalidate mApps size = "
									+ mApps.size() + " , info = " + info
									+ ", this = " + this);
				}
            }
        }
    }
    
    @Override
    public void addApps(ArrayList<ApplicationInfo> list) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "addApps: list = " + list + ",this = " + this);
        }
        addAppsWithoutInvalidate(list);
        reorderApps();
        updatePageCounts();
        invalidatePageData();
    }
    
    private int findAppByComponent(List<ApplicationInfo> list, ApplicationInfo item) {
        ComponentName removeComponent = item.intent.getComponent();
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
            if (info.intent.getComponent().equals(removeComponent)) {
                return i;
            }
        }
        return -1;
    }
    
    private void removeAppsWithoutInvalidate(ArrayList<ApplicationInfo> list) {
        // loop through all the apps and remove apps that have the same component
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
            int removeIndex = findAppByComponent(mApps, info);
            if (removeIndex > -1) {
                mApps.remove(removeIndex);
				if (LauncherLog.DEBUG) {
					LauncherLog.d(TAG,
							" <----- removeAppsWithoutInvalidate: removeIndex = "
									+ removeIndex + "ApplicationInfo info = "
									+ info + " ,this = " + this);
				}
            }
        }
    }
    
    @Override
    public void removeApps(ArrayList<ApplicationInfo> list) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "removeApps: list = " + list + ",this = " + this);
        }
        removeAppsWithoutInvalidate(list);
        reorderApps();
        updatePageCounts();
        invalidatePageData();
    }
    
    @Override
    public void updateApps(ArrayList<ApplicationInfo> list) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "updateApps: list = " + list + ",this = " + this);
        }
        // We remove and re-add the updated applications list because it's properties may have
        // changed (ie. the title), and this will ensure that the items will be in their proper
        // place in the list.
        removeAppsWithoutInvalidate(list);
        addAppsWithoutInvalidate(list);
        reorderApps();
        updatePageCounts();

        invalidatePageData();
    }

    @Override
    public void reset() {
        AppsCustomizeTabHost tabHost = getTabHost();
        String tag = tabHost.getCurrentTabTag();
        if (tag != null) {
            if (!tag.equals(tabHost.getTabTagForContentType(ContentType.Applications))) {
                tabHost.setCurrentTabFromContent(ContentType.Applications);
            }
        }
        if (mCurrentPage != 0) {
            invalidatePageData(0);
        }
    }

    private AppsCustomizeTabHost getTabHost() {
        return (AppsCustomizeTabHost) mLauncher.findViewById(R.id.apps_customize_pane);
    }

    @Override
    public void dumpState() {
        // TODO: Dump information related to current list of Applications, Widgets, etc.
        ApplicationInfo.dumpApplicationInfoList(TAG, "mApps", mApps);
        dumpAppWidgetProviderInfoList(TAG, "mWidgets", mWidgets);
    }

    private void dumpAppWidgetProviderInfoList(String tag, String label,
            ArrayList<Object> list) {
        Log.d(tag, label + " size=" + list.size());
        for (Object i: list) {
            if (i instanceof AppWidgetProviderInfo) {
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) i;
                Log.d(tag, "   label=\"" + info.label + "\" previewImage=" + info.previewImage
                        + " resizeMode=" + info.resizeMode + " configure=" + info.configure
                        + " initialLayout=" + info.initialLayout
                        + " minWidth=" + info.minWidth + " minHeight=" + info.minHeight);
            } else if (i instanceof ResolveInfo) {
                ResolveInfo info = (ResolveInfo) i;
                Log.d(tag, "   label=\"" + info.loadLabel(mPackageManager) + "\" icon="
                        + info.icon);
            }
        }
    }
    @Override
    public void surrender() {
        // TODO: If we are in the middle of any process (ie. for holographic outlines, etc) we
        // should stop this now.

        // Stop all background tasks
        cancelAllTasks();
    }

    /*
     * We load an extra page on each side to prevent flashes from scrolling and loading of the
     * widget previews in the background with the AsyncTasks.
     */
    final static int sLookBehindPageCount = 2;
    final static int sLookAheadPageCount = 2;
    protected int getAssociatedLowerPageBound(int page) {
        final int count = getChildCount();
        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
        int windowMinIndex = Math.max(Math.min(page - sLookBehindPageCount, count - windowSize), 0);
        return windowMinIndex;
    }
    protected int getAssociatedUpperPageBound(int page) {
        final int count = getChildCount();
        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
        int windowMaxIndex = Math.min(Math.max(page + sLookAheadPageCount, windowSize - 1),
                count - 1);
        return windowMaxIndex;
    }

    @Override
    protected String getCurrentPageDescription() {
        int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        int stringId = R.string.default_scroll_format;
        int count = 0;
        
        if (page < mNumAppsPages) {
            stringId = R.string.apps_customize_apps_scroll_format;
            count = mNumAppsPages;
        } else {
            page -= mNumAppsPages;
            stringId = R.string.apps_customize_widgets_scroll_format;
            count = mNumWidgetPages;
        }

        return String.format(mContext.getString(stringId), page + 1, count);
    }
    
    public void reorderApps(){
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "reorderApps: mApps = " + mApps + ",this = " + this);
        }
		if (AllAppsList.mTopPackages == null || mApps == null
				|| AllAppsList.mTopPackages.isEmpty() || mApps.isEmpty()) {
			return;
		}

		ArrayList<ApplicationInfo> dataReorder = new ArrayList<ApplicationInfo>(
				AllAppsList.DEFAULT_APPLICATIONS_NUMBER);

		for (AllAppsList.TopPackage tp : AllAppsList.mTopPackages) {
			int loop = 0;
			for (ApplicationInfo ai : mApps) {
				if (ai.componentName.getPackageName().equals(tp.mPackageName)
						&& ai.componentName.getClassName().equals(tp.mClassName)) {
					mApps.remove(ai);
					dataReorder.add(ai);
					break;
				}
				loop++;
			}
		}

		for (AllAppsList.TopPackage tp : AllAppsList.mTopPackages) {
			int newIndex = 0;
			for (ApplicationInfo ai : dataReorder) {
				if (ai.componentName.getPackageName().equals(tp.mPackageName)
						&& ai.componentName.getClassName().equals(tp.mClassName)) {
					newIndex = Math.min(Math.max(tp.mOrder, 0), mApps.size());
					mApps.add(newIndex, ai);
					break;
				}
			}
		}
    }
    
    /**
     * Update unread number of the given component in app customize paged view
     * with the given value, first find the icon, and then update the number.
     * NOTES: since maybe not all applications are added in the customize paged
     * view, we should update the apps info at the same time.
     * 
     * @param component
     * @param unreadNum
     */
    public void updateAppsUnreadChanged(ComponentName component, int unreadNum) {
        if (LauncherLog.DEBUG_UNREAD) {
            LauncherLog.d(TAG, "updateAppsUnreadChanged: component = " + component
                    + ",unreadNum = " + unreadNum + ",mNumAppsPages = " + mNumAppsPages);
        }
        updateUnreadNumInAppInfo(component, unreadNum);
        for (int i = 0; i < mNumAppsPages; i++) {
            PagedViewCellLayout cl = (PagedViewCellLayout) getPageAt(i);
            if (cl == null) {
                return;
            }
            final int count = cl.getPageChildCount();
            for (int j = 0; j < count; j++) {
                MTKAppIcon appIcon = (MTKAppIcon) cl.getChildOnPageAt(j);
                final ApplicationInfo appInfo = (ApplicationInfo) appIcon.getTag();
                if (LauncherLog.DEBUG_UNREAD) {
                    LauncherLog.d(TAG, "updateAppsUnreadChanged: component = " + component
                            + ",appInfo = " + appInfo.componentName + ",appIcon = " + appIcon);
                }
                if (appInfo != null && appInfo.componentName.equals(component)) {
                    appIcon.updateUnreadNum(unreadNum);
                }
            }
        }
    }

    /**
     * Update unread number of all application info with data in MTKUnreadLoader.
     */
    public void updateAppsUnread() {
        if (LauncherLog.DEBUG_UNREAD) {
            LauncherLog.d(TAG, "updateAppsUnreadChanged: mNumAppsPages = " + mNumAppsPages);
        }

        updateUnreadNumInAppInfo(mApps);
        // Update apps which already shown in the customized pane.
        for (int i = 0; i < mNumAppsPages; i++) {
            PagedViewCellLayout cl = (PagedViewCellLayout) getPageAt(i);
            if (cl == null) {
                return;
            }
            final int count = cl.getPageChildCount();
            for (int j = 0; j < count; j++) {
                MTKAppIcon appIcon = (MTKAppIcon) cl.getChildOnPageAt(j);
                final ApplicationInfo appInfo = (ApplicationInfo) appIcon.getTag();
                final int unreadNum = MTKUnreadLoader.getUnreadNumberOfComponent(appInfo.componentName);
                appIcon.updateUnreadNum(unreadNum);
                if (LauncherLog.DEBUG_UNREAD) {
                    LauncherLog.d(TAG, "updateAppsUnreadChanged: i = " + i + ",appInfo = "
                            + appInfo.componentName + ",unreadNum = " + unreadNum);
                }
            }
        }
    }
    
    /**
     * Update the unread number of the app info with given component.
     * 
     * @param component
     * @param unreadNum
     */
    private void updateUnreadNumInAppInfo(ComponentName component, int unreadNum) {
        for (int i = 0; i < mApps.size(); i++) {
            final ApplicationInfo appInfo = mApps.get(i);
            if (appInfo.intent.getComponent().equals(component)) {
                appInfo.unreadNum = unreadNum;
            }
        }
    }

    /**
     * Update unread number of all application info with data in MTKUnreadLoader.
     */
    public static void updateUnreadNumInAppInfo(final ArrayList<ApplicationInfo> apps) {
        for (int i = 0; i < apps.size(); i++) {
            final ApplicationInfo appInfo = apps.get(i);
            appInfo.unreadNum = MTKUnreadLoader.getUnreadNumberOfComponent(appInfo.componentName);
        }
    }
    
    private boolean mAppsHasSet = false;
}
