package fr.chtilevia.presentation

import fr.chtilevia.application.BikeStations
import fr.chtilevia.application.ChtiError
import fr.chtilevia.application.Departures
import fr.chtilevia.application.FavoritesListing
import fr.chtilevia.application.Nearby
import fr.chtilevia.application.StopList
import fr.chtilevia.domain.BikeMode
import fr.chtilevia.domain.BikeStation
import fr.chtilevia.domain.Line
import kotlinx.datetime.TimeZone

/** Documents du contrat (`libs/contract/schema/response.schema.json`, ≡ `chti --json`) */
internal class JsonFormatter(private val zone: TimeZone) {
    private fun line(line: Line) = Json.obj(
        "id" to Json.Str(line.id),
        "name" to Json.Str(line.name),
        "mode" to Json.Str(line.mode.name.lowercase()),
        "label" to Json.Str(line.label),
        "description" to Json.Str(line.description),
    )

    fun departures(result: Departures) = Json.obj(
        "type" to Json.Str("departures"),
        "line" to line(result.line),
        "now" to Json.Str(isoLocal(result.now, zone)),
        "stops" to Json.arr(
            result.stops.map { stop ->
                Json.obj(
                    "id" to Json.Str(stop.stopId),
                    "name" to Json.Str(stop.name),
                    "description" to Json.Str(stop.description),
                    "directions" to Json.arr(
                        stop.directions.map { direction ->
                            Json.obj(
                                "headsign" to Json.Str(direction.headsign),
                                "departures" to Json.arr(
                                    direction.departures.map {
                                        Json.obj(
                                            "time" to Json.Str(isoLocal(it.time, zone)),
                                            "minutes" to Json.Num(maxOf(0, minutesUntil(it.time, result.now))),
                                            "realtime" to Json.Bool(it.realtime),
                                        )
                                    },
                                ),
                            )
                        },
                    ),
                )
            },
        ),
        "warnings" to Json.arr(
            result.warnings.mapNotNull { notice ->
                notice.warningCode()?.let { Json.obj("code" to Json.Str(it), "message" to Json.Str(notice.text())) }
            },
        ),
    )

    fun stopList(result: StopList) = Json.obj(
        "type" to Json.Str("stops"),
        "line" to line(result.line),
        "directions" to Json.arr(
            result.directions.map {
                Json.obj("headsign" to Json.Str(it.headsign), "stops" to Json.arr(it.stops.map(Json::Str)))
            },
        ),
    )

    fun bikeStations(result: BikeStations) = Json.obj(
        "type" to Json.Str("bike_stations"),
        "query" to Json.Str(result.query),
        "mode" to Json.Str(
            when (result.mode) {
                BikeMode.ALL -> "all"
                BikeMode.TAKE -> "take"
                BikeMode.RETURN -> "return"
            },
        ),
        "stations" to Json.arr(result.stations.map { station(it, result.now) }),
    )

    /** Station V'Lille et son état ; [extra] : champs ajoutés à la fin (distance…) */
    private fun station(station: BikeStation, now: Long, vararg extra: Pair<String, Json>): Json {
        val status = station.status
        return Json.obj(
            "id" to Json.Str(station.id),
            "name" to Json.Str(station.name),
            "capacity" to Json.num(station.capacity),
            "status" to Json.Str(
                when (station.availability) {
                    BikeStation.Availability.UNKNOWN -> "unknown"
                    BikeStation.Availability.OUT_OF_SERVICE -> "out_of_service"
                    BikeStation.Availability.OK -> "ok"
                },
            ),
            "bikes" to Json.num(status?.bikes),
            "docks" to Json.num(status?.docks),
            "renting" to Json.bool(status?.renting),
            "returning" to Json.bool(status?.returning),
            "last_reported" to Json.str(status?.lastReported?.let(::isoUtc)),
            "age_minutes" to Json.num(status?.lastReported?.let { (now - it).floorDiv(60L) }),
            *extra,
        )
    }

    fun nearby(result: Nearby) = Json.obj(
        "type" to Json.Str("nearby"),
        "stops" to Json.arr(
            result.stops.map { near ->
                Json.obj(
                    "id" to Json.Str(near.stop.id),
                    "name" to Json.Str(near.stop.name),
                    "description" to Json.Str(near.stop.description.trim(' ', '-')),
                    "distance_m" to Json.Num(near.distance.toLong()),
                    "lines" to Json.arr(near.lines.map(::line)),
                )
            },
        ),
        "bike_stations" to Json.arr(
            result.stations.orEmpty().map { station(it.station, result.now, "distance_m" to Json.Num(it.distance.toLong())) },
        ),
        "warnings" to Json.arr(
            result.warnings.mapNotNull { notice ->
                notice.warningCode()?.let { Json.obj("code" to Json.Str(it), "message" to Json.Str(notice.text())) }
            },
        ),
    )

    fun favorites(listing: FavoritesListing) = Json.obj(
        "type" to Json.Str("favorites"),
        "path" to Json.Str(listing.location),
        "favorites" to Json.arr(
            listing.favorites.map {
                Json.obj("name" to Json.Str(it.name), "commands" to Json.arr(it.commands.map(Json::Str)))
            },
        ),
    )

    fun favorite(name: String, results: List<Json>) =
        Json.obj("type" to Json.Str("favorite"), "name" to Json.Str(name), "results" to Json.arr(results))

    fun update(location: String) = Json.obj("type" to Json.Str("update"), "path" to Json.Str(location))

    fun error(error: ChtiError): Json {
        val message = error.message()
        return Json.obj(
            "type" to Json.Str("error"),
            "code" to Json.Str(error.code),
            "message" to Json.Str(message.text),
            "details" to if (message.hintTitle != null) Json.obj(message.hintKey to Json.arr(message.hint.map(Json::Str))) else Json.obj(),
        )
    }
}
