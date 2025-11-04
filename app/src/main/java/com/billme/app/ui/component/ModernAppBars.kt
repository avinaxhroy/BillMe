package com.billme.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

/**
 * Modern top app bar with gradient background
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowBack,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    useGradient: Boolean = false,
    gradientColors: List<Color> = listOf(Primary, PrimaryLight),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val containerColor = if (useGradient) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (useGradient) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (useGradient) {
                    Modifier.background(
                        brush = Brush.horizontalGradient(gradientColors)
                    )
                } else {
                    Modifier
                        .shadow(
                            elevation = 4.dp,
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                        .background(containerColor)
                }
            )
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }
            },
            navigationIcon = {
                if (navigationIcon != null && onNavigationClick != null) {
                    IconButton(onClick = onNavigationClick) {
                        Icon(
                            imageVector = navigationIcon,
                            contentDescription = "Navigate back",
                            tint = contentColor
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                navigationIconContentColor = contentColor,
                titleContentColor = contentColor,
                actionIconContentColor = contentColor
            ),
            scrollBehavior = scrollBehavior
        )
    }
}

/**
 * Enhanced large top app bar with gradient
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernLargeTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowBack,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    useGradient: Boolean = true,
    gradientColors: List<Color> = listOf(Primary, PrimaryLight),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val containerColor = if (useGradient) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (useGradient) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (useGradient) {
                    Modifier.background(
                        brush = Brush.verticalGradient(gradientColors)
                    )
                } else {
                    Modifier.background(containerColor)
                }
            )
    ) {
        LargeTopAppBar(
            title = {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }
            },
            navigationIcon = {
                if (navigationIcon != null && onNavigationClick != null) {
                    IconButton(onClick = onNavigationClick) {
                        Icon(
                            imageVector = navigationIcon,
                            contentDescription = "Navigate back",
                            tint = contentColor
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = if (useGradient) {
                    gradientColors.first()
                } else {
                    containerColor
                },
                navigationIconContentColor = contentColor,
                titleContentColor = contentColor,
                actionIconContentColor = contentColor
            ),
            scrollBehavior = scrollBehavior
        )
    }
}

/**
 * Modern bottom app bar with FAB
 */
@Composable
fun ModernBottomAppBar(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {}
) {
    BottomAppBar(
        modifier = modifier.shadow(
            elevation = 8.dp,
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ),
        actions = actions,
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * Modern floating action button with gradient
 */
@Composable
fun ModernFloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    useGradient: Boolean = true,
    gradientColors: List<Color> = listOf(Primary, PrimaryLight)
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(64.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = if (useGradient) gradientColors.first().copy(alpha = 0.3f)
                              else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ),
        containerColor = if (useGradient) Color.Transparent else MaterialTheme.colorScheme.primaryContainer,
        contentColor = Color.White,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (useGradient) {
                        Modifier.background(
                            brush = Brush.linearGradient(gradientColors),
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(28.dp),
                tint = Color.White
            )
        }
    }
}

/**
 * Extended FAB with text
 */
@Composable
fun ModernExtendedFAB(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    useGradient: Boolean = true,
    gradientColors: List<Color> = listOf(Primary, PrimaryLight)
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier.shadow(
            elevation = 12.dp,
            shape = CircleShape,
            ambientColor = if (useGradient) gradientColors.first().copy(alpha = 0.3f)
                          else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        expanded = expanded,
        text = { Text(text) },
        icon = { Icon(icon, contentDescription = null) },
        containerColor = if (useGradient) gradientColors.first() else MaterialTheme.colorScheme.primaryContainer,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 12.dp,
            pressedElevation = 16.dp,
            hoveredElevation = 14.dp
        )
    )
}

/**
 * Modern icon button with background
 */
@Composable
fun ModernIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Toolbar action button
 */
@Composable
fun ToolbarAction(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Box(modifier = modifier) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
        }
        
        if (badge != null) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
