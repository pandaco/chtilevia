package fr.chtilevia.android

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Réponse des boutons du widget, sans écran par-dessus l'accueil : service court (Android le laisse
 * tourner le temps de la réponse, sans geler l'appli) qui lance la commande, affiche la réponse
 * écrite compacte (`--court`) en notification et, pour « Parler », lit la réponse à voix haute.
 */
class AnswerService : Service() {
    private val main = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val question = intent?.getStringExtra(EXTRA_QUESTION).orEmpty()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE else 0
        ServiceCompat.startForeground(this, Answers.WORKING_ID, Answers.working(this, question), type)
        val args = intent?.getStringArrayListExtra(EXTRA_ARGS)
        val message = intent?.getStringExtra(EXTRA_MESSAGE)
        val speak = intent?.getBooleanExtra(EXTRA_SPEAK, false) == true
        val app = application as ChtiApplication
        app.worker.execute {
            // Voix d'abord : c'est elle qu'on attend après « Parler »
            val spoken = if (speak) message ?: args?.let { clean(app.chti.run(listOf("--voix") + it).text) } else null
            spoken?.let { main.post { app.speaker.say(it) { main.post { stopSelf(startId) } } } }
            val written = message ?: args?.let { clean(app.chti.run(listOf("--court") + it).text) }.orEmpty()
            main.post {
                Answers.show(this, question, written.ifEmpty { NO_ANSWER }, args)
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                if (spoken == null) stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    /** Service court dépassé (3 minutes) : on s'arrête */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf(startId)
    }

    @Deprecated("Remplacée par onTimeout(Int, Int) à partir d'Android 15")
    override fun onTimeout(startId: Int) {
        stopSelf(startId)
    }

    companion object {
        private const val EXTRA_QUESTION = "question"
        private const val EXTRA_ARGS = "arguments"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_SPEAK = "voix"
        private const val NO_ANSWER = "Désolé, je n'ai pas de réponse."

        /** Sans la ligne de mise à jour du référentiel, inutile à lire */
        private fun clean(text: String) =
            text.lines().filterNot { it.startsWith("Mise à jour du référentiel") }.joinToString("\n").trim().ifEmpty { NO_ANSWER }

        /** Lance la commande chti [args] (sans --voix ni --court) ; à appeler depuis un écran au premier plan */
        fun answer(context: Context, question: String, args: List<String>, speak: Boolean) =
            start(context, Intent(context, AnswerService::class.java).putStringArrayListExtra(EXTRA_ARGS, ArrayList(args)), question, speak)

        /** Rend un message tout fait (ex. position introuvable) comme une réponse */
        fun say(context: Context, question: String, message: String, speak: Boolean) =
            start(context, Intent(context, AnswerService::class.java).putExtra(EXTRA_MESSAGE, message), question, speak)

        private fun start(context: Context, intent: Intent, question: String, speak: Boolean) {
            ContextCompat.startForegroundService(context, intent.putExtra(EXTRA_QUESTION, question).putExtra(EXTRA_SPEAK, speak))
        }
    }
}
