package com.hippo.ehviewer.ui.fragment;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.hippo.android.resource.AttrResources;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.LinearDividerItemDecoration;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhFilter;
import com.hippo.ehviewer.dao.Filter;
import com.hippo.view.ViewTransition;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.ViewUtils;

import java.util.List;

public class FilterFragment extends BaseFragment {

    @Nullable
    private ViewTransition mViewTransition;
    @Nullable
    private FilterAdapter mAdapter;
    @Nullable
    private FilterList mFilterList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_filter, container, false);

        mFilterList = new FilterList();

        RecyclerView recyclerView = (EasyRecyclerView) ViewUtils.$$(view, R.id.recycler_view);
        TextView tip = (TextView) ViewUtils.$$(view, R.id.tip);
        mViewTransition = new ViewTransition(recyclerView, tip);
        FloatingActionButton fab = view.findViewById(R.id.fab);

        Drawable drawable = ContextCompat.getDrawable(requireActivity(), R.drawable.big_filter);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        tip.setCompoundDrawables(null, drawable, null, null);

        mAdapter = new FilterAdapter();
        mAdapter.setHasStableIds(true);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setClipToPadding(false);
        recyclerView.setClipChildren(false);
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL,
                AttrResources.getAttrColor(requireActivity(), R.attr.dividerColor),
                LayoutUtils.dp2pix(requireActivity(), 1));
        decoration.setShowLastDivider(true);
        recyclerView.addItemDecoration(decoration);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        DefaultItemAnimator defaultItemAnimator = (DefaultItemAnimator) recyclerView.getItemAnimator();
        if (defaultItemAnimator != null) {
            defaultItemAnimator.setSupportsChangeAnimations(false);
        }
        recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(),
                recyclerView.getPaddingBottom() + getResources().getDimensionPixelOffset(R.dimen.gallery_padding_bottom_fab));

        fab.setOnClickListener(v -> showAddFilterDialog());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateView(false);
    }

    private void updateView(boolean animation) {
        if (null == mViewTransition) {
            return;
        }

        if (null == mFilterList || 0 == mFilterList.size()) {
            mViewTransition.showView(1, animation);
        } else {
            mViewTransition.showView(0, animation);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mViewTransition = null;
        mAdapter = null;
        mFilterList = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_filter, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_tip) {
            showTipDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showTipDialog() {
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.filter)
                .setMessage(R.string.filter_tip)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showAddFilterDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.add_filter)
                .setView(R.layout.dialog_add_filter)
                .setPositiveButton(R.string.add, null)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        AddFilterDialogHelper helper = new AddFilterDialogHelper();
        helper.setDialog(dialog);
    }

    private void showDeleteFilterDialog(final Filter filter) {
        String message = getString(R.string.delete_filter, filter.text);
        new MaterialAlertDialogBuilder(requireActivity())
                .setMessage(message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (DialogInterface.BUTTON_POSITIVE != which || null == mFilterList) {
                        return;
                    }
                    mFilterList.delete(filter);
                    if (null != mAdapter) {
                        mAdapter.notifyDataSetChanged();
                    }
                    updateView(true);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private class AddFilterDialogHelper implements View.OnClickListener {

        @Nullable
        private AlertDialog mDialog;
        @Nullable
        private Spinner mSpinner;
        @Nullable
        private TextInputLayout mInputLayout;
        @Nullable
        private EditText mEditText;

        public void setDialog(AlertDialog dialog) {
            mDialog = dialog;
            mSpinner = (Spinner) ViewUtils.$$(dialog, R.id.spinner);
            mInputLayout = (TextInputLayout) ViewUtils.$$(dialog, R.id.text_input_layout);
            mEditText = mInputLayout.getEditText();
            View button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (null != button) {
                button.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            if (null == mFilterList || null == mDialog || null == mSpinner ||
                    null == mInputLayout || null == mEditText) {
                return;
            }

            String text = mEditText.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                mInputLayout.setError(getString(R.string.text_is_empty));
                return;
            } else {
                mInputLayout.setError(null);
            }
            int mode = mSpinner.getSelectedItemPosition();

            Filter filter = new Filter();
            filter.mode = mode;
            filter.text = text;
            if (!mFilterList.add(filter)) {
                mInputLayout.setError(getString(R.string.label_text_exist));
                return;
            } else {
                mInputLayout.setError(null);
            }

            if (null != mAdapter) {
                mAdapter.notifyDataSetChanged();
            }
            updateView(true);

            mDialog.dismiss();
            mDialog = null;
            mSpinner = null;
            mInputLayout = null;
            mEditText = null;
        }
    }

    private static class FilterHolder extends RecyclerView.ViewHolder {

        private final MaterialCheckBox checkbox;
        private final TextView text;
        private final ImageView delete;

        public FilterHolder(View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox);
            text = itemView.findViewById(R.id.text);
            delete = itemView.findViewById(R.id.delete);
        }
    }

    private class FilterAdapter extends RecyclerView.Adapter<FilterHolder> {

        private static final int TYPE_ITEM = 0;
        private static final int TYPE_HEADER = 1;

        @Override
        public int getItemViewType(int position) {
            if (null == mFilterList) {
                return TYPE_ITEM;
            }

            if (mFilterList.get(position).mode == FilterList.MODE_HEADER) {
                return TYPE_HEADER;
            } else {
                return TYPE_ITEM;
            }
        }

        @NonNull
        @Override
        public FilterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layoutId;
            switch (viewType) {
                default:
                case TYPE_ITEM:
                    layoutId = R.layout.item_filter;
                    break;
                case TYPE_HEADER:
                    layoutId = R.layout.item_filter_header;
                    break;
            }

            return new FilterHolder(getLayoutInflater().inflate(layoutId, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull FilterHolder holder, int position) {
            if (null == mFilterList) {
                return;
            }

            Filter filter = mFilterList.get(position);
            if (FilterList.MODE_HEADER == filter.mode) {
                holder.text.setText(filter.text);
            } else {
                holder.checkbox.setText(filter.text);
                holder.checkbox.setChecked(filter.enable);
                holder.itemView.setOnClickListener(v -> {
                    mFilterList.trigger(filter);

                    //for updating delete line on filter text
                    if (mAdapter != null) {
                        mAdapter.notifyItemChanged(position);
                    }
                });
                holder.delete.setOnClickListener(v -> showDeleteFilterDialog(filter));
            }
        }

        @Override
        public int getItemCount() {
            return null != mFilterList ? mFilterList.size() : 0;
        }

        @Override
        public long getItemId(int position) {
            if (mFilterList == null) {
                return 0;
            } else {
                Filter filter = mFilterList.get(position);
                if (filter.getId() != null) {
                    return (filter.text.hashCode() >> filter.mode) + filter.getId();
                }
                return filter.text.hashCode() >> filter.mode;
            }
        }
    }

    private class FilterList {

        public static final int MODE_HEADER = -1;

        private final EhFilter mEhFilter;
        private final List<Filter> mTitleFilterList;
        private final List<Filter> mUploaderFilterList;
        private final List<Filter> mTagFilterList;
        private final List<Filter> mTagNamespaceFilterList;

        private Filter mTitleHeader;
        private Filter mUploaderHeader;
        private Filter mTagHeader;
        private Filter mTagNamespaceHeader;

        public FilterList() {
            mEhFilter = EhFilter.getInstance();
            mTitleFilterList = mEhFilter.getTitleFilterList();
            mUploaderFilterList = mEhFilter.getUploaderFilterList();
            mTagFilterList = mEhFilter.getTagFilterList();
            mTagNamespaceFilterList = mEhFilter.getTagNamespaceFilterList();
        }

        public int size() {
            int count = 0;
            int size = mTitleFilterList.size();
            count += 0 == size ? 0 : size + 1;
            size = mUploaderFilterList.size();
            count += 0 == size ? 0 : size + 1;
            size = mTagFilterList.size();
            count += 0 == size ? 0 : size + 1;
            size = mTagNamespaceFilterList.size();
            count += 0 == size ? 0 : size + 1;
            return count;
        }

        private Filter getTitleHeader() {
            if (null == mTitleHeader) {
                mTitleHeader = new Filter();
                mTitleHeader.mode = MODE_HEADER;
                mTitleHeader.text = getString(R.string.filter_title);
            }
            return mTitleHeader;
        }

        private Filter getUploaderHeader() {
            if (null == mUploaderHeader) {
                mUploaderHeader = new Filter();
                mUploaderHeader.mode = MODE_HEADER;
                mUploaderHeader.text = getString(R.string.filter_uploader);
            }
            return mUploaderHeader;
        }

        private Filter getTagHeader() {
            if (null == mTagHeader) {
                mTagHeader = new Filter();
                mTagHeader.mode = MODE_HEADER;
                mTagHeader.text = getString(R.string.filter_tag);
            }
            return mTagHeader;
        }

        private Filter getTagNamespaceHeader() {
            if (null == mTagNamespaceHeader) {
                mTagNamespaceHeader = new Filter();
                mTagNamespaceHeader.mode = MODE_HEADER;
                mTagNamespaceHeader.text = getString(R.string.filter_tag_namespace);
            }
            return mTagNamespaceHeader;
        }

        public Filter get(int index) {
            int size = mTitleFilterList.size();
            if (0 != size) {
                if (index == 0) {
                    return getTitleHeader();
                } else if (index <= size) {
                    return mTitleFilterList.get(index - 1);
                } else {
                    index -= size + 1;
                }
            }

            size = mUploaderFilterList.size();
            if (0 != size) {
                if (index == 0) {
                    return getUploaderHeader();
                } else if (index <= size) {
                    return mUploaderFilterList.get(index - 1);
                } else {
                    index -= size + 1;
                }
            }

            size = mTagFilterList.size();
            if (0 != size) {
                if (index == 0) {
                    return getTagHeader();
                } else if (index <= size) {
                    return mTagFilterList.get(index - 1);
                } else {
                    index -= size + 1;
                }
            }

            size = mTagNamespaceFilterList.size();
            if (0 != size) {
                if (index == 0) {
                    return getTagNamespaceHeader();
                } else if (index <= size) {
                    return mTagNamespaceFilterList.get(index - 1);
                }
            }

            throw new IndexOutOfBoundsException();
        }

        public boolean add(Filter filter) {
            return mEhFilter.addFilter(filter);
        }

        public void delete(Filter filter) {
            mEhFilter.deleteFilter(filter);
        }

        public void trigger(Filter filter) {
            mEhFilter.triggerFilter(filter);
        }
    }

    @Override
    public int getFragmentTitle() {
        return R.string.filter;
    }
}
