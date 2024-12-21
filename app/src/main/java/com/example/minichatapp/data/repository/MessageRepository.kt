package com.example.minichatapp.data.repository

import com.example.minichatapp.data.local.MessageDao
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.domain.model.MessageStatus
import com.example.minichatapp.domain.model.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MessageRepository @Inject constructor(
    private val messageDao: MessageDao
) {
    // 获取特定聊天室的所有消息
    fun getMessagesForRoom(roomId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesByRoomId(roomId)
    }

    // 保存新消息
    suspend fun saveMessage(message: ChatMessage) {
        try {
            println("MessageRepository: Attempting to save message with id: ${message.id}")
            println("MessageRepository: Message content: ${message.content}")
            println("MessageRepository: Message type: ${message.type}")
            println("MessageRepository: Message status: ${message.status}")

            withContext(Dispatchers.IO) {
                try {
                    println("MessageRepository: Trying to Inserting message")
                    messageDao.insertMessage(message)
                    println("MessageRepository: Message saved successfully")
                } catch (e: Exception) {
                    println("MessageRepository: Error during insertion: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
            }
        } catch (e: Exception) {
            println("MessageRepository: Outer error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    // 更新消息状态
    suspend fun updateMessageStatus(message: ChatMessage, newStatus: MessageStatus) {
        try {
            messageDao.updateMessage(message.copy(status = newStatus))
            println("MessageRepository: Message status updated to $newStatus")
        } catch (e: Exception) {
            println("MessageRepository: Error updating message status: ${e.message}")
            throw e
        }
    }

    // 删除消息
    suspend fun deleteMessage(message: ChatMessage) {
        try {
            messageDao.deleteMessage(message)
        } catch (e: Exception) {
            println("MessageRepository: Error deleting message: ${e.message}")
            throw e
        }
    }

    // 清除聊天室的所有消息
    suspend fun clearRoomMessages(roomId: String) {
        try {
            messageDao.clearRoomMessages(roomId)
        } catch (e: Exception) {
            println("MessageRepository: Error clearing room messages: ${e.message}")
            throw e
        }
    }

    suspend fun testDatabaseConnection() {
        withContext(Dispatchers.IO) {
            try {
                // 尝试插入测试消息
                val testMessage = ChatMessage(
                    id = System.currentTimeMillis(),
                    roomId = "test",
                    senderId = "test",
                    content = "test message",
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.TEXT,
                    status = MessageStatus.SENDING
                )

                println("MessageRepository: Testing database - Inserting test message")
                messageDao.insertMessage(testMessage)
                println("MessageRepository: Test message inserted successfully")

                // 验证插入
                val messages = messageDao.getMessagesByRoomId("test").first()
                println("MessageRepository: Retrieved ${messages.size} messages from test room")

            } catch (e: Exception) {
                println("MessageRepository: Database test failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}