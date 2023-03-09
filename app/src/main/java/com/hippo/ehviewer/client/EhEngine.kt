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
package com.hippo.ehviewer.client

import android.util.Log
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.GetText
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryCommentList
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.client.parser.ArchiveParser
import com.hippo.ehviewer.client.parser.EventPaneParser
import com.hippo.ehviewer.client.parser.FavoritesParser
import com.hippo.ehviewer.client.parser.ForumsParser
import com.hippo.ehviewer.client.parser.GalleryApiParser
import com.hippo.ehviewer.client.parser.GalleryDetailParser
import com.hippo.ehviewer.client.parser.GalleryListParser
import com.hippo.ehviewer.client.parser.GalleryNotAvailableParser
import com.hippo.ehviewer.client.parser.GalleryPageApiParser
import com.hippo.ehviewer.client.parser.GalleryPageParser
import com.hippo.ehviewer.client.parser.GalleryTokenApiParser
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.client.parser.ProfileParser
import com.hippo.ehviewer.client.parser.RateGalleryParser
import com.hippo.ehviewer.client.parser.SignInParser
import com.hippo.ehviewer.client.parser.TorrentParser
import com.hippo.ehviewer.client.parser.VoteCommentParser
import com.hippo.ehviewer.client.parser.VoteTagParser
import com.hippo.ehviewer.dailycheck.showEventNotification
import com.hippo.ehviewer.dailycheck.today
import com.hippo.network.StatusCodeException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.executeAsync
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import kotlin.math.ceil

private val okHttpClient = EhApplication.okHttpClient
private val MEDIA_TYPE_JSON: MediaType = "application/json; charset=utf-8".toMediaType()
private const val TAG = "EhEngine"
private const val MAX_REQUEST_SIZE = 25
private const val SAD_PANDA_DISPOSITION = "inline; filename=\"sadpanda.jpg\""
private const val SAD_PANDA_TYPE = "image/gif"
private const val SAD_PANDA_LENGTH = "9615"
private const val KOKOMADE_URL = "https://exhentai.org/img/kokomade.jpg"
private const val U_CONFIG_TEXT = "Selected Profile"
private val MEDIA_TYPE_JPEG: MediaType = "image/jpeg".toMediaType()
private var sEhFilter = EhFilter

private fun rethrowExactly(code: Int, headers: Headers, body: String, e: Throwable) {
    // Check sad panda
    if (SAD_PANDA_DISPOSITION == headers["Content-Disposition"] && SAD_PANDA_TYPE == headers["Content-Type"] && SAD_PANDA_LENGTH == headers["Content-Length"]) {
        throw EhException("Sad Panda")
    }

    // Check sad panda(without panda)
    if ("text/html; charset=UTF-8" == headers["Content-Type"] && "0" == headers["Content-Length"] && EhUtils.isExHentai) {
        throw EhException("Sad Panda\n(without panda)")
    }

    // Check kokomade
    if (body.contains(KOKOMADE_URL)) {
        throw EhException("今回はここまで ${GetText.getString(R.string.kokomade_tip)}".trimIndent())
    }

    // Check Gallery Not Available
    if (body.contains("Gallery Not Available - ")) {
        val error = GalleryNotAvailableParser.parse(body)
        if (error.isNotBlank()) {
            throw EhException(error)
        }
    }

    // Check bad response code
    if (code >= 400) {
        throw StatusCodeException(code)
    }

    if (e is ParseException) {
        if (body.isEmpty()) {
            throw EhException(GetText.getString(R.string.error_empty_html))
        } else if (!body.contains("<")) {
            throw EhException(body)
        } else {
            if (Settings.saveParseErrorBody) {
                AppConfig.saveParseErrorBody(e as ParseException?)
            }
            throw EhException(GetText.getString(R.string.error_parse_error))
        }
    }

    // We can't translate it, rethrow it anyway
    throw e
}

private suspend inline fun <T> Request.Builder.executeAndParsingWith(block: String.() -> T): T {
    return okHttpClient.newCall(this.build()).executeAsync().use { response ->
        val body = response.body.string()
        runCatching {
            block(body)
        }.onFailure {
            rethrowExactly(response.code, response.headers, body, it)
        }.getOrThrow()
    }
}

