package com.hippo.ehviewer.client.parser

import android.os.Parcelable
import com.hippo.ehviewer.client.exception.InsufficientFundsException
import com.hippo.ehviewer.client.exception.ParseException
import kotlinx.parcelize.Parcelize
import java.nio.ByteBuffer

object HomeParser {
    private val PATTERN_FUNDS = Regex("Available: ([\\d,]+) Credits.*Available: ([\\d,]+) kGP", RegexOption.DOT_MATCHES_ALL)
    private const val INSUFFICIENT_FUNDS = "Insufficient funds."

    fun parse(body: ByteBuffer) = parseLimit(body)

    fun parseResetLimits(body: String) {
        if (body.contains(INSUFFICIENT_FUNDS)) {
            throw InsufficientFundsException()
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
    data class Funds(val fundsGP: Int, val fundsC: Int) : Parcelable
    class Result(val limits: Limits, val funds: Funds)
}

@Parcelize
data class Limits(val current: Int, val maximum: Int, val resetCost: Int) : Parcelable

private external fun parseLimit(body: ByteBuffer, limit: Int = body.limit()): Limits
