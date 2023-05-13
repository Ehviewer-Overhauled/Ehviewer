package com.hippo.ehviewer.widget

import android.content.Context
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.util.pickVisualMedia
import kotlinx.coroutines.launch

class ImageSearchLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {
    @Composable
    override fun Content() {
        Mdc3Theme {
            val coroutineScope = rememberCoroutineScope()
            var uss by rememberSaveable { mutableStateOf(false) }
            var osc by rememberSaveable { mutableStateOf(false) }
            var path by rememberSaveable { mutableStateOf("") }

            fun selectImage() = coroutineScope.launch {
                path = pickVisualMedia(ImageOnly).toString()
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
    }
}
