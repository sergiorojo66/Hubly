package com.example.rankup.ui.authScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.rankup.domain.model.AuthEvent
import com.example.rankup.ui.authScreen.components.HublyTextField
import com.example.rankup.ui.screen.Screen

@Composable
fun SignupScreen(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {
    val state = viewModel.state
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Signup.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF6D31FF), Color(0xFF4B1EBF))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("H", color = Color(0xFF6D31FF), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text("Hubly", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 12.dp))
            Text("Crea tu cuenta y comienza", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(40.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(32.dp)) {
                    Text("Registro", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                    if (state.error != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                state.error!!,
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HublyTextField(
                        label = "Correo electrónico",
                        value = state.email,
                        icon = Icons.Default.Email,
                        isError = state.emailError != null, // Se activa si hay mensaje
                        errorMessage = state.emailError,
                        onValueChange = { viewModel.onEvent(AuthEvent.EmailChanged(it)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))


                    HublyTextField(
                        label = "Contraseña",
                        value = state.password,
                        placeholder = "······",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = isPasswordVisible,
                        onPasswordToggleClick = { isPasswordVisible = !isPasswordVisible },
                        isError = state.passwordError != null,
                        errorMessage = state.passwordError,
                        onValueChange = { viewModel.onEvent(AuthEvent.PasswordChanged(it)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))


                    HublyTextField(
                        label = "Confirmar contraseña",
                        value = state.confirmPassword,
                        placeholder = "······",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = isPasswordVisible,
                        onPasswordToggleClick = { isPasswordVisible = !isPasswordVisible },
                        isError = state.passwordError != null,
                        errorMessage = state.passwordError,
                        onValueChange = { viewModel.onEvent(AuthEvent.ConfirmPasswordChanged(it)) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.onEvent(AuthEvent.RegisterClicked) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D31FF))
                    ) {
                        if (state.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Crear cuenta", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                        Text("  O continúa con  ", color = Color.Gray, fontSize = 12.sp)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                    }

                    OutlinedButton(
                        onClick = { viewModel.onEvent(AuthEvent.GoogleSignInClicked(context)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Icon(
                            painter = painterResource(id = com.example.rankup.R.drawable.ic_google),
                            contentDescription = "Google Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Continuar con Google", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("¿Ya tienes cuenta? ", color = Color.Gray, fontSize = 14.sp)
                        TextButton(onClick = { navController.navigate(Screen.Login.route) }) {
                            Text("Inicia sesión", color = Color(0xFF6D31FF), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}