package com.example.minichatapp.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minichatapp.data.remote.ChatService
import com.example.minichatapp.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val chatService: ChatService
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Initial)
    val registerState: StateFlow<RegisterState> = _registerState

    // 暴露连接状态
    val connectionState: StateFlow<ChatService.ConnectionState> = chatService.connectionState

    init {
        // 初始化时建立连接
        chatService.initConnection()
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // 确保连接已建立
                if (chatService.connectionState.value !is ChatService.ConnectionState.Connected) {
                    _loginState.value = LoginState.Error("未连接到服务器")
                    return@launch
                }

                val result = chatService.login(username, password)
                result.fold(
                    onSuccess = {
                        _loginState.value = LoginState.Success(User(username = username, password = password))
                    },
                    onFailure = { e ->
                        _loginState.value = LoginState.Error(e.message ?: "登录失败")
                    }
                )
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "登录失败")
            }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                // 等待建立连接
                if (chatService.connectionState.first() !is ChatService.ConnectionState.Connected) {
                    _registerState.value = RegisterState.Error("未能连接到服务器")
                    return@launch
                }

                val result = chatService.register(username, password)
                result.fold(
                    onSuccess = {
                        _registerState.value = RegisterState.Success(User(username = username, password = password))
                    },
                    onFailure = { e ->
                        _registerState.value = RegisterState.Error(e.message ?: "注册失败")
                    }
                )
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "注册失败")
            }
        }
    }

    sealed class LoginState {
        object Initial : LoginState()
        object Loading : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    sealed class RegisterState {
        object Initial : RegisterState()
        object Loading : RegisterState()
        data class Success(val user: User) : RegisterState()
        data class Error(val message: String) : RegisterState()
    }
}