package com.hippo.ehviewer.client.data

interface AbstractDownloadInfo {
    var state: Int
    var legacy: Int
    var time: Long
    var label: String?
    var position: Int
    var speed: Long
    var remaining: Long
    var finished: Int
    var downloaded: Int
    var total: Int
}
