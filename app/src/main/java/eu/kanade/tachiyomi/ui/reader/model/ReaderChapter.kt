package eu.kanade.tachiyomi.ui.reader.model

import com.hippo.gallery.GalleryProvider
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.flow.MutableStateFlow

data class ReaderChapter(val chapter: GalleryProvider) {

    val stateFlow = MutableStateFlow<State>(State.Wait)
    var state: State
        get() = stateFlow.value
        set(value) {
            stateFlow.value = value
        }

    val pages: List<ReaderPage>?
        get() = (state as? State.Loaded)?.pages

    var pageLoader: PageLoader? = null

    var requestedPage: Int = 0

    private var references = 0

    fun ref() {
        references++
    }

    fun unref() {
        references--
        if (references == 0) {
            if (pageLoader != null) {
                logcat { "Recycling galleryProvider $chapter" }
            }
            pageLoader?.recycle()
            pageLoader = null
            state = State.Wait
        }
    }

    sealed class State {
        object Wait : State()
        object Loading : State()
        class Error(val error: Throwable) : State()
        class Loaded(val pages: List<ReaderPage>) : State()
    }
}
