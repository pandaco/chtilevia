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
import fr.chtilevia.application.FavoritesFile
import fr.chtilevia.application.NearbyMode
import fr.chtilevia.domain.Favorite
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

    /** Thème choisi (réglage gardé) : true sombre, false clair, null celui du téléphone */
    var darkTheme by mutableStateOf(app.darkTheme)
        private set

    /** Bascule clair ↔ sombre ; [current] : le thème affiché, celui du téléphone tant que rien n'est choisi */
    fun toggleTheme(current: Boolean) {
        darkTheme = !current
        app.darkTheme = darkTheme
    }

    /** Saisie au clavier ouverte */
    var typing by mutableStateOf(false)

    /** Propositions pour la question en cours de frappe */
    var suggestions by mutableStateOf(QuerySuggestions("", emptyList()))
        private set
    private var suggesting: Job? = null

    /**
     * Propositions pour [input], un court instant après la dernière lettre tapée ; [favorite] : pour
     * une question de favori (sans « à proximité »)
     */
    fun suggest(input: String, favorite: Boolean = false) {
        suggesting?.cancel()
        suggesting = viewModelScope.launch {
            delay(120)
            val found = withContext(Dispatchers.Default) {
                runCatching { if (favorite) app.chti.suggestFavorite(input) else app.chti.suggest(input) }.getOrNull()
            }
            if (found != null) suggestions = found
        }
    }

    /**
     * Demande de clavier sur le champ de saisie (« Écrire », question touchée), consommée une fois :
     * revenir sur l'écran (depuis l'aide, la carte) ne rouvre jamais le clavier
     */
    var focusField by mutableStateOf(false)

    /** Favoris, avec leurs commandes (écran des favoris) */
    var favoriteList by mutableStateOf(emptyList<Favorite>())
        private set

    /** Favori en cours de modification ou de création, ou null */
    var draft by mutableStateOf<FavoriteDraft?>(null)

    /** Question du brouillon en cours de saisie : son rang, [NEW_QUESTION] pour une nouvelle, ou null */
    var editedQuestion by mutableStateOf<Int?>(null)
        private set
    var questionError by mutableStateOf<String?>(null)
    var map by mutableStateOf(MapState(NearbyMode.ALL))
        private set

    enum class Page { MAIN, HELP, MAP, FAVORITES }

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

    /** Écran des favoris ; [create] : directement sur un nouveau favori */
    fun openFavorites(create: Boolean) {
        page = Page.FAVORITES
        draft = if (create) FavoriteDraft(null, "", emptyList()) else null
        editedQuestion = null
        background {
            val list = app.chti.favorites()
            onMain { favoriteList = list }
        }
    }

    fun editFavorite(index: Int) {
        val favorite = favoriteList[index]
        draft = FavoriteDraft(index, favorite.name, favorite.commands)
    }

    /** Saisie de la question [index] du brouillon ([NEW_QUESTION] : une de plus), propositions comprises */
    fun editQuestion(index: Int) {
        editedQuestion = index
        questionError = null
        focusField = true
        suggest(draft?.commands?.getOrNull(index)?.let { FavoritesFile.phrase(it) + " " }.orEmpty(), favorite = true)
    }

    fun closeQuestion() {
        editedQuestion = null
    }

    /** Question tapée [text] : comprise, elle prend sa place dans le brouillon ; sinon, pourquoi */
    fun confirmQuestion(text: String) {
        val index = editedQuestion ?: return
        background {
            val result = app.chti.favoriteCommand(text)
            onMain {
                val current = draft ?: return@onMain
                result.onSuccess { command ->
                    val commands = if (index == NEW_QUESTION) current.commands + command else current.commands.toMutableList().also { it[index] = command }
                    draft = current.copy(commands = commands, error = null)
                    editedQuestion = null
                }.onFailure { questionError = it.message }
            }
        }
    }

    fun removeQuestion(index: Int) {
        val current = draft ?: return
        draft = current.copy(commands = current.commands.filterIndexed { i, _ -> i != index })
    }

    /** Enregistre le brouillon s'il est complet (nom libre, au moins une question) ; sinon dit ce qui manque */
    fun saveDraft() {
        val current = draft ?: return
        val others = favoriteList.filterIndexed { i, _ -> i != current.index }.map { it.name }
        val error = FavoritesFile.invalidName(current.name, others)
            ?: if (current.commands.isEmpty()) "Ajoute au moins une question." else null
        if (error != null) {
            draft = current.copy(error = error)
            return
        }
        val favorite = Favorite(current.name.trim(), current.commands)
        persist(if (current.index == null) favoriteList + favorite else favoriteList.toMutableList().also { it[current.index] = favorite })
        draft = null
    }

    fun deleteDraft() {
        val index = draft?.index
        if (index != null) persist(favoriteList.filterIndexed { i, _ -> i != index })
        draft = null
    }

    /** Monte le favori [index] d'un rang : l'ordre est celui des boutons de l'écran et du widget */
    fun moveUp(index: Int) {
        if (index <= 0) return
        persist(favoriteList.toMutableList().also { it.add(index - 1, it.removeAt(index)) })
    }

    private fun persist(list: List<Favorite>) {
        favoriteList = list
        favorites = list.map { it.name }
        background {
            app.chti.saveFavorites(list)
            Shortcuts.update(app, list.map { it.name })
            // Les boutons du widget suivent les favoris
            onMain { viewModelScope.launch { ChtiWidget.refresh(app) } }
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

    companion object {
        const val NEW_QUESTION = -1
        private const val MAP_COUNT = 8
    }
}

/** Favori en cours de modification : [index] son rang (null : nouveau), et ce qui empêche de l'enregistrer */
data class FavoriteDraft(val index: Int?, val name: String, val commands: List<String>, val error: String? = null)
