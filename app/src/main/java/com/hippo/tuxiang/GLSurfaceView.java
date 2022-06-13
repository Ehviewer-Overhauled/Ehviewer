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

package com.hippo.tuxiang;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.lang.ref.WeakReference;

// android-9.0.0_r30

/**
 * An implementation of SurfaceView that uses the dedicated surface for
 * displaying OpenGL rendering.
 * <p>
 * A GLSurfaceView provides the following features:
 * <p>
 * <ul>
 * <li>Manages a surface, which is a special piece of memory that can be
 * composited into the Android view system.
 * <li>Manages an EGL display, which enables OpenGL to render into a surface.
 * <li>Accepts a user-provided Renderer object that does the actual rendering.
 * <li>Renders on a dedicated thread to decouple rendering performance from the
 * UI thread.
 * <li>Supports both on-demand and continuous rendering.
 * <li>Optionally wraps, traces, and/or error-checks the renderer's OpenGL calls.
 * </ul>
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For more information about how to use OpenGL, read the
 * <a href="{@docRoot}guide/topics/graphics/opengl.html">OpenGL</a> developer guide.</p>
 * </div>
 *
 * <h3>Using GLSurfaceView</h3>
 * <p>
 * Typically you use GLSurfaceView by subclassing it and overriding one or more of the
 * View system input event methods. If your application does not need to override event
 * methods then GLSurfaceView can be used as-is. For the most part
 * GLSurfaceView behavior is customized by calling "set" methods rather than by subclassing.
 * For example, unlike a regular View, drawing is delegated to a separate Renderer object which
 * is registered with the GLSurfaceView
 * using the {@link #setRenderer(Renderer)} call.
 * <p>
 * <h3>Initializing GLSurfaceView</h3>
 * All you have to do to initialize a GLSurfaceView is call {@link #setRenderer(Renderer)}.
 * However, if desired, you can modify the default behavior of GLSurfaceView by calling one or
 * more of these methods before calling setRenderer:
 * <ul>
 * <li>{@link #setDebugFlags(int)}
 * <li>{@link #setEGLConfigChooser(EGLConfigChooser)}
 * <li>{@link #setGLWrapper(GLWrapper)}
 * </ul>
 * <p>
 * <h4>Specifying the android.view.Surface</h4>
 * By default GLSurfaceView will create a PixelFormat.RGB_888 format surface. If a translucent
 * surface is required, call getHolder().setFormat(PixelFormat.TRANSLUCENT).
 * The exact format of a TRANSLUCENT surface is device dependent, but it will be
 * a 32-bit-per-pixel surface with 8 bits per component.
 * <p>
 * <h4>Choosing an EGL Configuration</h4>
 * A given Android device may support multiple EGLConfig rendering configurations.
 * The available configurations may differ in how many channels of data are present, as
 * well as how many bits are allocated to each channel. Therefore, the first thing
 * GLSurfaceView has to do when starting to render is choose what EGLConfig to use.
 * <p>
 * By default GLSurfaceView chooses a EGLConfig that has an RGB_888 pixel format,
 * with at least a 16-bit depth buffer and no stencil.
 * <p>
 * If you would prefer a different EGLConfig
 * you can override the default behavior by calling one of the
 * setEGLConfigChooser methods.
 * <p>
 * <h4>Debug Behavior</h4>
 * You can optionally modify the behavior of GLSurfaceView by calling
 * one or more of the debugging methods {@link #setDebugFlags(int)},
 * and {@link #setGLWrapper}. These methods may be called before and/or after setRenderer, but
 * typically they are called before setRenderer so that they take effect immediately.
 * <p>
 * <h4>Setting a Renderer</h4>
 * Finally, you must call {@link #setRenderer} to register a {@link Renderer}.
 * The renderer is
 * responsible for doing the actual OpenGL rendering.
 * <p>
 * <h3>Rendering Mode</h3>
 * Once the renderer is set, you can control whether the renderer draws
 * continuously or on-demand by calling
 * {@link #setRenderMode}. The default is continuous rendering.
 * <p>
 * <h3>Activity Life-cycle</h3>
 * A GLSurfaceView must be notified when to pause and resume rendering. GLSurfaceView clients
 * are required to call {@link #onPause()} when the activity stops and
 * {@link #onResume()} when the activity starts. These calls allow GLSurfaceView to
 * pause and resume the rendering thread, and also allow GLSurfaceView to release and recreate
 * the OpenGL display.
 * <p>
 * <h3>Handling events</h3>
 * <p>
 * To handle an event you will typically subclass GLSurfaceView and override the
 * appropriate method, just as you would with any other View. However, when handling
 * the event, you may need to communicate with the Renderer object
 * that's running in the rendering thread. You can do this using any
 * standard Java cross-thread communication mechanism. In addition,
 * one relatively easy way to communicate with your renderer is
 * to call
 * {@link #queueEvent(Runnable)}. For example:
 * <pre class="prettyprint">
 * class MyGLSurfaceView extends GLSurfaceView {
 *
 *     private MyRenderer mMyRenderer;
 *
 *     public void start() {
 *         mMyRenderer = ...;
 *         setRenderer(mMyRenderer);
 *     }
 *
 *     public boolean onKeyDown(int keyCode, KeyEvent event) {
 *         if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
 *             queueEvent(new Runnable() {
 *                 // This method will be called on the rendering
 *                 // thread:
 *                 public void run() {
 *                     mMyRenderer.handleDpadCenter();
 *                 }});
 *             return true;
 *         }
 *         return super.onKeyDown(keyCode, event);
 *     }
 * }
 * </pre>
 *
 */
