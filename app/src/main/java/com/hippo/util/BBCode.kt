package com.hippo.util

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.*

object BBCode {
    fun Spanned.toBBCode(): String {
        val out = StringBuilder()
        withinParagraph(out, this, this.length)
        return out.toString()
    }

    private fun withinParagraph(out: StringBuilder, text: Spanned, end: Int) {
        var next: Int
        var i = 0
        while (i < text.length) {
            next = text.nextSpanTransition(i, end, CharacterStyle::class.java)
            val style = text.getSpans(i, next, CharacterStyle::class.java)
            for (j in style.indices) {
                if (style[j] is StyleSpan) {
                    val s = (style[j] as StyleSpan).style
                    if (s and Typeface.BOLD != 0) {
                        out.append("[b]")
                    }
                    if (s and Typeface.ITALIC != 0) {
                        out.append("[i]")
                    }
                }
                if (style[j] is UnderlineSpan) {
                    out.append("[u]")
                }
                if (style[j] is StrikethroughSpan) {
                    out.append("[s]")
                }
                if (style[j] is URLSpan) {
                    out.append("[url=")
                    out.append((style[j] as URLSpan).url)
                    out.append("]")
                }
                if (style[j] is ImageSpan) {
                    out.append("[img]")
                    out.append((style[j] as ImageSpan).source)
                    out.append("[/img]")
                }
            }
            out.append(text.subSequence(i, next))
            for (j in style.indices.reversed()) {
                if (style[j] is URLSpan) {
                    out.append("[/url]")
                }
                if (style[j] is StrikethroughSpan) {
                    out.append("[/s]")
                }
                if (style[j] is UnderlineSpan) {
                    out.append("[/u]")
                }
                if (style[j] is StyleSpan) {
                    val s = (style[j] as StyleSpan).style
                    if (s and Typeface.BOLD != 0) {
                        out.append("[/b]")
                    }
                    if (s and Typeface.ITALIC != 0) {
                        out.append("[/i]")
                    }
                }
            }
            i = next
        }
    }
}