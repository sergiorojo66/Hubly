package com.example.rankup.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    @ServerTimestamp val timestamp: Date? = null
)