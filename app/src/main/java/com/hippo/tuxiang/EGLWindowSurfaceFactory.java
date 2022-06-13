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

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

// android-9.0.0_r30

/**
 * An interface for customizing the eglCreateWindowSurface and eglDestroySurface calls.
 * <p>
 * This interface must be implemented by clients wishing to call
 * {@link GLStuff#setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory)}
 */
public interface EGLWindowSurfaceFactory {
    /**
     *  @return null if the surface cannot be constructed.
     */
    EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config,
            Object nativeWindow);
    void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface);
}
