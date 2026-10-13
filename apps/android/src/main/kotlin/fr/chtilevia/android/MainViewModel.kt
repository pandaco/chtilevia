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
import fr.chtilevia.application.QuerySuggestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** Réponses lues à voix haute (réglage gardé) */
    var speakAnswers by mutableStateOf(app.speakAnswers)
        private set

    fun toggleSpeech() {
        speakAnswers = !speakAnswers
        app.speakAnswers = speakAnswers
        if (!speakAnswers) app.speaker.stop()
    }

    /** Saisie au clavier ouverte */
    var typing by mutableStateOf(false)

    /** Propositions pour la question en cours de frappe */
    var suggestions by mutableStateOf(QuerySuggestions("", emptyList()))
        private set
    private var suggesting: Job? = null

    /** Propositions pour [input], un court instant après la dernière lettre tapée */
    fun suggest(input: String) {
        suggesting?.cancel()
        suggesting = viewModelScope.launch {
            delay(120)
            val found = withContext(Dispatchers.Default) { runCatching { app.chti.suggest(input) }.getOrNull() }
            if (found != null) suggestions = found
        }
    }

    /**
     * Demande de clavier sur le champ de saisie (« Écrire », question touchée), consommée une fois :
     * revenir sur l'écran (depuis l'aide, la carte) ne rouvre jamais le clavier
     */
    var focusField by mutableStateOf(false)

    /** Texte du fichier des favoris en cours de modification, ou null */
    var editing by mutableStateOf<String?>(null)

    init {
        background {
            // Les favoris d'abord : instantanés, alors que la préparation des horaires prend quelques secondes
            val names = app.chti.favoriteNames()
            onMain { favorites = names }
            Shortcuts.update(app, names)
            try {
                app.chti.warmUp { onMain { status = "Téléchargement des horaires…" } }
                onMain { status = null }
            } catch (_: ChtiError) {
                onMain { status = "Horaires indisponibles : vérifie la connexion, ils seront téléchargés à la première question." }
            }
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
