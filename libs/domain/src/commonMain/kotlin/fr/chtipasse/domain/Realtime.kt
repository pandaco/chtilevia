package fr.chtipasse.domain

import kotlin.math.abs

/** Prévision temps réel d'une course à un arrêt : heure (epoch s) et/ou retard (s), selon le flux */
data class StopTimeUpdate(val time: Long?, val delay: Long?)

/** Mise à jour temps réel d'une course (GTFS-RT TripUpdate) */
data class TripUpdate(
    val cancelled: Boolean,
    /** Arrêts à venir, dans l'ordre du flux ; les arrêts sautés en sont absents */
    val stops: Map<String, StopTimeUpdate>,
)

/**
 * Heures temps réel d'une course, par arrêt. Le flux ne donne que les prochains arrêts : le
 * dernier retard connu est propagé aux arrêts suivants (règle GTFS-RT), et les arrêts qui
 * précèdent la première mise à jour sont considérés comme déjà passés.
 *
 * [stopTimes] : passages théoriques triés ; [origins] : origines des jours de service possibles
 * (voir [serviceDayOrigin]), celle qui colle le mieux au premier horaire temps réel est retenue.
 */
fun realtimeTimes(stopTimes: List<StopTime>, update: TripUpdate, origins: List<Long>): Map<String, Long> {
    val first = stopTimes.firstOrNull { it.stopId in update.stops } ?: return emptyMap()
    val firstUpdate = update.stops.getValue(first.stopId)
    val origin = origins.minBy { origin ->
        val time = firstUpdate.time?.takeIf { it != 0L } ?: (origin + first.time + (firstUpdate.delay ?: 0))
        abs(time - origin - first.time)
    }
    val times = LinkedHashMap<String, Long>()
    var delay: Long? = null
    for (stopTime in stopTimes) {
        val scheduled = origin + stopTime.time
        val known = update.stops[stopTime.stopId]
        val time = known?.time ?: known?.delay?.let { scheduled + it }
        if (time != null) {
            delay = time - scheduled
            times[stopTime.stopId] = time
        } else if (delay != null) {
            times[stopTime.stopId] = scheduled + delay
        }
    }
    return times
}
