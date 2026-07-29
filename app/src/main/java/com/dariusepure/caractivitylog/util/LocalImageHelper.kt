package com.dariusepure.caractivitylog.util

import android.content.Context
import android.net.Uri
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
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val file = File(getImagesFolder(context), "$carId.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
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
