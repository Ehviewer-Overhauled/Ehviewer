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

object TorrentParser {
    fun parse(body: String): List<Result> {
        val list = mutableListOf<Array<String>>()
        parseTorrent(body, list)
        return list.map { Result(it[0], it[1], it[2].toInt(), it[3].toInt(), it[4].toInt(), it[5], it[6]) }
    }

    class Result(val posted: String, val size: String, val seeds: Int, val peers: Int, val downloads: Int, val url: String, val name: String) {
        fun format() = "[$posted] $name [$size] [↑$seeds ↓$peers ✓$downloads]"
    }
}

private external fun parseTorrent(body: String, list: List<Array<String>>)
