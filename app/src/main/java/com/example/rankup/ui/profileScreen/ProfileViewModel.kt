package com.example.rankup.ui.profileScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.model.User
import com.example.rankup.domain.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val eventRepository: EventRepository
) : ViewModel() {

    var userState by mutableStateOf<User?>(null)
        private set

    private val _eventHistory = MutableStateFlow<List<Event>>(emptyList())
    val eventHistory = _eventHistory.asStateFlow()

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(uid).get().await()
                userState = snapshot.toObject(User::class.java)
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    fun updateProfile(newName: String, newBio: String) {
        val uid = auth.currentUser?.uid ?: return
        val newInitials = if (newName.isNotBlank()) newName.take(1).uppercase() else "U"

        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update(
                        "displayName", newName,
                        "bio", newBio,
                        "initials", newInitials // Actualizamos también las iniciales
                    ).await()

                userState = userState?.copy(
                    displayName = newName,
                    bio = newBio,
                    initials = newInitials
                )
            } catch (e: Exception) {
                println("Error al actualizar perfil: ${e.message}")
            }
        }
    }

    fun loadEventHistory() {
        viewModelScope.launch {
            try {
                eventRepository.getUserEventHistory(auth.currentUser?.uid).collect { events ->
                    _eventHistory.value = events
                }
            } catch (e: Exception) {
                println("Error al cargar historial de eventos: ${e.message}")
            }
        }
    }
}