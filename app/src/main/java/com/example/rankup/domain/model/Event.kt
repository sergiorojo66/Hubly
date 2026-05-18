package com.example.rankup.domain.model

import com.google.firebase.firestore.PropertyName

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val date: Long = 0L,
    @get:PropertyName("private")
    @PropertyName("private")
    val isPrivate: Boolean = false,
    val password: String = "",
    @get:PropertyName("isFinished")
    @PropertyName("isFinished")
    val isFinished: Boolean = false,
    val imageUrl: String = "",
    val organizer: String = "",
    val participants: List<String> = emptyList(),
    val maxParticipants: Int? = null,
    val modules: List<String> = listOf("info"),
    val participantsIds: List<String> = emptyList()
)