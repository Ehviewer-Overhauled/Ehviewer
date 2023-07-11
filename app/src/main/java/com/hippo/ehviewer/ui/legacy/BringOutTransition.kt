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
package com.hippo.ehviewer.ui.legacy

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.widget.FrameLayout

class BringOutTransition(contentLayout: FrameLayout, mSearchLayout: SearchLayout) :
    ViewTransition(contentLayout, mSearchLayout) {
    override fun startAnimations(hiddenView: View, shownView: View) {
        mAnimator1 = hiddenView.animate().alpha(0f).scaleY(0.7f).scaleX(0.7f).apply {
            setDuration(ANIMATE_TIME).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    hiddenView.visibility = View.GONE
                    mAnimator1 = null
                }
            }).start()
        }
        shownView.alpha = 0f
        shownView.scaleX = 0.7f
        shownView.scaleY = 0.7f
        shownView.visibility = View.VISIBLE
        mAnimator2 = shownView.animate().alpha(1f).scaleX(1f).scaleY(1f).apply {
            setDuration(ANIMATE_TIME).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mAnimator2 = null
                }
            }).start()
        }
    }
}
