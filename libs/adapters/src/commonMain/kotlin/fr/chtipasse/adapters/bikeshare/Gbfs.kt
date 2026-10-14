package fr.chtipasse.adapters.bikeshare

import fr.chtipasse.application.BikeShareFeed
import fr.chtipasse.domain.BikeStation
import fr.chtipasse.domain.BikeStationStatus
import fr.chtipasse.domain.Position
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Vélos en libre service au format GBFS : [fetch] lit un fichier de la racine du flux
 * (`station_information.json`, `station_status.json`) et renvoie null s'il est injoignable.
 */
class GbfsFeed(private val fetch: (file: String) -> ByteArray?) : BikeShareFeed {
    override fun stations(): List<BikeStation>? {
        val information = fetch("station_information.json") ?: return null
        val status = fetch("station_status.json") ?: return null
        return Gbfs.stations(information.decodeToString(), status.decodeToString())
    }
}

object Gbfs {
    /** Stations dans l'ordre de `station_information`, chacune avec son état s'il figure dans `station_status` */
    fun stations(information: String, status: String): List<BikeStation> {
        val statusById = stationsOf(status).associateBy { it["station_id"]?.text() }
        return stationsOf(information).map { station ->
            val id = station["station_id"]?.text().orEmpty()
            BikeStation(
                id = id,
                name = station["name"]?.text().orEmpty(),
                capacity = station["capacity"]?.number()?.toInt(),
                position = station["lat"]?.number()?.let { lat -> station["lon"]?.number()?.let { Position(lat, it) } },
                status = statusById[id]?.let { s ->
                    BikeStationStatus(
                        installed = s["is_installed"]?.flag(),
                        renting = s["is_renting"]?.flag(),
                        returning = s["is_returning"]?.flag(),
                        bikes = s["num_bikes_available"]?.number()?.toInt(),
                        docks = s["num_docks_available"]?.number()?.toInt(),
                        lastReported = s["last_reported"]?.number()?.toLong(),
                    )
                },
            )
        }
    }

    private fun stationsOf(document: String): List<JsonObject> =
        (Json.parseToJsonElement(document).jsonObject["data"]?.jsonObject?.get("stations") as? JsonArray)
            .orEmpty()
            .map { it.jsonObject }

    private fun JsonElement.text(): String? = (this as? JsonPrimitive)?.contentOrNull

    private fun JsonElement.number(): Double? = (this as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull

    private fun JsonElement.flag(): Boolean? = (this as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull
}
