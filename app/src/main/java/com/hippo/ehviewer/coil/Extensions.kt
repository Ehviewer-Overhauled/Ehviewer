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
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.thumbUrl
import com.hippo.ehviewer.client.url

fun ImageRequest.Builder.ehUrl(info: GalleryInfo) = apply {
    val key = info.thumbKey!!
    data(info.thumbUrl)
    memoryCacheKey(key)
    diskCacheKey(key)
    size(Size.ORIGINAL)
}

fun ImageRequest.Builder.ehPreview(preview: GalleryPreview) = apply {
    data(preview.url)
    memoryCacheKey(preview.imageKey)
    diskCacheKey(preview.imageKey)
    size(Size.ORIGINAL)
}

private val stubResult = DecodeResult(ColorDrawable(Color.BLACK), false)
private val stubFactory = Decoder { stubResult }

fun ImageRequest.Builder.justDownload() = apply {
    memoryCachePolicy(CachePolicy.DISABLED)
    decoderFactory { _, _, _ -> stubFactory }
}

inline fun Context.imageRequest(preview: GalleryPreview, builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(this).ehPreview(preview).apply(builder).build()
inline fun Context.imageRequest(info: GalleryInfo, builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(this).ehUrl(info).apply(builder).build()
inline fun Context.imageRequest(builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(this).apply(builder).build()
inline fun diskCache(builder: DiskCache.Builder.() -> Unit) = DiskCache.Builder().apply(builder).build()
