package com.example.rankup.ui.eventDetailScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rankup.domain.model.RankingUser
import com.example.rankup.ui.eventDetailScreen.EventDetailViewModel

@Composable
fun RankingSection(viewModel: EventDetailViewModel) {
    val rankings by viewModel.rankings.collectAsState()
    val event by viewModel.event.collectAsState()

    val currentUserId = viewModel.currentUserId?.trim() ?: ""
    val organizerId = event?.organizer?.trim() ?: ""
    val isOrganizer = currentUserId.isNotEmpty() && currentUserId == organizerId

    var showScoreDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<RankingUser?>(null) }
    var scoreInput by remember { mutableStateOf("") }

    // Diálogo (se mantiene igual)
    if (showScoreDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showScoreDialog = false },
            title = { Text("Asignar puntos a ${selectedUser?.userName}") },
            text = {
                OutlinedTextField(
                    value = scoreInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) scoreInput = it },
                    label = { Text("Puntos extra") },
                    placeholder = { Text("Ej: 50") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val points = scoreInput.toIntOrNull() ?: 0
                    viewModel.updateUserPoints(selectedUser!!.id, points)
                    showScoreDialog = false
                    scoreInput = ""
                }) {
                    Text("Sumar Puntos", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScoreDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    if (rankings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
            Text("Aún no hay puntuaciones en este evento", color = Color.Gray)
        }
    } else {
        // CAMBIO CLAVE: Usamos LazyColumn para permitir scroll
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // Añadimos padding inferior para que el último elemento no lo tape el menú
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 100.dp
            )
        ) {
            item {
                Text(
                    "Clasificación actual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Renderizado eficiente de la lista
            items(rankings) { user ->
                RankingUserRow(
                    user = user,
                    isOrganizer = isOrganizer,
                    onAddScoreClick = {
                        selectedUser = user
                        showScoreDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun RankingUserRow(
    user: RankingUser,
    isOrganizer: Boolean,
    onAddScoreClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Posición (mismo código que tenías)
            // Dentro de RankingUserRow
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                when (user.position) {
                    1 -> Text("🥇", fontSize = 24.sp)
                    2 -> Text("🥈", fontSize = 24.sp)
                    3 -> Text("🥉", fontSize = 24.sp)
                    else -> Text(
                        text = "#${user.position}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Info Usuario
            Column(modifier = Modifier.weight(1f)) {
                Text(user.userName.ifEmpty { "Usuario Anónimo" }, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("Nivel ${user.level}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            // Puntos y botón de edición
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${user.points} pts",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF6200EE),
                    fontSize = 18.sp
                )

                if (isOrganizer) {
                    IconButton(onClick = onAddScoreClick) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Añadir puntos",
                            tint = Color(0xFF6200EE)
                        )
                    }
                }
            }
        }
    }
}