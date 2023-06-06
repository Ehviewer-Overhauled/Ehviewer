/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.thumbUrl
import com.hippo.ehviewer.util.tellClipboardWithToast

private const val INDEX_URL = 2
private const val INDEX_PARENT = 9

private val keys = arrayOf(
    R.string.key_gid,
    R.string.key_token,
    R.string.key_url,
    R.string.key_title,
    R.string.key_title_jpn,
    R.string.key_thumb,
    R.string.key_category,
    R.string.key_uploader,
    R.string.key_posted,
    R.string.key_parent,
    R.string.key_visible,
    R.string.key_language,
    R.string.key_pages,
    R.string.key_size,
    R.string.key_favorite_count,
    R.string.key_favorited,
    R.string.key_rating_count,
    R.string.key_rating,
    R.string.key_torrents,
    R.string.key_torrent_url,
    R.string.favorite_name,
)

@Composable
fun GalleryInfoBottomSheet(
    galleryDetail: GalleryDetail,
    onDismissRequest: () -> Unit,
) {
    val data = remember(galleryDetail) {
        galleryDetail.run {
            arrayOf(
                gid.toString(),
                token,
                EhUrl.getGalleryDetailUrl(gid, token),
                title,
                titleJpn,
                thumbUrl,
                EhUtils.getCategory(category),
                uploader,
                posted,
                parent,
                visible,
                language,
                pages.toString(),
                size,
                favoriteCount.toString(),
                isFavorited.toString(),
                ratingCount.toString(),
                rating.toString(),
                torrentCount.toString(),
                torrentUrl,
                favoriteName,
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        windowInsets = WindowInsets(0),
    ) {
        check(data.size == keys.size)
        val context = LocalContext.current
        val navController = LocalNavController.current
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(id = R.string.gallery_info), style = MaterialTheme.typography.titleLarge)
        }
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
            LazyColumn {
                itemsIndexed(keys) { index, i ->
                    val key = stringResource(i)
                    Row(
                        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.keyline_margin)).clickable {
                            if (index == INDEX_PARENT) {
                                data[index]?.let { navController.navWithUrl(it) }
                            } else {
                                context tellClipboardWithToast data[index]
                                if (index == INDEX_URL) {
                                    // Save it to avoid detect the gallery
                                    Settings.clipboardTextHashCode = data[index].hashCode()
                                }
                            }
                        }.fillMaxWidth(),
                    ) {
                        Text(key, modifier = Modifier.width(90.dp).padding(8.dp))
                        Text(data[index].orEmpty(), modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}
