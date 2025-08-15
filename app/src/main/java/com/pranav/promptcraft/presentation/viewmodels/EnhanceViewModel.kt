package com.pranav.promptcraft.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.promptcraft.domain.repository.PromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EnhanceUiState {
    object Idle : EnhanceUiState
    object Loading : EnhanceUiState
    data class Success(
        val response: String
    ) : EnhanceUiState
    data class Error(val message: String) : EnhanceUiState
}

@HiltViewModel
class EnhanceViewModel @Inject constructor(
    private val promptRepository: PromptRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<EnhanceUiState>(EnhanceUiState.Idle)
    val uiState: StateFlow<EnhanceUiState> = _uiState.asStateFlow()
    
    fun startEnhancement(prompt: String, types: List<String>) {
        _uiState.value = EnhanceUiState.Loading
        
        viewModelScope.launch {
            try {
                val response = promptRepository.enhancePrompt(prompt, types)
                _uiState.value = EnhanceUiState.Success(response)
            } catch (e: Exception) {
                _uiState.value = EnhanceUiState.Error(
                    "Failed to enhance prompt: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }
    
    fun resetEnhancement() {
        _uiState.value = EnhanceUiState.Idle
    }
    
    fun getCurrentStep(): String {
        return when (val state = _uiState.value) {
            is EnhanceUiState.Success -> "Complete"
            is EnhanceUiState.Loading -> "Processing..."
            is EnhanceUiState.Error -> "Error occurred"
            EnhanceUiState.Idle -> "Ready to start"
        }
    }
}
