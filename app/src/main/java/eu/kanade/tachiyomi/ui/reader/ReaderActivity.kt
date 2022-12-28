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

package eu.kanade.tachiyomi.ui.reader

import android.Manifest
import android.annotation.SuppressLint
import android.app.assist.AssistContent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.text.TextUtils
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View.LAYER_TYPE_HARDWARE
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.slider.Slider
import com.google.android.material.transition.MaterialContainerTransform
import com.hippo.app.EditTextDialogBuilder
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.databinding.ReaderActivityBinding
import com.hippo.ehviewer.gallery.ArchivePageLoader
import com.hippo.ehviewer.gallery.DirPageLoader
import com.hippo.ehviewer.gallery.EhPageLoader
import com.hippo.ehviewer.gallery.PageLoader2
import com.hippo.ehviewer.ui.EhActivity
import com.hippo.image.Image
import com.hippo.unifile.UniFile
import com.hippo.util.ExceptionUtils
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.IOUtils
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsSheet
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.util.preference.toggle
import eu.kanade.tachiyomi.util.system.applySystemAnimatorScale
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import eu.kanade.tachiyomi.util.system.isNightMode
import eu.kanade.tachiyomi.util.view.copy
import eu.kanade.tachiyomi.util.view.popupMenu
import eu.kanade.tachiyomi.util.view.setTooltip
import eu.kanade.tachiyomi.widget.listener.SimpleAnimationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max

class ReaderActivity : EhActivity() {
    lateinit var binding: ReaderActivityBinding
    private var mAction: String? = null
    private var mFilename: String? = null
    private var mUri: Uri? = null
    private var mGalleryInfo: GalleryInfo? = null
    private var mPage: Int = 0
    private var mCacheFileName: String? = null

    /**
     * Whether the menu is currently visible.
     */
    var menuVisible = false
        private set

