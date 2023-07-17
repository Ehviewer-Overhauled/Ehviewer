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
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import arrow.core.partially1
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hippo.ehviewer.EhApplication.Companion.imageCache
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.Settings.detailSize
import com.hippo.ehviewer.Settings.listThumbSize
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.coil.read
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.databinding.DrawerListRvBinding
import com.hippo.ehviewer.databinding.ItemDownloadBinding
import com.hippo.ehviewer.databinding.ItemDrawerListBinding
import com.hippo.ehviewer.databinding.SceneDownloadBinding
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadManager.DownloadInfoListener
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.DownloadService.Companion.clear
import com.hippo.ehviewer.ktbuilder.imageRequest
import com.hippo.ehviewer.spider.getGalleryDownloadDir
import com.hippo.ehviewer.spider.putToDownloadDir
import com.hippo.ehviewer.ui.legacy.AutoStaggeredGridLayoutManager
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.CheckBoxDialogBuilder
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import com.hippo.ehviewer.ui.legacy.FabLayout
import com.hippo.ehviewer.ui.legacy.FabLayout.OnClickFabListener
import com.hippo.ehviewer.ui.legacy.FastScroller.OnDragHandlerListener
import com.hippo.ehviewer.ui.legacy.HandlerDrawable
import com.hippo.ehviewer.ui.legacy.STRATEGY_MIN_SIZE
import com.hippo.ehviewer.ui.legacy.ViewTransition
import com.hippo.ehviewer.ui.main.requestOf
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.ui.tools.CropDefaults
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.LongList
import com.hippo.ehviewer.util.sendTo
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.pxToDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.core.res.resolveColor
import com.hippo.ehviewer.download.DownloadManager as downloadManager

