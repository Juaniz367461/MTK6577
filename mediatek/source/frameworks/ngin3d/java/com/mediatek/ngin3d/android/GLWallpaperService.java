/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.mediatek.ngin3d.android;

import java.io.Writer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

// Original code provided by Robert Green
// http://www.rbgrn.net/content/354-glsurfaceview-adapted-3d-live-wallpapers
public class GLWallpaperService extends WallpaperService {
    private static final String TAG = "GLWallpaperService";

    @Override
    public Engine onCreateEngine() {
        return new GLEngine();
    }

    public class GLEngine extends Engine {
        public static final int RENDERMODE_WHEN_DIRTY = 0;
        public static final int RENDERMODE_CONTINUOUSLY = 1;

        private GLThread mGLThread;
        private EGLConfigChooser mEGLConfigChooser;
        private EGLContextFactory mEGLContextFactory;
        private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
        private GLWrapper mGLWrapper;
        private int mDebugFlags;

        public GLEngine() {
            super();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                onResume();
            } else {
                onPause();
            }
            super.onVisibilityChanged(visible);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mGLThread.requestExitAndWait();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mGLThread.onWindowResize(width, height);
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            mGLThread.surfaceCreated(holder);
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "onSurfaceDestroyed()");
            mGLThread.surfaceDestroyed();
            super.onSurfaceDestroyed(holder);
        }

        /**
         * An EGL helper class.
         */
        public void setGLWrapper(GLWrapper glWrapper) {
            mGLWrapper = glWrapper;
        }

        public void setDebugFlags(int debugFlags) {
            mDebugFlags = debugFlags;
        }

        public int getDebugFlags() {
            return mDebugFlags;
        }

        public void setRenderer(Renderer renderer) {
            checkRenderThreadState();
            if (mEGLConfigChooser == null) {
                mEGLConfigChooser = new SimpleEGLConfigChooser(true);
            }
            if (mEGLContextFactory == null) {
                mEGLContextFactory = new DefaultContextFactory();
            }
            if (mEGLWindowSurfaceFactory == null) {
                mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
            }
            mGLThread = new GLThread(renderer, mEGLConfigChooser, mEGLContextFactory, mEGLWindowSurfaceFactory, mGLWrapper);
            mGLThread.start();
        }

        /**
         * Install a custom EGLWindowSurfaceFactory.
         * <p>If this method is
         * called, it must be called before {@link #setRenderer(Renderer)}
         * is called.
         * <p>
         * If this method is not called, then by default
         * a window surface will be created with a null attribute list.
         */
        public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory) {
            checkRenderThreadState();
            mEGLWindowSurfaceFactory = factory;
        }

        /**
         * Install a custom EGLContextFactory.
         * <p>If this method is
         * called, it must be called before {@link #setRenderer(Renderer)}
         * is called.
         * <p>
         * If this method is not called, then by default
         * a context will be created with no shared context and
         * with a null attribute list.
         */
        public void setEGLContextFactory(EGLContextFactory factory) {
            checkRenderThreadState();
            mEGLContextFactory = factory;
        }

        /**
         * Install a config chooser which will choose a config
         * as close to 16-bit RGB as possible, with or without an optional depth
         * buffer as close to 16-bits as possible.
         * <p>If this method is
         * called, it must be called before {@link #setRenderer(Renderer)}
         * is called.
         * <p>
         * If no setEGLConfigChooser method is called, then by default the
         * view will choose an RGB_565 surface with a depth buffer depth of
         * at least 16 bits.
         *
         * @param needDepth
         */
        public void setEGLConfigChooser(boolean needDepth) {
            setEGLConfigChooser(new SimpleEGLConfigChooser(needDepth));
        }

        public void setEGLConfigChooser(EGLConfigChooser configChooser) {
            checkRenderThreadState();
            mEGLConfigChooser = configChooser;
        }

        /**
         * Install a config chooser which will choose a config
         * with at least the specified depthSize and stencilSize,
         * and exactly the specified redSize, greenSize, blueSize and alphaSize.
         * <p>If this method is
         * called, it must be called before {@link #setRenderer(Renderer)}
         * is called.
         * <p>
         * If no setEGLConfigChooser method is called, then by default the
         * view will choose an RGB_565 surface with a depth buffer depth of
         * at least 16 bits.
         *
         */
        public void setEGLConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize,
                                        int stencilSize) {
            setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize, blueSize, alphaSize, depthSize,
                stencilSize));
        }

        public void setRenderMode(int renderMode) {
            mGLThread.setRenderMode(renderMode);
        }

        public int getRenderMode() {
            return mGLThread.getRenderMode();
        }

        public void requestRender() {
            mGLThread.requestRender();
        }

        public void onPause() {
            mGLThread.onPause();
        }

        public void onResume() {
            mGLThread.onResume();
        }

        public void queueEvent(Runnable r) {
            mGLThread.queueEvent(r);
        }

        private void checkRenderThreadState() {
            if (mGLThread != null) {
                throw new IllegalStateException("setRenderer has already been called for this instance.");
            }
        }
    }

    public interface Renderer {

        void onSurfaceCreated(GL10 gl, EGLConfig config);

        void onSurfaceChanged(GL10 gl, int width, int height);

        void onDrawFrame(GL10 gl);
    }
}

