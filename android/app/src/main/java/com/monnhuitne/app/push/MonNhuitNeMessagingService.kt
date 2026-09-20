package com.monnhuitne.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Topic FCM auquel l'app s'abonne — voir docs/NOTIFICATIONS.md pour la config côté n8n. */
const val FCM_FAILURE_TOPIC = "workflow-failures"
private const val CHANNEL_ID = "workflow-failures"

/**
 * Reçoit les push FCM envoyées directement par n8n (Error Workflow → node
 * HTTP Request vers l'API FCM, topic $FCM_FAILURE_TOPIC) : aucun backend
 * intermédiaire, aucun token d'appareil à stocker nulle part.
 */
class MonNhuitNeMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Échec de workflow"
        val body = message.notification?.body ?: message.data["body"] ?: "Voir l'app pour le détail."
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        // Rien à faire : la diffusion passe par un topic, pas par token individuel.
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Échecs de workflow", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

/** Abonne l'appareil au topic de diffusion. Ne fait rien si Firebase n'est pas configuré
 *  (pas encore de google-services.json — voir docs/NOTIFICATIONS.md). */
fun subscribeToFailureTopic(context: Context) {
    if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return
    com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(FCM_FAILURE_TOPIC)
}
