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

import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.client.EhFilter
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.EhUtils.getCategory
import com.hippo.ehviewer.client.EhUtils.handleThumbUrlResolution
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.client.data.GalleryCommentList
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryTagGroup
import com.hippo.ehviewer.client.data.LargePreviewSet
import com.hippo.ehviewer.client.data.NormalPreviewSet
import com.hippo.ehviewer.client.data.PreviewSet
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.OffensiveException
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.client.exception.PiningException
import com.hippo.util.ExceptionUtils
import com.hippo.util.JsoupUtils
import com.hippo.yorozuya.NumberUtils
import com.hippo.yorozuya.StringUtils
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
import java.util.regex.Pattern

object GalleryDetailParser {
    private val PATTERN_ERROR = Pattern.compile("<div class=\"d\">\n<p>([^<]+)</p>")
    private val PATTERN_DETAIL = Pattern.compile(
        "var gid = (\\d+);.+?var token = \"([a-f0-9]+)\";.+?var apiuid = ([\\-\\d]+);.+?var apikey = \"([a-f0-9]+)\";",
        Pattern.DOTALL
    )
    private val PATTERN_TORRENT =
        Pattern.compile("<a[^<>]*onclick=\"return popUp\\('([^']+)'[^)]+\\)\">Torrent Download[^<]+(\\d+)[^<]+</a")
    private val PATTERN_ARCHIVE =
        Pattern.compile("<a[^<>]*onclick=\"return popUp\\('([^']+)'[^)]+\\)\">Archive Download</a>")
    private val PATTERN_COVER = Pattern.compile("width:(\\d+)px; height:(\\d+)px.+?url\\((.+?)\\)")
    private val PATTERN_PAGES =
        Pattern.compile("<tr><td[^<>]*>Length:</td><td[^<>]*>([\\d,]+) pages</td></tr>")
    private val PATTERN_PREVIEW_PAGES =
        Pattern.compile("<td[^>]+><a[^>]+>([\\d,]+)</a></td><td[^>]+>(?:<a[^>]+>)?&gt;(?:</a>)?</td>")
    private val PATTERN_NORMAL_PREVIEW =
        Pattern.compile("<div class=\"gdtm\"[^<>]*><div[^<>]*width:(\\d+)[^<>]*height:(\\d+)[^<>]*\\((.+?)\\)[^<>]*-(\\d+)px[^<>]*><a[^<>]*href=\"(.+?)\"[^<>]*><img alt=\"([\\d,]+)\"")
    private val PATTERN_LARGE_PREVIEW =
        Pattern.compile("<div class=\"gdtl\".+?<a href=\"(.+?)\"><img alt=\"([\\d,]+)\".+?src=\"(.+?)\"")
    private val PATTERN_NEWER_DATE = Pattern.compile(", added (.+?)<br />")
    private val PATTERN_FAVORITE_SLOT =
        Pattern.compile("/fav.png\\); background-position:0px -(\\d+)px")
    private val EMPTY_GALLERY_TAG_GROUP_ARRAY = arrayOf<GalleryTagGroup>()
    private val EMPTY_GALLERY_COMMENT_ARRAY = GalleryCommentList(arrayOf(), false)
    private val WEB_COMMENT_DATE_FORMAT = DateTimeFormatter
        .ofPattern("dd MMMM yyyy, HH:mm", Locale.US).withZone(ZoneOffset.UTC)
    private const val OFFENSIVE_STRING =
        "<p>(And if you choose to ignore this warning, you lose all rights to complain about it in the future.)</p>"
    private const val PINING_STRING = "<p>This gallery is pining for the fjords.</p>"
    @Throws(EhException::class)
    fun parse(body: String): GalleryDetail {
        if (body.contains(OFFENSIVE_STRING)) {
            throw OffensiveException()
        }
        if (body.contains(PINING_STRING)) {
            throw PiningException()
        }

        // Error info
        val m = PATTERN_ERROR.matcher(body)
        if (m.find()) {
            throw EhException(m.group(1))
        }
        val galleryDetail = GalleryDetail()
        val document = Jsoup.parse(body)
        parseDetail(galleryDetail, document, body)
        galleryDetail.tags = parseTagGroups(document)
        galleryDetail.comments = parseComments(document)
        galleryDetail.previewPages = parsePreviewPages(document, body)
        galleryDetail.previewSet = parsePreviewSet(document, body)

        // Generate simpleLanguage for local favorites
        galleryDetail.generateSLang()
        return galleryDetail
    }

