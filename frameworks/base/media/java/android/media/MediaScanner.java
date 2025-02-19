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

package android.media;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.IContentProvider;
import android.database.Cursor;
import android.database.SQLException;
import android.drm.DrmManagerClient;
import android.graphics.BitmapFactory;
import android.mtp.MtpConstants;
import android.net.Uri;
import android.os.Environment;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Audio.Playlists;
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.RootElement;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.mediatek.audioprofile.AudioProfileManager;
import com.mediatek.mpo.MpoDecoder;
import com.mediatek.stereo3d.JpsParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Internal service helper that no-one should use directly.
 *
 * The way the scan currently works is:
 * - The Java MediaScannerService creates a MediaScanner (this class), and calls
 *   MediaScanner.scanDirectories on it.
 * - scanDirectories() calls the native processDirectory() for each of the specified directories.
 * - the processDirectory() JNI method wraps the provided mediascanner client in a native
 *   'MyMediaScannerClient' class, then calls processDirectory() on the native MediaScanner
 *   object (which got created when the Java MediaScanner was created).
 * - native MediaScanner.processDirectory() (currently part of opencore) calls
 *   doProcessDirectory(), which recurses over the folder, and calls
 *   native MyMediaScannerClient.scanFile() for every file whose extension matches.
 * - native MyMediaScannerClient.scanFile() calls back on Java MediaScannerClient.scanFile,
 *   which calls doScanFile, which after some setup calls back down to native code, calling
 *   MediaScanner.processFile().
 * - MediaScanner.processFile() calls one of several methods, depending on the type of the
 *   file: parseMP3, parseMP4, parseMidi, parseOgg or parseWMA.
 * - each of these methods gets metadata key/value pairs from the file, and repeatedly
 *   calls native MyMediaScannerClient.handleStringTag, which calls back up to its Java
 *   counterparts in this file.
 * - Java handleStringTag() gathers the key/value pairs that it's interested in.
 * - once processFile returns and we're back in Java code in doScanFile(), it calls
 *   Java MyMediaScannerClient.endFile(), which takes all the data that's been
 *   gathered and inserts an entry in to the database.
 *
 * In summary:
 * Java MediaScannerService calls
 * Java MediaScanner scanDirectories, which calls
 * Java MediaScanner processDirectory (native method), which calls
 * native MediaScanner processDirectory, which calls
 * native MyMediaScannerClient scanFile, which calls
 * Java MyMediaScannerClient scanFile, which calls
 * Java MediaScannerClient doScanFile, which calls
 * Java MediaScanner processFile (native method), which calls
 * native MediaScanner processFile, which calls
 * native parseMP3, parseMP4, parseMidi, parseOgg or parseWMA, which calls
 * native MyMediaScanner handleStringTag, which calls
 * Java MyMediaScanner handleStringTag.
 * Once MediaScanner processFile returns, an entry is inserted in to the database.
 *
 * The MediaScanner class is not thread-safe, so it should only be used in a single threaded manner.
 *
 * {@hide}
 */
public class MediaScanner
{
    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    private final static String TAG = "MediaScanner";
    private final static boolean LOG = true;

    private static final String[] FILES_PRESCAN_PROJECTION = new String[] {
            Files.FileColumns._ID, // 0
            Files.FileColumns.DATA, // 1
            Files.FileColumns.FORMAT, // 2
            Files.FileColumns.DATE_MODIFIED, // 3
    };

    private static final String[] ID_PROJECTION = new String[] {
            Files.FileColumns._ID,
    };

    private static final int FILES_PRESCAN_ID_COLUMN_INDEX = 0;
    private static final int FILES_PRESCAN_PATH_COLUMN_INDEX = 1;
    private static final int FILES_PRESCAN_FORMAT_COLUMN_INDEX = 2;
    private static final int FILES_PRESCAN_DATE_MODIFIED_COLUMN_INDEX = 3;

    private static final String[] PLAYLIST_MEMBERS_PROJECTION = new String[] {
            Audio.Playlists.Members.PLAYLIST_ID, // 0
     };

    private static final int ID_PLAYLISTS_COLUMN_INDEX = 0;
    private static final int PATH_PLAYLISTS_COLUMN_INDEX = 1;
    private static final int DATE_MODIFIED_PLAYLISTS_COLUMN_INDEX = 2;

    private static final String RINGTONES_DIR = "/ringtones/";
    private static final String NOTIFICATIONS_DIR = "/notifications/";
    private static final String ALARMS_DIR = "/alarms/";
    private static final String MUSIC_DIR = "/music/";
    private static final String PODCAST_DIR = "/podcasts/";

    private static final String[] ID3_GENRES = {
        // ID3v1 Genres
        "Blues",
        "Classic Rock",
        "Country",
        "Dance",
        "Disco",
        "Funk",
        "Grunge",
        "Hip-Hop",
        "Jazz",
        "Metal",
        "New Age",
        "Oldies",
        "Other",
        "Pop",
        "R&B",
        "Rap",
        "Reggae",
        "Rock",
        "Techno",
        "Industrial",
        "Alternative",
        "Ska",
        "Death Metal",
        "Pranks",
        "Soundtrack",
        "Euro-Techno",
        "Ambient",
        "Trip-Hop",
        "Vocal",
        "Jazz+Funk",
        "Fusion",
        "Trance",
        "Classical",
        "Instrumental",
        "Acid",
        "House",
        "Game",
        "Sound Clip",
        "Gospel",
        "Noise",
        "AlternRock",
        "Bass",
        "Soul",
        "Punk",
        "Space",
        "Meditative",
        "Instrumental Pop",
        "Instrumental Rock",
        "Ethnic",
        "Gothic",
        "Darkwave",
        "Techno-Industrial",
        "Electronic",
        "Pop-Folk",
        "Eurodance",
        "Dream",
        "Southern Rock",
        "Comedy",
        "Cult",
        "Gangsta",
        "Top 40",
        "Christian Rap",
        "Pop/Funk",
        "Jungle",
        "Native American",
        "Cabaret",
        "New Wave",
        "Psychadelic",
        "Rave",
        "Showtunes",
        "Trailer",
        "Lo-Fi",
        "Tribal",
        "Acid Punk",
        "Acid Jazz",
        "Polka",
        "Retro",
        "Musical",
        "Rock & Roll",
        "Hard Rock",
        // The following genres are Winamp extensions
        "Folk",
        "Folk-Rock",
        "National Folk",
        "Swing",
        "Fast Fusion",
        "Bebob",
        "Latin",
        "Revival",
        "Celtic",
        "Bluegrass",
        "Avantgarde",
        "Gothic Rock",
        "Progressive Rock",
        "Psychedelic Rock",
        "Symphonic Rock",
        "Slow Rock",
        "Big Band",
        "Chorus",
        "Easy Listening",
        "Acoustic",
        "Humour",
        "Speech",
        "Chanson",
        "Opera",
        "Chamber Music",
        "Sonata",
        "Symphony",
        "Booty Bass",
        "Primus",
        "Porn Groove",
        "Satire",
        "Slow Jam",
        "Club",
        "Tango",
        "Samba",
        "Folklore",
        "Ballad",
        "Power Ballad",
        "Rhythmic Soul",
        "Freestyle",
        "Duet",
        "Punk Rock",
        "Drum Solo",
        "A capella",
        "Euro-House",
        "Dance Hall",
        // The following ones seem to be fairly widely supported as well
        "Goa",
        "Drum & Bass",
        "Club-House",
        "Hardcore",
        "Terror",
        "Indie",
        "Britpop",
        "Negerpunk",
        "Polsk Punk",
        "Beat",
        "Christian Gangsta",
        "Heavy Metal",
        "Black Metal",
        "Crossover",
        "Contemporary Christian",
        "Christian Rock",
        "Merengue",
        "Salsa",
        "Thrash Metal",
        "Anime",
        "JPop",
        "Synthpop",
        // 148 and up don't seem to have been defined yet.
    };

    private int mNativeContext;
    private Context mContext;
    private int mContext1;
    private IContentProvider mMediaProvider;
    private Uri mAudioUri;
    private Uri mVideoUri;
    private Uri mImagesUri;
    private Uri mThumbsUri;
    private Uri mVideoThumbsUri;
    private Uri mPlaylistsUri;
    private Uri mFilesUri;
    private boolean mProcessPlaylists, mProcessGenres;
    private int mMtpObjectHandle;

    private final String mExternalStoragePath;

    // WARNING: Bulk inserts sounded like a great idea and gave us a good performance improvement,
    // but unfortunately it also introduced a number of bugs.  Many of those bugs were fixed,
    // but (at least) one problem is still outstanding:
    //
    // - Bulk inserts broke the code that sets the default ringtones, notifications, and alarms
    //   on first boot
    //
    // This problem might be solvable by moving the logic to the media provider or disabling bulk
    // inserts only for those cases. For now, we are disabling bulk inserts until we have a solid
    // fix for this problem.
    private static final boolean ENABLE_BULK_INSERTS = false;

