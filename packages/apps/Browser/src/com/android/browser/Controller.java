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

package com.android.browser;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.content.ClipboardManager;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.net.http.SslError;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceActivity;
import android.provider.Browser;
import android.provider.BrowserContract;
import android.provider.BrowserContract.Images;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents.Insert;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.android.browser.IntentHandler.UrlData;
import com.android.browser.UI.ComboViews;
import com.android.browser.provider.BrowserProvider;
import com.android.browser.provider.BrowserProvider2;
import com.android.browser.provider.BrowserProvider2.Thumbnails;
import com.android.browser.provider.SnapshotProvider.Snapshots;
import com.android.browser.search.SearchEngine;
import com.android.browser.sitenavigation.SiteNavigation;
import com.android.common.Search;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//M: import related package
import android.content.DialogInterface;
import android.app.AlertDialog;
import com.mediatek.xlog.Xlog;

import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.app.ActivityManagerNative;
import java.util.List;
import com.android.internal.telephony.TelephonyIntents;
import android.telephony.TelephonyManager;
import android.os.SystemProperties;
import android.content.SharedPreferences;
import java.io.FileNotFoundException;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.net.ParseException;
import android.net.WebAddress;

import com.android.browser.sitenavigation.SiteNavigationAddDialog;

import java.io.IOException;
/**
 * Controller for browser
 */
