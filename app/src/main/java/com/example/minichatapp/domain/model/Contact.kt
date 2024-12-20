package com.example.minichatapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val username: String,
    val nickname: String = username,
    val avatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)