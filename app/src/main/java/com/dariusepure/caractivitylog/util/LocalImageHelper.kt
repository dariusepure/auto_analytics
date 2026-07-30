package com.dariusepure.caractivitylog.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object LocalImageHelper {
    private const val FOLDER_NAME = "car_images"

    private fun getImagesFolder(context: Context): File {
        val folder = File(context.filesDir, FOLDER_NAME)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    fun getCarImageFile(context: Context, carId: String): File? {
        val file = File(getImagesFolder(context), "$carId.jpg")
        return if (file.exists()) file else null
    }

    fun saveCarImage(context: Context, carId: String, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return false
            
            // Resize if too large (max 1280px)
            val maxDimension = 1280
            val width = originalBitmap.width
            val height = originalBitmap.height
            val (newWidth, newHeight) = if (width > height) {
                if (width > maxDimension) {
                    val ratio = maxDimension.toFloat() / width
                    (maxDimension to (height * ratio).toInt())
                } else width to height
            } else {
                if (height > maxDimension) {
                    val ratio = maxDimension.toFloat() / height
                    ((width * ratio).toInt() to maxDimension)
                } else width to height
            }

            val resizedBitmap = if (newWidth != width || newHeight != height) {
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            val file = File(getImagesFolder(context), "$carId.jpg")
            val outputStream = FileOutputStream(file)
            outputStream.use { output ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, output)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteCarImage(context: Context, carId: String): Boolean {
        val file = File(getImagesFolder(context), "$carId.jpg")
        return if (file.exists()) file.delete() else false
    }
}
