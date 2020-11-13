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

import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.widget.ContentLayout;

public class BringOutTransition extends ViewTransition {

    public BringOutTransition(ContentLayout contentLayout, SearchLayout mSearchLayout) {
        super(contentLayout, mSearchLayout);
    }

    @Override
    protected void startAnimations(final View hiddenView, final View shownView) {
        mAnimator1 = hiddenView.animate().alpha(0).scaleY(0.7f).scaleX(0.7f);
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
        shownView.setScaleX(0.7f);
        shownView.setScaleY(0.7f);
        shownView.setVisibility(View.VISIBLE);
        mAnimator2 = shownView.animate().alpha(1f).scaleX(1).scaleY(1);
        mAnimator2.setDuration(ANIMATE_TIME).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimator2 = null;
            }
        }).start();
    }
}
