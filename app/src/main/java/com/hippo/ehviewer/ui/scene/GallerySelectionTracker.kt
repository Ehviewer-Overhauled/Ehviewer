package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import com.hippo.ehviewer.client.data.GalleryInfo

class GallerySelectionTracker<T : GalleryInfo>(
    selectionId: String,
    recyclerView: RecyclerView,
    private val getItems: () -> List<T>,
    getItemKey: RecyclerView.ViewHolder.() -> Long,
    builder: SelectionTracker.Builder<Long>.() -> Unit = {},
) {
    private val tracker: SelectionTracker<Long>
    val selectionSize get() = tracker.selection.size()
    var isInCustomChoice = false
        private set

    init {
        tracker = SelectionTracker.Builder(
            selectionId,
            recyclerView,
            object : ItemKeyProvider<Long>(SCOPE_MAPPED) {
                override fun getKey(position: Int) = getItems()[position].gid
                override fun getPosition(key: Long) = getItems().indexOfFirst { it.gid == key }
            },
            object : ItemDetailsLookup<Long>() {
                override fun getItemDetails(e: MotionEvent) =
                    recyclerView.findChildViewUnder(e.x, e.y)?.let {
                        recyclerView.getChildViewHolder(it).run {
                            object : ItemDetails<Long>() {
                                override fun getPosition() = bindingAdapterPosition
                                override fun getSelectionKey() = getItemKey()
                            }
                        }
                    }
            },
            StorageStrategy.createLongStorage(),
        ).apply(builder).build()
    }

    fun addCustomChoiceListener(onIntoCustomChoiceListener: () -> Unit, onOutOfCustomChoiceListener: () -> Unit) {
        tracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                if (tracker.selection.isEmpty) {
                    if (isInCustomChoice) {
                        isInCustomChoice = false
                        onOutOfCustomChoiceListener()
                    }
                } else if (!isInCustomChoice) {
                    isInCustomChoice = true
                    onIntoCustomChoiceListener()
                }
            }

            override fun onSelectionRestored() {
                if (!tracker.selection.isEmpty) {
                    isInCustomChoice = true
                    onIntoCustomChoiceListener()
                }
            }
        })
    }

    fun isSelected(key: Long) = tracker.isSelected(key)
    fun selectAll() = tracker.setItemsSelected(getItems().map(GalleryInfo::gid), true)
    fun clearSelection() = tracker.clearSelection()
    fun saveSelection(state: Bundle) = tracker.onSaveInstanceState(state)
    fun restoreSelection(state: Bundle?) = tracker.onRestoreInstanceState(state)
    fun getAndClearSelection() = getItems().run { tracker.selection.map { gid -> first { it.gid == gid } } }
        .also { clearSelection() }
}
