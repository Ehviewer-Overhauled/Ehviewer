/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.hippo.glview.util;

import android.graphics.Color;

import com.hippo.yorozuya.AssertError;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GalleryUtils {

    private static final String TAG = "GalleryUtils";
    private static final List<Integer> sRenderThreads = new CopyOnWriteArrayList<>();

    public static float[] intColorToFloatARGBArray(int from) {
        return new float[]{
                Color.alpha(from) / 255f,
                Color.red(from) / 255f,
                Color.green(from) / 255f,
                Color.blue(from) / 255f
        };
    }

    // Below are used the detect using database in the render thread. It only
    // works most of the time, but that's ok because it's for debugging only.

    public static int floatARGBArrayTointColor(float[] from) {
        return Color.argb((int) (from[0] * 255), (int) (from[1] * 255), (int) (from[2] * 255), (int) (from[3] * 255));
    }

    public static void setRenderThread() {
        sRenderThreads.add(Thread.currentThread().hashCode());
    }

    public static boolean isRenderThread() {
        return sRenderThreads.contains(Thread.currentThread().hashCode());
    }

    public static void assertInRenderThread() {
        if (!isRenderThread()) {
            throw new AssertError("Should not do this in non-render thread");
        }
    }
}
