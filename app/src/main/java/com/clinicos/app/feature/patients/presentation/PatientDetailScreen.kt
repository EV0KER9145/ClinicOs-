package com.clinicos.app.feature.patients.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clinicos.app.core.network.dto.PatientDetailDto
import com.clinicos.app.core.network.dto.TagDto

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientDetailState: PatientDetailState,
    actionState: PatientActionState,
    availableTags: List<TagDto>,
    onRefresh: () -> Unit,
    onUpdatePatient: (fullName: String, phone: String?, email: String?, dob: String?, gender: String?, address: String?, notes: String?) -> Unit,
    onUpdateTags: (List<String>) -> Unit,
    onAddNote: (String) -> Unit,
    onArchivePatient: () -> Unit,
    onCreateTag: (String, (String) -> Unit) -> Unit,
    onClearActionState: () -> Unit,
    onBackClick: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (actionState) {
            is PatientActionState.Success -> {
                snackbarHostState.showSnackbar(actionState.message)
                isEditing = false
                showArchiveDialog = false
                newNoteText = ""
                onClearActionState()
            }
            is PatientActionState.Error -> {
                snackbarHostState.showSnackbar(actionState.message)
                onClearActionState()
            }
            else -> {}
        }
    }

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("Archive Patient?") },
            text = { Text("This patient will remain in historical records but won't appear in active patient lists.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onArchivePatient()
                        showArchiveDialog = false
                    }
                ) {
                    Text("Archive Patient", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showArchiveDialog = true }) {
                        Icon(imageVector = Icons.Default.Archive, contentDescription = "Archive Patient", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (patientDetailState) {
                is PatientDetailState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is PatientDetailState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = patientDetailState.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onRefresh) { Text("Retry") }
                        }
                    }
                }

                is PatientDetailState.Success -> {
                    val patient = patientDetailState.patient

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        // Header Card
                        PatientHeaderCard(patient = patient, onEditClick = { isEditing = !isEditing })

                        Spacer(modifier = Modifier.height(20.dp))

                        if (isEditing) {
                            // Edit Form
                            EditPatientCard(
                                patient = patient,
                                isLoading = actionState is PatientActionState.Loading,
                                onCancel = { isEditing = false },
                                onSave = onUpdatePatient
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Tags Section
                        TagsCard(
                            patient = patient,
                            availableTags = availableTags,
                            onUpdateTags = onUpdateTags,
                            onCreateTag = onCreateTag
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Notes Section
                        NotesCard(
                            patient = patient,
                            newNoteText = newNoteText,
                            isAddingNote = actionState is PatientActionState.Loading,
                            onNoteTextChange = { newNoteText = it },
                            onAddNoteClick = { onAddNote(newNoteText) }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Activity Placeholder Card
                        ActivityPlaceholderCard()

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientHeaderCard(
    patient: PatientDetailDto,
    onEditClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patient.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = patient.phone ?: "No phone recorded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile")
                }
            }

            if (patient.email != null || patient.gender != null || patient.address != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                if (patient.email != null) {
                    DetailRow(icon = Icons.Default.Email, text = patient.email)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (patient.gender != null || patient.dateOfBirth != null) {
                    val info = listOfNotNull(patient.gender, patient.dateOfBirth?.let { "DOB: $it" }).joinToString(" • ")
                    DetailRow(icon = Icons.Default.CalendarToday, text = info)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (patient.address != null) {
                    DetailRow(icon = Icons.Default.Business, text = patient.address)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsCard(
    patient: PatientDetailDto,
    availableTags: List<TagDto>,
    onUpdateTags: (List<String>) -> Unit,
    onCreateTag: (String, (String) -> Unit) -> Unit
) {
    var showNewTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    if (showNewTagDialog) {
        AlertDialog(
            onDismissRequest = { showNewTagDialog = false },
            title = { Text("Create New Tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Tag Name (e.g. VIP, Referral)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            onCreateTag(newTagName) { newTagId ->
                                val currentTagIds = patient.tags.map { it.id }.toSet()
                                onUpdateTags((currentTagIds + newTagId).toList())
                            }
                            newTagName = ""
                            showNewTagDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewTagDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Patient Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            val currentTagIds = patient.tags.map { it.id }.toSet()

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableTags.forEach { tag ->
                    val isSelected = tag.id in currentTagIds
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newTagIds = if (isSelected) {
                                currentTagIds - tag.id
                            } else {
                                currentTagIds + tag.id
                            }
                            onUpdateTags(newTagIds.toList())
                        },
                        label = { Text(tag.name) }
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { showNewTagDialog = true },
                    label = { Text("+ New Tag") }
                )
            }
        }
    }
}

@Composable
private fun NotesCard(
    patient: PatientDetailDto,
    newNoteText: String,
    isAddingNote: Boolean,
    onNoteTextChange: (String) -> Unit,
    onAddNoteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Administrative Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // Add Note Input
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = onNoteTextChange,
                    placeholder = { Text("Add administrative note...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onAddNoteClick,
                    enabled = !isAddingNote && newNoteText.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Add Note", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (patient.notesList.isEmpty()) {
                Text(
                    text = "No notes recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                patient.notesList.forEach { note ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = note.content, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "By ${note.author?.fullName ?: "Staff"} • ${note.createdAt.take(10)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityPlaceholderCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Appointments and follow-ups history will appear here once recorded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditPatientCard(
    patient: PatientDetailDto,
    isLoading: Boolean,
    onCancel: () -> Unit,
    onSave: (fullName: String, phone: String?, email: String?, dob: String?, gender: String?, address: String?, notes: String?) -> Unit
) {
    var fullName by remember(patient) { mutableStateOf(patient.fullName) }
    var phone by remember(patient) { mutableStateOf(patient.phone ?: "") }
    var email by remember(patient) { mutableStateOf(patient.email ?: "") }
    var dob by remember(patient) { mutableStateOf(patient.dateOfBirth ?: "") }
    var gender by remember(patient) { mutableStateOf(patient.gender ?: "") }
    var address by remember(patient) { mutableStateOf(patient.address ?: "") }
    var notes by remember(patient) { mutableStateOf(patient.notes ?: "") }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Edit Patient Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Gender") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { onSave(fullName, phone, email, dob, gender, address, notes) },
                    enabled = !isLoading && fullName.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Save Changes")
                }
            }
        }
    }
}
