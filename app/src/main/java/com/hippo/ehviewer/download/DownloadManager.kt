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
package com.hippo.ehviewer.download

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.util.Log
import android.util.SparseLongArray
import androidx.collection.LongSparseArray
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.DownloadLabel
import com.hippo.ehviewer.spider.SpiderDen
import com.hippo.ehviewer.spider.SpiderInfo
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.OnSpiderListener
import com.hippo.image.Image
import com.hippo.util.IoThreadPoolExecutor
import com.hippo.yorozuya.ConcurrentPool
import com.hippo.yorozuya.MathUtils
import com.hippo.yorozuya.ObjectUtils
import com.hippo.yorozuya.SimpleHandler
import com.hippo.yorozuya.collect.LongList
import java.io.IOException
import java.util.LinkedList

class DownloadManager : OnSpiderListener {
    // All download info list
    private val mAllInfoList: LinkedList<DownloadInfo>

    // All download info map
    private val mAllInfoMap: LongSparseArray<DownloadInfo>

    // label and info list map, without default label info list
    private val mMap: MutableMap<String?, LinkedList<DownloadInfo>>

    // All labels without default label
    private val mLabelList: MutableList<DownloadLabel>

    // Store download info with default label
    private val mDefaultInfoList: LinkedList<DownloadInfo>

    // Store download info wait to start
    private val mWaitList: LinkedList<DownloadInfo>
    private val mSpeedReminder: SpeedReminder
    private val mDownloadInfoListeners: MutableList<DownloadInfoListener?>
    private val mNotifyTaskPool = ConcurrentPool<NotifyTask?>(5)
    private var mDownloadListener: DownloadListener? = null
    private var mCurrentTask: DownloadInfo? = null
    private var mCurrentSpider: SpiderQueen? = null

    init {

        // Get all labels
        val labels = EhDB.getAllDownloadLabelList()
        mLabelList = labels

        // Create list for each label
        val map = HashMap<String?, LinkedList<DownloadInfo>>()
        mMap = map
        for ((_, label1) in labels) {
            map[label1] = LinkedList()
        }

        // Create default for non tag
        mDefaultInfoList = LinkedList()

        // Get all info
        val allInfoList = EhDB.getAllDownloadInfo()
        mAllInfoList = LinkedList(allInfoList)

        // Create all info map
        val allInfoMap = LongSparseArray<DownloadInfo>(allInfoList.size + 10)
        mAllInfoMap = allInfoMap
        for (info in allInfoList) {

            // Add to all info map
            allInfoMap.put(info.gid, info)

            // Add to each label list
            var list = getInfoListForLabel(info.label)
            if (list == null) {
                // Can't find the label in label list
                list = LinkedList()
                map[info.label] = list
                if (!containLabel(info.label)) {
                    // Add label to DB and list
                    labels.add(EhDB.addDownloadLabel(info.label))
                }
            }
            list.add(info)
        }
        mWaitList = LinkedList()
        mSpeedReminder = SpeedReminder()
        mDownloadInfoListeners = ArrayList()
    }

    private fun getInfoListForLabel(label: String?): LinkedList<DownloadInfo>? {
        return if (label == null) {
            mDefaultInfoList
        } else {
            mMap[label]
        }
    }

    fun containLabel(label: String?): Boolean {
        if (label == null) {
            return false
        }
        for ((_, label1) in mLabelList) {
            if (label == label1) {
                return true
            }
        }
        return false
    }

    fun containDownloadInfo(gid: Long): Boolean {
        return mAllInfoMap.indexOfKey(gid) >= 0
    }

    val labelList: List<DownloadLabel>
        get() = mLabelList
    val allDownloadInfoList: List<DownloadInfo>
        get() = mAllInfoList
    val defaultDownloadInfoList: List<DownloadInfo>
        get() = mDefaultInfoList

    fun getLabelDownloadInfoList(label: String?): List<DownloadInfo>? {
        return mMap[label]
    }

    fun getDownloadInfo(gid: Long): DownloadInfo? {
        return mAllInfoMap[gid]
    }

    fun getDownloadState(gid: Long): Int {
        val info = mAllInfoMap[gid]
        return info?.state ?: DownloadInfo.STATE_INVALID
    }

    fun addDownloadInfoListener(downloadInfoListener: DownloadInfoListener?) {
        mDownloadInfoListeners.add(downloadInfoListener)
    }

