package com.focx.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Tech Theme Color System
val TechBlue = Color(0xFF00D4FF)
val NeonGreen = Color(0xFF00FF88)
val TechPurple = Color(0xFF8B5CF6)
val CyberYellow = Color(0xFFFFD700)

// Primary Colors
val Primary = TechBlue
val PrimaryVariant = Color(0xFF0099CC)
val Secondary = NeonGreen
val SecondaryVariant = Color(0xFF00CC66)

// Background Colors
val BackgroundDark = Color(0xFF0A0A0A)
val BackgroundMedium = Color(0xFF1A1A1A)
val SurfaceDark = Color(0xFF1E1E1E)
val SurfaceMedium = Color(0xFF2A2A2A)

// Text Colors
val OnPrimary = Color(0xFF000000)
val OnSecondary = Color(0xFF000000)
val OnBackground = Color(0xFFFFFFFF)
val OnSurface = Color(0xFFFFFFFF)
val OnSurfaceVariant = Color(0xFFB3B3B3)

// Status Colors
val Success = NeonGreen
val Warning = CyberYellow
val Error = Color(0xFFFF4444)
val Info = TechBlue

// Price Color
val PriceColor = Color(0xFFFF6B35)
val DiscountColor = Color(0xFF00FF88)

// Border and Divider
val BorderColor = Color(0xFF333333)
val DividerColor = Color(0xFF2A2A2A)

// Glow Effect Colors
val GlowBlue = Color(0x4000D4FF)
val GlowGreen = Color(0x4000FF88)
val GlowPurple = Color(0x408B5CF6)

// Gradient Colors
val GradientStart = Color(0xFF1A1A1A)
val GradientEnd = Color(0xFF0A0A0A)

// Alpha Variants
val SurfaceAlpha12 = Color(0x1FFFFFFF)
val SurfaceAlpha24 = Color(0x3DFFFFFF)
val SurfaceAlpha38 = Color(0x61FFFFFF)

// Light theme colors (for contrast)
val LightPrimary = Color(0xFF0066CC)
val LightSecondary = Color(0xFF00AA55)
val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFFFFFF)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF1A1A1A)
val LightOnSurface = Color(0xFF1A1A1A)

// TechColors object for component usage
object TechColors {
    val primary = Primary
    val onPrimary = OnPrimary
    val secondary = Secondary
    val onSecondary = OnSecondary
    val surface = SurfaceDark
    val onSurface = OnSurface
    val surfaceVariant = SurfaceMedium
    val onSurfaceVariant = OnSurfaceVariant
    val outline = BorderColor
    val background = BackgroundDark
    val onBackground = OnBackground
}