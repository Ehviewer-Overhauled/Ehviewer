package com.hippo.ehviewer.ui.scene

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hippo.ehviewer.databinding.ItemGalleryPreviewFooterBinding

class FooterAdapter(private val onClick: () -> Unit) :
    RecyclerView.Adapter<FooterAdapter.ViewHolder>() {
    var text: String? = null
        set(value) {
            field = value
            notifyItemChanged(0)
        }

    inner class ViewHolder(val binding: ItemGalleryPreviewFooterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener { onClick() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemGalleryPreviewFooterBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.text.text = text
    }
}