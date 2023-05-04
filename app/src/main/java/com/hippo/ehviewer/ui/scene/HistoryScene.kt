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
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import arrow.core.partially1
import com.hippo.app.BaseDialogBuilder
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.addToFavorites
import com.hippo.ehviewer.ui.dialog.SelectItemWithIconAdapter
import com.hippo.ehviewer.ui.removeFromFavorites
import com.hippo.ehviewer.ui.widget.Deferred
import com.hippo.ehviewer.ui.widget.LazyColumnWithScrollBar
import com.hippo.ehviewer.ui.widget.ListInfoCard
import com.hippo.ehviewer.ui.widget.setMD3Content
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.pxToDp
import kotlinx.coroutines.delay
import moe.tarsin.coroutines.runSuspendCatching

class HistoryScene : BaseScene() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(inflater.context).apply {
            setMD3Content {
                val coroutineScope = rememberCoroutineScope()
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                val historyData = remember { Pager(PagingConfig(20)) { EhDB.historyLazyList }.flow.cachedIn(viewLifecycleOwner.lifecycleScope) }.collectAsLazyPagingItems()
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        TopAppBar(
                            title = { Text(text = stringResource(id = R.string.history)) },
                            navigationIcon = {
                                IconButton(onClick = ::onNavigationClick) {
                                    Icon(imageVector = Icons.Default.Menu, contentDescription = null)
                                }
                            },
                            actions = {
                                IconButton(onClick = ::showClearAllDialog) {
                                    Icon(imageVector = Icons.Default.ClearAll, contentDescription = null)
                                }
                            },
                            scrollBehavior = scrollBehavior,
                        )
                    },
                ) { paddingValues ->
                    LazyColumnWithScrollBar(
                        contentPadding = paddingValues,
                    ) {
                        items(
                            count = historyData.itemCount,
                            key = historyData.itemKey(key = { item -> item.gid }),
                            contentType = historyData.itemContentType(),
                        ) { index ->
                            val info = historyData[index]
                            // TODO: item delete & add animation
                            // Bug tracker: https://issuetracker.google.com/issues/150812265
                            info?.let {
                                val dismissState = rememberDismissState(
                                    confirmValueChange = {
                                        if (it == DismissValue.DismissedToStart) {
                                            coroutineScope.launchIO {
                                                EhDB.deleteHistoryInfo(info)
                                            }
                                        }
                                        true
                                    },
                                )

                                SwipeToDismiss(
                                    state = dismissState,
                                    background = {},
                                    dismissContent = {
                                        // TODO: item delete & add animation
                                        // Bug tracker: https://issuetracker.google.com/issues/150812265
                                        ListInfoCard(
                                            onClick = ::onItemClick.partially1(it),
                                            onLongClick = ::onItemLongClick.partially1(it),
                                            info = it,
                                            modifier = Modifier.height(cardHeight),
                                        )
                                    },
                                    directions = setOf(DismissDirection.EndToStart),
                                )
                            }
                        }
                    }
                    Deferred({ delay(200) }) {
                        if (historyData.itemCount == 0) {
                            NoHistory(modifier = Modifier.padding(paddingValues))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NoHistory(
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.big_history),
                contentDescription = null,
                Modifier.padding(16.dp),
            )
            Text(
                text = stringResource(id = R.string.no_history),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }

    private fun onNavigationClick() {
        toggleDrawer(GravityCompat.START)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showClearAllDialog() {
        BaseDialogBuilder(requireContext())
            .setMessage(R.string.clear_all_history)
            .setPositiveButton(R.string.clear_all) { _: DialogInterface?, which: Int ->
                if (DialogInterface.BUTTON_POSITIVE != which) {
                    return@setPositiveButton
                }
                lifecycleScope.launchIO {
                    EhDB.clearHistoryInfo()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onItemClick(gi: GalleryInfo) {
        val args = Bundle()
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO)
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, gi)
        navigate(R.id.galleryDetailScene, args)
    }

    private fun onItemLongClick(gi: GalleryInfo) {
        val context = requireContext()
        val activity = mainActivity ?: return
        val downloaded = DownloadManager.getDownloadState(gi.gid) != DownloadInfo.STATE_INVALID
        val favourite = gi.favoriteSlot != -2
        val items = if (downloaded) {
            arrayOf<CharSequence>(
                context.getString(R.string.read),
                context.getString(R.string.delete_downloads),
                context.getString(if (favourite) R.string.remove_from_favourites else R.string.add_to_favourites),
                context.getString(R.string.download_move_dialog_title),
            )
        } else {
            arrayOf<CharSequence>(
                context.getString(R.string.read),
                context.getString(R.string.download),
                context.getString(if (favourite) R.string.remove_from_favourites else R.string.add_to_favourites),
            )
        }
        val icons = if (downloaded) {
            intArrayOf(
                R.drawable.v_book_open_x24,
                R.drawable.v_delete_x24,
                if (favourite) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24,
                R.drawable.v_folder_move_x24,
            )
        } else {
            intArrayOf(
                R.drawable.v_book_open_x24,
                R.drawable.v_download_x24,
                if (favourite) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24,
            )
        }
        BaseDialogBuilder(context)
            .setTitle(EhUtils.getSuitableTitle(gi))
            .setAdapter(
                SelectItemWithIconAdapter(
                    context,
                    items,
                    icons,
                ),
            ) { _: DialogInterface?, which: Int ->
                when (which) {
                    0 -> {
                        val intent = Intent(activity, ReaderActivity::class.java)
                        intent.action = ReaderActivity.ACTION_EH
                        intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, gi)
                        startActivity(intent)
                    }

                    1 -> if (downloaded) {
                        BaseDialogBuilder(context)
                            .setTitle(R.string.download_remove_dialog_title)
                            .setMessage(
                                getString(
                                    R.string.download_remove_dialog_message,
                                    gi.title,
                                ),
                            )
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                DownloadManager.deleteDownload(gi.gid)
                            }
                            .show()
                    } else {
                        CommonOperations.startDownload(activity, gi, false)
                    }

                    2 -> if (favourite) {
                        lifecycleScope.launchIO {
                            runSuspendCatching {
                                removeFromFavorites(gi)
                                showTip(R.string.remove_from_favorite_success, LENGTH_SHORT)
                            }.onFailure {
                                showTip(R.string.remove_from_favorite_failure, LENGTH_LONG)
                            }
                        }
                    } else {
                        lifecycleScope.launchIO {
                            runSuspendCatching {
                                requireContext().addToFavorites(gi)
                                showTip(R.string.add_to_favorite_success, LENGTH_SHORT)
                            }.onFailure {
                                showTip(R.string.add_to_favorite_failure, LENGTH_LONG)
                            }
                        }
                    }

                    3 -> {
                        val labelRawList = DownloadManager.labelList
                        val labelList: MutableList<String> = ArrayList(labelRawList.size + 1)
                        labelList.add(getString(R.string.default_download_label_name))
                        var i = 0
                        val n = labelRawList.size
                        while (i < n) {
                            labelRawList[i].label?.let { labelList.add(it) }
                            i++
                        }
                        val labels = labelList.toTypedArray()
                        val helper = MoveDialogHelper(labels, gi)
                        BaseDialogBuilder(context)
                            .setTitle(R.string.download_move_dialog_title)
                            .setItems(labels, helper)
                            .show()
                    }
                }
            }.show()
    }

    private inner class MoveDialogHelper(
        private val mLabels: Array<String>,
        private val mGi: GalleryInfo,
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            val downloadManager = DownloadManager
            val downloadInfo = downloadManager.getDownloadInfo(mGi.gid) ?: return
            val label = if (which == 0) null else mLabels[which]
            downloadManager.changeLabel(listOf(downloadInfo), label)
        }
    }

    private val cardHeight = (Settings.listThumbSize * 3).pxToDp.dp
}
