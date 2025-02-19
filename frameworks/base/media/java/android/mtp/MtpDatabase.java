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

package android.mtp;

import android.content.Context;
import android.content.ContentValues;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScanner;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

//Added for USB Develpment debug, more log for more debuging help
import com.mediatek.xlog.SXlog;
import java.lang.Integer;
//Added Modification for ALPS00278882
import android.os.SystemProperties;
//Added Modification for ALPS00278882
//Added for USB Develpment debug, more log for more debuging help

/**
 * {@hide}
 */
public class MtpDatabase {

    private static final String TAG = "MtpDatabase";

    private final Context mContext;
    private final IContentProvider mMediaProvider;
    private final String mVolumeName;
    private final Uri mObjectsUri;
    // path to primary storage
    private final String mMediaStoragePath;
    // if not null, restrict all queries to these subdirectories
    private final String[] mSubDirectories;
    // where clause for restricting queries to files in mSubDirectories
    private String mSubDirectoriesWhere;
    // where arguments for restricting queries to files in mSubDirectories
    private String[] mSubDirectoriesWhereArgs;

    private final HashMap<String, MtpStorage> mStorageMap = new HashMap<String, MtpStorage>();

    // cached property groups for single properties
    private final HashMap<Integer, MtpPropertyGroup> mPropertyGroupsByProperty
            = new HashMap<Integer, MtpPropertyGroup>();

    // cached property groups for all properties for a given format
    private final HashMap<Integer, MtpPropertyGroup> mPropertyGroupsByFormat
            = new HashMap<Integer, MtpPropertyGroup>();

    // true if the database has been modified in the current MTP session
    private boolean mDatabaseModified;

    // SharedPreferences for writable MTP device properties
    private SharedPreferences mDeviceProperties;
    private static final int DEVICE_PROPERTIES_DATABASE_VERSION = 1;

    private static final String[] ID_PROJECTION = new String[] {
            Files.FileColumns._ID, // 0
    };
    private static final String[] PATH_PROJECTION = new String[] {
            Files.FileColumns._ID, // 0
            Files.FileColumns.DATA, // 1
    };
    private static final String[] PATH_SIZE_FORMAT_PROJECTION = new String[] {
            Files.FileColumns._ID, // 0
            Files.FileColumns.DATA, // 1
            Files.FileColumns.SIZE, // 2
            Files.FileColumns.FORMAT, // 3
    };
    private static final String[] OBJECT_INFO_PROJECTION = new String[] {
            Files.FileColumns._ID, // 0
            Files.FileColumns.STORAGE_ID, // 1
            Files.FileColumns.FORMAT, // 2
            Files.FileColumns.PARENT, // 3
            Files.FileColumns.DATA, // 4
            Files.FileColumns.SIZE, // 5
            Files.FileColumns.DATE_MODIFIED, // 6
    };
    private static final String ID_WHERE = Files.FileColumns._ID + "=?";
    private static final String PATH_WHERE = Files.FileColumns.DATA + "=?";

    private static final String STORAGE_WHERE = Files.FileColumns.STORAGE_ID + "=?";
    //Added Modification for ALPS00255822, bug from WHQL test
    //The format/parent query string are inverted
    /*private static final String FORMAT_WHERE = Files.FileColumns.PARENT + "=?";
    private static final String PARENT_WHERE = Files.FileColumns.FORMAT + "=?";*/
    private static final String FORMAT_WHERE = Files.FileColumns.FORMAT + "=?";
    private static final String PARENT_WHERE = Files.FileColumns.PARENT + "=?";
    //Added Modification for ALPS00255822, bug from WHQL test
    private static final String STORAGE_FORMAT_WHERE = STORAGE_WHERE + " AND "
                                            + Files.FileColumns.FORMAT + "=?";
    private static final String STORAGE_PARENT_WHERE = STORAGE_WHERE + " AND "
                                            + Files.FileColumns.PARENT + "=?";
    private static final String FORMAT_PARENT_WHERE = FORMAT_WHERE + " AND "
                                            + Files.FileColumns.PARENT + "=?";
    private static final String STORAGE_FORMAT_PARENT_WHERE = STORAGE_FORMAT_WHERE + " AND "
                                            + Files.FileColumns.PARENT + "=?";

    private final MediaScanner mMediaScanner;

    static {
        System.loadLibrary("media_jni");
    }

