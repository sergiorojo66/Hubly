package com.example.rankup.ui.homeScreen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.example.rankup.ui.homeScreen.components.CategoryChip
import com.example.rankup.ui.homeScreen.components.FeaturedEventCard
import com.example.rankup.ui.homeScreen.components.StatCard

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF7C4DFF), Color(0xFF6200EE))
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Column {
                    Text(
                        text = "Hubly",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Organiza eventos, conecta personas",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar eventos...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.DarkGray,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        )
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatCard(
                    Icons.Default.TrendingUp,
                    "${stats.activeEvents}",
                    "Eventos",
                    Color(0xFF6200EE),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                StatCard(
                    Icons.Default.Group,
                    stats.participants,
                    "Personas",
                    Color(0xFF3F51B5),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                StatCard(
                    Icons.Default.EmojiEvents,
                    "${stats.competitions}",
                    "Torneos",
                    Color(0xFFFFB300),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "Categorías",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryChip(Icons.Default.EmojiEvents, "Competitivo", Color(0xFFFFF8E1), Color(0xFFFF8F00))
                    CategoryChip(Icons.Default.SportsEsports, "eSports", Color(0xFFF3E5F5), Color(0xFF7B1FA2))
                    CategoryChip(Icons.Default.MusicNote, "Social", Color(0xFFFCE4EC), Color(0xFFC2185B))
                    CategoryChip(Icons.Default.People, "Reuniones", Color(0xFFE3F2FD), Color(0xFF1976D2))
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Eventos destacados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                TextButton(onClick = { navController.navigate("explore") }) {
                    Text("Ver todos", color = Color(0xFF6200EE))
                }
            }
        }

        if (events.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF6200EE))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando eventos...", color = Color.Gray)
                }
            }
        } else {
            val filteredEvents = events.filter { it.title.contains(searchQuery, ignoreCase = true) }

            items(filteredEvents, key = { it.id }) { event ->
                FeaturedEventCard(
                    event = event,
                    onClick = { navController.navigate("event_detail/${event.id}") }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}