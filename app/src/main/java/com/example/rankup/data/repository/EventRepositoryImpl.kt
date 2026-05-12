package com.example.rankup.data.repository

import android.util.Log
import com.example.rankup.domain.model.ChatMessage
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.repository.EventRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : EventRepository {

    private val eventCollection = firestore.collection("events")

    override fun getEvents(): Flow<List<Event>> = callbackFlow {
        val subscription = eventCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error de permisos (esperado en logout): ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val events = snapshot.toObjects(Event::class.java)
                trySend(events)
            }
        }
        awaitClose { subscription.remove() }
    }

    override fun getEventById(eventId: String): Flow<Event?> = callbackFlow {
        val docRef = firestore.collection("events").document(eventId)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val event = snapshot?.toObject(Event::class.java)
            trySend(event)
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun createEvent(event: Event): Result<Unit> {
        return try {
            val eventsCollection = firestore.collection("events")
            val newDocumentRef = eventsCollection.document()
            val eventWithId = event.copy(id = newDocumentRef.id)

            newDocumentRef.set(eventWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            val eventRef = firestore.collection("events").document(eventId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val event = snapshot.toObject(Event::class.java)
                val participants = event?.participantsIds ?: emptyList()
                val max = event?.maxParticipants ?: Int.MAX_VALUE

                if (participants.size >= max) {
                    throw Exception("EVENT_FULL")
                }
                if (participants.contains(userId)) {
                    throw Exception("ALREADY_JOINED")
                }

                transaction.update(eventRef, "participantsIds", FieldValue.arrayUnion(userId))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getChatMessages(eventId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = firestore.collection("events")
            .document(eventId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val msgs = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                trySend(msgs) // Enviamos los mensajes al Flow
            }
        awaitClose { subscription.remove() } // Limpiamos la conexión al cerrar
    }

    override suspend fun sendMessage(eventId: String, message: ChatMessage) {
        firestore.collection("events")
            .document(eventId)
            .collection("messages")
            .add(message).await()
    }
}