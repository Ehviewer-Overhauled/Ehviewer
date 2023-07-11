/*
 * Copyright 2015 Hippo Seven
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
package com.hippo.ehviewer.ui.legacy

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.math.MathUtils.dist
import com.hippo.ehviewer.R
import com.hippo.ehviewer.util.AnimationUtils
import com.hippo.ehviewer.util.SimpleAnimatorListener
import com.hippo.ehviewer.util.SimpleHandler
import com.hippo.ehviewer.util.dp2px
import kotlin.math.abs

class FastScroller : View {
    private var mDraggable = false
    private var mMinHandlerHeight = 0
    private var mRecyclerView: RecyclerView? = null
    private var mOnScrollChangeListener: RecyclerView.OnScrollListener? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mAdapterDataObserver: AdapterDataObserver? = null
    private var mHandler: Drawable? = null
    private var mHandlerOffset = INVALID
    private var mHandlerHeight = INVALID
    private var mDownX = INVALID.toFloat()
    private var mDownY = INVALID.toFloat()
    private var mLastMotionY = INVALID.toFloat()
    private var mDragged = false
    private var mCantDrag = false
    private var mTouchSlop = 0
    private var mListener: OnDragHandlerListener? = null
    private var mShowAnimator: ObjectAnimator? = null
    private var mHideAnimator: ObjectAnimator? = null
    private val mHideRunnable = Runnable { mHideAnimator!!.start() }

    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FastScroller, defStyleAttr, 0)
        mHandler = a.getDrawable(R.styleable.FastScroller_handler)
        mDraggable = a.getBoolean(R.styleable.FastScroller_draggable, true)
        a.recycle()
        alpha = 0.0f
        visibility = INVISIBLE
        mMinHandlerHeight = dp2px(context, MIN_HANDLER_HEIGHT_DP.toFloat())
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mShowAnimator = ObjectAnimator.ofFloat(this, "alpha", 1.0f).apply {
            interpolator = AnimationUtils.FAST_SLOW_INTERPOLATOR
            duration = SCROLL_BAR_FADE_DURATION.toLong()
        }
        mHideAnimator = ObjectAnimator.ofFloat(this, "alpha", 0.0f).apply {
            interpolator = AnimationUtils.SLOW_FAST_INTERPOLATOR
            duration = SCROLL_BAR_FADE_DURATION.toLong()
            addListener(object : SimpleAnimatorListener() {
                private var mCancel = false
                override fun onAnimationCancel(animation: Animator) {
                    mCancel = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (mCancel) {
                        mCancel = false
                    } else {
                        visibility = INVISIBLE
                    }
                }
            })
        }
    }

    fun setOnDragHandlerListener(listener: OnDragHandlerListener?) {
        mListener = listener
    }

    private fun updatePosition(show: Boolean) {
        if (mRecyclerView == null) {
            return
        }
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val height = height - paddingTop - paddingBottom
        val offset = mRecyclerView!!.computeVerticalScrollOffset()
        val extent = mRecyclerView!!.computeVerticalScrollExtent()
        val range = mRecyclerView!!.computeVerticalScrollRange()
        if (height <= 0 || extent >= range || extent <= 0) {
            return
        }
        var endOffset = height * offset / range
        var endHeight = height * extent / range
        endHeight = endHeight.coerceAtLeast(mMinHandlerHeight)
        endOffset = endOffset.coerceAtMost(height - endHeight)
        mHandlerOffset = endOffset + paddingTop
        mHandlerHeight = endHeight
        if (show) {
            if (mHideAnimator!!.isRunning) {
                mHideAnimator!!.cancel()
                mShowAnimator!!.start()
            } else if (visibility != VISIBLE && !mShowAnimator!!.isRunning) {
                visibility = VISIBLE
                mShowAnimator!!.start()
            }
            SimpleHandler.removeCallbacks(mHideRunnable)
            if (!mDragged) {
                SimpleHandler.postDelayed(mHideRunnable, SCROLL_BAR_DELAY.toLong())
            }
        }
    }

    fun setHandlerDrawable(drawable: Drawable?) {
        mHandler = drawable
        invalidate()
    }

    var isDraggable: Boolean
        get() = mDraggable
        set(draggable) {
            mDraggable = draggable
            if (mDragged) {
                mDragged = false
            }
            SimpleHandler.removeCallbacks(mHideRunnable)
            mHideRunnable.run()
        }
    val isAttached: Boolean
        get() = mRecyclerView != null

    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (recyclerView == null) {
            return
        }
        check(mRecyclerView == null) {
            "The FastScroller is already attached to a RecyclerView, " +
                "call detachedFromRecyclerView first"
        }
        mRecyclerView = recyclerView
        mOnScrollChangeListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {}
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updatePosition(true)
                invalidate()
            }
        }
        recyclerView.addOnScrollListener(mOnScrollChangeListener!!)
        mAdapter = recyclerView.adapter
        if (mAdapter != null) {
            mAdapterDataObserver = object : AdapterDataObserver() {
                override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                    super.onItemRangeChanged(positionStart, itemCount)
                    updatePosition(false)
                    invalidate()
                }

                override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                    super.onItemRangeChanged(positionStart, itemCount, payload)
                    updatePosition(false)
                    invalidate()
                }

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    updatePosition(false)
                    invalidate()
                }

                override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                    super.onItemRangeRemoved(positionStart, itemCount)
                    updatePosition(false)
                    invalidate()
                }

                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                    super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                    updatePosition(false)
                    invalidate()
                }
            }
            mAdapter!!.registerAdapterDataObserver(mAdapterDataObserver!!)
        }
    }

    fun detachedFromRecyclerView() {
        if (mRecyclerView != null && mOnScrollChangeListener != null) {
            mRecyclerView!!.removeOnScrollListener(mOnScrollChangeListener!!)
        }
        mRecyclerView = null
        mOnScrollChangeListener = null
        if (mAdapter != null && mAdapterDataObserver != null) {
            mAdapter!!.unregisterAdapterDataObserver(mAdapterDataObserver!!)
        }
        mAdapter = null
        mAdapterDataObserver = null
        alpha = 0.0f
        visibility = INVISIBLE
    }

    override fun onDraw(canvas: Canvas) {
        if (mRecyclerView == null || mHandler == null) {
            return
        }
        if (mHandlerHeight == INVALID) {
            updatePosition(false)
        }
        if (mHandlerHeight == INVALID) {
            return
        }
        val paddingLeft = paddingLeft
        val saved = canvas.save()
        canvas.translate(paddingLeft.toFloat(), mHandlerOffset.toFloat())
        mHandler!!.setBounds(0, 0, width - paddingLeft - paddingRight, mHandlerHeight)
        mHandler!!.draw(canvas)
        canvas.restoreToCount(saved)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mDraggable || visibility != VISIBLE || mRecyclerView == null || mHandlerHeight == INVALID) {
            return false
        }
        val action = event.action
        if (action == MotionEvent.ACTION_DOWN) {
            mCantDrag = false
        }
        if (mCantDrag) {
            return false
        }
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mDragged = false
                mDownX = event.x
                mDownY = event.y
                if (mDownY < mHandlerOffset || mDownY > mHandlerOffset + mHandlerHeight) {
                    mCantDrag = true
                    return false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!mDragged) {
                    val x = event.x
                    val y = event.y
                    // Check touch slop
                    if (dist(x, y, mDownX, mDownY) < mTouchSlop) {
                        return true
                    }
                    if (abs(x - mDownX) > abs(y - mDownY) || y < mHandlerOffset || y > mHandlerOffset + mHandlerHeight) {
                        mCantDrag = true
                        return false
                    } else {
                        mDragged = true
                        SimpleHandler.removeCallbacks(mHideRunnable)
                        // Update mLastMotionY
                        mLastMotionY =
                            if (mDownY < mHandlerOffset || mDownY >= mHandlerOffset + mHandlerHeight) {
                                // the point out of handler, make the point in handler center
                                (mHandlerOffset + mHandlerHeight / 2).toFloat()
                            } else {
                                mDownY
                            }
                        // Notify
                        if (mListener != null) {
                            mListener!!.onStartDragHandler()
                        }
                    }
                }
                val range = mRecyclerView!!.computeVerticalScrollRange()
                if (range <= 0) {
                    return true
                }
                val y = event.y
                val scroll =
                    (range * (y - mLastMotionY) / (height - paddingTop - paddingBottom)).toInt()
                mRecyclerView!!.scrollBy(0, scroll)
                mLastMotionY = y
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Notify
                if (mDragged && mListener != null) {
                    mListener!!.onEndDragHandler()
                }
                mDragged = false
                SimpleHandler.postDelayed(mHideRunnable, SCROLL_BAR_DELAY.toLong())
            }
        }
        return true
    }

    interface OnDragHandlerListener {
        fun onStartDragHandler()
        fun onEndDragHandler()
    }

    companion object {
        private const val INVALID = -1
        private const val SCROLL_BAR_FADE_DURATION = 500
        private const val SCROLL_BAR_DELAY = 1000
        private const val MIN_HANDLER_HEIGHT_DP = 32
    }
}
