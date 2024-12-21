package com.example.minichatapp.data.local

import androidx.room.*
import com.example.minichatapp.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Transaction
    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesByRoomId(roomId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessageSync(message: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>)

    @Update
    suspend fun updateMessage(message: ChatMessage)

    @Delete
    suspend fun deleteMessage(message: ChatMessage)

    @Query("DELETE FROM messages WHERE roomId = :roomId")
    suspend fun clearRoomMessages(roomId: String)

    @Query("SELECT * FROM messages WHERE roomId = :roomId AND timestamp > :timestamp ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstMessageAfterTimestamp(roomId: String, timestamp: Long): ChatMessage?
}