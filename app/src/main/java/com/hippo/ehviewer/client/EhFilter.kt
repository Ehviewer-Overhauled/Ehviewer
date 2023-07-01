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

import androidx.compose.runtime.mutableStateListOf
import arrow.core.memoize
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.dao.Filter

private val regex = { p: Filter -> Regex(p.text) }.memoize()
private inline fun List<Filter>.anyActive(predicate: (Filter) -> Boolean) = any { it.enable && predicate(it) }

object EhFilter {
    val titleFilterList = mutableStateListOf<Filter>()
    val uploaderFilterList = mutableStateListOf<Filter>()
    val tagFilterList = mutableStateListOf<Filter>()
    val tagNamespaceFilterList = mutableStateListOf<Filter>()
    val commenterFilterList = mutableStateListOf<Filter>()
    val commentFilterList = mutableStateListOf<Filter>()

    const val MODE_TITLE = 0
    const val MODE_UPLOADER = 1
    const val MODE_TAG = 2
    const val MODE_TAG_NAMESPACE = 3
    const val MODE_COMMENTER = 4
    const val MODE_COMMENT = 5

    init {
        EhDB.allFilter.forEach(::memorizeFilter)
    }

    private fun memorizeFilter(filter: Filter) {
        when (filter.mode) {
            MODE_TITLE -> titleFilterList.add(filter.apply { text = text.lowercase() })
            MODE_TAG -> tagFilterList.add(filter.apply { text = text.lowercase() })
            MODE_TAG_NAMESPACE -> tagNamespaceFilterList.add(filter.apply { text = text.lowercase() })
            MODE_UPLOADER -> uploaderFilterList.add(filter)
            MODE_COMMENTER -> commenterFilterList.add(filter)
            MODE_COMMENT -> commentFilterList.add(filter)
            else -> error("Unknown mode: " + filter.mode)
        }
    }

    fun addFilter(filter: Filter) = EhDB.addFilter(filter).also { if (it) memorizeFilter(filter) }
    fun triggerFilter(filter: Filter) = EhDB.triggerFilter(filter)

    fun deleteFilter(filter: Filter) {
        EhDB.deleteFilter(filter)
        when (filter.mode) {
            MODE_TITLE -> titleFilterList.remove(filter)
            MODE_TAG -> tagFilterList.remove(filter)
            MODE_TAG_NAMESPACE -> tagNamespaceFilterList.remove(filter)
            MODE_UPLOADER -> uploaderFilterList.remove(filter)
            MODE_COMMENTER -> commenterFilterList.remove(filter)
            MODE_COMMENT -> commentFilterList.remove(filter)
            else -> error("Unknown mode: " + filter.mode)
        }
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

    private fun matchTagNamespace(tag: String, filter: String): Boolean {
        val (nameSpace, _) = spiltTag(tag)
        return nameSpace == filter
    }

    fun needTags() = tagFilterList.isNotEmpty() || tagNamespaceFilterList.isNotEmpty()
    fun filterTitle(info: GalleryInfo) = titleFilterList.anyActive { it.text in info.title.orEmpty().lowercase() }
    fun filterUploader(info: GalleryInfo) = uploaderFilterList.anyActive { it.text == info.uploader }
    fun filterTag(info: GalleryInfo) = info.simpleTags?.any { tag -> tagFilterList.anyActive { matchTag(tag, it.text) } } ?: false
    fun filterTagNamespace(info: GalleryInfo) = info.simpleTags?.any { tag -> tagNamespaceFilterList.anyActive { matchTagNamespace(tag, it.text) } } ?: false
    fun filterCommenter(commenter: String) = commenterFilterList.anyActive { it.text == commenter }
    fun filterComment(comment: String) = commentFilterList.anyActive { regex(it).containsMatchIn(comment) }
}
