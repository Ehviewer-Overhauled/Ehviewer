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
import arrow.fx.coroutines.parMap
import arrow.fx.coroutines.parZip
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.GetText
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryInfo
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
import com.hippo.ehviewer.network.StatusCodeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import moe.tarsin.coroutines.runSuspendCatching
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okio.ByteString.Companion.decodeHex
import org.json.JSONArray
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.io.File
import java.util.function.Consumer
import kotlin.experimental.xor
import kotlin.math.ceil

private const val TAG = "EhEngine"
private const val MAX_REQUEST_SIZE = 25
private const val SAD_PANDA_DISPOSITION = "inline; filename=\"sadpanda.jpg\""
private const val SAD_PANDA_TYPE = "image/gif"
private const val SAD_PANDA_LENGTH = "9615"
private const val KOKOMADE_URL = "https://exhentai.org/img/kokomade.jpg"
private const val U_CONFIG_TEXT = "Selected Profile"
private val MEDIA_TYPE_JPEG: MediaType = "image/jpeg".toMediaType()

private fun rethrowExactly(code: Int, headers: Headers, body: String, e: Throwable): Nothing {
    // Don't translate coroutine cancellation
    if (e is CancellationException) throw e

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
        if (!error.isNullOrBlank()) {
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
        } else if ("<" !in body) {
            throw EhException(body)
        } else {
            if (Settings.saveParseErrorBody) AppConfig.saveParseErrorBody(e)
            throw EhException(GetText.getString(R.string.error_parse_error))
        }
    }

    // We can't translate it, rethrow it anyway
    throw e
}

private const val EMAIL_PROTECTED = "[email protected]"
private const val RAW_EMAIL_PROTECTED = "[email&#160;protected]"

private fun resolveEmailProtected(origin: String): Document {
    // Temporary workaround, see https://github.com/jhy/jsoup/issues/1850
    val doc = Jsoup.parse(origin.replace("del>", "s>"))

    if (RAW_EMAIL_PROTECTED !in origin) return doc
    fun Element.decodeProtectedEmail() {
        if (EMAIL_PROTECTED in text()) {
            if (EMAIL_PROTECTED in ownText()) {
                forEachNode(
                    Consumer { node ->
                        if (node is TextNode) {
                            if (EMAIL_PROTECTED in node.text()) {
                                val encoded = attr("data-cfemail").chunked(2).toMutableList()
                                val k = encoded.removeFirst().decodeHex()[0]
                                val email = encoded.map { it.decodeHex()[0] xor k }.toByteArray().decodeToString()
                                val text = node.text()
                                node.text(text.replace(EMAIL_PROTECTED, email))
                            }
                        }
                    },
                )
            }
            children().forEach {
                it.decodeProtectedEmail()
            }
        }
    }
    return doc.apply { decodeProtectedEmail() }
}

private suspend inline fun <T> Request.executeParsingDocument(block: (String, Document) -> T) = executeParsing {
    val doc = resolveEmailProtected(this)
    block(this, doc)
}

private suspend inline fun <T> Request.executeParsing(block: String.() -> T): T {
    Log.d(TAG, url.toString())
    return execute {
        val body = body.string()
        try {
            block(body)
        } catch (e: Exception) {
            rethrowExactly(code, headers, body, e)
        }
    }
}

object EhEngine {
    suspend fun getTorrentList(url: String, gid: Long, token: String?): List<TorrentParser.Result> {
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        return ehRequest(url, referer).executeParsingDocument { _, d -> TorrentParser.parse(d) }
    }

    suspend fun getArchiveList(url: String, gid: Long, token: String?) = ehRequest(url, EhUrl.getGalleryDetailUrl(gid, token))
        .executeParsingDocument(ArchiveParser::parse)
        .apply { funds = funds ?: ehRequest(EhUrl.URL_FUNDS).executeParsingDocument { s, _ -> HomeParser.parseFunds(s) } }

