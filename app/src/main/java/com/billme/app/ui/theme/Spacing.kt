package com.billme.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Consistent spacing system for the entire app
 * Following Material Design 3 guidelines with 4dp baseline grid
 */
object AppSpacing {
    // Baseline spacing units
    val None = 0.dp
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val MediumLarge = 16.dp
    val Large = 20.dp
    val ExtraLarge = 24.dp
    val ExtraExtraLarge = 32.dp
    val Huge = 48.dp
    
    // Component-specific spacing
    object Card {
        val Padding = MediumLarge
        val Spacing = Medium
        val MarginHorizontal = MediumLarge
        val MarginVertical = Small
    }
    
    object Button {
        val PaddingHorizontal = Large
        val PaddingVertical = Medium
        val IconSpacing = Small
    }
    
    object TextField {
        val Padding = MediumLarge
        val Spacing = Medium
    }
    
    object List {
        val ItemPadding = MediumLarge
        val ItemSpacing = Small
        val SectionSpacing = ExtraLarge
    }
    
    object Screen {
        val Padding = MediumLarge
        val HorizontalPadding = MediumLarge
        val VerticalPadding = Medium
    }
    
    object Dialog {
        val Padding = ExtraLarge
        val ContentSpacing = MediumLarge
    }
}

/**
 * Icon sizes for consistent iconography
 */
object AppIconSize {
    val ExtraSmall = 16.dp
    val Small = 20.dp
    val Medium = 24.dp
    val Large = 32.dp
    val ExtraLarge = 48.dp
    val Huge = 64.dp
}
