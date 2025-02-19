/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.music;

import com.android.music.MusicUtils.ServiceToken;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Adapter;
import android.widget.AlphabetIndexer;
import android.widget.CursorAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;

import java.text.Collator;

//import com.mediatek.featureoption.FeatureOption;

public class AlbumBrowserActivity extends ListActivity
    implements View.OnCreateContextMenuListener, MusicUtils.Defs, ServiceConnection
{
    private static final String TAG = "Album";
    private String mCurrentAlbumId;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    boolean mIsUnknownArtist;
    boolean mIsUnknownAlbum;
    private AlbumListAdapter mAdapter;
    private boolean mAdapterSent;
    private final static int SEARCH = CHILD_MENU_BASE;
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;
    private ServiceToken mToken;
    
    // reset SD card status to refresh database error UI
    private boolean mResetSdStatus = false;
    private boolean mIsMounted = true;
    
    private FakeMenu mFakeMenu = null;
    private boolean mWithtabs = false;
    
    // Add search view
    private SearchView mSearchView;
    private MenuItem mSearchItem;
    
    private IMediaPlaybackService mService = null;

    public AlbumBrowserActivity()
    {
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        if (icicle != null) {
            mCurrentAlbumId = icicle.getString("selectedalbum");
            mArtistId = icicle.getString("artist");
        } else {
            // ALPS00122166
            //mArtistId = getIntent().getStringExtra("artist");
            
            mCurrentAlbumId = intent.getStringExtra("selectedalbum");
            mArtistId = intent.getStringExtra("artist");
        }
        mWithtabs = intent.getBooleanExtra("withtabs", false);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        if(mWithtabs) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else {
            //Set the action bar on the right to be up navigation
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mToken = MusicUtils.bindToService(this, this);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addAction(Intent.ACTION_MEDIA_EJECT);
        f.addAction(Intent.ACTION_MEDIA_MOUNTED);
        f.addDataScheme("file");
        registerReceiver(mScanListener, f);

        setContentView(R.layout.media_picker_activity);
        
        LinearLayout mainLayout = (LinearLayout)findViewById(R.id.mainLayout);
        /*
        if (FeatureOption.MTK_THEMEMANAGER_APP) {
            mainLayout.setThemeContentBgColor(0xff000000);
        }
        */
        if(mWithtabs) {
            mFakeMenu =  new FakeMenu(this);
        }
        
        MusicUtils.updateButtonBar(this, R.id.albumtab);
        ListView lv = getListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);

        mAdapter = (AlbumListAdapter) getLastNonConfigurationInstance();
        if (mAdapter == null) {
            MusicLogUtils.v(TAG, "starting query");
            mAdapter = new AlbumListAdapter(
                    getApplication(),
                    this,
                    R.layout.track_list_item,
                    mAlbumCursor,
                    new String[] {},
                    new int[] {});
            setListAdapter(mAdapter);
            setTitle(R.string.working_albums);
            getAlbumCursor(mAdapter.getQueryHandler(), null);
        } else {
            mAdapter.setActivity(this);
            mAdapter.reloadStringOnLocaleChanges();
            setListAdapter(mAdapter);
            mAlbumCursor = mAdapter.getCursor();
            if (mAlbumCursor != null) {
                init(mAlbumCursor);
            } else {
                getAlbumCursor(mAdapter.getQueryHandler(), null);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);

        if (mFakeMenu != null) {
            mFakeMenu.snapshotFakeMenu();
        }
        
        // If showing database error UI, need not to refresh UI.
        if ((findViewById(R.id.sd_icon).getVisibility()) == View.VISIBLE) {
            MusicLogUtils.d(TAG, "Configuration Changed at database error, return!");
            return;
        }
        setContentView(R.layout.media_picker_activity);
        
        if (mFakeMenu != null) {
            mFakeMenu.changeFakeMenu();
        }
        
        MusicUtils.updateButtonBar(this, R.id.albumtab);
        if (mService != null) {
            MusicUtils.updateNowPlaying(this);
        }
        ListView lv = getListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);

    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mAdapterSent = true;
        return mAdapter;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putString("selectedalbum", mCurrentAlbumId);
        outcicle.putString("artist", mArtistId);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() {
        ListView lv = getListView();
        if (lv != null) {
            mLastListPosCourse = lv.getFirstVisiblePosition();
            View cv = lv.getChildAt(0);
            if (cv != null) {
                mLastListPosFine = cv.getTop();
            }
        }
        MusicUtils.unbindFromService(mToken);
        mService = null;
        mFakeMenu = null;
        // If we have an adapter and didn't send it off to another activity yet, we should
        // close its cursor, which we do by assigning a null cursor to it. Doing this
        // instead of closing the cursor directly keeps the framework from accessing
        // the closed cursor later.
        if (!mAdapterSent && mAdapter != null) {
            mAdapter.changeCursor(null);
        }
        // Because we pass the adapter to the next activity, we need to make
        // sure it doesn't keep a reference to this activity. We can do this
        // by clearing its DatasetObservers, which setListAdapter(null) does.
        setListAdapter(null);
        mAdapter = null;
        unregisterReceiver(mScanListener);
        super.onDestroy();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        registerReceiver(mTrackListListener, f);
        mTrackListListener.onReceive(null, null);

        MusicUtils.setSpinnerState(this);

    }

    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getListView().invalidateViews();
            if (mService != null) {
                MusicUtils.updateNowPlaying(AlbumBrowserActivity.this);
            }
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String status = Environment.getExternalStorageState();
            MusicLogUtils.d(TAG, "mScanListener.onReceive:" + action + ", status = " + status);
            if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action) ||
                    Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                MusicUtils.setSpinnerState(AlbumBrowserActivity.this);
                mReScanHandler.sendEmptyMessage(0);
            } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                // Avoid to rescan database when SD card is bad removed.Just rescan database
                // when SD card is normal removed.
                mIsMounted = false;
                if (!Environment.MEDIA_BAD_REMOVAL.equals(status)) {
                    mResetSdStatus = true;
                    mReScanHandler.sendEmptyMessage(0);
                    mReScanHandler.sendEmptyMessageDelayed(0, 1000);
                }
            } else if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                // If SD card is bad removed, send message to rescan database and send the message
                // again to make sure the database has been dispatch when we rescan database
                mIsMounted = false;
                if (Environment.MEDIA_BAD_REMOVAL.equals(status)) {
                    mReScanHandler.sendEmptyMessage(0);
                    closeContextMenu();
                    closeOptionsMenu();
                    mReScanHandler.sendEmptyMessageDelayed(0, 1000);
                    MusicLogUtils.d(TAG, "SD card is bad removed, show database error UI!");
                }
            } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                mIsMounted = true;
                mReScanHandler.sendEmptyMessage(0);
            }  // if MEDIA_MOUNTED ends
    }
    };
    
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mAdapter != null) {
                getAlbumCursor(mAdapter.getQueryHandler(), null);
            }
        }
    };

    @Override
    public void onPause() {
        unregisterReceiver(mTrackListListener);
        mReScanHandler.removeCallbacksAndMessages(null);
        //ALPS00122166
        setIntent(new Intent(getIntent()).putExtra("selectedalbum", mCurrentAlbumId));
        super.onPause();
    }

    public void init(Cursor c) {

        if (mAdapter == null) {
            return;
        }
        mAdapter.changeCursor(c); // also sets mAlbumCursor

        if (mAlbumCursor == null) {
            MusicLogUtils.d(TAG, "mAlbumCursor is null");
            MusicUtils.displayDatabaseError(this,mIsMounted);
            closeContextMenu();
            mReScanHandler.sendEmptyMessageDelayed(0, 1000);
            return;
        }

        // restore previous position
        if (mLastListPosCourse >= 0) {
            getListView().setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
            mLastListPosCourse = -1;
        }
        
        if (mFakeMenu != null) {
            mFakeMenu.snapshotFakeMenu();
        }
        
        MusicUtils.hideDatabaseError(this);
        setContentView(R.layout.media_picker_activity);
        
        if (mFakeMenu != null) {
            mFakeMenu.changeFakeMenu();
        }
        
        MusicUtils.updateButtonBar(this, R.id.albumtab);
        setTitle();
        if (mService != null) {
            MusicUtils.updateNowPlaying(this);
        }
        ListView lv = getListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);
    }

    private void setTitle() {
        CharSequence fancyName = "";
        if (mAlbumCursor != null && mAlbumCursor.getCount() > 0) {
            mAlbumCursor.moveToFirst();
            fancyName = mAlbumCursor.getString(
                    mAlbumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST));
            if (fancyName == null || fancyName.equals(MediaStore.UNKNOWN_STRING))
                fancyName = getText(R.string.unknown_artist_name);
        }

        if (mArtistId != null && fancyName != null)
            setTitle(fancyName);
        else
            setTitle(R.string.albums_title);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, sub);
        menu.add(0, DELETE_ITEM, 0, R.string.delete_item);

        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;
        mAlbumCursor.moveToPosition(mi.position);
        mCurrentAlbumId = mAlbumCursor.getString(mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
        mCurrentAlbumName = mAlbumCursor.getString(mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
        mCurrentArtistNameForAlbum = mAlbumCursor.getString(
                mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
        mIsUnknownArtist = mCurrentArtistNameForAlbum == null ||
                mCurrentArtistNameForAlbum.equals(MediaStore.UNKNOWN_STRING);
        mIsUnknownAlbum = mCurrentAlbumName == null ||
                mCurrentAlbumName.equals(MediaStore.UNKNOWN_STRING);
        if (mIsUnknownAlbum) {
            menu.setHeaderTitle(getString(R.string.unknown_album_name));
        } else {
            menu.setHeaderTitle(mCurrentAlbumName);
        }
        if (!mIsUnknownAlbum || !mIsUnknownArtist) {
            menu.add(0, SEARCH, 0, R.string.search_title);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION: {
                // play the selected album
                long [] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                MusicUtils.playAll(this, list, 0);
                return true;
            }

            case QUEUE: {
                long [] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                MusicUtils.addToCurrentPlaylist(this, list);
                return true;
            }

            case NEW_PLAYLIST: {
                Intent intent = new Intent();
                intent.setClass(this, CreatePlaylist.class);
                startActivityForResult(intent, NEW_PLAYLIST);
                return true;
            }

            case PLAYLIST_SELECTED: {
                long [] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                long playlist = item.getIntent().getLongExtra("playlist", 0);
                MusicUtils.addToPlaylist(this, list, playlist);
                return true;
            }
            case DELETE_ITEM: {
                long [] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                String f;
                //if (android.os.Environment.isExternalStorageRemovable()) {
                    f = getString(R.string.delete_album_desc);
                //} else {
                //    f = getString(R.string.delete_album_desc_nosdcard);
                //}
                String desc = String.format(f, mCurrentAlbumName);
                Bundle b = new Bundle();
                b.putString("description", desc);
                b.putLongArray("items", list);
                Intent intent = new Intent();
                intent.setClass(this, DeleteItems.class);
                intent.putExtras(b);
                startActivityForResult(intent, -1);
                return true;
            }
            case SEARCH:
                doSearch();
                return true;

        }
        return super.onContextItemSelected(item);
    }

    void doSearch() {
        CharSequence title = null;
        String query = "";
        
        Intent i = new Intent();
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        title = "";
        if (!mIsUnknownAlbum) {
            query = mCurrentAlbumName;
            i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
            title = mCurrentAlbumName;
        }
        if(!mIsUnknownArtist) {
            query = query + " " + mCurrentArtistNameForAlbum;
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
            title = title + " " + mCurrentArtistNameForAlbum;
        }
        // Since we hide the 'search' menu item when both album and artist are
        // unknown, the query and title strings will have at least one of those.
        i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE);
        title = getString(R.string.mediasearch, title);
        i.putExtra(SearchManager.QUERY, query);

        startActivity(Intent.createChooser(i, title));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case SCAN_DONE:
                if (resultCode == RESULT_CANCELED) {
                    finish();
                } else {
                    getAlbumCursor(mAdapter.getQueryHandler(), null);
                }
                break;

            case NEW_PLAYLIST:
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        long [] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                        MusicUtils.addToPlaylist(this, list, Long.parseLong(uri.getLastPathSegment()));
                    }
                }
                break;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent.putExtra("album", Long.valueOf(id).toString());
        intent.putExtra("artist", mArtistId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle); // icon will be set in onPrepareOptionsMenu()
        menu.add(0, SHUFFLE_ALL, 0, R.string.shuffle_all).setIcon(R.drawable.ic_menu_shuffle);
        menu.add(0, EFFECTS_PANEL, 0, R.string.effects_list_title).setIcon(R.drawable.ic_menu_eq);
        // Add search view
        getMenuInflater().inflate(R.menu.music_search_menu, menu);
        mSearchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) mSearchItem.getActionView();

        mSearchView.setOnQueryTextListener(mQueryTextListener);
        mSearchView.setQueryHint(getString(R.string.search_hint));
        mSearchView.setIconifiedByDefault(true);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        if (searchManager != null) {
            SearchableInfo info = searchManager.getSearchableInfo(this.getComponentName());
            mSearchView.setSearchableInfo(info);
        }
        return true;
    }

    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        public boolean onQueryTextSubmit(String query) {
            Intent intent = new Intent();
            intent.setClass(AlbumBrowserActivity.this, QueryBrowserActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            return true;
        }

        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MusicUtils.setPartyShuffleMenuIcon(menu);
        
        boolean isEffectMenuVisible = true;
        Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        if (getPackageManager().resolveActivity(i, 0) == null) {
            isEffectMenuVisible = false;
        }
        menu.findItem(EFFECTS_PANEL).setVisible(isEffectMenuVisible);
        
        //if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
        if (mAlbumCursor == null) {
            MusicLogUtils.v(TAG, "Album cursor is null, need not show option menu: mAlbumCursor = " + mAlbumCursor);
            return false;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        Cursor cursor;
        switch (item.getItemId()) {
            case android.R.id.home:
                // Navigation button press back
                onBackPressed();
                break;
                
            case PARTY_SHUFFLE:
                MusicUtils.togglePartyShuffle();
                break;

            case SHUFFLE_ALL:
                cursor = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String [] { MediaStore.Audio.Media._ID},
                        MediaStore.Audio.Media.IS_MUSIC + "=1", null,
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                if (cursor != null) {
                    MusicUtils.shuffleAll(this, cursor);
                    cursor.close();
                }
                return true;
            case EFFECTS_PANEL:
                try {
                    Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                    i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mService.getAudioSessionId());
                    startActivityForResult(i, EFFECTS_PANEL);
                } catch (RemoteException re) {
                }
                return true;
                
            case R.id.search:
                onSearchRequested();
                return true;
                
        }
        return super.onOptionsItemSelected(item);
    }

    private Cursor getAlbumCursor(AsyncQueryHandler async, String filter) {
        String[] cols = new String[] {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_ART
        };


        Cursor ret = null;
        if (mArtistId != null) {
            Uri uri = MediaStore.Audio.Artists.Albums.getContentUri("external",
                    Long.valueOf(mArtistId));
            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
            }
            if (async != null) {
                async.startQuery(0, null, uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            } else {
                ret = MusicUtils.query(this, uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            }
        } else {
            Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
            }
            if (async != null) {
                async.startQuery(0, null,
                        uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            } else {
                ret = MusicUtils.query(this, uri,
                        cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            }
        }
        return ret;
    }
    
    static class AlbumListAdapter extends SimpleCursorAdapter implements SectionIndexer {
        
        private final Drawable mNowPlayingOverlay;
        private final BitmapDrawable mDefaultAlbumIcon;
        private int mAlbumIdx;
        private int mArtistIdx;
        private int mAlbumArtIndex;
        private final Resources mResources;
        private final StringBuilder mStringBuilder = new StringBuilder();
        private String mUnknownAlbum;
        private String mUnknownArtist;
        private final String mAlbumSongSeparator;
        private final Object[] mFormatArgs = new Object[1];
        private AlphabetIndexer mIndexer;
        private AlbumBrowserActivity mActivity;
        private AsyncQueryHandler mQueryHandler;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        
        static class ViewHolder {
            TextView line1;
            TextView line2;
            ImageView play_indicator;
            ImageView icon;
        }

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver res) {
                super(res);
            }
            
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                MusicLogUtils.v(TAG, "query complete");
                mActivity.init(cursor);
            }
        }

        AlbumListAdapter(Context context, AlbumBrowserActivity currentactivity,
                int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);

            mActivity = currentactivity;
            mQueryHandler = new QueryHandler(context.getContentResolver());
            
            mUnknownAlbum = context.getString(R.string.unknown_album_name);
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
            mAlbumSongSeparator = context.getString(R.string.albumsongseparator);

            Resources r = context.getResources();
            mNowPlayingOverlay = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);

            Bitmap b = BitmapFactory.decodeResource(r, R.drawable.albumart_mp_unknown_list);
            mDefaultAlbumIcon = new BitmapDrawable(context.getResources(), b);
            // no filter or dither, it's a lot faster and we can't tell the difference
            mDefaultAlbumIcon.setFilterBitmap(false);
            mDefaultAlbumIcon.setDither(false);
            getColumnIndices(cursor);
            mResources = context.getResources();
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                mAlbumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
                mArtistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST);
                mAlbumArtIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART);
                
                if (mIndexer != null) {
                    mIndexer.setCursor(cursor);
                } else {
                    mIndexer = new MusicAlphabetIndexer(cursor, mAlbumIdx, mResources.getString(
                            R.string.fast_scroll_alphabet));
                }
            }
        }
        
        public void reloadStringOnLocaleChanges() {
            String sUnknownArtist = mActivity.getString(R.string.unknown_artist_name);
            String sUnknownAlbum = mActivity.getString(R.string.unknown_album_name);
            if (mUnknownArtist != null && !mUnknownArtist.equals(sUnknownArtist)) {
                mUnknownArtist = sUnknownArtist;
            }
            if (mUnknownAlbum != null && !mUnknownAlbum.equals(sUnknownAlbum)) {
                mUnknownAlbum = sUnknownAlbum;
            }
        }
        
        public void setActivity(AlbumBrowserActivity newactivity) {
            mActivity = newactivity;
        }
        
        public AsyncQueryHandler getQueryHandler() {
            return mQueryHandler;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
           View v = super.newView(context, cursor, parent);
           ViewHolder vh = new ViewHolder();
           vh.line1 = (TextView) v.findViewById(R.id.line1);
           vh.line2 = (TextView) v.findViewById(R.id.line2);
           vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
           vh.icon = (ImageView) v.findViewById(R.id.icon);
           vh.icon.setBackgroundDrawable(mDefaultAlbumIcon);
           vh.icon.setPadding(0, 0, 1, 0);
           v.setTag(vh);
           return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            ViewHolder vh = (ViewHolder) view.getTag();

            String name = cursor.getString(mAlbumIdx);
            String displayname = name;
            boolean unknown = name == null || name.equals(MediaStore.UNKNOWN_STRING); 
            if (unknown) {
                displayname = mUnknownAlbum;
            }
            vh.line1.setText(displayname);
            
            name = cursor.getString(mArtistIdx);
            displayname = name;
            if (name == null || name.equals(MediaStore.UNKNOWN_STRING)) {
                displayname = mUnknownArtist;
            }
            vh.line2.setText(displayname);

            ImageView iv = vh.icon;
            // We don't actually need the path to the thumbnail file,
            // we just use it to see if there is album art or not
            String art = cursor.getString(mAlbumArtIndex);
            long aid = cursor.getLong(0);
            if (unknown || art == null || art.length() == 0) {
                iv.setImageDrawable(null);
            } else {
                Drawable d = MusicUtils.getCachedArtwork(context, aid, mDefaultAlbumIcon);
                iv.setImageDrawable(d);
            }
            
            long currentalbumid = MusicUtils.getCurrentAlbumId();
            iv = vh.play_indicator;
            if (currentalbumid == aid) {
                iv.setImageDrawable(mNowPlayingOverlay);
            } else {
                iv.setImageDrawable(null);
            }
        }
        
        @Override
        public void changeCursor(Cursor cursor) {
            if (mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != mActivity.mAlbumCursor) {
                mActivity.mAlbumCursor = cursor;
                getColumnIndices(cursor);
                super.changeCursor(cursor);
            }
        }
        
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (mConstraintIsValid && (
                    (s == null && mConstraint == null) ||
                    (s != null && s.equals(mConstraint)))) {
                return getCursor();
            }
            Cursor c = mActivity.getAlbumCursor(null, s);
            mConstraint = s;
            mConstraintIsValid = true;
            return c;
        }
        
        public Object[] getSections() {
            return mIndexer.getSections();
        }
        
        public int getPositionForSection(int section) {
            if (mIndexer != null) {
                return mIndexer.getPositionForSection(section);
            }
            return 0;
        }
        
        public int getSectionForPosition(int position) {
            if (mIndexer != null) {
                return mIndexer.getSectionForPosition(position);
            }
            return 0;
        }
    }

    private Cursor mAlbumCursor;
    private String mArtistId;

    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IMediaPlaybackService.Stub.asInterface(service);
        MusicUtils.updateNowPlaying(this);
    }

    public void onServiceDisconnected(ComponentName name) {
        mService = null;
        finish();
    }
}

