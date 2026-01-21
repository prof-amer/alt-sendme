package com.altsendme.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.altsendme.app.ui.theme.AltSendmeTheme

/**
 * Glass-effect card component matching the desktop app design
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    borderStrength: GlassBorderStrength = GlassBorderStrength.NORMAL,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val borderColor = when (borderStrength) {
        GlassBorderStrength.LIGHT -> AltSendmeTheme.colors.glassBorder.copy(alpha = 0.05f)
        GlassBorderStrength.NORMAL -> AltSendmeTheme.colors.glassBorder
        GlassBorderStrength.STRONG -> AltSendmeTheme.colors.glassBorderStrong
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(AltSendmeTheme.colors.surface)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(contentPadding),
        content = content
    )
}

enum class GlassBorderStrength {
    LIGHT,
    NORMAL,
    STRONG
}
