package fr.chtipasse.android

import fr.chtipasse.presentation.distance
import org.json.JSONArray
import org.json.JSONObject

/** Passage en badge : « 4 min », « à l'approche » ou « 13:56 », en temps réel ou théorique */
data class Badge(val text: String, val realtime: Boolean)

/** Arrêt (zone GTFS) ou station V'Lille dont parle une carte : la toucher l'ouvre sur la carte */
data class Place(val id: String, val name: String, val kind: MapPoint.Kind) {
    val isStation get() = kind == MapPoint.Kind.BIKE
}

/** Une carte de la réponse affichée : de quoi on parle, puis la réponse (texte ou badges de passages) */
data class AnswerCard(
    val title: String,
    val subtitle: String? = null,
    val value: String? = null,
    val badges: List<Badge> = emptyList(),
    val place: Place? = null,
)

/**
 * Cartes de la réponse, tirées du document JSON du contrat (`chti --json`, voir
 * libs/contract/schema) : il dit, passage par passage, s'il vient du temps réel. Null si le document
 * n'est pas lisible ou d'un type sans carte (liste d'arrêts…) : l'écran montre alors le texte.
 */
object AnswerCards {
    fun parse(output: String): List<AnswerCard>? = try {
        // Les messages du traitement (« Mise à jour du référentiel… ») précèdent le document
        cards(JSONObject(output.substring(output.indexOf('{'))))
    } catch (_: Exception) {
        null
    }

    private fun cards(doc: JSONObject): List<AnswerCard>? = when (doc.getString("type")) {
        "departures" -> departures(doc)
        "bike_stations" -> doc.getJSONArray("stations").objects().map {
            AnswerCard(it.getString("name"), value = bikeState(it, doc.getString("mode")), place = station(it))
        }
        "nearby" -> nearby(doc)
        "favorite" -> doc.getJSONArray("results").objects().flatMap { cards(it) ?: emptyList() }
        "error" -> listOf(AnswerCard(doc.getString("message").replaceFirstChar { it.uppercaseChar() }))
        else -> null
    }

    private fun departures(doc: JSONObject): List<AnswerCard> {
        val line = doc.getJSONObject("line")
        val icon = when (line.getString("mode")) {
            "tram" -> "🚊"
            "metro" -> "🚇"
            else -> "🚌"
        }
        val kind = kindOf(listOf(line.getString("mode")))
        return doc.getJSONArray("stops").objects().flatMap { stop ->
            val title = "$icon ${line.getString("label")} · ${stop.getString("name")}"
            val place = Place(stop.getString("id"), stop.getString("name"), kind)
            val directions = stop.getJSONArray("directions").objects()
            if (directions.isEmpty()) {
                listOf(AnswerCard(title, value = "aucun passage prévu", place = place))
            } else {
                directions.map { direction ->
                    val badges = direction.getJSONArray("departures").objects().map { departure ->
                        val minutes = departure.getInt("minutes")
                        val text = when {
                            minutes <= 0 -> "à l'approche"
                            minutes < 60 -> "$minutes min"
                            // « 2026-10-10T13:56:00+02:00 » : heure locale
                            else -> departure.getString("time").substring(11, 16)
                        }
                        Badge(text, departure.getBoolean("realtime"))
                    }
                    AnswerCard(title, subtitle = "→ ${direction.getString("headsign")}", badges = badges, place = place)
                }
            }
        }
    }

    private fun nearby(doc: JSONObject): List<AnswerCard> =
        doc.getJSONArray("stops").objects().map { stop ->
            val lines = stop.getJSONArray("lines").objects()
            val place = Place(stop.getString("id"), stop.getString("name"), kindOf(lines.map { it.getString("mode") }))
            AnswerCard(
                "📍 ${stop.getString("name")}",
                subtitle = distance(stop.getInt("distance_m")),
                value = lines.joinToString(", ") { it.getString("label") },
                place = place,
            )
        } + doc.getJSONArray("bike_stations").objects().map {
            AnswerCard(it.getString("name"), subtitle = distance(it.getInt("distance_m")), value = bikeState(it, "all"), place = station(it))
        }

    private fun station(json: JSONObject) = Place(json.getString("id"), json.getString("name"), MapPoint.Kind.BIKE)

    /** Couleur d'un arrêt sur la carte : son mode le plus « lourd » (métro, puis tram, puis bus) */
    private fun kindOf(modes: List<String>): MapPoint.Kind = when {
        "metro" in modes -> MapPoint.Kind.METRO
        "tram" in modes -> MapPoint.Kind.TRAM
        else -> MapPoint.Kind.BUS
    }

    /** Comme `chti --court` : « 🚲 14 · 🅿️ 18 », seulement ce qui compte pour [mode] (all, take, return) */
    private fun bikeState(station: JSONObject, mode: String): String {
        val bikes = station.opt("bikes")
        val docks = station.opt("docks")
        return when {
            station.getString("status") == "unknown" -> "statut indisponible"
            station.getString("status") == "out_of_service" -> "⛔ hors service"
            mode == "take" -> if (station.optBoolean("renting")) "🚲 $bikes" else "⛔ location impossible"
            mode == "return" -> if (station.optBoolean("returning")) "🅿️ $docks" else "⛔ dépôt impossible"
            !station.optBoolean("renting") -> "⛔ hors service"
            else -> "🚲 $bikes · 🅿️ $docks" + if (station.optBoolean("returning")) "" else " (dépôt impossible)"
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
}