class LogWriter extends Writer {
    private final StringBuilder mBuilder = new StringBuilder();

    @Override
    public void close() {
        flushBuilder();
    }

    @Override
    public void flush() {
        flushBuilder();
    }

    @Override
    public void write(char[] buf, int offset, int count) {
        for (int i = 0; i < count; i++) {
            char c = buf[offset + i];
            if (c == '\n') {
                flushBuilder();
            } else {
                mBuilder.append(c);
            }
        }
    }

    private void flushBuilder() {
        if (mBuilder.length() > 0) {
            Log.v("GLSurfaceView", mBuilder.toString());
            mBuilder.delete(0, mBuilder.length());
        }
    }
}

// ----------------------------------------------------------------------

/**
 * An interface for customizing the eglCreateContext and eglDestroyContext calls.
 * <p/>
 * <p/>
 * This interface must be implemented by clients wishing to call
 * {@link GLWallpaperService#setEGLContextFactory(EGLContextFactory)}
 */
interface EGLContextFactory {
    EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig);

    void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context);
}

class DefaultContextFactory implements EGLContextFactory {

    public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
        final int eglContextClientVersion = 0x3098;
        int[] attribList = {eglContextClientVersion, 2, EGL10.EGL_NONE};
        return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attribList);
    }

    public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
        egl.eglDestroyContext(display, context);
    }
}

/**
 * An interface for customizing the eglCreateWindowSurface and eglDestroySurface calls.
 * <p/>
 * <p/>
 * This interface must be implemented by clients wishing to call
 * {@link GLWallpaperService#setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory)}
 */
interface EGLWindowSurfaceFactory {
    EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config, Object nativeWindow);

    void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface);
}

class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {

    public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay
        display, EGLConfig config, Object nativeWindow) {
        // this is a bit of a hack to work around Droid init problems - if you don't have this, it'll get hung up on orientation changes
        EGLSurface eglSurface = null;
        while (eglSurface == null) {
            try {
                eglSurface = egl.eglCreateWindowSurface(display,
                    config, nativeWindow, null);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                if (eglSurface == null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException t) {
                        t.printStackTrace();
                    }
                }
            }
        }
        return eglSurface;
    }

    public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
        egl.eglDestroySurface(display, surface);
    }
}

interface GLWrapper {
    /**
     * Wraps a gl interface in another gl interface.
     *
     * @param gl a GL interface that is to be wrapped.
     * @return either the input argument or another GL object that wraps the input argument.
     */
    GL wrap(GL gl);
}

class EglHelper {

    private EGL10 mEgl;
    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;
    private EGLContext mEglContext;
    EGLConfig mEglConfig;

    private final EGLConfigChooser mEGLConfigChooser;
    private final EGLContextFactory mEGLContextFactory;
    private final EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private final GLWrapper mGLWrapper;

    public EglHelper(EGLConfigChooser chooser, EGLContextFactory contextFactory,
                     EGLWindowSurfaceFactory surfaceFactory, GLWrapper wrapper) {
        this.mEGLConfigChooser = chooser;
        this.mEGLContextFactory = contextFactory;
        this.mEGLWindowSurfaceFactory = surfaceFactory;
        this.mGLWrapper = wrapper;
    }

