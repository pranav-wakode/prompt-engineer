package com.pranav.promptcraft.domain.repository

import com.pranav.promptcraft.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {
    suspend fun signInWithGoogle(): Result<User>
    suspend fun signInWithGoogleSuccess(uid: String, email: String?, displayName: String?): Result<User>
    suspend fun signInAsGuest(): Result<User>
    suspend fun signOut()
    fun getCurrentUser(): Flow<User?>
    fun isUserSignedIn(): Boolean
}
