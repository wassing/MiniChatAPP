package com.example.minichatapp.data.remote

import com.example.minichatapp.data.local.AppSettings
import com.example.minichatapp.data.repository.MessageRepository
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.domain.model.ChatRoom
import com.example.minichatapp.domain.model.MessageStatus
import com.example.minichatapp.domain.model.MessageType
import com.example.minichatapp.domain.model.RoomType
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
    private val appSettings: AppSettings,
    private val messageRepository: MessageRepository
) {
    private var webSocket: WebSocket? = null
    private var currentUsername: String? = null
    private var reconnectJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val messageChannels = mutableMapOf<String, Channel<ChatMessage>>()
    private val _currentRoom = MutableStateFlow<ChatRoom?>(null)
    val currentRoom: StateFlow<ChatRoom?> = _currentRoom

    private val _onlineUsers = MutableStateFlow<Set<String>>(emptySet())
    val onlineUsers: StateFlow<Set<String>> = _onlineUsers

    fun connectToChat(username: String) {
        this.currentUsername = username
        connectWebSocket()
        // 连接成功后自动加入公共聊天室
        joinRoom(createPublicRoom())
        // 清除旧的消息通道
        messageChannels.clear()
    }

    private fun connectWebSocket() {
        serviceScope.launch {
            try {
                val host = appSettings.serverHost.first()
                val port = appSettings.serverPort.first()
                val url = "ws://$host:$port/chat?username=$currentUsername"

                println("Connecting to WebSocket: $url")

                val request = Request.Builder()
                    .url(url)
                    .build()

                _connectionState.value = ConnectionState.Connecting

                // 断开现有连接
                webSocket?.cancel()
                webSocket = null

                webSocket = client.newWebSocket(request, createWebSocketListener())
                println("WebSocket connection initiated")
            } catch (e: Exception) {
                println("WebSocket connection failed: ${e.message}")
                _connectionState.value = ConnectionState.Failed("连接失败: ${e.message}")
                startReconnectJob()
            }
        }
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                serviceScope.launch {
                    println("WebSocket connection opened")
                    _connectionState.value = ConnectionState.Connected
                    cancelReconnectJob()
                    broadcastSystemMessage(createPublicRoom().id, "已连接到聊天服务器")

                    // 发送初始连接消息
                    val message = ChatMessage(
                        roomId = "public",
                        senderId = currentUsername ?: "unknown",
                        content = "joined",
                        type = MessageType.TEXT
                    )
                    println("Sending join message: ${gson.toJson(message)}")
                    val sent = webSocket.send(gson.toJson(message))
                    println("Join message sent: $sent")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                serviceScope.launch {
                    try {
                        val message = gson.fromJson(text, ChatMessage::class.java)
                        handleIncomingMessage(message)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        broadcastSystemMessage(
                            createPublicRoom().id,
                            "消息处理错误: ${e.message}"
                        )
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                serviceScope.launch {
                    _connectionState.value = ConnectionState.Failed(t.message ?: "连接失败")
                    broadcastSystemMessage(createPublicRoom().id, "连接失败: ${t.message}")
                    webSocket.cancel()
                    this@ChatService.webSocket = null
                    startReconnectJob()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                serviceScope.launch {
                    _connectionState.value = ConnectionState.Disconnected
                    broadcastSystemMessage(createPublicRoom().id, "连接已断开")
                    webSocket.close(1000, null)
                    this@ChatService.webSocket = null
                    startReconnectJob()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                serviceScope.launch {
                    _connectionState.value = ConnectionState.Disconnected
                    if (code != 1000) {
                        startReconnectJob()
                    }
                }
            }
        }
    }

    private fun handleIncomingMessage(message: ChatMessage) {
        serviceScope.launch {
            // 更新消息状态并保存到本地
            val updatedMessage = message.copy(status = MessageStatus.SENT)
            messageRepository.saveMessage(updatedMessage)

            // 将消息发送到对应聊天室的 Channel
            messageChannels[message.roomId]?.send(updatedMessage)
        }
    }

    private fun broadcastSystemMessage(roomId: String, content: String) {
        val systemMessage = ChatMessage(
            roomId = roomId,
            senderId = "System",
            content = content
        )
        handleIncomingMessage(systemMessage)
    }

    fun sendMessage(message: ChatMessage) {
        serviceScope.launch {
            try {
                println("ChatService: Starting message send process")
                println("ChatService: Message details - id: ${message.id}, roomId: ${message.roomId}, sender: ${message.senderId}")

                // 检查连接状态
                val currentState = _connectionState.value
                println("ChatService: Current connection state: $currentState")

                // 获取当前的WebSocket实例
                val currentWebSocket = webSocket
                println("ChatService: Current WebSocket instance: ${if (currentWebSocket != null) "exists" else "null"}")

                if (currentState !is ConnectionState.Connected || currentWebSocket == null) {
                    println("ChatService: Cannot send message - not properly connected")
                    throw Exception("Not properly connected to server")
                }

                try {
                    // 先保存消息到本地
                    println("ChatService: Saving message to local repository")
                    messageRepository.saveMessage(message)
                    println("ChatService: Message saved to local repository successfully")
                } catch (e: Exception) {
                    println("ChatService: Error saving message to repository: ${e.message}")
                    throw e
                }

                try {
                    // 准备发送的消息
                    val messageJson = gson.toJson(message)
                    println("ChatService: Message serialized to JSON: $messageJson")

                    println("ChatService: Attempting to send message via WebSocket")
                    val sent = currentWebSocket.send(messageJson)
                    println("ChatService: WebSocket.send() returned: $sent")

                    if (sent) {
                        try {
                            println("ChatService: Message sent successfully, updating status")
                            messageRepository.updateMessageStatus(message, MessageStatus.SENT)
                            println("ChatService: Message status updated to SENT")
                        } catch (e: Exception) {
                            println("ChatService: Error updating message status: ${e.message}")
                            throw e
                        }
                    } else {
                        println("ChatService: Send returned false")
                        throw Exception("Message send returned false")
                    }
                } catch (e: Exception) {
                    println("ChatService: Error during message send: ${e.message}")
                    println("ChatService: Error stack trace: ${e.stackTrace.joinToString("\\n")}")
                    throw e
                }
            } catch (e: Exception) {
                println("ChatService: Critical error in send process: ${e.message}")
                println("ChatService: Full stack trace: ${e.stackTrace.joinToString("\\n")}")
                try {
                    messageRepository.updateMessageStatus(message, MessageStatus.FAILED)
                } catch (repoEx: Exception) {
                    println("ChatService: Error updating message status to FAILED: ${repoEx.message}")
                }

                if (_connectionState.value !is ConnectionState.Connected) {
                    println("ChatService: Connection lost, starting reconnect")
                    startReconnectJob()
                }
            }
        }
    }

    fun joinRoom(room: ChatRoom) {
        _currentRoom.value = room
        if (!messageChannels.containsKey(room.id)) {
            messageChannels[room.id] = Channel(Channel.UNLIMITED)
        }
    }

    fun leaveCurrentRoom() {
        _currentRoom.value = null
    }

    fun getMessagesForRoom(roomId: String): Flow<List<ChatMessage>> {
        return messageRepository.getMessagesForRoom(roomId)
    }

    fun getCurrentRoomMessages(): Flow<ChatMessage> {
        return currentRoom.filterNotNull().flatMapLatest { room ->
            messageChannels[room.id]?.receiveAsFlow() ?: emptyFlow()
        }
    }

    private fun createPublicRoom(): ChatRoom {
        return ChatRoom(
            id = "public",
            type = RoomType.PUBLIC,
            name = "公共聊天室",
            participants = emptyList()
        )
    }

    fun createPrivateRoom(participant: String): ChatRoom {
        val currentUser = currentUsername ?: throw IllegalStateException("User not logged in")
        return ChatRoom(
            id = generatePrivateRoomId(currentUser, participant),
            type = RoomType.PRIVATE,
            name = participant,  // 使用对方的用户名作为房间名
            participants = listOf(currentUser, participant)
        )
    }

    private fun generatePrivateRoomId(user1: String, user2: String): String {
        // 确保房间 ID 的唯一性和一致性
        return listOf(user1, user2).sorted().joinToString("-")
    }

    fun disconnect() {
        serviceScope.launch {
            cancelReconnectJob()
            webSocket?.close(1000, "User disconnected")
            webSocket = null
            _connectionState.value = ConnectionState.Disconnected
            messageChannels.clear()
            _currentRoom.value = null
        }
    }

    private fun startReconnectJob() {
        reconnectJob?.cancel()
        reconnectJob = serviceScope.launch {
            val interval = appSettings.reconnectInterval.first()
            delay(interval)
            if (_connectionState.value !is ConnectionState.Connected) {
                connectWebSocket()
            }
        }
    }

    private fun cancelReconnectJob() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Failed(val message: String) : ConnectionState()
    }
}