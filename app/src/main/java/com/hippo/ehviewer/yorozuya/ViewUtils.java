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

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicInteger;

public final class ViewUtils {
    public static final int MAX_SIZE = Integer.MAX_VALUE & ~(0x3 << 30);
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    private ViewUtils() {
    }

    /**
     * Remove view from its parent
     *
     * @param view the view to remove
     */
    public static void removeFromParent(View view) {
        ViewParent vp = view.getParent();
        if (vp instanceof ViewGroup)
            ((ViewGroup) vp).removeView(view);
    }

    /**
     * Generate a value suitable for use in {@link View#setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    public static int generateViewId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    @NonNull
    public static View $$(Activity activity, @IdRes int id) {
        View result = activity.findViewById(id);
        if (null == result) {
            throw new NullPointerException("Can't find view with id: " + id);
        }
        return result;
    }

    @NonNull
    public static View $$(Dialog dialog, @IdRes int id) {
        View result = dialog.findViewById(id);
        if (null == result) {
            throw new NullPointerException("Can't find view with id: " + id);
        }
        return result;
    }

    @NonNull
    public static View $$(View view, @IdRes int id) {
        View result = view.findViewById(id);
        if (null == result) {
            throw new NullPointerException("Can't find view with id: " + id);
        }
        return result;
    }
}
