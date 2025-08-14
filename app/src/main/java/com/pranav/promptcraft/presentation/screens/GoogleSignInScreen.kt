package com.pranav.promptcraft.presentation.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.pranav.promptcraft.R
import com.pranav.promptcraft.presentation.components.ErrorMessage
import com.pranav.promptcraft.presentation.components.LoadingIndicator
import com.pranav.promptcraft.presentation.viewmodels.AuthViewModel

/**
 * Complete Google Sign-In screen with proper Firebase integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSignInScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Navigate to home when signed in
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onNavigateToHome()
        }
    }

    // Configure Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    // Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                
                Log.d("GoogleSignIn", "Google Sign-In successful. Account: ${account.email}")
                Log.d("GoogleSignIn", "ID Token available: ${idToken != null}")
                
                if (idToken != null) {
                    // Create Firebase credential
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    
                    // Sign in to Firebase
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnSuccessListener { authResult ->
                            val user = authResult.user
                            Log.d("GoogleSignIn", "Firebase sign-in successful. User: ${user?.email}")
                            
                            // Update the repository with successful sign-in
                            viewModel.onGoogleSignInSuccess(
                                uid = user?.uid ?: "",
                                email = user?.email,
                                displayName = user?.displayName
                            )
                        }
                        .addOnFailureListener { exception ->
                            Log.e("GoogleSignIn", "Firebase sign-in failed", exception)
                            viewModel.onGoogleSignInError("Firebase authentication failed: ${exception.message}")
                        }
                } else {
                    Log.e("GoogleSignIn", "ID token is null")
                    viewModel.onGoogleSignInError("Failed to get ID token from Google")
                }
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Google Sign-In failed", e)
                viewModel.onGoogleSignInError("Google Sign-In failed: ${e.message}")
            }
        } else {
            Log.d("GoogleSignIn", "Google Sign-In cancelled or failed. Result code: ${result.resultCode}")
            viewModel.onGoogleSignInError("Sign-in was cancelled")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Welcome to PromptCraft",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo/Icon placeholder
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "PromptCraft Logo",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "PromptCraft",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enhance your prompts with AI-powered suggestions",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (uiState.isLoading) {
                LoadingIndicator(text = "Signing in...")
            } else {
                Column {
                    // Google Sign-In Button
                    Button(
                        onClick = {
                            Log.d("GoogleSignIn", "Starting Google Sign-In")
                            viewModel.startGoogleSignIn()
                            val signInIntent = googleSignInClient.signInIntent
                            signInLauncher.launch(signInIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person, // Replace with Google icon if available
                            contentDescription = "Google",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Guest Sign-In Button
                    OutlinedButton(
                        onClick = { viewModel.signInAsGuest() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            text = "Continue as Guest",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Error message
            uiState.error?.let { error ->
                ErrorMessage(
                    message = error,
                    onRetry = { viewModel.clearError() }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "By signing in, you agree to our Terms of Service and Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
