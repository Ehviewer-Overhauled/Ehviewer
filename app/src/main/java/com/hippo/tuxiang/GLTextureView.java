/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.hippo.tuxiang;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import java.lang.ref.WeakReference;

/**
 * TextureView version {@link GLSurfaceView}
 */
public class GLTextureView extends TextureView implements GLStuff, TextureView.SurfaceTextureListener {
    private final static String TAG = "GLSurfaceView";

    public GLTextureView(Context context) {
        super(context);
        init();
    }

    public GLTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GLTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
        setSurfaceTextureListener(this);
    }

    @Override
    public Object getNativeWindow() {
        return getSurfaceTexture();
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
     * This method is part of the {@link android.view.TextureView.SurfaceTextureListener}
     * interface, and is not normally called or subclassed by clients of GLTextureView.
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mGLThread.surfaceCreated();
        mGLThread.onWindowResize(width, height);
    }

    /**
     * This method is part of the {@link android.view.TextureView.SurfaceTextureListener}
     * interface, and is not normally called or subclassed by clients of GLTextureView.
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mGLThread.onWindowResize(width, height);
    }

    /**
     * This method is part of the {@link android.view.TextureView.SurfaceTextureListener}
     * interface, and is not normally called or subclassed by clients of GLTextureView.
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        // Surface will be destroyed when we return
        mGLThread.surfaceDestroyed();
        return true;
    }

    /**
     * This method is part of the {@link android.view.TextureView.SurfaceTextureListener}
     * interface, and is not normally called or subclassed by clients of GLTextureView.
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        requestRender();
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
     * called or subclassed by clients of GLTextureView.
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
