package com.hippo.ehviewer.ui.main

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.ui.tools.CropDefaults
import com.hippo.ehviewer.ui.tools.CrystalCard

@Composable
@ReadOnlyComposable
fun requestOf(model: GalleryPreview): ImageRequest {
    return LocalContext.current.imageRequest(model)
}

/**
 * Show a part of the original drawable, non-animated only implementation.
 */
@Composable
fun EhAsyncPreview(
    model: GalleryPreview,
    modifier: Modifier = Modifier,
) {
    var contentScale by remember(model.imageKey) { mutableStateOf(ContentScale.Fit) }
    AsyncImage(
        model = requestOf(model),
        contentDescription = null,
        modifier = modifier,
        transform = {
            model.run {
                if (it is AsyncImagePainter.State.Success && this is NormalGalleryPreview) {
                    it.copy(
                        painter = BitmapPainter(
                            (it.result.drawable as BitmapDrawable).bitmap.asImageBitmap(),
                            IntOffset(offsetX, 0),
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
                        if (CropDefaults.shouldCrop(clipWidth, clipHeight)) {
                            contentScale = ContentScale.Crop
                        }
                    } else {
                        it.result.drawable.run {
                            if (CropDefaults.shouldCrop(intrinsicWidth, intrinsicHeight)) {
                                contentScale = ContentScale.Crop
                            }
                        }
                    }
                }
            }
        },
        contentScale = contentScale,
    )
}

@Composable
fun EhPreviewItem(
    galleryPreview: GalleryPreview?,
    position: Int,
    onClick: () -> Unit,
) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(contentAlignment = Alignment.Center) {
        CrystalCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.6666667F),
        ) {
            if (galleryPreview != null) EhAsyncPreview(model = galleryPreview, modifier = Modifier.fillMaxSize())
        }
    }
    Text((position + 1).toString())
}
