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

import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

// android-9.0.0_r30

public class DefaultContextFactory implements EGLContextFactory {

    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    private final int mEGLContextClientVersion;

    public DefaultContextFactory(int eglContextClientVersion) {
        mEGLContextClientVersion = eglContextClientVersion;
    }

    @Override
    public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
        final int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
                EGL10.EGL_NONE };

        return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT,
                mEGLContextClientVersion != 0 ? attrib_list : null);
    }

    @Override
    public void destroyContext(EGL10 egl, EGLDisplay display,
            EGLContext context) {
        if (!egl.eglDestroyContext(display, context)) {
            Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
            if (GLStuff.LOG_THREADS) {
                Log.i("DefaultContextFactory", "tid=" + Thread.currentThread().getId());
            }
            EglHelper.throwEglException("eglDestroyContex", egl.eglGetError());
        }
    }
}
