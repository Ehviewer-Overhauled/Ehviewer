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

public abstract class BaseConfigChooser implements EGLConfigChooser {

    private final int mEGLContextClientVersion;

    public BaseConfigChooser(int eglContextClientVersion, int[] configSpec) {
        mEGLContextClientVersion = eglContextClientVersion;
        mConfigSpec = filterConfigSpec(configSpec);
    }

    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        final int[] num_config = new int[1];
        if (!egl.eglChooseConfig(display, mConfigSpec, null, 0,
                num_config)) {
            throw new IllegalArgumentException("eglChooseConfig failed");
        }

        final int numConfigs = num_config[0];

        if (numConfigs <= 0) {
            throw new IllegalArgumentException(
                    "No configs match configSpec");
        }

        final EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs,
                num_config)) {
            throw new IllegalArgumentException("eglChooseConfig#2 failed");
        }
        final EGLConfig config = chooseConfig(egl, display, configs);
        if (config == null) {
            throw new IllegalArgumentException("No config chosen");
        }
        return config;
    }

    public abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
            EGLConfig[] configs);

    protected int[] mConfigSpec;

    private int[] filterConfigSpec(int[] configSpec) {
        if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) {
            return configSpec;
        }
        /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
         * And we know the configSpec is well formed.
         */
        final int len = configSpec.length;
        final int[] newConfigSpec = new int[len + 2];
        System.arraycopy(configSpec, 0, newConfigSpec, 0, len-1);
        newConfigSpec[len-1] = EGL10.EGL_RENDERABLE_TYPE;
        if (mEGLContextClientVersion == 2) {
            newConfigSpec[len] = 0x0004;  /* EGL_OPENGL_ES2_BIT */
        } else {
            newConfigSpec[len] = 0x0040; /* EGL_OPENGL_ES3_BIT_KHR */
        }
        newConfigSpec[len+1] = EGL10.EGL_NONE;
        return newConfigSpec;
    }
}
