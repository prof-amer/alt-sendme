package com.altsendme.app.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altsendme.app.sendme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ReceiveUiState(
    val transferState: TransferState = TransferState.IDLE,
    val ticket: String = "",
    val savePath: String? = null,
    val saveUri: Uri? = null,
    val progress: TransferProgress? = null,
    val fileNames: List<String> = emptyList(),
    val error: String? = null,
    val showErrorDialog: Boolean = false,
    val showInstructionsDialog: Boolean = false,
    val transferMetadata: TransferMetadata? = null
)

class ReceiveViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = _uiState.asStateFlow()

    private var transferStartTime: Long = 0
    private var downloadedFile: File? = null

    init {
        // Set default save path to Downloads
        _uiState.update {
            it.copy(savePath = "Downloads")
        }
    }

    fun onTicketChange(ticket: String) {
        _uiState.update { it.copy(ticket = ticket) }
    }

    fun onDirectorySelected(context: Context, uri: Uri) {
        // Take persistent permission
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)

        val documentFile = DocumentFile.fromTreeUri(context, uri)
        val displayName = documentFile?.name ?: uri.lastPathSegment ?: "Selected folder"

        _uiState.update {
            it.copy(
                saveUri = uri,
                savePath = displayName
            )
        }
    }

    fun startReceiving(context: Context) {
        val ticket = _uiState.value.ticket.trim()

        if (ticket.isEmpty()) {
            _uiState.update {
                it.copy(
                    error = "Please enter a ticket",
                    showErrorDialog = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(transferState = TransferState.PREPARING) }

            try {
                val sendme = SendmeLibrary.getInstance(context)

                // Validate ticket
                if (!sendme.isValidTicket(ticket)) {
                    throw Exception("Invalid ticket format")
                }

                transferStartTime = System.currentTimeMillis()

                // Determine output directory
                val outputDir = _uiState.value.saveUri?.let { uri ->
                    // For SAF URIs, we need to use a cache directory and then copy
                    File(context.cacheDir, "downloads").also { it.mkdirs() }
                } ?: File(context.getExternalFilesDir(null), "Downloads").also { it.mkdirs() }

                // Start receiving with event callbacks
                sendme.startReceiving(ticket, outputDir) { event ->
                    handleReceiveEvent(event, outputDir)
                }

                _uiState.update { it.copy(transferState = TransferState.LISTENING) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        transferState = TransferState.FAILED,
                        error = e.message ?: "Failed to start download",
                        showErrorDialog = true
                    )
                }
            }
        }
    }

    private fun handleReceiveEvent(event: TransferEvent, outputDir: File) {
        when (event) {
            is TransferEvent.Started -> {
                _uiState.update {
                    it.copy(transferState = TransferState.TRANSFERRING)
                }
            }

            is TransferEvent.Progress -> {
                _uiState.update {
                    it.copy(progress = event.progress)
                }
            }

            is TransferEvent.FileNames -> {
                _uiState.update {
                    it.copy(fileNames = event.names)
                }
            }

            is TransferEvent.Completed -> {
                val duration = System.currentTimeMillis() - transferStartTime
                val totalBytes = _uiState.value.progress?.totalBytes ?: 0
                val avgSpeed = if (duration > 0) {
                    totalBytes.toDouble() / (duration / 1000.0)
                } else 0.0

                val fileName = _uiState.value.fileNames.firstOrNull() ?: "received_file"
                downloadedFile = File(outputDir, fileName)

                _uiState.update {
                    it.copy(
                        transferState = TransferState.COMPLETED,
                        transferMetadata = TransferMetadata(
                            fileName = fileName,
                            fileSize = totalBytes,
                            duration = duration,
                            averageSpeed = avgSpeed,
                            outputPath = outputDir.absolutePath
                        )
                    )
                }
            }

            is TransferEvent.Failed -> {
                _uiState.update {
                    it.copy(
                        transferState = TransferState.FAILED,
                        error = event.error,
                        showErrorDialog = true
                    )
                }
            }
        }
    }

    fun stopReceiving() {
        viewModelScope.launch {
            _uiState.update { it.copy(transferState = TransferState.STOPPED) }
            // Note: In production, call sendme.stopReceive() here
        }
    }

    fun openDownloadFolder(context: Context) {
        try {
            downloadedFile?.let { file ->
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file.parentFile ?: file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "resource/folder")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open folder"))
                }
            } ?: run {
                // Open general file manager
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse("content://com.android.externalstorage.documents/root/primary"), "vnd.android.document/root")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            // Fallback: open default file manager
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Could not open file manager",
                        showErrorDialog = true
                    )
                }
            }
        }
    }

    fun resetForNewTransfer() {
        downloadedFile = null
        transferStartTime = 0
        _uiState.update {
            ReceiveUiState(
                savePath = it.savePath,
                saveUri = it.saveUri
            )
        }
    }

    fun showInstructions() {
        _uiState.update { it.copy(showInstructionsDialog = true) }
    }

    fun dismissInstructions() {
        _uiState.update { it.copy(showInstructionsDialog = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(showErrorDialog = false) }
    }
}
