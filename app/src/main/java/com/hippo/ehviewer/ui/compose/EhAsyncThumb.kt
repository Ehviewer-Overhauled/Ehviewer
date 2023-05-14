package com.hippo.ehviewer.ui.compose

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
import com.hippo.ehviewer.coil.imageRequest

@Composable
@ReadOnlyComposable
fun requestOf(model: String?): ImageRequest {
    return LocalContext.current.imageRequest(model)
}

@Composable
fun EhAsyncThumb(
    model: String?,
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
    model: String?,
    modifier: Modifier = Modifier,
) {
    var contentScale by remember { mutableStateOf(ContentScale.Fit) }
    AsyncImage(
        model = requestOf(model),
        contentDescription = null,
        modifier = modifier,
        onState = {
            if (it is AsyncImagePainter.State.Success) {
                it.result.drawable.run {
                    if (intrinsicWidth.toFloat() / intrinsicHeight in 0.5..0.8) {
                        if (contentScale == ContentScale.Fit) contentScale = ContentScale.Crop
                    }
                }
            }
        },
        contentScale = contentScale,
    )
}
