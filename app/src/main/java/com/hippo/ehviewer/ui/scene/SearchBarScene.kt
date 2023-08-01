package com.hippo.ehviewer.ui.scene

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.search.SearchView
import com.google.android.material.shape.MaterialShapeDrawable
import com.hippo.ehviewer.EhApplication.Companion.searchDatabase
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.dao.Search
import com.hippo.ehviewer.dao.SearchDao
import com.hippo.ehviewer.databinding.SceneSearchbarBinding
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.setMD3Content
import com.jamal.composeprefs3.ui.ifNotNullThen
import com.jamal.composeprefs3.ui.ifTrueThen
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

abstract class SearchBarScene : BaseScene(), ToolBarScene {
    private var _binding: SceneSearchbarBinding? = null
    private val binding get() = _binding!!
    private var mSuggestionList by mutableStateOf(emptyList<Suggestion>())
    private var mSuggestionProvider: SuggestionProvider? = null
    var allowEmptySearch = true
    private val mSearchDatabase = searchDatabase.searchDao()
    private var onApplySearch: (String) -> Unit = {}
    protected val mSearchFab get() = binding.searchFab

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SceneSearchbarBinding.inflate(inflater, container, false)
        binding.appbar.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(context)
        binding.searchview.editText.addTextChangedListener { updateSuggestions() }
        binding.searchview.editText.setOnEditorActionListener { _, _, _ ->
            onApplySearch()
            true
        }
        binding.searchBarList.setMD3Content {
            LazyColumn {
                items(mSuggestionList) {
                    ListItem(
                        headlineContent = { Text(text = it.keyword) },
                        supportingContent = it.hint.ifNotNullThen { Text(text = it.hint!!) },
                        leadingContent = it.canOpenDirectly.ifTrueThen {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                            )
                        },
                        trailingContent = it.canDelete.ifTrueThen {
                            IconButton(onClick = { deleteKeyword(it.keyword) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { it.onClick() },
                    )
                }
            }
        }
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

    private suspend fun addQuery(query: String) {
        mSearchDatabase.deleteQuery(query)
        if (query.isBlank()) return
        val search = Search(System.currentTimeMillis(), query)
        mSearchDatabase.insert(search)
    }

    fun onApplySearch() {
        binding.toolbar.setText(binding.searchview.text)
        binding.searchview.hide()
        val query = binding.toolbar.text.toString().trim()
        if (!allowEmptySearch && query.isEmpty()) return
        lifecycleScope.launchIO { addQuery(query) }
        onApplySearch(query)
    }

    fun interface SuggestionProvider {
        fun providerSuggestions(text: String): Suggestion?
    }

    private fun deleteKeyword(keyword: String) {
        BaseDialogBuilder(requireContext())
            .setMessage(requireContext().getString(R.string.delete_search_history, keyword))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launchIO {
                    mSearchDatabase.deleteQuery(keyword)
                    updateSuggestions()
                }
            }
            .show()
    }

    abstract class Suggestion {
        abstract val keyword: String
        open val hint: String? = null
        abstract fun onClick()
        open val canDelete: Boolean = false
        open val canOpenDirectly: Boolean = false
    }

    inner class TagSuggestion(
        override val hint: String?,
        override val keyword: String,
    ) : Suggestion() {
        override fun onClick() {
            binding.searchview.editText.let {
                var keywords = it.text.toString().substringBeforeLast(' ', "")
                if (keywords.isNotEmpty()) keywords += ' '
                keywords += wrapTagKeyword(keyword)
                if (!keywords.endsWith(':')) keywords += ' '
                it.setText(keywords)
                it.setSelection(keywords.length)
            }
        }
    }

    inner class KeywordSuggestion(
        override val keyword: String,
    ) : Suggestion() {
        override val canDelete = true
        override fun onClick() {
            binding.searchview.editText.run {
                setText(keyword)
                setSelection(length())
            }
        }
    }

    private fun updateSuggestions() {
        _binding ?: return
        viewLifecycleOwner.lifecycleScope.launchIO {
            mSuggestionList = mergedSuggestionFlow().toList()
        }
    }

    private suspend fun SearchDao.suggestions(prefix: String, limit: Int) = (if (prefix.isBlank()) list(limit) else rawSuggestions(prefix, limit)).map { it.query }

    private fun mergedSuggestionFlow(): Flow<Suggestion> = flow {
        binding.searchview.editText.text?.toString()?.let { text ->
            mSuggestionProvider?.run { providerSuggestions(text)?.let { emit(it) } }
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
