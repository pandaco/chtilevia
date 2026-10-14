package fr.chtipasse.android

import android.content.Context
import fr.chtipasse.adapters.appDirectory
import fr.chtipasse.adapters.bikeshare.GbfsFeed
import fr.chtipasse.adapters.favorites.FileFavoritesStore
import fr.chtipasse.adapters.fetch
import fr.chtipasse.adapters.gtfs.GtfsCache
import fr.chtipasse.adapters.realtime.GtfsRealtimeFeed
import fr.chtipasse.application.ChtiError
import fr.chtipasse.application.Clock
import fr.chtipasse.application.CommandLine
import fr.chtipasse.application.FavoritesFile
import fr.chtipasse.application.FindFavorite
import fr.chtipasse.application.FindNearby
import fr.chtipasse.application.GetBikeStations
import fr.chtipasse.application.GetDepartures
import fr.chtipasse.application.InterpretUtterance
import fr.chtipasse.application.ListFavorites
import fr.chtipasse.application.Nearby
import fr.chtipasse.application.NearbyMode
import fr.chtipasse.application.NetworkProfile
import fr.chtipasse.application.QuerySuggestions
import fr.chtipasse.application.SuggestQuery
import fr.chtipasse.application.Notice
import fr.chtipasse.application.Notices
import fr.chtipasse.application.UpdateTimetable
import fr.chtipasse.domain.Favorite
import fr.chtipasse.domain.Position
import fr.chtipasse.domain.serviceDates
import fr.chtipasse.presentation.Chti
import fr.chtipasse.presentation.ChtiServices
import fr.chtipasse.presentation.Console
import java.io.File

/**
 * Racine de composition de l'appli : le cœur avec les adaptateurs JVM, le référentiel dans les
 * fichiers privés de l'appli (`files/chtipasse/gtfs`) et les favoris dans `files/chtipasse/favoris`.
 */
class AndroidChti(context: Context, private val profile: NetworkProfile = NetworkProfile.ILEVIA) {
    private val root = appDirectory(context.filesDir.path)
    // Cache propre à l'appli : stop_times.txt ne sert plus une fois l'index construit
    val gtfs = GtfsCache(root, profile.gtfsUrl, keepStopTimes = false)
    private val clock = Clock { System.currentTimeMillis() / 1000 }
    private val favoritesFile = File("$root/favoris")
    private val favorites = FileFavoritesStore(favoritesFile.path)
    private val listFavorites = ListFavorites(favorites)
    private val interpretUtterance = InterpretUtterance(favorites)
    private val bikeShare = GbfsFeed { fetch("${profile.bikeShareUrl}/$it") }
    private val findNearby = FindNearby(gtfs.timetable, gtfs, bikeShare, clock)
    private val suggestQuery = SuggestQuery(gtfs.timetable, bikeShare, favorites, clock)
    private val chti = Chti(
        ChtiServices(
            departures = GetDepartures(gtfs.timetable, gtfs, GtfsRealtimeFeed { fetch(profile.realtimeUrl) }, clock, profile),
            bikeStations = GetBikeStations(bikeShare, clock),
            nearby = findNearby,
            listFavorites = listFavorites,
            findFavorite = FindFavorite(favorites),
            interpretUtterance = interpretUtterance,
            updateTimetable = UpdateTimetable(gtfs),
        ),
        profile.timeZone,
    )

    /**
     * Prépare le référentiel : téléchargé s'il manque ou date de plus de 24 h ([downloading] est
     * alors appelé), puis lu en mémoire pour que la première question soit rapide. Lève
     * [fr.chtipasse.application.ChtiError] si aucun référentiel n'est disponible.
     */
    @Synchronized
    fun warmUp(downloading: () -> Unit = {}) {
        gtfs.refresh(force = false, notices = Notices { if (it == Notice.TIMETABLE_DOWNLOAD) downloading() })
        gtfs.timetable.lines()
        gtfs.timetable.stops()
        gtfs.timetable.activeDates(serviceDates(clock.now(), profile.timeZone))
    }

    /** Télécharge le référentiel s'il date de plus de 24 h ; lève [ChtiError] s'il n'y en a aucun */
    @Synchronized
    fun refreshTimetable() {
        gtfs.refresh(force = false, notices = Notices.NONE)
    }

