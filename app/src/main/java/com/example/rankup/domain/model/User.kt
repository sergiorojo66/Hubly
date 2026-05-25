package com.example.rankup.domain.model

data class User(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val bio: String = "¡Hola! Soy nuevo en Hubly.",
    val initials: String = "U",
    val rating: Double = 0.0,
    val fcmToken: String? = null
)