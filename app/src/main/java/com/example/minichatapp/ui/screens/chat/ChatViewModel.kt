package com.example.minichatapp.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minichatapp.data.remote.ChatService
import com.example.minichatapp.domain.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatService: ChatService
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    // 暴露ChatService的连接状态
    val connectionState: StateFlow<ChatService.ConnectionState> = chatService.connectionState

    fun connectToChat(username: String) {
        chatService.connectToChat(username)
        viewModelScope.launch {
            chatService.getMessages().collect { message ->
                _messages.value = _messages.value + message
            }
        }
    }

    fun sendMessage(senderId: String, content: String) {
        val message = ChatMessage(
            senderId = senderId,
            content = content
        )
        chatService.sendMessage(message)
    }

    override fun onCleared() {
        super.onCleared()
        chatService.disconnect()
    }
}