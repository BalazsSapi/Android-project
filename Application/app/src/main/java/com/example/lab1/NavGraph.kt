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
import androidx.navigation.toRoute
import com.example.lab1.utils.SessionManager

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }

    // Auto-login: ha van mentett token, a Login-t átugorjuk.
    val startDestination = remember {
        if (sessionManager.isLoggedIn) MainGraph else Login
    }

    val bottomNavItems = listOf(
        BottomNavItem(route = Home, label = "Home"),
        BottomNavItem(route = Profile, label = "Profile")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Az alsó sáv csak az elsődleges képernyőkön (Home, Profile) látszik
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
                    },
                    onNavigateToResetPassword = {
                        navController.navigate(ResetPassword)
                    }
                )
            }

            composable<Register> {
                RegisterScreen(
                    onRegisterSuccess = {
                        // A signup is session-t ad, ezért rögtön be vagyunk lépve
                        navController.navigate(MainGraph) {
                            popUpTo<Login> { inclusive = true }
                        }
                    }
                )
            }

            composable<ResetPassword> {
                ResetPasswordScreen(
                    onBackToLogin = { navController.popBackStack() }
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
                        onScheduleClick = { scheduleId ->
                            navController.navigate(ScheduleDetails(scheduleId)) { launchSingleTop = true }
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
                    ProfileScreen(
                        onAddHabit = {
                            navController.navigate(AddHabit) { launchSingleTop = true }
                        },
                        onEditProfile = {
                            navController.navigate(EditProfile) { launchSingleTop = true }
                        },
                        onLoggedOut = {
                            navController.navigate(Login) {
                                popUpTo<MainGraph> { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable<EditProfile> {
                    EditProfileScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }

                composable<AddHabit> {
                    AddHabitScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            navController.navigate(Home) {
                                // Home újralétrehozása, hogy újratöltse a habiteket a backendről
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

                composable<ScheduleDetails> { backStackEntry ->
                    val route: ScheduleDetails = backStackEntry.toRoute()
                    ScheduleDetailsScreen(
                        scheduleId = route.scheduleId,
                        onBack = { navController.popBackStack() },
                        onEdit = {
                            navController.navigate(EditSchedule(route.scheduleId)) { launchSingleTop = true }
                        },
                        onDeleted = {
                            // Törlés után a Home újratöltődik
                            navController.navigate(Home) {
                                popUpTo<Home> { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable<EditSchedule> { backStackEntry ->
                    val route: EditSchedule = backStackEntry.toRoute()
                    EditScheduleScreen(
                        scheduleId = route.scheduleId,
                        onBack = { navController.popBackStack() },
                        // Mentés után vissza a részletekhez, ami újratölti az adatokat
                        onSaved = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private data class BottomNavItem(val route: Any, val label: String)
