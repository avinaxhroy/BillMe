package com.billme.app.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animation durations following Material Design guidelines
 */
object AnimationDurations {
    const val Fast = 150
    const val Medium = 300
    const val Slow = 500
    const val VerySlow = 700
}

/**
 * Standard easing curves
 */
object AnimationEasing {
    val FastOutSlowIn: Easing = FastOutSlowInEasing
    val LinearOutSlowIn: Easing = LinearOutSlowInEasing
    val FastOutLinearIn: Easing = FastOutLinearInEasing
    val EaseInOut: Easing = androidx.compose.animation.core.EaseInOut
    val EaseOut: Easing = androidx.compose.animation.core.EaseOut
    val EaseIn: Easing = androidx.compose.animation.core.EaseIn
}

/**
 * Fade animation for content transitions
 */
fun fadeInAnimation(
    duration: Int = AnimationDurations.Medium,
    delay: Int = 0
): EnterTransition {
    return fadeIn(
        animationSpec = tween(
            durationMillis = duration,
            delayMillis = delay,
            easing = AnimationEasing.FastOutSlowIn
        )
    )
}

fun fadeOutAnimation(
    duration: Int = AnimationDurations.Medium,
    delay: Int = 0
): ExitTransition {
    return fadeOut(
        animationSpec = tween(
            durationMillis = duration,
            delayMillis = delay,
            easing = AnimationEasing.FastOutSlowIn
        )
    )
}

/**
 * Slide animation for screen transitions
 */
fun slideInFromBottomAnimation(
    duration: Int = AnimationDurations.Medium
): EnterTransition {
    return slideInVertically(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutSlowIn
        ),
        initialOffsetY = { fullHeight -> fullHeight }
    ) + fadeIn(
        animationSpec = tween(duration)
    )
}

fun slideOutToBottomAnimation(
    duration: Int = AnimationDurations.Medium
): ExitTransition {
    return slideOutVertically(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutLinearIn
        ),
        targetOffsetY = { fullHeight -> fullHeight }
    ) + fadeOut(
        animationSpec = tween(duration)
    )
}

fun slideInFromRightAnimation(
    duration: Int = AnimationDurations.Medium
): EnterTransition {
    return slideInHorizontally(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutSlowIn
        ),
        initialOffsetX = { fullWidth -> fullWidth }
    ) + fadeIn(
        animationSpec = tween(duration)
    )
}

fun slideOutToRightAnimation(
    duration: Int = AnimationDurations.Medium
): ExitTransition {
    return slideOutHorizontally(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutLinearIn
        ),
        targetOffsetX = { fullWidth -> fullWidth }
    ) + fadeOut(
        animationSpec = tween(duration)
    )
}

fun slideInFromLeftAnimation(
    duration: Int = AnimationDurations.Medium
): EnterTransition {
    return slideInHorizontally(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutSlowIn
        ),
        initialOffsetX = { fullWidth -> -fullWidth }
    ) + fadeIn(
        animationSpec = tween(duration)
    )
}

fun slideOutToLeftAnimation(
    duration: Int = AnimationDurations.Medium
): ExitTransition {
    return slideOutHorizontally(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutLinearIn
        ),
        targetOffsetX = { fullWidth -> -fullWidth }
    ) + fadeOut(
        animationSpec = tween(duration)
    )
}

/**
 * Scale animation for dialogs and modals
 */
fun scaleInAnimation(
    duration: Int = AnimationDurations.Medium,
    initialScale: Float = 0.8f
): EnterTransition {
    return scaleIn(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutSlowIn
        ),
        initialScale = initialScale
    ) + fadeIn(
        animationSpec = tween(duration)
    )
}

fun scaleOutAnimation(
    duration: Int = AnimationDurations.Medium,
    targetScale: Float = 0.8f
): ExitTransition {
    return scaleOut(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutLinearIn
        ),
        targetScale = targetScale
    ) + fadeOut(
        animationSpec = tween(duration)
    )
}

/**
 * Expand/Collapse animations for lists and content
 */
@OptIn(ExperimentalAnimationApi::class)
fun expandVerticallyAnimation(
    duration: Int = AnimationDurations.Medium
): EnterTransition {
    return expandVertically(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutSlowIn
        )
    ) + fadeIn(
        animationSpec = tween(duration)
    )
}

@OptIn(ExperimentalAnimationApi::class)
fun shrinkVerticallyAnimation(
    duration: Int = AnimationDurations.Medium
): ExitTransition {
    return shrinkVertically(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutLinearIn
        )
    ) + fadeOut(
        animationSpec = tween(duration)
    )
}

/**
 * Infinite pulsing animation for loading states
 */
@Composable
fun rememberPulseAnimation(
    minAlpha: Float = 0.3f,
    maxAlpha: Float = 1f,
    duration: Int = 1000
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    ).value
}

/**
 * Shimmer effect animation for loading placeholders
 */
@Composable
fun rememberShimmerAnimation(
    duration: Int = 1500
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    return infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    ).value
}

/**
 * Bounce animation for interactive elements
 */
fun bounceAnimation(
    duration: Int = AnimationDurations.Medium
): EnterTransition {
    return scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeIn(
        animationSpec = tween(duration)
    )
}

/**
 * Spring-based animations for natural motion
 */
object SpringAnimations {
    val bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    
    val smooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val stiff = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    val gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Navigation transition animations
 */
object NavigationAnimations {
    val enterFromRight = slideInFromRightAnimation()
    val exitToLeft = slideOutToLeftAnimation()
    val enterFromLeft = slideInFromLeftAnimation()
    val exitToRight = slideOutToRightAnimation()
    val enterFromBottom = slideInFromBottomAnimation()
    val exitToBottom = slideOutToBottomAnimation()
    val fadeIn = fadeInAnimation()
    val fadeOut = fadeOutAnimation()
}

/**
 * List item animations
 */
fun listItemAnimation(
    index: Int,
    delay: Int = 50
): Modifier {
    return Modifier // Note: animateItemPlacement is available in Compose 1.2+
}

/**
 * Reveal animation with size change
 */
@OptIn(ExperimentalAnimationApi::class)
fun revealAnimation(
    duration: Int = AnimationDurations.Medium
): EnterTransition {
    return expandIn(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutSlowIn
        )
    ) + fadeIn(
        animationSpec = tween(duration)
    )
}

@OptIn(ExperimentalAnimationApi::class)
fun hideAnimation(
    duration: Int = AnimationDurations.Medium
): ExitTransition {
    return shrinkOut(
        animationSpec = tween(
            durationMillis = duration,
            easing = AnimationEasing.FastOutLinearIn
        )
    ) + fadeOut(
        animationSpec = tween(duration)
    )
}
