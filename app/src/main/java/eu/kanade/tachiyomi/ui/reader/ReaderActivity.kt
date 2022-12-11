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
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference

class ReaderActivity : EhActivity() {
    lateinit var binding: ReaderActivityBinding
    private var mAction: String? = null
    private var mFilename: String? = null
    private var mUri: Uri? = null
    private var mGalleryInfo: GalleryInfo? = null
    private var mPage: Int = 0
    private var mCacheFileName: String? = null
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

        // Get start page
        val startPage: Int = if (savedInstanceState == null) {
            if (mPage >= 0) mPage else mGalleryProvider!!.startPage
        } else {
            mCurrentIndex
        }

        mSize = mGalleryProvider!!.size()
        mCurrentIndex = startPage
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mGalleryProvider != null) {
            mGalleryProvider!!.setListener(null)
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
}
