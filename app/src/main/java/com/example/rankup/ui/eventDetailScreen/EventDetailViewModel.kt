package com.example.rankup.ui.eventDetailScreen

import android.content.Context
import androidx.compose.runtime.getValue
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.text.get

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: EventRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context // <-- Inyectamos el contexto de forma segura
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
                    message = text,
                    timestamp = java.util.Date()
                )

                // 1. Enviamos el mensaje al chat
                repository.sendMessage(eventId, newMessage)

                // 2. Lógica de Notificaciones: Traer tokens de los participantes (menos el emisor)
                val idsANotificar = eventCurrent.participantsIds.filter { it != userUid }

                if (idsANotificar.isNotEmpty()) {
                    val tokensList = mutableListOf<String>()

                    // Consultamos los tokens en Firestore
                    for (id in idsANotificar) {
                        val doc = firestore.collection("users").document(id).get().await()
                        val token = doc.getString("fcmToken")
                        if (!token.isNullOrBlank()) {
                            tokensList.add(token)
                        }
                    }

                    // Si hay dispositivos válidos, enviamos el push
                    if (tokensList.isNotEmpty()) {
                        com.example.rankup.data.network.FcmSender.enviarNotificacionPush(
                            context = context,
                            tokensParticipantes = tokensList,
                            titulo = "Chat: ${eventCurrent.title}",
                            mensaje = "$senderName: $text"
                        )
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error al enviar mensaje o notificación"
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
        val eventCurrent = _event.value ?: return
        val userUid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            repository.finishEvent(eventId).onSuccess {
                _error.value = "Evento finalizado correctamente"

                // Lógica de Notificaciones: Avisar a los participantes que el evento ha terminado
                viewModelScope.launch {
                    try {
                        // Filtramos para no mandarnos un push a nosotros mismos como organizadores
                        val idsANotificar = eventCurrent.participantsIds.filter { it != userUid }

                        if (idsANotificar.isNotEmpty()) {
                            val tokensList = mutableListOf<String>()

                            for (id in idsANotificar) {
                                val doc = firestore.collection("users").document(id).get().await()
                                val token = doc.getString("fcmToken")
                                if (!token.isNullOrBlank()) {
                                    tokensList.add(token)
                                }
                            }

                            if (tokensList.isNotEmpty()) {
                                com.example.rankup.data.network.FcmSender.enviarNotificacionPush(
                                    context = context,
                                    tokensParticipantes = tokensList,
                                    titulo = "🏁 ¡Evento Finalizado!",
                                    mensaje = "El evento '${eventCurrent.title}' ha terminado. ¡Entra para ver los resultados y el ranking final!"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationFinish", "Error al enviar push de finalización", e)
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

    fun loadParticipantsProfiles(participantsIds: List<String>) {
        viewModelScope.launch {
            try {
                val usersList = mutableListOf<User>()
                // Hacemos las peticiones en paralelo o secuenciales cortas
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

        // 1. Validar que la fecha no sea anterior a la actual (hoy a las 00:00 para evitar desajustes de horas)
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

        // 2. Validar participantes inscritos vs nuevo límite
        val currentParticipantsCount = currentEvent.participantsIds.size
        val newMax = editMaxParticipants.toIntOrNull()

        if (newMax != null && newMax < currentParticipantsCount) {
            onError("El límite no puede ser menor que los inscritos actuales ($currentParticipantsCount).")
            return false
        }

        if (editIsPrivate && editPassword.length < 4) {
            onError("La contraseña debe tener al menos 4 caracteres.")
            return false
        }

        return true
    }

    fun saveUpdatedEvent(onSuccess: () -> Unit) {
        val currentEvent = _event.value ?: return
        val userUid = auth.currentUser?.uid ?: return
        isSavingUpdate = true

        viewModelScope.launch {
            // Mapeamos la URL de la imagen automáticamente al cambiar categorías
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

                // Lógica de Notificaciones: Avisar a los participantes del cambio (excluyendo al organizador)
                viewModelScope.launch {
                    try {
                        val idsANotificar = currentEvent.participantsIds.filter { it != userUid }
                        if (idsANotificar.isNotEmpty()) {
                            val tokensList = mutableListOf<String>()

                            for (id in idsANotificar) {
                                val doc = firestore.collection("users").document(id).get().await()
                                val token = doc.getString("fcmToken")
                                if (!token.isNullOrBlank()) {
                                    tokensList.add(token)
                                }
                            }

                            if (tokensList.isNotEmpty()) {
                                com.example.rankup.data.network.FcmSender.enviarNotificacionPush(
                                    context = context,
                                    tokensParticipantes = tokensList,
                                    titulo = "¡Evento actualizado!",
                                    mensaje = "El evento '$editTitle' ha sido modificado por el organizador."
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Fallo silencioso en notificación para no romper la experiencia de guardado
                        android.util.Log.e("NotificationUpdate", "Error enviando push de actualización", e)
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
}

