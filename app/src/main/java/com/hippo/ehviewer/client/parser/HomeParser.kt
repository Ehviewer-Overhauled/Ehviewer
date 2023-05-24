package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.exception.ParseException
import org.jsoup.nodes.Document

object HomeParser {
    private val PATTERN_FUNDS =
        Regex("Available: ([\\d,]+) Credits.*Available: ([\\d,]+) kGP", RegexOption.DOT_MATCHES_ALL)
    private const val RESET_SUCCEED = "Image limit was successfully reset."

    fun parse(doc: Document): Limits {
        doc.selectFirst("div.homebox")?.let {
            val es = it.select("p > strong")
            if (es.size == 3) {
                val current = ParserUtils.parseInt(es[0].text(), 0)
                val maximum = ParserUtils.parseInt(es[1].text(), 0)
                val resetCost = ParserUtils.parseInt(es[2].text(), 0)
                return Limits(current, maximum, resetCost)
            }
        }
        throw ParseException("Parse image limits error", doc.html())
    }

    fun parseResetLimits(body: String, doc: Document): Limits? {
        return if (body.contains(RESET_SUCCEED)) {
            null
        } else {
            parse(doc)
        }
    }

    fun parseFunds(body: String): Funds {
        PATTERN_FUNDS.find(body)?.groupValues?.run {
            val fundsC = ParserUtils.parseInt(get(1), 0)
            val fundsGP = ParserUtils.parseInt(get(2), 0) * 1000
            return Funds(fundsGP, fundsC)
        }
        throw ParseException("Parse funds error", body)
    }

    data class Limits(val current: Int = 0, val maximum: Int, val resetCost: Int = 0)
    data class Funds(val fundsGP: Int, val fundsC: Int)
    class Result(val limits: Limits, val funds: Funds)
}
