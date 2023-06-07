package com.hippo.ehviewer.ui.tools

object CropDefaults {
    private const val CROP_MIN_ASPECT = 0.5F
    private const val CROP_MAX_ASPECT = 0.8f
    private val CROP_GOOD_RANGE = CROP_MIN_ASPECT..CROP_MAX_ASPECT
    fun shouldCrop(width: Int, height: Int) = width.toFloat() / height in CROP_GOOD_RANGE
}
