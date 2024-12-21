package com.example.minichatapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.minichatapp.domain.model.ChatRoom
import com.example.minichatapp.domain.model.RoomType
import com.example.minichatapp.ui.navigation.BottomNavItem
import com.example.minichatapp.ui.screens.chat.ChatScreen
import com.example.minichatapp.ui.screens.chat.ChatViewModel
import com.example.minichatapp.ui.screens.contact.ContactScreen
import com.example.minichatapp.data.remote.ChatService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    username: String,
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    chatRoom: ChatRoom? = null,
    isPublicChat: Boolean = true,
    onNavigateToPrivateChat: ((String) -> Unit)? = null
) {
    val bottomNavController = rememberNavController()
    val chatViewModel: ChatViewModel = hiltViewModel()

    // 监控当前导航状态
    val currentRoute = bottomNavController.currentBackStackEntryAsState().value?.destination?.route

    // 如果提供了特定的聊天室，则使用它；否则使用公共聊天室
    val currentChatRoom = chatRoom ?: ChatRoom(
        id = "public",
        type = RoomType.PUBLIC,
        name = "公共聊天室",
        participants = emptyList()
    )

    // 初始化聊天
    LaunchedEffect(currentChatRoom.id, username) {
        println("MainScreen: Initializing chat - username: $username, room: ${currentChatRoom.id}")
        chatViewModel.initChat(username, currentChatRoom)
    }

    // 收集状态
    val messages by chatViewModel.messages.collectAsState()
    val connectionState by chatViewModel.connectionState.collectAsState()

    // 监听连接状态变化
    LaunchedEffect(connectionState) {
        println("MainScreen: Connection state changed to: $connectionState")
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            // 仅在公共聊天时显示底部导航栏
            if (isPublicChat) {
                NavigationBar {
                    val items = listOf(
                        BottomNavItem.PublicChat,
                        BottomNavItem.Contacts
                    )
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                bottomNavController.navigate(item.route) {
                                    popUpTo(bottomNavController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isPublicChat) {
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.PublicChat.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(BottomNavItem.PublicChat.route) {
                    println("MainScreen: Setting up PublicChat screen")
                    ChatScreen(
                        messages = messages,
                        currentUsername = username,
                        chatRoom = currentChatRoom,
                        onSendMessage = { content ->
                            println("MainScreen: Sending message in public chat")
                            chatViewModel.sendMessage(username, content)
                        },
                        isConnected = connectionState is ChatService.ConnectionState.Connected
                    )
                }

                composable(BottomNavItem.Contacts.route) {
                    ContactScreen(
                        onContactClick = { contact ->
                            onNavigateToPrivateChat?.invoke(contact.username)
                        }
                    )
                }
            }
        } else {
            // 私聊界面
            ChatScreen(
                messages = messages,
                currentUsername = username,
                chatRoom = currentChatRoom,
                onSendMessage = { content ->
                    chatViewModel.sendMessage(username, content)
                },
                onBackClick = {
                    chatViewModel.leaveRoom()
                    navController?.popBackStack()
                },
                isConnected = connectionState is ChatService.ConnectionState.Connected,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    // 清理效果
    DisposableEffect(currentChatRoom.id) {
        onDispose {
            if (!isPublicChat) {
                chatViewModel.leaveRoom()
            }
        }
    }
}