package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.NavigationScreen
import com.example.ui.screens.PlaceDetailScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LocationViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val viewModel: LocationViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isNavigatingState by viewModel.isNavigating.collectAsStateWithLifecycle()

    // Manage bottom bar visibility - show only on main listing screens to maximize space!
    val showBottomBar = currentRoute in listOf("explore", "saved")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("main_navigation_bar"),
                    containerColor = Color.White.copy(alpha = 0.95f),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "explore",
                        onClick = {
                            navController.navigate("explore") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "explore") Icons.Filled.Explore else Icons.Outlined.Explore,
                                contentDescription = "Explore"
                            )
                        },
                        label = { Text("Explore", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF002020),
                            selectedTextColor = Color(0xFF002020),
                            indicatorColor = Color(0xFFCCE8E8),
                            unselectedIconColor = Color(0xFF3F4848),
                            unselectedTextColor = Color(0xFF3F4848)
                        ),
                        modifier = Modifier.testTag("tab_explore")
                    )

                    // Navigation HUD shortcut if there is an active navigation running!
                    if (isNavigatingState) {
                        NavigationBarItem(
                            selected = currentRoute == "navigation",
                            onClick = {
                                navController.navigate("navigation") {
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == "navigation") Icons.Filled.Navigation else Icons.Outlined.Navigation,
                                    contentDescription = "Active Nav"
                                )
                            },
                            label = { Text("HUD Nav", fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF002020),
                                selectedTextColor = Color(0xFF002020),
                                indicatorColor = Color(0xFFCCE8E8),
                                unselectedIconColor = Color(0xFF006A6A),
                                unselectedTextColor = Color(0xFF006A6A)
                            ),
                            modifier = Modifier.testTag("tab_active_nav")
                        )
                    }

                    NavigationBarItem(
                        selected = currentRoute == "saved",
                        onClick = {
                            navController.navigate("saved") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == "saved") Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Saved"
                            )
                        },
                        label = { Text("Saved Hub", fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF002020),
                            selectedTextColor = Color(0xFF002020),
                            indicatorColor = Color(0xFFCCE8E8),
                            unselectedIconColor = Color(0xFF3F4848),
                            unselectedTextColor = Color(0xFF3F4848)
                        ),
                        modifier = Modifier.testTag("tab_saved")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "explore",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("explore") {
                ExploreScreen(
                    viewModel = viewModel,
                    onPlaceClick = { place ->
                        viewModel.setPlaceDetail(place)
                        navController.navigate("detail")
                    }
                )
            }

            composable("saved") {
                SavedScreen(
                    viewModel = viewModel,
                    onPlaceClick = { place ->
                        viewModel.setPlaceDetail(place)
                        navController.navigate("detail")
                    }
                )
            }

            composable("detail") {
                PlaceDetailScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        viewModel.clearPlaceDetail()
                        navController.popBackStack()
                    },
                    onNavigateClick = { place ->
                        viewModel.startNavigation(place)
                        navController.navigate("navigation") {
                            // Ensure back stack pop goes back to explore
                            popUpTo("explore")
                        }
                    }
                )
            }

            composable("navigation") {
                NavigationScreen(
                    viewModel = viewModel,
                    onCloseClick = {
                        viewModel.stopNavigation()
                        navController.navigate("explore") {
                            popUpTo("explore") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
