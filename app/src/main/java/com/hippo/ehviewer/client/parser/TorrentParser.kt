package com.hippo.ehviewer.client.parser

object TorrentParser {
    fun parse(body: String) = run {
        val list = mutableListOf<Torrent>()
        parseTorrent(body, list)
        list
    }
}

class Torrent(
    val posted: String,
    val size: String,
    val seeds: Int,
    val peers: Int,
    val downloads: Int,
    val url: String,
    val name: String,
) {
    fun format() = "[$posted] $name [$size] [↑$seeds ↓$peers ✓$downloads]"
}

typealias TorrentResult = List<Torrent>

private external fun parseTorrent(body: String, list: List<Torrent>)
