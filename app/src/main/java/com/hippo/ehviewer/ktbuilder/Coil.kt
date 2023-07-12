package com.hippo.ehviewer.ktbuilder

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.ImageRequest
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.coil.ehPreview
import com.hippo.ehviewer.coil.ehUrl

inline fun Context.imageRequest(preview: GalleryPreview, builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(this).ehPreview(preview).apply(builder).build()
inline fun Context.imageRequest(info: GalleryInfo, builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(this).ehUrl(info).apply(builder).build()
inline fun Context.imageRequest(builder: ImageRequest.Builder.() -> Unit = {}) = ImageRequest.Builder(this).apply(builder).build()
inline fun diskCache(builder: DiskCache.Builder.() -> Unit) = DiskCache.Builder().apply(builder).build()
inline fun Context.imageLoader(builder: ImageLoader.Builder.() -> Unit) = ImageLoader.Builder(this).apply(builder).build()