public class Controller
        implements WebViewController, UiController {

    private static final String LOGTAG = "Controller";
    private static final String XLOGTAG = "browser/Controller";
    private static final String SEND_APP_ID_EXTRA =
        "android.speech.extras.SEND_APPLICATION_ID_EXTRA";
    private static final String INCOGNITO_URI = "browser:incognito";


    // public message ids
    public final static int LOAD_URL = 1001;
    public final static int STOP_LOAD = 1002;

    // Message Ids
    private static final int FOCUS_NODE_HREF = 102;
    private static final int RELEASE_WAKELOCK = 107;

    static final int UPDATE_BOOKMARK_THUMBNAIL = 108;

    private static final int OPEN_BOOKMARKS = 201;

    private static final int EMPTY_MENU = -1;

    // activity requestCode
    final static int COMBO_VIEW = 1;
    final static int PREFERENCES_PAGE = 3;
    final static int FILE_SELECTED = 4;
    final static int AUTOFILL_SETUP = 5;

    private final static int WAKELOCK_TIMEOUT = 5 * 60 * 1000; // 5 minutes

    // As the ids are dynamically created, we can't guarantee that they will
    // be in sequence, so this static array maps ids to a window number.
    final static private int[] WINDOW_SHORTCUT_ID_ARRAY =
    { R.id.window_one_menu_id, R.id.window_two_menu_id,
      R.id.window_three_menu_id, R.id.window_four_menu_id,
      R.id.window_five_menu_id, R.id.window_six_menu_id,
      R.id.window_seven_menu_id, R.id.window_eight_menu_id };

    // "source" parameter for Google search through search key
    final static String GOOGLE_SEARCH_SOURCE_SEARCHKEY = "browser-key";
    // "source" parameter for Google search through simplily type
    final static String GOOGLE_SEARCH_SOURCE_TYPE = "browser-type";

    // "no-crash-recovery" parameter in intent to suppress crash recovery
    final static String NO_CRASH_RECOVERY = "no-crash-recovery";

    // A bitmap that is re-used in createScreenshot as scratch space
    private static Bitmap sThumbnailBitmap;

    private Activity mActivity;
    private UI mUi;
    private TabControl mTabControl;
    private BrowserSettings mSettings;
    private WebViewFactory mFactory;

    private WakeLock mWakeLock;

    private UrlHandler mUrlHandler;
    private UploadHandler mUploadHandler;
    private IntentHandler mIntentHandler;
    private PageDialogsHandler mPageDialogsHandler;
    private NetworkStateHandler mNetworkHandler;

    private Message mAutoFillSetupMessage;

    private boolean mShouldShowErrorConsole;

    private SystemAllowGeolocationOrigins mSystemAllowGeolocationOrigins;

    // FIXME, temp address onPrepareMenu performance problem.
    // When we move everything out of view, we should rewrite this.
    private int mCurrentMenuState = 0;
    private int mMenuState = R.id.MAIN_MENU;
    private int mOldMenuState = EMPTY_MENU;
    private Menu mCachedMenu;

    private boolean mMenuIsDown;

    // For select and find, we keep track of the ActionMode so that
    // finish() can be called as desired.
    private ActionMode mActionMode;

    /**
     * Only meaningful when mOptionsMenuOpen is true.  This variable keeps track
     * of whether the configuration has changed.  The first onMenuOpened call
     * after a configuration change is simply a reopening of the same menu
     * (i.e. mIconView did not change).
     */
    private boolean mConfigChanged;

    /**
     * Keeps track of whether the options menu is open. This is important in
     * determining whether to show or hide the title bar overlay
     */
    private boolean mOptionsMenuOpen;

    /**
     * Whether or not the options menu is in its bigger, popup menu form. When
     * true, we want the title bar overlay to be gone. When false, we do not.
     * Only meaningful if mOptionsMenuOpen is true.
     */
    private boolean mExtendedMenuOpen;

    private boolean mActivityPaused = true;
    private boolean mLoadStopped;

    private Handler mHandler;
    // Checks to see when the bookmarks database has changed, and updates the
    // Tabs' notion of whether they represent bookmarked sites.
    private ContentObserver mBookmarksObserver;
    private CrashRecoveryHandler mCrashRecoveryHandler;

    private boolean mBlockEvents;

    private SmsHandler mSmsHandler;

    private boolean mNeedUpdateThumbnail;
    private String mUrlNeedUpdateThumbnail;
    public Controller(Activity browser, boolean preloadCrashState) {
        mActivity = browser;
        mSettings = BrowserSettings.getInstance();
        mTabControl = new TabControl(this);
        mSettings.setController(this);
        mCrashRecoveryHandler = CrashRecoveryHandler.initialize(this);
        if (preloadCrashState) {
            mCrashRecoveryHandler.preloadCrashState();
        }
        mFactory = new BrowserWebViewFactory(browser);

        mUrlHandler = new UrlHandler(this);
        mIntentHandler = new IntentHandler(mActivity, this);
        mPageDialogsHandler = new PageDialogsHandler(mActivity, this);

        startHandler();
        mBookmarksObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                int size = mTabControl.getTabCount();
                for (int i = 0; i < size; i++) {
                    mTabControl.getTab(i).updateBookmarkedStatus();
                }
            }

        };
        browser.getContentResolver().registerContentObserver(
                BrowserContract.Bookmarks.CONTENT_URI, true, mBookmarksObserver);

        mNetworkHandler = new NetworkStateHandler(mActivity, this);
        // Start watching the default geolocation permissions
        mSystemAllowGeolocationOrigins =
                new SystemAllowGeolocationOrigins(mActivity.getApplicationContext());
        mSystemAllowGeolocationOrigins.start();

        openIconDatabase();

        mSmsHandler = null;
        // MTK_OP02_PROTECT_START
        String optr = SystemProperties.get("ro.operator.optr");
        if (optr != null && optr.equals("OP02")) {

            mSmsHandler = new SmsHandler(mActivity);            
        }
        // MTK_OP02_PROTECT_END
        
    }

    void start(final Bundle icicle, final Intent intent) {
        boolean noCrashRecovery = intent.getBooleanExtra(NO_CRASH_RECOVERY, false);
        if (icicle != null || noCrashRecovery) {
            doStart(icicle, intent, false);
        } else {
            mCrashRecoveryHandler.startRecovery(intent);
        }
    }

    void doStart(final Bundle icicle, final Intent intent, final boolean fromCrash) {
        // Unless the last browser usage was within 24 hours, destroy any
        // remaining incognito tabs.

        Calendar lastActiveDate = icicle != null ?
                (Calendar) icicle.getSerializable("lastActiveDate") : null;
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        final boolean restoreIncognitoTabs = !(lastActiveDate == null
            || lastActiveDate.before(yesterday)
            || lastActiveDate.after(today));

        // Find out if we will restore any state and remember the tab.
        final long currentTabId =
                mTabControl.canRestoreState(icicle, restoreIncognitoTabs);

        if (currentTabId == -1) {
            // Not able to restore so we go ahead and clear session cookies.  We
            // must do this before trying to login the user as we don't want to
            // clear any session cookies set during login.
            CookieManager.getInstance().removeSessionCookie();
        }

        GoogleAccountLogin.startLoginIfNeeded(mActivity,
                new Runnable() {
                    @Override public void run() {
                        onPreloginFinished(icicle, intent, currentTabId, restoreIncognitoTabs,
                                fromCrash);
                    }
                });
    }

    private void onPreloginFinished(Bundle icicle, Intent intent, long currentTabId,
            boolean restoreIncognitoTabs, boolean fromCrash) {
        if (currentTabId == -1) {
            BackgroundHandler.execute(new PruneThumbnails(mActivity, null));
            final Bundle extra = intent.getExtras();
            // Create an initial tab.
            // If the intent is ACTION_VIEW and data is not null, the Browser is
            // invoked to view the content by another application. In this case,
            // the tab will be close when exit.
            UrlData urlData = null;
            if (intent.getData() != null 
            	&& Intent.ACTION_VIEW.equals(intent.getAction())
            	&& intent.getData().toString().startsWith("content://")) {
            	urlData = new UrlData(intent.getData().toString());
            } else {
                urlData = IntentHandler.getUrlDataFromIntent(intent);
            }
            Tab t = null;
            if (urlData.isEmpty()) {
                t = openTabToHomePage();
            } else {
                t = openTab(urlData);
            }
            if (t != null) {
                t.setAppId(intent.getStringExtra(Browser.EXTRA_APPLICATION_ID));
            }
            WebView webView = t.getWebView();
            if (extra != null) {
                int scale = extra.getInt(Browser.INITIAL_ZOOM_LEVEL, 0);
                if (scale > 0 && scale <= 1000) {
                    webView.setInitialScale(scale);
                }
            }
            mUi.updateTabs(mTabControl.getTabs());
        } else {
            mTabControl.restoreState(icicle, currentTabId, restoreIncognitoTabs,
                    mUi.needsRestoreAllTabs());
            List<Tab> tabs = mTabControl.getTabs();
            ArrayList<Long> restoredTabs = new ArrayList<Long>(tabs.size());
            for (Tab t : tabs) {
                restoredTabs.add(t.getId());
            }
            BackgroundHandler.execute(new PruneThumbnails(mActivity, restoredTabs));
            if (tabs.size() == 0) {
                openTabToHomePage();
            }
            mUi.updateTabs(tabs);
            // TabControl.restoreState() will create a new tab even if
            // restoring the state fails.
            setActiveTab(mTabControl.getCurrentTab());
            // Handle the intent if needed. If icicle != null, we are restoring
            // and the intent will be stale - ignore it.
            if (icicle == null || fromCrash) {
                mIntentHandler.onNewIntent(intent);
            }
        }
        // Read JavaScript flags if it exists.
        String jsFlags = getSettings().getJsEngineFlags();
        if (jsFlags.trim().length() != 0) {
            getCurrentWebView().setJsFlags(jsFlags);
        }
        if (BrowserActivity.ACTION_SHOW_BOOKMARKS.equals(intent.getAction())) {
            bookmarksOrHistoryPicker(ComboViews.Bookmarks);
        }
    }

    private static class PruneThumbnails implements Runnable {
        private Context mContext;
        private List<Long> mIds;

        PruneThumbnails(Context context, List<Long> preserveIds) {
            mContext = context.getApplicationContext();
            mIds = preserveIds;
        }

        @Override
        public void run() {
            ContentResolver cr = mContext.getContentResolver();
            if (mIds == null || mIds.size() == 0) {
                cr.delete(Thumbnails.CONTENT_URI, null, null);
            } else {
                int length = mIds.size();
                StringBuilder where = new StringBuilder();
                where.append(Thumbnails._ID);
                where.append(" not in (");
                for (int i = 0; i < length; i++) {
                    where.append(mIds.get(i));
                    if (i < (length - 1)) {
                        where.append(",");
                    }
                }
                where.append(")");
                cr.delete(Thumbnails.CONTENT_URI, where.toString(), null);
            }
        }

    }

    @Override
    public WebViewFactory getWebViewFactory() {
        return mFactory;
    }

    @Override
    public void onSetWebView(Tab tab, WebView view) {
        mUi.onSetWebView(tab, view);
    }

    @Override
    public void createSubWindow(Tab tab) {
        endActionMode();
        WebView mainView = tab.getWebView();
        WebView subView = mFactory.createWebView((mainView == null)
                ? false
                : mainView.isPrivateBrowsingEnabled());
        mUi.createSubWindow(tab, subView);
    }

    @Override
    public Context getContext() {
        return mActivity;
    }

    @Override
    public Activity getActivity() {
        return mActivity;
    }

    void setUi(UI ui) {
        mUi = ui;
    }

    BrowserSettings getSettings() {
        return mSettings;
    }

    IntentHandler getIntentHandler() {
        return mIntentHandler;
    }

    @Override
    public UI getUi() {
        return mUi;
    }

    int getMaxTabs() {
        return mActivity.getResources().getInteger(R.integer.max_tabs);
    }

    @Override
    public TabControl getTabControl() {
        return mTabControl;
    }

    @Override
    public List<Tab> getTabs() {
        return mTabControl.getTabs();
    }

    // Open the icon database.
    private void openIconDatabase() {
        // We have to call getInstance on the UI thread
        final WebIconDatabase instance = WebIconDatabase.getInstance();
        BackgroundHandler.execute(new Runnable() {

            @Override
            public void run() {
                instance.open(mActivity.getDir("icons", 0).getPath());
            }
        });
    }

    private void startHandler() {
        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case OPEN_BOOKMARKS:
                        bookmarksOrHistoryPicker(ComboViews.Bookmarks);
                        break;
                    case FOCUS_NODE_HREF:
                    {
                        String url = (String) msg.getData().get("url");
                        String title = (String) msg.getData().get("title");
                        String src = (String) msg.getData().get("src");
                        if (url == "") url = src; // use image if no anchor
                        if (TextUtils.isEmpty(url)) {
                            break;
                        }
                        HashMap focusNodeMap = (HashMap) msg.obj;
                        WebView view = (WebView) focusNodeMap.get("webview");
                        // Only apply the action if the top window did not change.
                        if (getCurrentTopWebView() != view) {
                            break;
                        }
                        switch (msg.arg1) {
                            case R.id.open_context_menu_id:
                                loadUrlFromContext(url);
                                break;
                            case R.id.view_image_context_menu_id:
                                loadUrlFromContext(src);
                                break;
                            case R.id.open_newtab_context_menu_id:
                                final Tab parent = mTabControl.getCurrentTab();
                                openTab(url, parent,
                                        !mSettings.openInBackground(), true);
                                break;
                            case R.id.copy_link_context_menu_id:
                                copy(url);
                                break;
                            case R.id.save_link_context_menu_id:
                            case R.id.download_context_menu_id:
                                DownloadHandler.onDownloadStartNoStream(
                                        mActivity, url, null, null, null,
                                        // view.isPrivateBrowsingEnabled());
                                        view.isPrivateBrowsingEnabled(), 0);
                                break;
                            case R.id.save_link_tobookmark_context_menu_id:
										            Intent bookmarkIntent = createBookmarkLinkIntent(url);
				                        if (bookmarkIntent != null) {
					                          mActivity.startActivity(bookmarkIntent);
				                        }               
                                break;
                        }
                        break;
                    }

                    case LOAD_URL:
                        loadUrlFromContext((String) msg.obj);
                        break;

                    case STOP_LOAD:
                        stopLoading();
                        break;

                    case RELEASE_WAKELOCK:
                        if (mWakeLock != null && mWakeLock.isHeld()) {
                            mWakeLock.release();
                            // if we reach here, Browser should be still in the
                            // background loading after WAKELOCK_TIMEOUT (5-min).
                            // To avoid burning the battery, stop loading.
                            mTabControl.stopAllLoading();
                        }
                        break;

                    case UPDATE_BOOKMARK_THUMBNAIL:
                        Tab tab = (Tab) msg.obj;
                        if (tab != null) {
                            updateScreenshot(tab);
                        }
                        break;
                }
            }
        };

    }

    @Override
    public Tab getCurrentTab() {
        return mTabControl.getCurrentTab();
    }

    @Override
    public void shareCurrentPage() {
        shareCurrentPage(mTabControl.getCurrentTab());
    }

    private void shareCurrentPage(Tab tab) {
        if (tab != null) {
            String title = tab.getTitle();
            String url = tab.getUrl();
            if (url == null || url.length() == 0) {
                url = title;
            }
            sharePage(mActivity, title,
                    url, tab.getFavicon(),
                    createScreenshot(tab.getWebView(),
                            getDesiredThumbnailWidth(mActivity),
                            getDesiredThumbnailHeight(mActivity)));
        }
    }

    /**
     * Share a page, providing the title, url, favicon, and a screenshot.  Uses
     * an {@link Intent} to launch the Activity chooser.
     * @param c Context used to launch a new Activity.
     * @param title Title of the page.  Stored in the Intent with
     *          {@link Intent#EXTRA_SUBJECT}
     * @param url URL of the page.  Stored in the Intent with
     *          {@link Intent#EXTRA_TEXT}
     * @param favicon Bitmap of the favicon for the page.  Stored in the Intent
     *          with {@link Browser#EXTRA_SHARE_FAVICON}
     * @param screenshot Bitmap of a screenshot of the page.  Stored in the
     *          Intent with {@link Browser#EXTRA_SHARE_SCREENSHOT}
     */
    static final void sharePage(Context c, String title, String url,
            Bitmap favicon, Bitmap screenshot) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, url);
        send.putExtra(Intent.EXTRA_SUBJECT, title);
        send.putExtra(Browser.EXTRA_SHARE_FAVICON, favicon);
        send.putExtra(Browser.EXTRA_SHARE_SCREENSHOT, screenshot);
        try {
            c.startActivity(Intent.createChooser(send, c.getString(
                    R.string.choosertitle_sharevia)));
        } catch(android.content.ActivityNotFoundException ex) {
            // if no app handles it, do nothing
        }
    }

    private void copy(CharSequence text) {
        ClipboardManager cm = (ClipboardManager) mActivity
                .getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(text);
    }

    // lifecycle

    protected void onConfgurationChanged(Configuration config) {
        mConfigChanged = true;
        if (mPageDialogsHandler != null) {
            mPageDialogsHandler.onConfigurationChanged(config);
        }
        mUi.onConfigurationChanged(config);
    }

    @Override
    public void handleNewIntent(Intent intent) {
        if (!mUi.isWebShowing()) {
            mUi.showWeb(false);
        }
        mIntentHandler.onNewIntent(intent);
    }

    protected void onPause() {
        if (mUi.isCustomViewShowing()) {
            hideCustomView();
        }
        if (mActivityPaused) {
            Log.e(LOGTAG, "BrowserActivity is already paused.");
            return;
        }
        mActivityPaused = true;
        Tab tab = mTabControl.getCurrentTab();
        if (tab != null) {
            tab.pause();
            if (!pauseWebViewTimers(tab)) {
                if (mWakeLock == null) {
                    PowerManager pm = (PowerManager) mActivity
                            .getSystemService(Context.POWER_SERVICE);
                    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Browser");
                }
                mWakeLock.acquire();
                mHandler.sendMessageDelayed(mHandler
                        .obtainMessage(RELEASE_WAKELOCK), WAKELOCK_TIMEOUT);
            }
        }
        mUi.onPause();
        mNetworkHandler.onPause();

        WebView.disablePlatformNotifications();
        NfcHandler.unregister(mActivity);
        if (sThumbnailBitmap != null) {
            sThumbnailBitmap.recycle();
            sThumbnailBitmap = null;
        }

        if (mSmsHandler != null)
        {
            mSmsHandler.onPause();
        }
    }

    void onSaveInstanceState(Bundle outState) {
        // the default implementation requires each view to have an id. As the
        // browser handles the state itself and it doesn't use id for the views,
        // don't call the default implementation. Otherwise it will trigger the
        // warning like this, "couldn't save which view has focus because the
        // focused view XXX has no id".

        // Save all the tabs
        mTabControl.saveState(outState);
        if (!outState.isEmpty()) {
            // Save time so that we know how old incognito tabs (if any) are.
            outState.putSerializable("lastActiveDate", Calendar.getInstance());
        }
    }

    void onResume() {
        if (!mActivityPaused) {
            Log.e(LOGTAG, "BrowserActivity is already resumed.");
            return;
        }
        mActivityPaused = false;
        Tab current = mTabControl.getCurrentTab();
        if (current != null) {
            current.resume();
            resumeWebViewTimers(current);
        }
        releaseWakeLock();

        mUi.onResume();
        mNetworkHandler.onResume();
        WebView.enablePlatformNotifications();
        NfcHandler.register(mActivity, this);

        if (mSmsHandler != null)
        {
            mSmsHandler.onResume();
        }        
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mHandler.removeMessages(RELEASE_WAKELOCK);
            mWakeLock.release();
        }
    }

    /**
     * resume all WebView timers using the WebView instance of the given tab
     * @param tab guaranteed non-null
     */
    private void resumeWebViewTimers(Tab tab) {
        boolean inLoad = tab.inPageLoad();
        if ((!mActivityPaused && !inLoad) || (mActivityPaused && inLoad)) {
            CookieSyncManager.getInstance().startSync();
            WebView w = tab.getWebView();
            WebViewTimersControl.getInstance().onBrowserActivityResume(w);
        }
    }

    /**
     * Pause all WebView timers using the WebView of the given tab
     * @param tab
     * @return true if the timers are paused or tab is null
     */
    private boolean pauseWebViewTimers(Tab tab) {
        if (tab == null) {
            return true;
        } else if (!tab.inPageLoad()) {
            CookieSyncManager.getInstance().stopSync();
            WebViewTimersControl.getInstance().onBrowserActivityPause(getCurrentWebView());
            return true;
        }
        return false;
    }

    void onDestroy() {
        if (mUploadHandler != null && !mUploadHandler.handled()) {
            mUploadHandler.onResult(Activity.RESULT_CANCELED, null);
            mUploadHandler = null;
        }
        if (mTabControl == null) return;
        mUi.onDestroy();
        // Remove the current tab and sub window
        Tab t = mTabControl.getCurrentTab();
        if (t != null) {
            dismissSubWindow(t);
            removeTab(t);
        }
        mActivity.getContentResolver().unregisterContentObserver(mBookmarksObserver);
        // Destroy all the tabs
        mTabControl.destroy();
        WebIconDatabase.getInstance().close();
        // Stop watching the default geolocation permissions
        mSystemAllowGeolocationOrigins.stop();
        mSystemAllowGeolocationOrigins = null;
    }

    protected boolean isActivityPaused() {
        return mActivityPaused;
    }

    protected void onLowMemory() {
        mTabControl.freeMemory();
    }

    @Override
    public boolean shouldShowErrorConsole() {
        return mShouldShowErrorConsole;
    }

    protected void setShouldShowErrorConsole(boolean show) {
        if (show == mShouldShowErrorConsole) {
            // Nothing to do.
            return;
        }
        mShouldShowErrorConsole = show;
        Tab t = mTabControl.getCurrentTab();
        if (t == null) {
            // There is no current tab so we cannot toggle the error console
            return;
        }
        mUi.setShouldShowErrorConsole(t, show);
    }

    @Override
    public void stopLoading() {
        mLoadStopped = true;
        Tab tab = mTabControl.getCurrentTab();
        WebView w = getCurrentTopWebView();
        w.stopLoading();
        mUi.onPageStopped(tab);
    }

    boolean didUserStopLoading() {
        return mLoadStopped;
    }
    
    //@ M: For OP01 wlan test case, first is true, will be false after using once.
    private boolean mOp01NoNetworkShouldNotify = true;
    
    //For OP01 wlan test case, used when only the first time open the browser
    //in onPageStarted function, using mOp01NoNetworkShouldNotify variable to judge.
	private void handleOp01NoNetworkNotify(WebView view){
		ConnectivityManager conMgr = (ConnectivityManager)this.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		WifiManager wifiMgr = (WifiManager) this.getContext().getSystemService(Context.WIFI_SERVICE);
		if( wifiMgr.isWifiEnabled()){
		    //WiFi is not connected
			NetworkInfo mNetworkInfo = conMgr.getActiveNetworkInfo();
			if (mNetworkInfo == null || (mNetworkInfo != null && mNetworkInfo.getType() != ConnectivityManager.TYPE_WIFI)) {
				//if (mNetworkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
					//For OP01 wlan test case 7.3.4
		            List<ScanResult> list = wifiMgr.getScanResults();
		            if (list != null && list.size() == 0) {
		                Xlog.d(XLOGTAG, "handleOp01NoNetworkNotify(): For OP01 wlan test case 7.3.4.");
		                SharedPreferences sh = this.getContext().getSharedPreferences("data_connection", this.getContext().MODE_WORLD_READABLE);
		                boolean needShow = !sh.getBoolean("pref_not_remind", false);
		                if(needShow){
		                    Intent dlg = new Intent(this.getContext(), DataConnectionDialog.class);
		                    this.getContext().startActivity(dlg);
		                }
		            }else{
		                //For OP01 wlan test case 7.3.2
		                if (ActivityManagerNative.isSystemReady()) {
		                    Xlog.d(XLOGTAG, "handleOp01NoNetworkNotify(): For OP01 wlan test case 7.3.2.");
		                    Intent intent = new Intent("android.net.wifi.PICK_WIFI_NETWORK_AND_GPRS");
		                    intent.putExtra("access_points_and_gprs", true);
		                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		                    this.getContext().startActivity(intent);
		                }else{
		                    Xlog.e(XLOGTAG, "handleOp01NoNetworkNotify(): Error, ActivityManagerNative.isSystemReady return false.");
		                }
		            }
		            mOp01NoNetworkShouldNotify = false;
				//}
			}
		}else {
			if(!mNetworkHandler.isNetworkUp()){
		        view.setNetworkAvailable(false);
		        Xlog.v(XLOGTAG, "handleOp01NoNetworkNotify(): WiFi is not enabled.");
		    }
		}
	}

    // WebViewController

    @Override
    public void onPageStarted(Tab tab, WebView view, Bitmap favicon) {

        // We've started to load a new page. If there was a pending message
        // to save a screenshot then we will now take the new page and save
        // an incorrect screenshot. Therefore, remove any pending thumbnail
        // messages from the queue.
        mHandler.removeMessages(Controller.UPDATE_BOOKMARK_THUMBNAIL,
                tab);

        // reset sync timer to avoid sync starts during loading a page
        CookieSyncManager.getInstance().resetSync();

        if (!mNetworkHandler.isNetworkUp()) {
            //MTK_OP01_PROTECT_START
            String optr = SystemProperties.get("ro.operator.optr");
            if (null != optr && optr.equals("OP01")) {
                Xlog.d(XLOGTAG, "onPageStarted(): network unavailable, show wlan ap selection activity or network dialog");
                if(true == mOp01NoNetworkShouldNotify){
                    handleOp01NoNetworkNotify(view);
                }else{
                    view.setNetworkAvailable(false);
                }
                //Only Launch will trigger the case, not when browsing
                mOp01NoNetworkShouldNotify = false;
            }else
            //MTK_OP01_PROTECT_END
            {
                view.setNetworkAvailable(false);
            }
        }else {
            //MTK_OP01_PROTECT_START
            String optr = SystemProperties.get("ro.operator.optr");
            if (null != optr && optr.equals("OP01")) {
                Xlog.d(XLOGTAG, "onPageStarted(): network available, if wlan enabled but no ap, then show network dialog");
                if(true == mOp01NoNetworkShouldNotify){
                    handleOp01NoNetworkNotify(view);
                }
                //Only Launch will trigger the case, not when browsing
                mOp01NoNetworkShouldNotify = false;
            }
            //MTK_OP01_PROTECT_END
        }

        // when BrowserActivity just starts, onPageStarted may be called before
        // onResume as it is triggered from onCreate. Call resumeWebViewTimers
        // to start the timer. As we won't switch tabs while an activity is in
        // pause state, we can ensure calling resume and pause in pair.
        if (mActivityPaused) {
            resumeWebViewTimers(tab);
        }
        mLoadStopped = false;
        endActionMode();

        mUi.onTabDataChanged(tab);

        String url = tab.getUrl();
        // update the bookmark database for favicon
        maybeUpdateFavicon(tab, null, url, favicon);

        Performance.tracePageStart(url);

        // Performance probe
        if (false) {
            Performance.onPageStarted();
        }

    }

    @Override
    public void onPageFinished(Tab tab) {
        Xlog.d(XLOGTAG, "Controller onPageFinished");
        mUi.onTabDataChanged(tab);
        if (!tab.isPrivateBrowsingEnabled()
                && !TextUtils.isEmpty(tab.getUrl())
                && !tab.isSnapshot()) {
            // Only update the bookmark screenshot if the user did not
            // cancel the load early and there is not already
            // a pending update for the tab.
            if (tab.inForeground() && !didUserStopLoading()
                    || !tab.inForeground()) {
                if (!mHandler.hasMessages(UPDATE_BOOKMARK_THUMBNAIL, tab)) {
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(
                            UPDATE_BOOKMARK_THUMBNAIL, 0, 0, tab),
                            500);
                }
            }
        }
        // pause the WebView timer and release the wake lock if it is finished
        // while BrowserActivity is in pause state.
        if (mActivityPaused && pauseWebViewTimers(tab)) {
            releaseWakeLock();
        }
        BaseUi ui = (BaseUi)getUi();
        Xlog.d(XLOGTAG, "Controller onPageFinished url:" + tab.getUrl());
        Xlog.d(XLOGTAG, "Controller onPageFinished ui.getInputUrlFlag():" + ui.getInputUrlFlag());
        if (ABOUT_BLANK_URL.equals(tab.getUrl()) && ui.getInputUrlFlag()) {
            ui.setInputUrlFlag(false);
            ui.editUrl(true);
        }
        // Performance probe
        if (false) {
            Performance.onPageFinished(tab.getUrl());
         }

        Performance.tracePageFinished();
    }

    @Override
    public void onProgressChanged(Tab tab) {
        mCrashRecoveryHandler.backupState();
        int newProgress = tab.getLoadProgress();

        if (newProgress == 100) {
            CookieSyncManager.getInstance().sync();
            // onProgressChanged() may continue to be called after the main
            // frame has finished loading, as any remaining sub frames continue
            // to load. We'll only get called once though with newProgress as
            // 100 when everything is loaded. (onPageFinished is called once
            // when the main frame completes loading regardless of the state of
            // any sub frames so calls to onProgressChanges may continue after
            // onPageFinished has executed)
            
            //M: comment it because we have seprate the comb menu
//            if (tab.inPageLoad()) {
//                updateInLoadMenuItems(mCachedMenu, tab);
//            }
        } else {
            //M: comment it because we have seprate the comb menu
//            if (!tab.inPageLoad()) {
//                // onPageFinished may have already been called but a subframe is
//                // still loading
//                // updating the progress and
//                // update the menu items.
//                updateInLoadMenuItems(mCachedMenu, tab);
//            }
        }
        mUi.onProgressChanged(tab);
    }

    @Override
    public void onUpdatedSecurityState(Tab tab) {
        mUi.onTabDataChanged(tab);
    }

    @Override
    public void onReceivedTitle(Tab tab, final String title) {
        mUi.onTabDataChanged(tab);
        final String pageUrl = tab.getOriginalUrl();
        if (TextUtils.isEmpty(pageUrl) || pageUrl.length()
                >= SQLiteDatabase.SQLITE_MAX_LIKE_PATTERN_LENGTH) {
            return;
        }
        // Update the title in the history database if not in private browsing mode
        if (!tab.isPrivateBrowsingEnabled()) {
            DataController.getInstance(mActivity).updateHistoryTitle(pageUrl, title);
        }
    }

    @Override
    public void onFavicon(Tab tab, WebView view, Bitmap icon) {
        mUi.onTabDataChanged(tab);
        maybeUpdateFavicon(tab, view.getOriginalUrl(), view.getUrl(), icon);
    }

    @Override
    public boolean shouldOverrideUrlLoading(Tab tab, WebView view, String url) {
        return mUrlHandler.shouldOverrideUrlLoading(tab, view, url);
    }

    @Override
    public boolean shouldOverrideKeyEvent(KeyEvent event) {
        if (mMenuIsDown) {
            // only check shortcut key when MENU is held
            return mActivity.getWindow().isShortcutKey(event.getKeyCode(),
                    event);
        } else {
            return false;
        }
    }

    @Override
    public void onUnhandledKeyEvent(KeyEvent event) {
        if (!isActivityPaused()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                mActivity.onKeyDown(event.getKeyCode(), event);
            } else {
                mActivity.onKeyUp(event.getKeyCode(), event);
            }
        }
    }

    @Override
    public void doUpdateVisitedHistory(Tab tab, boolean isReload) {
        // Don't save anything in private browsing mode
        if (tab.isPrivateBrowsingEnabled()) return;
        String url = tab.getOriginalUrl();

        if (TextUtils.isEmpty(url)
                || url.regionMatches(true, 0, "about:", 0, 6)) {
            return;
        }
        DataController.getInstance(mActivity).updateVisitedHistory(url);
        mCrashRecoveryHandler.backupState();
    }

    @Override
    public void getVisitedHistory(final ValueCallback<String[]> callback) {
        AsyncTask<Void, Void, String[]> task =
                new AsyncTask<Void, Void, String[]>() {
            @Override
            public String[] doInBackground(Void... unused) {
                return Browser.getVisitedHistory(mActivity.getContentResolver());
            }
            @Override
            public void onPostExecute(String[] result) {
                callback.onReceiveValue(result);
            }
        };
        task.execute();
    }

    @Override
    public void onReceivedHttpAuthRequest(Tab tab, WebView view,
            final HttpAuthHandler handler, final String host,
            final String realm) {
        String username = null;
        String password = null;

        boolean reuseHttpAuthUsernamePassword
                = handler.useHttpAuthUsernamePassword();

        if (reuseHttpAuthUsernamePassword && view != null) {
            String[] credentials = view.getHttpAuthUsernamePassword(host, realm);
            if (credentials != null && credentials.length == 2) {
                username = credentials[0];
                password = credentials[1];
            }
        }

        if (username != null && password != null) {
            handler.proceed(username, password);
        } else {
            if (tab.inForeground() && !handler.suppressDialog()) {
                mPageDialogsHandler.showHttpAuthentication(tab, handler, host, realm);
            } else {
                handler.cancel();
            }
        }
    }

    @Override
    public void onDownloadStart(Tab tab, String url, String userAgent,
            String contentDisposition, String mimetype, long contentLength) {
        WebView w = tab.getWebView();
        DownloadHandler.onDownloadStart(mActivity, url, userAgent,
                // contentDisposition, mimetype, w.isPrivateBrowsingEnabled());
                contentDisposition, mimetype, w.isPrivateBrowsingEnabled(), contentLength);
        if (w != null && w.copyBackForwardList().getSize() == 0) {
            // This Tab was opened for the sole purpose of downloading a
            // file. Remove it.
            if (tab == mTabControl.getCurrentTab()) {
                // In this case, the Tab is still on top.
                //goBackOnePageOrQuit();
            } else {
                // In this case, it is not.
                closeTab(tab);
            }
        }
    }

    @Override
    public Bitmap getDefaultVideoPoster() {
        return mUi.getDefaultVideoPoster();
    }

    @Override
    public View getVideoLoadingProgressView() {
        return mUi.getVideoLoadingProgressView();
    }

    @Override
    public void showSslCertificateOnError(WebView view, SslErrorHandler handler,
            SslError error) {
        mPageDialogsHandler.showSSLCertificateOnError(view, handler, error);
    }

    @Override
    public void showAutoLogin(Tab tab) {
        assert tab.inForeground();
        // Update the title bar to show the auto-login request.
        mUi.showAutoLogin(tab);
    }

    @Override
    public void hideAutoLogin(Tab tab) {
        assert tab.inForeground();
        mUi.hideAutoLogin(tab);
    }

    // helper method

    /*
     * Update the favorites icon if the private browsing isn't enabled and the
     * icon is valid.
     */
    private void maybeUpdateFavicon(Tab tab, final String originalUrl,
            final String url, Bitmap favicon) {
        if (favicon == null) {
            return;
        }
        if (!tab.isPrivateBrowsingEnabled()) {
            Bookmarks.updateFavicon(mActivity
                    .getContentResolver(), originalUrl, url, favicon);
        }
        
//        //M: Save favorite icons for customized preset bookmarks
//        //Do not remove it and keep it as comment for for coming use
//        try{
//            saveFavoriteBitmapToFile(System.currentTimeMillis() + "F", favicon);
//        }catch(IOException e){
//            e.printStackTrace();
//        }
        
    }
    
