package fr.chtilevia.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/** Position en degrés décimaux (WGS 84) */
data class Position(val latitude: Double, val longitude: Double)

/**
 * Distance à vol d'oiseau (haversine, Terre de rayon 6 371 km), arrondie au mètre. Même calcul,
 * dans le même ordre, que le shell : les arrondis tombent pareil.
 */
fun distanceMeters(from: Position, to: Position): Int {
    val p1 = from.latitude * (PI / 180.0)
    val p2 = to.latitude * (PI / 180.0)
    val s1 = sin((p2 - p1) / 2)
    val s2 = sin((to.longitude - from.longitude) * (PI / 180.0) / 2)
    val a = s1 * s1 + cos(p1) * cos(p2) * s2 * s2
    return floor(2 * 6371000.0 * asin(sqrt(a)) + 0.5).toInt()
}
