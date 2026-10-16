@file:OptIn(ExperimentalJsExport::class)

package fr.chtipasse.node

import fr.chtipasse.adapters.appDirectory
import fr.chtipasse.adapters.bikeshare.GbfsFeed
import fr.chtipasse.adapters.favorites.FileFavoritesStore
import fr.chtipasse.adapters.fetch
import fr.chtipasse.adapters.gtfs.GtfsCache
import fr.chtipasse.adapters.realtime.GtfsRealtimeFeed
import fr.chtipasse.application.Clock
import fr.chtipasse.application.FindFavorite
import fr.chtipasse.application.FindNearby
import fr.chtipasse.application.GetBikeStations
import fr.chtipasse.application.GetDepartures
import fr.chtipasse.application.InterpretUtterance
import fr.chtipasse.application.ListFavorites
import fr.chtipasse.application.NetworkProfile
import fr.chtipasse.application.UpdateTimetable
import fr.chtipasse.presentation.Chti
import fr.chtipasse.presentation.ChtiServices
import fr.chtipasse.presentation.Console
import kotlin.js.Date

/**
 * Caches GTFS par dossier et source : le référentiel reste en mémoire d'un appel à l'autre, comme dans
 * l'appli Android (voir CachedTimetable), au lieu d'être relu à chaque recherche du serveur MCP
 */
private val gtfsCaches = HashMap<Pair<String, String>, GtfsCache>()

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
    val cacheDir = appDirectory(variable("XDG_CACHE_HOME") ?: "$home/.cache")
    val gtfs = gtfsCaches.getOrPut(cacheDir to profile.gtfsUrl) { GtfsCache(cacheDir, profile.gtfsUrl) }
    val favorites = FileFavoritesStore("${appDirectory(variable("XDG_CONFIG_HOME") ?: "$home/.config")}/favoris")
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
