package com.hippo.ehviewer.ui.scene

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

abstract class BaseListAdapter<T>(diffCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, BaseViewHolder<T>>(diffCallback) {
    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}