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

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.View
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.hippo.drawable.TriangleDrawable
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.databinding.ItemGalleryGridBinding
import com.hippo.ehviewer.databinding.ItemGalleryListBinding
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.widget.EhAsyncThumb
import com.hippo.ehviewer.ui.widget.setMD3Content
import eu.kanade.tachiyomi.util.system.pxToDp

internal abstract class GalleryHolder(binding: ViewBinding) :
    RecyclerView.ViewHolder(binding.root) {
    abstract fun bind(galleryInfo: GalleryInfo)
}

internal class ListGalleryHolder(
    private val binding: ItemGalleryListBinding,
    private val showFavourited: Boolean,
) : GalleryHolder(binding) {

    private val height = (Settings.listThumbSize * 3).pxToDp.dp

    @SuppressLint("SetTextI18n")
    override fun bind(galleryInfo: GalleryInfo) {
        binding.run {
            thumb.setMD3Content {
                Card {
                    EhAsyncThumb(
                        model = galleryInfo.thumb,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .height(height)
                            .aspectRatio(0.6666667F),
                    )
                }
            }
            title.text = EhUtils.getSuitableTitle(galleryInfo)
            uploader.text = galleryInfo.uploader
            uploader.alpha = if (galleryInfo.disowned) .5f else 1f
            rating.rating = galleryInfo.rating
            val newCategoryText = EhUtils.getCategory(galleryInfo.category)
            if (newCategoryText != category.text.toString()) {
                category.text = newCategoryText
                category.setBackgroundColor(EhUtils.getCategoryColor(galleryInfo.category))
            }
            posted.text = galleryInfo.posted
            if (galleryInfo.pages == 0 || !Settings.showGalleryPages) {
                pages.text = null
                pages.visibility = View.GONE
            } else {
                pages.text = "${galleryInfo.pages}P"
                pages.visibility = View.VISIBLE
            }
            if (TextUtils.isEmpty(galleryInfo.simpleLanguage)) {
                simpleLanguage.text = null
                simpleLanguage.visibility = View.GONE
            } else {
                simpleLanguage.text = galleryInfo.simpleLanguage
                simpleLanguage.visibility = View.VISIBLE
            }
            favourited.visibility =
                if (showFavourited && galleryInfo.favoriteSlot >= -1 && galleryInfo.favoriteSlot <= 10) View.VISIBLE else View.GONE
            downloaded.visibility =
                if (DownloadManager.containDownloadInfo(galleryInfo.gid)) View.VISIBLE else View.GONE
        }
    }
}

internal class GridGalleryHolder(private val binding: ItemGalleryGridBinding) :
    GalleryHolder(binding) {
    override fun bind(galleryInfo: GalleryInfo) {
        binding.run {
            val aspect = (galleryInfo.thumbWidth.toFloat() / galleryInfo.thumbHeight).coerceIn(0.33F, 1.5F)
            thumb.setMD3Content {
                EhAsyncThumb(
                    model = galleryInfo.thumb,
                    modifier = Modifier.aspectRatio(aspect),
                    contentScale = ContentScale.Crop,
                )
            }
            var drawable = category.background
            val color = EhUtils.getCategoryColor(galleryInfo.category)
            if (drawable !is TriangleDrawable) {
                drawable = TriangleDrawable(color)
                category.background = drawable
            } else {
                drawable.setColor(color)
            }
            simpleLanguage.text = galleryInfo.simpleLanguage
        }
    }
}
