package com.example.minichatapp.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.minichatapp.domain.model.ChatMessage
import com.example.minichatapp.ui.components.CommonTextField
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    currentUsername: String,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }

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
            title = { Text("聊天室") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
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
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("输入消息...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = MaterialTheme.colorScheme.primary
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
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Sender info: username and time
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

        // Message bubble
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

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}