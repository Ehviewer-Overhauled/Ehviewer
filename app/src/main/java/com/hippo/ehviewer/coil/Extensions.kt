package com.hippo.ehviewer.coil

import android.content.Context
import coil.disk.DiskCache
import coil.request.ImageRequest
import coil.size.Size
import com.hippo.ehviewer.client.getPreviewThumbKey

fun ImageRequest.Builder.ehUrl(url: String?) = apply {
    url?.let {
        val key = getPreviewThumbKey(url)
        data(url)
        memoryCacheKey(key)
        diskCacheKey(key)
        size(Size.ORIGINAL)
    }
}

inline fun Context.imageRequest(url: String?, builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(this).ehUrl(url).apply(builder).build()
inline fun diskCache(builder: DiskCache.Builder.() -> Unit) = DiskCache.Builder().apply(builder).build()
