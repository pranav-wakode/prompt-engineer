package com.pranav.promptcraft

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.pranav.promptcraft.domain.model.InAppNotification
import com.pranav.promptcraft.presentation.components.ForceUpdateDialog
import com.pranav.promptcraft.presentation.components.NotificationItem
import com.pranav.promptcraft.presentation.navigation.BottomNavItem
import com.pranav.promptcraft.presentation.navigation.Destinations
import com.pranav.promptcraft.presentation.screens.*
import com.pranav.promptcraft.presentation.viewmodels.AuthViewModel
import com.pranav.promptcraft.ui.theme.PromptCraftTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfig
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            PromptCraftApp(remoteConfig = remoteConfig)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptCraftApp(remoteConfig: FirebaseRemoteConfig) {
    // Theme state
    var isDarkMode by remember { mutableStateOf(true) } // Default to dark mode as requested
    val systemDarkMode = isSystemInDarkTheme()
    
    // Force update state
    var showForceUpdateDialog by remember { mutableStateOf(false) }
    var isUpdateCancellable by remember { mutableStateOf(true) }
    var updateUrl by remember { mutableStateOf("") }
    
    // Notifications state
    var showNotificationIcon by remember { mutableStateOf(true) }
    var notificationHasBadge by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf<List<InAppNotification>>(emptyList()) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Fetch remote config values
    LaunchedEffect(Unit) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Check for force update
                    val minVersionCode = remoteConfig.getLong("android_min_version_code").toInt()
                    val currentVersionCode = try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                    } catch (e: PackageManager.NameNotFoundException) {
                        Int.MAX_VALUE // Default to max value if package info not found
                    }
                    
                    if (currentVersionCode < minVersionCode) {
                        isUpdateCancellable = remoteConfig.getBoolean("is_update_dialog_cancellable")
                        updateUrl = remoteConfig.getString("update_url")
                        showForceUpdateDialog = true
                    }
                    
                    // Update notification settings
                    showNotificationIcon = remoteConfig.getBoolean("show_notification_icon")
                    notificationHasBadge = remoteConfig.getBoolean("notification_icon_has_badge")
                    
                    // Parse notifications JSON
                    val notificationsJson = remoteConfig.getString("in_app_notifications_json")
                    try {
                        notifications = Json.decodeFromString<List<InAppNotification>>(notificationsJson)
                    } catch (e: Exception) {
                        // Handle parsing error gracefully
                        notifications = emptyList()
                    }
                }
            }
    }
    
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
                    },
                    showNotificationIcon = showNotificationIcon,
                    notificationHasBadge = notificationHasBadge,
                    onNotificationClick = { showNotificationSheet = true }
                )
            }

            // Account Screen (accessible via bottom nav)
            composable(Destinations.ACCOUNT) {
                MainAppContent(
                    isDarkMode = isDarkMode,
                    onThemeToggle = { isDarkMode = it },
                    initialDestination = Destinations.ACCOUNT,
                    onNavigateToSettings = {
                        navController.navigate(Destinations.SETTINGS)
                    },
                    showNotificationIcon = showNotificationIcon,
                    notificationHasBadge = notificationHasBadge,
                    onNotificationClick = { showNotificationSheet = true }
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
        
        // Force Update Dialog
        if (showForceUpdateDialog) {
            ForceUpdateDialog(
                isCancellable = isUpdateCancellable,
                updateUrl = updateUrl,
                onDismiss = {
                    showForceUpdateDialog = false
                }
            )
        }
        
        // Notification Bottom Sheet
        if (showNotificationSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNotificationSheet = false }
            ) {
                NotificationBottomSheetContent(
                    notifications = notifications,
                    onDismiss = { showNotificationSheet = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    initialDestination: String = Destinations.HOME,
    onNavigateToSettings: (() -> Unit)? = null,
    showNotificationIcon: Boolean = false,
    notificationHasBadge: Boolean = false,
    onNotificationClick: () -> Unit = {}
) {
    val bottomNavController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "PromptCraft",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Notification icon (conditionally displayed)
                    if (showNotificationIcon) {
                        BadgedBox(
                            badge = {
                                if (notificationHasBadge) {
                                    Badge()
                                }
                            }
                        ) {
                            IconButton(onClick = onNotificationClick) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications"
                                )
                            }
                        }
                    }
                    
                    // Settings icon (always displayed)
                    onNavigateToSettings?.let { settingsCallback ->
                        IconButton(onClick = settingsCallback) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                }
            )
        },
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

@Composable
fun NotificationBottomSheetContent(
    notifications: List<InAppNotification>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
        
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Notifications list
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No notifications",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Check back later for updates",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
