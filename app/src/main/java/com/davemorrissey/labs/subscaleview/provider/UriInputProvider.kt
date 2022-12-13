package com.davemorrissey.labs.subscaleview.provider

import android.content.Context
import android.net.Uri
import java.io.IOException
import kotlin.Throws

class UriInputProvider(context: Context, val uri: Uri) : InputProvider {
    private val resolver = context.contentResolver

    @Throws(IOException::class)
    override fun openStream() = resolver.openInputStream(uri)
}
