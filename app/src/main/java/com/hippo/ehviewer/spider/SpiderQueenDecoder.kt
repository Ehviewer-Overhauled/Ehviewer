package com.hippo.ehviewer.spider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class SpiderQueenDecoder(private val queen: SpiderQueen) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO.limitedParallelism(2) + SupervisorJob()
}
