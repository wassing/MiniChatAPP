package com.example.minichatapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.minichatapp.ui.screens.MainScreen
import com.example.minichatapp.ui.screens.auth.LoginScreen
import com.example.minichatapp.ui.screens.auth.RegisterScreen
import com.example.minichatapp.ui.screens.settings.SettingsScreen
import com.example.minichatapp.ui.screens.settings.SettingsViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Settings : Screen("settings")
    object Main : Screen("main/{username}") {
        fun createRoute(username: String) = "main/$username"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

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
            val serverHost by settingsViewModel.serverHost.collectAsState()
            val serverPort by settingsViewModel.serverPort.collectAsState()
            val reconnectInterval by settingsViewModel.reconnectInterval.collectAsState()

            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                initialServerHost = serverHost,
                initialServerPort = serverPort,
                initialReconnectInterval = reconnectInterval,
                onSettingsChanged = { host, port, interval ->
                    settingsViewModel.updateSettings(host, port, interval)
                }
            )
        }

        composable(
            route = Screen.Main.route,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            MainScreen(username = username)
        }
    }
}