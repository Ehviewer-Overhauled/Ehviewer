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

/**
 * An interface for customizing the eglCreateContext and eglDestroyContext calls.
 */
package com.hippo.tuxiang;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

// android-9.0.0_r30

/**
 * An interface for customizing the eglCreateContext and eglDestroyContext calls.
 * <p>
 * This interface must be implemented by clients wishing to call
 * {@link GLStuff#setEGLContextFactory(EGLContextFactory)}
 */
public interface EGLContextFactory {
    EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig);
    void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context);
}