    fun removeDownloadInfoListener(downloadInfoListener: DownloadInfoListener?) {
        mDownloadInfoListeners.remove(downloadInfoListener)
    }

    fun setDownloadListener(listener: DownloadListener?) {
        mDownloadListener = listener
    }

    private fun ensureDownload() {
        if (mCurrentTask != null) {
            // Only one download
            return
        }

        // Get download from wait list
        if (!mWaitList.isEmpty()) {
            val info = mWaitList.removeFirst()
            val spider = SpiderQueen.obtainSpiderQueen(info, SpiderQueen.MODE_DOWNLOAD)
            mCurrentTask = info
            mCurrentSpider = spider
            spider.addOnSpiderListener(this)
            info.state = DownloadInfo.STATE_DOWNLOAD
            info.speed = -1
            info.remaining = -1
            info.total = -1
            info.finished = 0
            info.downloaded = 0
            info.legacy = -1
            // Update in DB
            EhDB.putDownloadInfo(info)
            // Start speed count
            mSpeedReminder.start()
            // Notify start downloading
            if (mDownloadListener != null) {
                mDownloadListener!!.onStart(info)
            }
            // Notify state update
            val list: List<DownloadInfo>? = getInfoListForLabel(info.label)
            if (list != null) {
                for (l in mDownloadInfoListeners) {
                    l!!.onUpdate(info, list)
                }
            }
        }
    }

    fun startDownload(galleryInfo: GalleryInfo, label: String?) {
        if (mCurrentTask != null && mCurrentTask!!.gid == galleryInfo.gid) {
            // It is current task
            return
        }

        // Check in download list
        var info = mAllInfoMap[galleryInfo.gid]
        if (info != null) { // Get it in download list
            if (info.state != DownloadInfo.STATE_WAIT) {
                // Set state DownloadInfo.STATE_WAIT
                info.state = DownloadInfo.STATE_WAIT
                // Add to wait list
                mWaitList.add(info)
                // Update in DB
                EhDB.putDownloadInfo(info)
                // Notify state update
                val list: List<DownloadInfo>? = getInfoListForLabel(info.label)
                if (list != null) {
                    for (l in mDownloadInfoListeners) {
                        l!!.onUpdate(info, list)
                    }
                }
                // Make sure download is running
                ensureDownload()
            }
        } else {
            // It is new download info
            info = DownloadInfo(galleryInfo)
            info.label = label
            info.state = DownloadInfo.STATE_WAIT
            info.time = System.currentTimeMillis()

            // Add to label download list
            val list = getInfoListForLabel(info.label)
            if (list == null) {
                Log.e(TAG, "Can't find download info list with label: $label")
                return
            }
            list.addFirst(info)

            // Add to all download list and map
            mAllInfoList.addFirst(info)
            mAllInfoMap.put(galleryInfo.gid, info)

            // Add to wait list
            mWaitList.add(info)

            // Save to
            EhDB.putDownloadInfo(info)

            // Notify
            for (l in mDownloadInfoListeners) {
                l!!.onAdd(info, list, list.size - 1)
            }
            // Make sure download is running
            ensureDownload()

            // Add it to history
            EhDB.putHistoryInfo(info)
        }
    }

    fun startRangeDownload(gidList: LongList) {
        var update = false
        for (i in 0 until gidList.size) {
            val gid = gidList[i]
            val info = mAllInfoMap[gid]
            if (null == info) {
                Log.d(TAG, "Can't get download info with gid: $gid")
                continue
            }
            if (info.state == DownloadInfo.STATE_NONE || info.state == DownloadInfo.STATE_FAILED || info.state == DownloadInfo.STATE_FINISH) {
                update = true
                // Set state DownloadInfo.STATE_WAIT
                info.state = DownloadInfo.STATE_WAIT
                // Add to wait list
                mWaitList.add(info)
                // Update in DB
                EhDB.putDownloadInfo(info)
            }
        }
        if (update) {
            // Notify Listener
            for (l in mDownloadInfoListeners) {
                l!!.onUpdateAll()
            }
            // Ensure download
            ensureDownload()
        }
    }

