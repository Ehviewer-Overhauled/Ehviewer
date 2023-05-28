package com.hippo.ehviewer.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder

class DohPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Preference(context, attrs) {
    override fun onClick() {
        val builder = EditTextDialogBuilder(context, Settings.dohUrl, context.getString(R.string.settings_advanced_dns_over_http_hint))
        builder.setTitle(R.string.settings_advanced_dns_over_http_title)
        builder.setPositiveButton(android.R.string.ok) { _, _ -> Settings.putDohUrl(builder.text.trim()) }.show()
    }
}
