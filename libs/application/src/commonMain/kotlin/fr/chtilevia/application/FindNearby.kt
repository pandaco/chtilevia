package fr.chtilevia.application

import fr.chtilevia.domain.BikeStation
import fr.chtilevia.domain.Line
import fr.chtilevia.domain.Position
import fr.chtilevia.domain.Stop
import fr.chtilevia.domain.distanceMeters

/** Zone d'arrêt proche : distance (m) et lignes qui la desservent, dans l'ordre du référentiel */
data class NearbyStop(val stop: Stop, val distance: Int, val lines: List<Line>)

data class NearbyStation(val station: BikeStation, val distance: Int)

/**
 * Arrêts et stations V'Lille les plus proches, à l'instant [now] (pour l'âge des données V'Lille) ;
 * [stations] est null si V'Lille est injoignable
 */
data class Nearby(
    val now: Long,
    val stops: List<NearbyStop>,
    val stations: List<NearbyStation>?,
    /** Avertissements dans l'ordre où ils sont survenus */
    val warnings: List<Notice>,
)

/** Arrêts desservis et stations V'Lille les plus proches d'une position, à vol d'oiseau */
class FindNearby(
    private val timetable: TimetableRepository,
    private val updater: TimetableUpdater,
    private val bikeShare: BikeShareFeed,
    private val clock: Clock,
) {
    fun execute(position: Position, count: Int, notices: Notices = Notices.NONE): Nearby {
        val warnings = mutableListOf<Notice>()
        val collect = Notices { notice ->
            if (notice != Notice.TIMETABLE_DOWNLOAD) warnings += notice
            notices.notice(notice)
        }
        updater.refresh(force = false, notices = collect)
        val stations = bikeShare.stations()
        if (stations == null) collect.notice(Notice.BIKE_SHARE_UNAVAILABLE)
        return Nearby(clock.now(), nearestStops(position, count), stations?.let { nearestStations(it, position, count) }, warnings)
    }

    private fun nearestStops(position: Position, count: Int): List<NearbyStop> {
        val stops = timetable.stops()
        // Zone d'arrêt -> elle-même et ses quais
        val members = LinkedHashMap<String, MutableSet<String>>()
        for ((id, stop) in stops) members.getOrPut(stop.parentStation ?: id) { LinkedHashSet() }.add(id)
        val zones = members.keys.mapNotNull { id ->
            val stop = stops[id] ?: return@mapNotNull null
            stop.position?.let { distanceMeters(position, it) to stop }
        }.sortedWith(compareBy({ it.first }, { it.second.name }, { it.second.id }))

        val lines = timetable.lines()
        val near = mutableListOf<NearbyStop>()
        // Lignes cherchées par lots : l'index du GTFS complet est long à parcourir
        for (batch in zones.chunked(count * 4)) {
            if (near.size >= count) break
            val served = timetable.linesServing(batch.flatMap { members.getValue(it.second.id) }.toSet())
            for ((distance, stop) in batch) {
                val ids = members.getValue(stop.id).flatMap { served[it].orEmpty() }.toSet()
                if (ids.isNotEmpty() && near.size < count) near += NearbyStop(stop, distance, lines.filter { it.id in ids })
            }
        }
        return near
    }

    private fun nearestStations(stations: List<BikeStation>, position: Position, count: Int): List<NearbyStation> =
        stations
            .mapNotNull { station -> station.position?.let { NearbyStation(station, distanceMeters(position, it)) } }
            .sortedWith(compareBy({ it.distance }, { it.station.name }, { it.station.id }))
            .take(count)
}
