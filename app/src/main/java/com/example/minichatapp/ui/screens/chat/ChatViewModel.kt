package com.example.minichatapp.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minichatapp.data.remote.ChatService
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.domain.model.ChatRoom
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
        println("ChatViewModel: Attempting to send message")
        val currentRoom = currentRoom.value
        if (currentRoom == null) {
            println("ChatViewModel: Error - No current room")
            return
        }
        println("ChatViewModel: Creating message for room ${currentRoom.id}")
        val message = ChatMessage(
            roomId = currentRoom.id,
            senderId = senderId,
            content = content
        )
        println("ChatViewModel: Sending message through ChatService")
        chatService.sendMessage(message)
    }

    fun leaveRoom() {
        chatService.leaveCurrentRoom()
    }

    override fun onCleared() {
        super.onCleared()
        chatService.disconnect()
    }
}