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
import com.hippo.ehviewer.dao.FilterMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val regex = { p: Filter -> Regex(p.text) }.memoize()
private inline fun List<Filter>.anyActive(predicate: (Filter) -> Boolean) = any { it.enable && predicate(it) }

object EhFilter : CoroutineScope {
    val titleFilterList = mutableStateListOf<Filter>()
    val uploaderFilterList = mutableStateListOf<Filter>()
    val tagFilterList = mutableStateListOf<Filter>()
    val tagNamespaceFilterList = mutableStateListOf<Filter>()
    val commenterFilterList = mutableStateListOf<Filter>()
    val commentFilterList = mutableStateListOf<Filter>()
    override val coroutineContext = Dispatchers.IO.limitedParallelism(1)
    private fun <R> Filter.launchOps(
        callback: ((R) -> Unit)? = null,
        ops: suspend Filter.() -> R,
    ) = launch { ops().let { callback?.invoke(it) } }
    fun Filter.remember(callback: ((Boolean) -> Unit)? = null) = launchOps(callback) {
        EhDB.addFilter(this).also { if (it) memorizeFilter(this) }
    }
    fun Filter.trigger(callback: ((Unit) -> Unit)? = null) = launchOps(callback) { EhDB.triggerFilter(this) }
    fun Filter.forget() = launchOps {
        EhDB.deleteFilter(this)
        when (mode) {
            FilterMode.TITLE -> titleFilterList.remove(this)
            FilterMode.TAG -> tagFilterList.remove(this)
            FilterMode.TAG_NAMESPACE -> tagNamespaceFilterList.remove(this)
            FilterMode.UPLOADER -> uploaderFilterList.remove(this)
            FilterMode.COMMENTER -> commenterFilterList.remove(this)
            FilterMode.COMMENT -> commentFilterList.remove(this)
        }
    }

    init { launch { EhDB.getAllFilter().forEach(::memorizeFilter) } }

    private fun memorizeFilter(filter: Filter) {
        when (filter.mode) {
            FilterMode.TITLE -> titleFilterList.add(filter.apply { text = text.lowercase() })
            FilterMode.TAG -> tagFilterList.add(filter.apply { text = text.lowercase() })
            FilterMode.TAG_NAMESPACE -> tagNamespaceFilterList.add(filter.apply { text = text.lowercase() })
            FilterMode.UPLOADER -> uploaderFilterList.add(filter)
            FilterMode.COMMENTER -> commenterFilterList.add(filter)
            FilterMode.COMMENT -> commentFilterList.add(filter)
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
