package com.hippo.ehviewer.network

import okhttp3.HttpUrl

@JvmInline
value class UrlBuilder(val url: HttpUrl.Builder) {
    fun addQuery(key: String, value: Any) {
        url.addEncodedQueryParameter(key, value.toString())
    }

    fun build(): String {
        return url.toString()
    }
}
