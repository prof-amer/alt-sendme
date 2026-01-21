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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altsendme.app.R
import com.altsendme.app.sendme.TransferState
import com.altsendme.app.ui.components.*
import com.altsendme.app.ui.theme.AltSendmeTheme
import com.altsendme.app.viewmodel.SendViewModel

@Composable
fun SendScreen(
    onTransferStateChange: (Boolean) -> Unit,
    viewModel: SendViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onFileSelected(context, it) }
    }

    // Multiple files picker
    val multipleFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onFileSelected(context, uris.first())
        }
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
                    selectedFileName = uiState.selectedFileName,
                    selectedFileSize = uiState.selectedFileSize,
                    onBrowseFile = { filePickerLauncher.launch("*/*") },
                    onStartSharing = { viewModel.startSharing(context) },
                    isFileSelected = uiState.selectedUri != null
                )
            }

            TransferState.PREPARING -> {
                PreparingContent()
            }

            TransferState.LISTENING, TransferState.TRANSFERRING -> {
                SharingContent(
                    ticket = uiState.ticket,
                    isTransporting = uiState.transferState == TransferState.TRANSFERRING,
                    isCompleted = false,
                    progress = uiState.progress,
                    copySuccess = uiState.copySuccess,
                    onCopyTicket = {
                        uiState.ticket?.let {
                            clipboardManager.setText(AnnotatedString(it))
                            viewModel.onTicketCopied()
                        }
                    },
                    onStopSharing = { viewModel.stopSharing() }
                )
            }

            TransferState.COMPLETED -> {
                TransferCompleteContent(
                    metadata = uiState.transferMetadata,
                    onNewTransfer = { viewModel.resetForNewTransfer() }
                )
            }

            TransferState.FAILED -> {
                ErrorContent(
                    error = uiState.error ?: stringResource(R.string.error_sharing_failed),
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

    // Error dialog
    if (uiState.showErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text(stringResource(R.string.error_sharing_failed)) },
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
    selectedFileName: String?,
    selectedFileSize: Long?,
    onBrowseFile: () -> Unit,
    onStartSharing: () -> Unit,
    isFileSelected: Boolean
) {
    // Header
    Text(
        text = stringResource(R.string.sender_title),
        style = MaterialTheme.typography.titleLarge,
        color = AltSendmeTheme.colors.textPrimary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.sender_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = AltSendmeTheme.colors.textMuted,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    // File selection area
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AltSendmeTheme.colors.inputBackground)
            .border(
                width = 1.dp,
                color = if (isFileSelected) AltSendmeTheme.colors.primaryBright else AltSendmeTheme.colors.glassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onBrowseFile() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isFileSelected && selectedFileName != null) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = AltSendmeTheme.colors.statusActive,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.sender_file_selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AltSendmeTheme.colors.statusActive
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedFileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = AltSendmeTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                selectedFileSize?.let { size ->
                    Text(
                        text = formatBytes(size),
                        style = MaterialTheme.typography.bodySmall,
                        color = AltSendmeTheme.colors.textMuted
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.CloudUpload,
                    contentDescription = null,
                    tint = AltSendmeTheme.colors.textMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.sender_select_files),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AltSendmeTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.sender_or_browse),
                    style = MaterialTheme.typography.bodySmall,
                    color = AltSendmeTheme.colors.textMuted
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Browse button
    AppButton(
        text = stringResource(R.string.sender_browse_file),
        onClick = onBrowseFile,
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

    // Start sharing button
    AppButton(
        text = stringResource(R.string.sender_start_sharing),
        onClick = onStartSharing,
        enabled = isFileSelected,
        style = ButtonStyle.PRIMARY,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Share,
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
        text = stringResource(R.string.sender_preparing_for_transport),
        style = MaterialTheme.typography.bodyMedium,
        color = AltSendmeTheme.colors.textSecondary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.sender_please_wait_processing),
        style = MaterialTheme.typography.bodySmall,
        color = AltSendmeTheme.colors.textMuted,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SharingContent(
    ticket: String?,
    isTransporting: Boolean,
    isCompleted: Boolean,
    progress: com.altsendme.app.sendme.TransferProgress?,
    copySuccess: Boolean,
    onCopyTicket: () -> Unit,
    onStopSharing: () -> Unit
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
            isCompleted -> stringResource(R.string.sender_transfer_completed)
            isTransporting -> stringResource(R.string.sender_sharing_in_progress)
            else -> stringResource(R.string.sender_listening_for_connection)
        },
        style = MaterialTheme.typography.titleMedium,
        color = when {
            isCompleted -> AltSendmeTheme.colors.statusCompleted
            isTransporting -> AltSendmeTheme.colors.statusActive
            else -> AltSendmeTheme.colors.textSecondary
        }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Ticket display
    ticket?.let {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.sender_share_this_ticket),
                style = MaterialTheme.typography.bodySmall,
                color = AltSendmeTheme.colors.textMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AltSendmeTheme.colors.inputBackground)
                    .border(
                        width = 1.dp,
                        color = AltSendmeTheme.colors.glassBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = ticket,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = AltSendmeTheme.colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AppButton(
                text = if (copySuccess) stringResource(R.string.sender_copied)
                       else stringResource(R.string.sender_copy_to_clipboard),
                onClick = onCopyTicket,
                style = if (copySuccess) ButtonStyle.PRIMARY else ButtonStyle.SECONDARY,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = if (copySuccess) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        tint = AltSendmeTheme.colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Progress bar (only when transferring)
    if (isTransporting && progress != null) {
        TransferProgressBar(progress = progress)
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Info text
    Text(
        text = stringResource(R.string.sender_keep_app_open),
        style = MaterialTheme.typography.bodySmall,
        color = AltSendmeTheme.colors.textMuted,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Stop button
    AppButton(
        text = stringResource(R.string.sender_stop_sharing),
        onClick = onStopSharing,
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
    onNewTransfer: () -> Unit
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
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

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
        text = stringResource(R.string.error_sharing_failed),
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
            color = AltSendmeTheme.colors.textMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = AltSendmeTheme.colors.textPrimary
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
