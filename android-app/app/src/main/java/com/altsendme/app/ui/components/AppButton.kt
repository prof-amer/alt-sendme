package com.altsendme.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.altsendme.app.ui.theme.AltSendmeTheme

enum class ButtonStyle {
    PRIMARY,
    SECONDARY,
    DESTRUCTIVE,
    OUTLINE
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = when {
        !enabled -> AltSendmeTheme.colors.buttonDisabled
        style == ButtonStyle.PRIMARY -> AltSendmeTheme.colors.buttonPrimary
        style == ButtonStyle.SECONDARY -> AltSendmeTheme.colors.buttonSecondary
        style == ButtonStyle.DESTRUCTIVE -> AltSendmeTheme.colors.buttonDestructive
        style == ButtonStyle.OUTLINE -> Color.Transparent
        else -> AltSendmeTheme.colors.buttonPrimary
    }

    val pressedColor = when {
        !enabled -> AltSendmeTheme.colors.buttonDisabled
        style == ButtonStyle.PRIMARY -> AltSendmeTheme.colors.primaryBright
        style == ButtonStyle.SECONDARY -> AltSendmeTheme.colors.accent.copy(alpha = 0.8f)
        style == ButtonStyle.DESTRUCTIVE -> AltSendmeTheme.colors.destructive.copy(alpha = 0.8f)
        style == ButtonStyle.OUTLINE -> AltSendmeTheme.colors.glassBorder
        else -> AltSendmeTheme.colors.primaryBright
    }

    val animatedBackground by animateColorAsState(
        targetValue = if (isPressed) pressedColor else backgroundColor,
        animationSpec = tween(100),
        label = "button_background"
    )

    val textColor = when {
        !enabled -> AltSendmeTheme.colors.textHint
        style == ButtonStyle.OUTLINE -> AltSendmeTheme.colors.textPrimary
        else -> AltSendmeTheme.colors.textPrimary
    }

    val borderColor = when (style) {
        ButtonStyle.OUTLINE -> AltSendmeTheme.colors.glassBorderStrong
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(animatedBackground)
            .then(
                if (style == ButtonStyle.OUTLINE) {
                    Modifier.border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = textColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                leadingIcon?.let {
                    it()
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )

                trailingIcon?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    it()
                }
            }
        }
    }
}
