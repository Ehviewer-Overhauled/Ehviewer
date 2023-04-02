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

import com.hippo.ehviewer.client.exception.EhException
import com.hippo.util.ExceptionUtils
import org.json.JSONArray

object GalleryMultiPageViewerPTokenParser {
    fun parse(body: String): List<String> {
        val imagelist = body.substring(
            body.indexOf("var imagelist = ") + 16,
            body.indexOf(";", body.indexOf("var imagelist = ")),
        )
        return runCatching {
            val ja = JSONArray(imagelist)
            (0 until ja.length()).map { ja.getJSONObject(it).getString("k") }
        }.getOrElse {
            ExceptionUtils.throwIfFatal(it)
            throw EhException(body)
        }
    }
}
