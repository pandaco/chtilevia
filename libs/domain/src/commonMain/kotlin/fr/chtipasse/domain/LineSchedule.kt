package fr.chtipasse.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** Passage à un arrêt : heure (epoch s), destination de la course, et s'il vient du temps réel */
data class Departure(val time: Long, val headsign: String, val realtime: Boolean) : Comparable<Departure> {
    override fun compareTo(other: Departure): Int =
        compareValuesBy(this, other, { it.time }, { it.headsign }, { it.realtime })
}

/** Passages vers une destination, dans l'ordre chronologique */
data class HeadsignDepartures(val headsign: String, val departures: List<Departure>)

/** Passages à une zone d'arrêt, par destination (la plus proche d'abord) */
data class StopDepartures(
    val stopId: String,
    val name: String,
    val description: String,
    val directions: List<HeadsignDepartures>,
)

/** Arrêts d'un sens de la ligne, dans l'ordre de parcours */
data class DirectionStops(val headsign: String, val stops: List<String>)

/**
 * Horaires théoriques d'une ligne : ses courses, les arrêts du réseau et les passages de chaque
 * course (dans l'ordre du fichier GTFS). Les recherches ignorent casse et accents ([fold]).
 */
class LineSchedule(
    val line: Line,
    /** Courses de la ligne, dans l'ordre de `trips.txt` */
    val trips: List<Trip>,
    private val stops: Map<String, Stop>,
    /** Passages par course, dans l'ordre de `stop_times.txt` */
    private val stopTimes: Map<String, List<StopTime>>,
) {
    /** Zone d'arrêt d'un quai : sa station parente, ou lui-même */
    fun area(stopId: String): String = stops[stopId]?.parentStation?.takeIf { it.isNotEmpty() } ?: stopId

    fun areaName(areaId: String): String = stops[areaId]?.name ?: areaId

    fun stopDescription(areaId: String): String = stops[areaId]?.description.orEmpty().trim(' ', '-')

    /** Destinations non vides, triées, des courses [of] */
    fun headsigns(of: List<Trip> = trips): List<String> =
        of.map { it.headsign }.filter { it.isNotEmpty() }.distinct().sorted()

    /** Courses dont la destination contient [direction], sinon celles dont la destination lui ressemble le plus */
    fun tripsTowards(direction: String): List<Trip> {
        val wanted = fold(direction)
        val containing = trips.filter { wanted in fold(it.headsign) }
        if (containing.isNotEmpty()) return containing
        val closest = headsigns().closestTo(direction) { listOf(it) }.toSet()
        return trips.filter { it.headsign in closest }
    }

    /** Quais desservis par [of], chacun une fois, dans l'ordre où les courses les rencontrent */
    private fun servedStops(of: List<Trip>): Set<String> {
        val served = LinkedHashSet<String>()
        for (trip in of) for (stopTime in stopTimes[trip.id].orEmpty()) served += stopTime.stopId
        return served
    }

    /** Zones d'arrêt desservies par [of] dont le nom (ou celui d'un quai) contient [query], avec leurs quais */
    fun matchStops(query: String, of: List<Trip>): Map<String, Set<String>> {
        val needle = fold(query)
        val found = LinkedHashMap<String, LinkedHashSet<String>>()
        // Un calcul par quai, pas par passage : une ligne de métro compte des centaines de milliers de passages
        for (stopId in servedStops(of)) {
            val area = area(stopId)
            if (needle in fold(areaName(area)) || needle in fold(stops[stopId]?.name.orEmpty())) {
                found.getOrPut(area) { LinkedHashSet() }.add(stopId)
            }
        }
        return found
    }

    /**
     * Zones d'arrêt desservies par [of] dont le nom (ou celui d'un quai) ressemble le plus à [query],
     * toutes celles à égalité, avec leurs quais ; pour quand [matchStops] ne trouve rien
     */
    fun approximateStops(query: String, of: List<Trip>): Map<String, Set<String>> {
        val names = LinkedHashMap<String, LinkedHashSet<String>>()
        val served = LinkedHashMap<String, LinkedHashSet<String>>()
        for (stopId in servedStops(of)) {
            val area = area(stopId)
            names.getOrPut(area) { LinkedHashSet() }.addAll(listOf(areaName(area), stops[stopId]?.name.orEmpty()))
            served.getOrPut(area) { LinkedHashSet() }.add(stopId)
        }
        return names.keys.closestTo(query) { names.getValue(it) }.associateWith { served.getValue(it) }
    }

    /** Destinations de [of] qui passent par l'une des zones [areas] */
    fun headsignsServing(areas: Set<String>, of: List<Trip> = trips): List<String> =
        headsigns(of.filter { trip -> stopTimes[trip.id].orEmpty().any { area(it.stopId) in areas } })

    /** Un arrêt de la ligne, pour illustrer un exemple : le premier du fichier */
    fun exampleStopName(): String? = stopTimes.values.firstOrNull()?.firstOrNull()?.let { areaName(area(it.stopId)) }

    /** Arrêts de chaque sens ; la course la plus longue de chaque sens en donne l'ordre */
    fun stopsByDirection(of: List<Trip>): List<DirectionStops> {
        val longest = HashMap<String, Trip>()
        for (trip in of) {
            val times = stopTimes[trip.id] ?: continue
            if (times.size > (longest[trip.directionId]?.let { stopTimes[it.id]?.size } ?: 0)) {
                longest[trip.directionId] = trip
            }
        }
        return longest.entries.sortedWith(compareBy({ it.key }, { it.value.id })).map { (_, trip) ->
            val names = mutableListOf<String>()
            for (stopTime in stopTimes.getValue(trip.id).sorted()) {
                val name = areaName(area(stopTime.stopId))
                if (names.lastOrNull() != name) names += name
            }
            DirectionStops(trip.headsign, names)
        }
    }

    /**
     * Prochains passages aux zones [matched] (voir [matchStops]) pour les courses [of], à partir de
     * [now] (passages de moins de 30 s compris). Les courses suivies en temps réel ([updates]) prennent
     * leurs heures du flux, les autres celles du calendrier ([activeDates] : dates actives par service).
     */
    fun departures(
        of: List<Trip>,
        matched: Map<String, Set<String>>,
        now: Long,
        zone: TimeZone,
        activeDates: Map<String, Set<LocalDate>>,
        updates: Map<String, TripUpdate>,
    ): List<StopDepartures> {
        val wantedStops = matched.values.flatten().toSet()
        val events = HashMap<String, MutableSet<Departure>>()
        fun add(stopId: String, departure: Departure) {
            if (stopId in wantedStops) events.getOrPut(stopId) { HashSet() }.add(departure)
        }
        for (trip in of) {
            val times = stopTimes[trip.id].orEmpty().sorted()
            if (times.isEmpty()) continue
            val headsign = trip.headsign.ifEmpty { areaName(area(times.last().stopId)) }
            val update = updates[trip.id]
            if (update == null) {
                for (date in activeDates[trip.serviceId].orEmpty()) {
                    val origin = serviceDayOrigin(date, zone)
                    for (stopTime in times) add(stopTime.stopId, Departure(origin + stopTime.time, headsign, false))
                }
                continue
            }
            if (update.cancelled) continue
            val dates = activeDates[trip.serviceId]?.takeIf { it.isNotEmpty() }?.sorted() ?: serviceDates(now, zone)
            for ((stopId, time) in realtimeTimes(times, update, dates.map { serviceDayOrigin(it, zone) })) {
                add(stopId, Departure(time, headsign, true))
            }
        }
        return matched.entries.sortedBy { areaName(it.key) }.map { (area, stopIds) ->
            val here = stopIds.flatMap { events[it].orEmpty() }.filter { it.time >= now - 30 }.sorted()
            val byHeadsign = LinkedHashMap<String, MutableList<Departure>>()
            for (departure in here) byHeadsign.getOrPut(departure.headsign) { mutableListOf() }.add(departure)
            StopDepartures(
                stopId = area,
                name = areaName(area),
                description = stopDescription(area),
                directions = byHeadsign.entries
                    .sortedBy { it.value.first().time }
                    .map { HeadsignDepartures(it.key, it.value) },
            )
        }
    }
}
