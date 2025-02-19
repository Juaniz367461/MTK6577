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
package com.android.browser;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.android.browser.UI.ComboViews;

import java.util.List;

public class NavigationBarTablet extends NavigationBarBase {

    private Drawable mStopDrawable;
    private Drawable mReloadDrawable;
    private String mStopDescription;
    private String mRefreshDescription;

    private View mUrlContainer;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private ImageView mStar;
    private ImageView mUrlIcon;
    private ImageView mSearchButton;
    private View mGoButton;
    private ImageView mStopButton;
    private View mAllButton;
    private View mClearButton;
    private ImageView mVoiceSearch;
    private View mNavButtons;
    private Drawable mFocusDrawable;
    private Drawable mUnfocusDrawable;
    private boolean mHideNavButtons;
    private Drawable mFaviconDrawable;

    public NavigationBarTablet(Context context) {
        super(context);
        init(context);
    }

    public NavigationBarTablet(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NavigationBarTablet(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        Resources resources = context.getResources();
        mStopDrawable = resources.getDrawable(R.drawable.ic_stop_holo_dark);
        mReloadDrawable = resources.getDrawable(R.drawable.ic_refresh_holo_dark);
        mStopDescription = resources.getString(R.string.accessibility_button_stop);
        mRefreshDescription = resources.getString(R.string.accessibility_button_refresh);
        mFocusDrawable = resources.getDrawable(
                R.drawable.textfield_active_holo_dark);
        mUnfocusDrawable = resources.getDrawable(
                R.drawable.textfield_default_holo_dark);
        mHideNavButtons = resources.getBoolean(R.bool.hide_nav_buttons);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAllButton = findViewById(R.id.all_btn);
        // TODO: Change enabled states based on whether you can go
        // back/forward.  Probably should be done inside onPageStarted.
        mNavButtons = findViewById(R.id.navbuttons);
        mBackButton = (ImageButton) findViewById(R.id.back);
        mForwardButton = (ImageButton) findViewById(R.id.forward);
        mUrlIcon = (ImageView) findViewById(R.id.url_icon);
        mStar = (ImageView) findViewById(R.id.star);
        mStopButton = (ImageView) findViewById(R.id.stop);
        mSearchButton = (ImageView) findViewById(R.id.search);
        mGoButton = findViewById(R.id.go);
        mClearButton = findViewById(R.id.clear);
        mVoiceSearch = (ImageView) findViewById(R.id.voicesearch);
        mUrlContainer = findViewById(R.id.urlbar_focused);
        mBackButton.setOnClickListener(this);
        mForwardButton.setOnClickListener(this);
        mStar.setOnClickListener(this);
        mAllButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mSearchButton.setOnClickListener(this);
        mGoButton.setOnClickListener(this);
        mClearButton.setOnClickListener(this);
        mVoiceSearch.setOnClickListener(this);
        mUrlInput.setContainer(mUrlContainer);
    }

    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        Resources res = mContext.getResources();
        mHideNavButtons = res.getBoolean(R.bool.hide_nav_buttons);
        if (mUrlInput.hasFocus()) {
            if (mHideNavButtons && (mNavButtons.getVisibility() == View.VISIBLE)) {
                int aw = mNavButtons.getMeasuredWidth();
                mNavButtons.setVisibility(View.GONE);
                mNavButtons.setAlpha(0f);
                mNavButtons.setTranslationX(-aw);
            } else if (!mHideNavButtons && (mNavButtons.getVisibility() == View.GONE)) {
                mNavButtons.setVisibility(View.VISIBLE);
                mNavButtons.setAlpha(1f);
                mNavButtons.setTranslationX(0);
            }
        }
    }

