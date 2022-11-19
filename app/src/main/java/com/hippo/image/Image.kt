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

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.ALLOCATOR_DEFAULT
import android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
import android.graphics.ImageDecoder.Source
import android.graphics.PixelFormat
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.core.graphics.component3
import androidx.core.graphics.component4
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import java.io.FileInputStream
import java.nio.channels.FileChannel

class Image private constructor(source: Source?, drawable: Drawable? = null,
                                val hardware: Boolean = true
) {
    internal var mObtainedDrawable: Drawable?
    private var mBitmap: Bitmap? = null
    init {
        mObtainedDrawable = null
        source?.let {
            mObtainedDrawable = ImageDecoder.decodeDrawable(source) { decoder: ImageDecoder, _, _ ->
                decoder.allocator = if (hardware) ALLOCATOR_DEFAULT else ALLOCATOR_SOFTWARE
            // Sadly we must use software memory since we need copy it to tile buffer, fuck glgallery
            // Idk it will cause how much performance regression
            } // Should we lazy decode it?
        }
        if (mObtainedDrawable == null) {
            mObtainedDrawable = drawable!!
        }
    }

    val animated = mObtainedDrawable is AnimatedImageDrawable
    val width = mObtainedDrawable!!.intrinsicWidth
    val height = mObtainedDrawable!!.intrinsicHeight
    val isRecycled = mObtainedDrawable == null

    var started = false

    @Synchronized
    fun recycle() {
        if (mObtainedDrawable is AnimatedImageDrawable) {
            (mObtainedDrawable as AnimatedImageDrawable?)?.stop()
        }
        if (mObtainedDrawable is BitmapDrawable) {
            (mObtainedDrawable as BitmapDrawable?)?.bitmap?.recycle()
        }
        mObtainedDrawable?.callback = null
        mObtainedDrawable = null
        mBitmap?.recycle()
        mBitmap = null
    }

    private fun prepareBitmap() {
        if (mBitmap != null) return
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    private fun updateBitmap() {
        prepareBitmap()
        val (oldLeft, oldTop, oldRight, oldBottom) = mObtainedDrawable!!.bounds
        mObtainedDrawable!!.setBounds(0, 0, width, height)
        mObtainedDrawable!!.draw(Canvas(mBitmap!!))
        mObtainedDrawable!!.setBounds(oldLeft, oldTop, oldRight, oldBottom)
    }

    fun render(
        srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int,
        width: Int, height: Int
    ) {
        check(!hardware) { "Hardware buffer cannot be used in glgallery" }
        val bitmap: Bitmap = if (animated) {
            updateBitmap()
            mBitmap!!
        } else {
            mObtainedDrawable!!.toBitmap()
        }
        nativeRender(
            bitmap,
            srcX,
            srcY,
            dst,
            dstX,
            dstY,
            width,
            height
        )
    }

    fun texImage(init: Boolean, offsetX: Int, offsetY: Int, width: Int, height: Int) {
        check(!hardware) { "Hardware buffer cannot be used in glgallery" }
        val bitmap: Bitmap = if (animated) {
            updateBitmap()
            mBitmap!!
        } else {
            mObtainedDrawable!!.toBitmap()
        }
        nativeTexImage(
            bitmap,
            init,
            offsetX,
            offsetY,
            width,
            height
        )
    }

    fun start() {
        if (!started) {
            (mObtainedDrawable as AnimatedImageDrawable?)?.start()
        }
    }

    val delay: Int
        get() {
            if (animated)
                return 100
            return 0
        }

    val isOpaque: Boolean
        get() {
            return mObtainedDrawable?.opacity == PixelFormat.OPAQUE
        }

    companion object {
        @JvmStatic
        fun decode(stream: FileInputStream, hardware: Boolean = true): Image {
            val src = ImageDecoder.createSource(
                stream.channel.map(
                    FileChannel.MapMode.READ_ONLY, 0,
                    stream.available().toLong()
                )
            )
            return Image(src, hardware = hardware)
        }

        @JvmStatic
        fun create(bitmap: Bitmap): Image {
            return Image(null, bitmap.toDrawable(Resources.getSystem()), false)
        }

        @JvmStatic
        private external fun nativeRender(
            bitmap: Bitmap,
            srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int,
            width: Int, height: Int
        )

        @JvmStatic
        private external fun nativeTexImage(
            bitmap: Bitmap,
            init: Boolean,
            offsetX: Int,
            offsetY: Int,
            width: Int,
            height: Int
        )
    }
}