package eu.kanade.tachiyomi.source.model

import eu.kanade.tachiyomi.network.ProgressListener
import rx.subjects.Subject

open class Page(
    val index: Int,
) : ProgressListener {

    val number: Int
        get() = index + 1

    @Volatile
    var status: State = State.QUEUE
        set(value) {
            field = value
            statusSubject?.onNext(value)
        }

    @Volatile
    var progress: Int = 0

    var statusSubject: Subject<State, State>? = null

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        progress = if (contentLength > 0) {
            (100 * bytesRead / contentLength).toInt()
        } else {
            -1
        }
    }

    enum class State {
        QUEUE,
        LOAD_PAGE,
        DOWNLOAD_IMAGE,
        READY,
        ERROR,
    }
}
