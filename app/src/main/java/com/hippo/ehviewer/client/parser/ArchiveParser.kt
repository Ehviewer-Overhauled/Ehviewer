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

import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.exception.NoHAtHClientException
import org.jsoup.Jsoup
import java.util.function.IntFunction
import java.util.regex.Pattern

object ArchiveParser {
    private val PATTERN_ARCHIVE_URL =
        Pattern.compile("<strong>(.*)</strong>.*<a href=\"([^\"]*)\">Click Here To Start Downloading</a>")
    private val PATTERN_CURRENT_FUNDS =
        Pattern.compile("<p>([\\d,]+) GP \\[[^]]*] &nbsp; ([\\d,]+) Credits \\[[^]]*]</p>")
    private val PATTERN_FUNDS =
        Pattern.compile("Available: ([\\d,]+) Credits.*Available: ([\\d,]+) kGP", Pattern.DOTALL)
    private val PATTERN_HATH_FORM =
        Pattern.compile("<form id=\"hathdl_form\" action=\"[^\"]*?or=([^=\"]*?)\" method=\"post\">")
    private val PATTERN_HATH_ARCHIVE =
        Pattern.compile("<p><a href=\"[^\"]*\" onclick=\"return do_hathdl\\('([0-9]+|org)'\\)\">([^<]+)</a></p>\\s*<p>([\\w. ]+)</p>\\s*<p>([\\w. ]+)</p>")
    private val PATTERN_NEED_HATH_CLIENT =
        Pattern.compile("You must have a H@H client assigned to your account to use this feature\\.")

    fun parse(body: String): Result? {
        var m = PATTERN_HATH_FORM.matcher(body)
        if (!m.find()) {
            return null
        }
        val paramOr = m.group(1)
        val archiveList = ArrayList<Archive>()
        val d = Jsoup.parse(body)
        val es = d.select("#db>div>div")
        for (e in es) {
            if (e.childrenSize() > 0 && !e.attr("style").contains("color:#CCCCCC")) {
                try {
                    val res = e.selectFirst("form>input")!!.attr("value")
                    val name = e.selectFirst("form>div>input")!!.attr("value")
                    val size = e.selectFirst("p>strong")!!.text()
                    val cost = e.selectFirst("div>strong")!!.text().replace(",", "")
                    val item = Archive(res, name, size, cost, false)
                    archiveList.add(item)
                } catch (ex: NullPointerException) {
                    ex.printStackTrace()
                }
            }
        }
        m = PATTERN_HATH_ARCHIVE.matcher(body)
        while (m.find()) {
            val res = ParserUtils.trim(m.group(1))
            val name = ParserUtils.trim(m.group(2))
            val size = ParserUtils.trim(m.group(3))
            val cost = ParserUtils.trim(m.group(4))
            val item = Archive(res, name, size, cost, true)
            archiveList.add(item)
        }
        var funds: Funds? = null
        m = PATTERN_CURRENT_FUNDS.matcher(body)
        if (m.find()) {
            val fundsGP = ParserUtils.parseInt(m.group(1), 0)
            val fundsC = ParserUtils.parseInt(m.group(2), 0)
            funds = Funds(fundsGP, fundsC)
        }
        return Result(paramOr, archiveList, funds)
    }

    @Throws(NoHAtHClientException::class)
    fun parseArchiveUrl(body: String): String? {
        val m = PATTERN_NEED_HATH_CLIENT.matcher(body)
        if (m.find()) {
            throw NoHAtHClientException("No H@H client")
        }
        val d = Jsoup.parse(body)
        val a = d.selectFirst("#continue>a[href]")
        return if (a != null) {
            a.attr("href") + "?start=1"
        } else null

        // TODO: Check more errors
    }

    fun parseFunds(body: String): Funds? {
        val m = PATTERN_FUNDS.matcher(body)
        return if (m.find()) {
            val fundsC = ParserUtils.parseInt(m.group(1), 0)
            val fundsGP = ParserUtils.parseInt(m.group(2), 0) * 1000
            return Funds(fundsGP, fundsC)
        } else null
    }

    class Archive(
        val res: String,
        val name: String,
        val size: String,
        val cost: String,
        val isHAtH: Boolean
    ) {
        fun format(getString: IntFunction<String?>): String {
            return if (isHAtH) {
                val costStr = if (cost == "Free") getString.apply(R.string.archive_free) else cost
                "[H@H] $name [$size] [$costStr]"
            } else {
                val nameStr =
                    getString.apply(if (res == "org") R.string.archive_original else R.string.archive_resample)
                val costStr = if (cost == "Free!") getString.apply(R.string.archive_free) else cost
                "$nameStr [$size] [$costStr]"
            }
        }
    }

    class Funds(var fundsGP: Int, var fundsC: Int)

    class Result(val paramOr: String?, val archiveList: List<Archive>, var funds: Funds?)
}