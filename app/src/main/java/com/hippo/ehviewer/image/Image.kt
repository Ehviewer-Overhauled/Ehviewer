/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.hippo.ehviewer.image

import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.ImageInfo
import android.graphics.ImageDecoder.Source
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
import splitties.init.appCtx
import java.nio.ByteBuffer
import kotlin.math.min

class Image private constructor(private val src: CloseableSource) {
    var mObtainedDrawable: Drawable? =
        ImageDecoder.decodeDrawable(src.source) { decoder: ImageDecoder, info: ImageInfo, _: Source ->
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                // Allocating hardware bitmap may cause a crash on framework versions prior to Android Q
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            decoder.setTargetColorSpace(colorSpace)
            decoder.setTargetSampleSize(
                calculateSampleSize(info, 2 * screenHeight, 2 * screenWidth),
            )
        }.also {
            if (it !is Animatable) src.close()
        }
        private set

    val size: Int
        get() = mObtainedDrawable!!.run { intrinsicHeight * intrinsicWidth * 4 * if (this is Animatable) 4 else 1 }

    @Synchronized
    fun recycle() {
        (mObtainedDrawable as? Animatable)?.stop()
        (mObtainedDrawable as? BitmapDrawable)?.bitmap?.recycle()
        mObtainedDrawable?.callback = null
        if (mObtainedDrawable is Animatable) src.close()
        mObtainedDrawable = null
    }

    companion object {
        fun calculateSampleSize(info: ImageInfo, targetHeight: Int, targetWeight: Int): Int {
            return min(
                info.size.width / targetWeight,
                info.size.height / targetHeight,
            ).coerceAtLeast(1)
        }

        private val imageSearchMaxSize = appCtx.resources.getDimensionPixelOffset(R.dimen.image_search_max_size)

        @JvmStatic
        val imageSearchDecoderSampleListener =
            ImageDecoder.OnHeaderDecodedListener { decoder, info, _ ->
                decoder.setTargetSampleSize(
                    calculateSampleSize(info, imageSearchMaxSize, imageSearchMaxSize),
                )
            }
        val screenWidth = appCtx.resources.displayMetrics.widthPixels
        val screenHeight = appCtx.resources.displayMetrics.heightPixels
        val isWideColorGamut = appCtx.resources.configuration.isScreenWideColorGamut
        var colorSpace = ColorSpace.get(
            if (isWideColorGamut && EhApplication.readerPreferences.wideColorGamut().get()) {
                ColorSpace.Named.DISPLAY_P3
            } else {
                ColorSpace.Named.SRGB
            },
        )

        @JvmStatic
        fun decode(src: CloseableSource): Image? {
            return runCatching {
                Image(src)
            }.onFailure {
                src.close()
                it.printStackTrace()
            }.getOrNull()
        }
    }

    interface CloseableSource : AutoCloseable {
        val source: Source
    }
}

external fun rewriteGifSource(buffer: ByteBuffer)
external fun rewriteGifSource2(fd: Int)
