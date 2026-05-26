package com.example.rankup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rankup.ui.authScreen.LoginScreen
import com.example.rankup.ui.authScreen.SignupScreen
import com.example.rankup.ui.screen.MainScreen
import com.example.rankup.ui.screen.Screen
import com.example.rankup.ui.theme.RankUpTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verificarYPedirPermisos()
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
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
        } else {
        }
    }

    fun verificarYPedirPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun RankUpNavGraph() {
    val navController = rememberNavController()
    val currentUser = FirebaseAuth.getInstance().currentUser

    val startDest = if (currentUser != null) {
        registrarTokenDispositivo(currentUser.uid)
        Screen.Main.route
    } else {
        Screen.Login.route
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Signup.route) { SignupScreen(navController) }

        composable(Screen.Main.route) {
            MainScreen(onLogout = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
                FirebaseAuth.getInstance().signOut()
            })
        }
    }
}

fun registrarTokenDispositivo(currentUserId: String) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val token = task.result
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .update("fcmToken", token)
        }
    }
}