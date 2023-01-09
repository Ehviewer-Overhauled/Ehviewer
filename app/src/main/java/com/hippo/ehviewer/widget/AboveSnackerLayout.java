package com.hippo.ehviewer.widget;

import static androidx.core.util.ObjectsCompat.requireNonNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.math.MathUtils;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.drakeet.drawer.FullDraggableHelper;
import com.google.android.material.snackbar.Snackbar;
import com.hippo.yorozuya.LayoutUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AboveSnackerLayout extends FrameLayout implements CoordinatorLayout.AttachedBehavior, FullDraggableHelper.Callback {
    @NonNull
    private final FullDraggableHelper helper;

    private DrawerLayout drawerLayout;

    private List<View> mAboveSnackViewList;

    public AboveSnackerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        helper = new FullDraggableHelper(context, this);
    }

    public AboveSnackerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        helper = new FullDraggableHelper(context, this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ensureDrawerLayout();
    }

    private void ensureDrawerLayout() {
        ViewParent parent = getParent().getParent();
        if (!(parent instanceof DrawerLayout)) {
            throw new IllegalStateException("This " + this + " must be added to a DrawerLayout");
        }
        drawerLayout = (DrawerLayout) parent;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return helper.onInterceptTouchEvent(event);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        return helper.onTouchEvent(event);
    }

    @NonNull
    @Override
    public View getDrawerMainContainer() {
        return this;
    }

    @Override
    public boolean isDrawerOpen(int gravity) {
        return drawerLayout.isDrawerOpen(gravity);
    }

    @Override
    public boolean hasEnabledDrawer(int gravity) {
        return drawerLayout.getDrawerLockMode(gravity) == DrawerLayout.LOCK_MODE_UNLOCKED
                && findDrawerWithGravity(gravity) != null;
    }

    @Override
    public void offsetDrawer(int gravity, float offset) {
        setDrawerToOffset(gravity, offset);
        drawerLayout.invalidate();
    }

    @Override
    public void smoothOpenDrawer(int gravity) {
        drawerLayout.openDrawer(gravity, true);
    }

    @Override
    public void smoothCloseDrawer(int gravity) {
        drawerLayout.closeDrawer(gravity, true);
    }

    @Override
    public void onDrawerDragging() {
        List<DrawerLayout.DrawerListener> drawerListeners = getDrawerListeners();
        if (drawerListeners != null) {
            int listenerCount = drawerListeners.size();
            for (int i = listenerCount - 1; i >= 0; --i) {
                drawerListeners.get(i).onDrawerStateChanged(DrawerLayout.STATE_DRAGGING);
            }
        }
    }

    @Nullable
    protected List<DrawerLayout.DrawerListener> getDrawerListeners() {
        try {
            Field field = DrawerLayout.class.getDeclaredField("mListeners");
            field.setAccessible(true);
            // noinspection unchecked
            return (List<DrawerLayout.DrawerListener>) field.get(drawerLayout);
        } catch (Exception e) {
            // throw to let developer know the api is changed
            throw new RuntimeException(e);
        }
    }

    protected void setDrawerToOffset(int gravity, float offset) {
        View drawerView = findDrawerWithGravity(gravity);
        float slideOffsetPercent = MathUtils.clamp(offset / requireNonNull(drawerView).getWidth(), 0f, 1f);
        try {
            Method method = DrawerLayout.class.getDeclaredMethod("moveDrawerToOffset", View.class, float.class);
            method.setAccessible(true);
            method.invoke(drawerLayout, drawerView, slideOffsetPercent);
            drawerView.setVisibility(VISIBLE);
        } catch (Exception e) {
            // throw to let developer know the api is changed
            throw new RuntimeException(e);
        }
    }

    // Copied from DrawerLayout
    @Nullable
    private View findDrawerWithGravity(int gravity) {
        final int absHorizontalGravity = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(drawerLayout)) & Gravity.HORIZONTAL_GRAVITY_MASK;
        final int childCount = drawerLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = drawerLayout.getChildAt(i);
            final int childAbsGravity = getDrawerViewAbsoluteGravity(child);
            if ((childAbsGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == absHorizontalGravity) {
                return child;
            }
        }
        return null;
    }

    // Copied from DrawerLayout
    private int getDrawerViewAbsoluteGravity(View drawerView) {
        final int gravity = ((DrawerLayout.LayoutParams) drawerView.getLayoutParams()).gravity;
        return GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(drawerLayout));
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
