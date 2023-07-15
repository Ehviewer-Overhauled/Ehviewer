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
package com.hippo.ehviewer.gallery

import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.obtainSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.releaseSpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.OnSpiderListener
import com.hippo.ehviewer.util.SimpleHandler
import com.hippo.unifile.UniFile
import java.util.Locale

class EhPageLoader(private val mGalleryInfo: GalleryInfo) : PageLoader2(), OnSpiderListener {
    private lateinit var mSpiderQueen: SpiderQueen
    override fun start() {
        mSpiderQueen = obtainSpiderQueen(mGalleryInfo, SpiderQueen.MODE_READ)
        mSpiderQueen.addOnSpiderListener(this)
    }

    override fun stop() {
        super.stop()
        mSpiderQueen.removeOnSpiderListener(this)
        // Activity recreate may called, so wait 0.5s
        SimpleHandler.postDelayed(ReleaseTask(mSpiderQueen), 500)
    }

    override val startPage
        get() = mSpiderQueen.startPage

    override fun getImageFilename(index: Int): String {
        return String.format(
            Locale.US,
            "%d-%s-%08d",
            mGalleryInfo.gid,
            mGalleryInfo.token,
            index + 1,
        )
    }

    override fun getImageFilenameWithExtension(index: Int): String {
        val filename = getImageFilename(index)
        return mSpiderQueen.getExtension(index)?.let { "$filename.$it" } ?: filename
    }

    override fun save(index: Int, file: UniFile): Boolean {
        return mSpiderQueen.save(index, file)
    }

    override fun save(index: Int, dir: UniFile, filename: String): UniFile? {
        return mSpiderQueen.save(index, dir, filename)
    }

    override fun putStartPage(page: Int) {
        mSpiderQueen.putStartPage(page)
    }

    override val size: Int
        get() = mSpiderQueen.size

    override fun onRequest(index: Int) {
        mSpiderQueen.request(index)
    }

    override fun onForceRequest(index: Int, orgImg: Boolean) {
        mSpiderQueen.forceRequest(index, orgImg)
    }

    override suspend fun awaitReady(): Boolean {
        return mSpiderQueen.awaitReady()
    }

    override val isReady: Boolean
        get() = ::mSpiderQueen.isInitialized && mSpiderQueen.isReady

    override fun onCancelRequest(index: Int) {
        mSpiderQueen.cancelRequest(index)
    }

    override fun onGetPages(pages: Int) {}

    override fun onGet509(index: Int) {}

    override fun onPageDownload(
        index: Int,
        contentLength: Long,
        receivedSize: Long,
        bytesRead: Int,
    ) {
        if (contentLength > 0) {
            notifyPagePercent(index, receivedSize.toFloat() / contentLength)
        }
    }

    override fun onPageSuccess(index: Int, finished: Int, downloaded: Int, total: Int) {}
    override fun onPageFailure(
        index: Int,
        error: String?,
        finished: Int,
        downloaded: Int,
        total: Int,
    ) {
        notifyPageFailed(index, error)
    }

    override fun onFinish(finished: Int, downloaded: Int, total: Int) {}
    override fun onGetImageSuccess(index: Int, image: Image?) {
        notifyPageSucceed(index, image!!)
    }

    override fun onGetImageFailure(index: Int, error: String?) {
        notifyPageFailed(index, error)
    }

    override fun preloadPages(pages: List<Int>, pair: Pair<Int, Int>) {
        mSpiderQueen.preloadPages(pages, pair)
    }

    private class ReleaseTask(private var mSpiderQueen: SpiderQueen?) : Runnable {
        override fun run() {
            mSpiderQueen?.let { releaseSpiderQueen(it, SpiderQueen.MODE_READ) }
            mSpiderQueen = null
        }
    }
}
