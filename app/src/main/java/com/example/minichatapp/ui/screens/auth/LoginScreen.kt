package com.example.minichatapp.ui.screens.auth

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.minichatapp.data.remote.ChatService
import com.example.minichatapp.ui.components.CommonButton
import com.example.minichatapp.ui.components.CommonTextField

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var rememberPassword by remember { mutableStateOf(false) }

    // 设置默认用户名和密码
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState() // 从 ViewModel 获取连接状态

    // 在组件首次加载时读取保存的登录信息
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        username = prefs.getString("username", "") ?: ""
        // 只有在之前选择了记住密码的情况下才读取密码
        if (prefs.getBoolean("remember_password", false)) {
            password = prefs.getString("password", "") ?: ""
            rememberPassword = true
        }
    }

    LaunchedEffect(loginState) {
        when (loginState) {
            is AuthViewModel.LoginState.Success -> {
                val user = (loginState as AuthViewModel.LoginState.Success).user
                if (rememberPassword) {
                    // 保存登录信息
                    val prefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("username", username)
                        .putString("password", password)
                        .putBoolean("remember_password", true)
                        .apply()
                }
                onLoginSuccess(user.username)
            }

            is AuthViewModel.LoginState.Error -> {
                Toast.makeText(
                    context,
                    (loginState as AuthViewModel.LoginState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {}
        }
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            is ChatService.ConnectionState.Failed -> {
                Toast.makeText(
                    context,
                    "无法连接到服务器：${(connectionState as ChatService.ConnectionState.Failed).message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is ChatService.ConnectionState.Connected -> {
                println("已连接到服务器")
            }
            is ChatService.ConnectionState.Connecting -> {
                println("正在连接服务器...")
            }
            else -> {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo or Title
        Text(
            text = "Mini Chat",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Username field
        CommonTextField(
            value = username,
            onValueChange = {
                username = it
                showError = false
            },
            label = "用户名",
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Password field
        CommonTextField(
            value = password,
            onValueChange = {
                password = it
                showError = false
            },
            label = "密码",
            isPassword = true,
            modifier = Modifier.padding(bottom = if (showError) 8.dp else 32.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberPassword,
                onCheckedChange = { rememberPassword = it }
            )
            Text(
                text = "记住密码",
                modifier = Modifier.clickable { rememberPassword = !rememberPassword }
            )
        }

        // Error message
        if (showError) {
            Text(
                text = "用户名或密码错误",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
        }

        // Login button
        CommonButton(
            text = when {
                loginState is AuthViewModel.LoginState.Loading -> "登录中..."
                connectionState !is ChatService.ConnectionState.Connected -> "连接中..."
                else -> "登录"
            },
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "用户名和密码不能为空", Toast.LENGTH_SHORT).show()
                    return@CommonButton
                }
                viewModel.login(username, password)
            },
            enabled = connectionState is ChatService.ConnectionState.Connected &&
                    loginState !is AuthViewModel.LoginState.Loading,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Register link
        TextButton(
            onClick = onRegisterClick,
        ) {
            Text("还没有账号？立即注册")
        }

        // Settings link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置"
                )
            }
        }
    }
}
