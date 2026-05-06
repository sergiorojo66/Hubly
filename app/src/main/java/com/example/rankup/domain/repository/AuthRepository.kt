package com.example.rankup.domain.repository

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    val currentUser: FirebaseUser?
    suspend fun loginWithEmail(email: String, pass: String): Result<AuthResult>
    suspend fun signUpWithEmail(email: String, pass: String): Result<AuthResult>
    fun logout()
    suspend fun loginWithGoogle(idToken: String): Result<AuthResult>
}