package com.hippo.ehviewer.client.parser

object TorrentParser {
    fun parse(body: String) = parseTorrent(body)
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

class TorrentResult(
    val list: ArrayList<Torrent>,
) : List<Torrent> by list

private external fun parseTorrent(body: String): TorrentResult
