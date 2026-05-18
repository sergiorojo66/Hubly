package com.example.rankup.ui.eventDetailScreen

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.rankup.domain.model.Event
import com.example.rankup.domain.model.User
import com.example.rankup.domain.model.enums.EventCategory
import com.example.rankup.ui.authScreen.components.HublyTextField
import com.example.rankup.ui.createEventScreen.components.CategorySelector
import com.example.rankup.ui.createEventScreen.components.CreateEventCard
import com.example.rankup.ui.createEventScreen.components.DatePickerField
import com.example.rankup.ui.createEventScreen.components.ModuleChip
import com.example.rankup.ui.eventDetailScreen.components.ChatSection
import com.example.rankup.ui.eventDetailScreen.components.RankingSection
import com.example.rankup.ui.profileScreen.UserProfileDialog
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
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    val isFinished = event?.isFinished ?: false
    var showFinishDialog by remember { mutableStateOf(false) } // Nuevo estado
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var showParticipantsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

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
            Column(modifier = Modifier.fillMaxSize()) {
                EventHeader(
                    e = e,
                    navController = navController,
                    organizerName = organizerName,
                    isJoined = isJoined,
                    isOrganizer = isOrganizer,
                    onLeaveEvent = { showLeaveDialog = true },
                    onDeleteEvent = { showDeleteDialog = true },
                    onShareEvent = { shareEvent(context, e) },
                    onFinishEvent = { showFinishDialog = true },
                    onOrganizerSelected = { user -> selectedUser = user },
                    onShowParticipants = {
                        viewModel.loadParticipantsProfiles(e.participantsIds)
                        showParticipantsDialog = true
                    },
                    onEditEvent = {
                        viewModel.prepareEditForm(e) // Rellena el formulario con los datos actuales del evento
                        showEditDialog = true
                    },
                    loadUserProfile = { userId, onResult -> viewModel.loadUserProfile(userId, onResult) }
                )
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

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        "info" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(24.dp)
                            ) {
                                InfoSection(e)
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                        "ranking" -> {
                            Box(modifier = Modifier.padding(24.dp)) {
                                if (isJoined) RankingSection(viewModel)
                                else LockedModulePlaceholder("Ranking")
                            }
                        }
                        "chat" -> {
                            if (isJoined) ChatSection(viewModel)
                            else Box(modifier = Modifier.padding(24.dp)) { LockedModulePlaceholder("Chat") }
                        }
                    }
                }
            }

            if (selectedTab == "info" && !isJoined) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    val isFull = (event?.participantsIds?.size ?: 0) >= (event?.maxParticipants ?: Int.MAX_VALUE)

                    Button(
                        onClick = {
                            if (!isFull) {
                                if (event?.isPrivate == true) {
                                    showPasswordDialog = true
                                } else {
                                    event?.let { viewModel.joinEvent(it.id) }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFull) Color(0xFFE57373) else Color(0xFF6200EE),
                            contentColor = Color.White,
                            disabledContainerColor = if (isFinished) Color(0xFF424242) else Color.Gray,
                            disabledContentColor = Color.White
                        ),
                        enabled = !isFull && !isFinished
                    ) {
                        Text(
                            text = when {
                                isFinished -> "Evento finalizado"
                                isFull -> "Evento lleno"
                                else -> "Unirme al evento"
                            },
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
            if (showPasswordDialog) {
                AlertDialog(
                    onDismissRequest = { showPasswordDialog = false },
                    title = { Text("Evento Privado") },
                    text = {
                        Column {
                            Text("Introduce la contraseña para unirte:")
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Contraseña") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (passwordInput == event?.password) {
                                showPasswordDialog = false
                                event?.let { viewModel.joinEvent(it.id) }
                            } else {
                                Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Validar y Unirme", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPasswordDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
            if (showFinishDialog) {
                AlertDialog(
                    onDismissRequest = { showFinishDialog = false },
                    title = { Text("¿Finalizar evento?", color = Color(0xFF4CAF50)) },
                    text = {
                        Text(
                            "Una vez finalizado, nadie más podrá unirse y el ranking se guardará de forma permanente.",
                            color = Color.Black
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showFinishDialog = false
                            event?.let { e ->
                                viewModel.finishEvent(e.id)
                            }
                        }) {
                            Text("Finalizar", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFinishDialog = false }) {
                            Text("Cancelar", color = Color.Gray)
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            if (showParticipantsDialog) {
                ParticipantsListDialog(
                    participants = viewModel.participantsProfiles.value,
                    onUserClick = { user ->
                        showParticipantsDialog = false
                        selectedUser = user
                    },
                    onDismiss = { showParticipantsDialog = false }
                )
            }

            if (showEditDialog) {
                EditEventDialog(
                    viewModel = viewModel,
                    currentEvent = e,
                    onDismiss = { showEditDialog = false }
                )
            }

            selectedUser?.let { user ->
                UserProfileDialog(
                    user = user,
                    onDismiss = { selectedUser = null }
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
    onShareEvent: () -> Unit,
    onFinishEvent: () -> Unit,
    onOrganizerSelected: (User) -> Unit,
    onShowParticipants: () -> Unit,
    onEditEvent: () -> Unit,
    loadUserProfile: (String, (User) -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
        AsyncImage(
            model = e.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

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
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }

            Box {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White)
                }

                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(12.dp))
                ) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
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
                                    tint = Color(0xFF6200EE)
                                )
                            },
                            onClick = {
                                expanded = false
                                onShareEvent()
                            }
                        )

                        if (isJoined && !isOrganizer) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
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
                                text = { Text("Modificar evento", color = Color.Black, fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF6200EE)) },
                                onClick = {
                                    expanded = false
                                    onEditEvent()
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                            DropdownMenuItem(
                                text = { Text("Ver participantes", color = Color.Black, fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF6200EE)) },
                                onClick = {
                                    expanded = false
                                    onShowParticipants()
                                }
                            )
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
                        if (isOrganizer && !e.isFinished) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                            DropdownMenuItem(
                                text = { Text("Finalizar evento", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50)) },
                                onClick = {
                                    expanded = false
                                    onFinishEvent()
                                }
                            )
                        }
                    }
                }
            }
        }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
            Badge(containerColor = Color(0xFF7C4DFF), contentColor = Color.White) {
                Text(e.category, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
            Text(e.title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Text(
                text = "Organizado por $organizerName",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        loadUserProfile(e.organizer) { user ->
                            onOrganizerSelected(user)
                        }
                    }
                    .padding(vertical = 2.dp)
            )
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

@Composable
fun ParticipantsListDialog(
    participants: List<User>,
    onUserClick: (User) -> Unit,
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
                text = "Participantes Inscritos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            if (participants.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cargando participantes...", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(participants) { participant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onUserClick(participant) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = Color(0xFF7C4DFF).copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = participant.initials.uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF7C4DFF),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = participant.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                                Text(
                                    text = participant.username,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun EditEventDialog(
    viewModel: EventDetailViewModel,
    currentEvent: Event,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = Color(0xFFF8F9FA), // Fondo de tu app
        shape = RoundedCornerShape(24.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Modificar Evento",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 1. INFORMACIÓN BÁSICA
                CreateEventCard {
                    Text("Información básica", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp), color = Color.Black)

                    HublyTextField(
                        label = "Título del evento *",
                        value = viewModel.editTitle,
                        placeholder = "Ej: Torneo de Pádel",
                        onValueChange = { viewModel.editTitle = it },
                        singleLine = true
                    )

                    HublyTextField(
                        label = "Descripción",
                        value = viewModel.editDescription,
                        placeholder = "Cuéntanos más...",
                        singleLine = false,
                        onValueChange = { viewModel.editDescription = it }
                    )

                    // Adaptador seguro para el selector Enum de tu CreateScreen anterior
                    val currentEnumCategory = try {
                        EventCategory.valueOf(viewModel.editCategory)
                    } catch (e: Exception) {
                        EventCategory.SPORTS
                    }

                    CategorySelector(
                        selectedCategory = currentEnumCategory,
                        expanded = viewModel.isCategoryMenuExpanded,
                        onExpandedChange = { viewModel.isCategoryMenuExpanded = it },
                        onCategorySelected = { viewModel.editCategory = it.name }
                    )

                    HublyTextField(
                        label = "Ubicación *",
                        value = viewModel.editLocation,
                        placeholder = "Ciudad o lugar",
                        onValueChange = { viewModel.editLocation = it }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            HublyTextField(
                                label = "Máx. part. (Inscritos: ${currentEvent.participantsIds.size})",
                                value = viewModel.editMaxParticipants,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = "Sin limite",
                                onValueChange = { viewModel.editMaxParticipants = it }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            DatePickerField(
                                selectedDate = viewModel.editDate,
                                onDateSelected = { viewModel.editDate = it }
                            )
                        }
                    }
                }

                // 2. PRIVACIDAD Y SEGURIDAD
                CreateEventCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (viewModel.editIsPrivate) Icons.Default.Lock else Icons.Default.Public,
                                contentDescription = null,
                                tint = if (viewModel.editIsPrivate) Color(0xFFFF9800) else Color(0xFF4CAF50),
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = if (viewModel.editIsPrivate) "Evento Privado" else "Evento Público",
                                    color = if (viewModel.editIsPrivate) Color(0xFFFF9800) else Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Switch(
                            checked = viewModel.editIsPrivate,
                            onCheckedChange = { viewModel.editIsPrivate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6D31FF)
                            )
                        )
                    }

                    if (viewModel.editIsPrivate) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HublyTextField(
                            label = "Contraseña de acceso *",
                            value = viewModel.editPassword,
                            placeholder = "Mínimo 4 caracteres",
                            singleLine = true,
                            onValueChange = { viewModel.editPassword = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                    }
                }

                // 3. MÓDULOS OPCIONALES
                CreateEventCard {
                    Text("Módulos opcionales", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp), color = Color.Black)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModuleChip(label = "Info", isSelected = true, enabled = false, onClick = {})
                        ModuleChip(
                            label = "Chat",
                            isSelected = viewModel.editModules.contains("chat"),
                            onClick = { viewModel.toggleEditModule("chat") }
                        )
                        ModuleChip(
                            label = "Ranking",
                            isSelected = viewModel.editModules.contains("ranking"),
                            onClick = { viewModel.toggleEditModule("ranking") }
                        )
                    }
                }

                // 4. ACCIONES DEL DIÁLOGO
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            val isValid = viewModel.validateForm(currentEvent) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                            if (isValid) {
                                viewModel.saveUpdatedEvent { onDismiss() }
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D31FF)),
                        enabled = !viewModel.isSavingUpdate
                    ) {
                        if (viewModel.isSavingUpdate) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    )
}

