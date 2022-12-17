/*
 * Copyright 2019 Hippo Seven
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
package com.hippo.ehviewer.client

import android.content.Context
import androidx.core.util.Pair
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.EhApplication.Companion.okHttpClient
import com.hippo.ehviewer.R
import com.hippo.util.ExceptionUtils
import com.hippo.util.HashCodeUtils
import com.hippo.util.IoThreadPoolExecutor
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.IOUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import okio.buffer
import okio.source
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class EhTagDatabase(private val name: String, source: BufferedSource) {
    private val tags: JSONObject?
    private var tagList: ArrayList<Pair<String, String>>? = null

    init {
        var tmpTags: JSONObject? = null
        try {
            tmpTags = JSONObject(source.readString(StandardCharsets.UTF_8))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        tags = tmpTags
        if (tags != null) {
            tagList = ArrayList()
            tags.keys().forEachRemaining { prefix: String ->
                val values = tags.optJSONObject(prefix)
                values?.let {
                    it.keys().forEachRemaining { tag: String ->
                        tagList!!.add(Pair("$prefix:$tag", values.optString(tag)))
                    }
                }
            }
        } else {
            tagList = null
        }
    }

    fun getTranslation(prefix: String?, tag: String?): String? {
        return tags!!.optJSONObject(prefix)?.optString(tag)
    }

    /* Construct a cold flow for tag database suggestions */
    fun suggestFlow(
        keyword: String,
        translate: Boolean,
        exactly: Boolean = false
    ): Flow<Pair<String?, String>> = flow {
        tagList?.forEach {
            val tag = it.first
            val hint = if (translate) it.second else null
            val index = tag.indexOf(':')
            if (tag.substring(index + 1).trim() == keyword) {
                if (exactly)
                    emit(Pair(hint, tag))
                return@forEach
            }
            if (exactly)
                return@forEach
            val keywordMatches: Boolean =
                if (index == -1 || index >= tag.length - 1 || keyword.length > 2) {
                    containsIgnoreSpace(tag, keyword)
                } else {
                    containsIgnoreSpace(tag.substring(index + 1), keyword)
                }
            if (keywordMatches || containsIgnoreSpace(hint, keyword)) {
                emit(Pair(hint, tag))
            }
        }
    }

    private fun containsIgnoreSpace(text: String?, key: String): Boolean {
        return text != null && text.replace(" ", "").contains(key.replace(" ", ""))
    }

    companion object {
        private val NAMESPACE_TO_PREFIX: MutableMap<String, String> = HashMap()

        // TODO more lock for different language
        private val lock: Lock = ReentrantLock()

        @JvmStatic
        @Volatile
        var instance: EhTagDatabase? = null
            private set

        init {
            NAMESPACE_TO_PREFIX["artist"] = "a"
            NAMESPACE_TO_PREFIX["cosplayer"] = "cos"
            NAMESPACE_TO_PREFIX["character"] = "c"
            NAMESPACE_TO_PREFIX["female"] = "f"
            NAMESPACE_TO_PREFIX["group"] = "g"
            NAMESPACE_TO_PREFIX["language"] = "l"
            NAMESPACE_TO_PREFIX["male"] = "m"
            NAMESPACE_TO_PREFIX["mixed"] = "x"
            NAMESPACE_TO_PREFIX["other"] = "o"
            NAMESPACE_TO_PREFIX["parody"] = "p"
            NAMESPACE_TO_PREFIX["reclass"] = "r"
        }

        @JvmStatic
        fun namespaceToPrefix(namespace: String): String? {
            return NAMESPACE_TO_PREFIX[namespace]
        }

        private fun getMetadata(context: Context): Array<String>? {
            val metadata = context.resources.getStringArray(R.array.tag_translation_metadata)
            return if (metadata.size == 4) {
                metadata
            } else {
                null
            }
        }

        fun isTranslatable(context: Context): Boolean {
            return context.resources.getBoolean(R.bool.tag_translatable)
        }

        private fun getFileContent(file: File): String? {
            try {
                file.source().buffer()
                    .use { source -> return source.readString(StandardCharsets.UTF_8) }
            } catch (e: IOException) {
                return null
            }
        }

        private fun getFileSha1(file: File): String? {
            try {
                FileInputStream(file).use { `is` ->
                    val digest = MessageDigest.getInstance("SHA-1")
                    var n: Int
                    val buffer = ByteArray(4 * 1024)
                    while (`is`.read(buffer).also { n = it } != -1) {
                        digest.update(buffer, 0, n)
                    }
                    return HashCodeUtils.bytesToHexString(digest.digest())
                }
            } catch (e: IOException) {
                return null
            } catch (e: NoSuchAlgorithmException) {
                return null
            }
        }

        private fun checkData(sha1File: File, dataFile: File): Boolean {
            val s1 = getFileContent(sha1File) ?: return false
            val s2 = getFileSha1(dataFile) ?: return false
            return s1 == s2
        }

        private fun save(client: OkHttpClient, url: String, file: File): Boolean {
            val request: Request = Request.Builder().url(url).build()
            val call = client.newCall(request)
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        return false
                    }
                    val body = response.body
                    body.byteStream()
                        .use { `is` -> FileOutputStream(file).use { os -> IOUtils.copy(`is`, os) } }
                    return true
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                ExceptionUtils.throwIfFatal(t)
                return false
            }
        }

        @JvmStatic
        fun update(context: Context) {
            val urls = getMetadata(context)
            if (urls == null || urls.size != 4) {
                // Clear tags if it's not possible
                instance = null
                return
            }
            val sha1Name = urls[0]
            val sha1Url = urls[1]
            val dataName = urls[2]
            val dataUrl = urls[3]

            // Clear tags if name if different
            val tmp = instance
            if (tmp != null && tmp.name != dataName) {
                instance = null
            }
            IoThreadPoolExecutor.getInstance().execute {
                if (!lock.tryLock()) {
                    return@execute
                }
                try {
                    val dir = AppConfig.getFilesDir("tag-translations") ?: return@execute

                    // Check current sha1 and current data
                    val sha1File = File(dir, sha1Name)
                    val dataFile = File(dir, dataName)
                    if (!checkData(sha1File, dataFile)) {
                        FileUtils.delete(sha1File)
                        FileUtils.delete(dataFile)
                    }

                    // Read current EhTagDatabase
                    if (instance == null && dataFile.exists()) {
                        try {
                            dataFile.source().buffer()
                                .use { source -> instance = EhTagDatabase(dataName, source) }
                        } catch (e: IOException) {
                            FileUtils.delete(sha1File)
                            FileUtils.delete(dataFile)
                        }
                    }
                    val client = okHttpClient

                    // Save new sha1
                    val tempSha1File = File(dir, "$sha1Name.tmp")
                    if (!save(client, sha1Url, tempSha1File)) {
                        FileUtils.delete(tempSha1File)
                        return@execute
                    }

                    // Check new sha1 and current data
                    if (checkData(tempSha1File, dataFile)) {
                        // The data is the same
                        FileUtils.delete(tempSha1File)
                        return@execute
                    }

                    // Save new data
                    val tempDataFile = File(dir, "$dataName.tmp")
                    if (!save(client, dataUrl, tempDataFile)) {
                        FileUtils.delete(tempDataFile)
                        return@execute
                    }

                    // Check new sha1 and new data
                    if (!checkData(tempSha1File, tempDataFile)) {
                        FileUtils.delete(tempSha1File)
                        FileUtils.delete(tempDataFile)
                        return@execute
                    }

                    // Replace current sha1 and current data with new sha1 and new data
                    FileUtils.delete(sha1File)
                    FileUtils.delete(dataFile)
                    tempSha1File.renameTo(sha1File)
                    tempDataFile.renameTo(dataFile)

                    // Read new EhTagDatabase
                    try {
                        dataFile.source().buffer()
                            .use { source -> instance = EhTagDatabase(dataName, source) }
                    } catch (e: IOException) {
                        // Ignore
                    }
                } finally {
                    lock.unlock()
                }
            }
        }
    }
}