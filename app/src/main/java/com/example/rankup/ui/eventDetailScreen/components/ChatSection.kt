package com.example.rankup.ui.eventDetailScreen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.rankup.domain.model.ChatMessage
import com.example.rankup.domain.model.User
import com.example.rankup.ui.eventDetailScreen.EventDetailViewModel
import com.example.rankup.ui.profileScreen.UserProfileDialog

@Composable
fun ChatSection(viewModel: EventDetailViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()

    // Estado para el usuario seleccionado
    var selectedUser by remember { mutableStateOf<User?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = messages,
                key = { it.id.ifEmpty { it.timestamp.toString() + it.senderId } }
            ) { msg ->
                val isMine = msg.senderId == viewModel.currentUserId

                ChatBubble(
                    msg = msg,
                    isMine = isMine,
                    onNameClick = {
                        // Aquí disparamos la carga del perfil del usuario
                        // Como solo tienes el ID, usamos la función del ViewModel
                        viewModel.loadUserProfile(msg.senderId) { user ->
                            selectedUser = user
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BARRA DE ENTRADA (CORREGIDA) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Escribe un mensaje...", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                // FORZAMOS EL COLOR NEGRO AQUÍ
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color.Black
                ),
                singleLine = false,
                maxLines = 4
            )

            Spacer(Modifier.width(8.dp))

            // RESTAURADO EL BOTÓN DE ENVIAR
            FloatingActionButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
    selectedUser?.let { user ->
        UserProfileDialog(
            user = user,
            onDismiss = { selectedUser = null }
        )
    }
}

@Composable
fun ChatBubble(
    msg: ChatMessage,
    isMine: Boolean,
    onNameClick: () -> Unit // Callback añadido
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (!isMine) {
            Text(
                text = msg.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 2.dp)
                    .clickable { onNameClick() } // Ejecuta el callback
            )
        }
        Surface(
            color = if (isMine) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 0.dp,
                bottomEnd = if (isMine) 0.dp else 16.dp
            ),
            shadowElevation = 1.dp
        ) {
            Text(
                text = msg.message,
                color = if (isMine) Color.White else Color.Black,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}