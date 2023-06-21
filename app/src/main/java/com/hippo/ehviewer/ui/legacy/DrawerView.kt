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

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class DrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private var mMaxWidth = 0

    init {
        val a = context.obtainStyledAttributes(attrs, SIZE_ATTRS)
        mMaxWidth = a.getDimensionPixelOffset(0, 0)
        a.recycle()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        var widthSpec = widthSpec
        when (MeasureSpec.getMode(widthSpec)) {
            MeasureSpec.EXACTLY -> {}
            MeasureSpec.AT_MOST -> widthSpec = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthSpec).coerceAtMost(mMaxWidth),
                MeasureSpec.EXACTLY,
            )

            MeasureSpec.UNSPECIFIED ->
                widthSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, MeasureSpec.EXACTLY)
        }
        // Let super sort out the height
        super.onMeasure(widthSpec, heightSpec)
    }

    companion object {
        private val SIZE_ATTRS = intArrayOf(android.R.attr.maxWidth)
    }
}
