/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdklib.internal.repository;

import com.android.sdklib.internal.repository.Archive.Arch;
import com.android.sdklib.internal.repository.Archive.Os;

import java.util.Properties;

/**
 * A mock {@link ToolPackage} for testing.
 *
 * By design, this package contains one and only one archive.
 */
public class MockToolPackage extends ToolPackage {

    /**
     * Creates a {@link MockToolPackage} with the given revision and hardcoded defaults
     * for everything else.
     * <p/>
     * By design, this creates a package with one and only one archive.
     */
    public MockToolPackage(int revision, int min_platform_tools_rev) {
        this(null /*source*/, revision, min_platform_tools_rev);
    }

    /**
     * Creates a {@link MockToolPackage} with the given revision and hardcoded defaults
     * for everything else.
     * <p/>
     * By design, this creates a package with one and only one archive.
     */
    public MockToolPackage(SdkSource source, int revision, int min_platform_tools_rev) {
        super(
            source, // source,
            createProps(min_platform_tools_rev), // props,
            revision,
            null, // license,
            "desc", // description,
            "url", // descUrl,
            Os.getCurrentOs(), // archiveOs,
            Arch.getCurrentArch(), // archiveArch,
            "foo" // archiveOsPath
            );
    }

    private static Properties createProps(int min_platform_tools_rev) {
        Properties props = new Properties();
        props.setProperty(ToolPackage.PROP_MIN_PLATFORM_TOOLS_REV,
                          Integer.toString((min_platform_tools_rev)));
        return props;
    }
}
