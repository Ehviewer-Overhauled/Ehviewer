/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.hippo.ehviewer.client.parser

import com.hippo.util.ExceptionUtils
import com.hippo.util.JsoupUtils
import org.jsoup.Jsoup

object GalleryNotAvailableParser {
    fun parse(body: String): String? {
        return runCatching {
            val document = Jsoup.parse(body)
            val d = JsoupUtils.getElementByClass(document, "d")
            d!!.child(0).html().replace("<br>", "\n")
        }.getOrElse {
            ExceptionUtils.throwIfFatal(it)
            it.printStackTrace()
            null
        }
    }
}
