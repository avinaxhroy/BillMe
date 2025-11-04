package com.billme.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Brand Colors - Modern Vibrant Blue Palette with Enhanced Visibility
val Primary = Color(0xFF2196F3)
val PrimaryLight = Color(0xFF64B5F6)
val PrimaryDark = Color(0xFF1565C0)
val PrimaryContainer = Color(0xFFE3F2FD)

// Secondary Colors - Elegant Purple/Violet with Better Contrast
val Secondary = Color(0xFF9C27B0)
val SecondaryLight = Color(0xFFBA68C8)
val SecondaryDark = Color(0xFF7B1FA2)
val SecondaryContainer = Color(0xFFF3E5F5)

// Tertiary Colors - Fresh Teal with Enhanced Visibility
val Tertiary = Color(0xFF00897B)
val TertiaryLight = Color(0xFF4DB6AC)
val TertiaryDark = Color(0xFF00695C)
val TertiaryContainer = Color(0xFFE0F2F1)

// Success, Warning, Error Colors with Better Contrast
val Success = Color(0xFF00C853)
val SuccessLight = Color(0xFF69F0AE)
val SuccessContainer = Color(0xFFE8F5E9)

val Warning = Color(0xFFFF9800)
val WarningLight = Color(0xFFFFB74D)
val WarningContainer = Color(0xFFFFF3E0)

val Error = Color(0xFFE53935)
val ErrorLight = Color(0xFFEF5350)
val ErrorContainer = Color(0xFFFFEBEE)

// Info Colors
val Info = Color(0xFF2196F3)
val InfoLight = Color(0xFF64B5F6)
val InfoContainer = Color(0xFFE3F2FD)

// Background Colors - Light Theme with Better Contrast
val BackgroundLight = Color(0xFFF8F9FA)
val SurfaceLight = Color(0xFFFFFFFF)
val Surface1Light = Color(0xFFF5F7FA)
val Surface2Light = Color(0xFFEDF0F5)
val Surface3Light = Color(0xFFE5E9EF)

// Background Colors - Dark Theme with Enhanced Visibility
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val Surface1Dark = Color(0xFF252525)
val Surface2Dark = Color(0xFF2C2C2C)
val Surface3Dark = Color(0xFF373737)

// Text Colors - Light Theme with Enhanced Readability
val TextPrimaryLight = Color(0xFF212121)
val TextSecondaryLight = Color(0xFF616161)
val TextTertiaryLight = Color(0xFF9E9E9E)
val TextDisabledLight = Color(0xFFBDBDBD)

// Text Colors - Dark Theme with Better Contrast
val TextPrimaryDark = Color(0xFFFAFAFA)
val TextSecondaryDark = Color(0xFFE0E0E0)
val TextTertiaryDark = Color(0xFF9E9E9E)
val TextDisabledDark = Color(0xFF757575)

// Outline Colors with Better Definition
val OutlineLight = Color(0xFFDEE2E6)
val OutlineVariantLight = Color(0xFFF1F3F5)
val OutlineDark = Color(0xFF404040)
val OutlineVariantDark = Color(0xFF303030)

// Scrim and Overlay
val Scrim = Color(0x88000000)
val ScrimLight = Color(0x33000000)

// Beautiful Gradient Colors
val GradientStart = Color(0xFF1E88E5)
val GradientMiddle = Color(0xFF7E57C2)
val GradientEnd = Color(0xFF26A69A)

// Additional Gradient Variations
val GradientPurpleBlue = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
val GradientOrangeRed = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
val GradientGreenBlue = listOf(Color(0xFF06BEB6), Color(0xFF48B1BF))
val GradientPinkPurple = listOf(Color(0xFFE96443), Color(0xFF904E95))

// Glassmorphism Colors
val GlassLight = Color(0xCCFFFFFF)
val GlassDark = Color(0xCC1A1F29)
val GlassBorder = Color(0x33FFFFFF)

// Card and Component Colors
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1E2433)
val CardElevatedLight = Color(0xFFFAFBFF)
val CardElevatedDark = Color(0xFF252B3B)

// Divider Colors
val DividerLight = Color(0xFFE5E5E5)
val DividerDark = Color(0xFF2D3340)

// Shimmer Effect Colors
val ShimmerHighLight = Color(0xCCFFFFFF)
val ShimmerBase = Color(0xFFE0E0E0)

// Chart Colors
val ChartBlue = Color(0xFF2196F3)
val ChartGreen = Color(0xFF4CAF50)
val ChartOrange = Color(0xFFFF9800)
val ChartRed = Color(0xFFF44336)
val ChartPurple = Color(0xFF9C27B0)
val ChartTeal = Color(0xFF009688)
val ChartIndigo = Color(0xFF3F51B5)
val ChartPink = Color(0xFFE91E63)

// Gradient Brushes for modern UI
object AppGradients {
    val PrimaryGradient = Brush.horizontalGradient(
        colors = listOf(GradientStart, GradientMiddle)
    )
    
    val SuccessGradient = Brush.horizontalGradient(
        colors = listOf(Tertiary, TertiaryLight)
    )
    
    val WarningGradient = Brush.horizontalGradient(
        colors = listOf(Warning, WarningLight)
    )
    
    val PurpleBlueGradient = Brush.horizontalGradient(
        colors = GradientPurpleBlue
    )
    
    val VerticalPrimaryGradient = Brush.verticalGradient(
        colors = listOf(GradientStart, GradientMiddle, GradientEnd)
    )
    
    val DiagonalGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF667EEA),
            Color(0xFF764BA2)
        )
    )
    
    val SunsetGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFF8E53),
            Color(0xFFFFC371)
        )
    )
    
    val OceanGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF00B4DB),
            Color(0xFF0083B0)
        )
    )
}

// Status Colors for Business Logic
val Paid = Color(0xFF00C853)
val Pending = Color(0xFFFFAB00)
val Overdue = Color(0xFFFF3D00)
val Draft = Color(0xFF9E9E9E)

// Invoice and Bill Colors
val InvoiceAccent = Color(0xFF0066FF)
val BillAccent = Color(0xFF7C4DFF)
val PaymentSuccess = Color(0xFF00C853)
val RefundColor = Color(0xFFFF6F00)
