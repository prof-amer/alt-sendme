package com.altsendme.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.altsendme.app.R
import com.altsendme.app.sendme.TransferProgress
import com.altsendme.app.ui.theme.AltSendmeTheme

/**
 * Animated progress bar with 30 bar segments, matching the desktop app design
 */
@Composable
fun TransferProgressBar(
    progress: TransferProgress,
    modifier: Modifier = Modifier
) {
    val barCount = 30
    val filledBars = (progress.percentage / 100f * barCount).toInt()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Progress header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.transfer_progress),
                style = MaterialTheme.typography.bodySmall,
                color = AltSendmeTheme.colors.textSecondary
            )
            Text(
                text = "${progress.percentage.format(1)}%",
                style = MaterialTheme.typography.bodySmall,
                color = AltSendmeTheme.colors.textSecondary
            )
        }

        // Progress bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(barCount) { index ->
                val isFilled = index < filledBars
                val isPartiallyFilled = index == filledBars && progress.percentage % (100f / barCount) > 0

                val fillPercentage = when {
                    isFilled -> 1f
                    isPartiallyFilled -> {
                        val barProgress = (progress.percentage % (100f / barCount)) / (100f / barCount)
                        barProgress
                    }
                    else -> 0f
                }

                // Animate fill height
                val animatedFillHeight by animateFloatAsState(
                    targetValue = fillPercentage,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "bar_fill_$index"
                )

                ProgressBarSegment(
                    fillPercentage = animatedFillHeight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Speed and size info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(R.string.transfer_speed)}: ${formatSpeed(progress.speedBps)}",
                style = MaterialTheme.typography.bodySmall,
                color = AltSendmeTheme.colors.textMuted
            )
            Text(
                text = "${formatBytes(progress.bytesTransferred)} / ${formatBytes(progress.totalBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = AltSendmeTheme.colors.textMuted
            )
        }

        // ETA
        progress.etaSeconds?.let { eta ->
            Text(
                text = "${stringResource(R.string.transfer_eta)}: ${formatEta(eta)}",
                style = MaterialTheme.typography.bodySmall,
                color = AltSendmeTheme.colors.textMuted
            )
        }
    }
}

@Composable
private fun ProgressBarSegment(
    fillPercentage: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(2.dp))
            .background(AltSendmeTheme.colors.progressBackground),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fillPercentage)
                .clip(RoundedCornerShape(2.dp))
                .background(AltSendmeTheme.colors.progressFill)
        )
    }
}

// Utility functions
private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)

fun formatSpeed(bytesPerSecond: Double): String {
    val mbps = bytesPerSecond / (1024 * 1024)
    val kbps = bytesPerSecond / 1024

    return if (mbps >= 1) {
        "%.2f MB/s".format(mbps)
    } else {
        "%.2f KB/s".format(kbps)
    }
}

fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    val kb = bytes / 1024.0

    return when {
        mb >= 1 -> "%.2f MB".format(mb)
        kb >= 1 -> "%.2f KB".format(kb)
        else -> "$bytes B"
    }
}

fun formatEta(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> {
            val mins = seconds / 60
            val secs = seconds % 60
            "${mins}m ${secs}s"
        }
        else -> {
            val hours = seconds / 3600
            val mins = (seconds % 3600) / 60
            "${hours}h ${mins}m"
        }
    }
}
