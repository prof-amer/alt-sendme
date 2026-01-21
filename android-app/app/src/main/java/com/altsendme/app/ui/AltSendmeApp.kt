package com.altsendme.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altsendme.app.R
import com.altsendme.app.ui.screens.ReceiveScreen
import com.altsendme.app.ui.screens.SendScreen
import com.altsendme.app.ui.theme.AltSendmeTheme

enum class AppTab {
    SEND, RECEIVE
}

@Composable
fun AltSendmeApp() {
    var selectedTab by remember { mutableStateOf(AppTab.SEND) }
    var isSending by remember { mutableStateOf(false) }
    var isReceiving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AltSendmeTheme.colors.background)
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Title
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = AltSendmeTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )

        // Tab Selector
        TabSelector(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            isSendDisabled = isReceiving,
            isReceiveDisabled = isSending,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Content Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(AltSendmeTheme.colors.surface)
                .border(
                    width = 1.dp,
                    color = AltSendmeTheme.colors.glassBorder,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState == AppTab.SEND) {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    } else {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    AppTab.SEND -> SendScreen(
                        onTransferStateChange = { isSending = it }
                    )
                    AppTab.RECEIVE -> ReceiveScreen(
                        onTransferStateChange = { isReceiving = it }
                    )
                }
            }
        }

        // Footer
        AppFooter()
    }
}

@Composable
private fun TabSelector(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    isSendDisabled: Boolean,
    isReceiveDisabled: Boolean,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        AppTab.SEND to stringResource(R.string.tab_send),
        AppTab.RECEIVE to stringResource(R.string.tab_receive)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AltSendmeTheme.colors.tabBackground)
            .padding(4.dp)
    ) {
        // Animated selector background
        val transition = updateTransition(selectedTab, label = "tab_transition")
        val offsetX by transition.animateFloat(
            transitionSpec = { tween(durationMillis = 200, easing = FastOutSlowInEasing) },
            label = "offset_x"
        ) { tab ->
            if (tab == AppTab.SEND) 0f else 1f
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .offset(x = (offsetX * 0.5f * LocalContext.current.resources.displayMetrics.widthPixels / LocalContext.current.resources.displayMetrics.density - offsetX * 16).dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AltSendmeTheme.colors.tabSelected)
                .border(
                    width = 1.dp,
                    color = AltSendmeTheme.colors.tabSelectedBorder,
                    shape = RoundedCornerShape(6.dp)
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { (tab, title) ->
                val isDisabled = when (tab) {
                    AppTab.SEND -> isSendDisabled
                    AppTab.RECEIVE -> isReceiveDisabled
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            enabled = !isDisabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            isDisabled -> AltSendmeTheme.colors.textHint
                            selectedTab == tab -> AltSendmeTheme.colors.textPrimary
                            else -> AltSendmeTheme.colors.textMuted
                        },
                        fontWeight = if (selectedTab == tab) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun AppFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donate button
        Text(
            text = stringResource(R.string.donate),
            style = MaterialTheme.typography.bodySmall,
            color = AltSendmeTheme.colors.textMuted,
            modifier = Modifier
                .clickable { /* Open donate URL */ }
                .padding(8.dp)
        )

        // Version
        Text(
            text = "v1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = AltSendmeTheme.colors.textMuted
        )

        // Placeholder for language switcher (future feature)
        Spacer(modifier = Modifier.width(48.dp))
    }
}
