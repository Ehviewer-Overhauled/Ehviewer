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

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import com.hippo.ehviewer.R

/**
 * not scrollable
 *
 * @author Hippo
 */
open class SimpleGridLayout : ViewGroup {
    private var mColumnCount = 0
    private var mItemMargin = 0
    private var mRowHeights: IntArray? = null
    private var mItemWidth = 0

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle,
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SimpleGridLayout)
        mColumnCount = a.getInteger(R.styleable.SimpleGridLayout_columnCount, DEFAULT_COLUMN_COUNT)
        mItemMargin = a.getDimensionPixelOffset(R.styleable.SimpleGridLayout_itemMargin, 0)
        a.recycle()
    }

    fun setItemMargin(itemMargin: Int) {
        if (mItemMargin != itemMargin) {
            mItemMargin = itemMargin
            requestLayout()
        }
    }

    fun setColumnCount(columnCount: Int) {
        check(columnCount > 0) { "Column count can't be $columnCount" }
        if (mColumnCount != columnCount) {
            mColumnCount = columnCount
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxRowCount = (childCount + mColumnCount - 1) / mColumnCount
        if (mRowHeights == null || mRowHeights!!.size != maxRowCount) {
            mRowHeights = IntArray(maxRowCount)
        }
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var maxWidth = MeasureSpec.getSize(widthMeasureSpec)
        var maxHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            maxWidth = 300
        }
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            maxHeight = Int.MAX_VALUE and (0x3 shl 30).inv()
        }

        // Get item width MeasureSpec
        mItemWidth =
            ((maxWidth - paddingLeft - paddingRight - (mColumnCount - 1) * mItemMargin) / mColumnCount)
                .coerceAtLeast(1)
        val itemWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mItemWidth, MeasureSpec.EXACTLY)
        val itemHeightMeasureSpec = MeasureSpec.UNSPECIFIED
        val measuredWidth = maxWidth
        var measuredHeight = 0
        var rowHeight = 0
        var row = 0
        val count = childCount
        var index = 0
        var indexInRow = 0
        while (index < count) {
            val child = getChildAt(index)
            if (child.visibility == GONE) {
                indexInRow--
                index++
                indexInRow++
                continue
            }
            child.measure(itemWidthMeasureSpec, itemHeightMeasureSpec)
            if (indexInRow == mColumnCount) {
                // New row
                indexInRow = 0
                rowHeight = 0
                row++
            }
            rowHeight = rowHeight.coerceAtLeast(child.measuredHeight)
            if (indexInRow == mColumnCount - 1 || index == count - 1) {
                mRowHeights!![row] = rowHeight
                measuredHeight += rowHeight + mItemMargin
            }
            index++
            indexInRow++
        }
        measuredHeight -= mItemMargin
        measuredHeight = (measuredHeight + paddingTop + paddingBottom).coerceIn(0, maxHeight)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val itemWidth = mItemWidth
        val itemMargin = mItemMargin
        val paddingLeft = paddingLeft
        var left = paddingLeft
        var top = paddingTop
        var row = 0
        val count = childCount
        var index = 0
        var indexInRow = 0
        while (index < count) {
            val child = getChildAt(index)
            if (child.visibility == GONE) {
                indexInRow--
                index++
                indexInRow++
                continue
            }
            if (indexInRow == mColumnCount) {
                // New row
                left = paddingLeft
                top += mRowHeights!![row] + itemMargin
                indexInRow = 0
                row++
            }
            child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)
            left += itemWidth + itemMargin
            index++
            indexInRow++
        }
    }

    companion object {
        private const val DEFAULT_COLUMN_COUNT = 3
    }
}
