package com.hippo.ehviewer.ui.scene

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.android.material.shape.MaterialShapeDrawable
import com.hippo.app.BaseDialogBuilder
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.widget.SearchDatabase
import com.hippo.scene.StageActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


abstract class SearchBarScene : ToolbarScene() {
    private var mSearchView: SearchView? = null
    private var mRecyclerView: RecyclerView? = null
    private var mSuggestionList: List<Suggestion>? = null
    private var mSuggestionAdapter: SuggestionAdapter? = null
    private var mSuggestionProvider: SuggestionProvider? = null
    private var mAllowEmptySearch = true
    private val mSearchDatabase by lazy { SearchDatabase.getInstance(context) }
    private var onApplySearch: (String) -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.scene_searchbar, container, false) as ViewGroup
        mToolbar = view.findViewById(R.id.toolbar)
        mAppBarLayout = view.findViewById(R.id.appbar)
        mAppBarLayout?.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(context)

        mSearchView = view.findViewById(R.id.searchview)
        mRecyclerView = view.findViewById(R.id.search_bar_list)
        mSearchView?.editText?.addTextChangedListener {
            updateSuggestions()
        }
        mSearchView?.editText?.setOnEditorActionListener { _, _, _ ->
            (mToolbar as SearchBar).text = mSearchView!!.text
            mSearchView?.hide()
            onApplySearch()
            true
        }
        mSuggestionList = ArrayList()
        mSuggestionAdapter = SuggestionAdapter(LayoutInflater.from(context))
        mRecyclerView?.adapter = mSuggestionAdapter
        val layoutManager = LinearLayoutManager(context)
        mRecyclerView?.layoutManager = layoutManager
        mSearchView?.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.SHOWING)
                onSearchViewExpanded()
            else if (newState == SearchView.TransitionState.HIDING)
                onSearchViewHidden()
        }
        val contentView = onCreateViewWithToolbar(inflater, view, savedInstanceState)
        return view.apply { addView(contentView, 0) }
    }

    @CallSuper
    open fun onSearchViewExpanded() {
        (requireActivity() as StageActivity).updateBackPressCallBackStatus()
        updateSuggestions()
    }

    @CallSuper
    open fun onSearchViewHidden() {
        (requireActivity() as StageActivity).updateBackPressCallBackStatus()
    }

    fun setSearchBarHint(hint: String?) {
        (mToolbar as? SearchBar)?.hint = hint
    }

    fun setSearchBarText(text: String?) {
        (mToolbar as? SearchBar)?.text = text
    }

    fun setEditTextHint(hint: String?) {
        mSearchView?.editText?.hint = hint
    }

    fun setEditTextText(text: String?) {
        mSearchView?.setText(text)
    }

    fun setNavigationDrawable(drawable: Drawable?) {
        (mToolbar as? SearchBar)?.navigationIcon = drawable
    }

    override fun onNavigationClick() {
        toggleDrawer(Gravity.START)
    }

    fun setOnApplySearch(lambda: (String) -> Unit) {
        onApplySearch = lambda
    }

    fun onApplySearch() {
        val query = mSearchView?.text.toString().trim()
        if (!mAllowEmptySearch && query.isEmpty()) {
            return
        }
        mSearchDatabase.addQuery(query)
        onApplySearch(query)
    }

    fun setAllowEmptySearch(allowEmptySearch: Boolean) {
        mAllowEmptySearch = allowEmptySearch
    }

    fun isSearchViewShown(): Boolean {
        return mSearchView?.isShowing ?: false
    }

    fun hideSearchView() {
        mSearchView?.hide()
    }

    private fun wrapTagKeyword(keyword: String): String {
        var keyword = keyword
        keyword = keyword.trim { it <= ' ' }

        val index1 = keyword.indexOf(':')
        if (index1 == -1 || index1 >= keyword.length - 1) {
            // Can't find :, or : is the last char
            return keyword
        }
        if (keyword[index1 + 1] == '"') {
            // The char after : is ", the word must be quoted
            return keyword
        }
        val index2 = keyword.indexOf(' ')
        return if (index2 <= index1) {
            // Can't find space, or space is before :
            keyword
        } else keyword.substring(0, index1 + 1) + "\"" + keyword.substring(index1 + 1) + "$\""

    }

    interface SuggestionProvider {

        fun providerSuggestions(text: String): List<Suggestion>?
    }

    abstract class Suggestion {

        abstract fun getText(textView: TextView): CharSequence?

        abstract fun onClick()

        open fun onLongClick(): Boolean {
            return false
        }

    }

    private class SuggestionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text1 = itemView.findViewById(android.R.id.text1) as TextView
        val text2 = itemView.findViewById(android.R.id.text2) as TextView
    }

    private inner class SuggestionAdapter constructor(private val mInflater: LayoutInflater) :
        RecyclerView.Adapter<SuggestionHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionHolder {
            return SuggestionHolder(mInflater.inflate(R.layout.item_simple_list_2, parent, false))
        }

        override fun onBindViewHolder(holder: SuggestionHolder, position: Int) {
            val suggestion = mSuggestionList?.get(position)
            val text1 = suggestion?.getText(holder.text1)
            val text2 = suggestion?.getText(holder.text2)
            holder.text1.text = text1
            if (text2 == null) {
                holder.text2.visibility = View.GONE
                holder.text2.text = ""
            } else {
                holder.text2.visibility = View.VISIBLE
                holder.text2.text = text2
            }

            holder.itemView.setOnClickListener {
                mSuggestionList?.run {
                    if (position < size) {
                        this[position].onClick()
                    }
                }
            }
            holder.itemView.setOnLongClickListener {
                mSuggestionList?.run {
                    if (position < size) {
                        return@setOnLongClickListener this[position].onLongClick()
                    }
                }
                return@setOnLongClickListener false
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItemCount(): Int {
            return mSuggestionList?.size ?: 0
        }

    }

    inner class TagSuggestion constructor(
        private var mHint: String?,
        private var mKeyword: String
    ) :
        Suggestion() {

        override fun getText(textView: TextView): CharSequence? {
            return if (textView.id == android.R.id.text1) {
                mKeyword
            } else {
                mHint
            }
        }

        override fun onClick() {
            val edittext = mSearchView?.editText
            edittext?.let {
                val text = it.text.toString()
                var temp = wrapTagKeyword(mKeyword) + " "
                if (text.contains(" ")) {
                    temp = text.substring(0, text.lastIndexOf(" ")) + " " + temp
                }
                it.setText(temp)
                it.setSelection(text.length)
            }
        }
    }

    inner class KeywordSuggestion constructor(private val mKeyword: String) : Suggestion() {

        override fun getText(textView: TextView): CharSequence? {
            return if (textView.id == android.R.id.text1) {
                mKeyword
            } else {
                null
            }
        }

        override fun onClick() {
            mSearchView?.editText?.run {
                setText(mKeyword)
                setSelection(length())
            }
        }

        override fun onLongClick(): Boolean {
            BaseDialogBuilder(requireContext())
                .setMessage(requireContext().getString(R.string.delete_search_history, mKeyword))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    mSearchDatabase.deleteQuery(mKeyword)
                    updateSuggestions(false)
                }
                .show()
            return true
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateSuggestions(scrollToTop: Boolean = true) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            val suggestions = mutableListOf<Suggestion>()
            mergedSuggestionFlow().collect {
                suggestions.add(it)
            }
            withContext(Dispatchers.Main) {
                mSuggestionList = suggestions
                mSuggestionAdapter?.notifyDataSetChanged()
            }
        }
        if (scrollToTop) {
            mRecyclerView?.scrollToPosition(0)
        }
    }

    private fun mergedSuggestionFlow(): Flow<Suggestion> = flow {
        mSearchView?.editText?.text?.toString()?.let { text ->
            mSuggestionProvider?.run { providerSuggestions(text)?.forEach { emit(it) } }
            mSearchDatabase.getSuggestions(text, 128).forEach { emit(KeywordSuggestion(it)) }
            EhTagDatabase.instance?.run {
                if (!TextUtils.isEmpty(text) && !text.endsWith(" ")) {
                    val s = text.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (s.isNotEmpty()) {
                        val keyword = s[s.size - 1]
                        val translate =
                            Settings.getShowTagTranslations() && EhTagDatabase.isTranslatable(
                                requireContext()
                            )
                        suggestFlow(keyword, translate).collect {
                            emit(TagSuggestion(it.first, it.second))
                        }
                    }
                }
            }
        }
    }

    fun setSuggestionProvider(suggestionProvider: SuggestionProvider) {
        mSuggestionProvider = suggestionProvider
    }

    fun showSearchBar() {
        mAppBarLayout?.setExpanded(true)
    }
}
