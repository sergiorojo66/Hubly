package com.example.rankup.ui.eventDetailScreen

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rankup.data.network.CloudFunctionsClient
import com.example.rankup.util.AppLogger
import com.example.rankup.domain.model.ChatMessage
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.model.RankingUser
import com.example.rankup.domain.model.User
import com.example.rankup.domain.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: EventRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _event = MutableStateFlow<Event?>(null)
    val event = _event.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _rankings = MutableStateFlow<List<RankingUser>>(emptyList())
    val rankings = _rankings.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    val currentUserId: String? = auth.currentUser?.uid

    private val _organizerName = MutableStateFlow("Cargando...")
    val organizerName = _organizerName.asStateFlow()

    val isUserJoined: StateFlow<Boolean> = _event.map { event ->
        val currentUserId = auth.currentUser?.uid
        event?.participantsIds?.contains(currentUserId) ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _participantsProfiles = MutableStateFlow<List<User>>(emptyList())
    val participantsProfiles = _participantsProfiles.asStateFlow()

    var editTitle by mutableStateOf("")
    var editDescription by mutableStateOf("")
    var editCategory by mutableStateOf("")
    var editLocation by mutableStateOf("")
    var editMaxParticipants by mutableStateOf("")
    var editDate by mutableStateOf(0L)
    var editIsPrivate by mutableStateOf(false)
    var editPassword by mutableStateOf("")
    var editModules by mutableStateOf<List<String>>(listOf("info"))
    var isCategoryMenuExpanded by mutableStateOf(false)
    var isSavingUpdate by mutableStateOf(false)

    fun prepareEditForm(currentEvent: Event) {
        editTitle = currentEvent.title
        editDescription = currentEvent.description
        editCategory = currentEvent.category
        editLocation = currentEvent.location
        editMaxParticipants = currentEvent.maxParticipants?.toString() ?: ""
        editDate = currentEvent.date
        editIsPrivate = currentEvent.isPrivate
        editPassword = currentEvent.password
        editModules = currentEvent.modules
    }

    private fun loadRankings(eventId: String) {
        FirebaseFirestore.getInstance()
            .collection("events")
            .document(eventId)
            .collection("rankings")
            .orderBy("points", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val documents = snapshot?.documents ?: emptyList()
                var currentPosition = 1

                val list = documents.mapIndexed { index, doc ->
                    val user = doc.toObject(RankingUser::class.java) ?: RankingUser()

                    if (index > 0) {
                        val previousPoints = documents[index - 1].getLong("points") ?: 0L
                        val currentPoints = doc.getLong("points") ?: 0L

                        if (currentPoints < previousPoints) {
                            currentPosition++
                        }
                    }

                    user.copy(position = currentPosition)
                }

                _rankings.value = list
            }
    }

    fun joinEvent(eventId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                val hublyUser = userDoc.toObject(User::class.java)
                val nameToRegister = hublyUser?.displayName ?: "Usuario"

                repository.joinEvent(eventId, userId, nameToRegister).onSuccess {
                    _error.value = null
                    loadRankings(eventId)
                }.onFailure { error ->
                    _error.value = when(error.message) {
                        "EVENT_FULL" -> "El evento está lleno"
                        "ALREADY_JOINED" -> "Ya estás en este evento"
                        else -> "Error al unirse"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión"
            }
        }
    }

    /**
     * Unirse a un evento PRIVADO. A diferencia de joinEvent (para eventos
     * públicos), esto pasa por la Cloud Function joinPrivateEvent: la
     * contraseña se valida en el servidor y nunca se compara en el cliente.
     * onResult(true) = unido correctamente; onResult(false) = contraseña
     * incorrecta u otro fallo (el mensaje ya queda en _error).
     */
    fun joinPrivateEvent(eventId: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = CloudFunctionsClient.joinPrivateEvent(eventId, password)
            result.onSuccess {
                _error.value = null
                loadRankings(eventId)
                onResult(true)
            }.onFailure { error ->
                _error.value = error.message ?: "No se pudo unir al evento"
                onResult(false)
            }
        }
    }

    fun loadChatMessages(eventId: String) {
        viewModelScope.launch {
            try {
                repository.getChatMessages(eventId).collect { msgs ->
                    _chatMessages.value = msgs
                }
            } catch (e: Exception) {
                AppLogger.e("EventDetailViewModel", "Error cargando mensajes del chat", e)
                _error.value = "No se pudo cargar el chat. Comprueba tu conexión."
            }
        }
    }

    fun sendMessage(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        if (cleanText.length > 1000) {
            _error.value = "El mensaje es demasiado largo (máximo 1000 caracteres)"
            return
        }

        val eventCurrent = _event.value ?: return
        val eventId = eventCurrent.id
        val userUid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(userUid).get().await()
                val hublyUser = userDoc.toObject(User::class.java)
                val senderName = hublyUser?.displayName ?: "Usuario"

                val newMessage = ChatMessage(
                    senderId = userUid,
                    senderName = senderName,
                    message = cleanText,
                    timestamp = java.util.Date()
                )

                repository.sendMessage(eventId, newMessage)

                CloudFunctionsClient.notifyEvent(
                    eventId = eventId,
                    title = "Chat: ${eventCurrent.title}",
                    body = "$senderName: $cleanText"
                )
            } catch (e: Exception) {
                AppLogger.e("EventDetailViewModel", "Error al enviar mensaje", e)
                _error.value = "Error al enviar mensaje"
            }
        }
    }

    fun loadEvent(id: String) {
        viewModelScope.launch {
            loadRankings(id)
            loadChatMessages(id)

            try {
                repository.getEventById(id).collect { eventData ->
                    _event.value = eventData

                    eventData?.organizer?.let { organizerId ->
                        loadOrganizerName(organizerId)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("EventDetailViewModel", "Error cargando el evento $id", e)
                _error.value = "No se pudo cargar el evento. Comprueba tu conexión."
            }
        }
    }

    fun loadOrganizerName(organizerId: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(organizerId).get().await()
                val user = doc.toObject(User::class.java)
                _organizerName.value = user?.displayName ?: "Usuario"
            } catch (e: Exception) {
                _organizerName.value = "Organizador"
            }
        }
    }

    fun updateUserPoints(userId: String, additionalPoints: Int) {
        val eventId = _event.value?.id ?: return

        viewModelScope.launch {
            try {
                val userRankingRef = firestore.collection("events")
                    .document(eventId)
                    .collection("rankings")
                    .document(userId)

                userRankingRef.update("points", com.google.firebase.firestore.FieldValue.increment(additionalPoints.toLong()))
                    .await()

            } catch (e: Exception) {
                _error.value = "Error al actualizar puntos: ${e.message}"
            }
        }
    }

    fun leaveEvent(eventId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            repository.leaveEvent(eventId, userId).onSuccess {

            }.onFailure {
                _error.value = "No se pudo anular la inscripción"
            }
        }
    }

    fun deleteEvent(eventId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteEvent(eventId).onSuccess {
                onComplete()
            }.onFailure {
                _error.value = "Error al eliminar el evento"
            }
        }
    }

    fun finishEvent(eventId: String) {
        val eventCurrent = _event.value ?: return
        val userUid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            repository.finishEvent(eventId).onSuccess {
                _error.value = "Evento finalizado correctamente"

                viewModelScope.launch {
                    try {
                        CloudFunctionsClient.notifyEvent(
                            eventId = eventId,
                            title = "🏁 ¡Evento Finalizado!",
                            body = "El evento '${eventCurrent.title}' ha terminado. ¡Entra para ver los resultados y el ranking final!"
                        )
                    } catch (e: Exception) {
                        AppLogger.e("NotificationFinish", "Error al enviar push de finalización", e)
                    }
                }

            }.onFailure {
                _error.value = "Error al finalizar el evento"
            }
        }
    }

    fun loadUserProfile(userId: String, onResult: (User) -> Unit) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(userId).get().await()
                val user = doc.toObject(User::class.java)
                if (user != null) {
                    onResult(user)
                }
            } catch (e: Exception) {
                _error.value = "No se pudo cargar el perfil"
            }
        }
    }

    fun loadParticipantsProfiles(participantsIds: List<String>) {
        viewModelScope.launch {
            try {
                val usersList = mutableListOf<User>()
                for (id in participantsIds) {
                    val doc = firestore.collection("users").document(id).get().await()
                    doc.toObject(User::class.java)?.let { usersList.add(it) }
                }
                _participantsProfiles.value = usersList
            } catch (e: Exception) {
                _error.value = "Error al cargar la lista de participantes"
            }
        }
    }

    fun validateForm(currentEvent: Event, onError: (String) -> Unit): Boolean {
        if (editTitle.isBlank() || editLocation.isBlank()) {
            onError("El título y la ubicación son obligatorios.")
            return false
        }

        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (editDate < todayStart) {
            onError("La fecha seleccionada no puede ser anterior a la actual.")
            return false
        }

        val currentParticipantsCount = currentEvent.participantsIds.size
        val newMax = editMaxParticipants.toIntOrNull()

        if (newMax != null && newMax < currentParticipantsCount) {
            onError("El límite no puede ser menor que los inscritos actuales ($currentParticipantsCount).")
            return false
        }

        if (editIsPrivate) {
            // El campo llega vacío al editar un evento que YA era privado (la
            // contraseña real vive en una subcolección no legible desde el
            // cliente): en blanco significa "no cambiar la contraseña actual".
            // Si el evento pasa de público a privado ahora, sí es obligatoria.
            val wasAlreadyPrivate = currentEvent.isPrivate
            if (editPassword.isBlank()) {
                if (!wasAlreadyPrivate) {
                    onError("Introduce una contraseña para el evento privado.")
                    return false
                }
            } else if (editPassword.length !in 4..16) {
                onError("La contraseña debe tener entre 4 y 16 caracteres.")
                return false
            }
        }

        return true
    }

    fun saveUpdatedEvent(onSuccess: () -> Unit) {
        val currentEvent = _event.value ?: return
        val userUid = auth.currentUser?.uid ?: return
        isSavingUpdate = true

        viewModelScope.launch {
            val newImageUrl = when (editCategory) {
                "SPORTS" -> "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=500"
                "SOCIAL" -> "https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=500"
                "ESPORTS" -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500"
                "EDUCATION" -> "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?q=80&w=1000"
                else -> "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=500"
            }

            val updatedEvent = currentEvent.copy(
                title = editTitle,
                description = editDescription,
                category = editCategory,
                location = editLocation,
                maxParticipants = editMaxParticipants.toIntOrNull(),
                date = editDate,
                isPrivate = editIsPrivate,
                password = if (editIsPrivate) editPassword else "",
                modules = editModules,
                imageUrl = newImageUrl
            )

            repository.updateEvent(updatedEvent).onSuccess {
                isSavingUpdate = false
                _error.value = "Evento actualizado correctamente"
                onSuccess()

                viewModelScope.launch {
                    try {
                        CloudFunctionsClient.notifyEvent(
                            eventId = eventId,
                            title = "¡Evento actualizado!",
                            body = "El evento '$editTitle' ha sido modificado por el organizador."
                        )
                    } catch (e: Exception) {
                        AppLogger.e("NotificationUpdate", "Error enviando push de actualización", e)
                    }
                }

            }.onFailure {
                isSavingUpdate = false
                _error.value = "Error al actualizar el evento en la base de datos."
            }
        }
    }

    fun toggleEditModule(module: String) {
        editModules = if (editModules.contains(module)) {
            editModules.filter { it != module }
        } else {
            editModules + module
        }
    }

    fun rateUser(targetUserId: String, score: Double, onComplete: () -> Unit) {
        val voterId = currentUserId ?: return

        viewModelScope.launch {
            try {
                val ratingDocRef = firestore.collection("users")
                    .document(targetUserId)
                    .collection("ratings")
                    .document(voterId)

                val ratingData = mapOf("score" to score)
                ratingDocRef.set(ratingData).await()

                val userRef = firestore.collection("users").document(targetUserId)

                firestore.runTransaction { transaction ->
                    val ratingsSnapshot = firestore.collection("users")
                        .document(targetUserId)
                        .collection("ratings")
                        .get()
                }.addOnSuccessListener {

                }

                val allRatingsSnapshot = firestore.collection("users")
                    .document(targetUserId)
                    .collection("ratings")
                    .get()
                    .await()

                val documents = allRatingsSnapshot.documents
                if (documents.isNotEmpty()) {
                    val sum = documents.sumOf { it.getDouble("score") ?: 0.0 }
                    val newAverage = sum / documents.size

                    val roundedAverage = String.format(java.util.Locale.US, "%.1f", newAverage).toDouble()

                    firestore.collection("users")
                        .document(targetUserId)
                        .update("rating", roundedAverage)
                        .await()
                }

                _error.value = "¡Valoración guardada con éxito!"
                onComplete()
            } catch (e: Exception) {
                _error.value = "Error al guardar la valoración: ${e.message}"
            }
        }
    }
}

