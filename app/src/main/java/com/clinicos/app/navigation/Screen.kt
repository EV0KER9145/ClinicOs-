package com.clinicos.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object ClinicSetup : Screen(
        route = "clinic_setup",
        title = "Clinic Setup",
        selectedIcon = Icons.Filled.Storefront,
        unselectedIcon = Icons.Outlined.Storefront
    )

    object ClinicProfile : Screen(
        route = "clinic_profile",
        title = "Clinic Profile",
        selectedIcon = Icons.Filled.Storefront,
        unselectedIcon = Icons.Outlined.Storefront
    )

    object Notifications : Screen(
        route = "notifications",
        title = "Notifications",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
    )

    object Team : Screen(
        route = "team",
        title = "Team Management",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    )

    object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Patients : Screen(
        route = "patients",
        title = "Patients",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    )

    object PatientDetail : Screen(
        route = "patient_detail/{patientId}",
        title = "Patient Profile",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    ) {
        fun createRoute(patientId: String) = "patient_detail/$patientId"
    }

    object Leads : Screen(
        route = "leads",
        title = "Leads",
        selectedIcon = Icons.AutoMirrored.Filled.Assignment,
        unselectedIcon = Icons.AutoMirrored.Outlined.Assignment
    )

    object LeadDetail : Screen(
        route = "lead_detail/{leadId}",
        title = "Lead Details",
        selectedIcon = Icons.AutoMirrored.Filled.Assignment,
        unselectedIcon = Icons.AutoMirrored.Outlined.Assignment
    ) {
        fun createRoute(leadId: String) = "lead_detail/$leadId"
    }

    object Appointments : Screen(
        route = "appointments",
        title = "Appts",
        selectedIcon = Icons.Filled.CalendarToday,
        unselectedIcon = Icons.Outlined.CalendarToday
    )

    object FollowUps : Screen(
        route = "follow_ups",
        title = "Follow-ups",
        selectedIcon = Icons.Filled.Schedule,
        unselectedIcon = Icons.Outlined.Schedule
    )

    object More : Screen(
        route = "more",
        title = "More",
        selectedIcon = Icons.Filled.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz
    )

    companion object {
        val bottomNavItems = listOf(
            Dashboard,
            Patients,
            Leads,
            Appointments,
            More
        )
    }
}
