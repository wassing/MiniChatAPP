package com.example.minichatapp.data.remote

import com.example.minichatapp.data.local.AppSettings
import com.example.minichatapp.domain.model.ChatMessage
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatService @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson,
    private val appSettings: AppSettings
) {
    private var webSocket: WebSocket? = null
    private var username: String? = null
    private var reconnectJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val messageChannel = Channel<ChatMessage>(Channel.UNLIMITED)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    fun connectToChat(username: String) {
        this.username = username
        connectWebSocket()
    }

    private fun connectWebSocket() {
        serviceScope.launch {
            val host = appSettings.serverHost.first()
            val port = appSettings.serverPort.first()

            val request = Request.Builder()
                .url("ws://$host:$port/chat?username=$username")
                .build()

            _connectionState.value = ConnectionState.Connecting

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    _connectionState.value = ConnectionState.Connected
                    cancelReconnectJob()
                    messageChannel.trySend(
                        ChatMessage(
                            senderId = "System",
                            content = "已连接到聊天室"
                        )
                    )
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val message = gson.fromJson(text, ChatMessage::class.java)
                        messageChannel.trySend(message)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _connectionState.value = ConnectionState.Failed(t.message ?: "连接失败")
                    messageChannel.trySend(
                        ChatMessage(
                            senderId = "System",
                            content = "连接失败: ${t.message}"
                        )
                    )
                    startReconnectJob()
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    _connectionState.value = ConnectionState.Disconnected
                    messageChannel.trySend(
                        ChatMessage(
                            senderId = "System",
                            content = "连接已断开"
                        )
                    )
                    startReconnectJob()
                }
            })
        }
    }

    private fun startReconnectJob() {
        reconnectJob?.cancel()
        reconnectJob = serviceScope.launch {
            val interval = appSettings.reconnectInterval.first()
            delay(interval)
            if (_connectionState.value !is ConnectionState.Connected) {
                messageChannel.trySend(
                    ChatMessage(
                        senderId = "System",
                        content = "正在尝试重新连接..."
                    )
                )
                connectWebSocket()
            }
        }
    }

    private fun cancelReconnectJob() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    fun sendMessage(message: ChatMessage) {
        if (_connectionState.value is ConnectionState.Connected) {
            webSocket?.send(gson.toJson(message))
        }
    }

    fun disconnect() {
        serviceScope.launch {
            cancelReconnectJob()
            webSocket?.close(1000, "User disconnected")
            webSocket = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun getMessages(): Flow<ChatMessage> = messageChannel.receiveAsFlow()

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Failed(val message: String) : ConnectionState()
    }
}