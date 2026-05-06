//package com.example.rankup.ui.eventDetailScreen.components
//
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.Send
//import androidx.compose.material3.FloatingActionButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.OutlinedTextFieldDefaults
//import androidx.compose.material3.SegmentedButtonDefaults.Icon
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//import com.example.rankup.ui.eventDetailScreen.EventDetailViewModel
//import com.google.firebase.auth.FirebaseAuth
//
//@Composable
//fun ChatSection(viewModel: EventDetailViewModel) {
//    val messages by viewModel.chatMessages.collectAsState()
//    var messageText by remember { mutableStateOf("") }
//    val scrollState = rememberLazyListState()
//
//    // Scroll automático al recibir nuevos mensajes
//    LaunchedEffect(messages.size) {
//        if (messages.isNotEmpty()) {
//            scrollState.animateScrollToItem(messages.size - 1)
//        }
//    }
//
//    Column(modifier = Modifier.fillMaxSize().height(500.dp)) {
//        // --- LISTA DE MENSAJES ---
//        LazyColumn(
//            state = scrollState,
//            modifier = Modifier.weight(1f).padding(bottom = 8.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            items(messages) { msg ->
//                val isMine = msg.senderId == FirebaseAuth.getInstance().currentUser?.uid
//                ChatBubble(msg, isMine)
//            }
//        }
//
//        // --- BARRA DE ENTRADA (FIGMA) ---
//        Row(
//            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            OutlinedTextField(
//                value = messageText,
//                onValueChange = { messageText = it },
//                placeholder = { Text("Escribe un mensaje...") },
//                modifier = Modifier.weight(1f),
//                shape = RoundedCornerShape(24.dp),
//                colors = OutlinedTextFieldDefaults.colors(
//                    unfocusedContainerColor = Color(0xFFF1F1F1),
//                    focusedContainerColor = Color(0xFFF1F1F1),
//                    unfocusedBorderColor = Color.Transparent,
//                    focusedBorderColor = Color(0xFF6200EE)
//                )
//            )
//            Spacer(Modifier.width(8.dp))
//            FloatingActionButton(
//                onClick = {
//                    if (messageText.isNotBlank()) {
//                        viewModel.sendMessage(messageText)
//                        messageText = ""
//                    }
//                },
//                containerColor = Color(0xFF6200EE),
//                contentColor = Color.White,
//                shape = CircleShape,
//                modifier = Modifier.size(48.dp)
//            ) {
//                Icon(Icons.Default.Send, contentDescription = "Enviar", modifier = Modifier.size(20.dp))
//            }
//        }
//    }
//}
//
//@Composable
//fun ChatBubble(msg: ChatMessage, isMine: Boolean) {
//    Column(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
//    ) {
//        if (!isMine) {
//            Text(msg.senderName, style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
//        }
//        Surface(
//            color = if (isMine) Color(0xFF6200EE) else Color(0xFFE0E0E0),
//            shape = RoundedCornerShape(
//                topStart = 16.dp,
//                topEnd = 16.dp,
//                bottomStart = if (isMine) 16.dp else 0.dp,
//                bottomEnd = if (isMine) 0.dp else 16.dp
//            )
//        ) {
//            Text(
//                text = msg.message,
//                color = if (isMine) Color.White else Color.Black,
//                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
//                style = MaterialTheme.typography.bodyMedium
//            )
//        }
//    }
//}