    fun startAllDownload() {
        var update = false
        // Start all STATE_NONE and STATE_FAILED item
        for (info in mAllInfoList) {
            if (info.state == DownloadInfo.STATE_NONE || info.state == DownloadInfo.STATE_FAILED) {
                update = true
                // Set state DownloadInfo.STATE_WAIT
                info.state = DownloadInfo.STATE_WAIT
                // Add to wait list
                mWaitList.add(info)
                // Update in DB
                EhDB.putDownloadInfo(info)
            }
        }
        if (update) {
            // Notify Listener
            for (l in mDownloadInfoListeners) {
                l!!.onUpdateAll()
            }
            // Ensure download
            ensureDownload()
        }
    }

    @JvmOverloads
    fun addDownload(downloadInfoList: List<DownloadInfo>, notify: Boolean = true) {
        for (info in downloadInfoList) {
            if (containDownloadInfo(info.gid)) {
                // Contain
                return
            }

            // Ensure download state
            if (DownloadInfo.STATE_WAIT == info.state ||
                DownloadInfo.STATE_DOWNLOAD == info.state
            ) {
                info.state = DownloadInfo.STATE_NONE
            }

            // Add to label download list
            var list = getInfoListForLabel(info.label)
            if (null == list) {
                // Can't find the label in label list
                list = LinkedList()
                mMap[info.label] = list
                if (!containLabel(info.label)) {
                    // Add label to DB and list
                    mLabelList.add(EhDB.addDownloadLabel(info.label))
                }
            }
            list.add(info)
            // Sort
            list.sortByDateDescending()

            // Add to all download list and map
            mAllInfoList.add(info)
            mAllInfoMap.put(info.gid, info)

            // Save to
            EhDB.putDownloadInfo(info)
        }

        // Sort all download list
        mAllInfoList.sortByDateDescending()

        // Notify
        if (notify) {
            for (l in mDownloadInfoListeners) {
                l!!.onReload()
            }
        }
    }

    fun addDownloadLabel(downloadLabelList: List<DownloadLabel>) {
        for (label in downloadLabelList) {
            val labelString = label.label
            if (!containLabel(labelString)) {
                mMap[labelString] = LinkedList()
                mLabelList.add(EhDB.addDownloadLabel(label))
            }
        }
    }

    fun addDownload(galleryInfo: GalleryInfo, label: String?) {
        if (containDownloadInfo(galleryInfo.gid)) {
            // Contain
            return
        }

        // It is new download info
        val info = DownloadInfo(galleryInfo)
        info.label = label
        info.state = DownloadInfo.STATE_NONE
        info.time = System.currentTimeMillis()

        // Add to label download list
        val list = getInfoListForLabel(info.label)
        if (list == null) {
            Log.e(TAG, "Can't find download info list with label: $label")
            return
        }
        list.addFirst(info)

        // Add to all download list and map
        mAllInfoList.addFirst(info)
        mAllInfoMap.put(galleryInfo.gid, info)

        // Save to
        EhDB.putDownloadInfo(info)

        // Notify
        for (l in mDownloadInfoListeners) {
            l!!.onAdd(info, list, list.size - 1)
        }
    }

    fun stopDownload(gid: Long) {
        val info = stopDownloadInternal(gid)
        if (info != null) {
            // Update listener
            val list: List<DownloadInfo>? = getInfoListForLabel(info.label)
            if (list != null) {
                for (l in mDownloadInfoListeners) {
                    l!!.onUpdate(info, list)
                }
            }
            // Ensure download
            ensureDownload()
        }
    }

    fun stopCurrentDownload() {
        val info = stopCurrentDownloadInternal()
        if (info != null) {
            // Update listener
            val list: List<DownloadInfo>? = getInfoListForLabel(info.label)
            if (list != null) {
                for (l in mDownloadInfoListeners) {
                    l!!.onUpdate(info, list)
                }
            }
            // Ensure download
            ensureDownload()
        }
    }

    fun stopRangeDownload(gidList: LongList) {
        stopRangeDownloadInternal(gidList)

        // Update listener
        for (l in mDownloadInfoListeners) {
            l!!.onUpdateAll()
        }

        // Ensure download
        ensureDownload()
    }

    fun stopAllDownload() {
        // Stop all in wait list
        for (info in mWaitList) {
            info.state = DownloadInfo.STATE_NONE
            // Update in DB
            EhDB.putDownloadInfo(info)
        }
        mWaitList.clear()

        // Stop current
        stopCurrentDownloadInternal()

        // Notify mDownloadInfoListener
        for (l in mDownloadInfoListeners) {
            l!!.onUpdateAll()
        }
    }

