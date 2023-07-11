package com.hippo.ehviewer.ui.scene

import android.annotation.SuppressLint
import android.os.Build
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
import com.google.android.material.shape.MaterialShapeDrawable
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.dao.SearchDatabase
import com.hippo.ehviewer.databinding.ItemSimpleList2Binding
import com.hippo.ehviewer.databinding.SceneSearchbarBinding
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import splitties.arch.room.roomDb

private val searchDatabase by lazy { roomDb<SearchDatabase>("search_database.db") }

abstract class SearchBarScene : BaseScene(), ToolBarScene {
    private var _binding: SceneSearchbarBinding? = null
    private val binding get() = _binding!!
    private var mSuggestionList: List<Suggestion>? = null
    private var mSuggestionAdapter: SuggestionAdapter? = null
    private var mSuggestionProvider: SuggestionProvider? = null
    private var mAllowEmptySearch = true
    private val mSearchDatabase = searchDatabase.searchDao()
    private var onApplySearch: (String) -> Unit = {}
    protected val mSearchFab get() = binding.searchFab

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
            if (newState == SearchView.TransitionState.SHOWING) {
                onSearchViewExpanded()
            } else if (newState == SearchView.TransitionState.HIDING) {
                onSearchViewHidden()
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            binding.searchview.addTransitionListener(mSearchViewOnBackPressedCallback)
            requireActivity().onBackPressedDispatcher.addCallback(mSearchViewOnBackPressedCallback)
        }
        onCreateViewWithToolbar(inflater, binding.root, savedInstanceState)
        binding.appbar.bringToFront()
        return binding.root
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) mSearchViewOnBackPressedCallback.remove()
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
                GravityCompat.START,
            )
        }
        privLockModeEnd?.let {
            setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END,
            )
        }
        updateSuggestions()
    }

    @CallSuper
    open fun onSearchViewHidden() {
        binding.toolbar.setText(binding.searchview.text)
        privLockModeStart?.let { setDrawerLockMode(it, GravityCompat.START) }
        privLockModeStart = null
        privLockModeEnd?.let { setDrawerLockMode(it, GravityCompat.END) }
        privLockModeEnd = null
    }

    fun setSearchBarHint(hint: String?) {
        binding.toolbar.hint = hint
    }

    fun setSearchBarText(text: String?) {
        binding.toolbar.setText(text)
        binding.searchview.setText(text)
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
        binding.toolbar.setText(binding.searchview.text)
        binding.searchview.hide()
        val query = binding.toolbar.text.toString().trim()
        if (!mAllowEmptySearch && query.isEmpty()) {
            return
        }
        lifecycleScope.launchIO { mSearchDatabase.addQuery(query) }
        onApplySearch(query)
    }

    fun setAllowEmptySearch(allowEmptySearch: Boolean) {
        mAllowEmptySearch = allowEmptySearch
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

    private class SuggestionHolder(private val binding: ItemSimpleList2Binding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(suggestion: Suggestion?) {
            val text1 = suggestion?.getText(binding.text1)
            val text2 = suggestion?.getText(binding.text2)
            binding.text1.text = text1
            if (text2 == null) {
                binding.text2.visibility = View.GONE
                binding.text2.text = ""
            } else {
                binding.text2.visibility = View.VISIBLE
                binding.text2.text = text2
            }
        }
    }

    private inner class SuggestionAdapter(private val mInflater: LayoutInflater) :
        RecyclerView.Adapter<SuggestionHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionHolder {
            return SuggestionHolder(ItemSimpleList2Binding.inflate(mInflater, parent, false))
        }

        override fun onBindViewHolder(holder: SuggestionHolder, position: Int) {
            val suggestion = mSuggestionList?.get(position)
            holder.bind(suggestion)

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

    inner class TagSuggestion(
        private var mHint: String?,
        private var mKeyword: String,
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
                var keywords = it.text.toString().substringBeforeLast(' ', "")
                if (keywords.isNotEmpty()) keywords += ' '
                keywords += wrapTagKeyword(mKeyword)
                if (!keywords.endsWith(':')) keywords += ' '
                it.setText(keywords)
                it.setSelection(keywords.length)
            }
        }
    }

    inner class KeywordSuggestion(private val mKeyword: String) : Suggestion() {

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
                    lifecycleScope.launchIO {
                        mSearchDatabase.deleteQuery(mKeyword)
                        withUIContext {
                            updateSuggestions(false)
                        }
                    }
                }
                .show()
            return true
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateSuggestions(scrollToTop: Boolean = true) {
        _binding ?: return
        viewLifecycleOwner.lifecycleScope.launchIO {
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
            mSearchDatabase.suggestions(text, 128).forEach { emit(KeywordSuggestion(it)) }
            EhTagDatabase.takeIf { it.initialized }?.run {
                if (text.isNotEmpty() && !text.endsWith(' ')) {
                    val keyword = text.substringAfterLast(' ')
                    val translate = Settings.showTagTranslations && isTranslatable(requireContext())
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
        object : OnBackPressedCallback(false), SearchView.TransitionListener {
            override fun handleOnBackPressed() {
                binding.searchview.hide()
            }

            override fun onStateChanged(
                searchView: SearchView,
                previousState: SearchView.TransitionState,
                newState: SearchView.TransitionState,
            ) {
                if (newState == SearchView.TransitionState.SHOWING) {
                    isEnabled = true
                } else if (newState == SearchView.TransitionState.HIDING) {
                    isEnabled = false
                }
            }
        }
}

fun wrapTagKeyword(keyword: String, translate: Boolean = false): String {
    return if (keyword.endsWith(':')) {
        keyword
    } else {
        val tag = keyword.substringAfter(':')
        val prefix = keyword.dropLast(tag.length + 1)
        if (translate) {
            val namespacePrefix = EhTagDatabase.namespaceToPrefix(prefix)
            val newPrefix = EhTagDatabase.getTranslation(tag = prefix) ?: prefix
            val newTag = EhTagDatabase.getTranslation(namespacePrefix, tag) ?: tag
            "$newPrefix：$newTag"
        } else if (keyword.contains(' ')) {
            "$prefix:\"$tag$\""
        } else {
            "$keyword$"
        }
    }
}
