package fr.chtipasse.presentation

import fr.chtipasse.application.BikeStations
import fr.chtipasse.application.Departures
import fr.chtipasse.application.Nearby
import fr.chtipasse.domain.BikeMode
import fr.chtipasse.domain.localDateTime
import kotlinx.datetime.TimeZone

/** Phrases sans emoji ni colonnes, pensées pour la synthèse vocale (≡ `chti --voix`) */
internal class VoiceFormatter(private val zone: TimeZone) {
    fun departures(result: Departures): String = buildString {
        for (stop in result.stops) {
            val head = "${result.line.label}, arrêt ${stop.name}"
            if (stop.directions.isEmpty()) append("$head : aucun passage prévu.\n")
            for (direction in stop.directions) {
                val allPlanned = direction.departures.none { it.realtime }
                val parts = direction.departures.map { departure ->
                    val minutes = minutesUntil(departure.time, result.now)
                    val time = when {
                        minutes <= 0 -> "à l'approche"
                        minutes < 60 -> "dans $minutes minute${if (minutes > 1) "s" else ""}"
                        else -> localDateTime(departure.time, zone).let {
                            if (it.minute == 0) "à ${it.hour} h" else "à ${it.hour} h ${it.minute.toString().padStart(2, '0')}"
                        }
                    }
                    if (!departure.realtime && !allPlanned) "$time (prévu)" else time
                }
                val tail = if (allPlanned) " Horaires théoriques." else ""
                append("$head, vers ${direction.headsign} : ${parts.joinToString(", puis ")}.$tail\n")
            }
        }
    }

    fun bikeStations(result: BikeStations): String = buildString {
        for (station in result.stations) {
            val status = station.status
            val state = if (status == null || status.isEmpty()) "statut indisponible" else when {
                status.installed != true -> "station hors service"
                result.mode == BikeMode.TAKE -> if (status.renting == true) bikes(status.bikes) else "location impossible"
                result.mode == BikeMode.RETURN -> if (status.returning == true) docks(status.docks) else "dépôt impossible"
                status.renting != true -> "station hors service"
                else -> bikes(status.bikes) + " et " + docks(status.docks)
            }
            append("${station.name} : $state.\n")
        }
    }

    fun nearby(result: Nearby): String = buildString {
        if (result.stops.isEmpty()) {
            if (result.mode.wantsStops) append("Aucun arrêt desservi à proximité.\n")
        } else {
            val title = if (result.stops.size == 1) "Arrêt le plus proche" else "Arrêts les plus proches"
            val parts = result.stops.map { near ->
                "${near.stop.name}, à ${distance(near.distance, voice = true)}, ${spokenList(near.lines.map { it.label })}"
            }
            append("$title : ${parts.joinToString(" ; ")}.\n")
        }
        val stations = result.stations
        if (stations == null) {
            if (result.mode.wantsBikes) append("V'Lille injoignable.\n")
        } else if (stations.isNotEmpty()) {
            val title = if (stations.size == 1) "Station V'Lille la plus proche" else "Stations V'Lille les plus proches"
            val parts = stations.map { near ->
                val status = near.station.status
                val state = when {
                    status == null || status.isEmpty() -> "statut indisponible"
                    status.installed != true || status.renting != true -> "station hors service"
                    else -> bikes(status.bikes) + " et " + docks(status.docks)
                }
                "${near.station.name}, à ${distance(near.distance, voice = true)}, $state"
            }
            append("$title : ${parts.joinToString(" ; ")}.\n")
        }
    }

    private fun bikes(count: Int?) = when (count) {
        0 -> "aucun vélo disponible"
        1 -> "1 vélo disponible"
        else -> "$count vélos disponibles"
    }

    private fun docks(count: Int?) = when (count) {
        0 -> "aucune place libre"
        1 -> "1 place libre"
        else -> "$count places libres"
    }
}
