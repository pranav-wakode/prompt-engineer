package com.pranav.promptcraft.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.promptcraft.domain.model.Prompt
import com.pranav.promptcraft.domain.repository.PromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the history screen
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val promptRepository: PromptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                promptRepository.getAllPrompts().collect { prompts ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        prompts = prompts,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to load history"
                )
            }
        }
    }

    fun deletePrompt(prompt: Prompt) {
        viewModelScope.launch {
            try {
                promptRepository.deletePrompt(prompt)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.localizedMessage ?: "Failed to delete prompt"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class HistoryUiState(
    val isLoading: Boolean = false,
    val prompts: List<Prompt> = emptyList(),
    val error: String? = null
)
