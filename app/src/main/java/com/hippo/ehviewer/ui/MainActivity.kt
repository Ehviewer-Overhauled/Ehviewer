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
import android.app.assist.AssistContent
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
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.hippo.app.BaseDialogBuilder
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrlOpener
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.databinding.ActivityMainBinding
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.ui.scene.GalleryDetailScene
import com.hippo.ehviewer.ui.scene.GalleryListScene
import com.hippo.ehviewer.ui.scene.ProgressScene
import com.hippo.io.UniFileInputStreamPipe
import com.hippo.unifile.UniFile
import com.hippo.util.BitmapUtils
import com.hippo.util.getClipboardManager
import com.hippo.util.getUrlFromClipboard
import com.hippo.yorozuya.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MainActivity : EhActivity() {
    private lateinit var binding: ActivityMainBinding

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
        /*
        if (intent == null) {
            return false
        }
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            val uri = intent.data ?: return false
            val announcer = EhUrlOpener.parseUrl(uri.toString())
            if (announcer != null) {
                // startScene(announcer)
                return true
            }
        } else if (Intent.ACTION_SEND == action) {
            val type = intent.type
            if ("text/plain" == type) {
                val builder = ListUrlBuilder()
                builder.keyword = intent.getStringExtra(Intent.EXTRA_TEXT)
                // startScene(GalleryListScene.getStartAnnouncer(builder))
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
                        // startScene(GalleryListScene.getStartAnnouncer(builder))
                        return true
                    }
                }
            }
        }

         */
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController
        navController.apply {
            graph = navInflater.inflate(R.navigation.nav_graph).apply {
                when (Settings.getLaunchPageGalleryListSceneAction()) {
                    GalleryListScene.ACTION_HOMEPAGE -> setStartDestination(R.id.nav_homepage)
                    GalleryListScene.ACTION_SUBSCRIPTION -> setStartDestination(R.id.nav_subscription)
                    GalleryListScene.ACTION_WHATS_HOT -> setStartDestination(R.id.nav_whats_hot)
                    GalleryListScene.ACTION_TOP_LIST -> setStartDestination(R.id.nav_toplist)
                }
            }
        }
        if (EhUtils.needSignedIn()) {
            navController.navigate(R.id.signInScene)
        }

        binding.navView.setupWithNavController(navController)
        binding.drawView.addDrawerListener(mDrawerOnBackPressedCallback)
        onBackPressedDispatcher.addCallback(mDrawerOnBackPressedCallback)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Snackbar.make(
                    binding.drawView,
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

    override fun onResume() {
        super.onResume()
        checkClipboardUrl()
    }

    private fun checkClipboardUrl() {
        // launch checkClipboardUrlInternal()
    }
    /*

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

     */

    private fun checkClipboardUrlInternal() {
        val text = this.getClipboardManager().getUrlFromClipboard(this)
        val hashCode = text?.hashCode() ?: 0
        if (text != null && hashCode != 0 && Settings.getClipboardTextHashCode() != hashCode) {
            /*
            val announcer = createAnnouncerFromClipboardUrl(text)
            if (announcer != null) {
                val snackbar = Snackbar.make(
                    binding.drawView,
                    R.string.clipboard_gallery_url_snack_message,
                    Snackbar.LENGTH_SHORT
                )
                snackbar.setAction(R.string.clipboard_gallery_url_snack_action) {
                    // startScene(announcer)
                }
                snackbar.show()
            }

             */
        }
        Settings.putClipboardTextHashCode(hashCode)
    }

    @SuppressLint("RtlHardcoded")
    fun createDrawerView(scene: Fragment?) {
        if (scene is BaseScene) {
            binding.rightDrawer.removeAllViews()
            val drawerView = scene.createDrawerView(
                scene.layoutInflater, binding.rightDrawer, null
            )
            if (drawerView != null) {
                binding.rightDrawer.addView(drawerView)
                binding.drawView.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNLOCKED,
                    GravityCompat.END
                )
            } else {
                binding.drawView.setDrawerLockMode(
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
        binding.drawView.setDrawerLockMode(lockMode, edgeGravity)
    }

    fun getDrawerLockMode(edgeGravity: Int): Int {
        return binding.drawView.getDrawerLockMode(edgeGravity)
    }

    fun openDrawer(drawerGravity: Int) {
        binding.drawView.openDrawer(drawerGravity)
    }

    fun closeDrawer(drawerGravity: Int) {
        binding.drawView.closeDrawer(drawerGravity)
    }

    fun toggleDrawer(drawerGravity: Int) {
        binding.drawView.run {
            if (isDrawerOpen(drawerGravity)) {
                closeDrawer(drawerGravity)
            } else {
                openDrawer(drawerGravity)
            }
        }
    }

    fun setNavCheckedItem(@IdRes resId: Int) {
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

    var mShareUrl: String? = null
    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        mShareUrl?.let { outContent?.webUri = Uri.parse(mShareUrl) }
    }

    private val mDrawerOnBackPressedCallback =
        object : OnBackPressedCallback(false), DrawerListener {
            val slideThreshold = 0.05
            override fun handleOnBackPressed() {
                binding.drawView.closeDrawers()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                isEnabled = slideOffset > slideThreshold
            }

            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        }
}