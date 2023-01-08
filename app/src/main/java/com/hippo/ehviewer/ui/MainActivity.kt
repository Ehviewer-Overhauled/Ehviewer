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
package com.hippo.ehviewer.ui

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.hippo.app.BaseDialogBuilder
import com.hippo.app.EditTextDialogBuilder
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrlOpener
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.ui.scene.CookieSignInScene
import com.hippo.ehviewer.ui.scene.DownloadsScene
import com.hippo.ehviewer.ui.scene.FavoritesScene
import com.hippo.ehviewer.ui.scene.GalleryCommentsScene
import com.hippo.ehviewer.ui.scene.GalleryDetailScene
import com.hippo.ehviewer.ui.scene.GalleryListScene
import com.hippo.ehviewer.ui.scene.GalleryPreviewsScene
import com.hippo.ehviewer.ui.scene.HistoryScene
import com.hippo.ehviewer.ui.scene.ProgressScene
import com.hippo.ehviewer.ui.scene.SelectSiteScene
import com.hippo.ehviewer.ui.scene.SignInScene
import com.hippo.ehviewer.ui.scene.SolidScene
import com.hippo.ehviewer.ui.scene.WebViewSignInScene
import com.hippo.io.UniFileInputStreamPipe
import com.hippo.scene.Announcer
import com.hippo.scene.SceneFragment
import com.hippo.scene.StageActivity
import com.hippo.unifile.UniFile
import com.hippo.util.BitmapUtils
import com.hippo.util.addTextToClipboard
import com.hippo.util.getClipboardManager
import com.hippo.util.getUrlFromClipboard
import com.hippo.widget.DrawerView
import com.hippo.yorozuya.IOUtils
import com.hippo.yorozuya.SimpleHandler
import com.hippo.yorozuya.ViewUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MainActivity : StageActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) refreshTopScene()
        }

    /*---------------
     Whole life cycle
     ---------------*/
    private var mDrawerLayout: DrawerLayout? = null
    private var mNavView: NavigationView? = null
    private var mRightDrawer: DrawerView? = null
    private var mNavCheckedItem = 0

    private fun saveImageToTempFile(file: UniFile?): File? {
        file ?: return null
        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapUtils.decodeStream(
                UniFileInputStreamPipe(file),
                -1,
                -1,
                500 * 500,
                false,
                false,
                null
            )
        } catch (e: OutOfMemoryError) {
            // Ignore
        }
        if (null == bitmap) {
            return null
        }
        val temp = AppConfig.createTempFile() ?: return null
        var os: OutputStream? = null
        return try {
            os = FileOutputStream(temp)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
            temp
        } catch (e: IOException) {
            null
        } finally {
            IOUtils.closeQuietly(os)
        }
    }

    private fun handleIntent(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        }
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            val uri = intent.data ?: return false
            val announcer = EhUrlOpener.parseUrl(uri.toString())
            if (announcer != null) {
                startScene(announcer)
                return true
            }
        } else if (Intent.ACTION_SEND == action) {
            val type = intent.type
            if ("text/plain" == type) {
                val builder = ListUrlBuilder()
                builder.keyword = intent.getStringExtra(Intent.EXTRA_TEXT)
                startScene(GalleryListScene.getStartAnnouncer(builder))
                return true
            } else if (type != null && type.startsWith("image/")) {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (null != uri) {
                    val file = UniFile.fromUri(this, uri)
                    val temp = saveImageToTempFile(file)
                    if (null != temp) {
                        val builder = ListUrlBuilder()
                        builder.mode = ListUrlBuilder.MODE_IMAGE_SEARCH
                        builder.imagePath = temp.path
                        builder.isUseSimilarityScan = true
                        startScene(GalleryListScene.getStartAnnouncer(builder))
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun onUnrecognizedIntent(intent: Intent?) {
        val clazz = topSceneClass
        if (clazz != null && SolidScene::class.java.isAssignableFrom(clazz)) {
            // TODO the intent lost
            return
        }
        if (!handleIntent(intent)) {
            if (intent != null && Intent.ACTION_VIEW == intent.action) {
                if (intent.data != null) {
                    val url = intent.data.toString()
                    EditTextDialogBuilder(this, url, "")
                        .setTitle(R.string.error_cannot_parse_the_url)
                        .setPositiveButton(android.R.string.copy) { _: DialogInterface?, _: Int ->
                            this.addTextToClipboard(
                                url,
                                false
                            )
                        }
                        .show()
                }
            }
        }
    }

    override fun onStartSceneFromIntent(clazz: Class<*>, args: Bundle?): Announcer {
        return Announcer(clazz).setArgs(args)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController
        mDrawerLayout = ViewUtils.`$$`(this, R.id.draw_view) as DrawerLayout
        mNavView = ViewUtils.`$$`(this, R.id.nav_view) as NavigationView
        mRightDrawer = ViewUtils.`$$`(this, R.id.right_drawer) as DrawerView
        mDrawerLayout?.addDrawerListener(mDrawerOnBackPressedCallback)
        onBackPressedDispatcher.addCallback(mDrawerOnBackPressedCallback)
        if (mNavView != null) {
            mNavView!!.setNavigationItemSelectedListener(this)
        }
        if (savedInstanceState == null) {
            checkDownloadLocation()
            if (Settings.getMeteredNetworkWarning()) {
                checkMeteredNetwork()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!Settings.getAppLinkVerifyTip()) {
                    try {
                        checkAppLinkVerify()
                    } catch (ignored: PackageManager.NameNotFoundException) {
                    }
                }
            }
        } else {
            onRestore(savedInstanceState)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Throws(PackageManager.NameNotFoundException::class)
    private fun checkAppLinkVerify() {
        val manager = getSystemService(DomainVerificationManager::class.java)
        val userState = manager.getDomainVerificationUserState(packageName) ?: return
        var hasUnverified = false
        val hostToStateMap = userState.hostToStateMap
        for (key in hostToStateMap.keys) {
            val stateValue = hostToStateMap[key]
            if (stateValue == null || stateValue == DomainVerificationUserState.DOMAIN_STATE_VERIFIED || stateValue == DomainVerificationUserState.DOMAIN_STATE_SELECTED) {
                continue
            }
            hasUnverified = true
            break
        }
        if (hasUnverified) {
            BaseDialogBuilder(this)
                .setTitle(R.string.app_link_not_verified_title)
                .setMessage(R.string.app_link_not_verified_message)
                .setPositiveButton(R.string.open_settings) { _: DialogInterface?, _: Int ->
                    try {
                        val intent = Intent(
                            android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    } catch (t: Throwable) {
                        val intent = Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.dont_show_again) { _: DialogInterface?, _: Int ->
                    Settings.putAppLinkVerifyTip(
                        true
                    )
                }
                .show()
        }
    }

    private fun checkDownloadLocation() {
        val uniFile = Settings.getDownloadLocation()
        // null == uniFile for first start
        if (null == uniFile || uniFile.ensureDir()) {
            return
        }
        BaseDialogBuilder(this)
            .setTitle(R.string.waring)
            .setMessage(R.string.invalid_download_location)
            .setPositiveButton(R.string.get_it, null)
            .show()
    }

    private fun checkMeteredNetwork() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (cm.isActiveNetworkMetered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mDrawerLayout != null) {
                Snackbar.make(
                    mDrawerLayout!!,
                    R.string.metered_network_warning,
                    Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.settings) {
                        val panelIntent =
                            Intent(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                        startActivity(panelIntent)
                    }
                    .show()
            } else {
                showTip(R.string.metered_network_warning, BaseScene.LENGTH_LONG)
            }
        }
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mNavCheckedItem = savedInstanceState.getInt(KEY_NAV_CHECKED_ITEM)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putInt(KEY_NAV_CHECKED_ITEM, mNavCheckedItem)
    }

    override fun onDestroy() {
        super.onDestroy()
        mDrawerLayout = null
        mNavView = null
        mRightDrawer = null
    }

    override fun onResume() {
        super.onResume()
        setNavCheckedItem(mNavCheckedItem)
        checkClipboardUrl()
    }

    override fun onTransactScene() {
        super.onTransactScene()
        checkClipboardUrl()
    }

    private fun checkClipboardUrl() {
        SimpleHandler.getInstance().postDelayed({
            if (!isSolid) {
                checkClipboardUrlInternal()
            }
        }, 300)
    }

    private val isSolid: Boolean
        get() {
            val topClass = topSceneClass
            return topClass == null || SolidScene::class.java.isAssignableFrom(topClass)
        }

    private fun createAnnouncerFromClipboardUrl(url: String): Announcer? {
        val result1 = GalleryDetailUrlParser.parse(url, false)
        if (result1 != null) {
            val args = Bundle()
            args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN)
            args.putLong(GalleryDetailScene.KEY_GID, result1.gid)
            args.putString(GalleryDetailScene.KEY_TOKEN, result1.token)
            return Announcer(GalleryDetailScene::class.java).setArgs(args)
        }
        val result2 = GalleryPageUrlParser.parse(url, false)
        if (result2 != null) {
            val args = Bundle()
            args.putString(ProgressScene.KEY_ACTION, ProgressScene.ACTION_GALLERY_TOKEN)
            args.putLong(ProgressScene.KEY_GID, result2.gid)
            args.putString(ProgressScene.KEY_PTOKEN, result2.pToken)
            args.putInt(ProgressScene.KEY_PAGE, result2.page)
            return Announcer(ProgressScene::class.java).setArgs(args)
        }
        return null
    }

    private fun checkClipboardUrlInternal() {
        val text = this.getClipboardManager().getUrlFromClipboard(this)
        val hashCode = text?.hashCode() ?: 0
        if (text != null && hashCode != 0 && Settings.getClipboardTextHashCode() != hashCode) {
            val announcer = createAnnouncerFromClipboardUrl(text)
            if (announcer != null && mDrawerLayout != null) {
                val snackbar = Snackbar.make(
                    mDrawerLayout!!,
                    R.string.clipboard_gallery_url_snack_message,
                    Snackbar.LENGTH_SHORT
                )
                snackbar.setAction(R.string.clipboard_gallery_url_snack_action) {
                    startScene(
                        announcer
                    )
                }
                snackbar.show()
            }
        }
        Settings.putClipboardTextHashCode(hashCode)
    }

    @SuppressLint("RtlHardcoded")
    fun createDrawerView(scene: SceneFragment?) {
        if (scene is BaseScene && mRightDrawer != null && mDrawerLayout != null) {
            mRightDrawer!!.removeAllViews()
            val drawerView = scene.createDrawerView(
                scene.layoutInflater, mRightDrawer, null
            )
            if (drawerView != null) {
                mRightDrawer!!.addView(drawerView)
                mDrawerLayout!!.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNLOCKED,
                    GravityCompat.END
                )
            } else {
                mDrawerLayout!!.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    GravityCompat.END
                )
            }
        }
    }

    fun addAboveSnackView(view: View?) {
    }

    fun removeAboveSnackView(view: View?) {
    }

    fun setDrawerLockMode(lockMode: Int, edgeGravity: Int) {
        mDrawerLayout?.setDrawerLockMode(lockMode, edgeGravity)
    }

    fun getDrawerLockMode(edgeGravity: Int): Int? {
        return mDrawerLayout?.getDrawerLockMode(edgeGravity)
    }

    fun openDrawer(drawerGravity: Int) {
        mDrawerLayout?.openDrawer(drawerGravity)
    }

    fun closeDrawer(drawerGravity: Int) {
        mDrawerLayout?.closeDrawer(drawerGravity)
    }

    fun toggleDrawer(drawerGravity: Int) {
        mDrawerLayout?.run {
            if (isDrawerOpen(drawerGravity)) {
                closeDrawer(drawerGravity)
            } else {
                openDrawer(drawerGravity)
            }
        }
    }

    fun setNavCheckedItem(@IdRes resId: Int) {
        mNavCheckedItem = resId
        mNavView?.run {
            if (resId == 0) {
                setCheckedItem(R.id.nav_stub)
            } else {
                setCheckedItem(resId)
            }
        }
    }

    @JvmOverloads
    fun showTip(@StringRes id: Int, length: Int, useToast: Boolean = false) {
        showTip(getString(id), length, useToast)
    }

    /**
     * If activity is running, show snack bar, otherwise show toast
     */
    @JvmOverloads
    fun showTip(message: CharSequence, length: Int, useToast: Boolean = false) {
        findViewById<View>(R.id.snackbar)?.takeUnless { useToast }?.apply {
            Snackbar.make(
                this, message,
                if (length == BaseScene.LENGTH_LONG) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
            ).show()
        } ?: Toast.makeText(
            this, message,
            if (length == BaseScene.LENGTH_LONG) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Don't select twice
        if (item.isChecked) {
            return false
        }
        val id = item.itemId
        when (id) {
            R.id.nav_homepage -> {
                val args = Bundle()
                args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_HOMEPAGE)
                startSceneFirstly(
                    Announcer(GalleryListScene::class.java)
                        .setArgs(args)
                )
            }

            R.id.nav_subscription -> {
                val args = Bundle()
                args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_SUBSCRIPTION)
                startSceneFirstly(
                    Announcer(GalleryListScene::class.java)
                        .setArgs(args)
                )
            }

            R.id.nav_whats_hot -> {
                val args = Bundle()
                args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_WHATS_HOT)
                startSceneFirstly(
                    Announcer(GalleryListScene::class.java)
                        .setArgs(args)
                )
            }

            R.id.nav_toplist -> {
                val args = Bundle()
                args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_TOP_LIST)
                startSceneFirstly(
                    Announcer(GalleryListScene::class.java)
                        .setArgs(args)
                )
            }

            R.id.nav_favourite -> {
                startScene(Announcer(FavoritesScene::class.java))
            }

            R.id.nav_history -> {
                startScene(Announcer(HistoryScene::class.java))
            }

            R.id.nav_downloads -> {
                startScene(Announcer(DownloadsScene::class.java))
            }

            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                settingsLauncher.launch(intent)
            }
        }
        if (id != R.id.nav_stub) {
            mDrawerLayout?.closeDrawers()
        }
        return true
    }

    private val mDrawerOnBackPressedCallback =
        object : OnBackPressedCallback(false), DrawerListener {
            val slideThreshold = 0.05
            override fun handleOnBackPressed() {
                mDrawerLayout?.closeDrawers()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                isEnabled = slideOffset > slideThreshold
            }

            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        }

    companion object {
        private const val KEY_NAV_CHECKED_ITEM = "nav_checked_item"

        init {
            registerLaunchMode(SignInScene::class.java, SceneFragment.LAUNCH_MODE_SINGLE_TASK)
            registerLaunchMode(
                WebViewSignInScene::class.java,
                SceneFragment.LAUNCH_MODE_SINGLE_TASK
            )
            registerLaunchMode(CookieSignInScene::class.java, SceneFragment.LAUNCH_MODE_SINGLE_TASK)
            registerLaunchMode(SelectSiteScene::class.java, SceneFragment.LAUNCH_MODE_SINGLE_TASK)
            registerLaunchMode(GalleryListScene::class.java, SceneFragment.LAUNCH_MODE_SINGLE_TOP)
            registerLaunchMode(GalleryDetailScene::class.java, SceneFragment.LAUNCH_MODE_STANDARD)
            registerLaunchMode(GalleryCommentsScene::class.java, SceneFragment.LAUNCH_MODE_STANDARD)
            registerLaunchMode(GalleryPreviewsScene::class.java, SceneFragment.LAUNCH_MODE_STANDARD)
            registerLaunchMode(DownloadsScene::class.java, SceneFragment.LAUNCH_MODE_SINGLE_TASK)
            registerLaunchMode(FavoritesScene::class.java, SceneFragment.LAUNCH_MODE_SINGLE_TASK)
            registerLaunchMode(HistoryScene::class.java, SceneFragment.LAUNCH_MODE_SINGLE_TOP)
            registerLaunchMode(ProgressScene::class.java, SceneFragment.LAUNCH_MODE_STANDARD)
        }
    }
}