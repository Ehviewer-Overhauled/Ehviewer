package com.hippo.ehviewer.ui.settings

import android.content.DialogInterface
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhFilter
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.databinding.DialogAddFilterBinding
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FilterScreen() {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scaffoldRecomposeScope = currentRecomposeScope
    class AddFilterDialogHelper(private val dialog: AlertDialog) : View.OnClickListener {
        private val mArray = dialog.context.resources.getStringArray(R.array.filter_entries)
        private val binding = DialogAddFilterBinding.bind(dialog.findViewById(R.id.base)!!)
        init { dialog.getButton(DialogInterface.BUTTON_POSITIVE)!!.setOnClickListener(this) }
        override fun onClick(v: View) {
            val emptyError = context.getString(R.string.text_is_empty)
            val text1: String?
            binding.spinner.run {
                text1 = editText?.text?.toString()
                if (text1.isNullOrBlank()) {
                    error = emptyError
                    return
                } else {
                    error = null
                }
            }
            val text: String?
            binding.textInputLayout.run {
                text = editText?.text?.toString()?.trim()
                if (text.isNullOrBlank()) {
                    error = emptyError
                    return
                } else {
                    error = null
                }
            }
            binding.textInputLayout.run {
                if (!EhFilter.addFilter(Filter(mArray.indexOf(text1), text))) {
                    error = context.getString(R.string.label_text_exist)
                    return
                } else {
                    error = null
                }
            }
            scaffoldRecomposeScope.invalidate()
            dialog.dismiss()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.filter)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { BaseDialogBuilder(context).setTitle(R.string.filter).setMessage(R.string.filter_tip).setPositiveButton(android.R.string.ok, null).show() }) {
                        Icon(imageVector = Icons.Default.Help, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { AddFilterDialogHelper(BaseDialogBuilder(context).setTitle(R.string.add_filter).setView(R.layout.dialog_add_filter).setPositiveButton(R.string.add, null).setNegativeButton(android.R.string.cancel, null).show()) }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) { paddingValues ->
        val lazyListRecomposeScope = currentRecomposeScope
        LazyColumn(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            fun filterItems(list: List<Filter>) = items(list) { filter ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val filterCheckBoxRecomposeScope = currentRecomposeScope
                    Checkbox(
                        checked = filter.enable ?: false,
                        onCheckedChange = {
                            coroutineScope.launch {
                                EhFilter.triggerFilter(filter)
                                filterCheckBoxRecomposeScope.invalidate()
                            }
                        },
                    )
                    Text(text = filter.text.orEmpty())
                    Spacer(modifier = Modifier.weight(1F))
                    IconButton(
                        onClick = {
                            BaseDialogBuilder(context).setMessage(context.getString(R.string.delete_filter, filter.text))
                                .setPositiveButton(R.string.delete) { _, which ->
                                    if (DialogInterface.BUTTON_POSITIVE == which) {
                                        coroutineScope.launch {
                                            EhFilter.deleteFilter(filter)
                                            lazyListRecomposeScope.invalidate()
                                        }
                                    }
                                }.setNegativeButton(android.R.string.cancel, null).show()
                        },
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    }
                }
            }
            fun header(@StringRes title: Int) = stickyHeader {
                Text(
                    text = stringResource(id = title),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            EhFilter.titleFilterList.takeIf { it.isNotEmpty() }?.let {
                header(R.string.filter_title)
                filterItems(it)
            }
            EhFilter.tagFilterList.takeIf { it.isNotEmpty() }?.let {
                header(R.string.filter_tag)
                filterItems(it)
            }
            EhFilter.commentFilterList.takeIf { it.isNotEmpty() }?.let {
                header(R.string.filter_comment)
                filterItems(it)
            }
            EhFilter.commenterFilterList.takeIf { it.isNotEmpty() }?.let {
                header(R.string.filter_commenter)
                filterItems(it)
            }
            EhFilter.uploaderFilterList.takeIf { it.isNotEmpty() }?.let {
                header(R.string.filter_uploader)
                filterItems(it)
            }
            EhFilter.tagNamespaceFilterList.takeIf { it.isNotEmpty() }?.let {
                header(R.string.filter_tag_namespace)
                filterItems(it)
            }
        }
    }
}
