package com.example.rankup.data.repository

import android.util.Log
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

    override suspend fun createEvent(event: Event): Result<String> {
        return try {
            val newDocumentRef = eventCollection.document()
            val eventWithId = event.copy(id = newDocumentRef.id)
            newDocumentRef.set(eventWithId).await()
            Result.success(newDocumentRef.id) // Devolvemos el ID
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

                // 1. Validaciones de negocio
                if (participants.size >= max) throw Exception("EVENT_FULL")
                if (participants.contains(userId)) throw Exception("ALREADY_JOINED")

                // 2. Preparar el objeto de Ranking
                val rankingData = RankingUser(
                    id = userId, // Asegúrate de que tu modelo usa 'userId' o 'id'
                    userName = userName, // Usamos el parámetro que pasamos
                    points = 0,
                    level = 1
                )

                // 3. Ejecutar operaciones dentro de la transacción
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
                    // Mapeamos el objeto y, si por alguna razón el ID viniera vacío, lo recuperamos del nombre del documento
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(msgs)
            }
        awaitClose { subscription.remove() }
    }

    // En EventRepositoryImpl.kt

    override suspend fun sendMessage(eventId: String, message: ChatMessage) {
        try {
            val messagesCollection = firestore.collection("events")
                .document(eventId)
                .collection("messages")

            // 1. Creamos una referencia de documento vacía para obtener la ID
            val newDocRef = messagesCollection.document()

            // 2. Copiamos el mensaje con la ID del documento recién creado
            val messageWithId = message.copy(id = newDocRef.id)

            // 3. Guardamos el objeto completo (ahora el campo 'id' en Firebase tendrá valor)
            newDocRef.set(messageWithId).await()
        } catch (e: Exception) {
            Log.e("Repository", "Error enviando mensaje: ${e.message}")
            throw e
        }
    }

    override suspend fun leaveEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            val eventRef = firestore.collection("events").document(eventId)
            val rankingRef = eventRef.collection("rankings").document(userId)

            firestore.runTransaction { transaction ->
                // 1. Eliminar del array de participantes
                transaction.update(eventRef, "participantsIds", FieldValue.arrayRemove(userId))
                // 2. Eliminar su documento de ranking
                transaction.delete(rankingRef)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            // Nota: En Firestore, borrar un documento no borra sus subcolecciones automáticamente.
            // Por simplicidad borramos el evento. Los rankings quedarían huérfanos pero no visibles.
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
                .update("isFinished", true) // Cambiamos el estado a true
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
                    Log.e("Firestore", "Error cargando historial: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val events = snapshot.toObjects(Event::class.java)
                    trySend(events)
                }
            }

        awaitClose { subscription.remove() }
    }
}