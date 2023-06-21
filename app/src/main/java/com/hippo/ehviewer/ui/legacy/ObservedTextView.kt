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
import androidx.appcompat.widget.AppCompatTextView

open class ObservedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatTextView(context, attrs) {
    private var mOnWindowAttachListener: OnWindowAttachListener? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mOnWindowAttachListener != null) {
            mOnWindowAttachListener!!.onAttachedToWindow()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mOnWindowAttachListener != null) {
            mOnWindowAttachListener!!.onDetachedFromWindow()
        }
    }

    fun setOnWindowAttachListener(onWindowAttachListener: OnWindowAttachListener?) {
        mOnWindowAttachListener = onWindowAttachListener
    }

    interface OnWindowAttachListener {
        fun onAttachedToWindow()
        fun onDetachedFromWindow()
    }
}
