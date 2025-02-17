package com.example.minichatapp.ui.screens.settings

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.minichatapp.ui.components.CommonTextField
import com.example.minichatapp.ui.theme.MiniChatAppTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    initialServerHost: String,
    initialServerPort: Int,
    initialReconnectInterval: Long,
    onSettingsChanged: (String, Int, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var serverHost by remember { mutableStateOf(initialServerHost) }
    var serverPort by remember { mutableStateOf(initialServerPort.toString()) }
    var reconnectInterval by remember { mutableStateOf((initialReconnectInterval / 1000).toString()) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            CommonTextField(
                value = serverHost,
                onValueChange = {
                    serverHost = it
                    showError = false
                },
                label = "服务器地址",
                modifier = Modifier.padding(bottom = 16.dp)
            )

            CommonTextField(
                value = serverPort,
                onValueChange = {
                    serverPort = it
                    showError = false
                },
                label = "端口号",
                modifier = Modifier.padding(bottom = 16.dp)
            )

            CommonTextField(
                value = reconnectInterval,
                onValueChange = {
                    reconnectInterval = it
                    showError = false
                },
                label = "重连间隔（秒）",
                modifier = Modifier.padding(bottom = if (showError) 8.dp else 32.dp)
            )

            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    try {
                        val port = serverPort.toInt()
                        val interval = reconnectInterval.toLong() * 1000 // 转换为毫秒

                        if (port !in 1..65535) {
                            showError = true
                            errorMessage = "端口号必须在1-65535之间"
                            return@Button
                        }

                        if (interval < 1000) {
                            showError = true
                            errorMessage = "重连间隔不能小于1秒"
                            return@Button
                        }

                        onSettingsChanged(serverHost, port, interval)
                        onNavigateBack()
                    } catch (e: NumberFormatException) {
                        showError = true
                        errorMessage = "请输入有效的数字"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }
        }
    }
}

// SettingsScreen.kt
@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MiniChatAppTheme {
        SettingsScreen(
            onNavigateBack = { },
            initialServerHost = "10.0.2.2",
            initialServerPort = 8080,
            initialReconnectInterval = 5000L,
            onSettingsChanged = { _, _, _ -> }
        )
    }
}