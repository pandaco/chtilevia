package fr.chtipasse.presentation

import kotlin.math.floor

/** Distance à l'écran (« 120 m », « 1,2 km ») ou à l'oral (« 120 mètres », « 1,2 kilomètre ») ; aussi utilisée par l'appli */
fun distance(meters: Int, voice: Boolean = false): String {
    if (meters < 1000) return if (voice) "$meters mètre${if (meters > 1) "s" else ""}" else "$meters m"
    val tenths = floor(meters / 100.0 + 0.5).toInt()
    val km = if (tenths % 10 != 0) "${tenths / 10},${tenths % 10}" else "${tenths / 10}"
    return if (voice) "$km kilomètre${if (tenths >= 20) "s" else ""}" else "$km km"
}

/** « a », « a et b », « a, b et c » */
internal fun spokenList(items: List<String>): String =
    if (items.size == 1) items[0] else items.dropLast(1).joinToString(", ") + " et " + items.last()
