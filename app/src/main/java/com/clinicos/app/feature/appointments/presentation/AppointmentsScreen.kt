package com.clinicos.app.feature.appointments.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clinicos.app.core.network.dto.AppointmentDto
import com.clinicos.app.core.network.dto.DoctorDto
import com.clinicos.app.core.network.dto.PatientDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    appointmentsState: AppointmentsListState,
    selectedDate: LocalDate,
    actionState: AppointmentActionState,
    patientsList: List<PatientDto>,
    doctorsList: List<DoctorDto>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onRefresh: () -> Unit,
    onBookAppointment: (patientId: String, doctorId: String, scheduledAtIso: String, durationMinutes: Int, notes: String?) -> Unit,
    onUpdateStatus: (appointmentId: String, newStatus: String) -> Unit,
    onClearActionState: () -> Unit
) {
    var showBookDialog by remember { mutableStateOf(false) }
    var selectedAppointment by remember { mutableStateOf<AppointmentDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (actionState) {
            is AppointmentActionState.Success -> {
                snackbarHostState.showSnackbar(actionState.message)
                showBookDialog = false
                selectedAppointment = null
                onClearActionState()
            }
            is AppointmentActionState.Error -> {
                snackbarHostState.showSnackbar(actionState.message)
                onClearActionState()
            }
            else -> {}
        }
    }

    if (showBookDialog) {
        BookAppointmentDialog(
            selectedDate = selectedDate,
            patients = patientsList,
            doctors = doctorsList,
            isLoading = actionState is AppointmentActionState.Loading,
            onDismiss = { showBookDialog = false },
            onBook = onBookAppointment
        )
    }

    if (selectedAppointment != null) {
        AppointmentActionDialog(
            appointment = selectedAppointment!!,
            isLoading = actionState is AppointmentActionState.Loading,
            onDismiss = { selectedAppointment = null },
            onUpdateStatus = { status ->
                onUpdateStatus(selectedAppointment!!.id, status)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Appointments & Calendar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBookDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Book Appointment", fontWeight = FontWeight.Bold)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Date Navigation Control Bar
            DateNavigationBar(
                selectedDate = selectedDate,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onToday = onToday
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (appointmentsState) {
                    is AppointmentsListState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    is AppointmentsListState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = appointmentsState.message, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = onRefresh) { Text("Retry") }
                            }
                        }
                    }

                    is AppointmentsListState.Success -> {
                        val appointments = appointmentsState.appointments
                        if (appointments.isEmpty()) {
                            EmptyAppointmentsView(
                                selectedDate = selectedDate,
                                onBookClick = { showBookDialog = true }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(appointments, key = { it.id }) { appt ->
                                    AppointmentItemCard(
                                        appointment = appt,
                                        onClick = { selectedAppointment = appt }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateNavigationBar(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    val isToday = selectedDate == LocalDate.now()
    val dateText = if (isToday) {
        "Today, ${selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM"))}"
    } else {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Day")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isToday) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onToday) {
                        Text("Today", fontWeight = FontWeight.Bold)
                    }
                }
            }

            IconButton(onClick = onNextDay) {
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Day")
            }
        }
    }
}

@Composable
private fun AppointmentItemCard(
    appointment: AppointmentDto,
    onClick: () -> Unit
) {
    val timeDisplay = try {
        val dt = LocalDateTime.parse(appointment.scheduledAt.take(19))
        dt.format(DateTimeFormatter.ofPattern("hh:mm a"))
    } catch (e: Exception) {
        appointment.scheduledAt
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Badge
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeDisplay,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${appointment.durationMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.patient.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${appointment.doctor.fullName} (${appointment.doctor.specialty ?: "General"})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!appointment.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = appointment.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                AppointmentStatusBadge(status = appointment.status)
            }
        }
    }
}

