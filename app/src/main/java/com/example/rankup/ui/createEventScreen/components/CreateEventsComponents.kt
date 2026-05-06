package com.example.rankup.ui.createEventScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rankup.domain.model.enums.EventCategory
import com.example.rankup.ui.authScreen.components.HublyTextField
import com.example.rankup.ui.createEventScreen.CreateEventViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreateEventCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

@Composable
fun ModuleSwitchRow(
    label: String,
    description: String,
    icon: ImageVector,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isActive) Color(0xFFEDE7F6) else Color(0xFFE0E0E0),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                icon, null,
                tint = if (isActive) Color(0xFF6D31FF) else Color.Gray,
                modifier = Modifier.padding(8.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(description, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(checked = isActive, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    selectedCategory: EventCategory,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCategorySelected: (EventCategory) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.menuAnchor()) {
            HublyTextField(
                label = "Categoría *",
                value = selectedCategory.displayName,
                placeholder = "Selecciona una categoría",
                icon = selectedCategory.icon,
                readOnly = true,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { onExpandedChange(!expanded) }
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(Color.White)
        ) {
            EventCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(category.icon, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                            Spacer(Modifier.width(12.dp))
                            Text(category.displayName, color = Color.Black)
                        }
                    },
                    onClick = {
                        onCategorySelected(category)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    var showModal by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
    )

    val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateText = formatter.format(Date(selectedDate))

    Box {
        HublyTextField(
            label = "Fecha *",
            value = dateText,
            placeholder = "Selecciona el día",
            icon = Icons.Default.CalendarToday,
            readOnly = true,
            onValueChange = { }
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showModal = true }
        )
    }

    if (showModal) {
        DatePickerDialog(
            onDismissRequest = { showModal = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(it)
                    }
                    showModal = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showModal = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun ModuleSelectionSection(viewModel: CreateEventViewModel) {
    val state = viewModel.state

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            "Módulos del evento",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Módulo Info (Deshabilitado porque es obligatorio)
            ModuleChip(
                label = "Info",
                isSelected = true,
                enabled = false,
                onClick = {}
            )

            // Módulo Chat
            ModuleChip(
                label = "Chat",
                isSelected = state.selectedModules.contains("chat"),
                onClick = { viewModel.toggleModule("chat") }
            )

            // Módulo Ranking
            ModuleChip(
                label = "Ranking",
                isSelected = state.selectedModules.contains("ranking"),
                onClick = { viewModel.toggleModule("ranking") }
            )
        }
    }
}

@Composable
fun ModuleChip(
    label: String,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        },
        enabled = enabled,
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            // Estado seleccionado: Morado vibrante con texto blanco puro
            selectedContainerColor = Color(0xFF6D31FF),
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White,
            // Estado no seleccionado: Fondo lila muy suave (no blanco total) para que destaque
            containerColor = Color(0xFFF3F0FF),
            labelColor = Color(0xFF6D31FF), // Texto morado para que se lea bien
            // Estado deshabilitado (Info)
            disabledContainerColor = Color(0xFFF1F1F1),
            disabledLabelColor = Color.LightGray
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (isSelected) Color.Transparent else Color(0xFFD1C4E9),
            borderWidth = 1.dp,
            selectedBorderColor = Color(0xFF4B1EBF),
            enabled = enabled,
            selected = isSelected
        ),
        modifier = Modifier.height(40.dp)
    )
}
