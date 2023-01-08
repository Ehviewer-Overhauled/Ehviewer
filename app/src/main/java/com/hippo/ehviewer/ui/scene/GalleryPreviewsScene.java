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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;
import com.hippo.app.BaseDialogBuilder;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.MarginItemDecoration;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.GalleryPreview;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.ehviewer.client.exception.EhException;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.widget.ContentLayout;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.recyclerview.AutoGridLayoutManager;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.ViewUtils;

import java.util.ArrayList;
import java.util.Locale;

import eu.kanade.tachiyomi.ui.reader.ReaderActivity;

public class GalleryPreviewsScene extends BaseToolbarScene {

    public static final String KEY_GALLERY_INFO = "gallery_info";
    private final static String KEY_HAS_FIRST_REFRESH = "has_first_refresh";

    /*---------------
     Whole life cycle
     ---------------*/
    @Nullable
    private EhClient mClient;
    @Nullable
    private GalleryInfo mGalleryInfo;

    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private GalleryPreviewAdapter mAdapter;
    @Nullable
    private GalleryPreviewHelper mHelper;

    private boolean mHasFirstRefresh = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        AssertUtils.assertNotNull(context);
        mClient = EhClient.INSTANCE;
        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    private void onInit() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }

        mGalleryInfo = args.getParcelable(KEY_GALLERY_INFO);
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mGalleryInfo = savedInstanceState.getParcelable(KEY_GALLERY_INFO);
        mHasFirstRefresh = savedInstanceState.getBoolean(KEY_HAS_FIRST_REFRESH);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        boolean hasFirstRefresh;
        if (mHelper != null && 1 == mHelper.getShownViewIndex()) {
            hasFirstRefresh = false;
        } else {
            hasFirstRefresh = mHasFirstRefresh;
        }
        outState.putBoolean(KEY_HAS_FIRST_REFRESH, hasFirstRefresh);
        outState.putParcelable(KEY_GALLERY_INFO, mGalleryInfo);
    }

    @NonNull
    @Override
    public View onCreateViewWithToolbar(LayoutInflater inflater,
                                        @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ContentLayout mContentLayout = (ContentLayout) inflater.inflate(
                R.layout.scene_gallery_previews, container, false);
        mContentLayout.hideFastScroll();
        mRecyclerView = mContentLayout.getRecyclerView();
        setLiftOnScrollTargetView(mRecyclerView);

        Context context = getContext();
        AssertUtils.assertNotNull(context);

        mAdapter = new GalleryPreviewAdapter();
        mRecyclerView.setAdapter(mAdapter);
        int columnWidth = Settings.getThumbSize();
        AutoGridLayoutManager layoutManager = new AutoGridLayoutManager(context, columnWidth, LayoutUtils.dp2pix(context, 16));
        layoutManager.setStrategy(AutoGridLayoutManager.STRATEGY_SUITABLE_SIZE);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setClipToPadding(false);
        int padding = LayoutUtils.dp2pix(context, 4);
        MarginItemDecoration decoration = new MarginItemDecoration(padding, padding, padding, padding, padding);
        mRecyclerView.addItemDecoration(decoration);

        mHelper = new GalleryPreviewHelper();
        mContentLayout.setHelper(mHelper);

        // Only refresh for the first time
        if (!mHasFirstRefresh) {
            mHasFirstRefresh = true;
            mHelper.firstRefresh();
        }

        return mContentLayout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mHelper) {
            if (1 == mHelper.getShownViewIndex()) {
                mHasFirstRefresh = false;
            }
        }
        if (null != mRecyclerView) {
            mRecyclerView.stopScroll();
            mRecyclerView = null;
        }

        mAdapter = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.gallery_previews);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
        if (mGalleryInfo != null) {
            if (((GalleryDetail) mGalleryInfo).previewPages > 2)
                showMenu(R.menu.scene_gallery_previews);
        }
    }

    @Override
    public boolean onMenuItemClick(@NonNull MenuItem item) {
        Context context = getContext();
        if (null == context) {
            return false;
        }

        int id = item.getItemId();
        if (id == R.id.action_go_to) {
            if (mHelper == null) {
                return true;
            }
            int pages = mHelper.getPages();
            if (pages > 1 && mHelper.canGoTo()) {
                GoToDialogHelper helper = new GoToDialogHelper(pages, mHelper.getPageForTop());
                AlertDialog dialog = new BaseDialogBuilder(context).setTitle(R.string.go_to)
                        .setView(R.layout.dialog_go_to)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                dialog.show();
                helper.setDialog(dialog);
            }
            return true;
        }
        return false;
    }

    public boolean onItemClick(int position) {
        Context context = getContext();
        if (null != context && null != mHelper && null != mGalleryInfo) {
            GalleryPreview p = mHelper.getDataAtEx(position);
            if (p != null) {
                Intent intent = new Intent(context, ReaderActivity.class);
                intent.setAction(ReaderActivity.ACTION_EH);
                intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, mGalleryInfo);
                intent.putExtra(ReaderActivity.KEY_PAGE, p.position);
                startActivity(intent);
            }
        }
        return true;
    }

    private void onGetPreviewSetSuccess(Pair<PreviewSet, Integer> result, int taskId) {
        if (null != mHelper && mHelper.isCurrentTask(taskId) && null != mGalleryInfo) {
            PreviewSet previewSet = result.first;
            int size = previewSet.size();
            ArrayList<GalleryPreview> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(previewSet.getGalleryPreview(mGalleryInfo.getGid(), i));
            }

            mHelper.onGetPageData(taskId, result.second, 0, null, null, list);
        }
    }

    private void onGetPreviewSetFailure(Exception e, int taskId) {
        if (mHelper != null && mHelper.isCurrentTask(taskId)) {
            mHelper.onGetException(taskId, e);
        }
    }

    private class GetPreviewSetListener extends EhCallback<GalleryPreviewsScene, Pair<PreviewSet, Integer>> {

        private final int mTaskId;

        public GetPreviewSetListener(Context context, int stageId, String sceneTag, int taskId) {
            super(context);
            mTaskId = taskId;
        }

        @Override
        public void onSuccess(Pair<PreviewSet, Integer> result) {
            GalleryPreviewsScene scene = GalleryPreviewsScene.this;
            scene.onGetPreviewSetSuccess(result, mTaskId);
        }

        @Override
        public void onFailure(Exception e) {
            GalleryPreviewsScene scene = GalleryPreviewsScene.this;
            scene.onGetPreviewSetFailure(e, mTaskId);
        }

        @Override
        public void onCancel() {

        }
    }

    private static class GalleryPreviewHolder extends RecyclerView.ViewHolder {

        public LoadImageView image;
        public TextView text;

        public GalleryPreviewHolder(View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.image);
            text = itemView.findViewById(R.id.text);
        }
    }

    private class GalleryPreviewAdapter extends RecyclerView.Adapter<GalleryPreviewHolder> {

        private final LayoutInflater mInflater;

        public GalleryPreviewAdapter() {
            mInflater = getLayoutInflater();
            AssertUtils.assertNotNull(mInflater);
        }

        @NonNull
        @Override
        public GalleryPreviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new GalleryPreviewHolder(mInflater.inflate(R.layout.item_gallery_preview, parent, false));
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull GalleryPreviewHolder holder, int position) {
            if (null != mHelper) {
                GalleryPreview preview = mHelper.getDataAtEx(position);
                if (preview != null) {
                    preview.load(holder.image);
                    holder.text.setText(Integer.toString(preview.position + 1));
                }
            }
            holder.itemView.setOnClickListener(v -> onItemClick(position));
        }

        @Override
        public int getItemCount() {
            return mHelper != null ? mHelper.size() : 0;
        }
    }

    private class GalleryPreviewHelper extends ContentLayout.ContentHelper<GalleryPreview> {

        @Override
        protected void getPageData(final int taskId, int type, int page, String index, boolean isNext) {
            MainActivity activity = getMainActivity();
            if (null == activity || null == mClient || null == mGalleryInfo) {
                onGetException(taskId, new EhException(getString(R.string.error_cannot_find_gallery)));
                return;
            }

            String url = EhUrl.getGalleryDetailUrl(mGalleryInfo.getGid(), mGalleryInfo.getToken(), page, false);
            EhRequest request = new EhRequest();
            request.setMethod(EhClient.METHOD_GET_PREVIEW_SET);
            request.setCallback(new GetPreviewSetListener(getContext(),
                    1, getTag(), taskId));
            request.setArgs(url);
            request.enqueue(GalleryPreviewsScene.this);
        }

        @Override
        protected Context getContext() {
            return GalleryPreviewsScene.this.getContext();
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void notifyDataSetChanged() {
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void notifyItemRangeInserted(int positionStart, int itemCount) {
            if (mAdapter != null) {
                mAdapter.notifyItemRangeInserted(positionStart, itemCount);
            }
        }

        @Override
        protected boolean isDuplicate(GalleryPreview d1, GalleryPreview d2) {
            return false;
        }
    }

    private class GoToDialogHelper implements View.OnClickListener,
            DialogInterface.OnDismissListener {

        private final int mPages;
        private final int mCurrentPage;

        @Nullable
        private Slider mSlider;
        @Nullable
        private Dialog mDialog;

        private GoToDialogHelper(int pages, int currentPage) {
            mPages = pages;
            mCurrentPage = currentPage;
        }

        public void setDialog(@NonNull AlertDialog dialog) {
            mDialog = dialog;

            ((TextView) ViewUtils.$$(dialog, R.id.start)).setText(String.format(Locale.US, "%d", 1));
            ((TextView) ViewUtils.$$(dialog, R.id.end)).setText(String.format(Locale.US, "%d", mPages));
            mSlider = (Slider) ViewUtils.$$(dialog, R.id.slider);
            mSlider.setValueTo(mPages);
            mSlider.setValue(mCurrentPage + 1);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this);
            dialog.setOnDismissListener(this);
        }

        @Override
        public void onClick(View v) {
            if (null == mSlider) {
                return;
            }

            int page = (int) (mSlider.getValue() - 1);
            if (page >= 0 && page < mPages && mHelper != null) {
                mHelper.goTo(page);
                if (mDialog != null) {
                    mDialog.dismiss();
                    mDialog = null;
                }
            } else {
                showTip(R.string.error_out_of_range, LENGTH_LONG);
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            mDialog = null;
            mSlider = null;
        }
    }
}
