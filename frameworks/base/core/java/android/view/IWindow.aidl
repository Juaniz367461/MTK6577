/* //device/java/android/android/view/IWindow.aidl
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package android.view;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * API back to a client window that the Window Manager uses to inform it of
 * interesting things happening.
 *
 * {@hide}
 */
oneway interface IWindow {
    /**
     * ===== NOTICE =====
     * The first method must remain the first method. Scripts
     * and tools rely on their transaction number to work properly.
     */

    /**
     * Invoked by the view server to tell a window to execute the specified
     * command. Any response from the receiver must be sent through the
     * specified file descriptor.
     */
    void executeCommand(String command, String parameters, in ParcelFileDescriptor descriptor);

    void resized(int w, int h, in Rect coveredInsets, in Rect visibleInsets,
            boolean reportDraw, in Configuration newConfig);
    void dispatchAppVisibility(boolean visible);
    void dispatchGetNewSurface();

    /**
     * Tell the window that it is either gaining or losing focus.  Keep it up
     * to date on the current state showing navigational focus (touch mode) too.
     */
    void windowFocusChanged(boolean hasFocus, boolean inTouchMode);
    
    void closeSystemDialogs(String reason);
    
    /**
     * Called for wallpaper windows when their offsets change.
     */
    void dispatchWallpaperOffsets(float x, float y, float xStep, float yStep, boolean sync);
    
    void dispatchWallpaperCommand(String action, int x, int y,
            int z, in Bundle extras, boolean sync);

    /**
     * Drag/drop events
     */
     void dispatchDragEvent(in DragEvent event);

    /**
     * System chrome visibility changes
     */
     void dispatchSystemUiVisibilityChanged(int seq, int globalVisibility,
            int localValue, int localChanges);

    /**
     * Enable/Disable the log
     */
    void enableLog(boolean enable);
 
    /**
     * Dispatch position change
     */
    void dispatchPositionChanged(int left, int top);

    /**
     * Dump input dispatching status
     */
    void dumpInputDispatchingStatus();

    /**
     * dump window information
     */
    void dispatchDumpWindowInfo();
}
