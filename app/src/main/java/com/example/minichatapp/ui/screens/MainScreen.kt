package com.example.minichatapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.ui.screens.chat.ChatScreen

@Composable
fun MainScreen(
    username: String
) {
    // 临时使用静态消息列表，后续会替换为实时数据
    val messages = remember { mutableStateListOf<ChatMessage>() }

    ChatScreen(
        messages = messages,
        currentUsername = username,
        onSendMessage = { content ->
            messages.add(
                ChatMessage(
                    senderId = username,
                    content = content
                )
            )
        }
    )
}