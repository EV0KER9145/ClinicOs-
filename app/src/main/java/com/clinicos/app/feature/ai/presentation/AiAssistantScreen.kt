package com.clinicos.app.feature.ai.presentation

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clinicos.app.core.network.dto.FollowUpRecommendationDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    recommendationsState: RecommendationsUiState,
    analyticsState: AnalyticsQueryUiState,
    onRefreshRecommendations: () -> Unit,
    onQueryAnalytics: (question: String) -> Unit,
    onNavigateToEntity: (entityType: String, entityId: String) -> Unit,
    onOpenMessageDialog: (entityType: String, entityId: String, recipientName: String) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Recommendations, 1 = Ask ClinicOS
    var questionText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val quickQuestions = listOf(
        "How many follow-ups are overdue?",
        "What is our lead conversion rate?",
        "How many appointments were missed?",
        "How many new patients registered this week?"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Assistant Workspace", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshRecommendations) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Action Suggestions", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ask ClinicOS", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == 0) {
                    // Recommendations Tab
                    when (recommendationsState) {
                        is RecommendationsUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        is RecommendationsUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = recommendationsState.message, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        is RecommendationsUiState.Success -> {
                            val recs = recommendationsState.data.recommendations
                            if (recs.isEmpty()) {
                                EmptyAiStateView(title = "No urgent recommendations", subtitle = "All follow-up tasks and enquiries are handled smoothly.")
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(recs) { rec ->
                                        RecommendationCard(
                                            recommendation = rec,
                                            onViewRecord = { onNavigateToEntity(rec.entityType, rec.entityId) },
                                            onDraftMessage = { onOpenMessageDialog(rec.entityType, rec.entityId, rec.entityName) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Natural Language Analytics Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Quick Question Chips
                        Text(text = "SUGGESTED QUESTIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickQuestions.forEach { q ->
                                SuggestionChip(
                                    onClick = {
                                        questionText = q
                                        onQueryAnalytics(q)
                                    },
                                    label = { Text(q) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search Input
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = questionText,
                                onValueChange = { questionText = it },
                                placeholder = { Text("Ask about appointments, leads, follow-ups...") },
                                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onQueryAnalytics(questionText) },
                                enabled = questionText.isNotBlank() && analyticsState !is AnalyticsQueryUiState.Loading
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Ask", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        when (analyticsState) {
                            is AnalyticsQueryUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Analyzing clinic metrics...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            is AnalyticsQueryUiState.Error -> {
                                Text(text = analyticsState.message, color = MaterialTheme.colorScheme.error)
                            }

                            is AnalyticsQueryUiState.Success -> {
                                val res = analyticsState.response
                                AnalyticsResultCard(response = res)
                            }

                            is AnalyticsQueryUiState.Idle -> {
                                EmptyAiStateView(title = "Ask ClinicOS Anything", subtitle = "Inquire about missed appointments, lead conversion rates, or overdue tasks in plain English.")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: FollowUpRecommendationDto,
    onViewRecord: () -> Unit,
    onDraftMessage: () -> Unit
) {
    val (priorityBg, priorityText) = when (recommendation.priority) {
        "URGENT" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        "HIGH" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = recommendation.entityName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(color = priorityBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = recommendation.priority,
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = recommendation.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "Action: ${recommendation.suggestedAction}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onViewRecord, modifier = Modifier.weight(1f)) {
                    Text("View Record")
                }
                Button(onClick = onDraftMessage, modifier = Modifier.weight(1f)) {
                    Text("Draft Message")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnalyticsResultCard(
    response: com.clinicos.app.core.network.dto.AnalyticsQueryResponse
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (response.intent == "CLINICAL_PROHIBITED") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (response.intent == "CLINICAL_PROHIBITED") Icons.Default.Warning else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (response.intent == "CLINICAL_PROHIBITED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = response.answer, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }

            if (response.suggestedActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "SUGGESTED NEXT STEPS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    response.suggestedActions.forEach { act ->
                        SuggestionChip(onClick = {}, label = { Text(act) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAiStateView(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
