package eu.kanade.tachiyomi.ui.reader.setting

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.hippo.ehviewer.R
import eu.kanade.tachiyomi.data.preference.PreferenceValues
import com.hippo.ehviewer.databinding.ReaderReadingModeSettingsBinding
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import eu.kanade.tachiyomi.util.preference.asHotFlow
import eu.kanade.tachiyomi.util.preference.bindToPreference
import kotlinx.coroutines.flow.launchIn

/**
 * Sheet to show reader and viewer preferences.
 */
class ReaderReadingModeSettings @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {

    private val readerPreferences: ReaderPreferences = ReaderActivity.readerPreferences

    private val binding = ReaderReadingModeSettingsBinding.inflate(LayoutInflater.from(context), this, false)

    init {
        addView(binding.root)

        initGeneralPreferences()

        when ((context as ReaderActivity).viewer) {
            is PagerViewer -> initPagerPreferences()
            is WebtoonViewer -> initWebtoonPreferences()
        }
    }

    /**
     * Init general reader preferences.
     */
    private fun initGeneralPreferences() {
        binding.viewer.onItemSelectedListener = { position ->
            val readingModeType = ReadingModeType.fromSpinner(position)
            ReaderActivity.readerPreferences.defaultReadingMode().set(readingModeType.flagValue)
            (context as ReaderActivity).setGallery()

            val mangaViewer = ReaderActivity.readerPreferences.defaultReadingMode().get()
            if (mangaViewer == ReadingModeType.WEBTOON.flagValue || mangaViewer == ReadingModeType.CONTINUOUS_VERTICAL.flagValue) {
                initWebtoonPreferences()
            } else {
                initPagerPreferences()
            }
        }
        binding.viewer.setSelection(ReaderActivity.readerPreferences.defaultReadingMode().get().let { ReadingModeType.fromPreference(it).prefValue })

        binding.rotationMode.onItemSelectedListener = { position ->
            val rotationType = OrientationType.fromSpinner(position)
            readerPreferences.defaultOrientationType().set(rotationType.flagValue)
            (context as ReaderActivity).setGallery()
        }
        binding.rotationMode.setSelection(ReaderActivity.readerPreferences.defaultOrientationType().get().let { OrientationType.fromPreference(it).prefValue })
    }

    /**
     * Init the preferences for the pager reader.
     */
    private fun initPagerPreferences() {
        binding.webtoonPrefsGroup.root.isVisible = false
        binding.pagerPrefsGroup.root.isVisible = true

        binding.pagerPrefsGroup.tappingInverted.bindToPreference(readerPreferences.pagerNavInverted(), PreferenceValues.TappingInvertMode::class.java)
        binding.pagerPrefsGroup.navigatePan.bindToPreference(readerPreferences.navigateToPan())

        binding.pagerPrefsGroup.pagerNav.bindToPreference(readerPreferences.navigationModePager())
        readerPreferences.navigationModePager()
            .asHotFlow {
                val isTappingEnabled = it != 5
                binding.pagerPrefsGroup.tappingInverted.isVisible = isTappingEnabled
                binding.pagerPrefsGroup.navigatePan.isVisible = isTappingEnabled
            }
            .launchIn((context as ReaderActivity).lifecycleScope)
        // Makes so that landscape zoom gets hidden away when image scale type is not fit screen
        binding.pagerPrefsGroup.scaleType.bindToPreference(readerPreferences.imageScaleType(), 1)
        readerPreferences.imageScaleType()
            .asHotFlow { binding.pagerPrefsGroup.landscapeZoom.isVisible = it == 1 }
            .launchIn((context as ReaderActivity).lifecycleScope)
        binding.pagerPrefsGroup.landscapeZoom.bindToPreference(readerPreferences.landscapeZoom())

        binding.pagerPrefsGroup.zoomStart.bindToPreference(readerPreferences.zoomStart(), 1)
        binding.pagerPrefsGroup.cropBorders.bindToPreference(readerPreferences.cropBorders())
    }

    /**
     * Init the preferences for the webtoon reader.
     */
    private fun initWebtoonPreferences() {
        binding.pagerPrefsGroup.root.isVisible = false
        binding.webtoonPrefsGroup.root.isVisible = true

        binding.webtoonPrefsGroup.tappingInverted.bindToPreference(readerPreferences.webtoonNavInverted(), PreferenceValues.TappingInvertMode::class.java)

        binding.webtoonPrefsGroup.webtoonNav.bindToPreference(readerPreferences.navigationModeWebtoon())
        readerPreferences.navigationModeWebtoon()
            .asHotFlow { binding.webtoonPrefsGroup.tappingInverted.isVisible = it != 5 }
            .launchIn((context as ReaderActivity).lifecycleScope)
        binding.webtoonPrefsGroup.cropBordersWebtoon.bindToPreference(readerPreferences.cropBordersWebtoon())
        binding.webtoonPrefsGroup.webtoonSidePadding.bindToIntPreference(readerPreferences.webtoonSidePadding(), R.array.webtoon_side_padding_values)
    }
}
