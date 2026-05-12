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

                val list = snapshot?.documents?.mapIndexed { index, doc ->
                    val user = doc.toObject(RankingUser::class.java)
                    user?.copy(position = index + 1) ?: RankingUser()
                } ?: emptyList()

                _rankings.value = list
            }
    }

    fun joinEvent(eventId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            repository.joinEvent(eventId, userId).onSuccess {
                _error.value = null
            }.onFailure { error ->
                if (error.message == "EVENT_FULL") {
                    _error.value = "Lo sentimos, el evento está lleno"
                } else {
                    _error.value = "Error al unirse al evento"
                }
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
}

