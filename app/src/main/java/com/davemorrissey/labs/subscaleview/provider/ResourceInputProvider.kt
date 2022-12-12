package com.davemorrissey.labs.subscaleview.provider

import android.content.Context
import java.io.IOException

class ResourceInputProvider(context: Context, val resource: Int) : InputProvider {
    private val resources = context.resources

    @Throws(IOException::class)
    override fun openStream() = resources.openRawResource(resource)
}
