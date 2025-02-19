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
package com.android.gallery3d.util;


import android.os.Process;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread factory that creates threads with a given thread priority.
 */
public class PriorityThreadFactory implements ThreadFactory {

    private final int mPriority;
    private final AtomicInteger mNumber = new AtomicInteger();
    private final String mName;
    
    public class CancelableThread extends Thread {
    	private boolean cancelled = false;
    	
    	public CancelableThread(Runnable r, String name) {
    		super(r, name);
    	}
    	public boolean isThreadCancelled() {
    		// This method automatically reset "cancelled" flag
    		boolean wasCancelled = cancelled;
    		cancelled = false;
    		return wasCancelled;
    	}
    	public void cancelThread() {
    		cancelled = true;
    	}
    	public void run() {
    		Process.setThreadPriority(mPriority);
    		super.run();
    	}
    }

    public PriorityThreadFactory(String name, int priority) {
        mName = name;
        mPriority = priority;
    }

    public Thread newThread(Runnable r) {
    	/*
        return new Thread(r, mName + '-' + mNumber.getAndIncrement()) {
            @Override
            public void run() {
                Process.setThreadPriority(mPriority);
                super.run();
            }
        };
        */
    	return new CancelableThread(r, mName + '-' + mNumber.getAndIncrement());
    }

}