//    //M: If you need preset some bookmarks, you should prepare the favorite icons for them
//    //This function is used to save the picture to sdcard and then you can pull it out and use it
//    //This function will be called when the favorite icon loading is finished
//    //This function should be used with saveThumbnailBitmapToFile
//    public void saveFavoriteBitmapToFile(String bitName, Bitmap bitmap) throws IOException {
//        try {
//        	File f = new File("/sdcard/Pictures/" + bitName + ".ico");
//            f.createNewFile();
//            FileOutputStream fOut = null;
//            fOut = new FileOutputStream(f);
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
//            fOut.flush();
//            fOut.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public void bookmarkedStatusHasChanged(Tab tab) {
        // TODO: Switch to using onTabDataChanged after b/3262950 is fixed
        mUi.bookmarkedStatusHasChanged(tab);
    }

    // end WebViewController

    protected void pageUp() {
        getCurrentTopWebView().pageUp(false);
    }

    protected void pageDown() {
        getCurrentTopWebView().pageDown(false);
    }

    // callback from phone title bar
    public void editUrl() {
        if (mOptionsMenuOpen) mActivity.closeOptionsMenu();
        mUi.editUrl(false);
    }

    public void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                mActivity.getComponentName().flattenToString());
        intent.putExtra(SEND_APP_ID_EXTRA, false);
        intent.putExtra(RecognizerIntent.EXTRA_WEB_SEARCH_ONLY, true);
        //M: ALPS00105851, we should first verify if there is any suitable resolver
        ResolveInfo info = this.getContext().getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if(info != null){
            mActivity.startActivity(intent);
        }else{
            int title = R.string.no_voice_search_alert_dialog_title;
            int msg = R.string.no_voice_search_alert_dialog_msg;
            new AlertDialog.Builder(this.getContext())
                .setTitle(title)
                .setIcon(R.drawable.ic_voice_search_holo_dark)
                .setMessage(msg)
                .setPositiveButton(R.string.ok, null)
                .show();
        }
    }

    @Override
    public void activateVoiceSearchMode(String title, List<String> results) {
        mUi.showVoiceTitleBar(title, results);
    }

    public void revertVoiceSearchMode(Tab tab) {
        mUi.revertVoiceTitleBar(tab);
    }

    public boolean supportsVoiceSearch() {
        SearchEngine searchEngine = getSettings().getSearchEngine();
        return (searchEngine != null && searchEngine.supportsVoiceSearch());
    }

    public void showCustomView(Tab tab, View view, int requestedOrientation,
            WebChromeClient.CustomViewCallback callback) {
        if (tab.inForeground()) {
            if (mUi.isCustomViewShowing()) {
                callback.onCustomViewHidden();
                return;
            }
            mUi.showCustomView(view, requestedOrientation, callback);
            // Save the menu state and set it to empty while the custom
            // view is showing.
            mOldMenuState = mMenuState;
            mMenuState = EMPTY_MENU;
            mActivity.invalidateOptionsMenu();
        }
    }

    @Override
    public void hideCustomView() {
        if (mUi.isCustomViewShowing()) {
            mUi.onHideCustomView();
            // Reset the old menu state.
            mMenuState = mOldMenuState;
            mOldMenuState = EMPTY_MENU;
            mActivity.invalidateOptionsMenu();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        if (getCurrentTopWebView() == null) return;
        switch (requestCode) {
            case PREFERENCES_PAGE:
                if (resultCode == Activity.RESULT_OK && intent != null) {
                    String action = intent.getStringExtra(Intent.EXTRA_TEXT);
                    if (PreferenceKeys.PREF_PRIVACY_CLEAR_HISTORY.equals(action)) {
                        mTabControl.removeParentChildRelationShips();
                    }
                }
                break;
            case FILE_SELECTED:
                // Chose a file from the file picker.
                if (null == mUploadHandler) break;
                mUploadHandler.onResult(resultCode, intent);
                break;
            case AUTOFILL_SETUP:
                // Determine whether a profile was actually set up or not
                // and if so, send the message back to the WebTextView to
                // fill the form with the new profile.
                if (getSettings().getAutoFillProfile() != null) {
                    mAutoFillSetupMessage.sendToTarget();
                    mAutoFillSetupMessage = null;
                }
                break;
            case COMBO_VIEW:
                if (intent == null || resultCode != Activity.RESULT_OK) {
                    break;
                }
                mUi.showWeb(false);
                if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                    Tab t = getCurrentTab();
                    Uri uri = intent.getData();
                    mNeedUpdateThumbnail = true;
                    mUrlNeedUpdateThumbnail = uri.toString();
                    loadUrl(t, uri.toString());
                } else if (intent.hasExtra(ComboViewActivity.EXTRA_OPEN_ALL)) {
                    String[] urls = intent.getStringArrayExtra(
                            ComboViewActivity.EXTRA_OPEN_ALL);
                    Tab parent = getCurrentTab();
                    for (String url : urls) {
                        parent = openTab(url, parent,
                                !mSettings.openInBackground(), true);
                    }
                } else if (intent.hasExtra(ComboViewActivity.EXTRA_OPEN_SNAPSHOT)) {
                    long id = intent.getLongExtra(
                            ComboViewActivity.EXTRA_OPEN_SNAPSHOT, -1);
                    if (id >= 0) {
                        createNewSnapshotTab(id, true);
                    }
                }
                break;
            case SITE_NAVIGATION_ADD_DIALOG:
                if (intent == null || resultCode != Activity.RESULT_OK) {
                    break;
                }
                
                if(intent.getBooleanExtra("need_refresh", false) && getCurrentTopWebView() != null){
                
                    getCurrentTopWebView().reload();
                }
            default:
                break;
        }
        getCurrentTopWebView().requestFocus();
    }

    /**
     * Open the Go page.
     * @param startWithHistory If true, open starting on the history tab.
     *                         Otherwise, start with the bookmarks tab.
     */
    @Override
    public void bookmarksOrHistoryPicker(ComboViews startView) {
        if (mTabControl.getCurrentWebView() == null) {
            return;
        }
        // clear action mode
        if (isInCustomActionMode()) {
            endActionMode();
        }
        Bundle extras = new Bundle();
        // Disable opening in a new window if we have maxed out the windows
        extras.putBoolean(BrowserBookmarksPage.EXTRA_DISABLE_WINDOW,
                !mTabControl.canCreateNewTab());
        mUi.showComboView(startView, extras);
    }

    // combo view callbacks

    // key handling
    protected void onBackKey() {
        if (!mUi.onBackKey()) {
            WebView subwindow = mTabControl.getCurrentSubWindow();
            if (subwindow != null) {
                if (subwindow.canGoBack()) {
                    subwindow.goBack();
                } else {
                    dismissSubWindow(mTabControl.getCurrentTab());
                }
            } else {
                goBackOnePageOrQuit();
            }
        }
    }

    protected boolean onMenuKey() {
        return mUi.onMenuKey();
    }

    // menu handling and state
    // TODO: maybe put into separate handler

    protected boolean onCreateOptionsMenu(Menu menu) {
        if (mMenuState == EMPTY_MENU) {
            return false;
        }
        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.browser, menu);
        return true;
    }

    protected void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        if (v instanceof TitleBar) {
            return;
        }
        if (!(v instanceof WebView)) {
            return;
        }
        final WebView webview = (WebView) v;
        WebView.HitTestResult result = webview.getHitTestResult();
        if (result == null) {
            return;
        }

        int type = result.getType();
        if (type == WebView.HitTestResult.UNKNOWN_TYPE) {
            Log.w(LOGTAG,
                    "We should not show context menu when nothing is touched");
            return;
        }
        if (type == WebView.HitTestResult.EDIT_TEXT_TYPE) {
            // let TextView handles context menu
            return;
        }

        // Note, http://b/issue?id=1106666 is requesting that
        // an inflated menu can be used again. This is not available
        // yet, so inflate each time (yuk!)
        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.browsercontext, menu);

        // Show the correct menu group
        final String extra = result.getExtra();
        
        //====================================site navigation modification start==================================
        //M: Add for site navigation to get the image anchor url
        final String imageAnchorUrlExtra = result.getImageAnchorUrlExtra();
        Xlog.d(XLOGTAG, "sitenavigation onCreateContextMenu imageAnchorUrlExtra is : " + imageAnchorUrlExtra);
        
        //@ M: Add for site navigation context menu
        String url = null;
        String itemUrl = null;

        TelephonyManager telephony = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        boolean mIsVoiceCapable = false;
        
        if (telephony != null){
            mIsVoiceCapable = telephony.isVoiceCapable();
        }
                    
        if(BrowserFeatureOption.BROWSER_SITE_NAVIGATION_SUPPORT == true){
            url = webview.getOriginalUrl();
            if(url != null && url.equalsIgnoreCase(SiteNavigation.SITE_NAVIGATION)){
                itemUrl = Uri.decode(imageAnchorUrlExtra);
                // If item's url != null, then should show edit menu; otherwise if it is null, then should show "add" menu
                if(itemUrl != null){
                    if(isSiteNavigationAboutBlankUrl(itemUrl)){
                        menu.setGroupVisible(R.id.SITE_NAVIGATION_ADD, type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
                        menu.setGroupVisible(R.id.SITE_NAVIGATION_EDIT, false);
                    }else{
                        menu.setGroupVisible(R.id.SITE_NAVIGATION_ADD, false);
                        menu.setGroupVisible(R.id.SITE_NAVIGATION_EDIT, type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
                    }
                    
                    // Other group should be invisible
                    menu.setGroupVisible(R.id.PHONE_MENU, false);
                    menu.setGroupVisible(R.id.NO_PHONE_MENU, false);
                    menu.setGroupVisible(R.id.EMAIL_MENU, false);
                    menu.setGroupVisible(R.id.GEO_MENU, false);
                    menu.setGroupVisible(R.id.IMAGE_MENU, false);
                    menu.setGroupVisible(R.id.ANCHOR_MENU, false);
                    menu.setGroupVisible(R.id.SELECT_TEXT_MENU, false);
                }else{
                    Xlog.d(XLOGTAG, "sitenavigation onCreateContextMenu itemUrl is null! Anchor text selected.");
                    menu.setGroupVisible(R.id.SITE_NAVIGATION_ADD, false);
                    menu.setGroupVisible(R.id.SITE_NAVIGATION_EDIT, false);        
                    menu.setGroupVisible(R.id.PHONE_MENU, false);
                    menu.setGroupVisible(R.id.NO_PHONE_MENU, false);
                    menu.setGroupVisible(R.id.EMAIL_MENU, false);
                    menu.setGroupVisible(R.id.GEO_MENU, false);
                    menu.setGroupVisible(R.id.IMAGE_MENU, false);
                    menu.setGroupVisible(R.id.ANCHOR_MENU, false);
                    menu.setGroupVisible(R.id.SELECT_TEXT_MENU, false);
                }
            }else{
                //If it is not ahout:sitenavigation, then hide the two menu groups
                menu.setGroupVisible(R.id.SITE_NAVIGATION_EDIT, false);
                menu.setGroupVisible(R.id.SITE_NAVIGATION_ADD, false);
                
                //Other group should be normal
                if(mIsVoiceCapable == true)
                {
                    menu.setGroupVisible(R.id.PHONE_MENU,
                        type == WebView.HitTestResult.PHONE_TYPE);
                    menu.setGroupVisible(R.id.NO_PHONE_MENU,
                        false);                    
                }
                else
                {
                    menu.setGroupVisible(R.id.PHONE_MENU,
                        false);
                    menu.setGroupVisible(R.id.NO_PHONE_MENU,
                        true);
                }
                
                menu.setGroupVisible(R.id.EMAIL_MENU,
                        type == WebView.HitTestResult.EMAIL_TYPE);
                menu.setGroupVisible(R.id.GEO_MENU,
                        type == WebView.HitTestResult.GEO_TYPE);
                menu.setGroupVisible(R.id.IMAGE_MENU,
                        type == WebView.HitTestResult.IMAGE_TYPE
                        || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
                menu.setGroupVisible(R.id.ANCHOR_MENU,
                        type == WebView.HitTestResult.SRC_ANCHOR_TYPE
                        || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
                boolean hitText = type == WebView.HitTestResult.SRC_ANCHOR_TYPE
                        || type == WebView.HitTestResult.PHONE_TYPE
                        || type == WebView.HitTestResult.EMAIL_TYPE
                        || type == WebView.HitTestResult.GEO_TYPE;
                menu.setGroupVisible(R.id.SELECT_TEXT_MENU, hitText);
                if (hitText) {
                    menu.findItem(R.id.select_text_menu_id)
                            .setOnMenuItemClickListener(new SelectText(webview));
                }
            }
        }else{
            //If site navigation is not enabled, then hide the two menu groups
            menu.setGroupVisible(R.id.SITE_NAVIGATION_EDIT, false);
            menu.setGroupVisible(R.id.SITE_NAVIGATION_ADD, false);
            
            //Other group should be normal
            if(mIsVoiceCapable == true)
            {
                menu.setGroupVisible(R.id.PHONE_MENU,
                    type == WebView.HitTestResult.PHONE_TYPE);
                menu.setGroupVisible(R.id.NO_PHONE_MENU,
                    false);
            }
            else
            {
                menu.setGroupVisible(R.id.PHONE_MENU,
                    false);
                menu.setGroupVisible(R.id.NO_PHONE_MENU,
                    true);
            }

            menu.setGroupVisible(R.id.EMAIL_MENU,
                    type == WebView.HitTestResult.EMAIL_TYPE);
            menu.setGroupVisible(R.id.GEO_MENU,
                    type == WebView.HitTestResult.GEO_TYPE);
            menu.setGroupVisible(R.id.IMAGE_MENU,
                    type == WebView.HitTestResult.IMAGE_TYPE
                    || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
            menu.setGroupVisible(R.id.ANCHOR_MENU,
                    type == WebView.HitTestResult.SRC_ANCHOR_TYPE
                    || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE);
            boolean hitText = type == WebView.HitTestResult.SRC_ANCHOR_TYPE
                    || type == WebView.HitTestResult.PHONE_TYPE
                    || type == WebView.HitTestResult.EMAIL_TYPE
                    || type == WebView.HitTestResult.GEO_TYPE;
            menu.setGroupVisible(R.id.SELECT_TEXT_MENU, hitText);
            if (hitText) {
                menu.findItem(R.id.select_text_menu_id)
                        .setOnMenuItemClickListener(new SelectText(webview));
            }
        }//@
        //====================================site navigation modification end==================================
        
        
        // Setup custom handling depending on the type
        switch (type) {
            case WebView.HitTestResult.PHONE_TYPE:
                menu.setHeaderTitle(Uri.decode(extra));                
                menu.findItem(R.id.dial_context_menu_id).setIntent(
                        new Intent(Intent.ACTION_VIEW, Uri
                                .parse(WebView.SCHEME_TEL + extra)));                                
                Intent addIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                addIntent.putExtra(Insert.PHONE, Uri.decode(extra));
                addIntent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);

                if(mIsVoiceCapable == true)
                {
                    menu.findItem(R.id.add_contact_context_menu_id).setIntent(
                            addIntent);
                    menu.findItem(R.id.copy_phone_context_menu_id)
                            .setOnMenuItemClickListener(
                            new Copy(extra));
                }
                else
                {
                    menu.findItem(R.id.add_contact_no_phone_context_menu_id).setIntent(
                            addIntent);
                    menu.findItem(R.id.copy_no_phone_context_menu_id)
                            .setOnMenuItemClickListener(
                            new Copy(extra));
                }
                
                break;

            case WebView.HitTestResult.EMAIL_TYPE:
                menu.setHeaderTitle(extra);
                menu.findItem(R.id.email_context_menu_id).setIntent(
                        new Intent(Intent.ACTION_VIEW, Uri
                                .parse(WebView.SCHEME_MAILTO + extra)));
                menu.findItem(R.id.copy_mail_context_menu_id)
                        .setOnMenuItemClickListener(
                        new Copy(extra));
                break;

            case WebView.HitTestResult.GEO_TYPE:
                menu.setHeaderTitle(extra);
                menu.findItem(R.id.map_context_menu_id).setIntent(
                        new Intent(Intent.ACTION_VIEW, Uri
                                .parse(WebView.SCHEME_GEO
                                        + URLEncoder.encode(extra))));
                menu.findItem(R.id.copy_geo_context_menu_id)
                        .setOnMenuItemClickListener(
                        new Copy(extra));
                break;

            case WebView.HitTestResult.SRC_ANCHOR_TYPE:
            case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
                menu.setHeaderTitle(extra);
                // decide whether to show the open link in new tab option
                boolean showNewTab = mTabControl.canCreateNewTab();
                MenuItem newTabItem
                        = menu.findItem(R.id.open_newtab_context_menu_id);
                newTabItem.setTitle(getSettings().openInBackground()
                        ? R.string.contextmenu_openlink_newwindow_background
                        : R.string.contextmenu_openlink_newwindow);
                newTabItem.setVisible(showNewTab);
                if (showNewTab) {
                    if (WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE == type) {
                        newTabItem.setOnMenuItemClickListener(
                                new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        final HashMap<String, WebView> hrefMap =
                                                new HashMap<String, WebView>();
                                        hrefMap.put("webview", webview);
                                        final Message msg = mHandler.obtainMessage(
                                                FOCUS_NODE_HREF,
                                                R.id.open_newtab_context_menu_id,
                                                0, hrefMap);
                                        webview.requestFocusNodeHref(msg);
                                        return true;
                                    }
                                });
                    } else {
                        newTabItem.setOnMenuItemClickListener(
                                new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        final Tab parent = mTabControl.getCurrentTab();
                                        openTab(extra, parent,
                                                !mSettings.openInBackground(),
                                                true);
                                        return true;
                                    }
                                });
                    }
                }
                //====================================site navigation modification start==================================
                //@ M:
                if(BrowserFeatureOption.BROWSER_SITE_NAVIGATION_SUPPORT == true){
                    if(url != null && url.equalsIgnoreCase(SiteNavigation.SITE_NAVIGATION)){
                        //Set the menu title
                        if(isSiteNavigationAboutBlankUrl(imageAnchorUrlExtra)){
                            menu.setHeaderTitle(ABOUT_BLANK_URL);
                        }else{
                            menu.setHeaderTitle(imageAnchorUrlExtra);
                        }
                        
                        //Set the openInNewTab invisible
                        menu.findItem(R.id.open_newtab_context_menu_id).setVisible(false);
                        
                        if(itemUrl != null){
                            if(isSiteNavigationAboutBlankUrl(itemUrl)){
                                //Add menu
                                menu.findItem(R.id.add_sn_context_menu_id)
                                    .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        final Intent intent = new Intent(Controller.this.getContext(), SiteNavigationAddDialog.class);
                                        Bundle bundle = new Bundle();
                                        String url = Uri.decode(imageAnchorUrlExtra);
                                        bundle.putBoolean("isAdding", true);
                                        bundle.putString("url", url);
                                        bundle.putString("name", getNameFromUrl(url));
                                        intent.putExtra("websites",bundle);
                                        mActivity.startActivityForResult(intent, SITE_NAVIGATION_ADD_DIALOG);
                                        return false;
                                    }
                                });
                            }else{
                                //Edit menu
                                menu.findItem(R.id.edit_sn_context_menu_id)
                                    .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        final Intent intent = new Intent(Controller.this.getContext(), SiteNavigationAddDialog.class);
                                        Bundle bundle = new Bundle();
                                        String url = Uri.decode(imageAnchorUrlExtra);
                                        bundle.putBoolean("isAdding", false);
                                        bundle.putString("url", url);
                                        bundle.putString("name", getNameFromUrl(url));
                                        intent.putExtra("websites",bundle);
                                        mActivity.startActivityForResult(intent, SITE_NAVIGATION_ADD_DIALOG);
                                        return false;
                                    }
                                });
                                //Delete menu
                                menu.findItem(R.id.delete_sn_context_menu_id)
                                    .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        showSiteNavigationDeleteDialog(Uri.decode(imageAnchorUrlExtra));
                                        return false;
                                    }
                               });
                            }
                            
                        }else{
                            Xlog.e(LOGTAG, "sitenavigation onCreateContextMenu itemUrl is null!");
                        }
                    }
                }//@
                //====================================site navigation modification end==================================
                
                if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                    break;
                }
                // otherwise fall through to handle image part
            case WebView.HitTestResult.IMAGE_TYPE:
                if (type == WebView.HitTestResult.IMAGE_TYPE) {
                    menu.setHeaderTitle(extra);
                }
                menu.findItem(R.id.view_image_context_menu_id)
                        .setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        openTab(extra, mTabControl.getCurrentTab(), true, true);
                        return false;
                    }
                });
                menu.findItem(R.id.download_context_menu_id).
                        setOnMenuItemClickListener(
                                new Download(mActivity, extra, webview.isPrivateBrowsingEnabled()));
                menu.findItem(R.id.set_wallpaper_context_menu_id).
                        setOnMenuItemClickListener(new WallpaperHandler(mActivity,
                                extra));
                break;

            default:
                Log.w(LOGTAG, "We should not get here.");
                break;
        }
        //update the ui
        mUi.onContextMenuCreated(menu);
    }
    
    //====================================site navigation modification start==================================
    //M: @ add for site navigation
    private void showSiteNavigationDeleteDialog(final String itemUrl){
        int title = R.string.delete;
        int msg = R.string.delete_site_navigation_msg;
        new AlertDialog.Builder(this.getContext())
            .setTitle(title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(msg)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    deleteSiteNavigationItem(itemUrl);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    
    private void deleteSiteNavigationItem(final String itemUrl){
        ContentResolver cr = this.getContext().getContentResolver();
        Cursor cursor = null;
        try {
            cursor = cr.query(SiteNavigation.SITE_NAVIGATION_URI, new String[] {SiteNavigation._ID}, "url = ?", new String[] {itemUrl}, null);
            if (null != cursor && true == cursor.moveToFirst()) {
                Uri uri = ContentUris.withAppendedId(SiteNavigation.SITE_NAVIGATION_URI, cursor.getLong(0));
                
                ContentValues values = new ContentValues();
                values.put(SiteNavigation.TITLE, ABOUT_BLANK_URL);
                values.put(SiteNavigation.URL, ABOUT_BLANK_URL + cursor.getLong(0));
                values.put(SiteNavigation.WEBSITE, 0 + "");
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Bitmap bm = BitmapFactory.decodeResource(this.getContext().getResources(), R.raw.sitenavigation_thumbnail_default);
                bm.compress(Bitmap.CompressFormat.PNG, 100, os);
                values.put(SiteNavigation.THUMBNAIL, os.toByteArray());
                Xlog.d(XLOGTAG, "Controller deleteSiteNavigationItem uri is : " + uri);
                cr.update(uri, values, null, null);
            }else{
                Xlog.e(LOGTAG, "deleteSiteNavigationItem the item does not exist!");
            }
        } catch (IllegalStateException e) {
            Xlog.e(LOGTAG, "deleteSiteNavigationItem", e);
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        
        //After delete, reload the page
		if (getCurrentTopWebView()!= null) {
	        getCurrentTopWebView().reload();
		}
    }
    
    private static final String ABOUT_BLANK_URL = "about:blank";
    public final static int SITE_NAVIGATION_ADD_DIALOG = 6;
    
    private String getNameFromUrl(String itemUrl){
        ContentResolver cr = this.getContext().getContentResolver();
        Cursor cursor = null;
        String name = null;
        try {
            cursor = cr.query(SiteNavigation.SITE_NAVIGATION_URI, new String[] {SiteNavigation.TITLE}, "url = ?", new String[] {itemUrl}, null);
            if (null != cursor && true == cursor.moveToFirst()) {
                name = cursor.getString(0);
            }else{
                Xlog.e(LOGTAG, "saveSiteNavigationItem the item does not exist!");
            }
        } catch (IllegalStateException e) {
            Xlog.e(LOGTAG, "saveSiteNavigationItem", e);
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        Xlog.d(XLOGTAG, "sitenavigation getNameFromUrl url is : " + itemUrl + ", will return name : " + name);
        return name;
    }
    
    
    private void updateSiteNavigationThumbnail(final String itemUrl, WebView webView){
        int width = mActivity.getResources().getDimensionPixelOffset(R.dimen.siteNavigationThumbnailWidth);
        int height = mActivity.getResources().getDimensionPixelOffset(R.dimen.siteNavigationThumbnailHeight);
        
        final Bitmap bm = createScreenshot(webView, width, height);
        
        if (bm == null) {
            Xlog.e(LOGTAG, "updateSiteNavigationThumbnail bm is null!");
            return;
        }

        final ContentResolver cr = mActivity.getContentResolver();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                ContentResolver cr = mActivity.getContentResolver();
                Cursor cursor = null;
                try {
                    cursor = cr.query(SiteNavigation.SITE_NAVIGATION_URI, new String[] {SiteNavigation._ID}, "url = ?", new String[] {itemUrl}, null);
                    if (null != cursor && true == cursor.moveToFirst()) {
                        final ByteArrayOutputStream os = new ByteArrayOutputStream();
                        bm.compress(Bitmap.CompressFormat.PNG, 100, os);

                        ContentValues values = new ContentValues();
                        values.put(SiteNavigation.THUMBNAIL, os.toByteArray());
                        Uri uri = ContentUris.withAppendedId(SiteNavigation.SITE_NAVIGATION_URI, cursor.getLong(0));
                        Xlog.d(XLOGTAG, "updateSiteNavigationThumbnail uri is : " + uri);
                        cr.update(uri, values, null, null);
                        os.close();
                    }
                } catch (IllegalStateException e) {
                    Xlog.e(LOGTAG, "updateSiteNavigationThumbnail", e);
                } catch(IOException e){
                    Xlog.e(LOGTAG, "updateSiteNavigationThumbnail", e);
                }finally {
                    if (null != cursor) {
                        cursor.close();
                    }
                }
                return null;
            }
        }.execute();
    }
    
    //about:blank+number
    private boolean isSiteNavigationAboutBlankUrl(String url){
        if(url != null && url.length() == 12 && url.startsWith(ABOUT_BLANK_URL)){
            String sub = url.substring(ABOUT_BLANK_URL.length());
            int index = Integer.valueOf(sub);
            if(0 <= index && index <=9){
                Xlog.d(XLOGTAG, "isSiteNavigationAboutBlankUrl will return true.");
                return true;
            }
        }
        
        return false;
    }
    
    //@
    //====================================site navigation modification end==================================

    //M: comment it because we have seprate the comb menu
//    /**
//     * As the menu can be open when loading state changes
//     * we must manually update the state of the stop/reload menu
//     * item
//     */
//    private void updateInLoadMenuItems(Menu menu, Tab tab) {
//        if (menu == null) {
//            return;
//        }
//        MenuItem dest = menu.findItem(R.id.stop_reload_menu_id);
//        MenuItem src = ((tab != null) && tab.inPageLoad()) ?
//                menu.findItem(R.id.stop_menu_id):
//                menu.findItem(R.id.reload_menu_id);
//        if (src != null) {
//            dest.setIcon(src.getIcon());
//            dest.setTitle(src.getTitle());
//        }
//    }

    /**
     * As the title do not contains url share page item should show disabled
     */
    private void updateShareMenuItems(Menu menu, Tab tab) {
        Xlog.d(XLOGTAG, "updateShareMenuItems start");
        if (menu == null) {
            return;
        }
        String url;
        MenuItem shareItem = menu.findItem(R.id.share_page_menu_id);
        if(tab == null) {
            Xlog.d(XLOGTAG, "tab == null");
            shareItem.setEnabled(false);   
        } else {
            url = tab.getUrl();          
            if (url == null || url.length() == 0) {
                Xlog.d(XLOGTAG, "url == null||url.length() == 0");
                shareItem.setEnabled(false);
            } else {
                Xlog.d(XLOGTAG, "url :" + url);
                shareItem.setEnabled(true);
            }
        }
        Xlog.d(XLOGTAG, "updateShareMenuItems end");
    }

    boolean onPrepareOptionsMenu(Menu menu) {
        //M: comment it because we have seprate the comb menu
//        updateInLoadMenuItems(menu, getCurrentTab());
    	
        // hold on to the menu reference here; it is used by the page callbacks
        // to update the menu based on loading state

        // As the title do not contains url share page item should show disabled
		if (getCurrentTab() != null) {
	        updateShareMenuItems(menu, getCurrentTab());
		}
        mCachedMenu = menu;
        // Note: setVisible will decide whether an item is visible; while
        // setEnabled() will decide whether an item is enabled, which also means
        // whether the matching shortcut key will function.
        switch (mMenuState) {
            case EMPTY_MENU:
                if (mCurrentMenuState != mMenuState) {
                    menu.setGroupVisible(R.id.MAIN_MENU, false);
                    menu.setGroupEnabled(R.id.MAIN_MENU, false);
                    menu.setGroupEnabled(R.id.MAIN_SHORTCUT_MENU, false);
                }
                break;
            default:
                if (mCurrentMenuState != mMenuState) {
                    menu.setGroupVisible(R.id.MAIN_MENU, true);
                    menu.setGroupEnabled(R.id.MAIN_MENU, true);
                    menu.setGroupEnabled(R.id.MAIN_SHORTCUT_MENU, true);
                }
                updateMenuState(getCurrentTab(), menu);
                break;
        }
        mCurrentMenuState = mMenuState;
        return mUi.onPrepareOptionsMenu(menu);
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
        boolean canGoBack = false;
        boolean canGoForward = false;
        boolean isHome = false;
        boolean isDesktopUa = false;
        boolean isLive = false;
        if (tab != null) {
            canGoBack = tab.canGoBack();
            canGoForward = tab.canGoForward();
            isHome = mSettings.getHomePage().equals(tab.getUrl());
            isDesktopUa = mSettings.hasDesktopUseragent(tab.getWebView());
            isLive = !tab.isSnapshot();
        }
        final MenuItem back = menu.findItem(R.id.back_menu_id);
        back.setEnabled(canGoBack);

        final MenuItem home = menu.findItem(R.id.homepage_menu_id);
        home.setEnabled(!isHome);

        final MenuItem forward = menu.findItem(R.id.forward_menu_id);
        forward.setEnabled(canGoForward);

        //M: comment it because we have seprate the comb menu
//        final MenuItem source = menu.findItem(isInLoad() ? R.id.stop_menu_id
//                : R.id.reload_menu_id);
//        final MenuItem dest = menu.findItem(R.id.stop_reload_menu_id);
//        if (source != null && dest != null) {
//            dest.setTitle(source.getTitle());
//            dest.setIcon(source.getIcon());
//        }
        final MenuItem stop = menu.findItem(R.id.stop_menu_id);
        stop.setEnabled(isInLoad());
        
        menu.setGroupVisible(R.id.NAV_MENU, isLive);

        // decide whether to show the share link option
        PackageManager pm = mActivity.getPackageManager();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        ResolveInfo ri = pm.resolveActivity(send,
                PackageManager.MATCH_DEFAULT_ONLY);
        menu.findItem(R.id.share_page_menu_id).setVisible(ri != null);

        boolean isNavDump = mSettings.enableNavDump();
        final MenuItem nav = menu.findItem(R.id.dump_nav_menu_id);
        nav.setVisible(isNavDump);
        nav.setEnabled(isNavDump);

        boolean showDebugSettings = mSettings.isDebugEnabled();
        final MenuItem counter = menu.findItem(R.id.dump_counters_menu_id);
        counter.setVisible(showDebugSettings);
        counter.setEnabled(showDebugSettings);
        final MenuItem uaSwitcher = menu.findItem(R.id.ua_desktop_menu_id);
        uaSwitcher.setChecked(isDesktopUa);
        menu.setGroupVisible(R.id.LIVE_MENU, isLive);
        menu.setGroupVisible(R.id.SNAPSHOT_MENU, !isLive);
        menu.setGroupVisible(R.id.COMBO_MENU, false);

        mUi.updateMenuState(tab, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (null == getCurrentTopWebView()) {
            return false;
        }
        if (mMenuIsDown) {
            // The shortcut action consumes the MENU. Even if it is still down,
            // it won't trigger the next shortcut action. In the case of the
            // shortcut action triggering a new activity, like Bookmarks, we
            // won't get onKeyUp for MENU. So it is important to reset it here.
            mMenuIsDown = false;
        }
        if (mUi.onOptionsItemSelected(item)) {
            // ui callback handled it
            return true;
        }
        switch (item.getItemId()) {
            // -- Main menu
            case R.id.new_tab_menu_id:
                openTabToHomePage();
                break;

            case R.id.incognito_menu_id:
                openIncognitoTab();
                break;

            case R.id.goto_menu_id:
                editUrl();
                break;

            case R.id.bookmarks_menu_id:
                bookmarksOrHistoryPicker(ComboViews.Bookmarks);
                break;

            case R.id.history_menu_id:
                bookmarksOrHistoryPicker(ComboViews.History);
                break;

            case R.id.snapshots_menu_id:
                bookmarksOrHistoryPicker(ComboViews.Snapshots);
                break;

            case R.id.add_bookmark_menu_id:
                Intent bookmarkIntent = createBookmarkCurrentPageIntent(false);
                if (bookmarkIntent != null) {
                    mActivity.startActivity(bookmarkIntent);
                }
                break;

              //M: comment it because we have seprate the comb menu
//            case R.id.stop_reload_menu_id:
//                if (isInLoad()) {
//                    stopLoading();
//                } else {
//                    getCurrentTopWebView().reload();
//                }
//                break;
                
            case R.id.reload_menu_id:
		if (getCurrentTopWebView() != null) {
            		getCurrentTopWebView().reload();
            	}
                break;
                
            case R.id.stop_menu_id:
                stopLoading();
                break;

            case R.id.back_menu_id:
                getCurrentTab().goBack();
                break;

            case R.id.forward_menu_id:
                getCurrentTab().goForward();
                break;

            case R.id.close_menu_id:
                // Close the subwindow if it exists.
                if (mTabControl.getCurrentSubWindow() != null) {
                    dismissSubWindow(mTabControl.getCurrentTab());
                    break;
                }
                closeCurrentTab();
                break;

            case R.id.homepage_menu_id:
            //M: Normal option menu item for "Home", not shortcut
            case R.id.home_menu_id:
                Tab current = mTabControl.getCurrentTab();
                loadUrl(current, mSettings.getHomePage());
                break;

            case R.id.preferences_menu_id:
                Intent intent = new Intent(mActivity, BrowserPreferencesPage.class);
                intent.putExtra(BrowserPreferencesPage.CURRENT_PAGE,
                        getCurrentTopWebView().getUrl());
                mActivity.startActivityForResult(intent, PREFERENCES_PAGE);
                break;

            case R.id.find_menu_id:
                getCurrentTopWebView().showFindDialog(null, true);
                break;

            case R.id.save_snapshot_menu_id:
                final Tab source = getTabControl().getCurrentTab();
                if (source == null) break;
                final ContentResolver cr = mActivity.getContentResolver();
                final ContentValues values = source.createSnapshotValues();
                if (values != null) {
                    new AsyncTask<Tab, Void, Long>() {

                        @Override
                        protected Long doInBackground(Tab... params) {
                            Uri result = cr.insert(Snapshots.CONTENT_URI, values);
                            if (result == null) {
                                return null;
                            }
                            long id = ContentUris.parseId(result);
                            return id;
                        }

                        @Override
                        protected void onPostExecute(Long id) {
                            if (id == null) {
                                Toast.makeText(mActivity, R.string.snapshot_failed,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Bundle b = new Bundle();
                            b.putLong(BrowserSnapshotPage.EXTRA_ANIMATE_ID, id);
                            mUi.showComboView(ComboViews.Snapshots, b);
                        };
                    }.execute(source);
                } else {
                    Toast.makeText(mActivity, R.string.snapshot_failed,
                            Toast.LENGTH_SHORT).show();
                }
                break;

            //M: Normal option menu item for browser "Close", not tab close, not shortcut
            case R.id.close_browser_menu_id:
                showCloseSelectionDialog();
                break;
                
            case R.id.page_info_menu_id:
                showPageInfo();
                break;

            case R.id.snapshot_go_live:
                goLive();
                return true;

            case R.id.share_page_menu_id:
                Tab currentTab = mTabControl.getCurrentTab();
                if (null == currentTab) {
                    return false;
                }
                shareCurrentPage(currentTab);
                break;

            case R.id.dump_nav_menu_id:
                getCurrentTopWebView().debugDump();
                break;

            case R.id.dump_counters_menu_id:
                getCurrentTopWebView().dumpV8Counters();
                break;

            case R.id.zoom_in_menu_id:
                getCurrentTopWebView().zoomIn();
                break;

            case R.id.zoom_out_menu_id:
                getCurrentTopWebView().zoomOut();
                break;

            case R.id.view_downloads_menu_id:
                viewDownloads();
                break;

            case R.id.ua_desktop_menu_id:
                WebView web = getCurrentWebView();
                mSettings.toggleDesktopUseragent(web);
                web.loadUrl(web.getOriginalUrl());
                break;

            case R.id.window_one_menu_id:
            case R.id.window_two_menu_id:
            case R.id.window_three_menu_id:
            case R.id.window_four_menu_id:
            case R.id.window_five_menu_id:
            case R.id.window_six_menu_id:
            case R.id.window_seven_menu_id:
            case R.id.window_eight_menu_id:
                {
                    int menuid = item.getItemId();
                    for (int id = 0; id < WINDOW_SHORTCUT_ID_ARRAY.length; id++) {
                        if (WINDOW_SHORTCUT_ID_ARRAY[id] == menuid) {
                            Tab desiredTab = mTabControl.getTab(id);
                            if (desiredTab != null &&
                                    desiredTab != mTabControl.getCurrentTab()) {
                                switchToTab(desiredTab);
                            }
                            break;
                        }
                    }
                }
                break;

            default:
                return false;
        }
        return true;
    }

    private void goLive() {
        Tab t = getCurrentTab();
        t.loadUrl(t.getUrl(), null);
    }
    
    //M: Function added for option "Close" menu
    private void showCloseSelectionDialog(){
        CharSequence[] items = new CharSequence[2];
        items[0] = mActivity.getString(R.string.minimize);
        items[1] = mActivity.getString(R.string.quit);
        new AlertDialog.Builder(mActivity)
        .setTitle(R.string.option)
        .setItems(items, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0){
                    mActivity.moveTaskToBack(true);
                }else if(which == 1){
                    mActivity.finish();
                    mCrashRecoveryHandler.clearState();
                    int pid = android.os.Process.myPid();
                    android.os.Process.killProcess(pid);
                }
            }
        })
        .show();
    }

    @Override
    public void showPageInfo() {
        mPageDialogsHandler.showPageInfo(mTabControl.getCurrentTab(), false, null);
    }

    public boolean onContextItemSelected(MenuItem item) {
        // Let the History and Bookmark fragments handle menus they created.
        if (item.getGroupId() == R.id.CONTEXT_MENU) {
            return false;
        }

        int id = item.getItemId();
        boolean result = true;
        switch (id) {
            // -- Browser context menu
            case R.id.open_context_menu_id:
            case R.id.save_link_context_menu_id:
            case R.id.copy_link_context_menu_id:
            case R.id.save_link_tobookmark_context_menu_id:
                final WebView webView = getCurrentTopWebView();
                if (null == webView) {
                    result = false;
                    break;
                }
                final HashMap<String, WebView> hrefMap =
                        new HashMap<String, WebView>();
                hrefMap.put("webview", webView);
                final Message msg = mHandler.obtainMessage(
                        FOCUS_NODE_HREF, id, 0, hrefMap);
                webView.requestFocusNodeHref(msg);
                break;

            default:
                // For other context menus
                result = onOptionsItemSelected(item);
        }
        return result;
    }

    /**
     * support programmatically opening the context menu
     */
    public void openContextMenu(View view) {
        mActivity.openContextMenu(view);
    }

    /**
     * programmatically open the options menu
     */
    public void openOptionsMenu() {
        mActivity.openOptionsMenu();
    }

    public boolean onMenuOpened(int featureId, Menu menu) {
        if (mOptionsMenuOpen) {
            if (mConfigChanged) {
                // We do not need to make any changes to the state of the
                // title bar, since the only thing that happened was a
                // change in orientation
                mConfigChanged = false;
            } else {
                if (!mExtendedMenuOpen) {
                    mExtendedMenuOpen = true;
                    mUi.onExtendedMenuOpened();
                } else {
                    // Switching the menu back to icon view, so show the
                    // title bar once again.
                    mExtendedMenuOpen = false;
                    mUi.onExtendedMenuClosed(isInLoad());
                }
            }
        } else {
            // The options menu is closed, so open it, and show the title
            mOptionsMenuOpen = true;
            mConfigChanged = false;
            mExtendedMenuOpen = false;
            mUi.onOptionsMenuOpened();
        }
        return true;
    }

    public void onOptionsMenuClosed(Menu menu) {
        mOptionsMenuOpen = false;
        mUi.onOptionsMenuClosed(isInLoad());
    }

    public void onContextMenuClosed(Menu menu) {
        mUi.onContextMenuClosed(menu, isInLoad());
    }

    // Helper method for getting the top window.
    @Override
    public WebView getCurrentTopWebView() {
        return mTabControl.getCurrentTopWebView();
    }

    @Override
    public WebView getCurrentWebView() {
        return mTabControl.getCurrentWebView();
    }

    /*
     * This method is called as a result of the user selecting the options
     * menu to see the download window. It shows the download window on top of
     * the current window.
     */
    void viewDownloads() {
        Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        mActivity.startActivity(intent);
    }

    int getActionModeHeight() {
        TypedArray actionBarSizeTypedArray = mActivity.obtainStyledAttributes(
                    new int[] { android.R.attr.actionBarSize });
        int size = (int) actionBarSizeTypedArray.getDimension(0, 0f);
        actionBarSizeTypedArray.recycle();
        return size;
    }

    // action mode

    void onActionModeStarted(ActionMode mode) {
        mUi.onActionModeStarted(mode);
        mActionMode = mode;
    }

    /*
     * True if a custom ActionMode (i.e. find or select) is in use.
     */
    @Override
    public boolean isInCustomActionMode() {
        return mActionMode != null;
    }

    /*
     * End the current ActionMode.
     */
    @Override
    public void endActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    /*
     * Called by find and select when they are finished.  Replace title bars
     * as necessary.
     */
    public void onActionModeFinished(ActionMode mode) {
        if (!isInCustomActionMode()) return;
        mUi.onActionModeFinished(isInLoad());
        mActionMode = null;
    }

    boolean isInLoad() {
        final Tab tab = getCurrentTab();
        return (tab != null) && tab.inPageLoad();
    }

    // bookmark handling

    /**
     * add the current page as a bookmark to the given folder id
     * @param folderId use -1 for the default folder
     * @param editExisting If true, check to see whether the site is already
     *          bookmarked, and if it is, edit that bookmark.  If false, and
     *          the site is already bookmarked, do not attempt to edit the
     *          existing bookmark.
     */
    @Override
    public Intent createBookmarkCurrentPageIntent(boolean editExisting) {
        WebView w = getCurrentTopWebView();
        if (w == null) {
            return null;
        }
        Intent i = new Intent(mActivity,
                AddBookmarkPage.class);
        i.putExtra(BrowserContract.Bookmarks.URL, w.getUrl());
        i.putExtra(BrowserContract.Bookmarks.TITLE, w.getTitle());
        String touchIconUrl = w.getTouchIconUrl();
        if (touchIconUrl != null) {
            i.putExtra(AddBookmarkPage.TOUCH_ICON_URL, touchIconUrl);
            WebSettings settings = w.getSettings();
            if (settings != null) {
                i.putExtra(AddBookmarkPage.USER_AGENT,
                        settings.getUserAgentString());
            }
        }
        i.putExtra(BrowserContract.Bookmarks.THUMBNAIL,
                createScreenshot(w, getDesiredThumbnailWidth(mActivity),
                getDesiredThumbnailHeight(mActivity)));
        i.putExtra(BrowserContract.Bookmarks.FAVICON, w.getFavicon());
        if (editExisting) {
            i.putExtra(AddBookmarkPage.CHECK_FOR_DUPE, true);
        }
        // Put the dialog at the upper right of the screen, covering the
        // star on the title bar.
        i.putExtra("gravity", Gravity.RIGHT | Gravity.TOP);
        return i;
    }
    
    // add link to bookmark handling, copy form createBookmarkCurrentPageIntent

    /**
     * add the current page as a bookmark to the given folder id
     * @param folderId use -1 for the default folder
     * @param editExisting If true, check to see whether the site is already
     *          bookmarked, and if it is, edit that bookmark.  If false, and
     *          the site is already bookmarked, do not attempt to edit the
     *          existing bookmark.
     */
    public Intent createBookmarkLinkIntent(String url) {
        WebView w = getCurrentTopWebView();
        if (w == null) {
            return null;
        }
        Intent i = new Intent(mActivity,
                AddBookmarkPage.class);
        i.putExtra(BrowserContract.Bookmarks.URL, url);
        i.putExtra(BrowserContract.Bookmarks.TITLE, "");
        String touchIconUrl = w.getTouchIconUrl();
        if (touchIconUrl != null) {
            i.putExtra(AddBookmarkPage.TOUCH_ICON_URL, touchIconUrl);
            WebSettings settings = w.getSettings();
            if (settings != null) {
                i.putExtra(AddBookmarkPage.USER_AGENT,
                        settings.getUserAgentString());
            }
        }
        i.putExtra(BrowserContract.Bookmarks.THUMBNAIL,
                createScreenshot(w, getDesiredThumbnailWidth(mActivity),
                getDesiredThumbnailHeight(mActivity)));
        i.putExtra(BrowserContract.Bookmarks.FAVICON, w.getFavicon());
        // Put the dialog at the upper right of the screen, covering the
        // star on the title bar.
        i.putExtra("gravity", Gravity.RIGHT | Gravity.TOP);
        return i;
    }    

    // file chooser
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        mUploadHandler = new UploadHandler(this);
        mUploadHandler.openFileChooser(uploadMsg, acceptType);
    }

    // thumbnails

    /**
     * Return the desired width for thumbnail screenshots, which are stored in
     * the database, and used on the bookmarks screen.
     * @param context Context for finding out the density of the screen.
     * @return desired width for thumbnail screenshot.
     */
    static int getDesiredThumbnailWidth(Context context) {
        return context.getResources().getDimensionPixelOffset(
                R.dimen.bookmarkThumbnailWidth);
    }

    /**
     * Return the desired height for thumbnail screenshots, which are stored in
     * the database, and used on the bookmarks screen.
     * @param context Context for finding out the density of the screen.
     * @return desired height for thumbnail screenshot.
     */
    static int getDesiredThumbnailHeight(Context context) {
        return context.getResources().getDimensionPixelOffset(
                R.dimen.bookmarkThumbnailHeight);
    }

    static Bitmap createScreenshot(WebView view, int width, int height) {
        if (view == null || view.getContentHeight() == 0
                || view.getContentWidth() == 0) {
            return null;
        }
        // We render to a bitmap 2x the desired size so that we can then
        // re-scale it with filtering since canvas.scale doesn't filter
        // This helps reduce aliasing at the cost of being slightly blurry
        final int filter_scale = 2;
        int scaledWidth = width * filter_scale;
        int scaledHeight = height * filter_scale;
        if (sThumbnailBitmap == null || sThumbnailBitmap.getWidth() != scaledWidth
                || sThumbnailBitmap.getHeight() != scaledHeight) {
            if (sThumbnailBitmap != null) {
                sThumbnailBitmap.recycle();
                sThumbnailBitmap = null;
            }
            sThumbnailBitmap =
                    Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.RGB_565);
        }
        Canvas canvas = new Canvas(sThumbnailBitmap);
        int contentWidth = view.getContentWidth();
        float overviewScale = scaledWidth / (view.getScale() * contentWidth);
        if (view instanceof BrowserWebView) {
            int dy = -((BrowserWebView)view).getTitleHeight();
            canvas.translate(0, dy * overviewScale);
        }

        canvas.scale(overviewScale, overviewScale);

        if (view instanceof BrowserWebView) {
            ((BrowserWebView)view).drawContent(canvas);
        } else {
            view.draw(canvas);
        }
        Bitmap ret = Bitmap.createScaledBitmap(sThumbnailBitmap,
                width, height, true);
        canvas.setBitmap(null);
        return ret;
    }

    private void updateScreenshot(Tab tab) {
        // If this is a bookmarked site, add a screenshot to the database.
        // FIXME: Would like to make sure there is actually something to
        // draw, but the API for that (WebViewCore.pictureReady()) is not
        // currently accessible here.

        WebView view = tab.getWebView();
        if (view == null) {
            // Tab was destroyed
            return;
        }
        final String url = tab.getUrl();
        final String originalUrl = view.getOriginalUrl();
        final String thumbnailUrl = mUrlNeedUpdateThumbnail;

        if (TextUtils.isEmpty(url)) {
            return;
        }
        
        //@ M: update site navigation thumbnail
        if (BrowserFeatureOption.BROWSER_SITE_NAVIGATION_SUPPORT && Patterns.WEB_URL.matcher(url).matches()) {
            // Check if it a url existed in site navigation
            boolean isSNUrl = SiteNavigationAddDialog.isSiteNavigationUrl(mActivity, url);
            if(isSNUrl == true){
                updateSiteNavigationThumbnail(url, view);
            }
        }//@

        // Only update thumbnails for web urls (http(s)://), not for
        // about:, javascript:, data:, etc...
        // Unless it is a bookmarked site, then always update
        if (!Patterns.WEB_URL.matcher(url).matches() && !tab.isBookmarkedSite()) {
            return;
        }

        final Bitmap bm = createScreenshot(view, getDesiredThumbnailWidth(mActivity),
                getDesiredThumbnailHeight(mActivity));
        if (bm == null) {
            return;
        }
        
//        //M: Save thumbnail pictures for customized preset bookmarks
//        //Do not remove it and keep it as comment for forcoming use
//        try {
//            saveThumbnailBitmapToFile(System.currentTimeMillis() + "", bm);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        final ContentResolver cr = mActivity.getContentResolver();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                Cursor cursor = null;
                try {
                    // TODO: Clean this up
                    cursor = Bookmarks.queryCombinedForUrl(cr, originalUrl, 
                    		mNeedUpdateThumbnail? ((thumbnailUrl != null)? thumbnailUrl : url) : url);
                    if (mNeedUpdateThumbnail) {
                        mNeedUpdateThumbnail = false;
                        mUrlNeedUpdateThumbnail = null;
                    }
                    if (cursor != null && cursor.moveToFirst()) {
                        final ByteArrayOutputStream os =
                                new ByteArrayOutputStream();
                        bm.compress(Bitmap.CompressFormat.PNG, 100, os);

                        ContentValues values = new ContentValues();
                        values.put(Images.THUMBNAIL, os.toByteArray());

                        do {
                            values.put(Images.URL, cursor.getString(0));
                            cr.update(Images.CONTENT_URI, values, null, null);
                        } while (cursor.moveToNext());
                    }
                } catch (IllegalStateException e) {
                    // Ignore
                } finally {
                    if (cursor != null) cursor.close();
                }
                return null;
            }
        }.execute();
    }
    