    // used when scanning the image database so we know whether we have to prune
    // old thumbnail files
    private int mOriginalCount;
    //old video thumbnail files
    private int mOriginalVideoCount;
    /** Whether the database had any entries in it before the scan started */
    private boolean mWasEmptyPriorToScan = false;
    /** Whether the scanner has set a default sound for the ringer ringtone. */
    private boolean mDefaultRingtoneSet;
    /** Whether the scanner has set a default sound for the notification ringtone. */
    private boolean mDefaultNotificationSet;
    /** Whether the scanner has set a default sound for the alarm ringtone. */
    private boolean mDefaultAlarmSet;
    /** The filename for the default sound for the ringer ringtone. */
    private String mDefaultRingtoneFilename;
    /** The filename for the default sound for the notification ringtone. */
    private String mDefaultNotificationFilename;
    /** The filename for the default sound for the alarm ringtone. */
    private String mDefaultAlarmAlertFilename;
    /**
     * The prefix for system properties that define the default sound for
     * ringtones. Concatenate the name of the setting from Settings
     * to get the full system property.
     */
    private static final String DEFAULT_RINGTONE_PROPERTY_PREFIX = "ro.config.";

    // set to true if file path comparisons should be case insensitive.
    // this should be set when scanning files on a case insensitive file system.
    private boolean mCaseInsensitivePaths;

    private BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();

    private static class FileCacheEntry {
        long mRowId;
        String mPath;
        long mLastModified;
        int mFormat;
        boolean mSeenInFileSystem;
        boolean mLastModifiedChanged;

        FileCacheEntry(long rowId, String path, long lastModified, int format) {
            mRowId = rowId;
            mPath = path;
            mLastModified = lastModified;
            mFormat = format;
            mSeenInFileSystem = false;
            mLastModifiedChanged = false;
        }

        @Override
        public String toString() {
            return mPath + " mRowId: " + mRowId;
        }
    }

    private MediaInserter mMediaInserter;

    // hashes file path to FileCacheEntry.
    // path should be lower case if mCaseInsensitivePaths is true
    private HashMap<String, FileCacheEntry> mFileCache;

    private ArrayList<FileCacheEntry> mPlayLists;

    private DrmManagerClient mDrmManagerClient = null;

