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
import android.graphics.ImageDecoder
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.view.GravityCompat
import androidx.customview.widget.Openable
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.onNavDestinationSelected2
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.Settings.launchPage
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.databinding.ActivityMainBinding
import com.hippo.ehviewer.download.DownloadService
import com.hippo.ehviewer.download.downloadLocation
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.ui.scene.GalleryDetailScene
import com.hippo.ehviewer.ui.scene.GalleryListScene
import com.hippo.ehviewer.ui.scene.GalleryListScene.Companion.toStartArgs
import com.hippo.ehviewer.ui.scene.ProgressScene
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.getParcelableExtraCompat
import com.hippo.ehviewer.util.getUrlFromClipboard
import com.hippo.ehviewer.yorozuya.IOUtils
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.systemservices.clipboardManager
import splitties.systemservices.connectivityManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class MainActivity : EhActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val availableNetworks = mutableListOf<Network>()

    private fun saveImageToTempFile(uri: Uri): File? {
        val src = ImageDecoder.createSource(contentResolver, uri)
        val bitmap = runCatching {
            ImageDecoder.decodeBitmap(src, Image.imageSearchDecoderSampleListener)
        }.getOrNull() ?: return null
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        lifecycleScope.launchUI {
            if (!handleIntent(intent)) {
                if (intent != null && Intent.ACTION_VIEW == intent.action) {
                    if (intent.data != null) {
                        val url = intent.data.toString()
                        EditTextDialogBuilder(this@MainActivity, url, "")
                            .setTitle(R.string.error_cannot_parse_the_url)
                            .setPositiveButton(android.R.string.copy) { _: DialogInterface?, _: Int ->
                                this@MainActivity.addTextToClipboard(
                                    url,
                                    false,
                                )
                            }
                            .show()
                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?): Boolean {
        intent ?: return false
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            val uri = intent.data ?: return false
            return navController.navWithUrl(uri.toString())
        } else if (Intent.ACTION_SEND == action) {
            val type = intent.type
            if ("text/plain" == type) {
                val builder = ListUrlBuilder()
                builder.keyword = intent.getStringExtra(Intent.EXTRA_TEXT)
                navController.navigate(
                    R.id.galleryListScene,
                    builder.toStartArgs(),
                )
                return true
            } else if (type != null && type.startsWith("image/")) {
                val uri = intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
                if (null != uri) {
                    val temp = saveImageToTempFile(uri)
                    if (null != temp) {
                        val builder = ListUrlBuilder()
                        builder.mode = ListUrlBuilder.MODE_IMAGE_SEARCH
                        builder.imagePath = temp.path
                        builder.isUseSimilarityScan = true
                        navController.navigate(
                            R.id.galleryListScene,
                            builder.toStartArgs(),
                        )
                        return true
                    }
                }
            }
        } else if (action == DownloadService.ACTION_START_DOWNLOADSCENE) {
            val args = intent.getBundleExtra(DownloadService.ACTION_START_DOWNLOADSCENE_ARGS)
            navController.navigate(R.id.nav_downloads, args)
        }

        return false
    }

    private fun setNavGraph() {
        navController.apply {
            graph = navInflater.inflate(R.navigation.nav_graph).apply {
                when (
                    when (val value = launchPage) {
                        0 -> GalleryListScene.ACTION_HOMEPAGE
                        1 -> GalleryListScene.ACTION_SUBSCRIPTION
                        2 -> GalleryListScene.ACTION_WHATS_HOT
                        3 -> GalleryListScene.ACTION_TOP_LIST
                        else -> throw IllegalStateException("Unexpected value: $value")
                    }
                ) {
                    GalleryListScene.ACTION_HOMEPAGE -> setStartDestination(R.id.nav_homepage)
                    GalleryListScene.ACTION_SUBSCRIPTION -> setStartDestination(R.id.nav_subscription)
                    GalleryListScene.ACTION_WHATS_HOT -> setStartDestination(R.id.nav_whats_hot)
                    GalleryListScene.ACTION_TOP_LIST -> setStartDestination(R.id.nav_toplist)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        if (!EhUtils.needSignedIn()) setNavGraph()
        binding.navView.setupWithNavController(navController)

        // Trick: Tweak NavigationUI to disable multiple backstack
        binding.navView.setNavigationItemSelectedListener {
            val navigationView = binding.navView
            val handled = onNavDestinationSelected2(it, navController)
            if (handled) {
                val parent = navigationView.parent
                if (parent is Openable) {
                    parent.close()
                } else {
                    val bottomSheetBehavior = NavigationUI.findBottomSheetBehavior(navigationView)
                    if (bottomSheetBehavior != null) {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }
            }
            handled
        }
        // Trick End

        if (savedInstanceState == null) {
            if (intent.action != Intent.ACTION_MAIN) {
                onNewIntent(intent)
            }
            checkDownloadLocation()
            if (Settings.meteredNetworkWarning) {
                checkMeteredNetwork()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!Settings.appLinkVerifyTip) {
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
                            Uri.parse("package:$packageName"),
                        )
                        startActivity(intent)
                    } catch (t: Throwable) {
                        val intent = Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:$packageName"),
                        )
                        startActivity(intent)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.dont_show_again) { _: DialogInterface?, _: Int ->
                    Settings.appLinkVerifyTip = true
                }
                .show()
        }
    }

    private fun checkDownloadLocation() {
        val uniFile = downloadLocation
        // null == uniFile for first start
        if (uniFile.ensureDir()) {
            return
        }
        BaseDialogBuilder(this)
            .setTitle(R.string.waring)
            .setMessage(R.string.invalid_download_location)
            .setPositiveButton(R.string.get_it, null)
            .show()
    }

    private fun checkMeteredNetwork() {
        if (connectivityManager.isActiveNetworkMetered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Snackbar.make(
                    binding.drawView,
                    R.string.metered_network_warning,
                    Snackbar.LENGTH_LONG,
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

    private val loginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (availableNetworks.isNotEmpty()) {
                connectivityManager.bindProcessToNetwork(availableNetworks.last())
            }
            setNavGraph()
        }

    override fun onResume() {
        if (EhUtils.needSignedIn()) {
            connectivityManager.bindProcessToNetwork(null)
            loginLauncher.launch(Intent(this, ConfigureActivity::class.java))
        }
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            checkClipboardUrl()
        }
    }

    private suspend fun checkClipboardUrl() {
        val text = clipboardManager.getUrlFromClipboard(this)
        val hashCode = text?.hashCode() ?: 0
        if (text != null && hashCode != 0 && Settings.clipboardTextHashCode != hashCode) {
            val result1 = GalleryDetailUrlParser.parse(text, false)
            var launch: (() -> Unit)? = null
            if (result1 != null) {
                val args = Bundle()
                args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN)
                args.putLong(GalleryDetailScene.KEY_GID, result1.gid)
                args.putString(GalleryDetailScene.KEY_TOKEN, result1.token)
                launch = { navController.navigate(R.id.galleryDetailScene, args) }
            }
            val result2 = GalleryPageUrlParser.parse(text, false)
            if (result2 != null) {
                val args = Bundle()
                args.putString(ProgressScene.KEY_ACTION, ProgressScene.ACTION_GALLERY_TOKEN)
                args.putLong(ProgressScene.KEY_GID, result2.gid)
                args.putString(ProgressScene.KEY_PTOKEN, result2.pToken)
                args.putInt(ProgressScene.KEY_PAGE, result2.page)
                launch = { navController.navigate(R.id.progressScene, args) }
            }
            launch?.let {
                withUIContext {
                    val snackbar = Snackbar.make(
                        binding.drawView,
                        R.string.clipboard_gallery_url_snack_message,
                        Snackbar.LENGTH_SHORT,
                    )
                    snackbar.setAction(R.string.clipboard_gallery_url_snack_action) {
                        it()
                    }
                    snackbar.show()
                }
            }
        }
        Settings.clipboardTextHashCode = hashCode
    }

    @SuppressLint("RtlHardcoded")
    fun createDrawerView(scene: Fragment?) {
        if (scene is BaseScene) {
            binding.rightDrawer.removeAllViews()
            val drawerView = scene.createDrawerView(
                scene.layoutInflater,
                binding.rightDrawer,
                null,
            )
            if (drawerView != null) {
                binding.rightDrawer.addView(drawerView)
                binding.drawView.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNLOCKED,
                    GravityCompat.END,
                )
            } else {
                binding.drawView.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    GravityCompat.END,
                )
            }
        }
    }

    fun addAboveSnackView(view: View?) {
        binding.absnacker.addAboveSnackView(view)
    }

    fun removeAboveSnackView(view: View?) {
        binding.absnacker.removeAboveSnackView(view)
    }

    fun setDrawerLockMode(lockMode: Int, edgeGravity: Int) {
        binding.drawView.setDrawerLockMode(lockMode, edgeGravity)
    }

    fun getDrawerLockMode(edgeGravity: Int): Int {
        return binding.drawView.getDrawerLockMode(edgeGravity)
    }

    fun isDrawerOpen(drawerGravity: Int) = binding.drawView.isDrawerOpen(drawerGravity)

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
                this,
                message,
                if (length == BaseScene.LENGTH_LONG) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT,
            ).show()
        } ?: Toast.makeText(
            this,
            message,
            if (length == BaseScene.LENGTH_LONG) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
        ).show()
    }

    var mShareUrl: String? = null
    override fun onProvideAssistContent(outContent: AssistContent?) {
        super.onProvideAssistContent(outContent)
        mShareUrl?.let { outContent?.webUri = Uri.parse(mShareUrl) }
    }
}
