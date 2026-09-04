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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clinicos.app.core.network.ApiClient
import com.clinicos.app.core.security.TokenManager
import com.clinicos.app.feature.appointments.AppointmentsScreen
import com.clinicos.app.feature.auth.data.AuthRepository
import com.clinicos.app.feature.auth.presentation.AuthState
import com.clinicos.app.feature.auth.presentation.AuthViewModel
import com.clinicos.app.feature.auth.presentation.LoginScreen
import com.clinicos.app.feature.auth.presentation.RegisterScreen
import com.clinicos.app.feature.clinic.data.ClinicRepository
import com.clinicos.app.feature.clinic.presentation.ClinicProfileScreen
import com.clinicos.app.feature.clinic.presentation.ClinicProfileState
import com.clinicos.app.feature.clinic.presentation.ClinicSetupScreen
import com.clinicos.app.feature.clinic.presentation.ClinicViewModel
import com.clinicos.app.feature.dashboard.DashboardScreen
import com.clinicos.app.feature.leads.LeadsScreen
import com.clinicos.app.feature.more.MoreScreen
import com.clinicos.app.feature.patients.data.PatientRepository
import com.clinicos.app.feature.patients.presentation.PatientDetailScreen
import com.clinicos.app.feature.patients.presentation.PatientViewModel
import com.clinicos.app.feature.patients.presentation.PatientsScreen
import com.clinicos.app.feature.team.data.TeamRepository
import com.clinicos.app.feature.team.presentation.TeamScreen
import com.clinicos.app.feature.team.presentation.TeamViewModel

