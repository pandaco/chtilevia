package fr.chtilevia.presentation

import fr.chtilevia.application.BikeStations
import fr.chtilevia.application.Departures
import fr.chtilevia.application.Nearby
import fr.chtilevia.domain.BikeMode
import fr.chtilevia.domain.BikeStation
import fr.chtilevia.domain.Line
import fr.chtilevia.domain.TransportMode
import kotlinx.datetime.TimeZone

/**
 * Réponse écrite compacte, une ligne par information, avec des pictos (≡ `chti --court`, propre au
 * cœur Kotlin) : pensée pour une notification ou un widget, là où les phrases de la voix sont trop
 * longues. Ex. « Gare Lille Europe : 🚲 14 · 🅿️ 18 ».
 */
internal class BriefFormatter(private val zone: TimeZone) {
    fun departures(result: Departures): String = buildString {
        val head = "${result.line.icon} ${result.line.label}"
        for (stop in result.stops) {
            if (stop.directions.isEmpty()) append("$head · ${stop.name} : aucun passage prévu\n")
            for (direction in stop.directions) {
                val times = direction.departures.joinToString(", ") { departure ->
                    val minutes = minutesUntil(departure.time, result.now)
                    when {
                        minutes <= 0 -> "à l'approche"
                        minutes < 60 -> "$minutes min"
                        else -> hhmm(departure.time, zone)
                    }
                }
                val planned = if (direction.departures.none { it.realtime }) " (théorique)" else ""
                append("$head · ${stop.name} → ${direction.headsign} : $times$planned\n")
            }
        }
    }

    fun bikeStations(result: BikeStations): String =
        result.stations.joinToString("") { "${it.name} : ${it.briefState(result.mode)}\n" }

    fun nearby(result: Nearby): String = buildString {
        if (result.mode.wantsStops && result.stops.isEmpty()) append("📍 Aucun arrêt desservi à proximité\n")
        for (near in result.stops) {
            append("📍 ${near.stop.name} (${distance(near.distance, voice = false)}) : ${near.lines.joinToString(", ") { it.label }}\n")
        }
        val stations = result.stations
        if (stations == null && result.mode.wantsBikes) append("V'Lille injoignable\n")
        for (near in stations.orEmpty()) {
            append("${near.station.name} (${distance(near.distance, voice = false)}) : ${near.station.briefState()}\n")
        }
    }

    private val Line.icon: String
        get() = when (mode) {
            TransportMode.BUS -> "🚌"
            TransportMode.TRAM -> "🚊"
            TransportMode.METRO -> "🚇"
        }
}

/**
 * État compact d'une station, comme dans `chti --court` : « 🚲 14 · 🅿️ 18 » (vélos disponibles,
 * places libres), seulement ce qui compte pour [mode] ; aussi utilisé par l'appli (liste de la carte)
 */
fun BikeStation.briefState(mode: BikeMode = BikeMode.ALL): String {
    val status = status
    return when {
        status == null || status.isEmpty() -> "statut indisponible"
        status.installed != true -> "⛔ hors service"
        mode == BikeMode.TAKE -> if (status.renting == true) "🚲 ${status.bikes}" else "⛔ location impossible"
        mode == BikeMode.RETURN -> if (status.returning == true) "🅿️ ${status.docks}" else "⛔ dépôt impossible"
        status.renting != true -> "⛔ hors service"
        else -> "🚲 ${status.bikes} · 🅿️ ${status.docks}" + if (status.returning == true) "" else " (dépôt impossible)"
    }
}
