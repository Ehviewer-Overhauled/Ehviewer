package com.hippo.ehviewer.ui.scene

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import arrow.core.partially1
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.ui.widget.CrystalCard
import com.hippo.ehviewer.ui.widget.EhAsyncPreview
import com.hippo.ehviewer.ui.widget.setMD3Content

class GalleryPreviewsAdapter(private val onClick: (GalleryPreview) -> Unit) : ListAdapter<GalleryPreview, GalleryPreviewsAdapter.ComposeHolder>(PreviewDiffCallback) {
    class ComposeHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ComposeHolder {
        return ComposeHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: ComposeHolder, position: Int) {
        val it = getItem(position)
        holder.composeView.setMD3Content {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CrystalCard(
                        onClick = onClick.partially1(it),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.6666667F),
                    ) {
                        EhAsyncPreview(
                            model = it,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Text((it.position + 1).toString())
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).position.toLong()
    }
}

object PreviewDiffCallback : DiffUtil.ItemCallback<GalleryPreview>() {
    override fun areItemsTheSame(oldItem: GalleryPreview, newItem: GalleryPreview): Boolean {
        return oldItem.position == newItem.position
    }

    override fun areContentsTheSame(oldItem: GalleryPreview, newItem: GalleryPreview): Boolean {
        return oldItem == newItem
    }
}
