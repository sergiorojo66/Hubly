package com.example.rankup.ui.createEventScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.model.RankingUser
import com.example.rankup.domain.model.enums.EventCategory
import com.example.rankup.domain.repository.EventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    var state by mutableStateOf(CreateEventState())
        private set

    val isFormValid: Boolean
        get() = state.title.isNotBlank() &&
                state.location.isNotBlank() &&
                state.date > 0L

    fun onTitleChange(newValue: String) { state = state.copy(title = newValue) }
    fun onDescriptionChange(newValue: String) { state = state.copy(description = newValue) }
    fun onCategoryChange(newValue: String) { state = state.copy(category = EventCategory.valueOf(newValue)) }
    fun onLocationChange(newValue: String) { state = state.copy(location = newValue) }
    fun onDateChange(newValue: Long) { state = state.copy(date = newValue) }
    fun onMaxParticipantsChange(newValue: String) {
        val digitsOnly = newValue.filter { it.isDigit() }
        val sanitizedValue = when {
            digitsOnly.isEmpty() -> ""
            digitsOnly.startsWith("0") -> digitsOnly.dropWhile { it == '0' }
            else -> digitsOnly
        }
        state = state.copy(maxParticipants = sanitizedValue)
    }

    fun toggleCategoryMenu(expanded: Boolean) {
        state = state.copy(isCategoryMenuExpanded = expanded)
    }

    fun togglePrivacy(isPrivate: Boolean) {
        state = state.copy(isPrivate = isPrivate)
    }

    fun onCategoryChange(category: EventCategory) {
        state = state.copy(
            category = category,
            isCategoryMenuExpanded = false
        )
    }
    fun createEvent(onSuccess: () -> Unit) {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            val currentUserId = auth.currentUser?.uid ?: return@launch

            // 1. Obtener el nombre real del creador antes de crear el evento
            val userDoc = FirebaseFirestore.getInstance().collection("users").document(currentUserId).get().await()
            val realName = userDoc.getString("displayName") ?: "Organizador"

            val categoryImageUrl = when (state.category) {
                EventCategory.SPORTS -> "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=500"
                EventCategory.SOCIAL -> "https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=500"
                EventCategory.ESPORTS -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500"
                EventCategory.EDUCATION -> "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?q=80&w=1000"
                EventCategory.OTHER -> "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=500"
            }

            val newEvent = Event(
                title = state.title,
                description = state.description,
                category = state.category.name,
                location = state.location,
                date = state.date,
                isPrivate = state.isPrivate,
                imageUrl = categoryImageUrl,
                organizer = currentUserId,
                maxParticipants = state.maxParticipants.toIntOrNull(),
                modules = state.selectedModules.toList(),
                participantsIds = listOf(currentUserId)
            )

            // 2. Crear el evento y recibir el ID generado
            eventRepository.createEvent(newEvent).onSuccess { generatedId ->
                val creatorRanking = RankingUser(
                    id = currentUserId,
                    userName = realName, // Ahora usa su nombre real
                    points = 0,
                    level = 1
                )

                // 3. Crear el ranking usando el ID generado (generatedId)
                FirebaseFirestore.getInstance()
                    .collection("events")
                    .document(generatedId) // <--- IMPORTANTE: Usar el ID que devuelve el repo
                    .collection("rankings")
                    .document(currentUserId)
                    .set(creatorRanking)
                    .await() // Esperamos a que se cree para evitar race conditions

                onSuccess()
            }.onFailure {
                state = state.copy(error = "Error al guardar el evento", isLoading = false)
            }
        }
    }
    fun toggleModule(moduleId: String) {
        if (moduleId == "info") return // No se puede quitar "info"

        val currentModules = state.selectedModules.toMutableSet()
        if (currentModules.contains(moduleId)) {
            currentModules.remove(moduleId)
        } else {
            currentModules.add(moduleId)
        }
        state = state.copy(selectedModules = currentModules)
    }
}

data class CreateEventState(
    val title: String = "",
    val description: String = "",
    val category: EventCategory = EventCategory.SPORTS,
    val isCategoryMenuExpanded: Boolean = false,
    val location: String = "",
    val date: Long = System.currentTimeMillis(),
    val maxParticipants: String = "",
    val selectedModules: Set<String> = setOf("info"),
    val isLoading: Boolean = false,
    val isPrivate: Boolean = false,
    val error: String? = null
)