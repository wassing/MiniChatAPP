package com.example.minichatapp.di

import android.content.Context
import androidx.room.Room
import com.example.minichatapp.data.local.AppDatabase
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
                // 在数据库创建时添加默认用户
                runBlocking {
                    val database = AppDatabase::class.java.getMethod("getOpenHelper").invoke(null)
                    val userDao = (database as AppDatabase).userDao()
                    val defaultUser = User(
                        username = "admin",
                        password = "111",
                        createdAt = System.currentTimeMillis()
                    )
                    userDao.insertUser(defaultUser)     // 插入默认用户,方便调试
                }
            }
        }).build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
}