package com.hippo.ehviewer.ui.legacy

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout.AttachedBehavior
import androidx.core.util.ObjectsCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.drakeet.drawer.FullDraggableHelper
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.hippo.ehviewer.util.dp2px

class AboveSnackerLayout : FrameLayout, AttachedBehavior, FullDraggableHelper.Callback {
    private val helper: FullDraggableHelper
    private var drawerLayout: DrawerLayout? = null
    private var mAboveSnackViewList: MutableList<View>? = null

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        helper = FullDraggableHelper(context, this)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) :
        super(context!!, attrs, defStyle) {
        helper = FullDraggableHelper(context, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ensureDrawerLayout()
    }

    private fun ensureDrawerLayout() {
        val parent = parent.parent
        check(parent is DrawerLayout) { "This $this must be added to a DrawerLayout" }
        drawerLayout = parent
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return helper.onInterceptTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return helper.onTouchEvent(event)
    }

    override fun getDrawerMainContainer(): View {
        return this
    }

    override fun isDrawerOpen(gravity: Int): Boolean {
        return drawerLayout!!.isDrawerOpen(gravity)
    }

    override fun hasEnabledDrawer(gravity: Int): Boolean {
        return drawerLayout!!.getDrawerLockMode(gravity) == DrawerLayout.LOCK_MODE_UNLOCKED &&
            findDrawerWithGravity(gravity) != null
    }

    override fun offsetDrawer(gravity: Int, offset: Float) {
        setDrawerToOffset(gravity, offset)
        drawerLayout!!.invalidate()
    }

    override fun smoothOpenDrawer(gravity: Int) {
        drawerLayout!!.openDrawer(gravity, true)
    }

    override fun smoothCloseDrawer(gravity: Int) {
        drawerLayout!!.closeDrawer(gravity, true)
    }

    override fun onDrawerDragging() {
        val drawerListeners = drawerListeners
        if (drawerListeners != null) {
            val listenerCount = drawerListeners.size
            for (i in listenerCount - 1 downTo 0) {
                drawerListeners[i].onDrawerStateChanged(DrawerLayout.STATE_DRAGGING)
            }
        }
    }

    private val drawerListeners: List<DrawerListener>?
        get() = try {
            val listeners = DrawerLayout::class.java.getDeclaredField("mListeners")
            listeners.isAccessible = true
            listeners[drawerLayout] as List<DrawerListener>
        } catch (e: Exception) {
            // throw to let developer know the api is changed
            throw RuntimeException(e)
        }

    private fun setDrawerToOffset(gravity: Int, offset: Float) {
        val drawerView = findDrawerWithGravity(gravity)
        val slideOffsetPercent =
            (offset / ObjectsCompat.requireNonNull(drawerView).width).coerceIn(0f, 1f)
        try {
            val method = DrawerLayout::class.java.getDeclaredMethod(
                "moveDrawerToOffset",
                View::class.java,
                Float::class.javaPrimitiveType,
            )
            method.isAccessible = true
            method.invoke(drawerLayout, drawerView, slideOffsetPercent)
            drawerView!!.visibility = VISIBLE
        } catch (e: Exception) {
            // throw to let developer know the api is changed
            throw RuntimeException(e)
        }
    }

    // Copied from DrawerLayout
    private fun findDrawerWithGravity(gravity: Int): View? {
        val absHorizontalGravity = GravityCompat.getAbsoluteGravity(
            gravity,
            ViewCompat.getLayoutDirection(drawerLayout!!),
        ) and Gravity.HORIZONTAL_GRAVITY_MASK
        val childCount = drawerLayout!!.childCount
        for (i in 0 until childCount) {
            val child = drawerLayout!!.getChildAt(i)
            val childAbsGravity = getDrawerViewAbsoluteGravity(child)
            if (childAbsGravity and Gravity.HORIZONTAL_GRAVITY_MASK == absHorizontalGravity) {
                return child
            }
        }
        return null
    }

    // Copied from DrawerLayout
    private fun getDrawerViewAbsoluteGravity(drawerView: View): Int {
        val gravity = (drawerView.layoutParams as DrawerLayout.LayoutParams).gravity
        return GravityCompat.getAbsoluteGravity(
            gravity,
            ViewCompat.getLayoutDirection(drawerLayout!!),
        )
    }

    fun addAboveSnackView(view: View) {
        if (null == mAboveSnackViewList) {
            mAboveSnackViewList = ArrayList()
        }
        mAboveSnackViewList!!.add(view)
    }

    fun removeAboveSnackView(view: View) {
        if (null == mAboveSnackViewList) {
            return
        }
        mAboveSnackViewList!!.remove(view)
    }

    val aboveSnackViewCount: Int
        get() = if (null == mAboveSnackViewList) 0 else mAboveSnackViewList!!.size

    fun getAboveSnackViewAt(index: Int): View? {
        return if (null == mAboveSnackViewList || index < 0 || index >= mAboveSnackViewList!!.size) {
            null
        } else {
            mAboveSnackViewList!![index]
        }
    }

    override fun getBehavior(): Behavior {
        return Behavior()
    }

    class Behavior : CoordinatorLayout.Behavior<AboveSnackerLayout>() {
        override fun layoutDependsOn(
            parent: CoordinatorLayout,
            child: AboveSnackerLayout,
            dependency: View,
        ): Boolean {
            return dependency is SnackbarLayout
        }

        override fun onDependentViewChanged(
            parent: CoordinatorLayout,
            child: AboveSnackerLayout,
            dependency: View,
        ): Boolean {
            for (i in 0 until child.aboveSnackViewCount) {
                val view = child.getAboveSnackViewAt(i)
                if (view != null) {
                    val translationY =
                        (dependency.translationY - dependency.height - dp2px(view.context, 8f))
                            .coerceAtMost(0f)
                    ViewCompat.animate(view).setInterpolator(FastOutSlowInInterpolator())
                        .translationY(translationY).setDuration(150).start()
                }
            }
            return false
        }

        override fun onDependentViewRemoved(
            parent: CoordinatorLayout,
            child: AboveSnackerLayout,
            dependency: View,
        ) {
            for (i in 0 until child.aboveSnackViewCount) {
                val view = child.getAboveSnackViewAt(i)
                if (view != null) {
                    ViewCompat.animate(view).setInterpolator(FastOutSlowInInterpolator())
                        .translationY(0f).setDuration(75).start()
                }
            }
        }
    }
}
