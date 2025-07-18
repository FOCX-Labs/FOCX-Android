package com.focx.presentation.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Spacing System
object Spacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val huge = 48.dp
    val massive = 64.dp
}

// Sizing System
object Dimensions {
    // Button Sizes
    val buttonHeight = 48.dp
    val buttonHeightSmall = 36.dp
    val buttonHeightLarge = 56.dp

    // Card Sizes
    val cardElevation = 4.dp
    val cardMinHeight = 120.dp

    // Product Image Sizes
    val productImageSmall = 80.dp
    val productImageMedium = 120.dp
    val productImageLarge = 200.dp
    val productImageFull = 300.dp

    // Avatar Sizes
    val avatarSmall = 32.dp
    val avatarMedium = 48.dp
    val avatarLarge = 64.dp

    // Icon Sizes
    val iconSmall = 16.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp
    val iconExtraLarge = 48.dp

    // Input Field Sizes
    val textFieldHeight = 56.dp
    val searchBarHeight = 48.dp

    // Bottom Navigation
    val bottomNavHeight = 80.dp
    val bottomNavIconSize = 24.dp

    // Toolbar
    val toolbarHeight = 56.dp

    // Divider
    val dividerThickness = 1.dp

    // Progress Bar
    val progressBarHeight = 4.dp
    val progressBarHeightLarge = 8.dp

    // Badge
    val badgeSize = 20.dp
    val badgeSizeSmall = 16.dp

    // Minimum Touch Target
    val minTouchTarget = 48.dp

    // List Item
    val listItemHeight = 72.dp
    val listItemHeightSmall = 56.dp
    val listItemHeightLarge = 88.dp

    // Grid
    val gridSpacing = 8.dp
    val gridItemAspectRatio = 0.75f

    // Border
    val borderWidth = 1.dp
    val borderWidthThick = 2.dp

    // Shadow
    val shadowElevationSmall = 2.dp
    val shadowElevationMedium = 4.dp
    val shadowElevationLarge = 8.dp
}

// Animation Durations
object AnimationDuration {
    const val fast = 150
    const val normal = 300
    const val slow = 500
    const val extraSlow = 1000
}

// Opacity
object Alpha {
    const val disabled = 0.38f
    const val medium = 0.54f
    const val high = 0.87f
    const val full = 1.0f
    const val overlay = 0.6f
    const val shimmer = 0.3f
}

// Font Size Extensions
object FontSize {
    val tiny = 10.sp
    val small = 12.sp
    val medium = 14.sp
    val large = 16.sp
    val extraLarge = 18.sp
    val huge = 20.sp
    val massive = 24.sp
    val title = 28.sp
    val headline = 32.sp
}

// Line Height
object LineHeight {
    val tight = 1.2f
    val normal = 1.4f
    val relaxed = 1.6f
    val loose = 1.8f
}

// Z-Index
object ZIndex {
    val background = 0f
    val content = 1f
    val overlay = 2f
    val modal = 3f
    val dropdown = 4f
    val tooltip = 5f
    val notification = 6f
    val maximum = 10f
}