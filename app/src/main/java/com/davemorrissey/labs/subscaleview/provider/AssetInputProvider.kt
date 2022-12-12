package com.davemorrissey.labs.subscaleview.provider

import android.content.Context
import java.io.IOException

class AssetInputProvider(context: Context, val assetName: String) : InputProvider {
    private val assets = context.assets

    @Throws(IOException::class)
    override fun openStream() = assets.open(assetName)
}
