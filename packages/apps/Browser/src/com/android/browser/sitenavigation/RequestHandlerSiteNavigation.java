
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
package com.android.browser.sitenavigation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.android.browser.R;

/**
 * M: Site navigation
 */
public class RequestHandlerSiteNavigation extends Thread {

    private static final String TAG = "RequestHandlerSiteNavigation";
    private static final int SITE_NAVIGATION = 1;
    private static final int RESOURCE = 2;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    Uri mUri;
    Context mContext;
    OutputStream mOutput;

    static {
        sUriMatcher.addURI(SiteNavigation.AUTHORITY, "websites/res/*/*", RESOURCE);
        sUriMatcher.addURI(SiteNavigation.AUTHORITY, "websites", SITE_NAVIGATION);
    }

    public RequestHandlerSiteNavigation(Context context, Uri uri, OutputStream out) {
        mUri = uri;
        mContext = context.getApplicationContext();
        mOutput = out;
    }

    @Override
    public void run() {
        super.run();
        try {
            doHandleRequest();
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle request: " + mUri, e);
        } finally {
            cleanup();
        }
    }

    void doHandleRequest() throws IOException {
        int match = sUriMatcher.match(mUri);
        switch (match) {
        case SITE_NAVIGATION:
        	writeTemplatedIndex();
        	break;
        case RESOURCE:
            writeResource(getUriResourcePath());
            break;
        }
    }

    private void writeTemplatedIndex()  throws IOException {
    	TemplateSiteNavigation t = TemplateSiteNavigation.getCachedTemplate(mContext, R.raw.site_navigation);
        Cursor cursor = mContext.getContentResolver().query(Uri.parse("content://com.android.browser.site_navigation/websites"),
        		new String[] { "url", "title", "thumbnail" },
                null, null, null);

        t.assignLoop("site_navigation", new TemplateSiteNavigation.CursorListEntityWrapper(cursor) {
            @Override
            public void writeValue(OutputStream stream, String key) throws IOException {
                Cursor cursor = getCursor();
                if (key.equals("url")) {
                    stream.write(htmlEncode(cursor.getString(0)));
                } else if (key.equals("title")) {
                    String title = cursor.getString(1);
                    if (title == null || title.length() == 0) {
                        title = mContext.getString(R.string.sitenavigation_add);
                    }
                    stream.write(htmlEncode(title));
                } else if (key.equals("thumbnail")) {
                    stream.write("data:image/png;base64,".getBytes());
                    byte[] thumb = cursor.getBlob(2);
                    stream.write(Base64.encode(thumb, Base64.DEFAULT));
                }
            }
        });
        t.write(mOutput);
        cursor.close();
	}

	byte[] htmlEncode(String s) {
        return TextUtils.htmlEncode(s).getBytes();
    }


    String getUriResourcePath() {
        final Pattern pattern = Pattern.compile("/?res/([\\w/]+)");
        Matcher m = pattern.matcher(mUri.getPath());
        if (m.matches()) {
            return m.group(1);
        } else {
            return mUri.getPath();
        }
    }

    void writeResource(String fileName) throws IOException {
        Resources res = mContext.getResources();
        String packageName = R.class.getPackage().getName();
        int id = res.getIdentifier(fileName, null, packageName);
        if (id != 0) {
            InputStream in = res.openRawResource(id);
            byte[] buf = new byte[4096];
            int read;
            while ((read = in.read(buf)) > 0) {
                mOutput.write(buf, 0, read);
            }
        }
    }

    void writeString(String str) throws IOException {
        mOutput.write(str.getBytes());
    }

    void writeString(String str, int offset, int count) throws IOException {
        mOutput.write(str.getBytes(), offset, count);
    }

    void cleanup() {
        try {
            mOutput.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to close pipe!", e);
        }
    }

}
