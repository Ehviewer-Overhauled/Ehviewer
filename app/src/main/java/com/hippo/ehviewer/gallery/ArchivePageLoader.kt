/*
 * Copyright 2023 Tarsin Norbin
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

import android.content.Context
import android.net.Uri
import com.hippo.Native.getFd
import com.hippo.UriArchiveAccessor
import com.hippo.ehviewer.Settings
import com.hippo.image.Image
import com.hippo.unifile.UniFile
import com.hippo.yorozuya.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.FileOutputStream
import java.io.IOException

class ArchivePageLoader(context: Context, uri: Uri, passwdFlow: Flow<String>) : PageLoader2(),
    CoroutineScope {
    override val coroutineContext = Dispatchers.IO + Job()
    private val archiveAccessor by lazy { UriArchiveAccessor(context, uri) }
    private val hostJob = launch(start = CoroutineStart.LAZY) {
        size = archiveAccessor.open()
        if (size == 0) {
            return@launch
        }
        archiveAccessor.run {
            if (needPassword()) {
                Settings.getArchivePasswds()?.forEach {
                    if (providePassword(it)) return@launch
                }
                passwdFlow.collect {
                    if (providePassword(it)) {
                        Settings.putPasswdToArchivePasswds(it)
                        currentCoroutineContext().cancel()
                    }
                }
            }
        }
    }

    override var size = 0
        private set

    override fun start() {
        hostJob.start()
    }

    override fun stop() {
        cancel()
        archiveAccessor.close()
        super.stop()
    }

    private val mJobMap = hashMapOf<Int, Job>()
    private val mSemaphore = Semaphore(4)

    override fun onRequest(index: Int) {
        synchronized(mJobMap) {
            val current = mJobMap[index]
            if (current?.isActive != true) {
                mJobMap[index] = launch {
                    mSemaphore.withPermit {
                        doRealWork(index)
                    }
                }
            }
        }
    }

    private suspend fun doRealWork(index: Int) {
        val src = archiveAccessor.getImageSource(index) ?: return
        runCatching {
            currentCoroutineContext().ensureActive()
        }.onFailure {
            src.close()
            throw it
        }
        val image = Image.decode(src) ?: return notifyPageFailed(index, null)
        runCatching {
            currentCoroutineContext().ensureActive()
        }.onFailure {
            image.recycle()
            throw it
        }
        notifyPageSucceed(index, image)
    }

    override fun onForceRequest(index: Int) {
        onRequest(index)
    }

    override suspend fun awaitReady(): Boolean {
        hostJob.join()
        return size != 0
    }

    override fun onCancelRequest(index: Int) {
        mJobMap[index]?.cancel()
    }

    override fun getImageFilename(index: Int): String {
        return FileUtils.getNameFromFilename(getImageFilenameWithExtension(index))
    }

    override fun getImageFilenameWithExtension(index: Int): String {
        return FileUtils.sanitizeFilename(archiveAccessor.getFilename(index))
    }

    override fun save(index: Int, file: UniFile): Boolean {
        val fd: Int
        val stream: FileOutputStream
        try {
            stream = file.openOutputStream() as FileOutputStream
            fd = getFd(stream.fd)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        archiveAccessor.extractToFd(index, fd)
        try {
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    override fun save(index: Int, dir: UniFile, filename: String): UniFile {
        val extension = FileUtils.getExtensionFromFilename(getImageFilenameWithExtension(index))
        val dst = dir.subFile(if (null != extension) "$filename.$extension" else filename)
        save(index, dst!!)
        return dst
    }

    override fun preloadPages(pages: List<Int>, pair: Pair<Int, Int>) {}
}