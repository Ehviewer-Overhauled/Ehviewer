package com.hippo.ehviewer.coil

import android.content.Context
import coil.request.ImageRequest
import com.hippo.ehviewer.client.EhCacheKeyFactory

fun ImageRequest.Builder.ehUrl(url: String) = apply {
    val key = EhCacheKeyFactory.getPreviewThumbKey(url)
    data(url)
    memoryCacheKey(key)
    diskCacheKey(key)
}

inline fun Context.imageRequest(
    url: String,
    builder: ImageRequest.Builder.() -> Unit = {}
): ImageRequest {
    return ImageRequest.Builder(this)
        .ehUrl(url)
        .apply(builder)
        .build()
}