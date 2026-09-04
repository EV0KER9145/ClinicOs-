package com.clinicos.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clinicos.app.core.network.dto.DashboardSummaryResponse
import com.clinicos.app.core.network.dto.RecentNoShowSummaryDto
import com.clinicos.app.core.network.dto.UpcomingAppointmentSummaryDto
import com.clinicos.app.feature.dashboard.presentation.DashboardUiState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToLeads: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToFollowUps: () -> Unit,
    onNavigateToOverdueFollowUps: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Action Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Dashboard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is DashboardUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is DashboardUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) { Text("Retry") }
                    }
                }

                is DashboardUiState.Success -> {
                    val summary = uiState.summary
                    val isNewClinic = summary.today.appointmentsCount == 0 &&
                            summary.today.pendingFollowUpsCount == 0 &&
                            summary.today.newLeadsCount == 0 &&
                            summary.today.newPatientsCount == 0

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Dynamic Greeting
                        GreetingHeader(summary = summary)

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isNewClinic) {
                            // New Clinic Welcome Card
                            WelcomeClinicCard(
                                onAddPatientClick = onNavigateToPatients,
                                onAddLeadClick = onNavigateToLeads,
                                onBookApptClick = onNavigateToAppointments
                            )
                        } else {
                            // ATTENTION REQUIRED Section
                            AttentionRequiredSection(
                                summary = summary,
                                onOverdueClick = onNavigateToOverdueFollowUps,
                                onNoShowsClick = onNavigateToAppointments,
                                onLeadsClick = onNavigateToLeads
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // TODAY'S SNAPSHOT Section (2x2 Grid)
                            TodaySnapshotSection(
                                summary = summary,
                                onApptsClick = onNavigateToAppointments,
                                onFollowUpsClick = onNavigateToFollowUps,
                                onPatientsClick = onNavigateToPatients,
                                onLeadsClick = onNavigateToLeads
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // UP NEXT Section
                            UpNextSection(
                                upcoming = summary.upcomingAppointments,
                                onSeeAllClick = onNavigateToAppointments
                            )

                            if (summary.recentNoShows.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                RecentNoShowsSection(
                                    noShows = summary.recentNoShows
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // QUICK ACTIONS BAR
                        QuickActionsBar(
                            onAddPatientClick = onNavigateToPatients,
                            onAddLeadClick = onNavigateToLeads,
                            onBookApptClick = onNavigateToAppointments,
                            onAddFollowUpClick = onNavigateToFollowUps
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingHeader(summary: DashboardSummaryResponse) {
    Column {
        Text(
            text = "Hello, ${summary.userName.ifEmpty { "Doctor" }}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "${summary.todayDate} • ${summary.clinicName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AttentionRequiredSection(
    summary: DashboardSummaryResponse,
    onOverdueClick: () -> Unit,
    onNoShowsClick: () -> Unit,
    onLeadsClick: () -> Unit
) {
    Column {
        Text(
            text = "WHAT NEEDS ATTENTION NOW",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Overdue Card
            AttentionCard(
                modifier = Modifier.weight(1f),
                title = "Overdue Tasks",
                count = summary.attention.overdueFollowUps,
                badgeColor = if (summary.attention.overdueFollowUps > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                textColor = if (summary.attention.overdueFollowUps > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                icon = Icons.Default.Warning,
                onClick = onOverdueClick
            )

            // Today's No Shows Card
            AttentionCard(
                modifier = Modifier.weight(1f),
                title = "No-Shows",
                count = summary.attention.noShowsToday,
                badgeColor = if (summary.attention.noShowsToday > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                textColor = if (summary.attention.noShowsToday > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                icon = Icons.Default.Person,
                onClick = onNoShowsClick
            )

            // New Enquiries Card
            AttentionCard(
                modifier = Modifier.weight(1f),
                title = "New Enquiries",
                count = summary.attention.newLeads,
                badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                textColor = MaterialTheme.colorScheme.tertiary,
                icon = Icons.AutoMirrored.Filled.Assignment,
                onClick = onLeadsClick
            )
        }
    }
}

@Composable
private fun AttentionCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    badgeColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = badgeColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun TodaySnapshotSection(
    summary: DashboardSummaryResponse,
    onApptsClick: () -> Unit,
    onFollowUpsClick: () -> Unit,
    onPatientsClick: () -> Unit,
    onLeadsClick: () -> Unit
) {
    Column {
        Text(
            text = "TODAY'S CLINIC SNAPSHOT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                modifier = Modifier.weight(1f),
                title = "Appointments",
                value = "${summary.today.appointmentsCount} scheduled",
                subtitle = "${summary.today.completedAppointments} completed",
                icon = Icons.Default.CalendarToday,
                onClick = onApptsClick
            )

            MetricTile(
                modifier = Modifier.weight(1f),
                title = "Due Follow-ups",
                value = "${summary.today.dueTodayFollowUpsCount} tasks",
                subtitle = "${summary.today.pendingFollowUpsCount} total pending",
                icon = Icons.Default.Schedule,
                onClick = onFollowUpsClick
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricTile(
                modifier = Modifier.weight(1f),
                title = "New Patients",
                value = "${summary.today.newPatientsCount} registered",
                subtitle = "Active CRM database",
                icon = Icons.Default.People,
                onClick = onPatientsClick
            )

            MetricTile(
                modifier = Modifier.weight(1f),
                title = "New Enquiries",
                value = "${summary.today.newLeadsCount} leads",
                subtitle = "Captured today",
                icon = Icons.AutoMirrored.Filled.Assignment,
                onClick = onLeadsClick
            )
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UpNextSection(
    upcoming: List<UpcomingAppointmentSummaryDto>,
    onSeeAllClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "UP NEXT APPOINTMENTS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onSeeAllClick) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "See All")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (upcoming.isEmpty()) {
                Text(
                    text = "No more upcoming appointments scheduled today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                upcoming.forEachIndexed { index, appt ->
                    val timeStr = try {
                        LocalDateTime.parse(appt.scheduledAt.take(19)).format(DateTimeFormatter.ofPattern("hh:mm a"))
                    } catch (e: Exception) {
                        appt.scheduledAt
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = appt.patientName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = appt.doctorName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (index < upcoming.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentNoShowsSection(
    noShows: List<RecentNoShowSummaryDto>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "RECENT NO-SHOWS FOR OUTREACH", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))

            noShows.forEachIndexed { index, ns ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = ns.patientName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Doctor: ${ns.doctorName} • ${ns.scheduledAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (index < noShows.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.errorContainer)
                }
            }
        }
    }
}

@Composable
private fun WelcomeClinicCard(
    onAddPatientClick: () -> Unit,
    onAddLeadClick: () -> Unit,
    onBookApptClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = Icons.Default.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Welcome to ClinicOS Command Center", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your operational dashboard helps you stay on top of patient callbacks, appointments, and enquiries.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddPatientClick) { Text("+ Patient") }
                Button(onClick = onAddLeadClick) { Text("+ Lead") }
                Button(onClick = onBookApptClick) { Text("+ Book Appt") }
            }
        }
    }
}

@Composable
private fun QuickActionsBar(
    onAddPatientClick: () -> Unit,
    onAddLeadClick: () -> Unit,
    onBookApptClick: () -> Unit,
    onAddFollowUpClick: () -> Unit
) {
    Column {
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onAddPatientClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                Text("+ Patient", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onAddLeadClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                Text("+ Lead", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBookApptClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                Text("+ Book Appt", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onAddFollowUpClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                Text("+ Task", fontWeight = FontWeight.Bold)
            }
        }
    }
}
