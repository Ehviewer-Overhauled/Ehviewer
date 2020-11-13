/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.ViewPropertyAnimator;

public class ViewTransition {

    protected static final long ANIMATE_TIME = 300L;

    private final View[] mViews;

    private int mShownView = -1;

    protected ViewPropertyAnimator mAnimator1;
    protected ViewPropertyAnimator mAnimator2;

    private OnShowViewListener mOnShowViewListener;

    public ViewTransition(View... views) {
        if (views.length < 2) {
            throw new IllegalStateException("You must pass view to ViewTransition");
        }
        for (View v : views) {
            if (v == null) {
                throw new IllegalStateException("Any View pass to ViewTransition must not be null");
            }
        }

        mViews = views;
        showView(0, false);
    }

    public void setOnShowViewListener(OnShowViewListener listener) {
        mOnShowViewListener = listener;
    }

    public int getShownViewIndex() {
        return mShownView;
    }

    public boolean showView(int shownView) {
        return showView(shownView, true);
    }

    public boolean showView(int shownView, boolean animation) {
        View[] views = mViews;
        int length = views.length;
        if (shownView >= length || shownView < 0) {
            throw new IndexOutOfBoundsException("Only " + length + " view(s) in " +
                    "the ViewTransition, but attempt to show " + shownView);
        }

        if (mShownView != shownView) {
            int oldShownView = mShownView;
            mShownView = shownView;

            // Cancel animation
            if (mAnimator1 != null) {
                mAnimator1.cancel();
            }
            if (mAnimator2 != null) {
                mAnimator2.cancel();
            }

            if (animation) {
                startAnimations(views[oldShownView], views[shownView]);
            } else {
                for (int i = 0; i < length; i++) {
                    View v = views[i];
                    if (i == shownView) {
                        v.setAlpha(1f);
                        v.setVisibility(View.VISIBLE);
                    } else {
                        v.setAlpha(0f);
                        v.setVisibility(View.GONE);
                    }
                }
            }

            if (null != mOnShowViewListener) {
                mOnShowViewListener.onShowView(views[oldShownView], views[shownView]);
            }

            return true;
        } else {
            return false;
        }
    }

    protected void startAnimations(final View hiddenView, final View shownView) {
        mAnimator1 = hiddenView.animate().alpha(0);
        mAnimator1.setDuration(ANIMATE_TIME).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (hiddenView != null) {
                    hiddenView.setVisibility(View.GONE);
                }
                mAnimator1 = null;
            }
        }).start();

        shownView.setAlpha(0);
        shownView.setVisibility(View.VISIBLE);
        mAnimator2 = shownView.animate().alpha(1);
        mAnimator2.setDuration(ANIMATE_TIME).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimator2 = null;
            }
        }).start();
    }

    public interface OnShowViewListener {
        void onShowView(View hiddenView, View shownView);
    }
}
