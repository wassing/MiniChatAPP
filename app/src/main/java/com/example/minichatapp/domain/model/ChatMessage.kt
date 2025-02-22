package com.example.minichatapp.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index("roomId")]
)
data class ChatMessage(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val roomId: String,
    var senderId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENDING
)

enum class MessageType {
    TEXT,           // 普通文本消息
    IMAGE,          // 图片消息
    CHECK_USER,     // 检查用户是否存在
    USER_RESPONSE,  // 用户查询响应
    CONTACT_ADDED,   // 添加联系人通知
    SYSTEM_NOTIFICATION, // 系统通知
    SYSTEM_NOTIFICATION2, // 系统通知2
    REGISTER,        // 注册请求
    LOGIN,          // 登录请求
    AUTH_RESPONSE   // 认证响应
}

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
}