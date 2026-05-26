package com.example.rankup.ui.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeStats(
    val activeEvents: Int = 0,
    val participants: String = "0",
    val competitions: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events = _events.asStateFlow()
    val ahoraMismoEvents = _events.map { list ->
        val currentTime = System.currentTimeMillis()
        list.filter { !it.isFinished && it.date <= currentTime }
    }.stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

    val proximamenteEvents = _events.map { list ->
        val currentTime = System.currentTimeMillis()
        val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000
        list.filter { !it.isFinished && it.date > currentTime && it.date <= (currentTime + sevenDaysInMillis) }
    }.stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

    private val _stats = MutableStateFlow(HomeStats())
    val stats = _stats.asStateFlow()

    init {
        sincronizarTokenDispositivo()
        loadDataFromFirebase()
        loadTotalHublers()
    }

    private fun sincronizarTokenDispositivo() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result

                firestore.collection("users")
                    .document(currentUserId)
                    .update("fcmToken", token)
                    .addOnFailureListener {
                        val data = hashMapOf("fcmToken" to token)
                        firestore.collection("users").document(currentUserId)
                            .set(data, SetOptions.merge())
                    }
            }
        }
    }

    private fun loadDataFromFirebase() {
        viewModelScope.launch {
            eventRepository.getEvents().collect { listEvents ->
                _events.value = listEvents
                updateStats(listEvents)
            }
        }
    }

    private fun loadTotalHublers() {
        firestore.collection("users").get()
            .addOnSuccessListener { snapshot ->
                val totalUsers = snapshot.size()
                val formatUsers = if (totalUsers >= 1000) {
                    String.format("%.1fK", totalUsers / 1000.0)
                } else {
                    "$totalUsers"
                }
                _stats.value = _stats.value.copy(participants = formatUsers)
            }
            .addOnFailureListener { e ->

            }
    }

    private fun updateStats(lista: List<Event>) {
        _stats.value = _stats.value.copy(
            activeEvents = lista.size,
            competitions = lista.count { it.category == "Competitivo" || it.category == "eSports" }
        )
    }
}