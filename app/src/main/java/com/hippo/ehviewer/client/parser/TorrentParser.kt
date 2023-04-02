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
package com.hippo.ehviewer.client.parser

import org.jsoup.Jsoup
import java.util.regex.Pattern

object TorrentParser {
    private val PATTERN_TORRENT =
        Pattern.compile("</span> ([0-9-]+) [0-9:]+</td>[\\s\\S]+</span> ([0-9.]+ [KMGT]B)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span>([^<]+)</td>[\\s\\S]+onclick=\"document.location='([^\"]+)'[^<]+>([^<]+)</a>")

    fun parse(body: String): List<Result> {
        val torrentList = ArrayList<Result>()
        val d = Jsoup.parse(body)
        val es = d.select("form>div>table")
        for (e in es) {
            val m = PATTERN_TORRENT.matcher(e.html())
            if (m.find()) {
                val posted = m.group(1)!!
                val size = m.group(2)!!
                val seeds = m.group(3)!!.toInt()
                val peers = m.group(4)!!.toInt()
                val downloads = m.group(5)!!.toInt()
                val url = ParserUtils.trim(m.group(7))
                val name = ParserUtils.trim(m.group(8))
                torrentList.add(Result(posted, size, seeds, peers, downloads, url, name))
            }
        }
        return torrentList
    }

    class Result(
        private val posted: String,
        private val size: String,
        private val seeds: Int,
        private val peers: Int,
        private val downloads: Int,
        val url: String,
        val name: String,
    ) {
        fun format() = "[$posted] $name [$size] [↑$seeds ↓$peers ✓$downloads]"
    }
}
