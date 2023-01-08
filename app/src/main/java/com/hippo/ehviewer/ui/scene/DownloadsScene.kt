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
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hippo.app.BaseDialogBuilder
import com.hippo.app.CheckBoxDialogBuilder
import com.hippo.app.EditTextDialogBuilder
import com.hippo.easyrecyclerview.EasyRecyclerView
import com.hippo.easyrecyclerview.EasyRecyclerView.CustomChoiceListener
import com.hippo.easyrecyclerview.FastScroller
import com.hippo.easyrecyclerview.FastScroller.OnDragHandlerListener
import com.hippo.easyrecyclerview.HandlerDrawable
import com.hippo.easyrecyclerview.MarginItemDecoration
import com.hippo.ehviewer.EhApplication.Companion.downloadManager
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCacheKeyFactory
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadManager.DownloadInfoListener
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.DownloadService.Companion.clear
import com.hippo.ehviewer.spider.SpiderDen
import com.hippo.ehviewer.widget.SimpleRatingView
import com.hippo.scene.Announcer
import com.hippo.unifile.UniFile
import com.hippo.view.ViewTransition
import com.hippo.widget.FabLayout
import com.hippo.widget.FabLayout.OnClickFabListener
import com.hippo.widget.FabLayout.OnExpandListener
import com.hippo.widget.LoadImageView
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager
import com.hippo.yorozuya.AssertUtils
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.ObjectUtils
import com.hippo.yorozuya.ViewUtils
import com.hippo.yorozuya.collect.LongList
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import rikka.core.res.resolveColor
import java.util.LinkedList

