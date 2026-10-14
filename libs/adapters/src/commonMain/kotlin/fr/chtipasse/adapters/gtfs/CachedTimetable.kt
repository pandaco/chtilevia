package fr.chtipasse.adapters.gtfs

import fr.chtipasse.application.TimetableRepository
import fr.chtipasse.domain.Line
import fr.chtipasse.domain.Stop
import fr.chtipasse.domain.StopTime
import fr.chtipasse.domain.Trip
import kotlinx.datetime.LocalDate

/**
 * Référentiel gardé en mémoire tant que [version] ne change pas (date d'installation du GTFS) :
 * une appli qui reste lancée ne relit pas ses fichiers à chaque demande. Les passages ne sont gardés
 * que pour les [keptLines] dernières lignes demandées. Pas d'appels simultanés depuis plusieurs fils.
 */
class CachedTimetable(
    private val source: TimetableRepository,
    private val keptLines: Int = 4,
    private val version: () -> Any?,
) : TimetableRepository {
    private var current: Any? = null
    private var lines: List<Line>? = null
    private var stops: Map<String, Stop>? = null
    private val trips = HashMap<String, List<Trip>>()
    private val stopTimes = LinkedHashMap<String, Map<String, List<StopTime>>>()
    private var activeDates: Pair<Set<LocalDate>, Map<String, Set<LocalDate>>>? = null
    private var linesByStop: Map<String, Set<String>>? = null

    /** Oublie tout si le GTFS a changé depuis la dernière demande */
    private fun checkVersion() {
        val now = version()
        if (now == current) return
        current = now
        lines = null
        stops = null
        trips.clear()
        stopTimes.clear()
        activeDates = null
        linesByStop = null
    }

    override fun lines(): List<Line> {
        checkVersion()
        return lines ?: source.lines().also { lines = it }
    }

    override fun trips(lineId: String): List<Trip> {
        checkVersion()
        return trips.getOrPut(lineId) { source.trips(lineId) }
    }

    override fun stops(): Map<String, Stop> {
        checkVersion()
        return stops ?: source.stops().also { stops = it }
    }

    override fun stopTimes(lineId: String): Map<String, List<StopTime>> {
        checkVersion()
        // Dernière utilisée en fin de liste ; la plus ancienne sort quand la place manque
        val kept = stopTimes.remove(lineId) ?: source.stopTimes(lineId)
        stopTimes[lineId] = kept
        while (stopTimes.size > keptLines) stopTimes.remove(stopTimes.keys.first())
        return kept
    }

    /** Calculé une fois pour tous les quais : il faut de toute façon parcourir l'index de chaque ligne */
    override fun linesServing(stopIds: Set<String>): Map<String, Set<String>> {
        checkVersion()
        val all = linesByStop ?: source.linesServing(stops().keys).also { linesByStop = it }
        return stopIds.mapNotNull { id -> all[id]?.let { id to it } }.toMap()
    }

    override fun activeDates(dates: Collection<LocalDate>): Map<String, Set<LocalDate>> {
        checkVersion()
        val wanted = dates.toSet()
        activeDates?.let { (cachedDates, active) -> if (cachedDates == wanted) return active }
        return source.activeDates(dates).also { activeDates = wanted to it }
    }
}
