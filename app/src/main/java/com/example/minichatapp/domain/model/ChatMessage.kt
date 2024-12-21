package com.example.minichatapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(
    tableName = "messages",
    indices = [Index("roomId")]
)
data class ChatMessage(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val roomId: String,
    val senderId: String,
    val content: String,
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