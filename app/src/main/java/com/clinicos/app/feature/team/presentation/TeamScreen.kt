package com.clinicos.app.feature.team.presentation

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.clinicos.app.core.network.dto.DoctorDto
import com.clinicos.app.core.network.dto.UserDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    doctorsState: DoctorsListState,
    staffState: StaffListState,
    actionState: ActionState,
    initialTab: Int = 0,
    onRefreshDoctors: () -> Unit,
    onRefreshStaff: () -> Unit,
    onCreateDoctor: (fullName: String, specialty: String?) -> Unit,
    onUpdateDoctor: (id: String, fullName: String, specialty: String?, isActive: Boolean) -> Unit,
    onCreateUser: (fullName: String, email: String, password: String, role: String, phone: String?) -> Unit,
    onToggleDoctorStatus: (id: String, currentStatus: Boolean) -> Unit,
    onToggleUserStatus: (id: String, currentStatus: Boolean) -> Unit,
    onClearActionState: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var showAddDoctorDialog by remember { mutableStateOf(false) }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var editingDoctor by remember { mutableStateOf<DoctorDto?>(null) }
    var deactivatingUser by remember { mutableStateOf<UserDto?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> {
                snackbarHostState.showSnackbar(actionState.message)
                showAddDoctorDialog = false
                showAddStaffDialog = false
                editingDoctor = null
                onClearActionState()
            }
            is ActionState.Error -> {
                snackbarHostState.showSnackbar(actionState.message)
                onClearActionState()
            }
            else -> {}
        }
    }

    // Confirmation Dialog for Staff Deactivation
    if (deactivatingUser != null) {
        val u = deactivatingUser!!
        AlertDialog(
            onDismissRequest = { deactivatingUser = null },
            title = { Text("Deactivate ${u.fullName}?") },
            text = { Text("${u.fullName} will no longer be able to log in or access ClinicOS. You can reactivate them later if needed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onToggleUserStatus(u.id, u.isActive)
                        deactivatingUser = null
                    }
                ) {
                    Text("Deactivate", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deactivatingUser = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add / Edit Doctor Dialog
    if (showAddDoctorDialog || editingDoctor != null) {
        DoctorFormDialog(
            doctor = editingDoctor,
            isLoading = actionState is ActionState.Loading,
            onDismiss = {
                showAddDoctorDialog = false
                editingDoctor = null
            },
            onSave = { fullName, specialty, isActive ->
                if (editingDoctor == null) {
                    onCreateDoctor(fullName, specialty)
                } else {
                    onUpdateDoctor(editingDoctor!!.id, fullName, specialty, isActive)
                }
            }
        )
    }

    // Add Staff Dialog
    if (showAddStaffDialog) {
        AddStaffDialog(
            isLoading = actionState is ActionState.Loading,
            onDismiss = { showAddStaffDialog = false },
            onSave = { fullName, email, password, role, phone ->
                onCreateUser(fullName, email, password, role, phone)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Team Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (selectedTab == 0) onRefreshDoctors() else onRefreshStaff()
                    }) {
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
                onClick = {
                    if (selectedTab == 0) showAddDoctorDialog = true else showAddStaffDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedTab == 0) "Add Doctor" else "Add Staff",
                        fontWeight = FontWeight.Bold
                    )
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
            // Segmented TabRow
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Doctors", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Staff Team", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == 0) {
                    DoctorsTabContent(
                        doctorsState = doctorsState,
                        onEditClick = { doc -> editingDoctor = doc },
                        onToggleStatus = { doc -> onToggleDoctorStatus(doc.id, doc.isActive) },
                        onAddClick = { showAddDoctorDialog = true }
                    )
                } else {
                    StaffTabContent(
                        staffState = staffState,
                        onDeactivateClick = { user -> deactivatingUser = user },
                        onReactivateClick = { user -> onToggleUserStatus(user.id, user.isActive) },
                        onAddClick = { showAddStaffDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun DoctorsTabContent(
    doctorsState: DoctorsListState,
    onEditClick: (DoctorDto) -> Unit,
    onToggleStatus: (DoctorDto) -> Unit,
    onAddClick: () -> Unit
) {
    when (doctorsState) {
        is DoctorsListState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is DoctorsListState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = doctorsState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is DoctorsListState.Success -> {
            val docs = doctorsState.doctors
            if (docs.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.MedicalServices,
                    title = "No doctors added yet",
                    subtitle = "Add doctor profiles to assign appointments and schedules.",
                    buttonText = "Add Doctor",
                    onButtonClick = onAddClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(docs, key = { it.id }) { doc ->
                        DoctorItemCard(
                            doctor = doc,
                            onEditClick = { onEditClick(doc) },
                            onToggleStatus = { onToggleStatus(doc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffTabContent(
    staffState: StaffListState,
    onDeactivateClick: (UserDto) -> Unit,
    onReactivateClick: (UserDto) -> Unit,
    onAddClick: () -> Unit
) {
    when (staffState) {
        is StaffListState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is StaffListState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = staffState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is StaffListState.Success -> {
            val users = staffState.users
            if (users.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.People,
                    title = "No staff members added yet",
                    subtitle = "Invite receptionists and admins to access ClinicOS.",
                    buttonText = "Add Staff",
                    onButtonClick = onAddClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(users, key = { it.id }) { user ->
                        StaffItemCard(
                            user = user,
                            onToggleStatus = {
                                if (user.isActive) onDeactivateClick(user) else onReactivateClick(user)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorItemCard(
    doctor: DoctorDto,
    onEditClick: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (doctor.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (doctor.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = if (doctor.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doctor.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (doctor.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = doctor.specialty ?: "General Practice",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(isActive = doctor.isActive)
            }

            IconButton(onClick = onEditClick) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}

@Composable
private fun StaffItemCard(
    user: UserDto,
    onToggleStatus: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (user.isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (user.isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (user.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RoleBadge(role = user.role)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(isActive = user.isActive)
            }

            if (user.role != "OWNER") {
                TextButton(onClick = onToggleStatus) {
                    Text(
                        text = if (user.isActive) "Deactivate" else "Reactivate",
                        color = if (user.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(isActive: Boolean) {
    Surface(
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = if (isActive) "Active" else "Inactive",
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun RoleBadge(role: String) {
    Surface(
        color = when (role) {
            "OWNER" -> MaterialTheme.colorScheme.primary
            "ADMIN" -> MaterialTheme.colorScheme.secondary
            "DOCTOR" -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = role,
            style = MaterialTheme.typography.labelSmall,
            color = when (role) {
                "OWNER" -> MaterialTheme.colorScheme.onPrimary
                "ADMIN" -> MaterialTheme.colorScheme.onSecondary
                "DOCTOR" -> MaterialTheme.colorScheme.onTertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onButtonClick, shape = RoundedCornerShape(12.dp)) {
            Text(buttonText)
        }
    }
}

@Composable
private fun DoctorFormDialog(
    doctor: DoctorDto?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (fullName: String, specialty: String?, isActive: Boolean) -> Unit
) {
    var fullName by remember(doctor) { mutableStateOf(doctor?.fullName ?: "") }
    var specialty by remember(doctor) { mutableStateOf(doctor?.specialty ?: "") }
    var isActive by remember(doctor) { mutableStateOf(doctor?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (doctor == null) "Add Doctor Profile" else "Edit Doctor Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Doctor Full Name *") },
                    leadingIcon = { Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("Specialty (e.g. Dermatology)") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(fullName, specialty, isActive) },
                enabled = !isLoading && fullName.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Doctor")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStaffDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (fullName: String, email: String, password: String, role: String, phone: String?) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STAFF") }
    var roleExpanded by remember { mutableStateOf(false) }

    val roles = listOf("STAFF", "ADMIN", "DOCTOR")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Staff Team Member", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address *") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min 8 chars) *") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Role Dropdown
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assigned Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        roles.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    role = r
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Optional)") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(fullName, email, password, role, phone) },
                enabled = !isLoading && fullName.isNotBlank() && email.isNotBlank() && password.length >= 8
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Add Member")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
