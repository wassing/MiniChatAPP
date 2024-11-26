package com.example.minichatapp.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.example.minichatapp.ui.components.CommonButton
import com.example.minichatapp.ui.components.CommonTextField

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 设置默认用户名和密码
    var username by remember { mutableStateOf("") }    // 默认用户名，在发布前记得删掉
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

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
            text = if (isLoading) "登录中..." else "登录",
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    showError = true
                    return@CommonButton
                }
                isLoading = true
                // 模拟登录过程
                onLoginClick(username, password)
            },
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