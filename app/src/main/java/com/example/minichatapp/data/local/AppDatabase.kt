package com.example.minichatapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.minichatapp.domain.model.Converters
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.domain.model.Contact
import com.example.minichatapp.domain.model.User

@Database(
    entities = [
        User::class,
        Contact::class,
        ChatMessage::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao

    companion object {
        // 定义数据库迁移策略
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 如果需要，添加消息表的创建语句
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id INTEGER PRIMARY KEY NOT NULL,
                        roomId TEXT NOT NULL,
                        senderId TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        status TEXT NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_roomId ON messages(roomId)")
            }
        }
    }
}