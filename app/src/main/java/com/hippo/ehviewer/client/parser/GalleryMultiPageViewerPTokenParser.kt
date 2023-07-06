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

import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.client.parseAs
import com.hippo.ehviewer.util.ExceptionUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object GalleryMultiPageViewerPTokenParser {
    private const val IMAGE_LIST_STRING = "var imagelist = "

    fun parse(body: String): List<String> {
        val index = body.indexOf(IMAGE_LIST_STRING)
        val imagelist = body.substring(index + IMAGE_LIST_STRING.length, body.indexOf(";", index))
        return runCatching {
            imagelist.parseAs<List<Item>>().map(Item::token)
        }.getOrElse {
            ExceptionUtils.throwIfFatal(it)
            throw ParseException("Parse pToken from MPV error", body, it)
        }
    }

    @Serializable
    data class Item(@SerialName("k") val token: String)
}