    public MediaScanner(Context c) {
        native_setup();
        mContext = c;
        mBitmapOptions.inSampleSize = 1;
        mBitmapOptions.inJustDecodeBounds = true;

        setDefaultRingtoneFileNames();

        mExternalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    private void setDefaultRingtoneFileNames() {
        mDefaultRingtoneFilename = SystemProperties.get(DEFAULT_RINGTONE_PROPERTY_PREFIX
                + Settings.System.RINGTONE);
        mDefaultNotificationFilename = SystemProperties.get(DEFAULT_RINGTONE_PROPERTY_PREFIX
                + Settings.System.NOTIFICATION_SOUND);
        mDefaultAlarmAlertFilename = SystemProperties.get(DEFAULT_RINGTONE_PROPERTY_PREFIX
                + Settings.System.ALARM_ALERT);
    }

    private MyMediaScannerClient mClient = new MyMediaScannerClient();

    private boolean isDrmEnabled() {
        String prop = SystemProperties.get("drm.service.enabled");
        return prop != null && prop.equals("true");
    }

    private class MyMediaScannerClient implements MediaScannerClient {

        private String mArtist;
        private String mAlbumArtist;    // use this if mArtist is missing
        private String mAlbum;
        private String mTitle;
        private String mComposer;
        private String mGenre;
        private String mMimeType;
        private int mFileType;
        private int mTrack;
        private int mYear;
        private int mDuration;
        private String mPath;
        private long mLastModified;
        private long mFileSize;
        private String mWriter;
        private int mIsAccurateDuration;
        private int mCompilation;
        private boolean mIsDrm;
        private boolean mNoMedia;   // flag to suppress file from appearing in media tables
        private int mWidth;
        private int mHeight;
        //add for mtk drm
        private String mDrmContentUr;
        private long mDrmOffset;
        private long mDrmDataLen;
        private String mDrmRightsIssuer;
        private String mDrmContentName;
        private String mDrmContentDescriptioin;
        private String mDrmContentVendor;
        private String mDrmIconUri;
        private long mDrmMethod;
        private int mStereoType;

        public FileCacheEntry beginFile(String path, String mimeType, long lastModified,
                long fileSize, boolean isDirectory, boolean noMedia) {
            mMimeType = mimeType;
            mFileType = 0;
            mFileSize = fileSize;

            if (!isDirectory) {
                if (!noMedia && isNoMediaFile(path)) {
                    noMedia = true;
                }
                mNoMedia = noMedia;

                // try mimeType first, if it is specified
                if (mimeType != null) {
                    mFileType = MediaFile.getFileTypeForMimeType(mimeType);
                }

                // OMA DRM v1: however, for those DCF file, normally when scanning it,
                // the input mime type should be "application/vnd.oma.drm.content";
                // however, there's case that the input mimetype is, for example, "image/*"
                // in these cases it will not call processFile() but processImageFile() instead.
                // for these cases, we change the {mFileType} back to ZERO,
                // and let Media.getFileType(path) to determine the type,
                // so that it can call processFile() as normal.
                if (MediaFile.isImageFileType(mFileType)) {
                    int lastDot = path.lastIndexOf(".");
                    if (lastDot > 0 && path.substring(lastDot + 1).toUpperCase().equals("DCF")) {
                        Log.d(TAG, "detect a *.DCF file with input mime type:"+mimeType);
                        mFileType = 0; // work around: change to ZERO
                    }
                }

                // if mimeType was not specified, compute file type based on file extension.
                if (mFileType == 0) {
                    MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
                    if (mediaFileType != null) {
                        mFileType = mediaFileType.fileType;
                        if (mMimeType == null || isValueslessMimeType(mMimeType)) {
                            mMimeType = mediaFileType.mimeType;
                        }
                    }
                }

                if (isDrmEnabled() && MediaFile.isDrmFileType(mFileType)) {
                    mFileType = getFileTypeFromDrm(path);
                }
            }

            String key = path;
            if (mCaseInsensitivePaths) {
                key = path.toLowerCase();
            }
            FileCacheEntry entry = mFileCache.get(key);
            // add some slack to avoid a rounding error
            long delta = (entry != null) ? (lastModified - entry.mLastModified) : 0;
            boolean wasModified = delta > 1 || delta < -1;
            if (entry == null || wasModified) {
                if (wasModified) {
                    entry.mLastModified = lastModified;
                } else {
                    entry = new FileCacheEntry(0, path, lastModified,
                            (isDirectory ? MtpConstants.FORMAT_ASSOCIATION : 0));
                    mFileCache.put(key, entry);
                }
                entry.mLastModifiedChanged = true;
            }
            entry.mSeenInFileSystem = true;

            if (mProcessPlaylists && MediaFile.isPlayListFileType(mFileType)) {
                mPlayLists.add(entry);
                // we don't process playlists in the main scan, so return null
                return null;
            }

            // clear all the metadata
            mArtist = null;
            mAlbumArtist = null;
            mAlbum = null;
            mTitle = null;
            mComposer = null;
            mGenre = null;
            mTrack = 0;
            mYear = 0;
            mDuration = 0;
            mPath = path;
            mLastModified = lastModified;
            mWriter = null;
            mIsAccurateDuration = 0;
            mCompilation = 0;
            mIsDrm = false;
            mWidth = 0;
            mHeight = 0;
            //add for mtk drm
            mDrmContentDescriptioin = null;
            mDrmContentName = null;
            mDrmContentUr = null;
            mDrmContentVendor = null;
            mDrmIconUri = null;
            mDrmRightsIssuer = null;
            mDrmDataLen = -1;
            mDrmOffset = -1;
            mDrmMethod = -1;   

            return entry;
        }

        @Override
        public void scanFile(String path, long lastModified, long fileSize,
                boolean isDirectory, boolean noMedia) {
            // This is the callback funtion from native codes.
            // Log.v(TAG, "scanFile: "+path);
            doScanFile(path, null, lastModified, fileSize, isDirectory, false, noMedia);
        }

        public Uri doScanFile(String path, String mimeType, long lastModified,
                long fileSize, boolean isDirectory, boolean scanAlways, boolean noMedia) {
            Uri result = null;
//            long t1 = System.currentTimeMillis();
            try {
                FileCacheEntry entry = beginFile(path, mimeType, lastModified,
                        fileSize, isDirectory, noMedia);
                // rescan for metadata if file was modified since last scan
                if (entry != null && (entry.mLastModifiedChanged || scanAlways)) {
                    if (noMedia) {
                        result = endFile(entry, false, false, false, false, false);
                    } else {
                        String lowpath = path.toLowerCase();
                        boolean ringtones = (lowpath.indexOf(RINGTONES_DIR) > 0);
                        boolean notifications = (lowpath.indexOf(NOTIFICATIONS_DIR) > 0);
                        boolean alarms = (lowpath.indexOf(ALARMS_DIR) > 0);
                        boolean podcasts = (lowpath.indexOf(PODCAST_DIR) > 0);
                        boolean music = (lowpath.indexOf(MUSIC_DIR) > 0) ||
                            (!ringtones && !notifications && !alarms && !podcasts);

                        // we only extract metadata for audio and video files
                        if (MediaFile.isAudioFileType(mFileType)
                                || MediaFile.isVideoFileType(mFileType)) {
                            processFile(path, mimeType, this);
                        }

                        if (MediaFile.isImageFileType(mFileType)) {
                            processImageFile(path);
                        }

                        result = endFile(entry, ringtones, notifications, alarms, music, podcasts);
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e);
            }
//            long t2 = System.currentTimeMillis();
//            Log.v(TAG, "scanFile: " + path + " took " + (t2-t1));
            return result;
        }

        private int parseSubstring(String s, int start, int defaultValue) {
            int length = s.length();
            if (start == length) return defaultValue;

            char ch = s.charAt(start++);
            // return defaultValue if we have no integer at all
            if (ch < '0' || ch > '9') return defaultValue;

            int result = ch - '0';
            while (start < length) {
                ch = s.charAt(start++);
                if (ch < '0' || ch > '9') return result;
                result = result * 10 + (ch - '0');
            }

            return result;
        }

        public void handleStringTag(String name, String value) {
            // if (LOG) Log.i(TAG, "handleStringTag: name=" + name + ",value=" + value);
            if (name.equalsIgnoreCase("title") || name.startsWith("title;")) {
                // Don't trim() here, to preserve the special \001 character
                // used to force sorting. The media provider will trim() before
                // inserting the title in to the database.
                mTitle = value;
            } else if (name.equalsIgnoreCase("artist") || name.startsWith("artist;")) {
                mArtist = value.trim();
            } else if (name.equalsIgnoreCase("albumartist") || name.startsWith("albumartist;")
                    || name.equalsIgnoreCase("band") || name.startsWith("band;")) {
                mAlbumArtist = value.trim();
            } else if (name.equalsIgnoreCase("album") || name.startsWith("album;")) {
                mAlbum = value.trim();
            } else if (name.equalsIgnoreCase("composer") || name.startsWith("composer;")) {
                mComposer = value.trim();
            } else if (mProcessGenres &&
                    (name.equalsIgnoreCase("genre") || name.startsWith("genre;"))) {
                mGenre = getGenreName(value);
            } else if (name.equalsIgnoreCase("year") || name.startsWith("year;")) {
                mYear = parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase("tracknumber") || name.startsWith("tracknumber;")) {
                // track number might be of the form "2/12"
                // we just read the number before the slash
                int num = parseSubstring(value, 0, 0);
                mTrack = (mTrack / 1000) * 1000 + num;
            } else if (name.equalsIgnoreCase("discnumber") ||
                    name.equals("set") || name.startsWith("set;")) {
                // set number might be of the form "1/3"
                // we just read the number before the slash
                int num = parseSubstring(value, 0, 0);
                mTrack = (num * 1000) + (mTrack % 1000);
            } else if (name.equalsIgnoreCase("duration")) {
                mDuration = parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase("writer") || name.startsWith("writer;")) {
                mWriter = value.trim();
            } else if (name.equalsIgnoreCase("isaccurateduration")) {
                //get the info representing that duration is accurate.
                //so we can speed up the getDuration() function.
                mIsAccurateDuration = parseSubstring(value, 0, 0);
                // if (LOG) Log.i(TAG, "get isaccurateduration: " + mIsAccurateDuration);
            } else if (name.equalsIgnoreCase("compilation")) {
                mCompilation = parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase("isdrm")) {
                mIsDrm = (parseSubstring(value, 0, 0) == 1);
            } else if (name.equalsIgnoreCase("drm_content_uri")) {
                mDrmContentUr = value.trim();
            } else if (name.equalsIgnoreCase("drm_offset")) {
                mDrmOffset = parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase("drm_dataLen")) {
                mDrmDataLen = parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase("drm_rights_issuer")) {
                mDrmRightsIssuer = value.trim();
            } else if (name.equalsIgnoreCase("drm_content_name")) {
                mDrmContentName = value.trim();
            } else if (name.equalsIgnoreCase("drm_content_description")) {
                mDrmContentDescriptioin = value.trim();
            } else if (name.equalsIgnoreCase("drm_content_vendor")) {
                mDrmContentVendor = value.trim();
            } else if (name.equalsIgnoreCase("drm_icon_uri")) {
                mDrmIconUri = value.trim();
            } else if (name.equalsIgnoreCase("drm_method")) {
                mDrmMethod = parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase("stereotype")) {
                mStereoType = parseSubstring(value, 0, 0);
                /// M: Changes the stereo type to unknown for videos.
                if (mStereoType == Video.Media.STEREO_TYPE_2D) {
                    mStereoType = Video.Media.STEREO_TYPE_UNKNOWN;
                }
            }
        }

        public String getGenreName(String genreTagValue) {

            if (genreTagValue == null) {
                return null;
            }
            final int length = genreTagValue.length();

            if (length > 0 && genreTagValue.charAt(0) == '(') {
                StringBuffer number = new StringBuffer();
                int i = 1;
                for (; i < length - 1; ++i) {
                    char c = genreTagValue.charAt(i);
                    if (Character.isDigit(c)) {
                        number.append(c);
                    } else {
                        break;
                    }
                }
                if (genreTagValue.charAt(i) == ')') {
                    try {
                        short genreIndex = Short.parseShort(number.toString());
                        if (genreIndex >= 0) {
                            if (genreIndex < ID3_GENRES.length) {
                                return ID3_GENRES[genreIndex];
                            } else if (genreIndex == 0xFF) {
                                return null;
                            } else if (genreIndex < 0xFF && (i + 1) < length) {
                                // genre is valid but unknown,
                                // if there is a string after the value we take it
                                return genreTagValue.substring(i + 1);
                            } else {
                                // else return the number, without parentheses
                                return number.toString();
                            }
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }

            return genreTagValue;
        }

        private void processImageFile(String path) {
            try {
                mBitmapOptions.outWidth = 0;
                mBitmapOptions.outHeight = 0;
                BitmapFactory.decodeFile(path, mBitmapOptions);
                mWidth = mBitmapOptions.outWidth;
                mHeight = mBitmapOptions.outHeight;
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        public void setMimeType(String mimeType) {
            if ("audio/mp4".equals(mMimeType) &&
                    mimeType.startsWith("video")) {
                // for feature parity with Donut, we force m4a files to keep the
                // audio/mp4 mimetype, even if they are really "enhanced podcasts"
                // with a video track
                return;
            }
            mMimeType = mimeType;
            mFileType = MediaFile.getFileTypeForMimeType(mimeType);
        }

        /**
         * Formats the data into a values array suitable for use with the Media
         * Content Provider.
         *
         * @return a map of values
         */
        private ContentValues toValues() {
            ContentValues map = new ContentValues();

            map.put(MediaStore.MediaColumns.DATA, mPath);
            map.put(MediaStore.MediaColumns.TITLE, mTitle);
            map.put(MediaStore.MediaColumns.DATE_MODIFIED, mLastModified);
            map.put(MediaStore.MediaColumns.SIZE, mFileSize);
            map.put(MediaStore.MediaColumns.MIME_TYPE, mMimeType);
            map.put(MediaStore.MediaColumns.IS_DRM, mIsDrm);

            if (mWidth > 0 && mHeight > 0) {
                map.put(MediaStore.MediaColumns.WIDTH, mWidth);
                map.put(MediaStore.MediaColumns.HEIGHT, mHeight);
            }

            if (!mNoMedia) {
                if (MediaFile.isVideoFileType(mFileType)) {
                    map.put(Video.Media.ARTIST, (mArtist != null && mArtist.length() > 0
                            ? mArtist : MediaStore.UNKNOWN_STRING));
                    map.put(Video.Media.ALBUM, (mAlbum != null && mAlbum.length() > 0
                            ? mAlbum : MediaStore.UNKNOWN_STRING));
                    map.put(Video.Media.DURATION, mDuration);
                    map.put(Video.Media.STEREO_TYPE, mStereoType);
                    // FIXME - add RESOLUTION
                } else if (MediaFile.isImageFileType(mFileType)) {
                    // FIXME - add DESCRIPTION
                } else if (MediaFile.isAudioFileType(mFileType)) {
                    map.put(Audio.Media.ARTIST, (mArtist != null && mArtist.length() > 0) ?
                            mArtist : MediaStore.UNKNOWN_STRING);
                    map.put(Audio.Media.ALBUM_ARTIST, (mAlbumArtist != null &&
                            mAlbumArtist.length() > 0) ? mAlbumArtist : null);
                    map.put(Audio.Media.ALBUM, (mAlbum != null && mAlbum.length() > 0) ?
                            mAlbum : MediaStore.UNKNOWN_STRING);
                    map.put(Audio.Media.COMPOSER, mComposer);
                    map.put(Audio.Media.GENRE, mGenre);
                    if (mYear != 0) {
                        map.put(Audio.Media.YEAR, mYear);
                    }
                    map.put(Audio.Media.TRACK, mTrack);
                    map.put(Audio.Media.DURATION, mDuration);
                    map.put(Audio.Media.COMPILATION, mCompilation);
                    map.put(Audio.Media.IS_ACCURATE_DURATION, mIsAccurateDuration);
                }
            }
            
            //drm media file, add new column values.
            if (mIsDrm) {
                map.put(MediaStore.MediaColumns.DRM_CONTENT_DESCRIPTION, mDrmContentDescriptioin);
                map.put(MediaStore.MediaColumns.DRM_CONTENT_NAME, mDrmContentName);
                map.put(MediaStore.MediaColumns.DRM_CONTENT_URI, mDrmContentUr);
                map.put(MediaStore.MediaColumns.DRM_CONTENT_VENDOR, mDrmContentVendor);
                map.put(MediaStore.MediaColumns.DRM_DATALEN, mDrmDataLen);
                map.put(MediaStore.MediaColumns.DRM_ICON_URI, mDrmIconUri);
                map.put(MediaStore.MediaColumns.DRM_OFFSET, mDrmOffset);
                map.put(MediaStore.MediaColumns.DRM_RIGHTS_ISSUER, mDrmRightsIssuer);
                map.put(MediaStore.MediaColumns.DRM_METHOD, mDrmMethod);
            }
            
            return map;
        }

        private Uri endFile(FileCacheEntry entry, boolean ringtones, boolean notifications,
                boolean alarms, boolean music, boolean podcasts)
                throws RemoteException {
            // update database

            // use album artist if artist is missing
            if (mArtist == null || mArtist.length() == 0) {
                mArtist = mAlbumArtist;
            }

            ContentValues values = toValues();
            String title = values.getAsString(MediaStore.MediaColumns.TITLE);
            if (title == null || TextUtils.isEmpty(title.trim())) {
                title = MediaFile.getFileTitle(values.getAsString(MediaStore.MediaColumns.DATA));
                values.put(MediaStore.MediaColumns.TITLE, title);
            }
            String album = values.getAsString(Audio.Media.ALBUM);
            if (MediaStore.UNKNOWN_STRING.equals(album)) {
                album = values.getAsString(MediaStore.MediaColumns.DATA);
                // extract last path segment before file name
                int lastSlash = album.lastIndexOf('/');
                if (lastSlash >= 0) {
                    int previousSlash = 0;
                    while (true) {
                        int idx = album.indexOf('/', previousSlash + 1);
                        if (idx < 0 || idx >= lastSlash) {
                            break;
                        }
                        previousSlash = idx;
                    }
                    if (previousSlash != 0) {
                        album = album.substring(previousSlash + 1, lastSlash);
                        values.put(Audio.Media.ALBUM, album);
                    }
                }
            }
            long rowId = entry.mRowId;
            if (MediaFile.isAudioFileType(mFileType) && (rowId == 0 || mMtpObjectHandle != 0)) {
                // Only set these for new entries. For existing entries, they
                // may have been modified later, and we want to keep the current
                // values so that custom ringtones still show up in the ringtone
                // picker.
                values.put(Audio.Media.IS_RINGTONE, ringtones);
                values.put(Audio.Media.IS_NOTIFICATION, notifications);
                values.put(Audio.Media.IS_ALARM, alarms);
                values.put(Audio.Media.IS_MUSIC, music);
                values.put(Audio.Media.IS_PODCAST, podcasts);
            } else if ((mFileType == MediaFile.FILE_TYPE_JPEG || mFileType == MediaFile.FILE_TYPE_MPO 
                    || mFileType == MediaFile.FILE_TYPE_JPS) && !mNoMedia) {
                ExifInterface exif = null;
                String filePath = entry.mPath;
                try {
                    exif = new ExifInterface(filePath);
                } catch (IOException ex) {
                    // exif is null
                    ex.printStackTrace();
                }
                if (exif != null) {
                    float[] latlng = new float[2];
                    if (exif.getLatLong(latlng)) {
                        values.put(Images.Media.LATITUDE, latlng[0]);
                        values.put(Images.Media.LONGITUDE, latlng[1]);
                    }

                    long time = exif.getGpsDateTime();
                    if (time != -1) {
                        values.put(Images.Media.DATE_TAKEN, time);
                    } else {
                        // If no time zone information is available, we should consider using
                        // EXIF local time as taken time if the difference between file time
                        // and EXIF local time is not less than 1 Day, otherwise MediaProvider
                        // will use file time as taken time.
                        time = exif.getDateTime();
                        if (Math.abs(mLastModified * 1000 - time) >= 86400000) {
                            values.put(Images.Media.DATE_TAKEN, time);
                        }
                    }

                    int orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, -1);
                    if (orientation != -1) {
                        // We only recognize a subset of orientation tag values.
                        int degree;
                        switch(orientation) {
                            case ExifInterface.ORIENTATION_ROTATE_90:
                                degree = 90;
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_180:
                                degree = 180;
                                break;
                            case ExifInterface.ORIENTATION_ROTATE_270:
                                degree = 270;
                                break;
                            default:
                                degree = 0;
                                break;
                        }
                        values.put(Images.Media.ORIENTATION, degree);
                    }
                }

                if (mFileType == MediaFile.FILE_TYPE_MPO) {
                    MpoDecoder mpoDecoder = MpoDecoder.decodeFile(entry.mPath);
                    if (mpoDecoder != null) {
                        int mpoType = mpoDecoder.suggestMtkMpoType();
                        values.put(Images.Media.MPO_TYPE, mpoType);
                        mpoDecoder.close();
                        if (mpoType == MpoDecoder.MTK_TYPE_Stereo ||
                            mpoType == MpoDecoder.MTK_TYPE_3DPan) {
                            //we perceive mpo file as sequencial stereo type
                            values.put(Images.Media.STEREO_TYPE, 
                                  Images.Media.STEREO_TYPE_FRAME_SEQUENCE);
                        }
                    } else {
                        if (LOG) Log.w(TAG, "endFile: Null MpoDecoder!");
                    }
                }

                if (mFileType == MediaFile.FILE_TYPE_JPS) {
                    JpsParser parser = JpsParser.parse(new File(entry.mPath));
                    if (parser != null) {
                        int layout = parser.getLayout();
                        if (JpsParser.S3D_LAYOUT_SIDE_BY_SIDE == layout) {
                            values.put(Images.Media.STEREO_TYPE, 
                                  Images.Media.STEREO_TYPE_SIDE_BY_SIDE);
                        } else if (JpsParser.S3D_LAYOUT_TOP_AND_BOTTOM == layout) {
                            values.put(Images.Media.STEREO_TYPE, 
                                    Images.Media.STEREO_TYPE_TOP_BOTTOM);
                        } else {
                            if(LOG) Log.w(TAG, "endFile: unexpected jps layout:" + layout);
                            values.put(Images.Media.STEREO_TYPE, Images.Media.STEREO_TYPE_SIDE_BY_SIDE);
                        }
                    } else {
                        if(LOG) Log.w(TAG, "endFile:parser==null set jps default layout side by side.");
                        values.put(Images.Media.STEREO_TYPE, Images.Media.STEREO_TYPE_SIDE_BY_SIDE);
                    }
                }
            }

            Uri tableUri = mFilesUri;
            MediaInserter inserter = mMediaInserter;
            if (!mNoMedia) {
                if (MediaFile.isVideoFileType(mFileType)) {
                    tableUri = mVideoUri;
                } else if (MediaFile.isImageFileType(mFileType)) {
                    tableUri = mImagesUri;
                } else if (MediaFile.isAudioFileType(mFileType)) {
                    tableUri = mAudioUri;
                }
            }
            Uri result = null;
            if (rowId == 0) {
                if (mMtpObjectHandle != 0) {
                    values.put(MediaStore.MediaColumns.MEDIA_SCANNER_NEW_OBJECT_ID, mMtpObjectHandle);
                }
                if (tableUri == mFilesUri) {
                    int format = entry.mFormat;
                    if (format == 0) {
                        format = MediaFile.getFormatCode(entry.mPath, mMimeType);
                    }
                    values.put(Files.FileColumns.FORMAT, format);
                }
                // new file, insert it
                // We insert directories immediately to ensure they are in the database
                // before the files they contain.
                // Otherwise we can get duplicate directory entries in the database
                // if one of the media FileInserters is flushed before the files table FileInserter
                if (inserter == null || entry.mFormat == MtpConstants.FORMAT_ASSOCIATION) {
                    result = mMediaProvider.insert(tableUri, values);
                } else {
                    inserter.insert(tableUri, values);
                }

                if (result != null) {
                    rowId = ContentUris.parseId(result);
                    entry.mRowId = rowId;
                }
            } else {
                // updated file
                result = ContentUris.withAppendedId(tableUri, rowId);
                // path should never change, and we want to avoid replacing mixed cased paths
                // with squashed lower case paths
                values.remove(MediaStore.MediaColumns.DATA);
                mMediaProvider.update(result, values, null, null);
            }

            if (notifications && mWasEmptyPriorToScan && !mDefaultNotificationSet) {
                if (TextUtils.isEmpty(mDefaultNotificationFilename) ||
                        doesPathHaveFilename(entry.mPath, mDefaultNotificationFilename)) {
                    setSettingIfNotSet(Settings.System.NOTIFICATION_SOUND, tableUri, rowId);
                    setProfileSettings(AudioProfileManager.TYPE_NOTIFICATION, tableUri, rowId);
                    mDefaultNotificationSet = true;
                }
            } else if (ringtones && mWasEmptyPriorToScan && !mDefaultRingtoneSet) {
                if (TextUtils.isEmpty(mDefaultRingtoneFilename) ||
                        doesPathHaveFilename(entry.mPath, mDefaultRingtoneFilename)) {
                    setSettingIfNotSet(Settings.System.RINGTONE, tableUri, rowId);
                    setSettingIfNotSet(Settings.System.VIDEO_CALL, tableUri, rowId);
                    setProfileSettings(AudioProfileManager.TYPE_RINGTONE, tableUri, rowId);
                    setProfileSettings(AudioProfileManager.TYPE_VIDEO_CALL, tableUri, rowId);
                    mDefaultRingtoneSet = true;
                }
            } else if (alarms && mWasEmptyPriorToScan && !mDefaultAlarmSet) {
                if (TextUtils.isEmpty(mDefaultAlarmAlertFilename) ||
                        doesPathHaveFilename(entry.mPath, mDefaultAlarmAlertFilename)) {
                    setSettingIfNotSet(Settings.System.ALARM_ALERT, tableUri, rowId);
                    mDefaultAlarmSet = true;
                }
            }

            return result;
        }

        private boolean doesPathHaveFilename(String path, String filename) {
            int pathFilenameStart = path.lastIndexOf(File.separatorChar) + 1;
            int filenameLength = filename.length();
            return path.regionMatches(pathFilenameStart, filename, 0, filenameLength) &&
                    pathFilenameStart + filenameLength == path.length();
        }

        private void setSettingIfNotSet(String settingName, Uri uri, long rowId) {
            String existingSettingValue = Settings.System.getString(mContext.getContentResolver(),
                    settingName);
            if (TextUtils.isEmpty(existingSettingValue)) {
                // Set the setting to the given URI
                Settings.System.putString(mContext.getContentResolver(), settingName,
                        ContentUris.withAppendedId(uri, rowId).toString());
            }
        }
        
        private void setProfileSettings(int type, Uri uri, long rowId) {
            if(type == AudioProfileManager.TYPE_NOTIFICATION) {
                setSettingIfNotSet(AudioProfileManager.KEY_DEFAULT_NOTIFICATION, uri, rowId);
            } else if(type == AudioProfileManager.TYPE_RINGTONE) {
                setSettingIfNotSet(AudioProfileManager.KEY_DEFAULT_RINGTONE, uri, rowId);
            } else if(type == AudioProfileManager.TYPE_VIDEO_CALL) {
                setSettingIfNotSet(AudioProfileManager.KEY_DEFAULT_VIDEO_CALL, uri, rowId);
            }
            List<String> keys = AudioProfileManager.getStreamUriKeys(type);
            for(String key : keys) {
                setSettingIfNotSet(key, uri, rowId);
            }
        }

        private int getFileTypeFromDrm(String path) {
            if (!isDrmEnabled()) {
                return 0;
            }

            int resultFileType = 0;

            if (mDrmManagerClient == null) {
                mDrmManagerClient = new DrmManagerClient(mContext);
            }

            if (mDrmManagerClient.canHandle(path, null)) {
                String drmMimetype = mDrmManagerClient.getOriginalMimeType(path);
                if (drmMimetype != null) {
                    mMimeType = drmMimetype;
                    resultFileType = MediaFile.getFileTypeForMimeType(drmMimetype);
                }
            }
            return resultFileType;
        }

    }; // end of anonymous MediaScannerClient instance

    private void prescan(String filePath, boolean prescanFiles) throws RemoteException {
        if (LOG) Log.d(TAG, "prescan>>>filePath=" + filePath + ",prescanFiles=" + prescanFiles);
        Cursor c = null;
        String where = null;
        String[] selectionArgs = null;

        if (mFileCache == null) {
            mFileCache = new HashMap<String, FileCacheEntry>();
        } else {
            mFileCache.clear();
        }
        if (mPlayLists == null) {
            mPlayLists = new ArrayList<FileCacheEntry>();
        } else {
            mPlayLists.clear();
        }

        if (filePath != null) {
            // query for only one file
            where = Files.FileColumns.DATA + "=?";
            selectionArgs = new String[] { filePath };
        }

        // Build the list of files from the content provider
        try {
            if (prescanFiles) {
                // mCaseInsensitivePaths=true indicates external sd card to be scanned.
                if (filePath == null && mCaseInsensitivePaths) {
                    mtkPrescan();
                }
                // First read existing files from the files table
                c = mMediaProvider.query(mFilesUri, FILES_PRESCAN_PROJECTION,
                        where, selectionArgs, null);

                if (c != null) {
                    mWasEmptyPriorToScan = c.getCount() == 0;
                    while (c.moveToNext()) {
                        long rowId = c.getLong(FILES_PRESCAN_ID_COLUMN_INDEX);
                        String path = c.getString(FILES_PRESCAN_PATH_COLUMN_INDEX);
                        int format = c.getInt(FILES_PRESCAN_FORMAT_COLUMN_INDEX);
                        long lastModified = c.getLong(FILES_PRESCAN_DATE_MODIFIED_COLUMN_INDEX);

                        // Only consider entries with absolute path names.
                        // This allows storing URIs in the database without the
                        // media scanner removing them.
                        if (path != null && path.startsWith("/")) {
                            String key = path;
                            if (mCaseInsensitivePaths) {
                                key = path.toLowerCase();
                            }

                            FileCacheEntry entry = new FileCacheEntry(rowId, path,
                                    lastModified, format);
                            mFileCache.put(key, entry);
                        }
                    }
                    c.close();
                    c = null;
                }
            }
        }
        finally {
            if (c != null) {
                c.close();
            }
        }

        // compute original size of images
        mOriginalCount = 0;
        c = mMediaProvider.query(mImagesUri, ID_PROJECTION, null, null, null);
        if (c != null) {
            mOriginalCount = c.getCount();
            c.close();
        }
        mOriginalVideoCount = 0;
        c = mMediaProvider.query(mVideoUri, ID_PROJECTION, null, null, null);
        if (c != null) {
            mOriginalVideoCount = c.getCount();
            c.close();
        }
        if (LOG) Log.d(TAG, "prescan<<<imageCount=" + mOriginalCount + ",videoCount=" + mOriginalVideoCount);
    }

    private boolean inScanDirectory(String path, String[] directories) {
        for (int i = 0; i < directories.length; i++) {
            String directory = directories[i];
            if (path.startsWith(directory)) {
                return true;
            }
        }
        return false;
    }

    private void pruneDeadThumbnailFiles() {
        HashSet<String> existingFiles = new HashSet<String>();
        String directory = Environment.getExternalStorageDirectory().toString() + "/DCIM/.thumbnails";
        String [] files = (new File(directory)).list();
        if (files == null) {
            files = new String[0];
        }
        for (int i = 0; i < files.length; i++) {
            String fullPathString = directory + "/" + files[i];
            existingFiles.add(fullPathString);
        }

        int videoCount = 0;
        int imageCount = 0;
        try {
            Cursor c = mMediaProvider.query(
                    mThumbsUri,
                    new String [] { "_data" },
                    null,
                    null,
                    null);
            Log.v(TAG, "pruneDeadThumbnailFiles... " + c);
            if (null != c) {
                imageCount = c.getCount();
                if (c.moveToFirst()) {
                    do {
                        String fullPathString = c.getString(0);
                        existingFiles.remove(fullPathString);
                    } while (c.moveToNext());
                }
                c.close();
            }
            c = mMediaProvider.query(
                    mVideoThumbsUri,
                    new String [] { "_data" },
                    null,
                    null,
                    null);
            if (null != c) {
                videoCount = c.getCount();
                if (c.moveToFirst()) {
                    do {
                        String fullPathString = c.getString(0);
                        existingFiles.remove(fullPathString);
                    } while (c.moveToNext());
                }
                c.close();
            }

            if (videoCount != 0) {
            	String fullPathString = MiniThumbFile.getThumbdataPath(mVideoThumbsUri);
            	for(Iterator<String> iter = existingFiles.iterator(); iter.hasNext();) {
                    String item = iter.next();
                    if (item != null && item.startsWith(fullPathString)) {
                    	iter.remove();
                    }
                }
            }
            if (imageCount != 0) {
                String fullPathString = MiniThumbFile.getThumbdataPath(mThumbsUri);
                for(Iterator<String> iter = existingFiles.iterator(); iter.hasNext();) {
                    String item = iter.next();
                    if (item != null && item.startsWith(fullPathString)) {
                    	iter.remove();
                    }
                }
            }
            for (String fileToDelete : existingFiles) {
                if (LOG)
                    Log.v(TAG, "fileToDelete is " + fileToDelete);
                try {
                    (new File(fileToDelete)).delete();
                } catch (SecurityException ex) {
                    ex.printStackTrace();
                }
            }
            Log.v(TAG, "/pruneDeadThumbnailFiles... " + c);
        } catch (RemoteException e) {
            // We will soon be killed...
            e.printStackTrace();
        }
    }

    private void postscan(String[] directories) throws RemoteException {
        Iterator<FileCacheEntry> iterator = mFileCache.values().iterator();

        while (iterator.hasNext()) {
            FileCacheEntry entry = iterator.next();
            String path = entry.mPath;

            // remove database entries for files that no longer exist.
            boolean fileMissing = false;

            if (!entry.mSeenInFileSystem && !MtpConstants.isAbstractObject(entry.mFormat)) {
                if (inScanDirectory(path, directories)) {
                    // we didn't see this file in the scan directory.
                    fileMissing = true;
                } else {
                    // the file actually a directory or other abstract object
                    // or is outside of our scan directory,
                    // so we need to check for file existence here.
                    File testFile = new File(path);
                    if (!testFile.exists()) {
                        fileMissing = true;
                    }
                }
            }

            if (fileMissing) {
                // Clear the file path to prevent the _DELETE_FILE database hook
                // in the media provider from deleting the file.
                // If the file is truly gone the delete is unnecessary, and we want to avoid
                // accidentally deleting files that are really there.
                ContentValues values = new ContentValues();
                values.put(Files.FileColumns.DATA, "");
                values.put(Files.FileColumns.DATE_MODIFIED, 0);
                mMediaProvider.update(ContentUris.withAppendedId(mFilesUri, entry.mRowId),
                        values, null, null);

                // do not delete missing playlists, since they may have been modified by the user.
                // the user can delete them in the media player instead.
                // instead, clear the path and lastModified fields in the row
                MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
                int fileType = (mediaFileType == null ? 0 : mediaFileType.fileType);

                if (!MediaFile.isPlayListFileType(fileType)) {
                    mMediaProvider.delete(ContentUris.withAppendedId(mFilesUri, entry.mRowId),
                            null, null);
                    iterator.remove();
                }
            }
        }

        // handle playlists last, after we know what media files are on the storage.
        if (mProcessPlaylists) {
            processPlayLists();
        }

        if ((mOriginalCount == 0 || mOriginalVideoCount == 0)
                && mImagesUri.equals(Images.Media.getContentUri("external"))) {
            pruneDeadThumbnailFiles();
        }

        // allow GC to clean up
        mPlayLists = null;
        mFileCache = null;
        mMediaProvider = null;
    }

    private void mtkPostscan(String[] directories) throws RemoteException {
        if (LOG) Log.d(TAG, "mtkPostscan>>>");
        ContentValues values = new ContentValues();
        values.put(Files.FileColumns.DATA, "");
        values.put(Files.FileColumns.DATE_MODIFIED, 0);
        
        int limit = 1000;
        List<String> deleteRowIds = new ArrayList<String>(limit);
        List<String> playListRowIds = new ArrayList<String>(limit);
        String[] whereArgs = null;
        Uri bulkUri = mFilesUri.buildUpon().appendQueryParameter("isbulk", "1").build();
        
        Iterator<FileCacheEntry> iterator = mFileCache.values().iterator();
        while (iterator.hasNext()) {
            FileCacheEntry entry = iterator.next();
            String path = entry.mPath;
            
            // remove database entries for files that no longer exist.
            boolean fileMissing = false;
            if (!entry.mSeenInFileSystem && !MtpConstants.isAbstractObject(entry.mFormat)) {
                if (inScanDirectory(path, directories)) {
                    // we didn't see this file in the scan directory.
                    fileMissing = true;
                } else {
                    // the file actually a directory or other abstract object
                    // or is outside of our scan directory,
                    // so we need to check for file existence here.
                    File testFile = new File(path);
                    if (!testFile.exists()) {
                        fileMissing = true;
                    }
                }
            }
            
            if(fileMissing) {
                MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
                int fileType = (mediaFileType == null ? 0 : mediaFileType.fileType);
                
                if (MediaFile.isPlayListFileType(fileType)) {
                    playListRowIds.add(String.valueOf(entry.mRowId));
                    if(playListRowIds.size() == limit) {
                        mMediaProvider.update(bulkUri, values, null, playListRowIds.toArray(new String[limit]));
                        playListRowIds.clear(); 
                    }
                } else {
                    deleteRowIds.add(String.valueOf(entry.mRowId));
                    if(deleteRowIds.size() == limit) {
                        long start = System.currentTimeMillis();
                        whereArgs = deleteRowIds.toArray(new String[limit]);
                        mMediaProvider.update(bulkUri, values, null, whereArgs);
                        mMediaProvider.delete(bulkUri, null, whereArgs);
                        deleteRowIds.clear();
                        if (LOG) Log.d(TAG, "mtkPostscan: delete " + limit + " items takes " + (System.currentTimeMillis() - start) + "ms.");
                    }
                }
            }
        }
        
        // delete left files.
        int leftSize = deleteRowIds.size();
        if(leftSize > 0) {
            long start = System.currentTimeMillis();
            whereArgs = deleteRowIds.toArray(new String[leftSize]);
            mMediaProvider.update(bulkUri, values, null, whereArgs);
            mMediaProvider.delete(bulkUri, null, whereArgs);
            if (LOG) Log.d(TAG, "mtkPostscan: delete " + leftSize + " items takes " + (System.currentTimeMillis() - start) + "ms.");
        }
        
        // update left playlists.
        leftSize = playListRowIds.size();
        if(leftSize > 0) {
            whereArgs = playListRowIds.toArray(new String[leftSize]);
            mMediaProvider.update(bulkUri, values, null, whereArgs);
        }
        
        // handle playlists last, after we know what media files are on the storage.
        if (mProcessPlaylists) {
            processPlayLists();
        }
        
        if ((mOriginalCount == 0 || mOriginalVideoCount == 0)
                && mImagesUri.equals(Images.Media.getContentUri("external"))) {
            long prune = System.currentTimeMillis();
            pruneDeadThumbnailFiles();
            if (LOG) Log.d(TAG, "mtkPostscan: pruneDeadThumbnailFiles takes " + (System.currentTimeMillis() - prune) + "ms.");
        }
        
        // allow GC to clean up
        mPlayLists = null;
        mFileCache = null;
        mMediaProvider = null;
        deleteRowIds = null;
        playListRowIds = null;
        if (LOG) Log.d(TAG, "mtkPostscan<<<");
    }
    
    private void mtkPrescan() throws RemoteException {
        if (LOG) Log.d(TAG, "mtkPrescan>>>");
        Uri uri = mFilesUri.buildUpon().appendQueryParameter("isprescan", "1").build();
        String where = FileColumns.DATA + "=?";
        String[] whereArgs = new String[] {""};
        int num = mMediaProvider.delete(uri, where, whereArgs);
        if (LOG) Log.d(TAG, "mtkPrescan: deleted=" + num);
        
        String mountPoint = "/mnt/sdcard2";
        StorageManager storageManager = (StorageManager)mContext.getSystemService(Context.STORAGE_SERVICE);
        String state = storageManager.getVolumeState(mountPoint);
        if (state != null && state.equals(Environment.MEDIA_REMOVED)) {
            StorageVolume[] volumes = storageManager.getVolumeList();
            if (volumes == null) {
                Log.e(TAG, "mtkPrescan<<<Null volumeList!");
                return;
            }
            
            int storageId = 0;
            for (StorageVolume volume : volumes) {
                if (mountPoint.equals(volume.getPath())) {
                    storageId = volume.getStorageId();
                    break;
                }
            }
            
            if (0 == storageId) {
                if (LOG) Log.d(TAG, "mtkPrescan<<<storageId=0");
                return;
            }
            
            ContentValues values = new ContentValues();
            values.put(Files.FileColumns.DATA, "");
            where = FileColumns.STORAGE_ID + "=?";
            whereArgs = new String[] { String.valueOf(storageId) };
            num = mMediaProvider.update(uri, values, where, whereArgs);
            if (LOG) Log.d(TAG, "mtkPrescan: updated=" + num);
            // now delete the records
            num = mMediaProvider.delete(uri, where, whereArgs);
        }
        if (LOG) Log.d(TAG, "mtkPrescan<<<state=" + state +",deleted=" + num);
    }

    private void initialize(String volumeName) {
        mMediaProvider = mContext.getContentResolver().acquireProvider("media");

        mAudioUri = Audio.Media.getContentUri(volumeName);
        mVideoUri = Video.Media.getContentUri(volumeName);
        mImagesUri = Images.Media.getContentUri(volumeName);
        mThumbsUri = Images.Thumbnails.getContentUri(volumeName);
        mVideoThumbsUri = Video.Thumbnails.getContentUri(volumeName);
        mFilesUri = Files.getContentUri(volumeName);

        if (!volumeName.equals("internal")) {
            // we only support playlists on external media
            mProcessPlaylists = true;
            mProcessGenres = true;
            mPlaylistsUri = Playlists.getContentUri(volumeName);

            mCaseInsensitivePaths = true;
        }
    }

    public void scanDirectories(String[] directories, String volumeName) {
        try {
            long start = System.currentTimeMillis();
            initialize(volumeName);
            prescan(null, true);
            long prescan = System.currentTimeMillis();

            if (ENABLE_BULK_INSERTS) {
                // create MediaInserter for bulk inserts
                mMediaInserter = new MediaInserter(mMediaProvider, 500);
            }

            for (int i = 0; i < directories.length; i++) {
                processDirectory(directories[i], mClient);
            }

            if (ENABLE_BULK_INSERTS) {
                // flush remaining inserts
                mMediaInserter.flushAll();
                mMediaInserter = null;
            }

            long scan = System.currentTimeMillis();
            // postscan(directories);
            mtkPostscan(directories);
            long end = System.currentTimeMillis();

            if (LOG) {
                Log.d(TAG, " prescan time: " + (prescan - start) + "ms\n");
                Log.d(TAG, "    scan time: " + (scan - prescan) + "ms\n");
                Log.d(TAG, "postscan time: " + (end - scan) + "ms\n");
                Log.d(TAG, "   total time: " + (end - start) + "ms\n");
            }
        } catch (SQLException e) {
            // this might happen if the SD card is removed while the media scanner is running
            Log.e(TAG, "SQLException in MediaScanner.scan()", e);
        } catch (UnsupportedOperationException e) {
            // this might happen if the SD card is removed while the media scanner is running
            Log.e(TAG, "UnsupportedOperationException in MediaScanner.scan()", e);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.scan()", e);
        }
    }

    // this function is used to scan a single file
    public Uri scanSingleFile(String path, String volumeName, String mimeType) {
        try {
            initialize(volumeName);
            prescan(path, true);

            File file = new File(path);
            if (!file.exists()) {
                Log.e(TAG, "scanSingleFile: Not exist path=" + path);
                return null;
            }

            // lastModified is in milliseconds on Files.
            long lastModifiedSeconds = file.lastModified() / 1000;

            // always scan the file, so we can return the content://media Uri for existing files
            return mClient.doScanFile(path, mimeType, lastModifiedSeconds, file.length(),
                    file.isDirectory(), true, isNoMediaPath(path));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e);
            return null;
        }
    }

    private static boolean isNoMediaFile(String path) {
        File file = new File(path);
        if (file.isDirectory()) return false;

        // special case certain file names
        // I use regionMatches() instead of substring() below
        // to avoid memory allocation
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash + 2 < path.length()) {
            // ignore those ._* files created by MacOS
            if (path.regionMatches(lastSlash + 1, "._", 0, 2)) {
                return true;
            }

            // ignore album art files created by Windows Media Player:
            // Folder.jpg, AlbumArtSmall.jpg, AlbumArt_{...}_Large.jpg
            // and AlbumArt_{...}_Small.jpg
            if (path.regionMatches(true, path.length() - 4, ".jpg", 0, 4)) {
                if (path.regionMatches(true, lastSlash + 1, "AlbumArt_{", 0, 10) ||
                        path.regionMatches(true, lastSlash + 1, "AlbumArt.", 0, 9)) {
                    return true;
                }
                int length = path.length() - lastSlash - 1;
                if ((length == 17 && path.regionMatches(
                        true, lastSlash + 1, "AlbumArtSmall", 0, 13)) ||
                        (length == 10
                         && path.regionMatches(true, lastSlash + 1, "Folder", 0, 6))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isNoMediaPath(String path) {
        if (path == null) return false;

        // return true if file or any parent directory has name starting with a dot
        if (path.indexOf("/.") >= 0) return true;

        // now check to see if any parent directories have a ".nomedia" file
        // start from 1 so we don't bother checking in the root directory
        int offset = 1;
        while (offset >= 0) {
            int slashIndex = path.indexOf('/', offset);
            if (slashIndex > offset) {
                slashIndex++; // move past slash
                File file = new File(path.substring(0, slashIndex) + ".nomedia");
                if (file.exists()) {
                    // we have a .nomedia in one of the parent directories
                    return true;
                }
            }
            offset = slashIndex;
        }
        return isNoMediaFile(path);
    }

    public void scanMtpFile(String path, String volumeName, int objectHandle, int format) {
        initialize(volumeName);
        MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
        int fileType = (mediaFileType == null ? 0 : mediaFileType.fileType);
        File file = new File(path);
        long lastModifiedSeconds = file.lastModified() / 1000;

        if (!MediaFile.isAudioFileType(fileType) && !MediaFile.isVideoFileType(fileType) &&
            !MediaFile.isImageFileType(fileType) && !MediaFile.isPlayListFileType(fileType)) {

            // no need to use the media scanner, but we need to update last modified and file size
            ContentValues values = new ContentValues();
            values.put(Files.FileColumns.SIZE, file.length());
            values.put(Files.FileColumns.DATE_MODIFIED, lastModifiedSeconds);
            try {
                String[] whereArgs = new String[] {  Integer.toString(objectHandle) };
                mMediaProvider.update(Files.getMtpObjectsUri(volumeName), values, "_id=?",
                        whereArgs);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in scanMtpFile", e);
            }
            return;
        }

        mMtpObjectHandle = objectHandle;
        try {
            if (MediaFile.isPlayListFileType(fileType)) {
                // build file cache so we can look up tracks in the playlist
                prescan(null, true);

                String key = path;
                if (mCaseInsensitivePaths) {
                    key = path.toLowerCase();
                }
                FileCacheEntry entry = mFileCache.get(key);
                if (entry != null) {
                    processPlayList(entry);
                }
            } else {
                // MTP will create a file entry for us so we don't want to do it in prescan
                prescan(path, false);

                // always scan the file, so we can return the content://media Uri for existing files
                mClient.doScanFile(path, mediaFileType.mimeType, lastModifiedSeconds, file.length(),
                    (format == MtpConstants.FORMAT_ASSOCIATION), true, isNoMediaPath(path));
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e);
        } finally {
            mMtpObjectHandle = 0;
        }
    }

    // returns the number of matching file/directory names, starting from the right
    private int matchPaths(String path1, String path2) {
        int result = 0;
        int end1 = path1.length();
        int end2 = path2.length();

        while (end1 > 0 && end2 > 0) {
            int slash1 = path1.lastIndexOf('/', end1 - 1);
            int slash2 = path2.lastIndexOf('/', end2 - 1);
            int backSlash1 = path1.lastIndexOf('\\', end1 - 1);
            int backSlash2 = path2.lastIndexOf('\\', end2 - 1);
            int start1 = (slash1 > backSlash1 ? slash1 : backSlash1);
            int start2 = (slash2 > backSlash2 ? slash2 : backSlash2);
            if (start1 < 0) start1 = 0; else start1++;
            if (start2 < 0) start2 = 0; else start2++;
            int length = end1 - start1;
            if (end2 - start2 != length) break;
            if (path1.regionMatches(true, start1, path2, start2, length)) {
                result++;
                end1 = start1 - 1;
                end2 = start2 - 1;
            } else break;
        }

        return result;
    }

    private boolean addPlayListEntry(String entry, String playListDirectory,
            Uri uri, ContentValues values, int index) {

        // watch for trailing whitespace
        int entryLength = entry.length();
        while (entryLength > 0 && Character.isWhitespace(entry.charAt(entryLength - 1))) entryLength--;
        // path should be longer than 3 characters.
        // avoid index out of bounds errors below by returning here.
        if (entryLength < 3) return false;
        if (entryLength < entry.length()) entry = entry.substring(0, entryLength);

        // does entry appear to be an absolute path?
        // look for Unix or DOS absolute paths
        char ch1 = entry.charAt(0);
        boolean fullPath = (ch1 == '/' ||
                (Character.isLetter(ch1) && entry.charAt(1) == ':' && entry.charAt(2) == '\\'));
        // if we have a relative path, combine entry with playListDirectory
        if (!fullPath)
            entry = playListDirectory + entry;

        //FIXME - should we look for "../" within the path?

        // best matching MediaFile for the play list entry
        FileCacheEntry bestMatch = null;

        // number of rightmost file/directory names for bestMatch
        int bestMatchLength = 0;

        Iterator<FileCacheEntry> iterator = mFileCache.values().iterator();
        while (iterator.hasNext()) {
            FileCacheEntry cacheEntry = iterator.next();
            String path = cacheEntry.mPath;

            if (path.equalsIgnoreCase(entry)) {
                bestMatch = cacheEntry;
                break;    // don't bother continuing search
            }

            int matchLength = matchPaths(path, entry);
            if (matchLength > bestMatchLength) {
                bestMatch = cacheEntry;
                bestMatchLength = matchLength;
            }
        }

        if (bestMatch == null) {
            return false;
        }

        try {
            // check rowid is set. Rowid may be missing if it is inserted by bulkInsert().
            if (bestMatch.mRowId == 0) {
                Cursor c = mMediaProvider.query(mAudioUri, ID_PROJECTION,
                        MediaStore.Files.FileColumns.DATA + "=?",
                        new String[] { bestMatch.mPath }, null);
                if (c != null) {
                    if (c.moveToNext()) {
                        bestMatch.mRowId = c.getLong(0);
                    }
                    c.close();
                }
                if (bestMatch.mRowId == 0) {
                    return false;
                }
            }
            // OK, now we are ready to add this to the database
            values.clear();
            values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(index));
            values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, Long.valueOf(bestMatch.mRowId));
            mMediaProvider.insert(uri, values);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.addPlayListEntry()", e);
            return false;
        }

        return true;
    }

    private void processM3uPlayList(String path, String playListDirectory, Uri uri, ContentValues values) {
        BufferedReader reader = null;
        try {
            File f = new File(path);
            if (f.exists()) {
                reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(f)), 8192);
                String line = reader.readLine();
                int index = 0;
                while (line != null) {
                    // ignore comment lines, which begin with '#'
                    if (line.length() > 0 && line.charAt(0) != '#') {
                        values.clear();
                        if (addPlayListEntry(line, playListDirectory, uri, values, index))
                            index++;
                    }
                    line = reader.readLine();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e);
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e);
            }
        }
    }

    private void processPlsPlayList(String path, String playListDirectory, Uri uri, ContentValues values) {
        BufferedReader reader = null;
        try {
            File f = new File(path);
            if (f.exists()) {
                reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(f)), 8192);
                String line = reader.readLine();
                int index = 0;
                while (line != null) {
                    // ignore comment lines, which begin with '#'
                    if (line.startsWith("File")) {
                        int equals = line.indexOf('=');
                        if (equals > 0) {
                            values.clear();
                            if (addPlayListEntry(line.substring(equals + 1), playListDirectory, uri, values, index))
                                index++;
                        }
                    }
                    line = reader.readLine();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e);
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e);
            }
        }
    }

