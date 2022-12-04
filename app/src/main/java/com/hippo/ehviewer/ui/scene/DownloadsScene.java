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

package com.hippo.ehviewer.ui.scene;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hippo.app.BaseDialogBuilder;
import com.hippo.app.CheckBoxDialogBuilder;
import com.hippo.app.EditTextDialogBuilder;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.FastScroller;
import com.hippo.easyrecyclerview.HandlerDrawable;
import com.hippo.easyrecyclerview.MarginItemDecoration;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.dao.DownloadInfo;
import com.hippo.ehviewer.dao.DownloadLabel;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.download.DownloadService;
import com.hippo.ehviewer.spider.SpiderDen;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.scene.Announcer;
import com.hippo.unifile.UniFile;
import com.hippo.util.IoThreadPoolExecutor;
import com.hippo.view.ViewTransition;
import com.hippo.widget.FabLayout;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.ObjectUtils;
import com.hippo.yorozuya.ViewUtils;
import com.hippo.yorozuya.collect.LongList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import rikka.core.res.ResourcesKt;

@SuppressLint("RtlHardcoded")
public class DownloadsScene extends ToolbarScene
        implements DownloadManager.DownloadInfoListener,
        FabLayout.OnClickFabListener, FastScroller.OnDragHandlerListener {

    public static final String KEY_GID = "gid";
    public static final String KEY_ACTION = "action";
    public static final String ACTION_CLEAR_DOWNLOAD_SERVICE = "clear_download_service";
    private static final String TAG = DownloadsScene.class.getSimpleName();
    private static final String KEY_LABEL = "label";
    /*---------------
     Whole life cycle
     ---------------*/
    @Nullable
    private DownloadManager mDownloadManager;
    @Nullable
    private String mLabel;
    @Nullable
    private List<DownloadInfo> mList;

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private TextView mTip;
    @Nullable
    private FastScroller mFastScroller;
    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private ViewTransition mViewTransition;
    @Nullable
    private FabLayout mFabLayout;
    @Nullable
    private DownloadAdapter mAdapter;
    @Nullable
    private AutoStaggeredGridLayoutManager mLayoutManager;

    private int mInitPosition = -1;

    private DownloadLabelAdapter mLabelAdapter;

    private List<String> mLabels;

    private int mType = -1;

    private static void deleteFileAsync(UniFile... files) {
        //noinspection deprecation
        new AsyncTask<UniFile, Void, Void>() {
            @Override
            protected Void doInBackground(UniFile... params) {
                for (UniFile file : params) {
                    if (file != null) {
                        file.delete();
                    }
                }
                return null;
            }
        }.executeOnExecutor(IoThreadPoolExecutor.getInstance(), files);
    }

    @Override
    public int getNavCheckedItem() {
        return R.id.nav_downloads;
    }

    private void initLabels() {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        List<DownloadLabel> listLabel = EhApplication.getDownloadManager().getLabelList();
        mLabels = new ArrayList<>(listLabel.size() + 1);
        // Add default label name
        mLabels.add(getString(R.string.default_download_label_name));
        for (DownloadLabel raw : listLabel) {
            mLabels.add(raw.getLabel());
        }
    }

    private boolean handleArguments(Bundle args) {
        if (null == args) {
            return false;
        }

        if (ACTION_CLEAR_DOWNLOAD_SERVICE.equals(args.getString(KEY_ACTION))) {
            DownloadService.clear();
        }

        long gid;
        if (null != mDownloadManager && -1L != (gid = args.getLong(KEY_GID, -1L))) {
            DownloadInfo info = mDownloadManager.getDownloadInfo(gid);
            if (null != info) {
                mLabel = info.getLabel();
                updateForLabel();
                updateView();

                // Get position
                if (null != mList) {
                    int position = mList.indexOf(info);
                    if (position >= 0 && null != mRecyclerView) {
                        mRecyclerView.scrollToPosition(position);
                    } else {
                        mInitPosition = position;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNewArguments(@NonNull Bundle args) {
        handleArguments(args);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        AssertUtils.assertNotNull(context);
        mDownloadManager = EhApplication.getDownloadManager();
        mDownloadManager.addDownloadInfoListener(this);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mList = null;

        DownloadManager manager = mDownloadManager;
        if (null == manager) {
            Context context = getContext();
            if (null != context) {
                manager = EhApplication.getDownloadManager();
            }
        } else {
            mDownloadManager = null;
        }

        if (null != manager) {
            manager.removeDownloadInfoListener(this);
        } else {
            Log.e(TAG, "Can't removeDownloadInfoListener");
        }
    }

    private void updateForLabel() {
        if (null == mDownloadManager) {
            return;
        }
        List<DownloadInfo> list;
        if (mLabel == null) {
            list = mDownloadManager.getDefaultDownloadInfoList();
        } else {
            list = mDownloadManager.getLabelDownloadInfoList(mLabel);
            if (list == null) {
                mLabel = null;
                list = mDownloadManager.getDefaultDownloadInfoList();
            }
        }

        if (mType != -1) {
            mList = new ArrayList<>();
            for (DownloadInfo info : list) {
                if (info.state == mType) {
                    mList.add(info);
                }
            }
        } else {
            mList = list;
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        updateTitle();
        Settings.putRecentDownloadLabel(mLabel);
    }

    private void updateTitle() {
        setTitle(getString(R.string.scene_download_title,
                mLabel != null ? mLabel : getString(R.string.default_download_label_name)));
    }

    private void onInit() {
        if (!handleArguments(getArguments())) {
            mLabel = Settings.getRecentDownloadLabel();
            updateForLabel();
        }
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mLabel = savedInstanceState.getString(KEY_LABEL);
        updateForLabel();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_LABEL, mLabel);
    }

    @Nullable
    @Override
    public View onCreateViewWithToolbar(LayoutInflater inflater,
                                        @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_download, container, false);

        View content = ViewUtils.$$(view, R.id.content);
        mRecyclerView = (EasyRecyclerView) ViewUtils.$$(content, R.id.recycler_view);
        setLiftOnScrollTargetView(mRecyclerView);
        mFastScroller = (FastScroller) ViewUtils.$$(content, R.id.fast_scroller);
        mFabLayout = (FabLayout) ViewUtils.$$(view, R.id.fab_layout);

        // Workaround
        ((ViewGroup) mFabLayout.getParent()).removeView(mFabLayout);
        container.addView(mFabLayout);

        mTip = (TextView) ViewUtils.$$(view, R.id.tip);
        mViewTransition = new ViewTransition(content, mTip);

        Context context = getContext();
        AssertUtils.assertNotNull(content);
        Resources resources = context.getResources();

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.big_download);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        mTip.setCompoundDrawables(null, drawable, null, null);

        mAdapter = new DownloadAdapter();
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        mLayoutManager = new AutoStaggeredGridLayoutManager(0, StaggeredGridLayoutManager.VERTICAL);
        mLayoutManager.setColumnSize(resources.getDimensionPixelOffset(Settings.getDetailSizeResId()));
        mLayoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE);
        mRecyclerView.setLayoutManager(mLayoutManager);
        //mRecyclerView.setSelector(Ripple.generateRippleDrawable(context, !ResourcesKt.resolveColor(getTheme(), .getAttrBoolean(context, R.attr.isLightTheme), new ColorDrawable(Color.TRANSPARENT)));
        //mRecyclerView.setDrawSelectorOnTop(true);
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setClipChildren(false);
        //mRecyclerView.setOnItemClickListener(this);
        //mRecyclerView.setOnItemLongClickListener(this);
        mRecyclerView.setChoiceMode(EasyRecyclerView.CHOICE_MODE_MULTIPLE_CUSTOM);
        mRecyclerView.setCustomCheckedListener(new DownloadChoiceListener());
        // Cancel change animation
        RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }
        int interval = resources.getDimensionPixelOffset(R.dimen.gallery_list_interval);
        int paddingH = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h);
        int paddingV = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v);
        MarginItemDecoration decoration = new MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV);
        mRecyclerView.addItemDecoration(decoration);
        if (mInitPosition >= 0) {
            mRecyclerView.scrollToPosition(mInitPosition);
            mInitPosition = -1;
        }

        mFastScroller.attachToRecyclerView(mRecyclerView);
        HandlerDrawable handlerDrawable = new HandlerDrawable();
        handlerDrawable.setColor(ResourcesKt.resolveColor(getTheme(), com.google.android.material.R.attr.colorPrimary));
        mFastScroller.setHandlerDrawable(handlerDrawable);
        mFastScroller.setOnDragHandlerListener(this);

        mFabLayout.setExpanded(false, false);
        mFabLayout.setHidePrimaryFab(true);
        mFabLayout.setAutoCancel(false);
        mFabLayout.setOnClickFabListener(this);
        addAboveSnackView(mFabLayout);

        updateView();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateTitle();
        setNavigationIcon(R.drawable.ic_baseline_menu_24);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mRecyclerView) {
            mRecyclerView.stopScroll();
            mRecyclerView = null;
        }
        if (null != mFabLayout) {
            removeAboveSnackView(mFabLayout);
            ((ViewGroup) mFabLayout.getParent()).removeView(mFabLayout);
            mFabLayout = null;
        }

        mRecyclerView = null;
        mViewTransition = null;
        mAdapter = null;
        mLayoutManager = null;
    }

    @Override
    public void onNavigationClick() {
        toggleDrawer(Gravity.LEFT);
    }

    @Override
    public int getMenuResId() {
        return R.menu.scene_download;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        // Skip when in choice mode
        Activity activity = getMainActivity();
        if (null == activity || null == mRecyclerView || mRecyclerView.isInCustomChoice()) {
            return false;
        }

        int id = item.getItemId();
        if (id == R.id.action_filter) {
            new BaseDialogBuilder(requireActivity())
                    .setSingleChoiceItems(R.array.download_state, mType + 1, (dialog, which) -> {
                        mType = which - 1;
                        updateForLabel();
                        updateView();
                        dialog.dismiss();
                    })
                    .show();
            return true;
        } else if (id == R.id.action_start_all) {
            Intent intent = new Intent(activity, DownloadService.class);
            intent.setAction(DownloadService.ACTION_START_ALL);
            ContextCompat.startForegroundService(activity, intent);
            return true;
        } else if (id == R.id.action_stop_all) {
            if (null != mDownloadManager) {
                mDownloadManager.stopAllDownload();
            }
            return true;
        } else if (id == R.id.action_open_download_labels) {
            openDrawer(Gravity.RIGHT);
            return true;
        } else if (id == R.id.action_reset_reading_progress) {
            new BaseDialogBuilder(getContext())
                    .setMessage(R.string.reset_reading_progress_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (mDownloadManager != null) {
                            mDownloadManager.resetAllReadingProgress();
                        }
                    }).show();
            return true;
        } else if (id == R.id.action_start_all_reversed) {
            List<DownloadInfo> list = mList;
            if (list == null) {
                return true;
            }
            LongList gidList = new LongList();
            for (int i = list.size() - 1; i > -1; i--) {
                DownloadInfo info = list.get(i);
                if (info.state != DownloadInfo.STATE_FINISH) {
                    gidList.add(info.gid);
                }
            }
            Intent intent = new Intent(activity, DownloadService.class);
            intent.setAction(DownloadService.ACTION_START_RANGE);
            intent.putExtra(DownloadService.KEY_GID_LIST, gidList);
            ContextCompat.startForegroundService(activity, intent);
            return true;
        }
        return false;
    }

    public void updateView() {
        if (mViewTransition != null) {
            if (mList == null || mList.size() == 0) {
                mViewTransition.showView(1);
            } else {
                mViewTransition.showView(0);
            }
        }
    }

    @Override
    public View onCreateDrawerView(LayoutInflater inflater,
                                   @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.drawer_list_rv, container, false);

        final Context context = getContext();
        AssertUtils.assertNotNull(context);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.download_labels);
        toolbar.inflateMenu(R.menu.drawer_download);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_add) {
                EditTextDialogBuilder builder = new EditTextDialogBuilder(context, null, getString(R.string.download_labels));
                builder.setTitle(R.string.new_label_title);
                builder.setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder.show();
                new NewLabelDialogHelper(builder, dialog);
                return true;
            } else if (id == R.id.action_default_download_label) {
                DownloadManager dm = mDownloadManager;
                if (null == dm) {
                    return true;
                }

                List<DownloadLabel> list = dm.getLabelList();
                final String[] items = new String[list.size() + 2];
                items[0] = getString(R.string.let_me_select);
                items[1] = getString(R.string.default_download_label_name);
                for (int i = 0, n = list.size(); i < n; i++) {
                    items[i + 2] = list.get(i).getLabel();
                }
                new BaseDialogBuilder(context)
                        .setTitle(R.string.default_download_label)
                        .setItems(items, (dialog1, which) -> {
                            if (which == 0) {
                                Settings.putHasDefaultDownloadLabel(false);
                            } else {
                                Settings.putHasDefaultDownloadLabel(true);
                                String label;
                                if (which == 1) {
                                    label = null;
                                } else {
                                    label = items[which];
                                }
                                Settings.putDefaultDownloadLabel(label);
                            }
                        }).show();
                return true;
            }
            return false;
        });

        initLabels();

        mLabelAdapter = new DownloadLabelAdapter(inflater);
        final EasyRecyclerView recyclerView = view.findViewById(R.id.recycler_view_drawer);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        mLabelAdapter.setHasStableIds(true);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new DownloadLabelItemTouchHelperCallback());
        itemTouchHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(mLabelAdapter);

        return view;
    }

    @Override
    public void onBackPressed() {
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            mRecyclerView.outOfCustomChoiceMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onStartDragHandler() {
        // Lock right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
    }

    @Override
    public void onEndDragHandler() {
        // Restore right drawer
        if (null != mRecyclerView && !mRecyclerView.isInCustomChoice()) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
        }
    }

    public boolean onItemClick(int position) {
        Activity activity = getMainActivity();
        EasyRecyclerView recyclerView = mRecyclerView;
        if (null == activity || null == recyclerView) {
            return false;
        }

        if (recyclerView.isInCustomChoice()) {
            recyclerView.toggleItemChecked(position);
            return true;
        } else {
            List<DownloadInfo> list = mList;
            if (list == null) {
                return false;
            }
            if (position < 0 || position >= list.size()) {
                return false;
            }

            Intent intent = new Intent(activity, GalleryActivity.class);
            intent.setAction(GalleryActivity.ACTION_EH);
            intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, list.get(position));
            startActivity(intent);
            return true;
        }
    }

    public boolean onItemLongClick(int position) {
        EasyRecyclerView recyclerView = mRecyclerView;
        if (recyclerView == null) {
            return false;
        }

        if (!recyclerView.isInCustomChoice()) {
            recyclerView.intoCustomChoiceMode();
        }
        recyclerView.toggleItemChecked(position);

        return true;
    }

    @Override
    public void onClickPrimaryFab(FabLayout view, FloatingActionButton fab) {
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            mRecyclerView.outOfCustomChoiceMode();
        }
    }

    @Override
    public void onClickSecondaryFab(FabLayout view, FloatingActionButton fab, int position) {
        Context context = getContext();
        Activity activity = getMainActivity();
        EasyRecyclerView recyclerView = mRecyclerView;
        if (null == context || null == activity || null == recyclerView) {
            return;
        }

        if (0 == position) {
            recyclerView.checkAll();
        } else {
            List<DownloadInfo> list = mList;
            if (list == null) {
                return;
            }

            LongList gidList = null;
            List<DownloadInfo> downloadInfoList = null;
            boolean collectGid = position == 1 || position == 2 || position == 3; // Start, Stop, Delete
            boolean collectDownloadInfo = position == 3 || position == 4; // Delete or Move
            if (collectGid) {
                gidList = new LongList();
            }
            if (collectDownloadInfo) {
                downloadInfoList = new LinkedList<>();
            }

            SparseBooleanArray stateArray = recyclerView.getCheckedItemPositions();
            for (int i = 0, n = stateArray.size(); i < n; i++) {
                if (stateArray.valueAt(i)) {
                    DownloadInfo info = list.get(stateArray.keyAt(i));
                    if (collectDownloadInfo) {
                        downloadInfoList.add(info);
                    }
                    if (collectGid) {
                        gidList.add(info.gid);
                    }
                }
            }

            switch (position) {
                case 1: { // Start
                    Intent intent = new Intent(activity, DownloadService.class);
                    intent.setAction(DownloadService.ACTION_START_RANGE);
                    intent.putExtra(DownloadService.KEY_GID_LIST, gidList);
                    ContextCompat.startForegroundService(activity, intent);
                    // Cancel check mode
                    recyclerView.outOfCustomChoiceMode();
                    break;
                }
                case 2: { // Stop
                    if (null != mDownloadManager) {
                        mDownloadManager.stopRangeDownload(gidList);
                    }
                    // Cancel check mode
                    recyclerView.outOfCustomChoiceMode();
                    break;
                }
                case 3: { // Delete
                    CheckBoxDialogBuilder builder = new CheckBoxDialogBuilder(context,
                            getString(R.string.download_remove_dialog_message_2, gidList.size()),
                            getString(R.string.download_remove_dialog_check_text),
                            Settings.getRemoveImageFiles());
                    DeleteRangeDialogHelper helper = new DeleteRangeDialogHelper(
                            downloadInfoList, gidList, builder);
                    builder.setTitle(R.string.download_remove_dialog_title)
                            .setPositiveButton(android.R.string.ok, helper)
                            .show();
                    break;
                }
                case 4: {// Move
                    List<DownloadLabel> labelRawList = EhApplication.getDownloadManager().getLabelList();
                    List<String> labelList = new ArrayList<>(labelRawList.size() + 1);
                    labelList.add(getString(R.string.default_download_label_name));
                    for (int i = 0, n = labelRawList.size(); i < n; i++) {
                        labelList.add(labelRawList.get(i).getLabel());
                    }
                    String[] labels = labelList.toArray(new String[0]);

                    MoveDialogHelper helper = new MoveDialogHelper(labels, downloadInfoList);

                    new BaseDialogBuilder(context)
                            .setTitle(R.string.download_move_dialog_title)
                            .setItems(labels, helper)
                            .show();
                    break;
                }
            }
        }
    }

    @Override
    public void onAdd(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list, int position) {
        if (mList != list) {
            return;
        }
        if (mAdapter != null) {
            mAdapter.notifyItemInserted(position);
        }
        updateView();
    }

    @Override
    public void onUpdate(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list) {
        if (mList != list) {
            return;
        }

        int index = list.indexOf(info);
        if (index >= 0 && mAdapter != null) {
            mAdapter.notifyItemChanged(index);
        }
    }

    @Override
    public void onUpdateAll() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onReload() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        updateView();
    }

    @Override
    public void onChange() {
        mLabel = null;
        updateForLabel();
        updateView();
    }

    @Override
    public void onRenameLabel(String from, String to) {
        if (!ObjectUtils.equal(mLabel, from)) {
            return;
        }

        mLabel = to;
        updateForLabel();
        updateView();
    }

    @Override
    public void onRemove(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list, int position) {
        if (mList != list) {
            return;
        }
        if (mAdapter != null) {
            mAdapter.notifyItemRemoved(position);
        }
        updateView();
    }

    @Override
    public void onUpdateLabels() {
        // TODO
    }

    private void bindForState(DownloadHolder holder, DownloadInfo info) {
        Context context = getContext();
        if (null == context) {
            return;
        }

        switch (info.state) {
            case DownloadInfo.STATE_NONE:
                bindState(holder, info, context.getString(R.string.download_state_none));
                break;
            case DownloadInfo.STATE_WAIT:
                bindState(holder, info, context.getString(R.string.download_state_wait));
                break;
            case DownloadInfo.STATE_DOWNLOAD:
                bindProgress(holder, info);
                break;
            case DownloadInfo.STATE_FAILED:
                String text;
                if (info.legacy <= 0) {
                    text = context.getString(R.string.download_state_failed);
                } else {
                    text = context.getString(R.string.download_state_failed_2, info.legacy);
                }
                bindState(holder, info, text);
                break;
            case DownloadInfo.STATE_FINISH:
                bindState(holder, info, context.getString(R.string.download_state_finish));
                break;
        }
    }

    private void bindState(DownloadHolder holder, DownloadInfo info, String state) {
        holder.uploader.setVisibility(View.VISIBLE);
        holder.rating.setVisibility(View.VISIBLE);
        holder.category.setVisibility(View.VISIBLE);
        holder.state.setVisibility(View.VISIBLE);
        holder.progressBar.setVisibility(View.GONE);
        holder.percent.setVisibility(View.GONE);
        holder.speed.setVisibility(View.GONE);
        if (info.state == DownloadInfo.STATE_WAIT || info.state == DownloadInfo.STATE_DOWNLOAD) {
            holder.start.setVisibility(View.GONE);
            holder.stop.setVisibility(View.VISIBLE);
        } else {
            holder.start.setVisibility(View.VISIBLE);
            holder.stop.setVisibility(View.GONE);
        }

        holder.state.setText(state);
    }

    @SuppressLint("SetTextI18n")
    private void bindProgress(DownloadHolder holder, DownloadInfo info) {
        holder.uploader.setVisibility(View.GONE);
        holder.rating.setVisibility(View.GONE);
        holder.category.setVisibility(View.GONE);
        holder.state.setVisibility(View.GONE);
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.percent.setVisibility(View.VISIBLE);
        holder.speed.setVisibility(View.VISIBLE);
        if (info.state == DownloadInfo.STATE_WAIT || info.state == DownloadInfo.STATE_DOWNLOAD) {
            holder.start.setVisibility(View.GONE);
            holder.stop.setVisibility(View.VISIBLE);
        } else {
            holder.start.setVisibility(View.VISIBLE);
            holder.stop.setVisibility(View.GONE);
        }

        if (info.total <= 0 || info.finished < 0) {
            holder.percent.setText(null);
            holder.progressBar.setIndeterminate(true);
        } else {
            holder.percent.setText(info.finished + "/" + info.total);
            holder.progressBar.setIndeterminate(false);
            holder.progressBar.setMax(info.total);
            holder.progressBar.setProgress(info.finished);
        }
        long speed = info.speed;
        if (speed < 0) {
            speed = 0;
        }
        holder.speed.setText(FileUtils.humanReadableByteCount(speed, false) + "/S");
    }

    private static class DownloadLabelHolder extends RecyclerView.ViewHolder {

        private final TextView label;
        private final ImageView option;

        private DownloadLabelHolder(View itemView) {
            super(itemView);
            label = (TextView) ViewUtils.$$(itemView, R.id.tv_key);
            option = (ImageView) ViewUtils.$$(itemView, R.id.iv_option);
        }
    }

    private class DownloadLabelAdapter extends RecyclerView.Adapter<DownloadLabelHolder> {

        private final LayoutInflater mInflater;

        private DownloadLabelAdapter(LayoutInflater inflater) {
            this.mInflater = inflater;
        }

        @NonNull
        @Override
        public DownloadLabelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new DownloadLabelHolder(mInflater.inflate(R.layout.item_drawer_list, parent, false));
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull DownloadLabelHolder holder, int position) {
            if (mLabels != null) {
                Context context = getContext();
                String label = mLabels.get(position);
                if (mDownloadManager == null) {
                    if (context != null) {
                        mDownloadManager = EhApplication.getDownloadManager();
                    }
                }
                List<DownloadInfo> list = null;
                if (mDownloadManager != null) {
                    if (position == 0) {
                        list = mDownloadManager.getDefaultDownloadInfoList();
                    } else {
                        list = mDownloadManager.getLabelDownloadInfoList(label);
                    }
                }
                if (list != null) {
                    holder.label.setText(label + " [" + list.size() + "]");
                } else {
                    holder.label.setText(label);
                }
                holder.itemView.setOnClickListener(v -> {
                    String label1;
                    if (position == 0) {
                        label1 = null;
                    } else {
                        label1 = mLabels.get(position);
                    }
                    if (!ObjectUtils.equal(label1, mLabel)) {
                        mLabel = label1;
                        updateForLabel();
                        updateView();
                        closeDrawer(Gravity.RIGHT);
                    }
                });
                if (position > 0) {
                    holder.option.setVisibility(View.VISIBLE);
                    holder.option.setOnClickListener(v -> {
                        if (context != null) {
                            EditTextDialogBuilder builder = new EditTextDialogBuilder(
                                    context, label, getString(R.string.download_labels));
                            builder.setTitle(R.string.rename_label_title);
                            builder.setPositiveButton(android.R.string.ok, null);
                            AlertDialog dialog = builder.show();
                            new RenameLabelDialogHelper(builder, dialog, label);
                        }
                    });
                } else {
                    holder.option.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public long getItemId(int position) {
            return mLabels != null ? mLabels.get(position).hashCode() : 0;
        }

        @Override
        public int getItemCount() {
            return mLabels != null ? mLabels.size() : 0;
        }
    }

    private class DeleteRangeDialogHelper implements DialogInterface.OnClickListener {

        private final List<DownloadInfo> mDownloadInfoList;
        private final LongList mGidList;
        private final CheckBoxDialogBuilder mBuilder;

        public DeleteRangeDialogHelper(List<DownloadInfo> downloadInfoList,
                                       LongList gidList, CheckBoxDialogBuilder builder) {
            mDownloadInfoList = downloadInfoList;
            mGidList = gidList;
            mBuilder = builder;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return;
            }

            // Cancel check mode
            if (mRecyclerView != null) {
                mRecyclerView.outOfCustomChoiceMode();
            }

            // Delete
            if (null != mDownloadManager) {
                mDownloadManager.deleteRangeDownload(mGidList);
            }

            // Delete image files
            boolean checked = mBuilder.isChecked();
            Settings.putRemoveImageFiles(checked);
            if (checked) {
                UniFile[] files = new UniFile[mDownloadInfoList.size()];
                int i = 0;
                for (DownloadInfo info : mDownloadInfoList) {
                    // Remove download path
                    EhDB.removeDownloadDirname(info.gid);
                    // Put file
                    files[i] = SpiderDen.getGalleryDownloadDir(info);
                    i++;
                }
                // Delete file
                deleteFileAsync(files);
            }
        }
    }

    private class MoveDialogHelper implements DialogInterface.OnClickListener {

        private final String[] mLabels;
        private final List<DownloadInfo> mDownloadInfoList;

        public MoveDialogHelper(String[] labels, List<DownloadInfo> downloadInfoList) {
            mLabels = labels;
            mDownloadInfoList = downloadInfoList;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            // Cancel check mode
            Context context = getContext();
            if (null == context) {
                return;
            }
            if (null != mRecyclerView) {
                mRecyclerView.outOfCustomChoiceMode();
            }

            String label;
            if (which == 0) {
                label = null;
            } else {
                label = mLabels[which];
            }
            EhApplication.getDownloadManager().changeLabel(mDownloadInfoList, label);
            if (mLabelAdapter != null) {
                mLabelAdapter.notifyDataSetChanged();
            }
        }
    }

    private class DownloadHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final LoadImageView thumb;
        public final TextView title;
        public final TextView uploader;
        public final SimpleRatingView rating;
        public final TextView category;
        public final View start;
        public final View stop;
        public final TextView state;
        public final ProgressBar progressBar;
        public final TextView percent;
        public final TextView speed;

        public DownloadHolder(View itemView) {
            super(itemView);

            thumb = itemView.findViewById(R.id.thumb);
            title = itemView.findViewById(R.id.title);
            uploader = itemView.findViewById(R.id.uploader);
            rating = itemView.findViewById(R.id.rating);
            category = itemView.findViewById(R.id.category);
            start = itemView.findViewById(R.id.start);
            stop = itemView.findViewById(R.id.stop);
            state = itemView.findViewById(R.id.state);
            progressBar = itemView.findViewById(R.id.progress_bar);
            percent = itemView.findViewById(R.id.percent);
            speed = itemView.findViewById(R.id.speed);

            // TODO cancel on click listener when select items
            thumb.setOnClickListener(this);
            start.setOnClickListener(this);
            stop.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Context context = getContext();
            Activity activity = getMainActivity();
            EasyRecyclerView recyclerView = mRecyclerView;
            if (null == context || null == activity || null == recyclerView || recyclerView.isInCustomChoice()) {
                return;
            }
            List<DownloadInfo> list = mList;
            if (list == null) {
                return;
            }
            int size = list.size();
            int index = recyclerView.getChildAdapterPosition(itemView);
            if (index < 0 || index >= size) {
                return;
            }

            if (thumb == v) {
                Bundle args = new Bundle();
                args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO);
                args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, list.get(index));
                Announcer announcer = new Announcer(GalleryDetailScene.class).setArgs(args);
                startScene(announcer);
            } else if (start == v) {
                Intent intent = new Intent(activity, DownloadService.class);
                intent.setAction(DownloadService.ACTION_START);
                intent.putExtra(DownloadService.KEY_GALLERY_INFO, list.get(index));
                ContextCompat.startForegroundService(activity, intent);
            } else if (stop == v) {
                if (null != mDownloadManager) {
                    mDownloadManager.stopDownload(list.get(index).gid);
                }
            }
        }
    }

    private class DownloadAdapter extends RecyclerView.Adapter<DownloadHolder> {

        private final LayoutInflater mInflater;
        private final int mListThumbWidth;
        private final int mListThumbHeight;

        public DownloadAdapter() {
            mInflater = getLayoutInflater();
            AssertUtils.assertNotNull(mInflater);

            @SuppressLint("InflateParams") View calculator = mInflater.inflate(R.layout.item_gallery_list_thumb_height, null);
            ViewUtils.measureView(calculator, 1024, ViewGroup.LayoutParams.WRAP_CONTENT);
            mListThumbHeight = calculator.getMeasuredHeight();
            mListThumbWidth = mListThumbHeight * 2 / 3;
        }

        @Override
        public long getItemId(int position) {
            if (mList == null || position < 0 || position >= mList.size()) {
                return 0;
            }
            return mList.get(position).gid;
        }

        @NonNull
        @Override
        public DownloadHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            DownloadHolder holder = new DownloadHolder(mInflater.inflate(R.layout.item_download, parent, false));

            ViewGroup.LayoutParams lp = holder.thumb.getLayoutParams();
            lp.width = mListThumbWidth;
            lp.height = mListThumbHeight;
            holder.thumb.setLayoutParams(lp);

            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull DownloadHolder holder, int position) {
            if (mList == null) {
                return;
            }
            DownloadInfo info = mList.get(position);
            holder.thumb.load(EhCacheKeyFactory.getThumbKey(info.gid), info.thumb);
            holder.title.setText(EhUtils.getSuitableTitle(info));
            holder.uploader.setText(info.uploader);
            holder.rating.setRating(info.rating);
            TextView category = holder.category;
            String newCategoryText = EhUtils.getCategory(info.category);
            if (!newCategoryText.contentEquals(category.getText())) {
                category.setText(newCategoryText);
                category.setBackgroundColor(EhUtils.getCategoryColor(info.category));
            }
            bindForState(holder, info);

            // Update transition name
            ViewCompat.setTransitionName(holder.thumb, TransitionNameFactory.getThumbTransitionName(info.gid));

            holder.itemView.setOnClickListener(v -> onItemClick(position));
            holder.itemView.setOnLongClickListener(v -> onItemLongClick(position));
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }
    }

    private class DownloadChoiceListener implements EasyRecyclerView.CustomChoiceListener {

        @Override
        public void onIntoCustomChoice(EasyRecyclerView view) {
            if (mFabLayout != null) {
                mFabLayout.setExpanded(true);
            }
            // Lock drawer
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
        }

        @Override
        public void onOutOfCustomChoice(EasyRecyclerView view) {
            if (mFabLayout != null) {
                mFabLayout.setExpanded(false);
            }
            // Unlock drawer
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
        }

        @Override
        public void onItemCheckedStateChanged(EasyRecyclerView view, int position, long id, boolean checked) {
            if (view.getCheckedItemCount() == 0) {
                view.outOfCustomChoiceMode();
            }
        }
    }

    private class RenameLabelDialogHelper implements View.OnClickListener {

        private final EditTextDialogBuilder mBuilder;
        private final AlertDialog mDialog;
        private final String mOriginalLabel;

        private RenameLabelDialogHelper(EditTextDialogBuilder builder, AlertDialog dialog,
                                        String originalLabel) {
            mBuilder = builder;
            mDialog = dialog;
            mOriginalLabel = originalLabel;
            Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            Context context = getContext();
            if (null == context) {
                return;
            }

            String text = mBuilder.getText();
            if (TextUtils.isEmpty(text)) {
                mBuilder.setError(getString(R.string.label_text_is_empty));
            } else if (getString(R.string.default_download_label_name).equals(text)) {
                mBuilder.setError(getString(R.string.label_text_is_invalid));
            } else if (EhApplication.getDownloadManager().containLabel(text)) {
                mBuilder.setError(getString(R.string.label_text_exist));
            } else {
                mBuilder.setError(null);
                mDialog.dismiss();
                EhApplication.getDownloadManager().renameLabel(mOriginalLabel, text);
                if (mLabelAdapter != null) {
                    initLabels();
                    mLabelAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private class NewLabelDialogHelper implements View.OnClickListener {

        private final EditTextDialogBuilder mBuilder;
        private final AlertDialog mDialog;

        private NewLabelDialogHelper(EditTextDialogBuilder builder, AlertDialog dialog) {
            mBuilder = builder;
            mDialog = dialog;
            Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            Context context = getContext();
            if (null == context) {
                return;
            }

            String text = mBuilder.getText();
            if (TextUtils.isEmpty(text)) {
                mBuilder.setError(getString(R.string.label_text_is_empty));
            } else if (getString(R.string.default_download_label_name).equals(text)) {
                mBuilder.setError(getString(R.string.label_text_is_invalid));
            } else if (EhApplication.getDownloadManager().containLabel(text)) {
                mBuilder.setError(getString(R.string.label_text_exist));
            } else {
                mBuilder.setError(null);
                mDialog.dismiss();
                EhApplication.getDownloadManager().addLabel(text);
                initLabels();
                if (mLabelAdapter != null && mLabels != null) {
                    mLabelAdapter.notifyItemInserted(mLabels.size() - 1);
                }
            }
        }
    }

    private class DownloadLabelItemTouchHelperCallback extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT);
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getBindingAdapterPosition();
            int toPosition = target.getBindingAdapterPosition();
            Context context = getContext();
            if (null == context || fromPosition == toPosition || toPosition == 0 || fromPosition == 0) {
                return false;
            }

            EhApplication.getDownloadManager().moveLabel(fromPosition - 1, toPosition - 1);
            final String item = mLabels.remove(fromPosition);
            mLabels.add(toPosition, item);
            mLabelAdapter.notifyDataSetChanged();
            return true;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getBindingAdapterPosition();
            if (mDownloadManager == null)
                return;
            if (position != 0) {
                String label = mLabels.get(position);
                mDownloadManager.deleteLabel(label);
            }
            mLabels.remove(position);
            mLabelAdapter.notifyDataSetChanged();
        }
    }
}
