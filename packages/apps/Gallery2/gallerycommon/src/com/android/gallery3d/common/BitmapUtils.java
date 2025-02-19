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

package com.android.gallery3d.common;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BitmapUtils {
    private static final String TAG = "BitmapUtils";
    // Change Compress_JPEG_QUALITY 90 to 100 in order to resolve block effect on specific image 
    private static final int COMPRESS_JPEG_QUALITY = 100;
    public static final int UNCONSTRAINED = -1;

    private BitmapUtils(){}

    /*
     * Compute the sample size as a function of minSideLength
     * and maxNumOfPixels.
     * minSideLength is used to specify that minimal width or height of a
     * bitmap.
     * maxNumOfPixels is used to specify the maximal size in pixels that is
     * tolerable in terms of memory usage.
     *
     * The function returns a sample size based on the constraints.
     * Both size and minSideLength can be passed in as UNCONSTRAINED,
     * which indicates no care of the corresponding constraint.
     * The functions prefers returning a sample size that
     * generates a smaller bitmap, unless minSideLength = UNCONSTRAINED.
     *
     * Also, the function rounds up the sample size to a power of 2 or multiple
     * of 8 because BitmapFactory only honors sample size this way.
     * For example, BitmapFactory downsamples an image by 2 even though the
     * request is 3. So we round up the sample size to avoid OOM.
     */
    public static int computeSampleSize(int width, int height,
            int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(
                width, height, minSideLength, maxNumOfPixels);

        return initialSize <= 8
                ? Utils.nextPowerOf2(initialSize)
                : (initialSize + 7) / 8 * 8;
    }

    private static int computeInitialSampleSize(int w, int h,
            int minSideLength, int maxNumOfPixels) {
        if (maxNumOfPixels == UNCONSTRAINED
                && minSideLength == UNCONSTRAINED) return 1;

        int lowerBound = (maxNumOfPixels == UNCONSTRAINED) ? 1 :
                (int) Math.ceil(Math.sqrt((double) (w * h) / maxNumOfPixels));

        if (minSideLength == UNCONSTRAINED) {
            return lowerBound;
        } else {
            int sampleSize = Math.min(w / minSideLength, h / minSideLength);
            return Math.max(sampleSize, lowerBound);
        }
    }

    // This computes a sample size which makes the longer side at least
    // minSideLength long. If that's not possible, return 1.
    public static int computeSampleSizeLarger(int w, int h,
            int minSideLength) {
        int initialSize = Math.max(w / minSideLength, h / minSideLength);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
                ? Utils.prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    // Fin the min x that 1 / x <= scale
    public static int computeSampleSizeLarger(float scale) {
        int initialSize = (int) Math.floor(1f / scale);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
                ? Utils.prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    // Find the max x that 1 / x >= scale.
    public static int computeSampleSize(float scale) {
        Utils.assertTrue(scale > 0);
        int initialSize = Math.max(1, (int) Math.ceil(1 / scale));
        return initialSize <= 8
                ? Utils.nextPowerOf2(initialSize)
                : (initialSize + 7) / 8 * 8;
    }

    public static Bitmap resizeDownToPixels(
            Bitmap bitmap, int targetPixels, boolean recycle) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = (float) Math.sqrt(
                (double) targetPixels / (width * height));
        if (scale >= 1.0f) return bitmap;
        return resizeBitmapByScale(bitmap, scale, recycle);
    }

    public static Bitmap resizeBitmapByScale(
            Bitmap bitmap, float scale, boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth()
                && height == bitmap.getHeight()) return bitmap;
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public static Bitmap resizeDownBySideLength(
            Bitmap bitmap, int maxLength, boolean recycle) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        float scale = Math.min(
                (float) maxLength / srcWidth, (float) maxLength / srcHeight);
        if (scale >= 1.0f) return bitmap;
        return resizeBitmapByScale(bitmap, scale, recycle);
    }

    // Resize the bitmap if each side is >= targetSize * 2
    public static Bitmap resizeDownIfTooBig(
            Bitmap bitmap, int targetSize, boolean recycle) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        float scale = Math.max(
                (float) targetSize / srcWidth, (float) targetSize / srcHeight);
        if (scale > 0.5f) return bitmap;
        return resizeBitmapByScale(bitmap, scale, recycle);
    }

    // Crops a square from the center of the original image.
    public static Bitmap cropCenter(Bitmap bitmap, boolean recycle) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == height) return bitmap;
        int size = Math.min(width, height);

        Bitmap target = Bitmap.createBitmap(size, size, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.translate((size - width) / 2, (size - height) / 2);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    public static Bitmap resizeDownAndCropCenter(Bitmap bitmap, int size,
            boolean recycle) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int minSide = Math.min(w, h);
        if (w == h && minSide <= size) return bitmap;
        size = Math.min(size, minSide);

        float scale = Math.max((float) size / bitmap.getWidth(),
                (float) size / bitmap.getHeight());
        Bitmap target = Bitmap.createBitmap(size, size, getConfig(bitmap));
        int width = Math.round(scale * bitmap.getWidth());
        int height = Math.round(scale * bitmap.getHeight());
        Canvas canvas = new Canvas(target);
        canvas.translate((size - width) / 2f, (size - height) / 2f);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    public static void recycleSilently(Bitmap bitmap) {
        if (bitmap == null) return;
        try {
            bitmap.recycle();
        } catch (Throwable t) {
            Log.w(TAG, "unable recycle bitmap", t);
        }
    }

    public static Bitmap rotateBitmap(Bitmap source, int rotation, boolean recycle) {
        if (rotation == 0) return source;
        int w = source.getWidth();
        int h = source.getHeight();
        Matrix m = new Matrix();
        m.postRotate(rotation);
        Bitmap bitmap = Bitmap.createBitmap(source, 0, 0, w, h, m, true);
        if (recycle) source.recycle();
        return bitmap;
    }

    public static Bitmap createVideoThumbnail(String filePath) {
        // MediaMetadataRetriever is available on API Level 8
        // but is hidden until API Level 10
        Class<?> clazz = null;
        Object instance = null;
        try {
            clazz = Class.forName("android.media.MediaMetadataRetriever");
            instance = clazz.newInstance();

            Method method = clazz.getMethod("setDataSource", String.class);
            method.invoke(instance, filePath);

            // The method name changes between API Level 9 and 10.
            if (Build.VERSION.SDK_INT <= 9) {
                return (Bitmap) clazz.getMethod("captureFrame").invoke(instance);
            } else {
                byte[] data = (byte[]) clazz.getMethod("getEmbeddedPicture").invoke(instance);
                if (data != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (bitmap != null) return bitmap;
                }
                return (Bitmap) clazz.getMethod("getFrameAtTime").invoke(instance);
            }
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
        } catch (InstantiationException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "createVideoThumbnail", e);
        } finally {
            try {
                if (instance != null) {
                    clazz.getMethod("release").invoke(instance);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static byte[] compressBitmap(Bitmap bitmap) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,
                COMPRESS_JPEG_QUALITY, os);
        return os.toByteArray();
    }

    public static boolean isSupportedByRegionDecoder(String mimeType) {
        if (mimeType == null) return false;
        mimeType = mimeType.toLowerCase();
        return mimeType.startsWith("image/") &&
                (!mimeType.equals("image/gif") && !mimeType.endsWith("bmp"));
    }

    public static boolean isSupportedByGifDecoder(String mimeType) {
        if (mimeType == null) return false;
        mimeType = mimeType.toLowerCase();
        return mimeType.equals("image/gif");
    }

    public static boolean isRotationSupported(String mimeType) {
        if (mimeType == null) return false;
        mimeType = mimeType.toLowerCase();
        return mimeType.equals("image/jpeg");
    }

    public static byte[] compressToBytes(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
        bitmap.compress(CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    //replace Bitmap's back ground with specified color
    public static Bitmap replaceBitmapBgColor(Bitmap b, int color, boolean recycleInput) {
        if (null == b) return null;
        if (b.getConfig() == Bitmap.Config.RGB_565) {
            Log.i(TAG,"replaceBitmapBgColor:Bitmap has no alpha, no bother");
            return b;
        }
        //Bitmap has alpha channel, and should be replace its background color
        //1,create a new bitmap with same dimension and ARGB_8888
        if (b.getWidth() <= 0 || b.getHeight() <= 0) {
            Log.w(TAG,"replaceBitmapBgColor:invalid Bitmap dimension");
            return b;
        }
        Bitmap b2 = Bitmap.createBitmap(b.getWidth(), 
                               b.getHeight(), Bitmap.Config.ARGB_8888);
        //2,create Canvas to encapulate new Bitmap
        Canvas canvas = new Canvas(b2);
        //3,draw background color
        canvas.drawColor(color);
        //4,draw original Bitmap on background
        canvas.drawBitmap(b,new Matrix(),null);
        //5,recycle original Bitmap if needed
        if (recycleInput) {
            b.recycle();
            b = null;
        }
        //6,return the output Bitmap
        return b2;
    }
    
    // crop input Bitmap to fit the desired aspect ratio
    public static Bitmap cropToFitAspectRatio(Bitmap image, int width, int height, boolean recycleInput) {
        Log.e(TAG, "cropToRetainAspectRatio");
        if (image  == null || width == 0 || height == 0) {
            return null;
        }
        int srcWidth = image.getWidth();
        int srcHeight = image.getHeight();
        float srcRatio = (float) srcWidth / (float) srcHeight;
        float destRatio = (float) width / (float) height;
        Log.i(TAG, " srcRatio=" + srcRatio + ", destRatio=" + destRatio);
        if (srcRatio == destRatio) {
            return image;
        }
        int destWidth = srcWidth;
        int destHeight = srcHeight;
        Bitmap ret = image;
        boolean shouldCropWidth = srcRatio > destRatio;
        Rect srcRect = null;
        Rect destRect = null;
        if (shouldCropWidth) {
            // crop width to fit the desired aspect ratio(which is width/height);
            destWidth = Math.round((float) srcHeight * destRatio);
            destHeight = srcHeight;
            srcRect = new Rect((srcWidth - destWidth) / 2, 0, (srcWidth + destWidth) / 2, srcHeight);
        } else {
            // crop height to fit the desired aspect ratio
            destHeight = Math.round((float) srcWidth / destRatio);
            destWidth = srcWidth;
            srcRect = new Rect(0, (srcHeight - destHeight) / 2, srcWidth, srcHeight / 2 + destHeight / 2);
        }
        destRect = new Rect(0, 0, destWidth, destHeight);
        ret = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(ret);
        c.drawBitmap(image, srcRect, destRect, null);
        
        if (recycleInput) {
            image.recycle();
        }
        return ret;
    }

}
