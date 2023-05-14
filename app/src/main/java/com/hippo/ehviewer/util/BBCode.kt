package com.hippo.ehviewer.util

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan

fun Spanned.toBBCode(): String {
    val text = this
    tailrec fun StringBuilder.processOneSpanTransition(cur: Int = 0) {
        val next = nextSpanTransition(cur, text.length, CharacterStyle::class.java)
        getSpans(cur, next, CharacterStyle::class.java).forEach {
            when (it) {
                is StyleSpan -> {
                    val s = it.style
                    if (s and Typeface.BOLD != 0) {
                        append("[b]")
                    }
                    if (s and Typeface.ITALIC != 0) {
                        append("[i]")
                    }
                }

                is UnderlineSpan -> append("[u]")
                is StrikethroughSpan -> append("[s]")
                is URLSpan -> {
                    append("[url=")
                    append(it.url)
                    append("]")
                }

                is ImageSpan -> {
                    append("[img]")
                    append(it.source)
                    append("[/img]")
                }
            }
        }
        append(text.subSequence(cur, next))
        getSpans(cur, next, CharacterStyle::class.java).reversed().forEach {
            when (it) {
                is StyleSpan -> {
                    val s = it.style
                    if (s and Typeface.BOLD != 0) {
                        append("[/b]")
                    }
                    if (s and Typeface.ITALIC != 0) {
                        append("[/i]")
                    }
                }

                is UnderlineSpan -> append("[/u]")
                is StrikethroughSpan -> append("[/s]")
                is URLSpan -> append("[/url]")
            }
        }
        if (next < text.length) processOneSpanTransition(next)
    }
    return StringBuilder().apply {
        processOneSpanTransition()
    }.toString()
}
