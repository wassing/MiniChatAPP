package com.example.minichatapp.ui.screens.auth

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.minichatapp.ui.components.CommonButton
import com.example.minichatapp.ui.components.CommonTextField
import com.example.minichatapp.ui.theme.MiniChatAppTheme

@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "注册账号",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // 用户名输入框
        CommonTextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = null
            },
            label = "用户名（至少3个字符）",
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 密码输入框
        CommonTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = "密码（至少6个字符）",
            isPassword = true,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 确认密码输入框
        CommonTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null
            },
            label = "确认密码",
            isPassword = true,
            modifier = Modifier.padding(bottom = if (errorMessage != null) 8.dp else 32.dp)
        )

        // 错误信息显示
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
        }

        // 注册按钮
        CommonButton(
            text = if (isLoading) "注册中..." else "注册",
            onClick = {
                // 输入验证
                when {
                    username.length < 3 -> {
                        errorMessage = "用户名至少需要3个字符"
                        return@CommonButton
                    }
                    password.length < 6 -> {
                        errorMessage = "密码至少需要6个字符"
                        return@CommonButton
                    }
                    password != confirmPassword -> {
                        errorMessage = "两次输入的密码不一致"
                        return@CommonButton
                    }
                    else -> {
                        isLoading = true
                        // 调用注册回调
                        onRegisterClick(username, password, confirmPassword)
                    }
                }
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 返回登录按钮
        TextButton(
            onClick = onBackToLogin,
        ) {
            Text("已有账号？返回登录")
        }
    }
}

// RegisterScreen.kt
@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    MiniChatAppTheme {
        RegisterScreen(
            onRegisterClick = { _, _, _ -> },
            onBackToLogin = { }
        )
    }
}