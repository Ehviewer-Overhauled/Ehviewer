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

// android-9.0.0_r30

/**
 * Choose a configuration with exactly the specified r,g,b,a sizes,
 * and at least the specified depth and stencil sizes.
 */
public class ComponentSizeChooser extends BaseConfigChooser {

    public ComponentSizeChooser(int eglContextClientVersion,
            int redSize, int greenSize, int blueSize,
            int alphaSize, int depthSize, int stencilSize) {
        super(eglContextClientVersion, new int[] {
                EGL10.EGL_RED_SIZE, redSize,
                EGL10.EGL_GREEN_SIZE, greenSize,
                EGL10.EGL_BLUE_SIZE, blueSize,
                EGL10.EGL_ALPHA_SIZE, alphaSize,
                EGL10.EGL_DEPTH_SIZE, depthSize,
                EGL10.EGL_STENCIL_SIZE, stencilSize,
                EGL10.EGL_NONE});
        mValue = new int[1];
        mRedSize = redSize;
        mGreenSize = greenSize;
        mBlueSize = blueSize;
        mAlphaSize = alphaSize;
        mDepthSize = depthSize;
        mStencilSize = stencilSize;
    }

    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
            EGLConfig[] configs) {
        for (final EGLConfig config : configs) {
            final int d = findConfigAttrib(egl, display, config,
                    EGL10.EGL_DEPTH_SIZE, 0);
            final int s = findConfigAttrib(egl, display, config,
                    EGL10.EGL_STENCIL_SIZE, 0);
            if ((d >= mDepthSize) && (s >= mStencilSize)) {
                final int r = findConfigAttrib(egl, display, config,
                        EGL10.EGL_RED_SIZE, 0);
                final int g = findConfigAttrib(egl, display, config,
                        EGL10.EGL_GREEN_SIZE, 0);
                final int b = findConfigAttrib(egl, display, config,
                        EGL10.EGL_BLUE_SIZE, 0);
                final int a = findConfigAttrib(egl, display, config,
                        EGL10.EGL_ALPHA_SIZE, 0);
                if ((r == mRedSize) && (g == mGreenSize)
                        && (b == mBlueSize) && (a == mAlphaSize)) {
                    return config;
                }
            }
        }
        return null;
    }

    private int findConfigAttrib(EGL10 egl, EGLDisplay display,
            EGLConfig config, int attribute, int defaultValue) {

        if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
            return mValue[0];
        }
        return defaultValue;
    }

    private final int[] mValue;
    // Subclasses can adjust these values:
    protected int mRedSize;
    protected int mGreenSize;
    protected int mBlueSize;
    protected int mAlphaSize;
    protected int mDepthSize;
    protected int mStencilSize;
}
