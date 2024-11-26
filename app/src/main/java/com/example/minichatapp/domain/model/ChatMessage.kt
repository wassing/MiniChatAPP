package com.example.minichatapp.domain.model

data class ChatMessage(
    val id: Long = System.currentTimeMillis(), // 使用时间戳作为临时ID
    val senderId: String,  // 发送者用户名
    val content: String,   // 消息内容
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT,
    IMAGE
}