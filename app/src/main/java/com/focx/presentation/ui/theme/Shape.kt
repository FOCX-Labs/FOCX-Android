package com.focx.presentation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Tech-inspired Shape System
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Custom Shapes
object TechShapes {
    val card = RoundedCornerShape(12.dp)
    val button = RoundedCornerShape(8.dp)
    val chip = RoundedCornerShape(16.dp)
    val bottomSheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val dialog = RoundedCornerShape(16.dp)
    val searchBar = RoundedCornerShape(24.dp)
    val productImage = RoundedCornerShape(8.dp)
    val categoryCard = RoundedCornerShape(12.dp)
    val filterChip = RoundedCornerShape(20.dp)
    val statusBadge = RoundedCornerShape(12.dp)
    val priceTag = RoundedCornerShape(6.dp)
    val avatar = RoundedCornerShape(50)
    val notification = RoundedCornerShape(8.dp)
    val progressBar = RoundedCornerShape(4.dp)
    val divider = RoundedCornerShape(1.dp)
}