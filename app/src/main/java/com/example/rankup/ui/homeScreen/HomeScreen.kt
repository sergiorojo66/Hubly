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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.rankup.domain.model.Event
import com.example.rankup.ui.homeScreen.components.FeaturedEventCard
import com.example.rankup.ui.homeScreen.components.formatDate

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val activeEvents = events.filter { !it.isFinished && it.title.contains(searchQuery, ignoreCase = true) }
    val upcomingEvents = activeEvents.takeLast(5)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF7C4DFF), Color(0xFF6200EE))))
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Column {
                        Text("Hubly", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("Descubre y compite", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(50.dp), // Un poco más fina
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF6200EE), modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.fillMaxSize(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SmallStat(Icons.Default.TrendingUp, "${stats.activeEvents}", "Eventos")

                    VerticalDivider(
                        modifier = Modifier.height(30.dp).align(Alignment.CenterVertically),
                        thickness = 1.dp,
                        color = Color(0xFFEEEEEE)
                    )

                    SmallStat(Icons.Default.Group, stats.participants, "Hublers")
                }
            }
        }

        item {
            SectionHeader("Ahora mismo", "Ver más") { navController.navigate("explore") }
        }

        item {
            if (activeEvents.isEmpty()) {
                EmptyState()
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(start = 24.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    activeEvents.take(3).forEach { event ->
                        FeaturedEventCard(
                            event = event,
                            modifier = Modifier.width(310.dp),
                            onClick = { navController.navigate("event_detail/${event.id}") }
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                }
            }
        }

        item {
            SectionHeader("Próximamente", "") {}
        }

        items(upcomingEvents) { event ->
            UpcomingEventItem(event) { navController.navigate("event_detail/${event.id}") }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun SmallStat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Color(0xFF6200EE), modifier = Modifier.size(20.dp))
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.Black)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun SectionHeader(title: String, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 32.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
        if (actionText.isNotEmpty()) {
            TextButton(onClick = onAction) { Text(actionText, color = Color(0xFF6200EE)) }
        }
    }
}

@Composable
fun UpcomingEventItem(event: Event, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(50.dp).background(Color(0xFFF3E5F5), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF6200EE))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(event.title, fontWeight = FontWeight.Bold, maxLines = 1, color = Color.Black)
                Row{
                    Text(event.location, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(", ${formatDate(event.date)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No se encontraron eventos", color = Color.Gray)
    }
}