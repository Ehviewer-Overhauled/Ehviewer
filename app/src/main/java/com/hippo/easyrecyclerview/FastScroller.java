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

package com.hippo.easyrecyclerview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.yorozuya.AnimationUtils;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.SimpleAnimatorListener;
import com.hippo.yorozuya.SimpleHandler;

public class FastScroller extends View {

    private static final int INVALID = -1;

    private static final int SCROLL_BAR_FADE_DURATION = 500;
    private static final int SCROLL_BAR_DELAY = 1000;

    private static final int MIN_HANDLER_HEIGHT_DP = 32;

    private Handler mSimpleHandler;

    private boolean mDraggable;

    private int mMinHandlerHeight;

    private RecyclerView mRecyclerView;
    private RecyclerView.OnScrollListener mOnScrollChangeListener;
    private RecyclerView.Adapter<?> mAdapter;
    private RecyclerView.AdapterDataObserver mAdapterDataObserver;

    private Drawable mHandler;
    private int mHandlerOffset = INVALID;
    private int mHandlerHeight = INVALID;

    private float mDownX = INVALID;
    private float mDownY = INVALID;

    private float mLastMotionY = INVALID;

    private boolean mDragged = false;

    private boolean mCantDrag = false;

    private int mTouchSlop;

    private OnDragHandlerListener mListener;

