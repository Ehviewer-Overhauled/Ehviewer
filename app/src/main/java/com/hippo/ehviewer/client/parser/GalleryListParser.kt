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
package com.hippo.ehviewer.client.parser

import android.util.Log
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.yorozuya.toIntOrDefault
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import splitties.init.appCtx
import java.util.regex.Pattern

object GalleryListParser {
    private val TAG = GalleryListParser::class.java.simpleName
    private const val NO_UNFILTERED_TEXT =
        "No unfiltered results in this page range. You either requested an invalid page or used too aggressive filters."
    private val PATTERN_NEXT_PAGE = Pattern.compile("p=(\\d+)")
    private val PATTERN_PREV = Pattern.compile("prev=(\\d+(-\\d+)?)")
    private val PATTERN_NEXT = Pattern.compile("next=(\\d+(-\\d+)?)")

    fun parse(body: String): Result {
        val d = Jsoup.parse(body)
        d.outputSettings().prettyPrint(false)
        return parse(d, body)
    }

    fun parse(d: Document, body: String): Result {
        val result = Result()
        try {
            val prev = d.getElementById("uprev")
            val next = d.getElementById("unext")
            assert(prev != null)
            assert(next != null)
            val matcherPrev = PATTERN_PREV.matcher(prev!!.attr("href"))
            val matcherNext = PATTERN_NEXT.matcher(next!!.attr("href"))
            if (matcherPrev.find()) result.prev = matcherPrev.group(1)
            if (matcherNext.find()) result.next = matcherNext.group(1)
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            result.noWatchedTags = body.contains("<p>You do not have any watched tags")
            if (body.contains("No hits found</p>")) {
                return result
            }
        }
        try { // For toplists
            val ptt = d.getElementsByClass("ptt").first()
            if (ptt != null) {
                val es = ptt.child(0).child(0).children()
                result.pages = es[es.size - 2].text().trim { it <= ' ' }.toInt()
                var e = es[es.size - 1]
                if (e != null) {
                    e = e.children().first()
                    if (e != null) {
                        val href = e.attr("href")
                        val matcher = PATTERN_NEXT_PAGE.matcher(href)
                        if (matcher.find()) {
                            result.nextPage = matcher.group(1)!!.toIntOrDefault(0)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            val itg = d.getElementsByClass("itg").first()
            val es = if ("table".equals(itg!!.tagName(), ignoreCase = true)) {
                itg.child(0).children().let {
                    // First one is table header in non-extended mode, skip it
                    if (!itg.hasClass("glte")) it.drop(1) else it
                }
            } else {
                // Thumbnail mode
                itg.children()
            }
            val list = result.galleryInfoList
            list.addAll(
                es.mapNotNull {
                    runCatching { parseGalleryInfo(it.toString()) }.onFailure { it.printStackTrace() }.getOrNull()?.apply {
                        if (favoriteSlot == -2 && EhDB.containLocalFavorites(gid)) {
                            favoriteSlot = -1
                            favoriteName = appCtx.getString(R.string.local_favorites)
                        }
                        generateSLang()
                    }
                },
            )
            if (list.isEmpty()) {
                if (es.size < 2 || NO_UNFILTERED_TEXT != es[1].text()) {
                    Log.d(TAG, "No gallery found")
                }
            }
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            e.printStackTrace()
            throw ParseException("Can't parse gallery list", body)
        }
        return result
    }

    class Result {
        var pages = 0
        var nextPage = 0
        var prev: String? = null
        var next: String? = null
        var noWatchedTags = false
        val galleryInfoList = mutableListOf<GalleryInfo>()
    }
}

private external fun parseGalleryInfo(e: String): GalleryInfo
