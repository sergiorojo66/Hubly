package com.example.rankup.ui.eventDetailScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rankup.domain.model.RankingUser
import com.example.rankup.ui.eventDetailScreen.EventDetailViewModel

@Composable
fun RankingSection(viewModel: EventDetailViewModel) {
    val rankings by viewModel.rankings.collectAsState()

    if (rankings.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
            Text("Aún no hay puntuaciones en este evento", color = Color.Gray)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Clasificación actual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            rankings.forEach { user ->
                RankingUserRow(user)
            }
        }
    }
}

@Composable
fun RankingUserRow(user: RankingUser) {
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
            // Posición o Medalla
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                when (user.position) {
                    1 -> Text("🥇", fontSize = 24.sp)
                    2 -> Text("🥈", fontSize = 24.sp)
                    3 -> Text("🥉", fontSize = 24.sp)
                    else -> Text(
                        "#${user.position}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Info Usuario
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.userName.ifEmpty { "Usuario Anónimo" },
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    "Nivel ${user.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Puntos
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${user.points} pts",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF6200EE),
                    fontSize = 18.sp
                )
            }
        }
    }
}