package com.clinicos.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clinicos.app.ui.components.AppointmentListItem
import com.clinicos.app.ui.components.MetricCard
import com.clinicos.app.ui.components.QuickActionButton
import com.clinicos.app.ui.components.SectionHeader
import com.clinicos.app.ui.theme.getAlertAmberColors
import com.clinicos.app.ui.theme.getAlertRedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddPatientClick: () -> Unit = {},
    onAddAppointmentClick: () -> Unit = {},
    onAddFollowUpClick: () -> Unit = {},
    onSeeAllAppointmentsClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = DashboardSampleData.greeting,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DashboardSampleData.clinicName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Notifications click UI placeholder */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            // Quick Actions Section
            SectionHeader(title = "Quick Actions")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    label = "+ Patient",
                    icon = Icons.Default.PersonAdd,
                    onClick = onAddPatientClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "+ Appt",
                    icon = Icons.Default.Event,
                    onClick = onAddAppointmentClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    label = "+ Follow-up",
                    icon = Icons.Default.Add,
                    onClick = onAddFollowUpClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Today Operational Section
            SectionHeader(title = "Today")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Appointments",
                    value = "${DashboardSampleData.appointmentsToday}",
                    subtitle = "Scheduled today",
                    icon = Icons.Default.CalendarToday,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "New Patients",
                    value = "${DashboardSampleData.newPatientsToday}",
                    subtitle = "Registered today",
                    icon = Icons.Default.Group,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Attention Required Section
            SectionHeader(title = "Attention Required")
            val amberColors = getAlertAmberColors()
            val redColors = getAlertRedColors()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Overdue Follow-ups",
                    value = "${DashboardSampleData.overdueFollowups}",
                    subtitle = "Pending action",
                    icon = Icons.Default.Schedule,
                    containerColor = amberColors.container,
                    contentColor = amberColors.content,
                    valueColor = amberColors.content,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "No-shows",
                    value = "${DashboardSampleData.noShows}",
                    subtitle = "Needs recovery",
                    icon = Icons.Default.Cancel,
                    containerColor = redColors.container,
                    contentColor = redColors.content,
                    valueColor = redColors.content,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Upcoming Appointments
            SectionHeader(
                title = "Upcoming Appointments",
                actionText = "See All",
                onActionClick = onSeeAllAppointmentsClick
            )

            DashboardSampleData.upcomingAppointments.forEach { appointment ->
                AppointmentListItem(
                    time = appointment.time,
                    patientName = appointment.patientName,
                    doctorName = appointment.doctorName,
                    status = appointment.status,
                    onClick = { /* Appointment detail UI placeholder */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
