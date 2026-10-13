package fr.chtilevia.android

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.chtilevia.application.ChtiError
import kotlinx.coroutines.launch

/** État de l'écran principal, gardé quand l'écran tourne ; les questions passent par [QuestionFlow] */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ChtiApplication
    private val main = Handler(Looper.getMainLooper())

    /** Message d'état (mise à jour des horaires, erreur de préparation), ou null */
    var status by mutableStateOf<String?>("Préparation des horaires…")
        private set
    var conversation by mutableStateOf(Conversation())
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
            Shortcuts.update(app, names)
            onMain { favorites = names }
        }
    }

    fun favoritesText(onLoaded: (String) -> Unit) = background {
        val text = app.chti.favoritesText()
        onMain { onLoaded(text) }
    }

    fun saveFavorites(text: String) = background {
        app.chti.saveFavorites(text)
        val names = app.chti.favoriteNames()
        Shortcuts.update(app, names)
        onMain {
            favorites = names
            // Les boutons du widget suivent les favoris
            viewModelScope.launch { ChtiWidget.refresh(app) }
        }
    }

    /** Mesure du GTFS complet (étape 8), lancée par `am start … --ez mesurer true` */
    fun measure() {
        conversation = Conversation(question = "Mesure du GTFS complet", status = "Mesure en cours…")
        background {
            Benchmark(app.chti, app.filesDir) { line -> onMain { conversation = conversation.copy(answer = conversation.answer + line + "\n") } }.run()
            onMain { conversation = conversation.copy(status = null) }
        }
    }

    fun stopSpeaking() = app.speaker.stop()

    private fun background(block: () -> Unit) {
        app.worker.execute(block)
    }

    private fun onMain(block: () -> Unit) {
        main.post(block)
    }
}
