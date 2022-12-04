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

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hippo.app.BaseDialogBuilder;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.UrlOpener;
import com.hippo.ehviewer.WindowInsetsAnimationHelper;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.GalleryComment;
import com.hippo.ehviewer.client.data.GalleryCommentList;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.parser.VoteCommentParser;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.scene.SceneFragment;
import com.hippo.text.URLImageGetter;
import com.hippo.util.ClipboardUtilKt;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.ReadableTime;
import com.hippo.util.TextUrl;
import com.hippo.view.ViewTransition;
import com.hippo.widget.FabLayout;
import com.hippo.widget.LinkifyTextView;
import com.hippo.widget.ObservedTextView;
import com.hippo.yorozuya.AnimationUtils;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.SimpleAnimatorListener;
import com.hippo.yorozuya.StringUtils;
import com.hippo.yorozuya.ViewUtils;
import com.hippo.yorozuya.collect.IntList;

import java.util.ArrayList;
import java.util.List;

import rikka.core.res.ResourcesKt;

public final class GalleryCommentsScene extends ToolbarScene
        implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG = GalleryCommentsScene.class.getSimpleName();

    public static final String KEY_API_UID = "api_uid";
    public static final String KEY_API_KEY = "api_key";
    public static final String KEY_GID = "gid";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_COMMENT_LIST = "comment_list";
    public static final String KEY_GALLERY_DETAIL = "gallery_detail";
    private static final int TYPE_COMMENT = 0;
    private static final int TYPE_MORE = 1;
    private static final int TYPE_PROGRESS = 2;
    private GalleryDetail mGalleryDetail;
    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private FabLayout mFabLayout;
    @Nullable
    private FloatingActionButton mFab;
    @Nullable
    private View mEditPanel;
    @Nullable
    private ImageView mSendImage;
    @Nullable
    private EditText mEditText;
    @Nullable
    private CommentAdapter mAdapter;
    @Nullable
    private ViewTransition mViewTransition;
    private SwipeRefreshLayout mRefreshLayout;
    private Drawable mSendDrawable;
    private Drawable mPencilDrawable;
    private long mCommentId;
    private boolean mInAnimation = false;
    private boolean mShowAllComments = false;
    private boolean mRefreshingComments = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    private void handleArgs(Bundle args) {
        if (args == null) {
            return;
        }

        mGalleryDetail = args.getParcelable(KEY_GALLERY_DETAIL);
        mShowAllComments = mGalleryDetail != null && mGalleryDetail.comments != null && !mGalleryDetail.comments.hasMore;
    }

    private void onInit() {
        handleArgs(getArguments());
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mGalleryDetail = savedInstanceState.getParcelable(KEY_GALLERY_DETAIL);
        mShowAllComments = mGalleryDetail != null && mGalleryDetail.comments != null && !mGalleryDetail.comments.hasMore;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_GALLERY_DETAIL, mGalleryDetail);
    }

    @NonNull
    @Override
    public View onCreateViewWithToolbar(LayoutInflater inflater,
                                        @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.scene_gallery_comments, container, false);
        mRecyclerView = (EasyRecyclerView) ViewUtils.$$(view, R.id.recycler_view);
        setLiftOnScrollTargetView(mRecyclerView);
        TextView tip = (TextView) ViewUtils.$$(view, R.id.tip);
        mEditPanel = ViewUtils.$$(view, R.id.edit_panel);
        mSendImage = (ImageView) ViewUtils.$$(mEditPanel, R.id.send);
        mEditText = (EditText) ViewUtils.$$(mEditPanel, R.id.edit_text);
        mFabLayout = (FabLayout) ViewUtils.$$(view, R.id.fab_layout);
        mFab = (FloatingActionButton) ViewUtils.$$(view, R.id.fab);
        mRefreshLayout = (SwipeRefreshLayout) ViewUtils.$$(view, R.id.refresh_layout);


        // Workaround for fab and edittext render out of screen
        view.removeView(mFabLayout);
        view.removeView((View) mEditPanel.getParent());
        assert container != null;
        container.addView(mFabLayout);
        container.addView((View) mEditPanel.getParent());

        ViewCompat.setWindowInsetsAnimationCallback(view, new WindowInsetsAnimationHelper(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
                mEditPanel,
                mFabLayout
        ));
        mRefreshLayout.setColorSchemeColors(ResourcesKt.resolveColor(getTheme(), androidx.appcompat.R.attr.colorPrimary));
        mRefreshLayout.setOnRefreshListener(this);

        Context context = requireContext();

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.big_sad_pandroid);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        tip.setCompoundDrawables(null, drawable, null, null);

        mSendDrawable = ContextCompat.getDrawable(context, R.drawable.v_send_dark_x24);
        mPencilDrawable = ContextCompat.getDrawable(context, R.drawable.v_pencil_dark_x24);

        mAdapter = new CommentAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context,
                RecyclerView.VERTICAL, false));
        mRecyclerView.setHasFixedSize(true);
        // Cancel change animator
        RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        if (itemAnimator instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }

        mSendImage.setOnClickListener(this);
        mFab.setOnClickListener(this);

        addAboveSnackView(mEditPanel);
        addAboveSnackView(mFabLayout);

        mViewTransition = new ViewTransition(mRecyclerView, tip);

        updateView(false);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mRecyclerView) {
            mRecyclerView.stopScroll();
            mRecyclerView = null;
        }
        if (null != mEditPanel) {
            removeAboveSnackView(mEditPanel);
            mEditPanel = null;
        }
        if (null != mFabLayout) {
            removeAboveSnackView(mFabLayout);
            mFabLayout = null;
        }

        mFab = null;
        mSendImage = null;
        mEditText = null;
        mAdapter = null;
        mViewTransition = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(R.string.gallery_comments);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
    }

    @Override
    public void onNavigationClick() {
        onBackPressed();
    }

    private void voteComment(long id, int vote) {
        Context context = getContext();
        MainActivity activity = getMainActivity();
        if (null == context || null == activity) {
            return;
        }

        EhRequest request = new EhRequest()
                .setMethod(EhClient.METHOD_VOTE_COMMENT)
                .setArgs(mGalleryDetail.apiUid, mGalleryDetail.apiKey, mGalleryDetail.gid, mGalleryDetail.token, id, vote)
                .setCallback(new VoteCommentListener(context,
                        activity.getStageId(), getTag()));
        EhApplication.getEhClient().execute(request);
    }

    @SuppressLint("InflateParams")
    public void showVoteStatusDialog(Context context, String voteStatus) {
        String[] temp = StringUtils.split(voteStatus, ',');
        final int length = temp.length;
        final String[] userArray = new String[length];
        final String[] voteArray = new String[length];
        for (int i = 0; i < length; i++) {
            String str = StringUtils.trim(temp[i]);
            int index = str.lastIndexOf(' ');
            if (index < 0) {
                Log.d(TAG, "Something wrong happened about vote state");
                userArray[i] = str;
                voteArray[i] = "";
            } else {
                userArray[i] = StringUtils.trim(str.substring(0, index));
                voteArray[i] = StringUtils.trim(str.substring(index + 1));
            }
        }

        BaseDialogBuilder builder = new BaseDialogBuilder(context);
        context = builder.getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        EasyRecyclerView rv = (EasyRecyclerView) inflater.inflate(R.layout.dialog_recycler_view, null);
        rv.setAdapter(new RecyclerView.Adapter<InfoHolder>() {
            @NonNull
            @Override
            public InfoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new InfoHolder(inflater.inflate(R.layout.item_drawer_favorites, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull InfoHolder holder, int position) {
                holder.key.setText(userArray[position]);
                holder.value.setText(voteArray[position]);
            }

            @Override
            public int getItemCount() {
                return length;
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setClipToPadding(false);
        builder.setView(rv).show();
    }

    private void showCommentDialog(int position) {
        final Context context = getContext();
        if (context == null || mGalleryDetail == null || mGalleryDetail.comments == null || mGalleryDetail.comments.comments == null || position >= mGalleryDetail.comments.comments.length || position < 0) {
            return;
        }

        final GalleryComment comment = mGalleryDetail.comments.comments[position];
        List<String> menu = new ArrayList<>();
        final IntList menuId = new IntList();
        Resources resources = context.getResources();

        menu.add(resources.getString(R.string.copy_comment_text));
        menuId.add(R.id.copy);
        if (comment.editable) {
            menu.add(resources.getString(R.string.edit_comment));
            menuId.add(R.id.edit_comment);
        }
        if (comment.voteUpAble) {
            menu.add(resources.getString(comment.voteUpEd ? R.string.cancel_vote_up : R.string.vote_up));
            menuId.add(R.id.vote_up);
        }
        if (comment.voteDownAble) {
            menu.add(resources.getString(comment.voteDownEd ? R.string.cancel_vote_down : R.string.vote_down));
            menuId.add(R.id.vote_down);
        }
        if (!TextUtils.isEmpty(comment.voteState)) {
            menu.add(resources.getString(R.string.check_vote_status));
            menuId.add(R.id.check_vote_status);
        }

        new BaseDialogBuilder(context)
                .setItems(menu.toArray(new String[0]), (dialog, which) -> {
                    if (which < 0 || which >= menuId.size()) {
                        return;
                    }
                    int id = menuId.get(which);
                    if (id == R.id.copy) {
                        ClipboardUtilKt.addTextToClipboard(requireActivity(), comment.comment, false);
                    } else if (id == R.id.vote_up) {
                        voteComment(comment.id, 1);
                    } else if (id == R.id.vote_down) {
                        voteComment(comment.id, -1);
                    } else if (id == R.id.check_vote_status) {
                        showVoteStatusDialog(context, comment.voteState);
                    } else if (id == R.id.edit_comment) {
                        prepareEditComment(comment.id);
                        if (!mInAnimation && mEditPanel != null && mEditPanel.getVisibility() != View.VISIBLE) {
                            showEditPanel(true);
                        }
                    }
                }).show();
    }

    public boolean onItemClick(EasyRecyclerView parent, View view, int position) {
        MainActivity activity = getMainActivity();
        if (null == activity) {
            return false;
        }

        RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
        if (holder instanceof ActualCommentHolder commentHolder) {
            ClickableSpan span = commentHolder.comment.getCurrentSpan();
            commentHolder.comment.clearCurrentSpan();

            if (span instanceof URLSpan) {
                UrlOpener.openUrl(activity, ((URLSpan) span).getURL(), true, mGalleryDetail);
            } else {
                showCommentDialog(position);
            }
        } else if (holder instanceof MoreCommentHolder && !mRefreshingComments && mAdapter != null) {
            mRefreshingComments = true;
            mShowAllComments = true;
            mAdapter.notifyItemChanged(position);

            String url = getGalleryDetailUrl();
            if (url != null) {
                // Request
                EhRequest request = new EhRequest()
                        .setMethod(EhClient.METHOD_GET_GALLERY_DETAIL)
                        .setArgs(url)
                        .setCallback(new RefreshCommentListener(activity, activity.getStageId(), getTag()));
                EhApplication.getEhClient().execute(request);
            }
        }

        return true;
    }

    private void updateView(boolean animation) {
        if (null == mViewTransition) {
            return;
        }

        if (mGalleryDetail == null || mGalleryDetail.comments == null || mGalleryDetail.comments.comments == null || mGalleryDetail.comments.comments.length <= 0) {
            mViewTransition.showView(1, animation);
        } else {
            mViewTransition.showView(0, animation);
        }
    }

    private void prepareNewComment() {
        mCommentId = 0;
        if (mSendImage != null) {
            mSendImage.setImageDrawable(mSendDrawable);
        }
    }

    private void prepareEditComment(long commentId) {
        mCommentId = commentId;
        if (mSendImage != null) {
            mSendImage.setImageDrawable(mPencilDrawable);
        }
    }

    private void showEditPanelWithAnimation() {
        if (null == mFab || null == mEditPanel) {
            return;
        }

        mInAnimation = true;
        mFab.setTranslationX(0.0f);
        mFab.setTranslationY(0.0f);
        mFab.setScaleX(1.0f);
        mFab.setScaleY(1.0f);
        int fabEndX = mEditPanel.getLeft() + (mEditPanel.getWidth() / 2) - (mFab.getWidth() / 2);
        int fabEndY = mEditPanel.getTop() + (mEditPanel.getHeight() / 2) - (mFab.getHeight() / 2);
        mFab.animate().x(fabEndX).y(fabEndY).scaleX(0.0f).scaleY(0.0f)
                .setInterpolator(AnimationUtils.SLOW_FAST_SLOW_INTERPOLATOR)
                .setDuration(300L).setListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (null == mFab || null == mEditPanel) {
                            return;
                        }

                        ((View) mFab).setVisibility(View.INVISIBLE);
                        mEditPanel.setVisibility(View.VISIBLE);
                        int halfW = mEditPanel.getWidth() / 2;
                        int halfH = mEditPanel.getHeight() / 2;
                        Animator animator = ViewAnimationUtils.createCircularReveal(mEditPanel, halfW, halfH, 0,
                                (float) Math.hypot(halfW, halfH)).setDuration(300L);
                        animator.addListener(new SimpleAnimatorListener() {
                            @Override
                            public void onAnimationEnd(Animator a) {
                                mInAnimation = false;
                            }
                        });
                        animator.start();
                    }
                }).start();
    }

    private void showEditPanel(boolean animation) {
        if (animation) {
            showEditPanelWithAnimation();
        } else {
            if (null == mFab || null == mEditPanel) {
                return;
            }

            ((View) mFab).setVisibility(View.INVISIBLE);
            mEditPanel.setVisibility(View.VISIBLE);
        }
    }

    private void hideEditPanelWithAnimation() {
        if (null == mFab || null == mEditPanel) {
            return;
        }

        mInAnimation = true;
        int halfW = mEditPanel.getWidth() / 2;
        int halfH = mEditPanel.getHeight() / 2;
        Animator animator = ViewAnimationUtils.createCircularReveal(mEditPanel, halfW, halfH,
                (float) Math.hypot(halfW, halfH), 0.0f).setDuration(300L);
        animator.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator a) {
                if (null == mFab || null == mEditPanel) {
                    return;
                }

                if (Looper.myLooper() != Looper.getMainLooper()) {
                    // Some devices may run this block in non-UI thread.
                    // It might be a bug of Android OS.
                    // Check it here to avoid crash.
                    return;
                }

                mEditPanel.setVisibility(View.GONE);
                ((View) mFab).setVisibility(View.VISIBLE);
                int fabStartX = mEditPanel.getLeft() + (mEditPanel.getWidth() / 2) - (mFab.getWidth() / 2);
                int fabStartY = mEditPanel.getTop() + (mEditPanel.getHeight() / 2) - (mFab.getHeight() / 2);
                mFab.setX(fabStartX);
                mFab.setY(fabStartY);
                mFab.setScaleX(0.0f);
                mFab.setScaleY(0.0f);
                mFab.setRotation(-45.0f);
                mFab.animate().translationX(0.0f).translationY(0.0f).scaleX(1.0f).scaleY(1.0f).rotation(0.0f)
                        .setInterpolator(AnimationUtils.SLOW_FAST_SLOW_INTERPOLATOR)
                        .setDuration(300L).setListener(new SimpleAnimatorListener() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mInAnimation = false;
                            }
                        }).start();
            }
        });
        animator.start();
    }

    private void hideEditPanel(boolean animation) {
        hideSoftInput();
        if (animation) {
            hideEditPanelWithAnimation();
        } else {
            if (null == mFab || null == mEditPanel) {
                return;
            }

            ((View) mFab).setVisibility(View.VISIBLE);
            mEditPanel.setVisibility(View.INVISIBLE);
        }
    }

    @Nullable
    private String getGalleryDetailUrl() {
        if (mGalleryDetail != null && mGalleryDetail.gid != -1 && mGalleryDetail.token != null) {
            return EhUrl.getGalleryDetailUrl(mGalleryDetail.gid, mGalleryDetail.token, 0, mShowAllComments);
        } else {
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        Context context = getContext();
        MainActivity activity = getMainActivity();
        if (null == context || null == activity || null == mEditText) {
            return;
        }

        if (mFab == v) {
            if (!mInAnimation) {
                prepareNewComment();
                showEditPanel(true);
            }
        } else if (mSendImage == v) {
            if (!mInAnimation) {
                String comment = mEditText.getText().toString();
                if (TextUtils.isEmpty(comment)) {
                    // Comment is empty
                    return;
                }
                String url = getGalleryDetailUrl();
                if (url == null) {
                    return;
                }
                // Request
                EhRequest request = new EhRequest()
                        .setMethod(EhClient.METHOD_GET_COMMENT_GALLERY)
                        .setArgs(url, comment, mCommentId != 0 ? Long.toString(mCommentId) : null)
                        .setCallback(new CommentGalleryListener(context,
                                activity.getStageId(), getTag(), mCommentId));
                EhApplication.getEhClient().execute(request);
                hideSoftInput();
                hideEditPanel(true);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mInAnimation) {
            return;
        }
        if (null != mEditPanel && mEditPanel.getVisibility() == View.VISIBLE) {
            hideEditPanel(true);
        } else {
            finish();
        }
    }

    private void onRefreshGallerySuccess(GalleryCommentList result) {
        if (mGalleryDetail == null || mAdapter == null) {
            return;
        }

        mRefreshLayout.setRefreshing(false);
        mRefreshingComments = false;
        mGalleryDetail.comments = result;
        mAdapter.notifyDataSetChanged();

        updateView(true);

        Bundle re = new Bundle();
        re.putParcelable(KEY_COMMENT_LIST, result);
        setResult(SceneFragment.RESULT_OK, re);
    }

    private void onRefreshGalleryFailure() {
        if (mAdapter == null) {
            return;
        }

        mRefreshLayout.setRefreshing(false);
        mRefreshingComments = false;
        int position = mAdapter.getItemCount() - 1;
        if (position >= 0) {
            mAdapter.notifyItemChanged(position);
        }
    }

    private void onCommentGallerySuccess(GalleryCommentList result) {
        if (mGalleryDetail == null || mAdapter == null) {
            return;
        }

        mGalleryDetail.comments = result;
        mAdapter.notifyDataSetChanged();
        Bundle re = new Bundle();
        re.putParcelable(KEY_COMMENT_LIST, result);
        setResult(SceneFragment.RESULT_OK, re);

        // Remove text
        if (mEditText != null) {
            mEditText.setText("");
        }

        updateView(true);
    }

    private void onVoteCommentSuccess(VoteCommentParser.Result result) {
        if (mAdapter == null || mGalleryDetail.comments == null || mGalleryDetail.comments.comments == null) {
            return;
        }

        int position = -1;
        for (int i = 0, n = mGalleryDetail.comments.comments.length; i < n; i++) {
            GalleryComment comment = mGalleryDetail.comments.comments[i];
            if (comment.id == result.id) {
                position = i;
                break;
            }
        }

        if (-1 == position) {
            Log.d(TAG, "Can't find comment with id " + result.id);
            return;
        }

        // Update comment
        GalleryComment comment = mGalleryDetail.comments.comments[position];
        comment.score = result.score;
        if (result.expectVote > 0) {
            comment.voteUpEd = 0 != result.vote;
            comment.voteDownEd = false;
        } else {
            comment.voteDownEd = 0 != result.vote;
            comment.voteUpEd = false;
        }

        mAdapter.notifyItemChanged(position);

        Bundle re = new Bundle();
        re.putParcelable(KEY_COMMENT_LIST, mGalleryDetail.comments);
        setResult(SceneFragment.RESULT_OK, re);
    }

    @Override
    public void onRefresh() {
        if (!mRefreshingComments && mAdapter != null) {
            MainActivity activity = (MainActivity) requireActivity();
            mRefreshingComments = true;

            String url = getGalleryDetailUrl();
            if (url != null) {
                // Request
                EhRequest request = new EhRequest()
                        .setMethod(EhClient.METHOD_GET_GALLERY_DETAIL)
                        .setArgs(url)
                        .setCallback(new RefreshCommentListener(activity, activity.getStageId(), getTag()));
                EhApplication.getEhClient().execute(request);
            }
        }
    }

    private static class RefreshCommentListener extends EhCallback<GalleryCommentsScene, GalleryDetail> {

        public RefreshCommentListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(GalleryDetail result) {
            GalleryCommentsScene scene = getScene();
            if (scene != null) {
                scene.onRefreshGallerySuccess(result.comments);
            }
        }

        @Override
        public void onFailure(Exception e) {
            GalleryCommentsScene scene = getScene();
            if (scene != null) {
                scene.onRefreshGalleryFailure();
            }
        }

        @Override
        public void onCancel() {
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryCommentsScene;
        }
    }

    private static class CommentGalleryListener extends EhCallback<GalleryCommentsScene, GalleryCommentList> {

        private final long mCommentId;

        public CommentGalleryListener(Context context, int stageId, String sceneTag, long commentId) {
            super(context, stageId, sceneTag);
            mCommentId = commentId;
        }

        @Override
        public void onSuccess(GalleryCommentList result) {
            showTip(mCommentId != 0 ? R.string.edit_comment_successfully : R.string.comment_successfully, LENGTH_SHORT);

            GalleryCommentsScene scene = getScene();
            if (scene != null) {
                scene.onCommentGallerySuccess(result);
            }
        }

        @Override
        public void onFailure(Exception e) {
            showTip(getContent().getString(mCommentId != 0 ? R.string.edit_comment_failed : R.string.comment_failed) + "\n" + ExceptionUtils.getReadableString(e), LENGTH_LONG);
        }

        @Override
        public void onCancel() {
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryCommentsScene;
        }
    }

    private static class VoteCommentListener extends EhCallback<GalleryCommentsScene, VoteCommentParser.Result> {

        public VoteCommentListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(VoteCommentParser.Result result) {
            showTip(result.expectVote > 0 ?
                            (0 != result.vote ? R.string.vote_up_successfully : R.string.cancel_vote_up_successfully) :
                            (0 != result.vote ? R.string.vote_down_successfully : R.string.cancel_vote_down_successfully),
                    LENGTH_SHORT);

            GalleryCommentsScene scene = getScene();
            if (scene != null) {
                scene.onVoteCommentSuccess(result);
            }
        }

        @Override
        public void onFailure(Exception e) {
            showTip(R.string.vote_failed, LENGTH_LONG);
        }

        @Override
        public void onCancel() {
        }

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryCommentsScene;
        }
    }

    private static class InfoHolder extends RecyclerView.ViewHolder {

        private final TextView key;
        private final TextView value;

        public InfoHolder(View itemView) {
            super(itemView);
            key = (TextView) ViewUtils.$$(itemView, R.id.key);
            value = (TextView) ViewUtils.$$(itemView, R.id.value);
        }
    }

    private abstract static class CommentHolder extends RecyclerView.ViewHolder {
        public CommentHolder(LayoutInflater inflater, int resId, ViewGroup parent) {
            super(inflater.inflate(resId, parent, false));
        }
    }

    private static class MoreCommentHolder extends CommentHolder {
        public MoreCommentHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater, R.layout.item_gallery_comment_more, parent);
        }
    }

    private static class ProgressCommentHolder extends CommentHolder {
        public ProgressCommentHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater, R.layout.item_gallery_comment_progress, parent);
        }
    }

    private class ActualCommentHolder extends CommentHolder {

        private final TextView user;
        private final TextView time;
        private final LinkifyTextView comment;

        public ActualCommentHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater, R.layout.item_gallery_comment, parent);
            user = itemView.findViewById(R.id.user);
            time = itemView.findViewById(R.id.time);
            comment = itemView.findViewById(R.id.comment);
        }

        private CharSequence generateComment(Context context, ObservedTextView textView, GalleryComment comment) {
            Spanned sp = Html.fromHtml(comment.comment, Html.FROM_HTML_MODE_LEGACY, new URLImageGetter(textView), null);

            SpannableStringBuilder ssb = new SpannableStringBuilder(sp);

            if (0 != comment.id && 0 != comment.score) {
                int score = comment.score;
                String scoreString = score > 0 ? "+" + score : Integer.toString(score);
                SpannableString ss = new SpannableString(scoreString);
                ss.setSpan(new RelativeSizeSpan(0.8f), 0, scoreString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, scoreString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new ForegroundColorSpan(ResourcesKt.resolveColor(getTheme(), android.R.attr.textColorSecondary))
                        , 0, scoreString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append("  ").append(ss);
            }

            if (comment.lastEdited != 0) {
                String str = context.getString(R.string.last_edited, ReadableTime.getTimeAgo(comment.lastEdited));
                SpannableString ss = new SpannableString(str);
                ss.setSpan(new RelativeSizeSpan(0.8f), 0, str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new ForegroundColorSpan(ResourcesKt.resolveColor(getTheme(), android.R.attr.textColorSecondary)),
                        0, str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append("\n\n").append(ss);
            }

            return TextUrl.handleTextUrl(ssb);
        }

        public void bind(GalleryComment value) {
            user.setText(value.uploader ? getString(R.string.comment_user_uploader, value.user) : value.user);
            user.setOnClickListener(v -> {
                ListUrlBuilder lub = new ListUrlBuilder();
                lub.setMode(ListUrlBuilder.MODE_UPLOADER);
                lub.setKeyword(value.user);
                GalleryListScene.startScene(GalleryCommentsScene.this, lub);
            });
            time.setText(ReadableTime.getTimeAgo(value.time));
            comment.setText(generateComment(comment.getContext(), comment, value));
        }
    }

    private class CommentAdapter extends RecyclerView.Adapter<CommentHolder> {

        private final LayoutInflater mInflater;

        public CommentAdapter() {
            mInflater = getLayoutInflater();
            AssertUtils.assertNotNull(mInflater);
        }

        @NonNull
        @Override
        public CommentHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case TYPE_COMMENT:
                    return new ActualCommentHolder(mInflater, parent);
                case TYPE_MORE:
                    return new MoreCommentHolder(mInflater, parent);
                case TYPE_PROGRESS:
                    return new ProgressCommentHolder(mInflater, parent);
                default:
                    throw new IllegalStateException("Invalid view type: " + viewType);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull CommentHolder holder, int position) {
            Context context = getContext();
            if (context == null || mGalleryDetail == null || mGalleryDetail.comments == null) {
                return;
            }

            holder.itemView.setOnClickListener(v -> onItemClick(mRecyclerView, holder.itemView, position));
            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);


            if (holder instanceof ActualCommentHolder) {
                ((ActualCommentHolder) holder).bind(mGalleryDetail.comments.comments[position]);
            }
        }

        @Override
        public int getItemCount() {
            if (mGalleryDetail == null || mGalleryDetail.comments == null || mGalleryDetail.comments.comments == null) {
                return 0;
            } else if (mGalleryDetail.comments.hasMore) {
                return mGalleryDetail.comments.comments.length + 1;
            } else {
                return mGalleryDetail.comments.comments.length;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position >= mGalleryDetail.comments.comments.length) {
                return mRefreshingComments ? TYPE_PROGRESS : TYPE_MORE;
            } else {
                return TYPE_COMMENT;
            }
        }
    }
}
