package com.hippo.ehviewer.ui.widget

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.data.NormalGalleryPreview
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

/**
 * Show a part of the original drawable, non-animated only implementation.
 */
@Composable
fun EhAsyncPreview(
    model: GalleryPreview,
    modifier: Modifier = Modifier,
) {
    var contentScale by remember { mutableStateOf(ContentScale.Fit) }
    AsyncImage(
        model = requestOf(model.imageUrl),
        contentDescription = "",
        modifier = modifier,
        transform = {
            model.run {
                if (it is AsyncImagePainter.State.Success && this is NormalGalleryPreview) {
                    it.copy(
                        painter = BitmapPainter(
                            (it.result.drawable as BitmapDrawable).bitmap.asImageBitmap(),
                            IntOffset(offsetX, offsetY),
                            IntSize(clipWidth - 1, clipHeight - 1),
                        ),
                    )
                } else {
                    it
                }
            }
        },
        onState = {
            if (it is AsyncImagePainter.State.Success) {
                model.run {
                    if (this is NormalGalleryPreview) {
                        if (clipWidth.toFloat() / clipHeight in 0.5..0.8) {
                            if (contentScale == ContentScale.Fit) contentScale = ContentScale.Crop
                        }
                    } else {
                        it.result.drawable.run {
                            if (intrinsicWidth.toFloat() / intrinsicHeight in 0.5..0.8) {
                                if (contentScale == ContentScale.Fit) contentScale = ContentScale.Crop
                            }
                        }
                    }
                }
            }
        },
        contentScale = contentScale,
    )
}
