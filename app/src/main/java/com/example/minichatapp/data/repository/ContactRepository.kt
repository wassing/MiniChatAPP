package com.example.minichatapp.data.repository

import com.example.minichatapp.data.local.ContactDao
import com.example.minichatapp.data.local.UserDao
import com.example.minichatapp.data.remote.ChatService
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.domain.model.Contact
import com.example.minichatapp.domain.model.MessageType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ContactRepository @Inject constructor(
    private val contactDao: ContactDao,
    private val userDao: UserDao,
    private val chatService: ChatService
) {
    // 获取所有联系人，按最后消息时间排序
    fun getAllContacts(): Flow<List<Contact>> {
        return contactDao.getAllContacts()
    }

    // 添加联系人
    suspend fun addContact(username: String): Result<Contact> {
        return try {
            // 获取当前用户名
            val currentUsername = chatService.currentUsername
                ?: return Result.failure(Exception("当前用户未登录"))

            // 先检查是否已经是联系人
            if (contactDao.getContactByUsername(username) != null) {
                return Result.failure(Exception("该用户已经是你的联系人"))
            }

            // 检查用户是否存在
            val userExists = chatService.checkUserExists(username)
            if (!userExists) {
                return Result.failure(Exception("用户不存在"))
            }

            // 创建并添加联系人
            val contact = Contact(
                username = username,
                nickname = username,
                addedAt = System.currentTimeMillis()
            )
            contactDao.insertContact(contact)

            // 向服务器发送用户添加请求
            val message = ChatMessage(
                roomId = "system",
                senderId = currentUsername ?: throw IllegalStateException("User not logged in"),
                content = username,  // 使用content字段传递目标用户名
                type = MessageType.CONTACT_ADDED
            )
            chatService.sendMessage(message)

            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 删除联系人
    suspend fun removeContact(username: String): Result<Unit> {
        return try {
            val contact = contactDao.getContactByUsername(username)
                ?: return Result.failure(Exception("联系人不存在"))

            contactDao.deleteContact(contact)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 更新联系人昵称
    suspend fun updateContactNickname(username: String, newNickname: String): Result<Unit> {
        return try {
            val contact = contactDao.getContactByUsername(username)
                ?: return Result.failure(Exception("联系人不存在"))

            contactDao.insertContact(contact.copy(nickname = newNickname))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 更新联系人最后一条消息
    suspend fun updateLastMessage(username: String, message: String) {
        contactDao.updateLastMessage(
            username = username,
            message = message,
            timestamp = System.currentTimeMillis()
        )
    }

    // 增加未读消息计数
    suspend fun incrementUnreadCount(username: String) {
        contactDao.incrementUnreadCount(username)
    }

    // 清除未读消息计数
    suspend fun clearUnreadCount(username: String) {
        contactDao.clearUnreadCount(username)
    }

    // 获取单个联系人信息
    suspend fun getContact(username: String): Result<Contact> {
        return try {
            val contact = contactDao.getContactByUsername(username)
                ?: return Result.failure(Exception("联系人不存在"))
            Result.success(contact)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}