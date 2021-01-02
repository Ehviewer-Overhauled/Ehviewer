package com.hippo.easyrecyclerview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.BridgeRecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public class ScrollLessRecyclerView extends BridgeRecyclerView {

    private boolean isScrollBefore = false;
    private int initialTouchX;
    private int initialTouchY;

    // A context-specific coefficient adjusted to physical values.
    private float mPhysicalCoeff;
    // Fling friction
    private final float mFlingFriction = ViewConfiguration.getScrollFriction();
    private static final float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));

    public float pageScrollThreshold;
    private int defaultOverflowItemScrollKeep;

    private boolean enableScroll = false;

    public ScrollLessRecyclerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ScrollLessRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ScrollLessRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float scale = context.getResources().getDisplayMetrics().density;
        final float ppi = scale * 160.0f;
        mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f; // look and feel tuning
        pageScrollThreshold = scale * 80;
    }

    public void setPageScrollThreshold(float pageScrollThreshold) {
        this.pageScrollThreshold = pageScrollThreshold;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (enableScroll) {
            return super.onTouchEvent(e);
        }
        boolean superTouchResult = super.onTouchEvent(e);
        if (superTouchResult) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialTouchX = (int) (e.getX() + 0.5f);
                    initialTouchY = (int) (e.getY() + 0.5f);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (initialTouchX < 0 || initialTouchY < 0) {
                        initialTouchX = (int) (e.getX() + 0.5f);
                        initialTouchY = (int) (e.getY() + 0.5f);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (isScrollBefore) {
                        int moveDistanceX = (int) (initialTouchX - e.getX() + flingMoveDistanceX);
                        int moveDistanceY = (int) (initialTouchY - e.getY() + flingMoveDistanceY);
                        onTouchScroll(moveDistanceX, moveDistanceY);
                        initialTouchX = initialTouchY = -1;
                    }
                    break;
            }
            isScrollBefore = getScrollState() == SCROLL_STATE_DRAGGING;
        }
        return superTouchResult;
    }

    @Override
    public boolean scrollByInternal(int x, int y, MotionEvent ev, int type) {
        if (enableScroll) {
            return super.scrollByInternal(x, y, ev, type);
        }
        //disable drag scroll effect，e-ink screen don't need it
        return false;
    }

    double flingMoveDistanceX;
    double flingMoveDistanceY;

    @Override
    public boolean fling(int velocityX, int velocityY) {
        if (enableScroll) {
            return super.fling(velocityX, velocityY);
        }
        flingMoveDistanceX = getSplineFlingDistance(velocityX) * Math.signum(velocityX);
        flingMoveDistanceY = getSplineFlingDistance(velocityY) * Math.signum(velocityY);
        //disable fling scroll effect,e-ink screen don't need it
        return false;
    }

    boolean canScrollVertical = true;
    boolean canScrollHorizontally = true;

    protected void onTouchScroll(int dx, int dy) {
        canScrollVertical = true;
        canScrollHorizontally = true;
        LayoutManager layout = getLayoutManager();
        if (layout != null) {
            if (dy > pageScrollThreshold) {
                if (canScrollVertical && layout.canScrollVertically()) {
                    canScrollVertical = false;
                    onScrollDown();
                } else if (canScrollHorizontally && layout.canScrollHorizontally()) {
                    canScrollHorizontally = false;
                    onScrollRight();
                }
            } else if (dy < -pageScrollThreshold) {
                if (canScrollVertical && layout.canScrollVertically()) {
                    canScrollVertical = false;
                    onScrollUp();
                } else if (canScrollHorizontally && layout.canScrollHorizontally()) {
                    canScrollHorizontally = false;
                    onScrollLeft();
                }
            }

            if (dx > pageScrollThreshold) {
                if (canScrollVertical && layout.canScrollVertically()) {
                    onScrollDown();
                } else if (canScrollHorizontally && layout.canScrollHorizontally()) {
                    onScrollRight();
                }
            } else if (dx < -pageScrollThreshold) {
                if (canScrollVertical && layout.canScrollVertically()) {
                    onScrollUp();
                } else if (canScrollHorizontally && layout.canScrollHorizontally()) {
                    onScrollLeft();
                }
            }
        }
    }

    protected void onScrollLeft() {
        LayoutManager layout = getLayoutManager();
        if (layout instanceof LinearLayoutManager) {
            LinearLayoutManager realLayout = (LinearLayoutManager) layout;
            int firstVisiblePosition = realLayout.findFirstVisibleItemPosition();
            ViewHolder firstVisibleItem = findViewHolderForAdapterPosition(firstVisiblePosition);

            if (firstVisibleItem == null) {
                return;
            }

            //when the item width overscreen, findLastCompletelyVisibleItemPosition always return NO_POSITION
            if (firstVisibleItem.itemView.getMeasuredWidth() > getMeasuredWidth()) {
                super.scrollByInternal(-getMeasuredWidth() + defaultOverflowItemScrollKeep, 0, null, ViewCompat.TYPE_TOUCH);
            } else if (firstVisibleItem.itemView.getMeasuredWidth() == getMeasuredWidth()) {
                super.scrollByInternal(-getMeasuredWidth(), 0, null, ViewCompat.TYPE_TOUCH);
            } else {
                super.scrollByInternal(firstVisibleItem.itemView.getRight() - getMeasuredWidth(), 0, null, ViewCompat.TYPE_TOUCH);
            }
        } else if (layout instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager realLayout = (StaggeredGridLayoutManager) layout;
            int[] firstVisiblePosition = realLayout.findFirstVisibleItemPositions(null);
            ViewHolder[] firstVisibleItems = new ViewHolder[firstVisiblePosition.length];
            for (int i = 0; i < firstVisiblePosition.length; i++) {
                firstVisibleItems[i] = findViewHolderForAdapterPosition(firstVisiblePosition[i]);
            }
            int scrollDistance = Integer.MAX_VALUE;
            for (ViewHolder firstVisibleItem : firstVisibleItems) {
                if (firstVisibleItem != null) {
                    if (firstVisibleItem.itemView.getRight() < scrollDistance) {
                        scrollDistance = firstVisibleItem.itemView.getRight();
                    }
                }
            }

            if (scrollDistance == getMeasuredWidth()) {
                scrollDistance = 0;
            }
            super.scrollByInternal(scrollDistance - getMeasuredWidth(), 0, null, ViewCompat.TYPE_TOUCH);
        }
    }

    protected void onScrollRight() {
        LayoutManager layout = getLayoutManager();
        if (layout instanceof LinearLayoutManager) {
            LinearLayoutManager realLayout = (LinearLayoutManager) layout;
            int lastVisiblePosition = realLayout.findLastVisibleItemPosition();
            ViewHolder lastVisibleItem = findViewHolderForAdapterPosition(lastVisiblePosition);

            if (lastVisibleItem == null) {
                return;
            }

            //when the item width overflow screen, findLastCompletelyVisibleItemPosition always return NO_POSITION
            if (lastVisibleItem.itemView.getMeasuredWidth() > getMeasuredWidth()) {
                super.scrollByInternal(getMeasuredWidth() - defaultOverflowItemScrollKeep, 0, null, ViewCompat.TYPE_TOUCH);
            } else if (lastVisibleItem.itemView.getMeasuredWidth() == getMeasuredWidth()) {
                super.scrollByInternal(getMeasuredWidth(), 0, null, ViewCompat.TYPE_TOUCH);
            } else {
                super.scrollByInternal(lastVisibleItem.itemView.getLeft(), 0, null, ViewCompat.TYPE_TOUCH);
            }
        } else if (layout instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager realLayout = (StaggeredGridLayoutManager) layout;
            int[] lastVisiblePosition = realLayout.findLastVisibleItemPositions(null);
            ViewHolder[] lastVisibleItems = new ViewHolder[lastVisiblePosition.length];
            for (int i = 0; i < lastVisiblePosition.length; i++) {
                lastVisibleItems[i] = findViewHolderForAdapterPosition(lastVisiblePosition[i]);
            }
            int scrollDistance = 0;
            for (ViewHolder lastVisibleItem : lastVisibleItems) {
                if (lastVisibleItem != null) {
                    if (lastVisibleItem.itemView.getLeft() > scrollDistance) {
                        scrollDistance = lastVisibleItem.itemView.getLeft();
                    }
                }
            }
            if (scrollDistance == 0) {
                scrollDistance = getMeasuredWidth();
            }
            super.scrollByInternal(scrollDistance, 0, null, ViewCompat.TYPE_TOUCH);
        }
    }

    protected void onScrollUp() {
        LayoutManager layout = getLayoutManager();
        if (layout instanceof LinearLayoutManager) {
            LinearLayoutManager realLayout = (LinearLayoutManager) layout;
            int firstVisiblePosition = realLayout.findFirstVisibleItemPosition();
            ViewHolder firstVisibleItem = findViewHolderForAdapterPosition(firstVisiblePosition);

            if (firstVisibleItem == null) {
                return;
            }

            //when the item height overscreen, findLastCompletelyVisibleItemPosition always return NO_POSITION
            if (firstVisibleItem.itemView.getMeasuredHeight() > getMeasuredHeight()) {
                super.scrollByInternal(0, -getMeasuredHeight() + defaultOverflowItemScrollKeep, null, ViewCompat.TYPE_TOUCH);
            } else if (firstVisibleItem.itemView.getMeasuredHeight() == getMeasuredHeight()) {
                super.scrollByInternal(0, -getMeasuredHeight(), null, ViewCompat.TYPE_TOUCH);
            } else {
                super.scrollByInternal(0, firstVisibleItem.itemView.getBottom() - getMeasuredHeight(), null, ViewCompat.TYPE_TOUCH);
            }
        } else if (layout instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager realLayout = (StaggeredGridLayoutManager) layout;
            int[] firstVisiblePosition = realLayout.findFirstVisibleItemPositions(null);
            ViewHolder[] firstVisibleItems = new ViewHolder[firstVisiblePosition.length];
            for (int i = 0; i < firstVisiblePosition.length; i++) {
                firstVisibleItems[i] = findViewHolderForAdapterPosition(firstVisiblePosition[i]);
            }
            int scrollDistance = Integer.MAX_VALUE;
            for (ViewHolder firstVisibleItem : firstVisibleItems) {
                if (firstVisibleItem != null) {
                    if (firstVisibleItem.itemView.getBottom() < scrollDistance) {
                        scrollDistance = firstVisibleItem.itemView.getBaseline();
                    }
                }
            }
            if (scrollDistance == getMeasuredWidth()) {
                scrollDistance = 0;
            }
            super.scrollByInternal(0, scrollDistance - getMeasuredHeight(), null, ViewCompat.TYPE_TOUCH);
        }
    }

    protected void onScrollDown() {
        LayoutManager layout = getLayoutManager();
        if (layout instanceof LinearLayoutManager) {
            LinearLayoutManager realLayout = (LinearLayoutManager) layout;
            int lastVisiblePosition = realLayout.findLastVisibleItemPosition();
            ViewHolder lastVisibleItem = findViewHolderForAdapterPosition(lastVisiblePosition);

            if (lastVisibleItem == null) {
                return;
            }

            //when the item height overflow screen, findLastCompletelyVisibleItemPosition always return NO_POSITION
            if (lastVisibleItem.itemView.getMeasuredHeight() > getMeasuredHeight()) {
                super.scrollByInternal(0, getMeasuredHeight() - defaultOverflowItemScrollKeep, null, ViewCompat.TYPE_TOUCH);
            } else if (lastVisibleItem.itemView.getMeasuredHeight() == getMeasuredHeight()) {
                super.scrollByInternal(0, getMeasuredHeight(), null, ViewCompat.TYPE_TOUCH);
            } else {
                super.scrollByInternal(0, lastVisibleItem.itemView.getTop(), null, ViewCompat.TYPE_TOUCH);
            }
        } else if (layout instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager realLayout = (StaggeredGridLayoutManager) layout;
            int[] lastVisiblePosition = realLayout.findLastVisibleItemPositions(null);
            ViewHolder[] lastVisibleItems = new ViewHolder[lastVisiblePosition.length];
            for (int i = 0; i < lastVisiblePosition.length; i++) {
                lastVisibleItems[i] = findViewHolderForAdapterPosition(lastVisiblePosition[i]);
            }
            int scrollDistance = 0;
            for (ViewHolder lastVisibleItem : lastVisibleItems) {
                if (lastVisibleItem != null) {
                    if (lastVisibleItem.itemView.getTop() > scrollDistance) {
                        scrollDistance = lastVisibleItem.itemView.getTop();
                    }
                }
            }
            if (scrollDistance == 0) {
                scrollDistance = getMeasuredHeight();
            }
            super.scrollByInternal(0, scrollDistance, null, ViewCompat.TYPE_TOUCH);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        defaultOverflowItemScrollKeep = (int) (h * 0.05f);
    }

    private double getSplineDeceleration(int velocity) {
        return Math.log(0.35f * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }

    private double getSplineFlingDistance(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l);
    }

    public void setEnableScroll(boolean enableScroll) {
        this.enableScroll = enableScroll;
    }

    public boolean isEnableScroll() {
        return enableScroll;
    }
}