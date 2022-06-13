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

import android.opengl.GLDebugHelper;
import android.opengl.GLUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.Writer;
import java.lang.ref.WeakReference;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;

// android-9.0.0_r30

/**
 * An EGL helper class.
 */
class EglHelper {
    public EglHelper(WeakReference<GLStuff> glStuffWeakRef) {
        mGLStuffViewWeakRef = glStuffWeakRef;
    }

    /**
     * Initialize EGL for a given configuration spec.
     */
    public void start() {
        if (GLStuff.LOG_EGL) {
            Log.w("EglHelper", "start() tid=" + Thread.currentThread().getId());
        }
        /*
         * Get an EGL instance
         */
        mEgl = (EGL10) EGLContext.getEGL();

        /*
         * Get to the default display.
         */
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        /*
         * We can now initialize EGL for that display
         */
        final int[] version = new int[2];
        if(!mEgl.eglInitialize(mEglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed");
        }
        final GLStuff stuff = mGLStuffViewWeakRef.get();
        if (stuff == null) {
            mEglConfig = null;
            mEglContext = null;
        } else {
            mEglConfig = stuff.getEGLConfigChooser().chooseConfig(mEgl, mEglDisplay);

            /*
             * Create an EGL context. We want to do this as rarely as we can, because an
             * EGL context is a somewhat heavy object.
             */
            mEglContext = stuff.getEGLContextFactory().createContext(mEgl, mEglDisplay, mEglConfig);
        }
        if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT) {
            mEglContext = null;
            throwEglException("createContext");
        }
        if (GLStuff.LOG_EGL) {
            Log.w("EglHelper", "createContext " + mEglContext + " tid=" + Thread.currentThread().getId());
        }

        mEglSurface = null;
    }

    /**
     * Create an egl surface for the current SurfaceHolder surface. If a surface
     * already exists, destroy it before creating the new surface.
     *
     * @return true if the surface was created successfully.
     */
    public boolean createSurface() {
        if (GLStuff.LOG_EGL) {
            Log.w("EglHelper", "createSurface()  tid=" + Thread.currentThread().getId());
        }
        /*
         * Check preconditions.
         */
        if (mEgl == null) {
            throw new RuntimeException("egl not initialized");
        }
        if (mEglDisplay == null) {
            throw new RuntimeException("eglDisplay not initialized");
        }
        if (mEglConfig == null) {
            throw new RuntimeException("mEglConfig not initialized");
        }

        /*
         *  The window size has changed, so we need to create a new
         *  surface.
         */
        destroySurfaceImp();

        /*
         * Create an EGL surface we can render into.
         */
        final GLStuff stuff = mGLStuffViewWeakRef.get();
        if (stuff != null) {
            mEglSurface = stuff.getEGLWindowSurfaceFactory().createWindowSurface(mEgl,
                    mEglDisplay, mEglConfig, stuff.getNativeWindow());
        } else {
            mEglSurface = null;
        }

        if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
            final int error = mEgl.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
            }
            return false;
        }

        /*
         * Before we can issue GL commands, we need to make sure
         * the context is current and bound to a surface.
         */
        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            /*
             * Could not make the context current, probably because the underlying
             * SurfaceView surface has been destroyed.
             */
            logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", mEgl.eglGetError());
            return false;
        }

        return true;
    }

    /**
     * Create a GL object for the current EGL context.
     */
    GL createGL() {

        GL gl = mEglContext.getGL();
        final GLStuff stuff = mGLStuffViewWeakRef.get();
        if (stuff != null) {
            final GLWrapper glWrapper = stuff.getGLWrapper();
            if (glWrapper != null) {
                gl = glWrapper.wrap(gl);
            }

            final int debugFlags = stuff.getDebugFlags();
            if ((debugFlags & (GLStuff.DEBUG_CHECK_GL_ERROR | GLStuff.DEBUG_LOG_GL_CALLS)) != 0) {
                int configFlags = 0;
                Writer log = null;
                if ((debugFlags & GLStuff.DEBUG_CHECK_GL_ERROR) != 0) {
                    configFlags |= GLDebugHelper.CONFIG_CHECK_GL_ERROR;
                }
                if ((debugFlags & GLStuff.DEBUG_LOG_GL_CALLS) != 0) {
                    log = new LogWriter();
                }
                gl = GLDebugHelper.wrap(gl, configFlags, log);
            }
        }
        return gl;
    }

    /**
     * Display the current render surface.
     * @return the EGL error code from eglSwapBuffers.
     */
    public int swap() {
        if (! mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
            return mEgl.eglGetError();
        }
        return EGL10.EGL_SUCCESS;
    }

    public void destroySurface() {
        if (GLStuff.LOG_EGL) {
            Log.w("EglHelper", "destroySurface()  tid=" + Thread.currentThread().getId());
        }
        destroySurfaceImp();
    }

    private void destroySurfaceImp() {
        if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT);
            final GLStuff stuff = mGLStuffViewWeakRef.get();
            if (stuff != null) {
                stuff.getEGLWindowSurfaceFactory().destroySurface(mEgl, mEglDisplay, mEglSurface);
            }
            mEglSurface = null;
        }
    }

    public void finish() {
        if (GLStuff.LOG_EGL) {
            Log.w("EglHelper", "finish() tid=" + Thread.currentThread().getId());
        }
        if (mEglContext != null) {
            final GLStuff stuff = mGLStuffViewWeakRef.get();
            if (stuff != null) {
                stuff.getEGLContextFactory().destroyContext(mEgl, mEglDisplay, mEglContext);
            }
            mEglContext = null;
        }
        if (mEglDisplay != null) {
            mEgl.eglTerminate(mEglDisplay);
            mEglDisplay = null;
        }
    }

    private void throwEglException(String function) {
        throwEglException(function, mEgl.eglGetError());
    }

    public static void throwEglException(String function, int error) {
        final String message = formatEglError(function, error);
        if (GLStuff.LOG_THREADS) {
            Log.e("EglHelper", "throwEglException tid=" + Thread.currentThread().getId() + " "
                    + message);
        }
        throw new RuntimeException(message);
    }

    public static void logEglErrorAsWarning(String tag, String function, int error) {
        Log.w(tag, formatEglError(function, error));
    }

    public static String formatEglError(String function, int error) {
        return function + " failed: " + GLUtils.getEGLErrorString(error);
    }

    private final WeakReference<GLStuff> mGLStuffViewWeakRef;
    EGL10 mEgl;
    EGLDisplay mEglDisplay;
    EGLSurface mEglSurface;
    EGLConfig mEglConfig;
    EGLContext mEglContext;


    static class LogWriter extends Writer {

        @Override public void close() {
            flushBuilder();
        }

        @Override public void flush() {
            flushBuilder();
        }

        @Override public void write(@NonNull char[] buf, int offset, int count) {
            for(int i = 0; i < count; i++) {
                final char c = buf[offset + i];
                if ( c == '\n') {
                    flushBuilder();
                }
                else {
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

        private final StringBuilder mBuilder = new StringBuilder();
    }
}
