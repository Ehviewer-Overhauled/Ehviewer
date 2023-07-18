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

import arrow.core.unzip
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.client.EhFilter
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.EhUtils.getCategory
import com.hippo.ehviewer.client.EhUtils.handleThumbUrlResolution
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.client.data.GalleryCommentList
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.LOCAL_FAVORITED
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.data.GalleryTagGroup
import com.hippo.ehviewer.client.data.LargeGalleryPreview
import com.hippo.ehviewer.client.data.NormalGalleryPreview
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.OffensiveException
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.client.exception.PiningException
import com.hippo.ehviewer.client.getThumbKey
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.toFloatOrDefault
import com.hippo.ehviewer.util.toIntOrDefault
import com.hippo.ehviewer.util.trimAnd
import com.hippo.ehviewer.util.unescapeXml
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.Elements
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object GalleryDetailParser {
    private val PATTERN_ERROR = Regex("<div class=\"d\">\n<p>([^<]+)</p>")
    private val PATTERN_DETAIL = Regex(
        "var gid = (\\d+);.+?var token = \"([a-f0-9]+)\";.+?var apiuid = ([\\-\\d]+);.+?var apikey = \"([a-f0-9]+)\";",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val PATTERN_TORRENT =
        Regex("<a[^<>]*onclick=\"return popUp\\('([^']+)'[^)]+\\)\">Torrent Download[^<]+(\\d+)[^<]+</a")
    private val PATTERN_ARCHIVE =
        Regex("<a[^<>]*onclick=\"return popUp\\('([^']+)'[^)]+\\)\">Archive Download</a>")
    private val PATTERN_COVER = Regex("width:(\\d+)px; height:(\\d+)px.+?url\\((.+?)\\)")
    private val PATTERN_PAGES =
        Regex("<tr><td[^<>]*>Length:</td><td[^<>]*>([\\d,]+) pages</td></tr>")
    private val PATTERN_PREVIEW_PAGES =
        Regex("<td[^>]+><a[^>]+>([\\d,]+)</a></td><td[^>]+>(?:<a[^>]+>)?&gt;(?:</a>)?</td>")
    private val PATTERN_NORMAL_PREVIEW =
        Regex("<div class=\"gdtm\"[^<>]*><div[^<>]*width:(\\d+)[^<>]*height:(\\d+)[^<>]*\\((.+?)\\)[^<>]*-(\\d+)px[^<>]*><a[^<>]*href=\"(.+?)\"[^<>]*><img alt=\"([\\d,]+)\"")
    private val PATTERN_LARGE_PREVIEW =
        Regex("<div class=\"gdtl\".+?<a href=\"(.+?)\"><img alt=\"([\\d,]+)\".+?src=\"(.+?)\"")
    private val PATTERN_NEWER_DATE = Regex(", added (.+?)<br />")
    private val PATTERN_FAVORITE_SLOT =
        Regex("/fav.png\\); background-position:0px -(\\d+)px")
    private val EMPTY_GALLERY_TAG_GROUP_ARRAY = arrayOf<GalleryTagGroup>()
    private val EMPTY_GALLERY_COMMENT_ARRAY = GalleryCommentList(arrayOf(), false)
    private val WEB_COMMENT_DATE_FORMAT = DateTimeFormatter
        .ofPattern("dd MMMM yyyy, HH:mm", Locale.US).withZone(ZoneOffset.UTC)
    private const val OFFENSIVE_STRING =
        "<p>(And if you choose to ignore this warning, you lose all rights to complain about it in the future.)</p>"
    private const val PINING_STRING = "<p>This gallery is pining for the fjords.</p>"

    suspend fun parse(body: String): GalleryDetail {
        if (body.contains(OFFENSIVE_STRING)) {
            throw OffensiveException()
        }
        if (body.contains(PINING_STRING)) {
            throw PiningException()
        }

        // Error info
        PATTERN_ERROR.find(body)?.run { throw EhException(groupValues[1]) }
        // Temporary workaround, see https://github.com/jhy/jsoup/issues/1850
        val document = Jsoup.parse(body.replace("del>", "s>"))
        val galleryDetail = GalleryDetail(
            tags = parseTagGroups(document),
            comments = parseComments(document),
            previewPages = parsePreviewPages(body),
            previewList = parsePreviewList(body).first,
        )
        parseDetail(galleryDetail, document, body)

        // Generate simpleLanguage for local favorites
        galleryDetail.generateSLang()
        return galleryDetail
    }

    private suspend fun parseDetail(gd: GalleryDetail, d: Document, body: String) {
        PATTERN_DETAIL.find(body)?.apply {
            gd.gid = groupValues[1].toLongOrNull() ?: -1L
            gd.token = groupValues[2]
            gd.apiUid = groupValues[3].toLongOrNull() ?: -1L
            gd.apiKey = groupValues[4]
        } ?: throw ParseException("Can't parse gallery detail", body)
        if (gd.gid == -1L) {
            throw ParseException("Can't parse gallery detail", body)
        }
        PATTERN_TORRENT.find(body)?.run {
            gd.torrentUrl = groupValues[1].trim().unescapeXml()
            gd.torrentCount = groupValues[2].toIntOrNull() ?: 0
        }
        PATTERN_ARCHIVE.find(body)?.run {
            gd.archiveUrl = groupValues[1].trim().unescapeXml()
        }
        try {
            val gm = d.getElementsByClass("gm")[0]
            // Thumb url
            gm.getElementById("gd1")?.child(0)?.attr("style")?.trim()?.let {
                gd.thumbKey = getThumbKey(
                    PATTERN_COVER.find(it)?.run {
                        handleThumbUrlResolution(groupValues[3])
                    }!!,
                )
            }

            gd.title = gm.getElementById("gn")?.text()?.trim()
            gd.titleJpn = gm.getElementById("gj")?.text()?.trim()

            // Category
            try {
                val gdc = gm.getElementById("gdc")!!
                var ce = gdc.getElementsByClass("cn").first()
                if (ce == null) {
                    ce = gdc.getElementsByClass("cs").first()
                }
                gd.category = getCategory(ce!!.text())
            } catch (e: Throwable) {
                ExceptionUtils.throwIfFatal(e)
                gd.category = EhUtils.UNKNOWN
            }

            // Uploader
            val gdn = gm.getElementById("gdn")
            if (null != gdn) {
                gd.disowned = gdn.attr("style").contains("opacity:0.5")
                gd.uploader = gdn.text().trim()
            } else {
                gd.uploader = ""
            }
            val gdd = gm.getElementById("gdd")
            gd.posted = ""
            gd.parent = ""
            gd.visible = ""
            gd.size = ""
            gd.pages = 0
            gd.favoriteCount = 0
            val es = gdd!!.child(0).child(0).children()
            es.forEach {
                parseDetailInfo(gd, it)
            }

            // Rating count
            val ratingCount = gm.getElementById("rating_count")
            if (null != ratingCount) {
                gd.ratingCount = ratingCount.text().trim().toIntOrDefault(0)
            } else {
                gd.ratingCount = 0
            }

            // Rating
            val ratingLabel = gm.getElementById("rating_label")
            if (null != ratingLabel) {
                val ratingStr = ratingLabel.text().trim()
                if ("Not Yet Rated" == ratingStr) {
                    gd.rating = -1.0f
                } else {
                    val index = ratingStr.indexOf(' ')
                    if (index == -1 || index >= ratingStr.length) {
                        gd.rating = 0f
                    } else {
                        gd.rating = ratingStr.substring(index + 1).toFloatOrDefault(0f)
                    }
                }
            } else {
                gd.rating = -1.0f
            }

            // isFavorited
            val gdf = gm.getElementById("gdf")
            if (gdf != null) {
                val favoriteName = gdf.text().trim()
                if (favoriteName != "Add to Favorites") {
                    gd.favoriteName = gdf.text().trim()
                    PATTERN_FAVORITE_SLOT.find(body)?.run {
                        gd.favoriteSlot = ((groupValues[1].toIntOrNull() ?: 2) - 2) / 19
                    }
                }
            }
            if (gd.favoriteSlot == NOT_FAVORITED && EhDB.containLocalFavorites(gd.gid)) {
                gd.favoriteSlot = LOCAL_FAVORITED
            }
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            throw ParseException("Can't parse gallery detail", body)
        }

        // newer version
        d.getElementById("gnd")?.run {
            val dates = PATTERN_NEWER_DATE.findAll(body).map { it.groupValues[1] }.toList()
            select("a").forEachIndexed { index, element ->
                val gi = BaseGalleryInfo()
                val result = GalleryDetailUrlParser.parse(element.attr("href"))
                if (result != null) {
                    gi.gid = result.gid
                    gi.token = result.token
                    gi.title = element.text().trim()
                    gi.posted = dates[index]
                    gd.newerVersions.add(gi)
                }
            }
        }
    }

    private fun parseDetailInfo(gd: GalleryDetail, e: Element) {
        val es = e.children()
        if (es.size < 2) {
            return
        }
        val key = es[0].text().trim()
        val value = es[1].ownText().trim()
        if (key.startsWith("Posted")) {
            gd.posted = value
        } else if (key.startsWith("Parent")) {
            val a = es[1].children().first()
            if (a != null) {
                gd.parent = a.attr("href")
            }
        } else if (key.startsWith("Visible")) {
            gd.visible = value
        } else if (key.startsWith("Language")) {
            gd.language = value
        } else if (key.startsWith("File Size")) {
            gd.size = value
        } else if (key.startsWith("Length")) {
            val index = value.indexOf(' ')
            if (index >= 0) {
                gd.pages = value.substring(0, index).toIntOrDefault(1)
            } else {
                gd.pages = 1
            }
        } else if (key.startsWith("Favorited")) {
            when (value) {
                "Never" -> gd.favoriteCount = 0
                "Once" -> gd.favoriteCount = 1
                else -> {
                    val index = value.indexOf(' ')
                    if (index == -1) {
                        gd.favoriteCount = 0
                    } else {
                        gd.favoriteCount = value.substring(0, index).toIntOrDefault(0)
                    }
                }
            }
        }
    }

    private fun parseTagGroup(element: Element): GalleryTagGroup? {
        return try {
            var nameSpace = element.child(0).text()
            // Remove last ':'
            nameSpace = nameSpace.substring(0, nameSpace.length - 1)
            val group = GalleryTagGroup(nameSpace)
            val tags = element.child(1).children()
            tags.forEach {
                var tag = it.text()
                // Sometimes parody tag is followed with '|' and english translate, just remove them
                val index = tag.indexOf('|')
                if (index >= 0) {
                    tag = tag.substring(0, index).trim()
                }
                if (it.className() == "gtw") {
                    tag = "_$tag" // weak tag
                }
                group.add(tag)
            }
            if (group.size > 0) group else null
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Parse tag groups with html parser
     */
    private fun parseTagGroups(document: Document): Array<GalleryTagGroup> {
        return try {
            val taglist = document.getElementById("taglist")!!
            val tagGroups = taglist.child(0).child(0).children()
            parseTagGroups(tagGroups)
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            e.printStackTrace()
            EMPTY_GALLERY_TAG_GROUP_ARRAY
        }
    }

    private fun parseTagGroups(trs: Elements): Array<GalleryTagGroup> {
        return try {
            trs.mapNotNull { parseTagGroup(it) }.toTypedArray()
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            e.printStackTrace()
            EMPTY_GALLERY_TAG_GROUP_ARRAY
        }
    }

    private suspend fun parseComment(element: Element): GalleryComment? {
        return try {
            val comment = GalleryComment()
            // Id
            val a = element.previousElementSibling()
            val name = a!!.attr("name")
            comment.id = name trimAnd { substring(1).toInt().toLong() }
            // Editable, vote up and vote down
            val c4 = element.getElementsByClass("c4").first()
            if (null != c4) {
                if ("Uploader Comment" == c4.text()) {
                    comment.uploader = true
                }
                for (e in c4.children()) {
                    when (e.text()) {
                        "Vote+" -> {
                            comment.voteUpAble = true
                            comment.voteUpEd = e.attr("style").trim().isNotEmpty()
                        }

                        "Vote-" -> {
                            comment.voteDownAble = true
                            comment.voteDownEd = e.attr("style").trim().isNotEmpty()
                        }

                        "Edit" -> comment.editable = true
                    }
                }
            }
            // Vote state
            val c7 = element.getElementsByClass("c7").first()
            if (null != c7) {
                comment.voteState = c7.text().trim()
            }
            // Score
            val c5 = element.getElementsByClass("c5").first()
            if (null != c5) {
                val es = c5.children()
                if (!es.isEmpty()) {
                    comment.score = es[0].text().trim().toIntOrDefault(0)
                }
            }
            // time
            val c3 = element.getElementsByClass("c3").first()
            val temp = c3!!.ownText()
            val time = if (temp.endsWith(':')) {
                // user
                comment.user = c3.child(0).text()
                temp.substring("Posted on ".length, temp.length - " by:".length)
            } else {
                temp.substring("Posted on ".length)
            }
            comment.time = Instant.from(WEB_COMMENT_DATE_FORMAT.parse(time)).toEpochMilli()
            // comment
            val c6 = element.getElementsByClass("c6").first()
            // fix underline support
            for (e in c6!!.children()) {
                if ("span" == e.tagName() && "text-decoration:underline;" == e.attr("style")) {
                    e.tagName("u")
                }
            }
            comment.comment = c6.html()
            // filter comment
            if (!comment.uploader) {
                val sEhFilter = EhFilter
                if (sEhFilter.filterCommenter(comment.user!!) || sEhFilter.filterComment(comment.comment!!)) {
                    return null
                }
            }
            // last edited
            val c8 = element.getElementsByClass("c8").first()
            if (c8 != null) {
                val e = c8.children().first()
                if (e != null) {
                    comment.lastEdited =
                        Instant.from(WEB_COMMENT_DATE_FORMAT.parse(e.text())).toEpochMilli()
                }
            }
            comment
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Parse comments with html parser
     */
    suspend fun parseComments(document: Document): GalleryCommentList {
        return try {
            val cdiv = document.getElementById("cdiv")!!
            val c1s = cdiv.getElementsByClass("c1")
            val list = c1s.mapNotNull { parseComment(it) }
            val chd = cdiv.getElementById("chd")
            var hasMore = false
            NodeTraversor.traverse(
                object : NodeVisitor {
                    override fun head(node: Node, depth: Int) {
                        if (node is Element && node.text() == "click to show all") {
                            hasMore = true
                        }
                    }

                    override fun tail(node: Node, depth: Int) {}
                },
                chd!!,
            )
            GalleryCommentList(list.toTypedArray(), hasMore)
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            e.printStackTrace()
            EMPTY_GALLERY_COMMENT_ARRAY
        }
    }

    /**
     * Parse preview pages with regular expressions
     */
    fun parsePreviewPages(body: String): Int {
        return PATTERN_PREVIEW_PAGES.find(body)?.groupValues?.get(1)?.toIntOrNull()
            ?: throw ParseException("Parse preview page count error", body)
    }

    /**
     * Parse pages with regular expressions
     */
    fun parsePages(body: String): Int {
        return PATTERN_PAGES.find(body)?.groupValues?.get(1)?.toIntOrNull()
            ?: throw ParseException("Parse pages error", body)
    }

    fun parsePreviewList(body: String): Pair<List<GalleryPreview>, List<String>> {
        return runCatching { parseNormalPreview(body) }.getOrElse { parseLargePreview(body) }
    }

    private fun parseLargePreview(body: String): Pair<List<GalleryPreview>, List<String>> {
        check(PATTERN_LARGE_PREVIEW.containsMatchIn(body))
        return PATTERN_LARGE_PREVIEW.findAll(body).unzip {
            val position = it.groupValues[2].toInt() - 1
            val imageKey = getThumbKey(it.groupValues[3].trim())
            val pageUrl = it.groupValues[1].trim()
            LargeGalleryPreview(imageKey, position) to pageUrl
        }.run {
            first.toList() to second.toList()
        }
    }

    private fun parseNormalPreview(body: String): Pair<List<GalleryPreview>, List<String>> {
        check(PATTERN_NORMAL_PREVIEW.containsMatchIn(body))
        return PATTERN_NORMAL_PREVIEW.findAll(body).unzip {
            val position = it.groupValues[6].toInt() - 1
            val imageKey = getThumbKey(it.groupValues[3].trim())
            val xOffset = it.groupValues[4].toIntOrNull() ?: 0
            val width = it.groupValues[1].toInt()
            val height = it.groupValues[2].toInt()
            val pageUrl = it.groupValues[5].trim()
            NormalGalleryPreview(imageKey, position, xOffset, width, height) to pageUrl
        }.run {
            first.toList() to second.toList()
        }
    }
}
