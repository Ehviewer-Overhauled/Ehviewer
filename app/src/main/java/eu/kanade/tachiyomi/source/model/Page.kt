package eu.kanade.tachiyomi.source.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

open class Page(val index: Int)  {

    val number: Int
        get() = index + 1

    var status = MutableStateFlow(State.QUEUE)

    private val _progressFlow = MutableStateFlow(0)

    val progressFlow = _progressFlow.asStateFlow()
    var progress: Int
        get() = _progressFlow.value
        set(value) {
            _progressFlow.value = value
        }

    enum class State {
        QUEUE,
        LOAD_PAGE,
        DOWNLOAD_IMAGE,
        READY,
        ERROR,
    }
}
