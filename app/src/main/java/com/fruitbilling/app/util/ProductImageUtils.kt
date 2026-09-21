package com.fruitbilling.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ProductImageUtils {
    private val memoryCache = LruCache<String, ImageBitmap>(100)

    /**
     * Copies and optimizes an image picked from device gallery into internal app storage.
     * Scales image down to max 512px to keep memory and storage footprint ultra low.
     * Returns the absolute file path, or null if reading failed.
     */
    fun saveImage(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "product_images").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(dir, "prod_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

                // Read EXIF orientation to correct camera rotation if needed
                val rotatedBitmap = try {
                    context.contentResolver.openInputStream(uri)?.use { exifStream ->
                        val exif = ExifInterface(exifStream)
                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        rotateBitmap(originalBitmap, orientation)
                    } ?: originalBitmap
                } catch (_: Exception) {
                    originalBitmap
                }

                // Scale down to max 512px
                val maxDim = max(rotatedBitmap.width, rotatedBitmap.height)
                val scaledBitmap = if (maxDim > 512) {
                    val scale = 512f / maxDim
                    Bitmap.createScaledBitmap(
                        rotatedBitmap,
                        (rotatedBitmap.width * scale).toInt(),
                        (rotatedBitmap.height * scale).toInt(),
                        true
                    )
                } else {
                    rotatedBitmap
                }

                FileOutputStream(outputFile).use { fos ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    fos.flush()
                }

                if (scaledBitmap != originalBitmap && !scaledBitmap.isRecycled) {
                    scaledBitmap.recycle()
                }
                if (rotatedBitmap != originalBitmap && !rotatedBitmap.isRecycled) {
                    rotatedBitmap.recycle()
                }
                if (!originalBitmap.isRecycled) {
                    originalBitmap.recycle()
                }

                outputFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Safely deletes a stored product image file and clears it from cache.
     */
    fun deleteImage(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            memoryCache.remove(path)
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    /**
     * Loads and caches an ImageBitmap from a local file path.
     */
    fun loadBitmap(path: String?): ImageBitmap? {
        if (path.isNullOrBlank()) return null
        val cached = memoryCache.get(path)
        if (cached != null) return cached

        return try {
            val file = File(path)
            if (!file.exists()) return null
            val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val imageBitmap = bmp.asImageBitmap()
            memoryCache.put(path, imageBitmap)
            imageBitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            rotated
        } catch (_: Exception) {
            bitmap
        }
    }
}

/**
 * Composable helper to remember and render an ImageBitmap for a product iconRef.
 */
@Composable
fun rememberProductImage(path: String?): ImageBitmap? {
    return remember(path) {
        ProductImageUtils.loadBitmap(path)
    }
}
