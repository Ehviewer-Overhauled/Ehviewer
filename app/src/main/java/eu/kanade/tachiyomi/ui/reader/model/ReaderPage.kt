package eu.kanade.tachiyomi.ui.reader.model

import com.hippo.image.Image
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.flow.MutableStateFlow

class ReaderPage(index: Int) : Page(index) {
    var image = MutableStateFlow<Image?>(null)
}