//    //M: If you need preset some bookmarks, you should prepare the thumbnail pictures for them
//    //This function is used to save the picture to sdcard and then you can pull it out and use it
//    //This functin will be called when the page loading is finished
//    //This function should be used with saveFavoriteBitmapToFile
//    public void saveThumbnailBitmapToFile(String bitName, Bitmap bitmap) throws IOException {
//        try {
//            File f = new File("/sdcard/Pictures/" + bitName + ".png");
//            f.createNewFile();
//            FileOutputStream fOut = null;
//            fOut = new FileOutputStream(f);
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
//            fOut.flush();
//            fOut.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private class Copy implements OnMenuItemClickListener {
        private CharSequence mText;

        public boolean onMenuItemClick(MenuItem item) {
            copy(mText);
            return true;
        }

        public Copy(CharSequence toCopy) {
            mText = toCopy;
        }
    }

    private static class Download implements OnMenuItemClickListener {
        private Activity mActivity;
        private String mText;
        private boolean mPrivateBrowsing;
        private static final String FALLBACK_EXTENSION = "dat";
        private static final String IMAGE_BASE_FORMAT = "yyyy-MM-dd-HH-mm-ss-";

        public boolean onMenuItemClick(MenuItem item) {
            if (DataUri.isDataUri(mText)) {
                saveDataUri();
            } else {
                DownloadHandler.onDownloadStartNoStream(mActivity, mText, null,
                        // null, null, mPrivateBrowsing);
                        null, null, mPrivateBrowsing, 0);
            }
            return true;
        }

        public Download(Activity activity, String toDownload, boolean privateBrowsing) {
            mActivity = activity;
            mText = toDownload;
            mPrivateBrowsing = privateBrowsing;
        }

        /**
         * Treats mText as a data URI and writes its contents to a file
         * based on the current time.
         */
        private void saveDataUri() {
            FileOutputStream outputStream = null;
            try {
                DataUri uri = new DataUri(mText);
                File target = getTarget(uri);
                outputStream = new FileOutputStream(target);
                outputStream.write(uri.getData());
                final DownloadManager manager =
                        (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
                 manager.addCompletedDownload(target.getName(),
                        mActivity.getTitle().toString(), false,
                        uri.getMimeType(), target.getAbsolutePath(),
                        (long)uri.getData().length, false);
            } catch (IOException e) {
                Log.e(LOGTAG, "Could not save data URL");
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        // ignore close errors
                    }
                }
            }
        }

        /**
         * Creates a File based on the current time stamp and uses
         * the mime type of the DataUri to get the extension.
         */
        private File getTarget(DataUri uri) throws IOException {
            File dir = mActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            DateFormat format = new SimpleDateFormat(IMAGE_BASE_FORMAT);
            String nameBase = format.format(new Date());
            String mimeType = uri.getMimeType();
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String extension = mimeTypeMap.getExtensionFromMimeType(mimeType);
            if (extension == null) {
                Log.w(LOGTAG, "Unknown mime type in data URI" + mimeType);
                extension = FALLBACK_EXTENSION;
            }
            extension = "." + extension; // createTempFile needs the '.'
            File targetFile = File.createTempFile(nameBase, extension, dir);
            return targetFile;
        }
    }

    private static class SelectText implements OnMenuItemClickListener {
        private WebView mWebView;

        public boolean onMenuItemClick(MenuItem item) {
            if (mWebView != null) {
                return mWebView.selectText();
            }
            return false;
        }

        public SelectText(WebView webView) {
            mWebView = webView;
        }

    }

    /********************** TODO: UI stuff *****************************/

    // these methods have been copied, they still need to be cleaned up

    /****************** tabs ***************************************************/

    // basic tab interactions:

    // it is assumed that tabcontrol already knows about the tab
    protected void addTab(Tab tab) {
        mUi.addTab(tab);
    }

    protected void removeTab(Tab tab) {
        mUi.removeTab(tab);
        mTabControl.removeTab(tab);
        mCrashRecoveryHandler.backupState();
    }

    @Override
    public void setActiveTab(Tab tab) {
        // monkey protection against delayed start
        if (tab != null) {
            mTabControl.setCurrentTab(tab);
            // the tab is guaranteed to have a webview after setCurrentTab
            mUi.setActiveTab(tab);
        }
    }

    protected void closeEmptyTab() {
        Tab current = mTabControl.getCurrentTab();
        if (current != null
                && current.getWebView().copyBackForwardList().getSize() == 0) {
            closeCurrentTab();
        }
    }

    protected void reuseTab(Tab appTab, UrlData urlData) {
        // Dismiss the subwindow if applicable.
        dismissSubWindow(appTab);
        // Since we might kill the WebView, remove it from the
        // content view first.
        mUi.detachTab(appTab);
        // Recreate the main WebView after destroying the old one.
        mTabControl.recreateWebView(appTab);
        // TODO: analyze why the remove and add are necessary
        mUi.attachTab(appTab);
        if (mTabControl.getCurrentTab() != appTab) {
            switchToTab(appTab);
            loadUrlDataIn(appTab, urlData);
        } else {
            // If the tab was the current tab, we have to attach
            // it to the view system again.
            setActiveTab(appTab);
            loadUrlDataIn(appTab, urlData);
        }
    }

    // Remove the sub window if it exists. Also called by TabControl when the
    // user clicks the 'X' to dismiss a sub window.
    public void dismissSubWindow(Tab tab) {
        removeSubWindow(tab);
        // dismiss the subwindow. This will destroy the WebView.
        tab.dismissSubWindow();
        WebView wv = getCurrentTopWebView();
        if (wv != null) {
            wv.requestFocus();
        }
    }

    @Override
    public void removeSubWindow(Tab t) {
        if (t.getSubWebView() != null) {
            mUi.removeSubWindow(t.getSubViewContainer());
        }
    }

    @Override
    public void attachSubWindow(Tab tab) {
        if (tab.getSubWebView() != null) {
            mUi.attachSubWindow(tab.getSubViewContainer());
            getCurrentTopWebView().requestFocus();
        }
    }

    private Tab showPreloadedTab(final UrlData urlData) {
        if (!urlData.isPreloaded()) {
            return null;
        }
        final PreloadedTabControl tabControl = urlData.getPreloadedTab();
        final String sbQuery = urlData.getSearchBoxQueryToSubmit();
        if (sbQuery != null) {
            if (!tabControl.searchBoxSubmit(sbQuery, urlData.mUrl, urlData.mHeaders)) {
                // Could not submit query. Fallback to regular tab creation
                tabControl.destroy();
                return null;
            }
        }
        // check tab count and make room for new tab
        if (!mTabControl.canCreateNewTab()) {
            Tab leastUsed = mTabControl.getLeastUsedTab(getCurrentTab());
            if (leastUsed != null) {
                closeTab(leastUsed);
            }
        }
        Tab t = tabControl.getTab();
        t.refreshIdAfterPreload();
        mTabControl.addPreloadedTab(t);
        addTab(t);
        setActiveTab(t);
        return t;
    }

    // open a non inconito tab with the given url data
    // and set as active tab
    public Tab openTab(UrlData urlData) {
        Tab tab = showPreloadedTab(urlData);
        if (tab == null) {
            tab = createNewTab(false, true, true);
            if ((tab != null) && !urlData.isEmpty()) {
                loadUrlDataIn(tab, urlData);
            }
        }
        return tab;
    }

    @Override
    public Tab openTabToHomePage() {
        return openTab(mSettings.getHomePage(), false, true, false);
    }

    @Override
    public Tab openIncognitoTab() {
        return openTab(INCOGNITO_URI, true, true, false);
    }

    @Override
    public Tab openTab(String url, boolean incognito, boolean setActive,
            boolean useCurrent) {
        return openTab(url, incognito, setActive, useCurrent, null);
    }

    @Override
    public Tab openTab(String url, Tab parent, boolean setActive,
            boolean useCurrent) {
        return openTab(url, (parent != null) && parent.isPrivateBrowsingEnabled(),
                setActive, useCurrent, parent);
    }

    public Tab openTab(String url, boolean incognito, boolean setActive,
            boolean useCurrent, Tab parent) {
        Tab tab = createNewTab(incognito, setActive, useCurrent);
        if (tab != null) {
            if (parent != null && parent != tab) {
                parent.addChildTab(tab);
            }
            if (url != null) {
                loadUrl(tab, url);
            }
        }
        return tab;
    }

    // this method will attempt to create a new tab
    // incognito: private browsing tab
    // setActive: ste tab as current tab
    // useCurrent: if no new tab can be created, return current tab
    private Tab createNewTab(boolean incognito, boolean setActive,
            boolean useCurrent) {
        Tab tab = null;
        if (mTabControl.canCreateNewTab()) {
            tab = mTabControl.createNewTab(incognito);
            addTab(tab);
            if (setActive) {
                setActiveTab(tab);
            }
        } else {
            if (useCurrent) {
                tab = mTabControl.getCurrentTab();
                reuseTab(tab, null);
            } else {
                mUi.showMaxTabsWarning();
            }
        }
        return tab;
    }

    @Override
    public SnapshotTab createNewSnapshotTab(long snapshotId, boolean setActive) {
        SnapshotTab tab = null;
        if (mTabControl.canCreateNewTab()) {
            tab = mTabControl.createSnapshotTab(snapshotId);
            addTab(tab);
            if (setActive) {
                setActiveTab(tab);
            }
        } else {
            mUi.showMaxTabsWarning();
        }
        return tab;
    }

    /**
     * @param tab the tab to switch to
     * @return boolean True if we successfully switched to a different tab.  If
     *                 the indexth tab is null, or if that tab is the same as
     *                 the current one, return false.
     */
    @Override
    public boolean switchToTab(Tab tab) {
        Tab currentTab = mTabControl.getCurrentTab();
        if (tab == null || tab == currentTab) {
            return false;
        }
        setActiveTab(tab);
        return true;
    }

    @Override
    public void closeCurrentTab() {
        closeCurrentTab(false);
    }

    protected void closeCurrentTab(boolean andQuit) {
        if (mTabControl.getTabCount() == 1) {
            mCrashRecoveryHandler.clearState();
            mTabControl.removeTab(getCurrentTab());
            mActivity.finish();
            return;
        }
        final Tab current = mTabControl.getCurrentTab();
        final int pos = mTabControl.getCurrentPosition();
        Tab newTab = current.getParent();
        if (newTab == null) {
            newTab = mTabControl.getTab(pos + 1);
            if (newTab == null) {
                newTab = mTabControl.getTab(pos - 1);
            }
        }
        if (andQuit) {
            mTabControl.setCurrentTab(newTab);
            closeTab(current);
        } else if (switchToTab(newTab)) {
            // Close window
            closeTab(current);
        }
    }

    /**
     * Close the tab, remove its associated title bar, and adjust mTabControl's
     * current tab to a valid value.
     */
    @Override
    public void closeTab(Tab tab) {
        if (tab == mTabControl.getCurrentTab()) {
            closeCurrentTab();
        } else {
            removeTab(tab);
        }
    }

    // Called when loading from context menu or LOAD_URL message
    protected void loadUrlFromContext(String url) {
        Tab tab = getCurrentTab();
        WebView view = tab != null ? tab.getWebView() : null;
        // In case the user enters nothing.
        if (url != null && url.length() != 0 && tab != null && view != null) {
            url = UrlUtils.smartUrlFilter(url);
            if (!view.getWebViewClient().shouldOverrideUrlLoading(view, url)) {
                loadUrl(tab, url);
            }
        }
    }

    /**
     * Load the URL into the given WebView and update the title bar
     * to reflect the new load.  Call this instead of WebView.loadUrl
     * directly.
     * @param view The WebView used to load url.
     * @param url The URL to load.
     */
    @Override
    public void loadUrl(Tab tab, String url) {
        loadUrl(tab, url, null);
    }

    protected void loadUrl(Tab tab, String url, Map<String, String> headers) {
        if (tab != null) {
            dismissSubWindow(tab);
            tab.loadUrl(url, headers);
            mUi.onProgressChanged(tab);
        }
    }

    /**
     * Load UrlData into a Tab and update the title bar to reflect the new
     * load.  Call this instead of UrlData.loadIn directly.
     * @param t The Tab used to load.
     * @param data The UrlData being loaded.
     */
    protected void loadUrlDataIn(Tab t, UrlData data) {
        if (data != null) {
            if (data.mVoiceIntent != null) {
                t.activateVoiceSearchMode(data.mVoiceIntent);
            } else if (data.isPreloaded()) {
                // this isn't called for preloaded tabs
            } else {
                loadUrl(t, data.mUrl, data.mHeaders);
            }
        }
    }

    @Override
    public void onUserCanceledSsl(Tab tab) {
        // TODO: Figure out the "right" behavior
        if (tab.canGoBack()) {
            tab.goBack();
        } else {
            tab.loadUrl(mSettings.getHomePage(), null);
        }
    }

    void goBackOnePageOrQuit() {
        Tab current = mTabControl.getCurrentTab();
        if (current == null) {
            /*
             * Instead of finishing the activity, simply push this to the back
             * of the stack and let ActivityManager to choose the foreground
             * activity. As BrowserActivity is singleTask, it will be always the
             * root of the task. So we can use either true or false for
             * moveTaskToBack().
             */
            mActivity.moveTaskToBack(true);
            return;
        }
        if (current.canGoBack()) {
            current.goBack();
        } else {
            // Check to see if we are closing a window that was created by
            // another window. If so, we switch back to that window.
            Tab parent = current.getParent();
            if (parent != null) {
                switchToTab(parent);
                // Now we close the other tab
                closeTab(current);
            } else {
                if ((current.getAppId() != null) || current.closeOnBack()) {
                    closeCurrentTab(true);
                }
                /*
                 * Instead of finishing the activity, simply push this to the back
                 * of the stack and let ActivityManager to choose the foreground
                 * activity. As BrowserActivity is singleTask, it will be always the
                 * root of the task. So we can use either true or false for
                 * moveTaskToBack().
                 */
                mActivity.moveTaskToBack(true);
            }
        }
    }

    /**
     * Feed the previously stored results strings to the BrowserProvider so that
     * the SearchDialog will show them instead of the standard searches.
     * @param result String to show on the editable line of the SearchDialog.
     */
    @Override
    public void showVoiceSearchResults(String result) {
        ContentProviderClient client = mActivity.getContentResolver()
                .acquireContentProviderClient(Browser.BOOKMARKS_URI);
        ContentProvider prov = client.getLocalContentProvider();
        BrowserProvider bp = (BrowserProvider) prov;
        bp.setQueryResults(mTabControl.getCurrentTab().getVoiceSearchResults());
        client.release();

        Bundle bundle = createGoogleSearchSourceBundle(
                GOOGLE_SEARCH_SOURCE_SEARCHKEY);
        bundle.putBoolean(SearchManager.CONTEXT_IS_VOICE, true);
        startSearch(result, false, bundle, false);
    }

    private void startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch) {
        if (appSearchData == null) {
            appSearchData = createGoogleSearchSourceBundle(
                    GOOGLE_SEARCH_SOURCE_TYPE);
        }

        SearchEngine searchEngine = mSettings.getSearchEngine();
        if (searchEngine != null && !searchEngine.supportsVoiceSearch()) {
            appSearchData.putBoolean(SearchManager.DISABLE_VOICE_SEARCH, true);
        }
        mActivity.startSearch(initialQuery, selectInitialQuery, appSearchData,
                globalSearch);
    }

    private Bundle createGoogleSearchSourceBundle(String source) {
        Bundle bundle = new Bundle();
        bundle.putString(Search.SOURCE, source);
        return bundle;
    }

    /**
     * helper method for key handler
     * returns the current tab if it can't advance
     */
    private Tab getNextTab() {
        return mTabControl.getTab(Math.min(mTabControl.getTabCount() - 1,
                mTabControl.getCurrentPosition() + 1));
    }

    /**
     * helper method for key handler
     * returns the current tab if it can't advance
     */
    private Tab getPrevTab() {
        return  mTabControl.getTab(Math.max(0,
                mTabControl.getCurrentPosition() - 1));
    }

    /**
     * handle key events in browser
     *
     * @param keyCode
     * @param event
     * @return true if handled, false to pass to super
     */
    boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean noModifiers = event.hasNoModifiers();
        // Even if MENU is already held down, we need to call to super to open
        // the IME on long press.
        if (!noModifiers
                && ((KeyEvent.KEYCODE_MENU == keyCode)
                        || (KeyEvent.KEYCODE_CTRL_LEFT == keyCode)
                        || (KeyEvent.KEYCODE_CTRL_RIGHT == keyCode))) {
            mMenuIsDown = true;
            return false;
        }

        WebView webView = getCurrentTopWebView();
        Tab tab = getCurrentTab();
        if (webView == null || tab == null) return false;

        boolean ctrl = event.hasModifiers(KeyEvent.META_CTRL_ON);
        boolean shift = event.hasModifiers(KeyEvent.META_SHIFT_ON);

        switch(keyCode) {
            case KeyEvent.KEYCODE_TAB:
                if (event.isCtrlPressed()) {
                    if (event.isShiftPressed()) {
                        // prev tab
                        switchToTab(getPrevTab());
                    } else {
                        // next tab
                        switchToTab(getNextTab());
                    }
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_SPACE:
                // WebView/WebTextView handle the keys in the KeyDown. As
                // the Activity's shortcut keys are only handled when WebView
                // doesn't, have to do it in onKeyDown instead of onKeyUp.
                if (shift) {
                    pageUp();
                } else if (noModifiers) {
                    pageDown();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (!noModifiers) break;
                event.startTracking();
                return true;
            case KeyEvent.KEYCODE_FORWARD:
                if (!noModifiers) break;
                tab.goForward();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (ctrl) {
                    tab.goBack();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (ctrl) {
                    tab.goForward();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_A:
                if (ctrl) {
                    webView.selectAll();
                    return true;
                }
                break;
//          case KeyEvent.KEYCODE_B:    // menu
            case KeyEvent.KEYCODE_C:
                if (ctrl) {
                    webView.copySelection();
                    return true;
                }
                break;
//          case KeyEvent.KEYCODE_D:    // menu
//          case KeyEvent.KEYCODE_E:    // in Chrome: puts '?' in URL bar
//          case KeyEvent.KEYCODE_F:    // menu
//          case KeyEvent.KEYCODE_G:    // in Chrome: finds next match
//          case KeyEvent.KEYCODE_H:    // menu
//          case KeyEvent.KEYCODE_I:    // unused
//          case KeyEvent.KEYCODE_J:    // menu
//          case KeyEvent.KEYCODE_K:    // in Chrome: puts '?' in URL bar
//          case KeyEvent.KEYCODE_L:    // menu
//          case KeyEvent.KEYCODE_M:    // unused
//          case KeyEvent.KEYCODE_N:    // in Chrome: new window
//          case KeyEvent.KEYCODE_O:    // in Chrome: open file
//          case KeyEvent.KEYCODE_P:    // in Chrome: print page
//          case KeyEvent.KEYCODE_Q:    // unused
//          case KeyEvent.KEYCODE_R:
//          case KeyEvent.KEYCODE_S:    // in Chrome: saves page
            case KeyEvent.KEYCODE_T:
                // we can't use the ctrl/shift flags, they check for
                // exclusive use of a modifier
                if (event.isCtrlPressed()) {
                    if (event.isShiftPressed()) {
                        openIncognitoTab();
                    } else {
                        openTabToHomePage();
                    }
                    return true;
                }
                break;
//          case KeyEvent.KEYCODE_U:    // in Chrome: opens source of page
//          case KeyEvent.KEYCODE_V:    // text view intercepts to paste
//          case KeyEvent.KEYCODE_W:    // menu
//          case KeyEvent.KEYCODE_X:    // text view intercepts to cut
//          case KeyEvent.KEYCODE_Y:    // unused
//          case KeyEvent.KEYCODE_Z:    // unused
        }
        // it is a regular key and webview is not null
         return mUi.dispatchKey(keyCode, event);
    }

    boolean onKeyLongPress(int keyCode, KeyEvent event) {
        switch(keyCode) {
        case KeyEvent.KEYCODE_BACK:
            if (mUi.isWebShowing()) {
                bookmarksOrHistoryPicker(ComboViews.History);
                return true;
            }
            break;
        }
        return false;
    }

    boolean onKeyUp(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_MENU == keyCode) {
            mMenuIsDown = false;
            if (event.isTracking() && !event.isCanceled()) {
                return onMenuKey();
            }
        }
        if (!event.hasNoModifiers()) return false;
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.isTracking() && !event.isCanceled()) {
                    onBackKey();
                    return true;
                }
                break;
        }
        return false;
    }

    public boolean isMenuDown() {
        return mMenuIsDown;
    }

    public void setupAutoFill(Message message) {
        // Open the settings activity at the AutoFill profile fragment so that
        // the user can create a new profile. When they return, we will dispatch
        // the message so that we can autofill the form using their new profile.
        Intent intent = new Intent(mActivity, BrowserPreferencesPage.class);
        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                AutoFillSettingsFragment.class.getName());
        mAutoFillSetupMessage = message;
        mActivity.startActivityForResult(intent, AUTOFILL_SETUP);
    }

    public boolean onSearchRequested() {
        mUi.editUrl(false);
        return true;
    }

    @Override
    public boolean shouldCaptureThumbnails() {
        return mUi.shouldCaptureThumbnails();
    }

    @Override
    public void setBlockEvents(boolean block) {
        mBlockEvents = block;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return mBlockEvents;
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return mBlockEvents;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mBlockEvents;
    }

    public boolean dispatchTrackballEvent(MotionEvent ev) {
        return mBlockEvents;
    }

    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        return mBlockEvents;
    }

}
