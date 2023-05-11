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
package com.hippo.ehviewer.ui.scene

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.widget.EhAsyncThumb
import com.hippo.ehviewer.ui.widget.ListInfoCard
import com.hippo.ehviewer.ui.widget.setMD3Content
import eu.kanade.tachiyomi.util.system.pxToDp

abstract class GalleryHolder(composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
    abstract fun bind(
        galleryInfo: GalleryInfo,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
    )
}

class ListGalleryHolder(
    private val composeView: ComposeView,
    private val showFavourited: Boolean,
) : GalleryHolder(composeView) {

    private val height = (Settings.listThumbSize * 3).pxToDp.dp

    override fun bind(galleryInfo: GalleryInfo, onClick: () -> Unit, onLongClick: () -> Unit) {
        composeView.setMD3Content {
            ListInfoCard(
                onClick = onClick,
                onLongClick = onLongClick,
                info = galleryInfo,
                modifier = Modifier.height(height),
            )
        }
    }
}

class GridGalleryHolder(private val composeView: ComposeView) : GalleryHolder(composeView) {
    override fun bind(galleryInfo: GalleryInfo, onClick: () -> Unit, onLongClick: () -> Unit) {
        val aspect = (galleryInfo.thumbWidth.toFloat() / galleryInfo.thumbHeight).coerceIn(0.33F, 1.5F)
        val color = EhUtils.getCategoryColor(galleryInfo.category)
        val simpleLang = galleryInfo.simpleLanguage
        composeView.setMD3Content {
            ElevatedCard(modifier = Modifier.padding(2.dp)) {
                EhAsyncThumb(
                    model = galleryInfo.thumb,
                    modifier = Modifier.aspectRatio(aspect).fillMaxWidth().combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
