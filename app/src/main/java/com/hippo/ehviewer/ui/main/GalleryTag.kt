package com.hippo.ehviewer.ui.main

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.data.GalleryTagGroup
import com.hippo.ehviewer.ui.tools.includeFontPadding

@Composable
fun GalleryTags(
    tagGroups: Array<GalleryTagGroup>,
    onTagClick: (String) -> Unit,
    onTagLongClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val canTranslate = Settings.showTagTranslations && EhTagDatabase.isTranslatable(context) && EhTagDatabase.initialized
    val ehTags = EhTagDatabase.takeIf { canTranslate }
    fun String.translate() = ehTags?.getTranslation(tag = this) ?: this
    fun String.translate(prefix: String?) = ehTags?.getTranslation(prefix = prefix, tag = this) ?: this
    Column(modifier) {
        tagGroups.forEach { tagGroup ->
            Row {
                BaseRoundText(
                    text = tagGroup.groupName.translate(),
                    isGroup = true,
                )
                val prefix = EhTagDatabase.namespaceToPrefix(tagGroup.groupName)
                FlowRow {
                    tagGroup.forEach {
                        val weak = it.startsWith('_')
                        val real = it.removePrefix("_")
                        val translated = real.translate(prefix)
                        val tag = tagGroup.groupName + ":" + real
                        val hapticFeedback = LocalHapticFeedback.current
                        BaseRoundText(
                            text = translated,
                            weak = weak,
                            modifier = Modifier.combinedClickable(
                                onClick = { onTagClick(tag) },
                                onLongClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTagLongClick(translated, tag)
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BaseRoundText(
    text: String,
    modifier: Modifier = Modifier,
    weak: Boolean = false,
    isGroup: Boolean = false,
) {
    val bgColor = if (isGroup) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    Surface(
        modifier = Modifier.padding(4.dp),
        color = bgColor,
        shape = GalleryTagCorner,
    ) {
        Text(
            text = text,
            modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.let { if (weak) it.copy(0.5F) else it },
            style = MaterialTheme.typography.labelLarge.includeFontPadding,
        )
    }
}

private val GalleryTagCorner = RoundedCornerShape(64.dp)
