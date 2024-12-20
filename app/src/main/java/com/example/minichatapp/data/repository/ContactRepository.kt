package com.example.minichatapp.data.repository

import com.example.minichatapp.data.local.ContactDao
import com.example.minichatapp.data.local.UserDao
import com.example.minichatapp.domain.model.Contact
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ContactRepository @Inject constructor(
    private val contactDao: ContactDao,
    private val userDao: UserDao
) {
    // 获取所有联系人，按最后消息时间排序
    fun getAllContacts(): Flow<List<Contact>> {
        return contactDao.getAllContacts()
    }

    // 添加联系人
    suspend fun addContact(username: String): Result<Contact> {
        return try {
            // 首先检查用户是否存在
            val user = userDao.getUserByUsername(username)
                ?: return Result.failure(Exception("用户不存在"))

            // 检查是否已经是联系人
            if (contactDao.getContactByUsername(username) != null) {
                return Result.failure(Exception("该用户已经是你的联系人"))
            }

            // 创建并添加联系人
            val contact = Contact(
                username = username,
                nickname = username, // 初始昵称与用户名相同
                addedAt = System.currentTimeMillis()
            )
            contactDao.insertContact(contact)
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