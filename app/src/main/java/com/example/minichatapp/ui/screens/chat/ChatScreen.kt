package com.example.minichatapp.ui.screens.chat

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.domain.model.ChatRoom
import com.example.minichatapp.domain.model.MessageStatus
import com.example.minichatapp.domain.model.MessageType
import com.example.minichatapp.domain.model.RoomType
import com.example.minichatapp.ui.components.rememberImagePickerLauncher
import com.example.minichatapp.ui.components.rememberPermissionLauncher
import com.example.minichatapp.ui.screens.chat.ImageUtils.handleImageSelection
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    currentUsername: String,
    chatRoom: ChatRoom,
    onSendMessage: (String) -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    isConnected: Boolean = false
) {
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePicker = rememberImagePickerLauncher { uri ->
        // 使用 scope 启动协程
        scope.launch {
            handleImageSelection(uri, chatViewModel, context)
        }
    }
    val permissionLauncher = rememberPermissionLauncher {
        // 权限获取后启动图片选择器
        imagePicker.launch("image/*")
    }

    // 处理图片选择按钮点击
    val onImageButtonClick = {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ 使用 READ_MEDIA_IMAGES
                println("Requesting READ_MEDIA_IMAGES permission") // 添加日志
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-12 使用 READ_EXTERNAL_STORAGE
                println("Requesting READ_EXTERNAL_STORAGE permission") // 添加日志
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                // 低版本Android直接启动选择器
                println("Launching image picker directly") // 添加日志
                imagePicker.launch("image/*")
            }
        }
    }

    // 自动滚动到最新消息
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(chatRoom.name) },
            navigationIcon = {
                if (chatRoom.type == RoomType.PRIVATE && onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(
                    message = message,
                    isCurrentUser = message.senderId == currentUsername
                )
            }
        }

        // Input Area
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 添加图片按钮
                IconButton(
                    onClick = onImageButtonClick,
                    enabled = isConnected
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "发送图片",
                        tint = if (isConnected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // 现有的消息输入框
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text(if (isConnected) "输入消息..." else "正在连接...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    singleLine = true,
                    enabled = isConnected
                )

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    },
                    enabled = isConnected && messageText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = if (isConnected && messageText.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}


@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (message.senderId == "System") Alignment.CenterHorizontally
        else if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (message.senderId != "System") {
            // 用户名和时间
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
            ) {
                Text(
                    text = "${message.senderId} · ${formatTimestamp(message.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 消息气泡和状态图标的容器
            Column(
                horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
            ) {
                when (message.type) {
                    MessageType.IMAGE -> {
                        ImageMessageContent(
                            base64Image = message.content,
                            isCurrentUser = isCurrentUser
                        )
                    }
                    else -> {
                        // 文本消息气泡
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                                        bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                                    )
                                )
                                .background(
                                    if (isCurrentUser) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = message.content,
                                color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 状态图标在气泡下方
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
                ) {
                    MessageStatusIcon(
                        status = message.status,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        } else {
            // 系统消息样式保持不变
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ImageMessageContent(
    base64Image: String,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(base64Image) {
        try {
            val bytes = Base64.decode(base64Image, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    bitmap?.let { bmp ->
        Box(
            modifier = modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                        bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isCurrentUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(4.dp)
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Shared image",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    } ?: Icon(
        imageVector = Icons.Default.BrokenImage,
        contentDescription = "Failed to load image",
        tint = MaterialTheme.colorScheme.error
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun MessageStatusIcon(
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    val icon = when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule
        MessageStatus.SENT -> Icons.Default.Done
        MessageStatus.FAILED -> Icons.Default.Error
    }

    val tint = when (status) {
        MessageStatus.SENDING -> Color.Gray
        MessageStatus.SENT -> Color.Green
        MessageStatus.FAILED -> Color.Red
    }

    Icon(
        imageVector = icon,
        contentDescription = status.name,
        modifier = modifier.size(12.dp),
        tint = tint
    )
}