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

package com.android.calendar;

import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS;
import static com.android.calendar.CalendarController.EVENT_ATTENDEE_RESPONSE;

import com.android.calendar.CalendarController.EventHandler;
import com.android.calendar.CalendarController.EventInfo;
import com.android.calendar.CalendarController.EventType;
import com.android.calendar.CalendarController.ViewType;
import com.android.calendar.agenda.AgendaFragment;
import com.android.calendar.features.Features;
import com.android.calendar.month.MonthByWeekFragment;
import com.android.calendar.selectcalendars.SelectVisibleCalendarsFragment;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SearchView;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AllInOneActivity extends Activity implements EventHandler,
        OnSharedPreferenceChangeListener, SearchView.OnQueryTextListener, ActionBar.TabListener,
        ActionBar.OnNavigationListener, OnSuggestionListener {
    private static final String TAG = "AllInOneActivity";
    private static final boolean DEBUG = false;
    private static final String EVENT_INFO_FRAGMENT_TAG = "EventInfoFragment";
    private static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    private static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    private static final String BUNDLE_KEY_RESTORE_VIEW = "key_restore_view";
    private static final String BUNDLE_KEY_CHECK_ACCOUNTS = "key_check_for_accounts";
    private static final int HANDLER_KEY = 0;
    private static final long CONTROLS_ANIMATE_DURATION = 400;
    private static int CONTROLS_ANIMATE_WIDTH = 280;
    private static float mScale = 0;

    // Indices of buttons for the drop down menu (tabs replacement)
    // Must match the strings in the array buttons_list in arrays.xml and the
    // OnNavigationListener
    private static final int BUTTON_DAY_INDEX = 0;
    private static final int BUTTON_WEEK_INDEX = 1;
    private static final int BUTTON_MONTH_INDEX = 2;
    private static final int BUTTON_AGENDA_INDEX = 3;

    private CalendarController mController;
    private static boolean mIsMultipane;
    private static boolean mIsTabletConfig;
    private static boolean mShowAgendaWithMonth;
    private static boolean mShowEventDetailsWithAgenda;
    private boolean mOnSaveInstanceStateCalled = false;
    private boolean mBackToPreviousView = false;
    private ContentResolver mContentResolver;
    private int mPreviousView;
    private int mCurrentView;
    private boolean mPaused = true;
    private boolean mUpdateOnResume = false;
    private boolean mHideControls = false;
    private boolean mShowSideViews = true;
    private boolean mShowWeekNum = false;
    private TextView mHomeTime;
    private TextView mDateRange;
    private TextView mWeekTextView;
    private View mMiniMonth;
    private View mCalendarsList;
    private View mMiniMonthContainer;
    private View mSecondaryPane;
    private String mTimeZone;
    private boolean mShowCalendarControls;
    private boolean mShowEventInfoFullScreenAgenda;
    private boolean mShowEventInfoFullScreen;
    private int mWeekNum;

    private long mViewEventId = -1;
    private long mIntentEventStartMillis = -1;
    private long mIntentEventEndMillis = -1;
    private int mIntentAttendeeResponse = CalendarController.ATTENDEE_NO_RESPONSE;

    // Action bar and Navigation bar (left side of Action bar)
    private ActionBar mActionBar;
    private ActionBar.Tab mDayTab;
    private ActionBar.Tab mWeekTab;
    private ActionBar.Tab mMonthTab;
    private ActionBar.Tab mAgendaTab;
    private SearchView mSearchView;
    private MenuItem mSearchMenu;
    private MenuItem mControlsMenu;
    private Menu mOptionsMenu;
    private CalendarViewAdapter mActionBarMenuSpinnerAdapter;
    private QueryHandler mHandler;
    private boolean mCheckForAccounts = true;

    private String mHideString;
    private String mShowString;

    ///M: to mark that it finished one clear all events operation.
    private static boolean mIsClearEventsCompleted = false;
    
    // Params for animating the controls on the right
    private LayoutParams mControlsParams = new LayoutParams(CONTROLS_ANIMATE_WIDTH, 0);

    private AnimatorListener mSlideAnimationDoneListener = new AnimatorListener() {

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationEnd(android.animation.Animator animation) {
            int visibility = mShowSideViews ? View.VISIBLE : View.GONE;
            mMiniMonth.setVisibility(visibility);
            mCalendarsList.setVisibility(visibility);
            mMiniMonthContainer.setVisibility(visibility);
        }

        @Override
        public void onAnimationRepeat(android.animation.Animator animation) {
        }

        @Override
        public void onAnimationStart(android.animation.Animator animation) {
        }
    };

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            mCheckForAccounts = false;
            // If the query didn't return a cursor for some reason return
            if (cursor == null || cursor.getCount() > 0 || isFinishing()) {
                ///M:before return, close the cursor first.@{
                if(cursor!=null) {
                    LogUtil.w(TAG, "AllInOneActivity, close cursor with do nothing.");
                    cursor.close();
                }
                ///@}
                return;
            }
            Bundle options = new Bundle();
            options.putCharSequence("introMessage",
                    getResources().getString(R.string.create_an_account_desc));
            options.putBoolean("allowSkip", true);

            AccountManager am = AccountManager.get(AllInOneActivity.this);
            am.addAccount("com.google", CalendarContract.AUTHORITY, null, options,
                    AllInOneActivity.this,
                    new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> future) {
                            if (future.isCancelled()) {
                                return;
                            }
                            try {
                                Bundle result = future.getResult();
                                boolean setupSkipped = result.getBoolean("setupSkipped");

                                if (setupSkipped) {
                                    Utils.setSharedPreference(AllInOneActivity.this,
                                            GeneralPreferences.KEY_SKIP_SETUP, true);
                                }

                            } catch (OperationCanceledException ignore) {
                                // The account creation process was canceled
                            } catch (IOException ignore) {
                            } catch (AuthenticatorException ignore) {
                            }
                        }
                    }, null);
        }
    }

    private Runnable mHomeTimeUpdater = new Runnable() {
        @Override
        public void run() {
            updateSecondaryTitleFields(-1);
        }
    };

    // Create an observer so that we can update the views whenever a
    // Calendar event changes.
    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            eventsChanged();
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (DEBUG)
            Log.d(TAG, "New intent received " + intent.toString());
        // Don't change the date if we're just returning to the app's home
        if (Intent.ACTION_VIEW.equals(action)
                && !intent.getBooleanExtra(Utils.INTENT_KEY_HOME, false)) {
            long millis = parseViewAction(intent);
            if (millis == -1) {
                millis = Utils.timeFromIntentInMillis(intent);
            }
            if (millis != -1 && mViewEventId == -1 && mController != null) {
                Time time = new Time(mTimeZone);
                time.set(millis);
                time.normalize(true);
                mController.sendEvent(this, EventType.GO_TO, time, time, -1, ViewType.CURRENT);
            }
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        if (Utils.getSharedPreference(this, OtherPreferences.KEY_OTHER_1, false)) {
            setTheme(R.style.CalendarTheme_WithActionBarWallpaper);
        }
        super.onCreate(icicle);

        if (icicle != null && icicle.containsKey(BUNDLE_KEY_CHECK_ACCOUNTS)) {
            mCheckForAccounts = icicle.getBoolean(BUNDLE_KEY_CHECK_ACCOUNTS);
        }
        // Launch add google account if this is first time and there are no
        // accounts yet
        if (mCheckForAccounts
                && !Utils.getSharedPreference(this, GeneralPreferences.KEY_SKIP_SETUP, false)) {

            mHandler = new QueryHandler(this.getContentResolver());
            mHandler.startQuery(0, null, Calendars.CONTENT_URI, new String[] {
                Calendars._ID
            }, null, null /* selection args */, null /* sort order */);
        }

        // This needs to be created before setContentView
        mController = CalendarController.getInstance(this);


        // Get time from intent or icicle
        long timeMillis = -1;
        int viewType = -1;
        final Intent intent = getIntent();
        if (icicle != null) {
            timeMillis = icicle.getLong(BUNDLE_KEY_RESTORE_TIME);
            viewType = icicle.getInt(BUNDLE_KEY_RESTORE_VIEW, -1);
        } else {
            String action = intent.getAction();
            if (Intent.ACTION_VIEW.equals(action)) {
                // Open EventInfo later
                timeMillis = parseViewAction(intent);
            }

            if (timeMillis == -1) {
                timeMillis = Utils.timeFromIntentInMillis(intent);
            }
        }

        if (viewType == -1) {
            viewType = Utils.getViewTypeFromIntentAndSharedPref(this);
        }
        mTimeZone = Utils.getTimeZone(this, mHomeTimeUpdater);
        Time t = new Time(mTimeZone);
        t.set(timeMillis);
        
        ///M:@{
        int julianDay = Time.getJulianDay(t.toMillis(false), t.gmtoff);
        int validJuLianDay = Utils.getValidJuLianDay(t);
        if(julianDay != validJuLianDay){
            t = Utils.changeToJulianDayTime(t, validJuLianDay);
            Utils.toastText(getApplication(),getApplication().getResources().getString(
                    R.string.valid_date_range));
        };
        ///@}
         t.set(t);
        timeMillis = t.toMillis(false);

        if (DEBUG) {
            if (icicle != null && intent != null) {
                Log.d(TAG, "both, icicle:" + icicle.toString() + "  intent:" + intent.toString());
            } else {
                Log.d(TAG, "not both, icicle:" + icicle + " intent:" + intent);
            }
        }

        Resources res = getResources();
        if (mScale == 0) {
            mScale = res.getDisplayMetrics().density;
            CONTROLS_ANIMATE_WIDTH *= mScale;
        }
        mHideString = res.getString(R.string.hide_controls);
        mShowString = res.getString(R.string.show_controls);
        mControlsParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        mIsMultipane = Utils.getConfigBool(this, R.bool.multiple_pane_config);
        mIsTabletConfig = Utils.getConfigBool(this, R.bool.tablet_config);
        mShowAgendaWithMonth = Utils.getConfigBool(this, R.bool.show_agenda_with_month);
        mShowCalendarControls = Utils.getConfigBool(this, R.bool.show_calendar_controls);
        mShowEventDetailsWithAgenda =
            Utils.getConfigBool(this, R.bool.show_event_details_with_agenda);
        mShowEventInfoFullScreenAgenda =
            Utils.getConfigBool(this, R.bool.agenda_show_event_info_full_screen);
        mShowEventInfoFullScreen =
            Utils.getConfigBool(this, R.bool.show_event_info_full_screen);

        Utils.setAllowWeekForDetailView(mIsMultipane);

        // setContentView must be called before configureActionBar
        setContentView(R.layout.all_in_one);

        if (mIsTabletConfig) {
            mDateRange = (TextView) findViewById(R.id.date_bar);
            mWeekTextView = (TextView) findViewById(R.id.week_num);
        } else {
            mDateRange = (TextView) getLayoutInflater().inflate(R.layout.date_range_title, null);
        }

        // configureActionBar auto-selects the first tab you add, so we need to
        // call it before we set up our own fragments to make sure it doesn't
        // overwrite us
        configureActionBar(viewType);

        mHomeTime = (TextView) findViewById(R.id.home_time);
        mMiniMonth = findViewById(R.id.mini_month);
        mCalendarsList = findViewById(R.id.calendar_list);
        mMiniMonthContainer = findViewById(R.id.mini_month_container);
        mSecondaryPane = findViewById(R.id.secondary_pane);

        // Must register as the first activity because this activity can modify
        // the list of event handlers in it's handle method. This affects who
        // the rest of the handlers the controller dispatches to are.
        mController.registerFirstEventHandler(HANDLER_KEY, this);

        initFragments(timeMillis, viewType, icicle);

        // Listen for changes that would require this to be refreshed
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        mContentResolver = getContentResolver();
    }

    private long parseViewAction(final Intent intent) {
        long timeMillis = -1;
        Uri data = intent.getData();
        if (data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("events")) {
                try {
                    mViewEventId = Long.valueOf(data.getLastPathSegment());
                    if (mViewEventId != -1) {
                        mIntentEventStartMillis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, 0);
                        mIntentEventEndMillis = intent.getLongExtra(EXTRA_EVENT_END_TIME, 0);
                        mIntentAttendeeResponse = intent.getIntExtra(
                                ATTENDEE_STATUS, CalendarController.ATTENDEE_NO_RESPONSE);
                        timeMillis = mIntentEventStartMillis;
                    }
                } catch (NumberFormatException e) {
                    // Ignore if mViewEventId can't be parsed
                }
            }
        }
        return timeMillis;
    }

    private void configureActionBar(int viewType) {
        if (mIsTabletConfig) {
            createTabs();
        } else {
            createButtonsSpinner(viewType);
        }
        if (mIsMultipane) {
            mActionBar.setDisplayOptions(
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
        } else {
            mActionBar.setDisplayOptions(0);
        }
    }

    private void createTabs() {
        mActionBar = getActionBar();
        if (mActionBar == null) {
            Log.w(TAG, "ActionBar is null.");
        } else {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mDayTab = mActionBar.newTab();
            mDayTab.setText(getString(R.string.day_view));
            mDayTab.setTabListener(this);
            mActionBar.addTab(mDayTab);
            mWeekTab = mActionBar.newTab();
            mWeekTab.setText(getString(R.string.week_view));
            mWeekTab.setTabListener(this);
            mActionBar.addTab(mWeekTab);
            mMonthTab = mActionBar.newTab();
            mMonthTab.setText(getString(R.string.month_view));
            mMonthTab.setTabListener(this);
            mActionBar.addTab(mMonthTab);
            mAgendaTab = mActionBar.newTab();
            mAgendaTab.setText(getString(R.string.agenda_view));
            mAgendaTab.setTabListener(this);
            mActionBar.addTab(mAgendaTab);
        }
    }

    private void createButtonsSpinner(int viewType) {
        mActionBarMenuSpinnerAdapter = new CalendarViewAdapter (this, viewType);
        mActionBar = getActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mActionBar.setListNavigationCallbacks(mActionBarMenuSpinnerAdapter, this);
        switch (viewType) {
            case ViewType.AGENDA:
                mActionBar.setSelectedNavigationItem(BUTTON_AGENDA_INDEX);
                break;
            case ViewType.DAY:
                mActionBar.setSelectedNavigationItem(BUTTON_DAY_INDEX);
                break;
            case ViewType.WEEK:
                mActionBar.setSelectedNavigationItem(BUTTON_WEEK_INDEX);
                break;
            case ViewType.MONTH:
                mActionBar.setSelectedNavigationItem(BUTTON_MONTH_INDEX);
                break;
            default:
                mActionBar.setSelectedNavigationItem(BUTTON_DAY_INDEX);
                break;
       }
    }
    // Clear buttons used in the agenda view
    private void clearOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }
        MenuItem cancelItem = mOptionsMenu.findItem(R.id.action_cancel);
        if (cancelItem != null) {
            cancelItem.setVisible(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ///M:#clear all events# to update the view. @{
        if(mOnSaveInstanceStateCalled && mController != null && mIsClearEventsCompleted) {
            mIsClearEventsCompleted = false;
            switch(mCurrentView){
            case ViewType.MONTH://send event change event to update month view(261245).
                mController.sendEvent(this, EventType.EVENTS_CHANGED, null, null, -1, mCurrentView);
                LogUtil.v(TAG, "After CLEAR EVENTS COMPLETED, send Event EVENTS_CHANGED.");
                break;
            }
        }
        ///@}

        // Must register as the first activity because this activity can modify
        // the list of event handlers in it's handle method. This affects who
        // the rest of the handlers the controller dispatches to are.
        mController.registerFirstEventHandler(HANDLER_KEY, this);

        mOnSaveInstanceStateCalled = false;
        mContentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI,
                true, mObserver);
        if (mUpdateOnResume) {
            initFragments(mController.getTime(), mController.getViewType(), null);
            mUpdateOnResume = false;
        }
        Time t = new Time(mTimeZone);
        t.set(mController.getTime());
        mController.sendEvent(this, EventType.UPDATE_TITLE, t, t, -1, ViewType.CURRENT,
                mController.getDateFlags(), null, null);
        // Make sure the drop-down menu will get its date updated at midnight
        if (mActionBarMenuSpinnerAdapter != null) {
            mActionBarMenuSpinnerAdapter.refresh(this);
        }

        if (mControlsMenu != null) {
            mControlsMenu.setTitle(mHideControls ? mShowString : mHideString);
        }
        mPaused = false;

        if (mViewEventId != -1 && mIntentEventStartMillis != -1 && mIntentEventEndMillis != -1) {
            long currentMillis = System.currentTimeMillis();
            long selectedTime = -1;
            if (currentMillis > mIntentEventStartMillis && currentMillis < mIntentEventEndMillis) {
                selectedTime = currentMillis;
            }
            mController.sendEventRelatedEventWithExtra(this, EventType.VIEW_EVENT, mViewEventId,
                    mIntentEventStartMillis, mIntentEventEndMillis, -1, -1,
                    mIntentAttendeeResponse, selectedTime);
            mViewEventId = -1;
            mIntentEventStartMillis = -1;
            mIntentEventEndMillis = -1;
        }
        LogUtil.performanceEnd("AllInOneActivity launch");
    }

    @Override
    protected void onPause() {
        super.onPause();

        mController.deregisterEventHandler(HANDLER_KEY);
        mPaused = true;
        mHomeTime.removeCallbacks(mHomeTimeUpdater);
        if (mActionBarMenuSpinnerAdapter != null) {
            mActionBarMenuSpinnerAdapter.onPause();
        }
        mContentResolver.unregisterContentObserver(mObserver);
        if (isFinishing()) {
            // Stop listening for changes that would require this to be refreshed
            SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        // FRAG_TODO save highlighted days of the week;
        if (mController.getViewType() != ViewType.EDIT) {
            Utils.setDefaultView(this, mController.getViewType());
        }
    }

    @Override
    protected void onUserLeaveHint() {
        mController.sendEvent(this, EventType.USER_HOME, null, null, -1, ViewType.CURRENT);
        super.onUserLeaveHint();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mOnSaveInstanceStateCalled = true;
        super.onSaveInstanceState(outState);

        outState.putLong(BUNDLE_KEY_RESTORE_TIME, mController.getTime());
        outState.putInt(BUNDLE_KEY_RESTORE_VIEW, mCurrentView);
        if (mCurrentView == ViewType.EDIT) {
            outState.putLong(BUNDLE_KEY_EVENT_ID, mController.getEventId());
        }
        outState.putBoolean(BUNDLE_KEY_CHECK_ACCOUNTS, mCheckForAccounts);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        mController.deregisterAllEventHandlers();

        CalendarController.removeInstance(this);
    }

    private void initFragments(long timeMillis, int viewType, Bundle icicle) {
        if (DEBUG) {
            Log.d(TAG, "Initializing to " + timeMillis + " for view " + viewType);
        }
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if (mShowCalendarControls) {
            Fragment miniMonthFrag = new MonthByWeekFragment(timeMillis, true);
            ft.replace(R.id.mini_month, miniMonthFrag);
            mController.registerEventHandler(R.id.mini_month, (EventHandler) miniMonthFrag);

            Fragment selectCalendarsFrag = new SelectVisibleCalendarsFragment();
            ft.replace(R.id.calendar_list, selectCalendarsFrag);
            mController.registerEventHandler(
                    R.id.calendar_list, (EventHandler) selectCalendarsFrag);
        }
        if (!mShowCalendarControls || viewType == ViewType.EDIT) {
            mMiniMonth.setVisibility(View.GONE);
            mCalendarsList.setVisibility(View.GONE);
        }

        EventInfo info = null;
        if (viewType == ViewType.EDIT) {
            mPreviousView = GeneralPreferences.getSharedPreferences(this).getInt(
                    GeneralPreferences.KEY_START_VIEW, GeneralPreferences.DEFAULT_START_VIEW);

            long eventId = -1;
            Intent intent = getIntent();
            Uri data = intent.getData();
            if (data != null) {
                try {
                    eventId = Long.parseLong(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    if (DEBUG) {
                        Log.d(TAG, "Create new event");
                    }
                }
            } else if (icicle != null && icicle.containsKey(BUNDLE_KEY_EVENT_ID)) {
                eventId = icicle.getLong(BUNDLE_KEY_EVENT_ID);
            }

            long begin = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
            long end = intent.getLongExtra(EXTRA_EVENT_END_TIME, -1);
            info = new EventInfo();
            if (end != -1) {
                info.endTime = new Time();
                info.endTime.set(end);
            }
            if (begin != -1) {
                info.startTime = new Time();
                info.startTime.set(begin);
            }
            info.id = eventId;
            // We set the viewtype so if the user presses back when they are
            // done editing the controller knows we were in the Edit Event
            // screen. Likewise for eventId
            mController.setViewType(viewType);
            mController.setEventId(eventId);
        } else {
            ///M:If current view is same as the next view.don't change the mPreviousView
            ///so when prefers change,it can handle right  whether back to previous view or 
            ///finish when press back key.@{
            if (mCurrentView != viewType) {
                mPreviousView = viewType;
            }else{
                LogUtil.v(TAG, "don't modify mPreviousView's value.mCurrentView:" + mCurrentView
                        + ",viewType:" + viewType + ",mPreviousView:" + mPreviousView);
            }
            ///@}
           
        }

        setMainPane(ft, R.id.main_pane, viewType, timeMillis, true);
        ft.commit(); // this needs to be after setMainPane()

        Time t = new Time(mTimeZone);
        t.set(timeMillis);
        if (viewType != ViewType.EDIT) {
            mController.sendEvent(this, EventType.GO_TO, t, null, -1, viewType);
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentView == ViewType.EDIT || mBackToPreviousView) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, mPreviousView);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.all_in_one_title_bar, menu);
        ///M:#ClearAllEvent# {
        if(!Features.isClearAllEventEnabled()){
            menu.findItem(R.id.action_delete_all_events).setVisible(false);
        }
        ///@}

        mSearchMenu = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearchMenu.getActionView();
        if (mSearchView != null) {
            Utils.setUpSearchView(mSearchView, this);
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setOnSuggestionListener(this);
        }

        // Hide the "show/hide controls" button if this is a phone
        // or the view type is "Month" or "Agenda".

        mControlsMenu = menu.findItem(R.id.action_hide_controls);
        if (!mShowCalendarControls) {
            if (mControlsMenu != null) {
                mControlsMenu.setVisible(false);
                mControlsMenu.setEnabled(false);
            }
        } else if (mControlsMenu != null && mController != null
                    && (mController.getViewType() == ViewType.MONTH ||
                        mController.getViewType() == ViewType.AGENDA)) {
            mControlsMenu.setVisible(false);
            mControlsMenu.setEnabled(false);
        } else if (mControlsMenu != null){
            mControlsMenu.setTitle(mHideControls ? mShowString : mHideString);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Time t = null;
        int viewType = ViewType.CURRENT;
        long extras = CalendarController.EXTRA_GOTO_TIME;
        switch (item.getItemId()) {
            case R.id.action_refresh:
                mController.refreshCalendars();
                return true;
            case R.id.action_today:
                viewType = ViewType.CURRENT;
                t = new Time(mTimeZone);
                t.setToNow();
                ///M:@{
                int julianDay = Time.getJulianDay(t.toMillis(false), t.gmtoff);
                int validJulianDay = Utils.getValidJuLianDay(t);
                if(julianDay != validJulianDay){
                    t = Utils.changeToJulianDayTime(t, validJulianDay);
                    Utils.toastText(getApplication(),getApplication().getResources().getString(
                            R.string.valid_date_range));
                };
                ///@}
                extras |= CalendarController.EXTRA_GOTO_TODAY;
                break;
            case R.id.action_create_event:
                t = new Time();
                ///M: modify for month view, if user want to create event from month view, just set now to start time.@{
                int viewtype = mController.getViewType();
                if(viewtype == ViewType.MONTH) {
                    t.setToNow();
                } else {
                    t.set(mController.getTime());
                }
                ///@}
                if (t.minute > 30) {
                    t.hour++;
                    t.minute = 0;
                } else if (t.minute > 0 && t.minute < 30) {
                    t.minute = 30;
                }
                mController.sendEventRelatedEvent(
                        this, EventType.CREATE_EVENT, -1, t.toMillis(true), 0, 0, 0, -1);
                return true;
            case R.id.action_select_visible_calendars:
                mController.sendEvent(this, EventType.LAUNCH_SELECT_VISIBLE_CALENDARS, null, null,
                        0, 0);
                return true;
            case R.id.action_settings:
                mController.sendEvent(this, EventType.LAUNCH_SETTINGS, null, null, 0, 0);
                return true;
            case R.id.action_hide_controls:
                mHideControls = !mHideControls;
                item.setTitle(mHideControls ? mShowString : mHideString);
                final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this, "controlsOffset",
                        mHideControls ? 0 : CONTROLS_ANIMATE_WIDTH,
                        mHideControls ? CONTROLS_ANIMATE_WIDTH : 0);
                slideAnimation.setDuration(CONTROLS_ANIMATE_DURATION);
                ObjectAnimator.setFrameDelay(0);
                slideAnimation.start();
                return true;
            case R.id.action_search:
                mOptionsMenu.close();
                return false;
            /// TODO: temp checkin
            ///M:#ClearAllEvents#
            case R.id.action_delete_all_events:
                Log.w(TAG, "delete all events.");
                //Selection must be specified for content://com.android.calendar/events
                mController.sendEvent(this, EventType.LAUNCH_SELECT_CLEARABLE_CALENDARS, null, null,
                        0, 0);
                return true;
            default:
                return false;
        }
        mController.sendEvent(this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
        return true;
    }

    /**
     * Sets the offset of the controls on the right for animating them off/on
     * screen. ProGuard strips this if it's not in proguard.flags
     *
     * @param controlsOffset The current offset in pixels
     */
    public void setControlsOffset(int controlsOffset) {
        mMiniMonth.setTranslationX(controlsOffset);
        mCalendarsList.setTranslationX(controlsOffset);
        mControlsParams.width = Math.max(0, CONTROLS_ANIMATE_WIDTH - controlsOffset);
        mMiniMonthContainer.setLayoutParams(mControlsParams);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(GeneralPreferences.KEY_WEEK_START_DAY)) {
            if (mPaused) {
                mUpdateOnResume = true;
            } else {
                initFragments(mController.getTime(), mController.getViewType(), null);
            }
        }
    }

    private void setMainPane(
            FragmentTransaction ft, int viewId, int viewType, long timeMillis, boolean force) {
        
        LogUtil.v(TAG, "setMainPane, viewId = " + viewId + ", viewType = " + viewType + ", time = " + timeMillis
                + "force = " + force);
        if (mOnSaveInstanceStateCalled) {
            return;
        }
        if (!force && mCurrentView == viewType) {
            return;
        }

        // Remove this when transition to and from month view looks fine.
        boolean doTransition = viewType != ViewType.MONTH && mCurrentView != ViewType.MONTH;
        FragmentManager fragmentManager = getFragmentManager();
        // Check if our previous view was an Agenda view
        // TODO remove this if framework ever supports nested fragments
        if (mCurrentView == ViewType.AGENDA) {
            // If it was, we need to do some cleanup on it to prevent the
            // edit/delete buttons from coming back on a rotation.
            Fragment oldFrag = fragmentManager.findFragmentById(viewId);
            if (oldFrag instanceof AgendaFragment) {
                ((AgendaFragment) oldFrag).removeFragments(fragmentManager);
            }
        }

        if (viewType != mCurrentView) {
            // The rules for this previous view are different than the
            // controller's and are used for intercepting the back button.
            if (mCurrentView != ViewType.EDIT && mCurrentView > 0) {
                mPreviousView = mCurrentView;
            }
            mCurrentView = viewType;
        }
        // Create new fragment
        Fragment frag = null;
        Fragment secFrag = null;
        switch (viewType) {
            case ViewType.AGENDA:
                if (mActionBar != null && (mActionBar.getSelectedTab() != mAgendaTab)) {
                    mActionBar.selectTab(mAgendaTab);
                }
                if (mActionBarMenuSpinnerAdapter != null) {
                    mActionBar.setSelectedNavigationItem(CalendarViewAdapter.AGENDA_BUTTON_INDEX);
                }
                frag = new AgendaFragment(timeMillis, false);
                break;
            case ViewType.DAY:
                if (mActionBar != null && (mActionBar.getSelectedTab() != mDayTab)) {
                    mActionBar.selectTab(mDayTab);
                }
                if (mActionBarMenuSpinnerAdapter != null) {
                    mActionBar.setSelectedNavigationItem(CalendarViewAdapter.DAY_BUTTON_INDEX);
                }
                frag = new DayFragment(timeMillis, 1);
                break;
            case ViewType.WEEK:
                if (mActionBar != null && (mActionBar.getSelectedTab() != mWeekTab)) {
                    mActionBar.selectTab(mWeekTab);
                }
                if (mActionBarMenuSpinnerAdapter != null) {
                    mActionBar.setSelectedNavigationItem(CalendarViewAdapter.WEEK_BUTTON_INDEX);
                }
                frag = new DayFragment(timeMillis, 7);
                break;
            case ViewType.MONTH:
                if (mActionBar != null && (mActionBar.getSelectedTab() != mMonthTab)) {
                    mActionBar.selectTab(mMonthTab);
                }
                if (mActionBarMenuSpinnerAdapter != null) {
                    mActionBar.setSelectedNavigationItem(CalendarViewAdapter.MONTH_BUTTON_INDEX);
                }
                frag = new MonthByWeekFragment(timeMillis, false);
                if (mShowAgendaWithMonth) {
                    secFrag = new AgendaFragment(timeMillis, false);
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "Must be Agenda, Day, Week, or Month ViewType, not " + viewType);
        }

        // Update the current view so that the menu can update its look according to the
        // current view.
        if (!mIsTabletConfig && mActionBarMenuSpinnerAdapter != null) {
            mActionBarMenuSpinnerAdapter.setTime(timeMillis);
            mActionBarMenuSpinnerAdapter.setMainView(viewType);
        }


        // Show date only on tablet configurations in views different than Agenda
        if (!mIsTabletConfig) {
            mDateRange.setVisibility(View.GONE);
        } else if (viewType != ViewType.AGENDA) {
            mDateRange.setVisibility(View.VISIBLE);
        } else {
            mDateRange.setVisibility(View.GONE);
        }

        // Clear unnecessary buttons from the option menu when switching from the agenda view
        if (viewType != ViewType.AGENDA) {
            clearOptionsMenu();
        }

        boolean doCommit = false;
        if (ft == null) {
            doCommit = true;
            ft = fragmentManager.beginTransaction();
        }

        if (doTransition) {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }

        ft.replace(viewId, frag);
        if (mShowAgendaWithMonth) {

            // Show/hide secondary fragment

            if (secFrag != null) {
                ft.replace(R.id.secondary_pane, secFrag);
                mSecondaryPane.setVisibility(View.VISIBLE);
            } else {
                mSecondaryPane.setVisibility(View.GONE);
                Fragment f = fragmentManager.findFragmentById(R.id.secondary_pane);
                if (f != null) {
                    ft.remove(f);
                }
                mController.deregisterEventHandler(R.id.secondary_pane);
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Adding handler with viewId " + viewId + " and type " + viewType);
        }
        // If the key is already registered this will replace it
        mController.registerEventHandler(viewId, (EventHandler) frag);
        if (secFrag != null) {
            mController.registerEventHandler(viewId, (EventHandler) secFrag);
        }

        if (doCommit) {
            if (DEBUG) {
                Log.d(TAG, "setMainPane AllInOne=" + this + " finishing:" + this.isFinishing());
            }
            ft.commit();
        }
    }

    private void setTitleInActionBar(EventInfo event) {
        if (event.eventType != EventType.UPDATE_TITLE || mActionBar == null) {
            return;
        }

        final long start = event.startTime.toMillis(false /* use isDst */);
        final long end;
        if (event.endTime != null) {
            end = event.endTime.toMillis(false /* use isDst */);
        } else {
            end = start;
        }

        final String msg = Utils.formatDateRange(this, start, end, (int) event.extraLong);
        CharSequence oldDate = mDateRange.getText();
        mDateRange.setText(msg);
        updateSecondaryTitleFields(event.selectedTime != null ? event.selectedTime.toMillis(true)
                : start);
        if (!TextUtils.equals(oldDate, msg)) {
            mDateRange.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            if (mShowWeekNum && mWeekTextView != null) {
                mWeekTextView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        }
    }

    private void updateSecondaryTitleFields(long visibleMillisSinceEpoch) {
        mShowWeekNum = Utils.getShowWeekNumber(this);
        mTimeZone = Utils.getTimeZone(this, mHomeTimeUpdater);
        if (visibleMillisSinceEpoch != -1) {
            int weekNum = Utils.getWeekNumberFromTime(visibleMillisSinceEpoch, this);
            mWeekNum = weekNum;
        }

        if (mShowWeekNum && (mCurrentView == ViewType.WEEK) && mIsTabletConfig
                && mWeekTextView != null) {
            String weekString = getResources().getQuantityString(R.plurals.weekN, mWeekNum,
                    mWeekNum);
            mWeekTextView.setText(weekString);
            mWeekTextView.setVisibility(View.VISIBLE);
        } else if (visibleMillisSinceEpoch != -1 && mWeekTextView != null
                && mCurrentView == ViewType.DAY && mIsTabletConfig) {
            Time time = new Time(mTimeZone);
            time.set(visibleMillisSinceEpoch);
            int julianDay = Time.getJulianDay(visibleMillisSinceEpoch, time.gmtoff);
            time.setToNow();
            int todayJulianDay = Time.getJulianDay(time.toMillis(false), time.gmtoff);
            String dayString = Utils.getDayOfWeekString(julianDay, todayJulianDay,
                    visibleMillisSinceEpoch, this);
            mWeekTextView.setText(dayString);
            mWeekTextView.setVisibility(View.VISIBLE);
        } else if (mWeekTextView != null && (!mIsTabletConfig || mCurrentView != ViewType.DAY)) {
            mWeekTextView.setVisibility(View.GONE);
        }

        if (mHomeTime != null
                && (mCurrentView == ViewType.DAY || mCurrentView == ViewType.WEEK
                        || mCurrentView == ViewType.AGENDA)
                && !TextUtils.equals(mTimeZone, Time.getCurrentTimezone())) {
            Time time = new Time(mTimeZone);
            time.setToNow();
            long millis = time.toMillis(true);
            boolean isDST = time.isDst != 0;
            int flags = DateUtils.FORMAT_SHOW_TIME;
            if (DateFormat.is24HourFormat(this)) {
                flags |= DateUtils.FORMAT_24HOUR;
            }
            // Formats the time as
            String timeString = (new StringBuilder(
                    Utils.formatDateRange(this, millis, millis, flags))).append(" ").append(
                    TimeZone.getTimeZone(mTimeZone).getDisplayName(
                            isDST, TimeZone.LONG, Locale.getDefault())).toString();
            mHomeTime.setText(timeString);
            mHomeTime.setVisibility(View.VISIBLE);
            // Update when the minute changes
            mHomeTime.removeCallbacks(mHomeTimeUpdater);
            mHomeTime.postDelayed(
                    mHomeTimeUpdater,
                    DateUtils.MINUTE_IN_MILLIS - (millis % DateUtils.MINUTE_IN_MILLIS));
        } else if (mHomeTime != null) {
            mHomeTime.setVisibility(View.GONE);
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.GO_TO | EventType.VIEW_EVENT | EventType.UPDATE_TITLE;
    }

    @Override
    public void handleEvent(EventInfo event) {
        long displayTime = -1;
        if (event.eventType == EventType.GO_TO) {
            if ((event.extraLong & CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS) != 0) {
                mBackToPreviousView = true;
            } else if (event.viewType != mController.getPreviousViewType()
                    && event.viewType != ViewType.EDIT) {
                // Clear the flag is change to a different view type
                mBackToPreviousView = false;
            }

            setMainPane(
                    null, R.id.main_pane, event.viewType, event.startTime.toMillis(false), false);
            if (mSearchView != null) {
                mSearchView.clearFocus();
            }

            if (mShowCalendarControls) {
                if (event.viewType == ViewType.MONTH || event.viewType == ViewType.AGENDA) {
                    // hide minimonth and calendar frag
                    mShowSideViews = false;
                    if (mControlsMenu != null) {
                        mControlsMenu.setVisible(false);
                        mControlsMenu.setEnabled(false);

                        if (!mHideControls) {
                            final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this,
                                    "controlsOffset", 0, CONTROLS_ANIMATE_WIDTH);
                            slideAnimation.addListener(mSlideAnimationDoneListener);
                            slideAnimation.setDuration(220);
                            ObjectAnimator.setFrameDelay(0);
                            slideAnimation.start();
                        }
                    } else {
                        mMiniMonth.setVisibility(View.GONE);
                        mCalendarsList.setVisibility(View.GONE);
                        mMiniMonthContainer.setVisibility(View.GONE);
                    }
                } else {
                    // show minimonth and calendar frag
                    mShowSideViews = true;
                    mMiniMonth.setVisibility(View.VISIBLE);
                    mCalendarsList.setVisibility(View.VISIBLE);
                    mMiniMonthContainer.setVisibility(View.VISIBLE);
                    if (mControlsMenu != null) {
                        mControlsMenu.setVisible(true);
                        mControlsMenu.setEnabled(true);
                        if (!mHideControls &&
                                (mController.getPreviousViewType() == ViewType.MONTH ||
                                 mController.getPreviousViewType() == ViewType.AGENDA)) {
                            final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this,
                                    "controlsOffset", CONTROLS_ANIMATE_WIDTH, 0);
                            slideAnimation.setDuration(220);
                            ObjectAnimator.setFrameDelay(0);
                            slideAnimation.start();
                        }
                    }
                }
            }
            displayTime = event.selectedTime != null ? event.selectedTime.toMillis(true)
                    : event.startTime.toMillis(true);
            if (!mIsTabletConfig) {
                mActionBarMenuSpinnerAdapter.setTime(displayTime);
            }
        } else if (event.eventType == EventType.VIEW_EVENT) {

            // If in Agenda view and "show_event_details_with_agenda" is "true",
            // do not create the event info fragment here, it will be created by the Agenda
            // fragment

            if (mCurrentView == ViewType.AGENDA && mShowEventDetailsWithAgenda) {
                if (event.startTime != null && event.endTime != null) {
                    mController.sendEvent(this, EventType.GO_TO, event.startTime, event.endTime,
                            event.id, ViewType.AGENDA);
                } else if (event.selectedTime != null) {
                    mController.sendEvent(this, EventType.GO_TO, event.selectedTime,
                        event.selectedTime, event.id, ViewType.AGENDA);
                }
            } else {
                // TODO Fix the temp hack below: && mCurrentView !=
                // ViewType.AGENDA
                if (event.selectedTime != null && mCurrentView != ViewType.AGENDA) {
                    mController.sendEvent(this, EventType.GO_TO, event.selectedTime,
                            event.selectedTime, -1, ViewType.CURRENT);
                }
                if ((mCurrentView == ViewType.AGENDA && mShowEventInfoFullScreenAgenda) ||
                        ((mCurrentView == ViewType.DAY || (mCurrentView == ViewType.WEEK) ||
                                mCurrentView == ViewType.MONTH) && mShowEventInfoFullScreen)){
                    // start event info as activity
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, event.id);
                    intent.setData(eventUri);
                    intent.setClass(this, EventInfoActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(EXTRA_EVENT_BEGIN_TIME, event.startTime.toMillis(false));
                    intent.putExtra(EXTRA_EVENT_END_TIME, event.endTime.toMillis(false));
                    intent.putExtra(EVENT_ATTENDEE_RESPONSE, (int)event.extraLong);
                    startActivity(intent);
                } else {
                    // start event info as a dialog
                    EventInfoFragment fragment = new EventInfoFragment(this,
                            event.id, event.startTime.toMillis(false),
                            event.endTime.toMillis(false), (int) event.extraLong, true,
                            EventInfoFragment.DIALOG_WINDOW_STYLE);
                    fragment.setDialogParams(event.x, event.y, mActionBar.getHeight());
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    // if we have an old popup replace it
                    Fragment fOld = fm.findFragmentByTag(EVENT_INFO_FRAGMENT_TAG);
                    if (fOld != null && fOld.isAdded()) {
                        ft.remove(fOld);
                    }
                        ft.add(fragment, EVENT_INFO_FRAGMENT_TAG);
                    ft.commit();
                }
            }
            displayTime = event.startTime.toMillis(true);
        } else if (event.eventType == EventType.UPDATE_TITLE) {
            setTitleInActionBar(event);
            if (!mIsTabletConfig) {
                mActionBarMenuSpinnerAdapter.setTime(mController.getTime());
            }
        }
        updateSecondaryTitleFields(displayTime);
    }

    // Needs to be in proguard whitelist
    // Specified as listener via android:onClick in a layout xml
    public void handleSelectSyncedCalendarsClicked(View v) {
        mController.sendEvent(this, EventType.LAUNCH_SETTINGS, null, null, null, 0, 0,
                CalendarController.EXTRA_GOTO_TIME, null,
                null);
    }

    @Override
    public void eventsChanged() {
        mController.sendEvent(this, EventType.EVENTS_CHANGED, null, null, -1, ViewType.CURRENT);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if ("TARDIS".equalsIgnoreCase(query)) {
            Utils.tardis();
        }
        mSearchMenu.collapseActionView();
        mController.sendEvent(this, EventType.SEARCH, null, null, -1, ViewType.CURRENT, 0, query,
                getComponentName());
        return true;
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        Log.w(TAG, "TabSelected AllInOne=" + this + " finishing:" + this.isFinishing());
        if (tab == mDayTab && mCurrentView != ViewType.DAY) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY);
        } else if (tab == mWeekTab && mCurrentView != ViewType.WEEK) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.WEEK);
        } else if (tab == mMonthTab && mCurrentView != ViewType.MONTH) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
        } else if (tab == mAgendaTab && mCurrentView != ViewType.AGENDA) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.AGENDA);
        } else {
            Log.w(TAG, "TabSelected event from unknown tab: "
                    + (tab == null ? "null" : tab.getText()));
            Log.w(TAG, "CurrentView:" + mCurrentView + " Tab:" + tab.toString() + " Day:" + mDayTab
                    + " Week:" + mWeekTab + " Month:" + mMonthTab + " Agenda:" + mAgendaTab);
        }
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }


    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        switch (itemPosition) {
            case CalendarViewAdapter.DAY_BUTTON_INDEX:
                if (mCurrentView != ViewType.DAY) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY);
                }
                break;
            case CalendarViewAdapter.WEEK_BUTTON_INDEX:
                if (mCurrentView != ViewType.WEEK) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.WEEK);
                }
                break;
            case CalendarViewAdapter.MONTH_BUTTON_INDEX:
                if (mCurrentView != ViewType.MONTH) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
                }
                break;
            case CalendarViewAdapter.AGENDA_BUTTON_INDEX:
                if (mCurrentView != ViewType.AGENDA) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.AGENDA);
                }
                break;
            default:
                Log.w(TAG, "ItemSelected event from unknown button: " + itemPosition);
                Log.w(TAG, "CurrentView:" + mCurrentView + " Button:" + itemPosition +
                        " Day:" + mDayTab + " Week:" + mWeekTab + " Month:" + mMonthTab +
                        " Agenda:" + mAgendaTab);
                break;
        }
        return false;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        mSearchMenu.collapseActionView();
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        if (mSearchMenu != null) {
            mSearchMenu.expandActionView();
        }
        return false;
    }

