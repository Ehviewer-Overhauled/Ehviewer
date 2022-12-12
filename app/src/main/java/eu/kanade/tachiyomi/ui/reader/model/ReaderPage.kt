package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.source.model.Page
import java.io.InputStream

open class ReaderPage(
    index: Int,
    var stream: (() -> InputStream)? = null,
) : Page(index) {

    open lateinit var chapter: ReaderChapter
}
