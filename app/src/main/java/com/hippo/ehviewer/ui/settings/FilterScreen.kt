package com.hippo.ehviewer.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhFilter
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder

@Composable
fun FilterScreen() {
    val navController = LocalNavController.current
    val context = LocalContext.current
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
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            @Composable
            fun FilterItem(filter: Filter) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = filter.enable ?: false, onCheckedChange = { })
                    Text(text = filter.text.orEmpty())
                    Spacer(modifier = Modifier.weight(1F))
                    IconButton(onClick = { }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    }
                }
            }
            fun filterItems(list: List<Filter>) = items(list) { FilterItem(filter = it) }
            stickyHeader {
                Text(text = stringResource(id = R.string.filter_title))
            }
            filterItems(EhFilter.titleFilterList)
            stickyHeader {
                Text(text = stringResource(id = R.string.filter_tag))
            }
            filterItems(EhFilter.tagFilterList)
            stickyHeader {
                Text(text = stringResource(id = R.string.filter_comment))
            }
            filterItems(EhFilter.commentFilterList)
            stickyHeader {
                Text(text = stringResource(id = R.string.filter_commenter))
            }
            filterItems(EhFilter.commenterFilterList)
            stickyHeader {
                Text(text = stringResource(id = R.string.filter_uploader))
            }
            filterItems(EhFilter.uploaderFilterList)
            stickyHeader {
                Text(text = stringResource(id = R.string.filter_tag_namespace))
            }
            filterItems(EhFilter.tagNamespaceFilterList)
        }
    }
}
