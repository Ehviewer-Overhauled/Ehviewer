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

package com.hippo.easyrecyclerview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Checkable;

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.BridgeRecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.hippo.ehviewer.Settings;
import com.hippo.yorozuya.NumberUtils;

/**
 * Add setChoiceMode for RecyclerView
 */
// Get some code from twoway-view and AbsListView.
public class EasyRecyclerView extends BridgeRecyclerView {

    /**
     * Represents an invalid position. All valid positions are in the range 0 to 1 less than the
     * number of items in the current adapter.
     */
    public static final int INVALID_POSITION = -1;

    /**
     * Normal list that does not indicate choices
     */
    public static final int CHOICE_MODE_NONE = 0;

    /**
     * The list allows up to one choice
     */
    public static final int CHOICE_MODE_SINGLE = 1;

    /**
     * The list allows multiple choices
     */
    public static final int CHOICE_MODE_MULTIPLE = 2;

    /**
     * The list allows multiple choices in a modal selection mode
     */
    public static final int CHOICE_MODE_MULTIPLE_MODAL = 3;

    /**
     * The list allows multiple choices in custom action
     */
    public static final int CHOICE_MODE_MULTIPLE_CUSTOM = 4;
    private static final float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
    // Fling friction
    private final float mFlingFriction = ViewConfiguration.getScrollFriction();
    public float pageScrollThreshold;
    double flingMoveDistanceX;
    double flingMoveDistanceY;
    boolean canScrollVertical = true;
    boolean canScrollHorizontally = true;
    /**
     * Wrapper for the multiple choice mode callback; AbsListView needs to perform
     * a few extra actions around what application code does.
     */
    MultiChoiceModeWrapper mMultiChoiceModeCallback;
    /**
     * Controls if/how the user may choose/check items in the list
     */
    private int mChoiceMode = CHOICE_MODE_NONE;
    /**
     * Controls CHOICE_MODE_MULTIPLE_MODAL. null when inactive.
     */
    private ActionMode mChoiceActionMode;
    /**
     * Listener for custom multiple choices
     */
    private CustomChoiceListener mCustomChoiceListener;

    private boolean mCustomChoice = false;

    /**
     * A lock, avoid OutOfCustomChoiceMode when doing OutOfCustomChoiceMode
     */
    private boolean mOutOfCustomChoiceModing = false;

    private SparseBooleanArray mTempCheckStates;

    /**
     * Running count of how many items are currently checked
     */
    private int mCheckedItemCount;

    /**
     * Running state of which positions are currently checked
     */
    private SparseBooleanArray mCheckStates;

    /**
     * Running state of which IDs are currently checked.
     * If there is a value for a given key, the checked state for that ID is true
     * and the value holds the last known position in the adapter for that id.
     */
    private LongSparseArray<Integer> mCheckedIdStates;

    private Adapter<?> mAdapter;
    private boolean isScrollBefore = false;
    private int initialTouchX;
    private int initialTouchY;
    // A context-specific coefficient adjusted to physical values.
    private float mPhysicalCoeff;
    private int defaultOverflowItemScrollKeep;
    private boolean enableScroll = false;

    public EasyRecyclerView(Context context) {
        super(context);
        EasyRecyclerView.this.init(context);
        setEnableScroll(!Settings.getEInkMode());
    }

