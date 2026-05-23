package com.example.rankup.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String = "", val icon: ImageVector = Icons.Default.Home) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Main : Screen("main")
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Explore : Screen("explore", "Explorar", Icons.Default.Explore)
    object Create : Screen("create", "Crear", Icons.Default.AddCircle)
    object MyEvents : Screen("my_events", "Actividad", Icons.Default.Event) // ✨ Nueva pestaña
    object Profile : Screen("profile", "Perfil", Icons.Default.Person)
    object EventDetail : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: String) = "event_detail/$eventId"
    }
}