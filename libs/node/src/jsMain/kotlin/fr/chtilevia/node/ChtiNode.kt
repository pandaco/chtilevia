@file:OptIn(ExperimentalJsExport::class)

package fr.chtilevia.node

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
import kotlin.js.Date

/** Résultat d'une commande : code retour, sortie standard et sortie d'erreur */
@JsExport
class ChtiRun(val exit: Int, val out: String, val err: String)

/**
 * Lance la commande chti du cœur Kotlin, comme `apps/shell/chti` [args] dans l'environnement [env]
 * (objet de chaînes, `process.env` par défaut côté appelant) : mêmes variables que le shell
 * (CHTI_NOW, CHTI_GTFS_URL, CHTI_RT_URL, CHTI_GBFS_URL, XDG_CACHE_HOME, XDG_CONFIG_HOME, HOME),
 * même cache GTFS, même fichier de favoris.
 */
@JsExport
fun runChti(args: Array<String>, env: dynamic): ChtiRun {
    fun variable(name: String): String? = (env[name] as String?)?.takeIf { it.isNotEmpty() }
    val home = variable("HOME") ?: ""
    val default = NetworkProfile.ILEVIA
    val profile = default.copy(
        gtfsUrl = variable("CHTI_GTFS_URL") ?: default.gtfsUrl,
        realtimeUrl = variable("CHTI_RT_URL") ?: default.realtimeUrl,
        bikeShareUrl = variable("CHTI_GBFS_URL") ?: default.bikeShareUrl,
    )
    val fixedNow = variable("CHTI_NOW")?.toLong()
    val clock = Clock { fixedNow ?: (Date.now() / 1000).toLong() }
    val gtfs = GtfsCache("${variable("XDG_CACHE_HOME") ?: "$home/.cache"}/chtilevia", profile.gtfsUrl)
    val favorites = FileFavoritesStore("${variable("XDG_CONFIG_HOME") ?: "$home/.config"}/chtilevia/favoris")
    val bikeShare = GbfsFeed { fetch("${profile.bikeShareUrl}/$it") }
    val services = ChtiServices(
        departures = GetDepartures(gtfs.timetable, gtfs, GtfsRealtimeFeed { fetch(profile.realtimeUrl) }, clock, profile),
        bikeStations = GetBikeStations(bikeShare, clock),
        nearby = FindNearby(gtfs.timetable, gtfs, bikeShare, clock),
        listFavorites = ListFavorites(favorites),
        findFavorite = FindFavorite(favorites),
        interpretUtterance = InterpretUtterance(favorites),
        updateTimetable = UpdateTimetable(gtfs),
    )
    val out = StringBuilder()
    val err = StringBuilder()
    val console = object : Console {
        override fun out(text: String) {
            out.append(text)
        }

        override fun err(text: String) {
            err.append(text)
        }
    }
    val exit = Chti(services, profile.timeZone).run(args.toList(), console)
    return ChtiRun(exit, out.toString(), err.toString())
}
