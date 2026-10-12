package fr.chtilevia.android

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Locale

/** Où en est une question : ce qui a été demandé, ce que l'on fait, la réponse */
data class Conversation(
    val question: String = "",
    val status: String? = null,
    val answer: String = "",
) {
    val busy get() = status != null
}

/**
 * Une question, de l'écoute à la réponse, pour l'écran principal comme pour le widget : la
 * reconnaissance vocale (français), la phrase comprise comme `chti vocal`, la position du téléphone
 * quand elle parle d'« à proximité », puis la réponse en mode voix, affichée, lue si [speak] et
 * gardée en notification. À créer dans `onCreate` (les demandes à Android y sont enregistrées).
 */
class QuestionFlow(
    private val activity: ComponentActivity,
    private val speak: Boolean,
    private val onUpdate: (Conversation) -> Unit,
) {
    private val app = activity.application as ChtiApplication
    private val main = Handler(Looper.getMainLooper())
    private var conversation = Conversation()
    private var waitingForPosition: String? = null

    private val listening = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val phrase = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (phrase == null) update(Conversation()) else ask(phrase)
    }

    private val locationPermission =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            val phrase = waitingForPosition ?: return@registerForActivityResult
            waitingForPosition = null
            if (granted.values.any { it }) answerNearby(phrase) else finish("Sans accès à ta position, je ne sais pas ce qui est à proximité.")
        }

    /** Ouvre la reconnaissance vocale ; false si le téléphone n'en a pas */
    fun listen(): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Ex. : prochain tram à Croisé Laroche vers Roubaix")
        return try {
            update(Conversation(status = "J'écoute…"))
            listening.launch(intent)
            true
        } catch (_: ActivityNotFoundException) {
            update(Conversation())
            false
        }
    }

    /** Phrase libre, dite ou tapée */
    fun ask(phrase: String) {
        update(Conversation(question = phrase, status = "Je cherche…"))
        app.worker.execute {
            val nearby = app.chti.interpret(phrase) == listOf("autour")
            main.post {
                when {
                    !nearby -> run(listOf("vocal", phrase))
                    hasLocationPermission() -> answerNearby(phrase)
                    else -> {
                        waitingForPosition = phrase
                        locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }
            }
        }
    }

    /** Favori, réponse en mode voix */
    fun favorite(name: String) {
        update(Conversation(question = name, status = "Je cherche…"))
        run(listOf("--voix", name))
    }

    private fun hasLocationPermission() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun answerNearby(phrase: String) {
        update(conversation.copy(question = phrase, status = "Je cherche ta position…"))
        app.worker.execute {
            val location = DeviceLocation.current(app)
            main.post {
                if (location == null) {
                    finish("Je ne trouve pas ta position : active la localisation et réessaie.")
                } else {
                    // Notation décimale fixe, point comme séparateur, quelle que soit la langue du téléphone
                    run(listOf("--voix", "autour", "%.6f".format(Locale.ROOT, location.latitude), "%.6f".format(Locale.ROOT, location.longitude)))
                }
            }
        }
    }

    private fun run(args: List<String>) {
        app.worker.execute {
            val reply = app.chti.run(args)
            // Sans la ligne de mise à jour du référentiel, inutile à lire
            val message = reply.text.lines().filterNot { it.startsWith("Mise à jour du référentiel") }
                .joinToString("\n").trim().ifEmpty { "Désolé, je n'ai pas de réponse." }
            main.post { finish(message) }
        }
    }

    private fun finish(message: String) {
        update(conversation.copy(status = null, answer = message))
        Answers.notify(activity, "chti · ${conversation.question}", message)
        if (speak) app.speaker.say(message)
    }

    private fun update(next: Conversation) {
        conversation = next
        onUpdate(next)
    }
}
