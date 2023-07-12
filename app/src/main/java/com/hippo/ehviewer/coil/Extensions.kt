package com.hippo.ehviewer.coil

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.data.NormalGalleryPreview
import com.hippo.ehviewer.client.thumbUrl
import com.hippo.ehviewer.client.url

fun ImageRequest.Builder.ehUrl(info: GalleryInfo) = apply {
    val key = info.thumbKey!!
    data(info.thumbUrl)
    memoryCacheKey(key)
    diskCacheKey(key)
}

fun ImageRequest.Builder.ehPreview(preview: GalleryPreview) = apply {
    data(preview.url)
    memoryCacheKey(preview.imageKey)
    diskCacheKey(preview.imageKey)
    if (preview is NormalGalleryPreview) size(Size.ORIGINAL)
}

private val stubResult = DecodeResult(ColorDrawable(Color.BLACK), false)
private val stubFactory = Decoder { stubResult }

fun ImageRequest.Builder.justDownload() = apply {
    memoryCachePolicy(CachePolicy.DISABLED)
    decoderFactory { _, _, _ -> stubFactory }
}
