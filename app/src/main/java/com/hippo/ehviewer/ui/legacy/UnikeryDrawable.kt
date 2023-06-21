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

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import coil.imageLoader
import coil.request.ImageRequest
import com.hippo.ehviewer.coil.imageRequest

class UnikeryDrawable(private val mTextView: ObservedTextView, url: String) :
    WrapDrawable(),
    ObservedTextView.OnWindowAttachListener {
    private val mImageRequest: ImageRequest

    init {
        mTextView.setOnWindowAttachListener(this)
        mImageRequest = mTextView.context.imageRequest {
            target(onSuccess = { onGetValue(it) })
            data(url)
        }
        load()
    }

    override fun onAttachedToWindow() {
        drawable ?: load()
    }

    override fun onDetachedFromWindow() {
        clearDrawable()
    }

    fun load() {
        mTextView.context.imageLoader.enqueue(mImageRequest)
    }

    private fun clearDrawable() {
        drawable = null
    }

    override var drawable: Drawable?
        get() = super.drawable
        set(drawable) {
            // Remove old callback
            val oldDrawable = drawable
            if (oldDrawable != null) {
                oldDrawable.callback = null
            }
            super.drawable = drawable
            if (drawable != null) {
                drawable.callback = mTextView
            }
            updateBounds()
            if (drawable != null) {
                invalidateSelf()
            }
        }

    override fun invalidateSelf() {
        val cs = mTextView.text
        mTextView.text = cs
    }

    private fun onGetValue(drawable: Drawable) {
        clearDrawable()
        this.drawable = drawable
        if (drawable is AnimatedImageDrawable) {
            drawable.start()
        }
    }
}
