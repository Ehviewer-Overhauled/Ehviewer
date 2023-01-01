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
package com.hippo.widget

import android.animation.Animator
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Interpolator
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hippo.ehviewer.R
import com.hippo.scene.StageActivity
import com.hippo.yorozuya.AnimationUtils
import com.hippo.yorozuya.SimpleAnimatorListener

class FabLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr), View.OnClickListener {
    private var mFabSize = 0
    private var mFabMiniSize = 0
    private var mIntervalPrimary = 0
    private var mIntervalSecondary = 0
    private var mExpanded = true
    private var mAutoCancel = true
    private var mHidePrimaryFab = false
    private var mMainFabCenterY = -1f
    private var mOnExpandListener: OnExpandListener? = null
    private var mOnClickFabListener: OnClickFabListener? = null

    init {
        isSoundEffectsEnabled = false
        clipToPadding = false
        mFabSize = context.resources.getDimensionPixelOffset(R.dimen.fab_size)
        mFabMiniSize = context.resources.getDimensionPixelOffset(R.dimen.fab_min_size)
        mIntervalPrimary =
            context.resources.getDimensionPixelOffset(R.dimen.fab_layout_primary_margin)
        mIntervalSecondary =
            context.resources.getDimensionPixelOffset(R.dimen.fab_layout_secondary_margin)
    }

    override fun addView(child: View, index: Int, params: LayoutParams) {
        check(child is FloatingActionButton) { "FloatingActionBarLayout should only " + "contain FloatingActionButton, but try to add " + child.javaClass.name }
        super.addView(child, index, params)
    }

    val primaryFab: FloatingActionButton?
        get() = getChildAt(childCount - 1) as? FloatingActionButton

    private val secondaryFabCount: Int
        get() = 0.coerceAtLeast(childCount - 1)

    private fun getSecondaryFabAt(index: Int): FloatingActionButton? {
        return if (index < 0 || index >= secondaryFabCount) {
            null
        } else getChildAt(index) as FloatingActionButton
    }

    fun setSecondaryFabVisibilityAt(index: Int, visible: Boolean) {
        getSecondaryFabAt(index)?.run {
            if (visible && visibility == GONE) {
                animate().cancel()
                visibility = if (mExpanded) VISIBLE else INVISIBLE
            } else if (!visible && visibility != GONE) {
                animate().cancel()
                visibility = GONE
            }
        }
    }

