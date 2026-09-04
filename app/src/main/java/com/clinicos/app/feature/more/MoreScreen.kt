package com.clinicos.app.feature.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clinicos.app.ui.components.SectionHeader
import com.clinicos.app.ui.components.SettingsListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onLogoutClick: () -> Unit = {},
    onClinicProfileClick: () -> Unit = {},
    onDoctorsClick: () -> Unit = {},
    onStaffClick: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out of ClinicOS?") },
            text = { Text("Are you sure you want to log out of your clinic session?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "More Options",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(title = "Clinic Operations")

            SettingsListItem(
                title = "Follow-ups",
                subtitle = "Track pending and overdue patient follow-ups",
                icon = Icons.Outlined.Schedule,
                onClick = { /* Follow-ups management UI placeholder */ }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 56.dp, end = 16.dp)
            )

            SettingsListItem(
                title = "Doctors",
                subtitle = "Manage doctor profiles, specialties and status",
                icon = Icons.Outlined.MedicalServices,
                onClick = onDoctorsClick
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 56.dp, end = 16.dp)
            )

            SettingsListItem(
                title = "Staff & Receptionists",
                subtitle = "Manage clinic receptionist team, roles and access",
                icon = Icons.Outlined.Badge,
                onClick = onStaffClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "Settings & Administration")

            SettingsListItem(
                title = "Clinic Profile & Settings",
                subtitle = "View and update clinic details, phone and timezone",
                icon = Icons.Outlined.Storefront,
                onClick = onClinicProfileClick
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 56.dp, end = 16.dp)
            )

            SettingsListItem(
                title = "App Preferences",
                subtitle = "Notifications, theme and display options",
                icon = Icons.Outlined.Settings,
                onClick = { /* App settings UI placeholder */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "Account")

            SettingsListItem(
                title = "Log Out",
                subtitle = "End your active session on this device",
                icon = Icons.AutoMirrored.Outlined.Logout,
                onClick = { showLogoutDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