@Composable
fun ClinicOSAppEntry() {
    val context = LocalContext.current.applicationContext
    val tokenManager = remember { TokenManager(context) }
    val authApiService = remember { ApiClient.getAuthApiService(tokenManager) }
    val authRepository = remember { AuthRepository(authApiService, tokenManager) }

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(authRepository)
    )

    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (authState) {
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
                val clinicApiService = remember { ApiClient.getClinicApiService(tokenManager) }
                val clinicRepository = remember { ClinicRepository(clinicApiService) }
                val clinicViewModel: ClinicViewModel = viewModel(
                    factory = ClinicViewModel.Factory(clinicRepository)
                )

                val doctorApiService = remember { ApiClient.getDoctorApiService(tokenManager) }
                val userApiService = remember { ApiClient.getUserApiService(tokenManager) }
                val teamRepository = remember { TeamRepository(doctorApiService, userApiService) }
                val teamViewModel: TeamViewModel = viewModel(
                    factory = TeamViewModel.Factory(teamRepository)
                )

                val patientApiService = remember { ApiClient.getPatientApiService(tokenManager) }
                val tagApiService = remember { ApiClient.getTagApiService(tokenManager) }
                val patientRepository = remember { PatientRepository(patientApiService, tagApiService) }
                val patientViewModel: PatientViewModel = viewModel(
                    factory = PatientViewModel.Factory(patientRepository)
                )

                ClinicOSMainScreen(
                    clinicViewModel = clinicViewModel,
                    teamViewModel = teamViewModel,
                    patientViewModel = patientViewModel,
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
    clinicViewModel: ClinicViewModel,
    teamViewModel: TeamViewModel,
    patientViewModel: PatientViewModel,
    onLogout: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val profileState by clinicViewModel.profileState.collectAsStateWithLifecycle()
    val clinicSaveState by clinicViewModel.saveState.collectAsStateWithLifecycle()

    val doctorsState by teamViewModel.doctorsState.collectAsStateWithLifecycle()
    val staffState by teamViewModel.staffState.collectAsStateWithLifecycle()
    val teamActionState by teamViewModel.actionState.collectAsStateWithLifecycle()

    val patientsState by patientViewModel.patientsState.collectAsStateWithLifecycle()
    val patientDetailState by patientViewModel.detailState.collectAsStateWithLifecycle()
    val patientActionState by patientViewModel.actionState.collectAsStateWithLifecycle()
    val searchQuery by patientViewModel.searchQuery.collectAsStateWithLifecycle()
    val tagsState by patientViewModel.tagsState.collectAsStateWithLifecycle()

    // Automatic onboarding redirect check
    LaunchedEffect(profileState) {
        if (profileState is ClinicProfileState.Success) {
            val clinic = (profileState as ClinicProfileState.Success).clinic
            val isSetupIncomplete = clinic.phone.isNullOrBlank() || clinic.clinicType.isNullOrBlank()
            if (isSetupIncomplete && currentRoute != Screen.ClinicSetup.route && currentRoute != Screen.ClinicProfile.route) {
                navController.navigate(Screen.ClinicSetup.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = false }
                }
            }
        }
    }

    val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.ClinicSetup.route) {
                val currentClinic = (profileState as? ClinicProfileState.Success)?.clinic
                ClinicSetupScreen(
                    currentClinic = currentClinic,
                    saveState = clinicSaveState,
                    onSaveClick = { name, clinicType, phone, email, timezone ->
                        clinicViewModel.updateClinicProfile(name, clinicType, phone, email, timezone)
                    },
                    onSetupComplete = {
                        clinicViewModel.clearSaveState()
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.ClinicSetup.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ClinicProfile.route) {
                ClinicProfileScreen(
                    profileState = profileState,
                    saveState = clinicSaveState,
                    onRefresh = { clinicViewModel.loadClinicProfile() },
                    onSaveProfile = { name, clinicType, phone, email, timezone ->
                        clinicViewModel.updateClinicProfile(name, clinicType, phone, email, timezone)
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "${Screen.Team.route}?tab={tab}",
                arguments = listOf(navArgument("tab") {
                    type = NavType.IntType
                    defaultValue = 0
                })
            ) { backStackEntry ->
                val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                TeamScreen(
                    doctorsState = doctorsState,
                    staffState = staffState,
                    actionState = teamActionState,
                    initialTab = tab,
                    onRefreshDoctors = { teamViewModel.loadDoctors() },
                    onRefreshStaff = { teamViewModel.loadStaff() },
                    onCreateDoctor = { name, spec -> teamViewModel.createDoctor(name, spec) },
                    onUpdateDoctor = { id, name, spec, active -> teamViewModel.updateDoctor(id, name, spec, active) },
                    onCreateUser = { name, email, pass, role, phone -> teamViewModel.createUser(name, email, pass, role, phone) },
                    onToggleDoctorStatus = { id, status -> teamViewModel.toggleDoctorStatus(id, status) },
                    onToggleUserStatus = { id, status -> teamViewModel.toggleUserStatus(id, status) },
                    onClearActionState = { teamViewModel.clearActionState() },
                    onBackClick = { navController.popBackStack() }
                )
            }

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
                    patientsState = patientsState,
                    searchQuery = searchQuery,
                    actionState = patientActionState,
                    tagsState = tagsState,
                    onSearchChange = { patientViewModel.onSearchQueryChanged(it) },
                    onRefresh = { patientViewModel.loadPatients(searchQuery.ifBlank { null }) },
                    onPatientClick = { patientId ->
                        patientViewModel.loadPatientDetail(patientId)
                        navController.navigate(Screen.PatientDetail.createRoute(patientId))
                    },
                    onCreatePatient = { name, phone, email, dob, gender, addr, notes, tagIds ->
                        patientViewModel.createPatient(name, phone, email, dob, gender, addr, notes, tagIds)
                    },
                    onCreateTag = { name, onTagCreated ->
                        patientViewModel.createTag(name, onTagCreated)
                    },
                    onClearActionState = { patientViewModel.clearActionState() }
                )
            }

            composable(
                route = Screen.PatientDetail.route,
                arguments = listOf(navArgument("patientId") { type = NavType.StringType })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
                PatientDetailScreen(
                    patientDetailState = patientDetailState,
                    actionState = patientActionState,
                    availableTags = tagsState.tags,
                    onRefresh = { patientViewModel.loadPatientDetail(patientId) },
                    onUpdatePatient = { name, phone, email, dob, gender, addr, notes ->
                        patientViewModel.updatePatient(patientId, name, phone, email, dob, gender, addr, notes)
                    },
                    onUpdateTags = { tagIds ->
                        patientViewModel.updatePatientTags(patientId, tagIds)
                    },
                    onAddNote = { content ->
                        patientViewModel.addPatientNote(patientId, content)
                    },
                    onArchivePatient = {
                        patientViewModel.archivePatient(patientId)
                        navController.popBackStack()
                    },
                    onCreateTag = { name, onTagCreated ->
                        patientViewModel.createTag(name, onTagCreated)
                    },
                    onClearActionState = { patientViewModel.clearActionState() },
                    onBackClick = { navController.popBackStack() }
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
                    onLogoutClick = onLogout,
                    onClinicProfileClick = {
                        navController.navigate(Screen.ClinicProfile.route)
                    },
                    onDoctorsClick = {
                        navController.navigate("${Screen.Team.route}?tab=0")
                    },
                    onStaffClick = {
                        navController.navigate("${Screen.Team.route}?tab=1")
                    }
                )
            }
        }
    }
}
