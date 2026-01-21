package com.altsendme.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altsendme.app.R
import com.altsendme.app.sendme.TransferState
import com.altsendme.app.ui.components.*
import com.altsendme.app.ui.theme.AltSendmeTheme
import com.altsendme.app.viewmodel.ReceiveViewModel

@Composable
fun ReceiveScreen(
    onTransferStateChange: (Boolean) -> Unit,
    viewModel: ReceiveViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Directory picker launcher
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.onDirectorySelected(context, it) }
    }

    // Update parent about transfer state
    LaunchedEffect(uiState.transferState) {
        val isActive = uiState.transferState != TransferState.IDLE &&
                       uiState.transferState != TransferState.COMPLETED &&
                       uiState.transferState != TransferState.FAILED
        onTransferStateChange(isActive)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState.transferState) {
            TransferState.IDLE -> {
                IdleContent(
                    ticket = uiState.ticket,
                    savePath = uiState.savePath,
                    onTicketChange = { viewModel.onTicketChange(it) },
                    onBrowseFolder = { directoryPickerLauncher.launch(null) },
                    onStartDownload = { viewModel.startReceiving(context) },
                    onShowInstructions = { viewModel.showInstructions() }
                )
            }

            TransferState.PREPARING -> {
                PreparingContent()
            }

            TransferState.LISTENING, TransferState.TRANSFERRING -> {
                ReceivingContent(
                    isTransporting = uiState.transferState == TransferState.TRANSFERRING,
                    isCompleted = false,
                    progress = uiState.progress,
                    fileNames = uiState.fileNames,
                    onStopReceiving = { viewModel.stopReceiving() }
                )
            }

            TransferState.COMPLETED -> {
                TransferCompleteContent(
                    metadata = uiState.transferMetadata,
                    onNewTransfer = { viewModel.resetForNewTransfer() },
                    onOpenFolder = { viewModel.openDownloadFolder(context) }
                )
            }

            TransferState.FAILED -> {
                ErrorContent(
                    error = uiState.error ?: stringResource(R.string.error_receive_failed),
                    onRetry = { viewModel.resetForNewTransfer() }
                )
            }

            TransferState.STOPPED -> {
                StoppedContent(
                    onNewTransfer = { viewModel.resetForNewTransfer() }
                )
            }
        }
    }

    // Instructions dialog
    if (uiState.showInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissInstructions() },
            title = { Text(stringResource(R.string.receiver_how_to_receive)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InstructionItem(number = 1, text = stringResource(R.string.receiver_instruction_1))
                    InstructionItem(number = 2, text = stringResource(R.string.receiver_instruction_2))
                    InstructionItem(number = 3, text = stringResource(R.string.receiver_instruction_3))
                    InstructionItem(number = 4, text = stringResource(R.string.receiver_instruction_4))
                    InstructionItem(number = 5, text = stringResource(R.string.receiver_instruction_5))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissInstructions() }) {
                    Text(stringResource(R.string.ok))
                }
            },
            containerColor = AltSendmeTheme.colors.surface,
            titleContentColor = AltSendmeTheme.colors.textPrimary,
            textContentColor = AltSendmeTheme.colors.textSecondary
        )
    }

    // Error dialog
    if (uiState.showErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text(stringResource(R.string.error_receive_failed)) },
            text = { Text(uiState.error ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text(stringResource(R.string.ok))
                }
            },
            containerColor = AltSendmeTheme.colors.surface,
            titleContentColor = AltSendmeTheme.colors.textPrimary,
            textContentColor = AltSendmeTheme.colors.textSecondary
        )
    }
}

