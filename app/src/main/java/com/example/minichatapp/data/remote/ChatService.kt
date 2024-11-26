package com.example.minichatapp.data.remote

import com.example.minichatapp.domain.model.ChatMessage
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatService @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private val messageChannel = Channel<ChatMessage>(Channel.UNLIMITED)

    fun connectToChat(username: String) {
        val request = Request.Builder()
            .url("ws://localhost:8080/chat?username=$username")  // 这里需要替换为实际的WebSocket服务器地址
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = gson.fromJson(text, ChatMessage::class.java)
                    messageChannel.trySend(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    fun sendMessage(message: ChatMessage) {
        webSocket?.send(gson.toJson(message))
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    fun getMessages(): Flow<ChatMessage> = messageChannel.receiveAsFlow()
}