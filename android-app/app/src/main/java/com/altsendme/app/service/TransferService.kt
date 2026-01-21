package com.altsendme.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.altsendme.app.AltSendmeApplication
import com.altsendme.app.MainActivity
import com.altsendme.app.R
import kotlinx.coroutines.*

/**
 * Foreground service for handling file transfers in the background.
 * This ensures transfers continue even when the app is in the background.
 */
class TransferService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_SEND = "com.altsendme.app.action.START_SEND"
        const val ACTION_START_RECEIVE = "com.altsendme.app.action.START_RECEIVE"
        const val ACTION_STOP = "com.altsendme.app.action.STOP"

        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_TICKET = "ticket"
        const val EXTRA_OUTPUT_DIR = "output_dir"
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SEND -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                if (filePath != null) {
                    startForegroundWithNotification(isSending = true)
                    // Transfer logic would go here
                }
            }
            ACTION_START_RECEIVE -> {
                val ticket = intent.getStringExtra(EXTRA_TICKET)
                val outputDir = intent.getStringExtra(EXTRA_OUTPUT_DIR)
                if (ticket != null) {
                    startForegroundWithNotification(isSending = false)
                    // Transfer logic would go here
                }
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun startForegroundWithNotification(isSending: Boolean) {
        val notification = createNotification(isSending)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(isSending: Boolean): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, TransferService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSending) {
            getString(R.string.notification_sending)
        } else {
            getString(R.string.notification_receiving)
        }

        return NotificationCompat.Builder(this, AltSendmeApplication.NOTIFICATION_CHANNEL_TRANSFERS)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.cancel),
                stopIntent
            )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateProgress(progress: Int, total: Int) {
        val notification = NotificationCompat.Builder(this, AltSendmeApplication.NOTIFICATION_CHANNEL_TRANSFERS)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Transferring...")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setProgress(total, progress, false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
