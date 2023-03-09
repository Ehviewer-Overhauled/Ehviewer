/*
 * Copyright 2016 Hippo Seven
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
package com.hippo.widget.recyclerview

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler

class AutoGridLayoutManager(context: Context?, columnSize: Int, private var fakePadding: Int = 0) :
    GridLayoutManager(context, 1) {
    private var mColumnSize = columnSize
    private var mColumnSizeChanged = true
    private var mStrategy = 0

    fun setColumnSize(columnSize: Int) {
        if (columnSize == mColumnSize) {
            return
        }
        mColumnSize = columnSize
        mColumnSizeChanged = true
    }

    fun setStrategy(strategy: Int) {
        if (strategy == mStrategy) {
            return
        }
        mStrategy = strategy
        mColumnSizeChanged = true
    }

    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        if (mColumnSizeChanged && mColumnSize > 0) {
            val totalSpace = if (orientation == RecyclerView.VERTICAL) {
                width - paddingRight - paddingLeft - fakePadding
            } else {
                height - paddingTop - paddingBottom - fakePadding
            }
            val spanCount = when (mStrategy) {
                STRATEGY_MIN_SIZE -> getSpanCountForMinSize(
                    totalSpace,
                    mColumnSize
                )

                STRATEGY_SUITABLE_SIZE -> getSpanCountForSuitableSize(
                    totalSpace,
                    mColumnSize
                )

                else -> getSpanCountForMinSize(totalSpace, mColumnSize)
            }
            setSpanCount(spanCount)
            mColumnSizeChanged = false
        }
        super.onLayoutChildren(recycler, state)
    }
}