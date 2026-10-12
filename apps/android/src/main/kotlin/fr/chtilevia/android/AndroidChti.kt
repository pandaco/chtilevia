package fr.chtilevia.android

import android.content.Context
import fr.chtilevia.adapters.bikeshare.GbfsFeed
import fr.chtilevia.adapters.favorites.FileFavoritesStore
import fr.chtilevia.adapters.fetch
import fr.chtilevia.adapters.gtfs.GtfsCache
import fr.chtilevia.adapters.realtime.GtfsRealtimeFeed
import fr.chtilevia.application.Clock
import fr.chtilevia.application.FindFavorite
import fr.chtilevia.application.FindNearby
import fr.chtilevia.application.GetBikeStations
import fr.chtilevia.application.GetDepartures
import fr.chtilevia.application.InterpretUtterance
import fr.chtilevia.application.ListFavorites
import fr.chtilevia.application.NetworkProfile
import fr.chtilevia.application.UpdateTimetable
import fr.chtilevia.presentation.Chti
import fr.chtilevia.presentation.ChtiServices
import fr.chtilevia.presentation.Console

/**
 * Racine de composition de l'appli : le cœur avec les adaptateurs JVM, le référentiel dans les
 * fichiers privés de l'appli (`files/chtilevia/gtfs`) et les favoris dans `files/chtilevia/favoris`.
 */
class AndroidChti(context: Context, private val profile: NetworkProfile = NetworkProfile.ILEVIA) {
    private val root = context.filesDir.resolve("chtilevia").path
    val gtfs = GtfsCache(root, profile.gtfsUrl)
    private val clock = Clock { System.currentTimeMillis() / 1000 }
    private val favorites = FileFavoritesStore("$root/favoris")
    private val bikeShare = GbfsFeed { fetch("${profile.bikeShareUrl}/$it") }
    private val chti = Chti(
        ChtiServices(
            departures = GetDepartures(gtfs.timetable, gtfs, GtfsRealtimeFeed { fetch(profile.realtimeUrl) }, clock, profile),
            bikeStations = GetBikeStations(bikeShare, clock),
            nearby = FindNearby(gtfs.timetable, gtfs, bikeShare, clock),
            listFavorites = ListFavorites(favorites),
            findFavorite = FindFavorite(favorites),
            interpretUtterance = InterpretUtterance(favorites),
            updateTimetable = UpdateTimetable(gtfs),
        ),
        profile.timeZone,
    )

    /** Résultat d'une commande : code retour et texte (sorties standard et d'erreur, dans l'ordre) */
    data class Reply(val exit: Int, val text: String)

    /** Lance une commande chti (mêmes arguments que le shell) ; à appeler hors du fil principal */
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
