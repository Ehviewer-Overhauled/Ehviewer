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

object EhFilter {
    val titleFilterList = mutableListOf<Filter>()
    val uploaderFilterList = mutableListOf<Filter>()
    val tagFilterList = mutableListOf<Filter>()
    val tagNamespaceFilterList = mutableListOf<Filter>()
    val commenterFilterList = mutableListOf<Filter>()
    val commentFilterList = mutableListOf<Filter>()

    const val MODE_TITLE = 0
    const val MODE_UPLOADER = 1
    const val MODE_TAG = 2
    const val MODE_TAG_NAMESPACE = 3
    const val MODE_COMMENTER = 4
    const val MODE_COMMENT = 5
    private const val TAG = "EhFilter"

    init {
        EhDB.allFilter.forEach { filter ->
            when (filter.mode) {
                MODE_TITLE -> {
                    filter.text = filter.text!!.lowercase()
                    titleFilterList.add(filter)
                }

                MODE_TAG -> {
                    filter.text = filter.text!!.lowercase()
                    tagFilterList.add(filter)
                }

                MODE_TAG_NAMESPACE -> {
                    filter.text = filter.text!!.lowercase()
                    tagNamespaceFilterList.add(filter)
                }

                MODE_UPLOADER -> uploaderFilterList.add(filter)
                MODE_COMMENTER -> commenterFilterList.add(filter)
                MODE_COMMENT -> commentFilterList.add(filter)
                else -> Log.d(TAG, "Unknown mode: " + filter.mode)
            }
        }
    }

    @Synchronized
    fun addFilter(filter: Filter): Boolean {
        // enable filter by default before it is added to database
        filter.enable = true
        if (!EhDB.addFilter(filter)) return false
        when (filter.mode) {
            MODE_TITLE -> {
                filter.text = filter.text!!.lowercase()
                titleFilterList.add(filter)
            }

            MODE_TAG -> {
                filter.text = filter.text!!.lowercase()
                tagFilterList.add(filter)
            }

            MODE_TAG_NAMESPACE -> {
                filter.text = filter.text!!.lowercase()
                tagNamespaceFilterList.add(filter)
            }

            MODE_UPLOADER -> uploaderFilterList.add(filter)
            MODE_COMMENTER -> commenterFilterList.add(filter)
            MODE_COMMENT -> commentFilterList.add(filter)
            else -> Log.d(TAG, "Unknown mode: " + filter.mode)
        }
        return true
    }

    @Synchronized
    fun triggerFilter(filter: Filter) {
        EhDB.triggerFilter(filter)
    }

    @Synchronized
    fun deleteFilter(filter: Filter) {
        EhDB.deleteFilter(filter)
        when (filter.mode) {
            MODE_TITLE -> titleFilterList.remove(filter)
            MODE_TAG -> tagFilterList.remove(filter)
            MODE_TAG_NAMESPACE -> tagNamespaceFilterList.remove(filter)
            MODE_UPLOADER -> uploaderFilterList.remove(filter)
            MODE_COMMENTER -> commenterFilterList.remove(filter)
            MODE_COMMENT -> commentFilterList.remove(filter)
            else -> Log.d(TAG, "Unknown mode: " + filter.mode)
        }
    }

    private inline fun <T> Iterable<T>.contains(predicate: (T) -> Boolean) = firstOrNull(predicate) != null

    private inline fun <T> Array<T>.contains(predicate: (T) -> Boolean) = firstOrNull(predicate) != null

    @Synchronized
    fun needTags(): Boolean {
        return 0 != tagFilterList.size || 0 != tagNamespaceFilterList.size
    }

    @Synchronized
    fun filterTitle(info: GalleryInfo): Boolean {
        val title = info.title ?: return false
        return titleFilterList.contains { it.enable!! && it.text!! in title.lowercase() }
    }

    @Synchronized
    fun filterUploader(info: GalleryInfo): Boolean {
        val uploader = info.uploader ?: return false
        return uploaderFilterList.contains { it.enable!! && it.text == uploader }
    }

    private fun spiltTag(tag: String) = tag.run {
        val index = indexOf(':')
        if (index < 0) null to this else substring(0, index) to substring(index + 1)
    }

    private fun matchTag(tag: String, filter: String): Boolean {
        val (tagNamespace, tagName) = spiltTag(tag)
        val (filterNamespace, filterName) = spiltTag(filter)
        return if (null != tagNamespace && null != filterNamespace && tagNamespace != filterNamespace) {
            false
        } else {
            tagName == filterName
        }
    }

    @Synchronized
    fun filterTag(info: GalleryInfo): Boolean {
        val tags = info.simpleTags ?: return false
        return tags.contains { tag -> tagFilterList.contains { it.enable!! && matchTag(tag, it.text!!) } }
    }

    private fun matchTagNamespace(tag: String, filter: String): Boolean {
        val (nameSpace, _) = spiltTag(tag)
        return nameSpace == filter
    }

    @Synchronized
    fun filterTagNamespace(info: GalleryInfo): Boolean {
        val tags = info.simpleTags ?: return false
        return tags.contains { tag -> tagNamespaceFilterList.contains { it.enable!! && matchTagNamespace(tag, it.text!!) } }
    }

    @Synchronized
    fun filterCommenter(commenter: String): Boolean {
        return commenterFilterList.contains { it.enable!! && it.text == commenter }
    }

    @Synchronized
    fun filterComment(comment: String): Boolean {
        return commentFilterList.contains { it.enable!! && Regex(it.text!!).containsMatchIn(comment) }
    }
}
