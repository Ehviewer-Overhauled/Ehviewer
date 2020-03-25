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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionDefault;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder;
import com.hippo.android.resource.AttrResources;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.FastScroller;
import com.hippo.easyrecyclerview.HandlerDrawable;
import com.hippo.easyrecyclerview.MarginItemDecoration;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.FavouriteStatusRouter;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.DownloadInfo;
import com.hippo.ehviewer.dao.HistoryInfo;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.ehviewer.ui.dialog.SelectItemWithIconAdapter;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.util.DrawableManager;
import com.hippo.view.ViewTransition;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.ViewUtils;

import java.util.List;

import de.greenrobot.dao.query.LazyList;

public class HistoryScene extends ToolbarScene {

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private ViewTransition mViewTransition;
    @Nullable
    private RecyclerView.Adapter<?> mAdapter;
    @Nullable
    private LazyList<HistoryInfo> mLazyList;

    private DownloadManager mDownloadManager;
    private DownloadManager.DownloadInfoListener mDownloadInfoListener;
    private FavouriteStatusRouter mFavouriteStatusRouter;
    private FavouriteStatusRouter.Listener mFavouriteStatusRouterListener;

    @Override
    public void onDestroy() {
        super.onDestroy();

        mDownloadManager.removeDownloadInfoListener(mDownloadInfoListener);
        mFavouriteStatusRouter.removeListener(mFavouriteStatusRouterListener);
    }

