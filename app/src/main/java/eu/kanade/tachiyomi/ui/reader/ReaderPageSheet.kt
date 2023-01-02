package eu.kanade.tachiyomi.ui.reader

import android.view.LayoutInflater
import android.view.View
import com.hippo.ehviewer.databinding.ReaderPageSheetBinding
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.widget.sheet.BaseBottomSheetDialog

/**
 * Sheet to show when a page is long clicked.
 */
class ReaderPageSheet(
    private val activity: ReaderActivity,
    private val page: ReaderPage,
) : BaseBottomSheetDialog(activity) {

    private lateinit var binding: ReaderPageSheetBinding

    override fun createView(inflater: LayoutInflater): View {
        binding = ReaderPageSheetBinding.inflate(activity.layoutInflater, null, false)

        binding.share.setOnClickListener { share() }
        binding.copy.setOnClickListener { copy() }
        binding.save.setOnClickListener { save() }
        binding.saveTo.setOnClickListener { saveTo() }

        return binding.root
    }

    /**
     * Shares the image of this page with external apps.
     */
    private fun share() {
        activity.shareImage(page.index)
        dismiss()
    }

    /**
     * Copy the image of this page with external apps.
     */
    private fun copy() {
        activity.copyImage(page.index)
        dismiss()
    }

    /**
     * Saves the image of this page on external storage.
     */
    private fun save() {
        activity.saveImage(page.index)
        dismiss()
    }

    /**
     * Saves the image of this page to a custom location on external storage.
     */
    private fun saveTo() {
        activity.saveImageTo(page.index)
        dismiss()
    }
}