public class GLSurfaceView extends SurfaceView implements GLStuff, SurfaceHolder.Callback2 {
    private final static String TAG = "GLSurfaceView";

    /**
     * Standard View constructor. In order to render something, you
     * must call {@link #setRenderer} to register a renderer.
     */
    public GLSurfaceView(Context context) {
        super(context);
        init();
    }

    /**
     * Standard View constructor. In order to render something, you
     * must call {@link #setRenderer} to register a renderer.
     */
    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mGLThread != null) {
                // GLThread may still be running if this view was never
                // attached to a window.
                mGLThread.requestExitAndWait();
            }
        } finally {
            super.finalize();
        }
    }

    private void init() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        // setFormat is done by SurfaceView in SDK 2.3 and newer. Uncomment
        // this statement if back-porting to 2.2 or older:
        // holder.setFormat(PixelFormat.RGB_565);
        //
        // setType is not needed for SDK 2.0 or newer. Uncomment this
        // statement if back-porting this code to older SDKs.
        // holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
    }

    @Override
    public Object getNativeWindow() {
        return getHolder();
    }

    @Override
    public void setGLWrapper(GLWrapper glWrapper) {
        mGLWrapper = glWrapper;
    }

    @Override
    public GLWrapper getGLWrapper() {
        return mGLWrapper;
    }

    @Override
    public void setDebugFlags(int debugFlags) {
        mDebugFlags = debugFlags;
    }

    @Override
    public int getDebugFlags() {
        return mDebugFlags;
    }

    @Override
    public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
        mPreserveEGLContextOnPause = preserveOnPause;
    }

    @Override
    public boolean getPreserveEGLContextOnPause() {
        return mPreserveEGLContextOnPause;
    }

    private void checkRenderThreadState() {
        if (mGLThread != null) {
            throw new IllegalStateException(
                    "setRenderer has already been called for this instance.");
        }
    }

    @Override
    public void setRenderer(Renderer renderer) {
        checkRenderThreadState();
        if (mEGLConfigChooser == null) {
            mEGLConfigChooser = new SimpleEGLConfigChooser(mEGLContextClientVersion, true);
        }
        if (mEGLContextFactory == null) {
            mEGLContextFactory = new DefaultContextFactory(mEGLContextClientVersion);
        }
        if (mEGLWindowSurfaceFactory == null) {
            mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
        }
        mRenderer = renderer;
        mGLThread = new GLThread(mThisWeakRef);
        mGLThread.start();
    }

    @Override
    public Renderer getRenderer() {
        return mRenderer;
    }

    @Override
    public void setEGLContextFactory(EGLContextFactory factory) {
        checkRenderThreadState();
        mEGLContextFactory = factory;
    }

    @Override
    public EGLContextFactory getEGLContextFactory() {
        return mEGLContextFactory;
    }

    @Override
    public void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory) {
        checkRenderThreadState();
        mEGLWindowSurfaceFactory = factory;
    }

    @Override
    public EGLWindowSurfaceFactory getEGLWindowSurfaceFactory() {
        return mEGLWindowSurfaceFactory;
    }

    @Override
    public void setEGLConfigChooser(EGLConfigChooser configChooser) {
        checkRenderThreadState();
        mEGLConfigChooser = configChooser;
    }

    @Override
    public EGLConfigChooser getEGLConfigChooser() {
        return mEGLConfigChooser;
    }

    @Override
    public void setEGLContextClientVersion(int version) {
        checkRenderThreadState();
        mEGLContextClientVersion = version;
    }

    @Override
    public int getEGLContextClientVersion() {
        return mEGLContextClientVersion;
    }

    @Override
    public void setRenderMode(int renderMode) {
        mGLThread.setRenderMode(renderMode);
    }

    @Override
    public int getRenderMode() {
        return mGLThread.getRenderMode();
    }

    @Override
    public void requestRender() {
        mGLThread.requestRender();
    }

    /**
     * This method is part of the SurfaceHolder.Callback2 interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mGLThread.surfaceCreated();
    }

    /**
     * This method is part of the SurfaceHolder.Callback2 interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return
        mGLThread.surfaceDestroyed();
    }

    /**
     * This method is part of the SurfaceHolder.Callback2 interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mGLThread.onWindowResize(w, h);
    }

    /**
     * This method is part of the SurfaceHolder.Callback2 interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    @Override
    public void surfaceRedrawNeededAsync(SurfaceHolder holder, Runnable finishDrawing) {
        if (mGLThread != null) {
            mGLThread.requestRenderAndNotify(finishDrawing);
        }
    }

    /**
     * This method is part of the SurfaceHolder.Callback2 interface, and is
     * not normally called or subclassed by clients of GLSurfaceView.
     */
    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {
        if (mGLThread != null) {
            mGLThread.requestRenderAndWait();
        }
    }

    @Override
    public void onPause() {
        mGLThread.onPause();
    }

    @Override
    public void onResume() {
        mGLThread.onResume();
    }

    @Override
    public void queueEvent(Runnable r) {
        mGLThread.queueEvent(r);
    }

    /**
     * This method is used as part of the View class and is not normally
     * called or subclassed by clients of GLSurfaceView.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onAttachedToWindow reattach =" + mDetached);
        }
        if (mDetached && (mRenderer != null)) {
            int renderMode = RENDERMODE_CONTINUOUSLY;
            if (mGLThread != null) {
                renderMode = mGLThread.getRenderMode();
            }
            mGLThread = new GLThread(mThisWeakRef);
            if (renderMode != RENDERMODE_CONTINUOUSLY) {
                mGLThread.setRenderMode(renderMode);
            }
            mGLThread.start();
        }
        mDetached = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (LOG_ATTACH_DETACH) {
            Log.d(TAG, "onDetachedFromWindow");
        }
        if (mGLThread != null) {
            mGLThread.requestExitAndWait();
        }
        mDetached = true;
        super.onDetachedFromWindow();
    }

    private final WeakReference<GLStuff> mThisWeakRef =
            new WeakReference<>((GLStuff) this);
    private GLThread mGLThread;
    private Renderer mRenderer;
    private boolean mDetached;
    private EGLConfigChooser mEGLConfigChooser;
    private EGLContextFactory mEGLContextFactory;
    private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private GLWrapper mGLWrapper;
    private int mDebugFlags;
    private int mEGLContextClientVersion;
    private boolean mPreserveEGLContextOnPause;
}
