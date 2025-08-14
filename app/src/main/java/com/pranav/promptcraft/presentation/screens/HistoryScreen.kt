package com.pranav.promptcraft.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pranav.promptcraft.domain.model.Prompt
import com.pranav.promptcraft.presentation.components.EmptyState
import com.pranav.promptcraft.presentation.components.ErrorMessage
import com.pranav.promptcraft.presentation.components.LoadingIndicator
import com.pranav.promptcraft.presentation.viewmodels.HistoryViewModel
import java.time.format.DateTimeFormatter

/**
 * History screen displaying saved prompts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "History",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        text = "Loading history..."
                    )
                }
                
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    ) {
                        ErrorMessage(
                            message = uiState.error ?: "Unknown error occurred",
                            onRetry = { viewModel.clearError() }
                        )
                    }
                }
                
                uiState.prompts.isEmpty() -> {
                    EmptyState(
                        title = "No History Yet",
                        description = "Start enhancing prompts to see them here",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.prompts,
                            key = { it.id }
                        ) { prompt ->
                            PromptHistoryItem(
                                prompt = prompt,
                                onCopy = { text ->
                                    clipboardManager.setText(AnnotatedString(text))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = {
                                    viewModel.deletePrompt(prompt)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptHistoryItem(
    prompt: Prompt,
    onCopy: (String) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm")

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with date and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prompt.createdAt.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row {
                    // Prompt types chips
                    prompt.promptTypes.take(2).forEach { type ->
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    if (prompt.promptTypes.size > 2) {
                        Text(
                            text = "+${prompt.promptTypes.size - 2}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Original prompt preview
            Text(
                text = "Original:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = prompt.originalPrompt,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(12.dp))

                // Enhanced prompt
                Text(
                    text = "Enhanced:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                SelectionContainer {
                    Text(
                        text = prompt.enhancedPrompt,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCopy(prompt.originalPrompt) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Original")
                    }
                    
                    Button(
                        onClick = { onCopy(prompt.enhancedPrompt) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Enhanced")
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
