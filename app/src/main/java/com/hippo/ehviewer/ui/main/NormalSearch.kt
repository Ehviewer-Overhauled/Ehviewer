package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhUtils

private val categoryTable = arrayOf(
    EhUtils.DOUJINSHI to R.string.doujinshi,
    EhUtils.MANGA to R.string.manga,
    EhUtils.ARTIST_CG to R.string.artist_cg,
    EhUtils.GAME_CG to R.string.game_cg,
    EhUtils.WESTERN to R.string.western,
    EhUtils.NON_H to R.string.non_h,
    EhUtils.IMAGE_SET to R.string.image_set,
    EhUtils.COSPLAY to R.string.cosplay,
    EhUtils.ASIAN_PORN to R.string.asian_porn,
    EhUtils.MISC to R.string.misc,
)

private val modeTable = arrayOf(
    1 to R.string.search_normal_search,
    2 to R.string.search_subscription_search,
    3 to R.string.search_specify_uploader,
    4 to R.string.search_specify_tag,
)

@Composable
fun NormalSearch(
    category: Int,
    onCategoryChanged: (Int) -> Unit,
    searchMode: Int,
    onSearchModeChanged: (Int) -> Unit,
    isAdvanced: Boolean,
    onAdvancedChanged: (Boolean) -> Unit,
    showInfo: () -> Unit,
) {
    FlowRow(modifier = Modifier.wrapContentHeight(align = Alignment.Top)) {
        categoryTable.forEach {
            val selected = category and it.first != 0
            FilterChip(
                selected = selected,
                onClick = { onCategoryChanged(if (selected) category xor it.first else category or it.first) },
                label = { Text(text = stringResource(id = it.second)) },
                leadingIcon = { if (selected) Icon(imageVector = Icons.Default.Check, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 4.dp).align(alignment = Alignment.CenterVertically),
            )
        }
    }
    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
        FlowRow {
            modeTable.forEach {
                Row(
                    modifier = Modifier.weight(1F).width(112.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = (it.first == searchMode),
                        onClick = { onSearchModeChanged(it.first) },
                    )
                    Text(
                        text = stringResource(id = it.second),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.Start) {
            Box(modifier = Modifier.weight(1f)) {
                IconButton(onClick = showInfo) {
                    Icon(imageVector = Icons.Default.Help, contentDescription = null)
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                Checkbox(checked = isAdvanced, onCheckedChange = onAdvancedChanged)
                Text(text = stringResource(id = R.string.search_enable_advance), modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}