    public MtpDatabase(Context context, String volumeName, String storagePath,
            String[] subDirectories) {
        native_setup();

        mContext = context;
        mMediaProvider = context.getContentResolver().acquireProvider("media");
        mVolumeName = volumeName;
        mMediaStoragePath = storagePath;
        mObjectsUri = Files.getMtpObjectsUri(volumeName);
        mMediaScanner = new MediaScanner(context);

        mSubDirectories = subDirectories;
        if (subDirectories != null) {
            // Compute "where" string for restricting queries to subdirectories
            StringBuilder builder = new StringBuilder();
            builder.append("(");
            int count = subDirectories.length;
            for (int i = 0; i < count; i++) {
                builder.append(Files.FileColumns.DATA + "=? OR "
                        + Files.FileColumns.DATA + " LIKE ?");
                if (i != count - 1) {
                    builder.append(" OR ");
                }
            }
            builder.append(")");
            mSubDirectoriesWhere = builder.toString();

            // Compute "where" arguments for restricting queries to subdirectories
            mSubDirectoriesWhereArgs = new String[count * 2];
            for (int i = 0, j = 0; i < count; i++) {
                String path = subDirectories[i];
                mSubDirectoriesWhereArgs[j++] = path;
                mSubDirectoriesWhereArgs[j++] = path + "/%";
            }
        }

        // Set locale to MediaScanner.
        Locale locale = context.getResources().getConfiguration().locale;
        if (locale != null) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            if (language != null) {
                if (country != null) {
                    mMediaScanner.setLocale(language + "_" + country);
                } else {
                    mMediaScanner.setLocale(language);
                }
            }
        }
        initDeviceProperties(context);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            native_finalize();
        } finally {
            super.finalize();
        }
    }

    public void addStorage(MtpStorage storage) {
        mStorageMap.put(storage.getPath(), storage);
    }

    public void removeStorage(MtpStorage storage) {
        mStorageMap.remove(storage.getPath());
    }

    private void initDeviceProperties(Context context) {
        final String devicePropertiesName = "device-properties";
        mDeviceProperties = context.getSharedPreferences(devicePropertiesName, Context.MODE_PRIVATE);
        File databaseFile = context.getDatabasePath(devicePropertiesName);

        if (databaseFile.exists()) {
            // for backward compatibility - read device properties from sqlite database
            // and migrate them to shared prefs
            SQLiteDatabase db = null;
            Cursor c = null;
            try {
                db = context.openOrCreateDatabase("device-properties", Context.MODE_PRIVATE, null);
                if (db != null) {
                    c = db.query("properties", new String[] { "_id", "code", "value" },
                            null, null, null, null, null);
                    if (c != null) {
                        SharedPreferences.Editor e = mDeviceProperties.edit();
                        while (c.moveToNext()) {
                            String name = c.getString(1);
                            String value = c.getString(2);
                            e.putString(name, value);
                        }
                        e.commit();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "failed to migrate device properties", e);
            } finally {
                if (c != null) c.close();
                if (db != null) db.close();
            }
            databaseFile.delete();
        }
    }

    // check to see if the path is contained in one of our storage subdirectories
    // returns true if we have no special subdirectories
    private boolean inStorageSubDirectory(String path) {
        if (mSubDirectories == null) return true;
        if (path == null) return false;

        boolean allowed = false;
        int pathLength = path.length();
        for (int i = 0; i < mSubDirectories.length && !allowed; i++) {
            String subdir = mSubDirectories[i];
            int subdirLength = subdir.length();
            if (subdirLength < pathLength &&
                    path.charAt(subdirLength) == '/' &&
                    path.startsWith(subdir)) {
                allowed = true;
            }
        }
        return allowed;
    }

    // check to see if the path matches one of our storage subdirectories
    // returns true if we have no special subdirectories
    private boolean isStorageSubDirectory(String path) {
    if (mSubDirectories == null) return false;
        for (int i = 0; i < mSubDirectories.length; i++) {
            if (path.equals(mSubDirectories[i])) {
                return true;
            }
        }
        return false;
    }

    private int beginSendObject(String path, int format, int parent,
                         int storageId, long size, long modified) {
        // if mSubDirectories is not null, do not allow copying files to any other locations
        if (!inStorageSubDirectory(path)) return -1;

        // make sure the object does not exist
        if (path != null) {
			//Added for USB Develpment debug, more log for more debuging help
			SXlog.d(TAG, "beginSendObject: path = "+ path);
			//Added for USB Develpment debug, more log for more debuging help
            Cursor c = null;
            try {
                c = mMediaProvider.query(mObjectsUri, ID_PROJECTION, PATH_WHERE,
                        new String[] { path }, null);
				//Added for USB Develpment debug, more log for more debuging help
				SXlog.i(TAG, "beginSendObject: mObjectsUri = "+ mObjectsUri);
				//Added for USB Develpment debug, more log for more debuging help

                if (c != null && c.getCount() > 0) {
					//Added for USB Develpment debug, more log for more debuging help
                    SXlog.e(TAG, "beginSendObject file already exists in beginSendObject: " + path);
					//Added for USB Develpment debug, more log for more debuging help
				    //Added Modification for ALPS00255822, bug from WHQL test
                    //replace with writeprotected
                    return MtpConstants.RESPONSE_OBJECT_WRITE_PROTECTED;
				    //Added Modification for ALPS00255822, bug from WHQL test
                }
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in beginSendObject", e);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        mDatabaseModified = true;
        ContentValues values = new ContentValues();
        values.put(Files.FileColumns.DATA, path);
        values.put(Files.FileColumns.FORMAT, format);
        values.put(Files.FileColumns.PARENT, parent);
        values.put(Files.FileColumns.STORAGE_ID, storageId);
        values.put(Files.FileColumns.SIZE, size);
        values.put(Files.FileColumns.DATE_MODIFIED, modified);
		//Added for USB Develpment debug, more log for more debuging help
		SXlog.i(TAG, "beginSendObject: path = "+path);
		SXlog.i(TAG, "beginSendObject: FORMAT = "+Integer.toHexString(format));
		SXlog.i(TAG, "beginSendObject: PARENT = "+Integer.toHexString(parent));
		SXlog.i(TAG, "beginSendObject: storageId = "+Integer.toHexString(storageId));
		SXlog.i(TAG, "beginSendObject: size = "+size);
		SXlog.i(TAG, "beginSendObject: DATE_MODIFIED = "+modified);
		//Added for USB Develpment debug, more log for more debuging help

        try {
            Uri uri = mMediaProvider.insert(mObjectsUri, values);
            if (uri != null) {
	            notifyBeginSend(mMediaProvider, path);
				//Log.e(TAG, "NO notifyNeginSend!");
                return Integer.parseInt(uri.getPathSegments().get(2));
            } else {
				//Added for USB Develpment debug, more log for more debuging help
				SXlog.e(TAG, "beginSendObject: uri insert failed!!");
				//Added for USB Develpment debug, more log for more debuging help
                return -1;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in beginSendObject", e);
            return -1;
        }
    }

    private void endSendObject(String path, int handle, int format, boolean succeeded) {
        notifyEndSend(mMediaProvider);
        //Log.e(TAG, "NO notifyEndSend!");
		//Added for USB Develpment debug, more log for more debuging help
		SXlog.d(TAG, "endSendObject: path = "+path + ", format = 0x"+ Integer.toHexString(format));
		//Added for USB Develpment debug, more log for more debuging help
        if (succeeded) {
            // handle abstract playlists separately
            // they do not exist in the file system so don't use the media scanner here
            if (format == MtpConstants.FORMAT_ABSTRACT_AV_PLAYLIST) {
                // extract name from path
                String name = path;
                int lastSlash = name.lastIndexOf('/');
                if (lastSlash >= 0) {
                    name = name.substring(lastSlash + 1);
                }
                // strip trailing ".pla" from the name
                if (name.endsWith(".pla")) {
                    name = name.substring(0, name.length() - 4);
                }

                ContentValues values = new ContentValues(1);
                values.put(Audio.Playlists.DATA, path);
                values.put(Audio.Playlists.NAME, name);
                values.put(Files.FileColumns.FORMAT, format);
                values.put(Files.FileColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000);
                values.put(MediaColumns.MEDIA_SCANNER_NEW_OBJECT_ID, handle);
                try {
                    Uri uri = mMediaProvider.insert(Audio.Playlists.EXTERNAL_CONTENT_URI, values);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in endSendObject", e);
                }
            } else {

				//Added for USB Develpment debug, more log for more debuging help
				SXlog.d(TAG, "endSendObject: mVolumeName = "+mVolumeName);
				//Added for USB Develpment debug, more log for more debuging help
                mMediaScanner.scanMtpFile(path, mVolumeName, handle, format);
            }
        } else {
            deleteFile(handle);
        }
    }

    private Cursor createObjectQuery(int storageID, int format, int parent) throws RemoteException {
        String where;
        String[] whereArgs;

		//Added for USB Develpment debug, more log for more debuging help
		SXlog.i(TAG, "createObjectQuery: storageID = 0x "+Integer.toHexString(storageID));
		SXlog.i(TAG, "createObjectQuery: format = 0x "+Integer.toHexString(format));
		SXlog.i(TAG, "createObjectQuery: parent = 0x "+Integer.toHexString(parent));
		//Added for USB Develpment debug, more log for more debuging help
        if (storageID == 0xFFFFFFFF) {
            // query all stores
            if (format == 0) {
                // query all formats
                if (parent == 0) {
                    // query all objects
                    where = null;
                    whereArgs = null;
                } else {
                    if (parent == 0xFFFFFFFF) {
                        // all objects in root of store
                        parent = 0;
                    }
                    where = PARENT_WHERE;
                    whereArgs = new String[] { Integer.toString(parent) };
                }
            } else {
                // query specific format
                if (parent == 0) {
                    // query all objects
                    where = FORMAT_WHERE;
                    whereArgs = new String[] { Integer.toString(format) };
                } else {
                    if (parent == 0xFFFFFFFF) {
                        // all objects in root of store
                        parent = 0;
                    }
                    where = FORMAT_PARENT_WHERE;
                    whereArgs = new String[] { Integer.toString(format),
                                               Integer.toString(parent) };
                }
            }
        } else {
            // query specific store
            if (format == 0) {
                // query all formats
                if (parent == 0) {
                    // query all objects
                    where = STORAGE_WHERE;
                    whereArgs = new String[] { Integer.toString(storageID) };
                } else {
                    if (parent == 0xFFFFFFFF) {
                        // all objects in root of store
                        parent = 0;
                    }
                    where = STORAGE_PARENT_WHERE;
                    whereArgs = new String[] { Integer.toString(storageID),
                                               Integer.toString(parent) };
                }
            } else {
                // query specific format
                if (parent == 0) {
                    // query all objects
                    where = STORAGE_FORMAT_WHERE;
                    whereArgs = new String[] {  Integer.toString(storageID),
                                                Integer.toString(format) };
                } else {
                    if (parent == 0xFFFFFFFF) {
                        // all objects in root of store
                        parent = 0;
                    }
                    where = STORAGE_FORMAT_PARENT_WHERE;
                    whereArgs = new String[] { Integer.toString(storageID),
                                               Integer.toString(format),
                                               Integer.toString(parent) };
                }
            }
        }

        // if we are restricting queries to mSubDirectories, we need to add the restriction
        // onto our "where" arguments
        if (mSubDirectoriesWhere != null) {
			//Added for USB Develpment debug, more log for more debuging help
			SXlog.i(TAG, "createObjectQuery: mSubDirectoriesWhere = "+mSubDirectoriesWhere);
			//Added for USB Develpment debug, more log for more debuging help
            if (where == null) {
                where = mSubDirectoriesWhere;
                whereArgs = mSubDirectoriesWhereArgs;
            } else {
                where = where + " AND " + mSubDirectoriesWhere;

                // create new array to hold whereArgs and mSubDirectoriesWhereArgs
                String[] newWhereArgs =
                        new String[whereArgs.length + mSubDirectoriesWhereArgs.length];
                int i, j;
                for (i = 0; i < whereArgs.length; i++) {
                    newWhereArgs[i] = whereArgs[i];
                }
                for (j = 0; j < mSubDirectoriesWhereArgs.length; i++, j++) {
                    newWhereArgs[i] = mSubDirectoriesWhereArgs[j];
                }
                whereArgs = newWhereArgs;
            }
        }
		else
		{
			//Added for USB Develpment debug, more log for more debuging help
			SXlog.e(TAG, "createObjectQuery: mSubDirectoriesWhere = null");
			//Added for USB Develpment debug, more log for more debuging help
		}

        return mMediaProvider.query(mObjectsUri, ID_PROJECTION, where, whereArgs, null);
    }

    private int[] getObjectList(int storageID, int format, int parent) {
        Cursor c = null;
        try {
			//Added for USB Develpment debug, more log for more debuging help
			SXlog.i(TAG, "getObjectList: storageID = 0x " + Integer.toHexString(storageID));
			SXlog.i(TAG, "getObjectList: format = 0x" + Integer.toHexString(format));
			SXlog.i(TAG, "getObjectList: parent = 0x" + Integer.toHexString(parent));
			//Added for USB Develpment debug, more log for more debuging help
            c = createObjectQuery(storageID, format, parent);
            if (c == null) {
				//Modeification for ALPS00326143
				SXlog.i(TAG, "getObjectList: c == null!!");
				//Modeification for ALPS00326143

                return null;
            }
            int count = c.getCount();
			//Added for USB Develpment debug, more log for more debuging help
			SXlog.d(TAG, "getObjectList: count = " + count);
			//Added for USB Develpment debug, more log for more debuging help
            if (count > 0) {
                int[] result = new int[count];
                for (int i = 0; i < count; i++) {
                    c.moveToNext();
                    result[i] = c.getInt(0);
					//Added for USB Develpment debug, more log for more debuging help
					SXlog.i(TAG, "getObjectList: result["+i+"] = 0x " + Integer.toHexString(result[i]));
					//Added for USB Develpment debug, more log for more debuging help
                }
                return result;
            }
			//Modeification for ALPS00326143
			else
			{
                int[] result = new int[1];
				result[0] = 0;
				SXlog.i(TAG, "getObjectList: result[0] = 0x00!!");
				return result;
			}
			//Modeification for ALPS00326143
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getObjectList", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    private int getNumObjects(int storageID, int format, int parent) {
        Cursor c = null;
        try {
            c = createObjectQuery(storageID, format, parent);
            if (c != null) {
                return c.getCount();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getNumObjects", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return -1;
    }

    private int[] getSupportedPlaybackFormats() {
        return new int[] {
            // allow transfering arbitrary files
            MtpConstants.FORMAT_UNDEFINED,

            MtpConstants.FORMAT_ASSOCIATION,
            MtpConstants.FORMAT_TEXT,
            MtpConstants.FORMAT_HTML,
            MtpConstants.FORMAT_WAV,
            MtpConstants.FORMAT_MP3,
            MtpConstants.FORMAT_MPEG,
            MtpConstants.FORMAT_EXIF_JPEG,
            MtpConstants.FORMAT_TIFF_EP,
            MtpConstants.FORMAT_GIF,
            MtpConstants.FORMAT_JFIF,
            MtpConstants.FORMAT_PNG,
            MtpConstants.FORMAT_TIFF,
            MtpConstants.FORMAT_WMA,
            MtpConstants.FORMAT_OGG,
            MtpConstants.FORMAT_AAC,
            MtpConstants.FORMAT_MP4_CONTAINER,
            MtpConstants.FORMAT_MP2,
            MtpConstants.FORMAT_3GP_CONTAINER,
            MtpConstants.FORMAT_ABSTRACT_AV_PLAYLIST,
            MtpConstants.FORMAT_WPL_PLAYLIST,
            MtpConstants.FORMAT_M3U_PLAYLIST,
            MtpConstants.FORMAT_PLS_PLAYLIST,
            MtpConstants.FORMAT_XML_DOCUMENT,
            MtpConstants.FORMAT_FLAC,
        };
    }

    private int[] getSupportedCaptureFormats() {
        // no capture formats yet
        return null;
    }

    static final int[] FILE_PROPERTIES = {
            // NOTE must match beginning of AUDIO_PROPERTIES, VIDEO_PROPERTIES
            // and IMAGE_PROPERTIES below
            MtpConstants.PROPERTY_STORAGE_ID,
            MtpConstants.PROPERTY_OBJECT_FORMAT,
            MtpConstants.PROPERTY_PROTECTION_STATUS,
            MtpConstants.PROPERTY_OBJECT_SIZE,
            MtpConstants.PROPERTY_OBJECT_FILE_NAME,
            MtpConstants.PROPERTY_DATE_MODIFIED,
            MtpConstants.PROPERTY_PARENT_OBJECT,
            MtpConstants.PROPERTY_PERSISTENT_UID,
            MtpConstants.PROPERTY_NAME,
            MtpConstants.PROPERTY_DATE_ADDED,
    };

    static final int[] AUDIO_PROPERTIES = {
            // NOTE must match FILE_PROPERTIES above
            MtpConstants.PROPERTY_STORAGE_ID,
            MtpConstants.PROPERTY_OBJECT_FORMAT,
            MtpConstants.PROPERTY_PROTECTION_STATUS,
            MtpConstants.PROPERTY_OBJECT_SIZE,
            MtpConstants.PROPERTY_OBJECT_FILE_NAME,
            MtpConstants.PROPERTY_DATE_MODIFIED,
            MtpConstants.PROPERTY_PARENT_OBJECT,
            MtpConstants.PROPERTY_PERSISTENT_UID,
            MtpConstants.PROPERTY_NAME,
            MtpConstants.PROPERTY_DISPLAY_NAME,
            MtpConstants.PROPERTY_DATE_ADDED,

            // audio specific properties
            MtpConstants.PROPERTY_ARTIST,
            MtpConstants.PROPERTY_ALBUM_NAME,
            MtpConstants.PROPERTY_ALBUM_ARTIST,
            MtpConstants.PROPERTY_TRACK,
            MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE,
            MtpConstants.PROPERTY_DURATION,
            MtpConstants.PROPERTY_GENRE,
            MtpConstants.PROPERTY_COMPOSER,
    };

    static final int[] VIDEO_PROPERTIES = {
            // NOTE must match FILE_PROPERTIES above
            MtpConstants.PROPERTY_STORAGE_ID,
            MtpConstants.PROPERTY_OBJECT_FORMAT,
            MtpConstants.PROPERTY_PROTECTION_STATUS,
            MtpConstants.PROPERTY_OBJECT_SIZE,
            MtpConstants.PROPERTY_OBJECT_FILE_NAME,
            MtpConstants.PROPERTY_DATE_MODIFIED,
            MtpConstants.PROPERTY_PARENT_OBJECT,
            MtpConstants.PROPERTY_PERSISTENT_UID,
            MtpConstants.PROPERTY_NAME,
            MtpConstants.PROPERTY_DISPLAY_NAME,
            MtpConstants.PROPERTY_DATE_ADDED,

            // video specific properties
            MtpConstants.PROPERTY_ARTIST,
            MtpConstants.PROPERTY_ALBUM_NAME,
            MtpConstants.PROPERTY_DURATION,
            MtpConstants.PROPERTY_DESCRIPTION,
    };

    static final int[] IMAGE_PROPERTIES = {
            // NOTE must match FILE_PROPERTIES above
            MtpConstants.PROPERTY_STORAGE_ID,
            MtpConstants.PROPERTY_OBJECT_FORMAT,
            MtpConstants.PROPERTY_PROTECTION_STATUS,
            MtpConstants.PROPERTY_OBJECT_SIZE,
            MtpConstants.PROPERTY_OBJECT_FILE_NAME,
            MtpConstants.PROPERTY_DATE_MODIFIED,
            MtpConstants.PROPERTY_PARENT_OBJECT,
            MtpConstants.PROPERTY_PERSISTENT_UID,
            MtpConstants.PROPERTY_NAME,
            MtpConstants.PROPERTY_DISPLAY_NAME,
            MtpConstants.PROPERTY_DATE_ADDED,

            // image specific properties
            MtpConstants.PROPERTY_DESCRIPTION,
    };

    static final int[] ALL_PROPERTIES = {
            // NOTE must match FILE_PROPERTIES above
            MtpConstants.PROPERTY_STORAGE_ID,
            MtpConstants.PROPERTY_OBJECT_FORMAT,
            MtpConstants.PROPERTY_PROTECTION_STATUS,
            MtpConstants.PROPERTY_OBJECT_SIZE,
            MtpConstants.PROPERTY_OBJECT_FILE_NAME,
            MtpConstants.PROPERTY_DATE_MODIFIED,
            MtpConstants.PROPERTY_PARENT_OBJECT,
            MtpConstants.PROPERTY_PERSISTENT_UID,
            MtpConstants.PROPERTY_NAME,
            MtpConstants.PROPERTY_DISPLAY_NAME,
            MtpConstants.PROPERTY_DATE_ADDED,

            // image specific properties
            MtpConstants.PROPERTY_DESCRIPTION,

            // audio specific properties
            MtpConstants.PROPERTY_ARTIST,
            MtpConstants.PROPERTY_ALBUM_NAME,
            MtpConstants.PROPERTY_ALBUM_ARTIST,
            MtpConstants.PROPERTY_TRACK,
            MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE,
            MtpConstants.PROPERTY_DURATION,
            MtpConstants.PROPERTY_GENRE,
            MtpConstants.PROPERTY_COMPOSER,

            // video specific properties
            MtpConstants.PROPERTY_ARTIST,
            MtpConstants.PROPERTY_ALBUM_NAME,
            MtpConstants.PROPERTY_DURATION,
            MtpConstants.PROPERTY_DESCRIPTION,

            // image specific properties
            MtpConstants.PROPERTY_DESCRIPTION,
    };

    private int[] getSupportedObjectProperties(int format) {

		//ALPS00120037, add log for support MTP debugging
		//Log.w(TAG, "getSupportedObjectProperties format = " + format);
		//ALPS00120037, add log for support MTP debugging
        switch (format) {
            case MtpConstants.FORMAT_MP3:
            case MtpConstants.FORMAT_WAV:
            case MtpConstants.FORMAT_WMA:
            case MtpConstants.FORMAT_OGG:
            case MtpConstants.FORMAT_AAC:
                return AUDIO_PROPERTIES;
            case MtpConstants.FORMAT_MPEG:
            case MtpConstants.FORMAT_3GP_CONTAINER:
            case MtpConstants.FORMAT_WMV:
                return VIDEO_PROPERTIES;
            case MtpConstants.FORMAT_EXIF_JPEG:
            case MtpConstants.FORMAT_GIF:
            case MtpConstants.FORMAT_PNG:
            case MtpConstants.FORMAT_BMP:
                return IMAGE_PROPERTIES;
            case 0:
                return ALL_PROPERTIES;
            default:
                return FILE_PROPERTIES;
        }
    }

    private int[] getSupportedDeviceProperties() {
        return new int[] {
            MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER,
            MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME,
            MtpConstants.DEVICE_PROPERTY_IMAGE_SIZE,
        };
    }


    private MtpPropertyList getObjectPropertyList(long handle, int format, long property,
                        int groupCode, int depth) {

		//ALPS00120037, add log for support MTP debugging
		SXlog.d(TAG, "getObjectPropertyList: handle = 0x" + Long.toHexString(handle) + ", property = 0x" + Long.toHexString(property));
		//ALPS00120037, add log for support MTP debugging

        // FIXME - implement group support
        if (groupCode != 0) {
			//Added for USB Develpment debug, more log for more debuging help
			SXlog.i(TAG, "getObjectPropertyList RESPONSE_SPECIFICATION_BY_GROUP_UNSUPPORTED");
			//Added for USB Develpment debug, more log for more debuging help
            return new MtpPropertyList(0, MtpConstants.RESPONSE_SPECIFICATION_BY_GROUP_UNSUPPORTED);
        }

        MtpPropertyGroup propertyGroup;
        if (property == 0xFFFFFFFFL) {
             propertyGroup = mPropertyGroupsByFormat.get(format);
             if (propertyGroup == null) {
                int[] propertyList = getSupportedObjectProperties(format);
                propertyGroup = new MtpPropertyGroup(this, mMediaProvider, mVolumeName, propertyList);
                mPropertyGroupsByFormat.put(new Integer(format), propertyGroup);
            }
        } else {
              propertyGroup = mPropertyGroupsByProperty.get(property);
             if (propertyGroup == null) {
                int[] propertyList = new int[] { (int)property };
                propertyGroup = new MtpPropertyGroup(this, mMediaProvider, mVolumeName, propertyList);
                mPropertyGroupsByProperty.put(new Integer((int)property), propertyGroup);
            }
        }

        return propertyGroup.getPropertyList((int)handle, format, depth);
    }

    private int renameFile(int handle, String newName) {
        Cursor c = null;

        // first compute current path
        String path = null;
        String[] whereArgs = new String[] {  Integer.toString(handle) };
        try {
            c = mMediaProvider.query(mObjectsUri, PATH_PROJECTION, ID_WHERE, whereArgs, null);
            if (c != null && c.moveToNext()) {
                path = c.getString(1);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getObjectFilePath", e);
            return MtpConstants.RESPONSE_GENERAL_ERROR;
        } finally {
            if (c != null) {
                c.close();
            }
        }
        if (path == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }

        // do not allow renaming any of the special subdirectories
        if (isStorageSubDirectory(path)) {
            return MtpConstants.RESPONSE_OBJECT_WRITE_PROTECTED;
        }

        // now rename the file.  make sure this succeeds before updating database
        File oldFile = new File(path);
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 1) {
            return MtpConstants.RESPONSE_GENERAL_ERROR;
        }
        String newPath = path.substring(0, lastSlash + 1) + newName;
        File newFile = new File(newPath);
        boolean success = oldFile.renameTo(newFile);
        if (!success) {
		    //Added Modification for ALPS00255822, bug from WHQL test
			//confirm that if the old name and new name are the same
			if(newPath.equals(path))
			{
				//nothing to do
				SXlog.e(TAG, "renaming "+ path + " to " + newPath + " is equal");
				//going!!	
			}
			else
			{
		    //Added Modification for ALPS00255822, bug from WHQL test
            SXlog.e(TAG, "renaming "+ path + " to " + newPath + " failed");
            return MtpConstants.RESPONSE_GENERAL_ERROR;
		    //Added Modification for ALPS00255822, bug from WHQL test
			}
		    //Added Modification for ALPS00255822, bug from WHQL test
        }

        // finally update database
        ContentValues values = new ContentValues();
        values.put(Files.FileColumns.DATA, newPath);
        int updated = 0;
        try {
            // note - we are relying on a special case in MediaProvider.update() to update
            // the paths for all children in the case where this is a directory.
            updated = mMediaProvider.update(mObjectsUri, values, ID_WHERE, whereArgs);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in mMediaProvider.update", e);
        }
        if (updated == 0) {
            Log.e(TAG, "Unable to update path for " + path + " to " + newPath);
            // this shouldn't happen, but if it does we need to rename the file to its original name
            newFile.renameTo(oldFile);
            return MtpConstants.RESPONSE_GENERAL_ERROR;
        }

        return MtpConstants.RESPONSE_OK;
    }

    private int setObjectProperty(int handle, int property,
                            long intValue, String stringValue) {
        switch (property) {
            case MtpConstants.PROPERTY_OBJECT_FILE_NAME:
                return renameFile(handle, stringValue);

            default:
                return MtpConstants.RESPONSE_OBJECT_PROP_NOT_SUPPORTED;
        }
    }

    private int getDeviceProperty(int property, long[] outIntValue, char[] outStringValue) {
		//Added for USB Develpment debug, more log for more debuging help
		SXlog.d(TAG, "getDeviceProperty  property = 0x" + Integer.toHexString(property));
		//Added for USB Develpment debug, more log for more debuging help

        switch (property) {
            case MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER:
            case MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME:
                // writable string properties kept in shared preferences
                String value = mDeviceProperties.getString(Integer.toString(property), "");
                int length = value.length();
                if (length > 255) {
                    length = 255;
                }
                value.getChars(0, length, outStringValue, 0);
                outStringValue[length] = 0;
				//Added for USB Develpment debug, more log for more debuging help
				if(length >0)
				{
					SXlog.i(TAG, "getDeviceProperty  property = " + Integer.toHexString(property));
					SXlog.i(TAG, "getDeviceProperty  value = " + value + ", length = " + length);
				}
				else
				{
					SXlog.i(TAG, "getDeviceProperty  length = " + length);
				    //Added Modification for ALPS00278882
					if(property == MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME)
					{
						//return the device name for the PC display if the FriendlyName is empty!!
						String deviceName;
						deviceName = SystemProperties.get("ro.product.name");
						
						int lengthDeviceName = deviceName.length();
						if (lengthDeviceName > 255) {
							lengthDeviceName = 255;
						}
						if(lengthDeviceName >0)
						{
							deviceName.getChars(0, lengthDeviceName, outStringValue, 0);
							outStringValue[lengthDeviceName] = 0;
							SXlog.d(TAG, "getDeviceProperty  deviceName = " + deviceName + ", lengthDeviceName = " + lengthDeviceName);
						}
						else
							SXlog.d(TAG, "getDeviceProperty  lengthDeviceName = " + lengthDeviceName);
							
					}
				    //Added Modification for ALPS00278882
				}
				//Added for USB Develpment debug, more log for more debuging help
                return MtpConstants.RESPONSE_OK;

            case MtpConstants.DEVICE_PROPERTY_IMAGE_SIZE:
                // use screen size as max image size
                Display display = ((WindowManager)mContext.getSystemService(
                        Context.WINDOW_SERVICE)).getDefaultDisplay();
                int width = display.getMaximumSizeDimension();
                int height = display.getMaximumSizeDimension();
				//Added for USB Develpment debug, more log for more debuging help
				SXlog.i(TAG, "getDeviceProperty DEVICE_PROPERTY_IMAGE_SIZE  width = " + width + ", height = " + height);
				//Added for USB Develpment debug, more log for more debuging help
                String imageSize = Integer.toString(width) + "x" +  Integer.toString(height);
                imageSize.getChars(0, imageSize.length(), outStringValue, 0);
                outStringValue[imageSize.length()] = 0;
				//Added for USB Develpment debug, more log for more debuging help
				SXlog.i(TAG, "getDeviceProperty DEVICE_PROPERTY_IMAGE_SIZE imageSize = " + imageSize);
				//Added for USB Develpment debug, more log for more debuging help
                return MtpConstants.RESPONSE_OK;

            default:
                return MtpConstants.RESPONSE_DEVICE_PROP_NOT_SUPPORTED;
        }
    }

    private int setDeviceProperty(int property, long intValue, String stringValue) {
        switch (property) {
            case MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER:
            case MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME:
                // writable string properties kept in shared prefs
                SharedPreferences.Editor e = mDeviceProperties.edit();
                e.putString(Integer.toString(property), stringValue);
                return (e.commit() ? MtpConstants.RESPONSE_OK
                        : MtpConstants.RESPONSE_GENERAL_ERROR);
        }

        return MtpConstants.RESPONSE_DEVICE_PROP_NOT_SUPPORTED;
    }

    private boolean getObjectInfo(int handle, int[] outStorageFormatParent,
                        char[] outName, long[] outSizeModified) {
        Cursor c = null;
		//ALPS00120037, add log for support MTP debugging
		Log.w(TAG, "getObjectInfo");
		//ALPS00120037, add log for support MTP debugging
        try {
            c = mMediaProvider.query(mObjectsUri, OBJECT_INFO_PROJECTION,
                            ID_WHERE, new String[] {  Integer.toString(handle) }, null);
            if (c != null && c.moveToNext()) {
                outStorageFormatParent[0] = c.getInt(1);
                outStorageFormatParent[1] = c.getInt(2);
                outStorageFormatParent[2] = c.getInt(3);

                // extract name from path
                String path = c.getString(4);
                int lastSlash = path.lastIndexOf('/');
                int start = (lastSlash >= 0 ? lastSlash + 1 : 0);
                int end = path.length();
                if (end - start > 255) {
                    end = start + 255;
                }
                path.getChars(start, end, outName, 0);
                outName[end - start] = 0;

				//ALPS00120037, add log for support MTP debugging
				//Log.w(TAG, "getObjectInfo outName = " + outName);
				//ALPS00120037, add log for support MTP debugging

                outSizeModified[0] = c.getLong(5);
                outSizeModified[1] = c.getLong(6);
                return true;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getObjectInfo", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return false;
    }

    private int getObjectFilePath(int handle, char[] outFilePath, long[] outFileLengthFormat) {
		//Added for USB Develpment debug, more log for more debuging help
		SXlog.i(TAG, "getObjectFilePath handle = " + Integer.toHexString(handle));
		//Added for USB Develpment debug, more log for more debuging help

        if (handle == 0) {
            // special case root directory
            mMediaStoragePath.getChars(0, mMediaStoragePath.length(), outFilePath, 0);
            outFilePath[mMediaStoragePath.length()] = 0;
            outFileLengthFormat[0] = 0;
            outFileLengthFormat[1] = MtpConstants.FORMAT_ASSOCIATION;
            return MtpConstants.RESPONSE_OK;
        }
        Cursor c = null;
        try {
            c = mMediaProvider.query(mObjectsUri, PATH_SIZE_FORMAT_PROJECTION,
                            ID_WHERE, new String[] {  Integer.toString(handle) }, null);
            if (c != null && c.moveToNext()) {
                String path = c.getString(1);
                path.getChars(0, path.length(), outFilePath, 0);
                outFilePath[path.length()] = 0;
                outFileLengthFormat[0] = c.getLong(2);
                outFileLengthFormat[1] = c.getLong(3);
				//Added for USB Develpment debug, more log for more debuging help
				SXlog.i(TAG, "getObjectFilePath RESPONSE_OK: path = " + path);
				//Added for USB Develpment debug, more log for more debuging help
				
                return MtpConstants.RESPONSE_OK;
            } else {
				//Added for USB Develpment debug, more log for more debuging help
				SXlog.i(TAG, "getObjectFilePath RESPONSE_INVALID_OBJECT_HANDLE, handle = " + handle);
				//Added for USB Develpment debug, more log for more debuging help
                return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getObjectFilePath", e);
            return MtpConstants.RESPONSE_GENERAL_ERROR;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private int deleteFile(int handle) {
        mDatabaseModified = true;
        String path = null;
        int format = 0;

        Cursor c = null;
        try {
            c = mMediaProvider.query(mObjectsUri, PATH_SIZE_FORMAT_PROJECTION,
                            ID_WHERE, new String[] {  Integer.toString(handle) }, null);
            if (c != null && c.moveToNext()) {
                // don't convert to media path here, since we will be matching
                // against paths in the database matching /data/media
                path = c.getString(1);
                format = c.getInt(3);
            } else {
                return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
            }

            if (path == null || format == 0) {
                return MtpConstants.RESPONSE_GENERAL_ERROR;
            }

            // do not allow deleting any of the special subdirectories
            if (isStorageSubDirectory(path)) {
                return MtpConstants.RESPONSE_OBJECT_WRITE_PROTECTED;
            }

            if (format == MtpConstants.FORMAT_ASSOCIATION) {
                // recursive case - delete all children first
                Uri uri = Files.getMtpObjectsUri(mVolumeName);
                int count = mMediaProvider.delete(uri, "_data LIKE ?",
                        new String[] { path + "/%"});
            }

            Uri uri = Files.getMtpObjectsUri(mVolumeName, handle);
            if (mMediaProvider.delete(uri, null, null) > 0) {
                return MtpConstants.RESPONSE_OK;
            } else {
                return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in deleteFile", e);
            return MtpConstants.RESPONSE_GENERAL_ERROR;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private int[] getObjectReferences(int handle) {
        Uri uri = Files.getMtpReferencesUri(mVolumeName, handle);
        Cursor c = null;
        try {
            c = mMediaProvider.query(uri, ID_PROJECTION, null, null, null);
            if (c == null) {
                return null;
            }
            int count = c.getCount();
            if (count > 0) {
                int[] result = new int[count];
                for (int i = 0; i < count; i++) {
                    c.moveToNext();
                    result[i] = c.getInt(0);
                }
                return result;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getObjectList", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    private int setObjectReferences(int handle, int[] references) {
        mDatabaseModified = true;
        Uri uri = Files.getMtpReferencesUri(mVolumeName, handle);
        int count = references.length;
        ContentValues[] valuesList = new ContentValues[count];
        for (int i = 0; i < count; i++) {
            ContentValues values = new ContentValues();
            values.put(Files.FileColumns._ID, references[i]);
            valuesList[i] = values;
        }
        try {
            if (mMediaProvider.bulkInsert(uri, valuesList) > 0) {
                return MtpConstants.RESPONSE_OK;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setObjectReferences", e);
        }
		//Added for USB Develpment debug, more log for more debuging help
        SXlog.e(TAG, "setObjectReferences: Not Playlist, we don't accept!!");
		//Added for USB Develpment debug, more log for more debuging help
        return MtpConstants.RESPONSE_GENERAL_ERROR;
    }

    private void sessionStarted() {
		//ALPS00120037, add log for support MTP debugging
		Log.w(TAG, "sessionStarted");
		//ALPS00120037, add log for support MTP debugging
        mDatabaseModified = false;
    }

    private void sessionEnded() {

		//ALPS00120037, add log for support MTP debugging
		Log.w(TAG, "sessionEnded, mDatabaseModified = " + mDatabaseModified);
		//ALPS00120037, add log for support MTP debugging
	//ALPS00120037, add log for support MTP debugging!! Marked, send out the session close any time
        if (mDatabaseModified)
	//ALPS00120037, add log for support MTP debugging!! Marked, send out the session close any time
		{
            mContext.sendBroadcast(new Intent(MediaStore.ACTION_MTP_SESSION_END));
            mDatabaseModified = false;
        }
    }

	//Added Modification for ALPS00264207, ALPS00248646
    private void getObjectBeginIndication(String storagePath)
   	{	
		SXlog.d(TAG, "getObjectBeginIndication: storagePath = " + storagePath);
		
		notifyBeginSend(mMediaProvider, storagePath);
    }
	private void getObjectEndIndication(String storagePath)
	{

		SXlog.d(TAG, "getObjectEndIndication: storagePath = " + storagePath);
		
		notifyEndSend(mMediaProvider);
	}
	//Added Modification for ALPS00264207, ALPS00248646
	
    private void notifyBeginSend(IContentProvider mediaProvider, String path) {
        if (mediaProvider == null || path == null) {
            Log.e(TAG, "notifyBeginSend: Null " + (path == null ? "path!" : "provider!"));
            return;
        }
        
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MTP_TRANSFER_FILE_PATH, path);
            mediaProvider.insert(MediaStore.getMtpTransferFileUri(), values);
        } catch (RemoteException e) {
            Log.e(TAG, "notifyBeginSend: RemoteException!", e);
        }
    }

    private void notifyEndSend(IContentProvider mediaProvider) {
        if (mediaProvider == null) {
            Log.e(TAG, "notifyEndSend: Null provider!");
            return;
        }
        
        try {
            mediaProvider.delete(MediaStore.getMtpTransferFileUri(), null, null);
        } catch (RemoteException e) {
            Log.e(TAG, "notifyEndSend: RemoteException!", e);
        }
    }

    // used by the JNI code
    private int mNativeContext;

    private native final void native_setup();
    private native final void native_finalize();
}
