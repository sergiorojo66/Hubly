package com.example.rankup.ui.eventDetailScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rankup.domain.model.ChatMessage
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.model.RankingUser
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
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: EventRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _event = MutableStateFlow<Event?>(null)
    val event = _event.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _rankings = MutableStateFlow<List<RankingUser>>(emptyList())
    val rankings = _rankings.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    val isUserJoined: StateFlow<Boolean> = _event.map { event ->
        val currentUserId = auth.currentUser?.uid
        event?.participantsIds?.contains(currentUserId) ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadEvent(id: String) {
        viewModelScope.launch {
            loadRankings(id)
            loadChatMessages(id)

            repository.getEventById(id).collect { eventData ->
                _event.value = eventData
            }
        }
    }

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
        FirebaseFirestore.getInstance()
            .collection("events")
            .document(eventId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val msgs = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                _chatMessages.value = msgs
            }
    }

    fun sendMessage(text: String) {
        val user = auth.currentUser ?: return
        val eventId = _event.value?.id ?: return

        val newMessage = ChatMessage(
            senderId = user.uid,
            senderName = user.displayName ?: "Usuario",
            message = text,
            timestamp = System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("events")
            .document(eventId)
            .collection("messages")
            .add(newMessage)
    }
}

