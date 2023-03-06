package com.hippo.ehviewer.coil

import android.content.Context
import coil.request.ImageRequest
import com.hippo.ehviewer.client.EhCacheKeyFactory
import com.hippo.ehviewer.client.EhUrl

fun ImageRequest.Builder.ehUrl(url: String) = apply {
    val key = EhCacheKeyFactory.getImageKey(url)
    val realUrl = EhUrl.thumbUrlPrefix + key
    data(realUrl)
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