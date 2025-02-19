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

package com.android.emailcommon.service;

public class SyncWindow {
//   public static final int SYNC_WINDOW_AUTO = -2;   // Temporary hide this option
    public static final int SYNC_WINDOW_USER = -1;
    public static final int SYNC_WINDOW_UNKNOWN = 0;
    public static final int SYNC_WINDOW_1_DAY = 1;
    public static final int SYNC_WINDOW_3_DAYS = 2;
    public static final int SYNC_WINDOW_1_WEEK = 3;
    public static final int SYNC_WINDOW_2_WEEKS = 4;
    public static final int SYNC_WINDOW_1_MONTH = 5;
    public static final int SYNC_WINDOW_ALL = 6;
}
