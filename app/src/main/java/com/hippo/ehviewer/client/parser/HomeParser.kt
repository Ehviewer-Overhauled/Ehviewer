package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.exception.ParseException

object HomeParser {
    private val PATTERN_FUNDS =
        Regex("Available: ([\\d,]+) Credits.*Available: ([\\d,]+) kGP", RegexOption.DOT_MATCHES_ALL)

    fun parseFunds(body: String): Funds {
        PATTERN_FUNDS.find(body)?.groupValues?.run {
            val fundsC = ParserUtils.parseInt(get(1), 0)
            val fundsGP = ParserUtils.parseInt(get(2), 0) * 1000
            return Funds(fundsGP, fundsC)
        }
        throw ParseException("Parse funds error", body)
    }

    data class Funds(val fundsGP: Int, val fundsC: Int)
}