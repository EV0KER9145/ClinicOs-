package com.clinicos.app.feature.ai.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMessageDialog(
    entityType: String,
    entityId: String,
    recipientName: String,
    messageState: MessageGenerationUiState,
    onDismiss: () -> Unit,
    onGenerate: (entityType: String, entityId: String, purpose: String, tone: String, additionalContext: String?) -> Unit
) {
    var purpose by remember { mutableStateOf("FOLLOW_UP") }
    var tone by remember { mutableStateOf("FRIENDLY") }
    var additionalContext by remember { mutableStateOf("") }
    var editableMessage by remember { mutableStateOf("") }
    var purposeExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val purposes = listOf(
        "FOLLOW_UP" to "Follow-up Outreach",
        "NO_SHOW_RECOVERY" to "No-Show Recovery",
        "LEAD_REENGAGEMENT" to "Lead Re-engagement",
        "APPOINTMENT_REMINDER" to "Appointment Reminder",
        "PATIENT_REACTIVATION" to "Patient Reactivation"
    )

    val tones = listOf("FRIENDLY", "PROFESSIONAL", "BRIEF")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Draft Message for $recipientName", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                // Purpose Selector
                ExposedDropdownMenuBox(
                    expanded = purposeExpanded,
                    onExpandedChange = { purposeExpanded = !purposeExpanded }
                ) {
                    OutlinedTextField(
                        value = purposes.firstOrNull { it.first == purpose }?.second ?: purpose,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Message Purpose") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = purposeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = purposeExpanded,
                        onDismissRequest = { purposeExpanded = false }
                    ) {
                        purposes.forEach { (pKey, pLabel) ->
                            DropdownMenuItem(
                                text = { Text(pLabel) },
                                onClick = {
                                    purpose = pKey
                                    purposeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tone Selector
                Text(text = "Tone", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tones.forEach { t ->
                        FilterChip(
                            selected = tone == t,
                            onClick = { tone = t },
                            label = { Text(t.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (messageState) {
                    is MessageGenerationUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    is MessageGenerationUiState.Success -> {
                        editableMessage = messageState.response.message
                        OutlinedTextField(
                            value = editableMessage,
                            onValueChange = { editableMessage = it },
                            label = { Text("Generated Message (Editable)") },
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AI Message", editableMessage)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Message copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Message")
                            }
                            IconButton(onClick = {
                                onGenerate(entityType, entityId, purpose, tone, additionalContext)
                            }) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate")
                            }
                        }
                    }

                    is MessageGenerationUiState.Error -> {
                        Text(text = messageState.message, color = MaterialTheme.colorScheme.error)
                    }

                    is MessageGenerationUiState.Idle -> {
                        OutlinedTextField(
                            value = additionalContext,
                            onValueChange = { additionalContext = it },
                            label = { Text("Extra Note / Context (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (messageState is MessageGenerationUiState.Idle) {
                Button(onClick = {
                    onGenerate(entityType, entityId, purpose, tone, additionalContext)
                }) {
                    Text("Generate Message")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Done")
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
