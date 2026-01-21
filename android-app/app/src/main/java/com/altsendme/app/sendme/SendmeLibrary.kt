package com.altsendme.app.sendme

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.security.SecureRandom
import kotlin.math.min

/**
 * Sendme library interface for P2P file transfer.
 *
 * This implementation provides a simulation of the Iroh-based P2P transfer functionality.
 * In production, this would be replaced with actual UniFFI bindings to the Rust sendme library.
 *
 * To integrate actual Iroh functionality:
 * 1. Build the sendme Rust library with uniffi-bindgen for Android targets
 * 2. Add the generated Kotlin bindings
 * 3. Include the native .so libraries for arm64-v8a, armeabi-v7a, x86_64
 */
class SendmeLibrary private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var currentSendJob: Job? = null
    private var currentReceiveJob: Job? = null

    private val _sendEvents = MutableSharedFlow<TransferEvent>()
    val sendEvents: SharedFlow<TransferEvent> = _sendEvents.asSharedFlow()

    private val _receiveEvents = MutableSharedFlow<TransferEvent>()
    val receiveEvents: SharedFlow<TransferEvent> = _receiveEvents.asSharedFlow()

    companion object {
        @Volatile
        private var instance: SendmeLibrary? = null

        fun getInstance(context: Context): SendmeLibrary {
            return instance ?: synchronized(this) {
                instance ?: SendmeLibrary(context.applicationContext).also { instance = it }
            }
        }

        // Ticket prefix for validation
        private const val TICKET_PREFIX = "blob"
    }

    /**
     * Start sharing a file or directory
     */
    suspend fun startShare(
        path: File,
        options: SendOptions = SendOptions()
    ): Result<SendResult> = withContext(Dispatchers.IO) {
        try {
            // Validate path exists
            if (!path.exists()) {
                return@withContext Result.failure(Exception("Path does not exist: ${path.absolutePath}"))
            }

            // Calculate total size
            val totalSize = if (path.isDirectory) {
                path.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else {
                path.length()
            }

            // Generate a mock ticket (in production, this comes from Iroh)
            val ticket = generateMockTicket(path, totalSize)
            val hash = generateHash()

            val result = SendResult(
                ticket = ticket,
                hash = hash,
                size = totalSize,
                entryType = if (path.isDirectory) "directory" else "file"
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start listening for incoming connections after share is created
     */
    fun startListening(sendResult: SendResult, onEvent: (TransferEvent) -> Unit) {
        currentSendJob?.cancel()
        currentSendJob = scope.launch {
            try {
                // Emit events to the shared flow
                _sendEvents.emit(TransferEvent.Started)
                onEvent(TransferEvent.Started)

                // Simulate waiting for connection and transfer
                // In production, this would be driven by actual Iroh events
                simulateSendTransfer(sendResult.size, onEvent)

            } catch (e: CancellationException) {
                // Transfer was cancelled
            } catch (e: Exception) {
                _sendEvents.emit(TransferEvent.Failed(e.message ?: "Unknown error"))
                onEvent(TransferEvent.Failed(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Stop the current share
     */
    fun stopShare() {
        currentSendJob?.cancel()
        currentSendJob = null
    }

    /**
     * Receive a file using a ticket
     */
    suspend fun receive(
        ticket: String,
        options: ReceiveOptions
    ): Result<ReceiveResult> = withContext(Dispatchers.IO) {
        try {
            // Validate ticket format
            if (!isValidTicket(ticket)) {
                return@withContext Result.failure(Exception("Invalid ticket format"))
            }

            // Parse ticket to get expected size (mock implementation)
            val expectedSize = parseTicketSize(ticket)

            val outputDir = options.outputDir
                ?: File(context.getExternalFilesDir(null), "Downloads")

            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // Emit start event
            _receiveEvents.emit(TransferEvent.Started)

            // Simulate file names event
            val fileNames = listOf(parseTicketFileName(ticket))
            _receiveEvents.emit(TransferEvent.FileNames(fileNames))

            // Simulate download progress
            simulateReceiveTransfer(expectedSize)

            // Create mock received file
            val fileName = parseTicketFileName(ticket)
            val outputFile = File(outputDir, fileName)

            // In production, the file would be written by Iroh
            // For simulation, we just create an empty file marker
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }

            _receiveEvents.emit(TransferEvent.Completed)

            Result.success(ReceiveResult(
                message = "Downloaded successfully",
                filePath = outputFile
            ))
        } catch (e: CancellationException) {
            Result.failure(Exception("Download cancelled"))
        } catch (e: Exception) {
            _receiveEvents.emit(TransferEvent.Failed(e.message ?: "Unknown error"))
            Result.failure(e)
        }
    }

    /**
     * Start receiving with callback
     */
    fun startReceiving(
        ticket: String,
        outputDir: File?,
        onEvent: (TransferEvent) -> Unit
    ) {
        currentReceiveJob?.cancel()
        currentReceiveJob = scope.launch {
            try {
                val options = ReceiveOptions(outputDir = outputDir)

                // Collect events and forward to callback
                launch {
                    receiveEvents.collect { event ->
                        onEvent(event)
                    }
                }

                receive(ticket, options)
            } catch (e: CancellationException) {
                // Cancelled
            }
        }
    }

    /**
     * Stop the current receive operation
     */
    fun stopReceive() {
        currentReceiveJob?.cancel()
        currentReceiveJob = null
    }

    /**
     * Validate ticket format
     */
    fun isValidTicket(ticket: String): Boolean {
        val trimmed = ticket.trim()
        // Basic validation - in production this would parse the actual BlobTicket format
        return trimmed.isNotEmpty() &&
               (trimmed.startsWith(TICKET_PREFIX) || trimmed.length > 50)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        instance = null
    }

    // ==================== Private Helper Methods ====================

    private fun generateMockTicket(path: File, size: Long): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val hash = bytes.joinToString("") { "%02x".format(it) }
        val fileName = path.name.replace(" ", "_")
        // Mock ticket format: blob<hash>:<filename>:<size>
        return "blob$hash:$fileName:$size"
    }

    private fun generateHash(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun parseTicketSize(ticket: String): Long {
        return try {
            val parts = ticket.split(":")
            if (parts.size >= 3) parts.last().toLong() else 1024 * 1024 // Default 1MB
        } catch (e: Exception) {
            1024 * 1024
        }
    }

    private fun parseTicketFileName(ticket: String): String {
        return try {
            val parts = ticket.split(":")
            if (parts.size >= 2) parts[1] else "received_file"
        } catch (e: Exception) {
            "received_file"
        }
    }

    private suspend fun simulateSendTransfer(totalSize: Long, onEvent: (TransferEvent) -> Unit) {
        var transferred = 0L
        val startTime = System.currentTimeMillis()
        val chunkSize = maxOf(totalSize / 100, 1024L) // At least 1KB chunks

        while (transferred < totalSize) {
            delay(50) // Simulate network delay

            transferred = min(transferred + chunkSize, totalSize)
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
            val speed = if (elapsed > 0) transferred / elapsed else 0.0
            val percentage = (transferred.toFloat() / totalSize.toFloat()) * 100f
            val eta = if (speed > 0) ((totalSize - transferred) / speed).toLong() else null

            val progress = TransferProgress(
                bytesTransferred = transferred,
                totalBytes = totalSize,
                speedBps = speed,
                percentage = percentage,
                etaSeconds = eta
            )

            _sendEvents.emit(TransferEvent.Progress(progress))
            onEvent(TransferEvent.Progress(progress))
        }

        _sendEvents.emit(TransferEvent.Completed)
        onEvent(TransferEvent.Completed)
    }

    private suspend fun simulateReceiveTransfer(totalSize: Long) {
        var transferred = 0L
        val startTime = System.currentTimeMillis()
        val chunkSize = maxOf(totalSize / 100, 1024L)

        while (transferred < totalSize) {
            delay(50)

            transferred = min(transferred + chunkSize, totalSize)
            val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
            val speed = if (elapsed > 0) transferred / elapsed else 0.0
            val percentage = (transferred.toFloat() / totalSize.toFloat()) * 100f
            val eta = if (speed > 0) ((totalSize - transferred) / speed).toLong() else null

            val progress = TransferProgress(
                bytesTransferred = transferred,
                totalBytes = totalSize,
                speedBps = speed,
                percentage = percentage,
                etaSeconds = eta
            )

            _receiveEvents.emit(TransferEvent.Progress(progress))
        }
    }
}
