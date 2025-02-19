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

package  com.android.pqtuningtool.common;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Constructs {@link HttpClient} instances and isolates client code from API
 * level differences.
 */
public final class HttpClientFactory {
    // TODO: migrate GDataClient to use this util method instead of apache's
    // DefaultHttpClient.
    /**
     * Creates an HttpClient with the userAgent string constructed from the
     * package name contained in the context.
     * @return the client
     */
    public static HttpClient newHttpClient(Context context) {
        return HttpClientFactory.newHttpClient(getUserAgent(context));
    }

    /**
     * Creates an HttpClient with the specified userAgent string.
     * @param userAgent the userAgent string
     * @return the client
     */
    public static HttpClient newHttpClient(String userAgent) {
        // AndroidHttpClient is available on all platform releases,
        // but is hidden until API Level 8
        try {
            Class<?> clazz = Class.forName("android.net.http.AndroidHttpClient");
            Method newInstance = clazz.getMethod("newInstance", String.class);
            Object instance = newInstance.invoke(null, userAgent);

            HttpClient client = (HttpClient) instance;

            // ensure we default to HTTP 1.1
            HttpParams params = client.getParams();
            params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

            // AndroidHttpClient sets these two parameters thusly by default:
            // HttpConnectionParams.setSoTimeout(params, 60 * 1000);
            // HttpConnectionParams.setConnectionTimeout(params, 60 * 1000);

            // however it doesn't set this one...
            ConnManagerParams.setTimeout(params, 60 * 1000);

            return client;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes an HttpClient.
     */
    public static void close(HttpClient client) {
        // AndroidHttpClient is available on all platform releases,
        // but is hidden until API Level 8
        try {
            Class<?> clazz = client.getClass();
            Method method = clazz.getMethod("close", (Class<?>[]) null);
            method.invoke(client, (Object[]) null);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static String sUserAgent = null;

    private static String getUserAgent(Context context) {
        if (sUserAgent == null) {
            PackageInfo pi;
            try {
                pi = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0);
            } catch (NameNotFoundException e) {
                throw new IllegalStateException("getPackageInfo failed");
            }
            sUserAgent = String.format("%s/%s; %s/%s/%s/%s; %s/%s/%s",
                    pi.packageName,
                    pi.versionName,
                    Build.BRAND,
                    Build.DEVICE,
                    Build.MODEL,
                    Build.ID,
                    Build.VERSION.SDK,
                    Build.VERSION.RELEASE,
                    Build.VERSION.INCREMENTAL);
        }
        return sUserAgent;
    }

    private HttpClientFactory() {
    }
}
