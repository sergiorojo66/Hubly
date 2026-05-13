package com.example.rankup.ui.eventDetailScreen

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.rankup.domain.model.Event
import com.example.rankup.ui.eventDetailScreen.components.ChatSection
import com.example.rankup.ui.eventDetailScreen.components.RankingSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventDetailScreen(
    eventId: String,
    navController: NavController,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val event by viewModel.event.collectAsState()
    var selectedTab by remember { mutableStateOf("info") }
    val isJoined by viewModel.isUserJoined.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    val currentUserId = viewModel.currentUserId
    val isOrganizer = event?.organizer == currentUserId
    val errorMessage by viewModel.error.collectAsState()
    val context = LocalContext.current
    val organizerName by viewModel.organizerName.collectAsState()

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(isJoined) {
        if (!isJoined && selectedTab != "info") {
            selectedTab = "info"
        }
    }

    event?.let { e ->
        val isFull = (e.participantsIds.size >= (e.maxParticipants ?: Int.MAX_VALUE))

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
            // USAMOS COLUMN SIN SCROLL GLOBAL PARA QUE EL CHAT FUNCIONE
            Column(modifier = Modifier.fillMaxSize()) {

                // --- 1. CABECERA (ESTÁTICA) ---
                EventHeader(
                    e = e,
                    navController = navController,
                    organizerName = organizerName,
                    isJoined = isJoined,
                    isOrganizer = isOrganizer,
                    onLeaveEvent = { showLeaveDialog = true }, // Cambiado: solo abre el diálogo
                    onDeleteEvent = { showDeleteDialog = true }, // Cambiado: solo abre el diálogo
                    onShareEvent = { shareEvent(context, e) } // <--- Aquí pasas el context y el evento
                )
                // --- 2. TABS (ESTÁTICAS) ---
                ScrollableTabRow(
                    selectedTabIndex = getTabIndex(selectedTab, e.modules),
                    containerColor = Color.White,
                    contentColor = Color(0xFF6200EE),
                    edgePadding = 24.dp,
                    divider = {}
                ) {
                    TabItem("info", "Info", selectedTab, isLocked = false) { selectedTab = "info" }

                    if (e.modules.contains("chat")) {
                        TabItem("chat", "Chat", selectedTab, isLocked = !isJoined) { selectedTab = "chat" }
                    }

                    if (e.modules.contains("ranking")) {
                        TabItem("ranking", "Ranking", selectedTab, isLocked = !isJoined) { selectedTab = "ranking" }
                    }
                }

                // --- 3. CONTENIDO DINÁMICO (USA WEIGHT PARA RELLENAR ESPACIO) ---
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        "info" -> {
                            // SOLO LA INFO TIENE SCROLL PROPIO
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(24.dp)
                            ) {
                                InfoSection(e)
                                Spacer(modifier = Modifier.height(100.dp)) // Espacio para el botón flotante
                            }
                        }
                        "ranking" -> {
                            Box(modifier = Modifier.padding(24.dp)) {
                                if (isJoined) RankingSection(viewModel)
                                else LockedModulePlaceholder("Ranking")
                            }
                        }
                        "chat" -> {
                            // EL CHAT NO LLEVA PADDING EXTRA PARA AJUSTARSE A LOS BORDES
                            if (isJoined) ChatSection(viewModel)
                            else Box(modifier = Modifier.padding(24.dp)) { LockedModulePlaceholder("Chat") }
                        }
                    }
                }
            }

            if (selectedTab == "info" && !isJoined) { // <--- Condición actualizada
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    val isFull = (event?.participantsIds?.size ?: 0) >= (event?.maxParticipants ?: Int.MAX_VALUE)

                    Button(
                        onClick = { if (!isFull) event?.let { viewModel.joinEvent(it.id) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFull) Color(0xFFE57373) else Color(0xFF6200EE),
                            disabledContainerColor = Color.Gray
                        ),
                        enabled = !isFull
                    ) {
                        Text(
                            text = if (isFull) "Evento lleno" else "Unirme al evento",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("¿Eliminar evento?", color = Color.Red) },
                    text = { Text("Esta acción no se puede deshacer. Se borrarán todos los datos y rankings asociados.", color = Color.Black) },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            event?.let { e ->
                                viewModel.deleteEvent(e.id) { navController.popBackStack() }
                            }
                        }) {
                            Text("Eliminar", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancelar", color = Color.Gray)
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // --- DIÁLOGO DE ANULAR INSCRIPCIÓN ---
            if (showLeaveDialog) {
                AlertDialog(
                    onDismissRequest = { showLeaveDialog = false },
                    title = { Text("Anular inscripción", color = Color.Red) },
                    text = { Text("¿Estás seguro de que quieres salirte de este evento? Perderás tu posición en el ranking.", color = Color.Black) },
                    confirmButton = {
                        TextButton(onClick = {
                            showLeaveDialog = false
                            event?.let { viewModel.leaveEvent(it.id) }
                        }) {
                            Text("Confirmar", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLeaveDialog = false }) {
                            Text("Volver", color = Color.Gray)
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun EventHeader(
    e: Event,
    navController: NavController,
    organizerName: String,
    isJoined: Boolean,
    isOrganizer: Boolean,
    onLeaveEvent: () -> Unit,
    onDeleteEvent: () -> Unit,
    onShareEvent: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
        AsyncImage(
            model = e.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Degradado más oscuro en la parte superior para que los botones blancos resalten
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                    startY = 0f
                )
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Atrás
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }

            // ... dentro de EventHeader

            Box {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                }

                // Usamos MaterialTheme envolviendo el menú para quitar esas esquinas feas
                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(12.dp))
                ) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                        // Al definir el background aquí dentro del shape, se eliminan los bordes negros
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Compartir evento",
                                    color = Color.Black, // Texto ahora en negro
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color(0xFF6200EE) // Mantenemos el lila para darle vida
                                )
                            },
                            onClick = {
                                expanded = false
                                onShareEvent()
                            }
                        )

                        if (isJoined && !isOrganizer) {
                            DropdownMenuItem(
                                text = { Text("Anular inscripción", color = Color(0xFFE53935)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935)
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onLeaveEvent()
                                }
                            )
                        }

                        if (isOrganizer) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Eliminar evento",
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onDeleteEvent()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Info del evento en la parte inferior (igual que antes)
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
            Badge(containerColor = Color(0xFF7C4DFF), contentColor = Color.White) {
                Text(e.category, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
            Text(e.title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Organizado por $organizerName", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun formatLongToDate(time: Long): String {
    val date = Date(time)
    val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return format.format(date)
}
@Composable
fun InfoSection(event: Event) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard {
            InfoRow(Icons.Default.CalendarToday, "Fecha", formatLongToDate(event.date))
            InfoRow(Icons.Default.LocationOn, "Ubicación", event.location)
            InfoRow(
                icon = Icons.Default.Group,
                label = "Participantes",
                value = "${event.participantsIds.size}/${event.maxParticipants ?: "∞"}"
            )

            InfoRow(
                icon = if (event.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                label = "Visibilidad",
                value = if (event.isPrivate) "Privado" else "Público"
            )
        }

        InfoCard {
            Text("Descripción", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.Black)
            Text(event.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.Gray)
        }

    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF6200EE), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Black)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.Gray)
        }
    }
}

