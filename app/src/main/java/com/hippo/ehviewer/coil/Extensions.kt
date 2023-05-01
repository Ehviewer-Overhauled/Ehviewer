package com.hippo.ehviewer.coil

import android.content.Context
import coil.disk.DiskCache
import coil.request.ImageRequest
import coil.size.Size
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.getPreviewThumbKey
import com.hippo.ehviewer.client.url

fun ImageRequest.Builder.ehUrl(url: String?) = apply {
    url?.let {
        val key = getPreviewThumbKey(url)
        data(url)
        memoryCacheKey(key)
        diskCacheKey(key)
        size(Size.ORIGINAL)
    }
}

fun ImageRequest.Builder.ehPreviewKey(key: GalleryPreview) = apply {
    data(key.url)
    memoryCacheKey(key.imageKey)
    diskCacheKey(key.imageKey)
    size(Size.ORIGINAL)
}

inline fun Context.imageRequest(key: GalleryPreview, builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(this).ehPreviewKey(key).apply(builder).build()
inline fun Context.imageRequest(key: String?, builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(this).ehUrl(key).apply(builder).build()
inline fun diskCache(builder: DiskCache.Builder.() -> Unit) = DiskCache.Builder().apply(builder).build()
