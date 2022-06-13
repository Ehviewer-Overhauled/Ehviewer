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
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

// android-9.0.0_r30

public class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {

    @Override
    public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display,
            EGLConfig config, Object nativeWindow) {
        EGLSurface result = null;
        try {
            result = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
        } catch (IllegalArgumentException e) {
            // This exception indicates that the surface flinger surface
            // is not valid. This can happen if the surface flinger surface has
            // been torn down, but the application has not yet been
            // notified via SurfaceHolder.Callback.surfaceDestroyed.
            // In theory the application should be notified first,
            // but in practice sometimes it is not. See b/4588890
            Log.e("DefWinSurfaceFactory", "eglCreateWindowSurface", e);
        }
        return result;
    }

    @Override
    public void destroySurface(EGL10 egl, EGLDisplay display,
            EGLSurface surface) {
        egl.eglDestroySurface(display, surface);
    }
}
