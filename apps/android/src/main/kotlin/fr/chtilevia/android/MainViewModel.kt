package fr.chtilevia.android

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import fr.chtilevia.application.ChtiError

/** État de l'écran principal ; les commandes tournent sur le fil de l'appli, l'état change sur le fil principal */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ChtiApplication
    private val speaker = Speaker(application)
    private val main = Handler(Looper.getMainLooper())

    /** Message d'état (mise à jour des horaires, erreur de préparation), ou null */
    var status by mutableStateOf<String?>("Préparation des horaires…")
        private set
    var busy by mutableStateOf(false)
        private set
    var question by mutableStateOf("")
        private set
    var answer by mutableStateOf("")
        private set
    var favorites by mutableStateOf(emptyList<String>())
        private set

    init {
        background {
            try {
                app.chti.warmUp { onMain { status = "Téléchargement des horaires…" } }
                onMain { status = null }
            } catch (_: ChtiError) {
                onMain { status = "Horaires indisponibles : vérifie la connexion, ils seront téléchargés à la première question." }
            }
            val names = app.chti.favoriteNames()
            onMain { favorites = names }
        }
    }

    /** Phrase libre (dite ou tapée), comprise comme « chti vocal » ; la réponse est lue */
    fun ask(phrase: String) {
        if (phrase.isBlank()) return
        command(phrase.trim(), listOf("vocal", phrase.trim()))
    }

    /** Lance un favori, réponse à l'oral */
    fun runFavorite(name: String) = command(name, listOf("--voix", name))

    fun favoritesText(onLoaded: (String) -> Unit) = background {
        val text = app.chti.favoritesText()
        onMain { onLoaded(text) }
    }

    fun saveFavorites(text: String) = background {
        app.chti.saveFavorites(text)
        val names = app.chti.favoriteNames()
        onMain { favorites = names }
    }

    /** Mesure du GTFS complet (étape 8), lancée par `am start … --ez mesurer true` */
    fun measure() {
        busy = true
        question = "Mesure du GTFS complet"
        answer = ""
        background {
            Benchmark(app.chti, app.filesDir) { line -> onMain { answer += line + "\n" } }.run()
            onMain { busy = false }
        }
    }

    fun stopSpeaking() = speaker.stop()

    private fun command(label: String, args: List<String>) {
        busy = true
        question = label
        answer = ""
        speaker.stop()
        background {
            val reply = app.chti.run(args)
            onMain {
                answer = reply.text.trim()
                busy = false
                speaker.say(answer)
                status = null
            }
        }
    }

    private fun background(block: () -> Unit) {
        app.worker.execute(block)
    }

    private fun onMain(block: () -> Unit) {
        main.post(block)
    }

    override fun onCleared() {
        speaker.shutdown()
    }
}
