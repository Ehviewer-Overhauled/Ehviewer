package com.hippo.ehviewer.spider

import kotlinx.serialization.Serializable

@Serializable
class SpiderInfo @JvmOverloads constructor(
    val gid: Long,

    val pages: Int,

    val pTokenMap: MutableMap<Int, String> = hashMapOf(),

    var startPage: Int = 0,

    var token: String? = null,

    var previewPages: Int = -1,

    var previewPerPage: Int = -1
)
