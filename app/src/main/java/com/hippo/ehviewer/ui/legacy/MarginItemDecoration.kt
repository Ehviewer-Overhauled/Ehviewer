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

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class MarginItemDecoration(
    margin: Int,
    paddingLeft: Int,
    paddingTop: Int,
    paddingRight: Int,
    paddingBottom: Int,
) : ItemDecoration() {
    private var mMargin = 0
    private var mPaddingLeft = 0
    private var mPaddingTop = 0
    private var mPaddingRight = 0
    private var mPaddingBottom = 0

    init {
        setMargin(margin, paddingLeft, paddingTop, paddingRight, paddingBottom)
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        outRect[mMargin, mMargin, mMargin] = mMargin
    }

    /**
     * @param margin        gap between two item
     * @param paddingLeft   gap between RecyclerView left and left item left
     * @param paddingTop    gap between RecyclerView top and top item top
     * @param paddingRight  gap between RecyclerView right and right item right
     * @param paddingBottom gap between RecyclerView bottom and bottom item bottom
     */
    fun setMargin(
        margin: Int,
        paddingLeft: Int,
        paddingTop: Int,
        paddingRight: Int,
        paddingBottom: Int,
    ) {
        val halfMargin = margin / 2
        mMargin = halfMargin
        mPaddingLeft = paddingLeft - halfMargin
        mPaddingTop = paddingTop - halfMargin
        mPaddingRight = paddingRight - halfMargin
        mPaddingBottom = paddingBottom - halfMargin
    }

    fun applyPaddings(view: View) {
        view.setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom)
    }
}
