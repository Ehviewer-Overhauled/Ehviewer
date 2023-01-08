package com.hippo.ehviewer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.snackbar.Snackbar;
import com.hippo.yorozuya.LayoutUtils;

import java.util.ArrayList;
import java.util.List;

public class AboveSnackerLayout extends FrameLayout implements CoordinatorLayout.AttachedBehavior {

    private List<View> mAboveSnackViewList;

    public AboveSnackerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AboveSnackerLayout(Context context, AttributeSet attrs, int defStyle) {
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
    public AboveSnackerLayout.Behavior getBehavior() {
        return new AboveSnackerLayout.Behavior();
    }

    public static class Behavior extends CoordinatorLayout.Behavior<AboveSnackerLayout> {

        @Override
        public boolean layoutDependsOn(@NonNull CoordinatorLayout parent, @NonNull AboveSnackerLayout child, @NonNull View dependency) {
            return dependency instanceof Snackbar.SnackbarLayout;
        }

        @Override
        public boolean onDependentViewChanged(@NonNull CoordinatorLayout parent, AboveSnackerLayout child, @NonNull View dependency) {
            for (int i = 0, n = child.getAboveSnackViewCount(); i < n; i++) {
                View view = child.getAboveSnackViewAt(i);
                if (view != null) {
                    float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight() - LayoutUtils.dp2pix(view.getContext(), 8));
                    ViewCompat.animate(view).setInterpolator(new FastOutSlowInInterpolator()).translationY(translationY).setDuration(150).start();
                }
            }
            return false;
        }

        @Override
        public void onDependentViewRemoved(@NonNull CoordinatorLayout parent, @NonNull AboveSnackerLayout child, @NonNull View dependency) {
            for (int i = 0, n = child.getAboveSnackViewCount(); i < n; i++) {
                View view = child.getAboveSnackViewAt(i);
                if (view != null) {
                    ViewCompat.animate(view).setInterpolator(new FastOutSlowInInterpolator()).translationY(0).setDuration(75).start();
                }
            }
        }
    }
}
