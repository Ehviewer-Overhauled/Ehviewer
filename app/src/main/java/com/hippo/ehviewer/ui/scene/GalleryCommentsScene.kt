/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui.scene

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.text.getSpans
import androidx.core.text.inSpans
import androidx.core.text.parseAsHtml
import androidx.core.text.set
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import androidx.viewbinding.ViewBinding
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.client.data.GalleryCommentList
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.parser.VoteCommentResult
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.databinding.ItemDrawerFavoritesBinding
import com.hippo.ehviewer.databinding.ItemGalleryCommentBinding
import com.hippo.ehviewer.databinding.ItemGalleryCommentMoreBinding
import com.hippo.ehviewer.databinding.ItemGalleryCommentProgressBinding
import com.hippo.ehviewer.databinding.SceneGalleryCommentsBinding
import com.hippo.ehviewer.ui.jumpToReaderByPage
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.CoilImageGetter
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import com.hippo.ehviewer.ui.legacy.ViewTransition
import com.hippo.ehviewer.ui.legacy.WindowInsetsAnimationHelper
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.scene.GalleryListScene.Companion.toStartArgs
import com.hippo.ehviewer.util.AnimationUtils
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.IntList
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.SimpleAnimatorListener
import com.hippo.ehviewer.util.TextUrl
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.getParcelableCompat
import com.hippo.ehviewer.util.toBBCode
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import moe.tarsin.coroutines.runSuspendCatching
import rikka.core.res.resolveColor
import kotlin.math.hypot

