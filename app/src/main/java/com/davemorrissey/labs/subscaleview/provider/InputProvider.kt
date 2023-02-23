package com.davemorrissey.labs.subscaleview.provider

import java.io.IOException
import java.io.InputStream
import kotlin.Throws

interface InputProvider {
    @Throws(IOException::class)
    fun openStream(): InputStream?
}
