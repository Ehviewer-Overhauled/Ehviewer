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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMissedOutgoing
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import arrow.core.partially1
import coil.imageLoader
import com.hippo.app.BaseDialogBuilder
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.coil.imageRequest
import com.hippo.ehviewer.databinding.DialogGoToBinding
import com.hippo.ehviewer.ui.widget.CrystalCard
import com.hippo.ehviewer.ui.widget.EhAsyncPreview
import com.hippo.ehviewer.ui.widget.setMD3Content
import com.hippo.util.getParcelableCompat
import com.hippo.widget.ContentLayout
import com.hippo.widget.recyclerview.getSpanCountForSuitableSize
import com.hippo.widget.rememberContentState
import eu.kanade.tachiyomi.util.lang.launchIO
import java.util.Locale

class GalleryPreviewsScene : BaseScene() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(inflater.context).apply {
            setMD3Content {
                val galleryDetail = rememberSaveable { requireArguments().getParcelableCompat<GalleryDetail>(KEY_GALLERY_DETAIL)!! }
                fun onPreviewCLick(index: Int) = mainActivity!!.startReaderActivity(galleryDetail, index)
                val toNextPage = rememberSaveable { requireArguments().getBoolean(KEY_NEXT_PAGE) }
                val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()
                val showJumpMenuButton by rememberSaveable { mutableStateOf(false) }
                // Padding is not subtracted here to have the same column count as gallery list and preview
                val totalSpace = LocalConfiguration.current.screenWidthDp
                val columnCount = getSpanCountForSuitableSize(totalSpace, Settings.thumbSizeDp)

                val state = rememberContentState(galleryDetail.previewList)
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.gallery_previews)) },
                            navigationIcon = {
                                IconButton(onClick = { findNavController().popBackStack() }) {
                                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                                }
                            },
                            actions = {
                                if (showJumpMenuButton) {
                                    IconButton(onClick = ::showGoto) {
                                        Icon(imageVector = Icons.Default.CallMissedOutgoing, contentDescription = null)
                                    }
                                }
                            },
                            scrollBehavior = scrollBehaviour,
                        )
                    },
                ) {
                    ContentLayout(state, getData = { page -> getPreviewListByPage(galleryDetail, page) }, modifier = Modifier.padding(top = it.calculateTopPadding())) { data: Array<GalleryPreview> ->
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnCount),
                            modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
                            contentPadding = it,
                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.gallery_grid_margin_h)),
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.gallery_grid_margin_v)),
                        ) {
                            items(data) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CrystalCard(
                                            onClick = ::onPreviewCLick.partially1(it.position),
                                            modifier = Modifier.fillMaxWidth().aspectRatio(0.6666667F),
                                        ) {
                                            EhAsyncPreview(
                                                model = it,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                    Text((it.position + 1).toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showGoto() {
        /*
        val pages = mHelper!!.pages
        if (pages > 1 && mHelper!!.canGoTo()) {
            showGoToDialog(requireContext(), pages, mHelper!!.pageForBottom)
        }

         */
    }

    private suspend fun getPreviewListByPage(galleryDetail: GalleryDetail, page: Int): List<GalleryPreview> {
        galleryDetail.run {
            val url = EhUrl.getGalleryDetailUrl(gid, token, page, false)
            val result = EhEngine.getPreviewList(url)
            if (Settings.preloadThumbAggressively) {
                lifecycleScope.launchIO { context?.run { result.first.first.forEach { imageLoader.enqueue(imageRequest(it)) } } }
            }
            return result.first.first
        }
    }

    private fun showGoToDialog(context: Context, pages: Int, currentPage: Int) {
        val dialogBinding = DialogGoToBinding.inflate(layoutInflater)
        dialogBinding.start.text = String.format(Locale.US, "%d", 1)
        dialogBinding.end.text = String.format(Locale.US, "%d", pages)
        dialogBinding.slider.valueTo = pages.toFloat()
        dialogBinding.slider.value = (currentPage + 1).toFloat()
        val dialog = BaseDialogBuilder(context)
            .setTitle(R.string.go_to)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val page = (dialogBinding.slider.value - 1).toInt()
                // mHelper!!.goTo(page)
            }
            .create()
        dialog.show()
    }

    companion object {
        const val KEY_GALLERY_DETAIL = "gallery_detail"
        const val KEY_NEXT_PAGE = "next_page"
    }
}
