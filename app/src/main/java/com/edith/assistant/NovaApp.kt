package com.edith.assistant

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.edith.assistant.data.NovaAppState
import com.edith.assistant.forge.ForgeController
import com.edith.assistant.ui.components.edithBottomNavigation
import com.edith.assistant.ui.screens.about.AboutScreen
import com.edith.assistant.ui.screens.chat.ChatScreen
import com.edith.assistant.ui.screens.debug.DebugScreen
import com.edith.assistant.ui.screens.forge.ForgeScreen
import com.edith.assistant.ui.screens.history.HistoryScreen
import com.edith.assistant.ui.screens.home.HomeScreen
import com.edith.assistant.ui.screens.onboarding.OnboardingScreen
import com.edith.assistant.ui.screens.profile.ProfileScreen
import com.edith.assistant.ui.screens.quickactions.QuickActionsScreen
import com.edith.assistant.ui.screens.settings.OrbStyleScreen
import com.edith.assistant.ui.screens.settings.PermissionsScreen
import com.edith.assistant.ui.screens.settings.PersonalityScreen
import com.edith.assistant.ui.screens.settings.SettingsScreen
import com.edith.assistant.ui.screens.voice.VoiceScreen
import com.edith.assistant.ui.theme.LocalBottomBarInset
import com.edith.assistant.ui.theme.nightSky

@Composable
fun NovaApp(appState: NovaAppState) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val pendingForgeOpen by ForgeController.pendingOpen.collectAsState()
    LaunchedEffect(pendingForgeOpen) {
        if (pendingForgeOpen) {
            ForgeController.consumePendingOpen()
            navController.navigate("forge") {
                launchSingleTop = true
            }
        }
    }

    val bottomBarRoutes = setOf("home", "chat", "history", "settings")
    val showBottomBar = currentRoute in bottomBarRoutes

    CompositionLocalProvider(
        LocalBottomBarInset provides if (showBottomBar) 88.dp else 0.dp
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomBar) {
                    edithBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onOpenVoice = {
                            navController.navigate("voice") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .nightSky()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = if (appState.hasOnboarded) "home" else "onboarding",
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() }
                ) {
                    composable("onboarding") {
                        OnboardingScreen(
                            appState = appState,
                            onComplete = {
                                navController.navigate("home") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            appState = appState,
                            onOpenVoice = { navController.navigate("voice") },
                            onOpenChat = { navController.navigate("chat") },
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenProfile = { navController.navigate("profile") },
                            onTrySaying = { phrase ->
                                if (phrase.contains("website", ignoreCase = true)) {
                                    ForgeController.start(navController.context, phrase)
                                    navController.navigate("forge")
                                } else {
                                    navController.navigate("chat")
                                }
                            }
                        )
                    }

                    composable("chat") {
                        ChatScreen(
                            appState = appState,
                            onBack = { navController.popBackStack() },
                            onOpenForge = { navController.navigate("forge") }
                        )
                    }

                    composable("voice") {
                        VoiceScreen(
                            appState = appState,
                            onClose = { navController.popBackStack() },
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }

                    composable("history") {
                        HistoryScreen(
                            appState = appState,
                            onBack = { navController.popBackStack() },
                            onStartVoice = { navController.navigate("voice") }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            appState = appState,
                            onBack = { navController.popBackStack() },
                            onNavigateToPersonality = { navController.navigate("personality") },
                            onNavigateToOrbStyle = { navController.navigate("orb_style") },
                            onNavigateToPermissions = { navController.navigate("permissions") },
                            onNavigateToDebug = { navController.navigate("debug") },
                            onNavigateToAbout = { navController.navigate("about") }
                        )
                    }

                    composable("personality") {
                        PersonalityScreen(
                            appState = appState,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("orb_style") {
                        OrbStyleScreen(
                            appState = appState,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("permissions") {
                        PermissionsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("quick_actions") {
                        QuickActionsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenForge = { navController.navigate("forge") }
                        )
                    }

                    composable("profile") {
                        ProfileScreen(
                            appState = appState,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("about") {
                        AboutScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("debug") {
                        DebugScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("forge") {
                        ForgeScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
