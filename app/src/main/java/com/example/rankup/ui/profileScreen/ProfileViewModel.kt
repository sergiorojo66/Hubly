package com.example.rankup.ui.profileScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.model.User
import com.example.rankup.domain.repository.EventRepository
import com.example.rankup.util.AppLogger
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

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        fetchUserProfile()
    }

    fun clearError() {
        _error.value = null
    }

    private fun fetchUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(uid).get().await()
                userState = snapshot.toObject(User::class.java)
            } catch (e: Exception) {
                AppLogger.e("ProfileViewModel", "Error al cargar perfil", e)
                _error.value = "No se pudo cargar tu perfil. Comprueba tu conexión e inténtalo de nuevo."
            }
        }
    }

    fun updateProfile(newName: String, newBio: String) {
        val uid = auth.currentUser?.uid ?: return
        val cleanName = newName.trim()

        if (cleanName.length !in 3..30) {
            _error.value = "El nombre debe tener entre 3 y 30 caracteres"
            return
        }

        val newInitials = cleanName.take(1).uppercase()

        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update(
                        "displayName", cleanName,
                        "bio", newBio.trim(),
                        "initials", newInitials
                    ).await()

                userState = userState?.copy(
                    displayName = cleanName,
                    bio = newBio.trim(),
                    initials = newInitials
                )
                _error.value = null
            } catch (e: Exception) {
                AppLogger.e("ProfileViewModel", "Error al actualizar perfil", e)
                _error.value = "No se pudo actualizar tu perfil. Inténtalo de nuevo."
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
                AppLogger.e("ProfileViewModel", "Error al cargar historial de eventos", e)
                _error.value = "No se pudo cargar tu historial de eventos."
            }
        }
    }
}
