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
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingDataAdapter
import androidx.paging.cachedIn
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.hippo.app.BaseDialogBuilder
import com.hippo.easyrecyclerview.HandlerDrawable
import com.hippo.ehviewer.*
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.HistoryInfo
import com.hippo.ehviewer.databinding.SceneHistoryBinding
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadManager.DownloadInfoListener
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.dialog.SelectItemWithIconAdapter
import com.hippo.ehviewer.ui.widget.ListInfoCard
import com.hippo.view.ViewTransition
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rikka.core.res.resolveColor

@SuppressLint("NotifyDataSetChanged")
class HistoryScene : BaseToolbarScene() {
    private var _binding: SceneHistoryBinding? = null
    private val binding get() = _binding!!
    private val mAdapter: HistoryAdapter by lazy {
        HistoryAdapter(object : DiffUtil.ItemCallback<HistoryInfo>() {
            override fun areItemsTheSame(oldItem: HistoryInfo, newItem: HistoryInfo): Boolean {
                return oldItem.gid == newItem.gid
            }

            override fun areContentsTheSame(oldItem: HistoryInfo, newItem: HistoryInfo): Boolean {
                return oldItem.gid == newItem.gid
            }
        })
    }
    private val mDownloadManager = DownloadManager
    private val mDownloadInfoListener: DownloadInfoListener by lazy {
        object : DownloadInfoListener {
            override fun onAdd(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
                mAdapter.notifyDataSetChanged()
            }

            override fun onUpdate(info: DownloadInfo, list: List<DownloadInfo>) {}
            override fun onUpdateAll() {}
            override fun onReload() {
                mAdapter.notifyDataSetChanged()
            }

            override fun onChange() {
                mAdapter.notifyDataSetChanged()
            }

            override fun onRenameLabel(from: String, to: String) {}
            override fun onRemove(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
                mAdapter.notifyDataSetChanged()
            }

            override fun onUpdateLabels() {}
        }
    }
    private val mFavouriteStatusRouter = EhApplication.favouriteStatusRouter
    private val mFavouriteStatusRouterListener: FavouriteStatusRouter.Listener by lazy {
        FavouriteStatusRouter.Listener { _: Long, _: Int ->
            mAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDownloadManager.removeDownloadInfoListener(mDownloadInfoListener)
        mFavouriteStatusRouter.removeListener(mFavouriteStatusRouterListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDownloadManager.addDownloadInfoListener(mDownloadInfoListener)
        mFavouriteStatusRouter.addListener(mFavouriteStatusRouterListener)
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SceneHistoryBinding.inflate(inflater, container, false)
        setLiftOnScrollTargetView(binding.recyclerView)
        val mViewTransition = ViewTransition(binding.content, binding.tip)
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.big_history)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        binding.tip.setCompoundDrawables(null, drawable, null, null)
        val historyData = Pager(
            PagingConfig(20)
        ) {
            EhDB.historyLazyList
        }.flow.cachedIn(viewLifecycleOwner.lifecycleScope)
        binding.recyclerView.adapter = mAdapter
        val layoutManager = AutoStaggeredGridLayoutManager(0, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.setColumnSize(resources.getDimensionPixelOffset(Settings.detailSizeResId))
        layoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE)
        binding.recyclerView.layoutManager = layoutManager
        val itemTouchHelper = ItemTouchHelper(HistoryItemTouchHelperCallback())
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
        binding.fastScroller.attachToRecyclerView(binding.recyclerView)
        val handlerDrawable = HandlerDrawable()
        handlerDrawable.setColor(theme.resolveColor(com.google.android.material.R.attr.colorPrimary))
        binding.fastScroller.setHandlerDrawable(handlerDrawable)
        lifecycleScope.launchIO {
            historyData.collectLatest { value ->
                mAdapter.submitData(
                    value
                )
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            mAdapter.onPagesUpdatedFlow.collectLatest {
                if (mAdapter.itemCount == 0) {
                    mViewTransition.showView(1, true)
                } else {
                    mViewTransition.showView(0, true)
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(R.string.history)
        setNavigationIcon(R.drawable.ic_baseline_menu_24)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.stopScroll()
        _binding = null
    }

    override fun onNavigationClick() {
        toggleDrawer(GravityCompat.START)
    }

    override fun getMenuResId(): Int {
        return R.menu.scene_history
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
                    withUIContext {
                        mAdapter.refresh()
                        mAdapter.notifyDataSetChanged()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_clear_all) {
            showClearAllDialog()
            return true
        }
        return false
    }

    fun onItemClick(gi: GalleryInfo): Boolean {
        val args = Bundle()
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO)
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, gi)
        navigate(R.id.galleryDetailScene, args)
        return true
    }

    fun onItemLongClick(gi: GalleryInfo): Boolean {
        val context = requireContext()
        val activity = mainActivity ?: return false
        val downloaded = mDownloadManager.getDownloadState(gi.gid) != DownloadInfo.STATE_INVALID
        val favourite = gi.favoriteSlot != -2
        val items = if (downloaded) arrayOf<CharSequence>(
            context.getString(R.string.read),
            context.getString(R.string.delete_downloads),
            context.getString(if (favourite) R.string.remove_from_favourites else R.string.add_to_favourites),
            context.getString(R.string.download_move_dialog_title)
        ) else arrayOf<CharSequence>(
            context.getString(R.string.read),
            context.getString(R.string.download),
            context.getString(if (favourite) R.string.remove_from_favourites else R.string.add_to_favourites),
        )
        val icons = if (downloaded) intArrayOf(
            R.drawable.v_book_open_x24,
            R.drawable.v_delete_x24,
            if (favourite) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24,
            R.drawable.v_folder_move_x24
        ) else intArrayOf(
            R.drawable.v_book_open_x24,
            R.drawable.v_download_x24,
            if (favourite) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24,
        )
        BaseDialogBuilder(context)
            .setTitle(EhUtils.getSuitableTitle(gi))
            .setAdapter(
                SelectItemWithIconAdapter(
                    context,
                    items,
                    icons
                )
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
                                    gi.title
                                )
                            )
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                mDownloadManager.deleteDownload(
                                    gi.gid
                                )
                            }
                            .show()
                    } else {
                        CommonOperations.startDownload(activity, gi, false)
                    }

                    2 -> if (favourite) {
                        CommonOperations.removeFromFavorites(
                            activity,
                            gi,
                            RemoveFromFavoriteListener(context)
                        )
                    } else {
                        CommonOperations.addToFavorites(
                            activity,
                            gi,
                            AddToFavoriteListener(context)
                        )
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
        return true
    }

    private class AddToFavoriteListener(context: Context) :
        EhCallback<GalleryListScene?, Void?>(context) {
        override fun onSuccess(result: Void?) {
            showTip(R.string.add_to_favorite_success, LENGTH_SHORT)
        }

        override fun onFailure(e: Exception) {
            showTip(R.string.add_to_favorite_failure, LENGTH_LONG)
        }

        override fun onCancel() {}
    }

    private class RemoveFromFavoriteListener(context: Context) :
        EhCallback<GalleryListScene?, Void?>(context) {
        override fun onSuccess(result: Void?) {
            showTip(R.string.remove_from_favorite_success, LENGTH_SHORT)
        }

        override fun onFailure(e: Exception) {
            showTip(R.string.remove_from_favorite_failure, LENGTH_LONG)
        }

        override fun onCancel() {}
    }

    private class ComposeHolder(val composeView: HistoryCardView) :
        RecyclerView.ViewHolder(composeView)

    private class HistoryCardView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : AbstractComposeView(context, attrs, defStyle) {
        var galleryInfo by mutableStateOf<GalleryInfo?>(null)
        var onClick by mutableStateOf({})
        var onLongClock by mutableStateOf({})

        @Composable
        override fun Content() {
            Mdc3Theme {
                galleryInfo?.let {
                    ListInfoCard(
                        onClick,
                        onLongClock,
                        it
                    )
                }
            }
        }
    }

    private inner class MoveDialogHelper(
        private val mLabels: Array<String>,
        private val mGi: GalleryInfo
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            val downloadManager = DownloadManager
            val downloadInfo = downloadManager.getDownloadInfo(mGi.gid) ?: return
            val label = if (which == 0) null else mLabels[which]
            downloadManager.changeLabel(listOf(downloadInfo), label)
        }
    }

    private inner class HistoryAdapter(diffCallback: DiffUtil.ItemCallback<HistoryInfo>) :
        PagingDataAdapter<HistoryInfo, ComposeHolder>(diffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposeHolder {
            val view = HistoryCardView(requireContext())
            return ComposeHolder(view)
        }

        override fun onBindViewHolder(holder: ComposeHolder, position: Int) {
            val gi: GalleryInfo = getItem(position) ?: return
            holder.composeView.galleryInfo = gi
            holder.composeView.onClick = { onItemClick(gi) }
            holder.composeView.onLongClock = { onItemLongClick(gi) }
        }
    }

    private inner class HistoryItemTouchHelperCallback : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeMovementFlags(0, ItemTouchHelper.LEFT)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val mPosition = viewHolder.bindingAdapterPosition
            lifecycleScope.launchIO {
                val info: HistoryInfo? = mAdapter.peek(mPosition)
                info?.let { EhDB.deleteHistoryInfo(info) }
                withUIContext {
                    mAdapter.refresh()
                }
            }
        }
    }
}