    suspend fun getImageLimits() = parZip(
        { ehRequest(EhUrl.URL_HOME).executeParsingDocument { _, d -> HomeParser.parse(d) } },
        { ehRequest(EhUrl.URL_FUNDS).executeParsingDocument { s, _ -> HomeParser.parseFunds(s) } },
        { limits, funds -> HomeParser.Result(limits, funds) },
    )

    suspend fun getNews(parse: Boolean) = ehRequest(EhUrl.URL_NEWS, EhUrl.REFERER_E)
        .executeParsingDocument { _, d -> if (parse) EventPaneParser.parse(d) else null }

    suspend fun getProfile(): ProfileParser.Result {
        val url = ehRequest(EhUrl.URL_FORUMS).executeParsing(ForumsParser::parse)
        return ehRequest(url, EhUrl.URL_FORUMS).executeParsingDocument { _, d -> ProfileParser.parse(d) }
    }

    suspend fun getUConfig(url: String = EhUrl.uConfigUrl) {
        runSuspendCatching {
            ehRequest(url).executeParsing { check(U_CONFIG_TEXT in this) { "U_CONFIG_TEXT not found!" } }
        }.onFailure {
            // It may get redirected when accessing ex for the first time
            if (EhUtils.isExHentai) {
                it.printStackTrace()
                ehRequest(url).executeParsing { check(U_CONFIG_TEXT in this) { "U_CONFIG_TEXT not found!" } }
            } else {
                throw it
            }
        }
    }

    suspend fun getGalleryPage(
        url: String,
        gid: Long,
        token: String?,
    ): GalleryPageParser.Result {
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        return ehRequest(url, referer).executeParsing(GalleryPageParser::parse)
    }

    suspend fun getGalleryList(url: String) = ehRequest(url, EhUrl.referer)
        .executeParsingDocument(GalleryListParser::parse)
        .apply { fillGalleryList(galleryInfoList, url, true) }

    suspend fun getGalleryDetail(url: String) = ehRequest(url, EhUrl.referer).executeParsingDocument { s, d ->
        EventPaneParser.parse(d)?.let {
            Settings.lastDawnDay = today
            showEventNotification(it)
        }
        GalleryDetailParser.parse(s, d)
    }

    suspend fun getPreviewList(url: String) = ehRequest(url, EhUrl.referer).executeParsing {
        GalleryDetailParser.parsePreviewList(this) to GalleryDetailParser.parsePreviewPages(this)
    }

    suspend fun getFavorites(url: String) = ehRequest(url, EhUrl.referer)
        .executeParsingDocument(FavoritesParser::parse)
        .apply { fillGalleryList(galleryInfoList, url, false) }

    suspend fun signIn(username: String, password: String): String {
        val referer = "https://forums.e-hentai.org/index.php?act=Login&CODE=00"
        val url = EhUrl.API_SIGN_IN
        val origin = "https://forums.e-hentai.org"
        return ehRequest(url, referer, origin) {
            formBody {
                add("referer", referer)
                add("b", "")
                add("bt", "")
                add("UserName", username)
                add("PassWord", password)
                add("CookieDate", "1")
            }
        }.executeParsing(SignInParser::parse)
    }

    suspend fun commentGallery(url: String, comment: String, id: String?) = ehRequest(url, url, EhUrl.origin) {
        formBody {
            if (id == null) {
                add("commenttext_new", comment)
            } else {
                add("commenttext_edit", comment)
                add("edit_comment", id)
            }
        }
    }.executeParsingDocument { _, d ->
        val elements = d.select("#chd + p")
        if (elements.size > 0) {
            throw EhException(elements[0].text())
        }
        GalleryDetailParser.parseComments(d)
    }

