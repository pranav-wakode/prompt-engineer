package com.pranav.promptcraft.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.pranav.promptcraft.domain.model.User
import com.pranav.promptcraft.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

/**
 * Implementation of AuthRepository using Firebase Authentication
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)

    init {
        // Initialize current user state
        firebaseAuth.currentUser?.let { firebaseUser ->
            _currentUser.value = User(
                id = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                isGuest = false
            )
        }
    }

    override suspend fun signInWithGoogle(): Result<User> {
        return try {
            // Check if user is already signed in to Firebase
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val user = User(
                    id = currentUser.uid,
                    email = currentUser.email,
                    displayName = currentUser.displayName ?: "User",
                    isGuest = false
                )
                _currentUser.value = user
                return Result.success(user)
            }
            
            // This method is now mainly for checking existing sign-in
            // The actual sign-in happens in the Compose screen with proper Google Sign-In flow
            Result.failure(Exception("No user signed in"))
        } catch (e: Exception) {
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))
        }
    }
    
    override suspend fun signInWithGoogleSuccess(uid: String, email: String?, displayName: String?): Result<User> {
        return try {
            val user = User(
                id = uid,
                email = email,
                displayName = displayName ?: "Google User",
                isGuest = false
            )
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to process Google Sign-In: ${e.message}"))
        }
    }

    override suspend fun signInAsGuest(): Result<User> {
        return try {
            val guestUser = User(
                id = "guest_${UUID.randomUUID()}",
                email = null,
                displayName = "Guest User",
                isGuest = true
            )
            _currentUser.value = guestUser
            Result.success(guestUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            _currentUser.value = null
        } catch (e: Exception) {
            // Handle sign out error
        }
    }

    override fun getCurrentUser(): Flow<User?> {
        return _currentUser.asStateFlow()
    }

    override fun isUserSignedIn(): Boolean {
        return _currentUser.value != null
    }
}
