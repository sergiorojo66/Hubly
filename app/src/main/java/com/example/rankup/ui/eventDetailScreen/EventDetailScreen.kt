package com.example.rankup.ui.eventDetailScreen

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
//import com.example.rankup.ui.eventDetailScreen.components.ChatSection
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
    val errorMessage by viewModel.error.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    event?.let { e ->
        val isFull = (e.participantsIds.size >= (e.maxParticipants ?: Int.MAX_VALUE))

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    AsyncImage(
                        model = e.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.9f)) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                            }
                        }
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.9f)) {
                            IconButton(onClick = { /* Share */ }) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                            }
                        }
                    }

                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                        Badge(containerColor = Color(0xFF7C4DFF), contentColor = Color.White) {
                            Text(e.category, modifier = Modifier.padding(4.dp))
                        }
                        Text(e.title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Organizado por USER${e.organizer.take(10)}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = getTabIndex(selectedTab, e.modules),
                    containerColor = Color.White,
                    contentColor = Color(0xFF6200EE),
                    edgePadding = 24.dp,
                    divider = {}
                ) {
                    TabItem("info", "Info", selectedTab, isLocked = false) {
                        selectedTab = "info"
                    }

                    if (e.modules.contains("chat")) {
                        TabItem(
                            id = "chat",
                            label = "Chat",
                            selectedId = selectedTab,
                            isLocked = !isJoined
                        ) {
                            selectedTab = "chat"
                        }
                    }

                    if (e.modules.contains("ranking")) {
                        TabItem(
                            id = "ranking",
                            label = "Ranking",
                            selectedId = selectedTab,
                            isLocked = !isJoined
                        ) {
                            selectedTab = "ranking"
                        }
                    }
                }

                val isJoined by viewModel.isUserJoined.collectAsState()

                Box(modifier = Modifier.padding(24.dp)) {
                    when (selectedTab) {
                        "info" -> InfoSection(e)
                        "ranking" -> {
                            if (isJoined) RankingSection(viewModel)
                            else LockedModulePlaceholder("Ranking")
                        }
//                        "chat" -> {
//                            if (isJoined) ChatSection(viewModel)
//                            else LockedModulePlaceholder("Chat")
//                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Button(
                    onClick = {
                        if (!isJoined && !isFull) viewModel.joinEvent(e.id)
                    },
                    modifier = Modifier.fillMaxWidth().padding(20.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isJoined -> Color.Gray
                            isFull -> Color(0xFFE57373)
                            else -> Color(0xFF6200EE)
                        },
                        disabledContainerColor = Color.Gray
                    ),
                    enabled = !isJoined && !isFull
                ) {
                    Text(
                        text = when {
                            isJoined -> "Ya estás inscrito"
                            isFull -> "Evento lleno"
                            else -> "Unirme al evento"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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

