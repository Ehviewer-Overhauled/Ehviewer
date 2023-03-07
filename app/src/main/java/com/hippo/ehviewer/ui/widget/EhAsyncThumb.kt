package com.hippo.ehviewer.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hippo.ehviewer.coil.ehUrl

@Composable
@ReadOnlyComposable
fun requestOf(model: String?): ImageRequest {
    return ImageRequest.Builder(LocalContext.current).apply {
        model?.let { ehUrl(it) }
    }.build()
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
