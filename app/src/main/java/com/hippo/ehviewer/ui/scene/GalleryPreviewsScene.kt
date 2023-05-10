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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMissedOutgoing
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import arrow.core.partially1
import coil.imageLoader
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.coil.imageRequest
import com.hippo.ehviewer.ui.widget.CrystalCard
import com.hippo.ehviewer.ui.widget.EhAsyncPreview
import com.hippo.ehviewer.ui.widget.rememberDialogState
import com.hippo.ehviewer.ui.widget.setMD3Content
import com.hippo.util.getParcelableCompat
import com.hippo.widget.recyclerview.getSpanCountForSuitableSize
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import java.util.Locale
import kotlin.math.roundToInt

class GalleryPreviewsScene : BaseScene() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(inflater.context).apply {
            setMD3Content {
                val galleryDetail = rememberSaveable { requireArguments().getParcelableCompat<GalleryDetail>(KEY_GALLERY_DETAIL)!! }
                fun onPreviewCLick(index: Int) = mainActivity!!.startReaderActivity(galleryDetail, index)
                val toNextPage = rememberSaveable { requireArguments().getBoolean(KEY_NEXT_PAGE) }
                val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()
                // Padding is not subtracted here to have the same column count as gallery list and preview
                val totalSpace = LocalConfiguration.current.screenWidthDp
                val columnCount = getSpanCountForSuitableSize(totalSpace, Settings.thumbSizeDp)
                val state = rememberLazyGridState()
                val dialogState = rememberDialogState()
                dialogState.Handler()
                val coroutineScope = rememberCoroutineScope()
                val pages = galleryDetail.pages
                val pgSize = galleryDetail.previewList.size

                suspend fun showGoToDialog() {
                    val goto = dialogState.show(initial = 1, title = R.string.go_to) {
                        var jumpTo by remember { mutableStateOf(1f) }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp)) {
                            Text(text = String.format(Locale.US, "%d", 1), modifier = Modifier.padding(12.dp))
                            Slider(
                                value = jumpTo,
                                onValueChange = { jumpTo = it },
                                modifier = Modifier.height(48.dp).width(0.dp).weight(1F).align(Alignment.CenterVertically),
                                valueRange = 1f..galleryDetail.previewPages.toFloat(),
                                steps = galleryDetail.previewPages - 2,
                                onValueChangeFinished = {
                                    this@show shouldReturn jumpTo.roundToInt()
                                },
                            )
                            Text(text = String.format(Locale.US, "%d", pages), modifier = Modifier.padding(12.dp))
                        }
                    }
                    state.scrollToItem((goto - 1) * pgSize)
                }

                // No Refresh support
                val source = remember {
                    object : PagingSource<Int, GalleryPreview>() {
                        override fun getRefreshKey(state: PagingState<Int, GalleryPreview>) = null
                        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryPreview> {
                            val key = params.key
                            if (key == null) {
                                return LoadResult.Page(galleryDetail.previewList, null, if (galleryDetail.previewPages == 1) null else pgSize, 0, pages - pgSize)
                            } else {
                                val page = key / pgSize
                                val result = runSuspendCatching { withIOContext { getPreviewListByPage(galleryDetail, page) } }.onFailure { return LoadResult.Error(it) }.getOrThrow()
                                val more = pages - result.size - key
                                return LoadResult.Page(result, key - 1, if (more == 0) null else key + result.size, 0, more)
                            }
                        }
                        override val jumpingSupported = true
                    }
                }

                val data = remember { Pager(PagingConfig(pgSize)) { source }.flow.cachedIn(lifecycleScope) }.collectAsLazyPagingItems()
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
                                if (galleryDetail.previewPages > 1) {
                                    IconButton(onClick = {
                                        coroutineScope.launch { showGoToDialog() }
                                    }) {
                                        Icon(imageVector = Icons.Default.CallMissedOutgoing, contentDescription = null)
                                    }
                                }
                            },
                            scrollBehavior = scrollBehaviour,
                        )
                    },
                ) { paddingValues ->
                    LaunchedEffect(toNextPage) {
                        delay(500) // Should we wait this animation?
                        if (toNextPage) state.scrollToItem(pgSize)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
                        state = state,
                        contentPadding = paddingValues,
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.gallery_grid_margin_h)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.gallery_grid_margin_v)),
                    ) {
                        items(
                            count = data.itemCount,
                            key = data.itemKey { item -> item.position },
                            contentType = data.itemContentType(),
                        ) {
                            data[it]?.run {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CrystalCard(
                                            onClick = ::onPreviewCLick.partially1(position),
                                            modifier = Modifier.fillMaxWidth().aspectRatio(0.6666667F),
                                        ) {
                                            EhAsyncPreview(
                                                model = this@run,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                    Text((position + 1).toString())
                                }
                            }
                        }
                    }
                }
            }
        }
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

    companion object {
        const val KEY_GALLERY_DETAIL = "gallery_detail"
        const val KEY_NEXT_PAGE = "next_page"
    }
}
