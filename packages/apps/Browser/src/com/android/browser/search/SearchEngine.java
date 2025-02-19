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
package com.android.browser.search;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

/**
 * Interface for search engines.
 */
public interface SearchEngine {

    // Used if the search engine is Google
    static final String GOOGLE = "google";
    
    static final String BAIDU = "baidu";

    /**
     * Gets the unique name of this search engine.
     */
    public String getName();

    /**
     * Gets the human-readable name of this search engine.
     */
    public CharSequence getLabel();

    /**
     * Starts a search.
     */
    public void startSearch(Context context, String query, Bundle appData, String extraData);

    /**
     * Gets search suggestions.
     */
    public Cursor getSuggestions(Context context, String query);

    /**
     * Checks whether this search engine supports search suggestions.
     */
    public boolean supportsSuggestions();

    /**
     * Closes this search engine.
     */
    public void close();

    /**
     * Checks whether this search engine supports voice search.
     */
    public boolean supportsVoiceSearch();

    /**
     * Checks whether this search engine should be sent zero char query.
     */
    public boolean wantsEmptyQuery();
}
