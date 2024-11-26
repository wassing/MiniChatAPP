package com.example.minichatapp.ui.screens

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minichatapp.data.remote.ChatService
import com.example.minichatapp.ui.screens.chat.ChatScreen
import com.example.minichatapp.ui.screens.chat.ChatViewModel

@Composable
fun MainScreen(
    username: String,
    viewModel: ChatViewModel = hiltViewModel()
) {
    // 连接到聊天服务器
    LaunchedEffect(Unit) {
        viewModel.connectToChat(username)
    }

    val messages by viewModel.messages.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    ChatScreen(
        messages = messages,
        currentUsername = username,
        onSendMessage = { content ->
            viewModel.sendMessage(username, content)
        },
        isConnected = connectionState is ChatService.ConnectionState.Connected
    )
}