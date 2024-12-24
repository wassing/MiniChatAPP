package com.example.minichatapp.ui.screens.chat

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object ImageUtils {
    fun uriToBase64(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: throw IOException("Cannot read image")
        inputStream.close()
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
}