    @Throws(ParseException::class)
    private fun parseDetail(gd: GalleryDetail, d: Document, body: String) {
        var matcher = PATTERN_DETAIL.matcher(body)
        if (matcher.find()) {
            gd.gid = NumberUtils.parseLongSafely(matcher.group(1), -1L)
            gd.token = matcher.group(2)
            gd.apiUid = NumberUtils.parseLongSafely(matcher.group(3), -1L)
            gd.apiKey = matcher.group(4)
        } else {
            throw ParseException("Can't parse gallery detail", body)
        }
        if (gd.gid == -1L) {
            throw ParseException("Can't parse gallery detail", body)
        }
        matcher = PATTERN_TORRENT.matcher(body)
        if (matcher.find()) {
            gd.torrentUrl = StringUtils.unescapeXml(StringUtils.trim(matcher.group(1)))
            gd.torrentCount = NumberUtils.parseIntSafely(matcher.group(2), 0)
        } else {
            gd.torrentCount = 0
            gd.torrentUrl = ""
        }
        matcher = PATTERN_ARCHIVE.matcher(body)
        if (matcher.find()) {
            gd.archiveUrl = StringUtils.unescapeXml(StringUtils.trim(matcher.group(1)))
        } else {
            gd.archiveUrl = ""
        }
        try {
            val gm = JsoupUtils.getElementByClass(d, "gm")

            // Thumb url
            val gd1 = gm!!.getElementById("gd1")
            try {
                gd.thumb = parseCoverStyle(
                    StringUtils.trim(
                        gd1!!.child(0).attr("style")
                    )
                )
            } catch (e: Throwable) {
                ExceptionUtils.throwIfFatal(e)
                gd.thumb = ""
            }

            // Title
            val gn = gm.getElementById("gn")
            if (null != gn) {
                gd.title = StringUtils.trim(gn.text())
            } else {
                gd.title = ""
            }

            // Jpn title
            val gj = gm.getElementById("gj")
            if (null != gj) {
                gd.titleJpn = StringUtils.trim(gj.text())
            } else {
                gd.titleJpn = ""
            }

            // Category
            val gdc = gm.getElementById("gdc")
            try {
                var ce = JsoupUtils.getElementByClass(gdc, "cn")
                if (ce == null) {
                    ce = JsoupUtils.getElementByClass(gdc, "cs")
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
                gd.uploader = StringUtils.trim(gdn.text())
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
                gd.ratingCount = NumberUtils.parseIntSafely(
                    StringUtils.trim(ratingCount.text()), 0
                )
            } else {
                gd.ratingCount = 0
            }

            // Rating
            val ratingLabel = gm.getElementById("rating_label")
            if (null != ratingLabel) {
                val ratingStr = StringUtils.trim(ratingLabel.text())
                if ("Not Yet Rated" == ratingStr) {
                    gd.rating = -1.0f
                } else {
                    val index = ratingStr.indexOf(' ')
                    if (index == -1 || index >= ratingStr.length) {
                        gd.rating = 0f
                    } else {
                        gd.rating = NumberUtils.parseFloatSafely(ratingStr.substring(index + 1), 0f)
                    }
                }
            } else {
                gd.rating = -1.0f
            }

            // isFavorited
            val gdf = gm.getElementById("gdf")
            gd.isFavorited = false
            if (gdf != null) {
                val favoriteName = StringUtils.trim(gdf.text())
                if (favoriteName == "Add to Favorites") {
                    gd.favoriteName = null
                } else {
                    gd.isFavorited = true
                    gd.favoriteName = StringUtils.trim(gdf.text())
                    matcher = PATTERN_FAVORITE_SLOT.matcher(body)
                    if (matcher.find()) {
                        gd.favoriteSlot = (NumberUtils.parseIntSafely(matcher.group(1), 2) - 2) / 19
                    }
                }
            }
            if (gd.favoriteSlot == -2 && EhDB.containLocalFavorites(gd.gid)) {
                gd.favoriteSlot = -1
            }
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            throw ParseException("Can't parse gallery detail", body)
        }

        // newer version
        try {
            val gnd = d.getElementById("gnd")
            if (gnd != null) {
                matcher = PATTERN_NEWER_DATE.matcher(body)
                val dates = ArrayList<String?>()
                while (matcher.find()) {
                    dates.add(matcher.group(1))
                }
                val elements = gnd.select("a")
                for (i in elements.indices) {
                    val element = elements[i]
                    val gi: GalleryInfo = BaseGalleryInfo()
                    val result = GalleryDetailUrlParser.parse(element.attr("href"))
                    if (result != null) {
                        gi.gid = result.gid
                        gi.token = result.token
                        gi.title = StringUtils.trim(element.text())
                        gi.posted = dates[i]
                        gd.newerVersions.add(gi)
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    // width:250px; height:356px; background:transparent url(https://exhentai.org/t/fe/1f/fe1fcfa9bf8fba2f03982eda0aa347cc9d6a6372-145921-1050-1492-jpg_250.jpg) 0 0 no-repeat
    private fun parseCoverStyle(str: String): String? {
        val matcher = PATTERN_COVER.matcher(str)
        return if (matcher.find()) {
            handleThumbUrlResolution(matcher.group(3))
        } else {
            ""
        }
    }

    private fun parseDetailInfo(gd: GalleryDetail, e: Element) {
        val es = e.children()
        if (es.size < 2) {
            return
        }
        val key = StringUtils.trim(es[0].text())
        val value = StringUtils.trim(es[1].ownText())
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
                gd.pages = NumberUtils.parseIntSafely(value.substring(0, index), 1)
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
                        gd.favoriteCount = NumberUtils.parseIntSafely(value.substring(0, index), 0)
                    }
                }
            }
        }
    }

    private fun parseTagGroup(element: Element): GalleryTagGroup? {
        return try {
            val group = GalleryTagGroup()
            var nameSpace = element.child(0).text()
            // Remove last ':'
            nameSpace = nameSpace.substring(0, nameSpace.length - 1)
            group.groupName = nameSpace
            val tags = element.child(1).children()
            tags.forEach {
                var tag = it.text()
                // Sometimes parody tag is followed with '|' and english translate, just remove them
                val index = tag.indexOf('|')
                if (index >= 0) {
                    tag = tag.substring(0, index).trim()
                }
                group.addTag(tag)
            }
            if (group.size() > 0) group else null
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

    private fun parseComment(element: Element): GalleryComment? {
        return try {
            val comment = GalleryComment()
            // Id
            val a = element.previousElementSibling()
            val name = a!!.attr("name")
            comment.id = StringUtils.trim(name).substring(1).toInt().toLong()
            // Editable, vote up and vote down
            val c4 = JsoupUtils.getElementByClass(element, "c4")
            if (null != c4) {
                if ("Uploader Comment" == c4.text()) {
                    comment.uploader = true
                }
                for (e in c4.children()) {
                    when (e.text()) {
                        "Vote+" -> {
                            comment.voteUpAble = true
                            comment.voteUpEd = StringUtils.trim(e.attr("style")).isNotEmpty()
                        }

                        "Vote-" -> {
                            comment.voteDownAble = true
                            comment.voteDownEd = StringUtils.trim(e.attr("style")).isNotEmpty()
                        }

                        "Edit" -> comment.editable = true
                    }
                }
            }
            // Vote state
            val c7 = JsoupUtils.getElementByClass(element, "c7")
            if (null != c7) {
                comment.voteState = StringUtils.trim(c7.text())
            }
            // Score
            val c5 = JsoupUtils.getElementByClass(element, "c5")
            if (null != c5) {
                val es = c5.children()
                if (!es.isEmpty()) {
                    comment.score = NumberUtils.parseIntSafely(
                        StringUtils.trim(es[0].text()), 0
                    )
                }
            }
            // time
            val c3 = JsoupUtils.getElementByClass(element, "c3")
            var temp = c3!!.ownText()
            temp = temp.substring("Posted on ".length, temp.length - " by:".length)
            comment.time = Instant.from(WEB_COMMENT_DATE_FORMAT.parse(temp)).toEpochMilli()
            // user
            comment.user = c3.child(0).text()
            // comment
            val c6 = JsoupUtils.getElementByClass(element, "c6")
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
                if (!sEhFilter.filterCommenter(comment.user) || !sEhFilter.filterComment(comment.comment)) {
                    return null
                }
            }
            // last edited
            val c8 = JsoupUtils.getElementByClass(element, "c8")
            if (c8 != null) {
                val e = c8.children().first()
                if (e != null) {
                    comment.lastEdited =
                        Instant.from(WEB_COMMENT_DATE_FORMAT.parse(temp)).toEpochMilli()
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
    fun parseComments(document: Document): GalleryCommentList {
        return try {
            val cdiv = document.getElementById("cdiv")!!
            val c1s = cdiv.getElementsByClass("c1")
            val list = c1s.mapNotNull { parseComment(it) }
            val chd = cdiv.getElementById("chd")
            var hasMore = false
            NodeTraversor.traverse(object : NodeVisitor {
                override fun head(node: Node, depth: Int) {
                    if (node is Element && node.text() == "click to show all") {
                        hasMore = true
                    }
                }

                override fun tail(node: Node, depth: Int) {}
            }, chd!!)
            GalleryCommentList(list.toTypedArray(), hasMore)
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            e.printStackTrace()
            EMPTY_GALLERY_COMMENT_ARRAY
        }
    }

    /**
     * Parse preview pages with html parser
     */
    @Throws(ParseException::class)
    fun parsePreviewPages(document: Document, body: String?): Int {
        return try {
            val ptt = document.getElementsByClass("ptt").first()!!
            val elements = ptt.child(0).child(0).children()
            elements[elements.size - 2].text().toInt()
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            e.printStackTrace()
            throw ParseException("Can't parse preview pages", body!!)
        }
    }

    /**
     * Parse preview pages with regular expressions
     */
    @JvmStatic
    @Throws(ParseException::class)
    fun parsePreviewPages(body: String): Int {
        val m = PATTERN_PREVIEW_PAGES.matcher(body)
        var previewPages = -1
        if (m.find()) {
            previewPages = ParserUtils.parseInt(m.group(1), -1)
        }
        if (previewPages <= 0) {
            throw ParseException("Parse preview page count error", body)
        }
        return previewPages
    }

    /**
     * Parse pages with regular expressions
     */
    @JvmStatic
    @Throws(ParseException::class)
    fun parsePages(body: String): Int {
        var pages = -1
        val m = PATTERN_PAGES.matcher(body)
        if (m.find()) {
            pages = ParserUtils.parseInt(m.group(1), -1)
        }
        if (pages < 0) {
            throw ParseException("Parse pages error", body)
        }
        return pages
    }

    @Throws(ParseException::class)
    fun parsePreviewSet(d: Document, body: String): PreviewSet {
        return try {
            parseLargePreviewSet(d, body)
        } catch (e: ParseException) {
            parseNormalPreviewSet(body)
        }
    }

    @JvmStatic
    @Throws(ParseException::class)
    fun parsePreviewSet(body: String): PreviewSet {
        return try {
            parseLargePreviewSet(body)
        } catch (e: ParseException) {
            parseNormalPreviewSet(body)
        }
    }

    /**
     * Parse large previews with regular expressions
     */
    @Throws(ParseException::class)
    private fun parseLargePreviewSet(d: Document, body: String): LargePreviewSet {
        return try {
            val largePreviewSet = LargePreviewSet()
            val gdt = d.getElementById("gdt")!!
            val gdtls = gdt.getElementsByClass("gdtl")
            val n = gdtls.size
            if (n == 0) {
                throw ParseException("Can't parse large preview", body)
            }
            for (i in 0 until n) {
                var element = gdtls[i].child(0)
                val pageUrl = element.attr("href")
                element = element.child(0)
                val imageUrl = element.attr("src")
                val index = element.attr("alt").toInt() - 1
                largePreviewSet.addItem(index, imageUrl, pageUrl)
            }
            largePreviewSet
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            throw ParseException("Can't parse large preview", body)
        }
    }

    /**
     * Parse large previews with regular expressions
     */
    @Throws(ParseException::class)
    private fun parseLargePreviewSet(body: String): LargePreviewSet {
        val m = PATTERN_LARGE_PREVIEW.matcher(body)
        val largePreviewSet = LargePreviewSet()
        while (m.find()) {
            val index = ParserUtils.parseInt(m.group(2), 0) - 1
            if (index < 0) {
                continue
            }
            val imageUrl = ParserUtils.trim(m.group(3))
            val pageUrl = ParserUtils.trim(m.group(1))
            largePreviewSet.addItem(index, imageUrl, pageUrl)
        }
        if (largePreviewSet.size() == 0) {
            throw ParseException("Can't parse large preview", body)
        }
        return largePreviewSet
    }

    /**
     * Parse normal previews with regular expressions
     */
    @Throws(ParseException::class)
    private fun parseNormalPreviewSet(body: String): NormalPreviewSet {
        val m = PATTERN_NORMAL_PREVIEW.matcher(body)
        val normalPreviewSet = NormalPreviewSet()
        while (m.find()) {
            val position = ParserUtils.parseInt(m.group(6), 0) - 1
            if (position < 0) {
                continue
            }
            val imageUrl = ParserUtils.trim(m.group(3))
            val xOffset = ParserUtils.parseInt(m.group(4), 0)
            val yOffset = 0
            val width = ParserUtils.parseInt(m.group(1), 0)
            if (width <= 0) {
                continue
            }
            val height = ParserUtils.parseInt(m.group(2), 0)
            if (height <= 0) {
                continue
            }
            val pageUrl = ParserUtils.trim(m.group(5))
            normalPreviewSet.addItem(position, imageUrl, xOffset, yOffset, width, height, pageUrl)
        }
        if (normalPreviewSet.size() == 0) {
            throw ParseException("Can't parse normal preview", body)
        }
        return normalPreviewSet
    }
}