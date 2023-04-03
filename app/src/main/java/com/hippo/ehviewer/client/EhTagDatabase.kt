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
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhApplication.Companion.nonCacheOkHttpClient
import com.hippo.ehviewer.R
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.copyToFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.tarsin.coroutines.runSuspendCatching
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.executeAsync
import okio.HashingSink.Companion.sha1
import okio.blackholeSink
import okio.buffer
import okio.source
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

private typealias TagGroup = Map<String, String>
private typealias TagGroups = Map<String, TagGroup>

object EhTagDatabase : CoroutineScope {
    private const val NAMESPACE_PREFIX = "n"
    private lateinit var tagGroups: TagGroups
    private val updateLock = Mutex()
    private val dbLock = Mutex()
    override val coroutineContext = Dispatchers.IO + Job()

    fun isInitialized(): Boolean {
        return this::tagGroups.isInitialized
    }

    private fun JSONObject.toTagGroups(): TagGroups =
        keys().asSequence().associateWith { getJSONObject(it).toTagGroup() }

    private fun JSONObject.toTagGroup(): TagGroup =
        keys().asSequence().associateWith { getString(it) }

    fun getTranslation(prefix: String? = NAMESPACE_PREFIX, tag: String?): String? {
        return tagGroups[prefix]?.get(tag)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun internalSuggestFlow(
        tags: Map<String, String>,
        keyword: String,
        translate: Boolean,
        exactly: Boolean,
    ): Flow<Pair<String?, String>> = flow {
        if (exactly) {
            tags[keyword]?.let {
                emit(Pair(it.takeIf { translate }, keyword))
            }
        } else {
            if (translate) {
                tags.forEach { (tag, hint) ->
                    if (tag != keyword &&
                        (tag.containsIgnoreSpace(keyword) || hint.containsIgnoreSpace(keyword))
                    ) {
                        emit(Pair(hint, tag))
                    }
                }
            } else {
                tags.keys.forEach { tag ->
                    if (tag != keyword && tag.containsIgnoreSpace(keyword)) {
                        emit(Pair(null, tag))
                    }
                }
            }
        }
    }

    /* Construct a cold flow for tag database suggestions */
    fun suggestFlow(
        keyword: String,
        translate: Boolean,
        exactly: Boolean = false,
    ): Flow<Pair<String?, String>> = flow {
        val keywordPrefix = keyword.substringBefore(':')
        val keywordTag = keyword.drop(keywordPrefix.length + 1)
        val prefix = namespaceToPrefix(keywordPrefix) ?: keywordPrefix
        val tags = tagGroups[prefix.takeIf { keywordTag.isNotEmpty() && it != NAMESPACE_PREFIX }]
        tags?.let {
            internalSuggestFlow(it, keywordTag, translate, exactly).collect { (hint, tag) ->
                emit(Pair(hint, "$prefix:$tag"))
            }
        } ?: tagGroups.forEach { (prefix, tags) ->
            if (prefix != NAMESPACE_PREFIX) {
                internalSuggestFlow(tags, keyword, translate, exactly).collect { (hint, tag) ->
                    emit(Pair(hint, "$prefix:$tag"))
                }
            } else {
                internalSuggestFlow(tags, keyword, translate, exactly).collect { (hint, tag) ->
                    emit(Pair(hint, "$tag:"))
                }
            }
        }
    }

    private fun String.removeSpace(): String = replace(" ", "")

    private fun String.containsIgnoreSpace(other: String, ignoreCase: Boolean = true): Boolean =
        removeSpace().contains(other.removeSpace(), ignoreCase)

    private val NAMESPACE_TO_PREFIX = HashMap<String, String>().also {
        it["artist"] = "a"
        it["cosplayer"] = "cos"
        it["character"] = "c"
        it["female"] = "f"
        it["group"] = "g"
        it["language"] = "l"
        it["male"] = "m"
        it["mixed"] = "x"
        it["other"] = "o"
        it["parody"] = "p"
        it["reclass"] = "r"
    }

    @JvmStatic
    fun namespaceToPrefix(namespace: String): String? {
        return NAMESPACE_TO_PREFIX[namespace]
    }

    private fun getMetadata(context: Context): Array<String>? {
        return context.resources.getStringArray(R.array.tag_translation_metadata)
            .takeIf { it.size == 4 }
    }

    fun isTranslatable(context: Context): Boolean {
        return context.resources.getBoolean(R.bool.tag_translatable)
    }

    private fun getFileContent(file: File): String? {
        return runCatching {
            file.source().buffer().use { it.readString(StandardCharsets.UTF_8) }
        }.getOrNull()
    }

    private fun getFileSha1(file: File): String? {
        return runCatching {
            file.source().buffer().use { source ->
                sha1(blackholeSink()).use {
                    source.readAll(it)
                    it.hash.hex()
                }
            }
        }.getOrNull()
    }

    private fun checkData(sha1: String?, data: File): Boolean {
        return sha1 != null && sha1 == getFileSha1(data)
    }

    private suspend fun save(client: OkHttpClient, url: String, file: File): Boolean {
        val request: Request = Request.Builder().url(url).build()
        val call = client.newCall(request)
        runCatching {
            call.executeAsync().use { response ->
                if (!response.isSuccessful) {
                    return false
                }
                response.body.use {
                    it.copyToFile(file)
                }
                return true
            }
        }.onFailure {
            file.delete()
            it.printStackTrace()
        }
        return false
    }

    suspend fun update() {
        updateLock.withLock {
            updateInternal()
        }
    }

    private suspend fun issueUpdateInMemoryData() {
        dbLock.withLock {
            getMetadata(EhApplication.application)?.let { urls ->
                val dataName = urls[2]
                val dir = AppConfig.getFilesDir("tag-translations")
                val dataFile = File(dir, dataName).takeIf { it.exists() } ?: return
                runSuspendCatching {
                    tagGroups = JSONObject(dataFile.source().buffer().readString(StandardCharsets.UTF_8)).toTagGroups()
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }

    init {
        launch {
            issueUpdateInMemoryData()
        }
    }

    private suspend fun updateInternal() {
        val urls = getMetadata(EhApplication.application)
        urls?.let {
            val sha1Name = urls[0]
            val sha1Url = urls[1]
            val dataName = urls[2]
            val dataUrl = urls[3]

            val dir = AppConfig.getFilesDir("tag-translations")
            checkNotNull(dir)
            val sha1File = File(dir, sha1Name)
            val dataFile = File(dir, dataName)

            runCatching {
                // Check current sha1 and current data
                val sha1 = getFileContent(sha1File)
                if (!checkData(sha1, dataFile)) {
                    FileUtils.delete(sha1File)
                    FileUtils.delete(dataFile)
                }

                val client = nonCacheOkHttpClient

                // Save new sha1
                val tempSha1File = File(dir, "$sha1Name.tmp")
                check(save(client, sha1Url, tempSha1File))
                val tempSha1 = getFileContent(tempSha1File)

                // Check new sha1 and current sha1
                if (tempSha1 == sha1) {
                    // The data is the same
                    FileUtils.delete(tempSha1File)
                    return@runCatching
                }

                // Save new data
                val tempDataFile = File(dir, "$dataName.tmp")
                check(save(client, dataUrl, tempDataFile))

                // Check new sha1 and new data
                if (!checkData(tempSha1, tempDataFile)) {
                    FileUtils.delete(tempSha1File)
                    FileUtils.delete(tempDataFile)
                    return@runCatching
                }

                // Replace current sha1 and current data with new sha1 and new data
                FileUtils.delete(sha1File)
                FileUtils.delete(dataFile)
                tempSha1File.renameTo(sha1File)
                tempDataFile.renameTo(dataFile)

                // Read new EhTagDatabase
                issueUpdateInMemoryData()
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}