    private ObjectAnimator mShowAnimator;
    private ObjectAnimator mHideAnimator;

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mHideAnimator.start();
        }
    };

    public FastScroller(Context context) {
        super(context);
        init(context, null, 0);
    }

    public FastScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public FastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mSimpleHandler = SimpleHandler.getInstance();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FastScroller, defStyleAttr, 0);
        mHandler = a.getDrawable(R.styleable.FastScroller_handler);
        mDraggable = a.getBoolean(R.styleable.FastScroller_draggable, true);
        a.recycle();

        setAlpha(0.0f);
        setVisibility(INVISIBLE);

        mMinHandlerHeight = LayoutUtils.dp2pix(context, MIN_HANDLER_HEIGHT_DP);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mShowAnimator = ObjectAnimator.ofFloat(this, "alpha", 1.0f);
        mShowAnimator.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
        mShowAnimator.setDuration(SCROLL_BAR_FADE_DURATION);

        mHideAnimator = ObjectAnimator.ofFloat(this, "alpha", 0.0f);
        mHideAnimator.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
        mHideAnimator.setDuration(SCROLL_BAR_FADE_DURATION);
        mHideAnimator.addListener(new SimpleAnimatorListener() {

            private boolean mCancel;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCancel = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mCancel) {
                    mCancel = false;
                } else {
                    setVisibility(INVISIBLE);
                }
            }
        });
    }

    public void setOnDragHandlerListener(OnDragHandlerListener listener) {
        mListener = listener;
    }

    private void updatePosition(boolean show) {
        if (mRecyclerView == null) {
            return;
        }

        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int height = getHeight() - paddingTop - paddingBottom;
        int offset = mRecyclerView.computeVerticalScrollOffset();
        int extent = mRecyclerView.computeVerticalScrollExtent();
        int range = mRecyclerView.computeVerticalScrollRange();

        if (height <= 0 || extent >= range || extent <= 0) {
            return;
        }

        int endOffset = height * offset / range;
        int endHeight = height * extent / range;

        endHeight = Math.max(endHeight, mMinHandlerHeight);
        endOffset = Math.min(endOffset, height - endHeight);

        mHandlerOffset = endOffset + paddingTop;
        mHandlerHeight = endHeight;

        if (show) {
            if (mHideAnimator.isRunning()) {
                mHideAnimator.cancel();
                mShowAnimator.start();
            } else if (getVisibility() != VISIBLE && !mShowAnimator.isRunning()) {
                setVisibility(VISIBLE);
                mShowAnimator.start();
            }

            Handler handler = mSimpleHandler;
            handler.removeCallbacks(mHideRunnable);

            if (!mDragged) {
                handler.postDelayed(mHideRunnable, SCROLL_BAR_DELAY);
            }
        }
    }

    public void setHandlerDrawable(Drawable drawable) {
        mHandler = drawable;
        invalidate();
    }

    public boolean isDraggable() {
        return mDraggable;
    }

    public void setDraggable(boolean draggable) {
        mDraggable = draggable;
        if (mDragged) {
            mDragged = false;
        }
        mSimpleHandler.removeCallbacks(mHideRunnable);
        mHideRunnable.run();
    }

    public boolean isAttached() {
        return mRecyclerView != null;
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        if (recyclerView == null) {
            return;
        }

        if (mRecyclerView != null) {
            throw new IllegalStateException("The FastScroller is already attached to a RecyclerView, " +
                    "call detachedFromRecyclerView first");
        }

        mRecyclerView = recyclerView;
        mOnScrollChangeListener = new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updatePosition(true);
                invalidate();
            }
        };

        recyclerView.addOnScrollListener(mOnScrollChangeListener);

        mAdapter = recyclerView.getAdapter();
        if (mAdapter != null) {
            mAdapterDataObserver = new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    super.onItemRangeChanged(positionStart, itemCount);
                    updatePosition(false);
                    invalidate();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                    super.onItemRangeChanged(positionStart, itemCount, payload);
                    updatePosition(false);
                    invalidate();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    super.onItemRangeInserted(positionStart, itemCount);
                    updatePosition(false);
                    invalidate();
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    super.onItemRangeRemoved(positionStart, itemCount);
                    updatePosition(false);
                    invalidate();
                }

                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                    updatePosition(false);
                    invalidate();
                }
            };
            mAdapter.registerAdapterDataObserver(mAdapterDataObserver);
        }
    }

    public void detachedFromRecyclerView() {
        if (mRecyclerView != null && mOnScrollChangeListener != null) {
            mRecyclerView.removeOnScrollListener(mOnScrollChangeListener);
        }
        mRecyclerView = null;
        mOnScrollChangeListener = null;

        if (mAdapter != null && mAdapterDataObserver != null) {
            mAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
        }
        mAdapter = null;
        mAdapterDataObserver = null;

        setAlpha(0.0f);
        setVisibility(INVISIBLE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mRecyclerView == null || mHandler == null) {
            return;
        }
        if (mHandlerHeight == INVALID) {
            updatePosition(false);
        }
        if (mHandlerHeight == INVALID) {
            return;
        }

        int paddingLeft = getPaddingLeft();
        int saved = canvas.save();
        canvas.translate(paddingLeft, mHandlerOffset);
        mHandler.setBounds(0, 0, getWidth() - paddingLeft - getPaddingRight(), mHandlerHeight);
        mHandler.draw(canvas);
        canvas.restoreToCount(saved);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mDraggable || getVisibility() != VISIBLE || mRecyclerView == null || mHandlerHeight == INVALID) {
            return false;
        }

        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mCantDrag = false;
        }

        if (mCantDrag) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mDragged = false;
                mDownX = event.getX();
                mDownY = event.getY();
                if (mDownY < mHandlerOffset || mDownY > mHandlerOffset + mHandlerHeight) {
                    mCantDrag = true;
                    return false;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!mDragged) {
                    float x = event.getX();
                    float y = event.getY();
                    // Check touch slop
                    if (MathUtils.dist(x, y, mDownX, mDownY) < mTouchSlop) {
                        return true;
                    }
                    if (Math.abs(x - mDownX) > Math.abs(y - mDownY) ||
                            y < mHandlerOffset || y > mHandlerOffset + mHandlerHeight) {
                        mCantDrag = true;
                        return false;
                    } else {
                        mDragged = true;
                        mSimpleHandler.removeCallbacks(mHideRunnable);
                        // Update mLastMotionY
                        if (mDownY < mHandlerOffset || mDownY >= mHandlerOffset + mHandlerHeight) {
                            // the point out of handler, make the point in handler center
                            mLastMotionY = mHandlerOffset + mHandlerHeight / 2;
                        } else {
                            mLastMotionY = mDownY;
                        }
                        // Notify
                        if (mListener != null) {
                            mListener.onStartDragHandler();
                        }
                    }
                }

                int range = mRecyclerView.computeVerticalScrollRange();
                if (range <= 0) {
                    break;
                }
                float y = event.getY();
                int scroll = (int) (range * (y - mLastMotionY) / (getHeight() - getPaddingTop() - getPaddingBottom()));
                mRecyclerView.scrollBy(0, scroll);
                mLastMotionY = y;
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Notify
                if (mDragged && mListener != null) {
                    mListener.onEndDragHandler();
                }
                mDragged = false;
                mSimpleHandler.postDelayed(mHideRunnable, SCROLL_BAR_DELAY);
                break;
        }

        return true;
    }

    public interface OnDragHandlerListener {
        void onStartDragHandler();

        void onEndDragHandler();
    }
}
