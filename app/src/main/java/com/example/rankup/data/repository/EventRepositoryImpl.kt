package com.example.rankup.data.repository

import com.example.rankup.util.AppLogger
import com.example.rankup.domain.model.ChatMessage
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.model.RankingUser
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
                AppLogger.e("Firestore", "Error de permisos (esperado en logout)", error)
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
                AppLogger.e("Firestore", "Ignorando error de permisos al cerrar sesión en getEventById")
                return@addSnapshotListener
            }
            val event = snapshot?.toObject(Event::class.java)
            trySend(event)
        }
        awaitClose { subscription.remove() }
    }

    override suspend fun createEvent(event: Event): Result<String> {
        return try {
            val newDocumentRef = eventCollection.document()
            // La contraseña NUNCA se escribe en el documento público del evento:
            // ese documento lo puede leer cualquier usuario autenticado. Se guarda
            // aparte, en una subcolección que las reglas de Firestore no exponen
            // al cliente (solo la lee la Cloud Function joinPrivateEvent vía Admin SDK).
            val eventWithId = event.copy(id = newDocumentRef.id, password = "")
            newDocumentRef.set(eventWithId).await()

            if (event.isPrivate && event.password.isNotBlank()) {
                newDocumentRef.collection("private").document("config")
                    .set(mapOf("password" to event.password)).await()
            }

            Result.success(newDocumentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinEvent(eventId: String, userId: String, userName: String): Result<Unit> {
        return try {
            val eventRef = firestore.collection("events").document(eventId)
            val rankingRef = eventRef.collection("rankings").document(userId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val event = snapshot.toObject(Event::class.java)

                val participants = event?.participantsIds ?: emptyList()
                val max = event?.maxParticipants ?: Int.MAX_VALUE

                if (participants.size >= max) throw Exception("EVENT_FULL")
                if (participants.contains(userId)) throw Exception("ALREADY_JOINED")

                val rankingData = RankingUser(
                    id = userId,
                    userName = userName,
                    points = 0
                )

                transaction.set(rankingRef, rankingData)
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
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val msgs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(msgs)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun sendMessage(eventId: String, message: ChatMessage) {
        try {
            val messagesCollection = firestore.collection("events")
                .document(eventId)
                .collection("messages")

            val newDocRef = messagesCollection.document()

            val messageWithId = message.copy(id = newDocRef.id)

            newDocRef.set(messageWithId).await()
        } catch (e: Exception) {
            AppLogger.e("Repository", "Error enviando mensaje", e)
            throw e
        }
    }

    override suspend fun leaveEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            val eventRef = firestore.collection("events").document(eventId)
            val rankingRef = eventRef.collection("rankings").document(userId)

            firestore.runTransaction { transaction ->
                transaction.update(eventRef, "participantsIds", FieldValue.arrayRemove(userId))
                transaction.delete(rankingRef)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            firestore.collection("events").document(eventId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun finishEvent(eventId: String): Result<Unit> {
        return try {
            firestore.collection("events")
                .document(eventId)
                .update("isFinished", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getUserEventHistory(userId: String?): Flow<List<Event>> = callbackFlow {
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = eventCollection
            .whereArrayContains("participantsIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.e("Firestore", "Error cargando historial", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val events = snapshot.toObjects(Event::class.java)
                    trySend(events)
                }
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            val eventRef = firestore.collection("events").document(event.id)

            // Igual que en createEvent: la contraseña no viaja en el documento
            // público. Si el campo viene en blanco, entendemos que el organizador
            // no quiere cambiarla y dejamos intacta la que ya hubiera guardada.
            eventRef.set(event.copy(password = "")).await()

            if (event.isPrivate && event.password.isNotBlank()) {
                eventRef.collection("private").document("config")
                    .set(mapOf("password" to event.password)).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}