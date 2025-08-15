package com.pranav.promptcraft.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.pranav.promptcraft.data.database.PromptDao
import com.pranav.promptcraft.domain.model.Prompt
import com.pranav.promptcraft.domain.repository.PromptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PromptRepository that handles both local database and AI operations
 */
@Singleton
class PromptRepositoryImpl @Inject constructor(
    private val promptDao: PromptDao,
    private val generativeModel: GenerativeModel
) : PromptRepository {

    override suspend fun insertPrompt(prompt: Prompt): Long {
        return promptDao.insertPrompt(prompt)
    }

    override suspend fun getAllPrompts(): Flow<List<Prompt>> {
        return promptDao.getAllPrompts()
    }

    override suspend fun getPromptById(id: Long): Prompt? {
        return promptDao.getPromptById(id)
    }

    override suspend fun deletePrompt(prompt: Prompt) {
        promptDao.deletePrompt(prompt)
    }

    override suspend fun enhancePrompt(originalPrompt: String, selectedTypes: List<String>): String {
        val typesText = if (selectedTypes.contains("Auto")) {
            "Auto"
        } else {
            selectedTypes.joinToString(", ")
        }

        val metaPrompt = buildMetaPrompt(originalPrompt, typesText)
        
        return try {
            val response = generativeModel.generateContent(metaPrompt)
            response.text ?: "Error: Unable to generate enhanced prompt"
        } catch (e: Exception) {
            when {
                e.message?.contains("models/gemini-pro is not found") == true -> 
                    "Model not found. Please check the API configuration."
                e.message?.contains("API_KEY_INVALID") == true -> 
                    "Invalid API key. Please check your Gemini API key."
                e.message?.contains("RATE_LIMIT_EXCEEDED") == true -> 
                    "Rate limit exceeded. Please try again later."
                else -> "Network error: ${e.localizedMessage ?: "Unknown error occurred"}"
            }
        }
    }
    
    private fun buildMetaPrompt(userPrompt: String, selectedTypes: String): String {
        return """
            You are an expert prompt engineer. Your task is to take a user's prompt and enhance it based on a specified technique.
            - **DO NOT** ask clarifying questions.
            - **ALWAYS** respond with the enhanced prompt, starting with the prefix "Enhanced Prompt: ".
            - Make the new prompt detailed, specific, and well-structured.
            - Apply the following technique: $selectedTypes
            - Include context, format requirements, and expected output style where appropriate.
            - Make it 2-3x longer than the original with clear, actionable instructions.
            
            User's original prompt: "$userPrompt"
            
            Enhanced Prompt:
        """.trimIndent()
    }
}
