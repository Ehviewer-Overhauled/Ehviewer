package eu.kanade.tachiyomi.ui.reader.model

import com.hippo.image.Image
import eu.kanade.tachiyomi.source.model.Page

class ReaderPage(index: Int) : Page(index) {
    lateinit var image: Image
}
