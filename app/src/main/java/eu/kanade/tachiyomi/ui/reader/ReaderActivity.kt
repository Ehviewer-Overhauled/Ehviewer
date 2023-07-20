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
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import android.provider.MediaStore
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.shape.MaterialShapeDrawable
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.databinding.ReaderActivityBinding
import com.hippo.ehviewer.gallery.ArchivePageLoader
import com.hippo.ehviewer.gallery.EhPageLoader
import com.hippo.ehviewer.gallery.PageLoader2
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.ui.EhActivity
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.util.AppConfig
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.FileUtils
import com.hippo.ehviewer.util.getParcelableCompat
import com.hippo.ehviewer.util.getParcelableExtraCompat
import com.hippo.ehviewer.util.sendTo
import com.hippo.unifile.UniFile
import dev.chrisbanes.insetter.applyInsetter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsSheet
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.BaseViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.applySystemAnimatorScale
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import eu.kanade.tachiyomi.util.system.isNightMode
import eu.kanade.tachiyomi.util.view.copy
import eu.kanade.tachiyomi.util.view.popupMenu
import eu.kanade.tachiyomi.util.view.setTooltip
import eu.kanade.tachiyomi.widget.listener.SimpleAnimationListener
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.systemservices.clipboardManager
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.math.abs

class ReaderActivity : EhActivity() {
    lateinit var binding: ReaderActivityBinding
    private var mAction: String? = null
    private var mFilename: String? = null
    private var mUri: Uri? = null
    private var mGalleryInfo: BaseGalleryInfo? = null
    private var mPage: Int = 0
    private var mCacheFileName: String? = null

    /**
     * Whether the menu is currently visible.
     */
    var menuVisible = false
        private set

    private var saveImageToLauncher = registerForActivityResult(
        CreateDocument("todo/todo"),
    ) { uri ->
        if (uri != null) {
            val filepath = AppConfig.externalTempDir.toString() + File.separator + mCacheFileName
            val cachefile = File(filepath)
            lifecycleScope.launchIO {
                try {
                    ParcelFileDescriptor.open(cachefile, MODE_READ_ONLY).use { from ->
                        contentResolver.openFileDescriptor(uri, "w")!!.use {
                            from sendTo it
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    runOnUiThread {
                        Toast.makeText(
                            this@ReaderActivity,
                            getString(R.string.image_saved, uri.path),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
                cachefile.delete()
            }
        }
    }
    var mGalleryProvider: PageLoader2? = null
    private var mCurrentIndex: Int = 0
    private var mSavingPage = -1
    private lateinit var builder: EditTextDialogBuilder
    private var dialogShown = false
    private lateinit var dialog: AlertDialog
    private var requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
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
                token = mGalleryInfo!!.token!!
            } else {
                return null
            }
            return EhUrl.getGalleryDetailUrl(gid, token, 0, false)
        }

    private fun buildProvider(replace: Boolean = false) {
        if (mGalleryProvider != null) {
            if (replace) mGalleryProvider!!.stop() else return
        }

        if (ACTION_EH == mAction) {
            mGalleryInfo?.let { mGalleryProvider = EhPageLoader(it) }
        } else if (Intent.ACTION_VIEW == mAction) {
            if (mUri != null) {
                try {
                    grantUriPermission(
                        BuildConfig.APPLICATION_ID,
                        mUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.error_reading_failed, Toast.LENGTH_SHORT).show()
                }

                val continuation: AtomicReference<Continuation<String>?> = AtomicReference(null)
                mGalleryProvider = ArchivePageLoader(
                    this,
                    mUri!!,
                    flow {
                        if (!dialogShown) {
                            withUIContext {
                                dialogShown = true
                                dialog.run {
                                    show()
                                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                        val passwd = builder.text
                                        if (passwd.isEmpty()) {
                                            builder.setError(getString(R.string.passwd_cannot_be_empty))
                                        } else {
                                            continuation.getAndSet(null)?.resume(passwd)
                                        }
                                    }
                                    setOnCancelListener {
                                        finish()
                                    }
                                }
                            }
                        }
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val r = suspendCancellableCoroutine {
                                continuation.set(it)
                                it.invokeOnCancellation { dialog.dismiss() }
                            }
                            emit(r)
                            withUIContext {
                                builder.setError(getString(R.string.passwd_wrong))
                            }
                        }
                    },
                )
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        mAction = intent.action
        mFilename = intent.getStringExtra(KEY_FILENAME)
        mUri = intent.data
        mGalleryInfo = intent.getParcelableExtraCompat(KEY_GALLERY_INFO)
        mPage = intent.getIntExtra(KEY_PAGE, -1)
    }

    private fun onInit() {
        handleIntent(intent)
        buildProvider()
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mAction = savedInstanceState.getString(KEY_ACTION)
        mFilename = savedInstanceState.getString(KEY_FILENAME)
        mUri = savedInstanceState.getParcelableCompat(KEY_URI)
        mGalleryInfo = savedInstanceState.getParcelableCompat(KEY_GALLERY_INFO)
        mPage = savedInstanceState.getInt(KEY_PAGE, -1)
        mCurrentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX)
        buildProvider()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
        buildProvider(true)
        mGalleryProvider?.let {
            lifecycleScope.launchIO {
                it.start()
                if (it.awaitReady()) {
                    withUIContext {
                        viewer?.setGalleryProvider(it)
                        moveToPageIndex(0)
                    }
                }
            }
        }
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
        window.colorMode = if (Image.isWideColorGamut && ReaderPreferences.wideColorGamut()
                .get()
        ) {
            ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        } else {
            ActivityInfo.COLOR_MODE_DEFAULT
        }
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
        binding = ReaderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        builder = EditTextDialogBuilder(this, null, getString(R.string.archive_passwd))
        builder.setTitle(getString(R.string.archive_need_passwd))
        builder.setPositiveButton(getString(android.R.string.ok), null)
        dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)

        mGalleryProvider.let {
            if (it == null) {
                finish()
                return
            }
            lifecycleScope.launchIO {
                it.start()
                if (it.awaitReady()) withUIContext { setGallery() }
            }
        }

        config = ReaderConfig()
        initializeMenu()
    }

    fun setGallery() {
        if (mGalleryProvider?.isReady != true) return
        // TODO: Not well place to call it
        dialog.dismiss()

        // Get start page
        if (mCurrentIndex == 0) {
            mCurrentIndex = if (mPage >= 0) mPage else mGalleryProvider!!.startPage
        }
        totalPage = mGalleryProvider!!.size
        val viewerMode = ReadingModeType.fromPreference(ReaderPreferences.defaultReadingMode().get())
        binding.actionReadingMode.setImageResource(viewerMode.iconRes)
        viewer?.destroy()
        viewer = ReadingModeType.toViewer(ReaderPreferences.defaultReadingMode().get(), this)
        isRtl = viewer is R2LPagerViewer
        updateViewerInset(ReaderPreferences.fullscreen().get())
        binding.viewerContainer.removeAllViews()
        setOrientation(ReaderPreferences.defaultOrientationType().get())
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

        val dir = AppConfig.externalTempDir
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_create_temp_file, Toast.LENGTH_SHORT).show()
            return
        }
        val file = mGalleryProvider!!.save(
            page,
            UniFile.fromFile(dir)!!,
            mGalleryProvider!!.getImageFilenameWithExtension(page),
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
            MimeTypeMap.getFileExtensionFromUrl(filename),
        )
        if (mimeType.isNullOrEmpty()) {
            mimeType = "image/jpeg"
        }

