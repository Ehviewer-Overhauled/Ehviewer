/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.snackbar.Snackbar;
import com.hippo.drawerlayout.DrawerLayout;
import com.hippo.yorozuya.LayoutUtils;

import java.util.ArrayList;
import java.util.List;

public class EhDrawerLayout extends DrawerLayout implements CoordinatorLayout.AttachedBehavior {

    private List<View> mAboveSnackViewList;

    public EhDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EhDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void addAboveSnackView(View view) {
        if (null == mAboveSnackViewList) {
            mAboveSnackViewList = new ArrayList<>();
        }
        mAboveSnackViewList.add(view);
    }

    public void removeAboveSnackView(View view) {
        if (null == mAboveSnackViewList) {
            return;
        }
        mAboveSnackViewList.remove(view);
    }

    public int getAboveSnackViewCount() {
        return null == mAboveSnackViewList ? 0 : mAboveSnackViewList.size();
    }

    @Nullable
    public View getAboveSnackViewAt(int index) {
        if (null == mAboveSnackViewList || index < 0 || index >= mAboveSnackViewList.size()) {
            return null;
        } else {
            return mAboveSnackViewList.get(index);
        }
    }

    @NonNull
    @Override
    public Behavior getBehavior() {
        return new Behavior();
    }

    public static class Behavior extends CoordinatorLayout.Behavior<EhDrawerLayout> {

        @Override
        public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull EhDrawerLayout child, @NonNull View dependency) {
            return dependency instanceof Snackbar.SnackbarLayout;
        }

        @Override
        public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, EhDrawerLayout child, @NonNull View dependency) {
            for (int i = 0, n = child.getAboveSnackViewCount(); i < n; i++) {
                View view = child.getAboveSnackViewAt(i);
                if (view != null) {
                    float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight() - LayoutUtils.dp2pix(view.getContext(), 8));
                    ViewCompat.animate(view).setInterpolator(new OvershootInterpolator()).translationY(translationY).start();
                }
            }
            return false;
        }

        @Override
        public void onDependentViewRemoved(@NonNull CoordinatorLayout parent, @NonNull EhDrawerLayout child, @NonNull View dependency) {
            if (child.getAboveSnackViewCount() > 1) {
                return;
            }
            for (int i = 0, n = child.getAboveSnackViewCount(); i < n; i++) {
                View view = child.getAboveSnackViewAt(i);
                if (view != null) {
                    ViewCompat.animate(view).setInterpolator(new OvershootInterpolator()).translationY(0).start();
                }
            }
        }
    }
}
