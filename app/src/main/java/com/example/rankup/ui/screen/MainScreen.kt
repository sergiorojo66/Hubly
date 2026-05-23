package com.example.rankup.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.rankup.ui.createEventScreen.CreateEventScreen
import com.example.rankup.ui.eventDetailScreen.EventDetailScreen
import com.example.rankup.ui.exploreScreen.ExploreScreen
import com.example.rankup.ui.homeScreen.HomeScreen
import com.example.rankup.ui.myEventsScreen.MyEventsScreen // ✨ Importamos tu nueva pantalla
import com.example.rankup.ui.profileScreen.ProfileScreen

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()

    // ✨ Incluimos Screen.MyEvents dejando el botón 'Crear' exactamente en medio
    val items = listOf(Screen.Home, Screen.Explore, Screen.Create, Screen.MyEvents, Screen.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White, // Fondo de la barra limpio
                tonalElevation = 8.dp // Sombra suave para separarla del contenido
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            // Icono y texto cuando están seleccionados
                            selectedIconColor = Color(0xFF6200EE),
                            selectedTextColor = Color(0xFF6200EE),
                            // Color de la "píldora" de fondo detrás del icono seleccionado
                            indicatorColor = Color(0xFF6200EE).copy(alpha = 0.1f),

                            // Icono y texto cuando NO están seleccionados (Gris apagado)
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Explore.route) { ExploreScreen(navController) }
            composable(Screen.Create.route) { CreateEventScreen(navController) }

            // ✨ Registro de la nueva pantalla en el grafo de navegación
            composable(Screen.MyEvents.route) { MyEventsScreen(navController) }

            composable(Screen.Profile.route) { ProfileScreen(navController, onLogout) }

            composable(
                route = Screen.EventDetail.route,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                EventDetailScreen(eventId = eventId, navController = navController)
            }
            composable(
                route = "eventDetail/{eventId}",
                deepLinks = listOf(
                    navDeepLink { uriPattern = "https://rankup.com/event/{eventId}" }
                ),
                arguments = listOf(
                    navArgument("eventId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                EventDetailScreen(eventId = eventId, navController = navController)
            }
        }
    }
}