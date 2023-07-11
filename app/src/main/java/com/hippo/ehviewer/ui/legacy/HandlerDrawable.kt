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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

class HandlerDrawable : Drawable() {
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTemp = RectF()
    private var mColor = Color.BLACK

    init {
        mPaint.color = mColor
        mPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()
        if (width > height) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mPaint)
        } else {
            mTemp[0f, 0f, width.toFloat()] = width.toFloat()
            canvas.drawArc(mTemp, -180f, 180f, true, mPaint)
            mTemp[0f, (height - width).toFloat(), width.toFloat()] = height.toFloat()
            canvas.drawArc(mTemp, 0f, 180f, true, mPaint)
            val halfWidth = width.toFloat() / 2.0f
            canvas.drawRect(0f, halfWidth, width.toFloat(), height - halfWidth, mPaint)
        }
    }

    fun setColor(color: Int) {
        if (mColor != color) {
            mColor = color
            mPaint.color = color
        }
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return when (Color.alpha(mColor)) {
            0xff -> {
                PixelFormat.OPAQUE
            }

            0x00 -> {
                PixelFormat.TRANSPARENT
            }

            else -> {
                PixelFormat.TRANSLUCENT
            }
        }
    }
}
