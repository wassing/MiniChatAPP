package com.example.minichatapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.domain.model.Contact
import com.example.minichatapp.domain.model.User

@Database(
    entities = [
        User::class,
        Contact::class,
        ChatMessage::class
    ],
    version = 2,  // 增加版本号
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
}