@Composable
fun AppointmentStatusBadge(status: String) {
    val (bg, text) = when (status) {
        "SCHEDULED" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "CONFIRMED" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        "COMPLETED" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        "CANCELLED" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        "NO_SHOW" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = text,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyAppointmentsView(
    selectedDate: LocalDate,
    onBookClick: () -> Unit
) {
    val isToday = selectedDate == LocalDate.now()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isToday) "No appointments today" else "No appointments scheduled",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isToday) "Enjoy the quiet — or book a new patient consultation." else "No appointments booked for this selected date.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBookClick, shape = RoundedCornerShape(12.dp)) {
            Text("Book Appointment")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookAppointmentDialog(
    selectedDate: LocalDate,
    patients: List<PatientDto>,
    doctors: List<DoctorDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onBook: (patientId: String, doctorId: String, scheduledAtIso: String, durationMinutes: Int, notes: String?) -> Unit
) {
    var selectedPatient by remember { mutableStateOf<PatientDto?>(null) }
    var selectedDoctor by remember { mutableStateOf<DoctorDto?>(doctors.firstOrNull()) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var durationMinutes by remember { mutableIntStateOf(30) }
    var notes by remember { mutableStateOf("") }

    var patientExpanded by remember { mutableStateOf(false) }
    var doctorExpanded by remember { mutableStateOf(false) }
    var timeExpanded by remember { mutableStateOf(false) }

    val times = listOf(
        LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30),
        LocalTime.of(11, 0), LocalTime.of(11, 30), LocalTime.of(12, 0), LocalTime.of(12, 30),
        LocalTime.of(14, 0), LocalTime.of(14, 30), LocalTime.of(15, 0), LocalTime.of(15, 30),
        LocalTime.of(16, 0), LocalTime.of(16, 30), LocalTime.of(17, 0), LocalTime.of(17, 30)
    )

    val durationOptions = listOf(15, 30, 45, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Book Appointment", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Scheduling for ${selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Patient Selector
                ExposedDropdownMenuBox(
                    expanded = patientExpanded,
                    onExpandedChange = { patientExpanded = !patientExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPatient?.fullName ?: "Select Patient *",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Patient *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = patientExpanded,
                        onDismissRequest = { patientExpanded = false }
                    ) {
                        patients.forEach { pat ->
                            DropdownMenuItem(
                                text = { Text("${pat.fullName} (${pat.phone ?: "No phone"})") },
                                onClick = {
                                    selectedPatient = pat
                                    patientExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Doctor Selector
                ExposedDropdownMenuBox(
                    expanded = doctorExpanded,
                    onExpandedChange = { doctorExpanded = !doctorExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDoctor?.fullName ?: "Select Doctor *",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Doctor *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = doctorExpanded,
                        onDismissRequest = { doctorExpanded = false }
                    ) {
                        doctors.forEach { doc ->
                            DropdownMenuItem(
                                text = { Text("${doc.fullName} (${doc.specialty ?: "General"})") },
                                onClick = {
                                    selectedDoctor = doc
                                    doctorExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time Selector
                ExposedDropdownMenuBox(
                    expanded = timeExpanded,
                    onExpandedChange = { timeExpanded = !timeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Appointment Time *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = timeExpanded,
                        onDismissRequest = { timeExpanded = false }
                    ) {
                        times.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.format(DateTimeFormatter.ofPattern("hh:mm a"))) },
                                onClick = {
                                    selectedTime = t
                                    timeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Duration Selector Chips
                Text(text = "Duration", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    durationOptions.forEach { dur ->
                        FilterChip(
                            selected = durationMinutes == dur,
                            onClick = { durationMinutes = dur },
                            label = { Text("$dur min") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dt = LocalDateTime.of(selectedDate, selectedTime)
                    val scheduledAtIso = dt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    onBook(
                        selectedPatient!!.id,
                        selectedDoctor!!.id,
                        scheduledAtIso,
                        durationMinutes,
                        notes
                    )
                },
                enabled = !isLoading && selectedPatient != null && selectedDoctor != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirm Booking")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AppointmentActionDialog(
    appointment: AppointmentDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    var showCancelConfirm by remember { mutableStateOf(false) }
    var showNoShowConfirm by remember { mutableStateOf(false) }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel Appointment?") },
            text = { Text("Are you sure you want to cancel this appointment for ${appointment.patient.fullName}?") },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateStatus("CANCELLED")
                    showCancelConfirm = false
                }) {
                    Text("Cancel Appointment", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Back") }
            }
        )
    }

    if (showNoShowConfirm) {
        AlertDialog(
            onDismissRequest = { showNoShowConfirm = false },
            title = { Text("Mark as No-Show?") },
            text = { Text("Mark ${appointment.patient.fullName} as no-show for this appointment?") },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateStatus("NO_SHOW")
                    showNoShowConfirm = false
                }) {
                    Text("Mark No-Show", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoShowConfirm = false }) { Text("Back") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appointment Details", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = appointment.patient.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${appointment.doctor.fullName} (${appointment.doctor.specialty ?: "General"})", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Current Status:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AppointmentStatusBadge(status = appointment.status)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (appointment.status in listOf("SCHEDULED", "CONFIRMED")) {
                    Text(text = "Change Status Stage:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (appointment.status == "SCHEDULED") {
                        Button(
                            onClick = { onUpdateStatus("CONFIRMED") },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm Appointment")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { onUpdateStatus("COMPLETED") },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mark Completed")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showNoShowConfirm = true },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("No-Show", color = MaterialTheme.colorScheme.error)
                        }

                        OutlinedButton(
                            onClick = { showCancelConfirm = true },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
