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
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hippo.app.BaseDialogBuilder;
import com.hippo.drawable.AddDeleteDrawable;
import com.hippo.drawable.DrawerArrowDrawable;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.FastScroller;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.WindowInsetsAnimationHelper;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.FavListUrlBuilder;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.parser.FavoritesParser;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.ehviewer.ui.annotation.DrawerLifeCircle;
import com.hippo.ehviewer.ui.annotation.ViewLifeCircle;
import com.hippo.ehviewer.ui.annotation.WholeLifeCircle;
import com.hippo.ehviewer.widget.GalleryInfoContentHelper;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.widget.ContentLayout;
import com.hippo.widget.FabLayout;
import com.hippo.widget.SearchBarMover;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.ObjectUtils;
import com.hippo.yorozuya.SimpleHandler;
import com.hippo.yorozuya.ViewUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rikka.core.res.ResourcesKt;

// TODO Get favorite, modify favorite, add favorite, what a mess!
@SuppressLint("RtlHardcoded")
public class FavoritesScene extends BaseScene implements
        FastScroller.OnDragHandlerListener, SearchBarMover.Helper, SearchBar.Helper,
        FabLayout.OnClickFabListener, FabLayout.OnExpandListener,
        EasyRecyclerView.CustomChoiceListener {

    private static final long ANIMATE_TIME = 300L;

    private static final String KEY_URL_BUILDER = "url_builder";
    private static final String KEY_SEARCH_MODE = "search_mode";
    private static final String KEY_HAS_FIRST_REFRESH = "has_first_refresh";
    private static final String KEY_FAV_COUNT_ARRAY = "fav_count_array";
    // For modify action
    private final List<GalleryInfo> mModifyGiList = new ArrayList<>();
    public int current; // -1 for error
    public int limit; // -1 for error
    @Nullable
    private ContentLayout mContentLayout;
    @Nullable
    @ViewLifeCircle
    private EasyRecyclerView mRecyclerView;
    @Nullable
    @ViewLifeCircle
    private SearchBar mSearchBar;
    @Nullable
    @ViewLifeCircle
    private FabLayout mFabLayout;
    @Nullable
    @ViewLifeCircle
    private FavoritesAdapter mAdapter;
    @Nullable
    @ViewLifeCircle
    private FavoritesHelper mHelper;
    @Nullable
    @ViewLifeCircle
    private SearchBarMover mSearchBarMover;
    @Nullable
    @ViewLifeCircle
    private DrawerArrowDrawable mLeftDrawable;
    private AddDeleteDrawable mActionFabDrawable;
    @Nullable
    private DrawerLayout mDrawerLayout;
    @Nullable
    @DrawerLifeCircle
    private FavDrawerAdapter mDrawerAdapter;
    @Nullable
    @WholeLifeCircle
    private EhClient mClient;
    @Nullable
    @WholeLifeCircle
    private String[] mFavCatArray;
    @Nullable
    @WholeLifeCircle
    private int[] mFavCountArray;
    @Nullable
    @WholeLifeCircle
    private FavListUrlBuilder mUrlBuilder;
    private final Runnable showNormalFabsRunnable = new Runnable() {
        @Override
        public void run() {
            if (mFabLayout != null) {
                mFabLayout.setSecondaryFabVisibilityAt(0, true);
                mFabLayout.setSecondaryFabVisibilityAt(1, true);
                mFabLayout.setSecondaryFabVisibilityAt(2, true);
                mFabLayout.setSecondaryFabVisibilityAt(3, false);
                mFabLayout.setSecondaryFabVisibilityAt(4, false);
                mFabLayout.setSecondaryFabVisibilityAt(5, false);
                mFabLayout.setSecondaryFabVisibilityAt(6, false);
            }
            updateJumpFab();
        }
    };
    private int mFavLocalCount = 0;
    private int mFavCountSum = 0;
    private boolean mHasFirstRefresh;
    private boolean mSearchMode;
    // Avoid unnecessary search bar update
    private String mOldFavCat;
    // Avoid unnecessary search bar update
    private String mOldKeyword;
    // For modify action
    private boolean mEnableModify;
    // For modify action
    private int mModifyFavCat;
    // For modify action
    private boolean mModifyAdd;

    @Override
    public int getNavCheckedItem() {
        return R.id.nav_favourite;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        AssertUtils.assertNotNull(context);
        mClient = EhApplication.getEhClient();
        mFavCatArray = Settings.getFavCat();
        mFavCountArray = Settings.getFavCount();
        mFavLocalCount = Settings.getFavLocalCount();
        mFavCountSum = Settings.getFavCloudCount();

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    private void onInit() {
        mUrlBuilder = new FavListUrlBuilder();
        mUrlBuilder.setFavCat(Settings.getRecentFavCat());
        mSearchMode = false;
    }

    private void onRestore(Bundle savedInstanceState) {
        mUrlBuilder = savedInstanceState.getParcelable(KEY_URL_BUILDER);
        if (mUrlBuilder == null) {
            mUrlBuilder = new FavListUrlBuilder();
        }
        mSearchMode = savedInstanceState.getBoolean(KEY_SEARCH_MODE);
        mHasFirstRefresh = savedInstanceState.getBoolean(KEY_HAS_FIRST_REFRESH);
        mFavCountArray = savedInstanceState.getIntArray(KEY_FAV_COUNT_ARRAY);
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
        outState.putParcelable(KEY_URL_BUILDER, mUrlBuilder);
        outState.putBoolean(KEY_SEARCH_MODE, mSearchMode);
        outState.putIntArray(KEY_FAV_COUNT_ARRAY, mFavCountArray);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mClient = null;
        mFavCatArray = null;
        mFavCountArray = null;
        mFavCountSum = 0;
        mUrlBuilder = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_favorites, container, false);
        mContentLayout = view.findViewById(R.id.content_layout);
        MainActivity activity = getMainActivity();
        AssertUtils.assertNotNull(activity);
        mDrawerLayout = (DrawerLayout) ViewUtils.$$(activity, R.id.draw_view);
        mRecyclerView = mContentLayout.getRecyclerView();
        FastScroller fastScroller = mContentLayout.getFastScroller();
        mSearchBar = (SearchBar) ViewUtils.$$(view, R.id.search_bar);
        mFabLayout = (FabLayout) ViewUtils.$$(view, R.id.fab_layout);
        ViewCompat.setWindowInsetsAnimationCallback(view, new WindowInsetsAnimationHelper(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
                mFabLayout
        ));

        Context context = getContext();
        AssertUtils.assertNotNull(context);
        Resources resources = context.getResources();
        int paddingTopSB = resources.getDimensionPixelOffset(R.dimen.gallery_padding_top_search_bar);

        mHelper = new FavoritesHelper();
        mHelper.setEmptyString(resources.getString(R.string.gallery_list_empty_hit));
        mContentLayout.setHelper(mHelper);
        mContentLayout.getFastScroller().setOnDragHandlerListener(this);
        mContentLayout.setFitPaddingTop(paddingTopSB);

        mAdapter = new FavoritesAdapter(inflater, resources, mRecyclerView, Settings.getListMode());
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setClipChildren(false);
        mRecyclerView.setChoiceMode(EasyRecyclerView.CHOICE_MODE_MULTIPLE_CUSTOM);
        mRecyclerView.setCustomCheckedListener(this);

        fastScroller.setPadding(fastScroller.getPaddingLeft(), fastScroller.getPaddingTop() + paddingTopSB,
                fastScroller.getPaddingRight(), fastScroller.getPaddingBottom());

        mLeftDrawable = new DrawerArrowDrawable(context, ResourcesKt.resolveColor(getTheme(), android.R.attr.colorControlNormal));
        mSearchBar.setLeftDrawable(mLeftDrawable);
        mSearchBar.setRightDrawable(ContextCompat.getDrawable(context, R.drawable.v_magnify_x24));
        mSearchBar.setHelper(this);
        mSearchBar.setAllowEmptySearch(false);
        updateSearchBar();
        updateJumpFab();
        mSearchBarMover = new SearchBarMover(this, mSearchBar, mRecyclerView);

        int colorID = ResourcesKt.resolveColor(getTheme(), com.google.android.material.R.attr.colorOnSurface);
        mActionFabDrawable = new AddDeleteDrawable(context, colorID);
        mFabLayout.getPrimaryFab().setImageDrawable(mActionFabDrawable);
        mFabLayout.setExpanded(false, false);
        mFabLayout.setAutoCancel(true);
        mFabLayout.setHidePrimaryFab(false);
        mFabLayout.setOnClickFabListener(this);
        mFabLayout.setOnExpandListener(this);
        addAboveSnackView(mFabLayout);

        // Restore search mode
        if (mSearchMode) {
            mSearchMode = false;
            enterSearchMode(false);
        }

        // Only refresh for the first time
        if (!mHasFirstRefresh) {
            mHasFirstRefresh = true;
            mHelper.firstRefresh();
        }

        return view;
    }

    // keyword of mUrlBuilder, fav cat of mUrlBuilder, mFavCatArray.
    // They changed, call it
    private void updateSearchBar() {
        Context context = getContext();
        if (null == context || null == mUrlBuilder || null == mSearchBar || null == mFavCatArray) {
            return;
        }

        // Update title
        int favCat = mUrlBuilder.getFavCat();
        String favCatName;
        if (favCat >= 0 && favCat < 10) {
            favCatName = mFavCatArray[favCat];
        } else if (favCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
            favCatName = getString(R.string.local_favorites);
        } else {
            favCatName = getString(R.string.cloud_favorites);
        }
        String keyword = mUrlBuilder.getKeyword();
        if (TextUtils.isEmpty(keyword)) {
            if (!ObjectUtils.equal(favCatName, mOldFavCat)) {
                mSearchBar.setTitle(getString(R.string.favorites_title, favCatName));
            }
        } else {
            if (!ObjectUtils.equal(favCatName, mOldFavCat) || !ObjectUtils.equal(keyword, mOldKeyword)) {
                mSearchBar.setTitle(getString(R.string.favorites_title_2, favCatName, keyword));
            }
        }

        // Update hint
        if (!ObjectUtils.equal(favCatName, mOldFavCat)) {
            mSearchBar.setEditTextHint(getString(R.string.favorites_search_bar_hint, favCatName));
        }

        mOldFavCat = favCatName;
        mOldKeyword = keyword;

        // Save recent fav cat
        Settings.putRecentFavCat(mUrlBuilder.getFavCat());
    }

    // Hide jump fab on local fav cat
    private void updateJumpFab() {
        if (mFabLayout != null && mUrlBuilder != null) {
            mFabLayout.setSecondaryFabVisibilityAt(1, mUrlBuilder.getFavCat() != FavListUrlBuilder.FAV_CAT_LOCAL);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mHelper) {
            mHelper.destroy();
            if (1 == mHelper.getShownViewIndex()) {
                mHasFirstRefresh = false;
            }
        }
        if (null != mRecyclerView) {
            mRecyclerView.stopScroll();
            mRecyclerView = null;
        }
        if (null != mFabLayout) {
            removeAboveSnackView(mFabLayout);
            mFabLayout = null;
        }

        mAdapter = null;

        mSearchBar = null;

        mSearchBarMover = null;
        mLeftDrawable = null;

        mOldFavCat = null;
        mOldKeyword = null;
    }

    @Override
    public View onCreateDrawerView(LayoutInflater inflater,
                                   @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.drawer_list_rv, container, false);
        final Context context = getContext();
        Toolbar toolbar = (Toolbar) ViewUtils.$$(view, R.id.toolbar);

        AssertUtils.assertNotNull(context);

        toolbar.setTitle(R.string.collections);
        toolbar.inflateMenu(R.menu.drawer_favorites);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_default_favorites_slot) {
                String[] items = new String[12];
                items[0] = getString(R.string.let_me_select);
                items[1] = getString(R.string.local_favorites);
                String[] favCat = Settings.getFavCat();
                System.arraycopy(favCat, 0, items, 2, 10);
                new BaseDialogBuilder(context)
                        .setTitle(R.string.default_favorites_collection)
                        .setItems(items, (dialog, which) -> Settings.putDefaultFavSlot(which - 2)).show();
                return true;
            }
            return false;
        });

        EasyRecyclerView recyclerView = view.findViewById(R.id.recycler_view_drawer);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        mDrawerAdapter = new FavDrawerAdapter(inflater);
        recyclerView.setAdapter(mDrawerAdapter);

        return view;
    }

    @Override
    public void onDestroyDrawerView() {
        super.onDestroyDrawerView();

        mDrawerAdapter = null;
    }

    @Override
    public void onBackPressed() {
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            mRecyclerView.outOfCustomChoiceMode();
        } else if (mFabLayout != null && mFabLayout.isExpanded()) {
            mFabLayout.toggle();
        } else if (mSearchMode) {
            exitSearchMode(true);
        } else {
            finish();
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

        if (mSearchBarMover != null) {
            mSearchBarMover.returnSearchBarPosition();
        }
    }

    public boolean onItemClick(View view, int position) {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            // Skip if in search mode
            if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
                return true;
            }

            if (mUrlBuilder == null || mHelper == null) {
                return true;
            }

            // Local favorite position is 0, All favorite position is 1, so position - 2 is OK
            int newFavCat = position - 2;

            // Check is the same
            if (mUrlBuilder.getFavCat() == newFavCat) {
                return true;
            }

            // Ensure outOfCustomChoiceMode to avoid error
            //if (mRecyclerView != null) {
            //    mRecyclerView.isInCustomChoice();
            //}

            exitSearchMode(true);

            mUrlBuilder.setKeyword(null);
            mUrlBuilder.setFavCat(newFavCat);
            updateSearchBar();
            updateJumpFab();
            mHelper.refresh();

            closeDrawer(Gravity.RIGHT);

        } else {
            if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
                mRecyclerView.toggleItemChecked(position);
            } else if (mHelper != null) {
                GalleryInfo gi = mHelper.getDataAtEx(position);
                if (gi == null) {
                    return true;
                }
                Bundle args = new Bundle();
                args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO);
                args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, gi);
                Announcer announcer = new Announcer(GalleryDetailScene.class).setArgs(args);
                startScene(announcer);
            }
        }
        return true;
    }

    public boolean onItemLongClick(int position) {
        // Can not into
        if (mRecyclerView != null && !mSearchMode) {
            if (!mRecyclerView.isInCustomChoice()) {
                mRecyclerView.intoCustomChoiceMode();
            }
            mRecyclerView.toggleItemChecked(position);
        }
        return true;
    }

    @Override
    public boolean isValidView(RecyclerView recyclerView) {
        return recyclerView == mRecyclerView;
    }

    @Override
    public RecyclerView getValidRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public boolean forceShowSearchBar() {
        return false;
    }

    @Override
    public void onClickTitle() {
        // Skip if in search mode
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            return;
        }

        if (!mSearchMode) {
            enterSearchMode(true);
        }
    }

    @Override
    public void onClickLeftIcon() {
        // Skip if in search mode
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            return;
        }

        if (mSearchMode) {
            exitSearchMode(true);
        } else {
            toggleDrawer(Gravity.LEFT);
        }
    }

    @Override
    public void onClickRightIcon() {
        // Skip if in search mode
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            return;
        }

        if (!mSearchMode) {
            enterSearchMode(true);
        } else if (mSearchBar != null) {
            if (mSearchBar.getEditText().length() == 0) {
                exitSearchMode(true);
            } else {
                mSearchBar.applySearch();
            }
        }
    }

    @Override
    public void onSearchEditTextClick() {
    }

    @Override
    public void onApplySearch(String query) {
        // Skip if in search mode
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            return;
        }

        if (mUrlBuilder == null || mHelper == null) {
            return;
        }

        // Ensure outOfCustomChoiceMode to avoid error
        //if (mRecyclerView != null) {
        //    mRecyclerView.isInCustomChoice();
        //}

        exitSearchMode(true);

        mUrlBuilder.setKeyword(query);
        updateSearchBar();
        mHelper.refresh();
    }

    @Override
    public void onSearchEditTextBackPressed() {
        onBackPressed();
    }

    @Override
    public void onExpand(boolean expanded) {
        if (expanded) {
            mActionFabDrawable.setDelete(ANIMATE_TIME);
        } else {
            mActionFabDrawable.setAdd(ANIMATE_TIME);
        }
    }

    @Override
    public void onClickPrimaryFab(FabLayout view, FloatingActionButton fab) {
        if (mRecyclerView != null && mFabLayout != null) {
            if (mRecyclerView.isInCustomChoice()) {
                mRecyclerView.outOfCustomChoiceMode();
            } else {
                mFabLayout.toggle();
            }
        }
    }

    private void showGoToDialog() {
        Context context = getContext();
        if (null == context || null == mHelper) {
            return;
        }

        LocalDateTime local = LocalDateTime.of(2007, 3, 21, 0, 0);
        long fromDate = local.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toInstant().toEpochMilli();
        long toDate = MaterialDatePicker.todayInUtcMilliseconds();

        ArrayList listValidators = new ArrayList<>();
        listValidators.add(DateValidatorPointForward.from(fromDate));
        listValidators.add(DateValidatorPointBackward.before(toDate));

        var constraintsBuilder = new CalendarConstraints.Builder()
                .setStart(fromDate)
                .setEnd(toDate)
                .setValidator(CompositeDateValidator.allOf(listValidators));

        var datePicker = MaterialDatePicker.Builder.datePicker()
                .setCalendarConstraints(constraintsBuilder.build())
                .setTitleText(R.string.go_to)
                .setSelection(toDate)
                .build();
        datePicker.show(requireActivity().getSupportFragmentManager(), "date-picker");
        datePicker.addOnPositiveButtonClickListener(v -> {
            mHelper.goTo(v);
        });
    }

    @Override
    public void onClickSecondaryFab(FabLayout view, FloatingActionButton fab, int position) {
        Context context = getContext();
        if (null == context || null == mRecyclerView || null == mHelper) {
            return;
        }

        if (!mRecyclerView.isInCustomChoice()) {
            switch (position) {
                case 0: // Open right
                    openDrawer(Gravity.RIGHT);
                    break;
                case 1: // Go to
                    if (mHelper.canGoTo()) {
                        showGoToDialog();
                    }
                    break;
                case 2: // Refresh
                    mHelper.refresh();
                    break;
            }
            view.setExpanded(false);
            return;
        }

        mModifyGiList.clear();
        SparseBooleanArray stateArray = mRecyclerView.getCheckedItemPositions();
        for (int i = 0, n = stateArray.size(); i < n; i++) {
            if (stateArray.valueAt(i)) {
                GalleryInfo gi = mHelper.getDataAtEx(stateArray.keyAt(i));
                if (gi != null) {
                    mModifyGiList.add(gi);
                }
            }
        }

        switch (position) {
            case 3: { // Check all
                mRecyclerView.checkAll();
                break;
            }
            case 4: { // Download
                Activity activity = getMainActivity();
                if (activity != null) {
                    CommonOperations.startDownload(getMainActivity(), mModifyGiList, false);
                }
                mModifyGiList.clear();
                if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
                    mRecyclerView.outOfCustomChoiceMode();
                }
                break;
            }
            case 5: { // Delete
                DeleteDialogHelper helper = new DeleteDialogHelper();
                new BaseDialogBuilder(context)
                        .setTitle(R.string.delete_favorites_dialog_title)
                        .setMessage(getString(R.string.delete_favorites_dialog_message, mModifyGiList.size()))
                        .setPositiveButton(android.R.string.ok, helper)
                        .setOnCancelListener(helper)
                        .show();
                break;
            }
            case 6: { // Move
                MoveDialogHelper helper = new MoveDialogHelper();
                // First is local favorite, the other 10 is cloud favorite
                String[] array = new String[11];
                array[0] = getString(R.string.local_favorites);
                System.arraycopy(Settings.getFavCat(), 0, array, 1, 10);
                new BaseDialogBuilder(context)
                        .setTitle(R.string.move_favorites_dialog_title)
                        .setItems(array, helper)
                        .setOnCancelListener(helper)
                        .show();
                break;
            }
        }
    }

    private void showNormalFabs() {
        // Delay showing normal fabs to avoid mutation
        SimpleHandler.getInstance().removeCallbacks(showNormalFabsRunnable);
        SimpleHandler.getInstance().postDelayed(showNormalFabsRunnable, 300);
    }

    private void showSelectionFabs() {
        SimpleHandler.getInstance().removeCallbacks(showNormalFabsRunnable);

        if (mFabLayout != null) {
            mFabLayout.setSecondaryFabVisibilityAt(0, false);
            mFabLayout.setSecondaryFabVisibilityAt(1, false);
            mFabLayout.setSecondaryFabVisibilityAt(2, false);
            mFabLayout.setSecondaryFabVisibilityAt(3, true);
            mFabLayout.setSecondaryFabVisibilityAt(4, true);
            mFabLayout.setSecondaryFabVisibilityAt(5, true);
            mFabLayout.setSecondaryFabVisibilityAt(6, true);
        }
    }

    @Override
    public void onIntoCustomChoice(EasyRecyclerView view) {
        if (mFabLayout != null) {
            showSelectionFabs();
            mFabLayout.setAutoCancel(false);
            // Delay expanding action to make layout work fine
            SimpleHandler.getInstance().post(() -> mFabLayout.setExpanded(true));
        }
        if (mHelper != null) {
            mHelper.setRefreshLayoutEnable(false);
        }
        // Lock drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
    }

    @Override
    public void onOutOfCustomChoice(EasyRecyclerView view) {
        if (mFabLayout != null) {
            showNormalFabs();
            mFabLayout.setAutoCancel(true);
            mFabLayout.setExpanded(false);
        }
        if (mHelper != null) {
            mHelper.setRefreshLayoutEnable(true);
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

    private void enterSearchMode(boolean animation) {
        if (mSearchMode || mSearchBar == null || mSearchBarMover == null || mLeftDrawable == null) {
            return;
        }
        mSearchMode = true;
        mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
        mSearchBarMover.returnSearchBarPosition(animation);
        mLeftDrawable.setArrow(ANIMATE_TIME);
    }

    private void exitSearchMode(boolean animation) {
        if (!mSearchMode || mSearchBar == null || mSearchBarMover == null || mLeftDrawable == null) {
            return;
        }
        mSearchMode = false;
        mSearchBar.setState(SearchBar.STATE_NORMAL, animation);
        mSearchBarMover.returnSearchBarPosition();
        mLeftDrawable.setMenu(ANIMATE_TIME);
    }

    private void onGetFavoritesSuccess(FavoritesParser.Result result, int taskId) {
        if (mHelper != null && mSearchBarMover != null &&
                mHelper.isCurrentTask(taskId)) {

            if (mFavCatArray != null) {
                System.arraycopy(result.catArray, 0, mFavCatArray, 0, 10);
            }

            mFavCountArray = result.countArray;
            if (mFavCountArray != null) {
                mFavCountSum = 0;
                for (int i = 0; i < 10; i++) {
                    mFavCountSum = mFavCountSum + mFavCountArray[i];
                }
                Settings.putFavCloudCount(mFavCountSum);
            }

            updateSearchBar();
            assert mUrlBuilder != null;

            int pages = 0;
            if (result.nextPage == null)
                pages = mHelper.pgCounter + 1;
            else
                pages = Integer.MAX_VALUE;

            String prev = result.prevPage, next = result.nextPage;
            switch (mHelper.operation) {
                case -1:
                    if (prev != null) mHelper.upperPage = prev;
                    break;
                case 1:
                    if (next != null) mHelper.lowerPage = next;
                    break;
                default:
                    mHelper.upperPage = prev;
                    mHelper.lowerPage = next;
                    break;
            }

            mHelper.onGetPageData(taskId, pages, mHelper.pgCounter + 1, result.galleryInfoList);

            if (mDrawerAdapter != null) {
                mDrawerAdapter.notifyDataSetChanged();
            }
        }
    }

    private void onGetFavoritesFailure(Exception e, int taskId) {
        if (mHelper != null && mSearchBarMover != null &&
                mHelper.isCurrentTask(taskId)) {
            mHelper.onGetException(taskId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void onGetFavoritesLocal(String keyword, int taskId) {
        if (mHelper != null && mHelper.isCurrentTask(taskId)) {
            List<GalleryInfo> list;
            if (TextUtils.isEmpty(keyword)) {
                list = EhDB.getAllLocalFavorites();
            } else {
                list = EhDB.searchLocalFavorites(keyword);
            }

            if (list.size() == 0) {
                mHelper.onGetPageData(taskId, 0, 0, Collections.EMPTY_LIST);
            } else {
                mHelper.onGetPageData(taskId, 1, 0, list);
            }

            if (TextUtils.isEmpty(keyword)) {
                mFavLocalCount = list.size();
                Settings.putFavLocalCount(mFavLocalCount);
                if (mDrawerAdapter != null) {
                    mDrawerAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private static class AddFavoritesListener extends EhCallback<FavoritesScene, Void> {

        private final int mTaskId;
        private final String mKeyword;
        private final List<GalleryInfo> mBackup;

        private AddFavoritesListener(Context context, int stageId,
                                     String sceneTag, int taskId, String keyword, List<GalleryInfo> backup) {
            super(context, stageId, sceneTag);
            mTaskId = taskId;
            mKeyword = keyword;
            mBackup = backup;
        }

        @Override
        public void onSuccess(Void result) {
            FavoritesScene scene = getScene();
            if (scene != null) {
                scene.onGetFavoritesLocal(mKeyword, mTaskId);
            }
        }

        @Override
        public void onFailure(Exception e) {
            // TODO It's a failure, add all of backup back to db.
            // But how to known which one is failed?
            EhDB.putLocalFavorites(mBackup);

            FavoritesScene scene = getScene();
            if (scene != null) {
                scene.onGetFavoritesLocal(mKeyword, mTaskId);
            }
        }

        @Override
        public void onCancel() {
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof FavoritesScene;
        }
    }

    private static class GetFavoritesListener extends EhCallback<FavoritesScene, FavoritesParser.Result> {

        private final int mTaskId;
        // Local fav is shown now, but operation need be done for cloud fav
        private final boolean mLocal;
        private final String mKeyword;

        private GetFavoritesListener(Context context, int stageId,
                                     String sceneTag, int taskId, boolean local, String keyword) {
            super(context, stageId, sceneTag);
            mTaskId = taskId;
            mLocal = local;
            mKeyword = keyword;
        }

        @Override
        public void onSuccess(FavoritesParser.Result result) {
            // Put fav cat
            Settings.putFavCat(result.catArray);
            Settings.putFavCount(result.countArray);
            FavoritesScene scene = getScene();
            if (scene != null) {
                if (mLocal) {
                    scene.onGetFavoritesLocal(mKeyword, mTaskId);
                } else {
                    scene.onGetFavoritesSuccess(result, mTaskId);
                }
            }
        }

        @Override
        public void onFailure(Exception e) {
            FavoritesScene scene = getScene();
            if (scene != null) {
                if (mLocal) {
                    e.printStackTrace();
                    scene.onGetFavoritesLocal(mKeyword, mTaskId);
                } else {
                    scene.onGetFavoritesFailure(e, mTaskId);
                }
            }
        }

        @Override
        public void onCancel() {
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof FavoritesScene;
        }
    }

    private static class FavDrawerHolder extends RecyclerView.ViewHolder {

        private final TextView key;
        private final TextView value;

        private FavDrawerHolder(View itemView) {
            super(itemView);
            key = (TextView) ViewUtils.$$(itemView, R.id.key);
            value = (TextView) ViewUtils.$$(itemView, R.id.value);
        }
    }

    private class FavDrawerAdapter extends RecyclerView.Adapter<FavDrawerHolder> {

        private final LayoutInflater mInflater;

        private FavDrawerAdapter(LayoutInflater inflater) {
            mInflater = inflater;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @NonNull
        @Override
        public FavDrawerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FavDrawerHolder(mInflater.inflate(R.layout.item_drawer_favorites, parent, false));
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onBindViewHolder(@NonNull FavDrawerHolder holder, int position) {
            if (0 == position) {
                holder.key.setText(R.string.local_favorites);
                holder.value.setText(Integer.toString(mFavLocalCount));
                holder.itemView.setEnabled(true);
            } else if (1 == position) {
                holder.key.setText(R.string.cloud_favorites);
                holder.value.setText(Integer.toString(mFavCountSum));
                holder.itemView.setEnabled(true);
            } else {
                if (null == mFavCatArray || null == mFavCountArray ||
                        mFavCatArray.length < (position - 1) ||
                        mFavCountArray.length < (position - 1)) {
                    return;
                }
                holder.key.setText(mFavCatArray[position - 2]);
                holder.value.setText(Integer.toString(mFavCountArray[position - 2]));
                holder.itemView.setEnabled(true);
            }
            holder.itemView.setOnClickListener(v -> onItemClick(holder.itemView, position));
        }

        @Override
        public int getItemCount() {
            if (null == mFavCatArray) {
                return 2;
            }
            return 12;
        }
    }

    private class DeleteDialogHelper implements DialogInterface.OnClickListener,
            DialogInterface.OnCancelListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return;
            }
            if (mRecyclerView == null || mHelper == null || mUrlBuilder == null) {
                return;
            }

            mRecyclerView.outOfCustomChoiceMode();

            if (mUrlBuilder.getFavCat() == FavListUrlBuilder.FAV_CAT_LOCAL) { // Delete local fav
                long[] gidArray = new long[mModifyGiList.size()];
                for (int i = 0, n = mModifyGiList.size(); i < n; i++) {
                    gidArray[i] = mModifyGiList.get(i).gid;
                }
                EhDB.removeLocalFavorites(gidArray);
                mModifyGiList.clear();
                mHelper.refresh();
            } else { // Delete cloud fav
                mEnableModify = true;
                mModifyFavCat = -1;
                mModifyAdd = false;
                mHelper.refresh();
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mModifyGiList.clear();
        }
    }

    private class MoveDialogHelper implements DialogInterface.OnClickListener,
            DialogInterface.OnCancelListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mRecyclerView == null || mHelper == null || mUrlBuilder == null) {
                return;
            }
            int srcCat = mUrlBuilder.getFavCat();
            int dstCat;
            if (which == 0) {
                dstCat = FavListUrlBuilder.FAV_CAT_LOCAL;
            } else {
                dstCat = which - 1;
            }
            if (srcCat == dstCat) {
                return;
            }

            mRecyclerView.outOfCustomChoiceMode();

            if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Move from local to cloud
                long[] gidArray = new long[mModifyGiList.size()];
                for (int i = 0, n = mModifyGiList.size(); i < n; i++) {
                    gidArray[i] = mModifyGiList.get(i).gid;
                }
                EhDB.removeLocalFavorites(gidArray);
                mEnableModify = true;
                mModifyFavCat = dstCat;
                mModifyAdd = true;
                mHelper.refresh();
            } else if (dstCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Move from cloud to local
                EhDB.putLocalFavorites(mModifyGiList);
                mEnableModify = true;
                mModifyFavCat = -1;
                mModifyAdd = false;
                mHelper.refresh();
            } else {
                mEnableModify = true;
                mModifyFavCat = dstCat;
                mModifyAdd = false;
                mHelper.refresh();
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mModifyGiList.clear();
        }
    }

    private class FavoritesAdapter extends GalleryAdapter {

        public FavoritesAdapter(@NonNull LayoutInflater inflater, @NonNull Resources resources,
                                @NonNull RecyclerView recyclerView, int type) {
            super(inflater, resources, recyclerView, type, false);
        }

        @Override
        public int getItemCount() {
            return null != mHelper ? mHelper.size() : 0;
        }

        @Override
        void onItemClick(View view, int position) {
            FavoritesScene.this.onItemClick(view, position);
        }

        @Override
        boolean onItemLongClick(View view, int position) {
            return FavoritesScene.this.onItemLongClick(position);
        }

        @Nullable
        @Override
        public GalleryInfo getDataAt(int position) {
            return null != mHelper ? mHelper.getDataAtEx(position) : null;
        }
    }

    private class FavoritesHelper extends GalleryInfoContentHelper {
        public String upperPage = null;
        public String lowerPage = null;
        public int operation = 0;
        public int pgCounter = 0;

        @Override
        protected void getPageData(final int taskId, int type, int page) {
            MainActivity activity = getMainActivity();
            if (null == activity || null == mUrlBuilder || null == mClient) {
                return;
            }
            pgCounter = page;

            if (mEnableModify) {
                mEnableModify = false;

                boolean local = mUrlBuilder.getFavCat() == FavListUrlBuilder.FAV_CAT_LOCAL;

                if (mModifyAdd) {
                    long[] gidArray = new long[mModifyGiList.size()];
                    String[] tokenArray = new String[mModifyGiList.size()];
                    for (int i = 0, n = mModifyGiList.size(); i < n; i++) {
                        GalleryInfo gi = mModifyGiList.get(i);
                        gidArray[i] = gi.gid;
                        tokenArray[i] = gi.token;
                    }
                    List<GalleryInfo> modifyGiListBackup = new ArrayList<>(mModifyGiList);
                    mModifyGiList.clear();

                    EhRequest request = new EhRequest();
                    request.setMethod(EhClient.METHOD_ADD_FAVORITES_RANGE);
                    request.setCallback(new AddFavoritesListener(getContext(),
                            activity.getStageId(), getTag(),
                            taskId, mUrlBuilder.getKeyword(), modifyGiListBackup));
                    request.setArgs(gidArray, tokenArray, mModifyFavCat);
                    mClient.execute(request);
                } else {
                    long[] gidArray = new long[mModifyGiList.size()];
                    for (int i = 0, n = mModifyGiList.size(); i < n; i++) {
                        gidArray[i] = mModifyGiList.get(i).gid;
                    }
                    mModifyGiList.clear();

                    String url;
                    if (local) {
                        // Local fav is shown now, but operation need be done for cloud fav
                        url = EhUrl.getFavoritesUrl();
                    } else {
                        url = mUrlBuilder.build();
                    }

                    mUrlBuilder.setNext(lowerPage);
                    EhRequest request = new EhRequest();
                    request.setMethod(EhClient.METHOD_MODIFY_FAVORITES);
                    request.setCallback(new GetFavoritesListener(getContext(),
                            activity.getStageId(), getTag(),
                            taskId, local, mUrlBuilder.getKeyword()));
                    request.setArgs(url, gidArray, mModifyFavCat, Settings.getShowJpnTitle());
                    mClient.execute(request);
                }
            } else if (mUrlBuilder.getFavCat() == FavListUrlBuilder.FAV_CAT_LOCAL) {
                final String keyword = mUrlBuilder.getKeyword();
                SimpleHandler.getInstance().post(() -> onGetFavoritesLocal(keyword, taskId));
            } else {
                String prev = null, next = null;
                if (jumpTo != null) {
                    next = Integer.toString(minGid);
                    operation = 0;
                } else if (page != 0) {
                    if (page >= mHelper.getPageForTop()) {
                        next = lowerPage;
                        operation = 1;
                    } else {
                        prev = upperPage;
                        operation = -1;
                    }
                }
                mUrlBuilder.setPrev(prev);
                mUrlBuilder.setNext(next);

                mUrlBuilder.setJumpTo(jumpTo);
                jumpTo = null;

                String url = mUrlBuilder.build();
                EhRequest request = new EhRequest();
                request.setMethod(EhClient.METHOD_GET_FAVORITES);
                request.setCallback(new GetFavoritesListener(getContext(),
                        activity.getStageId(), getTag(),
                        taskId, false, mUrlBuilder.getKeyword()));
                request.setArgs(url, Settings.getShowJpnTitle());
                mClient.execute(request);
            }
        }

        @Override
        protected Context getContext() {
            return FavoritesScene.this.getContext();
        }

        @Override
        protected void notifyDataSetChanged() {
            // Ensure outOfCustomChoiceMode to avoid error
            if (mRecyclerView != null) {
                mRecyclerView.outOfCustomChoiceMode();
            }

            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void notifyItemRangeRemoved(int positionStart, int itemCount) {
            if (mAdapter != null) {
                mAdapter.notifyItemRangeRemoved(positionStart, itemCount);
            }
        }

        @Override
        protected void notifyItemRangeInserted(int positionStart, int itemCount) {
            if (mAdapter != null) {
                mAdapter.notifyItemRangeInserted(positionStart, itemCount);
            }
        }

        @Override
        public void onShowView(View hiddenView, View shownView) {
            if (null != mSearchBarMover) {
                mSearchBarMover.showSearchBar();
            }
        }

        @Override
        protected boolean isDuplicate(GalleryInfo d1, GalleryInfo d2) {
            return d1.gid == d2.gid;
        }

        @Override
        protected void onScrollToPosition(int postion) {
            if (0 == postion) {
                if (null != mSearchBarMover) {
                    mSearchBarMover.showSearchBar();
                }
            }
        }

        @Override
        protected void onClearData() {
            super.onClearData();
        }

        @Override
        protected void beforeRefresh() {
            super.beforeRefresh();
            upperPage = null;
            lowerPage = null;
            operation = 0;
        }

        @Override
        protected Parcelable saveInstanceState(Parcelable superState) {
            Bundle bundle = (Bundle) super.saveInstanceState(superState);
            bundle.putString(KEY_UPPER_PAGE, upperPage);
            bundle.putString(KEY_LOWER_PAGE, lowerPage);
            return bundle;
        }

        @Override
        protected Parcelable restoreInstanceState(Parcelable state) {
            Bundle bundle = (Bundle) state;
            upperPage = bundle.getString(KEY_UPPER_PAGE);
            lowerPage = bundle.getString(KEY_LOWER_PAGE);
            return super.restoreInstanceState(state);
        }
    }
}
