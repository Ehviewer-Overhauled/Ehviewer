package com.hippo.ehviewer.client

import arrow.core.memoize
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private val regex = { p: Filter -> Regex(p.text) }.memoize()

object EhFilter : CoroutineScope {
    override val coroutineContext = Dispatchers.IO.limitedParallelism(1)
    val filters = async { EhDB.getAllFilter() as MutableList }
    private suspend inline fun anyActive(mode: FilterMode, predicate: (Filter) -> Boolean) = filters.await().any { it.mode == mode && it.enable && predicate(it) }
    private fun <R> Filter.launchOps(
        callback: ((R) -> Unit)? = null,
        ops: suspend Filter.() -> R,
    ) = launch { ops().let { callback?.invoke(it) } }
    fun Filter.remember(callback: ((Boolean) -> Unit)? = null) = launchOps(callback) {
        EhDB.addFilter(this).also { if (it) filters.await().add(this) }
    }
    fun Filter.trigger(callback: ((Unit) -> Unit)? = null) = launchOps(callback) {
        enable = !enable
        EhDB.updateFilter(this)
    }
    fun Filter.forget(callback: ((Unit) -> Unit)? = null) = launchOps(callback) {
        EhDB.deleteFilter(this)
        filters.await().remove(this)
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

    suspend fun needTags() = filters.await().any { it.enable && (it.mode == FilterMode.TAG || it.mode == FilterMode.TAG_NAMESPACE) }
    suspend fun filterTitle(info: GalleryInfo) = anyActive(FilterMode.TITLE) { info.title.orEmpty().contains(it.text, true) }
    suspend fun filterUploader(info: GalleryInfo) = anyActive(FilterMode.UPLOADER) { it.text == info.uploader }
    suspend fun filterTag(info: GalleryInfo) = info.simpleTags?.any { tag -> anyActive(FilterMode.TAG) { matchTag(tag, it.text.lowercase()) } } ?: false
    suspend fun filterTagNamespace(info: GalleryInfo) = info.simpleTags?.any { tag -> anyActive(FilterMode.TAG_NAMESPACE) { matchTagNamespace(tag, it.text.lowercase()) } } ?: false
    suspend fun filterCommenter(commenter: String) = anyActive(FilterMode.COMMENTER) { it.text == commenter }
    suspend fun filterComment(comment: String) = anyActive(FilterMode.COMMENT) { regex(it).containsMatchIn(comment) }
}