    class WplHandler implements ElementListener {

        final ContentHandler handler;
        String playListDirectory;
        Uri uri;
        ContentValues values = new ContentValues();
        int index = 0;

        public WplHandler(String playListDirectory, Uri uri) {
            this.playListDirectory = playListDirectory;
            this.uri = uri;

            RootElement root = new RootElement("smil");
            Element body = root.getChild("body");
            Element seq = body.getChild("seq");
            Element media = seq.getChild("media");
            media.setElementListener(this);

            this.handler = root.getContentHandler();
        }

        public void start(Attributes attributes) {
            String path = attributes.getValue("", "src");
            if (path != null) {
                values.clear();
                if (addPlayListEntry(path, playListDirectory, uri, values, index)) {
                    index++;
                }
            }
        }

       public void end() {
       }

        ContentHandler getContentHandler() {
            return handler;
        }
    }

    private void processWplPlayList(String path, String playListDirectory, Uri uri) {
        FileInputStream fis = null;
        try {
            File f = new File(path);
            if (f.exists()) {
                fis = new FileInputStream(f);

                Xml.parse(fis, Xml.findEncodingByName("UTF-8"), new WplHandler(playListDirectory, uri).getContentHandler());
            }
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException in MediaScanner.processWplPlayList()", e);
            }
        }
    }

    private void processPlayList(FileCacheEntry entry) throws RemoteException {
        String path = entry.mPath;
        ContentValues values = new ContentValues();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) throw new IllegalArgumentException("bad path " + path);
        Uri uri, membersUri;
        long rowId = entry.mRowId;

        // make sure we have a name
        String name = values.getAsString(MediaStore.Audio.Playlists.NAME);
        if (name == null) {
            name = values.getAsString(MediaStore.MediaColumns.TITLE);
            if (name == null) {
                // extract name from file name
                int lastDot = path.lastIndexOf('.');
                name = (lastDot < 0 ? path.substring(lastSlash + 1)
                        : path.substring(lastSlash + 1, lastDot));
            }
        }

        values.put(MediaStore.Audio.Playlists.NAME, name);
        values.put(MediaStore.Audio.Playlists.DATE_MODIFIED, entry.mLastModified);

        if (rowId == 0) {
            values.put(MediaStore.Audio.Playlists.DATA, path);
            uri = mMediaProvider.insert(mPlaylistsUri, values);
            rowId = ContentUris.parseId(uri);
            membersUri = Uri.withAppendedPath(uri, Playlists.Members.CONTENT_DIRECTORY);
        } else {
            uri = ContentUris.withAppendedId(mPlaylistsUri, rowId);
            mMediaProvider.update(uri, values, null, null);

            // delete members of existing playlist
            membersUri = Uri.withAppendedPath(uri, Playlists.Members.CONTENT_DIRECTORY);
            mMediaProvider.delete(membersUri, null, null);
        }

        String playListDirectory = path.substring(0, lastSlash + 1);
        MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
        int fileType = (mediaFileType == null ? 0 : mediaFileType.fileType);

        if (fileType == MediaFile.FILE_TYPE_M3U) {
            processM3uPlayList(path, playListDirectory, membersUri, values);
        } else if (fileType == MediaFile.FILE_TYPE_PLS) {
            processPlsPlayList(path, playListDirectory, membersUri, values);
        } else if (fileType == MediaFile.FILE_TYPE_WPL) {
            processWplPlayList(path, playListDirectory, membersUri);
        }
    }

    private void processPlayLists() throws RemoteException {
        Iterator<FileCacheEntry> iterator = mPlayLists.iterator();
        while (iterator.hasNext()) {
            FileCacheEntry entry = iterator.next();
            // only process playlist files if they are new or have been modified since the last scan
            if (entry.mLastModifiedChanged) {
                processPlayList(entry);
            }
        }
    }

    private native void processDirectory(String path, MediaScannerClient client);
    private native void processFile(String path, String mimeType, MediaScannerClient client);
    public native void setLocale(String locale);

    public native byte[] extractAlbumArt(FileDescriptor fd);

    private static native final void native_init();
    private native final void native_setup();
    private native final void native_finalize();

    /**
     * Releases resouces associated with this MediaScanner object.
     * It is considered good practice to call this method when
     * one is done using the MediaScanner object. After this method
     * is called, the MediaScanner object can no longer be used.
     */
    public void release() {
        native_finalize();
    }

    @Override
    protected void finalize() {
        mContext.getContentResolver().releaseProvider(mMediaProvider);
        native_finalize();
    }
    
    private static final String MIME_APPLICATION_OCTET_STREAM = "application/octet-stream";
    private boolean isValueslessMimeType(String mimetype) {
        boolean valueless = false;
        if (MIME_APPLICATION_OCTET_STREAM.equalsIgnoreCase(mimetype)) {
            valueless = true;
            if (LOG) Log.v(TAG, "isValueslessMimeType: mimetype=" + mimetype);
        }
        return valueless;
    }
}