    /** Étape et propositions pour compléter une question en cours de frappe */
    @Synchronized
    fun suggest(input: String): QuerySuggestions = suggestQuery.execute(input)

    /** Position d'une zone d'arrêt (identifiant GTFS), pour la carte */
    @Synchronized
    fun stopPosition(stopId: String): Position? = gtfs.timetable.stops()[stopId]?.position

    /** Position d'une station V'Lille (identifiant GBFS), pour la carte ; null si V'Lille est injoignable */
    @Synchronized
    fun stationPosition(stationId: String): Position? = bikeShare.stations()?.firstOrNull { it.id == stationId }?.position

    /** Arrêts et stations proches, en objets (pour la carte) */
    @Synchronized
    fun nearby(position: Position, count: Int, mode: NearbyMode): Nearby = findNearby.execute(position, count, mode)

    /** Commande comprise dans une phrase libre (comme `chti vocal`), ou null si elle n'est pas comprise */
    @Synchronized
    fun interpret(phrase: String): List<String>? = try {
        interpretUtterance.execute(phrase)
    } catch (_: ChtiError) {
        null
    }

    /** Noms des favoris, dans l'ordre du fichier */
    @Synchronized
    fun favoriteNames(): List<String> = listFavorites.execute().favorites.map { it.name }

    /** Favoris, dans l'ordre du fichier, avec leurs commandes */
    @Synchronized
    fun favorites(): List<Favorite> = listFavorites.execute().favorites

    /** Réécrit le fichier des favoris (même format que celui du shell : `nom: commande`) pour contenir [list] */
    @Synchronized
    fun saveFavorites(list: List<Favorite>) {
        favoritesFile.parentFile?.mkdirs()
        // Commentaires et lignes inchangées gardés : le fichier est aussi écrit à la main
        favoritesFile.writeText(FavoritesFile.update(favorites.read().orEmpty(), list))
    }

    /** Étape et propositions pour une question de favori : celles de [suggest], sans « à proximité » */
    @Synchronized
    fun suggestFavorite(input: String): QuerySuggestions = suggestQuery.execute(input, nearby = false)

    /**
     * Commande à enregistrer dans un favori pour [text] : une phrase comprise (celles des
     * propositions), ou une commande chti avec ses options (`velo rihour --prendre`) ; échoue avec
     * un message à montrer sinon
     */
    @Synchronized
    fun favoriteCommand(text: String): Result<String> {
        val typed = FavoritesFile.words(text)
        val words = when {
            typed.isEmpty() -> return failure("Écris une question.")
            typed.any { it.startsWith("-") } -> typed
            else -> try {
                interpretUtterance.execute(text)
            } catch (_: ChtiError) {
                return failure("Je ne comprends pas cette question : choisis parmi les propositions.")
            }
        }
        if (words.first() == "autour") return failure("« À proximité » dépend de l'endroit où tu es : pose la question au moment voulu.")
        val command = FavoritesFile.command(words)
        return try {
            CommandLine.parse(FavoritesFile.words(command))
            Result.success(command)
        } catch (_: ChtiError) {
            failure("Question incomplète : choisis parmi les propositions.")
        }
    }

    private fun failure(message: String) = Result.failure<String>(IllegalArgumentException(message))

    /**
     * Résultat d'une commande : code retour, texte (sorties standard et d'erreur, dans l'ordre) et
     * sortie standard seule (le document de `--json`, sans les messages)
     */
    data class Reply(val exit: Int, val text: String, val out: String = text)

    /**
     * Lance une commande chti (mêmes arguments que le shell) ; à appeler hors du fil principal.
     * Une commande à la fois : le référentiel en mémoire n'est pas partagé entre fils.
     */
    @Synchronized
    fun run(args: List<String>): Reply {
        val output = StringBuilder()
        val standard = StringBuilder()
        val console = object : Console {
            override fun out(text: String) {
                output.append(text)
                standard.append(text)
            }

            override fun err(text: String) {
                output.append(text)
            }
        }
        val exit = chti.run(args, console)
        return Reply(exit, output.toString(), standard.toString())
    }
}
