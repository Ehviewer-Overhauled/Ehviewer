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
package com.hippo.ehviewer.client

import android.util.Log
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.dao.Filter
import java.util.Locale
import java.util.regex.Pattern

object EhFilter {
    private val mTitleFilterList: MutableList<Filter> = ArrayList()
    private val mUploaderFilterList: MutableList<Filter> = ArrayList()
    private val mTagFilterList: MutableList<Filter> = ArrayList()
    private val mTagNamespaceFilterList: MutableList<Filter> = ArrayList()
    private val mCommenterFilterList: MutableList<Filter> = ArrayList()
    private val mCommentFilterList: MutableList<Filter> = ArrayList()

    const val MODE_TITLE = 0
    const val MODE_UPLOADER = 1
    const val MODE_TAG = 2
    const val MODE_TAG_NAMESPACE = 3
    const val MODE_COMMENTER = 4
    const val MODE_COMMENT = 5
    private val TAG = EhFilter::class.java.simpleName

    init {
        val list = EhDB.getAllFilter()
        var i = 0
        val n = list.size
        while (i < n) {
            val filter = list[i]
            when (filter.mode) {
                MODE_TITLE -> {
                    filter.text = filter.text!!.lowercase(Locale.getDefault())
                    mTitleFilterList.add(filter)
                }

                MODE_TAG -> {
                    filter.text = filter.text!!.lowercase(Locale.getDefault())
                    mTagFilterList.add(filter)
                }

                MODE_TAG_NAMESPACE -> {
                    filter.text = filter.text!!.lowercase(Locale.getDefault())
                    mTagNamespaceFilterList.add(filter)
                }

                MODE_UPLOADER -> mUploaderFilterList.add(filter)
                MODE_COMMENTER -> mCommenterFilterList.add(filter)
                MODE_COMMENT -> mCommentFilterList.add(filter)
                else -> Log.d(TAG, "Unknown mode: " + filter.mode)
            }
            i++
        }
    }

    val titleFilterList: List<Filter>
        get() = mTitleFilterList
    val uploaderFilterList: List<Filter>
        get() = mUploaderFilterList
    val tagFilterList: List<Filter>
        get() = mTagFilterList
    val tagNamespaceFilterList: List<Filter>
        get() = mTagNamespaceFilterList
    val commenterFilterList: List<Filter>
        get() = mCommenterFilterList
    val commentFilterList: List<Filter>
        get() = mCommentFilterList

    @Synchronized
    fun addFilter(filter: Filter): Boolean {
        // enable filter by default before it is added to database
        filter.enable = true
        if (!EhDB.addFilter(filter)) return false
        when (filter.mode) {
            MODE_TITLE -> {
                filter.text = filter.text!!.lowercase(Locale.getDefault())
                mTitleFilterList.add(filter)
            }

            MODE_TAG -> {
                filter.text = filter.text!!.lowercase(Locale.getDefault())
                mTagFilterList.add(filter)
            }

            MODE_TAG_NAMESPACE -> {
                filter.text = filter.text!!.lowercase(Locale.getDefault())
                mTagNamespaceFilterList.add(filter)
            }

            MODE_UPLOADER -> mUploaderFilterList.add(filter)
            MODE_COMMENTER -> mCommenterFilterList.add(filter)
            MODE_COMMENT -> mCommentFilterList.add(filter)
            else -> Log.d(TAG, "Unknown mode: " + filter.mode)
        }
        return true
    }

    @Synchronized
    fun triggerFilter(filter: Filter?) {
        EhDB.triggerFilter(filter)
    }

    @Synchronized
    fun deleteFilter(filter: Filter) {
        EhDB.deleteFilter(filter)
        when (filter.mode) {
            MODE_TITLE -> mTitleFilterList.remove(filter)
            MODE_TAG -> mTagFilterList.remove(filter)
            MODE_TAG_NAMESPACE -> mTagNamespaceFilterList.remove(filter)
            MODE_UPLOADER -> mUploaderFilterList.remove(filter)
            MODE_COMMENTER -> mCommenterFilterList.remove(filter)
            MODE_COMMENT -> mCommentFilterList.remove(filter)
            else -> Log.d(TAG, "Unknown mode: " + filter.mode)
        }
    }

