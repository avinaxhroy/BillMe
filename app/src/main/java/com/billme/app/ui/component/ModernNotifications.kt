package com.billme.app.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billme.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Modern snackbar with enhanced styling
 */
@Composable
fun ModernSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    type: SnackbarType = SnackbarType.INFO,
    icon: ImageVector? = null
) {
    Snackbar(
        modifier = modifier
            .padding(16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        containerColor = when (type) {
            SnackbarType.SUCCESS -> Color(0xFF00C853)
            SnackbarType.ERROR -> MaterialTheme.colorScheme.error
            SnackbarType.WARNING -> Color(0xFFFF9800)
            SnackbarType.INFO -> MaterialTheme.colorScheme.primary
        },
        contentColor = Color.White,
        action = if (actionLabel != null && onActionClick != null) {
            {
                TextButton(
                    onClick = onActionClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val displayIcon = icon ?: when (type) {
                SnackbarType.SUCCESS -> Icons.Default.CheckCircle
                SnackbarType.ERROR -> Icons.Default.Error
                SnackbarType.WARNING -> Icons.Default.Warning
                SnackbarType.INFO -> Icons.Default.Info
            }
            
            Icon(
                imageVector = displayIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

/**
 * Custom toast notification
 */
@Composable
fun ModernToast(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    type: SnackbarType = SnackbarType.INFO,
    duration: Long = 3000L
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(duration)
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it }
        ) + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = when (type) {
                        SnackbarType.SUCCESS -> Color(0xFF00C853)
                        SnackbarType.ERROR -> MaterialTheme.colorScheme.error
                        SnackbarType.WARNING -> Color(0xFFFF9800)
                        SnackbarType.INFO -> MaterialTheme.colorScheme.primary
                    }.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (icon, color) = when (type) {
                    SnackbarType.SUCCESS -> Icons.Default.CheckCircle to Color(0xFF00C853)
                    SnackbarType.ERROR -> Icons.Default.Error to MaterialTheme.colorScheme.error
                    SnackbarType.WARNING -> Icons.Default.Warning to Color(0xFFFF9800)
                    SnackbarType.INFO -> Icons.Default.Info to MaterialTheme.colorScheme.primary
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Banner notification for important messages
 */
@Composable
fun ModernBanner(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    type: SnackbarType = SnackbarType.INFO,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = when (type) {
                SnackbarType.SUCCESS -> Color(0xFF00C853).copy(alpha = 0.1f)
                SnackbarType.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                SnackbarType.WARNING -> Color(0xFFFF9800).copy(alpha = 0.1f)
                SnackbarType.INFO -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (icon, color) = when (type) {
                    SnackbarType.SUCCESS -> Icons.Default.CheckCircle to Color(0xFF00C853)
                    SnackbarType.ERROR -> Icons.Default.Error to MaterialTheme.colorScheme.error
                    SnackbarType.WARNING -> Icons.Default.Warning to Color(0xFFFF9800)
                    SnackbarType.INFO -> Icons.Default.Info to MaterialTheme.colorScheme.primary
                }
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                if (actionLabel != null && onActionClick != null) {
                    TextButton(onClick = onActionClick) {
                        Text(
                            text = actionLabel,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Snackbar type enum
 */
enum class SnackbarType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

/**
 * Snackbar host state wrapper with enhanced functionality
 */
class ModernSnackbarController {
    private val _snackbarState = mutableStateOf<ModernSnackbarState?>(null)
    val snackbarState: State<ModernSnackbarState?> = _snackbarState
    
    suspend fun showSnackbar(
        message: String,
        type: SnackbarType = SnackbarType.INFO,
        actionLabel: String? = null,
        duration: Long = 3000L
    ) {
        _snackbarState.value = ModernSnackbarState(
            message = message,
            type = type,
            actionLabel = actionLabel,
            visible = true
        )
        
        delay(duration)
        _snackbarState.value = null
    }
    
    fun dismiss() {
        _snackbarState.value = null
    }
}

/**
 * Snackbar state data class
 */
data class ModernSnackbarState(
    val message: String,
    val type: SnackbarType,
    val actionLabel: String?,
    val visible: Boolean
)

/**
 * Remember snackbar controller
 */
@Composable
fun rememberModernSnackbarController(): ModernSnackbarController {
    return remember { ModernSnackbarController() }
}
