package com.example.minichatapp.data.repository

import com.example.minichatapp.data.local.MessageDao
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow
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
        messageDao.insertMessage(message)
    }

    // 批量保存消息
    suspend fun saveMessages(messages: List<ChatMessage>) {
        messageDao.insertMessages(messages)
    }

    // 更新消息状态
    suspend fun updateMessageStatus(message: ChatMessage, newStatus: MessageStatus) {
        messageDao.updateMessage(message.copy(status = newStatus))
    }

    // 删除消息
    suspend fun deleteMessage(message: ChatMessage) {
        messageDao.deleteMessage(message)
    }

    // 清除聊天室的所有消息
    suspend fun clearRoomMessages(roomId: String) {
        messageDao.clearRoomMessages(roomId)
    }

    // 获取某个时间点之后的第一条消息
    suspend fun getFirstMessageAfterTimestamp(roomId: String, timestamp: Long): ChatMessage? {
        return messageDao.getFirstMessageAfterTimestamp(roomId, timestamp)
    }
}