@Composable
fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) { content() }
    }
}

@Composable
fun TabItem(
    id: String,
    label: String,
    selectedId: String,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Tab(
        selected = id == selectedId,
        onClick = onClick,
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontWeight = if(id == selectedId) FontWeight.Bold else FontWeight.Normal,
                    color = if (isLocked && id != "info") Color.Gray else Color.Unspecified
                )
                if (isLocked && id != "info") {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                }
            }
        }
    )
}

fun getTabIndex(selected: String, modules: List<String>): Int {
    return when(selected) {
        "info" -> 0
        "chat" -> if (modules.contains("chat")) 1 else 0
        "ranking" -> {
            var index = 1
            if (modules.contains("chat")) index++
            if (modules.contains("ranking")) index else 0
        }
        else -> 0
    }
}

@Composable
fun LockedModulePlaceholder(moduleName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFF3E5F5),
            modifier = Modifier.size(80.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF6200EE),
                modifier = Modifier.padding(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Módulo de $moduleName bloqueado",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Debes unirte al evento para participar",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

fun shareEvent(context: android.content.Context, event: Event) {
    // Creamos la URL con el ID del evento
    val eventLink = "https://rankup.com/event/${event.id}"

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT,
            "¡Únete a mi evento en Hubly!\n\n" +
                    "📌 ${event.title}\n" +
                    "🔗 Haz clic aquí para ver los detalles: $eventLink"
        )
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Compartir evento")
    context.startActivity(shareIntent)
}

