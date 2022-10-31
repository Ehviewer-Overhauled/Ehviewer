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
import com.hippo.Native.getFd
import java.io.FileDescriptor
import java.util.function.Consumer

/**
 * The `Image` is a image which stored pixel data in native heap
 */
class Image private constructor(nativePtr: Long) {
    private var mNativePtr: Long
    private var needRecycle = false
    private var nativeProcessCnt = 0

    init {
        if (needEvictAll) evictAll()
        mNativePtr = nativePtr
        imageList.add(this)
    }

    /**
     * Return the format of the image
     */
    val format: Int
        get() {
            checkRecycledAndLock()
            val ret = nativeGetFormat(mNativePtr)
            releaseNativeLock()
            return ret
        }

    /**
     * Return the width of the image
     */
    val width: Int
        get() {
            checkRecycledAndLock()
            val ret = nativeGetWidth(mNativePtr)
            releaseNativeLock()
            return ret
        }

    /**
     * Return the height of the image
     */
    val height: Int
        get() {
            checkRecycledAndLock()
            val ret = nativeGetHeight(mNativePtr)
            releaseNativeLock()
            return ret
        }

    @Synchronized
    private fun checkRecycledAndLock() {
        check(mNativePtr != 0L) { "The image is recycled." }
        nativeProcessCnt++
    }

    @Synchronized
    private fun releaseNativeLock() {
        nativeProcessCnt--
        if (needRecycle) recycle()
    }

    /**
     * Render the image to `Bitmap`
     */
    fun render(
        srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int,
        width: Int, height: Int, fillBlank: Boolean, defaultColor: Int
    ) {
        checkRecycledAndLock()
        nativeRender(mNativePtr, srcX, srcY, dst, dstX, dstY, width, height)
        releaseNativeLock()
    }

    /**
     * Call `glTexImage2D` for init is true and
     * call `glTexSubImage2D` for init is false.
     * width * height must <= 512 * 512 or do nothing
     */
    fun texImage(init: Boolean, offsetX: Int, offsetY: Int, width: Int, height: Int) {
        checkRecycledAndLock()
        nativeTexImage(mNativePtr, init, offsetX, offsetY, width, height)
        releaseNativeLock()
    }

    /**
     * Move to next frame. Do nothing for non-animation image
     */
    fun advance() {
        checkRecycledAndLock()
        nativeAdvance(mNativePtr)
        releaseNativeLock()
    }

    /**
     * Return current frame delay. 0 for non-animation image
     */
    val delay: Int
        get() {
            checkRecycledAndLock()
            val delay = nativeGetDelay(mNativePtr)
            releaseNativeLock()
            return if (delay <= 10) 100 else delay
        }

    /**
     * Return is the image opaque
     */
    val isOpaque: Boolean
        get() {
            checkRecycledAndLock()
            val ret = nativeIsOpaque(mNativePtr)
            releaseNativeLock()
            return ret
        }

    /**
     * Free the native object associated with this image.
     * It must be called when the image will not be used.
     * The image can't be used after this method is called.
     */
    @Synchronized
    fun recycle() {
        if (mNativePtr != 0L) {
            if (nativeProcessCnt != 0) {
                needRecycle = true
                return
            }
            nativeRecycle(mNativePtr)
            mNativePtr = 0
            imageList.remove(this)
        }
    }

    /**
     * Returns true if this image has been recycled.
     */
    val isRecycled: Boolean
        get() = mNativePtr == 0L

    companion object {
        const val FORMAT_NORMAL = 0
        const val FORMAT_ANIMATED = 1
        private val imageList = ArrayList<Image>()
        private var needEvictAll = false
        private fun newFromAddr(addr: Long): Image? {
            return if (addr == 0L) null else Image(addr)
        }

        @JvmStatic
        fun lazyEvictAll() {
            needEvictAll = true
        }

        private fun evictAll() {
            ArrayList(imageList).forEach(
                Consumer { obj: Image -> obj.recycle() })
            needEvictAll = false
        }

        /**
         * Decode image from `InputStream`
         */
        @JvmStatic
        fun decode(fd: FileDescriptor?, partially: Boolean): Image? {
            return newFromAddr(nativeDecode(getFd(fd)))
        }

        /**
         * Decode image from `InputStream`
         */
        @JvmStatic
        fun decode(fd: Int, partially: Boolean): Image? {
            return newFromAddr(nativeDecode(fd))
        }

        /**
         * Decode image from `InputStream`
         */
        @JvmStatic
        fun decodeAddr(addr: Long, partially: Boolean): Image? {
            return newFromAddr(nativeDecodeAddr(addr))
        }

        /**
         * Create a plain image from Bitmap
         */
        @JvmStatic
        fun create(bitmap: Bitmap): Image? {
            return newFromAddr(nativeCreate(bitmap))
        }

        /**
         * Return all un-recycled `Image` instance count.
         * It is useful for debug.
         */
        @JvmStatic
        val imageCount: Int
            get() = imageList.size

        @JvmStatic
        private external fun nativeDecode(fd: Int): Long

        @JvmStatic
        private external fun nativeDecodeAddr(addr: Long): Long

        @JvmStatic
        private external fun nativeCreate(bitmap: Bitmap): Long

        @JvmStatic
        private external fun nativeRender(
            nativePtr: Long,
            srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int,
            width: Int, height: Int
        )

        @JvmStatic
        private external fun nativeTexImage(
            nativePtr: Long,
            init: Boolean,
            offsetX: Int,
            offsetY: Int,
            width: Int,
            height: Int
        )

        @JvmStatic
        private external fun nativeAdvance(nativePtr: Long)

        @JvmStatic
        private external fun nativeGetDelay(nativePtr: Long): Int

        @JvmStatic
        private external fun nativeIsOpaque(nativePtr: Long): Boolean

        @JvmStatic
        private external fun nativeRecycle(nativePtr: Long)

        @JvmStatic
        private external fun nativeGetFormat(nativePtr: Long): Int

        @JvmStatic
        private external fun nativeGetWidth(nativePtr: Long): Int

        @JvmStatic
        private external fun nativeGetHeight(nativePtr: Long): Int
    }
}