package fr.chtilevia.presentation

import fr.chtilevia.application.BikeStations
import fr.chtilevia.application.Departures
import fr.chtilevia.application.FavoritesListing
import fr.chtilevia.application.StopList
import fr.chtilevia.domain.BikeMode
import fr.chtilevia.domain.BikeStationStatus
import kotlinx.datetime.TimeZone

/** Sortie terminal, colonnes et emojis (≡ `chti` sans option) */
internal class TextFormatter(private val zone: TimeZone) {
    fun departures(result: Departures): String = buildString {
        for (stop in result.stops) {
            append("\n${result.line.name} — ${stop.name}  (${stop.description})\n")
            if (stop.directions.isEmpty()) append("  aucun passage prévu\n")
            for (direction in stop.directions) {
                append("  → ${direction.headsign}\n")
                for (departure in direction.departures) {
                    val minutes = minutesUntil(departure.time, result.now)
                    val time = when {
                        minutes <= 0 -> "à l'approche"
                        minutes < 60 -> "dans $minutes min"
                        else -> "dans ${minutes / 60} h ${(minutes % 60).toString().padStart(2, '0')}"
                    }
                    val source = if (departure.realtime) "⏱  temps réel" else "🕒 théorique"
                    append("     ${hhmm(departure.time, zone)}  ${time.padEnd(14)} $source\n")
                }
            }
        }
    }

    fun stopList(result: StopList): String = buildString {
        append("Ligne ${result.line.name} — ${result.line.description}\n")
        for (direction in result.directions) {
            append("\n  Direction ${direction.headsign} :\n")
            for (stop in direction.stops) append("    • $stop\n")
        }
    }

    fun bikeStations(result: BikeStations): String = buildString {
        for (station in result.stations) {
            val status = station.status
            val age = "\n  maj il y a ${(result.now - (status?.lastReported ?: 0)).floorDiv(60L)} min"
            append("${station.name}  (${station.capacity} bornes)\n")
            append(
                if (status == null || status.isEmpty()) "  ⚠️  statut indisponible" else when {
                    status.installed != true -> "  ⛔ station hors service"
                    result.mode == BikeMode.TAKE ->
                        if (status.renting == true) "  🚲 ${status.bikes} vélo(s) dispo$age" else "  ⛔ location impossible"
                    result.mode == BikeMode.RETURN ->
                        if (status.returning == true) "  🅿️  ${status.docks} place(s) libre(s)$age" else "  ⛔ dépôt impossible"
                    status.renting != true -> "  ⛔ station hors service"
                    else -> "  🚲 ${status.bikes} vélo(s) dispo   🅿️  ${status.docks} place(s) libre(s)" +
                        (if (status.returning == true) "" else "   ⚠️  dépôt impossible") + age
                },
            )
            append("\n\n")
        }
    }

    fun favorites(listing: FavoritesListing): String = if (listing.isEmpty) {
        "Aucun favori. Crée le fichier ${listing.location}\n" +
            "au format « nom: commande », ex : travail: tram croise laroche vers roubaix\n"
    } else {
        "Favoris (${listing.location}) :\n" + listing.lines.joinToString("") { "  $it\n" }
    }
}

/** État sans aucun champ connu : rien à en dire */
internal fun BikeStationStatus.isEmpty(): Boolean =
    listOf(installed, renting, returning, bikes, docks, lastReported).all { it == null }
