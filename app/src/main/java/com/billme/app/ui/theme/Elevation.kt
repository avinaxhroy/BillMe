package com.billme.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation system following Material Design 3 guidelines
 * Provides consistent elevation values across the app with enhanced depth
 */
object Elevations {
    // Level 0 - No elevation (flat)
    val Level0: Dp = 0.dp
    
    // Level 1 - Subtle elevation for cards and surfaces
    val Level1: Dp = 2.dp
    
    // Level 2 - Standard elevation for most cards
    val Level2: Dp = 4.dp
    
    // Level 3 - Elevated cards and components
    val Level3: Dp = 8.dp
    
    // Level 4 - Navigation elements and app bars
    val Level4: Dp = 12.dp
    
    // Level 5 - FABs and prominent actions
    val Level5: Dp = 16.dp
    
    // Specific component elevations
    val Card: Dp = Level2
    val CardHovered: Dp = Level3
    val CardPressed: Dp = Level1
    
    val Button: Dp = Level2
    val ButtonHovered: Dp = Level3
    val ButtonPressed: Dp = Level1
    
    val Dialog: Dp = Level5
    val BottomSheet: Dp = Level5
    
    val AppBar: Dp = Level2
    val NavigationBar: Dp = Level3
    
    val FAB: Dp = Level3
    val FABHovered: Dp = Level4
    val FABPressed: Dp = Level5
    
    val Menu: Dp = Level4
    val ModalDrawer: Dp = Level5
    
    val Chip: Dp = Level1
    val ChipPressed: Dp = Level2
    
    // Responsive elevation based on interaction
    fun responsive(
        default: Dp = Level2,
        hovered: Dp = Level3,
        pressed: Dp = Level1
    ): Dp = default
}
