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
import android.content.pm.PackageManager
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
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.databinding.ReaderActivityBinding
import com.hippo.ehviewer.gallery.ArchiveGalleryProvider
import com.hippo.ehviewer.gallery.DirGalleryProvider
import com.hippo.ehviewer.gallery.EhGalleryProvider
import com.hippo.ehviewer.gallery.GalleryProvider2
import com.hippo.ehviewer.ui.EhActivity
import com.hippo.unifile.UniFile
import com.hippo.util.ExceptionUtils
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.IOUtils
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.core.preference.AndroidPreferenceStore
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.util.system.applySystemAnimatorScale
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import eu.kanade.tachiyomi.util.system.isNightMode
import eu.kanade.tachiyomi.util.view.copy
import eu.kanade.tachiyomi.widget.listener.SimpleAnimationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
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
    private var mGalleryProvider: GalleryProvider2? = null
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
                mGalleryProvider = DirGalleryProvider(UniFile.fromFile(File(mFilename!!))!!)
            }
        } else if (ACTION_EH == mAction) {
            if (mGalleryInfo != null) {
                mGalleryProvider = EhGalleryProvider(this, mGalleryInfo)
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

                mGalleryProvider = ArchiveGalleryProvider(this, mUri)
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
        ArchiveGalleryProvider.showPasswd = ShowPasswdDialogHandler(this)
        mGalleryProvider!!.start()

        val viewerMode =
            ReadingModeType.fromPreference(readerPreferences.defaultReadingMode().get())
        binding.actionReadingMode.setImageResource(viewerMode.iconRes)
        viewer = ReadingModeType.toViewer(readerPreferences.defaultReadingMode().get(), this)
        updateViewerInset(readerPreferences.fullscreen().get())
        binding.viewerContainer.addView(viewer?.getView())
        viewer?.setGalleryProvider(mGalleryProvider!!)

        // Get start page
        val startPage: Int = if (savedInstanceState == null) {
            if (mPage >= 0) mPage else mGalleryProvider!!.startPage
        } else {
            mCurrentIndex
        }

        mSize = mGalleryProvider!!.size()
        mCurrentIndex = startPage
        initializeMenu()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewer?.destroy()
        viewer = null
        if (mGalleryProvider != null) {
            mGalleryProvider!!.stop()
            mGalleryProvider = null
        }
    }

    private fun shareImage(page: Int) {
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

    private fun saveImage(page: Int) {
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

    private fun saveImageTo(page: Int) {
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
            ArchiveGalleryProvider.passwd = passwd
            ArchiveGalleryProvider.pv.v()
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

    private fun setCutoutShort(enabled: Boolean) {
        window.attributes.layoutInDisplayCutoutMode = when (enabled) {
            true -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            false -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
        }

        // Trigger relayout
        setMenuVisibility(menuVisible)
    }

    private val readerPreferences by lazy { ReaderPreferences(AndroidPreferenceStore(this)) }

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
                val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_top)
                toolbarAnimation.applySystemAnimatorScale(this)
                toolbarAnimation.setAnimationListener(
                    object : SimpleAnimationListener() {
                        override fun onAnimationStart(animation: Animation) {
                            // Fix status bar being translucent the first time it's opened.
                            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                        }
                    },
                )
                binding.toolbar.startAnimation(toolbarAnimation)

                val bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom)
                bottomAnimation.applySystemAnimatorScale(this)
                binding.readerMenuBottom.startAnimation(bottomAnimation)
            }
            /*

            if (readerPreferences.showPageNumber().get()) {
                config?.setPageNumberVisibility(false)
            }

             */
        } else {
            if (readerPreferences.fullscreen().get()) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            if (animate) {
                val toolbarAnimation = AnimationUtils.loadAnimation(this, R.anim.exit_to_top)
                toolbarAnimation.applySystemAnimatorScale(this)
                toolbarAnimation.setAnimationListener(
                    object : SimpleAnimationListener() {
                        override fun onAnimationEnd(animation: Animation) {
                            binding.readerMenu.isVisible = false
                        }
                    },
                )
                binding.toolbar.startAnimation(toolbarAnimation)

                val bottomAnimation = AnimationUtils.loadAnimation(this, R.anim.exit_to_bottom)
                bottomAnimation.applySystemAnimatorScale(this)
                binding.readerMenuBottom.startAnimation(bottomAnimation)
            }
            /*

            if (readerPreferences.showPageNumber().get()) {
                config?.setPageNumberVisibility(true)
            }

             */
        }
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
     * Updates viewer inset depending on fullscreen reader preferences.
     */
    fun updateViewerInset(fullscreen: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, !fullscreen)
        setCutoutShort(true)
        viewer?.getView()?.applyInsetter {
            if (!fullscreen) {
                type(navigationBars = true, statusBars = true) {
                    padding()
                }
            }
        }
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
    }

    /**
     * Called from the viewer whenever a [page] is long clicked. A bottom sheet with a list of
     * actions to perform is shown.
     */
    fun onPageLongTap(page: ReaderPage) {
    }

    /**
     * Moves the viewer to the given page [index]. It does nothing if the viewer is null or the
     * page is not found.
     */
    fun moveToPageIndex(index: Int) {
        val viewer = viewer ?: return
        val page = mGalleryProvider?.mPages?.getOrNull(index) ?: return
        viewer.moveToPage(page)
    }

    /**
     * Initializes the reader menu. It sets up click listeners and the initial visibility.
     */
    @SuppressLint("PrivateResource")
    private fun initializeMenu() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.toolbar.applyInsetter {
            type(navigationBars = true, statusBars = true) {
                margin(top = true, horizontal = true)
            }
        }
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

        // initBottomShortcuts()

        val toolbarBackground = (binding.toolbar.background as MaterialShapeDrawable).apply {
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
}
