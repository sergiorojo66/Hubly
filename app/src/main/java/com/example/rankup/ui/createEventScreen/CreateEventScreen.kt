package com.example.rankup.ui.createEventScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.rankup.domain.model.enums.EventCategory
import com.example.rankup.ui.authScreen.components.HublyTextField
import com.example.rankup.ui.createEventScreen.components.CategorySelector
import com.example.rankup.ui.createEventScreen.components.CreateEventCard
import com.example.rankup.ui.createEventScreen.components.DatePickerField
import com.example.rankup.ui.createEventScreen.components.ModuleChip

@Composable
fun CreateEventScreen(
    navController: NavController,
    viewModel: CreateEventViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFF6D31FF), Color(0xFF4B1EBF))))
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Column {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text("Crear evento", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Configura tu evento personalizado", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CreateEventCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Previsualización del evento", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp), color = Black)

                    AsyncImage(
                        model = when (state.category) { // Misma lógica que el ViewModel
                            EventCategory.SPORTS -> "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=500"
                            EventCategory.SOCIAL -> "https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=500"
                            EventCategory.ESPORTS -> "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500"
                            EventCategory.EDUCATION -> "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?q=80&w=1000"
                            else -> "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=500"
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        "La imagen se asigna según la categoría",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            CreateEventCard {
                Text("Información básica", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp), color = Black)

                HublyTextField(
                    label = "Título del evento *",
                    value = state.title,
                    placeholder = "Ej: Torneo de Pádel",
                    onValueChange = { viewModel.onTitleChange(it) },
                    singleLine = true
                )

                HublyTextField(
                    label = "Descripción",
                    value = state.description,
                    placeholder = "Cuéntanos más...",
                    singleLine = false,
                    onValueChange = { viewModel.onDescriptionChange(it) }
                )

                CategorySelector(
                    selectedCategory = state.category,
                    expanded = state.isCategoryMenuExpanded,
                    onExpandedChange = { viewModel.toggleCategoryMenu(it) },
                    onCategorySelected = { viewModel.onCategoryChange(it) }
                )

                HublyTextField(
                    label = "Ubicación *",
                    value = state.location,
                    placeholder = "Ciudad o lugar",
                    onValueChange = { viewModel.onLocationChange(it) }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        HublyTextField(
                            label = "Máx. part.",
                            value = state.maxParticipants,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Muestra teclado numérico,
                            placeholder = "Sin limite",
                            onValueChange = { viewModel.onMaxParticipantsChange(it) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DatePickerField(
                            selectedDate = state.date,
                            onDateSelected = { viewModel.onDateChange(it) }
                        )
                    }
                }
            }

            CreateEventCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (state.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                            contentDescription = null,
                            tint = if (state.isPrivate) Color(0xFFFF9800) else Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )

                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = if (state.isPrivate) "Evento Privado" else "Evento Público",
                                color = if (state.isPrivate) Color(0xFFFF9800) else Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (state.isPrivate) "Solo invitados con enlace" else "Visible para todos en la comunidad",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Switch(
                        checked = state.isPrivate,
                        onCheckedChange = { viewModel.togglePrivacy(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6D31FF)
                        )
                    )
                }
            }

            CreateEventCard {
                Text(
                    "Módulos opcionales",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp),
                    color = Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // INFO siempre activo
                    ModuleChip(
                        label = "Info",
                        isSelected = true,
                        enabled = false,
                        onClick = {}
                    )

                    ModuleChip(
                        label = "Chat",
                        isSelected = state.selectedModules.contains("chat"),
                        onClick = { viewModel.toggleModule("chat") }
                    )

                    ModuleChip(
                        label = "Ranking",
                        isSelected = state.selectedModules.contains("ranking"),
                        onClick = { viewModel.toggleModule("ranking") }
                    )
                }
                Text(
                    "Añade funciones extra a tu evento",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (viewModel.isFormValid) {
                        viewModel.createEvent { navController.popBackStack() }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = viewModel.isFormValid && !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6D31FF),
                    disabledContainerColor = Color.LightGray
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Crear evento",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.isFormValid) Color.White else Color.DarkGray
                    )
                }
            }

        }
    }
}