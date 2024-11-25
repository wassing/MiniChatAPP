package com.example.minichatapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
//import com.example.minichatapp.ui.screens.chat.ChatListScreen
//import com.example.minichatapp.ui.screens.contact.ContactScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "chats",
                    onClick = { navController.navigate("chats") },
                    icon = { Icon(Icons.Filled.Face, contentDescription = "聊天") },
                    label = { Text("聊天") }
                )
                NavigationBarItem(
                    selected = currentRoute == "contacts",
                    onClick = { navController.navigate("contacts") },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "联系人") },
                    label = { Text("联系人") }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "chats",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("chats") {
                ChatListScreen(
                    onChatClick = { chatId ->
                        navController.navigate("chat/$chatId")
                    }
                )
            }
            composable("contacts") {
                ContactScreen(
                    onContactClick = { userId ->
                        navController.navigate("chat/$userId")
                    }
                )
            }
        }
    }
}

@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit
) {
    // 临时实现
    Box(modifier = Modifier.fillMaxSize()) {
        Text("聊天列表界面")
    }
}

@Composable
fun ContactScreen(
    onContactClick: (String) -> Unit
) {
    // 临时实现
    Box(modifier = Modifier.fillMaxSize()) {
        Text("联系人界面")
    }
}