    @Override
    public void setTitleBar(TitleBar titleBar) {
        super.setTitleBar(titleBar);
        
        // TakMan: 2012/03/13
        // mHideNavButtons: To execute "hide/show navigation buttons' animation" 
        //                          during focusing/unfocusing.
        // hideNavButtons: "hide navigation buttons' animation"
        // showNavButtons: "show navigation buttons' animation"
        //
        // For ALPS00233726, we don't call setFocusState(false) below.
        // Instead, we copy the contents from setFocusState() and paste them 
        // to the following. It's because we don't want to execute showNavButtons() 
        // when setTitleBar() is called (when the browser is just launched). This
        // action is meaningless when the browser is just launched and harmful
        // when mNavButtons.getMeasuredWidth() is not already to get a correct
        // value which is calculated by another thread.
        //
        //setFocusState(false);
        if (mHideNavButtons)
        {
            mNavButtons.setVisibility(View.VISIBLE); //from showNavButtons()
        }
        
        mGoButton.setVisibility(View.GONE);
        mVoiceSearch.setVisibility(View.GONE);
        showHideStar(mUiController.getCurrentTab());
        mClearButton.setVisibility(View.GONE);
        if (mTitleBar.useQuickControls()) {
            mSearchButton.setVisibility(View.GONE);
        } else {
            mSearchButton.setVisibility(View.VISIBLE);
        }
        updateUrlIcon();
        mUrlContainer.setBackgroundDrawable(mUnfocusDrawable);

    }

    void updateNavigationState(Tab tab) {
        if (tab != null) {
            mBackButton.setImageResource(tab.canGoBack()
                    ? R.drawable.ic_back_holo_dark
                    : R.drawable.ic_back_disabled_holo_dark);
            mForwardButton.setImageResource(tab.canGoForward()
                    ? R.drawable.ic_forward_holo_dark
                    : R.drawable.ic_forward_disabled_holo_dark);
        }
        updateUrlIcon();
    }

    @Override
    public void onTabDataChanged(Tab tab) {
        super.onTabDataChanged(tab);
        showHideStar(tab);
    }

    @Override
    public void setCurrentUrlIsBookmark(boolean isBookmark) {
        mStar.setActivated(isBookmark);
    }

    @Override
    public void onClick(View v) {
        if (mBackButton == v) {
            mUiController.getCurrentTab().goBack();
        } else if (mForwardButton == v) {
            mUiController.getCurrentTab().goForward();
        } else if (mStar == v) {
            Intent intent = mUiController.createBookmarkCurrentPageIntent(true);
            if (intent != null) {
                getContext().startActivity(intent);
            }
        } else if (mAllButton == v) {
            mUiController.bookmarksOrHistoryPicker(ComboViews.Bookmarks);
        } else if (mSearchButton == v) {
            mBaseUi.editUrl(true);
        } else if (mStopButton == v) {
            stopOrRefresh();
        } else if (mGoButton == v) {
            if (!TextUtils.isEmpty(mUrlInput.getText())) {
                onAction(mUrlInput.getText().toString(), null,
                        UrlInputView.TYPED);
            }
        } else if (mClearButton == v) {
            clearOrClose();
        } else if (mVoiceSearch == v) {
            mUiController.startVoiceSearch();
        } else {
            super.onClick(v);
        }
    }

    private void clearOrClose() {
        if (TextUtils.isEmpty(mUrlInput.getText())) {
            // close
            mUrlInput.clearFocus();
        } else {
            // clear
            mUrlInput.setText("");
        }
    }

    @Override
    public void setFavicon(Bitmap icon) {
        mFaviconDrawable = mBaseUi.getFaviconDrawable(icon);
        updateUrlIcon();
    }

    void updateUrlIcon() {
        if (mUrlInput.hasFocus()) {
            mUrlIcon.setImageResource(R.drawable.ic_search_holo_dark);
        } else {
            if (mInVoiceMode) {
                mUrlIcon.setImageResource(R.drawable.ic_search_holo_dark);
            } else {
                if (mFaviconDrawable == null) {
                    mFaviconDrawable = mBaseUi.getFaviconDrawable(null);
                }
                mUrlIcon.setImageDrawable(mFaviconDrawable);
            }
        }
    }

