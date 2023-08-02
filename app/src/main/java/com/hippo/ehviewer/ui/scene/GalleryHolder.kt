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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.Settings.listThumbSize
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import com.hippo.ehviewer.ui.setMD3Content
import eu.kanade.tachiyomi.util.system.pxToDp

abstract class GalleryHolder(composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
    var galleryId = 0L

    abstract fun bind(
        galleryInfo: GalleryInfo,
        isChecked: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
    )
}

class ListGalleryHolder(private val composeView: ComposeView, private val showFavourite: Boolean) : GalleryHolder(composeView) {

    private val height = (3 * listThumbSize * 3).pxToDp.dp
    private val showPages = Settings.showGalleryPages

    override fun bind(galleryInfo: GalleryInfo, isChecked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
        composeView.setMD3Content {
            CheckableItem(checked = isChecked) {
                GalleryInfoListItem(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    info = galleryInfo,
                    modifier = Modifier.height(height),
                    isInFavScene = !showFavourite,
                    showPages = showPages,
                )
            }
        }
    }
}

class GridGalleryHolder(private val composeView: ComposeView) : GalleryHolder(composeView) {
    override fun bind(galleryInfo: GalleryInfo, isChecked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
        composeView.setMD3Content {
            CheckableItem(checked = isChecked) {
                GalleryInfoGridItem(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    info = galleryInfo,
                )
            }
        }

        // Workaround https://github.com/Ehviewer-Overhauled/Ehviewer/issues/1023
        // See https://issuetracker.google.com/issues/240449681
        composeView.getChildAt(0)?.requestLayout()
    }
}

@Composable
fun CheckableItem(checked: Boolean, content: @Composable () -> Unit) {
    Box {
        content()
        if (checked) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}
