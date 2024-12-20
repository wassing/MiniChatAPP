package com.example.minichatapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")  // 注意这里的表名是 "messages"
data class ChatMessage(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val roomId: String,                     // 聊天室ID
    val senderId: String,                   // 发送者用户名
    val content: String,                    // 消息内容
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENDING
)

enum class MessageType {
    TEXT,
    IMAGE
}

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
}