package com.pranav.promptcraft.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pranav.promptcraft.domain.model.PromptType
import com.pranav.promptcraft.presentation.components.ErrorMessage
import com.pranav.promptcraft.presentation.components.FollowUpQuestionDialog
import com.pranav.promptcraft.presentation.components.LoadingIndicator
import com.pranav.promptcraft.presentation.viewmodels.PromptEnhancerViewModel

/**
 * Main home screen for prompt enhancement
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: PromptEnhancerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Handle follow-up question dialog
    if (uiState.showFollowUpDialog && uiState.followUpQuestion != null) {
        val followUpQuestion = uiState.followUpQuestion ?: ""
        FollowUpQuestionDialog(
            question = followUpQuestion,
            onAnswer = { answer ->
                viewModel.answerFollowUpQuestion(answer)
            },
            onDismiss = {
                viewModel.dismissFollowUpDialog()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "PromptCraft",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.showResult && uiState.enhancedPrompt != null) {
                // Show result
                val enhancedPrompt = uiState.enhancedPrompt ?: ""
                ResultContent(
                    originalPrompt = uiState.inputPrompt,
                    enhancedPrompt = enhancedPrompt,
                    onCopy = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onEdit = { /* TODO: Implement edit functionality */ },
                    onBackToHome = {
                        viewModel.clearResult()
                    }
                )
            } else {
                // Show main prompt enhancement UI
                PromptEnhancementContent(
                    uiState = uiState,
                    onInputChange = viewModel::updateInputPrompt,
                    onTypeSelect = viewModel::selectPromptType,
                    onEnhance = viewModel::enhancePrompt,
                    onClearError = viewModel::clearError
                )
            }
        }
    }
}

@Composable
private fun PromptEnhancementContent(
    uiState: com.pranav.promptcraft.presentation.viewmodels.PromptEnhancerUiState,
    onInputChange: (String) -> Unit,
    onTypeSelect: (PromptType) -> Unit,
    onEnhance: () -> Unit,
    onClearError: () -> Unit
) {
    // Input field
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Enter your prompt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = uiState.inputPrompt,
                onValueChange = onInputChange,
                placeholder = { Text("Type your initial prompt here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                minLines = 4
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Prompt type selection
    Text(
        text = "Prompt Type",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(PromptType.values()) { type ->
            FilterChip(
                onClick = { onTypeSelect(type) },
                label = { Text(type.displayName) },
                selected = uiState.selectedPromptTypes.contains(type),
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Enhance button
    if (uiState.isLoading) {
        LoadingIndicator(text = "Enhancing your prompt...")
    } else {
        Button(
            onClick = onEnhance,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = uiState.inputPrompt.isNotBlank(),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Enhance Prompt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Error message
    uiState.error?.let { error ->
        ErrorMessage(
            message = error,
            onRetry = onClearError
        )
    }
}

@Composable
private fun ResultContent(
    originalPrompt: String,
    enhancedPrompt: String,
    onCopy: (String) -> Unit,
    onEdit: () -> Unit,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enhanced Prompt",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(onClick = onBackToHome) {
                Text("New Prompt")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Original prompt
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Original Prompt",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = originalPrompt,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Enhanced prompt
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Enhanced Prompt",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = enhancedPrompt,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onCopy(enhancedPrompt) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Copy")
            }
            
            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Edit")
            }
        }
    }
}
