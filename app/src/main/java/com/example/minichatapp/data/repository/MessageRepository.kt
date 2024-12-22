package com.example.minichatapp.data.repository

import android.util.Log
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
    private val TAG = "MessageRepository"

    // 获取特定聊天室的所有消息
    fun getMessagesForRoom(roomId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesByRoomId(roomId)
    }

    // 保存新消息
    suspend fun saveMessage(message: ChatMessage) {
        try {
            // 检查是否已存在相同ID的消息
            val existingMessage = messageDao.getMessageById(message.id)
            if (existingMessage == null) {
                messageDao.insertMessage(message)
                Log.d(TAG, "Message saved: ${message.id}")
            } else {
                // 如果消息已存在，只更新状态
                if (existingMessage.status != message.status) {
                    messageDao.updateMessage(message)
                    Log.d(TAG, "Message updated: ${message.id}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving message", e)
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

}