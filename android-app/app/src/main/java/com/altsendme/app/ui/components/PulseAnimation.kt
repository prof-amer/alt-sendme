package com.altsendme.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.altsendme.app.ui.theme.AltSendmeTheme

/**
 * Pulse animation component matching the desktop app's Lottie animation
 */
@Composable
fun PulseAnimation(
    isTransporting: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    // Determine color based on state
    val baseColor = when {
        isCompleted -> AltSendmeTheme.colors.statusCompleted
        isTransporting -> AltSendmeTheme.colors.statusActive
        else -> AltSendmeTheme.colors.statusListening
    }

    // Animation for the pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )

    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing, delayMillis = 500),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2"
    )

    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing, delayMillis = 500),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val centerX = this.size.width / 2
            val centerY = this.size.height / 2
            val maxRadius = this.size.minDimension / 2

            // Outer pulse rings (only when not completed)
            if (!isCompleted) {
                // First ring
                drawCircle(
                    color = baseColor.copy(alpha = alpha1),
                    radius = maxRadius * scale1,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                )

                // Second ring (delayed)
                drawCircle(
                    color = baseColor.copy(alpha = alpha2),
                    radius = maxRadius * scale2,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                )
            }

            // Center circle
            drawCircle(
                color = baseColor,
                radius = maxRadius * 0.4f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }
    }
}

/**
 * Simple status indicator dot
 */
@Composable
fun StatusIndicator(
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    val color = when {
        isCompleted -> AltSendmeTheme.colors.statusCompleted
        isActive -> AltSendmeTheme.colors.statusActive
        else -> AltSendmeTheme.colors.statusListening
    }

    // Pulse animation for active state
    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive && !isCompleted) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_alpha"
    )

    Canvas(modifier = modifier.size(size)) {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = this.size.minDimension / 2
        )
    }
}
