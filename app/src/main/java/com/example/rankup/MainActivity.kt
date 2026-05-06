package com.example.rankup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rankup.ui.authScreen.LoginScreen
import com.example.rankup.ui.authScreen.SignupScreen
import com.example.rankup.ui.screen.MainScreen
import com.example.rankup.ui.screen.Screen
import com.example.rankup.ui.theme.RankUpTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RankUpTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RankUpNavGraph()
                }
            }
        }
    }
}

@Composable
fun RankUpNavGraph() {
    val navController = rememberNavController()
    val currentUser = FirebaseAuth.getInstance().currentUser

    val startDest = if (currentUser != null) Screen.Main.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDest) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Signup.route) { SignupScreen(navController) }

        composable(Screen.Main.route) {
            MainScreen(onLogout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
    }
}