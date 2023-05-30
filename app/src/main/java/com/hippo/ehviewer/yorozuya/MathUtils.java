/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.yorozuya;

import java.util.Random;

// Get most code from android.util.MathUtils
public final class MathUtils {
    private static final Random sRandom = new Random();

    private MathUtils() {
    }

    public static float abs(float v) {
        return v > 0 ? v : -v;
    }

    public static float log(float a) {
        return (float) Math.log(a);
    }

    public static float max(float a, float b) {
        return a > b ? a : b;
    }

    public static float max(int a, int b) {
        return a > b ? a : b;
    }

    public static float max(float a, float b, float c) {
        return a > b ? (a > c ? a : c) : (b > c ? b : c);
    }

    public static float max(int a, int b, int c) {
        return a > b ? (a > c ? a : c) : (b > c ? b : c);
    }

    public static float max(float... arg) {
        int length = arg.length;
        if (length <= 0) {
            throw new IllegalArgumentException("Empty argument");
        } else {
            float n = arg[0];
            float m;
            for (int i = 1; i < length; i++) {
                m = arg[i];
                if (m > n)
                    n = m;
            }
            return n;
        }
    }

    public static int max(int... arg) {
        int length = arg.length;
        if (length <= 0) {
            throw new IllegalArgumentException("Empty argument");
        } else {
            int n = arg[0];
            int m;
            for (int i = 1; i < length; i++) {
                m = arg[i];
                if (m > n)
                    n = m;
            }
            return n;
        }
    }

    public static float min(float a, float b) {
        return a < b ? a : b;
    }

    public static float min(int a, int b) {
        return a < b ? a : b;
    }

    public static float min(float a, float b, float c) {
        return a < b ? (a < c ? a : c) : (b < c ? b : c);
    }

    public static float min(int a, int b, int c) {
        return a < b ? (a < c ? a : c) : (b < c ? b : c);
    }

    public static float min(float... args) {
        int length = args.length;
        if (length <= 0) {
            throw new IllegalArgumentException("Empty argument");
        } else {
            float n = args[0];
            float m;
            for (int i = 1; i < length; i++) {
                m = args[i];
                if (m < n)
                    n = m;
            }
            return n;
        }
    }

    public static int min(int... args) {
        int length = args.length;
        if (length <= 0) {
            throw new IllegalArgumentException("Empty argument");
        } else {
            int n = args[0];
            int m;
            for (int i = 1; i < length; i++) {
                m = args[i];
                if (m < n)
                    n = m;
            }
            return n;
        }
    }

    public static float dist(float x1, float y1, float x2, float y2) {
        final float x = (x2 - x1);
        final float y = (y2 - y1);
        return (float) Math.hypot(x, y);
    }

    public static float dist(float x1, float y1, float z1, float x2, float y2, float z2) {
        final float x = (x2 - x1);
        final float y = (y2 - y1);
        final float z = (z2 - z1);
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static float cross(float v1x, float v1y, float v2x, float v2y) {
        return v1x * v2y - v1y * v2x;
    }

    public static int lerp(int start, int stop, float amount) {
        return start + (int) ((stop - start) * amount);
    }

    public static float lerp(float start, float stop, float amount) {
        return start + (stop - start) * amount;
    }

    public static float map(float minStart, float minStop, float maxStart, float maxStop, float value) {
        return maxStart + (maxStart - maxStop) * ((value - minStart) / (minStop - minStart));
    }

    /**
     * Returns the input value x clamped to the range [min, max].
     */
    public static int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    /**
     * divide and ceil
     */
    public static int ceilDivide(int a, int b) {
        return (a + b - 1) / b;
    }

    /**
     * [0, howbig)
     */
    public static int random(int howbig) {
        return (int) (sRandom.nextFloat() * howbig);
    }

    /**
     * [howsmall, howbig)
     */
    public static int random(int howsmall, int howbig) {
        if (howsmall >= howbig)
            return howsmall;
        return lerp(howsmall, howbig, sRandom.nextFloat());
    }

    /**
     * [0, howbig)
     */
    public static float random(float howbig) {
        return sRandom.nextFloat() * howbig;
    }

    /**
     * [howsmall, howbig)
     */
    public static float random(float howsmall, float howbig) {
        if (howsmall >= howbig)
            return howsmall;
        return lerp(howsmall, howbig, sRandom.nextFloat());
    }
}
