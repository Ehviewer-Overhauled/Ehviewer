/*
 * Copyright 2015-2016 Hippo Seven
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

package com.hippo.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.IntDef
import androidx.appcompat.content.res.AppCompatResources
import coil.load
import coil.size.Size
import com.hippo.drawable.PreciselyClipDrawable
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
import com.hippo.ehviewer.coil.ehUrl

private val application = EhApplication.application
private val errorDrawable = AppCompatResources.getDrawable(application, R.drawable.image_failed)

open class LoadImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FixedAspectImageView(context, attrs, defStyleAttr),
    View.OnClickListener,
    View.OnLongClickListener {
    private var mOffsetX = Integer.MIN_VALUE
    private var mOffsetY = Integer.MIN_VALUE
    private var mClipWidth = Integer.MIN_VALUE
    private var mClipHeight = Integer.MIN_VALUE
    private var mUrl: String? = null

    @RetryType
    private val mRetryType: Int =
        context.obtainStyledAttributes(attrs, R.styleable.LoadImageView, defStyleAttr, 0).run {
            getInt(R.styleable.LoadImageView_retryType, 0).also { recycle() }
        }

    private fun setRetry(canRetry: Boolean) {
        when (mRetryType) {
            RETRY_TYPE_CLICK -> {
                setOnClickListener(if (canRetry) this else null)
                isClickable = canRetry
            }

            RETRY_TYPE_LONG_CLICK -> {
                setOnLongClickListener(if (canRetry) this else null)
                isLongClickable = canRetry
            }

            RETRY_TYPE_NONE -> {}
        }
    }

    fun setClip(offsetX: Int, offsetY: Int, clipWidth: Int, clipHeight: Int) {
        mOffsetX = offsetX
        mOffsetY = offsetY
        mClipWidth = clipWidth
        mClipHeight = clipHeight
    }

    fun resetClip() {
        mOffsetX = Integer.MIN_VALUE
        mOffsetY = Integer.MIN_VALUE
        mClipWidth = Integer.MIN_VALUE
        mClipHeight = Integer.MIN_VALUE
    }

    fun load(url: String, crossfade: Boolean = true) {
        mUrl = url
        load(url) {
            size(Size.ORIGINAL)
            if (!crossfade) crossfade(false)
            listener(
                { setRetry(false) },
                { setRetry(true) },
                { _, _ ->
                    super.setImageDrawable(errorDrawable)
                    setRetry(true)
                },
                { _, _ -> setRetry(false) },
            )
            ehUrl(url)
        }
    }

    private fun reload() {
        mUrl?.let { this.load(it) }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        var newDrawable = drawable
        if (newDrawable != null) {
            if (Integer.MIN_VALUE != mOffsetX) {
                newDrawable =
                    PreciselyClipDrawable(newDrawable, mOffsetX, mOffsetY, mClipWidth, mClipHeight)
            }
            onPreSetImageDrawable(newDrawable, true)
        }
        super.setImageDrawable(newDrawable)
    }

    override fun getDrawable(): Drawable? {
        var newDrawable = super.getDrawable()
        if (newDrawable is PreciselyClipDrawable) {
            newDrawable = newDrawable.drawable
        }
        return newDrawable
    }

    override fun onClick(v: View) {
        reload()
    }

    override fun onLongClick(v: View): Boolean {
        reload()
        return true
    }

    open fun onPreSetImageDrawable(drawable: Drawable?, isTarget: Boolean) {}

    @IntDef(RETRY_TYPE_NONE, RETRY_TYPE_CLICK, RETRY_TYPE_LONG_CLICK)
    @Retention(AnnotationRetention.SOURCE)
    private annotation class RetryType

    companion object {
        const val RETRY_TYPE_NONE = 0
        const val RETRY_TYPE_CLICK = 1
        const val RETRY_TYPE_LONG_CLICK = 2
    }
}
