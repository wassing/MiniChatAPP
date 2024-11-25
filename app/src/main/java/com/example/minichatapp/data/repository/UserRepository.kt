package com.example.minichatapp.data.repository

import com.example.minichatapp.data.local.UserDao
import com.example.minichatapp.domain.model.User
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    suspend fun registerUser(username: String, password: String): Result<User> {
        return try {
            val existingUser = userDao.getUserByUsername(username)
            if (existingUser != null) {
                Result.failure(Exception("用户名已存在"))
            } else {
                val user = User(username = username, password = password)
                userDao.insertUser(user)
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(username: String, password: String): Result<User> {
        return try {
            val user = userDao.getUserByCredentials(username, password)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("用户名或密码错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}