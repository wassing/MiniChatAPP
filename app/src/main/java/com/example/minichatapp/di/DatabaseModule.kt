package com.example.minichatapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.minichatapp.data.local.AppDatabase
import com.example.minichatapp.data.local.ContactDao
import com.example.minichatapp.data.local.MessageDao
import com.example.minichatapp.data.local.UserDao
import com.example.minichatapp.data.repository.MessageRepository
import com.example.minichatapp.domain.model.User
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "minichat.db"
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    println("DatabaseModule: Database created, creating tables...")
                    // 手动创建消息表
                    db.execSQL("""
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
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_roomId ON messages(roomId)")
                    println("DatabaseModule: Messages table created")

                    // 验证表是否创建成功
                    val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='messages'")
                    cursor.use {
                        if (it.moveToFirst()) {
                            println("DatabaseModule: Messages table exists after creation")
                        } else {
                            println("DatabaseModule: Failed to create messages table!")
                        }
                    }
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    println("DatabaseModule: Database opened")
                    // 检查表结构
                    try {
                        val cursor = db.query("PRAGMA table_info(messages)")
                        cursor.use {
                            println("DatabaseModule: Messages table structure:")
                            while (it.moveToNext()) {
                                val columnName = it.getString(it.getColumnIndex("name"))
                                val columnType = it.getString(it.getColumnIndex("type"))
                                println("DatabaseModule: Column: $columnName, Type: $columnType")
                            }
                        }
                    } catch (e: Exception) {
                        println("DatabaseModule: Error checking table structure: ${e.message}")
                    }
                }
            })
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // 添加这行以减少 SQLite 锁定问题
            .fallbackToDestructiveMigration() // 在开发阶段使用，生产环境需要proper migration
            .build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao {
        return database.contactDao()
    }

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideMessageRepository(messageDao: MessageDao): MessageRepository {
        return MessageRepository(messageDao)
    }
}