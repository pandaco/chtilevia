package fr.chtilevia.domain

/** Mode de transport d'une ligne, déduit du `route_type` GTFS */
enum class TransportMode {
    BUS,
    TRAM,
    METRO,
    ;

    companion object {
        /** `route_type` GTFS : 0 tram, 1 métro, tout le reste est traité comme un bus */
        fun fromRouteType(routeType: String): TransportMode = when (routeType) {
            "0" -> TRAM
            "1" -> METRO
            else -> BUS
        }
    }
}

/** Ligne du réseau (`routes.txt`) */
data class Line(
    val id: String,
    /** Nom court affiché aux voyageurs : « 10 », « TRAM », « M1 » */
    val name: String,
    val mode: TransportMode,
    val description: String,
) {
    /** Nom de la ligne tel qu'on le prononce : « Bus 10 », « Tram », « Métro 1 » */
    val label: String
        get() = when (mode) {
            TransportMode.TRAM -> "Tram"
            TransportMode.METRO -> "Métro " + name.trimStart('M', 'm')
            TransportMode.BUS -> "Bus $name"
        }
}

/** Arrêt physique ou zone d'arrêt (`stops.txt`) */
data class Stop(
    val id: String,
    val name: String,
    val description: String,
    /** Zone d'arrêt qui regroupe les quais ; null pour une zone ou un arrêt isolé */
    val parentStation: String?,
)

/** Course (`trips.txt`) */
data class Trip(
    val id: String,
    val lineId: String,
    val serviceId: String,
    /** Destination affichée ; peut être vide */
    val headsign: String,
    val directionId: String,
)

/**
 * Passage théorique d'une course à un arrêt. [time] est compté en secondes depuis l'origine du
 * jour de service (voir [serviceDayOrigin]) et peut dépasser 24 h pour les courses après minuit.
 */
data class StopTime(
    val sequence: Int,
    val stopId: String,
    val time: Int,
) : Comparable<StopTime> {
    override fun compareTo(other: StopTime): Int = compareValuesBy(this, other, { it.sequence }, { it.stopId }, { it.time })
}
