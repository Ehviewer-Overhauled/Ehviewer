package com.hippo.ehviewer.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.AttributeSet
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.image.Image
import com.hippo.unifile.UniFile
import com.hippo.util.pickVisualMedia
import kotlinx.coroutines.launch

class ImageSearchLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {
    private var uss by mutableStateOf(false)
    private var osc by mutableStateOf(false)
    private var path by mutableStateOf("")

    @Composable
    override fun Content() {
        Mdc3Theme {
            val coroutineScope = rememberCoroutineScope()
            fun selectImage() = coroutineScope.launch {
                pickVisualMedia(ImageOnly)?.let {
                    path = it.toString()
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Card(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    val maxSize = dimensionResource(id = R.dimen.image_search_max_size)
                    if (path.isNotBlank()) {
                        AsyncImage(
                            model = path,
                            contentDescription = null,
                            modifier = Modifier.sizeIn(maxWidth = maxSize, maxHeight = maxSize),
                        )
                    }
                }
                FilledTonalButton(
                    onClick = ::selectImage,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                ) {
                    Text(text = stringResource(id = R.string.select_image))
                }
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    Row {
                        Row(modifier = Modifier.weight(1f)) {
                            Checkbox(checked = uss, onCheckedChange = { uss = it })
                            Text(text = stringResource(id = R.string.search_uss), modifier = Modifier.align(Alignment.CenterVertically))
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            Checkbox(checked = osc, onCheckedChange = { osc = it })
                            Text(text = stringResource(id = R.string.search_osc), modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                }
            }
        }
    }

    fun formatListUrlBuilder(lub: ListUrlBuilder) {
        if (path.isBlank()) throw EhException(context.getString(R.string.select_image_first))
        val uri = Uri.parse(path)
        val src = UniFile.fromUri(context, uri).imageSource
        val temp = AppConfig.createTempFile() ?: return
        val bitmap = ImageDecoder.decodeBitmap(src, Image.imageSearchDecoderSampleListener)
        temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        lub.imagePath = temp.path
        lub.isUseSimilarityScan = uss
        lub.isOnlySearchCovers = osc
    }
}