    /**
     * Initialize EGL for a given configuration spec.
     */
    public void start() {
        // Log.d("EglHelper" + instanceId, "start()");
        if (mEgl == null) {
            // Log.d("EglHelper" + instanceId, "getting new EGL");
            /*
                * Get an EGL instance
                */
            mEgl = (EGL10) EGLContext.getEGL();
        }

        if (mEglDisplay == null) {
            // Log.d("EglHelper" + instanceId, "getting new display");
            /*
                * Get to the default display.
                */
            mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        }

        if (mEglConfig == null) {
            // Log.d("EglHelper" + instanceId, "getting new config");
            /*
                * We can now initialize EGL for that display
                */
            int[] version = new int[2];
            mEgl.eglInitialize(mEglDisplay, version);
            mEglConfig = mEGLConfigChooser.chooseConfig(mEgl, mEglDisplay);
        }

        if (mEglContext == null) {
            // Log.d("EglHelper" + instanceId, "creating new context");
            /*
                * Create an OpenGL ES context. This must be done only once, an OpenGL context is a somewhat heavy object.
                */
            mEglContext = mEGLContextFactory.createContext(mEgl, mEglDisplay, mEglConfig);
            if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT) {
                throw new RuntimeException("createContext failed");
            }
        }

        mEglSurface = null;
    }

    /*
      * React to the creation of a new surface by creating and returning an OpenGL interface that renders to that
      * surface.
      */
    public GL createSurface(SurfaceHolder holder) {
        /*
           * The window size has changed, so we need to create a new surface.
           */
        if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {

            /*
                * Unbind and destroy the old EGL surface, if there is one.
                */
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
        }

        /*
           * Create an EGL surface we can render into.
           */
        mEglSurface = mEGLWindowSurfaceFactory.createWindowSurface(mEgl, mEglDisplay, mEglConfig, holder);

        if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
            throw new RuntimeException("createWindowSurface failed");
        }

        /*
           * Before we can issue GL commands, we need to make sure the context is current and bound to a surface.
           */
        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed.");
        }

        GL gl = mEglContext.getGL();
        if (mGLWrapper != null) {
            gl = mGLWrapper.wrap(gl);
        }

        return gl;
    }

    /**
     * Display the current render surface.
     *
     * @return false if the context has been lost.
     */
    public boolean swap() {
        mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);

        /*
           * Always check for EGL_CONTEXT_LOST, which means the context and all associated data were lost (For instance
           * because the device went to sleep). We need to sleep until we get a new surface.
           */
        return mEgl.eglGetError() != EGL11.EGL_CONTEXT_LOST;
    }

    public void destroySurface() {
        if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
            mEglSurface = null;
        }
    }

    public void finish() {
        if (mEglContext != null) {
            mEGLContextFactory.destroyContext(mEgl, mEglDisplay, mEglContext);
            mEglContext = null;
        }
        if (mEglDisplay != null) {
            mEgl.eglTerminate(mEglDisplay);
            mEglDisplay = null;
        }
    }
}

class GLThread extends Thread {
    private static final boolean LOG_THREADS = false;

    private final GLThreadManager mGLThreadManager = new GLThreadManager();
    private GLThread mEglOwner;

    private final EGLConfigChooser mEGLConfigChooser;
    private final EGLContextFactory mEGLContextFactory;
    private final EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private final GLWrapper mGLWrapper;

    public SurfaceHolder mHolder;
    private boolean mSizeChanged = true;

    // Once the thread is started, all accesses to the following member
    // variables are protected by the mGLThreadManager monitor
    public boolean mDone;
    private boolean mPaused;
    private boolean mHasSurface;
    private boolean mWaitingForSurface;
    private boolean mHaveEgl;
    private int mWidth;
    private int mHeight;
    private int mRenderMode;
    private boolean mRequestRender;
    private boolean mEventsWaiting;
    // End of member variables protected by the mGLThreadManager monitor.

    private final GLWallpaperService.Renderer mRenderer;
    private final ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
    private EglHelper mEglHelper;

    GLThread(GLWallpaperService.Renderer renderer, EGLConfigChooser chooser, EGLContextFactory contextFactory,
             EGLWindowSurfaceFactory surfaceFactory, GLWrapper wrapper) {
        super();
        mDone = false;
        mWidth = 0;
        mHeight = 0;
        mRequestRender = true;
        mRenderMode = GLWallpaperService.GLEngine.RENDERMODE_CONTINUOUSLY;
        mRenderer = renderer;
        this.mEGLConfigChooser = chooser;
        this.mEGLContextFactory = contextFactory;
        this.mEGLWindowSurfaceFactory = surfaceFactory;
        this.mGLWrapper = wrapper;
    }

