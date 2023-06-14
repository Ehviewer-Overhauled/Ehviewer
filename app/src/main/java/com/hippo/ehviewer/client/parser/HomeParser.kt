package com.hippo.ehviewer.client.parser

import android.os.Parcelable
import com.hippo.ehviewer.client.exception.ParseException
import kotlinx.parcelize.Parcelize

object HomeParser {
    private val PATTERN_FUNDS = Regex("Available: ([\\d,]+) Credits.*Available: ([\\d,]+) kGP", RegexOption.DOT_MATCHES_ALL)
    private const val RESET_SUCCEED = "Image limit was successfully reset."

    fun parse(body: String): Limits {
        val value = parseLimit(body)
        if (value[0] == -1) throw ParseException("Parse image limits error", body)
        return Limits(value[0], value[1], value[2])
    }

    fun parseResetLimits(body: String): Limits? {
        return if (body.contains(RESET_SUCCEED)) {
            null
        } else {
            parse(body)
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

    @Parcelize
    data class Limits(val current: Int = 0, val maximum: Int = 0, val resetCost: Int = 0) : Parcelable

    @Parcelize
    data class Funds(val fundsGP: Int, val fundsC: Int) : Parcelable
    class Result(val limits: Limits, val funds: Funds)
}

private external fun parseLimit(body: String): IntArray
