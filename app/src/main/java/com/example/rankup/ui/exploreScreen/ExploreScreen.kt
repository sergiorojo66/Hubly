package com.example.rankup.ui.exploreScreen

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.rankup.domain.model.enums.EventCategory
import com.example.rankup.ui.homeScreen.components.FeaturedEventCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val state = viewModel.state
    var showFilterSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF7C4DFF), Color(0xFF6200EE))
                    )
                )
                .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Column {
                Text(
                    "Explorar eventos",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Buscar por nombre...", color = Color.White.copy(alpha = 0.6f)) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.FilterList, null, tint = Color.White)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("Todos", "Públicos", "Privados")

                    filters.forEach { filter ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable { viewModel.onTypeFilterChanged(filter) },
                            color = if (state.selectedType == filter) Color(0xFF454F63) else Color.White,
                            border = if (state.selectedType == filter) null else BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = filter,
                                    color = if (state.selectedType == filter) Color.White else Color.Black,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.filteredEvents, key = { it.id }) { event ->
                FeaturedEventCard(
                    event = event,
                    onClick = { navController.navigate("event_detail/${event.id}") }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    val sheetState = rememberModalBottomSheetState()

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
        ) {
            AdvancedFilterContent(
                state = state,
                onApply = { loc, start, end, categories ->
                    viewModel.onAdvancedFiltersApplied(loc, start, end, categories)
                    showFilterSheet = false
                },
                onReset = {
                    viewModel.onAdvancedFiltersApplied("", null, null, emptyList())
                    showFilterSheet = false
                }
            )
        }
    }
}

@Composable
fun DatePickerField(
    label: String,
    selectedDate: Long?,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateText = selectedDate?.let {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
    } ?: label

    OutlinedButton(
        onClick = {
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, dayOfMonth)
                    onDateSelected(selectedCalendar.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(dateText, color = if (selectedDate != null) Color.Black else Color.Gray)
            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF6200EE))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedFilterContent(
    state: ExploreState,
    onApply: (String, Long?, Long?, List<EventCategory>) -> Unit,
    onReset: () -> Unit
) {
    var location by remember { mutableStateOf(state.locationFilter) }
    var startDate by remember { mutableStateOf(state.startDateFilter) }
    var endDate by remember { mutableStateOf(state.endDateFilter) }
    var selectedCats by remember { mutableStateOf(state.selectedCategories) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Filtros avanzados", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EventCategory.entries.forEach { category ->
                    val isSelected = selectedCats.contains(category)

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCats = if (isSelected) {
                                selectedCats - category
                            } else {
                                selectedCats + category
                            }
                        },
                        label = { Text(category.name) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6200EE),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color.Gray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) Color.Transparent else Color.LightGray,
                            borderWidth = 1.dp,
                            selectedBorderColor = Color.Transparent,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Ubicación") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Place, null, tint = Color(0xFF6200EE)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                DatePickerField("Desde", startDate) { startDate = it }
            }
            Box(modifier = Modifier.weight(1f)) {
                DatePickerField("Hasta", endDate) { endDate = it }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                Text("Limpiar", color = Color.Gray)
            }
            Button(
                onClick = { onApply(location, startDate, endDate, selectedCats) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                Text("Aplicar", color = Color.White)
            }
        }
    }
}