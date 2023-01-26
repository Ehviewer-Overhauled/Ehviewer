/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.hippo.ehviewer.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputLayout
import com.hippo.app.BaseDialogBuilder
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhFilter
import com.hippo.ehviewer.dao.Filter
import com.hippo.view.ViewTransition
import com.hippo.yorozuya.ViewUtils

class FilterFragment : BaseFragment() {
    private var mViewTransition: ViewTransition? = null
    private var mAdapter: FilterAdapter = FilterAdapter()
    private var mFilterList: FilterList = FilterList()
    private val mMenuProvider: MenuProvider = FilterMenuProvider()

    inner class FilterMenuProvider : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.activity_filter, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            val itemId = menuItem.itemId
            if (itemId == R.id.action_tip) {
                showTipDialog()
                return true
            }
            return false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val recyclerView =
            inflater.inflate(R.layout.rv_layout, container, false) as RecyclerView
        val tip = getTipView(R.string.filter)
        mViewTransition = ViewTransition(recyclerView, tip)
        val fab = getFabViewAndShow()
        val drawable = ContextCompat.getDrawable(requireActivity(), R.drawable.big_filter)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        tip.setCompoundDrawables(null, drawable, null, null)
        mAdapter.setHasStableIds(true)
        recyclerView.adapter = mAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        val defaultItemAnimator = recyclerView.itemAnimator as DefaultItemAnimator?
        defaultItemAnimator?.supportsChangeAnimations = false
        fab.setOnClickListener { showAddFilterDialog() }
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateView(false)
        requireActivity().addMenuProvider(mMenuProvider)
    }

    private fun updateView(animation: Boolean) {
        if (null == mViewTransition) {
            return
        }
        if (0 == mFilterList.size()) {
            mViewTransition!!.showView(1, animation)
        } else {
            mViewTransition!!.showView(0, animation)
        }
    }

    override fun onDestroyView() {
        mViewTransition = null
        requireActivity().removeMenuProvider(mMenuProvider)
        super.onDestroyView()
    }

    private fun showTipDialog() {
        BaseDialogBuilder(requireActivity())
            .setTitle(R.string.filter)
            .setMessage(R.string.filter_tip)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showAddFilterDialog() {
        val dialog = BaseDialogBuilder(requireActivity())
            .setTitle(R.string.add_filter)
            .setView(R.layout.dialog_add_filter)
            .setPositiveButton(R.string.add, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        AddFilterDialogHelper(dialog)
    }

    private fun showDeleteFilterDialog(filter: Filter) {
        val message = getString(R.string.delete_filter, filter.text)
        BaseDialogBuilder(requireActivity())
            .setMessage(message)
            .setPositiveButton(R.string.delete) { _: DialogInterface?, which: Int ->
                if (DialogInterface.BUTTON_POSITIVE != which) {
                    return@setPositiveButton
                }
                mFilterList.delete(filter)
                mAdapter.notifyDataSetChanged()
                updateView(true)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun getFragmentTitle(): Int {
        return R.string.filter
    }

    private class FilterHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: MaterialCheckBox?
        val text: TextView?
        val delete: ImageView?

        init {
            checkbox = itemView.findViewById(R.id.checkbox)
            text = itemView.findViewById(R.id.text)
            delete = itemView.findViewById(R.id.delete)
        }
    }

    private inner class AddFilterDialogHelper(dialog: AlertDialog) : View.OnClickListener {
        private var mDialog: AlertDialog
        private var mSpinner: TextInputLayout
        private var mInputLayout: TextInputLayout
        private var mEditText: EditText
        private val mArray: Array<String> =
            context!!.resources.getStringArray(R.array.filter_entries)

        init {
            mDialog = dialog
            mSpinner = ViewUtils.`$$`(dialog, R.id.spinner) as TextInputLayout
            mInputLayout = ViewUtils.`$$`(dialog, R.id.text_input_layout) as TextInputLayout
            mEditText = mInputLayout.editText!!
            val button: View? = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            button?.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            val text1 = mSpinner.editText!!.text.toString()
            if (TextUtils.isEmpty(text1)) {
                mSpinner.error = getString(R.string.text_is_empty)
                return
            } else {
                mSpinner.error = null
            }
            val text = mEditText.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(text)) {
                mInputLayout.error = getString(R.string.text_is_empty)
                return
            } else {
                mInputLayout.error = null
            }
            val mode = mArray.indexOf(text1)
            val filter = Filter()
            filter.mode = mode
            filter.text = text
            if (!mFilterList.add(filter)) {
                mInputLayout.error = getString(R.string.label_text_exist)
                return
            } else {
                mInputLayout.error = null
            }
            mAdapter.notifyDataSetChanged()
            updateView(true)
            mDialog.dismiss()
        }
    }

    private inner class FilterAdapter : RecyclerView.Adapter<FilterHolder>() {
        override fun getItemViewType(position: Int): Int {
            return if (mFilterList[position].mode == MODE_HEADER) {
                TYPE_HEADER
            } else {
                TYPE_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterHolder {
            val layoutId: Int = when (viewType) {
                TYPE_ITEM -> R.layout.item_filter
                TYPE_HEADER -> R.layout.item_filter_header
                else -> R.layout.item_filter
            }
            return FilterHolder(layoutInflater.inflate(layoutId, parent, false))
        }

        override fun onBindViewHolder(holder: FilterHolder, position: Int) {
            val filter = mFilterList[position]
            if (MODE_HEADER == filter.mode) {
                holder.text?.text = filter.text
            } else {
                holder.checkbox?.text = filter.text
                holder.checkbox?.isChecked = filter.enable!!
                holder.itemView.setOnClickListener {
                    mFilterList.trigger(filter)

                    //for updating delete line on filter text
                    mAdapter.notifyItemChanged(position)
                }
                holder.delete?.setOnClickListener { showDeleteFilterDialog(filter) }
            }
        }

        override fun getItemCount(): Int {
            return mFilterList.size()
        }

        override fun getItemId(position: Int): Long {
            return run {
                val filter = mFilterList[position]
                if (filter.id != null) {
                    (filter.text.hashCode() shr filter.mode) + filter.id!!
                } else (filter.text.hashCode() shr filter.mode).toLong()
            }
        }
    }

    private inner class FilterList {
        private val mEhFilter: EhFilter = EhFilter
        private val mTitleFilterList: List<Filter> = mEhFilter.titleFilterList
        private val mUploaderFilterList: List<Filter> = mEhFilter.uploaderFilterList
        private val mTagFilterList: List<Filter> = mEhFilter.tagFilterList
        private val mTagNamespaceFilterList: List<Filter> = mEhFilter.tagNamespaceFilterList
        private val mCommenterFilterList: List<Filter> = mEhFilter.commenterFilterList
        private val mCommentFilterList: List<Filter> = mEhFilter.commentFilterList
        private var mTitleHeader: Filter? = null
        private var mUploaderHeader: Filter? = null
        private var mTagHeader: Filter? = null
        private var mTagNamespaceHeader: Filter? = null
        private var mCommenterHeader: Filter? = null
        private var mCommentHeader: Filter? = null
        fun size(): Int {
            var count = 0
            var size = mTitleFilterList.size
            count += if (0 == size) 0 else size + 1
            size = mUploaderFilterList.size
            count += if (0 == size) 0 else size + 1
            size = mTagFilterList.size
            count += if (0 == size) 0 else size + 1
            size = mTagNamespaceFilterList.size
            count += if (0 == size) 0 else size + 1
            size = mCommenterFilterList.size
            count += if (0 == size) 0 else size + 1
            size = mCommentFilterList.size
            count += if (0 == size) 0 else size + 1
            return count
        }

        private val titleHeader: Filter
            get() {
                if (null == mTitleHeader) {
                    mTitleHeader = Filter()
                    mTitleHeader!!.mode = MODE_HEADER
                    mTitleHeader!!.text = getString(R.string.filter_title)
                }
                return mTitleHeader!!
            }
        private val uploaderHeader: Filter
            get() {
                if (null == mUploaderHeader) {
                    mUploaderHeader = Filter()
                    mUploaderHeader!!.mode = MODE_HEADER
                    mUploaderHeader!!.text = getString(R.string.filter_uploader)
                }
                return mUploaderHeader!!
            }
        private val tagHeader: Filter
            get() {
                if (null == mTagHeader) {
                    mTagHeader = Filter()
                    mTagHeader!!.mode = MODE_HEADER
                    mTagHeader!!.text = getString(R.string.filter_tag)
                }
                return mTagHeader!!
            }
        private val tagNamespaceHeader: Filter
            get() {
                if (null == mTagNamespaceHeader) {
                    mTagNamespaceHeader = Filter()
                    mTagNamespaceHeader!!.mode = MODE_HEADER
                    mTagNamespaceHeader!!.text = getString(R.string.filter_tag_namespace)
                }
                return mTagNamespaceHeader!!
            }
        private val commenterHeader: Filter
            get() {
                if (null == mCommenterHeader) {
                    mCommenterHeader = Filter()
                    mCommenterHeader!!.mode = MODE_HEADER
                    mCommenterHeader!!.text = getString(R.string.filter_commenter)
                }
                return mCommenterHeader!!
            }
        private val commentHeader: Filter
            get() {
                if (null == mCommentHeader) {
                    mCommentHeader = Filter()
                    mCommentHeader!!.mode = MODE_HEADER
                    mCommentHeader!!.text = getString(R.string.filter_comment)
                }
                return mCommentHeader!!
            }

        operator fun get(index: Int): Filter {
            var index1 = index
            var size = mTitleFilterList.size
            if (0 != size) {
                index1 -= if (index1 == 0) {
                    return titleHeader
                } else if (index1 <= size) {
                    return mTitleFilterList[index1 - 1]
                } else {
                    size + 1
                }
            }
            size = mUploaderFilterList.size
            if (0 != size) {
                index1 -= if (index1 == 0) {
                    return uploaderHeader
                } else if (index1 <= size) {
                    return mUploaderFilterList[index1 - 1]
                } else {
                    size + 1
                }
            }
            size = mTagFilterList.size
            if (0 != size) {
                index1 -= if (index1 == 0) {
                    return tagHeader
                } else if (index1 <= size) {
                    return mTagFilterList[index1 - 1]
                } else {
                    size + 1
                }
            }
            size = mTagNamespaceFilterList.size
            if (0 != size) {
                index1 -= if (index1 == 0) {
                    return tagNamespaceHeader
                } else if (index1 <= size) {
                    return mTagNamespaceFilterList[index1 - 1]
                } else {
                    size + 1
                }
            }
            size = mCommenterFilterList.size
            if (0 != size) {
                index1 -= if (index1 == 0) {
                    return commenterHeader
                } else if (index1 <= size) {
                    return mCommenterFilterList[index1 - 1]
                } else {
                    size + 1
                }
            }
            size = mCommentFilterList.size
            if (0 != size) {
                if (index1 == 0) {
                    return commentHeader
                } else if (index1 <= size) {
                    return mCommentFilterList[index1 - 1]
                }
            }
            throw IndexOutOfBoundsException()
        }

        fun add(filter: Filter): Boolean {
            return mEhFilter.addFilter(filter)
        }

        fun delete(filter: Filter) {
            mEhFilter.deleteFilter(filter)
        }

        fun trigger(filter: Filter?) {
            mEhFilter.triggerFilter(filter)
        }
    }

    companion object {
        private const val MODE_HEADER = -1
        private const val TYPE_ITEM = 0
        private const val TYPE_HEADER = 1
    }
}