    private fun getChildMeasureSpec(parentMeasureSpec: Int): Int {
        val parentMode = MeasureSpec.getMode(parentMeasureSpec)
        val parentSize = MeasureSpec.getSize(parentMeasureSpec)
        val childMode: Int
        val childSize: Int
        when (parentMode) {
            MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> {
                childMode = MeasureSpec.AT_MOST
                childSize = parentSize
            }

            MeasureSpec.UNSPECIFIED -> {
                childMode = MeasureSpec.UNSPECIFIED
                childSize = parentSize
            }

            else -> {
                childMode = MeasureSpec.AT_MOST
                childSize = parentSize
            }
        }
        return MeasureSpec.makeMeasureSpec(childSize, childMode)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec)
        val childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec)
        measureChildren(childWidthMeasureSpec, childHeightMeasureSpec)
        var maxWidth = 0
        var maxHeight = 0
        val count = childCount
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                continue
            }
            maxWidth = maxWidth.coerceAtLeast(child.measuredWidth)
            maxHeight += child.measuredHeight
        }
        maxWidth += paddingLeft + paddingRight
        maxHeight += paddingTop + paddingBottom
        maxHeight = maxHeight.coerceAtLeast(suggestedMinimumHeight)
        maxWidth = maxWidth.coerceAtLeast(suggestedMinimumWidth)
        setMeasuredDimension(
            resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
            resolveSizeAndState(maxHeight, heightMeasureSpec, 0)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var centerX = 0
        var bottom = measuredHeight - paddingBottom
        val count = childCount
        var i = count
        while (--i >= 0) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                continue
            }
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight
            var layoutBottom: Int
            var layoutRight: Int
            if (i == count - 1) {
                layoutBottom = bottom + (childHeight - mFabSize) / 2
                layoutRight = measuredWidth - paddingRight + (childWidth - mFabSize) / 2
                bottom -= mFabSize + mIntervalPrimary
                centerX = layoutRight - childWidth / 2
                mMainFabCenterY = layoutBottom - childHeight / 2f
            } else {
                layoutBottom = bottom + (childHeight - mFabMiniSize) / 2
                layoutRight = centerX + childWidth / 2
                bottom -= mFabMiniSize + mIntervalSecondary
            }
            child.layout(
                layoutRight - childWidth,
                layoutBottom - childHeight,
                layoutRight,
                layoutBottom
            )
        }
    }

    fun setOnExpandListener(listener: OnExpandListener?) {
        mOnExpandListener = listener
    }

    fun setOnClickFabListener(listener: OnClickFabListener?) {
        mOnClickFabListener = listener
        if (listener != null) {
            var i = 0
            val n = childCount
            while (i < n) {
                getChildAt(i).setOnClickListener(this)
                i++
            }
        } else {
            var i = 0
            val n = childCount
            while (i < n) {
                getChildAt(i).isClickable = false
                i++
            }
        }
    }

    fun setHidePrimaryFab(hidePrimaryFab: Boolean) {
        if (mHidePrimaryFab != hidePrimaryFab) {
            mHidePrimaryFab = hidePrimaryFab
            val expanded = mExpanded
            val count = childCount
            if (!expanded && count > 0) {
                getChildAt(count - 1).visibility = if (hidePrimaryFab) INVISIBLE else VISIBLE
            }
        }
    }

    fun setAutoCancel(autoCancel: Boolean) {
        if (mAutoCancel != autoCancel) {
            mAutoCancel = autoCancel
            if (mExpanded) {
                if (autoCancel) {
                    setOnClickListener(this)
                } else {
                    isClickable = false
                }
            }
        }
    }

    fun toggle() {
        isExpanded = !mExpanded
    }

    var isExpanded: Boolean
        get() = mExpanded
        set(expanded) {
            setExpanded(expanded, true)
        }

    fun setExpanded(expanded: Boolean, animation: Boolean) {
        if (mExpanded != expanded) {
            mExpanded = expanded
            if (mAutoCancel) {
                if (expanded) {
                    setOnClickListener(this)
                } else {
                    isClickable = false
                }
            }
            val count = childCount
            if (count > 0) {
                if (mMainFabCenterY == -1f || !animation) {
                    // It is before first onLayout
                    val checkCount = if (mHidePrimaryFab) count else count - 1
                    for (i in 0 until checkCount) {
                        val child = getChildAt(i)
                        if (child.visibility == GONE) {
                            continue
                        }
                        child.visibility = if (expanded) VISIBLE else INVISIBLE
                        if (expanded) {
                            child.alpha = 1f
                        }
                    }
                } else {
                    if (mHidePrimaryFab) {
                        setPrimaryFabAnimation(getChildAt(count - 1), expanded, !expanded)
                    }
                    for (i in 0 until count - 1) {
                        val child = getChildAt(i)
                        if (child.visibility == GONE) {
                            continue
                        }
                        setSecondaryFabAnimation(child, expanded, expanded)
                    }
                }
            }
            mOnExpandListener?.onExpand(expanded)
        }
        (context as StageActivity).updateBackPressCallBackStatus()
    }

    private fun setPrimaryFabAnimation(child: View, expanded: Boolean, delay: Boolean) {
        val startRotation: Float
        val endRotation: Float
        val startScale: Float
        val endScale: Float
        val interpolator: Interpolator
        if (expanded) {
            startRotation = -45.0f
            endRotation = 0.0f
            startScale = 0.0f
            endScale = 1.0f
            interpolator = AnimationUtils.FAST_SLOW_INTERPOLATOR
        } else {
            startRotation = 0.0f
            endRotation = 0.0f
            startScale = 1.0f
            endScale = 0.0f
            interpolator = AnimationUtils.SLOW_FAST_INTERPOLATOR
        }
        child.scaleX = startScale
        child.scaleY = startScale
        child.rotation = startRotation
        child.animate()
            .scaleX(endScale)
            .scaleY(endScale)
            .rotation(endRotation)
            .setStartDelay(if (delay) ANIMATE_TIME else 0L)
            .setDuration(ANIMATE_TIME)
            .setInterpolator(interpolator)
            .setListener(object : SimpleAnimatorListener() {
                override fun onAnimationStart(animation: Animator) {
                    if (expanded) {
                        child.visibility = VISIBLE
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!expanded) {
                        child.visibility = INVISIBLE
                    }
                }
            }).start()
    }

    private fun setSecondaryFabAnimation(child: View, expanded: Boolean, delay: Boolean) {
        val startTranslationY: Float
        val endTranslationY: Float
        val startAlpha: Float
        val endAlpha: Float
        val interpolator: Interpolator
        if (expanded) {
            startTranslationY = mMainFabCenterY - child.top - child.height / 2
            endTranslationY = 0f
            startAlpha = 0f
            endAlpha = 1f
            interpolator = AnimationUtils.FAST_SLOW_INTERPOLATOR
        } else {
            startTranslationY = 0f
            endTranslationY = mMainFabCenterY - child.top - child.height / 2
            startAlpha = 1f
            endAlpha = 0f
            interpolator = AnimationUtils.SLOW_FAST_INTERPOLATOR
        }
        child.alpha = startAlpha
        child.translationY = startTranslationY
        child.animate()
            .alpha(endAlpha)
            .translationY(endTranslationY)
            .setStartDelay(if (delay) ANIMATE_TIME else 0L)
            .setDuration(ANIMATE_TIME)
            .setInterpolator(interpolator)
            .setListener(object : SimpleAnimatorListener() {
                override fun onAnimationStart(animation: Animator) {
                    if (expanded) {
                        child.visibility = VISIBLE
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!expanded) {
                        child.visibility = INVISIBLE
                    }
                }
            }).start()
    }

    override fun onClick(v: View) {
        if (this === v) {
            isExpanded = false
        } else mOnClickFabListener?.let {
            val position = indexOfChild(v)
            if (position == childCount - 1) {
                it.onClickPrimaryFab(this, v as FloatingActionButton)
            } else if (position >= 0 && mExpanded) {
                it.onClickSecondaryFab(this, v as FloatingActionButton, position)
            }
        }
    }

    override fun dispatchSetPressed(pressed: Boolean) {
        // Don't dispatch it to children
    }

    override fun onSaveInstanceState(): Parcelable {
        val state = Bundle()
        state.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState())
        state.putBoolean(STATE_KEY_AUTO_CANCEL, mAutoCancel)
        state.putBoolean(STATE_KEY_EXPANDED, mExpanded)
        return state
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getBundle(STATE_KEY_SUPER))
            setAutoCancel(state.getBoolean(STATE_KEY_AUTO_CANCEL))
            setExpanded(state.getBoolean(STATE_KEY_EXPANDED), false)
        }
    }

    interface OnExpandListener {
        fun onExpand(expanded: Boolean)
    }

    interface OnClickFabListener {
        fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton)
        fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int)
    }

    companion object {
        private const val ANIMATE_TIME = 300L
        private const val STATE_KEY_SUPER = "super"
        private const val STATE_KEY_AUTO_CANCEL = "auto_cancel"
        private const val STATE_KEY_EXPANDED = "expanded"
    }
}