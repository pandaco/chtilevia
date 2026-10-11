package fr.chtilevia.presentation

import fr.chtilevia.application.BikeStations
import fr.chtilevia.application.Departures
import fr.chtilevia.domain.BikeMode
import fr.chtilevia.domain.localDateTime
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