    public EasyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        EasyRecyclerView.this.init(context);
        setEnableScroll(!Settings.getEInkMode());
    }

    public EasyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        EasyRecyclerView.this.init(context);
        setEnableScroll(!Settings.getEInkMode());
    }

    public static void setViewChecked(View view, boolean checked) {
        if (view instanceof Checkable) {
            ((Checkable) view).setChecked(checked);
        } else {
            view.setActivated(checked);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {

        super.setAdapter(adapter);

        mAdapter = adapter;
        if (adapter != null) {
            if (mChoiceMode != CHOICE_MODE_NONE && adapter.hasStableIds() &&
                    mCheckedIdStates == null) {
                mCheckedIdStates = new LongSparseArray<>();
            }
        }

        if (mCheckStates != null) {
            mCheckStates.clear();
        }

        if (mCheckedIdStates != null) {
            mCheckedIdStates.clear();
        }
    }

    @Override
    public void onChildAttachedToWindow(@NonNull View child) {
        super.onChildAttachedToWindow(child);

        if (mCheckStates != null) {
            int position = getChildAdapterPosition(child);
            if (position >= 0) {
                setViewChecked(child, mCheckStates.get(position));
            }
        }
    }

    /**
     * Returns the number of items currently selected. This will only be valid
     * if the choice mode is not {@link #CHOICE_MODE_NONE} (default).
     *
     * <p>To determine the specific items that are currently selected, use one of
     * the <code>getChecked*</code> methods.
     *
     * @return The number of items currently selected
     * @see #getCheckedItemPosition()
     * @see #getCheckedItemPositions()
     * @see #getCheckedItemIds()
     */
    public int getCheckedItemCount() {
        return mCheckedItemCount;
    }

    /**
     * Returns the checked state of the specified position. The result is only
     * valid if the choice mode has been set to {@link #CHOICE_MODE_SINGLE}
     * or {@link #CHOICE_MODE_MULTIPLE}.
     *
     * @param position The item whose checked state to return
     * @return The item's checked state or <code>false</code> if choice mode
     * is invalid
     * @see #setChoiceMode(int)
     */
    public boolean isItemChecked(int position) {
        //noinspection SimplifiableIfStatement
        if (mChoiceMode != CHOICE_MODE_NONE && mCheckStates != null) {
            return mCheckStates.get(position);
        }

        return false;
    }

    /**
     * Returns the currently checked item. The result is only valid if the choice
     * mode has been set to {@link #CHOICE_MODE_SINGLE}.
     *
     * @return The position of the currently checked item or
     * {@link #INVALID_POSITION} if nothing is selected
     * @see #setChoiceMode(int)
     */
    public int getCheckedItemPosition() {
        if (mChoiceMode == CHOICE_MODE_SINGLE && mCheckStates != null && mCheckStates.size() == 1) {
            return mCheckStates.keyAt(0);
        }

        return INVALID_POSITION;
    }

    /**
     * Returns the set of checked items in the list. The result is only valid if
     * the choice mode has not been set to {@link #CHOICE_MODE_NONE}.
     *
     * @return A SparseBooleanArray which will return true for each call to
     * get(int position) where position is a checked position in the
     * list and false otherwise, or <code>null</code> if the choice
     * mode is set to {@link #CHOICE_MODE_NONE}.
     */
    public SparseBooleanArray getCheckedItemPositions() {
        if (mChoiceMode != CHOICE_MODE_NONE) {
            return mCheckStates;
        }
        return null;
    }

    /**
     * Returns the set of checked items ids. The result is only valid if the
     * choice mode has not been set to {@link #CHOICE_MODE_NONE} and the adapter
     * has stable IDs. ({@link android.widget.ListAdapter#hasStableIds()} == {@code true})
     *
     * @return A new array which contains the id of each checked item in the
     * list.
     */
    public long[] getCheckedItemIds() {
        if (mChoiceMode == CHOICE_MODE_NONE || mCheckedIdStates == null || mAdapter == null) {
            return new long[0];
        }

        final LongSparseArray<Integer> idStates = mCheckedIdStates;
        final int count = idStates.size();
        final long[] ids = new long[count];

        for (int i = 0; i < count; i++) {
            ids[i] = idStates.keyAt(i);
        }

        return ids;
    }

    public boolean isInCustomChoice() {
        return mCustomChoice;
    }

    public void intoCustomChoiceMode() {
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM && !mCustomChoice) {
            mCustomChoice = true;

            mCustomChoiceListener.onIntoCustomChoice(this);
        }
    }

    public void outOfCustomChoiceMode() {
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM && mCustomChoice && !mOutOfCustomChoiceModing) {
            mOutOfCustomChoiceModing = true;

            // Copy mCheckStates
            mTempCheckStates.clear();
            for (int i = 0, n = mCheckStates.size(); i < n; i++) {
                mTempCheckStates.put(mCheckStates.keyAt(i), mCheckStates.valueAt(i));
            }
            // Uncheck remain checked items
            for (int i = 0, n = mTempCheckStates.size(); i < n; i++) {
                if (mTempCheckStates.valueAt(i)) {
                    setItemChecked(mTempCheckStates.keyAt(i), false);
                }
            }

            mCustomChoice = false;
            mCustomChoiceListener.onOutOfCustomChoice(this);

            mOutOfCustomChoiceModing = false;
        }
    }

    /**
     * Clear any choices previously set
     */
    private void clearChoices() {
        if (mCheckStates != null) {
            mCheckStates.clear();
        }
        if (mCheckedIdStates != null) {
            mCheckedIdStates.clear();
        }
        mCheckedItemCount = 0;
        updateOnScreenCheckedViews();
    }

    /**
     * Sets all items checked.
     */
    public void checkAll() {
        if (mChoiceMode == CHOICE_MODE_NONE || mChoiceMode == CHOICE_MODE_SINGLE) {
            return;
        }

        // Check is intoCheckMode
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM && !mCustomChoice) {
            throw new IllegalStateException("Call intoCheckMode first");
        }

        // Start selection mode if needed. We don't need to if we're unchecking something.
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode == null) {
            if (mMultiChoiceModeCallback == null ||
                    !mMultiChoiceModeCallback.hasWrappedCallback()) {
                throw new IllegalStateException("EasyRecyclerView: attempted to start selection mode " +
                        "for CHOICE_MODE_MULTIPLE_MODAL but no choice mode callback was " +
                        "supplied. Call setMultiChoiceModeListener to set a callback.");
            }
            mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
        }

        for (int i = 0, n = mAdapter.getItemCount(); i < n; i++) {
            boolean oldValue = mCheckStates.get(i);
            mCheckStates.put(i, true);
            if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
                mCheckedIdStates.put(mAdapter.getItemId(i), i);
            }
            if (!oldValue) {
                mCheckedItemCount++;
            }
            if (mChoiceActionMode != null) {
                final long id = mAdapter.getItemId(i);
                mMultiChoiceModeCallback.onItemCheckedStateChanged(mChoiceActionMode, i, id, true);
            }
            if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM) {
                final long id = mAdapter.getItemId(i);
                mCustomChoiceListener.onItemCheckedStateChanged(this, i, id, true);
            }
        }

        updateOnScreenCheckedViews();
    }

    public void toggleItemChecked(int position) {
        if (mCheckStates != null) {
            setItemChecked(position, !mCheckStates.get(position));
        }
    }

    /**
     * Sets the checked state of the specified position. The is only valid if
     * the choice mode has been set to {@link #CHOICE_MODE_SINGLE} or
     * {@link #CHOICE_MODE_MULTIPLE}.
     *
     * @param position The item whose checked state is to be checked
     * @param value    The new checked state for the item
     */
    public void setItemChecked(int position, boolean value) {
        if (mChoiceMode == CHOICE_MODE_NONE) {
            return;
        }

        // Check is intoCheckMode
        if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM && !mCustomChoice) {
            throw new IllegalStateException("Call intoCheckMode first");
        }

        // Start selection mode if needed. We don't need to if we're unchecking something.
        if (value && mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mChoiceActionMode == null) {
            if (mMultiChoiceModeCallback == null ||
                    !mMultiChoiceModeCallback.hasWrappedCallback()) {
                throw new IllegalStateException("EasyRecyclerView: attempted to start selection mode " +
                        "for CHOICE_MODE_MULTIPLE_MODAL but no choice mode callback was " +
                        "supplied. Call setMultiChoiceModeListener to set a callback.");
            }
            mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
        }

        if (mChoiceMode == CHOICE_MODE_MULTIPLE || mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL || mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM) {
            boolean oldValue = mCheckStates.get(position);
            mCheckStates.put(position, value);
            if (mCheckedIdStates != null && mAdapter.hasStableIds()) {
                if (value) {
                    mCheckedIdStates.put(mAdapter.getItemId(position), position);
                } else {
                    mCheckedIdStates.remove(mAdapter.getItemId(position));
                }
            }
            if (oldValue != value) {
                if (value) {
                    mCheckedItemCount++;
                } else {
                    mCheckedItemCount--;
                }
            }
            if (mChoiceActionMode != null) {
                final long id = mAdapter.getItemId(position);
                mMultiChoiceModeCallback.onItemCheckedStateChanged(mChoiceActionMode,
                        position, id, value);
            }
            if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM) {
                final long id = mAdapter.getItemId(position);
                mCustomChoiceListener.onItemCheckedStateChanged(this, position, id, value);
            }
        } else {
            boolean updateIds = mCheckedIdStates != null && mAdapter.hasStableIds();
            // Clear all values if we're checking something, or unchecking the currently
            // selected item
            if (value || isItemChecked(position)) {
                mCheckStates.clear();
                if (updateIds) {
                    mCheckedIdStates.clear();
                }
            }
            // this may end up selecting the value we just cleared but this way
            // we ensure length of mCheckStates is 1, a fact getCheckedItemPosition relies on
            if (value) {
                mCheckStates.put(position, true);
                if (updateIds) {
                    mCheckedIdStates.put(mAdapter.getItemId(position), position);
                }
                mCheckedItemCount = 1;
            } else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0)) {
                mCheckedItemCount = 0;
            }
        }

        updateOnScreenCheckedViews();
    }

    /**
     * @return The current choice mode
     * @see #setChoiceMode(int)
     */
    public int getChoiceMode() {
        return mChoiceMode;
    }

    /**
     * Defines the choice behavior for the List. By default, Lists do not have any choice behavior
     * ({@link #CHOICE_MODE_NONE}). By setting the choiceMode to {@link #CHOICE_MODE_SINGLE}, the
     * List allows up to one item to  be in a chosen state. By setting the choiceMode to
     * {@link #CHOICE_MODE_MULTIPLE}, the list allows any number of items to be chosen.
     *
     * @param choiceMode One of {@link #CHOICE_MODE_NONE}, {@link #CHOICE_MODE_SINGLE}, or
     *                   {@link #CHOICE_MODE_MULTIPLE}
     */
    public void setChoiceMode(int choiceMode) {
        mChoiceMode = choiceMode;
        if (mChoiceActionMode != null) {
            mChoiceActionMode.finish();
            mChoiceActionMode = null;
        }
        if (mChoiceMode != CHOICE_MODE_NONE) {
            if (mCheckStates == null) {
                mCheckStates = new SparseBooleanArray(0);
            }
            if (mCheckedIdStates == null && mAdapter != null && mAdapter.hasStableIds()) {
                mCheckedIdStates = new LongSparseArray<>(0);
            }
            // Modal multi-choice mode only has choices when the mode is active. Clear them.
            if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL) {
                clearChoices();
                setLongClickable(true);
            } else if (mChoiceMode == CHOICE_MODE_MULTIPLE_CUSTOM) {
                if (mTempCheckStates == null) {
                    mTempCheckStates = new SparseBooleanArray(0);
                }
                clearChoices();
            }
        }
    }

    /**
     * Set a {@link MultiChoiceModeListener} that will manage the lifecycle of the
     * selection {@link android.view.ActionMode}. Only used when the choice mode is set to
     * {@link #CHOICE_MODE_MULTIPLE_MODAL}.
     *
     * @param listener Listener that will manage the selection mode
     * @see #setChoiceMode(int)
     */
    public void setMultiChoiceModeListener(MultiChoiceModeListener listener) {
        if (mMultiChoiceModeCallback == null) {
            mMultiChoiceModeCallback = new MultiChoiceModeWrapper();
        }
        mMultiChoiceModeCallback.setWrapped(listener);
    }

    /**
     * @param listener
     */
    public void setCustomCheckedListener(CustomChoiceListener listener) {
        mCustomChoiceListener = listener;
    }

    /**
     * Perform a quick, in-place update of the checked or activated state
     * on all visible item views. This should only be called when a valid
     * choice mode is active.
     */
    private void updateOnScreenCheckedViews() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int position = getChildAdapterPosition(child);
            setViewChecked(child, mCheckStates.get(position));
        }
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

    public boolean isEnableScroll() {
        return enableScroll;
    }

    public void setEnableScroll(boolean enableScroll) {
        this.enableScroll = enableScroll;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final SavedState ss = new SavedState(super.onSaveInstanceState());

        ss.choiceMode = mChoiceMode;
        ss.customChoice = mCustomChoice;
        ss.checkedItemCount = mCheckedItemCount;
        ss.checkState = mCheckStates;
        ss.checkIdState = mCheckedIdStates;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setChoiceMode(ss.choiceMode);
        mCustomChoice = ss.customChoice;
        mCheckedItemCount = ss.checkedItemCount;
        if (ss.checkState != null) {
            mCheckStates = ss.checkState;
        }
        if (ss.checkIdState != null) {
            mCheckedIdStates = ss.checkIdState;
        }

        if (mChoiceMode == CHOICE_MODE_MULTIPLE_MODAL && mCheckedItemCount > 0) {
            mChoiceActionMode = startActionMode(mMultiChoiceModeCallback);
        }
        updateOnScreenCheckedViews();
    }

    /**
     * A MultiChoiceModeListener receives events for {@link android.widget.AbsListView#CHOICE_MODE_MULTIPLE_MODAL}.
     * It acts as the {@link android.view.ActionMode.Callback} for the selection mode and also receives
     * {@link #onItemCheckedStateChanged(android.view.ActionMode, int, long, boolean)} events when the user
     * selects and deselects list items.
     */
    public interface MultiChoiceModeListener extends ActionMode.Callback {
        /**
         * Called when an item is checked or unchecked during selection mode.
         *
         * @param mode     The {@link android.view.ActionMode} providing the selection mode
         * @param position Adapter position of the item that was checked or unchecked
         * @param id       Adapter ID of the item that was checked or unchecked
         * @param checked  <code>true</code> if the item is now checked, <code>false</code>
         *                 if the item is now unchecked.
         */
        void onItemCheckedStateChanged(ActionMode mode,
                                       int position, long id, boolean checked);
    }

    /**
     * Custom checked
     */
    public interface CustomChoiceListener {

        void onIntoCustomChoice(EasyRecyclerView view);

        void onOutOfCustomChoice(EasyRecyclerView view);

        void onItemCheckedStateChanged(EasyRecyclerView view, int position, long id, boolean checked);
    }

    /**
     * This saved state class is a Parcelable and should not extend
     * {@link android.view.View.BaseSavedState} nor {@link android.view.AbsSavedState}
     * because its super class AbsSavedState's constructor
     * currently passes null
     * as a class loader to read its superstate from Parcelable.
     * This causes {@link android.os.BadParcelableException} when restoring saved states.
     * <p/>
     * The super class "RecyclerView" is a part of the support library,
     * and restoring its saved state requires the class loader that loaded the RecyclerView.
     * It seems that the class loader is not required when restoring from RecyclerView itself,
     * but it is required when restoring from RecyclerView's subclasses.
     */
    static class SavedState implements Parcelable {

        public static final SavedState EMPTY_STATE = new SavedState() {
        };
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int choiceMode;
        boolean customChoice;
        int checkedItemCount;
        SparseBooleanArray checkState;
        LongSparseArray<Integer> checkIdState;
        // This keeps the parent(RecyclerView)'s state
        Parcelable mSuperState;

        SavedState() {
            mSuperState = null;
        }

        /**
         * Constructor called from {@link #onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            mSuperState = superState != EMPTY_STATE ? superState : null;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            // Parcel 'in' has its parent(RecyclerView)'s saved state.
            // To restore it, class loader that loaded RecyclerView is required.
            Parcelable superState = in.readParcelable(RecyclerView.class.getClassLoader());
            mSuperState = superState != null ? superState : EMPTY_STATE;

            choiceMode = in.readInt();
            customChoice = NumberUtils.int2boolean(in.readInt());
            checkedItemCount = in.readInt();
            checkState = in.readSparseBooleanArray();
            final int N = in.readInt();
            if (N > 0) {
                checkIdState = new LongSparseArray<>();
                for (int i = 0; i < N; i++) {
                    final long key = in.readLong();
                    final int value = in.readInt();
                    checkIdState.put(key, value);
                }
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            out.writeParcelable(mSuperState, flags);

            out.writeInt(choiceMode);
            out.writeInt(NumberUtils.boolean2int(customChoice));
            out.writeInt(checkedItemCount);
            out.writeSparseBooleanArray(checkState);
            final int N = checkIdState != null ? checkIdState.size() : 0;
            out.writeInt(N);
            for (int i = 0; i < N; i++) {
                out.writeLong(checkIdState.keyAt(i));
                out.writeInt(checkIdState.valueAt(i));
            }
        }

        public Parcelable getSuperState() {
            return mSuperState;
        }
    }

    class MultiChoiceModeWrapper implements MultiChoiceModeListener {
        private MultiChoiceModeListener mWrapped;

        public void setWrapped(MultiChoiceModeListener wrapped) {
            mWrapped = wrapped;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean hasWrappedCallback() {
            return mWrapped != null;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mWrapped.onCreateActionMode(mode, menu)) {
                // Initialize checked graphic state?
                setLongClickable(false);
                return true;
            }
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            mChoiceActionMode = null;

            // Ending selection mode means deselecting everything.
            clearChoices();

            requestLayout();

            setLongClickable(true);
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode,
                                              int position, long id, boolean checked) {
            mWrapped.onItemCheckedStateChanged(mode, position, id, checked);

            // If there are no items selected we no longer need the selection mode.
            if (getCheckedItemCount() == 0) {
                mode.finish();
            }
        }
    }
}
