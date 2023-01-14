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
package com.hippo.image

import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.DecodeException
import android.graphics.ImageDecoder.ImageInfo
import android.graphics.ImageDecoder.Source
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import coil.decode.DecodeUtils
import coil.decode.FrameDelayRewritingSource
import coil.decode.isGif
import com.hippo.UriArchiveAccessor
import com.hippo.ehviewer.EhApplication
import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.math.min

class Image private constructor(source: Source, private val byteBuffer: ByteBuffer? = null) {
    var mObtainedDrawable: Drawable?

    init {
        mObtainedDrawable =
            ImageDecoder.decodeDrawable(source) { decoder: ImageDecoder, info: ImageInfo, _: Source ->
                decoder.setTargetColorSpace(colorSpace)
                decoder.setTargetSampleSize(
                    min(
                        info.size.width / (2 * screenWidth),
                        info.size.height / (2 * screenHeight)
                    ).coerceAtLeast(1)
                )
            }
        if (mObtainedDrawable is BitmapDrawable)
            byteBuffer?.let { UriArchiveAccessor.releaseByteBuffer(it) }
    }

    val width: Int
        get() = mObtainedDrawable?.intrinsicWidth ?: 0
    val height: Int
        get() = mObtainedDrawable?.intrinsicHeight ?: 0

    @Synchronized
    fun recycle() {
        (mObtainedDrawable as? Animatable)?.run {
            stop()
            byteBuffer?.let { UriArchiveAccessor.releaseByteBuffer(it) }
        }
        (mObtainedDrawable as? BitmapDrawable)?.run { bitmap.recycle() }
        mObtainedDrawable?.callback = null
        mObtainedDrawable = null
    }

    companion object {
        val screenWidth = EhApplication.application.resources.displayMetrics.widthPixels
        val screenHeight = EhApplication.application.resources.displayMetrics.heightPixels
        val isWideColorGamut =
            EhApplication.application.resources.configuration.isScreenWideColorGamut
        var colorSpace = ColorSpace.get(
            if (isWideColorGamut && EhApplication.readerPreferences.wideColorGamut()
                    .get()
            ) ColorSpace.Named.DISPLAY_P3 else ColorSpace.Named.SRGB
        )

        @Throws(DecodeException::class)
        @JvmStatic
        fun decode(stream: FileInputStream): Image {
            val source = stream.source().buffer()
            val src = createSource(source)
            return Image(src)
        }

        @Throws(DecodeException::class)
        @JvmStatic
        fun decode(buffer: ByteBuffer): Image {
            val source = Buffer().apply { write(buffer) }
            val src = createSource(source)
            return Image(src, buffer)
        }

        private fun createSource(source: BufferedSource): Source {
            val bufferedSource = if (DecodeUtils.isGif(source)) {
                FrameDelayRewritingSource(source).buffer()
            } else {
                source
            }
            return ImageDecoder.createSource(
                ByteBuffer.wrap(bufferedSource.use { it.readByteArray() })
            )
        }
    }
}