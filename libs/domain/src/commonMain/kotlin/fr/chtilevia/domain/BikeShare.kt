package fr.chtilevia.domain

/** Ce que l'on veut faire à une station : tout voir, prendre un vélo ou en déposer un */
enum class BikeMode { ALL, TAKE, RETURN }

/**
 * État d'une station (GBFS `station_status`). Les champs absents du flux restent null : on ne
 * devine rien, l'absence est montrée telle quelle.
 */
data class BikeStationStatus(
    val installed: Boolean?,
    val renting: Boolean?,
    val returning: Boolean?,
    val bikes: Int?,
    val docks: Int?,
    /** Dernière mise à jour de la station (epoch s) */
    val lastReported: Long?,
)

/** Station V'Lille (GBFS `station_information`), avec son état s'il est connu */
data class BikeStation(
    val id: String,
    val name: String,
    val capacity: Int?,
    val status: BikeStationStatus?,
) {
    /** Disponibilité d'ensemble : inconnue, hors service (ni location ni dépôt), ou en service */
    val availability: Availability
        get() = when {
            status == null -> Availability.UNKNOWN
            status.installed != true || (status.renting != true && status.returning != true) -> Availability.OUT_OF_SERVICE
            else -> Availability.OK
        }

    enum class Availability { UNKNOWN, OUT_OF_SERVICE, OK }
}

/** Stations dont le nom contient [query] (casse et accents ignorés), dans l'ordre du flux */
fun List<BikeStation>.matching(query: String): List<BikeStation> {
    val needle = fold(query)
    return filter { needle in fold(it.name) }
}