@SuppressLint("RtlHardcoded")
class DownloadsScene : BaseToolbarScene(), DownloadInfoListener, OnClickFabListener,
    OnDragHandlerListener {
    /*---------------
     Whole life cycle
     ---------------*/
    private var mDownloadManager: DownloadManager? = null
    private var mLabel: String? = null
    private var mList: MutableList<DownloadInfo>? = null

    /*---------------
     View life cycle
     ---------------*/
    private var mTip: TextView? = null
    private var mFastScroller: FastScroller? = null
    private var mRecyclerView: EasyRecyclerView? = null
    private var mViewTransition: ViewTransition? = null
    private var mFabLayout: FabLayout? = null
    private var mAdapter: DownloadAdapter? = null
    private var mLayoutManager: AutoStaggeredGridLayoutManager? = null
    private var mInitPosition = -1
    private var mLabelAdapter: DownloadLabelAdapter? = null
    private lateinit var mLabels: MutableList<String>
    private var mType = -1

    override var navCheckedItem: Int = R.id.nav_downloads

    private fun initLabels() {
        context ?: return
        val listLabel = downloadManager.labelList
        mLabels = ArrayList(listLabel.size + 1)
        // Add default label name
        mLabels.add(getString(R.string.default_download_label_name))
        for ((_, label) in listLabel) {
            mLabels.add(label!!)
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
        if (null != mDownloadManager && -1L != gid) {
            val info = mDownloadManager!!.getDownloadInfo(gid)
            if (null != info) {
                mLabel = info.label
                updateForLabel()
                updateView()

                // Get position
                if (null != mList) {
                    val position = mList!!.indexOf(info)
                    if (position >= 0 && null != mRecyclerView) {
                        mRecyclerView!!.scrollToPosition(position)
                    } else {
                        mInitPosition = position
                    }
                }
                return true
            }
        }
        return false
    }

    override fun onNewArguments(args: Bundle) {
        handleArguments(args)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = context
        AssertUtils.assertNotNull(context)
        mDownloadManager = downloadManager
        mDownloadManager!!.addDownloadInfoListener(this)
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mList = null
        var manager = mDownloadManager
        if (null == manager) {
            val context = context
            if (null != context) {
                manager = downloadManager
            }
        } else {
            mDownloadManager = null
        }
        manager?.removeDownloadInfoListener(this)
            ?: Log.e(TAG, "Can't removeDownloadInfoListener")
    }

    private fun updateForLabel() {
        if (null == mDownloadManager) {
            return
        }
        var list: MutableList<DownloadInfo>?
        if (mLabel == null) {
            list = mDownloadManager!!.defaultDownloadInfoList
        } else {
            list = mDownloadManager!!.getLabelDownloadInfoList(mLabel)
            if (list == null) {
                mLabel = null
                list = mDownloadManager!!.defaultDownloadInfoList
            }
        }
        if (mType != -1) {
            mList = ArrayList()
            for (info in list) {
                if (info.state == mType) {
                    mList!!.add(info)
                }
            }
        } else {
            mList = list
        }
        if (mAdapter != null) {
            mAdapter!!.notifyDataSetChanged()
        }
        Settings.putRecentDownloadLabel(mLabel)
    }

    private fun updateTitle() {
        setTitle(
            getString(
                R.string.scene_download_title,
                if (mLabel != null) mLabel else getString(R.string.default_download_label_name)
            )
        )
    }

    private fun onInit() {
        if (!handleArguments(arguments)) {
            mLabel = Settings.getRecentDownloadLabel()
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
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.scene_download, container, false)
        val content = ViewUtils.`$$`(view, R.id.content)
        mRecyclerView = ViewUtils.`$$`(content, R.id.recycler_view) as EasyRecyclerView
        setLiftOnScrollTargetView(mRecyclerView)
        mFastScroller = ViewUtils.`$$`(content, R.id.fast_scroller) as FastScroller
        mFabLayout = ViewUtils.`$$`(view, R.id.fab_layout) as FabLayout

        // Workaround
        (mFabLayout!!.parent as ViewGroup).removeView(mFabLayout)
        container!!.addView(mFabLayout)
        mTip = ViewUtils.`$$`(view, R.id.tip) as TextView
        mViewTransition = ViewTransition(content, mTip)
        val context = context
        AssertUtils.assertNotNull(content)
        val resources = context!!.resources
        val drawable = ContextCompat.getDrawable(context, R.drawable.big_download)
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        mTip!!.setCompoundDrawables(null, drawable, null, null)
        mAdapter = DownloadAdapter()
        mAdapter!!.setHasStableIds(true)
        mRecyclerView!!.adapter = mAdapter
        mLayoutManager = AutoStaggeredGridLayoutManager(0, StaggeredGridLayoutManager.VERTICAL)
        mLayoutManager!!.setColumnSize(resources.getDimensionPixelOffset(Settings.getDetailSizeResId()))
        mLayoutManager!!.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE)
        mRecyclerView!!.layoutManager = mLayoutManager
        //mRecyclerView.setSelector(Ripple.generateRippleDrawable(context, !ResourcesKt.resolveColor(getTheme(), .getAttrBoolean(context, R.attr.isLightTheme), new ColorDrawable(Color.TRANSPARENT)));
        //mRecyclerView.setDrawSelectorOnTop(true);
        mRecyclerView!!.clipToPadding = false
        mRecyclerView!!.clipChildren = false
        //mRecyclerView.setOnItemClickListener(this);
        //mRecyclerView.setOnItemLongClickListener(this);
        mRecyclerView!!.setChoiceMode(EasyRecyclerView.CHOICE_MODE_MULTIPLE_CUSTOM)
        mRecyclerView!!.setCustomCheckedListener(DownloadChoiceListener())
        // Cancel change animation
        val itemAnimator = mRecyclerView!!.itemAnimator
        if (itemAnimator is SimpleItemAnimator) {
            itemAnimator.supportsChangeAnimations = false
        }
        val interval = resources.getDimensionPixelOffset(R.dimen.gallery_list_interval)
        val paddingH = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h)
        val paddingV = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v)
        val decoration = MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV)
        mRecyclerView!!.addItemDecoration(decoration)
        if (mInitPosition >= 0) {
            mRecyclerView!!.scrollToPosition(mInitPosition)
            mInitPosition = -1
        }
        mFastScroller!!.attachToRecyclerView(mRecyclerView)
        val handlerDrawable = HandlerDrawable()
        handlerDrawable.setColor(theme.resolveColor(com.google.android.material.R.attr.colorPrimary))
        mFastScroller!!.setHandlerDrawable(handlerDrawable)
        mFastScroller!!.setOnDragHandlerListener(this)
        mFabLayout!!.addOnExpandListener(FabLayoutListener())
        mFabLayout!!.setExpanded(expanded = false, animation = false)
        mFabLayout!!.setHidePrimaryFab(true)
        mFabLayout!!.setAutoCancel(false)
        mFabLayout!!.setOnClickFabListener(this)
        addAboveSnackView(mFabLayout)
        updateView()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateTitle()
        setNavigationIcon(R.drawable.ic_baseline_menu_24)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (null != mRecyclerView) {
            mRecyclerView!!.stopScroll()
            mRecyclerView = null
        }
        if (null != mFabLayout) {
            removeAboveSnackView(mFabLayout)
            (mFabLayout!!.parent as ViewGroup).removeView(mFabLayout)
            mFabLayout = null
        }
        mRecyclerView = null
        mViewTransition = null
        mAdapter = null
        mLayoutManager = null
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
        if (null == activity || null == mRecyclerView || mRecyclerView!!.isInCustomChoice) {
            return false
        }
        when (item.itemId) {
            R.id.action_filter -> {
                BaseDialogBuilder(requireActivity())
                    .setSingleChoiceItems(
                        R.array.download_state,
                        mType + 1
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
                if (null != mDownloadManager) {
                    mDownloadManager!!.stopAllDownload()
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
                        if (mDownloadManager != null) {
                            mDownloadManager!!.resetAllReadingProgress()
                        }
                    }.show()
                return true
            }

            R.id.action_start_all_reversed -> {
                val list = mList ?: return true
                val gidList = LongList()
                for (i in list.size - 1 downTo -1 + 1) {
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
            if (mList == null || mList!!.size == 0) {
                mViewTransition!!.showView(1)
            } else {
                mViewTransition!!.showView(0)
            }
        }
        updateTitle()
    }

    override fun onCreateDrawerView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.drawer_list_rv, container, false)
        val context = context
        AssertUtils.assertNotNull(context)
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitle(R.string.download_labels)
        toolbar.inflateMenu(R.menu.drawer_download)
        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            val id = item.itemId
            if (id == R.id.action_add) {
                val builder =
                    EditTextDialogBuilder(context!!, null, getString(R.string.download_labels))
                builder.setTitle(R.string.new_label_title)
                builder.setPositiveButton(android.R.string.ok, null)
                val dialog = builder.show()
                NewLabelDialogHelper(builder, dialog)
                return@setOnMenuItemClickListener true
            } else if (id == R.id.action_default_download_label) {
                val dm = mDownloadManager ?: return@setOnMenuItemClickListener true
                val list = dm.labelList
                val items = arrayOfNulls<String>(list.size + 2)
                items[0] = getString(R.string.let_me_select)
                items[1] = getString(R.string.default_download_label_name)
                var i = 0
                val n = list.size
                while (i < n) {
                    items[i + 2] = list[i].label
                    i++
                }
                BaseDialogBuilder(context!!)
                    .setTitle(R.string.default_download_label)
                    .setItems(items) { _: DialogInterface?, which: Int ->
                        if (which == 0) {
                            Settings.putHasDefaultDownloadLabel(false)
                        } else {
                            Settings.putHasDefaultDownloadLabel(true)
                            val label: String? = if (which == 1) {
                                null
                            } else {
                                items[which]
                            }
                            Settings.putDefaultDownloadLabel(label)
                        }
                    }.show()
                return@setOnMenuItemClickListener true
            }
            false
        }
        initLabels()
        mLabelAdapter = DownloadLabelAdapter(inflater)
        val recyclerView = view.findViewById<EasyRecyclerView>(R.id.recycler_view_drawer)
        recyclerView.layoutManager = LinearLayoutManager(context)
        mLabelAdapter!!.setHasStableIds(true)
        val itemTouchHelper = ItemTouchHelper(DownloadLabelItemTouchHelperCallback())
        itemTouchHelper.attachToRecyclerView(recyclerView)
        recyclerView.adapter = mLabelAdapter
        return view
    }

    override fun onStartDragHandler() {
        // Lock right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
    }

    override fun onEndDragHandler() {
        // Restore right drawer
        if (null != mRecyclerView && !mRecyclerView!!.isInCustomChoice) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
        }
    }

    fun onItemClick(position: Int): Boolean {
        val activity: Activity? = mainActivity
        val recyclerView = mRecyclerView
        if (null == activity || null == recyclerView) {
            return false
        }
        return if (recyclerView.isInCustomChoice) {
            recyclerView.toggleItemChecked(position)
            true
        } else {
            val list = mList ?: return false
            if (position < 0 || position >= list.size) {
                return false
            }
            val intent = Intent(activity, ReaderActivity::class.java)
            intent.action = ReaderActivity.ACTION_EH
            intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, list[position])
            startActivity(intent)
            true
        }
    }

    fun onItemLongClick(position: Int): Boolean {
        val recyclerView = mRecyclerView ?: return false
        if (!recyclerView.isInCustomChoice) {
            recyclerView.intoCustomChoiceMode()
        }
        recyclerView.toggleItemChecked(position)
        return true
    }

    override fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton) {
        if (mRecyclerView != null && mRecyclerView!!.isInCustomChoice) {
            mRecyclerView!!.outOfCustomChoiceMode()
        }
    }

    override fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int) {
        val context = context
        val activity: Activity? = mainActivity
        val recyclerView = mRecyclerView
        if (null == context || null == activity || null == recyclerView) {
            return
        }
        if (0 == position) {
            recyclerView.checkAll()
        } else {
            val list = mList ?: return
            var gidList: LongList? = null
            var downloadInfoList: MutableList<DownloadInfo>? = null
            val collectGid = position == 1 || position == 2 || position == 3 // Start, Stop, Delete
            val collectDownloadInfo = position == 3 || position == 4 // Delete or Move
            if (collectGid) {
                gidList = LongList()
            }
            if (collectDownloadInfo) {
                downloadInfoList = LinkedList()
            }
            val stateArray = recyclerView.checkedItemPositions
            var i = 0
            val n = stateArray.size()
            while (i < n) {
                if (stateArray.valueAt(i)) {
                    val info = list[stateArray.keyAt(i)]
                    if (collectDownloadInfo) {
                        downloadInfoList!!.add(info)
                    }
                    if (collectGid) {
                        gidList!!.add(info.gid)
                    }
                }
                i++
            }
            when (position) {
                1 -> {
                    // Start
                    val intent = Intent(activity, DownloadService::class.java)
                    intent.action = DownloadService.ACTION_START_RANGE
                    intent.putExtra(DownloadService.KEY_GID_LIST, gidList)
                    ContextCompat.startForegroundService(activity, intent)
                    // Cancel check mode
                    recyclerView.outOfCustomChoiceMode()
                }

                2 -> {
                    // Stop
                    if (null != mDownloadManager) {
                        mDownloadManager!!.stopRangeDownload(gidList)
                    }
                    // Cancel check mode
                    recyclerView.outOfCustomChoiceMode()
                }

                3 -> {
                    // Delete
                    val builder = CheckBoxDialogBuilder(
                        context,
                        getString(R.string.download_remove_dialog_message_2, gidList!!.size()),
                        getString(R.string.download_remove_dialog_check_text),
                        Settings.getRemoveImageFiles()
                    )
                    val helper = DeleteRangeDialogHelper(
                        downloadInfoList, gidList, builder
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
                    var i = 0
                    val n = labelRawList.size
                    while (i < n) {
                        labelRawList[i].label?.let { labelList.add(it) }
                        i++
                    }
                    val labels = labelList.toTypedArray()
                    val helper = MoveDialogHelper(labels, downloadInfoList!!)
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
        if (mAdapter != null) {
            mAdapter!!.notifyItemInserted(position)
        }
        updateView()
    }

    override fun onUpdate(info: DownloadInfo, list: List<DownloadInfo>) {
        if (mList !== list) {
            return
        }
        val index = list.indexOf(info)
        if (index >= 0 && mAdapter != null) {
            mAdapter!!.notifyItemChanged(index)
        }
    }

    override fun onUpdateAll() {
        if (mAdapter != null) {
            mAdapter!!.notifyDataSetChanged()
        }
    }

    override fun onReload() {
        if (mAdapter != null) {
            mAdapter!!.notifyDataSetChanged()
        }
        updateView()
    }

    override fun onChange() {
        lifecycleScope.launchUI {
            mLabel = null
            updateForLabel()
            updateView()
        }
    }

    override fun onRenameLabel(from: String, to: String) {
        if (!ObjectUtils.equal(mLabel, from)) {
            return
        }
        mLabel = to
        updateForLabel()
        updateView()
    }

    override fun onRemove(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
        if (mList !== list) {
            return
        }
        if (mAdapter != null) {
            mAdapter!!.notifyItemRemoved(position)
        }
        updateView()
    }

    override fun onUpdateLabels() {
        // TODO
    }

    private fun bindForState(holder: DownloadHolder, info: DownloadInfo) {
        val context = context ?: return
        when (info.state) {
            DownloadInfo.STATE_NONE -> bindState(
                holder,
                info,
                context.getString(R.string.download_state_none)
            )

            DownloadInfo.STATE_WAIT -> bindState(
                holder,
                info,
                context.getString(R.string.download_state_wait)
            )

            DownloadInfo.STATE_DOWNLOAD -> bindProgress(holder, info)
            DownloadInfo.STATE_FAILED -> {
                val text: String = if (info.legacy <= 0) {
                    context.getString(R.string.download_state_failed)
                } else {
                    context.getString(R.string.download_state_failed_2, info.legacy)
                }
                bindState(holder, info, text)
            }

            DownloadInfo.STATE_FINISH -> bindState(
                holder,
                info,
                context.getString(R.string.download_state_finish)
            )
        }
    }

    private fun bindState(holder: DownloadHolder, info: DownloadInfo, state: String) {
        holder.uploader.visibility = View.VISIBLE
        holder.rating.visibility = View.VISIBLE
        holder.category.visibility = View.VISIBLE
        holder.state.visibility = View.VISIBLE
        holder.progressBar.visibility = View.GONE
        holder.percent.visibility = View.GONE
        holder.speed.visibility = View.GONE
        if (info.state == DownloadInfo.STATE_WAIT || info.state == DownloadInfo.STATE_DOWNLOAD) {
            holder.start.visibility = View.GONE
            holder.stop.visibility = View.VISIBLE
        } else {
            holder.start.visibility = View.VISIBLE
            holder.stop.visibility = View.GONE
        }
        holder.state.text = state
    }

    @SuppressLint("SetTextI18n")
    private fun bindProgress(holder: DownloadHolder, info: DownloadInfo) {
        holder.uploader.visibility = View.GONE
        holder.rating.visibility = View.GONE
        holder.category.visibility = View.GONE
        holder.state.visibility = View.GONE
        holder.progressBar.visibility = View.VISIBLE
        holder.percent.visibility = View.VISIBLE
        holder.speed.visibility = View.VISIBLE
        if (info.state == DownloadInfo.STATE_WAIT || info.state == DownloadInfo.STATE_DOWNLOAD) {
            holder.start.visibility = View.GONE
            holder.stop.visibility = View.VISIBLE
        } else {
            holder.start.visibility = View.VISIBLE
            holder.stop.visibility = View.GONE
        }
        if (info.total <= 0 || info.finished < 0) {
            holder.percent.text = null
            holder.progressBar.isIndeterminate = true
        } else {
            holder.percent.text = info.finished.toString() + "/" + info.total
            holder.progressBar.isIndeterminate = false
            holder.progressBar.max = info.total
            holder.progressBar.progress = info.finished
        }
        var speed = info.speed
        if (speed < 0) {
            speed = 0
        }
        holder.speed.text = FileUtils.humanReadableByteCount(speed, false) + "/S"
    }

    private class DownloadLabelHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val label: TextView
        val edit: ImageView
        val option: ImageView

        init {
            label = ViewUtils.`$$`(itemView, R.id.tv_key) as TextView
            edit = ViewUtils.`$$`(itemView, R.id.iv_edit) as ImageView
            option = ViewUtils.`$$`(itemView, R.id.iv_option) as ImageView
        }
    }

    private inner class DownloadLabelAdapter(private val mInflater: LayoutInflater) :
        RecyclerView.Adapter<DownloadLabelHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadLabelHolder {
            return DownloadLabelHolder(mInflater.inflate(R.layout.item_drawer_list, parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: DownloadLabelHolder, position: Int) {
            val context = context
            val label = mLabels[position]
            if (mDownloadManager == null) {
                if (context != null) {
                    mDownloadManager = downloadManager
                }
            }
            var list: List<DownloadInfo?>? = null
            if (mDownloadManager != null) {
                list = if (position == 0) {
                    mDownloadManager!!.defaultDownloadInfoList
                } else {
                    mDownloadManager!!.getLabelDownloadInfoList(label)
                }
            }
            if (list != null) {
                holder.label.text = label + " [" + list.size + "]"
            } else {
                holder.label.text = label
            }
            holder.itemView.setOnClickListener {
                val label1: String? = if (position == 0) {
                    null
                } else {
                    mLabels[position]
                }
                if (!ObjectUtils.equal(label1, mLabel)) {
                    mLabel = label1
                    updateForLabel()
                    updateView()
                    closeDrawer(GravityCompat.END)
                }
            }
            if (position > 0) {
                holder.edit.visibility = View.VISIBLE
                holder.edit.setOnClickListener {
                    if (context != null) {
                        val builder = EditTextDialogBuilder(
                            context, label, getString(R.string.download_labels)
                        )
                        builder.setTitle(R.string.rename_label_title)
                        builder.setPositiveButton(android.R.string.ok, null)
                        val dialog = builder.show()
                        RenameLabelDialogHelper(builder, dialog, label)
                    }
                }
            } else {
                holder.option.visibility = View.GONE
            }
        }

        override fun getItemId(position: Int): Long {
            return mLabels[position].hashCode().toLong()
        }

        override fun getItemCount(): Int {
            return mLabels.size
        }
    }

    private inner class DeleteRangeDialogHelper(
        private val mDownloadInfoList: List<DownloadInfo>?,
        private val mGidList: LongList?, private val mBuilder: CheckBoxDialogBuilder
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return
            }

            // Cancel check mode
            if (mRecyclerView != null) {
                mRecyclerView!!.outOfCustomChoiceMode()
            }

            // Delete
            if (null != mDownloadManager) {
                mDownloadManager!!.deleteRangeDownload(mGidList)
            }

            // Delete image files
            val checked = mBuilder.isChecked
            Settings.putRemoveImageFiles(checked)
            if (checked) {
                val files = arrayOfNulls<UniFile>(mDownloadInfoList!!.size)
                for ((i, info) in mDownloadInfoList.withIndex()) {
                    // Put file
                    files[i] = SpiderDen.getGalleryDownloadDir(info.gid)
                    // Remove download path
                    EhDB.removeDownloadDirname(info.gid)
                }
                // Delete file
                lifecycleScope.launchIO {
                    files.forEach { it?.delete() }
                }
            }
        }
    }

    private inner class MoveDialogHelper(
        private val mLabels: Array<String>,
        private val mDownloadInfoList: List<DownloadInfo>
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            // Cancel check mode
            context ?: return
            if (null != mRecyclerView) {
                mRecyclerView!!.outOfCustomChoiceMode()
            }
            val label: String? = if (which == 0) {
                null
            } else {
                mLabels[which]
            }
            downloadManager.changeLabel(mDownloadInfoList, label)
            if (mLabelAdapter != null) {
                mLabelAdapter!!.notifyDataSetChanged()
            }
        }
    }

    private inner class DownloadHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val thumb: LoadImageView
        val title: TextView
        val uploader: TextView
        val rating: SimpleRatingView
        val category: TextView
        val start: View
        val stop: View
        val state: TextView
        val progressBar: ProgressBar
        val percent: TextView
        val speed: TextView

        init {
            thumb = itemView.findViewById(R.id.thumb)
            title = itemView.findViewById(R.id.title)
            uploader = itemView.findViewById(R.id.uploader)
            rating = itemView.findViewById(R.id.rating)
            category = itemView.findViewById(R.id.category)
            start = itemView.findViewById(R.id.start)
            stop = itemView.findViewById(R.id.stop)
            state = itemView.findViewById(R.id.state)
            progressBar = itemView.findViewById(R.id.progress_bar)
            percent = itemView.findViewById(R.id.percent)
            speed = itemView.findViewById(R.id.speed)

            // TODO cancel on click listener when select items
            thumb.setOnClickListener(this)
            start.setOnClickListener(this)
            stop.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val context = context
            val activity: Activity? = mainActivity
            val recyclerView = mRecyclerView
            if (null == context || null == activity || null == recyclerView || recyclerView.isInCustomChoice) {
                return
            }
            val list = mList ?: return
            val size = list.size
            val index = recyclerView.getChildAdapterPosition(itemView)
            if (index < 0 || index >= size) {
                return
            }
            if (thumb === v) {
                val args = Bundle()
                args.putString(
                    GalleryDetailScene.KEY_ACTION,
                    GalleryDetailScene.ACTION_GALLERY_INFO
                )
                args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, list[index])
                val announcer = Announcer(GalleryDetailScene::class.java).setArgs(args)
                startScene(announcer)
            } else if (start === v) {
                val intent = Intent(activity, DownloadService::class.java)
                intent.action = DownloadService.ACTION_START
                intent.putExtra(DownloadService.KEY_GALLERY_INFO, list[index])
                ContextCompat.startForegroundService(activity, intent)
            } else if (stop === v) {
                if (null != mDownloadManager) {
                    mDownloadManager!!.stopDownload(list[index].gid)
                }
            }
        }
    }

    private inner class DownloadAdapter : RecyclerView.Adapter<DownloadHolder>() {
        private val mInflater: LayoutInflater = layoutInflater
        private val mListThumbWidth: Int
        private val mListThumbHeight: Int

        init {
            AssertUtils.assertNotNull(mInflater)
            @SuppressLint("InflateParams") val calculator =
                mInflater.inflate(R.layout.item_gallery_list_thumb_height, null)
            ViewUtils.measureView(calculator, 1024, ViewGroup.LayoutParams.WRAP_CONTENT)
            mListThumbHeight = calculator.measuredHeight
            mListThumbWidth = mListThumbHeight * 2 / 3
        }

        override fun getItemId(position: Int): Long {
            return if (mList == null || position < 0 || position >= mList!!.size) {
                0
            } else mList!![position].gid
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadHolder {
            val holder = DownloadHolder(mInflater.inflate(R.layout.item_download, parent, false))
            val lp = holder.thumb.layoutParams
            lp.width = mListThumbWidth
            lp.height = mListThumbHeight
            holder.thumb.layoutParams = lp
            return holder
        }

        override fun onBindViewHolder(holder: DownloadHolder, position: Int) {
            if (mList == null) {
                return
            }
            val info = mList!![position]
            if (info.thumb != null) {
                holder.thumb.load(EhCacheKeyFactory.getThumbKey(info.gid), info.thumb!!)
            }
            holder.title.text = EhUtils.getSuitableTitle(info)
            holder.uploader.text = info.uploader
            holder.rating.rating = info.rating
            val category = holder.category
            val newCategoryText = EhUtils.getCategory(info.category)
            if (!newCategoryText.contentEquals(category.text)) {
                category.text = newCategoryText
                category.setBackgroundColor(EhUtils.getCategoryColor(info.category))
            }
            bindForState(holder, info)

            // Update transition name
            ViewCompat.setTransitionName(
                holder.thumb,
                TransitionNameFactory.getThumbTransitionName(info.gid)
            )
            holder.itemView.setOnClickListener { onItemClick(position) }
            holder.itemView.setOnLongClickListener { onItemLongClick(position) }
        }

        override fun getItemCount(): Int {
            return if (mList == null) 0 else mList!!.size
        }
    }

    private inner class FabLayoutListener : OnExpandListener {
        override fun onExpand(expanded: Boolean) {
            if (!expanded && mRecyclerView != null && mRecyclerView!!.isInCustomChoice) mRecyclerView!!.outOfCustomChoiceMode()
        }
    }

    private inner class DownloadChoiceListener : CustomChoiceListener {
        override fun onIntoCustomChoice(view: EasyRecyclerView) {
            if (mFabLayout != null) {
                mFabLayout!!.isExpanded = true
            }
            // Lock drawer
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
        }

        override fun onOutOfCustomChoice(view: EasyRecyclerView) {
            if (mFabLayout != null) {
                mFabLayout!!.isExpanded = false
            }
            // Unlock drawer
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
        }

        override fun onItemCheckedStateChanged(
            view: EasyRecyclerView,
            position: Int,
            id: Long,
            checked: Boolean
        ) {
            if (view.checkedItemCount == 0) {
                view.outOfCustomChoiceMode()
            }
        }
    }

    private inner class RenameLabelDialogHelper(
        private val mBuilder: EditTextDialogBuilder, private val mDialog: AlertDialog,
        private val mOriginalLabel: String?
    ) : View.OnClickListener {
        init {
            val button: Button = mDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            button.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            context ?: return
            val text = mBuilder.text
            if (TextUtils.isEmpty(text)) {
                mBuilder.setError(getString(R.string.label_text_is_empty))
            } else if (getString(R.string.default_download_label_name) == text) {
                mBuilder.setError(getString(R.string.label_text_is_invalid))
            } else if (downloadManager.containLabel(text)) {
                mBuilder.setError(getString(R.string.label_text_exist))
            } else {
                mBuilder.setError(null)
                mDialog.dismiss()
                downloadManager.renameLabel(mOriginalLabel!!, text)
                if (mLabelAdapter != null) {
                    initLabels()
                    mLabelAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    private inner class NewLabelDialogHelper(
        private val mBuilder: EditTextDialogBuilder,
        private val mDialog: AlertDialog
    ) : View.OnClickListener {
        init {
            val button: Button = mDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            button.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            context ?: return
            val text = mBuilder.text
            if (TextUtils.isEmpty(text)) {
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
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeMovementFlags(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT
            )
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            val context = context
            if (null == context || fromPosition == toPosition || toPosition == 0 || fromPosition == 0) {
                return false
            }
            downloadManager.moveLabel(fromPosition - 1, toPosition - 1)
            val item = mLabels.removeAt(fromPosition)
            mLabels.add(toPosition, item)
            mLabelAdapter!!.notifyDataSetChanged()
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            if (mDownloadManager == null) return
            lifecycleScope.launchIO {
                if (position != 0) {
                    val label = mLabels[position]
                    mDownloadManager!!.deleteLabel(label)
                }
                mLabels.removeAt(position)
                withUIContext {
                    mLabelAdapter!!.notifyItemRemoved(position)
                }
            }
        }
    }

    companion object {
        const val KEY_GID = "gid"
        const val KEY_ACTION = "action"
        const val ACTION_CLEAR_DOWNLOAD_SERVICE = "clear_download_service"
        private val TAG = DownloadsScene::class.java.simpleName
        private const val KEY_LABEL = "label"
    }
}