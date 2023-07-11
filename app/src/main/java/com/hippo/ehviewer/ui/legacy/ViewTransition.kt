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
import android.view.ViewPropertyAnimator

open class ViewTransition(vararg views: View) {
    private val mViews: Array<View>
    protected var mAnimator1: ViewPropertyAnimator? = null
    protected var mAnimator2: ViewPropertyAnimator? = null
    var shownViewIndex = -1
        private set
    private var mOnShowViewListener: OnShowViewListener? = null

    init {
        check(views.size >= 2) { "You must pass view to ViewTransition" }
        mViews = arrayOf(*views)
        showView(0, false)
    }

    fun setOnShowViewListener(listener: OnShowViewListener?) {
        mOnShowViewListener = listener
    }

    fun showView(shownView: Int, animation: Boolean = true): Boolean {
        val views = mViews
        val length = views.size
        if (shownView >= length || shownView < 0) {
            throw IndexOutOfBoundsException(
                "Only " + length + " view(s) in " +
                    "the ViewTransition, but attempt to show " + shownView,
            )
        }
        return if (shownViewIndex != shownView) {
            val oldShownView = shownViewIndex
            shownViewIndex = shownView

            // Cancel animation
            if (mAnimator1 != null) {
                mAnimator1!!.cancel()
            }
            if (mAnimator2 != null) {
                mAnimator2!!.cancel()
            }
            if (animation) {
                startAnimations(views[oldShownView], views[shownView])
            } else {
                for (i in 0 until length) {
                    val v = views[i]
                    if (i == shownView) {
                        v.alpha = 1f
                        v.visibility = View.VISIBLE
                    } else {
                        v.alpha = 0f
                        v.visibility = View.GONE
                    }
                }
            }
            if (null != mOnShowViewListener) {
                mOnShowViewListener!!.onShowView(views[oldShownView], views[shownView])
            }
            true
        } else {
            false
        }
    }

    protected open fun startAnimations(hiddenView: View, shownView: View) {
        mAnimator1 = hiddenView.animate().alpha(0f)
        mAnimator1!!.setDuration(ANIMATE_TIME).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                hiddenView.visibility = View.GONE
                mAnimator1 = null
            }
        }).start()
        shownView.alpha = 0f
        shownView.visibility = View.VISIBLE
        mAnimator2 = shownView.animate().alpha(1f)
        mAnimator2!!.setDuration(ANIMATE_TIME).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mAnimator2 = null
            }
        }).start()
    }

    interface OnShowViewListener {
        fun onShowView(hiddenView: View, shownView: View)
    }

    companion object {
        const val ANIMATE_TIME = 300L
    }
}
