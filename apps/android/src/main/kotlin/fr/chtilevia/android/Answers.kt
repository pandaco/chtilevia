package fr.chtilevia.android

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Notifications de l'appli : la recherche en cours, puis la réponse, qui s'affiche en haut de l'écran */
object Answers {
    private const val ANSWERS = "reponses_ecran"
    private const val WORKING = "recherche"
    const val ANSWER_ID = 1
    const val WORKING_ID = 2

    private fun channels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // Ancien canal sans affichage en haut de l'écran (importance non modifiable une fois créée)
        manager.deleteNotificationChannel("reponses")
        manager.createNotificationChannel(
            NotificationChannel(ANSWERS, "Réponses", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Réponses aux questions et aux favoris, affichées en haut de l'écran, sans son"
                setSound(null, null)
                enableVibration(false)
            },
        )
        manager.createNotificationChannel(NotificationChannel(WORKING, "Recherche en cours", NotificationManager.IMPORTANCE_LOW))
    }

    /** Notification du service pendant la recherche */
    fun working(context: Context, question: String): Notification {
        channels(context)
        return NotificationCompat.Builder(context, WORKING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(question)
            .setContentText("Je cherche…")
            .setProgress(0, 0, true)
            .build()
    }

    /**
     * La réponse, en notification ; en message à l'écran si les notifications ne sont pas autorisées.
     * La toucher ouvre l'appli sur la même question ([args] : la commande chti, relancée).
     */
    fun show(context: Context, question: String, message: String, args: List<String>? = null) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            return
        }
        channels(context)
        // Android n'affiche jamais une notification dépliée d'office en haut de l'écran : on lui donne
        // une vue qui montre toute la réponse, quelques lignes au plus, aussi dans le volet
        val full = RemoteViews(context.packageName, R.layout.notification_answer).apply {
            setTextViewText(R.id.answer, message)
        }
        val notification = NotificationCompat.Builder(context, ANSWERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Brand.red.toArgb())
            .setContentTitle(question)
            .setContentText(message)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomHeadsUpContentView(full)
            .setCustomBigContentView(full)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(MainActivity.reopen(context, question, args))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ANSWER_ID, notification)
    }
}
