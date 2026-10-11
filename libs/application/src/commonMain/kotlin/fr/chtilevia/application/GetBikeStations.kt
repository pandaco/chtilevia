package fr.chtilevia.application

import fr.chtilevia.domain.BikeMode
import fr.chtilevia.domain.BikeStation
import fr.chtilevia.domain.matching

/** Stations V'Lille trouvées pour [query], à l'instant [now] (pour l'âge des données) */
data class BikeStations(val query: String, val mode: BikeMode, val now: Long, val stations: List<BikeStation>)

/** Disponibilité des stations V'Lille dont le nom contient la recherche */
class GetBikeStations(private val feed: BikeShareFeed, private val clock: Clock) {
    fun execute(query: String, mode: BikeMode): BikeStations {
        if (query.isEmpty()) throw ChtiError.MissingStation()
        val stations = feed.stations() ?: throw ChtiError.BikeServiceUnreachable()
        val found = stations.matching(query)
        if (found.isEmpty()) throw ChtiError.UnknownStation(query)
        return BikeStations(query, mode, clock.now(), found)
    }
}
