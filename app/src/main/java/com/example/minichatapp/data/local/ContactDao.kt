package com.example.minichatapp.data.local

import androidx.room.*
import com.example.minichatapp.domain.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY lastMessageTime DESC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE username = :username")
    suspend fun getContactByUsername(username: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("UPDATE contacts SET lastMessage = :message, lastMessageTime = :timestamp WHERE username = :username")
    suspend fun updateLastMessage(username: String, message: String, timestamp: Long)

    @Query("UPDATE contacts SET unreadCount = unreadCount + 1 WHERE username = :username")
    suspend fun incrementUnreadCount(username: String)

    @Query("UPDATE contacts SET unreadCount = 0 WHERE username = :username")
    suspend fun clearUnreadCount(username: String)
}