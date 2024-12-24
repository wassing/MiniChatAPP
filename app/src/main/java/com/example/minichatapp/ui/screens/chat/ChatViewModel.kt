package com.example.minichatapp.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minichatapp.data.remote.ChatService
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.domain.model.ChatRoom
import com.example.minichatapp.domain.model.MessageStatus
import com.example.minichatapp.domain.model.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatService: ChatService
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    // 暴露ChatService的连接状态和当前房间
    val connectionState: StateFlow<ChatService.ConnectionState> = chatService.connectionState
    val currentRoom: StateFlow<ChatRoom?> = chatService.currentRoom
    val currentUsername: String?
        get() = chatService.currentUsername

    fun initChat(username: String, room: ChatRoom) {
        // 如果还没有连接，则先连接到聊天服务器
        if (connectionState.value !is ChatService.ConnectionState.Connected) {
            chatService.connectToChat(username)
        }

        // 加入指定的聊天室
        chatService.joinRoom(room)

        // 订阅消息
        viewModelScope.launch {
            // 获取历史消息
            chatService.getMessagesForRoom(room.id).collect { messages ->
                _messages.value = messages
            }
        }

        // 订阅新消息
        viewModelScope.launch {
            chatService.getCurrentRoomMessages().collect { message ->
                _messages.value = _messages.value + message
            }
        }
    }

    fun sendMessage(senderId: String, content: String) {
        val currentRoom = currentRoom.value
        if (currentRoom == null) {
            println("ChatViewModel: Error - No current room")
            return
        }
        val message = ChatMessage(
            roomId = currentRoom.id,
            senderId = senderId,
            content = content,
            type = MessageType.TEXT,
            status = MessageStatus.SENDING
        )
        chatService.sendMessage(message)
    }

    fun leaveRoom() {
        chatService.leaveCurrentRoom()
    }

    override fun onCleared() {
        super.onCleared()
        chatService.disconnect()
    }

    fun sendImageMessage(base64Image: String) {
        viewModelScope.launch {
            try {
                println("Image size: ${base64Image.length} bytes") // 添加日志
                val currentRoom = currentRoom.value ?: throw IllegalStateException("No current room")

                val message = ChatMessage(
                    roomId = currentRoom.id,
                    senderId = currentUsername ?: throw IllegalStateException("User not logged in"),
                    content = base64Image,
                    type = MessageType.IMAGE,
                    status = MessageStatus.SENDING
                )

                // 发送到服务器
                chatService.sendMessage(message)

            } catch (e: Exception) {
                println("Error sending image: ${e.message}")
                e.printStackTrace() // 添加堆栈跟踪以便调试
            }
        }
    }

}