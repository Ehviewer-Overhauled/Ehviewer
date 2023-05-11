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

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.widget.GalleryInfoGridItem
import com.hippo.ehviewer.ui.widget.GalleryInfoListItem
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
    private val showPages = Settings.showGalleryPages

    override fun bind(galleryInfo: GalleryInfo, onClick: () -> Unit, onLongClick: () -> Unit) {
        composeView.setMD3Content {
            GalleryInfoListItem(
                onClick = onClick,
                onLongClick = onLongClick,
                info = galleryInfo,
                modifier = Modifier.height(height),
                isInFavScene = !showFavourited,
                showPages = showPages,
            )
        }
    }
}

class GridGalleryHolder(private val composeView: ComposeView) : GalleryHolder(composeView) {
    override fun bind(galleryInfo: GalleryInfo, onClick: () -> Unit, onLongClick: () -> Unit) {
        composeView.setMD3Content {
            GalleryInfoGridItem(
                onClick = onClick,
                onLongClick = onLongClick,
                info = galleryInfo,
                modifier = Modifier.padding(2.dp),
            )
        }
    }
}
