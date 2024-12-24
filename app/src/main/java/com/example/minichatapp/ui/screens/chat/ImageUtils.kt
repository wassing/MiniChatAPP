package com.example.minichatapp.ui.screens.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

object ImageUtils {
    fun uriToBase64(context: Context, uri: Uri): String {
        val maxSize = 1024 * 1024 // 1MB
        val bitmap = loadAndCompressBitmap(context, uri, maxSize)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    suspend fun handleImageSelection(uri: Uri, chatViewModel: ChatViewModel, context: Context) {
        try {
            withContext(Dispatchers.IO) {
                val base64Image = uriToBase64(context, uri)
                chatViewModel.sendImageMessage(base64Image)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAndCompressBitmap(context: Context, uri: Uri, maxSize: Int): Bitmap {
        // 获取原始尺寸
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        var sampleSize = 1
        val originalSize = options.outWidth * options.outHeight * 4 // 4 bytes per pixel
        while (originalSize / (sampleSize * sampleSize) > maxSize) {
            sampleSize *= 2
        }

        // 加载压缩后的图片
        options.apply {
            inJustDecodeBounds = false
            inSampleSize = sampleSize
        }

        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IOException("Cannot load image")
    }
}