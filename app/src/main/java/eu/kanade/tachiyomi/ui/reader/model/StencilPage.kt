package eu.kanade.tachiyomi.ui.reader.model

import java.io.InputStream

class StencilPage(
    parent: ReaderPage,
    stencilStream: () -> InputStream,
) : ReaderPage(parent.index) {

    override var chapter: ReaderChapter = parent.chapter

    init {
        status = State.READY
        stream = stencilStream
    }
}
