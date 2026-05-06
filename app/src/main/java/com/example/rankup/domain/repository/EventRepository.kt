package com.example.rankup.domain.repository

import com.example.rankup.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEvents(): Flow<List<Event>>
    fun getEventById(eventId: String): Flow<Event?>
    suspend fun createEvent(event: Event): Result<Unit>
    suspend fun joinEvent(eventId: String, userId: String): Result<Unit>
}