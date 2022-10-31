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
package com.hippo.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Animatable
import android.os.Handler
import android.os.Looper
import java.io.FileDescriptor
import java.lang.ref.WeakReference

/**
 * A image with [Image] for data and [Bitmap] for render.
 */
class ImageBitmap : Animatable, Runnable {
    private val mBitmap: Bitmap

    /**
     * Return the format of the image
     */
    val format: Int

    /**
     * Return image is opaque
     */
    val isOpaque: Boolean
    private val mCallbackSet: MutableSet<WeakReference<Callback>> = LinkedHashSet()
    private var mImage: Image? = null
    private var mReferences = 0
    private var mAnimationReferences = 0
    private var mRunning = false

    private constructor(image: Image) {
        val width = image.width
        val height = image.height
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        image.render(0, 0, mBitmap, 0, 0, width, height, false, 0)
        format = image.format
        isOpaque = image.isOpaque
        if (format == 1) {
            // For animated image, save image object
            mImage = image
        } else {
            // Free the image
            image.recycle()
        }
    }

    private constructor(bitmap: Bitmap) {
        format = Image.FORMAT_NORMAL
        mBitmap = bitmap
        isOpaque = !bitmap.hasAlpha()
    }

    /**
     * Obtain the image bitmap
     *
     * @return false for the image is recycled and obtain failed
     */
    @Synchronized
    fun obtain(): Boolean {
        return if (mBitmap.isRecycled) {
            false
        } else {
            ++mReferences
            true
        }
    }

    /**
     * Release the image bitmap
     */
    @Synchronized
    fun release() {
        --mReferences
        if (mReferences <= 0 && !mBitmap.isRecycled) {
            mBitmap.recycle()
            if (mImage != null) {
                mImage!!.recycle()
            }
        }
    }

    /**
     * Add a callback for invalidating
     */
    fun addCallback(callback: Callback) {
        val iterator = mCallbackSet.iterator()
        var c: Callback?
        while (iterator.hasNext()) {
            c = iterator.next().get()
            if (c == null) {
                // Remove from the set if the reference has been cleared or
                // it can't be used.
                iterator.remove()
            } else if (c === callback) {
                return
            }
        }
        mCallbackSet.add(WeakReference(callback))
    }

    /**
     * Remove a callback
     */
    fun removeCallback(callback: Callback) {
        val iterator = mCallbackSet.iterator()
        var c: Callback?
        while (iterator.hasNext()) {
            c = iterator.next().get()
            if (c == null) {
                // Remove from the set if the reference has been cleared or
                // it can't be used.
                iterator.remove()
            } else if (c === callback) {
                iterator.remove()
                return
            }
        }
    }

    /**
     * Return image width
     */
    val width: Int
        get() = mBitmap.width

    /**
     * Return image height
     */
    val height: Int
        get() = mBitmap.height

    /**
     * Return image is animated
     */
    val isAnimated: Boolean
        get() = mImage != null

    /**
     * Draw image to canvas
     */
    fun draw(canvas: Canvas, left: Float, top: Float, paint: Paint?) {
        if (!mBitmap.isRecycled) {
            canvas.drawBitmap(mBitmap, left, top, paint)
        }
    }

    /**
     * Draw image to canvas
     */
    fun draw(canvas: Canvas, src: Rect?, dst: Rect, paint: Paint?) {
        if (!mBitmap.isRecycled) {
            canvas.drawBitmap(mBitmap, src, dst, paint)
        }
    }

    /**
     * Draw image to canvas
     */
    fun draw(canvas: Canvas, src: Rect?, dst: RectF, paint: Paint?) {
        if (!mBitmap.isRecycled) {
            canvas.drawBitmap(mBitmap, src, dst, paint)
        }
    }

    /**
     * `start()` and `stop()` is a pair
     */
    override fun start() {
        mAnimationReferences++
        if (mBitmap.isRecycled || mImage == null || mRunning) {
            return
        }
        mRunning = true
        HANDLER.postDelayed(this, Math.max(0, mImage!!.delay).toLong())
    }

    /**
     * `start()` and `stop()` is a pair
     */
    override fun stop() {
        mAnimationReferences--
        if (mAnimationReferences <= 0) {
            mRunning = false
            HANDLER.removeCallbacks(this)
        }
    }

    override fun isRunning(): Boolean {
        return mRunning
    }

    private fun notifyUpdate(): Boolean {
        var hasCallback = false
        val iterator = mCallbackSet.iterator()
        var callback: Callback?
        while (iterator.hasNext()) {
            callback = iterator.next().get()
            if (callback != null) {
                // Render bitmap int the first time
                if (!hasCallback) {
                    hasCallback = true
                    mImage!!.render(0, 0, mBitmap, 0, 0, mImage!!.width, mImage!!.height, false, 0)
                }
                callback.invalidateImage(this)
            } else {
                // Remove from the set if the reference has been cleared or
                // it can't be used.
                iterator.remove()
            }
        }
        return hasCallback
    }

    override fun run() {
        // Check recycled
        if (mBitmap.isRecycled || mImage == null) {
            mRunning = false
            return
        }
        mImage!!.advance()
        if (notifyUpdate()) {
            if (mRunning) {
                HANDLER.postDelayed(this, Math.max(0, mImage!!.delay).toLong())
            }
        } else {
            mRunning = false
        }
    }

    interface Callback {
        fun invalidateImage(who: ImageBitmap?)
    }

    companion object {
        private val HANDLER = Handler(Looper.getMainLooper())

        /**
         * Decode `InputStream`, then create image.
         */
        @JvmStatic
        fun decode(fd: FileDescriptor): ImageBitmap? {
            val image = Image.decode(fd, false)
            return if (image != null) {
                create(image)
            } else {
                null
            }
        }

        /**
         * Create `ImageBitmap` from `Image`.
         * It is not recommended. Use [.decode] if you can.
         *
         * @param image the image should not be used before and
         * it must not be recycled. And the image should
         * not be used directly anymore.
         */
        fun create(image: Image): ImageBitmap? {
            return if (!image.isRecycled) {
                try {
                    ImageBitmap(image)
                } catch (e: OutOfMemoryError) {
                    image.recycle()
                    null
                }
            } else {
                null
            }
        }

        /**
         * Create `ImageBitmap` from `Bitmap`.
         *
         * @param bitmap the bitmap should not be recycled
         */
        fun create(bitmap: Bitmap): ImageBitmap? {
            return if (!bitmap.isRecycled) {
                ImageBitmap(bitmap)
            } else {
                null
            }
        }
    }
}