package com.pranav.promptcraft.di

import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
/**
 * Hilt module for providing network and API dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        // Using your actual Gemini API key - make sure you've added it to NetworkModule.kt
        return GenerativeModel(
            modelName = "gemini-1.5-flash", // Updated to current model
            apiKey = "AIzaSyAdseX58GVtZl0axIn7PbR6PBMWMBX6k48" // Replace with your actual API key
        )
    }
}