class GalleryCommentsScene : BaseToolbarScene(), View.OnClickListener, OnRefreshListener {
    private var _binding: SceneGalleryCommentsBinding? = null
    private val binding get() = _binding!!
    private val callback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (!mInAnimation && binding.editPanel.visibility == View.VISIBLE) {
                hideEditPanel(true)
            }
        }
    }
    private var mGalleryDetail: GalleryDetail? = null
    private var mAdapter: CommentAdapter? = null
    private var mViewTransition: ViewTransition? = null
    private var mSendDrawable: Drawable? = null
    private var mPencilDrawable: Drawable? = null
    private var mCommentId: Long = 0
    private var mInAnimation = false
    private var mShowAllComments = false
    private var mRefreshingComments = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(callback)
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    private fun handleArgs(args: Bundle?) {
        if (args == null) {
            return
        }
        mGalleryDetail = args.getParcelableCompat(KEY_GALLERY_DETAIL)
        mShowAllComments =
            mGalleryDetail != null && true && !mGalleryDetail!!.comments.hasMore
    }

    private fun onInit() {
        handleArgs(arguments)
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mGalleryDetail = savedInstanceState.getParcelableCompat(KEY_GALLERY_DETAIL)
        mShowAllComments = mGalleryDetail != null && !mGalleryDetail!!.comments.hasMore
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_GALLERY_DETAIL, mGalleryDetail)
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SceneGalleryCommentsBinding.inflate(inflater, container!!)
        setLiftOnScrollTargetView(binding.recyclerView)
        val tip = binding.tip
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.root,
            WindowInsetsAnimationHelper(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
                binding.editPanel,
                binding.fabLayout,
            ),
        )
        binding.refreshLayout.setOnRefreshListener(this)
        val context = requireContext()
        val drawable = ContextCompat.getDrawable(context, R.drawable.big_sad_pandroid)
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        tip.setCompoundDrawables(null, drawable, null, null)
        mSendDrawable = ContextCompat.getDrawable(context, R.drawable.v_send_dark_x24)
        mPencilDrawable = ContextCompat.getDrawable(context, R.drawable.v_pencil_dark_x24)
        mAdapter = CommentAdapter()
        binding.recyclerView.adapter = mAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(
            context,
            RecyclerView.VERTICAL,
            false,
        )
        binding.recyclerView.setHasFixedSize(true)
        // Cancel change animator
        val itemAnimator = binding.recyclerView.itemAnimator
        if (itemAnimator is DefaultItemAnimator) {
            itemAnimator.supportsChangeAnimations = false
        }
        binding.send.setOnClickListener(this)
        binding.editText.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                requireActivity().menuInflater.inflate(R.menu.context_comment, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return true
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                item?.let {
                    val text = binding.editText.editableText
                    val start = binding.editText.selectionStart
                    val end = binding.editText.selectionEnd
                    when (item.itemId) {
                        R.id.action_bold -> text[start, end] = StyleSpan(Typeface.BOLD)

                        R.id.action_italic -> text[start, end] = StyleSpan(Typeface.ITALIC)

                        R.id.action_underline -> text[start, end] = UnderlineSpan()

                        R.id.action_strikethrough -> text[start, end] = StrikethroughSpan()

                        R.id.action_url -> {
                            val oldSpans = text.getSpans<URLSpan>(start, end)
                            var oldUrl = "https://"
                            oldSpans.forEach {
                                if (!it.url.isNullOrEmpty()) {
                                    oldUrl = it.url
                                }
                            }
                            val builder = EditTextDialogBuilder(
                                context,
                                oldUrl,
                                getString(R.string.format_url),
                            )
                            builder.setTitle(getString(R.string.format_url))
                            builder.setPositiveButton(android.R.string.ok, null)
                            val dialog = builder.show()
                            val button: View? = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                            button?.setOnClickListener(
                                View.OnClickListener {
                                    val url = builder.text.trim()
                                    if (url.isEmpty()) {
                                        builder.setError(getString(R.string.text_is_empty))
                                        return@OnClickListener
                                    } else {
                                        builder.setError(null)
                                    }
                                    text.clearSpan(start, end, true)
                                    text[start, end] = URLSpan(url)
                                    dialog.dismiss()
                                },
                            )
                        }

                        R.id.action_clear -> {
                            text.clearSpan(start, end, false)
                        }

                        else -> return false
                    }
                    mode?.finish()
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
            }
        }
        binding.fab.setOnClickListener(this)
        addAboveSnackView(binding.editPanel)
        addAboveSnackView(binding.fabLayout)
        mViewTransition = ViewTransition(binding.refreshLayout, tip)
        updateView(false)
        return binding.root
    }

    fun Spannable.clearSpan(start: Int, end: Int, url: Boolean) {
        val spans = if (url) getSpans<URLSpan>(start, end) else getSpans<CharacterStyle>(start, end)
        spans.forEach {
            val spanStart = getSpanStart(it)
            val spanEnd = getSpanEnd(it)
            removeSpan(it)
            if (spanStart < start) {
                this[spanStart, start] = it
            }
            if (spanEnd > end) {
                this[end, spanEnd] = it
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        callback.remove()
        binding.recyclerView.stopScroll()
        removeAboveSnackView(binding.editPanel)
        removeAboveSnackView(binding.fabLayout)
        mAdapter = null
        mViewTransition = null
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(R.string.gallery_comments)
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24)
    }

    private fun showFilterCommenterDialog(commenter: String?, position: Int) {
        val context = context
        if (context == null || commenter == null) {
            return
        }
        BaseDialogBuilder(context)
            .setMessage(getString(R.string.filter_the_commenter, commenter))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                Filter(FilterMode.COMMENTER, commenter).remember()
                hideComment(position)
                showTip(R.string.filter_added, LENGTH_SHORT)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun hideComment(position: Int) {
        if (mGalleryDetail == null) {
            return
        }
        val oldCommentsList = mGalleryDetail!!.comments.comments
        val newCommentsList = arrayOfNulls<GalleryComment>(
            oldCommentsList.size - 1,
        )
        var i = 0
        var j = 0
        while (i < oldCommentsList.size) {
            if (i != position) {
                newCommentsList[j] = oldCommentsList[i]
                j++
            }
            i++
        }
        mGalleryDetail!!.comments.comments = newCommentsList.requireNoNulls()
        mAdapter!!.notifyDataSetChanged()
        updateView(true)
    }

    private fun voteComment(id: Long, vote: Int) {
        val gd = mGalleryDetail ?: return
        lifecycleScope.launchIO {
            runSuspendCatching {
                EhEngine.voteComment(gd.apiUid, gd.apiKey, gd.gid, gd.token, id, vote)
            }.onSuccess { result ->
                val voteUp = vote > 0
                showTip(
                    if (voteUp) (if (0 != result.vote) R.string.vote_up_successfully else R.string.cancel_vote_up_successfully) else if (0 != result.vote) R.string.vote_down_successfully else R.string.cancel_vote_down_successfully,
                    LENGTH_SHORT,
                )
                withUIContext { onVoteCommentSuccess(result, voteUp) }
            }.onFailure {
                showTip(R.string.vote_failed, LENGTH_LONG)
            }
        }
    }

    @SuppressLint("InflateParams")
    fun showVoteStatusDialog(context: Context, voteStatus: String) {
        val temp = voteStatus.split(',')
        val length = temp.size
        val userArray = arrayOfNulls<String>(length)
        val voteArray = arrayOfNulls<String>(length)
        for (i in 0 until length) {
            val str = temp[i].trim()
            val index = str.lastIndexOf(' ')
            if (index < 0) {
                Log.d(TAG, "Something wrong happened about vote state")
                userArray[i] = str
                voteArray[i] = ""
            } else {
                userArray[i] = str.substring(0, index).trim()
                voteArray[i] = str.substring(index + 1).trim()
            }
        }
        val builder = BaseDialogBuilder(context)
        val builderContext = builder.context
        val inflater = LayoutInflater.from(builderContext)
        val rv = inflater.inflate(R.layout.dialog_recycler_view, null) as RecyclerView
        rv.adapter = object : RecyclerView.Adapter<VoteHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoteHolder {
                return VoteHolder(ItemDrawerFavoritesBinding.inflate(inflater, parent, false))
            }

            override fun onBindViewHolder(holder: VoteHolder, position: Int) {
                holder.bind(userArray[position], voteArray[position])
            }

            override fun getItemCount(): Int {
                return length
            }
        }
        rv.layoutManager = LinearLayoutManager(builderContext)
        rv.clipToPadding = false
        builder.setView(rv).show()
    }

    private fun showCommentDialog(position: Int, text: CharSequence) {
        val context = context
        if (context == null || mGalleryDetail == null || position >= mGalleryDetail!!.comments.comments.size || position < 0) {
            return
        }
        val comment = mGalleryDetail!!.comments.comments[position]
        val menu: MutableList<String> = ArrayList()
        val menuId = IntList()
        val resources = context.resources
        menu.add(resources.getString(R.string.copy_comment_text))
        menuId.add(R.id.copy)
        if (!comment.uploader && !comment.editable) {
            menu.add(resources.getString(R.string.block_commenter))
            menuId.add(R.id.block_commenter)
        }
        if (comment.editable) {
            menu.add(resources.getString(R.string.edit_comment))
            menuId.add(R.id.edit_comment)
        }
        if (comment.voteUpAble) {
            menu.add(resources.getString(if (comment.voteUpEd) R.string.cancel_vote_up else R.string.vote_up))
            menuId.add(R.id.vote_up)
        }
        if (comment.voteDownAble) {
            menu.add(resources.getString(if (comment.voteDownEd) R.string.cancel_vote_down else R.string.vote_down))
            menuId.add(R.id.vote_down)
        }
        if (!comment.voteState.isNullOrEmpty()) {
            menu.add(resources.getString(R.string.check_vote_status))
            menuId.add(R.id.check_vote_status)
        }
        BaseDialogBuilder(context)
            .setItems(menu.toTypedArray()) { _: DialogInterface?, which: Int ->
                if (which < 0 || which >= menuId.size) {
                    return@setItems
                }
                when (menuId[which]) {
                    R.id.copy -> requireActivity().addTextToClipboard(text, false)
                    R.id.block_commenter -> showFilterCommenterDialog(comment.user, position)
                    R.id.vote_up -> voteComment(comment.id, 1)
                    R.id.vote_down -> voteComment(comment.id, -1)
                    R.id.check_vote_status -> showVoteStatusDialog(context, comment.voteState!!)
                    R.id.edit_comment -> {
                        prepareEditComment(comment.id, text)
                        if (!mInAnimation && binding.editPanel.visibility != View.VISIBLE) {
                            showEditPanel(true)
                        }
                    }
                }
            }.show()
    }

    fun onItemClick(parent: RecyclerView?, view2: View?, position: Int): Boolean {
        val activity = mainActivity ?: return false
        val holder = parent!!.getChildViewHolder(view2!!)
        if (holder is ActualCommentHolder) {
            val span = holder.binding.comment.currentSpan
            holder.binding.comment.clearCurrentSpan()
            val detail = mGalleryDetail ?: return false
            if (span is URLSpan) {
                if (!activity.jumpToReaderByPage(span.url, detail)) {
                    if (!findNavController().navWithUrl(span.url)) {
                        activity.openBrowser(span.url)
                    }
                }
            } else {
                showCommentDialog(position, holder.sp)
            }
        } else if (holder is MoreCommentHolder && !mRefreshingComments && mAdapter != null) {
            mRefreshingComments = true
            mShowAllComments = true
            mAdapter!!.notifyItemChanged(position)
            galleryDetailUrl?.let { refreshComment(it) }
        }
        return true
    }

    private fun refreshComment(url: String) {
        lifecycleScope.launchIO {
            runSuspendCatching {
                EhEngine.getGalleryDetail(url)
            }.onSuccess {
                withUIContext {
                    onRefreshGallerySuccess(it.comments)
                }
            }.onFailure {
                withUIContext {
                    onRefreshGalleryFailure()
                }
            }
        }
    }

    private fun updateView(animation: Boolean) {
        if (null == mViewTransition) {
            return
        }
        if (mGalleryDetail == null || mGalleryDetail!!.comments.comments.isEmpty()) {
            mViewTransition!!.showView(1, animation)
        } else {
            mViewTransition!!.showView(0, animation)
        }
    }

    private fun prepareNewComment() {
        mCommentId = 0
        binding.send.setImageDrawable(mSendDrawable)
    }

    private fun prepareEditComment(commentId: Long, text: CharSequence) {
        mCommentId = commentId
        binding.editText.setText(text)
        binding.send.setImageDrawable(mPencilDrawable)
    }

    private fun showEditPanelWithAnimation() {
        mInAnimation = true
        binding.fab.translationX = 0.0f
        binding.fab.translationY = 0.0f
        binding.fab.scaleX = 1.0f
        binding.fab.scaleY = 1.0f
        val fabEndX = binding.editPanel.left + binding.editPanel.width / 2 - binding.fab.width / 2
        val fabEndY = binding.editPanel.top + binding.editPanel.height / 2 - binding.fab.height / 2
        binding.fab.animate().x(fabEndX.toFloat()).y(fabEndY.toFloat()).scaleX(0.0f).scaleY(0.0f)
            .setInterpolator(AnimationUtils.SLOW_FAST_SLOW_INTERPOLATOR)
            .setDuration(300L).setListener(object : SimpleAnimatorListener() {
                override fun onAnimationEnd(animation: Animator) {
                    (binding.fab as View).visibility = View.INVISIBLE
                    binding.editPanel.visibility = View.VISIBLE
                    val halfW = binding.editPanel.width / 2
                    val halfH = binding.editPanel.height / 2
                    val animator = ViewAnimationUtils.createCircularReveal(
                        binding.editPanel,
                        halfW,
                        halfH,
                        0f,
                        hypot(halfW.toDouble(), halfH.toDouble()).toFloat(),
                    ).setDuration(300L)
                    animator.addListener(object : SimpleAnimatorListener() {
                        override fun onAnimationEnd(animation: Animator) {
                            mInAnimation = false
                        }
                    })
                    animator.start()
                }
            }).start()
    }

    private fun showEditPanel(animation: Boolean) {
        callback.isEnabled = true
        if (animation) {
            showEditPanelWithAnimation()
        } else {
            (binding.fab as View).visibility = View.INVISIBLE
            binding.editPanel.visibility = View.VISIBLE
        }
    }

    private fun hideEditPanelWithAnimation() {
        mInAnimation = true
        val halfW = binding.editPanel.width / 2
        val halfH = binding.editPanel.height / 2
        val animator = ViewAnimationUtils.createCircularReveal(
            binding.editPanel,
            halfW,
            halfH,
            hypot(halfW.toDouble(), halfH.toDouble()).toFloat(),
            0.0f,
        ).setDuration(300L)
        animator.addListener(object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    // Some devices may run this block in non-UI thread.
                    // It might be a bug of Android OS.
                    // Check it here to avoid crash.
                    return
                }
                binding.editPanel.visibility = View.GONE
                (binding.fab as View).visibility = View.VISIBLE
                val fabStartX =
                    binding.editPanel.left + binding.editPanel.width / 2 - binding.fab.width / 2
                val fabStartY =
                    binding.editPanel.top + binding.editPanel.height / 2 - binding.fab.height / 2
                binding.fab.x = fabStartX.toFloat()
                binding.fab.y = fabStartY.toFloat()
                binding.fab.scaleX = 0.0f
                binding.fab.scaleY = 0.0f
                binding.fab.rotation = -45.0f
                binding.fab.animate().translationX(0.0f).translationY(0.0f).scaleX(1.0f)
                    .scaleY(1.0f)
                    .rotation(0.0f)
                    .setInterpolator(AnimationUtils.SLOW_FAST_SLOW_INTERPOLATOR)
                    .setDuration(300L).setListener(object : SimpleAnimatorListener() {
                        override fun onAnimationEnd(animation: Animator) {
                            mInAnimation = false
                        }
                    }).start()
            }
        })
        animator.start()
    }

    private fun hideEditPanel(animation: Boolean) {
        callback.isEnabled = false
        hideSoftInput()
        if (animation) {
            hideEditPanelWithAnimation()
        } else {
            (binding.fab as View).visibility = View.VISIBLE
            binding.editPanel.visibility = View.INVISIBLE
        }
    }

    private val galleryDetailUrl: String?
        get() = if (mGalleryDetail != null && mGalleryDetail!!.gid != -1L && mGalleryDetail!!.token != null) {
            EhUrl.getGalleryDetailUrl(
                mGalleryDetail!!.gid,
                mGalleryDetail!!.token,
                0,
                mShowAllComments,
            )
        } else {
            null
        }

    override fun onClick(v: View) {
        val context = context
        val activity = mainActivity
        if (null == context || null == activity) {
            return
        }
        if (binding.fab === v) {
            if (!mInAnimation) {
                prepareNewComment()
                showEditPanel(true)
            }
        } else if (binding.send === v) {
            if (!mInAnimation) {
                val comment = binding.editText.text?.toBBCode()?.takeIf { it.isNotBlank() } ?: return
                val url = galleryDetailUrl ?: return
                lifecycleScope.launchIO {
                    runSuspendCatching {
                        EhEngine.commentGallery(
                            url,
                            comment,
                            if (mCommentId != 0L) mCommentId.toString() else null,
                        )
                    }.onSuccess {
                        showTip(
                            if (mCommentId != 0L) R.string.edit_comment_successfully else R.string.comment_successfully,
                            LENGTH_SHORT,
                        )
                        withUIContext {
                            onCommentGallerySuccess(it)
                        }
                    }.onFailure {
                        showTip(
                            """
    ${getString(if (mCommentId != 0L) R.string.edit_comment_failed else R.string.comment_failed)}
    ${ExceptionUtils.getReadableString(it)}
                            """.trimIndent(),
                            LENGTH_LONG,
                        )
                    }
                }
                hideSoftInput()
                hideEditPanel(true)
            }
        }
    }

    private fun onRefreshGallerySuccess(result: GalleryCommentList) {
        if (mGalleryDetail == null || mAdapter == null) {
            return
        }
        binding.refreshLayout.isRefreshing = false
        mRefreshingComments = false
        mGalleryDetail!!.comments = result
        mAdapter!!.notifyDataSetChanged()
        updateView(true)
    }

    private fun onRefreshGalleryFailure() {
        if (mAdapter == null) {
            return
        }
        binding.refreshLayout.isRefreshing = false
        mRefreshingComments = false
        val position = mAdapter!!.itemCount - 1
        if (position >= 0) {
            mAdapter!!.notifyItemChanged(position)
        }
    }

    private fun onCommentGallerySuccess(result: GalleryCommentList) {
        if (mGalleryDetail == null || mAdapter == null) {
            return
        }
        mGalleryDetail!!.comments = result
        mAdapter!!.notifyDataSetChanged()

        // Remove text
        binding.editText.setText("")
        updateView(true)
    }

    private fun onVoteCommentSuccess(result: VoteCommentResult, voteUp: Boolean) {
        if (mAdapter == null) {
            return
        }
        var position = -1
        var i = 0
        val n = mGalleryDetail!!.comments.comments.size
        while (i < n) {
            val comment = mGalleryDetail!!.comments.comments[i]
            if (comment.id == result.id) {
                position = i
                break
            }
            i++
        }
        if (-1 == position) {
            Log.d(TAG, "Can't find comment with id " + result.id)
            return
        }

        // Update comment
        val comment = mGalleryDetail!!.comments.comments[position]
        comment.score = result.score
        if (voteUp) {
            comment.voteUpEd = 0 != result.vote
            comment.voteDownEd = false
        } else {
            comment.voteDownEd = 0 != result.vote
            comment.voteUpEd = false
        }
        mAdapter!!.notifyItemChanged(position)
    }

    override fun onRefresh() {
        if (!mRefreshingComments && mAdapter != null) {
            mRefreshingComments = true
            galleryDetailUrl?.let { refreshComment(it) }
        }
    }

    private class VoteHolder(private val binding: ItemDrawerFavoritesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: String?, vote: String?) {
            binding.key.text = user
            binding.value.text = vote
        }
    }

    private abstract class CommentHolder(binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class MoreCommentHolder(binding: ItemGalleryCommentMoreBinding) : CommentHolder(binding)

    private class ProgressCommentHolder(binding: ItemGalleryCommentProgressBinding) :
        CommentHolder(binding)

    private inner class ActualCommentHolder(val binding: ItemGalleryCommentBinding) :
        CommentHolder(binding) {
        lateinit var sp: CharSequence

        private fun generateComment(
            context: Context,
            textView: TextView,
            comment: GalleryComment,
        ): CharSequence {
            sp = comment.comment.orEmpty().parseAsHtml(imageGetter = CoilImageGetter(textView))
            val ssb = SpannableStringBuilder(sp)
            if (0L != comment.id && 0 != comment.score) {
                val score = comment.score
                val scoreString = if (score > 0) "+$score" else score.toString()
                ssb.append("  ").inSpans(
                    RelativeSizeSpan(0.8f),
                    StyleSpan(Typeface.BOLD),
                    ForegroundColorSpan(theme.resolveColor(android.R.attr.textColorSecondary)),
                ) {
                    append(scoreString)
                }
            }
            if (comment.lastEdited != 0L) {
                val str = context.getString(
                    R.string.last_edited,
                    ReadableTime.getTimeAgo(comment.lastEdited),
                )
                ssb.append("\n\n").inSpans(
                    RelativeSizeSpan(0.8f),
                    StyleSpan(Typeface.BOLD),
                    ForegroundColorSpan(theme.resolveColor(android.R.attr.textColorSecondary)),
                ) {
                    append(str)
                }
            }
            return TextUrl.handleTextUrl(ssb)
        }

        fun bind(value: GalleryComment) {
            binding.run {
                user.text = value.user?.let {
                    if (value.uploader) getString(R.string.comment_user_uploader, it) else it
                }
                user.setOnClickListener {
                    value.user?.let {
                        val lub = ListUrlBuilder().apply {
                            mode = ListUrlBuilder.MODE_UPLOADER
                            keyword = it
                        }
                        navAnimated(R.id.galleryListScene, lub.toStartArgs(), true)
                    }
                }
                time.text = ReadableTime.getTimeAgo(value.time)
                comment.text = generateComment(binding.comment.context, binding.comment, value)
            }
        }
    }

    private inner class CommentAdapter : RecyclerView.Adapter<CommentHolder>() {
        private val mInflater: LayoutInflater = layoutInflater

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentHolder {
            return when (viewType) {
                TYPE_COMMENT -> ActualCommentHolder(
                    ItemGalleryCommentBinding.inflate(mInflater, parent, false),
                )

                TYPE_MORE -> MoreCommentHolder(
                    ItemGalleryCommentMoreBinding.inflate(mInflater, parent, false),
                )

                TYPE_PROGRESS -> ProgressCommentHolder(
                    ItemGalleryCommentProgressBinding.inflate(mInflater, parent, false),
                )

                else -> throw IllegalStateException("Invalid view type: $viewType")
            }
        }

        override fun onBindViewHolder(holder: CommentHolder, position: Int) {
            val context = context
            if (context == null || mGalleryDetail == null) {
                return
            }
            holder.itemView.setOnClickListener {
                onItemClick(
                    binding.recyclerView,
                    holder.itemView,
                    position,
                )
            }
            holder.itemView.isClickable = true
            holder.itemView.isFocusable = true
            if (holder is ActualCommentHolder) {
                holder.bind(mGalleryDetail!!.comments.comments[position])
            }
        }

        override fun getItemCount(): Int {
            return if (mGalleryDetail == null) {
                0
            } else if (mGalleryDetail!!.comments.hasMore) {
                mGalleryDetail!!.comments.comments.size + 1
            } else {
                mGalleryDetail!!.comments.comments.size
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position >= mGalleryDetail!!.comments.comments.size) {
                if (mRefreshingComments) TYPE_PROGRESS else TYPE_MORE
            } else {
                TYPE_COMMENT
            }
        }
    }

    companion object {
        val TAG: String = GalleryCommentsScene::class.java.simpleName
        const val KEY_API_UID = "api_uid"
        const val KEY_API_KEY = "api_key"
        const val KEY_GID = "gid"
        const val KEY_TOKEN = "token"
        const val KEY_COMMENT_LIST = "comment_list"
        const val KEY_GALLERY_DETAIL = "gallery_detail"
        private const val TYPE_COMMENT = 0
        private const val TYPE_MORE = 1
        private const val TYPE_PROGRESS = 2
    }
}