    @Synchronized
    fun needTags(): Boolean {
        return 0 != mTagFilterList.size || 0 != mTagNamespaceFilterList.size
    }

    @Synchronized
    fun filterTitle(info: GalleryInfo?): Boolean {
        if (null == info) {
            return false
        }

        // Title
        val title = info.title
        val filters: List<Filter> = mTitleFilterList
        if (null != title && filters.isNotEmpty()) {
            var i = 0
            val n = filters.size
            while (i < n) {
                if (filters[i].enable!! && title.lowercase(Locale.getDefault()).contains(
                        filters[i].text!!
                    )
                ) {
                    return false
                }
                i++
            }
        }
        return true
    }

    @Synchronized
    fun filterUploader(info: GalleryInfo?): Boolean {
        if (null == info) {
            return false
        }

        // Uploader
        val uploader = info.uploader
        val filters: List<Filter> = mUploaderFilterList
        if (null != uploader && filters.isNotEmpty()) {
            var i = 0
            val n = filters.size
            while (i < n) {
                if (filters[i].enable!! && uploader == filters[i].text) {
                    return false
                }
                i++
            }
        }
        return true
    }

    private fun matchTag(tag: String?, filter: String?): Boolean {
        if (null == tag || null == filter) {
            return false
        }
        val tagNamespace: String?
        val tagName: String
        val filterNamespace: String?
        val filterName: String
        var index = tag.indexOf(':')
        if (index < 0) {
            tagNamespace = null
            tagName = tag
        } else {
            tagNamespace = tag.substring(0, index)
            tagName = tag.substring(index + 1)
        }
        index = filter.indexOf(':')
        if (index < 0) {
            filterNamespace = null
            filterName = filter
        } else {
            filterNamespace = filter.substring(0, index)
            filterName = filter.substring(index + 1)
        }
        return if (null != tagNamespace && null != filterNamespace &&
            tagNamespace != filterNamespace
        ) {
            false
        } else tagName == filterName
    }

    @Synchronized
    fun filterTag(info: GalleryInfo?): Boolean {
        if (null == info) {
            return false
        }

        // Tag
        val tags = info.simpleTags
        val filters: List<Filter> = mTagFilterList
        if (null != tags && filters.isNotEmpty()) {
            for (tag in tags) {
                var i = 0
                val n = filters.size
                while (i < n) {
                    if (filters[i].enable!! && matchTag(tag, filters[i].text)) {
                        return false
                    }
                    i++
                }
            }
        }
        return true
    }

    private fun matchTagNamespace(tag: String?, filter: String?): Boolean {
        if (null == tag || null == filter) {
            return false
        }
        val tagNamespace: String
        val index = tag.indexOf(':')
        return if (index >= 0) {
            tagNamespace = tag.substring(0, index)
            tagNamespace == filter
        } else {
            false
        }
    }

    @Synchronized
    fun filterTagNamespace(info: GalleryInfo?): Boolean {
        if (null == info) {
            return false
        }
        val tags = info.simpleTags
        val filters: List<Filter> = mTagNamespaceFilterList
        if (null != tags && filters.isNotEmpty()) {
            for (tag in tags) {
                var i = 0
                val n = filters.size
                while (i < n) {
                    if (filters[i].enable!! && matchTagNamespace(tag, filters[i].text)) {
                        return false
                    }
                    i++
                }
            }
        }
        return true
    }

    @Synchronized
    fun filterCommenter(commenter: String?): Boolean {
        if (null == commenter) {
            return false
        }
        val filters: List<Filter> = mCommenterFilterList
        if (filters.isNotEmpty()) {
            var i = 0
            val n = filters.size
            while (i < n) {
                if (filters[i].enable!! && commenter == filters[i].text) {
                    return false
                }
                i++
            }
        }
        return true
    }

    @Synchronized
    fun filterComment(comment: String?): Boolean {
        if (null == comment) {
            return false
        }
        val filters: List<Filter> = mCommentFilterList
        if (filters.isNotEmpty()) {
            var i = 0
            val n = filters.size
            while (i < n) {
                if (filters[i].enable!!) {
                    val p = Pattern.compile(filters[i].text!!)
                    val m = p.matcher(comment)
                    if (m.find()) return false
                }
                i++
            }
        }
        return true
    }
}