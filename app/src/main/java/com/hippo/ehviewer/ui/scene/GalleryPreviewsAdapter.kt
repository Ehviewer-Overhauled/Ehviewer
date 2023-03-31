package com.hippo.ehviewer.ui.scene

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.databinding.ItemGalleryPreviewBinding

class GalleryPreviewsAdapter(private val onClick: (GalleryPreview) -> Unit) :
    BaseListAdapter<GalleryPreview>(PreviewDiffCallback) {
    class GalleryPreviewHolder(
        private val binding: ItemGalleryPreviewBinding,
        onClick: (GalleryPreview) -> Unit,
    ) : BaseViewHolder<GalleryPreview>(binding, onClick) {
        @SuppressLint("SetTextI18n")
        override fun bind(item: GalleryPreview) {
            currentItem = item
            item.load(binding.image)
            binding.text.text = (item.position + 1).toString()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BaseViewHolder<GalleryPreview> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemGalleryPreviewBinding.inflate(inflater, parent, false)
        return GalleryPreviewHolder(binding, onClick)
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
