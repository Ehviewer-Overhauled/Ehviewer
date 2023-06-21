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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.ListView
import com.hippo.ehviewer.R

@SuppressLint("CustomViewStyleable")
class IndicatingListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ListView(context, attrs) {
    private val mPaint = Paint()
    private val mTemp = Rect()
    private var mIndicatorHeight = 0

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Indicating)
        mIndicatorHeight = a.getDimensionPixelOffset(R.styleable.Indicating_indicatorHeight, 1)
        mPaint.color = a.getColor(
            R.styleable.Indicating_indicatorColor,
            Color.BLACK,
        )
        mPaint.style = Paint.Style.FILL
        a.recycle()
    }

    private fun fillTopIndicatorDrawRect() {
        mTemp[0, 0, width] = mIndicatorHeight
    }

    private fun fillBottomIndicatorDrawRect() {
        mTemp[0, height - mIndicatorHeight, width] = height
    }

    private fun needShowTopIndicator(): Boolean {
        return canScrollVertically(-1)
    }

    private fun needShowBottomIndicator(): Boolean {
        return canScrollVertically(1)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val restoreCount = canvas.save()
        canvas.translate(scrollX.toFloat(), scrollY.toFloat())

        // Draw top indicator
        if (needShowTopIndicator()) {
            fillTopIndicatorDrawRect()
            canvas.drawRect(mTemp, mPaint)
        }
        // Draw bottom indicator
        if (needShowBottomIndicator()) {
            fillBottomIndicatorDrawRect()
            canvas.drawRect(mTemp, mPaint)
        }
        canvas.restoreToCount(restoreCount)
    }
}