@Composable
private fun IdleContent(
    ticket: String,
    savePath: String?,
    onTicketChange: (String) -> Unit,
    onBrowseFolder: () -> Unit,
    onStartDownload: () -> Unit,
    onShowInstructions: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    // Header with info button
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.receiver_title),
            style = MaterialTheme.typography.titleLarge,
            color = AltSendmeTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onShowInstructions,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Instructions",
                tint = AltSendmeTheme.colors.textMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.receiver_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = AltSendmeTheme.colors.textMuted,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Save folder selection
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.receiver_save_to_folder),
            style = MaterialTheme.typography.bodySmall,
            color = AltSendmeTheme.colors.textMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AltSendmeTheme.colors.inputBackground)
                .border(
                    width = 1.dp,
                    color = AltSendmeTheme.colors.glassBorder,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onBrowseFolder() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = AltSendmeTheme.colors.textMuted,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = savePath ?: stringResource(R.string.receiver_no_folder_selected),
                style = MaterialTheme.typography.bodyMedium,
                color = if (savePath != null) AltSendmeTheme.colors.textSecondary
                        else AltSendmeTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = AltSendmeTheme.colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Ticket input
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.receiver_paste_ticket),
            style = MaterialTheme.typography.bodySmall,
            color = AltSendmeTheme.colors.textMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AltSendmeTheme.colors.inputBackground)
                .border(
                    width = 1.dp,
                    color = AltSendmeTheme.colors.glassBorder,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            BasicTextField(
                value = ticket,
                onValueChange = onTicketChange,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = AltSendmeTheme.colors.textPrimary,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(AltSendmeTheme.colors.primaryBright),
                decorationBox = { innerTextField ->
                    if (ticket.isEmpty()) {
                        Text(
                            text = stringResource(R.string.receiver_ticket_placeholder),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = AltSendmeTheme.colors.textHint
                        )
                    }
                    innerTextField()
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Start download button
    AppButton(
        text = stringResource(R.string.receiver_start_download),
        onClick = onStartDownload,
        enabled = ticket.isNotBlank(),
        style = ButtonStyle.PRIMARY,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                tint = AltSendmeTheme.colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

@Composable
private fun PreparingContent() {
    Spacer(modifier = Modifier.height(48.dp))

    CircularProgressIndicator(
        color = AltSendmeTheme.colors.accentLight,
        modifier = Modifier.size(48.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.receiver_connecting_to_sender),
        style = MaterialTheme.typography.bodyMedium,
        color = AltSendmeTheme.colors.textSecondary
    )
}

@Composable
private fun ReceivingContent(
    isTransporting: Boolean,
    isCompleted: Boolean,
    progress: com.altsendme.app.sendme.TransferProgress?,
    fileNames: List<String>,
    onStopReceiving: () -> Unit
) {
    // Pulse animation
    PulseAnimation(
        isTransporting = isTransporting,
        isCompleted = isCompleted,
        modifier = Modifier.padding(vertical = 16.dp)
    )

    // Status text
    Text(
        text = when {
            isCompleted -> stringResource(R.string.receiver_download_completed)
            isTransporting -> stringResource(R.string.receiver_downloading_in_progress)
            else -> stringResource(R.string.receiver_connecting_to_sender)
        },
        style = MaterialTheme.typography.titleMedium,
        color = when {
            isCompleted -> AltSendmeTheme.colors.statusCompleted
            isTransporting -> AltSendmeTheme.colors.statusActive
            else -> AltSendmeTheme.colors.textSecondary
        }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // File names
    if (fileNames.isNotEmpty()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Files:",
                    style = MaterialTheme.typography.bodySmall,
                    color = AltSendmeTheme.colors.textMuted
                )
                fileNames.take(3).forEach { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = AltSendmeTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (fileNames.size > 3) {
                    Text(
                        text = "... and ${fileNames.size - 3} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = AltSendmeTheme.colors.textMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Progress bar
    if (progress != null) {
        TransferProgressBar(progress = progress)
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Info text
    Text(
        text = stringResource(R.string.receiver_keep_app_open),
        style = MaterialTheme.typography.bodySmall,
        color = AltSendmeTheme.colors.textMuted,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Stop button
    AppButton(
        text = stringResource(R.string.receiver_stop_receiving),
        onClick = onStopReceiving,
        style = ButtonStyle.DESTRUCTIVE,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Stop,
                contentDescription = null,
                tint = AltSendmeTheme.colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

@Composable
private fun TransferCompleteContent(
    metadata: com.altsendme.app.sendme.TransferMetadata?,
    onNewTransfer: () -> Unit,
    onOpenFolder: () -> Unit
) {
    Icon(
        imageVector = Icons.Outlined.CheckCircle,
        contentDescription = null,
        tint = AltSendmeTheme.colors.statusCompleted,
        modifier = Modifier.size(64.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.transfer_complete),
        style = MaterialTheme.typography.titleLarge,
        color = AltSendmeTheme.colors.statusCompleted
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.transfer_success_message),
        style = MaterialTheme.typography.bodyMedium,
        color = AltSendmeTheme.colors.textSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Transfer metadata
    metadata?.let { meta ->
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetadataRow(
                    label = stringResource(R.string.transfer_file_name),
                    value = meta.fileName
                )
                MetadataRow(
                    label = stringResource(R.string.transfer_file_size),
                    value = formatBytes(meta.fileSize)
                )
                MetadataRow(
                    label = stringResource(R.string.transfer_duration),
                    value = formatDuration(meta.duration)
                )
                MetadataRow(
                    label = stringResource(R.string.transfer_avg_speed),
                    value = formatSpeed(meta.averageSpeed)
                )
                meta.outputPath?.let { path ->
                    MetadataRow(
                        label = stringResource(R.string.transfer_download_path),
                        value = path
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Open folder button
    AppButton(
        text = stringResource(R.string.transfer_open),
        onClick = onOpenFolder,
        style = ButtonStyle.SECONDARY,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = AltSendmeTheme.colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // New transfer button
    AppButton(
        text = stringResource(R.string.transfer_new_transfer),
        onClick = onNewTransfer,
        style = ButtonStyle.PRIMARY,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Icon(
        imageVector = Icons.Outlined.Error,
        contentDescription = null,
        tint = AltSendmeTheme.colors.statusError,
        modifier = Modifier.size(64.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.error_receive_failed),
        style = MaterialTheme.typography.titleLarge,
        color = AltSendmeTheme.colors.statusError
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = error,
        style = MaterialTheme.typography.bodyMedium,
        color = AltSendmeTheme.colors.textSecondary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    AppButton(
        text = stringResource(R.string.transfer_new_transfer),
        onClick = onRetry,
        style = ButtonStyle.PRIMARY,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StoppedContent(
    onNewTransfer: () -> Unit
) {
    Icon(
        imageVector = Icons.Outlined.Stop,
        contentDescription = null,
        tint = AltSendmeTheme.colors.textMuted,
        modifier = Modifier.size(64.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.transfer_stopped),
        style = MaterialTheme.typography.titleLarge,
        color = AltSendmeTheme.colors.textSecondary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.transfer_was_stopped),
        style = MaterialTheme.typography.bodyMedium,
        color = AltSendmeTheme.colors.textMuted,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    AppButton(
        text = stringResource(R.string.transfer_new_transfer),
        onClick = onNewTransfer,
        style = ButtonStyle.PRIMARY,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InstructionItem(number: Int, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            color = AltSendmeTheme.colors.textSecondary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = AltSendmeTheme.colors.textSecondary
        )
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AltSendmeTheme.colors.textMuted,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = AltSendmeTheme.colors.textPrimary,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${seconds}s"
    }
}
