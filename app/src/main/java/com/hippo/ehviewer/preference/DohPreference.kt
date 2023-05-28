package com.hippo.ehviewer.preference

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import androidx.preference.Preference
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.systemDns
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress

class DohPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Preference(context, attrs) {
    override fun onClick() {
        val builder = EditTextDialogBuilder(context, Settings.dohUrl, context.getString(R.string.settings_advanced_dns_over_http_hint))
        builder.setTitle(R.string.settings_advanced_dns_over_http_title)
        builder.setPositiveButton(android.R.string.ok, null)
        val dialog = builder.create()
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val text = builder.text.trim()
            runCatching {
                if (text.isNotBlank()) text.toHttpUrl()
            }.onFailure {
                builder.setError("Invalid URL!")
            }.onSuccess {
                Settings.putDohUrl(text)
                dialog.dismiss()
            }
        }
    }
}

private fun buildDoHDNS(url: String): DnsOverHttps {
    return DnsOverHttps.Builder().apply {
        client(EhApplication.okHttpClient)
        url(url.toHttpUrl())
        post(true)
        systemDns(systemDns)
    }.build()
}

private var doh: DnsOverHttps? = Settings.dohUrl.runCatching { buildDoHDNS(this) }.getOrNull()

object EhDoH {
    fun lookup(hostname: String): List<InetAddress>? = doh?.runCatching { lookup(hostname).takeIf { it.isNotEmpty() } }?.getOrNull()
}
