package fr.chtilevia.android

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Un seul cœur par processus : le référentiel en mémoire sert à l'écran comme au widget */
class ChtiApplication : Application() {
    val chti by lazy { AndroidChti(this) }

    /** Voix partagée par l'écran et le widget : une réponse coupe la précédente */
    val speaker by lazy { Speaker(this) }

    /** Fil des commandes : réseau et fichiers hors du fil principal, une commande à la fois */
    val worker: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "chti") }

    override fun onCreate() {
        super.onCreate()
        // Horaires mis à jour en arrière-plan, en Wi-Fi : la question n'attend jamais le téléchargement
        val daily = PeriodicWorkRequestBuilder<TimetableWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(TimetableWorker.NAME, ExistingPeriodicWorkPolicy.KEEP, daily)
    }
}