    @Override
    public int getNavCheckedItem() {
        return R.id.nav_history;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getContext2();
        assert context != null;
        mDownloadManager = EhApplication.getDownloadManager(context);
        mDownloadManager = EhApplication.getDownloadManager(context);
        mFavouriteStatusRouter = EhApplication.getFavouriteStatusRouter(context);

        mDownloadInfoListener = new DownloadManager.DownloadInfoListener() {
            @Override
            public void onAdd(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list, int position) {
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onUpdate(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list) {
            }

            @Override
            public void onUpdateAll() {
            }

            @Override
            public void onReload() {
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChange() {
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onRenameLabel(String from, String to) {
            }

            @Override
            public void onRemove(@NonNull DownloadInfo info, @NonNull List<DownloadInfo> list, int position) {
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onUpdateLabels() {
            }
        };
        mDownloadManager.addDownloadInfoListener(mDownloadInfoListener);

        mFavouriteStatusRouterListener = (gid, slot) -> {
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        };
        mFavouriteStatusRouter.addListener(mFavouriteStatusRouterListener);
    }

    @Nullable
    @Override
    public View onCreateView3(LayoutInflater inflater,
                              @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_history, container, false);
        View content = ViewUtils.$$(view, R.id.content);
        mRecyclerView = (EasyRecyclerView) ViewUtils.$$(content, R.id.recycler_view);
        FastScroller fastScroller = (FastScroller) ViewUtils.$$(content, R.id.fast_scroller);
        TextView tip = (TextView) ViewUtils.$$(view, R.id.tip);
        mViewTransition = new ViewTransition(content, tip);

        Context context = getContext2();
        AssertUtils.assertNotNull(context);
        Resources resources = context.getResources();

        Drawable drawable = DrawableManager.getVectorDrawable(context, R.drawable.big_history);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        tip.setCompoundDrawables(null, drawable, null, null);

        RecyclerViewTouchActionGuardManager guardManager = new RecyclerViewTouchActionGuardManager();
        guardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        guardManager.setEnabled(true);
        RecyclerViewSwipeManager swipeManager = new RecyclerViewSwipeManager();
        mAdapter = new HistoryAdapter();
        mAdapter.setHasStableIds(true);
        mAdapter = swipeManager.createWrappedAdapter(mAdapter);
        mRecyclerView.setAdapter(mAdapter);
        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();
        animator.setSupportsChangeAnimations(false);
        mRecyclerView.setItemAnimator(animator);
        AutoStaggeredGridLayoutManager layoutManager = new AutoStaggeredGridLayoutManager(
                0, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setColumnSize(resources.getDimensionPixelOffset(Settings.getDetailSizeResId()));
        layoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE);
        mRecyclerView.setLayoutManager(layoutManager);
        //mRecyclerView.setSelector(Ripple.generateRippleDrawable(context, !AttrResources.getAttrBoolean(context, R.attr.isLightTheme), new ColorDrawable(Color.TRANSPARENT)));
        //mRecyclerView.setDrawSelectorOnTop(true);
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setClipChildren(false);
        //mRecyclerView.setOnItemClickListener(this);
        //mRecyclerView.setOnItemLongClickListener(this);
        int interval = resources.getDimensionPixelOffset(R.dimen.gallery_list_interval);
        int paddingH = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h);
        int paddingV = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v);
        MarginItemDecoration decoration = new MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV);
        mRecyclerView.addItemDecoration(decoration);
        decoration.applyPaddings(mRecyclerView);
        guardManager.attachRecyclerView(mRecyclerView);
        swipeManager.attachRecyclerView(mRecyclerView);

        fastScroller.attachToRecyclerView(mRecyclerView);
        HandlerDrawable handlerDrawable = new HandlerDrawable();
        handlerDrawable.setColor(AttrResources.getAttrColor(context, R.attr.widgetColorThemeAccent));
        fastScroller.setHandlerDrawable(handlerDrawable);

        updateLazyList();
        updateView(false);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.history);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mLazyList) {
            mLazyList.close();
            mLazyList = null;
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
        if (null != mRecyclerView) {
            mRecyclerView.stopScroll();
            mRecyclerView = null;
        }

        mViewTransition = null;
        mAdapter = null;
    }

    // Remember to notify
    private void updateLazyList() {
        LazyList<HistoryInfo> lazyList = EhDB.getHistoryLazyList();
        if (mLazyList != null) {
            mLazyList.close();
        }
        mLazyList = lazyList;
    }

    private void updateView(boolean animation) {
        if (null == mAdapter || null == mViewTransition) {
            return;
        }

        if (mAdapter.getItemCount() == 0) {
            mViewTransition.showView(1, animation);
        } else {
            mViewTransition.showView(0, animation);
        }
    }

    @Override
    public void onNavigationClick() {
        onBackPressed();
    }

    @Override
    public int getMenuResId() {
        return R.menu.scene_history;
    }

    private void showClearAllDialog() {
        new MaterialAlertDialogBuilder(getContext2())
                .setMessage(R.string.clear_all_history)
                .setPositiveButton(R.string.clear_all, (dialog, which) -> {
                    if (DialogInterface.BUTTON_POSITIVE != which || null == mAdapter) {
                        return;
                    }

                    EhDB.clearHistoryInfo();
                    updateLazyList();
                    mAdapter.notifyDataSetChanged();
                    updateView(true);
                }).show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        // Skip when in choice mode
        Context context = getContext2();
        if (null == context) {
            return false;
        }

        int id = item.getItemId();
        switch (id) {
            case R.id.action_clear_all: {
                showClearAllDialog();
                return true;
            }
        }
        return false;
    }

    public boolean onItemClick(View view, int position) {
        if (null == mLazyList) {
            return false;
        }

        Bundle args = new Bundle();
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO);
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, mLazyList.get(position));
        Announcer announcer = new Announcer(GalleryDetailScene.class).setArgs(args);
        View thumb;
        if (null != (thumb = view.findViewById(R.id.thumb))) {
            announcer.setTranHelper(new EnterGalleryDetailTransaction(thumb));
        }
        startScene(announcer);
        return true;
    }

    public boolean onItemLongClick(int position) {
        final Context context = getContext2();
        final MainActivity activity = getActivity2();
        if (null == context || null == activity || null == mLazyList) {
            return false;
        }

        final GalleryInfo gi = mLazyList.get(position);

        if (gi == null) {
            return true;
        }

        boolean downloaded = mDownloadManager.getDownloadState(gi.gid) != DownloadInfo.STATE_INVALID;
        boolean favourited = gi.favoriteSlot != -2;

        CharSequence[] items = new CharSequence[]{
                context.getString(R.string.read),
                context.getString(downloaded ? R.string.delete_downloads : R.string.download),
                context.getString(favourited ? R.string.remove_from_favourites : R.string.add_to_favourites),
        };

        int[] icons = new int[]{
                R.drawable.v_book_open_x24,
                downloaded ? R.drawable.v_delete_x24 : R.drawable.v_download_x24,
                favourited ? R.drawable.v_heart_broken_x24 : R.drawable.v_heart_x24,
        };

        new MaterialAlertDialogBuilder(context)
                .setTitle(EhUtils.getSuitableTitle(gi))
                .setAdapter(new SelectItemWithIconAdapter(context, items, icons), (dialog, which) -> {
                    switch (which) {
                        case 0: // Read
                            Intent intent = new Intent(activity, GalleryActivity.class);
                            intent.setAction(GalleryActivity.ACTION_EH);
                            intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, gi);
                            startActivity(intent);
                            break;
                        case 1: // Download
                            if (downloaded) {
                                new MaterialAlertDialogBuilder(context)
                                        .setTitle(R.string.download_remove_dialog_title)
                                        .setMessage(getString(R.string.download_remove_dialog_message, gi.title))
                                        .setPositiveButton(android.R.string.ok, (dialog1, which1) -> mDownloadManager.deleteDownload(gi.gid))
                                        .show();
                            } else {
                                CommonOperations.startDownload(activity, gi, false);
                            }
                            break;
                        case 2: // Favorites
                            if (favourited) {
                                CommonOperations.removeFromFavorites(activity, gi, new RemoveFromFavoriteListener(context, activity.getStageId(), getTag()));
                            } else {
                                CommonOperations.addToFavorites(activity, gi, new AddToFavoriteListener(context, activity.getStageId(), getTag()));
                            }
                            break;
                    }
                }).show();
        return true;
    }

    private static class AddToFavoriteListener extends EhCallback<GalleryListScene, Void> {

        public AddToFavoriteListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(Void result) {
            showTip(R.string.add_to_favorite_success, LENGTH_SHORT);
        }

        @Override
        public void onFailure(Exception e) {
            showTip(R.string.add_to_favorite_failure, LENGTH_LONG);
        }

        @Override
        public void onCancel() {
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryListScene;
        }
    }

    private static class RemoveFromFavoriteListener extends EhCallback<GalleryListScene, Void> {

        public RemoveFromFavoriteListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(Void result) {
            showTip(R.string.remove_from_favorite_success, LENGTH_SHORT);
        }

        @Override
        public void onFailure(Exception e) {
            showTip(R.string.remove_from_favorite_failure, LENGTH_LONG);
        }

        @Override
        public void onCancel() {
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryListScene;
        }
    }

    private static class HistoryHolder extends AbstractSwipeableItemViewHolder {

        public final View card;
        public final LoadImageView thumb;
        public final TextView title;
        public final TextView uploader;
        public final SimpleRatingView rating;
        public final TextView category;
        public final TextView posted;
        public final TextView simpleLanguage;

        public HistoryHolder(View itemView) {
            super(itemView);

            card = itemView.findViewById(R.id.card);
            thumb = itemView.findViewById(R.id.thumb);
            title = itemView.findViewById(R.id.title);
            uploader = itemView.findViewById(R.id.uploader);
            rating = itemView.findViewById(R.id.rating);
            category = itemView.findViewById(R.id.category);
            posted = itemView.findViewById(R.id.posted);
            simpleLanguage = itemView.findViewById(R.id.simple_language);
        }

        @Override
        public View getSwipeableContainerView() {
            return card;
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryHolder>
            implements SwipeableItemAdapter<HistoryHolder> {

        private final LayoutInflater mInflater;
        private final int mListThumbWidth;
        private final int mListThumbHeight;

        public HistoryAdapter() {
            mInflater = getLayoutInflater2();

            View calculator = mInflater.inflate(R.layout.item_gallery_list_thumb_height, null);
            ViewUtils.measureView(calculator, 1024, ViewGroup.LayoutParams.WRAP_CONTENT);
            mListThumbHeight = calculator.getMeasuredHeight();
            mListThumbWidth = mListThumbHeight * 2 / 3;
        }

        @Override
        public long getItemId(int position) {
            if (null == mLazyList) {
                return super.getItemId(position);
            } else {
                return mLazyList.get(position).gid;
            }
        }

        @Override
        public HistoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            HistoryHolder holder = new HistoryHolder(mInflater.inflate(R.layout.item_history, parent, false));

            ViewGroup.LayoutParams lp = holder.thumb.getLayoutParams();
            lp.width = mListThumbWidth;
            lp.height = mListThumbHeight;
            holder.thumb.setLayoutParams(lp);

            return holder;
        }

        @Override
        public void onBindViewHolder(HistoryHolder holder, int position) {
            if (null == mLazyList) {
                return;
            }

            GalleryInfo gi = mLazyList.get(position);
            holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb);
            holder.title.setText(EhUtils.getSuitableTitle(gi));
            holder.uploader.setText(gi.uploader);
            holder.rating.setRating(gi.rating);
            TextView category = holder.category;
            String newCategoryText = EhUtils.getCategory(gi.category);
            if (!newCategoryText.equals(category.getText())) {
                category.setText(newCategoryText);
                category.setBackgroundColor(EhUtils.getCategoryColor(gi.category));
            }
            holder.posted.setText(gi.posted);
            holder.simpleLanguage.setText(gi.simpleLanguage);

            // Update transition name
            long gid = gi.gid;
            ViewCompat.setTransitionName(holder.thumb, TransitionNameFactory.getThumbTransitionName(gid));

            holder.card.setOnClickListener(v -> onItemClick(holder.itemView, position));
            holder.card.setOnLongClickListener(v -> onItemLongClick(position));
        }

        @Override
        public int getItemCount() {
            return null != mLazyList ? mLazyList.size() : 0;
        }

        @Override
        public int onGetSwipeReactionType(HistoryHolder holder, int position, int x, int y) {
            return SwipeableItemConstants.REACTION_CAN_SWIPE_LEFT;
        }

        @Override
        public void onSwipeItemStarted(HistoryHolder holder, int position) {
        }

        @Override
        public void onSetSwipeBackground(HistoryHolder holder, int position, int type) {
        }

        @Override
        public SwipeResultAction onSwipeItem(HistoryHolder holder, int position, int result) {
            switch (result) {
                case SwipeableItemConstants.RESULT_SWIPED_LEFT:
                    return new SwipeResultActionClear(position);
                case SwipeableItemConstants.RESULT_SWIPED_RIGHT:
                case SwipeableItemConstants.RESULT_CANCELED:
                default:
                    return new SwipeResultActionDefault();
            }
        }
    }

    private class SwipeResultActionClear extends SwipeResultActionRemoveItem {

        private final int mPosition;

        protected SwipeResultActionClear(int position) {
            mPosition = position;
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();
            if (null == mLazyList || null == mAdapter) {
                return;
            }

            HistoryInfo info = mLazyList.get(mPosition);
            EhDB.deleteHistoryInfo(info);
            updateLazyList();
            mAdapter.notifyDataSetChanged();
            updateView(true);
        }
    }
}
