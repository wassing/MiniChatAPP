package com.example.minichatapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val password: String,
    val createdAt: Long = System.currentTimeMillis()
)