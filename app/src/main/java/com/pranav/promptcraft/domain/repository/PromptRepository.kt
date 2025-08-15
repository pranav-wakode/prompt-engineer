package com.pranav.promptcraft.domain.repository

import com.pranav.promptcraft.domain.model.Prompt
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for prompt operations
 */
interface PromptRepository {
    suspend fun insertPrompt(prompt: Prompt): Long
    suspend fun getAllPrompts(): Flow<List<Prompt>>
    suspend fun getPromptById(id: Long): Prompt?
    suspend fun deletePrompt(prompt: Prompt)
    suspend fun enhancePrompt(originalPrompt: String, selectedTypes: List<String>): String
}
