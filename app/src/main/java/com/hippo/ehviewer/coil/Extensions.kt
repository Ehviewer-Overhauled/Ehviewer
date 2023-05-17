package com.hippo.ehviewer.coil

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.disk.DiskCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.getUrlByThumbKey
import com.hippo.ehviewer.client.url

fun ImageRequest.Builder.ehUrl(key: String) = apply {
    data(getUrlByThumbKey(key))
    memoryCacheKey(key)
    diskCacheKey(key)
    size(Size.ORIGINAL)
}

fun ImageRequest.Builder.ehPreviewKey(key: GalleryPreview) = apply {
    data(key.url)
    memoryCacheKey(key.imageKey)
    diskCacheKey(key.imageKey)
    size(Size.ORIGINAL)
}

private val stubResult = DecodeResult(ColorDrawable(Color.BLACK), false)
private val stubFactory = Decoder { stubResult }

fun ImageRequest.Builder.justDownload() = apply {
    memoryCachePolicy(CachePolicy.DISABLED)
    decoderFactory { _, _, _ -> stubFactory }
}

inline fun Context.imageRequest(key: GalleryPreview, builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(this).ehPreviewKey(key).apply(builder).build()
inline fun Context.imageRequest(key: String, builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(this).ehUrl(key).apply(builder).build()
inline fun diskCache(builder: DiskCache.Builder.() -> Unit) = DiskCache.Builder().apply(builder).build()
