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

import androidx.annotation.UiThread
import com.hippo.image.Image
import com.hippo.yorozuya.OSUtils
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

abstract class GalleryProvider {
    val mPages by lazy { (0..size()).map { ReaderPage(it) } }
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
    }

    abstract fun size(): Int

    fun request(index: Int) {
        onRequest(index)
    }

    fun forceRequest(index: Int) {
        onForceRequest(index)
    }

    protected abstract fun onRequest(index: Int)

    protected abstract fun onForceRequest(index: Int)

    fun cancelRequest(index: Int) {
        onCancelRequest(index)
    }

    protected abstract fun onCancelRequest(index: Int)

    fun notifyDataChanged() {}

    fun notifyDataChanged(index: Int) {
        onRequest(index)
    }

    fun notifyPageWait(index: Int) {
        mPages[index].status.value = Page.State.QUEUE
    }

    fun notifyPagePercent(index: Int, percent: Float) {
        mPages[index].status.compareAndSet(Page.State.QUEUE, Page.State.DOWNLOAD_IMAGE)
        mPages[index].progress.value = percent
    }

    fun notifyPageSucceed(index: Int, image: Image) {
        mPages[index].image.value = image
        mPages[index].status.value = Page.State.READY
    }

    fun notifyPageFailed(index: Int) {
        mPages[index].status.value = Page.State.ERROR
    }

    companion object {
        const val STATE_WAIT = -1
        const val STATE_ERROR = -2
    }
}
