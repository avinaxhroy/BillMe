package com.billme.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Extra Small - For chips, small buttons
    extraSmall = RoundedCornerShape(6.dp),
    
    // Small - For small cards, contained buttons
    small = RoundedCornerShape(10.dp),
    
    // Medium - For standard cards, dialogs
    medium = RoundedCornerShape(14.dp),
    
    // Large - For sheets, large cards
    large = RoundedCornerShape(18.dp),
    
    // Extra Large - For bottom sheets, modals
    extraLarge = RoundedCornerShape(32.dp)
)

// Additional custom shapes for specific components
object CustomShapes {
    // Card shapes with different corner radii
    val cardSmall = RoundedCornerShape(10.dp)
    val cardMedium = RoundedCornerShape(14.dp)
    val cardLarge = RoundedCornerShape(18.dp)
    val cardExtraLarge = RoundedCornerShape(24.dp)
    
    // Button shapes
    val buttonSmall = RoundedCornerShape(10.dp)
    val buttonMedium = RoundedCornerShape(14.dp)
    val buttonLarge = RoundedCornerShape(18.dp)
    val buttonRound = RoundedCornerShape(50)
    
    // Input field shapes
    val textFieldSmall = RoundedCornerShape(10.dp)
    val textFieldMedium = RoundedCornerShape(14.dp)
    
    // Dialog and modal shapes
    val dialogShape = RoundedCornerShape(28.dp)
    val bottomSheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Chip shapes
    val chipShape = RoundedCornerShape(10.dp)
    val chipRound = RoundedCornerShape(50)
    
    // Badge and indicator shapes
    val badgeShape = RoundedCornerShape(50)
    val indicatorShape = RoundedCornerShape(6.dp)
    
    // Image and avatar shapes
    val avatarShape = RoundedCornerShape(50)
    val imageSmall = RoundedCornerShape(10.dp)
    val imageMedium = RoundedCornerShape(14.dp)
    val imageLarge = RoundedCornerShape(18.dp)
    
    // Asymmetric shapes for creative UI elements
    val topRounded = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    val bottomRounded = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 18.dp,
        bottomEnd = 18.dp
    )
    
    // Invoice and bill specific shapes
    val invoiceCard = RoundedCornerShape(18.dp)
    val billCard = RoundedCornerShape(14.dp)
    
    // Search bar shape
    val searchBar = RoundedCornerShape(28.dp)
}