    private var saveImageToLauncher = registerForActivityResult(
        CreateDocument("todo/todo")
    ) { uri ->
        if (uri != null) {
            val filepath =
                AppConfig.getExternalTempDir().toString() + File.separator + mCacheFileName
            val cachefile = File(filepath)
            val resolver = contentResolver
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    var `is`: InputStream? = null
                    var os: OutputStream? = null
                    try {
                        `is` = FileInputStream(cachefile)
                        os = resolver.openOutputStream(uri)
                        IOUtils.copy(`is`, os)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        IOUtils.closeQuietly(`is`)
                        IOUtils.closeQuietly(os)
                        runOnUiThread {
                            Toast.makeText(
                                this@ReaderActivity,
                                getString(R.string.image_saved, uri.path),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    cachefile.delete()
                }
            }
        }
    }
    var mGalleryProvider: PageLoader2? = null
    private var mSize: Int = 0
    private var mCurrentIndex: Int = 0
    private var mSavingPage = -1
    private var builder: EditTextDialogBuilder? = null
    private var dialogShown = false
    private var dialog: AlertDialog? = null
    private var requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result!! && mSavingPage != -1) {
            saveImage(mSavingPage)
        } else {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show()
        }
        mSavingPage = -1
    }

    private val galleryDetailUrl: String?
        get() {
            val gid: Long
            val token: String
            if (mGalleryInfo != null) {
                gid = mGalleryInfo!!.gid
                token = mGalleryInfo!!.token
            } else {
                return null
            }
            return EhUrl.getGalleryDetailUrl(gid, token, 0, false)
        }

    private fun buildProvider() {
        if (mGalleryProvider != null) {
            return
        }

        if (ACTION_DIR == mAction) {
            if (mFilename != null) {
                mGalleryProvider = DirPageLoader(
                    UniFile.fromFile(File(mFilename!!))!!
                )
            }
        } else if (ACTION_EH == mAction) {
            if (mGalleryInfo != null) {
                mGalleryProvider =
                    EhPageLoader(mGalleryInfo)
            }
        } else if (Intent.ACTION_VIEW == mAction) {
            if (mUri != null) {
                try {
                    grantUriPermission(
                        BuildConfig.APPLICATION_ID,
                        mUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.error_reading_failed, Toast.LENGTH_SHORT).show()
                }

                mGalleryProvider =
                    ArchivePageLoader(this, mUri)
            }
        }
    }

    private fun onInit() {
        val intent = intent ?: return
        mAction = intent.action
        mFilename = intent.getStringExtra(KEY_FILENAME)
        mUri = intent.data
        mGalleryInfo = intent.getParcelableExtra(KEY_GALLERY_INFO)
        mPage = intent.getIntExtra(KEY_PAGE, -1)
        buildProvider()
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mAction = savedInstanceState.getString(KEY_ACTION)
        mFilename = savedInstanceState.getString(KEY_FILENAME)
        mUri = savedInstanceState.getParcelable(KEY_URI)
        mGalleryInfo = savedInstanceState.getParcelable(KEY_GALLERY_INFO)
        mPage = savedInstanceState.getInt(KEY_PAGE, -1)
        mCurrentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX)
        buildProvider()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ACTION, mAction)
        outState.putString(KEY_FILENAME, mFilename)
        outState.putParcelable(KEY_URI, mUri)
        if (mGalleryInfo != null) {
            outState.putParcelable(KEY_GALLERY_INFO, mGalleryInfo)
        }
        outState.putInt(KEY_PAGE, mPage)
        outState.putInt(KEY_CURRENT_INDEX, mCurrentIndex)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.colorMode = if (Image.isWideColorGamut && readerPreferences.wideColorGamut().get()) ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT else ActivityInfo.COLOR_MODE_DEFAULT
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
        binding = ReaderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        builder = EditTextDialogBuilder(this, null, getString(R.string.archive_passwd))
        builder!!.setTitle(getString(R.string.archive_need_passwd))
        builder!!.setPositiveButton(getString(android.R.string.ok), null)
        dialog = builder!!.create()
        dialog!!.setCanceledOnTouchOutside(false)
        if (mGalleryProvider == null) {
            finish()
            return
        }
        ArchivePageLoader.showPasswd = ShowPasswdDialogHandler(this)

        mGalleryProvider!!.start()

        // Get start page
        if (savedInstanceState == null) {
            mCurrentIndex = if (mPage >= 0) mPage else mGalleryProvider!!.startPage
        }

        lifecycleScope.launch(Dispatchers.Main) {
            mGalleryProvider!!.state.collect {
                if (it == PageLoader.STATE_READY) {
                    setGallery()
                    cancel()
                }
            }
        }

        config = ReaderConfig()
        initializeMenu()
    }

    fun setGallery() {
        mSize = mGalleryProvider!!.size()
        val viewerMode =
            ReadingModeType.fromPreference(readerPreferences.defaultReadingMode().get())
        binding.actionReadingMode.setImageResource(viewerMode.iconRes)
        viewer?.destroy()
        viewer = ReadingModeType.toViewer(readerPreferences.defaultReadingMode().get(), this)
        binding.pageSlider.isRTL = viewer is R2LPagerViewer
        updateViewerInset(readerPreferences.fullscreen().get())
        binding.viewerContainer.removeAllViews()
        setOrientation(readerPreferences.defaultOrientationType().get())
        binding.viewerContainer.addView(viewer?.getView())
        viewer?.setGalleryProvider(mGalleryProvider!!)
        moveToPageIndex(mCurrentIndex)
    }

    override fun onDestroy() {
        super.onDestroy()
        config = null
        viewer?.destroy()
        viewer = null
        if (mGalleryProvider != null) {
            mGalleryProvider!!.stop()
            mGalleryProvider = null
        }
    }

    fun shareImage(page: Int) {
        if (null == mGalleryProvider) {
            return
        }

        val dir = AppConfig.getExternalTempDir()
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_create_temp_file, Toast.LENGTH_SHORT).show()
            return
        }
        val file = mGalleryProvider!!.save(
            page,
            UniFile.fromFile(dir)!!,
            mGalleryProvider!!.getImageFilename(page)
        )
        if (file == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show()
            return
        }
        val filename = file.name
        if (filename == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show()
            return
        }

        var mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            MimeTypeMap.getFileExtensionFromUrl(filename)
        )
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "image/jpeg"
        }

        val uri = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            File(dir, filename)
        )

        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        if (mGalleryInfo != null)
            intent.putExtra(
                Intent.EXTRA_TEXT,
                EhUrl.getGalleryDetailUrl(mGalleryInfo!!.gid, mGalleryInfo!!.token)
            )
        intent.setDataAndType(uri, mimeType)

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_image)))
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            Toast.makeText(this, R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show()
        }

    }

    private fun copyImage(page: Int) {
        if (null == mGalleryProvider) {
            return
        }

        val dir = AppConfig.getExternalCopyTempDir()
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_create_temp_file, Toast.LENGTH_SHORT).show()
            return
        }
        val file = mGalleryProvider!!.save(
            page,
            UniFile.fromFile(dir)!!,
            mGalleryProvider!!.getImageFilename(page)
        )
        if (file == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show()
            return
        }
        val filename = file.name
        if (filename == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            File(dir, filename)
        )

        val clipboardManager = getSystemService(ClipboardManager::class.java)
        if (clipboardManager != null) {
            val clipData = ClipData.newUri(contentResolver, "ehviewer", uri)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }
    }

    fun saveImage(page: Int) {
        if (null == mGalleryProvider) {
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            mSavingPage = page
            requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }

        val filename = mGalleryProvider!!.getImageFilenameWithExtension(page)
        var mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            MimeTypeMap.getFileExtensionFromUrl(filename)
        )
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "image/jpeg"
        }

        val realPath: String
        val resolver = contentResolver
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + File.separator + AppConfig.APP_DIRNAME
            )
            values.put(MediaStore.MediaColumns.IS_PENDING, 1)
            realPath = Environment.DIRECTORY_PICTURES + File.separator + AppConfig.APP_DIRNAME
        } else {
            val path = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                AppConfig.APP_DIRNAME
            )
            realPath = path.toString()
            if (!FileUtils.ensureDirectory(path)) {
                Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show()
                return
            }
            values.put(MediaStore.MediaColumns.DATA, path.toString() + File.separator + filename)
        }
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (imageUri == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show()
            return
        }
        if (!mGalleryProvider!!.save(page, UniFile.fromMediaUri(this, imageUri))) {
            try {
                resolver.delete(imageUri, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show()
            return
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
        }

        Toast.makeText(
            this,
            getString(R.string.image_saved, realPath + File.separator + filename),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun saveImageTo(page: Int) {
        if (null == mGalleryProvider) {
            return
        }
        val dir = AppConfig.getExternalTempDir()
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_create_temp_file, Toast.LENGTH_SHORT).show()
            return
        }
        val file = mGalleryProvider!!.save(
            page,
            UniFile.fromFile(dir)!!,
            mGalleryProvider!!.getImageFilename(page)
        )
        if (file == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show()
            return
        }
        val filename = file.name
        if (filename == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show()
            return
        }
        mCacheFileName = filename
        try {
            saveImageToLauncher.launch(filename)
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            Toast.makeText(this, R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show()
        }

    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)

        val url = galleryDetailUrl
        if (url != null) {
            outContent.webUri = Uri.parse(url)
        }
    }

    private fun showPasswdDialog() {
        if (!dialogShown) {
            dialogShown = true
            dialog!!.show()
            if (dialog!!.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog!!.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener { v -> this@ReaderActivity.onProvidePasswd() }
            }
            dialog!!.setOnCancelListener { v -> finish() }
        }
    }

    private fun onProvidePasswd() {
        val passwd = builder!!.text
        if (passwd.isEmpty())
            builder!!.setError(getString(R.string.passwd_cannot_be_empty))
        else {
            ArchivePageLoader.passwd = passwd
            ArchivePageLoader.pv.v()
        }
    }

    private fun onPasswdWrong() {
        builder!!.setError(getString(R.string.passwd_wrong))
    }

    private fun onPasswdCorrect() {
        dialog!!.dismiss()
    }

    private class ShowPasswdDialogHandler(activity: ReaderActivity) : Handler() {
        private val weakReference: WeakReference<ReaderActivity>

        init {
            this.weakReference = WeakReference(activity)
        }

        override fun handleMessage(msg: Message) {
            val activity = weakReference.get()
            super.handleMessage(msg)
            if (null != activity) {
                when (msg.what) {
                    0 -> activity.showPasswdDialog()
                    1 -> activity.onPasswdWrong()
                    2 -> activity.onPasswdCorrect()
                }
            }
        }
    }

    companion object {
        const val ACTION_DIR = "dir"
        const val ACTION_EH = "eh"
        const val KEY_ACTION = "action"
        const val KEY_FILENAME = "filename"
        const val KEY_URI = "uri"
        const val KEY_GALLERY_INFO = "gallery_info"
        const val KEY_PAGE = "page"
        const val KEY_CURRENT_INDEX = "current_index"

        val readerPreferences = EhApplication.readerPreferences
    }

    /* Tachiyomi funcs */

    var isScrollingThroughPages = false
        private set

    /**
     * Viewer used to display the pages (pager, webtoon, ...).
     */
    var viewer: BaseViewer? = null
        private set

    val hasCutout by lazy { hasDisplayCutout() }

    private var config: ReaderConfig? = null

    private val windowInsetsController by lazy {
        WindowInsetsControllerCompat(
            window,
            binding.root
        )
    }

    /**
     * Sets the visibility of the menu according to [visible] and with an optional parameter to
     * [animate] the views.
     */
    fun setMenuVisibility(visible: Boolean, animate: Boolean = true) {
        menuVisible = visible
        if (visible) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            binding.readerMenu.isVisible = true

            if (animate) {
                val bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom)
                bottomAnimation.applySystemAnimatorScale(this)
                binding.readerMenuBottom.startAnimation(bottomAnimation)
            }

            if (readerPreferences.showPageNumber().get()) {
                config?.setPageNumberVisibility(false)
            }
        } else {
            if (readerPreferences.fullscreen().get()) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            if (animate) {
                val bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.exit_to_bottom)
                bottomAnimation.applySystemAnimatorScale(this)
                bottomAnimation.setAnimationListener(
                    object : SimpleAnimationListener() {
                        override fun onAnimationEnd(animation: Animation) {
                            binding.readerMenu.isVisible = false
                        }
                    },
                )
                binding.readerMenuBottom.startAnimation(bottomAnimation)
            }

            if (readerPreferences.showPageNumber().get()) {
                config?.setPageNumberVisibility(true)
            }
        }
    }

    /**
     * Initializes the reader menu. It sets up click listeners and the initial visibility.
     */
    @SuppressLint("PrivateResource")
    private fun initializeMenu() {
        binding.readerMenuBottom.applyInsetter {
            type(navigationBars = true) {
                margin(bottom = true, horizontal = true)
            }
        }

        // Init listeners on bottom menu
        binding.pageSlider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    isScrollingThroughPages = true
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    isScrollingThroughPages = false
                }
            },
        )
        binding.pageSlider.addOnChangeListener { slider, value, fromUser ->
            if (viewer != null && fromUser) {
                isScrollingThroughPages = true
                moveToPageIndex(value.toInt())
                slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }

        initBottomShortcuts()

        val toolbarBackground = MaterialShapeDrawable.createWithElevationOverlay(this).apply {
            elevation =
                resources.getDimension(com.google.android.material.R.dimen.m3_sys_elevation_level2)
            alpha = if (isNightMode()) 230 else 242 // 90% dark 95% light
        }
        binding.toolbarBottom.background = toolbarBackground.copy(this@ReaderActivity)

        binding.readerSeekbar.background = toolbarBackground.copy(this@ReaderActivity)?.apply {
            setCornerSize(999F)
        }

        val toolbarColor = ColorUtils.setAlphaComponent(
            toolbarBackground.resolvedTintColor,
            toolbarBackground.alpha,
        )

        window.statusBarColor = toolbarColor
        window.navigationBarColor = toolbarColor

        // Set initial visibility
        setMenuVisibility(menuVisible)
    }

    private fun initBottomShortcuts() {
        // Reading mode
        with(binding.actionReadingMode) {
            setTooltip(R.string.viewer)

            setOnClickListener {
                popupMenu(
                    items = ReadingModeType.values().map { it.flagValue to it.stringRes },
                    selectedItemId = readerPreferences.defaultReadingMode().get(),
                ) {
                    val newReadingMode = ReadingModeType.fromPreference(itemId)

                    readerPreferences.defaultReadingMode().set(newReadingMode.flagValue)
                    setGallery()

                    updateCropBordersShortcut()
                }
            }
        }

        // Crop borders
        with(binding.actionCropBorders) {
            setTooltip(R.string.pref_crop_borders)

            setOnClickListener {
                val isPagerType =
                    ReadingModeType.isPagerType(readerPreferences.defaultReadingMode().get())
                if (isPagerType) {
                    readerPreferences.cropBorders().toggle()
                } else {
                    readerPreferences.cropBordersWebtoon().toggle()
                }
            }
        }
        updateCropBordersShortcut()
        listOf(readerPreferences.cropBorders(), readerPreferences.cropBordersWebtoon())
            .forEach { pref ->
                pref.changes()
                    .onEach { updateCropBordersShortcut() }
                    .launchIn(lifecycleScope)
            }

        // Rotation
        with(binding.actionRotation) {
            setTooltip(R.string.rotation_type)

            setOnClickListener {
                popupMenu(
                    items = OrientationType.values().map { it.flagValue to it.stringRes },
                    selectedItemId = readerPreferences.defaultOrientationType().get(),
                ) {
                    val newOrientation = OrientationType.fromPreference(itemId)

                    readerPreferences.defaultOrientationType().set(newOrientation.flagValue)
                    setGallery()
                }
            }
        }

        // Settings sheet
        with(binding.actionSettings) {
            setTooltip(R.string.action_settings)
            val readerSettingSheetDialog = ReaderSettingsSheet(this@ReaderActivity)
            setOnClickListener {
                if (!readerSettingSheetDialog.isShowing) {
                    readerSettingSheetDialog.show()
                }
            }

            setOnLongClickListener {
                ReaderSettingsSheet(this@ReaderActivity, showColorFilterSettings = true).show()
                true
            }
        }
    }

    private fun updateOrientationShortcut(preference: Int) {
        val orientation = OrientationType.fromPreference(preference)
        binding.actionRotation.setImageResource(orientation.iconRes)
    }

    private fun updateCropBordersShortcut() {
        val isPagerType = ReadingModeType.isPagerType(readerPreferences.defaultReadingMode().get())
        val enabled = if (isPagerType) {
            readerPreferences.cropBorders().get()
        } else {
            readerPreferences.cropBordersWebtoon().get()
        }

        binding.actionCropBorders.setImageResource(
            if (enabled) {
                R.drawable.ic_crop_24dp
            } else {
                R.drawable.ic_crop_off_24dp
            },
        )
    }


    /**
     * Dispatches a key event. If the viewer doesn't handle it, call the default implementation.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = viewer?.handleKeyEvent(event) ?: false
        return handled || super.dispatchKeyEvent(event)
    }

    /**
     * Dispatches a generic motion event. If the viewer doesn't handle it, call the default
     * implementation.
     */
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = viewer?.handleGenericMotionEvent(event) ?: false
        return handled || super.dispatchGenericMotionEvent(event)
    }

    private fun buildContainerTransform(entering: Boolean): MaterialContainerTransform {
        return MaterialContainerTransform(this, entering).apply {
            duration = 350 // ms
            addTarget(android.R.id.content)
        }
    }

    /**
     * Moves the viewer to the given page [index]. It does nothing if the viewer is null or the
     * page is not found.
     */
    private fun moveToPageIndex(index: Int) {
        val viewer = viewer ?: return
        val page = mGalleryProvider?.mPages?.getOrNull(index) ?: return
        viewer.moveToPage(page)
    }

    /**
     * Called from the viewer whenever a [page] is marked as active. It updates the values of the
     * bottom menu and delegates the change to the presenter.
     */
    @SuppressLint("SetTextI18n")
    fun onPageSelected(page: ReaderPage) {
        val pages = mGalleryProvider?.mPages ?: return

        // Set bottom page number
        binding.pageNumber.text = "${page.number}/${pages.size}"

        // Set page numbers
        if (viewer !is R2LPagerViewer) {
            binding.leftPageText.text = "${page.number}"
            binding.rightPageText.text = "${pages.size}"
        } else {
            binding.rightPageText.text = "${page.number}"
            binding.leftPageText.text = "${pages.size}"
        }

        // Set slider progress
        binding.pageSlider.isEnabled = pages.size > 1
        binding.pageSlider.valueTo = max(pages.lastIndex.toFloat(), 1f)
        binding.pageSlider.value = page.index.toFloat()

        mCurrentIndex = page.index
        mGalleryProvider?.putStartPage(mCurrentIndex)
    }

    /**
     * Called from the viewer whenever a [page] is long clicked. A bottom sheet with a list of
     * actions to perform is shown.
     */
    fun onPageLongTap(page: ReaderPage) {
        ReaderPageSheet(this, page).show()
    }

    /**
     * Called from the viewer to toggle the visibility of the menu. It's implemented on the
     * viewer because each one implements its own touch and key events.
     */
    fun toggleMenu() {
        setMenuVisibility(!menuVisible)
    }

    /**
     * Called from the viewer to show the menu.
     */
    fun showMenu() {
        if (!menuVisible) {
            setMenuVisibility(true)
        }
    }

    /**
     * Called from the viewer to hide the menu.
     */
    fun hideMenu() {
        if (menuVisible) {
            setMenuVisibility(false)
        }
    }

    /**
     * Forces the user preferred [orientation] on the activity.
     */
    private fun setOrientation(orientation: Int) {
        val newOrientation = OrientationType.fromPreference(orientation)
        if (newOrientation.flag != requestedOrientation) {
            requestedOrientation = newOrientation.flag
        }
        updateOrientationShortcut(readerPreferences.defaultOrientationType().get())
    }

    /**
     * Updates viewer inset depending on fullscreen reader preferences.
     */
    fun updateViewerInset(fullscreen: Boolean) {
        viewer?.getView()?.applyInsetter {
            if (!fullscreen) {
                type(navigationBars = true, statusBars = true) {
                    padding()
                }
            }
        }
    }

    /**
     * Class that handles the user preferences of the reader.
     */
    private inner class ReaderConfig {

        private fun getCombinedPaint(grayscale: Boolean, invertedColors: Boolean): Paint {
            return Paint().apply {
                colorFilter = ColorMatrixColorFilter(
                    ColorMatrix().apply {
                        if (grayscale) {
                            setSaturation(0f)
                        }
                        if (invertedColors) {
                            postConcat(
                                ColorMatrix(
                                    floatArrayOf(
                                        -1f, 0f, 0f, 0f, 255f,
                                        0f, -1f, 0f, 0f, 255f,
                                        0f, 0f, -1f, 0f, 255f,
                                        0f, 0f, 0f, 1f, 0f,
                                    ),
                                ),
                            )
                        }
                    },
                )
            }
        }

        /**
         * Initializes the reader subscriptions.
         */
        init {
            readerPreferences.readerTheme().changes()
                .onEach { theme ->
                    binding.readerContainer.setBackgroundResource(
                        when (theme) {
                            0 -> android.R.color.white
                            2 -> R.color.reader_background_dark
                            3 -> automaticBackgroundColor()
                            else -> android.R.color.black
                        },
                    )
                }
                .launchIn(lifecycleScope)

            readerPreferences.showPageNumber().changes()
                .onEach { setPageNumberVisibility(it) }
                .launchIn(lifecycleScope)

            readerPreferences.trueColor().changes()
                .onEach { setTrueColor(it) }
                .launchIn(lifecycleScope)

            readerPreferences.cutoutShort().changes()
                .onEach { setCutoutShort(it) }
                .launchIn(lifecycleScope)

            readerPreferences.keepScreenOn().changes()
                .onEach { setKeepScreenOn(it) }
                .launchIn(lifecycleScope)

            readerPreferences.customBrightness().changes()
                .onEach { setCustomBrightness(it) }
                .launchIn(lifecycleScope)

            readerPreferences.colorFilter().changes()
                .onEach { setColorFilter(it) }
                .launchIn(lifecycleScope)

            readerPreferences.colorFilterMode().changes()
                .onEach { setColorFilter(readerPreferences.colorFilter().get()) }
                .launchIn(lifecycleScope)

            merge(
                readerPreferences.grayscale().changes(),
                readerPreferences.invertedColors().changes()
            )
                .onEach {
                    setLayerPaint(
                        readerPreferences.grayscale().get(),
                        readerPreferences.invertedColors().get()
                    )
                }
                .launchIn(lifecycleScope)

            readerPreferences.fullscreen().changes()
                .onEach {
                    WindowCompat.setDecorFitsSystemWindows(window, !it)
                    updateViewerInset(it)
                }
                .launchIn(lifecycleScope)
        }

        /**
         * Picks background color for [ReaderActivity] based on light/dark theme preference
         */
        private fun automaticBackgroundColor(): Int {
            return if (baseContext.isNightMode()) {
                R.color.reader_background_dark
            } else {
                android.R.color.white
            }
        }

        /**
         * Sets the visibility of the bottom page indicator according to [visible].
         */
        fun setPageNumberVisibility(visible: Boolean) {
            binding.pageNumber.isVisible = visible
        }

        /**
         * Sets the 32-bit color mode according to [enabled].
         */
        private fun setTrueColor(enabled: Boolean) {
            // TODO()
        }

        private fun setCutoutShort(enabled: Boolean) {
            window.attributes.layoutInDisplayCutoutMode = when (enabled) {
                true -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                false -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }

            // Trigger relayout
            setMenuVisibility(menuVisible)
        }

        /**
         * Sets the keep screen on mode according to [enabled].
         */
        private fun setKeepScreenOn(enabled: Boolean) {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        /**
         * Sets the custom brightness overlay according to [enabled].
         */
        @OptIn(FlowPreview::class)
        private fun setCustomBrightness(enabled: Boolean) {
            if (enabled) {
                readerPreferences.customBrightnessValue().changes()
                    .sample(100)
                    .onEach { setCustomBrightnessValue(it) }
                    .launchIn(lifecycleScope)
            } else {
                setCustomBrightnessValue(0)
            }
        }

        /**
         * Sets the color filter overlay according to [enabled].
         */
        @OptIn(FlowPreview::class)
        private fun setColorFilter(enabled: Boolean) {
            if (enabled) {
                readerPreferences.colorFilterValue().changes()
                    .sample(100)
                    .onEach { setColorFilterValue(it) }
                    .launchIn(lifecycleScope)
            } else {
                binding.colorOverlay.isVisible = false
            }
        }

        /**
         * Sets the brightness of the screen. Range is [-75, 100].
         * From -75 to -1 a semi-transparent black view is overlaid with the minimum brightness.
         * From 1 to 100 it sets that value as brightness.
         * 0 sets system brightness and hides the overlay.
         */
        private fun setCustomBrightnessValue(value: Int) {
            // Calculate and set reader brightness.
            val readerBrightness = when {
                value > 0 -> {
                    value / 100f
                }

                value < 0 -> {
                    0.01f
                }

                else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }

            window.attributes = window.attributes.apply { screenBrightness = readerBrightness }

            // Set black overlay visibility.
            if (value < 0) {
                binding.brightnessOverlay.isVisible = true
                val alpha = (abs(value) * 2.56).toInt()
                binding.brightnessOverlay.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
            } else {
                binding.brightnessOverlay.isVisible = false
            }
        }

        /**
         * Sets the color filter [value].
         */
        private fun setColorFilterValue(value: Int) {
            binding.colorOverlay.isVisible = true
            binding.colorOverlay.setFilterColor(value, readerPreferences.colorFilterMode().get())
        }

        private fun setLayerPaint(grayscale: Boolean, invertedColors: Boolean) {
            val paint = if (grayscale || invertedColors) getCombinedPaint(
                grayscale,
                invertedColors
            ) else null
            binding.viewerContainer.setLayerType(LAYER_TYPE_HARDWARE, paint)
        }
    }
}
