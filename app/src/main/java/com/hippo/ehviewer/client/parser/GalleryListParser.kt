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
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.client.getThumbKey
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.JsoupUtils
import com.hippo.ehviewer.yorozuya.NumberUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import splitties.init.appCtx
import java.util.regex.Pattern

object GalleryListParser {
    private val TAG = GalleryListParser::class.java.simpleName
    private const val NO_UNFILTERED_TEXT =
        "No unfiltered results in this page range. You either requested an invalid page or used too aggressive filters."
    private val PATTERN_RATING = Pattern.compile("\\d+px")
    private val PATTERN_THUMB_SIZE = Pattern.compile("height:(\\d+)px;width:(\\d+)px")
    private val PATTERN_FAVORITE_SLOT =
        Pattern.compile("background-color:rgba\\((\\d+),(\\d+),(\\d+),")
    private val PATTERN_PAGES = Pattern.compile("(\\d+) page")
    private val PATTERN_NEXT_PAGE = Pattern.compile("p=(\\d+)")
    private val PATTERN_PREV = Pattern.compile("prev=(\\d+(-\\d+)?)")
    private val PATTERN_NEXT = Pattern.compile("next=(\\d+(-\\d+)?)")
    private val FAVORITE_SLOT_RGB = arrayOf(
        Triple("0", "0", "0"),
        Triple("240", "0", "0"),
        Triple("240", "160", "0"),
        Triple("208", "208", "0"),
        Triple("0", "128", "0"),
        Triple("144", "240", "64"),
        Triple("64", "176", "240"),
        Triple("0", "0", "240"),
        Triple("80", "0", "128"),
        Triple("224", "128", "224"),
    )

    private fun parseRating(ratingStyle: String): String? {
        val m = PATTERN_RATING.matcher(ratingStyle)
        var num1 = Int.MIN_VALUE
        var num2 = Int.MIN_VALUE
        var rate = 5
        if (m.find()) {
            num1 = ParserUtils.parseInt(m.group().replace("px", ""), Int.MIN_VALUE)
        }
        if (m.find()) {
            num2 = ParserUtils.parseInt(m.group().replace("px", ""), Int.MIN_VALUE)
        }
        if (num1 == Int.MIN_VALUE || num2 == Int.MIN_VALUE) {
            return null
        }
        rate -= num1 / 16
        return if (num2 == 21) {
            "${rate - 1}.5"
        } else {
            rate.toString()
        }
    }

    private fun parseFavoriteSlot(style: String): Int {
        val m = PATTERN_FAVORITE_SLOT.matcher(style)
        if (m.find()) {
            val r = m.group(1)!!
            val g = m.group(2)!!
            val b = m.group(3)!!
            return FAVORITE_SLOT_RGB.indexOf(Triple(r, g, b))
        }
        return -2
    }

