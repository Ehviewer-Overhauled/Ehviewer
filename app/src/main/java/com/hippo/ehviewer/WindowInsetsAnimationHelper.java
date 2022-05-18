package com.hippo.ehviewer;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hippo.yorozuya.MathUtils;

import java.util.HashMap;
import java.util.List;

public class WindowInsetsAnimationHelper extends WindowInsetsAnimationCompat.Callback {
    private final View[] views;
    private final HashMap<View, Integer> startPaddings = new HashMap<>();
    private final HashMap<View, Integer> endPaddings = new HashMap<>();
    WindowInsetsAnimationCompat animation;

    public WindowInsetsAnimationHelper(int dispatchMode, View... views) {
        super(dispatchMode);
        this.views = views;
    }

    @Override
    public void onPrepare(@NonNull WindowInsetsAnimationCompat animation) {
        super.onPrepare(animation);
        this.animation = animation;
        for (View view : views) {
            if (view == null) {
                continue;
            }
            startPaddings.put(view, view.getPaddingBottom());
        }
    }

    @NonNull
    @Override
    public WindowInsetsAnimationCompat.BoundsCompat onStart(@NonNull WindowInsetsAnimationCompat animation, @NonNull WindowInsetsAnimationCompat.BoundsCompat bounds) {
        this.animation = animation;
        for (View view : views) {
            if (view == null) {
                continue;
            }
            endPaddings.put(view, view.getPaddingBottom());
            int startPadding = startPaddings.containsKey(view) ? startPaddings.get(view) : 0;
            view.setTranslationY(-(startPadding - view.getPaddingBottom()));
        }
        return bounds;
    }

    @NonNull
    @Override
    public WindowInsetsCompat onProgress(@NonNull WindowInsetsCompat insets, @NonNull List<WindowInsetsAnimationCompat> runningAnimations) {
        if (animation == null) {
            return insets;
        }
        WindowInsetsAnimationCompat imeAnimation = null;
        for (WindowInsetsAnimationCompat animation : runningAnimations) {
            if ((animation.getTypeMask() & WindowInsetsCompat.Type.ime()) != 0) {
                imeAnimation = animation;
                break;
            }
        }
        if (imeAnimation != null) {
            for (View view : views) {
                if (view == null) {
                    continue;
                }
                int startPadding = startPaddings.containsKey(view) ? startPaddings.get(view) : 0;
                int endPadding = endPaddings.containsKey(view) ? endPaddings.get(view) : 0;
                view.setTranslationY(MathUtils.lerp(endPadding - startPadding, 0, animation.getInterpolatedFraction()));
            }
        }
        return insets;
    }

    @Override
    public void onEnd(@NonNull WindowInsetsAnimationCompat animation) {
        super.onEnd(animation);
        startPaddings.clear();
        endPaddings.clear();
        this.animation = null;
        for (View view : views) {
            if (view == null) {
                continue;
            }
            view.setTranslationY(0);
        }
    }
}
