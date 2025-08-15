package com.pranav.promptcraft.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.promptcraft.domain.model.Prompt
import com.pranav.promptcraft.domain.model.PromptType
import com.pranav.promptcraft.domain.repository.PromptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for the main prompt enhancement screen
 */
@HiltViewModel
class PromptEnhancerViewModel @Inject constructor(
    private val promptRepository: PromptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromptEnhancerUiState())
    val uiState: StateFlow<PromptEnhancerUiState> = _uiState.asStateFlow()

    fun updateInputPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(inputPrompt = prompt)
    }

    fun selectPromptType(type: PromptType) {
        val currentState = _uiState.value
        val newSelectedTypes = if (type == PromptType.AUTO) {
            // If AUTO is selected, deselect all others
            setOf(PromptType.AUTO)
        } else {
            // If other type is selected, deselect AUTO
            val updatedTypes = currentState.selectedPromptTypes.toMutableSet()
            updatedTypes.remove(PromptType.AUTO)
            
            if (updatedTypes.contains(type)) {
                updatedTypes.remove(type)
            } else {
                updatedTypes.add(type)
            }
            
            // If no types selected, default back to AUTO
            if (updatedTypes.isEmpty()) {
                setOf(PromptType.AUTO)
            } else {
                updatedTypes
            }
        }
        
        _uiState.value = currentState.copy(selectedPromptTypes = newSelectedTypes)
    }

    fun enhancePrompt() {
        val currentState = _uiState.value
        if (currentState.inputPrompt.isBlank()) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            try {
                val selectedTypeNames = currentState.selectedPromptTypes.map { it.displayName }
                val enhancedText = promptRepository.enhancePrompt(
                    currentState.inputPrompt,
                    selectedTypeNames
                )

                // Save to history and navigate to result
                val prompt = Prompt(
                    originalPrompt = currentState.inputPrompt,
                    enhancedPrompt = enhancedText,
                    promptTypes = selectedTypeNames
                )
                promptRepository.insertPrompt(prompt)

                _uiState.value = currentState.copy(
                    isLoading = false,
                    enhancedPrompt = enhancedText,
                    showResult = true
                )
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is SocketTimeoutException -> {
                        "Request timed out. Please check your connection and try again."
                    }
                    is IOException -> {
                        "Network error. Please check your internet connection."
                    }
                    else -> {
                        // Handle 503 Service Unavailable, MissingFieldException and other API errors
                        val message = e.message ?: e.localizedMessage ?: "Unknown error"
                        when {
                            message.contains("503") || message.contains("overloaded") || message.contains("UNAVAILABLE") || 
                            message.contains("MissingFieldException") || e.javaClass.simpleName.contains("MissingField") -> {
                                "The service is busy, please try again later."
                            }
                            message.contains("401") || message.contains("API_KEY_INVALID") -> {
                                "Invalid API key. Please check your configuration."
                            }
                            message.contains("429") || message.contains("RATE_LIMIT_EXCEEDED") -> {
                                "Too many requests. Please wait a moment and try again."
                            }
                            message.contains("400") || message.contains("INVALID_ARGUMENT") -> {
                                "Invalid request. Please check your prompt and try again."
                            }
                            else -> {
                                "Something went wrong. Please try again."
                            }
                        }
                    }
                }
                
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }


    fun clearResult() {
        _uiState.value = _uiState.value.copy(
            showResult = false,
            enhancedPrompt = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearPrompt() {
        _uiState.value = PromptEnhancerUiState()
    }
    
    fun editPrompt(originalPrompt: String) {
        _uiState.value = PromptEnhancerUiState(
            inputPrompt = originalPrompt
        )
    }

}

data class PromptEnhancerUiState(
    val inputPrompt: String = "",
    val selectedPromptTypes: Set<PromptType> = setOf(PromptType.AUTO),
    val isLoading: Boolean = false,
    val enhancedPrompt: String? = null,
    val showResult: Boolean = false,
    val error: String? = null
)
