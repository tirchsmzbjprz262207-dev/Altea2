package com.altea.salud

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

/**
 * Servicio persistente de notificación. La conexión BLE se integrará aquí
 * con el administrador GATT en la siguiente iteración del firmware.
 * Android puede detener servicios por batería/fabricante; no existe garantía absoluta.
 */
class MonitoringService : Service() {
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel("altea_monitor", "Monitoreo ALTEA", NotificationManager.IMPORTANCE_LOW))
        }
        val notification: Notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, "altea_monitor")
                .setContentTitle("ALTEA está activo")
                .setContentText("Servicio de monitoreo en segundo plano")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setContentTitle("ALTEA está activo")
                .setContentText("Servicio de monitoreo en segundo plano")
                .setSmallIcon(android.R.drawable.ic_dialog_info).build()
        }
        startForeground(41, notification)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}
