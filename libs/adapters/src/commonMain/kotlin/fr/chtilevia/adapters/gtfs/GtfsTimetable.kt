package fr.chtilevia.adapters.gtfs

import fr.chtilevia.application.TimetableRepository
import fr.chtilevia.domain.Line
import fr.chtilevia.domain.Position
import fr.chtilevia.domain.Stop
import fr.chtilevia.domain.StopTime
import fr.chtilevia.domain.TransportMode
import fr.chtilevia.domain.Trip
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format

/** Fichiers d'un GTFS décompressé, lus ligne à ligne */
interface GtfsFiles {
    /** Applique [block] aux lignes du fichier [name] (ex. `trips.txt`), ou renvoie null s'il est absent */
    fun <T> read(name: String, block: (Sequence<String>) -> T): T?
}

/**
 * Référentiel théorique lu dans un GTFS décompressé, complété d'un index par ligne
 * (`idx/<route_id>.tsv`, voir [buildIndex]) pour ne pas relire tout `stop_times.txt` à chaque
 * demande. Même disposition que le cache du shell : les deux peuvent le partager.
 */
class GtfsTimetable(private val files: GtfsFiles) : TimetableRepository {
    private fun rows(name: String): List<Map<String, String>> =
        files.read(name) { csvRows(it).toList() } ?: error("fichier GTFS absent : $name")

    override fun lines(): List<Line> = rows("routes.txt").map {
        Line(
            id = it.getValue("route_id"),
            name = it["route_short_name"].orEmpty(),
            mode = TransportMode.fromRouteType(it["route_type"].orEmpty()),
            description = it["route_desc"].orEmpty(),
        )
    }

    override fun trips(lineId: String): List<Trip> = rows("trips.txt").filter { it["route_id"] == lineId }.map {
        Trip(
            id = it.getValue("trip_id"),
            lineId = lineId,
            serviceId = it["service_id"].orEmpty(),
            headsign = it["trip_headsign"].orEmpty(),
            directionId = it["direction_id"].orEmpty(),
        )
    }

    override fun stops(): Map<String, Stop> = rows("stops.txt").associate {
        val id = it.getValue("stop_id")
        val latitude = it["stop_lat"]?.toDoubleOrNull()
        val longitude = it["stop_lon"]?.toDoubleOrNull()
        val position = if (latitude != null && longitude != null) Position(latitude, longitude) else null
        id to Stop(id, it["stop_name"].orEmpty(), it["stop_desc"].orEmpty(), it["parent_station"]?.ifEmpty { null }, position)
    }

    override fun stopTimes(lineId: String): Map<String, List<StopTime>> {
        val byTrip = LinkedHashMap<String, MutableList<StopTime>>()
        files.read(indexFile(lineId)) { lines ->
            for (line in lines) {
                val (trip, stop, sequence, time) = line.split('\t').takeIf { it.size == 4 } ?: continue
                byTrip.getOrPut(trip) { mutableListOf() }.add(StopTime(sequence.toInt(), stop, seconds(time)))
            }
        }
        return byTrip
    }

    override fun linesServing(stopIds: Set<String>): Map<String, Set<String>> {
        val found = LinkedHashMap<String, MutableSet<String>>()
        for (line in lines()) {
            files.read(indexFile(line.id)) { rows ->
                for (row in rows) {
                    val stop = row.substringAfter('\t').substringBefore('\t')
                    if (stop in stopIds) found.getOrPut(stop) { LinkedHashSet() }.add(line.id)
                }
            }
        }
        return found
    }

    override fun activeDates(dates: Collection<LocalDate>): Map<String, Set<LocalDate>> {
        val wanted = dates.associateBy { it.format(BASIC_DATE) }
        val active = LinkedHashMap<String, MutableSet<LocalDate>>()
        files.read("calendar_dates.txt") { lines ->
            for (line in lines) {
                // service_id,date,exception_type ; 1 = service ajouté ce jour-là
                val parts = line.removePrefix("﻿").trimEnd('\r').split(',')
                val date = wanted[parts.getOrNull(1)]
                if (parts.size == 3 && date != null && parts[2] == "1") active.getOrPut(parts[0]) { mutableSetOf() }.add(date)
            }
        }
        return active
    }

    private fun seconds(hms: String): Int {
        val (h, m, s) = hms.split(':').map { it.trim().toInt() }
        return h * 3600 + m * 60 + s
    }

    companion object {
        private val BASIC_DATE = LocalDate.Format {
            year()
            monthNumber()
            day()
        }

        fun indexFile(lineId: String) = "idx/$lineId.tsv"

        /**
         * Index par ligne : pour chaque passage de `stop_times.txt`, une ligne
         * `trip_id⇥stop_id⇥stop_sequence⇥heure` écrite par [write] dans le fichier de la ligne de la course.
         */
        fun buildIndex(trips: Sequence<String>, stopTimes: Sequence<String>, write: (lineId: String, line: String) -> Unit) {
            val lineOf = csvRows(trips).associate { it.getValue("trip_id") to it.getValue("route_id") }
            for (row in csvRows(stopTimes)) {
                val lineId = lineOf[row["trip_id"]] ?: continue
                val time = row["departure_time"].orEmpty().ifEmpty { row["arrival_time"].orEmpty() }
                write(lineId, "${row["trip_id"]}\t${row["stop_id"]}\t${row["stop_sequence"]}\t$time\n")
            }
        }
    }
}