        val uri = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            File(dir, filename),
        )

        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        if (mGalleryInfo != null) {
            intent.putExtra(
                Intent.EXTRA_TEXT,
                EhUrl.getGalleryDetailUrl(mGalleryInfo!!.gid, mGalleryInfo!!.token),
            )
        }
        intent.setDataAndType(uri, mimeType)

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_image)))
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            Toast.makeText(this, R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show()
        }
    }

    fun copyImage(page: Int) {
        if (null == mGalleryProvider) {
            return
        }

        val dir = AppConfig.externalCopyTempDir
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_create_temp_file, Toast.LENGTH_SHORT).show()
            return
        }
        val file = mGalleryProvider!!.save(
            page,
            UniFile.fromFile(dir)!!,
            mGalleryProvider!!.getImageFilenameWithExtension(page),
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
            File(dir, filename),
        )

        val clipData = ClipData.newUri(contentResolver, "ehviewer", uri)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    fun saveImage(page: Int) {
        if (null == mGalleryProvider) {
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            mSavingPage = page
            requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }

        val filename = mGalleryProvider!!.getImageFilenameWithExtension(page)
        var mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            MimeTypeMap.getFileExtensionFromUrl(filename),
        )
        if (mimeType.isNullOrEmpty()) {
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
                Environment.DIRECTORY_PICTURES + File.separator + AppConfig.APP_DIRNAME,
            )
            values.put(MediaStore.MediaColumns.IS_PENDING, 1)
            realPath = Environment.DIRECTORY_PICTURES + File.separator + AppConfig.APP_DIRNAME
        } else {
            val path = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                AppConfig.APP_DIRNAME,
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
            Toast.LENGTH_SHORT,
        ).show()
    }

    fun saveImageTo(page: Int) {
        if (null == mGalleryProvider) {
            return
        }
        val dir = AppConfig.externalTempDir
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_create_temp_file, Toast.LENGTH_SHORT).show()
            return
        }
        val file = mGalleryProvider!!.save(
            page,
            UniFile.fromFile(dir)!!,
            mGalleryProvider!!.getImageFilenameWithExtension(page),
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

    companion object {
        const val ACTION_EH = "eh"
        const val KEY_ACTION = "action"
        const val KEY_FILENAME = "filename"
        const val KEY_URI = "uri"
        const val KEY_GALLERY_INFO = "gallery_info"
        const val KEY_PAGE = "page"
        const val KEY_CURRENT_INDEX = "current_index"
    }

    // Tachiyomi funcs

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
            binding.root,
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

            if (ReaderPreferences.showPageNumber().get()) {
                config?.setPageNumberVisibility(false)
            }
        } else {
            if (ReaderPreferences.fullscreen().get()) {
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

            if (ReaderPreferences.showPageNumber().get()) {
                config?.setPageNumberVisibility(true)
            }
        }
    }

    private var currentPage by mutableIntStateOf(-1)
    private var totalPage by mutableIntStateOf(-1)
    private var isRtl by mutableStateOf(false)

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

        binding.pageNumber.setMD3Content {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall,
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            ) {
                PageIndicatorText(
                    currentPage = currentPage,
                    totalPages = totalPage,
                )
            }
        }

        // Init listeners on bottom menu
        binding.readerNav.setMD3Content {
            ChapterNavigator(
                isRtl = isRtl,
                currentPage = currentPage,
                totalPages = totalPage,
                onSliderValueChange = {
                    isScrollingThroughPages = true
                    moveToPageIndex(it)
                },
            )
        }

        initBottomShortcuts()

        val toolbarBackground = MaterialShapeDrawable.createWithElevationOverlay(this).apply {
            elevation = resources.getDimension(com.google.android.material.R.dimen.m3_sys_elevation_level2)
            alpha = if (isNightMode()) 230 else 242 // 90% dark 95% light
        }
        binding.toolbarBottom.background = toolbarBackground.copy(this@ReaderActivity)

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
                    selectedItemId = ReaderPreferences.defaultReadingMode().get(),
                ) {
                    val newReadingMode = ReadingModeType.fromPreference(itemId)
                    ReaderPreferences.defaultReadingMode().set(newReadingMode.flagValue)
                }
            }
        }

        // Rotation
        with(binding.actionRotation) {
            setTooltip(R.string.rotation_type)

            setOnClickListener {
                popupMenu(
                    items = OrientationType.values().map { it.flagValue to it.stringRes },
                    selectedItemId = ReaderPreferences.defaultOrientationType().get(),
                ) {
                    val newOrientation = OrientationType.fromPreference(itemId)
                    ReaderPreferences.defaultOrientationType().set(newOrientation.flagValue)
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
        // Set bottom page number
        currentPage = page.number

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
        updateOrientationShortcut(ReaderPreferences.defaultOrientationType().get())
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
            ReaderPreferences.defaultReadingMode().changes()
                .onEach { setGallery() }
                .launchIn(lifecycleScope)

            ReaderPreferences.defaultOrientationType().changes()
                .onEach { setGallery() }
                .launchIn(lifecycleScope)

            ReaderPreferences.readerTheme().changes()
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

            ReaderPreferences.showPageNumber().changes()
                .onEach { setPageNumberVisibility(it) }
                .launchIn(lifecycleScope)

            ReaderPreferences.showReaderSeekbar().changes()
                .onEach { setReaderSeekbarVisibility(it) }
                .launchIn(lifecycleScope)

            ReaderPreferences.trueColor().changes()
                .onEach { setTrueColor(it) }
                .launchIn(lifecycleScope)

            ReaderPreferences.cutoutShort().changes()
                .onEach { setCutoutShort(it) }
                .launchIn(lifecycleScope)

            ReaderPreferences.keepScreenOn().changes()
                .onEach { setKeepScreenOn(it) }
                .launchIn(lifecycleScope)

            ReaderPreferences.customBrightness().changes()
                .onEach { setCustomBrightness(it) }
                .launchIn(lifecycleScope)

            ReaderPreferences.colorFilter().changes()
                .onEach { setColorFilter(it) }
                .launchIn(lifecycleScope)

            ReaderPreferences.colorFilterMode().changes()
                .onEach { setColorFilter(ReaderPreferences.colorFilter().get()) }
                .launchIn(lifecycleScope)

            merge(
                ReaderPreferences.grayscale().changes(),
                ReaderPreferences.invertedColors().changes(),
            )
                .onEach {
                    setLayerPaint(
                        ReaderPreferences.grayscale().get(),
                        ReaderPreferences.invertedColors().get(),
                    )
                }
                .launchIn(lifecycleScope)

            ReaderPreferences.fullscreen().changes()
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

        fun setReaderSeekbarVisibility(visible: Boolean) {
            binding.readerNav.isVisible = visible
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
                ReaderPreferences.customBrightnessValue().changes()
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
                ReaderPreferences.colorFilterValue().changes()
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
            binding.colorOverlay.setFilterColor(value, ReaderPreferences.colorFilterMode().get())
        }

        private fun setLayerPaint(grayscale: Boolean, invertedColors: Boolean) {
            val paint = if (grayscale || invertedColors) {
                getCombinedPaint(
                    grayscale,
                    invertedColors,
                )
            } else {
                null
            }
            binding.viewerContainer.setLayerType(LAYER_TYPE_HARDWARE, paint)
        }
    }
}
