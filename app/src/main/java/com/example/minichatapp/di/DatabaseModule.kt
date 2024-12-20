package com.example.minichatapp.di

import android.content.Context
import androidx.room.Room
import com.example.minichatapp.data.local.AppDatabase
import com.example.minichatapp.data.local.ContactDao
import com.example.minichatapp.data.local.MessageDao
import com.example.minichatapp.data.local.UserDao
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
        ).addCallback(object : androidx.room.RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                runBlocking {
                    // 在这里添加默认数据
                    val database = provideAppDatabase(context)
                    val userDao = database.userDao()

                    // 添加默认用户
                    val defaultUser = User(
                        username = "admin",
                        password = "111",
                        createdAt = System.currentTimeMillis()
                    )
                    try {
                        userDao.insertUser(defaultUser)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
            .fallbackToDestructiveMigration() // 在数据库升级时删除所有数据并重新创建
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
}