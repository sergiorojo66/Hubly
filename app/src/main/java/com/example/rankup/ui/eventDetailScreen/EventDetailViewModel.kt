package com.example.rankup.ui.eventDetailScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rankup.domain.model.ChatMessage
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.model.RankingUser
import com.example.rankup.domain.model.User
import com.example.rankup.domain.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.text.get

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: EventRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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

                    // Lógica de empate
                    if (index > 0) {
                        val previousPoints = documents[index - 1].getLong("points") ?: 0L
                        val currentPoints = doc.getLong("points") ?: 0L

                        // Si NO hay empate, la posición sube
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
                // Obtenemos el nombre real del usuario de nuestra colección 'users'
                val userDoc = firestore.collection("users").document(userId).get().await()
                val hublyUser = userDoc.toObject(User::class.java)
                val nameToRegister = hublyUser?.displayName ?: "Usuario"

                // Pasamos el nombre al repositorio
                repository.joinEvent(eventId, userId, nameToRegister).onSuccess {
                    _error.value = null
                    // Al unirse con éxito, podrías recargar el ranking
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

    fun loadChatMessages(eventId: String) {
        viewModelScope.launch {
            repository.getChatMessages(eventId).collect { msgs ->
                _chatMessages.value = msgs
            }
        }
    }

    fun sendMessage(text: String) {
        val eventId = _event.value?.id ?: return
        val userUid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(userUid).get().await()
                val hublyUser = userDoc.toObject(User::class.java)

                val newMessage = ChatMessage(
                    senderId = userUid,
                    senderName = hublyUser?.displayName ?: "Usuario",
                    message = text,
                    // Ajuste aquí para evitar el Type Mismatch
                    timestamp = java.util.Date()
                )
                repository.sendMessage(eventId, newMessage)
            } catch (e: Exception) {
                _error.value = "Error al enviar mensaje"
            }
        }
    }

    // En EventDetailViewModel.kt

    fun loadEvent(id: String) {
        viewModelScope.launch {
            loadRankings(id)
            loadChatMessages(id)

            repository.getEventById(id).collect { eventData ->
                _event.value = eventData

                // IMPORTANTE: Disparar la carga del nombre cuando tengamos el evento
                eventData?.organizer?.let { organizerId ->
                    loadOrganizerName(organizerId)
                }
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

                // Usamos una transacción o FieldValue.increment para que sea seguro
                userRankingRef.update("points", com.google.firebase.firestore.FieldValue.increment(additionalPoints.toLong()))
                    .await()

                // Opcional: mostrar un mensaje de éxito o actualizar localmente
            } catch (e: Exception) {
                _error.value = "Error al actualizar puntos: ${e.message}"
            }
        }
    }

    fun leaveEvent(eventId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            repository.leaveEvent(eventId, userId).onSuccess {
                // Firestore actualizará el isUserJoined automáticamente si usas snapshots
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
        viewModelScope.launch {
            repository.finishEvent(eventId).onSuccess {
                _error.value = "Evento finalizado correctamente"
            }.onFailure {
                _error.value = "Error al finalizar el evento"
            }
        }
    }

    fun loadUserProfile(userId: String, onResult: (User) -> Unit) {
        viewModelScope.launch {
            try {
                // Buscamos el documento del usuario en la colección "users"
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
}

