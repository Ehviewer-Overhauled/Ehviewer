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

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.Settings.listThumbSize
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.main.GalleryInfoGridItem
import com.hippo.ehviewer.ui.main.GalleryInfoListItem
import eu.kanade.tachiyomi.util.system.pxToDp

abstract class GalleryHolder(composeView: CheckableComposeView) : RecyclerView.ViewHolder(composeView) {
    var galleryId = 0L

    abstract fun bind(
        galleryInfo: GalleryInfo,
        isChecked: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
    )
}

class ListGalleryHolder(private val composeView: CheckableComposeView, private val showFavourite: Boolean) : GalleryHolder(composeView) {

    private val height = (3 * listThumbSize * 3).pxToDp.dp
    private val showPages = Settings.showGalleryPages

    override fun bind(galleryInfo: GalleryInfo, isChecked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
        composeView.isChecked = isChecked
        composeView.setMD3Content {
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

class GridGalleryHolder(private val composeView: CheckableComposeView) : GalleryHolder(composeView) {
    override fun bind(galleryInfo: GalleryInfo, isChecked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
        composeView.isChecked = isChecked
        composeView.setMD3Content {
            GalleryInfoGridItem(
                onClick = onClick,
                onLongClick = onLongClick,
                info = galleryInfo,
            )
        }

        // Workaround https://github.com/Ehviewer-Overhauled/Ehviewer/issues/1023
        // See https://issuetracker.google.com/issues/240449681
        composeView.getChildAt(0)?.requestLayout()
    }
}

class CheckableComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr), Checkable {

    private val content = mutableStateOf<(@Composable (Boolean) -> Unit)?>(null)

    private var mChecked by mutableStateOf(false)

    @Suppress("RedundantVisibilityModifier")
    protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @Composable
    override fun Content() {
        content.value?.invoke(mChecked)
    }

    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

    private fun setContent(content: @Composable () -> Unit) {
        shouldCreateCompositionOnAttachedToWindow = true
        this.content.value = { selected ->
            Mdc3Theme {
                Box {
                    content()
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.TopEnd),
                        )
                    }
                }
            }
        }
        if (isAttachedToWindow) {
            createComposition()
        }
    }

    override fun setChecked(value: Boolean) {
        mChecked = value
    }

    override fun isChecked() = mChecked

    override fun toggle() {
        mChecked = !mChecked
    }

    fun setMD3Content(content: @Composable () -> Unit) = setContent(content)
}
