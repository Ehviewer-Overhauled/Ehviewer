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
import com.hippo.ehviewer.client.parser.Limits
import com.hippo.ehviewer.client.parser.ProfileParser
import com.hippo.ehviewer.client.parser.RateGalleryParser
import com.hippo.ehviewer.client.parser.SignInParser
import com.hippo.ehviewer.client.parser.TorrentParser
import com.hippo.ehviewer.client.parser.TorrentResult
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
import org.json.JSONArray
import org.jsoup.Jsoup
import splitties.init.appCtx
import java.io.File
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
        throw EhException("今回はここまで ${appCtx.getString(R.string.kokomade_tip)}".trimIndent())
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
            throw EhException(appCtx.getString(R.string.error_empty_html))
        } else if ("<" !in body) {
            throw EhException(body)
        } else {
            if (Settings.saveParseErrorBody) AppConfig.saveParseErrorBody(e)
            throw EhException(appCtx.getString(R.string.error_parse_error))
        }
    }

    // We can't translate it, rethrow it anyway
    throw e
}

private suspend inline fun <T> Request.executeAndParsingWith(block: String.() -> T): T {
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
    suspend fun getTorrentList(url: String, gid: Long, token: String?): TorrentResult {
        val referer = EhUrl.getGalleryDetailUrl(gid, token)
        return ehRequest(url, referer).executeAndParsingWith(TorrentParser::parse)
    }

    suspend fun getArchiveList(url: String, gid: Long, token: String?) = ehRequest(url, EhUrl.getGalleryDetailUrl(gid, token))
        .executeAndParsingWith(ArchiveParser::parse)
        .apply { funds = funds ?: ehRequest(EhUrl.URL_FUNDS).executeAndParsingWith(HomeParser::parseFunds) }

    suspend fun getImageLimits() = parZip(
        { ehRequest(EhUrl.URL_HOME).executeAndParsingWith(HomeParser::parse) },
        { ehRequest(EhUrl.URL_FUNDS).executeAndParsingWith(HomeParser::parseFunds) },
        { limits, funds -> HomeParser.Result(limits, funds) },
    )

    suspend fun getNews(parse: Boolean) = ehRequest(EhUrl.URL_NEWS, EhUrl.REFERER_E)
        .executeAndParsingWith { if (parse) EventPaneParser.parse(this) else null }

    suspend fun getProfile(): ProfileParser.Result {
        val url = ehRequest(EhUrl.URL_FORUMS).executeAndParsingWith(ForumsParser::parse)
        return ehRequest(url, EhUrl.URL_FORUMS).executeAndParsingWith(ProfileParser::parse)
    }

    suspend fun getUConfig(url: String = EhUrl.uConfigUrl) {
        runSuspendCatching {
            ehRequest(url).executeAndParsingWith { check(contains(U_CONFIG_TEXT)) { "Unable to load config from $url!" } }
        }.onFailure {
            // It may get redirected when accessing ex for the first time
            if (url == EhUrl.URL_UCONFIG_EX) {
                it.printStackTrace()
                ehRequest(url).executeAndParsingWith { check(contains(U_CONFIG_TEXT)) { "Unable to load config from $url!" } }
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
        return ehRequest(url, referer).executeAndParsingWith(GalleryPageParser::parse)
    }

    suspend fun getGalleryList(url: String) = ehRequest(url, EhUrl.referer)
        .executeAndParsingWith(GalleryListParser::parse)
        .apply { fillGalleryList(galleryInfoList, url, true) }

    suspend fun getGalleryDetail(url: String) = ehRequest(url, EhUrl.referer).executeAndParsingWith {
        EventPaneParser.parse(this)?.let {
            Settings.lastDawnDay = today
            showEventNotification(it)
        }
        GalleryDetailParser.parse(this)
    }

    suspend fun getPreviewList(url: String) = ehRequest(url, EhUrl.referer).executeAndParsingWith {
        GalleryDetailParser.parsePreviewList(this) to GalleryDetailParser.parsePreviewPages(this)
    }

    suspend fun getFavorites(url: String) = ehRequest(url, EhUrl.referer)
        .executeAndParsingWith(FavoritesParser::parse)
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
        }.executeAndParsingWith(SignInParser::parse)
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
    }.executeAndParsingWith {
        val document = Jsoup.parse(this)
        val elements = document.select("#chd + p")
        if (elements.size > 0) {
            throw EhException(elements[0].text())
        }
        GalleryDetailParser.parseComments(document)
    }

    /**
     * @param dstCat -1 for delete, 0 - 9 for cloud favorite, others throw Exception
     * @param note   max 250 characters
     */
    suspend fun addFavorites(
        gid: Long,
        token: String?,
        dstCat: Int = -1,
        note: String? = null,
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
        }.executeAndParsingWith { }
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

    suspend fun resetImageLimits(): Limits? {
        return ehRequest(EhUrl.URL_HOME) {
            formBody {
                add("act", "limits")
                add("reset", "Reset Limit")
            }
        }.executeAndParsingWith(HomeParser::parseResetLimits)
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
        }.executeAndParsingWith(FavoritesParser::parse).apply { fillGalleryList(galleryInfoList, url, false) }
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
        }.executeAndParsingWith(GalleryPageApiParser::parse)
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
    }.executeAndParsingWith(RateGalleryParser::parse)

    suspend fun fillGalleryListByApi(galleryInfoList: List<GalleryInfo>, referer: String) = galleryInfoList.chunked(MAX_REQUEST_SIZE).parMap {
        ehRequest(EhUrl.apiUrl, referer, EhUrl.origin) {
            jsonBody {
                put("method", "gdata")
                array("gidlist") { it.forEach { put(jsonArrayOf(it.gid, it.token)) } }
                put("namespace", 1)
            }
        }.executeAndParsingWith { GalleryApiParser.parse(this, it) }
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
    }.executeAndParsingWith { VoteCommentParser.parse(this, commentVote) }

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
    }.executeAndParsingWith(VoteTagParser::parse)

    suspend fun getGalleryToken(gid: Long, gtoken: String?, page: Int) = ehRequest(EhUrl.apiUrl, EhUrl.referer, EhUrl.origin) {
        jsonBody {
            put("method", "gtoken")
            put("pagelist", JSONArray().put(JSONArray().put(gid).put(gtoken).put(page + 1)))
        }
    }.executeAndParsingWith(GalleryTokenApiParser::parse)

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
    }.executeAndParsingWith(GalleryListParser::parse).apply { fillGalleryList(galleryInfoList, EhUrl.imageSearchUrl, true) }

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

    suspend fun addFavoritesRange(gidTokenArray: Array<Pair<Long, String>>, dstCat: Int) {
        gidTokenArray.forEach { (gid, token) -> addFavorites(gid, token, dstCat) }
    }
}
