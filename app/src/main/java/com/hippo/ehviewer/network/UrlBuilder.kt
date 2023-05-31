package com.hippo.ehviewer.network

import okhttp3.HttpUrl.Companion.toHttpUrl

class UrlBuilder(mRootUrl: String) {
    private val url = mRootUrl.toHttpUrl().newBuilder()
    fun addQuery(key: String, value: Any) {
        url.addEncodedQueryParameter(key, value.toString())
    }

    fun build(): String {
        return url.toString()
    }
}
