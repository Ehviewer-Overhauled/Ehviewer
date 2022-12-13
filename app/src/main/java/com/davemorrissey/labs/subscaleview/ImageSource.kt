package com.davemorrissey.labs.subscaleview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.davemorrissey.labs.subscaleview.provider.AssetInputProvider
import com.davemorrissey.labs.subscaleview.provider.InputProvider
import com.davemorrissey.labs.subscaleview.provider.OpenStreamProvider
import com.davemorrissey.labs.subscaleview.provider.ResourceInputProvider
import com.davemorrissey.labs.subscaleview.provider.UriInputProvider
import java.io.InputStream

/**
 * Helper class used to set the source and additional attributes from a variety of sources. Supports
 * use of a bitmap, asset, resource, external file or any other URI.
 *
 *
 * When you are using a preview image, you must set the dimensions of the full size image on the
 * ImageSource object for the full size image using the [.dimensions] method.
 */
class ImageSource {
    val bitmap: Bitmap?
    val provider: InputProvider?

    private var sWidth = 0
    private var sHeight = 0

    var sRegion = Rect(0, 0, 0, 0)
        private set
    var isCached = false
        private set

    private constructor(bitmap: Bitmap, cached: Boolean) {
        this.bitmap = bitmap
        provider = null
        sWidth = bitmap.width
        sHeight = bitmap.height
        isCached = cached
    }

    private constructor(provider: InputProvider) {
        bitmap = null
        this.provider = provider
    }

    /**
     * Use a region of the source image. Region must be set independently for the full size image and the preview if
     * you are using one.
     *
     * @param sRegion the region of the source image to be displayed.
     * @return this instance for chaining.
     */
    fun region(sRegion: Rect): ImageSource {
        this.sRegion = sRegion
        setInvariants()
        return this
    }

    /**
     * Declare the dimensions of the image. This is only required for a full size image, when you are specifying a URI
     * and also a preview image. When displaying a bitmap object, or not using a preview, you do not need to declare
     * the image dimensions. Note if the declared dimensions are found to be incorrect, the view will reset.
     *
     * @param sWidth  width of the source image.
     * @param sHeight height of the source image.
     * @return this instance for chaining.
     */
    fun dimensions(sWidth: Int, sHeight: Int): ImageSource {
        if (bitmap == null) {
            this.sWidth = sWidth
            this.sHeight = sHeight
        }
        setInvariants()
        return this
    }

    private fun setInvariants() {
        if (!sRegion.isEmpty) {
            sWidth = sRegion.width()
            sHeight = sRegion.height()
        }
    }

    companion object {
        /**
         * Create an instance from a resource. The correct resource for the device screen resolution will be used.
         *
         * @param resId resource ID.
         * @return an [ImageSource] instance.
         */
        @JvmStatic
        fun resource(context: Context, resId: Int): ImageSource {
            return ImageSource(ResourceInputProvider(context, resId))
        }

        /**
         * Create an instance from an asset name.
         *
         * @param assetName asset name.
         * @return an [ImageSource] instance.
         */
        @JvmStatic
        fun asset(context: Context, assetName: String): ImageSource {
            return ImageSource(AssetInputProvider(context, assetName))
        }

        /**
         * Create an instance from a URI.
         *
         * @param uri image URI.
         * @return an [ImageSource] instance.
         */
        fun uri(context: Context, uri: Uri): ImageSource {
            return ImageSource(UriInputProvider(context, uri))
        }

        /**
         * Create an instance from an input provider.
         *
         * @param provider input stream provider.
         * @return an [ImageSource] instance.
         */
        fun provider(provider: InputProvider): ImageSource {
            return ImageSource(provider)
        }

        /**
         * Create an instance from an input stream.
         *
         * @param stream open input stream.
         * @return an [ImageSource] instance.
         */
        fun inputStream(stream: InputStream): ImageSource {
            return ImageSource(OpenStreamProvider(stream))
        }

        /**
         * Provide a loaded bitmap for display.
         *
         * @param bitmap bitmap to be displayed.
         * @return an [ImageSource] instance.
         */
        fun bitmap(bitmap: Bitmap): ImageSource {
            return ImageSource(bitmap, false)
        }

        /**
         * Provide a loaded and cached bitmap for display. This bitmap will not be recycled when it is no
         * longer needed. Use this method if you loaded the bitmap with an image loader such as Picasso
         * or Volley.
         *
         * @param bitmap bitmap to be displayed.
         * @return an [ImageSource] instance.
         */
        fun cachedBitmap(bitmap: Bitmap): ImageSource {
            return ImageSource(bitmap, true)
        }
    }
}
