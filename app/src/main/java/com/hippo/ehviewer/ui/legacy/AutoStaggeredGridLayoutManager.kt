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
package com.hippo.ehviewer.ui.legacy

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.hippo.ehviewer.Settings
import kotlin.math.roundToInt

class AutoStaggeredGridLayoutManager(columnSize: Int, orientation: Int) :
    StaggeredGridLayoutManager(1, orientation) {
    private var mColumnSize = columnSize
    private var mColumnSizeChanged = true
    private var mStrategy = 0
    var supportsPredictiveItemAnimations = true

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

    override fun supportsPredictiveItemAnimations(): Boolean {
        return supportsPredictiveItemAnimations && super.supportsPredictiveItemAnimations()
    }

    override fun onMeasure(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        widthSpec: Int,
        heightSpec: Int,
    ) {
        if (mColumnSizeChanged && mColumnSize > 0) {
            val totalSpace = if (orientation == VERTICAL) {
                check(
                    View.MeasureSpec.EXACTLY == View.MeasureSpec.getMode(
                        widthSpec,
                    ),
                ) { "RecyclerView need a fixed width for AutoStaggeredGridLayoutManager" }
                View.MeasureSpec.getSize(widthSpec) - paddingRight - paddingLeft
            } else {
                check(
                    View.MeasureSpec.EXACTLY == View.MeasureSpec.getMode(
                        heightSpec,
                    ),
                ) { "RecyclerView need a fixed height for AutoStaggeredGridLayoutManager" }
                View.MeasureSpec.getSize(heightSpec) - paddingTop - paddingBottom
            }
            val spanCount = when (mStrategy) {
                STRATEGY_MIN_SIZE -> getSpanCountForMinSize(
                    totalSpace,
                    mColumnSize,
                )

                STRATEGY_SUITABLE_SIZE -> getSpanCountForSuitableSize(
                    totalSpace,
                    mColumnSize,
                )

                else -> getSpanCountForMinSize(
                    totalSpace,
                    mColumnSize,
                )
            }
            setSpanCount(spanCount)
            mColumnSizeChanged = false
        }
        super.onMeasure(recycler, state, widthSpec, heightSpec)
    }
}

const val STRATEGY_MIN_SIZE = 0
const val STRATEGY_SUITABLE_SIZE = 1
fun getSpanCountForSuitableSize(total: Int, single: Int): Int {
    return (total / single.toFloat()).roundToInt().coerceAtLeast(1)
}

@Composable
fun calculateSuitableSpanCount(): Int {
    val totalSpace = LocalConfiguration.current.screenWidthDp
    return getSpanCountForSuitableSize(totalSpace, Settings.thumbSizeDp)
}

fun getSpanCountForMinSize(total: Int, single: Int): Int {
    return (total / single).coerceAtLeast(1)
}
