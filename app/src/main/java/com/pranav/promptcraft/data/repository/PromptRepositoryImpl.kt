package com.pranav.promptcraft.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.pranav.promptcraft.data.database.PromptDao
import com.pranav.promptcraft.domain.model.Prompt
import com.pranav.promptcraft.domain.repository.PromptRepository
import com.pranav.promptcraft.presentation.viewmodels.PromptLength
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

    override suspend fun enhancePrompt(originalPrompt: String, selectedTypes: List<String>, promptLength: PromptLength): String {
        val typesText = if (selectedTypes.contains("Auto")) {
            "Auto"
        } else {
            selectedTypes.joinToString(", ")
        }

        val metaPrompt = buildMetaPrompt(originalPrompt, typesText, promptLength)
        
        return try {
            val response = generativeModel.generateContent(metaPrompt)
            response.text ?: "Error: Unable to generate enhanced prompt"
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error"
            val errorDetails = "Error: $errorMessage, Type: ${e.javaClass.simpleName}"
            
            when {
                errorMessage.contains("models/gemini-pro is not found") || 
                errorMessage.contains("is not found") || 
                errorMessage.contains("NOT_FOUND") -> 
                    "Model not found. Error details: $errorDetails"
                errorMessage.contains("API_KEY_INVALID") || 
                errorMessage.contains("INVALID_ARGUMENT") || 
                errorMessage.contains("Invalid API key") -> 
                    "Invalid API key. Error details: $errorDetails"
                errorMessage.contains("RATE_LIMIT_EXCEEDED") || 
                errorMessage.contains("RESOURCE_EXHAUSTED") -> 
                    "Rate limit exceeded. Please try again later."
                else -> "Network error: Unexpected Response: $errorDetails"
            }
        }
    }
    
    private fun buildMetaPrompt(userPrompt: String, selectedTypes: String, promptLength: PromptLength): String {
        val lengthInstruction = when (promptLength) {
            PromptLength.SHORT -> "A 'Short' prompt should be 2-3 concise sentences."
            PromptLength.MEDIUM -> "A 'Medium' prompt should be a detailed paragraph (around 80-120 words)."
            PromptLength.LONG -> "A 'Long' prompt should be a comprehensive, multi-paragraph prompt (over 150 words)."
        }
        
        return """
            You are an expert prompt engineer. Your task is to take a user's prompt and enhance it.
            - **DO NOT** ask clarifying questions.
            - **ALWAYS** respond with the enhanced prompt.
            - **Generate a response with a "${promptLength.displayName}" length.** $lengthInstruction
            - Make the new prompt detailed, specific, and well-structured.
            - Apply the following technique: $selectedTypes
            - Include context, format requirements, and expected output style where appropriate.
            
            User's original prompt: "$userPrompt"
            
            Enhanced Prompt:
        """.trimIndent()
    }
}
