package com.example.minichatapp.domain.model

data class ChatRoom(
    val id: String,
    val type: RoomType,
    val name: String,
    val participants: List<String>,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RoomType {
    PUBLIC,    // 公共聊天室
    PRIVATE    // 私人聊天
}