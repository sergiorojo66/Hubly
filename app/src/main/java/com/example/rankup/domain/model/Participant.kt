package com.example.rankup.domain.model

import com.google.firebase.firestore.DocumentId

data class Participant(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val points: Int = 0 ,
    val matchesPlayed: Int = 0
)