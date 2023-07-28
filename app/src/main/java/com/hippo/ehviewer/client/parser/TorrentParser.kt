package com.hippo.ehviewer.client.parser

import java.nio.ByteBuffer

object TorrentParser {
    fun parse(body: ByteBuffer) = parseTorrent(body)
}

class Torrent(
    val posted: String,
    val size: String,
    val seeds: Int,
    val peers: Int,
    val downloads: Int,
    val url: String,
    val name: String,
)

fun Torrent.format() = "[$posted] $name [$size] [↑$seeds ↓$peers ✓$downloads]"

typealias TorrentResult = ArrayList<Torrent>

private external fun parseTorrent(body: ByteBuffer, size: Int = body.limit()): ArrayList<Torrent>