///M: override onCongigurationChanged will do harm to tablet, so don't do that.
/// @{
//    ///M: prevent orientation change destroy the Activity. @{
//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        LogUtil.d(TAG, "onConfChanged: " + newConfig);
//        int viewType = mController.getViewType();
//        recreateOptionMenu();
//
//        long timeMillis = mController.getTime();
//        if (viewType != ViewType.EDIT) {
//            if (viewType != ViewType.AGENDA) {
//                LogUtil.d(TAG, "Conf changed: view type is month, force setMainPane.");
//                // Force set main pane for Month view, in order to re-draw the events
//                setMainPane(null, R.id.main_pane, viewType, timeMillis, true);
//                ///M:trancesfer screen ,clear the Month event list items
//        		if (viewType == ViewType.MONTH && mOnSaveInstanceStateCalled) {
//        			MonthByWeekFragment fg = (MonthByWeekFragment) getFragmentManager()
//        					.findFragmentById(R.id.main_pane);
//        			fg.resetAdapter();
//        		}
//                ///@}
//            } else {
//                LogUtil.d(TAG, "Conf changed: view type is: " + viewType);
//                //reset the time.(because the time in action bar is not right.)
//                Time time = new Time(mTimeZone);
//                time.set(timeMillis);
//                mController.sendEvent(this, EventType.GO_TO, time, null, -1, viewType);
//            }
//        } else {
//                LogUtil.d(TAG, "view type = Edit");
//        }
//    }
//    ///@}
//    
//    ///M: recreate option menu. for Calendar has different menu in land and port layout @{
//    /**
//     * clear the option menu, and recreate it. for configure changed.
//     * this function will do things almost like onCreateOptionMenu()
//     */
//    private void recreateOptionMenu() {
//        
//        if (mOptionsMenu == null) {
//            LogUtil.d(TAG, "abort recreating option menu, it's null");
//            return;
//        }
//
//        mOptionsMenu.clear();
//        getMenuInflater().inflate(R.menu.all_in_one_title_bar, mOptionsMenu);
//        ///M:#ClearAllEvent# {
//        if(!Features.isClearAllEventEnabled()){
//            mOptionsMenu.findItem(R.id.action_delete_all_events).setVisible(false);
//        }
//        ///@}
//
//        mSearchMenu = mOptionsMenu.findItem(R.id.action_search);
//        mSearchView = (SearchView) mSearchMenu.getActionView();
//        if (mSearchView != null) {
//            Utils.setUpSearchView(mSearchView, this);
//            mSearchView.setOnQueryTextListener(this);
//            mSearchView.setOnSuggestionListener(this);
//        }
//
//        // Hide the "show/hide controls" button if this is a phone
//        // or the view type is "Month" or "Agenda".
//
//        mControlsMenu = mOptionsMenu.findItem(R.id.action_hide_controls);
//        if (!mShowCalendarControls) {
//            if (mControlsMenu != null) {
//                mControlsMenu.setVisible(false);
//                mControlsMenu.setEnabled(false);
//            }
//        } else if (mControlsMenu != null && mController != null
//                    && (mController.getViewType() == ViewType.MONTH ||
//                        mController.getViewType() == ViewType.AGENDA)) {
//            mControlsMenu.setVisible(false);
//            mControlsMenu.setEnabled(false);
//        } else if (mControlsMenu != null){
//            mControlsMenu.setTitle(mHideControls ? mShowString : mHideString);
//        }
//    }
//    ///@}
/// @}
    
    ///M:#clear all events#.@{
    public static void setClearEventsCompletedStatus(boolean status) {
        mIsClearEventsCompleted = status;
    }
    ///@}
    
    ///M:if the user press menu key,close the SearchView.@{
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mSearchMenu != null && mSearchMenu.isActionViewExpanded()) {
            mSearchMenu.collapseActionView();
        }
        return super.onPrepareOptionsMenu(menu);
    }
    ///@}
    
}
