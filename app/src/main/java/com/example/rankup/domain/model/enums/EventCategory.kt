package com.example.rankup.domain.model.enums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.ui.graphics.vector.ImageVector

enum class EventCategory(val displayName: String, val icon: ImageVector) {
    SPORTS("Deporte", Icons.Default.SportsBasketball),
    SOCIAL("Social", Icons.Default.Groups),
    ESPORTS("eSports", Icons.Default.VideogameAsset),
    EDUCATION("Educación", Icons.Default.School),
    OTHER("Otro", Icons.Default.Category)
}