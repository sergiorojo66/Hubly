package com.example.rankup.domain.repository

import com.example.rankup.domain.model.ChatMessage
import com.example.rankup.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEvents(): Flow<List<Event>>
    fun getEventById(eventId: String): Flow<Event?>
    suspend fun createEvent(event: Event): Result<String> // Cambia Unit por String
    suspend fun joinEvent(eventId: String, userId: String, userName: String): Result<Unit>
    fun getChatMessages(eventId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(eventId: String, message: ChatMessage)
    suspend fun leaveEvent(eventId: String, userId: String): Result<Unit>
    suspend fun deleteEvent(eventId: String): Result<Unit>
    suspend fun finishEvent(eventId: String): Result<Unit>
}