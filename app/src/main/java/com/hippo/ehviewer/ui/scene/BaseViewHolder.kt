package com.hippo.ehviewer.ui.scene

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseViewHolder<T>(binding: ViewBinding, onClick: (T) -> Unit) :
    RecyclerView.ViewHolder(binding.root) {
    protected var currentItem: T? = null

    init {
        itemView.setOnClickListener {
            currentItem?.let { onClick(it) }
        }
    }

    abstract fun bind(item: T)
}