    private fun parseGalleryInfo(e: Element): GalleryInfo? {
        val gi: GalleryInfo = BaseGalleryInfo()

        // Title, gid, token (required), tags
        val glname = JsoupUtils.getElementByClass(e, "glname")
        if (glname != null) {
            var a = JsoupUtils.getElementByTag(glname, "a")
            if (a == null) {
                val parent = glname.parent()
                if (parent != null && "a" == parent.tagName()) {
                    a = parent
                }
            }
            if (a != null) {
                val result = GalleryDetailUrlParser.parse(a.attr("href"))
                if (result != null) {
                    gi.gid = result.gid
                    gi.token = result.token
                }
            }
            var child: Element = glname
            var children = glname.children()
            while (children.size != 0) {
                child = children[0]
                children = child.children()
            }
            gi.title = child.text().trim { it <= ' ' }
        }
        if (gi.title == null) {
            return null
        }

        // Tags
        val gts = e.select(".gt, .gtl")
        if (gts.size != 0) {
            val tags = ArrayList<String>()
            for (gt in gts) {
                tags.add(gt.attr("title"))
            }
            gi.simpleTags = tags
        }

        // Category
        gi.category = EhUtils.UNKNOWN
        var ce = JsoupUtils.getElementByClass(e, "cn")
        if (ce == null) {
            ce = JsoupUtils.getElementByClass(e, "cs")
        }
        if (ce != null) {
            gi.category = EhUtils.getCategory(ce.text())
        }

        // Thumb
        var thumbUrl: String? = null
        val glthumb = JsoupUtils.getElementByClass(e, "glthumb")
        if (glthumb != null) {
            val img = glthumb.select("div:nth-child(1)>img").first()
            if (img != null) {
                // Thumb size
                val m = PATTERN_THUMB_SIZE.matcher(img.attr("style"))
                if (m.find()) {
                    gi.thumbWidth = NumberUtils.parseIntSafely(m.group(2), 0)
                    gi.thumbHeight = NumberUtils.parseIntSafely(m.group(1), 0)
                } else {
                    Log.w(TAG, "Can't parse gallery info thumb size")
                    gi.thumbWidth = 0
                    gi.thumbHeight = 0
                }
                // Thumb url
                var url = img.attr("data-src")
                if (url.isEmpty()) {
                    url = img.attr("src")
                }
                if (url.isNotEmpty()) {
                    thumbUrl = EhUtils.handleThumbUrlResolution(url)!!
                }
            }

            // Pages
            val div = glthumb.select("div:nth-child(2)>div:nth-child(2)>div:nth-child(2)").first()
            if (div != null) {
                val matcher = PATTERN_PAGES.matcher(div.text())
                if (matcher.find()) {
                    gi.pages = NumberUtils.parseIntSafely(matcher.group(1), 0)
                }
            }
        }
        // Try extended and thumbnail version
        if (thumbUrl == null) {
            var gl = JsoupUtils.getElementByClass(e, "gl1e")
            if (gl == null) {
                gl = JsoupUtils.getElementByClass(e, "gl3t")
            }
            if (gl != null) {
                val img = JsoupUtils.getElementByTag(gl, "img")
                if (img != null) {
                    // Thumb size
                    val m = PATTERN_THUMB_SIZE.matcher(img.attr("style"))
                    if (m.find()) {
                        gi.thumbWidth = NumberUtils.parseIntSafely(m.group(2), 0)
                        gi.thumbHeight = NumberUtils.parseIntSafely(m.group(1), 0)
                    } else {
                        Log.w(TAG, "Can't parse gallery info thumb size")
                        gi.thumbWidth = 0
                        gi.thumbHeight = 0
                    }
                    thumbUrl = EhUtils.handleThumbUrlResolution(img.attr("src"))
                }
            }
        }

        gi.thumbKey = getThumbKey(thumbUrl!!)

        // Posted
        val posted = e.getElementById("posted_" + gi.gid)
        if (posted != null) {
            gi.posted = posted.text().trim { it <= ' ' }
            gi.favoriteSlot = parseFavoriteSlot(posted.attr("style"))
        }
        if (gi.favoriteSlot < 0) {
            gi.favoriteSlot = if (EhDB.containLocalFavorites(gi.gid)) -1 else -2
        }

        // Rating
        val ir = JsoupUtils.getElementByClass(e, "ir")
        if (ir != null) {
            gi.rating = NumberUtils.parseFloatSafely(parseRating(ir.attr("style")), -1.0f)
            // TODO The gallery may be rated even if it doesn't has one of these classes
            gi.rated = ir.hasClass("irr") || ir.hasClass("irg") || ir.hasClass("irb")
        }

        // Uploader and pages
        var gl = JsoupUtils.getElementByClass(e, "glhide")
        var uploaderIndex = 0
        var pagesIndex = 1
        if (gl == null) {
            // For extended
            gl = JsoupUtils.getElementByClass(e, "gl3e")
            uploaderIndex = 3
            pagesIndex = 4
        }
        if (gl != null) {
            val children = gl.children()
            if (children.size > uploaderIndex) {
                val div = children[uploaderIndex]
                if (div != null) {
                    gi.disowned = div.attr("style").contains("opacity:0.5")
                    val a = div.children().first()
                    gi.uploader = a?.text()?.trim { it <= ' ' } ?: div.text().trim { it <= ' ' }
                }
            }
            if (children.size > pagesIndex) {
                val matcher = PATTERN_PAGES.matcher(children[pagesIndex].text())
                if (matcher.find()) {
                    gi.pages = NumberUtils.parseIntSafely(matcher.group(1), 0)
                }
            }
        }
        // For thumbnail
        val gl5t = JsoupUtils.getElementByClass(e, "gl5t")
        if (gl5t != null) {
            val div = gl5t.select("div:nth-child(2)>div:nth-child(2)").first()
            if (div != null) {
                val matcher = PATTERN_PAGES.matcher(div.text())
                if (matcher.find()) {
                    gi.pages = NumberUtils.parseIntSafely(matcher.group(1), 0)
                }
            }
        }
        gi.generateSLang()
        return gi
    }

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
                            result.nextPage = NumberUtils.parseIntSafely(matcher.group(1), 0)
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
                    parseGalleryInfo(it.toString())?.apply {
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

private external fun parseGalleryInfo(e: String): GalleryInfo?