object EhEngine {
    suspend fun signIn(username: String, password: String): String {
        val referer = "https://forums.e-hentai.org/index.php?act=Login&CODE=00"
        val builder = FormBody.Builder()
            .add("referer", referer)
            .add("b", "")
            .add("bt", "")
            .add("UserName", username)
            .add("PassWord", password)
            .add("CookieDate", "1")
        val url = EhUrl.API_SIGN_IN
        val origin = "https://forums.e-hentai.org"
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer, origin)
            .post(builder.build())
            .executeAndParsingWith(SignInParser::parse)
    }

    private suspend fun fillGalleryList(
        list: MutableList<GalleryInfo>,
        url: String,
        filter: Boolean
    ) {
        // Filter title and uploader
        if (filter) {
            var i = 0
            var n = list.size
            while (i < n) {
                val info = list[i]
                if (!sEhFilter.filterTitle(info) || !sEhFilter.filterUploader(info)) {
                    list.removeAt(i)
                    i--
                    n--
                }
                i++
            }
        }
        var hasTags = false
        var hasPages = false
        var hasRated = false
        for (gi in list) {
            if (gi.simpleTags != null) {
                hasTags = true
            }
            if (gi.pages != 0) {
                hasPages = true
            }
            if (gi.rated) {
                hasRated = true
            }
        }
        val needApi =
            filter && sEhFilter.needTags() && !hasTags || Settings.showGalleryPages && !hasPages ||
                    hasRated
        if (needApi) {
            fillGalleryListByApi(list, url)
        }

        // Filter tag
        if (filter) {
            var i = 0
            var n = list.size
            while (i < n) {
                val info = list[i]
                // Thumbnail mode need filter uploader again
                if (!sEhFilter.filterUploader(info) || !sEhFilter.filterTag(info) || !sEhFilter.filterTagNamespace(
                        info
                    )
                ) {
                    list.removeAt(i)
                    i--
                    n--
                }
                i++
            }
        }
    }

    suspend fun getGalleryList(url: String): GalleryListParser.Result {
        val referer = EhUrl.referer
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer).executeAndParsingWith(GalleryListParser::parse)
            .apply { fillGalleryList(galleryInfoList, url, true) }
    }

    suspend fun fillGalleryListByApi(
        galleryInfoList: List<GalleryInfo>,
        referer: String
    ): List<GalleryInfo> {
        val requestItems: MutableList<GalleryInfo> = ArrayList(MAX_REQUEST_SIZE)
        var i = 0
        val size = galleryInfoList.size
        while (i < size) {
            requestItems.add(galleryInfoList[i])
            if (requestItems.size == MAX_REQUEST_SIZE || i == size - 1) {
                doFillGalleryListByApi(requestItems, referer)
                requestItems.clear()
            }
            i++
        }
        return galleryInfoList
    }

    private suspend fun doFillGalleryListByApi(
        galleryInfoList: List<GalleryInfo>,
        referer: String
    ) {
        val json = JSONObject()
        json.put("method", "gdata")
        val ja = JSONArray()
        var i = 0
        val size = galleryInfoList.size
        while (i < size) {
            val gi = galleryInfoList[i]
            val g = JSONArray()
            g.put(gi.gid)
            g.put(gi.token)
            ja.put(g)
            i++
        }
        json.put("gidlist", ja)
        json.put("namespace", 1)
        val url = EhUrl.apiUrl
        val origin = EhUrl.origin
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer, origin)
            .post(json.toString().toRequestBody(MEDIA_TYPE_JSON))
            .executeAndParsingWith {
                GalleryApiParser.parse(this, galleryInfoList)
            }
    }

    suspend fun getGalleryDetail(url: String): GalleryDetail {
        val referer = EhUrl.referer
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer).executeAndParsingWith {
            EventPaneParser.parse(this)?.let {
                Settings.lastDawnDay = today
                showEventNotification(it)
            }
            GalleryDetailParser.parse(this)
        }
    }

    suspend fun getPreviewList(url: String): Pair<List<GalleryPreview>, Int> {
        val referer = EhUrl.referer
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer).executeAndParsingWith {
            GalleryDetailParser.parsePreviewList(this) to GalleryDetailParser.parsePreviewPages(this)
        }
    }

    @Throws(Throwable::class)
    suspend fun rateGallery(
        apiUid: Long, apiKey: String?, gid: Long, token: String?,
        rating: Float
    ): RateGalleryParser.Result {
        val json = JSONObject()
        json.put("method", "rategallery")
        json.put("apiuid", apiUid)
        json.put("apikey", apiKey)
        json.put("gid", gid)
        json.put("token", token)
        json.put("rating", ceil((rating * 2).toDouble()).toInt())
        val requestBody: RequestBody = json.toString().toRequestBody(MEDIA_TYPE_JSON)
        val url = EhUrl.apiUrl
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        val origin = EhUrl.origin
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer, origin)
            .post(requestBody)
            .executeAndParsingWith(RateGalleryParser::parse)
    }

    suspend fun commentGallery(
        url: String?, comment: String, id: String?
    ): GalleryCommentList {
        val builder = FormBody.Builder()
        if (id == null) {
            builder.add("commenttext_new", comment)
        } else {
            builder.add("commenttext_edit", comment)
            builder.add("edit_comment", id)
        }
        val origin = EhUrl.origin
        Log.d(TAG, url!!)
        return EhRequestBuilder(url, url, origin)
            .post(builder.build())
            .executeAndParsingWith {
                val document = Jsoup.parse(this)
                val elements = document.select("#chd + p")
                if (elements.size > 0) {
                    throw EhException(elements[0].text())
                }
                GalleryDetailParser.parseComments(document)
            }
    }

    suspend fun getGalleryToken(
        gid: Long,
        gtoken: String?, page: Int
    ): String {
        val json = JSONObject()
            .put("method", "gtoken")
            .put(
                "pagelist", JSONArray().put(
                    JSONArray().put(gid).put(gtoken).put(page + 1)
                )
            )
        val requestBody: RequestBody = json.toString().toRequestBody(MEDIA_TYPE_JSON)
        val url = EhUrl.apiUrl
        val referer = EhUrl.referer
        val origin = EhUrl.origin
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer, origin)
            .post(requestBody)
            .executeAndParsingWith(GalleryTokenApiParser::parse)
    }

    suspend fun getFavorites(
        url: String
    ): FavoritesParser.Result {
        val referer = EhUrl.referer
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer).executeAndParsingWith(FavoritesParser::parse)
            .apply { fillGalleryList(galleryInfoList, url, false) }
    }

    /**
     * @param dstCat -1 for delete, 0 - 9 for cloud favorite, others throw Exception
     * @param note   max 250 characters
     */
    suspend fun addFavorites(
        gid: Long,
        token: String?, dstCat: Int, note: String?
    ) {
        val catStr: String = when (dstCat) {
            -1 -> {
                "favdel"
            }

            in 0..9 -> {
                dstCat.toString()
            }

            else -> {
                throw EhException("Invalid dstCat: $dstCat")
            }
        }
        val builder = FormBody.Builder()
        builder.add("favcat", catStr)
        builder.add("favnote", note ?: "")
        // submit=Add+to+Favorites is not necessary, just use submit=Apply+Changes all the time
        builder.add("submit", "Apply Changes")
        builder.add("update", "1")
        val url = EhUrl.getAddFavorites(gid, token)
        val origin = EhUrl.origin
        Log.d(TAG, url)
        return EhRequestBuilder(url, url, origin)
            .post(builder.build())
            .executeAndParsingWith { }
    }

    @Throws(Throwable::class)
    suspend fun addFavoritesRange(
        gidArray: LongArray,
        tokenArray: Array<String?>, dstCat: Int
    ): Void? {
        check(gidArray.size == tokenArray.size)
        var i = 0
        val n = gidArray.size
        while (i < n) {
            addFavorites(gidArray[i], tokenArray[i], dstCat, null)
            i++
        }
        return null
    }

    suspend fun modifyFavorites(
        url: String,
        gidArray: LongArray, dstCat: Int
    ): FavoritesParser.Result {
        val catStr: String = when (dstCat) {
            -1 -> {
                "delete"
            }

            in 0..9 -> {
                "fav$dstCat"
            }

            else -> {
                throw EhException("Invalid dstCat: $dstCat")
            }
        }
        val builder = FormBody.Builder()
        builder.add("ddact", catStr)
        for (gid in gidArray) {
            builder.add("modifygids[]", gid.toString())
        }
        builder.add("apply", "Apply")
        val origin = EhUrl.origin
        Log.d(TAG, url)
        return EhRequestBuilder(url, url, origin)
            .post(builder.build())
            .executeAndParsingWith(FavoritesParser::parse)
            .apply { fillGalleryList(galleryInfoList, url, false) }
    }

    suspend fun getTorrentList(
        url: String, gid: Long, token: String?
    ): List<TorrentParser.Result> {
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer).executeAndParsingWith(TorrentParser::parse)
    }

    suspend fun getArchiveList(
        url: String, gid: Long, token: String?
    ): ArchiveParser.Result {
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer).executeAndParsingWith(ArchiveParser::parse)!!
            .apply { funds = funds ?: getFunds() }
    }

    suspend fun downloadArchive(
        gid: Long,
        token: String?, or: String?, res: String?, isHAtH: Boolean
    ): String? {
        if (or.isNullOrEmpty()) {
            throw EhException("Invalid form param or: $or")
        }
        if (res.isNullOrEmpty()) {
            throw EhException("Invalid res: $res")
        }
        val builder = FormBody.Builder()
        if (isHAtH) {
            builder.add("hathdl_xres", res)
        } else {
            builder.add("dltype", res)
            if (res == "org") {
                builder.add("dlcheck", "Download Original Archive")
            } else {
                builder.add("dlcheck", "Download Resample Archive")
            }
        }
        val url = EhUrl.getDownloadArchive(gid, token, or)
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        val origin = EhUrl.origin
        Log.d(TAG, url)
        val request = EhRequestBuilder(url, referer, origin)
            .post(builder.build())
        var result = request.executeAndParsingWith(ArchiveParser::parseArchiveUrl)
        if (!isHAtH) {
            if (result == null) {
                // Wait for the server to prepare archives
                delay(1000)
                result = request.executeAndParsingWith(ArchiveParser::parseArchiveUrl)
                if (result == null) {
                    throw EhException("Archive unavailable")
                }
            }
            return result
        }
        return null
    }

    private suspend fun getFunds(): HomeParser.Funds {
        val url = EhUrl.URL_FUNDS
        Log.d(TAG, url)
        return EhRequestBuilder(url).executeAndParsingWith(HomeParser::parseFunds)
    }

    private suspend fun getImageLimitsInternal(): HomeParser.Limits {
        val url = EhUrl.URL_HOME
        Log.d(TAG, url)
        return EhRequestBuilder(url).executeAndParsingWith(HomeParser::parse)
    }

    suspend fun getImageLimits(): HomeParser.Result = coroutineScope {
        val limitsDeferred = async {
            getImageLimitsInternal()
        }
        val fundsDeferred = async {
            getFunds()
        }
        HomeParser.Result(limitsDeferred.await(), fundsDeferred.await())
    }

    suspend fun resetImageLimits(): HomeParser.Limits? {
        val builder = FormBody.Builder()
            .add("act", "limits")
            .add("reset", "Reset Limit")
        val url = EhUrl.URL_HOME
        Log.d(TAG, url)
        return EhRequestBuilder(url)
            .post(builder.build())
            .executeAndParsingWith(HomeParser::parseResetLimits)
    }

    suspend fun getNews(parse: Boolean): String? {
        val url = EhUrl.URL_NEWS
        val referer = EhUrl.REFERER_E
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer).executeAndParsingWith {
            if (parse) EventPaneParser.parse(this) else null
        }
    }

    private suspend fun getProfileInternal(
        url: String, referer: String
    ): ProfileParser.Result {
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer).executeAndParsingWith(ProfileParser::parse)
    }

    suspend fun getProfile(): ProfileParser.Result {
        val url = EhUrl.URL_FORUMS
        Log.d(TAG, url)
        return EhRequestBuilder(url).executeAndParsingWith(ForumsParser::parse).let {
            getProfileInternal(it, url)
        }
    }

    private suspend fun getUConfigInternal(url: String) {
        Log.d(TAG, url)
        EhRequestBuilder(url).executeAndParsingWith {
            check(contains(U_CONFIG_TEXT)) { "U_CONFIG_TEXT not found!" }
        }
    }

    suspend fun getUConfig(url: String = EhUrl.uConfigUrl) {
        runCatching {
            getUConfigInternal(url)
        }.onFailure {
            // It may get redirected when accessing ex for the first time
            if (EhUtils.isExHentai) {
                it.printStackTrace()
                getUConfigInternal(url)
            } else {
                throw it
            }
        }
    }

    suspend fun voteComment(
        apiUid: Long,
        apiKey: String?, gid: Long, token: String?, commentId: Long, commentVote: Int
    ): VoteCommentParser.Result {
        val json = JSONObject()
        json.put("method", "votecomment")
        json.put("apiuid", apiUid)
        json.put("apikey", apiKey)
        json.put("gid", gid)
        json.put("token", token)
        json.put("comment_id", commentId)
        json.put("comment_vote", commentVote)
        val requestBody: RequestBody = json.toString().toRequestBody(MEDIA_TYPE_JSON)
        val url = EhUrl.apiUrl
        val referer = EhUrl.referer
        val origin = EhUrl.origin
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer, origin)
            .post(requestBody)
            .executeAndParsingWith {
                VoteCommentParser.parse(this, commentVote)
            }
    }

    suspend fun voteTag(
        apiUid: Long,
        apiKey: String?, gid: Long, token: String?, tags: String?, vote: Int
    ): VoteTagParser.Result {
        val json = JSONObject()
        json.put("method", "taggallery")
        json.put("apiuid", apiUid)
        json.put("apikey", apiKey)
        json.put("gid", gid)
        json.put("token", token)
        json.put("tags", tags)
        json.put("vote", vote)
        val requestBody: RequestBody = json.toString().toRequestBody(MEDIA_TYPE_JSON)
        val url = EhUrl.apiUrl
        val referer = EhUrl.referer
        val origin = EhUrl.origin
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer, origin)
            .post(requestBody)
            .executeAndParsingWith(VoteTagParser::parse)
    }

    /**
     * @param image Must be jpeg
     */
    suspend fun imageSearch(
        image: File,
        uss: Boolean, osc: Boolean, se: Boolean
    ): GalleryListParser.Result {
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        builder.addPart(
            Headers.headersOf(
                "Content-Disposition",
                "form-data; name=\"sfile\"; filename=\"a.jpg\""
            ),
            image.asRequestBody(MEDIA_TYPE_JPEG)
        )
        if (uss) {
            builder.addPart(
                Headers.headersOf("Content-Disposition", "form-data; name=\"fs_similar\""),
                "on".toRequestBody()
            )
        }
        if (osc) {
            builder.addPart(
                Headers.headersOf("Content-Disposition", "form-data; name=\"fs_covers\""),
                "on".toRequestBody()
            )
        }
        if (se) {
            builder.addPart(
                Headers.headersOf("Content-Disposition", "form-data; name=\"fs_exp\""),
                "on".toRequestBody()
            )
        }
        builder.addPart(
            Headers.headersOf("Content-Disposition", "form-data; name=\"f_sfile\""),
            "File Search".toRequestBody()
        )
        val url = EhUrl.imageSearchUrl
        val referer = EhUrl.referer
        val origin = EhUrl.origin
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer, origin)
            .post(builder.build())
            .executeAndParsingWith(GalleryListParser::parse)
            .apply { fillGalleryList(galleryInfoList, url, true) }
    }

    suspend fun getGalleryPage(
        url: String?,
        gid: Long,
        token: String?
    ): GalleryPageParser.Result {
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        Log.d(TAG, url!!)
        return EhRequestBuilder(url, referer).executeAndParsingWith(GalleryPageParser::parse)
    }

    suspend fun getGalleryPageApi(
        gid: Long,
        index: Int,
        pToken: String?,
        showKey: String?,
        previousPToken: String?
    ): GalleryPageApiParser.Result {
        val json = JSONObject()
        json.put("method", "showpage")
        json.put("gid", gid)
        json.put("page", index + 1)
        json.put("imgkey", pToken)
        json.put("showkey", showKey)
        val requestBody: RequestBody = json.toString().toRequestBody(MEDIA_TYPE_JSON)
        val url = EhUrl.apiUrl
        var referer: String? = null
        if (index > 0 && previousPToken != null) {
            referer = EhUrl.getPageUrl(gid, index - 1, previousPToken)
        }
        val origin = EhUrl.origin
        Log.d(TAG, url)
        return EhRequestBuilder(url, referer, origin)
            .post(requestBody)
            .executeAndParsingWith(GalleryPageApiParser::parse)
    }
}