@SuppressLint("RtlHardcoded")
class DownloadsScene :
    BaseToolbarScene(),
    DownloadInfoListener,
    OnClickFabListener,
    OnDragHandlerListener {
    /*---------------
     Whole life cycle
     ---------------*/
    private var mLabel: String? = null
    private var mList: List<DownloadInfo>? = null

    /*---------------
     View life cycle
     ---------------*/
    private var _binding: SceneDownloadBinding? = null
    private val binding get() = _binding!!
    private var mViewTransition: ViewTransition? = null
    private var mAdapter: DownloadAdapter? = null
    private var mItemTouchHelper: ItemTouchHelper? = null
    private val tracker get() = mAdapter!!.tracker!!
    private var mInitPosition = -1
    private var mLabelAdapter: DownloadLabelAdapter? = null
    private lateinit var mLabels: MutableList<String>
    private var mType = -1

    private fun initLabels() {
        context ?: return
        val listLabel = downloadManager.labelList
        mLabels = ArrayList(listLabel.size + LABEL_OFFSET)
        // Add "All" and "Default" label names
        mLabels.add(getString(R.string.download_all))
        mLabels.add(getString(R.string.default_download_label_name))
        listLabel.forEach {
            mLabels.add(it.label)
        }
    }

    private fun handleArguments(args: Bundle?): Boolean {
        if (null == args) {
            return false
        }
        if (ACTION_CLEAR_DOWNLOAD_SERVICE == args.getString(KEY_ACTION)) {
            clear()
        }
        val gid = args.getLong(KEY_GID, -1L)
        if (-1L != gid) {
            val info = DownloadManager.getDownloadInfo(gid)
            if (null != info) {
                mLabel = info.label
                updateForLabel()
                updateView()

                // Get position
                if (null != mList) {
                    val position = mList!!.indexOf(info)
                    if (position >= 0 && null != _binding) {
                        binding.recyclerView.scrollToPosition(position)
                    } else {
                        mInitPosition = position
                    }
                }
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DownloadManager.addDownloadInfoListener(this)
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mList = null
        DownloadManager.removeDownloadInfoListener(this)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateForLabel() {
        val list = when (mLabel) {
            null -> DownloadManager.allInfoList
            getString(R.string.default_download_label_name) -> DownloadManager.defaultInfoList
            else -> DownloadManager.getLabelDownloadInfoList(mLabel)
                ?: DownloadManager.allInfoList.also { mLabel = null }
        }
        mList = if (mType != -1) {
            list.filter { it.state == mType }
        } else {
            list
        }
        if (mAdapter != null) {
            mAdapter!!.notifyDataSetChanged()
        }
        Settings.recentDownloadLabel = mLabel
    }

    private fun updateTitle() {
        setTitle(
            getString(
                R.string.scene_download_title,
                if (mLabel != null) mLabel else getString(R.string.download_all),
            ),
        )
    }

    private fun onInit() {
        if (!handleArguments(arguments)) {
            mLabel = Settings.recentDownloadLabel
            updateForLabel()
        }
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mLabel = savedInstanceState.getString(KEY_LABEL)
        updateForLabel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_LABEL, mLabel)
        mAdapter?.let { tracker.saveSelection(outState) }
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SceneDownloadBinding.inflate(inflater, container!!)
        binding.run {
            setLiftOnScrollTargetView(recyclerView)
            mViewTransition = ViewTransition(content, tip)
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.big_download)
            drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            tip.setCompoundDrawables(null, drawable, null, null)
            mAdapter = DownloadAdapter()
            mAdapter!!.setHasStableIds(true)
            recyclerView.adapter = mAdapter
            mAdapter!!.tracker = GallerySelectionTracker(
                "download-selection",
                recyclerView,
                { mList!! },
                { (this as DownloadHolder).itemId },
            ).apply {
                addCustomChoiceListener({
                    binding.fabLayout.isExpanded = true
                    // Lock drawer
                    setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
                    setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
                }) {
                    binding.fabLayout.isExpanded = false
                    // Unlock drawer
                    setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
                    setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
                }
                restoreSelection(savedInstanceState)
            }
            val layoutManager = AutoStaggeredGridLayoutManager(0, StaggeredGridLayoutManager.VERTICAL)
            layoutManager.setColumnSize(
                resources.getDimensionPixelOffset(
                    when (detailSize) {
                        0 -> R.dimen.gallery_list_column_width_long
                        1 -> R.dimen.gallery_list_column_width_short
                        else -> throw IllegalStateException("Unexpected value: $detailSize")
                    },
                ),
            )
            layoutManager.setStrategy(STRATEGY_MIN_SIZE)
            layoutManager.supportsPredictiveItemAnimations = false
            recyclerView.layoutManager = layoutManager
            recyclerView.clipToPadding = false
            recyclerView.clipChildren = false
            // Cancel change animation
            val itemAnimator = recyclerView.itemAnimator
            if (itemAnimator is SimpleItemAnimator) {
                itemAnimator.supportsChangeAnimations = false
            }
            val interval = resources.getDimensionPixelOffset(R.dimen.gallery_list_interval)
            val decoration = object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    outRect.set(0, interval / 2, 0, interval / 2)
                }
            }
            recyclerView.addItemDecoration(decoration)
            if (mInitPosition >= 0) {
                recyclerView.scrollToPosition(mInitPosition)
                mInitPosition = -1
            }
            mItemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ACTION_STATE_DRAG) {
                        tracker.clearSelection()
                    }
                }
                override fun isLongPressDragEnabled() = false
                override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                    return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                }
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder,
                ): Boolean {
                    val fromPosition = viewHolder.bindingAdapterPosition
                    val toPosition = target.bindingAdapterPosition
                    if (fromPosition == toPosition) {
                        return false
                    }
                    lifecycleScope.launchIO {
                        when (mLabel) {
                            null -> DownloadManager.moveDownload(fromPosition, toPosition)
                            getString(R.string.default_download_label_name) -> DownloadManager.moveDownload(null, fromPosition, toPosition)
                            else -> DownloadManager.moveDownload(mLabel, fromPosition, toPosition)
                        }
                        withUIContext {
                            mAdapter!!.notifyItemMoved(fromPosition, toPosition)
                        }
                    }
                    return true
                }
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            })
            mItemTouchHelper!!.attachToRecyclerView(recyclerView)
            fastScroller.attachToRecyclerView(recyclerView)
            val handlerDrawable = HandlerDrawable()
            handlerDrawable.setColor(theme.resolveColor(com.google.android.material.R.attr.colorPrimary))
            fastScroller.setHandlerDrawable(handlerDrawable)
            fastScroller.setOnDragHandlerListener(this@DownloadsScene)
            fabLayout.setExpanded(expanded = false, animation = false)
            fabLayout.addOnExpandListener { if (!it && tracker.isInCustomChoice) tracker.clearSelection() }
            fabLayout.setHidePrimaryFab(true)
            fabLayout.setAutoCancel(false)
            fabLayout.setOnClickFabListener(this@DownloadsScene)
            addAboveSnackView(fabLayout)
            updateView()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateTitle()
        setNavigationIcon(R.drawable.ic_baseline_menu_24)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.stopScroll()
        removeAboveSnackView(binding.fabLayout)
        mViewTransition = null
        mAdapter = null
        mLabelAdapter = null
        mItemTouchHelper = null
        _binding = null
    }

    override fun onNavigationClick() {
        toggleDrawer(GravityCompat.START)
    }

    override fun getMenuResId(): Int {
        return R.menu.scene_download
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        // Skip when in choice mode
        val activity: Activity? = mainActivity
        if (null == activity || tracker.isInCustomChoice) {
            return false
        }
        when (item.itemId) {
            R.id.action_filter -> {
                BaseDialogBuilder(requireActivity())
                    .setSingleChoiceItems(
                        R.array.download_state,
                        mType + 1,
                    ) { dialog: DialogInterface, which: Int ->
                        mType = which - 1
                        updateForLabel()
                        updateView()
                        dialog.dismiss()
                    }
                    .show()
                return true
            }

            R.id.action_start_all -> {
                val intent = Intent(activity, DownloadService::class.java)
                intent.action = DownloadService.ACTION_START_ALL
                ContextCompat.startForegroundService(activity, intent)
                return true
            }

            R.id.action_stop_all -> {
                lifecycleScope.launchIO {
                    DownloadManager.stopAllDownload()
                }
                return true
            }

            R.id.action_open_download_labels -> {
                openDrawer(GravityCompat.END)
                return true
            }

            R.id.action_reset_reading_progress -> {
                BaseDialogBuilder(requireContext())
                    .setMessage(R.string.reset_reading_progress_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                        lifecycleScope.launchNonCancellable {
                            DownloadManager.resetAllReadingProgress()
                        }
                    }.show()
                return true
            }

            R.id.action_start_all_reversed -> {
                val list = mList ?: return true
                val gidList = LongList()
                for (i in list.size - 1 downTo 0) {
                    val info = list[i]
                    if (info.state != DownloadInfo.STATE_FINISH) {
                        gidList.add(info.gid)
                    }
                }
                val intent = Intent(activity, DownloadService::class.java)
                intent.action = DownloadService.ACTION_START_RANGE
                intent.putExtra(DownloadService.KEY_GID_LIST, gidList)
                ContextCompat.startForegroundService(activity, intent)
                return true
            }

            else -> return false
        }
    }

    fun updateView() {
        if (mViewTransition != null) {
            if (mList.isNullOrEmpty()) {
                mViewTransition!!.showView(1)
            } else {
                mViewTransition!!.showView(0)
            }
        }
        updateTitle()
    }

    override fun onCreateDrawerView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val drawerBinding = DrawerListRvBinding.inflate(inflater, container, false)
        drawerBinding.toolbar.setTitle(R.string.download_labels)
        drawerBinding.toolbar.inflateMenu(R.menu.drawer_download)
        drawerBinding.toolbar.setOnMenuItemClickListener { item: MenuItem ->
            val id = item.itemId
            if (id == R.id.action_add) {
                val builder = EditTextDialogBuilder(
                    requireContext(),
                    null,
                    getString(R.string.download_labels),
                )
                builder.setTitle(R.string.new_label_title)
                builder.setPositiveButton(android.R.string.ok, null)
                val dialog = builder.show()
                NewLabelDialogHelper(builder, dialog)
                return@setOnMenuItemClickListener true
            } else if (id == R.id.action_default_download_label) {
                val list = DownloadManager.labelList.map { it.label }.toTypedArray()
                val items = arrayOf(
                    getString(R.string.let_me_select),
                    getString(R.string.default_download_label_name),
                    *list,
                )
                BaseDialogBuilder(requireContext())
                    .setTitle(R.string.default_download_label)
                    .setItems(items) { _: DialogInterface?, which: Int ->
                        if (which == 0) {
                            Settings.hasDefaultDownloadLabel = false
                        } else {
                            Settings.hasDefaultDownloadLabel = true
                            val label: String? = if (which == 1) {
                                null
                            } else {
                                items[which]
                            }
                            Settings.defaultDownloadLabel = label
                        }
                    }.show()
                return@setOnMenuItemClickListener true
            }
            false
        }
        initLabels()
        mLabelAdapter = DownloadLabelAdapter(inflater)
        drawerBinding.recyclerViewDrawer.layoutManager = LinearLayoutManager(context)
        mLabelAdapter!!.setHasStableIds(true)
        val itemTouchHelper = ItemTouchHelper(DownloadLabelItemTouchHelperCallback())
        itemTouchHelper.attachToRecyclerView(drawerBinding.recyclerViewDrawer)
        drawerBinding.recyclerViewDrawer.adapter = mLabelAdapter
        return drawerBinding.root
    }

    override fun onStartDragHandler() {
        // Lock right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
    }

    override fun onEndDragHandler() {
        // Restore right drawer
        if (!tracker.isInCustomChoice) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
        }
    }

    fun onItemClick(position: Int): Boolean {
        val context = context ?: return false
        val list = mList ?: return false
        if (position < 0 || position >= list.size) {
            return false
        }
        context.navToReader(list[position])
        return true
    }

    override fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton) {
        if (tracker.isInCustomChoice) {
            tracker.clearSelection()
        }
    }

    override fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int) {
        val context = context
        val activity: Activity? = mainActivity
        if (null == context || null == activity) {
            return
        }
        if (0 == position) {
            tracker.selectAll()
        } else {
            val downloadInfoList = tracker.getAndClearSelection()
            val gidList = if (position in 1..3) {
                LongList(downloadInfoList.map(DownloadInfo::gid).toMutableList())
            } else {
                null
            }
            when (position) {
                1 -> {
                    // Start
                    val intent = Intent(activity, DownloadService::class.java)
                    intent.action = DownloadService.ACTION_START_RANGE
                    intent.putExtra(DownloadService.KEY_GID_LIST, gidList)
                    ContextCompat.startForegroundService(activity, intent)
                }

                2 -> {
                    // Stop
                    lifecycleScope.launchIO {
                        DownloadManager.stopRangeDownload(gidList!!)
                    }
                }

                3 -> {
                    // Delete
                    val builder = CheckBoxDialogBuilder(
                        context,
                        getString(R.string.download_remove_dialog_message_2, gidList!!.size),
                        getString(R.string.download_remove_dialog_check_text),
                        Settings.removeImageFiles,
                    )
                    val helper = DeleteRangeDialogHelper(
                        downloadInfoList,
                        gidList,
                        builder,
                    )
                    builder.setTitle(R.string.download_remove_dialog_title)
                        .setPositiveButton(android.R.string.ok, helper)
                        .show()
                }

                4 -> {
                    // Move
                    val labelRawList = downloadManager.labelList
                    val labelList: MutableList<String> = ArrayList(labelRawList.size + 1)
                    labelList.add(getString(R.string.default_download_label_name))
                    labelRawList.forEach {
                        labelList.add(it.label)
                    }
                    val labels = labelList.toTypedArray()
                    val helper = MoveDialogHelper(labels, downloadInfoList)
                    BaseDialogBuilder(context)
                        .setTitle(R.string.download_move_dialog_title)
                        .setItems(labels, helper)
                        .show()
                }
            }
        }
    }

    override fun onAdd(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
        if (mList !== list) {
            return
        }
        lifecycleScope.launchUI {
            if (mAdapter != null) {
                mAdapter!!.notifyItemInserted(position)
            }
            updateView()
        }
    }

    override fun onUpdate(info: DownloadInfo, list: List<DownloadInfo>) {
        if (mLabel != null && mList !== list) {
            return
        }
        val index = mList!!.indexOf(info)
        lifecycleScope.launchUI {
            if (index >= 0 && mAdapter != null) {
                mAdapter!!.notifyItemChanged(index, PAYLOAD_STATE)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onUpdateAll() {
        lifecycleScope.launchUI {
            if (mAdapter != null) {
                mAdapter!!.notifyDataSetChanged()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onReload() {
        lifecycleScope.launchUI {
            if (mAdapter != null) {
                mAdapter!!.notifyDataSetChanged()
            }
            updateView()
        }
    }

    override fun onRemove(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
        if (mList !== list) {
            return
        }
        lifecycleScope.launchUI {
            if (mAdapter != null) {
                mAdapter!!.notifyItemRemoved(position)
            }
            updateView()
        }
    }

    private class DownloadLabelHolder(val binding: ItemDrawerListBinding) :
        RecyclerView.ViewHolder(binding.root)

    private inner class DownloadLabelAdapter(private val mInflater: LayoutInflater) :
        RecyclerView.Adapter<DownloadLabelHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadLabelHolder {
            val holder =
                DownloadLabelHolder(ItemDrawerListBinding.inflate(mInflater, parent, false))
            holder.itemView.setOnClickListener {
                val position = holder.bindingAdapterPosition
                val label1: String? = if (position == 0) {
                    null
                } else {
                    mLabels[position]
                }
                if (mLabel != label1) {
                    mLabel = label1
                    updateForLabel()
                    updateView()
                    closeDrawer(GravityCompat.END)
                }
            }
            holder.binding.edit.setOnClickListener {
                val context = context
                val label = mLabels[holder.bindingAdapterPosition]
                if (context != null) {
                    val builder = EditTextDialogBuilder(
                        context,
                        label,
                        getString(R.string.download_labels),
                    )
                    builder.setTitle(R.string.rename_label_title)
                    builder.setPositiveButton(android.R.string.ok, null)
                    val dialog = builder.show()
                    RenameLabelDialogHelper(builder, dialog, label)
                }
            }
            return holder
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: DownloadLabelHolder, position: Int) {
            val label = mLabels[position]
            val list: List<DownloadInfo?>? = when (position) {
                0 -> {
                    DownloadManager.allInfoList
                }

                1 -> {
                    DownloadManager.defaultInfoList
                }

                else -> {
                    DownloadManager.getLabelDownloadInfoList(label)
                }
            }
            holder.binding.run {
                if (list != null) {
                    text.text = label + " [" + list.size + "]"
                } else {
                    text.text = label
                }
                if (position < LABEL_OFFSET) {
                    edit.visibility = View.GONE
                    option.visibility = View.GONE
                } else {
                    edit.visibility = View.VISIBLE
                    option.visibility = View.VISIBLE
                }
            }
        }

        override fun getItemId(position: Int): Long {
            return (if (position < LABEL_OFFSET) position else mLabels[position].hashCode()).toLong()
        }

        override fun getItemCount(): Int {
            return mLabels.size
        }
    }

    private inner class DeleteRangeDialogHelper(
        private val mDownloadInfoList: List<DownloadInfo>,
        private val mGidList: LongList,
        private val mBuilder: CheckBoxDialogBuilder,
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return
            }

            lifecycleScope.launchIO {
                // Delete image files
                val checked = mBuilder.isChecked
                Settings.removeImageFiles = checked
                if (checked) {
                    // Delete
                    DownloadManager.deleteRangeDownload(mGidList)
                    val files = arrayOfNulls<UniFile>(mDownloadInfoList.size)
                    for ((i, info) in mDownloadInfoList.withIndex()) {
                        // Put file
                        files[i] = getGalleryDownloadDir(info.gid)
                        // Remove download path
                        EhDB.removeDownloadDirname(info.gid)
                    }
                    // Delete file
                    files.forEach { it?.delete() }
                }
            }
        }
    }

    private inner class MoveDialogHelper(
        private val mLabels: Array<String>,
        private val mDownloadInfoList: List<DownloadInfo>,
    ) : DialogInterface.OnClickListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onClick(dialog: DialogInterface, which: Int) {
            context ?: return
            val label: String? = if (which == 0) {
                null
            } else {
                mLabels[which]
            }
            lifecycleScope.launchIO {
                downloadManager.changeLabel(mDownloadInfoList, label)
                withUIContext {
                    if (mLabelAdapter != null) {
                        mLabelAdapter!!.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private inner class DownloadHolder(
        private val binding: ItemDownloadBinding,
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            // TODO cancel on click listener when select items
            binding.start.setOnClickListener(this)
            binding.stop.setOnClickListener(this)
            binding.thumb.setMD3Content {
                Spacer(modifier = Modifier.height(height).fillMaxWidth())
            }
            binding.handle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    mItemTouchHelper!!.startDrag(this)
                }
                false
            }
        }

        override fun onClick(v: View) {
            val context = context
            val activity: Activity? = mainActivity
            val recyclerView = this@DownloadsScene.binding.recyclerView
            if (null == context || null == activity || tracker.isInCustomChoice) {
                return
            }
            val list = mList ?: return
            val size = list.size
            val index = recyclerView.getChildAdapterPosition(itemView)
            if (index < 0 || index >= size) {
                return
            }
            when (v) {
                binding.thumb -> {
                    val args = Bundle()
                    args.putString(
                        GalleryDetailScene.KEY_ACTION,
                        GalleryDetailScene.ACTION_GALLERY_INFO,
                    )
                    args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, list[index])
                    navAnimated(R.id.galleryDetailScene, args)
                }

                binding.start -> {
                    val intent = Intent(activity, DownloadService::class.java)
                    intent.action = DownloadService.ACTION_START
                    intent.putExtra(DownloadService.KEY_GALLERY_INFO, list[index])
                    ContextCompat.startForegroundService(activity, intent)
                }

                binding.stop -> {
                    lifecycleScope.launchIO {
                        DownloadManager.stopDownload(list[index].gid)
                    }
                }
            }
        }

        private val height = (3 * listThumbSize * 3).pxToDp.dp

        fun bind(info: DownloadInfo, isChecked: Boolean) {
            binding.root.isChecked = isChecked
            lifecycleScope.launchIO {
                val downloadDir = getGalleryDownloadDir(info.gid) ?: run {
                    info.putToDownloadDir()
                    getGalleryDownloadDir(info.gid)!!
                }
                downloadDir.ensureDir()
                val thumbLocation = downloadDir.subFile(".thumb")!!
                withUIContext {
                    binding.thumb.setMD3Content {
                        Card(onClick = ::onClick.partially1(binding.thumb)) {
                            CompanionAsyncThumb(
                                info = info,
                                path = thumbLocation,
                                modifier = Modifier.height(height).aspectRatio(0.6666667F),
                            )
                        }
                    }
                }
            }
            binding.title.text = EhUtils.getSuitableTitle(info)
            binding.uploader.text = info.uploader
            binding.rating.rating = info.rating
            val category = binding.category
            val newCategoryText = EhUtils.getCategory(info.category)
            if (!newCategoryText.contentEquals(category.text)) {
                category.text = newCategoryText
                category.setBackgroundColor(EhUtils.getCategoryColor(info.category))
            }
            bindForState(info)
        }

        fun bindForState(info: DownloadInfo) {
            val context = context ?: return
            when (info.state) {
                DownloadInfo.STATE_NONE -> bindState(
                    info,
                    context.getString(R.string.download_state_none),
                )

                DownloadInfo.STATE_WAIT -> bindState(
                    info,
                    context.getString(R.string.download_state_wait),
                )

                DownloadInfo.STATE_DOWNLOAD -> bindProgress(info)
                DownloadInfo.STATE_FAILED -> {
                    val text: String = if (info.legacy <= 0) {
                        context.getString(R.string.download_state_failed)
                    } else {
                        context.getString(R.string.download_state_failed_2, info.legacy)
                    }
                    bindState(info, text)
                }

                DownloadInfo.STATE_FINISH -> bindState(
                    info,
                    context.getString(R.string.download_state_finish),
                )
            }
        }

        private fun bindState(info: DownloadInfo, newState: String) {
            binding.run {
                uploader.visibility = View.VISIBLE
                rating.visibility = View.VISIBLE
                category.visibility = View.VISIBLE
                state.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                percent.visibility = View.GONE
                speed.visibility = View.GONE
                if (info.state == DownloadInfo.STATE_WAIT || info.state == DownloadInfo.STATE_DOWNLOAD) {
                    start.visibility = View.GONE
                    stop.visibility = View.VISIBLE
                } else {
                    start.visibility = View.VISIBLE
                    stop.visibility = View.GONE
                }
                state.text = newState
            }
        }

        @SuppressLint("SetTextI18n")
        private fun bindProgress(info: DownloadInfo) {
            binding.run {
                uploader.visibility = View.GONE
                rating.visibility = View.GONE
                category.visibility = View.GONE
                state.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                percent.visibility = View.VISIBLE
                speed.visibility = View.VISIBLE
                if (info.state == DownloadInfo.STATE_WAIT || info.state == DownloadInfo.STATE_DOWNLOAD) {
                    start.visibility = View.GONE
                    stop.visibility = View.VISIBLE
                } else {
                    start.visibility = View.VISIBLE
                    stop.visibility = View.GONE
                }
                if (info.total <= 0 || info.finished < 0) {
                    percent.text = null
                    progressBar.isIndeterminate = true
                } else {
                    percent.text = info.finished.toString() + "/" + info.total
                    progressBar.isIndeterminate = false
                    progressBar.max = info.total
                    progressBar.progress = info.finished
                }
                speed.text =
                    FileUtils.humanReadableByteCount(info.speed.coerceAtLeast(0), false) + "/S"
            }
        }
    }

    private inner class DownloadAdapter : RecyclerView.Adapter<DownloadHolder>() {
        private val mInflater: LayoutInflater = layoutInflater
        var tracker: GallerySelectionTracker<DownloadInfo>? = null

        override fun getItemId(position: Int): Long {
            return if (mList == null || position < 0 || position >= mList!!.size) {
                0
            } else {
                mList!![position].gid
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadHolder {
            val holder = DownloadHolder(ItemDownloadBinding.inflate(mInflater, parent, false))
            holder.itemView.setOnClickListener { onItemClick(holder.bindingAdapterPosition) }
            return holder
        }

        override fun onBindViewHolder(holder: DownloadHolder, position: Int) {
            mList?.let {
                val info = it[position]
                holder.bind(info, tracker?.isSelected(info.gid) ?: false)
            }
        }

        override fun onBindViewHolder(
            holder: DownloadHolder,
            position: Int,
            payloads: MutableList<Any>,
        ) {
            payloads.forEach { payload ->
                when (payload) {
                    PAYLOAD_STATE -> {
                        mList?.let { holder.bindForState(it[position]) }
                        return
                    }
                }
            }
            super.onBindViewHolder(holder, position, payloads)
        }

        override fun getItemCount(): Int {
            return if (mList == null) 0 else mList!!.size
        }
    }

    private inner class RenameLabelDialogHelper(
        private val mBuilder: EditTextDialogBuilder,
        private val mDialog: AlertDialog,
        private val mOriginalLabel: String?,
    ) : View.OnClickListener {
        init {
            val button: Button = mDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            button.setOnClickListener(this)
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onClick(v: View) {
            context ?: return
            val text = mBuilder.text
            if (text.isEmpty()) {
                mBuilder.setError(getString(R.string.label_text_is_empty))
            } else if (getString(R.string.default_download_label_name) == text) {
                mBuilder.setError(getString(R.string.label_text_is_invalid))
            } else if (downloadManager.containLabel(text)) {
                mBuilder.setError(getString(R.string.label_text_exist))
            } else {
                mBuilder.setError(null)
                mDialog.dismiss()
                lifecycleScope.launchIO {
                    downloadManager.renameLabel(mOriginalLabel!!, text)
                    if (mLabelAdapter != null) {
                        withUIContext {
                            initLabels()
                            mLabelAdapter!!.notifyDataSetChanged()
                            if (mLabel == mOriginalLabel) {
                                mLabel = text
                                updateForLabel()
                                updateView()
                            }
                        }
                    }
                }
            }
        }
    }

    private inner class NewLabelDialogHelper(
        private val mBuilder: EditTextDialogBuilder,
        private val mDialog: AlertDialog,
    ) : View.OnClickListener {
        init {
            val button: Button = mDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            button.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            context ?: return
            val text = mBuilder.text
            if (text.isEmpty()) {
                mBuilder.setError(getString(R.string.label_text_is_empty))
            } else if (getString(R.string.default_download_label_name) == text) {
                mBuilder.setError(getString(R.string.label_text_is_invalid))
            } else if (downloadManager.containLabel(text)) {
                mBuilder.setError(getString(R.string.label_text_exist))
            } else {
                mBuilder.setError(null)
                mDialog.dismiss()
                lifecycleScope.launchIO {
                    downloadManager.addLabel(text)
                    initLabels()
                    withUIContext {
                        mLabelAdapter?.notifyItemInserted(mLabels.size)
                    }
                }
            }
        }
    }

    private inner class DownloadLabelItemTouchHelperCallback : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
        ): Int {
            val position = viewHolder.bindingAdapterPosition
            return if (position < LABEL_OFFSET) {
                makeMovementFlags(0, 0)
            } else {
                makeMovementFlags(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    ItemTouchHelper.LEFT,
                )
            }
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            val context = context
            if (null == context || fromPosition == toPosition || toPosition < LABEL_OFFSET) {
                return false
            }
            lifecycleScope.launchIO {
                downloadManager.moveLabel(fromPosition - LABEL_OFFSET, toPosition - LABEL_OFFSET)
            }
            val item = mLabels.removeAt(fromPosition)
            mLabels.add(toPosition, item)
            mLabelAdapter!!.notifyItemMoved(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            val label = mLabels[position]
            BaseDialogBuilder(context!!)
                .setMessage(getString(R.string.delete_label, label))
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launchIO {
                        DownloadManager.deleteLabel(label)
                        mLabels.removeAt(position)
                        withUIContext {
                            mLabel = null
                            updateForLabel()
                            updateView()
                            mLabelAdapter!!.notifyItemRemoved(position)
                        }
                    }
                }.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }.setOnCancelListener {
                    mLabelAdapter!!.notifyItemChanged(position)
                }.show()
        }
    }

    companion object {
        const val KEY_GID = "gid"
        const val KEY_ACTION = "action"
        const val ACTION_CLEAR_DOWNLOAD_SERVICE = "clear_download_service"
        private const val KEY_LABEL = "label"
        private const val LABEL_OFFSET = 2
        private const val PAYLOAD_STATE = 0
    }
}

@Composable
private fun CompanionAsyncThumb(
    info: GalleryInfo,
    path: UniFile,
    modifier: Modifier = Modifier,
) {
    var contentScale by remember(info.gid) { mutableStateOf(ContentScale.Fit) }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    var localReq by remember(info.gid) {
        path.takeIf { it.isFile }?.uri?.let {
            context.imageRequest {
                data(it.toString())
                memoryCacheKey(info.thumbKey)
            }
        }.let { mutableStateOf(it) }
    }
    AsyncImage(
        model = localReq ?: requestOf(info),
        contentDescription = null,
        modifier = modifier,
        onState = { state ->
            if (state is AsyncImagePainter.State.Success) {
                state.result.drawable.run {
                    if (CropDefaults.shouldCrop(intrinsicWidth, intrinsicHeight)) {
                        contentScale = ContentScale.Crop
                    }
                }
                coroutineScope.launch {
                    runCatching {
                        if (!path.exists() && path.ensureFile()) {
                            val key = info.thumbKey!!
                            imageCache.read(key) {
                                UniFile.fromFile(data.toFile())!!.openFileDescriptor("r").use { src ->
                                    path.openFileDescriptor("w").use { dst ->
                                        src sendTo dst
                                    }
                                }
                            }
                        }
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
            }
            if (state is AsyncImagePainter.State.Error) {
                coroutineScope.launch {
                    if (path.exists()) {
                        path.delete()
                        localReq = null
                    }
                }
            }
        },
        contentScale = contentScale,
    )
}
