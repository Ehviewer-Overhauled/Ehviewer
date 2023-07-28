@file:Suppress("BlockingMethodInNonBlockingContext")

package com.hippo.ehviewer.coil

import android.net.Uri
import android.webkit.MimeTypeMap
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.hippo.ehviewer.cronet.copyToChannel
import com.hippo.ehviewer.cronet.cronetRequest
import com.hippo.ehviewer.cronet.execute
import java.io.RandomAccessFile

/**
 * A HttpUriFetcher impl use Cronet for jvmheapless IO
 * cacheHeader is never respected | recorded since thumb is immutable
 */
class CronetHttpUriFetcher(private val data: String, private val options: Options, private val imageLoader: ImageLoader) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val diskCacheKey = options.diskCacheKey ?: data
        val diskCache = requireNotNull(imageLoader.diskCache)
        val snapshot = diskCache.openSnapshot(diskCacheKey) ?: run {
            val success = cronetRequest(data) {
                disableCache()
            }.execute {
                diskCache.suspendEdit(diskCacheKey) {
                    RandomAccessFile(data.toFile(), "rw").use {
                        copyToChannel(it.channel)
                    }
                }
            }
            check(success)
            // diskcache snapshot MUST exist here
            requireNotNull(diskCache.openSnapshot(diskCacheKey))
        }
        return SourceResult(
            source = ImageSource(snapshot.data, diskCache.fileSystem, diskCacheKey, snapshot),
            mimeType = getMimeType(data),
            dataSource = DataSource.DISK,
        )
    }
}

fun ComponentRegistry.Builder.installCronetHttpUriFetcher() = add { data: Uri, options, loader ->
    if (data.scheme != "http" && data.scheme != "https") return@add null
    CronetHttpUriFetcher(data.toString(), options, loader)
}

private fun getMimeType(url: String) = MimeTypeMap.getSingleton().getMimeTypeFromUrl(url)

private fun MimeTypeMap.getMimeTypeFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val extension = url
        .substringBeforeLast('#') // Strip the fragment.
        .substringBeforeLast('?') // Strip the query.
        .substringAfterLast('/') // Get the last path segment.
        .substringAfterLast('.', missingDelimiterValue = "") // Get the file extension.
    return getMimeTypeFromExtension(extension)
}
