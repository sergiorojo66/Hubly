package com.example.rankup.domain.model

data class AuthState(
    val name: String = "",
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

sealed class AuthEvent {
    data class NameChanged(val name: String) : AuthEvent()
    data class EmailChanged(val email: String) : AuthEvent()
    data class PasswordChanged(val password: String) : AuthEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : AuthEvent()
    object RegisterClicked : AuthEvent()
    data class GoogleSignInClicked(val context: android.content.Context) : AuthEvent()
    object LoginClicked : AuthEvent()
    object Logout : AuthEvent()
    object ForgotPasswordClicked : AuthEvent()
}