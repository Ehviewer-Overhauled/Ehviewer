package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.R

@Composable
fun SearchAdvanced(
    state: AdvancedSearchOption,
    onStateChanged: (AdvancedSearchOption) -> Unit,
) {
    Column {
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            val adv = state.advanceSearch
            fun checked(bit: Int) = adv and bit != 0
            fun AdvancedSearchOption.inv(checked: Boolean, bit: Int) = onStateChanged(copy(advanceSearch = if (!checked) advanceSearch xor bit else advanceSearch or bit))
            Row {
                Row(modifier = Modifier.weight(1f)) {
                    Checkbox(checked = checked(AdvanceTable.SH), onCheckedChange = { state.inv(it, AdvanceTable.SH) })
                    Text(text = stringResource(id = R.string.search_sh), modifier = Modifier.align(Alignment.CenterVertically))
                }
                Row(modifier = Modifier.weight(1f)) {
                    Checkbox(checked = checked(AdvanceTable.STO), onCheckedChange = { state.inv(it, AdvanceTable.STO) })
                    Text(text = stringResource(id = R.string.search_sto), modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }
    }
}
