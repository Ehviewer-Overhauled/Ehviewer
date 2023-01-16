package com.hippo.ehviewer.spider

import com.hippo.streampipe.OutputStreamPipe
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking

fun completeUninterruptible(outputStreamPipe: OutputStreamPipe) {
    runBlocking {
        GlobalScope.launchIO {
            outputStreamPipe.close()
            outputStreamPipe.release()
        }.join()
    }
}