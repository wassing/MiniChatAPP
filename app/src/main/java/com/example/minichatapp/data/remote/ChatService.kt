package com.example.minichatapp.data.remote

import android.util.Log
import com.example.minichatapp.data.local.AppSettings
import com.example.minichatapp.data.local.ContactDao
import com.example.minichatapp.data.repository.MessageRepository
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.domain.model.ChatRoom
import com.example.minichatapp.domain.model.Contact
import com.example.minichatapp.domain.model.MessageStatus
import com.example.minichatapp.domain.model.MessageType
import com.example.minichatapp.domain.model.RoomType
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Frame
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
    val messageRepository: MessageRepository,
    private val contactDao: ContactDao
) {
    private val TAG = "ChatService"  // 在类顶部定义

    private var webSocket: WebSocket? = null
    private var _currentUsername: String? = null
    val currentUsername: String?
        get() = _currentUsername
    private var reconnectJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val messageChannels = mutableMapOf<String, Channel<ChatMessage>>()
    private val _currentRoom = MutableStateFlow<ChatRoom?>(null)
    val currentRoom: StateFlow<ChatRoom?> = _currentRoom
    private var _publicRoom : ChatRoom? = null

    private val _onlineUsers = MutableStateFlow<Set<String>>(emptySet())
    val onlineUsers: StateFlow<Set<String>> = _onlineUsers

    private val authResponseChannel = Channel<String>(Channel.BUFFERED)

    fun connectToChat(username: String) {
        this._currentUsername = username
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
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                serviceScope.launch {
                    try {
                        val message = gson.fromJson(text, ChatMessage::class.java)

                        if (message.type != MessageType.IMAGE)
                            println("Received message: $message")
                        else
                            println("Received image message")

                        when (message.type) {
                            MessageType.USER_RESPONSE -> {
                                // USER_RESPONSE 查询用户是否存在的响应
                                val exists = message.content.toBoolean()
                                userCheckResponses.trySend(exists)
                            }
                            MessageType.CONTACT_ADDED -> {
                                // CONTACT_ADDED 被他人添加为联系人
                                val adderUsername = message.senderId
                                val contact = Contact(
                                    username = adderUsername,
                                    nickname = adderUsername,
                                    addedAt = System.currentTimeMillis()
                                )
                                contactDao.insertContact(contact)
                                message.senderId = "System"
                                messageChannels[message.roomId]?.send(message)
                                messageRepository.saveMessage(message)
                            }
                            else -> handleIncomingMessage(message)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing message", e)
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
            try {
                // 如果是自己发送的消息，不需要重复处理
                if (message.senderId == currentUsername) {
                    messageRepository.updateMessageStatus(message, message.status)
                    return@launch
                }

                when (message.type) {
                    MessageType.AUTH_RESPONSE -> {
                        authResponseChannel.send(message.content)
                    }
                    MessageType.USER_RESPONSE -> {
                        // USER_RESPONSE 消息只用于用户检查，不需要存储
                        val exists = message.content.toBoolean()
                        userCheckResponses.trySend(exists)
                    }
                    MessageType.SYSTEM_NOTIFICATION -> {
                        // SYSTEM_NOTIFICATION 消息只用于系统通知，不需要存储
                        messageChannels[message.roomId]?.send(message)
                        messageRepository.saveMessage(message)
//                        if (message.senderId != "System") {
//                            messageRepository.saveMessage(message)
//                        }
                    }
                    MessageType.TEXT -> {
                        // 普通文本消息正常存储
                        messageChannels[message.roomId]?.send(message)
                        messageRepository.saveMessage(message)
                    }
                    MessageType.IMAGE -> {
                        // 普通图片消息正常存储
                        messageChannels[message.roomId]?.send(message)
                        messageRepository.saveMessage(message)
                    }
                    else -> {
                        messageChannels[message.roomId]?.send(message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message", e)
            }
        }
    }

    private fun broadcastSystemMessage(roomId: String, content: String) {
        val systemMessage = ChatMessage(
            roomId = roomId,
            senderId = "System",
            content = content,
            type = MessageType.TEXT,
            status = MessageStatus.SENDING
        )
        handleIncomingMessage(systemMessage)
    }

    fun sendMessage(message: ChatMessage) {
        serviceScope.launch {
            try {
                println("ChatService: Sending Message - id: ${message.id}, roomId: ${message.roomId}, sender: ${message.senderId}, text: ${message.content}")

                val currentWebSocket = webSocket

                if (_connectionState.value !is ConnectionState.Connected || webSocket == null) {
                    println("ChatService: Cannot send message - not properly connected")
                    throw Exception("Not properly connected to server")
                }

                messageRepository.saveMessage(message)

                // 准备发送的消息
                val messageJson = gson.toJson(message)
                val sent = currentWebSocket?.send(messageJson)

                if (sent == true) {
                    messageRepository.updateMessageStatus(message, MessageStatus.SENT)
                } else {
                    println("ChatService: Send returned false")
                    throw Exception("Message send returned false")
                }

            } catch (e: Exception) {
                println("ChatService: Error in send process: ${e.message}")
                messageRepository.updateMessageStatus(message, MessageStatus.FAILED)
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
        _currentRoom.value = _publicRoom
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
        _publicRoom = ChatRoom(
            id = "public",
            type = RoomType.PUBLIC,
            name = "公共聊天室",
            participants = emptyList()
        )
        return _publicRoom as ChatRoom
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

    private val userCheckResponses = Channel<Boolean>(Channel.BUFFERED)


    /////////////////////////////////////////////////
    // 用户验证相关
    /////////////////////////////////////////////////
    suspend fun login(username: String, password: String): Result<Unit> {
        if (_connectionState.value !is ConnectionState.Connected) {
            return Result.failure(Exception("未连接到服务器"))
        }

        return try {
            val message = ChatMessage(
                roomId = "auth",
                senderId = username,
                content = "$username:$password",
                type = MessageType.LOGIN
            )
            val response = sendAuthMessage(message) // 新增方法等待认证响应
            when (response) {
                "LOGIN_SUCCESS" -> Result.success(Unit)
                "LOGIN_FAILED" -> Result.failure(Exception("用户名或密码错误"))
                else -> Result.failure(Exception("登录失败，请稍后重试"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("网络错误，请检查网络连接"))
        }
    }

    suspend fun register(username: String, password: String): Result<Unit> {
        if (_connectionState.value !is ConnectionState.Connected) {
            return Result.failure(Exception("未连接到服务器"))
        }

        return try {
            val message = ChatMessage(
                roomId = "auth",
                senderId = username,
                content = "$username:$password",
                type = MessageType.REGISTER
            )
            val response = sendAuthMessage(message)
            when (response) {
                "REGISTRATION_SUCCESS" -> Result.success(Unit)
                "USERNAME_EXISTS" -> Result.failure(Exception("用户名已存在"))
                else -> Result.failure(Exception("注册失败，请稍后重试"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("网络错误，请检查网络连接"))
        }
    }

    private suspend fun sendAuthMessage(message: ChatMessage): String {
        println("Sending auth message: ${message.type}")  // 添加日志
        return try {
            withTimeout(2000) { // 2秒超时
                webSocket?.send(gson.toJson(message))
                    ?: throw Exception("WebSocket 未连接")
                // 等待服务器响应
                authResponseChannel.receive()
            }
        } catch (e: TimeoutCancellationException) {
            throw Exception("认证超时，请稍后重试")
        }
    }

    suspend fun checkUserExists(username: String): Boolean {
        if (_connectionState.value !is ConnectionState.Connected) {
            throw Exception("Not connected to server")
        }

        try {
            val message = ChatMessage(
                roomId = "system",
                senderId = currentUsername ?: "unknown",
                content = username,
                type = MessageType.CHECK_USER
            )

            Log.d(TAG, "Sending user check message for: $username")
            webSocket?.send(gson.toJson(message))

            // 等待响应，设置5秒超时
            return withTimeout(5000) {
                userCheckResponses.receive()
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "User check timeout")
            throw Exception("查询用户超时")
        }
    }
}