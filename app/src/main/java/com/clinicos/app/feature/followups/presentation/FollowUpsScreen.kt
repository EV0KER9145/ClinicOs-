package com.clinicos.app.feature.followups.presentation

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.clinicos.app.core.network.dto.FollowUpDto
import com.clinicos.app.core.network.dto.LeadDto
import com.clinicos.app.core.network.dto.PatientDto
import com.clinicos.app.core.network.dto.UserDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpsScreen(
    followUpsState: FollowUpsListState,
    actionState: FollowUpActionState,
    activeFilter: String,
    patientsList: List<PatientDto>,
    leadsList: List<LeadDto>,
    staffList: List<UserDto>,
    currentUserId: String?,
    preselectedPatientId: String? = null,
    preselectedLeadId: String? = null,
    onFilterSelect: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateFollowUp: (title: String, dueAtIso: String, notes: String?, assignedUserId: String?, patientId: String?, leadId: String?) -> Unit,
    onCompleteFollowUp: (String) -> Unit,
    onCancelFollowUp: (String) -> Unit,
    onClearActionState: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(preselectedPatientId != null || preselectedLeadId != null) }
    var selectedFollowUp by remember { mutableStateOf<FollowUpDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val filterOptions = listOf(
        "PENDING" to "Pending Tasks",
        "OVERDUE" to "Overdue ⚠",
        "MY_TASKS" to "My Tasks",
        "COMPLETED" to "Completed"
    )

    LaunchedEffect(actionState) {
        when (actionState) {
            is FollowUpActionState.Success -> {
                snackbarHostState.showSnackbar(actionState.message)
                showAddDialog = false
                selectedFollowUp = null
                onClearActionState()
            }
            is FollowUpActionState.Error -> {
                snackbarHostState.showSnackbar(actionState.message)
                onClearActionState()
            }
            else -> {}
        }
    }

    if (showAddDialog) {
        AddFollowUpDialog(
            patients = patientsList,
            leads = leadsList,
            staff = staffList,
            preselectedPatientId = preselectedPatientId,
            preselectedLeadId = preselectedLeadId,
            isLoading = actionState is FollowUpActionState.Loading,
            onDismiss = { showAddDialog = false },
            onSave = onCreateFollowUp
        )
    }

    if (selectedFollowUp != null) {
        FollowUpDetailDialog(
            followUp = selectedFollowUp!!,
            isLoading = actionState is FollowUpActionState.Loading,
            onDismiss = { selectedFollowUp = null },
            onComplete = { onCompleteFollowUp(selectedFollowUp!!.id) },
            onCancel = { onCancelFollowUp(selectedFollowUp!!.id) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Follow-ups & Queue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Add Follow-up", fontWeight = FontWeight.Bold)
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
            // Filter Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { (key, label) ->
                    val isSelected = activeFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelect(key) },
                        label = { Text(label) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (followUpsState) {
                    is FollowUpsListState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    is FollowUpsListState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = followUpsState.message, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = onRefresh) { Text("Retry") }
                            }
                        }
                    }

                    is FollowUpsListState.Success -> {
                        val items = followUpsState.followUps
                        if (items.isEmpty()) {
                            EmptyFollowUpsView(
                                activeFilter = activeFilter,
                                onAddClick = { showAddDialog = true }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items, key = { it.id }) { fu ->
                                    FollowUpItemCard(
                                        followUp = fu,
                                        onClick = { selectedFollowUp = fu },
                                        onQuickComplete = { onCompleteFollowUp(fu.id) }
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
private fun FollowUpItemCard(
    followUp: FollowUpDto,
    onClick: () -> Unit,
    onQuickComplete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (followUp.isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
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
            // Icon Badge
            Surface(
                shape = CircleShape,
                color = if (followUp.isOverdue) MaterialTheme.colorScheme.errorContainer
                else if (followUp.status == "COMPLETED") MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (followUp.isOverdue) Icons.Default.Warning
                        else if (followUp.status == "COMPLETED") Icons.Default.CheckCircle
                        else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (followUp.isOverdue) MaterialTheme.colorScheme.error
                        else if (followUp.status == "COMPLETED") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = followUp.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Related Patient or Lead
                if (followUp.patient != null) {
                    Text(
                        text = "Patient: ${followUp.patient.fullName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } else if (followUp.lead != null) {
                    Text(
                        text = "Lead: ${followUp.lead.fullName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (followUp.isOverdue) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "OVERDUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onError,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = "Due: ${followUp.dueAt.take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (followUp.status == "PENDING") {
                IconButton(onClick = onQuickComplete) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFollowUpsView(
    activeFilter: String,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ListAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (activeFilter == "OVERDUE") "No overdue follow-ups!" else "You're all caught up!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (activeFilter == "OVERDUE") "Great job staying on top of clinic communication." else "No follow-ups require your attention right now.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddClick, shape = RoundedCornerShape(12.dp)) {
            Text("Add Follow-up Task")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFollowUpDialog(
    patients: List<PatientDto>,
    leads: List<LeadDto>,
    staff: List<UserDto>,
    preselectedPatientId: String?,
    preselectedLeadId: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, dueAtIso: String, notes: String?, assignedUserId: String?, patientId: String?, leadId: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedTargetType by remember { mutableIntStateOf(if (preselectedLeadId != null) 1 else 0) } // 0 = Patient, 1 = Lead
    var selectedPatient by remember { mutableStateOf(patients.firstOrNull { it.id == preselectedPatientId }) }
    var selectedLead by remember { mutableStateOf(leads.firstOrNull { it.id == preselectedLeadId }) }
    var selectedAssignee by remember { mutableStateOf<UserDto?>(null) }
    var dueDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var dueTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var notes by remember { mutableStateOf("") }

    var patientExpanded by remember { mutableStateOf(false) }
    var leadExpanded by remember { mutableStateOf(false) }
    var staffExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Follow-up Task", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title * (e.g. Call for review)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Target Selector Tabs (Patient vs Lead)
                Text("Related Entity *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                TabRow(selectedTabIndex = selectedTargetType) {
                    Tab(
                        selected = selectedTargetType == 0,
                        onClick = { selectedTargetType = 0 },
                        text = { Text("Patient") }
                    )
                    Tab(
                        selected = selectedTargetType == 1,
                        onClick = { selectedTargetType = 1 },
                        text = { Text("Enquiry Lead") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedTargetType == 0) {
                    // Patient Dropdown
                    ExposedDropdownMenuBox(
                        expanded = patientExpanded,
                        onExpandedChange = { patientExpanded = !patientExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPatient?.fullName ?: "Select Patient *",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Patient") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = patientExpanded,
                            onDismissRequest = { patientExpanded = false }
                        ) {
                            patients.forEach { pat ->
                                DropdownMenuItem(
                                    text = { Text(pat.fullName) },
                                    onClick = {
                                        selectedPatient = pat
                                        patientExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Lead Dropdown
                    ExposedDropdownMenuBox(
                        expanded = leadExpanded,
                        onExpandedChange = { leadExpanded = !leadExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedLead?.fullName ?: "Select Lead *",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Lead") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = leadExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = leadExpanded,
                            onDismissRequest = { leadExpanded = false }
                        ) {
                            leads.forEach { l ->
                                DropdownMenuItem(
                                    text = { Text(l.fullName) },
                                    onClick = {
                                        selectedLead = l
                                        leadExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Assign Staff Dropdown
                ExposedDropdownMenuBox(
                    expanded = staffExpanded,
                    onExpandedChange = { staffExpanded = !staffExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAssignee?.fullName ?: "Me (Current User)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign To") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = staffExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = staffExpanded,
                        onDismissRequest = { staffExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Me (Current User)") },
                            onClick = {
                                selectedAssignee = null
                                staffExpanded = false
                            }
                        )
                        staff.forEach { u ->
                            DropdownMenuItem(
                                text = { Text("${u.fullName} (${u.role})") },
                                onClick = {
                                    selectedAssignee = u
                                    staffExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Context") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Note, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dt = LocalDateTime.of(dueDate, dueTime)
                    val dueAtIso = dt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    val targetPatientId = if (selectedTargetType == 0) selectedPatient?.id else null
                    val targetLeadId = if (selectedTargetType == 1) selectedLead?.id else null

                    onSave(
                        title,
                        dueAtIso,
                        notes,
                        selectedAssignee?.id,
                        targetPatientId,
                        targetLeadId
                    )
                },
                enabled = !isLoading && title.isNotBlank() && ((selectedTargetType == 0 && selectedPatient != null) || (selectedTargetType == 1 && selectedLead != null))
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("Create Follow-up")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FollowUpDetailDialog(
    followUp: FollowUpDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    var showCancelConfirm by remember { mutableStateOf(false) }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel Follow-up?") },
            text = { Text("This task will remain in historical records but will no longer require action.") },
            confirmButton = {
                TextButton(onClick = {
                    onCancel()
                    showCancelConfirm = false
                }) {
                    Text("Cancel Follow-up", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Back") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(followUp.title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (followUp.patient != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Patient: ${followUp.patient.fullName}", style = MaterialTheme.typography.titleMedium)
                    }
                } else if (followUp.lead != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Lead: ${followUp.lead.fullName}", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Due Date: ${followUp.dueAt.take(10)}", style = MaterialTheme.typography.bodyMedium)
                if (followUp.assignedUser != null) {
                    Text(text = "Assigned To: ${followUp.assignedUser.fullName}", style = MaterialTheme.typography.bodyMedium)
                }
                if (!followUp.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Notes: ${followUp.notes}", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (followUp.status == "PENDING") {
                    Button(
                        onClick = onComplete,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark Completed")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showCancelConfirm = true },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Follow-up", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
