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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.hippo.drawable.TriangleDrawable;
import com.hippo.easyrecyclerview.MarginItemDecoration;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.widget.TileThumb;
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager;
import com.hippo.yorozuya.ViewUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

abstract class GalleryAdapter extends RecyclerView.Adapter<GalleryHolder> {

    public static final int TYPE_INVALID = -1;
    public static final int TYPE_LIST = 0;
    public static final int TYPE_GRID = 1;
    private final LayoutInflater mInflater;
    private final Resources mResources;
    private final RecyclerView mRecyclerView;
    private final AutoStaggeredGridLayoutManager mLayoutManager;
    private final int mPaddingTopSB;
    private final int mListThumbWidth;
    private final int mListThumbHeight;
    private final boolean mShowFavourited;
    private final DownloadManager mDownloadManager;
    private MarginItemDecoration mListDecoration;
    private MarginItemDecoration mGirdDecoration;
    private int mType = TYPE_INVALID;

    public GalleryAdapter(@NonNull LayoutInflater inflater, @NonNull Resources resources,
                          @NonNull RecyclerView recyclerView, int type, boolean showFavourited) {
        mInflater = inflater;
        mResources = resources;
        mRecyclerView = recyclerView;
        mLayoutManager = new AutoStaggeredGridLayoutManager(0, StaggeredGridLayoutManager.VERTICAL);
        mPaddingTopSB = resources.getDimensionPixelOffset(R.dimen.gallery_padding_top_search_bar);
        mShowFavourited = showFavourited;

        mRecyclerView.setAdapter(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        @SuppressLint("InflateParams") View calculator = inflater.inflate(R.layout.item_gallery_list_thumb_height, null);
        ViewUtils.measureView(calculator, 1024, ViewGroup.LayoutParams.WRAP_CONTENT);
        mListThumbHeight = calculator.getMeasuredHeight();
        mListThumbWidth = mListThumbHeight * 2 / 3;

        setType(type);

        mDownloadManager = EhApplication.getDownloadManager();
    }

    private void adjustPaddings() {
        RecyclerView recyclerView = mRecyclerView;
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop() + mPaddingTopSB,
                recyclerView.getPaddingRight(), recyclerView.getPaddingBottom());
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        if (type == mType) {
            return;
        }
        mType = type;

        RecyclerView recyclerView = mRecyclerView;
        switch (type) {
            case GalleryAdapter.TYPE_LIST -> {
                int columnWidth = mResources.getDimensionPixelOffset(Settings.getDetailSizeResId());
                mLayoutManager.setColumnSize(columnWidth);
                mLayoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE);
                if (null != mGirdDecoration) {
                    recyclerView.removeItemDecoration(mGirdDecoration);
                }
                if (null == mListDecoration) {
                    int interval = mResources.getDimensionPixelOffset(R.dimen.gallery_list_interval);
                    int paddingH = mResources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h);
                    int paddingV = mResources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v);
                    mListDecoration = new MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV);
                }
                recyclerView.addItemDecoration(mListDecoration);
                adjustPaddings();
                notifyDataSetChanged();
            }
            case GalleryAdapter.TYPE_GRID -> {
                int columnWidth = Settings.getThumbSize();
                mLayoutManager.setColumnSize(columnWidth);
                mLayoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_SUITABLE_SIZE);
                if (null != mListDecoration) {
                    recyclerView.removeItemDecoration(mListDecoration);
                }
                if (null == mGirdDecoration) {
                    int interval = mResources.getDimensionPixelOffset(R.dimen.gallery_grid_interval);
                    int paddingH = mResources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_h);
                    int paddingV = mResources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_v);
                    mGirdDecoration = new MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV);
                }
                recyclerView.addItemDecoration(mGirdDecoration);
                adjustPaddings();
                notifyDataSetChanged();
            }
        }
    }

    @NonNull
    @Override
    public GalleryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = switch (viewType) {
            case TYPE_LIST -> R.layout.item_gallery_list;
            case TYPE_GRID -> R.layout.item_gallery_grid;
            default -> throw new IllegalStateException("Unexpected value: " + viewType);
        };

        GalleryHolder holder = new GalleryHolder(mInflater.inflate(layoutId, parent, false));

        if (viewType == TYPE_LIST) {
            ViewGroup.LayoutParams lp = holder.thumb.getLayoutParams();
            lp.width = mListThumbWidth;
            lp.height = mListThumbHeight;
            holder.thumb.setLayoutParams(lp);
        }

        holder.card.setOnClickListener(v -> onItemClick(holder.itemView, holder.getBindingAdapterPosition()));
        holder.card.setOnLongClickListener(v -> onItemLongClick(holder.itemView, holder.getBindingAdapterPosition()));

        return holder;
    }

    abstract void onItemClick(View view, int position);

    abstract boolean onItemLongClick(View view, int position);

    @Override
    public int getItemViewType(int position) {
        return mType;
    }

    @Nullable
    public abstract GalleryInfo getDataAt(int position);

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull GalleryHolder holder, int position) {
        GalleryInfo gi = getDataAt(position);
        if (null == gi) {
            return;
        }

        switch (mType) {
            case TYPE_LIST -> {
                holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb);
                holder.title.setText(EhUtils.getSuitableTitle(gi));
                holder.uploader.setText(gi.uploader);
                holder.uploader.setAlpha(gi.disowned ? .5f : 1f);
                holder.rating.setRating(gi.rating);
                TextView category = holder.category;
                String newCategoryText = EhUtils.getCategory(gi.category);
                if (!newCategoryText.equals(category.getText().toString())) {
                    category.setText(newCategoryText);
                    category.setBackgroundColor(EhUtils.getCategoryColor(gi.category));
                }
                holder.posted.setText(gi.posted);
                if (gi.pages == 0 || !Settings.getShowGalleryPages()) {
                    holder.pages.setText(null);
                    holder.pages.setVisibility(View.GONE);
                } else {
                    holder.pages.setText(gi.pages + "P");
                    holder.pages.setVisibility(View.VISIBLE);
                }
                if (TextUtils.isEmpty(gi.simpleLanguage)) {
                    holder.simpleLanguage.setText(null);
                    holder.simpleLanguage.setVisibility(View.GONE);
                } else {
                    holder.simpleLanguage.setText(gi.simpleLanguage);
                    holder.simpleLanguage.setVisibility(View.VISIBLE);
                }
                holder.favourited.setVisibility((mShowFavourited && gi.favoriteSlot >= -1 && gi.favoriteSlot <= 10) ? View.VISIBLE : View.GONE);
                holder.downloaded.setVisibility(mDownloadManager.containDownloadInfo(gi.gid) ? View.VISIBLE : View.GONE);
            }
            case TYPE_GRID -> {
                ((TileThumb) holder.thumb).setThumbSize(gi.thumbWidth, gi.thumbHeight);
                holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb);
                View category = holder.category;
                Drawable drawable = category.getBackground();
                int color = EhUtils.getCategoryColor(gi.category);
                if (!(drawable instanceof TriangleDrawable)) {
                    drawable = new TriangleDrawable(color);
                    category.setBackground(drawable);
                } else {
                    ((TriangleDrawable) drawable).setColor(color);
                }
                holder.simpleLanguage.setText(gi.simpleLanguage);
            }
        }

        // Update transition name
        ViewCompat.setTransitionName(holder.thumb, TransitionNameFactory.getThumbTransitionName(gi.gid));
    }

    @IntDef({TYPE_LIST, TYPE_GRID})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }
}
