package fr.chtilevia.android

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import fr.chtilevia.application.ChtiError

/** Tâche quotidienne : télécharge les horaires s'ils datent de plus de 24 h (même règle que le shell) */
class TimetableWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result = try {
        (applicationContext as ChtiApplication).chti.refreshTimetable()
        Result.success()
    } catch (_: ChtiError) {
        // Aucun référentiel et réseau indisponible : WorkManager réessaiera plus tard
        Result.retry()
    }

    companion object {
        const val NAME = "horaires"
    }
}
