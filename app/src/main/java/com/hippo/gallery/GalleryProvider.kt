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

package com.hippo.gallery

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.annotation.IntDef
import androidx.annotation.UiThread
import com.hippo.image.Image
import com.hippo.yorozuya.ConcurrentPool
import com.hippo.yorozuya.OSUtils

abstract class GalleryProvider {
    private val mNotifyTaskPool = ConcurrentPool<NotifyTask>(5)
    private val mImageCache = ImageCache()
    @Volatile
    private var mListener: Listener? = null
    private var mStarted = false
    abstract val error: String

    @UiThread
    open fun start() {
        OSUtils.checkMainLoop()
        check(!mStarted) { "Can't start it twice" }
        mStarted = true
    }

    @UiThread
    open fun stop() {
        OSUtils.checkMainLoop()
        mImageCache.evictAll()
    }

    abstract fun size(): Int

    fun request(index: Int) {}

    fun forceRequest(index: Int) {
        onForceRequest(index)
    }

    fun removeCache(index: Int) {
        mImageCache.remove(index)
    }

    protected abstract fun onRequest(index: Int)

    protected abstract fun onForceRequest(index: Int)

    fun cancelRequest(index: Int) {
        onCancelRequest(index)
    }

    protected abstract fun onCancelRequest(index: Int)

    fun setListener(listener: Listener?) {
        mListener = listener
    }

    fun notifyDataChanged() {
        notify(NotifyTask.TYPE_DATA_CHANGED, -1, 0.0f, null, null)
    }

    fun notifyDataChanged(index: Int) {
        notify(NotifyTask.TYPE_DATA_CHANGED, index, 0.0f, null, null)
    }

    fun notifyPageWait(index: Int) {
        notify(NotifyTask.TYPE_WAIT, index, 0.0f, null, null)
    }

    fun notifyPagePercent(index: Int, percent: Float) {
        notify(NotifyTask.TYPE_PERCENT, index, percent, null, null)
    }

    fun notifyPageSucceed(index: Int, image: Image) {
        mImageCache.put(index, image)
        notify(NotifyTask.TYPE_SUCCEED, index, 0.0f, image, null)
    }

    fun notifyPageFailed(index: Int, error: String) {
        notify(NotifyTask.TYPE_FAILED, index, 0.0f, null, error)
    }

    private fun notify(
        @NotifyTask.Type type: Int,
        index: Int,
        percent: Float,
        image: Image?,
        error: String?
    ) {
        val listener = mListener ?: return
    }

    interface Listener {

        fun onDataChanged()

        fun onPageWait(index: Int)

        fun onPagePercent(index: Int, percent: Float)

        fun onPageSucceed(index: Int, image: Drawable)

        fun onPageFailed(index: Int, error: String)

        fun onDataChanged(index: Int)
    }

    private class NotifyTask(
        private val mListener: Listener,
        private val mPool: ConcurrentPool<NotifyTask>
    ) {
        @Type
        private var mType: Int = 0
        private var mIndex: Int = 0
        private var mPercent: Float = 0.toFloat()
        private var mImage: Drawable? = null
        private var mError: String? = null

        fun setData(@Type type: Int, index: Int, percent: Float, image: Drawable, error: String) {
            mType = type
            mIndex = index
            mPercent = percent
            mImage = image
            mError = error
        }

        @IntDef(TYPE_DATA_CHANGED, NotifyTask.TYPE_WAIT, TYPE_PERCENT, TYPE_SUCCEED, TYPE_FAILED)
        @Retention(AnnotationRetention.SOURCE)
        annotation class Type
        companion object {
            const val TYPE_DATA_CHANGED = 0
            const val TYPE_WAIT = 1
            const val TYPE_PERCENT = 2
            const val TYPE_SUCCEED = 3
            const val TYPE_FAILED = 4
        }
    }

    private class ImageCache : LruCache<Int, Image>(CACHE_SIZE) {
        override fun sizeOf(key: Int?, value: Image): Int {
            return value.height * value.width * 4
        }

        override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Image?, newValue: Image?) {
            oldValue?.recycle()
        }

        companion object {
            private const val CACHE_SIZE = 512 * 1024 * 1024
        }
    }

    companion object {
        const val STATE_WAIT = -1
        const val STATE_ERROR = -2
    }
}