    fun deleteDownload(gid: Long) {
        stopDownloadInternal(gid)
        val info = mAllInfoMap[gid]
        if (info != null) {
            // Remove from DB
            EhDB.removeDownloadInfo(info)

            // Remove all list and map
            mAllInfoList.remove(info)
            mAllInfoMap.remove(info.gid)

            // Remove label list
            val list = getInfoListForLabel(info.label)
            if (list != null) {
                val index = list.indexOf(info)
                if (index >= 0) {
                    list.remove(info)
                    // Update listener
                    for (l in mDownloadInfoListeners) {
                        l!!.onRemove(info, list, index)
                    }
                }
            }

            // Ensure download
            ensureDownload()
        }
    }

    fun deleteRangeDownload(gidList: LongList) {
        stopRangeDownloadInternal(gidList)
        for (i in 0 until gidList.size) {
            val gid = gidList[i]
            val info = mAllInfoMap[gid]
            if (null == info) {
                Log.d(TAG, "Can't get download info with gid: $gid")
                continue
            }

            // Remove from DB
            EhDB.removeDownloadInfo(info)

            // Remove from all info map
            mAllInfoList.remove(info)
            mAllInfoMap.remove(info.gid)

            // Remove from label list
            val list = getInfoListForLabel(info.label)
            list?.remove(info)
        }

        // Update listener
        for (l in mDownloadInfoListeners) {
            l!!.onReload()
        }

        // Ensure download
        ensureDownload()
    }

    fun moveDownload(fromPosition: Int, toPosition: Int) {
        if (fromPosition > toPosition) {
            val time = mAllInfoList[toPosition].time
            for (i in toPosition until fromPosition) {
                mAllInfoList[i].time = mAllInfoList[i + 1].time
            }
            mAllInfoList[fromPosition].time = time
            EhDB.updateDownloadInfo(mAllInfoList.slice(toPosition..fromPosition))
        } else {
            val time = mAllInfoList[fromPosition].time
            for (i in fromPosition until toPosition) {
                mAllInfoList[i].time = mAllInfoList[i + 1].time
            }
            mAllInfoList[toPosition].time = time
            EhDB.updateDownloadInfo(mAllInfoList.slice(fromPosition..toPosition))
        }
        val label = mAllInfoList[fromPosition].label
        mAllInfoList.sortByDateDescending()
        val list = getInfoListForLabel(label)!!
        list.sortByDateDescending()
    }

