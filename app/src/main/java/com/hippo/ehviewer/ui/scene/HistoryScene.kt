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
import android.text.TextUtils
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingDataAdapter
import androidx.paging.cachedIn
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.hippo.app.BaseDialogBuilder
import com.hippo.easyrecyclerview.FastScroller
import com.hippo.easyrecyclerview.HandlerDrawable
import com.hippo.ehviewer.*
import com.hippo.ehviewer.client.EhCacheKeyFactory
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.HistoryInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadManager.DownloadInfoListener
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.GalleryActivity
import com.hippo.ehviewer.ui.dialog.SelectItemWithIconAdapter
import com.hippo.ehviewer.widget.SimpleRatingView
import com.hippo.scene.Announcer
import com.hippo.scene.SceneFragment
import com.hippo.view.ViewTransition
import com.hippo.widget.LoadImageView
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager
import com.hippo.yorozuya.ViewUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rikka.core.res.resolveColor

class HistoryScene : ToolbarScene() {
    private var mRecyclerView: RecyclerView? = null
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
    private val mDownloadManager = EhApplication.downloadManager
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

    override fun getNavCheckedItem(): Int {
        return R.id.nav_history
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
        val view = inflater.inflate(R.layout.scene_history, container, false)
        val content = ViewUtils.`$$`(view, R.id.content)
        val recyclerView = ViewUtils.`$$`(content, R.id.recycler_view) as RecyclerView
        setLiftOnScrollTargetView(recyclerView)
        val mFastScroller = ViewUtils.`$$`(content, R.id.fast_scroller) as FastScroller
        val mTip = ViewUtils.`$$`(view, R.id.tip) as TextView
        ViewTransition(content, mTip)
        val resources = requireContext().resources
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.big_history)
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        mTip.setCompoundDrawables(null, drawable, null, null)
        val historyData = Pager(
            PagingConfig(20)
        ) {
            EhDB.getHistoryLazyList()
        }.flow.cachedIn(viewLifecycleOwner.lifecycleScope)
        recyclerView.adapter = mAdapter
        val layoutManager = AutoStaggeredGridLayoutManager(
            0, StaggeredGridLayoutManager.VERTICAL
        )
        layoutManager.setColumnSize(resources.getDimensionPixelOffset(Settings.getDetailSizeResId()))
        layoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE)
        recyclerView.layoutManager = layoutManager
        recyclerView.clipToPadding = false
        recyclerView.clipChildren = false
        val itemTouchHelper = ItemTouchHelper(HistoryItemTouchHelperCallback())
        itemTouchHelper.attachToRecyclerView(recyclerView)
        mFastScroller.attachToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
        val handlerDrawable = HandlerDrawable()
        handlerDrawable.setColor(theme.resolveColor(com.google.android.material.R.attr.colorPrimary))
        mFastScroller.setHandlerDrawable(handlerDrawable)
        viewLifecycleOwner.lifecycleScope.launch {
            historyData.collectLatest { value ->
                mAdapter.submitData(
                    value
                )
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(R.string.history)
        setNavigationIcon(R.drawable.ic_baseline_menu_24)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRecyclerView?.stopScroll()
        mRecyclerView = null
    }

    @SuppressLint("RtlHardcoded")
    override fun onNavigationClick() {
        toggleDrawer(Gravity.LEFT)
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
                EhDB.clearHistoryInfo()
                mAdapter.refresh()
                mAdapter.notifyDataSetChanged()
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
        val announcer = Announcer(GalleryDetailScene::class.java).setArgs(args)
        startScene(announcer)
        return true
    }

    fun onItemLongClick(gi: GalleryInfo): Boolean {
        val context = requireContext()
        val activity = mainActivity ?: return false
        val downloaded = mDownloadManager.getDownloadState(gi.gid) != DownloadInfo.STATE_INVALID
        val favourited = gi.favoriteSlot != -2
        val items = if (downloaded) arrayOf<CharSequence>(
            context.getString(R.string.read),
            context.getString(R.string.delete_downloads),
            context.getString(if (favourited) R.string.remove_from_favourites else R.string.add_to_favourites),
            context.getString(R.string.download_move_dialog_title)
        ) else arrayOf<CharSequence>(
            context.getString(R.string.read),
            context.getString(R.string.download),
            context.getString(if (favourited) R.string.remove_from_favourites else R.string.add_to_favourites),
        )
        val icons = if (downloaded) intArrayOf(
            R.drawable.v_book_open_x24,
            R.drawable.v_delete_x24,
            if (favourited) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24,
            R.drawable.v_folder_move_x24
        ) else intArrayOf(
            R.drawable.v_book_open_x24,
            R.drawable.v_download_x24,
            if (favourited) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24,
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
                        val intent = Intent(activity, GalleryActivity::class.java)
                        intent.action = GalleryActivity.ACTION_EH
                        intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, gi)
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

                    2 -> if (favourited) {
                        CommonOperations.removeFromFavorites(
                            activity,
                            gi,
                            RemoveFromFavoriteListener(context, activity.stageId, tag)
                        )
                    } else {
                        CommonOperations.addToFavorites(
                            activity,
                            gi,
                            AddToFavoriteListener(context, activity.stageId, tag)
                        )
                    }

                    3 -> {
                        val labelRawList = EhApplication.downloadManager.labelList
                        val labelList: MutableList<String> = ArrayList(labelRawList.size + 1)
                        labelList.add(getString(R.string.default_download_label_name))
                        var i = 0
                        val n = labelRawList.size
                        while (i < n) {
                            labelList.add(labelRawList[i].label)
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

    private class AddToFavoriteListener(context: Context?, stageId: Int, sceneTag: String?) :
        EhCallback<GalleryListScene?, Void?>(context, stageId, sceneTag) {
        override fun onSuccess(result: Void?) {
            showTip(R.string.add_to_favorite_success, LENGTH_SHORT)
        }

        override fun onFailure(e: Exception) {
            showTip(R.string.add_to_favorite_failure, LENGTH_LONG)
        }

        override fun onCancel() {}
        override fun isInstance(scene: SceneFragment): Boolean {
            return scene is GalleryListScene
        }
    }

    private class RemoveFromFavoriteListener(context: Context?, stageId: Int, sceneTag: String?) :
        EhCallback<GalleryListScene?, Void?>(context, stageId, sceneTag) {
        override fun onSuccess(result: Void?) {
            showTip(R.string.remove_from_favorite_success, LENGTH_SHORT)
        }

        override fun onFailure(e: Exception) {
            showTip(R.string.remove_from_favorite_failure, LENGTH_LONG)
        }

        override fun onCancel() {}
        override fun isInstance(scene: SceneFragment): Boolean {
            return scene is GalleryListScene
        }
    }

    private class HistoryHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: View = itemView.findViewById(R.id.card)
        val thumb: LoadImageView = itemView.findViewById(R.id.thumb)
        val title: TextView = itemView.findViewById(R.id.title)
        val uploader: TextView = itemView.findViewById(R.id.uploader)
        val rating: SimpleRatingView = itemView.findViewById(R.id.rating)
        val category: TextView = itemView.findViewById(R.id.category)
        val posted: TextView = itemView.findViewById(R.id.posted)
        val simpleLanguage: TextView = itemView.findViewById(R.id.simple_language)
        val pages: TextView = itemView.findViewById(R.id.pages)
        val downloaded: ImageView = itemView.findViewById(R.id.downloaded)
    }

    private inner class MoveDialogHelper(
        private val mLabels: Array<String>,
        private val mGi: GalleryInfo
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            val downloadManager = EhApplication.downloadManager
            val downloadInfo = downloadManager.getDownloadInfo(mGi.gid) ?: return
            val label = if (which == 0) null else mLabels[which]
            downloadManager.changeLabel(listOf(downloadInfo), label)
        }
    }

    private inner class HistoryAdapter(diffCallback: DiffUtil.ItemCallback<HistoryInfo>) :
        PagingDataAdapter<HistoryInfo, HistoryHolder>(diffCallback) {
        private val mInflater: LayoutInflater = layoutInflater
        private val mListThumbWidth: Int
        private val mListThumbHeight: Int

        init {
            @SuppressLint("InflateParams") val calculator =
                mInflater.inflate(R.layout.item_gallery_list_thumb_height, null)
            ViewUtils.measureView(calculator, 1024, ViewGroup.LayoutParams.WRAP_CONTENT)
            mListThumbHeight = calculator.measuredHeight
            mListThumbWidth = mListThumbHeight * 2 / 3
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
            val holder = HistoryHolder(mInflater.inflate(R.layout.item_history, parent, false))
            val lp = holder.thumb.layoutParams
            lp.width = mListThumbWidth
            lp.height = mListThumbHeight
            holder.thumb.layoutParams = lp
            return holder
        }

        override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
            val gi: GalleryInfo? = getItem(position)
            gi ?: return
            holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb)
            holder.title.text = EhUtils.getSuitableTitle(gi)
            holder.uploader.text = gi.uploader
            holder.rating.rating = gi.rating
            val category = holder.category
            val newCategoryText = EhUtils.getCategory(gi.category)
            if (!newCategoryText.contentEquals(category.text)) {
                category.text = newCategoryText
                category.setBackgroundColor(EhUtils.getCategoryColor(gi.category))
            }
            holder.posted.text = gi.posted
            holder.pages.text = null
            holder.pages.visibility = View.GONE
            if (TextUtils.isEmpty(gi.simpleLanguage)) {
                holder.simpleLanguage.text = null
                holder.simpleLanguage.visibility = View.GONE
            } else {
                holder.simpleLanguage.text = gi.simpleLanguage
                holder.simpleLanguage.visibility = View.VISIBLE
            }
            holder.downloaded.visibility =
                if (mDownloadManager.containDownloadInfo(gi.gid)) View.VISIBLE else View.GONE
            holder.card.setOnClickListener { onItemClick(gi) }
            holder.card.setOnLongClickListener { onItemLongClick(gi) }
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
            val info: HistoryInfo? = mAdapter.peek(mPosition)
            info?.let { EhDB.deleteHistoryInfo(info) }
            mAdapter.refresh()
        }
    }
}