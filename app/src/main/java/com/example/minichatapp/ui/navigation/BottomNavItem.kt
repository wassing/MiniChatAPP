package com.example.minichatapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Forum
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object PublicChat : BottomNavItem("public_chat", Icons.Default.Forum, "公共聊天")
    object Contacts : BottomNavItem("contacts", Icons.Default.Contacts, "联系人")
    object PrivateChat : BottomNavItem("private_chat", Icons.Default.Chat, "私聊")
}