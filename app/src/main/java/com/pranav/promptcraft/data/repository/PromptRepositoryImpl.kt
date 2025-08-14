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
            val errorMessage = when {
                e.message?.contains("models/gemini-pro is not found") == true -> 
                    "Model not found. Please check the API configuration."
                e.message?.contains("API_KEY_INVALID") == true -> 
                    "Invalid API key. Please check your Gemini API key."
                e.message?.contains("RATE_LIMIT_EXCEEDED") == true -> 
                    "Rate limit exceeded. Please try again later."
                else -> "Network error: ${e.localizedMessage ?: "Unknown error occurred"}"
            }
            errorMessage
        }
    }

    private fun buildMetaPrompt(userPrompt: String, selectedTypes: String): String {
        return """
            You are an expert prompt engineer. Analyze the following user prompt and either:
            
            1. If the prompt is too vague (lacks context, purpose, or specific details), respond with ONLY a single, brief clarifying question (max 20 words).
            2. If the prompt has enough detail, create a comprehensive enhanced version following these rules:
               - Start with "Enhanced Prompt: "
               - Make it detailed, specific, and well-structured
               - Apply the technique: $selectedTypes
               - Include context, format requirements, and expected output style
               - Make it 2-3x longer than the original with clear instructions
            
            User's original prompt: "$userPrompt"
            
            Respond with either a clarifying question OR an enhanced prompt (starting with "Enhanced Prompt: "):
        """.trimIndent()
    }
}
