package com.clinicos.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.clinicos.app.core.network.ApiClient
import com.clinicos.app.core.security.TokenManager
import com.clinicos.app.feature.appointments.AppointmentsScreen
import com.clinicos.app.feature.auth.data.AuthRepository
import com.clinicos.app.feature.auth.presentation.AuthState
import com.clinicos.app.feature.auth.presentation.AuthViewModel
import com.clinicos.app.feature.auth.presentation.LoginScreen
import com.clinicos.app.feature.auth.presentation.RegisterScreen
import com.clinicos.app.feature.dashboard.DashboardScreen
import com.clinicos.app.feature.leads.LeadsScreen
import com.clinicos.app.feature.more.MoreScreen
import com.clinicos.app.feature.patients.PatientsScreen

@Composable
fun ClinicOSAppEntry() {
    val context = LocalContext.current.applicationContext
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiClient.getAuthApiService(tokenManager) }
    val repository = remember { AuthRepository(apiService, tokenManager) }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(repository)
    )

    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = authState) {
            is AuthState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Verifying session...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is AuthState.Unauthenticated -> {
                AuthNavHost(authViewModel = authViewModel)
            }

            is AuthState.Authenticated -> {
                ClinicOSMainScreen(
                    onLogout = { authViewModel.logout() }
                )
            }
        }
    }
}

@Composable
fun AuthNavHost(
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController()
) {
    val loginUiState by authViewModel.loginUiState.collectAsStateWithLifecycle()
    val registerUiState by authViewModel.registerUiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                uiState = loginUiState,
                onLoginClick = { email, password ->
                    authViewModel.login(email, password)
                },
                onNavigateToRegister = {
                    authViewModel.clearRegisterState()
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                uiState = registerUiState,
                onRegisterClick = { clinicName, clinicType, clinicPhone, clinicEmail, fullName, email, password ->
                    authViewModel.register(
                        clinicName,
                        clinicType,
                        clinicPhone,
                        clinicEmail,
                        fullName,
                        email,
                        password
                    )
                },
                onNavigateToLogin = {
                    authViewModel.clearLoginError()
                    navController.popBackStack("login", inclusive = false)
                }
            )
        }
    }
}

@Composable
fun ClinicOSMainScreen(
    onLogout: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Screen.bottomNavItems.forEach { screen ->
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
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onAddPatientClick = {
                        navController.navigate(Screen.Patients.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddAppointmentClick = {
                        navController.navigate(Screen.Appointments.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddFollowUpClick = {
                        navController.navigate(Screen.More.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSeeAllAppointmentsClick = {
                        navController.navigate(Screen.Appointments.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Patients.route) {
                PatientsScreen(
                    onAddPatientClick = { /* UI action placeholder */ }
                )
            }

            composable(Screen.Leads.route) {
                LeadsScreen(
                    onAddLeadClick = { /* UI action placeholder */ }
                )
            }

            composable(Screen.Appointments.route) {
                AppointmentsScreen(
                    onAddAppointmentClick = { /* UI action placeholder */ }
                )
            }

            composable(Screen.More.route) {
                MoreScreen(
                    onLogoutClick = onLogout
                )
            }
        }
    }
}
