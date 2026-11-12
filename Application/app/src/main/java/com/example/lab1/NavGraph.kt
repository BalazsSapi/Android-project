package com.example.lab1

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.lab1.utils.SessionManager

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }

    // Auto-login: skip Login when a token is already stored.
    val startDestination = remember {
        if (sessionManager.isLoggedIn) MainGraph else Login
    }

    val bottomNavItems = listOf(
        BottomNavItem(route = Home, label = "Home"),
        BottomNavItem(route = Profile, label = "Profile")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // The bottom bar is only shown on the primary screens (Home, Profile)
    val showBottomBar = currentDestination?.hierarchy?.any {
        it.hasRoute(Home::class) || it.hasRoute(Profile::class)
    } == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(item.route::class)
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo<MainGraph> { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(text = item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Login> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(MainGraph) {
                            popUpTo<Login> { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Register)
                    }
                )
            }

            composable<Register> {
                RegisterScreen(
                    onRegisterSuccess = {
                        // Signup also returns a session, so the user is logged in right away
                        navController.navigate(MainGraph) {
                            popUpTo<Login> { inclusive = true }
                        }
                    }
                )
            }

            navigation<MainGraph>(startDestination = Home) {
                composable<Home> {
                    HomeScreen(
                        onAddHabit = {
                            navController.navigate(AddHabit) { launchSingleTop = true }
                        },
                        onCreateSchedule = {
                            navController.navigate(CreateSchedule) { launchSingleTop = true }
                        },
                        onLogout = {
                            navController.navigate(Login) {
                                popUpTo<MainGraph> { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable<Profile> {
                    ProfileScreen()
                }

                composable<AddHabit> {
                    AddHabitScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            navController.navigate(Home) {
                                // Recreate Home so it reloads the habits from the backend
                                popUpTo<Home> { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToCreateSchedule = {
                            navController.navigate(CreateSchedule) { launchSingleTop = true }
                        }
                    )
                }

                composable<CreateSchedule> {
                    CreateScheduleScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            navController.navigate(Home) {
                                popUpTo<Home> { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToAddHabit = {
                            navController.navigate(AddHabit) { launchSingleTop = true }
                        }
                    )
                }
            }
        }
    }
}

private data class BottomNavItem(val route: Any, val label: String)
