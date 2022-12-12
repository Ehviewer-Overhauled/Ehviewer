package eu.kanade.tachiyomi.ui.reader.model

class InsertPage(val parent: ReaderPage) : ReaderPage(parent.index) {

    override var chapter: ReaderChapter = parent.chapter

    init {
        stream = parent.stream
    }
}
