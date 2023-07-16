package com.hippo.ehviewer.ui.legacy

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import kotlinx.parcelize.Parcelize

class SelectableGalleryInfoRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {
    private var customChoiceListener: CustomChoiceListener? = null
    private var mOutOfCustomChoiceModing = false
    var isInCustomChoice = false
        private set
    val checkedItemCount
        get() = checkedGid.size
    lateinit var gidGetter: (Int) -> Long?
    private var checkedGid = mutableSetOf<Long>()

    private fun checkChild(child: View) {
        val position = getChildAdapterPosition(child).takeIf { it != NO_POSITION } ?: return
        val gid = gidGetter(position) ?: return
        setViewChecked(child, gid in checkedGid)
    }

    override fun onChildAttachedToWindow(child: View) {
        super.onChildAttachedToWindow(child)
        checkChild(child)
    }

    val checkedItemGid: Set<Long>
        get() = checkedGid

    fun intoCustomChoiceMode() {
        if (!isInCustomChoice) {
            isInCustomChoice = true
            customChoiceListener?.onIntoCustomChoice(this)
        }
    }

    fun outOfCustomChoiceMode() {
        if (isInCustomChoice && !mOutOfCustomChoiceModing) {
            mOutOfCustomChoiceModing = true
            clearChoices()
            isInCustomChoice = false
            customChoiceListener?.onOutOfCustomChoice(this)
            mOutOfCustomChoiceModing = false
        }
    }

    private fun clearChoices() {
        checkedGid.clear()
        updateOnScreenCheckedViews()
    }

    fun checkAll() {
        check(isInCustomChoice) { "Call intoCheckMode first" }
        // Foreach checked
        // Notify customChoiceListener!!.onItemCheckedStateChanged(this)
        updateOnScreenCheckedViews()
    }

    fun toggleItemChecked(position: Int) {
        check(isInCustomChoice) { "Call intoCheckMode first" }
        val gid = gidGetter(position) ?: return
        if (gid in checkedGid) {
            checkedGid.remove(gid)
        } else {
            checkedGid.add(gid)
        }
        customChoiceListener?.onItemCheckedStateChanged(this)
        updateOnScreenCheckedViews()
    }

    fun setCustomCheckedListener(listener: CustomChoiceListener?) {
        customChoiceListener = listener
    }

    private fun updateOnScreenCheckedViews() {
        forEach {
            checkChild(it)
        }
    }

    override fun onSaveInstanceState() = SavedState(
        isInCustomChoice,
        checkedGid,
        super.onSaveInstanceState(),
    )

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        isInCustomChoice = ss.customChoice
        checkedGid = ss.checkedGid
        updateOnScreenCheckedViews()
    }

    interface CustomChoiceListener {
        fun onIntoCustomChoice(view: SelectableGalleryInfoRecyclerView)
        fun onOutOfCustomChoice(view: SelectableGalleryInfoRecyclerView)
        fun onItemCheckedStateChanged(view: SelectableGalleryInfoRecyclerView)
    }

    @Parcelize
    class SavedState(
        val customChoice: Boolean,
        val checkedGid: MutableSet<Long>,
        val superState: Parcelable?,
    ) : Parcelable
}

private fun setViewChecked(view: View, checked: Boolean) {
    if (view is Checkable) {
        (view as Checkable).isChecked = checked
    } else {
        view.isActivated = checked
    }
}
