package com.example.rankup.ui.exploreScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.model.enums.EventCategory
import com.example.rankup.domain.repository.EventRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    var state by mutableStateOf(ExploreState())
        private set

    private var allEvents = listOf<Event>()

    init {
        loadEvents()
    }

    private fun loadEvents() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)

            // Recolectamos el Flow de eventos en tiempo real
            eventRepository.getEvents().collect { events ->
                allEvents = events
                filterEvents() // Aplicamos los filtros cada vez que cambien los datos o los filtros
                state = state.copy(isLoading = false)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        state = state.copy(searchQuery = query)
        filterEvents()
    }

    fun onTypeFilterChanged(type: String) {
        state = state.copy(selectedType = type)
        filterEvents()
    }

    private fun filterEvents() {
        var filtered = allEvents

        if (state.selectedCategories.isNotEmpty()) {
            val categoryNames = state.selectedCategories.map { it.name }
            filtered = filtered.filter { it.category in categoryNames }
        }

        filtered = when (state.selectedType) {
            "Públicos" -> filtered.filter { it.isPrivate == false }
            "Privados" -> filtered.filter { it.isPrivate == true }
            else -> filtered
        }

        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
        }

        if (state.locationFilter.isNotBlank()) {
            filtered = filtered.filter { it.location.contains(state.locationFilter, ignoreCase = true) }
        }

        filtered = filtered.filter { event ->
            val eventDate = event.date
            val start = state.startDateFilter
            val end = state.endDateFilter

            when {
                start != null && end != null -> eventDate in start..end
                start != null -> eventDate >= start
                else -> true
            }
        }

        state = state.copy(filteredEvents = filtered)
    }

    fun onAdvancedFiltersApplied(
        location: String,
        start: Long?,
        end: Long?,
        categories: List<EventCategory>
    ) {
        state = state.copy(
            locationFilter = location,
            startDateFilter = start,
            endDateFilter = end,
            selectedCategories = categories
        )
        filterEvents()
    }

    fun searchEventById(eventId: String, onFound: (String) -> Unit) {
        viewModelScope.launch {
            // Buscas directamente el documento en Firestore
            val doc = firestore.collection("events").document(eventId).get().await()
            if (doc.exists()) {
                onFound(eventId) // Si existe, devuelves el ID para navegar
            } else {
                //_error.value = "Evento no encontrado"
            }
        }
    }
}

data class ExploreState(
    val searchQuery: String = "",
    val selectedType: String = "Todos",
    val selectedCategories: List<EventCategory> = emptyList(),
    val locationFilter: String = "",
    val startDateFilter: Long? = null,
    val endDateFilter: Long? = null,
    val filteredEvents: List<Event> = emptyList(),
    val isLoading: Boolean = false
)