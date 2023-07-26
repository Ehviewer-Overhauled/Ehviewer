package com.hippo.ehviewer.coil

import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.hippo.ehviewer.spider.awaitBodyFully
import com.hippo.ehviewer.spider.cronetRequest
import com.hippo.ehviewer.spider.execute
import java.io.RandomAccessFile

class CronetHttpUriFetcher(val data: String, val options: Options, val imageLoader: ImageLoader) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val diskCacheKey = options.diskCacheKey ?: data
        val diskCache = requireNotNull(imageLoader.diskCache)
        val snapshot = diskCache.openSnapshot(diskCacheKey) ?: kotlin.run {
            cronetRequest(data) {
                disableCache()
            }.execute {
                val success = diskCache.suspendEdit(diskCacheKey) {
                    RandomAccessFile(data.toFile(), "rw").use { f ->
                        val chan = f.channel
                        awaitBodyFully { buffer ->
                            chan.write(buffer)
                        }
                    }
                }
                check(success)
            }
            requireNotNull(diskCache.openSnapshot(diskCacheKey))
        }
        val src = ImageSource(snapshot.data, diskCache.fileSystem, diskCacheKey, snapshot)
        return SourceResult(
            source = src,
            mimeType = getMimeType(data),
            dataSource = DataSource.DISK,
        )
    }
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
