package com.altsendme.app.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altsendme.app.sendme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class SendUiState(
    val transferState: TransferState = TransferState.IDLE,
    val selectedUri: Uri? = null,
    val selectedFileName: String? = null,
    val selectedFileSize: Long? = null,
    val ticket: String? = null,
    val progress: TransferProgress? = null,
    val copySuccess: Boolean = false,
    val error: String? = null,
    val showErrorDialog: Boolean = false,
    val transferMetadata: TransferMetadata? = null
)

class SendViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    private var sendResult: SendResult? = null
    private var transferStartTime: Long = 0

    fun onFileSelected(context: Context, uri: Uri) {
        val fileName = getFileName(context, uri)
        val fileSize = getFileSize(context, uri)

        _uiState.update {
            it.copy(
                selectedUri = uri,
                selectedFileName = fileName,
                selectedFileSize = fileSize,
                error = null
            )
        }
    }

    fun startSharing(context: Context) {
        val uri = _uiState.value.selectedUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(transferState = TransferState.PREPARING) }

            try {
                val sendme = SendmeLibrary.getInstance(context)

                // Copy file to cache directory for sharing
                val cacheFile = copyUriToCache(context, uri)
                    ?: throw Exception("Failed to prepare file for sharing")

                val result = sendme.startShare(cacheFile).getOrThrow()
                sendResult = result
                transferStartTime = System.currentTimeMillis()

                _uiState.update {
                    it.copy(
                        transferState = TransferState.LISTENING,
                        ticket = result.ticket
                    )
                }

                // Start listening for connections
                sendme.startListening(result) { event ->
                    handleSendEvent(event)
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        transferState = TransferState.FAILED,
                        error = e.message ?: "Failed to start sharing",
                        showErrorDialog = true
                    )
                }
            }
        }
    }

    private fun handleSendEvent(event: TransferEvent) {
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

            is TransferEvent.Completed -> {
                val duration = System.currentTimeMillis() - transferStartTime
                val totalBytes = sendResult?.size ?: 0
                val avgSpeed = if (duration > 0) {
                    totalBytes.toDouble() / (duration / 1000.0)
                } else 0.0

                _uiState.update {
                    it.copy(
                        transferState = TransferState.COMPLETED,
                        transferMetadata = TransferMetadata(
                            fileName = it.selectedFileName ?: "Unknown",
                            fileSize = totalBytes,
                            duration = duration,
                            averageSpeed = avgSpeed,
                            isDirectory = sendResult?.entryType == "directory"
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

            else -> {}
        }
    }

    fun stopSharing() {
        viewModelScope.launch {
            _uiState.update { it.copy(transferState = TransferState.STOPPED) }
            // Note: In production, call sendme.stopShare() here
        }
    }

    fun onTicketCopied() {
        viewModelScope.launch {
            _uiState.update { it.copy(copySuccess = true) }
            delay(2000)
            _uiState.update { it.copy(copySuccess = false) }
        }
    }

    fun resetForNewTransfer() {
        sendResult = null
        transferStartTime = 0
        _uiState.update { SendUiState() }
    }

    fun dismissError() {
        _uiState.update { it.copy(showErrorDialog = false) }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        cursor.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    private fun getFileSize(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        cursor.getLong(sizeIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun copyUriToCache(context: Context, uri: Uri): File? {
        return try {
            val fileName = getFileName(context, uri) ?: "shared_file"
            val cacheFile = File(context.cacheDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            cacheFile
        } catch (e: Exception) {
            null
        }
    }
}
