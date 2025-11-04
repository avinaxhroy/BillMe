package com.billme.app.ui.util

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utility extensions and helpers for UI components
 */

/**
 * Add conditional modifier
 */
fun Modifier.conditional(
    condition: Boolean,
    modifier: Modifier.() -> Modifier
): Modifier {
    return if (condition) {
        then(modifier())
    } else {
        this
    }
}

/**
 * Responsive spacing based on screen size
 */
object ResponsiveSpacing {
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 16.dp
    val large: Dp = 24.dp
    val extraLarge: Dp = 32.dp
    val huge: Dp = 48.dp
}

/**
 * Vertical spacer with default size
 */
@Composable
fun VerticalSpacer(size: Dp = ResponsiveSpacing.medium) {
    Spacer(modifier = Modifier.height(size))
}

/**
 * Horizontal spacer with default size
 */
@Composable
fun HorizontalSpacer(size: Dp = ResponsiveSpacing.medium) {
    Spacer(modifier = Modifier.width(size))
}

/**
 * Weight modifier for responsive layouts
 * Note: weight() is only available in Row/Column scope
 */
// fun Modifier.responsiveWeight(weight: Float = 1f): Modifier {
//     return this.weight(weight)
// }

/**
 * Padding utilities
 */
object PaddingValues {
    fun all(value: Dp) = androidx.compose.foundation.layout.PaddingValues(value)
    fun horizontal(value: Dp) = androidx.compose.foundation.layout.PaddingValues(horizontal = value)
    fun vertical(value: Dp) = androidx.compose.foundation.layout.PaddingValues(vertical = value)
    fun symmetric(horizontal: Dp = 0.dp, vertical: Dp = 0.dp) = 
        androidx.compose.foundation.layout.PaddingValues(horizontal = horizontal, vertical = vertical)
}

/**
 * Animation duration constants
 */
object AnimationDurations {
    const val FAST = 150
    const val MEDIUM = 300
    const val SLOW = 500
    const val VERY_SLOW = 700
}

/**
 * Common UI dimensions
 */
object UIDimensions {
    // Icon sizes
    val iconSmall: Dp = 16.dp
    val iconMedium: Dp = 24.dp
    val iconLarge: Dp = 32.dp
    val iconExtraLarge: Dp = 48.dp
    
    // Button sizes
    val buttonHeightSmall: Dp = 40.dp
    val buttonHeightMedium: Dp = 48.dp
    val buttonHeightLarge: Dp = 56.dp
    
    // Card dimensions
    val cardElevation: Dp = 4.dp
    val cardElevationHovered: Dp = 8.dp
    
    // Corner radius
    val cornerRadiusSmall: Dp = 8.dp
    val cornerRadiusMedium: Dp = 14.dp
    val cornerRadiusLarge: Dp = 18.dp
    val cornerRadiusExtraLarge: Dp = 24.dp
    
    // Divider
    val dividerThickness: Dp = 1.dp
    
    // Image sizes
    val thumbnailSize: Dp = 60.dp
    val avatarSizeSmall: Dp = 32.dp
    val avatarSizeMedium: Dp = 48.dp
    val avatarSizeLarge: Dp = 64.dp
}

/**
 * Layout constraints
 */
object LayoutConstraints {
    val maxContentWidth: Dp = 1200.dp
    val minTouchTarget: Dp = 48.dp
}