    @Override
    public void run() {
        setName("GLThread " + getId());
        if (LOG_THREADS) {
            Log.i("GLThread", "starting tid=" + getId());
        }

        try {
            guardedRun();
        } catch (InterruptedException e) {
            // fall thru and exit normally
            e.printStackTrace();
        } finally {
            mGLThreadManager.threadExiting(this);
        }
    }

    /*
      * This private method should only be called inside a synchronized(mGLThreadManager) block.
      */
    private void stopEglLocked() {
        if (mHaveEgl) {
            mHaveEgl = false;
            mEglHelper.destroySurface();
            mGLThreadManager.releaseEglSurface(this);
        }
    }

    private void guardedRun() throws InterruptedException {
        mEglHelper = new EglHelper(mEGLConfigChooser, mEGLContextFactory, mEGLWindowSurfaceFactory, mGLWrapper);
        try {
            GL10 gl = null;
            boolean tellRendererSurfaceCreated = true;
            boolean tellRendererSurfaceChanged = true;

            /*
                * This is our main activity thread's loop, we go until asked to quit.
                */
            while (!isDone()) {
                /*
                     * Update the asynchronous state (window size)
                     */
                int w = 0;
                int h = 0;
                boolean changed = false;
                boolean needStart = false;
                boolean eventsWaiting = false;

                synchronized (mGLThreadManager) {
                    while (true) {
                        // Manage acquiring and releasing the SurfaceView
                        // surface and the EGL surface.
                        if (mPaused) {
                            stopEglLocked();
                        }
                        if (mHasSurface) {
                            if (!mHaveEgl) {
                                if (mGLThreadManager.tryAcquireEglSurface(this)) {
                                    mHaveEgl = true;
                                    mEglHelper.start();
                                    mRequestRender = true;
                                    needStart = true;
                                }
                            }
                        } else {
                            if (!mWaitingForSurface) {
                                stopEglLocked();
                                mWaitingForSurface = true;
                                mGLThreadManager.notifyAll();
                            }
                        }

                        // Check if we need to wait. If not, update any state
                        // that needs to be updated, copy any state that
                        // needs to be copied, and use "break" to exit the
                        // wait loop.

                        if (mDone) {
                            return;
                        }

                        if (mEventsWaiting) {
                            eventsWaiting = true;
                            mEventsWaiting = false;
                            break;
                        }

                        if ((!mPaused) && mHasSurface && mHaveEgl && (mWidth > 0) && (mHeight > 0)
                            && (mRequestRender || (mRenderMode == GLWallpaperService.GLEngine.RENDERMODE_CONTINUOUSLY))) {
                            changed = mSizeChanged;
                            w = mWidth;
                            h = mHeight;
                            mSizeChanged = false;
                            mRequestRender = false;
                            if (mHasSurface && mWaitingForSurface) {
                                changed = true;
                                mWaitingForSurface = false;
                                mGLThreadManager.notifyAll();
                            }
                            break;
                        }

                        // By design, this is the only place where we wait().

                        if (LOG_THREADS) {
                            Log.i("GLThread", "waiting tid=" + getId());
                        }
                        mGLThreadManager.wait();
                    }
                } // end of synchronized(mGLThreadManager)

                /*
                     * Handle queued events
                     */
                if (eventsWaiting) {
                    Runnable r;
                    while ((r = getEvent()) != null) {
                        r.run();
                        if (isDone()) {
                            return;
                        }
                    }
                    // Go back and see if we need to wait to render.
                    continue;
                }

                if (needStart) {
                    tellRendererSurfaceCreated = true;
                    changed = true;
                }
                if (changed) {
                    gl = (GL10) mEglHelper.createSurface(mHolder);
                    tellRendererSurfaceChanged = true;
                }
                if (tellRendererSurfaceCreated) {
                    mRenderer.onSurfaceCreated(gl, mEglHelper.mEglConfig);
                    tellRendererSurfaceCreated = false;
                }
                if (tellRendererSurfaceChanged) {
                    mRenderer.onSurfaceChanged(gl, w, h);
                    tellRendererSurfaceChanged = false;
                }
                if ((w > 0) && (h > 0)) {
                    /* draw a frame here */
                    mRenderer.onDrawFrame(gl);

                    /*
                          * Once we're done with GL, we need to call swapBuffers() to instruct the system to display the
                          * rendered frame
                          */
                    mEglHelper.swap();
                    Thread.sleep(10);
                }
            }
        } finally {
            /*
                * clean-up everything...
                */
            synchronized (mGLThreadManager) {
                stopEglLocked();
                mEglHelper.finish();
            }
        }
    }

