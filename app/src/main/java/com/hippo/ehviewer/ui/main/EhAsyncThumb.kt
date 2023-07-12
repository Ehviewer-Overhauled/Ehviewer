package com.hippo.ehviewer.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.ui.tools.CropDefaults

@Composable
@ReadOnlyComposable
fun requestOf(model: GalleryInfo): ImageRequest {
    return LocalContext.current.imageRequest(model)
}

@Composable
fun EhAsyncThumb(
    model: GalleryInfo,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) = AsyncImage(
    model = requestOf(model),
    contentDescription = "",
    modifier = modifier,
    contentScale = contentScale,
)

@Composable
fun EhAsyncCropThumb(
    key: GalleryInfo,
    modifier: Modifier = Modifier,
) {
    var contentScale by remember(key.gid) { mutableStateOf(ContentScale.Fit) }
    AsyncImage(
        model = requestOf(key),
        contentDescription = null,
        modifier = modifier,
        onState = {
            if (it is AsyncImagePainter.State.Success) {
                it.result.drawable.run {
                    if (CropDefaults.shouldCrop(intrinsicWidth, intrinsicHeight)) {
                        contentScale = ContentScale.Crop
                    }
                }
            }
        },
        contentScale = contentScale,
    )
}
