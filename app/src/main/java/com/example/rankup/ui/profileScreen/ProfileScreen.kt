package com.example.rankup.ui.profileScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.model.User
import com.example.rankup.ui.eventDetailScreen.EventDetailViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user = viewModel.userState

    // Estados para diálogos y edición
    var showDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var tempBio by remember { mutableStateOf("") }

    val eventHistory by viewModel.eventHistory.collectAsState()
    var showHistoryDialog by remember { mutableStateOf(false) }

    // ✨ Estados para el menú de Ajustes (Engranaje)
    var showSettingsDialog by remember { mutableStateOf(false) }
    var notificationsSilenced by remember { mutableStateOf(false) }
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        viewModel.loadEventHistory()
    }

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF6200EE))
        }
    } else {
        // --- DIÁLOGO: EDITAR PERFIL ---
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Editar Perfil", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("Nombre real") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = tempBio,
                            onValueChange = { if (it.length <= 150) tempBio = it },
                            label = { Text("Sobre mí") },
                            placeholder = { Text("Cuéntanos algo sobre ti...") },
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            maxLines = 4,
                            supportingText = {
                                Text(
                                    text = "${tempBio.length} / 150",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateProfile(tempName, tempBio)
                        showDialog = false
                    }) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }

        // --- ✨ DIÁLOGO: AJUSTES (Engranaje) ---
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Ajustes de la cuenta", color = Color.Black, fontWeight = FontWeight.Bold) },
                containerColor = Color.White,
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Switch de Notificaciones
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (notificationsSilenced) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF6200EE)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Silenciar notificaciones", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                            }
                            Switch(
                                checked = notificationsSilenced,
                                onCheckedChange = { notificationsSilenced = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFF6200EE)
                                )
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

                        // Botón de Cerrar Sesión integrado aquí dentro
                        TextButton(
                            onClick = {
                                showSettingsDialog = false
                                onLogout()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Aceptar", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE))
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
        ) {
            // --- CABECERA MORADA ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF7C4DFF), Color(0xFF6200EE))))
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Perfil", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                        // Botón del Engranaje (Ajustes)
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            border = BorderStroke(2.dp, Color.White)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = user.initials, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            }
                        }

                        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                            Text(text = user.displayName, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = user.username, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                Text(" ${user.rating} valoración", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Button(
                            onClick = {
                                tempName = user.displayName
                                tempBio = user.bio
                                showDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Text(" Editar", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    if (user.bio.isNotEmpty()) {
                        Text(
                            text = user.bio,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 20.dp),
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // --- CUERPO DE LA PANTALLA ---
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ✨ 1. SECCIÓN DE ESTADÍSTICAS DEL USUARIO (Justo después de la cabecera)
                Text(
                    text = "Mis Estadísticas",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(value = "${eventHistory.size}", label = "Eventos")
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFE0E0E0)))
                        StatItem(value = "${user.rating} ★", label = "Reputación")
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFE0E0E0)))
                        StatItem(
                            value = when {
                                eventHistory.any { it.organizer == currentUserId } -> "Organizador"
                                eventHistory.any { it.participantsIds.contains(currentUserId) } -> "Participante"
                                else -> "Inactivo"
                            },
                            label = "Rol"
                        )
                    }
                }

                Text(
                    text = "Actividad",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                ProfileMenuButton(Icons.Default.History, "Historial de Eventos") {
                    showHistoryDialog = true
                }
            }
        }
    }

    if (showHistoryDialog) {
        EventHistoryDialog(events = eventHistory, onDismiss = { showHistoryDialog = false })
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF6200EE)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun ProfileMenuButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color(0xFF6200EE), modifier = Modifier.size(24.dp))
            Text(label, modifier = Modifier.padding(start = 16.dp).weight(1f), fontWeight = FontWeight.SemiBold, color = Color.Black)
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
fun UserProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    var isRatingMode by remember { mutableStateOf(false) }
    var selectedRating by remember { mutableStateOf(0) }
    var isSavingRating by remember { mutableStateOf(false) } // Para evitar doble clic al guardar
    val currentUserId = viewModel.currentUserId

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar usando las iniciales de tu modelo
                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = CircleShape,
                    color = Color(0xFF7C4DFF)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.initials.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Fila de Nombre Público + Botón de Valorar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    if (currentUserId != user.id) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { isRatingMode = !isRatingMode },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Valorar usuario",
                                tint = if (isRatingMode) Color(0xFFFFB300) else Color.Gray
                            )
                        }
                    }
                }

                // Nombre único (username)
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6200EE),
                    fontWeight = FontWeight.Medium
                )

                // Puntuación actual
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                    Text(" ${user.rating} reputación global", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PANEL INTERACTIVO DE VALORACIÓN
                if (isRatingMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Modificar/Añadir puntuación",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6200EE)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Selector interactivo de 5 estrellas
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..5).forEach { index ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (index <= selectedRating) Color(0xFFFFB300) else Color.Gray.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable { selectedRating = index } // Cambia el estado visual
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (selectedRating > 0 && !isSavingRating) {
                                        isSavingRating = true // Bloqueamos el botón inmediatamente
                                        viewModel.rateUser(user.id, selectedRating.toDouble()) {
                                            isRatingMode = false
                                            isSavingRating = false
                                            onDismiss() // Cerramos para refrescar la vista
                                        }
                                    }
                                },
                                enabled = selectedRating > 0 && !isSavingRating, // Se deshabilita durante la subida
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                if (isSavingRating) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Confirmar Puntuación", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Bio del usuario
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}
@Composable
fun EventHistoryDialog(
    events: List<Event>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color(0xFF6200EE), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Historial de Eventos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No has participado en ningún evento aún.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                // Ordenamos por fecha descendente de forma cronológica
                val sortedEvents = remember(events) { events.sortedByDescending { it.date } }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp), // Ajuste dinámico con scroll si hay muchos
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedEvents) { event ->
                        HistoryEventCard(event = event)
                    }
                }
            }
        }
    )
}

@Composable
fun HistoryEventCard(event: Event) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Imagen de fondo del evento
            AsyncImage(
                model = event.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Degradado oscuro para que los textos blancos resalten perfectamente
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Black.copy(alpha = 0.3f))
                        )
                    )
            )

            // Contenido de la tarjeta estilo FeaturedCard
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Título y Categoría
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = event.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = event.category,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Mini Etiqueta de Estado (En curso / Finalizado)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (event.isFinished) Color(0xFFE57373) else Color(0xFF81C784),
                        contentColor = Color.White
                    ) {
                        Text(
                            text = if (event.isFinished) "Finalizado" else "En curso",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Fila inferior: Fecha y participantes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(event.date)),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "👥 ${event.participantsIds.size}/${event.maxParticipants ?: "∞"}",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}