    fun moveDownload(label: String?, fromPosition: Int, toPosition: Int) {
        val list = getInfoListForLabel(label)
        list?.let {
            val absFromPosition = mAllInfoList.indexOf(it[fromPosition])
            val absToPosition = mAllInfoList.indexOf(it[toPosition])
            moveDownload(absFromPosition, absToPosition)
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun resetAllReadingProgress() {
        val list = LinkedList(mAllInfoList)
        object : AsyncTask<Void?, Void?, Void?>() {
            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg voids: Void?): Void? {
                val galleryInfo: GalleryInfo = BaseGalleryInfo()
                for (downloadInfo in list) {
                    galleryInfo.gid = downloadInfo.gid
                    galleryInfo.token = downloadInfo.token
                    galleryInfo.title = downloadInfo.title
                    galleryInfo.thumb = downloadInfo.thumb
                    galleryInfo.category = downloadInfo.category
                    galleryInfo.posted = downloadInfo.posted
                    galleryInfo.uploader = downloadInfo.uploader
                    galleryInfo.rating = downloadInfo.rating
                    val downloadDir = SpiderDen.getGalleryDownloadDir(galleryInfo.gid) ?: continue
                    val file = downloadDir.findFile(SpiderQueen.SPIDER_INFO_FILENAME) ?: continue
                    val spiderInfo = SpiderInfo.readCompatFromUniFile(file) ?: continue
                    spiderInfo.startPage = 0
                    try {
                        spiderInfo.write(file)
                    } catch (e: IOException) {
                        Log.e(TAG, "Can't write SpiderInfo", e)
                    }
                }
                return null
            }
        }.executeOnExecutor(IoThreadPoolExecutor.getInstance())
    }

    // Update in DB
    // Update listener
    // No ensureDownload
    private fun stopDownloadInternal(gid: Long): DownloadInfo? {
        // Check current task
        if (mCurrentTask != null && mCurrentTask!!.gid == gid) {
            // Stop current
            return stopCurrentDownloadInternal()
        }
        val iterator = mWaitList.iterator()
        while (iterator.hasNext()) {
            val info = iterator.next()
            if (info.gid == gid) {
                // Remove from wait list
                iterator.remove()
                // Update state
                info.state = DownloadInfo.STATE_NONE
                // Update in DB
                EhDB.putDownloadInfo(info)
                return info
            }
        }
        return null
    }

    // Update in DB
    // Update mDownloadListener
    private fun stopCurrentDownloadInternal(): DownloadInfo? {
        val info = mCurrentTask
        val spider = mCurrentSpider
        // Release spider
        if (spider != null) {
            spider.removeOnSpiderListener(this@DownloadManager)
            SpiderQueen.releaseSpiderQueen(spider, SpiderQueen.MODE_DOWNLOAD)
        }
        mCurrentTask = null
        mCurrentSpider = null
        // Stop speed reminder
        mSpeedReminder.stop()
        if (info == null) {
            return null
        }

        // Update state
        info.state = DownloadInfo.STATE_NONE
        // Update in DB
        EhDB.putDownloadInfo(info)
        // Listener
        if (mDownloadListener != null) {
            mDownloadListener!!.onCancel(info)
        }
        return info
    }

    // Update in DB
    // Update mDownloadListener
    private fun stopRangeDownloadInternal(gidList: LongList) {
        // Two way
        if (gidList.size < mWaitList.size) {
            for (i in 0 until gidList.size) {
                stopDownloadInternal(gidList[i])
            }
        } else {
            // Check current task
            if (mCurrentTask != null && gidList.contains(mCurrentTask!!.gid)) {
                // Stop current
                stopCurrentDownloadInternal()
            }

            // Check all in wait list
            val iterator = mWaitList.iterator()
            while (iterator.hasNext()) {
                val info = iterator.next()
                if (gidList.contains(info.gid)) {
                    // Remove from wait list
                    iterator.remove()
                    // Update state
                    info.state = DownloadInfo.STATE_NONE
                    // Update in DB
                    EhDB.putDownloadInfo(info)
                }
            }
        }
    }

    /**
     * @param label Not allow new label
     */
    fun changeLabel(list: List<DownloadInfo>, label: String?) {
        if (null != label && !containLabel(label)) {
            Log.e(TAG, "Not exits label: $label")
            return
        }
        val dstList: MutableList<DownloadInfo>? = getInfoListForLabel(label)
        if (dstList == null) {
            Log.e(TAG, "Can't find label with label: $label")
            return
        }
        for (info in list) {
            if (ObjectUtils.equal(info.label, label)) {
                continue
            }
            val srcList: MutableList<DownloadInfo>? = getInfoListForLabel(info.label)
            if (srcList == null) {
                Log.e(TAG, "Can't find label with label: " + info.label)
                continue
            }
            srcList.remove(info)
            dstList.add(info)
            info.label = label
            dstList.sortByDateDescending()

            // Save to DB
            EhDB.putDownloadInfo(info)
        }
        for (l in mDownloadInfoListeners) {
            l!!.onReload()
        }
    }

    fun addLabel(label: String?) {
        if (label == null || containLabel(label)) {
            return
        }
        mLabelList.add(EhDB.addDownloadLabel(label))
        mMap[label] = LinkedList()
        for (l in mDownloadInfoListeners) {
            l!!.onUpdateLabels()
        }
    }

    fun moveLabel(fromPosition: Int, toPosition: Int) {
        val item = mLabelList.removeAt(fromPosition)
        mLabelList.add(toPosition, item)
        EhDB.moveDownloadLabel(fromPosition, toPosition)
        for (l in mDownloadInfoListeners) {
            l!!.onUpdateLabels()
        }
    }

    fun renameLabel(from: String, to: String) {
        // Find in label list
        var found = false
        for (raw in mLabelList) {
            if (from == raw.label) {
                found = true
                raw.label = to
                // Update in DB
                EhDB.updateDownloadLabel(raw)
                break
            }
        }
        if (!found) {
            return
        }
        val list = mMap.remove(from) ?: return

        // Update info label
        for (info in list) {
            info.label = to
            // Update in DB
            EhDB.putDownloadInfo(info)
        }
        // Put list back with new label
        mMap[to] = list

        // Notify listener
        for (l in mDownloadInfoListeners) {
            l!!.onRenameLabel(from, to)
        }
    }

    fun deleteLabel(label: String) {
        // Find in label list and remove
        var found = false
        val iterator = mLabelList.iterator()
        while (iterator.hasNext()) {
            val raw = iterator.next()
            if (label == raw.label) {
                found = true
                iterator.remove()
                EhDB.removeDownloadLabel(raw)
                break
            }
        }
        if (!found) {
            return
        }
        val list = mMap.remove(label) ?: return

        // Update info label
        for (info in list) {
            info.label = null
            // Update in DB
            EhDB.putDownloadInfo(info)
            mDefaultInfoList.add(info)
        }

        // Sort
        mDefaultInfoList.sortByDateDescending()

        // Notify listener
        for (l in mDownloadInfoListeners) {
            l!!.onChange()
        }
    }

    val isIdle: Boolean
        get() = mCurrentTask == null && mWaitList.isEmpty()

    override fun onGetPages(pages: Int) {
        var task = mNotifyTaskPool.pop()
        if (task == null) {
            task = NotifyTask()
        }
        task.setOnGetPagesData(pages)
        SimpleHandler.getInstance().post(task)
    }

    override fun onGet509(index: Int) {
        var task = mNotifyTaskPool.pop()
        if (task == null) {
            task = NotifyTask()
        }
        task.setOnGet509Data(index)
        SimpleHandler.getInstance().post(task)
    }

    override fun onPageDownload(
        index: Int,
        contentLength: Long,
        receivedSize: Long,
        bytesRead: Int
    ) {
        var task = mNotifyTaskPool.pop()
        if (task == null) {
            task = NotifyTask()
        }
        task.setOnPageDownloadData(index, contentLength, receivedSize, bytesRead)
        SimpleHandler.getInstance().post(task)
    }

    override fun onPageSuccess(index: Int, finished: Int, downloaded: Int, total: Int) {
        var task = mNotifyTaskPool.pop()
        if (task == null) {
            task = NotifyTask()
        }
        task.setOnPageSuccessData(index, finished, downloaded, total)
        SimpleHandler.getInstance().post(task)
    }

    override fun onPageFailure(
        index: Int,
        error: String,
        finished: Int,
        downloaded: Int,
        total: Int
    ) {
        var task = mNotifyTaskPool.pop()
        if (task == null) {
            task = NotifyTask()
        }
        task.setOnPageFailureDate(index, error, finished, downloaded, total)
        SimpleHandler.getInstance().post(task)
    }

    override fun onFinish(finished: Int, downloaded: Int, total: Int) {
        var task = mNotifyTaskPool.pop()
        if (task == null) {
            task = NotifyTask()
        }
        task.setOnFinishDate(finished, downloaded, total)
        SimpleHandler.getInstance().post(task)
    }

    override fun onGetImageSuccess(index: Int, image: Image) {
        // Ignore
    }

    override fun onGetImageFailure(index: Int, error: String) {
        // Ignore
    }

    interface DownloadInfoListener {
        /**
         * Add the special info to the special position
         */
        fun onAdd(info: DownloadInfo, list: List<DownloadInfo>, position: Int)

        /**
         * The special info is changed
         */
        fun onUpdate(info: DownloadInfo, list: List<DownloadInfo>)

        /**
         * Maybe all data is changed, but size is the same
         */
        fun onUpdateAll()

        /**
         * Maybe all data is changed, maybe list is changed
         */
        fun onReload()

        /**
         * The list is gone, use default list please
         */
        fun onChange()

        /**
         * Rename label
         */
        fun onRenameLabel(from: String, to: String)

        /**
         * Remove the special info from the special position
         */
        fun onRemove(info: DownloadInfo, list: List<DownloadInfo>, position: Int)
        fun onUpdateLabels()
    }

    interface DownloadListener {
        /**
         * Get 509 error
         */
        fun onGet509()

        /**
         * Start download
         */
        fun onStart(info: DownloadInfo)

        /**
         * Update download speed
         */
        fun onDownload(info: DownloadInfo)

        /**
         * Update page downloaded
         */
        fun onGetPage(info: DownloadInfo)

        /**
         * Download done
         */
        fun onFinish(info: DownloadInfo)

        /**
         * Download done
         */
        fun onCancel(info: DownloadInfo)
    }

    private inner class NotifyTask : Runnable {
        private var mType = 0
        private var mPages = 0
        private var mIndex = 0
        private var mContentLength: Long = 0
        private var mReceivedSize: Long = 0
        private var mBytesRead = 0
        private var mError: String? = null
        private var mFinished = 0
        private var mDownloaded = 0
        private var mTotal = 0
        fun setOnGetPagesData(pages: Int) {
            mType = Companion.TYPE_ON_GET_PAGES
            mPages = pages
        }

        fun setOnGet509Data(index: Int) {
            mType = Companion.TYPE_ON_GET_509
            mIndex = index
        }

        fun setOnPageDownloadData(
            index: Int,
            contentLength: Long,
            receivedSize: Long,
            bytesRead: Int
        ) {
            mType = Companion.TYPE_ON_PAGE_DOWNLOAD
            mIndex = index
            mContentLength = contentLength
            mReceivedSize = receivedSize
            mBytesRead = bytesRead
        }

        fun setOnPageSuccessData(index: Int, finished: Int, downloaded: Int, total: Int) {
            mType = Companion.TYPE_ON_PAGE_SUCCESS
            mIndex = index
            mFinished = finished
            mDownloaded = downloaded
            mTotal = total
        }

        fun setOnPageFailureDate(
            index: Int,
            error: String?,
            finished: Int,
            downloaded: Int,
            total: Int
        ) {
            mType = Companion.TYPE_ON_PAGE_FAILURE
            mIndex = index
            mError = error
            mFinished = finished
            mDownloaded = downloaded
            mTotal = total
        }

        fun setOnFinishDate(finished: Int, downloaded: Int, total: Int) {
            mType = Companion.TYPE_ON_FINISH
            mFinished = finished
            mDownloaded = downloaded
            mTotal = total
        }

        override fun run() {
            when (mType) {
                Companion.TYPE_ON_GET_PAGES -> {
                    val info = mCurrentTask
                    if (info == null) {
                        Log.e(TAG, "Current task is null, but it should not be")
                    } else {
                        info.total = mPages
                        val list: List<DownloadInfo>? = getInfoListForLabel(info.label)
                        if (list != null) {
                            for (l in mDownloadInfoListeners) {
                                l!!.onUpdate(info, list)
                            }
                        }
                    }
                }

                Companion.TYPE_ON_GET_509 -> {
                    if (mDownloadListener != null) {
                        mDownloadListener!!.onGet509()
                    }
                }

                Companion.TYPE_ON_PAGE_DOWNLOAD -> mSpeedReminder.onDownload(
                    mIndex,
                    mContentLength,
                    mReceivedSize,
                    mBytesRead
                )

                Companion.TYPE_ON_PAGE_SUCCESS -> {
                    mSpeedReminder.onDone(mIndex)
                    val info = mCurrentTask
                    if (info == null) {
                        Log.e(TAG, "Current task is null, but it should not be")
                    } else {
                        info.finished = mFinished
                        info.downloaded = mDownloaded
                        info.total = mTotal
                        if (mDownloadListener != null) {
                            mDownloadListener!!.onGetPage(info)
                        }
                        val list: List<DownloadInfo>? = getInfoListForLabel(info.label)
                        if (list != null) {
                            for (l in mDownloadInfoListeners) {
                                l!!.onUpdate(info, list)
                            }
                        }
                    }
                }

                Companion.TYPE_ON_PAGE_FAILURE -> {
                    mSpeedReminder.onDone(mIndex)
                    val info = mCurrentTask
                    if (info == null) {
                        Log.e(TAG, "Current task is null, but it should not be")
                    } else {
                        info.finished = mFinished
                        info.downloaded = mDownloaded
                        info.total = mTotal
                        val list: List<DownloadInfo>? = getInfoListForLabel(info.label)
                        if (list != null) {
                            for (l in mDownloadInfoListeners) {
                                l!!.onUpdate(info, list)
                            }
                        }
                    }
                }

                Companion.TYPE_ON_FINISH -> {
                    mSpeedReminder.onFinish()
                    // Download done
                    val info = mCurrentTask
                    mCurrentTask = null
                    val spider = mCurrentSpider
                    mCurrentSpider = null
                    // Release spider
                    if (spider != null) {
                        spider.removeOnSpiderListener(this@DownloadManager)
                        SpiderQueen.releaseSpiderQueen(spider, SpiderQueen.MODE_DOWNLOAD)
                    }
                    // Check null
                    if (info == null || spider == null) {
                        Log.e(TAG, "Current stuff is null, but it should not be")
                    } else {
                        // Stop speed count
                        mSpeedReminder.stop()
                        // Update state
                        info.finished = mFinished
                        info.downloaded = mDownloaded
                        info.total = mTotal
                        info.legacy = mTotal - mFinished
                        if (info.legacy == 0) {
                            info.state = DownloadInfo.STATE_FINISH
                        } else {
                            info.state = DownloadInfo.STATE_FAILED
                        }
                        // Update in DB
                        EhDB.putDownloadInfo(info)
                        // Notify
                        if (mDownloadListener != null) {
                            mDownloadListener!!.onFinish(info)
                        }
                        val list: List<DownloadInfo>? = getInfoListForLabel(info.label)
                        if (list != null) {
                            for (l in mDownloadInfoListeners) {
                                l!!.onUpdate(info, list)
                            }
                        }
                        // Start next download
                        ensureDownload()
                    }
                }
            }
            mNotifyTaskPool.push(this)
        }
    }

    internal inner class SpeedReminder : Runnable {
        private val mContentLengthMap = SparseLongArray()
        private val mReceivedSizeMap = SparseLongArray()
        private var mStop = true
        private var mBytesRead: Long = 0
        private var oldSpeed: Long = -1
        fun start() {
            if (mStop) {
                mStop = false
                SimpleHandler.getInstance().post(this)
            }
        }

        fun stop() {
            if (!mStop) {
                mStop = true
                mBytesRead = 0
                oldSpeed = -1
                mContentLengthMap.clear()
                mReceivedSizeMap.clear()
                SimpleHandler.getInstance().removeCallbacks(this)
            }
        }

        fun onDownload(index: Int, contentLength: Long, receivedSize: Long, bytesRead: Int) {
            mContentLengthMap.put(index, contentLength)
            mReceivedSizeMap.put(index, receivedSize)
            mBytesRead += bytesRead.toLong()
        }

        fun onDone(index: Int) {
            mContentLengthMap.delete(index)
            mReceivedSizeMap.delete(index)
        }

        fun onFinish() {
            mContentLengthMap.clear()
            mReceivedSizeMap.clear()
        }

        override fun run() {
            val info = mCurrentTask
            if (info != null) {
                var newSpeed = mBytesRead / 2
                if (oldSpeed != -1L) {
                    newSpeed =
                        MathUtils.lerp(oldSpeed.toFloat(), newSpeed.toFloat(), 0.75f).toLong()
                }
                oldSpeed = newSpeed
                info.speed = newSpeed

                // Calculate remaining
                if (info.total <= 0) {
                    info.remaining = -1
                } else if (newSpeed == 0L) {
                    info.remaining = 300L * 24L * 60L * 60L * 1000L // 300 days
                } else {
                    var downloadingCount = 0
                    var downloadingContentLengthSum: Long = 0
                    var totalSize: Long = 0
                    for (i in 0 until maxOf(mContentLengthMap.size(), mReceivedSizeMap.size())) {
                        val contentLength = mContentLengthMap.valueAt(i)
                        val receivedSize = mReceivedSizeMap.valueAt(i)
                        downloadingCount++
                        downloadingContentLengthSum += contentLength
                        totalSize += contentLength - receivedSize
                    }
                    if (downloadingCount != 0) {
                        totalSize += downloadingContentLengthSum * (info.total - info.downloaded - downloadingCount) / downloadingCount
                        info.remaining = totalSize / newSpeed * 1000
                    }
                }
                if (mDownloadListener != null) {
                    mDownloadListener!!.onDownload(info)
                }
                val list: List<DownloadInfo>? = getInfoListForLabel(info.label)
                if (list != null) {
                    for (l in mDownloadInfoListeners) {
                        l!!.onUpdate(info, list)
                    }
                }
            }
            mBytesRead = 0
            if (!mStop) {
                SimpleHandler.getInstance().postDelayed(this, 2000)
            }
        }
    }

    companion object {
        private val TAG = DownloadManager::class.java.simpleName
        private const val TYPE_ON_GET_PAGES = 0
        private const val TYPE_ON_GET_509 = 1
        private const val TYPE_ON_PAGE_DOWNLOAD = 2
        private const val TYPE_ON_PAGE_SUCCESS = 3
        private const val TYPE_ON_PAGE_FAILURE = 4
        private const val TYPE_ON_FINISH = 5

        fun MutableList<DownloadInfo>.sortByDateDescending() {
            sortByDescending { it.time }
        }
    }
}