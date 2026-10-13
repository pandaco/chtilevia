package fr.chtilevia.android

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.chtilevia.application.ChtiError
import fr.chtilevia.application.NearbyMode
import fr.chtilevia.domain.Line
import fr.chtilevia.domain.Position
import fr.chtilevia.domain.TransportMode
import fr.chtilevia.domain.distanceMeters
import fr.chtilevia.presentation.briefState
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

    /** Écran affiché : principal, aide ou carte */
    var page by mutableStateOf(Page.MAIN)

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
    var map by mutableStateOf(MapState(NearbyMode.ALL))
        private set

    enum class Page { MAIN, HELP, MAP }

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

    /** Carte des arrêts et stations proches pour [mode] ; l'autorisation de localisation est déjà accordée */
    fun openMap(mode: NearbyMode) {
        page = Page.MAP
        map = MapState(mode, me = map.me)
        background {
            val location = DeviceLocation.current(app)
            if (location == null) {
                onMain { map = map.copy(message = "Je ne trouve pas ta position : active la localisation et réessaie.") }
                return@background
            }
            val me = Position(location.latitude, location.longitude)
            onMain { map = map.copy(me = me, message = "Je cherche…") }
            val nearby = app.chti.nearby(me, MAP_COUNT, mode)
            // Du plus proche au plus lointain, arrêts et stations mêlés
            val points = (
                nearby.stops.mapNotNull { near ->
                    near.stop.position?.let {
                        MapPoint("arret:${near.stop.id}", it, near.stop.name, kindOf(near.lines), near.lines.joinToString(", ") { l -> l.label }, near.distance)
                    }
                } + nearby.stations.orEmpty().mapNotNull { near ->
                    near.station.position?.let {
                        MapPoint("velo:${near.station.id}", it, near.station.name, MapPoint.Kind.BIKE, near.station.briefState(), near.distance)
                    }
                }
                ).sortedBy { it.distance }
            val message = when {
                nearby.stations == null && points.isEmpty() -> "V'Lille injoignable."
                points.isEmpty() -> "Rien à proximité."
                else -> null
            }
            onMain { if (map.mode == mode) map = map.copy(points = points, message = message) }
        }
    }

    /**
     * Carte centrée sur l'emplacement d'une carte de réponse ([detail] : ce que la réponse en dit),
     * avec ta position si elle est autorisée
     */
    fun openPlace(place: Place, detail: String) {
        page = Page.MAP
        map = MapState(NearbyMode.ALL, me = map.me, message = "Je cherche l'emplacement…")
        background {
            val position = if (place.isStation) app.chti.stationPosition(place.id) else app.chti.stopPosition(place.id)
            if (position == null) {
                onMain { map = map.copy(message = "Emplacement introuvable.") }
                return@background
            }
            val allowed = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .any { ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED }
            val me = if (allowed) DeviceLocation.current(app, timeoutSeconds = 5)?.let { Position(it.latitude, it.longitude) } else null
            val id = (if (place.isStation) "velo:" else "arret:") + place.id
            val point = MapPoint(id, position, place.name, place.kind, detail, me?.let { distanceMeters(it, position) })
            onMain { map = MapState(NearbyMode.ALL, me = me, points = listOf(point), selected = id, message = null) }
        }
    }

    /** Emplacement touché (liste ou carte) : mis en avant ; null : plus rien de sélectionné */
    fun selectPlace(id: String?) {
        map = map.copy(selected = id)
    }

    /** Couleur d'un arrêt : son mode le plus « lourd » (métro, puis tram, puis bus) */
    private fun kindOf(lines: List<Line>): MapPoint.Kind = when {
        lines.any { it.mode == TransportMode.METRO } -> MapPoint.Kind.METRO
        lines.any { it.mode == TransportMode.TRAM } -> MapPoint.Kind.TRAM
        else -> MapPoint.Kind.BUS
    }

    fun stopSpeaking() = app.speaker.stop()

    private fun background(block: () -> Unit) {
        app.worker.execute(block)
    }

    private fun onMain(block: () -> Unit) {
        main.post(block)
    }

    private companion object {
        const val MAP_COUNT = 8
    }
}