    @Override
    protected void setFocusState(boolean focus) {
        super.setFocusState(focus);
        if (focus) {
            if (mHideNavButtons) {
                hideNavButtons();
            }
            mSearchButton.setVisibility(View.GONE);
            mStar.setVisibility(View.GONE);
            mClearButton.setVisibility(View.VISIBLE);
            mUrlIcon.setImageResource(R.drawable.ic_search_holo_dark);
            updateSearchMode(false);
        } else {
            if (mHideNavButtons) {
                showNavButtons();
            }
            mGoButton.setVisibility(View.GONE);
            mVoiceSearch.setVisibility(View.GONE);
            showHideStar(mUiController.getCurrentTab());
            mClearButton.setVisibility(View.GONE);
            if (mTitleBar.useQuickControls()) {
                mSearchButton.setVisibility(View.GONE);
            } else {
                mSearchButton.setVisibility(View.VISIBLE);
            }
            updateUrlIcon();
        }
        mUrlContainer.setBackgroundDrawable(focus
                ? mFocusDrawable : mUnfocusDrawable);
    }

    private void stopOrRefresh() {
        if (mUiController == null) return;
        if (mTitleBar.isInLoad()) {
            mUiController.stopLoading();
        } else {
            mUiController.getCurrentTopWebView().reload();
        }
    }

    @Override
    public void onProgressStarted() {
        mStopButton.setImageDrawable(mStopDrawable);
        mStopButton.setContentDescription(mStopDescription);
    }

    @Override
    public void onProgressStopped() {
        mStopButton.setImageDrawable(mReloadDrawable);
        mStopButton.setContentDescription(mRefreshDescription);
    }

    protected void updateSearchMode(boolean userEdited) {
        setSearchMode(!userEdited || TextUtils.isEmpty(mUrlInput.getText()));
    }

    @Override
    protected void setSearchMode(boolean voiceSearchEnabled) {
        boolean showvoicebutton = voiceSearchEnabled &&
                mUiController.supportsVoiceSearch();
        mVoiceSearch.setVisibility(showvoicebutton ? View.VISIBLE :
                View.GONE);
        mGoButton.setVisibility(voiceSearchEnabled ? View.GONE :
                View.VISIBLE);
    }

    @Override
    public void setInVoiceMode(boolean voicemode, List<String> voiceResults) {
        super.setInVoiceMode(voicemode, voiceResults);
        if (voicemode) {
            mUrlIcon.setImageDrawable(mSearchButton.getDrawable());
        }
    }

    private void hideNavButtons() {
        int awidth = mNavButtons.getMeasuredWidth();
        Animator anim1 = ObjectAnimator.ofFloat(mNavButtons, View.TRANSLATION_X, 0, - awidth);
        Animator anim2 = ObjectAnimator.ofInt(mUrlContainer, "left", mUrlContainer.getLeft(),
                mUrlContainer.getPaddingLeft());
        Animator anim3 = ObjectAnimator.ofFloat(mNavButtons, View.ALPHA, 1f, 0f);
        AnimatorSet combo = new AnimatorSet();
        combo.playTogether(anim1, anim2, anim3);
        combo.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mNavButtons.setVisibility(View.GONE);
            }
        });
        combo.setDuration(150);
        combo.start();
    }

    private void showNavButtons() {
        int awidth = mNavButtons.getMeasuredWidth();
        Animator anim1 = ObjectAnimator.ofFloat(mNavButtons, View.TRANSLATION_X, -awidth, 0);
        Animator anim2 = ObjectAnimator.ofInt(mUrlContainer, "left", 0, awidth);
        Animator anim3 = ObjectAnimator.ofFloat(mNavButtons, View.ALPHA, 0f, 1f);
        AnimatorSet combo = new AnimatorSet();
        combo.playTogether(anim1, anim2, anim3);
        mNavButtons.setVisibility(View.VISIBLE);
        combo.setDuration(150);
        combo.start();
    }

    private void showHideStar(Tab tab) {
        // hide the bookmark star for data URLs
        if (tab != null && tab.inForeground()) {
            int starVisibility = View.VISIBLE;
            String url = tab.getUrl();             
            if (DataUri.isDataUri(url)) {
                starVisibility = View.GONE;
            }
            mStar.setVisibility(starVisibility);
        }
    }

}
