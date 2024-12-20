package com.example.minichatapp.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.minichatapp.ui.navigation.BottomNavItem
import com.example.minichatapp.ui.screens.MainScreen
import com.example.minichatapp.ui.screens.auth.LoginScreen
import com.example.minichatapp.ui.screens.auth.RegisterScreen
import com.example.minichatapp.ui.screens.contact.ContactScreen
import com.example.minichatapp.ui.screens.settings.SettingsScreen
import com.example.minichatapp.domain.model.ChatRoom
import com.example.minichatapp.domain.model.RoomType

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Settings : Screen("settings")
    object Main : Screen("main/{username}") {
        fun createRoute(username: String) = "main/$username"
    }
    object PublicChat : Screen("public_chat")
    object Contacts : Screen("contacts")
    object PrivateChat : Screen("private_chat/{contactUsername}") {
        fun createRoute(contactUsername: String) = "private_chat/$contactUsername"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginClick = { username, password ->
                    navController.navigate(Screen.Main.createRoute(username)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterClick = { username, password, confirmPassword ->
                    navController.popBackStack()
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel = hiltViewModel<com.example.minichatapp.ui.screens.settings.SettingsViewModel>()
            val serverHost by viewModel.serverHost.collectAsState()
            val serverPort by viewModel.serverPort.collectAsState()
            val reconnectInterval by viewModel.reconnectInterval.collectAsState()

            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                initialServerHost = serverHost,
                initialServerPort = serverPort,
                initialReconnectInterval = reconnectInterval,
                onSettingsChanged = viewModel::updateSettings
            )
        }

        composable(
            route = Screen.Main.route,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            MainScreen(
                username = username,
                navController = navController,
                onNavigateToPrivateChat = { contactUsername ->
                    navController.navigate(Screen.PrivateChat.createRoute(contactUsername))
                }
            )
        }

        composable(Screen.PublicChat.route) {
            MainScreen(
                username = navController.previousBackStackEntry?.arguments?.getString("username") ?: "",
                isPublicChat = true
            )
        }

        composable(
            route = Screen.PrivateChat.route,
            arguments = listOf(navArgument("contactUsername") { type = NavType.StringType })
        ) { backStackEntry ->
            val contactUsername = backStackEntry.arguments?.getString("contactUsername") ?: ""
            val username = navController.previousBackStackEntry?.arguments?.getString("username") ?: ""
            MainScreen(
                username = username,
                isPublicChat = false,
                chatRoom = ChatRoom(
                    id = listOf(username, contactUsername).sorted().joinToString("-"),
                    type = RoomType.PRIVATE,
                    name = contactUsername,
                    participants = listOf(username, contactUsername)
                )
            )
        }

        composable(Screen.Contacts.route) {
            ContactScreen(
                onContactClick = { contact ->
                    navController.navigate(Screen.PrivateChat.createRoute(contact.username))
                }
            )
        }
    }
}