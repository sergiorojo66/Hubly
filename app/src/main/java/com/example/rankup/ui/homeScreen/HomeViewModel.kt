package com.example.rankup.ui.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _stats = MutableStateFlow(HomeStats())
    val stats = _stats.asStateFlow()

    init {
        // 🚀 ¡AQUÍ SE EJECUTA AUTOMÁTICAMENTE AL ABRIR LA PANTALLA!
        sincronizarTokenDispositivo()
    }

    private fun sincronizarTokenDispositivo() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result

                // Guardamos o actualizamos el token en el documento del usuario en Firestore
                firestore.collection("users")
                    .document(currentUserId)
                    .update("fcmToken", token)
                    .addOnFailureListener {
                        // Por si el documento no existe aún, puedes usar set con Merge
                        val data = hashMapOf("fcmToken" to token)
                        firestore.collection("users").document(currentUserId)
                            .set(data, com.google.firebase.firestore.SetOptions.merge())
                    }
            }
        }
    }

    init {
        loadDataFromFirebase()
    }

    private fun loadDataFromFirebase() {
        viewModelScope.launch {
            eventRepository.getEvents().collect { listEvents ->
                _events.value = listEvents
                updateStats(listEvents)
            }
        }
    }

    private fun updateStats(lista: List<Event>) {
        val totalParticipants = lista.sumOf { it.participants.toString().toIntOrNull() ?: 0 }

        _stats.value = HomeStats(
            activeEvents = lista.size,
            participants = if (totalParticipants >= 1000) {
                String.format("%.1fK", totalParticipants / 1000.0)
            } else {
                "$totalParticipants"
            },
            competitions = lista.count { it.category == "Competitivo" || it.category == "eSports" }
        )
    }
}