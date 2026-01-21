package com.altsendme.app.sendme

import java.io.File

/**
 * Transfer progress data
 */
data class TransferProgress(
    val bytesTransferred: Long,
    val totalBytes: Long,
    val speedBps: Double,
    val percentage: Float,
    val etaSeconds: Long?
) {
    companion object {
        fun fromPayload(payload: String): TransferProgress? {
            return try {
                val parts = payload.split(":")
                if (parts.size >= 3) {
                    val bytesTransferred = parts[0].toLong()
                    val totalBytes = parts[1].toLong()
                    val speedBps = parts[2].toLong() / 1000.0 // Convert back from int
                    val percentage = if (totalBytes > 0) {
                        (bytesTransferred.toFloat() / totalBytes.toFloat()) * 100f
                    } else 0f
                    val etaSeconds = if (speedBps > 0 && totalBytes > bytesTransferred) {
                        ((totalBytes - bytesTransferred) / speedBps).toLong()
                    } else null
                    TransferProgress(bytesTransferred, totalBytes, speedBps, percentage, etaSeconds)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Result of starting a share operation
 */
data class SendResult(
    val ticket: String,
    val hash: String,
    val size: Long,
    val entryType: String // "file" or "directory"
)

/**
 * Result of a receive operation
 */
data class ReceiveResult(
    val message: String,
    val filePath: File
)

/**
 * Options for sending
 */
data class SendOptions(
    val relayMode: RelayMode = RelayMode.Default
)

/**
 * Options for receiving
 */
data class ReceiveOptions(
    val outputDir: File? = null,
    val relayMode: RelayMode = RelayMode.Default
)

/**
 * Relay mode configuration
 */
sealed class RelayMode {
    object Disabled : RelayMode()
    object Default : RelayMode()
    data class Custom(val url: String) : RelayMode()
}

/**
 * Transfer state enum
 */
enum class TransferState {
    IDLE,
    PREPARING,
    LISTENING,
    TRANSFERRING,
    COMPLETED,
    FAILED,
    STOPPED
}

/**
 * Transfer metadata for completion screen
 */
data class TransferMetadata(
    val fileName: String,
    val fileSize: Long,
    val duration: Long, // in milliseconds
    val averageSpeed: Double, // bytes per second
    val isDirectory: Boolean = false,
    val outputPath: String? = null
)

/**
 * Event types emitted during transfer
 */
sealed class TransferEvent {
    object Started : TransferEvent()
    data class Progress(val progress: TransferProgress) : TransferEvent()
    object Completed : TransferEvent()
    data class Failed(val error: String) : TransferEvent()
    data class FileNames(val names: List<String>) : TransferEvent()
}
