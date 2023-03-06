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
package com.hippo.drawable

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import coil.Coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.hippo.widget.ObservedTextView

class UnikeryDrawable(private val mTextView: ObservedTextView) : WrapDrawable(),
    ObservedTextView.OnWindowAttachListener {
    private var mUrl: String? = null
    private var task: Disposable? = null

    init {
        mTextView.setOnWindowAttachListener(this)
    }

    override fun onAttachedToWindow() {
        load(mUrl)
    }

    override fun onDetachedFromWindow() {
        if (task != null && !task!!.isDisposed) task!!.dispose()
        clearDrawable()
    }

    fun load(url: String?) {
        if (url != null) {
            mUrl = url
            val imageLoader = imageLoader(mTextView.context)
            val request = ImageRequest.Builder(mTextView.context)
                .data(url)
                .memoryCacheKey(url)
                .diskCacheKey(url)
                .target(onSuccess = { onGetValue(it) })
                .build()
            task = imageLoader.enqueue(request)
        }
    }

    private fun clearDrawable() {
        drawable = null
    }

    override fun setDrawable(drawable: Drawable?) {
        // Remove old callback
        val oldDrawable = getDrawable()
        if (oldDrawable != null) {
            oldDrawable.callback = null
        }
        super.setDrawable(drawable)
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
        setDrawable(drawable)
        if (drawable is AnimatedImageDrawable) {
            drawable.start()
        }
    }
}