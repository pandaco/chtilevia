package fr.chtilevia.android

import android.content.Context
import fr.chtilevia.adapters.bikeshare.GbfsFeed
import fr.chtilevia.adapters.favorites.FileFavoritesStore
import fr.chtilevia.adapters.fetch
import fr.chtilevia.adapters.gtfs.GtfsCache
import fr.chtilevia.adapters.realtime.GtfsRealtimeFeed
import fr.chtilevia.application.ChtiError
import fr.chtilevia.application.Clock
import fr.chtilevia.application.FindFavorite
import fr.chtilevia.application.FindNearby
import fr.chtilevia.application.GetBikeStations
import fr.chtilevia.application.GetDepartures
import fr.chtilevia.application.InterpretUtterance
import fr.chtilevia.application.ListFavorites
import fr.chtilevia.application.NetworkProfile
import fr.chtilevia.application.Notice
import fr.chtilevia.application.Notices
import fr.chtilevia.application.UpdateTimetable
import fr.chtilevia.domain.serviceDates
import fr.chtilevia.presentation.Chti
import fr.chtilevia.presentation.ChtiServices
import fr.chtilevia.presentation.Console
import java.io.File

/**
 * Racine de composition de l'appli : le cœur avec les adaptateurs JVM, le référentiel dans les
 * fichiers privés de l'appli (`files/chtilevia/gtfs`) et les favoris dans `files/chtilevia/favoris`.
 */
class AndroidChti(context: Context, private val profile: NetworkProfile = NetworkProfile.ILEVIA) {
    private val root = context.filesDir.resolve("chtilevia").path
    // Cache propre à l'appli : stop_times.txt ne sert plus une fois l'index construit
    val gtfs = GtfsCache(root, profile.gtfsUrl, keepStopTimes = false)
    private val clock = Clock { System.currentTimeMillis() / 1000 }
    private val favoritesFile = File("$root/favoris")
    private val favorites = FileFavoritesStore(favoritesFile.path)
    private val listFavorites = ListFavorites(favorites)
    private val interpretUtterance = InterpretUtterance(favorites)
    private val bikeShare = GbfsFeed { fetch("${profile.bikeShareUrl}/$it") }
    private val chti = Chti(
        ChtiServices(
            departures = GetDepartures(gtfs.timetable, gtfs, GtfsRealtimeFeed { fetch(profile.realtimeUrl) }, clock, profile),
            bikeStations = GetBikeStations(bikeShare, clock),
            nearby = FindNearby(gtfs.timetable, gtfs, bikeShare, clock),
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
     * [fr.chtilevia.application.ChtiError] si aucun référentiel n'est disponible.
     */
    @Synchronized
    fun warmUp(downloading: () -> Unit = {}) {
        gtfs.refresh(force = false, notices = Notices { if (it == Notice.TIMETABLE_DOWNLOAD) downloading() })
        gtfs.timetable.lines()
        gtfs.timetable.stops()
        gtfs.timetable.activeDates(serviceDates(clock.now(), profile.timeZone))
    }

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

    /** Fichier des favoris, au format du shell (`nom: commande`) ; vide s'il n'existe pas */
    @Synchronized
    fun favoritesText(): String = favoritesFile.takeIf { it.isFile }?.readText().orEmpty()

    @Synchronized
    fun saveFavorites(text: String) {
        favoritesFile.parentFile?.mkdirs()
        favoritesFile.writeText(text.trimEnd() + "\n")
    }

    /** Résultat d'une commande : code retour et texte (sorties standard et d'erreur, dans l'ordre) */
    data class Reply(val exit: Int, val text: String)

    /**
     * Lance une commande chti (mêmes arguments que le shell) ; à appeler hors du fil principal.
     * Une commande à la fois : le référentiel en mémoire n'est pas partagé entre fils.
     */
    @Synchronized
    fun run(args: List<String>): Reply {
        val output = StringBuilder()
        val console = object : Console {
            override fun out(text: String) {
                output.append(text)
            }

            override fun err(text: String) {
                output.append(text)
            }
        }
        val exit = chti.run(args, console)
        return Reply(exit, output.toString())
    }
}
