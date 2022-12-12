package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.blue
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.hippo.unifile.UniFile
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLConnection
import kotlin.math.abs
import kotlin.math.min

object ImageUtil {
    fun getExtensionFromMimeType(mime: String?): String {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: SUPPLEMENTARY_MIMETYPE_MAPPING[mime]
            ?: "jpg"
    }

    enum class ImageType(val mime: String, val extension: String) {
        AVIF("image/avif", "avif"),
        GIF("image/gif", "gif"),
        HEIF("image/heif", "heif"),
        JPEG("image/jpeg", "jpg"),
        JXL("image/jxl", "jxl"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp"),
    }

    /**
     * Check whether the image is wide (which we consider a double-page spread).
     *
     * @return true if the width is greater than the height
     */
    fun isWideImage(imageStream: BufferedInputStream): Boolean {
        val options = extractImageOptions(imageStream)
        return options.outWidth > options.outHeight
    }

    /**
     * Extract the 'side' part from imageStream and return it as InputStream.
     */
    fun splitInHalf(imageStream: InputStream, side: Side): InputStream {
        val imageBytes = imageStream.readBytes()

        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val height = imageBitmap.height
        val width = imageBitmap.width

        val singlePage = Rect(0, 0, width / 2, height)

        val half = createBitmap(width / 2, height)
        val part = when (side) {
            Side.RIGHT -> Rect(width - width / 2, 0, width, height)
            Side.LEFT -> Rect(0, 0, width / 2, height)
        }
        half.applyCanvas {
            drawBitmap(imageBitmap, part, singlePage, null)
        }
        val output = ByteArrayOutputStream()
        half.compress(Bitmap.CompressFormat.JPEG, 100, output)

        return ByteArrayInputStream(output.toByteArray())
    }

    /**
     * Split the image into left and right parts, then merge them into a new image.
     */
    fun splitAndMerge(imageStream: InputStream, upperSide: Side): InputStream {
        val imageBytes = imageStream.readBytes()

        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val height = imageBitmap.height
        val width = imageBitmap.width

        val result = createBitmap(width / 2, height * 2)
        result.applyCanvas {
            // right -> upper
            val rightPart = when (upperSide) {
                Side.RIGHT -> Rect(width - width / 2, 0, width, height)
                Side.LEFT -> Rect(0, 0, width / 2, height)
            }
            val upperPart = Rect(0, 0, width / 2, height)
            drawBitmap(imageBitmap, rightPart, upperPart, null)
            // left -> bottom
            val leftPart = when (upperSide) {
                Side.LEFT -> Rect(width - width / 2, 0, width, height)
                Side.RIGHT -> Rect(0, 0, width / 2, height)
            }
            val bottomPart = Rect(0, height, width / 2, height * 2)
            drawBitmap(imageBitmap, leftPart, bottomPart, null)
        }

        val output = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, 100, output)
        return ByteArrayInputStream(output.toByteArray())
    }

    enum class Side {
        RIGHT,
        LEFT,
    }

    /**
     * Check whether the image is considered a tall image.
     *
     * @return true if the height:width ratio is greater than 3.
     */
    private fun isTallImage(imageStream: InputStream): Boolean {
        val options = extractImageOptions(imageStream, resetAfterExtraction = false)
        return (options.outHeight / options.outWidth) > 3
    }

    private fun splitImageName(filenamePrefix: String, index: Int) = "${filenamePrefix}__${"%03d".format(index + 1)}.jpg"

    /**
     * Split the imageStream according to the provided splitData
     */
    fun splitStrip(splitData: SplitData, streamFn: () -> InputStream): InputStream {
        val bitmapRegionDecoder = getBitmapRegionDecoder(streamFn())
            ?: throw Exception("Failed to create new instance of BitmapRegionDecoder")

        logcat {
            "WebtoonSplit #${splitData.index} with topOffset=${splitData.topOffset} " +
                "splitHeight=${splitData.splitHeight} bottomOffset=${splitData.bottomOffset}"
        }

        val region = Rect(0, splitData.topOffset, splitData.splitWidth, splitData.bottomOffset)

        try {
            val splitBitmap = bitmapRegionDecoder.decodeRegion(region, null)
            val outputStream = ByteArrayOutputStream()
            splitBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            return ByteArrayInputStream(outputStream.toByteArray())
        } catch (e: Throwable) {
            throw e
        } finally {
            bitmapRegionDecoder.recycle()
        }
    }

    fun getSplitDataForStream(imageStream: InputStream): List<SplitData> {
        return extractImageOptions(imageStream).splitData
    }

    private val BitmapFactory.Options.splitData
        get(): List<SplitData> {
            val imageHeight = outHeight
            val imageWidth = outWidth

            // -1 so it doesn't try to split when imageHeight = optimalImageHeight
            val partCount = (imageHeight - 1) / optimalImageHeight + 1
            val optimalSplitHeight = imageHeight / partCount

            logcat {
                "Generating SplitData for image (height: $imageHeight): " +
                    "$partCount parts @ ${optimalSplitHeight}px height per part"
            }

            return mutableListOf<SplitData>().apply {
                val range = 0 until partCount
                for (index in range) {
                    // Only continue if the list is empty or there is image remaining
                    if (isNotEmpty() && imageHeight <= last().bottomOffset) break

                    val topOffset = index * optimalSplitHeight
                    var splitHeight = min(optimalSplitHeight, imageHeight - topOffset)

                    if (index == range.last) {
                        val remainingHeight = imageHeight - (topOffset + splitHeight)
                        splitHeight += remainingHeight
                    }

                    add(SplitData(index, topOffset, splitHeight, imageWidth))
                }
            }
        }

    data class SplitData(
        val index: Int,
        val topOffset: Int,
        val splitHeight: Int,
        val splitWidth: Int,
    ) {
        val bottomOffset = topOffset + splitHeight
    }

    private fun @receiver:ColorInt Int.isDark(): Boolean =
        red < 40 && blue < 40 && green < 40 && alpha > 200

    private fun @receiver:ColorInt Int.isCloseTo(other: Int): Boolean =
        abs(red - other.red) < 30 && abs(green - other.green) < 30 && abs(blue - other.blue) < 30

    private fun @receiver:ColorInt Int.isWhite(): Boolean =
        red + blue + green > 740

    /**
     * Used to check an image's dimensions without loading it in the memory.
     */
    private fun extractImageOptions(
        imageStream: InputStream,
        resetAfterExtraction: Boolean = true,
    ): BitmapFactory.Options {
        imageStream.mark(imageStream.available() + 1)

        val imageBytes = imageStream.readBytes()
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        if (resetAfterExtraction) imageStream.reset()
        return options
    }

    private fun getBitmapRegionDecoder(imageStream: InputStream): BitmapRegionDecoder? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(imageStream)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(imageStream, false)
        }
    }

    private val optimalImageHeight = getDisplayMaxHeightInPx * 2

    // Android doesn't include some mappings
    private val SUPPLEMENTARY_MIMETYPE_MAPPING = mapOf(
        // https://issuetracker.google.com/issues/182703810
        "image/jxl" to "jxl",
    )
}
