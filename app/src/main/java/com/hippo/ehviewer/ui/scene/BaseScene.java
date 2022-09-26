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

package com.hippo.ehviewer.ui.scene;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.scene.SceneFragment;

import kotlin.Unit;

public abstract class BaseScene extends SceneFragment {

    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    public static final String KEY_DRAWER_VIEW_STATE =
            "com.hippo.ehviewer.ui.scene.BaseScene:DRAWER_VIEW_STATE";

    private WindowInsetsControllerCompat insetsController;

    @Nullable
    private View drawerView;
    @Nullable
    private SparseArray<Parcelable> drawerViewState;

    public void addAboveSnackView(View view) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).addAboveSnackView(view);
        }
    }

    public void removeAboveSnackView(View view) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).removeAboveSnackView(view);
        }
    }

    public void setDrawerLockMode(int lockMode, int edgeGravity) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).setDrawerLockMode(lockMode, edgeGravity);
        }
    }

    public void openDrawer(int drawerGravity) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).openDrawer(drawerGravity);
        }
    }

    public void closeDrawer(int drawerGravity) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).closeDrawer(drawerGravity);
        }
    }

    public void toggleDrawer(int drawerGravity) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).toggleDrawer(drawerGravity);
        }
    }

    public void showTip(CharSequence message, int length) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).showTip(message, length);
        }
    }

    public Unit showTip(@StringRes int id, int length) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).showTip(id, length);
        }
        return null;
    }

    public boolean needShowLeftDrawer() {
        return true;
    }

    public int getNavCheckedItem() {
        return 0;
    }

    /**
     * @param resId 0 for clear
     */
    public void setNavCheckedItem(@IdRes int resId) {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).setNavCheckedItem(resId);
        }
    }

    public void recreateDrawerView() {
        MainActivity activity = getMainActivity();
        if (activity != null) {
            activity.createDrawerView(this);
        }
    }

    public final View createDrawerView(LayoutInflater inflater,
                                       @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        drawerView = onCreateDrawerView(inflater, container, savedInstanceState);

        if (drawerView != null) {
            SparseArray<Parcelable> saved = drawerViewState;
            if (saved == null && savedInstanceState != null) {
                saved = savedInstanceState.getSparseParcelableArray(KEY_DRAWER_VIEW_STATE);
            }
            if (saved != null) {
                drawerView.restoreHierarchyState(saved);
            }
        }

        return drawerView;
    }

    public View onCreateDrawerView(LayoutInflater inflater,
                                   @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    public final void destroyDrawerView() {
        if (drawerView != null) {
            drawerViewState = new SparseArray<>();
            drawerView.saveHierarchyState(drawerViewState);
        }

        onDestroyDrawerView();

        drawerView = null;
    }

    public void onDestroyDrawerView() {
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        postponeEnterTransition();
        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                startPostponedEnterTransition();
                return true;
            }
        });

        // Update left drawer locked state
        if (needShowLeftDrawer()) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
        } else {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
        }

        // Update nav checked item
        setNavCheckedItem(getNavCheckedItem());

        // Hide soft ime
        hideSoftInput();

        insetsController = getInsetsController();
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars((requireActivity().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) <= 0);
        }
    }

    @Nullable
    public Resources getResourcesOrNull() {
        Context context = getContext();
        if (context != null) {
            return context.getResources();
        } else {
            return null;
        }
    }

    @Nullable
    public MainActivity getMainActivity() {
        FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            return (MainActivity) activity;
        } else {
            return null;
        }
    }

    public void hideSoftInput() {
        var insetsController = getInsetsController();
        if (insetsController != null) {
            insetsController.hide(WindowInsetsCompat.Type.ime());
        }
    }

    public void showSoftInput(@Nullable View view) {
        if (view != null) {
            view.requestFocus();
            view.post(() -> {
                var insetsController = getInsetsController();
                if (insetsController != null) {
                    insetsController.show(WindowInsetsCompat.Type.ime());
                }
            });
        }
    }

    public WindowInsetsControllerCompat getInsetsController() {
        if (insetsController == null) {
            var activity = getActivity();
            if (activity != null) {
                insetsController = ViewCompat.getWindowInsetsController(activity.getWindow().getDecorView());
            }
        }
        return insetsController;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        insetsController = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (drawerView != null) {
            drawerViewState = new SparseArray<>();
            drawerView.saveHierarchyState(drawerViewState);
            outState.putSparseParcelableArray(KEY_DRAWER_VIEW_STATE, drawerViewState);
        }
    }

    public Resources.Theme getTheme() {
        return requireActivity().getTheme();
    }
}