    private boolean isDone() {
        synchronized (mGLThreadManager) {
            return mDone;
        }
    }

    public void setRenderMode(int renderMode) {
        if (!((GLWallpaperService.GLEngine.RENDERMODE_WHEN_DIRTY <= renderMode) && (renderMode <= GLWallpaperService.GLEngine.RENDERMODE_CONTINUOUSLY))) {
            throw new IllegalArgumentException("renderMode");
        }
        synchronized (mGLThreadManager) {
            mRenderMode = renderMode;
            if (renderMode == GLWallpaperService.GLEngine.RENDERMODE_CONTINUOUSLY) {
                mGLThreadManager.notifyAll();
            }
        }
    }

    public int getRenderMode() {
        synchronized (mGLThreadManager) {
            return mRenderMode;
        }
    }

    public void requestRender() {
        synchronized (mGLThreadManager) {
            mRequestRender = true;
            mGLThreadManager.notifyAll();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;
        synchronized (mGLThreadManager) {
            if (LOG_THREADS) {
                Log.i("GLThread", "surfaceCreated tid=" + getId());
            }
            mHasSurface = true;
            mGLThreadManager.notifyAll();
        }
    }

    public void surfaceDestroyed() {
        synchronized (mGLThreadManager) {
            if (LOG_THREADS) {
                Log.i("GLThread", "surfaceDestroyed tid=" + getId());
            }
            mHasSurface = false;
            mGLThreadManager.notifyAll();
            while (!mWaitingForSurface && isAlive() && !mDone) {
                try {
                    mGLThreadManager.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void onPause() {
        synchronized (mGLThreadManager) {
            mPaused = true;
            mGLThreadManager.notifyAll();
        }
    }

    public void onResume() {
        synchronized (mGLThreadManager) {
            mPaused = false;
            mRequestRender = true;
            mGLThreadManager.notifyAll();
        }
    }

    public void onWindowResize(int w, int h) {
        synchronized (mGLThreadManager) {
            mWidth = w;
            mHeight = h;
            mSizeChanged = true;
            mGLThreadManager.notifyAll();
        }
    }

    public void requestExitAndWait() {
        // don't call this from GLThread thread or it is a guaranteed
        // deadlock!
        synchronized (mGLThreadManager) {
            mDone = true;
            mGLThreadManager.notifyAll();
        }
        try {
            join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Queue an "event" to be run on the GL rendering thread.
     *
     * @param r the runnable to be run on the GL rendering thread.
     */
    public void queueEvent(Runnable r) {
        synchronized (this) {
            mEventQueue.add(r);
            synchronized (mGLThreadManager) {
                mEventsWaiting = true;
                mGLThreadManager.notifyAll();
            }
        }
    }

    private Runnable getEvent() {
        synchronized (this) {
            if (mEventQueue.isEmpty()) {
                return mEventQueue.remove(0);
            }

        }
        return null;
    }

    private class GLThreadManager {
        @SuppressWarnings("PMD")
        public void threadExiting(GLThread thread) {
            synchronized (this) {
                if (LOG_THREADS) {
                    Log.i("GLThread", "exiting tid=" + thread.getId());
                }
                thread.mDone = true;

                if (mEglOwner == thread) {
                    mEglOwner = null;
                }
                notifyAll();
            }
        }

        /*
           * Tries once to acquire the right to use an EGL surface. Does not block.
           *
           * @return true if the right to use an EGL surface was acquired.
           */
        @SuppressWarnings("PMD")
        public boolean tryAcquireEglSurface(GLThread thread) {
            synchronized (this) {
                if (mEglOwner == thread || mEglOwner == null) {
                    mEglOwner = thread;
                    notifyAll();
                    return true;
                }
                return false;
            }
        }

        @SuppressWarnings("PMD")
        public void releaseEglSurface(GLThread thread) {
            synchronized (this) {
                if (mEglOwner == thread) {
                    mEglOwner = null;
                }
                notifyAll();
            }
        }
    }
}

