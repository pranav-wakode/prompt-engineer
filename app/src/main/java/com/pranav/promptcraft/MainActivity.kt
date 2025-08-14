package com.pranav.promptcraft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pranav.promptcraft.presentation.navigation.BottomNavItem
import com.pranav.promptcraft.presentation.navigation.Destinations
import com.pranav.promptcraft.presentation.screens.*
import com.pranav.promptcraft.presentation.viewmodels.AuthViewModel
import com.pranav.promptcraft.ui.theme.PromptCraftTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            PromptCraftApp()
        }
    }
}

@Composable
fun PromptCraftApp() {
    // Theme state
    var isDarkMode by remember { mutableStateOf(true) } // Default to dark mode as requested
    val systemDarkMode = isSystemInDarkTheme()
    
    PromptCraftTheme(
        darkTheme = isDarkMode
    ) {
        val navController = rememberNavController()
        val authViewModel: AuthViewModel = hiltViewModel()
        val currentUser by authViewModel.currentUser.collectAsState()
        
        // Start destination based on authentication state
        val startDestination = if (currentUser != null) Destinations.HOME else Destinations.LOGIN

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            // Login Screen
            composable(Destinations.LOGIN) {
                GoogleSignInScreen(
                    onNavigateToHome = {
                        navController.navigate(Destinations.HOME) {
                            popUpTo(Destinations.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            // Main App with Bottom Navigation
            composable(Destinations.HOME) {
                MainAppContent(
                    isDarkMode = isDarkMode,
                    onThemeToggle = { isDarkMode = it },
                    onNavigateToSettings = {
                        navController.navigate(Destinations.SETTINGS)
                    }
                )
            }

            // Account Screen (accessible via bottom nav)
            composable(Destinations.ACCOUNT) {
                MainAppContent(
                    isDarkMode = isDarkMode,
                    onThemeToggle = { isDarkMode = it },
                    initialDestination = Destinations.ACCOUNT
                )
            }

            // Settings Screen
            composable(Destinations.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    isDarkMode = isDarkMode,
                    onThemeToggle = { isDarkMode = it }
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    initialDestination: String = Destinations.HOME,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val bottomNavController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                BottomNavItem.values().forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    BottomNavItem.HOME -> Icons.Default.Home
                                    BottomNavItem.ACCOUNT -> Icons.Default.AccountCircle
                                },
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
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
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = initialDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Destinations.HOME) {
                HomeScreen(
                    onNavigateToSettings = onNavigateToSettings ?: {}
                )
            }
            
            composable(Destinations.ACCOUNT) {
                AccountScreen()
            }
        }
    }
}
