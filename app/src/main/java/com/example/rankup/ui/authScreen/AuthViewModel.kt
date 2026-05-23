package com.example.rankup.ui.authScreen

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rankup.domain.model.AuthEvent
import com.example.rankup.domain.model.AuthState
import com.example.rankup.domain.model.User
import com.example.rankup.domain.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    var state by mutableStateOf(AuthState())
        private set

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.NameChanged -> state = state.copy(name = event.name)
            is AuthEvent.EmailChanged -> {
                state = state.copy(email = event.email, emailError = null)
            }
            is AuthEvent.PasswordChanged -> {
                state = state.copy(password = event.password, passwordError = null)
            }
            is AuthEvent.ConfirmPasswordChanged -> {
                state = state.copy(confirmPassword = event.confirmPassword, passwordError = null)
            }
            is AuthEvent.RegisterClicked -> performSignUp()
            is AuthEvent.GoogleSignInClicked -> performGoogleSignIn(event.context)
            AuthEvent.LoginClicked -> { validateAndLogin() }
            is AuthEvent.Logout -> performLogout()
            else -> {}
        }
    }

    private fun performSignUp() {
        state = state.copy(
            error = null,
            emailError = null,
            passwordError = null
        )
        val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()
        val isPasswordShort = state.password.length < 6
        val passwordsMatch = state.password == state.confirmPassword

        if (!isEmailValid || isPasswordShort || !passwordsMatch) {
            state = state.copy(
                emailError = if (!isEmailValid) "Formato de correo inválido" else null,
                passwordError = when {
                    isPasswordShort -> "Mínimo 6 caracteres"
                    !passwordsMatch -> "Las contraseñas no coinciden"
                    else -> null
                }
            )
            return
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true)
            val result = authRepository.signUpWithEmail(state.email, state.password)

            result.onSuccess {
                ensureUserProfileExists()
                state = state.copy(isLoading = false, isSuccess = true)
            }.onFailure { e ->
                state = state.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Error al crear la cuenta"
                )
            }
        }
    }

    private fun validateAndLogin() {
        val emailResult = state.email.isBlank()
        val passwordResult = state.password.isBlank()

        if (emailResult || passwordResult) {
            state = state.copy(
                emailError = if (emailResult) "El correo no puede estar vacío" else null,
                passwordError = if (passwordResult) "La contraseña no puede estar vacía" else null
            )
            return
        }
        performLogin()
    }

    private fun performLogin() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            val result = authRepository.loginWithEmail(state.email, state.password)

            result.onSuccess {
                ensureUserProfileExists()
                state = state.copy(isLoading = false, isSuccess = true)
            }.onFailure { e ->
                state = state.copy(isLoading = false, error = "Credenciales incorrectas")
            }
        }
    }

    private fun performGoogleSignIn(context: Context) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("301672827415-qcrg3hlki9lv6350227vcs1jm8fmk1ma.apps.googleusercontent.com")
                .build()

            val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

            try {
                val result = credentialManager.getCredential(context, request)
                val googleIdCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val authResult = authRepository.loginWithGoogle(googleIdCredential.idToken)

                authResult.onSuccess {
                    ensureUserProfileExists()
                    state = state.copy(isLoading = false, isSuccess = true)
                }.onFailure { e ->
                    state = state.copy(isLoading = false, error = e.localizedMessage)
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = "Cancelado o error en Google")
            }
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            state = state.copy(isSuccess = false)
            authRepository.logout()
            state = AuthState()
        }
    }

    // 1. Asegúrate de importar Firebase Messaging arriba del archivo si no lo tienes:
// import com.google.firebase.messaging.FirebaseMessaging

    suspend fun ensureUserProfileExists() {
        val currentUser = firebaseAuth.currentUser ?: return
        val uid = currentUser.uid
        val email = currentUser.email ?: ""

        val sanitizedUsername = "@" + email.substringBefore("@").lowercase().replace(".", "_")

        val defaultName = currentUser.displayName
            ?: if (state.name.isNotBlank()) state.name
            else "Usuario ${uid.take(5)}"

        try {
            // Recuperamos el token de FCM de manera asíncrona pero secuencial
            val fcmToken = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) {
                println("Error al obtener FCM Token: ${e.message}")
                null
            }

            val userRef = firestore.collection("users").document(uid)
            val doc = userRef.get().await()

            if (!doc.exists()) {
                val newUser = User(
                    id = uid,
                    username = sanitizedUsername,
                    displayName = defaultName,
                    email = email,
                    initials = defaultName.take(1).uppercase(),
                    bio = "¡Hola! Soy nuevo en Hubly.",
                    rating = 0.0,
                    fcmToken = fcmToken
                )
                userRef.set(newUser).await()
            } else {
                // Si el usuario ya existe (Sergio logueándose), actualizamos el token por si acaso
                if (fcmToken != null) {
                    userRef.update("fcmToken", fcmToken).await()
                }
            }
        } catch (e: Exception) {
            println("Error al crear perfil o registrar token: ${e.message}")
        }
    }
}