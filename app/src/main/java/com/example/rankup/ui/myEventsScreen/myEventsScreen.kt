package com.example.rankup.ui.myEventsScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.rankup.ui.homeScreen.HomeViewModel
import com.example.rankup.ui.homeScreen.UpcomingEventItem
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MyEventsScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mis eventos", "Mis inscripciones")
    var showFinished by remember { mutableStateOf(false) }
    val baseMisEventos = events.filter { it.organizer == currentUserId }
    val baseMisInscripciones = events.filter {
        it.participantsIds.contains(currentUserId) && it.organizer != currentUserId
    }

    val listaFiltrada = if (selectedTabIndex == 0) {
        if (showFinished) baseMisEventos else baseMisEventos.filter { !it.isFinished }
    } else {
        if (showFinished) baseMisInscripciones else baseMisInscripciones.filter { !it.isFinished }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF7C4DFF), Color(0xFF6200EE))))
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Mi Actividad",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = Color(0xFF6200EE),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF6200EE)
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) Color(0xFF6200EE) else Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mostrar finalizados",
                style = MaterialTheme.typography.labelMedium,
                color = if (showFinished) Color(0xFF6200EE) else Color.Gray,
                modifier = Modifier.padding(end = 8.dp)
            )
            Switch(
                checked = showFinished,
                onCheckedChange = { showFinished = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF6200EE),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFFE0E0E0)
                )
            )
        }

        if (listaFiltrada.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedTabIndex == 0) {
                        if (showFinished) "No tienes ningún evento registrado" else "No tienes eventos activos"
                    } else {
                        if (showFinished) "No estás inscrito a ningún evento" else "No tienes inscripciones activas"
                    },
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val listaOrdenada = listaFiltrada.sortedBy { it.isFinished }

                items(listaOrdenada) { event ->
                    UpcomingEventItem(event = event) {
                        navController.navigate("event_detail/${event.id}")
                    }
                }
            }
        }
    }
}