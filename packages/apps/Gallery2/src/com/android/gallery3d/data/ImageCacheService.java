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

package com.android.gallery3d.data;

import com.android.gallery3d.common.BlobCache;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.CacheManager;
import com.android.gallery3d.util.GalleryUtils;

import android.content.Context;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageCacheService {
    @SuppressWarnings("unused")
    private static final String TAG = "ImageCacheService";

    private static final String IMAGE_CACHE_FILE = "imgcache";
    private static final int IMAGE_CACHE_MAX_ENTRIES = 5000;
    private static final int IMAGE_CACHE_MAX_BYTES = 200 * 1024 * 1024;
    private static final int IMAGE_CACHE_VERSION = 3;

    private BlobCache mCache;
    
    // for closing/re-opening cache
    private Context mContext;
    private Object mCacheLock = new Object();

    public ImageCacheService(Context context) {
        mContext = context;
        mCache = CacheManager.getCache(context, IMAGE_CACHE_FILE,
                IMAGE_CACHE_MAX_ENTRIES, IMAGE_CACHE_MAX_BYTES,
                IMAGE_CACHE_VERSION);
    }

    public static class ImageData {
        public ImageData(byte[] data, int offset) {
            mData = data;
            mOffset = offset;
        }
        public byte[] mData;
        public int mOffset;
    }

    public ImageData getImageData(Path path, int type) {
        if (mCache == null) {
            Log.e(TAG, "getImageData: cache file is null!");
            return null;
        }
        byte[] key = makeKey(path, type);
        long cacheKey = Utils.crc64Long(key);
        try {
            byte[] value = null;
            synchronized (mCacheLock) {
                if (mCache != null) {
                    value = mCache.lookup(cacheKey);
                }
            }
            if (value == null) return null;
            if (isSameKey(key, value)) {
                int offset = key.length;
                return new ImageData(value, offset);
            }
        } catch (IOException ex) {
            // ignore.
        }
        return null;
    }

    public void putImageData(Path path, int type, byte[] value) {
        if (mCache == null) {
            Log.e(TAG, "putImageData: cache file is null!");
            return;
        }
        byte[] key = makeKey(path, type);
        long cacheKey = Utils.crc64Long(key);
        ByteBuffer buffer = ByteBuffer.allocate(key.length + value.length);
        buffer.put(key);
        buffer.put(value);
        synchronized (mCacheLock) {
            try {
                if (mCache != null) {
                    mCache.insert(cacheKey, buffer.array());
                }
            } catch (IOException ex) {
                // ignore.
            }
        }
    }

    private static byte[] makeKey(Path path, int type) {
        return GalleryUtils.getBytes(path.toString() + "+" + type);
    }

    private static boolean isSameKey(byte[] key, byte[] buffer) {
        int n = key.length;
        if (buffer.length < n) {
            return false;
        }
        for (int i = 0; i < n; ++i) {
            if (key[i] != buffer[i]) {
                return false;
            }
        }
        return true;
    }
    
    //check dateTaken api
    public ImageData getImageData(Path path, int type, long dateModifiedInSec) {
        if (mCache == null) {
            Log.e(TAG, "getImageData: cache file is null!");
            return null;
        }
        byte[] key = makeKey(path, type, dateModifiedInSec);
        long cacheKey = Utils.crc64Long(key);
        try {
            byte[] value = null;
            synchronized (mCacheLock) {
                if (mCache != null) {
                    value = mCache.lookup(cacheKey);
                }
            }
            if (value == null) return null;
            if (isSameKey(key, value)) {
                int offset = key.length;
                return new ImageData(value, offset);
            }
        } catch (IOException ex) {
            // ignore.
        } catch (Exception e) {
            Log.e(TAG, "Exception in getImageData: ", e);
        }
        return null;
    }

    public void putImageData(Path path, int type, byte[] value, long dateModifiedInSec) {
        if (mCache == null) {
            Log.e(TAG, "putImageData: cache file is null!");
            return;
        }
        byte[] key = makeKey(path, type, dateModifiedInSec);
        long cacheKey = Utils.crc64Long(key);
        ByteBuffer buffer = ByteBuffer.allocate(key.length + value.length);
        buffer.put(key);
        buffer.put(value);
        synchronized (mCacheLock) {
            try {
                if (mCache != null) {
                    mCache.insert(cacheKey, buffer.array());
                }
            } catch (IOException ex) {
                // ignore.
            } catch (Exception e) {
                Log.e(TAG, "Exception in putImageData: ", e);
            }
        }
    }

    private static byte[] makeKey(Path path, int type, long dateModifiedInSec) {
        return GalleryUtils.getBytes(path.toString() + "+" + type + "+" + dateModifiedInSec);
    }
    
    // for closing/re-opening cache
    public void closeCache() {
        synchronized (mCacheLock) {
            // simply clear the reference,
            // since the BlobCache should already be closed in CacheManager
            mCache = null;
        }
    }
    
    public void openCache() {
        synchronized (mCacheLock) {
            if (mCache == null) {
                // re-open the cache
                mCache = CacheManager.getCache(mContext, IMAGE_CACHE_FILE,
                        IMAGE_CACHE_MAX_ENTRIES, IMAGE_CACHE_MAX_BYTES,
                        IMAGE_CACHE_VERSION);
            }
        }
    }
}
