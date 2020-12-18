/*
 * Copyright 2015 Hippo Seven
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

package com.hippo.ehviewer.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hippo.android.resource.AttrResources;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.LinearDividerItemDecoration;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhTagDatabase;
import com.hippo.view.ViewTransition;
import com.hippo.yorozuya.AnimationUtils;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.SimpleAnimatorListener;
import com.hippo.yorozuya.ViewUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchBar extends MaterialCardView implements View.OnClickListener,
        TextView.OnEditorActionListener, TextWatcher,
        SearchEditText.SearchEditTextListener {

    public static final int STATE_NORMAL = 0;
    public static final int STATE_SEARCH = 1;
    public static final int STATE_SEARCH_LIST = 2;
    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_STATE = "state";
    private static final long ANIMATE_TIME = 300L;
    private final Rect mRect = new Rect();
    private int mState = STATE_NORMAL;
    private int mWidth;
    private int mHeight;
    private int mBaseHeight;
    private float mProgress;

    private ImageView mMenuButton;
    private TextView mTitleTextView;
    private ImageView mActionButton;
    private SearchEditText mEditText;
    private EasyRecyclerView mListView;
    private View mListContainer;

    private ViewTransition mViewTransition;

    private SearchDatabase mSearchDatabase;
    private List<Suggestion> mSuggestionList;
    private SuggestionAdapter mSuggestionAdapter;

    private Helper mHelper;
    private OnStateChangeListener mOnStateChangeListener;
    private SuggestionProvider mSuggestionProvider;

    private boolean mAllowEmptySearch = true;

    private boolean mInAnimation;

    public SearchBar(Context context) {
        super(context);
        init(context);
    }

    public SearchBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SearchBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mSearchDatabase = SearchDatabase.getInstance(getContext());

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.widget_search_bar, this);
        mMenuButton = (ImageView) ViewUtils.$$(this, R.id.search_menu);
        mTitleTextView = (TextView) ViewUtils.$$(this, R.id.search_title);
        mActionButton = (ImageView) ViewUtils.$$(this, R.id.search_action);
        mEditText = (SearchEditText) ViewUtils.$$(this, R.id.search_edit_text);
        mListContainer = ViewUtils.$$(this, R.id.list_container);
        mListView = (EasyRecyclerView) ViewUtils.$$(mListContainer, R.id.search_bar_list);

        mViewTransition = new ViewTransition(mTitleTextView, mEditText);

        mTitleTextView.setOnClickListener(this);
        mMenuButton.setOnClickListener(this);
        mActionButton.setOnClickListener(this);
        mEditText.setSearchEditTextListener(this);
        mEditText.setOnEditorActionListener(this);
        mEditText.addTextChangedListener(this);

        // Get base height
        ViewUtils.measureView(this, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mBaseHeight = getMeasuredHeight();

        mSuggestionList = new ArrayList<>();
        mSuggestionAdapter = new SuggestionAdapter(LayoutInflater.from(getContext()));
        mListView.setAdapter(mSuggestionAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL,
                AttrResources.getAttrColor(context, R.attr.dividerColor),
                LayoutUtils.dp2pix(context, 1));
        decoration.setShowFirstDivider(true);
        decoration.setShowLastDivider(false);
        mListView.addItemDecoration(decoration);
        mListView.setLayoutManager(layoutManager);
    }

    private void updateSuggestions() {
        updateSuggestions(true);
    }

    private void updateSuggestions(boolean scrollToTop) {
        List<Suggestion> suggestions = new ArrayList<>();

        String text = mEditText.getText().toString();

        if (mSuggestionProvider != null) {
            List<Suggestion> providerSuggestions = mSuggestionProvider.providerSuggestions(text);
            if (providerSuggestions != null && !providerSuggestions.isEmpty()) {
                suggestions.addAll(providerSuggestions);
            }
        }

        String[] keywords = mSearchDatabase.getSuggestions(text, 128);
        for (String keyword : keywords) {
            suggestions.add(new KeywordSuggestion(keyword));
        }

        EhTagDatabase ehTagDatabase = EhTagDatabase.getInstance(getContext());
        if (!TextUtils.isEmpty(text) && ehTagDatabase != null && !text.endsWith(" ")) {
            String[] s = text.split(" ");
            if (s.length > 0) {
                String keyword = s[s.length - 1];
                ArrayList<Pair<String, String>> searchHints = ehTagDatabase.suggest(keyword);

                for (Pair<String, String> searchHint : searchHints) {
                    suggestions.add(new TagSuggestion(searchHint.first, searchHint.second));
                }

            }
        }

        mSuggestionList = suggestions;
        mSuggestionAdapter.notifyDataSetChanged();

        if (scrollToTop) {
            mListView.scrollToPosition(0);
        }
    }

    public void setAllowEmptySearch(boolean allowEmptySearch) {
        mAllowEmptySearch = allowEmptySearch;
    }

    public void setEditTextHint(CharSequence hint) {
        mEditText.setHint(hint);
    }

    public void setHelper(Helper helper) {
        mHelper = helper;
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        mOnStateChangeListener = listener;
    }

    public void setSuggestionProvider(SuggestionProvider suggestionProvider) {
        mSuggestionProvider = suggestionProvider;
    }

    public String getText() {
        return mEditText.getText().toString();
    }

    public void setText(String text) {
        mEditText.setText(text);
    }

    public void cursorToEnd() {
        mEditText.setSelection(mEditText.getText().length());
    }

    public void setTitle(String title) {
        mTitleTextView.setText(title);
    }

    public void setSearch(String search) {
        mTitleTextView.setText(search);
        mEditText.setText(search);
    }

    public void setLeftDrawable(Drawable drawable) {
        mMenuButton.setImageDrawable(drawable);
    }

    public void setRightDrawable(Drawable drawable) {
        mActionButton.setImageDrawable(drawable);
    }

    public void applySearch() {
        String query = mEditText.getText().toString().trim();

        if (!mAllowEmptySearch && TextUtils.isEmpty(query)) {
            return;
        }

        // Put it into db
        mSearchDatabase.addQuery(query);
        // Callback
        mHelper.onApplySearch(query);
    }

    @Override
    public void onClick(View v) {
        if (v == mTitleTextView) {
            mHelper.onClickTitle();
        } else if (v == mMenuButton) {
            mHelper.onClickLeftIcon();
        } else if (v == mActionButton) {
            mHelper.onClickRightIcon();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == mEditText) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_NULL) {
                applySearch();
                return true;
            }
        }
        return false;
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        setState(state, true);
    }

    public void setState(int state, boolean animation) {
        if (mState != state) {
            int oldState = mState;
            mState = state;

            switch (oldState) {
                default:
                case STATE_NORMAL:
                    mViewTransition.showView(1, animation);
                    mEditText.requestFocus();

                    if (state == STATE_SEARCH_LIST) {
                        showImeAndSuggestionsList(animation);
                    }
                    if (mOnStateChangeListener != null) {
                        mOnStateChangeListener.onStateChange(this, state, oldState, animation);
                    }
                    break;
                case STATE_SEARCH:
                    if (state == STATE_NORMAL) {
                        mViewTransition.showView(0, animation);
                    } else if (state == STATE_SEARCH_LIST) {
                        showImeAndSuggestionsList(animation);
                    }
                    if (mOnStateChangeListener != null) {
                        mOnStateChangeListener.onStateChange(this, state, oldState, animation);
                    }
                    break;
                case STATE_SEARCH_LIST:
                    hideImeAndSuggestionsList(animation);
                    if (state == STATE_NORMAL) {
                        mViewTransition.showView(0, animation);
                    }
                    if (mOnStateChangeListener != null) {
                        mOnStateChangeListener.onStateChange(this, state, oldState, animation);
                    }
                    break;
            }
        }
    }

    public void showImeAndSuggestionsList(boolean animation) {
        // Show ime
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEditText, 0);
        // update suggestion for show suggestions list
        updateSuggestions();
        // Show suggestions list
        if (animation) {
            ObjectAnimator oa = ObjectAnimator.ofFloat(this, "progress", 1f);
            oa.setDuration(ANIMATE_TIME);
            oa.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
            oa.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mListContainer.setVisibility(View.VISIBLE);
                    mInAnimation = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mInAnimation = false;
                }
            });
            oa.setAutoCancel(true);
            oa.start();
        } else {
            mListContainer.setVisibility(View.VISIBLE);
            setProgress(1f);
        }
    }

    private void hideImeAndSuggestionsList(boolean animation) {
        // Hide ime
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        // Hide suggestions list
        if (animation) {
            ObjectAnimator oa = ObjectAnimator.ofFloat(this, "progress", 0f);
            oa.setDuration(ANIMATE_TIME);
            oa.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
            oa.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mInAnimation = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mListContainer.setVisibility(View.GONE);
                    mInAnimation = false;
                }
            });
            oa.setAutoCancel(true);
            oa.start();
        } else {
            setProgress(0f);
            mListContainer.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mListContainer.getVisibility() == View.VISIBLE && (!mInAnimation || mHeight == 0)) {
            mWidth = right - left;
            mHeight = bottom - top;
        }
    }

    public float getProgress() {
        return mProgress;
    }

    @Keep
    public void setProgress(float progress) {
        mProgress = progress;
        invalidate();
    }

    public SearchEditText getEditText() {
        return mEditText;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mInAnimation && mHeight != 0) {
            final int state = canvas.save();
            int bottom = MathUtils.lerp(mBaseHeight, mHeight, mProgress);
            mRect.set(0, 0, mWidth, bottom);
            setClipBounds(mRect);
            canvas.clipRect(mRect);
            super.draw(canvas);
            canvas.restoreToCount(state);
        } else {
            setClipBounds(null);
            super.draw(canvas);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Empty
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Empty
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateSuggestions();
    }

    @Override
    public void onClick() {
        mHelper.onSearchEditTextClick();
    }

    @Override
    public void onBackPressed() {
        mHelper.onSearchEditTextBackPressed();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState());
        state.putInt(STATE_KEY_STATE, mState);
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;
            super.onRestoreInstanceState(savedState.getParcelable(STATE_KEY_SUPER));
            setState(savedState.getInt(STATE_KEY_STATE), false);
        }
    }

    public interface Helper {
        void onClickTitle();

        void onClickLeftIcon();

        void onClickRightIcon();

        void onSearchEditTextClick();

        void onApplySearch(String query);

        void onSearchEditTextBackPressed();
    }

    public interface OnStateChangeListener {

        void onStateChange(SearchBar searchBar, int newState, int oldState, boolean animation);
    }

    public interface SuggestionProvider {

        List<Suggestion> providerSuggestions(String text);
    }

    public abstract static class Suggestion {

        public abstract CharSequence getText(TextView textView);

        public abstract void onClick();

        public boolean onLongClick() {
            return false;
        }

    }

    private static class SuggestionHolder extends RecyclerView.ViewHolder {
        TextView text1;
        TextView text2;

        public SuggestionHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }

    private class SuggestionAdapter extends RecyclerView.Adapter<SuggestionHolder> {

        private final LayoutInflater mInflater;

        private SuggestionAdapter(LayoutInflater inflater) {
            mInflater = inflater;
        }

        @NonNull
        @Override
        public SuggestionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new SuggestionHolder(mInflater.inflate(R.layout.item_simple_list_2, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull SuggestionHolder holder, int position) {
            Suggestion suggestion = mSuggestionList.get(position);
            CharSequence text1 = suggestion.getText(holder.text1);
            CharSequence text2 = suggestion.getText(holder.text2);
            holder.text1.setText(text1);
            if (text2 == null) {
                holder.text2.setVisibility(View.GONE);
                holder.text2.setText("");
            } else {
                holder.text2.setVisibility(View.VISIBLE);
                holder.text2.setText(text2);
            }

            holder.itemView.setOnClickListener(v -> {
                if (position < mSuggestionList.size()) {
                    mSuggestionList.get(position).onClick();
                }
            });
            holder.itemView.setOnLongClickListener(v -> {
                if (position < mSuggestionList.size()) {
                    return mSuggestionList.get(position).onLongClick();
                } else {
                    return false;
                }
            });
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return mSuggestionList.size();
        }

    }

    public class TagSuggestion extends SearchBar.Suggestion {
        public String mHint;
        public String mKeyword;

        private TagSuggestion(String hint, String keyword) {
            mHint = hint;
            mKeyword = keyword;
        }

        @Override
        public CharSequence getText(TextView textView) {
            if (textView.getId() == android.R.id.text1) {
                return mKeyword;
            } else {
                return mHint;
            }
        }

        @Override
        public void onClick() {
            Editable editable = mEditText.getText();
            if (editable != null) {
                String text = editable.toString();
                String temp = wrapTagKeyword(mKeyword) + " ";
                if (text.contains(" ")) {
                    temp = text.substring(0, text.lastIndexOf(" ")) + " " + temp;
                }
                mEditText.setText(temp);
                mEditText.setSelection(editable.length());
            }
        }
    }

    public class KeywordSuggestion extends Suggestion {

        private final String mKeyword;

        private KeywordSuggestion(String keyword) {
            mKeyword = keyword;
        }

        @Override
        public CharSequence getText(TextView textView) {
            if (textView.getId() == android.R.id.text1) {
                return mKeyword;
            } else {
                return null;
            }
        }

        @Override
        public void onClick() {
            mEditText.setText(mKeyword);
            mEditText.setSelection(mEditText.length());
        }

        @Override
        public boolean onLongClick() {
            new MaterialAlertDialogBuilder(getContext())
                    .setMessage(getContext().getString(R.string.delete_search_history, mKeyword))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        mSearchDatabase.deleteQuery(mKeyword);
                        updateSuggestions(false);
                    })
                    .show();
            return true;
        }
    }

    private String wrapTagKeyword(String keyword) {
        keyword = keyword.trim();

        int index1 = keyword.indexOf(':');
        if (index1 == -1 || index1 >= keyword.length() - 1) {
            // Can't find :, or : is the last char
            return keyword;
        }
        if (keyword.charAt(index1 + 1) == '"') {
            // The char after : is ", the word must be quoted
            return keyword;
        }
        int index2 = keyword.indexOf(' ');
        if (index2 <= index1) {
            // Can't find space, or space is before :
            return keyword;
        }

        return keyword.substring(0, index1 + 1) + "\"" + keyword.substring(index1 + 1) + "$\"";
    }
}