    /**
     * @param dstCat -1 for delete, 0 - 9 for cloud favorite, others throw Exception
     * @param note   max 250 characters
     */
    suspend fun addFavorites(
        gid: Long,
        token: String?,
        dstCat: Int,
        note: String?,
    ) {
        val catStr: String = when (dstCat) {
            -1 -> "favdel"
            in 0..9 -> dstCat.toString()
            else -> throw EhException("Invalid dstCat: $dstCat")
        }
        val url = EhUrl.getAddFavorites(gid, token)
        return ehRequest(url, url, EhUrl.origin) {
            formBody {
                add("favcat", catStr)
                add("favnote", note ?: "")
                // submit=Add+to+Favorites is not necessary, just use submit=Apply+Changes all the time
                add("submit", "Apply Changes")
                add("update", "1")
            }
        }.executeParsing { }
    }

    suspend fun downloadArchive(
        gid: Long,
        token: String?,
        or: String?,
        res: String?,
        isHAtH: Boolean,
    ): String? {
        if (or.isNullOrEmpty()) {
            throw EhException("Invalid form param or: $or")
        }
        if (res.isNullOrEmpty()) {
            throw EhException("Invalid res: $res")
        }
        val url = EhUrl.getDownloadArchive(gid, token, or)
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        val request = ehRequest(url, referer, EhUrl.origin) {
            formBody {
                if (isHAtH) {
                    add("hathdl_xres", res)
                } else {
                    add("dltype", res)
                    if (res == "org") {
                        add("dlcheck", "Download Original Archive")
                    } else {
                        add("dlcheck", "Download Resample Archive")
                    }
                }
            }
        }
        var result = request.executeParsing(ArchiveParser::parseArchiveUrl)
        if (!isHAtH) {
            if (result == null) {
                // Wait for the server to prepare archives
                delay(1000)
                result = request.executeParsing(ArchiveParser::parseArchiveUrl)
                if (result == null) {
                    throw EhException("Archive unavailable")
                }
            }
            return result
        }
        return null
    }

    suspend fun resetImageLimits(): HomeParser.Limits? {
        return ehRequest(EhUrl.URL_HOME) {
            formBody {
                add("act", "limits")
                add("reset", "Reset Limit")
            }
        }.executeParsingDocument(HomeParser::parseResetLimits)
    }

    suspend fun modifyFavorites(url: String, gidArray: LongArray, dstCat: Int): FavoritesParser.Result {
        val catStr: String = when (dstCat) {
            -1 -> "delete"
            in 0..9 -> "fav$dstCat"
            else -> throw EhException("Invalid dstCat: $dstCat")
        }
        return ehRequest(url, url, EhUrl.origin) {
            formBody {
                add("ddact", catStr)
                gidArray.forEach { add("modifygids[]", it.toString()) }
                add("apply", "Apply")
            }
        }.executeParsingDocument(FavoritesParser::parse).apply { fillGalleryList(galleryInfoList, url, false) }
    }

    suspend fun getGalleryPageApi(
        gid: Long,
        index: Int,
        pToken: String,
        showKey: String?,
        previousPToken: String?,
    ): GalleryPageApiParser.Result {
        val referer = if (index > 0 && previousPToken != null) EhUrl.getPageUrl(gid, index - 1, previousPToken) else null
        return ehRequest(EhUrl.apiUrl, referer, EhUrl.origin) {
            jsonBody {
                put("method", "showpage")
                put("gid", gid)
                put("page", index + 1)
                put("imgkey", pToken)
                put("showkey", showKey)
            }
        }.executeParsing(GalleryPageApiParser::parse)
    }

    suspend fun rateGallery(apiUid: Long, apiKey: String?, gid: Long, token: String?, rating: Float) = ehRequest(EhUrl.apiUrl, EhUrl.getGalleryDetailUrl(gid, token), EhUrl.origin) {
        jsonBody {
            put("method", "rategallery")
            put("apiuid", apiUid)
            put("apikey", apiKey)
            put("gid", gid)
            put("token", token)
            put("rating", ceil((rating * 2).toDouble()).toInt())
        }
    }.executeParsing(RateGalleryParser::parse)

