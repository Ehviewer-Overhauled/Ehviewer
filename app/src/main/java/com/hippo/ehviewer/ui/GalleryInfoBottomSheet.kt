package com.hippo.ehviewer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.LOCAL_FAVORITED
import com.hippo.ehviewer.client.thumbUrl
import com.hippo.ehviewer.databinding.InfoSheetBinding
import com.hippo.ehviewer.ui.scene.navWithUrl
import com.hippo.ehviewer.util.tellClipboardWithToast

private const val INDEX_URL = 2
private const val INDEX_PARENT = 9

private fun GalleryDetail.generateContent() = arrayOf(
    R.string.key_gid to gid.toString(),
    R.string.key_token to token,
    R.string.key_url to EhUrl.getGalleryDetailUrl(gid, token),
    R.string.key_title to title,
    R.string.key_title_jpn to titleJpn,
    R.string.key_thumb to thumbUrl,
    R.string.key_category to EhUtils.getCategory(category),
    R.string.key_uploader to uploader,
    R.string.key_posted to posted,
    R.string.key_parent to parent,
    R.string.key_visible to visible,
    R.string.key_language to language,
    R.string.key_pages to pages.toString(),
    R.string.key_size to size,
    R.string.key_favorite_count to favoriteCount.toString(),
    R.string.key_favorited to (favoriteSlot > LOCAL_FAVORITED).toString(),
    R.string.key_rating_count to ratingCount.toString(),
    R.string.key_rating to rating.toString(),
    R.string.key_torrents to torrentCount.toString(),
    R.string.key_torrent_url to torrentUrl,
    R.string.favorite_name to favoriteName,
)

class GalleryInfoBottomSheet(private val detail: GalleryDetail) : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = InfoSheetBinding.inflate(layoutInflater, container, false).apply {
        content.setMD3Content {
            Column(modifier = Modifier.fillMaxWidth()) {
                val context = LocalContext.current
                val navController = remember { findNavController() }
                Text(
                    text = stringResource(id = R.string.gallery_info),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.titleLarge,
                )
                val data = remember(detail) { detail.generateContent() }
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    LazyColumn {
                        itemsIndexed(data) { index, (key, content) ->
                            Row(
                                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.keyline_margin)).clickable {
                                    if (index == INDEX_PARENT) {
                                        if (content != null) {
                                            navController.navWithUrl(content)
                                        }
                                    } else {
                                        context tellClipboardWithToast content
                                        if (index == INDEX_URL) {
                                            // Save it to avoid detect the gallery
                                            Settings.clipboardTextHashCode = data[index].hashCode()
                                        }
                                    }
                                }.fillMaxWidth(),
                            ) {
                                Text(stringResource(id = key), modifier = Modifier.width(90.dp).padding(8.dp))
                                Text(content.orEmpty(), modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }.root
}
