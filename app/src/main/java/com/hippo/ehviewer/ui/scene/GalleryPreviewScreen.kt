package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import arrow.core.partially1
import arrow.fx.coroutines.parMap
import coil.imageLoader
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.coil.imageRequest
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.widget.EhPreviewItem
import com.hippo.ehviewer.ui.widget.rememberDialogState
import com.hippo.ehviewer.ui.widget.setMD3Content
import com.hippo.ehviewer.widget.recyclerview.calculateSuitableSpanCount
import com.hippo.util.getParcelableCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import kotlin.math.roundToInt

typealias PreviewPage = List<GalleryPreview>

class GalleryPreviewScreen : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(inflater.context).apply {
            setMD3Content {
                val galleryDetail = rememberSaveable { requireArguments().getParcelableCompat<GalleryDetail>(KEY_GALLERY_DETAIL)!! }
                val context = LocalContext.current
                fun onPreviewCLick(index: Int) = context.navToReader(galleryDetail, index)
                val toNextPage = rememberSaveable { requireArguments().getBoolean(KEY_NEXT_PAGE) }
                val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()
                val columnCount = calculateSuitableSpanCount()
                val state = rememberLazyGridState()
                val dialogState = rememberDialogState()
                dialogState.Handler()
                val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
                val pages = galleryDetail.pages
                val pgSize = galleryDetail.previewList.size
                var initialKey by rememberSaveable { mutableStateOf(if (toNextPage) 2 else 1) }

                suspend fun getPreviewListByPage(page: Int) = galleryDetail.run {
                    val url = EhUrl.getGalleryDetailUrl(gid, token, page, false)
                    val result = EhEngine.getPreviewList(url)
                    if (Settings.preloadThumbAggressively) {
                        coroutineScope.launch {
                            context.run { result.first.first.forEach { imageLoader.enqueue(imageRequest(it)) } }
                        }
                    }
                    result.first.first
                }

                suspend fun showGoToDialog() {
                    val goto = dialogState.show(initial = 1, title = R.string.go_to) {
                        var jumpTo by remember { mutableStateOf(1f) }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp)) {
                            Text(text = "1", modifier = Modifier.padding(12.dp))
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
                            Text(text = pages.toString(), modifier = Modifier.padding(12.dp))
                        }
                    }
                    initialKey = goto
                }

                val previewPagesMap = rememberSaveable { mutableMapOf<Int, PreviewPage>().apply { put(1, galleryDetail.previewList) } }
                val data = remember(initialKey) {
                    Pager(PagingConfig(1, prefetchDistance = 4, enablePlaceholders = false, initialLoadSize = 1), initialKey) {
                        object : PagingSource<Int, PreviewPage>() {
                            override fun getRefreshKey(state: PagingState<Int, PreviewPage>) = null
                            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PreviewPage> {
                                val up = params.key ?: 1
                                val end = up + params.loadSize - 1
                                runSuspendCatching {
                                    (up..end).mapNotNull { it.takeUnless { previewPagesMap.contains(it) } }
                                        .parMap(Dispatchers.IO) { getPreviewListByPage(it - 1).apply { previewPagesMap[it] = this } }
                                }.onFailure {
                                    return LoadResult.Error(it)
                                }
                                val r = (up..end).map { previewPagesMap[it]!! }
                                val prevK = if (up == 1) null else up - 1
                                val nextK = if (end == galleryDetail.previewPages) null else end + 1
                                return LoadResult.Page(r, prevK, nextK)
                            }
                            override val keyReuseSupported = true
                        }
                    }.flow.cachedIn(lifecycleScope)
                }.collectAsLazyPagingItems()

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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
                        state = state,
                        contentPadding = paddingValues,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val realList = data.itemSnapshotList.items.flatten()
                        items(
                            count = realList.size,
                            key = { realList[it].position },
                        ) { index ->
                            data[index / pgSize] // Trigger item preload
                            val item = realList[index]
                            item.position.let { ::onPreviewCLick.partially1(it) }.let { EhPreviewItem(item, it) }
                        }
                    }
                }
            }
        }
    }
}

const val KEY_GALLERY_DETAIL = "gallery_detail"
const val KEY_NEXT_PAGE = "next_page"
