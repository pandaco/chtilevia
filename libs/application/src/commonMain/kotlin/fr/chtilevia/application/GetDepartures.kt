package fr.chtilevia.application

import fr.chtilevia.domain.DirectionStops
import fr.chtilevia.domain.HeadsignDepartures
import fr.chtilevia.domain.Line
import fr.chtilevia.domain.LineSchedule
import fr.chtilevia.domain.StopDepartures
import fr.chtilevia.domain.serviceDates

/** Demande de passages : [stop] vide = liste des arrêts ; [direction] vide = toutes directions */
data class DeparturesQuery(val line: String, val stop: String, val direction: String, val count: Int)

sealed interface DeparturesResult {
    val line: Line
}

/** Prochains passages ([count] au plus par destination) aux arrêts trouvés */
data class Departures(
    override val line: Line,
    val now: Long,
    val stops: List<StopDepartures>,
    /** Avertissements dans l'ordre où ils sont survenus */
    val warnings: List<Notice>,
) : DeparturesResult

/** Pas d'arrêt demandé : arrêts de chaque sens, et un arrêt de la ligne pour donner un exemple */
data class StopList(
    override val line: Line,
    val directions: List<DirectionStops>,
    val exampleStop: String?,
) : DeparturesResult

/** Prochains passages d'une ligne à un arrêt, ou liste de ses arrêts */
class GetDepartures(
    private val timetable: TimetableRepository,
    private val updater: TimetableUpdater,
    private val realtime: RealtimeFeed,
    private val clock: Clock,
    private val profile: NetworkProfile,
) {
    fun execute(query: DeparturesQuery, notices: Notices = Notices.NONE): DeparturesResult {
        val warnings = mutableListOf<Notice>()
        val collect = Notices { notice ->
            if (notice != Notice.TIMETABLE_DOWNLOAD) warnings += notice
            notices.notice(notice)
        }
        updater.refresh(force = false, notices = collect)
        val snapshot = if (query.line in profile.linesWithoutRealtime) {
            null
        } else {
            realtime.fetch() ?: null.also { collect.notice(Notice.REALTIME_UNAVAILABLE) }
        }

        val line = timetable.lines().firstOrNull { it.name.uppercase() == query.line.uppercase() }
            ?: throw ChtiError.UnknownLine(query.line)
        val schedule = LineSchedule(line, timetable.trips(line.id), timetable.stops(), timetable.stopTimes(line.id))

        var trips = schedule.trips
        if (query.direction.isNotEmpty()) {
            trips = schedule.tripsTowards(query.direction)
            if (trips.isEmpty()) throw ChtiError.UnknownDirection(line.name, query.direction, schedule.headsigns())
        }
        if (query.stop.isEmpty()) return StopList(line, schedule.stopsByDirection(trips), schedule.exampleStopName())

        val matched = schedule.matchStops(query.stop, trips)
        if (matched.isEmpty() && query.direction.isNotEmpty()) {
            // L'arrêt existe peut-être, mais pas dans cette direction : le dire plutôt que « introuvable »
            val elsewhere = schedule.matchStops(query.stop, schedule.trips)
            if (elsewhere.isNotEmpty()) {
                throw ChtiError.StopNotInDirection(
                    line = line.name,
                    stops = elsewhere.keys.map(schedule::areaName).distinct().sorted(),
                    direction = query.direction,
                    headsigns = schedule.headsignsServing(elsewhere.keys),
                )
            }
        }
        if (matched.isEmpty()) throw ChtiError.UnknownStop(line.name, query.stop)

        val now = clock.now()
        val tripIds = trips.map { it.id }.toSet()
        val stops = schedule.departures(
            of = trips,
            matched = matched,
            now = now,
            zone = profile.timeZone,
            activeDates = timetable.activeDates(serviceDates(now, profile.timeZone)),
            updates = snapshot?.tripUpdates(tripIds).orEmpty(),
        ).map { stop ->
            stop.copy(directions = stop.directions.map { HeadsignDepartures(it.headsign, it.departures.take(query.count)) })
        }
        return Departures(line, now, stops, warnings)
    }
}
