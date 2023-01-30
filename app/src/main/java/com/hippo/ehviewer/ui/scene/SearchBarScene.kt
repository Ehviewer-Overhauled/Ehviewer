package com.hippo.ehviewer.ui.scene

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchView
import com.google.android.material.search.SearchView.TransitionListener
import com.google.android.material.shape.MaterialShapeDrawable
import com.hippo.app.BaseDialogBuilder
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.databinding.SceneSearchbarBinding
import com.hippo.ehviewer.widget.SearchDatabase
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch


abstract class SearchBarScene : BaseScene(), ToolBarScene {
    private var _binding: SceneSearchbarBinding? = null
    private val binding get() = _binding!!
    private var mSuggestionList: List<Suggestion>? = null
    private var mSuggestionAdapter: SuggestionAdapter? = null
    private var mSuggestionProvider: SuggestionProvider? = null
    private var mAllowEmptySearch = true
    private val mSearchDatabase by lazy { SearchDatabase.getInstance(context) }
    private var onApplySearch: (String) -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SceneSearchbarBinding.inflate(inflater, container, false)
        binding.appbar.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(context)
        binding.searchview.editText.addTextChangedListener {
            updateSuggestions()
        }
        binding.searchview.editText.setOnEditorActionListener { _, _, _ ->
            onApplySearch()
            true
        }
        mSuggestionList = ArrayList()
        mSuggestionAdapter = SuggestionAdapter(LayoutInflater.from(context))
        binding.searchBarList.adapter = mSuggestionAdapter
        val layoutManager = LinearLayoutManager(context)
        binding.searchBarList.layoutManager = layoutManager
        binding.searchview.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.SHOWING)
                onSearchViewExpanded()
            else if (newState == SearchView.TransitionState.HIDING)
                onSearchViewHidden()
        }
        binding.searchview.addTransitionListener(mSearchViewOnBackPressedCallback)
        val contentView = onCreateViewWithToolbar(inflater, binding.root, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(mSearchViewOnBackPressedCallback)
        return binding.root.apply { addView(contentView, 0) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.apply {
            val menuResId = getMenuResId()
            if (menuResId != 0) {
                inflateMenu(menuResId)
                setOnMenuItemClickListener { item: MenuItem -> onMenuItemClick(item) }
            }
            setNavigationOnClickListener { onNavigationClick() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mSearchViewOnBackPressedCallback.remove()
        _binding = null
    }

    private var privLockModeStart: Int? = null
    private var privLockModeEnd: Int? = null

    @CallSuper
    open fun onSearchViewExpanded() {
        privLockModeStart = getDrawerLockMode(GravityCompat.START)
        privLockModeEnd = getDrawerLockMode(GravityCompat.END)
        privLockModeStart?.let {
            setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.START
            )
        }
        privLockModeEnd?.let {
            setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END
            )
        }
        updateSuggestions()
    }

    @CallSuper
    open fun onSearchViewHidden() {
        binding.toolbar.text = binding.searchview.text
        privLockModeStart?.let { setDrawerLockMode(it, GravityCompat.START) }
        privLockModeStart = null
        privLockModeEnd?.let { setDrawerLockMode(it, GravityCompat.END) }
        privLockModeEnd = null
    }

    fun setSearchBarHint(hint: String?) {
        binding.toolbar.hint = hint
    }

    fun setSearchBarText(text: String?) {
        binding.toolbar.text = text
    }

    fun setEditTextHint(hint: String?) {
        binding.searchview.editText.hint = hint
    }

    override fun onNavigationClick() {
        toggleDrawer(Gravity.START)
    }

    override fun getMenuResId(): Int {
        return 0
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return false
    }

    override fun setLiftOnScrollTargetView(view: View?) {
        binding.appbar.setLiftOnScrollTargetView(view)
    }

    fun setOnApplySearch(lambda: (String) -> Unit) {
        onApplySearch = lambda
    }

    fun onApplySearch() {
        binding.toolbar.text = binding.searchview.text
        binding.searchview.hide()
        val query = binding.toolbar.text.toString().trim()
        if (!mAllowEmptySearch && query.isEmpty()) {
            return
        }
        mSearchDatabase.addQuery(query)
        onApplySearch(query)
    }

    fun setAllowEmptySearch(allowEmptySearch: Boolean) {
        mAllowEmptySearch = allowEmptySearch
    }

    private fun wrapTagKeyword(keyword: String): String {
        return if (keyword.endsWith(':')) {
            keyword
        } else if (keyword.contains(' ')) {
            val tag = keyword.substringAfter(':')
            val prefix = keyword.dropLast(tag.length)
            "$prefix\"$tag$\" "
        } else {
            "$keyword$ "
        }
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
            val edittext = binding.searchview.editText
            edittext.let {
                val keywords = it.text.toString().substringBeforeLast(' ', "")
                val keyword = wrapTagKeyword(mKeyword)
                val newKeywords = if (keywords.isNotEmpty()) "$keywords $keyword" else keyword
                it.setText(newKeywords)
                it.setSelection(newKeywords.length)
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
            binding.searchview.editText.run {
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
        _binding ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val suggestions = mutableListOf<Suggestion>()
            mergedSuggestionFlow().collect {
                suggestions.add(it)
            }
            withUIContext {
                mSuggestionList = suggestions
                mSuggestionAdapter?.notifyDataSetChanged()
            }
        }
        if (scrollToTop) {
            binding.searchBarList.scrollToPosition(0)
        }
    }

    private fun mergedSuggestionFlow(): Flow<Suggestion> = flow {
        binding.searchview.editText.text?.toString()?.let { text ->
            mSuggestionProvider?.run { providerSuggestions(text)?.forEach { emit(it) } }
            mSearchDatabase.getSuggestions(text, 128).forEach { emit(KeywordSuggestion(it)) }
            EhTagDatabase.takeIf { it.isInitialized() }?.run {
                if (text.isNotEmpty() && !text.endsWith(' ')) {
                    val keyword = text.substringAfterLast(' ')
                    val translate =
                        Settings.getShowTagTranslations() && isTranslatable(requireContext())
                    suggestFlow(keyword, translate, true).collect {
                        emit(TagSuggestion(it.first, it.second))
                    }
                    suggestFlow(keyword, translate).collect {
                        emit(TagSuggestion(it.first, it.second))
                    }
                }
            }
        }
    }

    fun setSuggestionProvider(suggestionProvider: SuggestionProvider) {
        mSuggestionProvider = suggestionProvider
    }

    fun showSearchBar() {
        _binding ?: return
        binding.appbar.setExpanded(true)
    }

    private val mSearchViewOnBackPressedCallback =
        object : OnBackPressedCallback(false), TransitionListener {
            override fun handleOnBackPressed() {
                binding.searchview.hide()
            }

            override fun onStateChanged(
                searchView: SearchView,
                previousState: SearchView.TransitionState,
                newState: SearchView.TransitionState
            ) {
                if (newState == SearchView.TransitionState.SHOWING)
                    isEnabled = true
                else if (newState == SearchView.TransitionState.HIDING)
                    isEnabled = false
            }
        }
}
