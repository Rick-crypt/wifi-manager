package com.example.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class PortalServerService : Service() {

    companion object {
        const val CHANNEL_ID = "PortalServerChannel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_PORTAL_SERVER"
        const val ACTION_STOP = "ACTION_STOP_PORTAL_SERVER"
        var serverInstance: EmbeddedPortalServer? = null
            private set

        fun startService(context: Context, localIp: String = "192.168.1.105") {
            val intent = Intent(context, PortalServerService::class.java).apply {
                action = ACTION_START
                putExtra("LOCAL_IP", localIp)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PortalServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            serverInstance?.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val localIp = intent?.getStringExtra("LOCAL_IP") ?: "192.168.1.105"

        if (serverInstance == null) {
            serverInstance = EmbeddedPortalServer(applicationContext)
        }

        serverInstance?.start(localIp)

        val notification = createNotification(localIp)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        serverInstance?.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Service du Portail Captif",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintient le serveur HTTP du portail captif actif sur le réseau local"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(localIp: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Portail Captif Actif 📶")
            .setContentText("Serveur Web local sur http://$localIp:8080")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
