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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhRequest
import com.hippo.util.ExceptionUtils
import com.hippo.view.ViewTransition
import com.hippo.yorozuya.AssertUtils
import com.hippo.yorozuya.ViewUtils

/**
 * Only show a progress with jobs in background
 */
class ProgressScene : BaseScene(), View.OnClickListener {
    private var mValid = false
    private var mError: String? = null
    private var mAction: String? = null
    private var mGid: Long = 0
    private var mPToken: String? = null
    private var mPage = 0
    private var mTip: TextView? = null
    private var mViewTransition: ViewTransition? = null
    override fun needShowLeftDrawer(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    private fun doJobs(): Boolean {
        val context = context
        val activity = mainActivity
        if (null == context || null == activity) {
            return false
        }
        if (ACTION_GALLERY_TOKEN == mAction) {
            if (mGid == -1L || mPToken == null || mPage == -1) {
                return false
            }
            val request = EhRequest()
                .setMethod(EhClient.METHOD_GET_GALLERY_TOKEN)
                .setArgs(mGid, mPToken!!, mPage)
                .setCallback(
                    GetGalleryTokenListener(
                        context
                    )
                )
            request.enqueue()
            return true
        }
        return false
    }

    private fun handleArgs(args: Bundle?): Boolean {
        if (args == null) {
            return false
        }
        mAction = args.getString(KEY_ACTION)
        if (ACTION_GALLERY_TOKEN == mAction) {
            mGid = args.getLong(KEY_GID, -1)
            mPToken = args.getString(KEY_PTOKEN, null)
            mPage = args.getInt(KEY_PAGE, -1)
            return mGid != -1L && mPToken != null && mPage != -1
        }
        return false
    }

    private fun onInit() {
        mValid = handleArgs(arguments)
        if (mValid) {
            mValid = doJobs()
        }
        if (!mValid) {
            mError = getString(R.string.error_something_wrong_happened)
        }
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mValid = savedInstanceState.getBoolean(KEY_VALID)
        mError = savedInstanceState.getString(KEY_ERROR)
        mAction = savedInstanceState.getString(KEY_ACTION)
        mGid = savedInstanceState.getLong(KEY_GID, -1)
        mPToken = savedInstanceState.getString(KEY_PTOKEN, null)
        mPage = savedInstanceState.getInt(KEY_PAGE, -1)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_VALID, mValid)
        outState.putString(KEY_ERROR, mError)
        outState.putString(KEY_ACTION, mAction)
        outState.putLong(KEY_GID, mGid)
        outState.putString(KEY_PTOKEN, mPToken)
        outState.putInt(KEY_PAGE, mPage)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.scene_progress, container, false)
        val progress = ViewUtils.`$$`(view, R.id.progress)
        mTip = ViewUtils.`$$`(view, R.id.tip) as TextView
        val context = context
        AssertUtils.assertNotNull(context)
        val drawable = ContextCompat.getDrawable(context!!, R.drawable.big_sad_pandroid)
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        mTip!!.setCompoundDrawables(null, drawable, null, null)
        mTip!!.setOnClickListener(this)
        mTip!!.text = mError
        mViewTransition = ViewTransition(progress, mTip)
        if (mValid) {
            mViewTransition!!.showView(0, false)
        } else {
            mViewTransition!!.showView(1, false)
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mTip = null
        mViewTransition = null
    }

    override fun onClick(v: View) {
        if (mTip === v) {
            if (doJobs()) {
                mValid = true
                // Show progress
                if (null != mViewTransition) {
                    mViewTransition!!.showView(0, true)
                }
            }
        }
    }

    private fun onGetGalleryTokenSuccess(result: String) {
        val arg = Bundle()
        arg.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN)
        arg.putLong(GalleryDetailScene.KEY_GID, mGid)
        arg.putString(GalleryDetailScene.KEY_TOKEN, result)
        arg.putInt(GalleryDetailScene.KEY_PAGE, mPage)
        findNavController().popBackStack()
        navigate(R.id.galleryDetailScene, arg)
    }

    private fun onGetGalleryTokenFailure(e: Exception) {
        mValid = false
        val context = context
        if (null != context && null != mViewTransition && null != mTip) {
            // Show tip
            mError = ExceptionUtils.getReadableString(e)
            mViewTransition!!.showView(1)
            mTip!!.text = mError
        }
    }

    private inner class GetGalleryTokenListener(
        context: Context
    ) : EhCallback<ProgressScene, String>(context) {
        override fun onSuccess(result: String) {
            val scene = this@ProgressScene
            scene.onGetGalleryTokenSuccess(result)
        }

        override fun onFailure(e: Exception) {
            val scene = this@ProgressScene
            scene.onGetGalleryTokenFailure(e)
        }

        override fun onCancel() {}
    }

    companion object {
        const val KEY_ACTION = "action"
        const val ACTION_GALLERY_TOKEN = "gallery_token"
        const val KEY_GID = "gid"
        const val KEY_PTOKEN = "ptoken"
        const val KEY_PAGE = "page"
        private const val KEY_VALID = "valid"
        private const val KEY_ERROR = "error"
    }
}