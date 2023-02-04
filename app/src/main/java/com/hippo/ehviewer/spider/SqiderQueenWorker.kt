package com.hippo.ehviewer.spider

import com.hippo.ehviewer.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class SpiderQueenWorker : CoroutineScope {
    private val maxParallelismSize = Settings.getMultiThreadDownload().coerceIn(1, 10)
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO.limitedParallelism(maxParallelismSize) + SupervisorJob()
}
