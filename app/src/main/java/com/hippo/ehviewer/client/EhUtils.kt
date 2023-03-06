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
package com.hippo.ehviewer.client

import android.text.TextUtils
import com.hippo.ehviewer.EhApplication.Companion.ehCookieStore
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryInfo
import java.util.regex.Pattern

object EhUtils {
    const val NONE = -1 // Use it for homepage
    const val MISC = 0x1
    const val DOUJINSHI = 0x2
    const val MANGA = 0x4
    const val ARTIST_CG = 0x8
    const val GAME_CG = 0x10
    const val IMAGE_SET = 0x20
    const val COSPLAY = 0x40
    const val ASIAN_PORN = 0x80
    const val NON_H = 0x100
    const val WESTERN = 0x200
    const val ALL_CATEGORY = 0x3ff
    const val PRIVATE = 0x400
    const val UNKNOWN = 0x800

    // https://youtrack.jetbrains.com/issue/KT-4749
    private const val BG_COLOR_DOUJINSHI = 0xfff44336u
    private const val BG_COLOR_MANGA = 0xffff9800u
    private const val BG_COLOR_ARTIST_CG = 0xfffbc02du
    private const val BG_COLOR_GAME_CG = 0xff4caf50u
    private const val BG_COLOR_WESTERN = 0xff8bc34au
    private const val BG_COLOR_NON_H = 0xff2196f3u
    private const val BG_COLOR_IMAGE_SET = 0xff3f51b5u
    private const val BG_COLOR_COSPLAY = 0xff9c27b0u
    private const val BG_COLOR_ASIAN_PORN = 0xff9575cdu
    private const val BG_COLOR_MISC = 0xfff06292u
    private const val BG_COLOR_UNKNOWN = 0xff000000u

    // Remove [XXX], (XXX), {XXX}, ~XXX~ stuff
    private val PATTERN_TITLE_PREFIX = Pattern.compile(
        "^(?:\\([^)]*\\)|\\[[^]]*]|\\{[^}]*\\}|~[^~]*~|\\s+)*"
    )

    // Remove [XXX], (XXX), {XXX}, ~XXX~ stuff and something like ch. 1-23
    private val PATTERN_TITLE_SUFFIX = Pattern.compile(
        "(?:\\s+ch.[\\s\\d-]+)?(?:\\([^)]*\\)|\\[[^]]*]|\\{[^}]*\\}|~[^~]*~|\\s+)*$",
        Pattern.CASE_INSENSITIVE
    )

    private val CATEGORY_VALUES = hashMapOf(
        MISC to arrayOf("misc"),
        DOUJINSHI to arrayOf("doujinshi"),
        MANGA to arrayOf("manga"),
        ARTIST_CG to arrayOf("artistcg", "Artist CG Sets", "Artist CG"),
        GAME_CG to arrayOf("gamecg", "Game CG Sets", "Game CG"),
        IMAGE_SET to arrayOf("imageset", "Image Sets", "Image Set"),
        COSPLAY to arrayOf("cosplay"),
        ASIAN_PORN to arrayOf("asianporn", "Asian Porn"),
        NON_H to arrayOf("non-h"),
        WESTERN to arrayOf("western"),
        PRIVATE to arrayOf("private"),
        UNKNOWN to arrayOf("unknown")
    )
    private val CATEGORY_STRINGS = CATEGORY_VALUES.entries.map { (k, v) -> v to k }

    val isExHentai: Boolean
        get() = Settings.gallerySite == EhUrl.SITE_EX

    @JvmStatic
    fun getCategory(type: String?): Int {
        for (entry in CATEGORY_STRINGS) {
            for (str in entry.first)
                if (str.equals(type, ignoreCase = true))
                    return entry.second
        }
        return UNKNOWN
    }

    fun getCategory(type: Int): String {
        return CATEGORY_VALUES.getOrDefault(type, CATEGORY_VALUES[UNKNOWN])!![0]
    }

    fun getCategoryColor(category: Int): Int {
        return when (category) {
            DOUJINSHI -> BG_COLOR_DOUJINSHI
            MANGA -> BG_COLOR_MANGA
            ARTIST_CG -> BG_COLOR_ARTIST_CG
            GAME_CG -> BG_COLOR_GAME_CG
            WESTERN -> BG_COLOR_WESTERN
            NON_H -> BG_COLOR_NON_H
            IMAGE_SET -> BG_COLOR_IMAGE_SET
            COSPLAY -> BG_COLOR_COSPLAY
            ASIAN_PORN -> BG_COLOR_ASIAN_PORN
            MISC -> BG_COLOR_MISC
            else -> BG_COLOR_UNKNOWN
        }.toInt()
    }

    fun signOut() {
        ehCookieStore.signOut()
        Settings.putAvatar(null)
        Settings.putDisplayName(null)
        Settings.putGallerySite(EhUrl.SITE_E)
        Settings.putNeedSignIn(true)
        Settings.putSelectSite(true)
    }

    fun needSignedIn(): Boolean {
        return Settings.needSignIn && !ehCookieStore.hasSignedIn()
    }

    @JvmStatic
    fun getSuitableTitle(gi: GalleryInfo): String {
        return if (Settings.showJpnTitle) {
            if (TextUtils.isEmpty(gi.titleJpn)) gi.title else gi.titleJpn
        } else {
            if (TextUtils.isEmpty(gi.title)) gi.titleJpn else gi.title
        }.orEmpty()
    }

    fun extractTitle(fullTitle: String?): String? {
        var title: String = fullTitle ?: return null
        title = PATTERN_TITLE_PREFIX.matcher(title).replaceFirst("")
        title = PATTERN_TITLE_SUFFIX.matcher(title).replaceFirst("")
        // Sometimes title is combined by romaji and english translation.
        // Only need romaji.
        // TODO But not sure every '|' means that
        val index = title.indexOf('|')
        if (index >= 0) {
            title = title.substring(0, index)
        }
        return title.ifEmpty { null }
    }

    @JvmStatic
    fun handleThumbUrlResolution(url: String?): String? {
        if (null == url) {
            return null
        }
        val resolution = when (Settings.thumbResolution) {
            0 -> return url
            1 -> "250"
            2 -> "300"
            else -> return url
        }
        val index1 = url.lastIndexOf('_')
        val index2 = url.lastIndexOf('.')
        return if (index1 >= 0 && index2 >= 0 && index1 < index2) {
            url.substring(0, index1 + 1) + resolution + url.substring(index2)
        } else {
            url
        }
    }
}