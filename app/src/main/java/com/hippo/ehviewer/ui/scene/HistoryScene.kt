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
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingDataAdapter
import androidx.paging.cachedIn
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hippo.easyrecyclerview.EasyRecyclerView
import com.hippo.easyrecyclerview.FastScroller
import com.hippo.easyrecyclerview.HandlerDrawable
import com.hippo.easyrecyclerview.MarginItemDecoration
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
    private var mRecyclerView: EasyRecyclerView? = null
    private var mViewTransition: ViewTransition? = null
    private var mAdapter: HistoryAdapter? = null
    private var mDownloadManager: DownloadManager? = null
    private var mDownloadInfoListener: DownloadInfoListener? = null
    private var mFavouriteStatusRouter: FavouriteStatusRouter? = null
    private var mFavouriteStatusRouterListener: FavouriteStatusRouter.Listener? = null

    override fun onDestroy() {
        super.onDestroy()
        mDownloadManager!!.removeDownloadInfoListener(mDownloadInfoListener)
        mFavouriteStatusRouter!!.removeListener(mFavouriteStatusRouterListener)
    }

    override fun getNavCheckedItem(): Int {
        return R.id.nav_history
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = requireContext()
        mDownloadManager = EhApplication.getDownloadManager(context)
        mFavouriteStatusRouter = EhApplication.getFavouriteStatusRouter(context)
        mDownloadInfoListener = object : DownloadInfoListener {
            override fun onAdd(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
                mAdapter?.notifyDataSetChanged()
            }

            override fun onUpdate(info: DownloadInfo, list: List<DownloadInfo>) {}
            override fun onUpdateAll() {}
            override fun onReload() {
                mAdapter?.notifyDataSetChanged()
            }

            override fun onChange() {
                mAdapter?.notifyDataSetChanged()
            }

            override fun onRenameLabel(from: String, to: String) {}
            override fun onRemove(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
                mAdapter?.notifyDataSetChanged()
            }

            override fun onUpdateLabels() {}
        }
        mDownloadManager!!.addDownloadInfoListener(mDownloadInfoListener)
        mFavouriteStatusRouterListener = FavouriteStatusRouter.Listener { _: Long, _: Int ->
            mAdapter?.notifyDataSetChanged()
        }
        mFavouriteStatusRouter!!.addListener(mFavouriteStatusRouterListener)
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.scene_history, container, false)
        val content = ViewUtils.`$$`(view, R.id.content)
        val recyclerView = ViewUtils.`$$`(content, R.id.recycler_view) as EasyRecyclerView
        val mFastScroller = ViewUtils.`$$`(content, R.id.fast_scroller) as FastScroller
        val mTip = ViewUtils.`$$`(view, R.id.tip) as TextView
        mViewTransition = ViewTransition(content, mTip)
        val resources = requireContext().resources
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.big_history)
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        mTip.setCompoundDrawables(null, drawable, null, null)
        val historyData = Pager(
            PagingConfig(20)
        ) {
            EhDB.getHistoryLazyList()
        }.flow.cachedIn(viewLifecycleOwner.lifecycleScope)
        mAdapter = HistoryAdapter(object : DiffUtil.ItemCallback<HistoryInfo>() {
            override fun areItemsTheSame(oldItem: HistoryInfo, newItem: HistoryInfo): Boolean {
                return oldItem.gid == newItem.gid
            }

            override fun areContentsTheSame(oldItem: HistoryInfo, newItem: HistoryInfo): Boolean {
                return oldItem.gid == newItem.gid
            }
        })
        recyclerView.adapter = mAdapter
        val layoutManager = AutoStaggeredGridLayoutManager(
            0, StaggeredGridLayoutManager.VERTICAL
        )
        layoutManager.setColumnSize(resources.getDimensionPixelOffset(Settings.getDetailSizeResId()))
        layoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE)
        recyclerView.layoutManager = layoutManager
        recyclerView.clipToPadding = false
        recyclerView.clipChildren = false
        val interval = resources.getDimensionPixelOffset(R.dimen.gallery_list_interval)
        val paddingH = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h)
        val paddingV = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v)
        val decoration = MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV)
        recyclerView.addItemDecoration(decoration)
        val itemTouchHelper = ItemTouchHelper(HistoryItemTouchHelperCallback())
        itemTouchHelper.attachToRecyclerView(recyclerView)
        mFastScroller.attachToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
        val handlerDrawable = HandlerDrawable()
        handlerDrawable.setColor(theme.resolveColor(com.google.android.material.R.attr.colorPrimary))
        mFastScroller.setHandlerDrawable(handlerDrawable)
        viewLifecycleOwner.lifecycleScope.launch {
            historyData.collectLatest { value ->
                mAdapter!!.submitData(
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
        mViewTransition = null
        mAdapter = null
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
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.clear_all_history)
            .setPositiveButton(R.string.clear_all) { _: DialogInterface?, which: Int ->
                if (DialogInterface.BUTTON_POSITIVE != which || null == mAdapter) {
                    return@setPositiveButton
                }
                EhDB.clearHistoryInfo()
                mAdapter!!.refresh()
                mAdapter!!.notifyDataSetChanged()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        // Skip when in choice mode
        context ?: return false
        val id = item.itemId
        if (id == R.id.action_clear_all) {
            showClearAllDialog()
            return true
        }
        return false
    }

    fun onItemClick(position: Int): Boolean {
        val args = Bundle()
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO)
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, mAdapter?.peek(position))
        val announcer = Announcer(GalleryDetailScene::class.java).setArgs(args)
        startScene(announcer)
        return true
    }

    fun onItemLongClick(position: Int): Boolean {
        val context = requireContext()
        val activity = mainActivity ?: return false
        val gi: GalleryInfo = mAdapter?.peek(position) ?: return true
        val downloaded = mDownloadManager!!.getDownloadState(gi.gid) != DownloadInfo.STATE_INVALID
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
        MaterialAlertDialogBuilder(context)
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
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.download_remove_dialog_title)
                            .setMessage(
                                getString(
                                    R.string.download_remove_dialog_message,
                                    gi.title
                                )
                            )
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                mDownloadManager!!.deleteDownload(
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
                        val labelRawList = EhApplication.getDownloadManager(context).labelList
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
                        MaterialAlertDialogBuilder(context)
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
        val card: View
        val thumb: LoadImageView
        val title: TextView
        val uploader: TextView
        val rating: SimpleRatingView
        val category: TextView
        val posted: TextView
        val simpleLanguage: TextView
        val pages: TextView
        val downloaded: ImageView

        init {
            card = itemView.findViewById(R.id.card)
            thumb = itemView.findViewById(R.id.thumb)
            title = itemView.findViewById(R.id.title)
            uploader = itemView.findViewById(R.id.uploader)
            rating = itemView.findViewById(R.id.rating)
            category = itemView.findViewById(R.id.category)
            posted = itemView.findViewById(R.id.posted)
            simpleLanguage = itemView.findViewById(R.id.simple_language)
            pages = itemView.findViewById(R.id.pages)
            downloaded = itemView.findViewById(R.id.downloaded)
        }
    }

    private inner class MoveDialogHelper(
        private val mLabels: Array<String>,
        private val mGi: GalleryInfo
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            mRecyclerView?.outOfCustomChoiceMode()
            val downloadManager = EhApplication.getDownloadManager(requireContext())
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
            if (!newCategoryText!!.contentEquals(category.text)) {
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
                if (mDownloadManager!!.containDownloadInfo(gi.gid)) View.VISIBLE else View.GONE

            // Update transition name
            val gid = gi.gid
            ViewCompat.setTransitionName(
                holder.thumb,
                TransitionNameFactory.getThumbTransitionName(gid)
            )
            holder.card.setOnClickListener { onItemClick(position) }
            holder.card.setOnLongClickListener { onItemLongClick(position) }
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
            if (mAdapter == null) {
                return
            }
            val info: HistoryInfo = mAdapter!!.peek(mPosition)!!
            EhDB.deleteHistoryInfo(info)
            mAdapter!!.refresh()
        }
    }
}