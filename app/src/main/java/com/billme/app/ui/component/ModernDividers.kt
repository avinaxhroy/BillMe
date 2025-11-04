package com.billme.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.billme.app.ui.theme.*

/**
 * Modern horizontal divider with gradient
 */
@Composable
fun ModernDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color
    )
}

/**
 * Gradient divider
 */
@Composable
fun GradientDivider(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(
        Color.Transparent,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        Color.Transparent
    ),
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(
                brush = Brush.horizontalGradient(gradientColors)
            )
    )
}

/**
 * Divider with text
 */
@Composable
fun DividerWithText(
    text: String,
    modifier: Modifier = Modifier,
    dividerColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = dividerColor
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = dividerColor
        )
    }
}

/**
 * Divider with icon
 */
@Composable
fun DividerWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    dividerColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = dividerColor
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = dividerColor
        )
    }
}

/**
 * Vertical divider for rows
 */
@Composable
fun ModernVerticalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
) {
    VerticalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color
    )
}

/**
 * Dotted divider
 */
@Composable
fun DottedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    dotSize: Dp = 4.dp,
    spacing: Dp = 8.dp
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(50) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * Section divider with padding
 */
@Composable
fun SectionDivider(
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 16.dp
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(verticalPadding))
        ModernDivider()
        Spacer(modifier = Modifier.height(verticalPadding))
    }
}

/**
 * Inset divider (with padding from start)
 */
@Composable
fun InsetDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startIndent)
                .height(1.dp)
                .background(color)
        )
    }
}
