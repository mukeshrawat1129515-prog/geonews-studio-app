package com.example.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.home.HomeScreen
import com.example.ui.news.NewsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.shorts.ShortsScreen
import com.example.ui.sources.SourcesScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.GeoNewsViewModel
import com.example.ui.youtube.YouTubeScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object News : Screen("news", "News", Icons.AutoMirrored.Filled.Article)
    object Shorts : Screen("shorts", "Shorts", Icons.Default.VideoCall)
    object YouTube : Screen("youtube", "YouTube", Icons.Default.PlayCircle)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Sources : Screen("sources", "Sources", Icons.Default.VerifiedUser)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: GeoNewsViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissUserMessage()
        }
    }

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.News,
        Screen.Shorts,
        Screen.YouTube,
        Screen.Settings
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentRoute) {
                            Screen.Home.route -> "GeoNews Studio"
                            Screen.News.route -> "Geopolitical Intelligence"
                            Screen.Shorts.route -> "Shorts Studio"
                            Screen.YouTube.route -> "YouTube Documentary"
                            Screen.Sources.route -> "Source Citations"
                            Screen.Settings.route -> "Studio Settings"
                            else -> "GeoNews Studio"
                        },
                        color = GeoTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Sources button on top bar for instant access to citations
                    IconButton(
                        onClick = {
                            if (currentRoute != Screen.Sources.route) {
                                navController.navigate(Screen.Sources.route)
                            } else {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.testTag("top_sources_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Sources",
                            tint = if (currentRoute == Screen.Sources.route) GeoPrimary else GeoTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GeoDarkBg,
                    titleContentColor = GeoTextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = GeoDarkSurface,
                contentColor = GeoTextPrimary,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GeoDarkBg,
                            selectedTextColor = GeoPrimary,
                            indicatorColor = GeoPrimary,
                            unselectedIconColor = GeoTextMuted,
                            unselectedTextColor = GeoTextMuted
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GeoDarkBg
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(180)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) },
            popExitTransition = { fadeOut(animationSpec = tween(180)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToShorts = {
                        navController.navigate(Screen.Shorts.route)
                    },
                    onNavigateToYouTube = {
                        navController.navigate(Screen.YouTube.route)
                    }
                )
            }
            composable(Screen.News.route) {
                NewsScreen(viewModel = viewModel)
            }
            composable(Screen.Shorts.route) {
                ShortsScreen(viewModel = viewModel)
            }
            composable(Screen.YouTube.route) {
                YouTubeScreen(viewModel = viewModel)
            }
            composable(Screen.Sources.route) {
                SourcesScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
