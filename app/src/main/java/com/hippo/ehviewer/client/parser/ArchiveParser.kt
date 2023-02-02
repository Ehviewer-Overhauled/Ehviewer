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

import com.hippo.ehviewer.client.exception.NoHAtHClientException
import org.jsoup.Jsoup

object ArchiveParser {
    private val PATTERN_ARCHIVE_URL =
        Regex("<strong>(.*)</strong>.*<a href=\"([^\"]*)\">Click Here To Start Downloading</a>")
    private val PATTERN_CURRENT_FUNDS =
        Regex("<p>([\\d,]+) GP \\[[^]]*] &nbsp; ([\\d,]+) Credits \\[[^]]*]</p>")
    private val PATTERN_FUNDS =
        Regex("Available: ([\\d,]+) Credits.*Available: ([\\d,]+) kGP", RegexOption.DOT_MATCHES_ALL)
    private val PATTERN_HATH_FORM =
        Regex("<form id=\"hathdl_form\" action=\"[^\"]*?or=([^=\"]*?)\" method=\"post\">")
    private val PATTERN_HATH_ARCHIVE =
        Regex("<p><a href=\"[^\"]*\" onclick=\"return do_hathdl\\('([0-9]+|org)'\\)\">([^<]+)</a></p>\\s*<p>([\\w. ]+)</p>\\s*<p>([\\w. ]+)</p>")
    private val PATTERN_NEED_HATH_CLIENT =
        Regex("You must have a H@H client assigned to your account to use this feature\\.")

    fun parse(body: String): Result? {
        val m = PATTERN_HATH_FORM.find(body) ?: return null
        val paramOr = m.groupValues[1]
        val archiveList = ArrayList<Archive>()
        Jsoup.parse(body).select("#db>div>div").forEach { element ->
            if (element.childrenSize() > 0 && !element.attr("style").contains("color:#CCCCCC")) {
                runCatching {
                    val res = element.selectFirst("form>input")!!.attr("value")
                    val name = element.selectFirst("form>div>input")!!.attr("value")
                    val size = element.selectFirst("p>strong")!!.text()
                    val cost = element.selectFirst("div>strong")!!.text().replace(",", "")
                    Archive(res, name, size, cost, false)
                }.onSuccess {
                    archiveList.add(it)
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
        PATTERN_HATH_ARCHIVE.findAll(body).forEach { matchResult ->
            val (res, name, size, cost) = matchResult.groupValues.slice(1..4)
                .map { ParserUtils.trim(it) }
            val item = Archive(res, name, size, cost, true)
            archiveList.add(item)
        }
        val result = Result(paramOr, archiveList, null)
        PATTERN_CURRENT_FUNDS.find(body)?.groupValues?.run {
            val fundsGP = ParserUtils.parseInt(get(1), 0)
            val fundsC = ParserUtils.parseInt(get(2), 0)
            val funds = Funds(fundsGP, fundsC)
            result.funds = funds
        }
        return result
    }

    @Throws(NoHAtHClientException::class)
    fun parseArchiveUrl(body: String): String? {
        if (PATTERN_NEED_HATH_CLIENT.containsMatchIn(body)) throw NoHAtHClientException("No H@H client")
        return Jsoup.parse(body).selectFirst("#continue>a[href]")
            ?.let { it.attr("href") + "?start=1" }
        // TODO: Check more errors
    }

    fun parseFunds(body: String): Funds? {
        return PATTERN_FUNDS.find(body)?.groupValues?.run {
            val fundsC = ParserUtils.parseInt(get(1), 0)
            val fundsGP = ParserUtils.parseInt(get(2), 0) * 1000
            return Funds(fundsGP, fundsC)
        }
    }

    class Archive(
        val res: String,
        val name: String,
        val size: String,
        val cost: String,
        val isHAtH: Boolean
    )

    class Funds(var fundsGP: Int, var fundsC: Int)

    class Result(val paramOr: String?, val archiveList: List<Archive>, var funds: Funds?)
}
