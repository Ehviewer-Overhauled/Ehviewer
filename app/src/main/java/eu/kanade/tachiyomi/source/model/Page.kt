package eu.kanade.tachiyomi.source.model

import kotlinx.coroutines.flow.MutableStateFlow

open class Page(val index: Int)  {

    val number: Int
        get() = index + 1

    var status = MutableStateFlow(State.QUEUE)

    var progress = MutableStateFlow(0f)

    enum class State {
        QUEUE,
        LOAD_PAGE,
        DOWNLOAD_IMAGE,
        READY,
        ERROR,
    }
}