    suspend fun fillGalleryListByApi(galleryInfoList: List<GalleryInfo>, referer: String) = galleryInfoList.chunked(MAX_REQUEST_SIZE).parMap {
        ehRequest(EhUrl.apiUrl, referer, EhUrl.origin) {
            jsonBody {
                put("method", "gdata")
                array("gidlist") { it.forEach { put(jsonArrayOf(it.gid, it.token)) } }
                put("namespace", 1)
            }
        }.executeParsing { GalleryApiParser.parse(this, it) }
    }

    suspend fun voteComment(
        apiUid: Long,
        apiKey: String?,
        gid: Long,
        token: String?,
        commentId: Long,
        commentVote: Int,
    ) = ehRequest(EhUrl.apiUrl, EhUrl.referer, EhUrl.origin) {
        jsonBody {
            put("method", "votecomment")
            put("apiuid", apiUid)
            put("apikey", apiKey)
            put("gid", gid)
            put("token", token)
            put("comment_id", commentId)
            put("comment_vote", commentVote)
        }
    }.executeParsing { VoteCommentParser.parse(this, commentVote) }

    suspend fun voteTag(
        apiUid: Long,
        apiKey: String?,
        gid: Long,
        token: String?,
        tags: String?,
        vote: Int,
    ) = ehRequest(EhUrl.apiUrl, EhUrl.referer, EhUrl.origin) {
        jsonBody {
            put("method", "taggallery")
            put("apiuid", apiUid)
            put("apikey", apiKey)
            put("gid", gid)
            put("token", token)
            put("tags", tags)
            put("vote", vote)
        }
    }.executeParsing(VoteTagParser::parse)

    suspend fun getGalleryToken(gid: Long, gtoken: String?, page: Int) = ehRequest(EhUrl.apiUrl, EhUrl.referer, EhUrl.origin) {
        jsonBody {
            put("method", "gtoken")
            put("pagelist", JSONArray().put(JSONArray().put(gid).put(gtoken).put(page + 1)))
        }
    }.executeParsing(GalleryTokenApiParser::parse)

    /**
     * @param image Must be jpeg
     */
    suspend fun imageSearch(image: File, uss: Boolean, osc: Boolean) = ehRequest(EhUrl.imageSearchUrl, EhUrl.referer, EhUrl.origin) {
        multipartBody {
            setType(MultipartBody.FORM)
            addFormDataPart("sfile", "a.jpg", image.asRequestBody(MEDIA_TYPE_JPEG))
            if (uss) addFormDataPart("fs_similar", "on")
            if (osc) addFormDataPart("fs_covers", "on")
            addFormDataPart("f_sfile", "File Search")
        }
    }.executeParsingDocument(GalleryListParser::parse).apply { fillGalleryList(galleryInfoList, EhUrl.imageSearchUrl, true) }

    private suspend fun fillGalleryList(list: MutableList<GalleryInfo>, url: String, filter: Boolean) {
        // Filter title and uploader
        if (filter) list.removeAll { EhFilter.filterTitle(it) || EhFilter.filterUploader(it) }

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
        val needApi = filter && EhFilter.needTags() && !hasTags || Settings.showGalleryPages && !hasPages || hasRated
        if (needApi) fillGalleryListByApi(list, url)

        // Filter tag, thumbnail mode need filter uploader again
        if (filter) list.removeAll { EhFilter.filterUploader(it) || EhFilter.filterTag(it) || EhFilter.filterTagNamespace(it) }
    }

    suspend fun addFavoritesRange(gidArray: LongArray, tokenArray: Array<String?>, dstCat: Int) {
        require(gidArray.size == tokenArray.size)
        gidArray.forEachIndexed { index, gid -> addFavorites(gid, tokenArray[index], dstCat, null) }
    }
}
