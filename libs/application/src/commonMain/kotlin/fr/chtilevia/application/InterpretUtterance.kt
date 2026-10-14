package fr.chtilevia.application

import fr.chtilevia.domain.NUMBER_WORDS
import fr.chtilevia.domain.fold

/**
 * Phrase libre (reconnaissance vocale) → mots d'une commande chti, ex. « quand passe le prochain
 * bus dix à l'arrêt Marais direction Lille Flandres » → `bus 10 marais -d lille flandres`.
 */
class InterpretUtterance(private val favorites: FavoritesStore) {
    fun execute(utterance: String): List<String> {
        var words = fold(utterance).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) throw ChtiError.EmptyUtterance()
        // « à proximité », « autour de moi », « près d'ici », « le plus proche » : autour de la
        // position, que l'appelant ajoute (le téléphone la connaît, pas le shell)
        if (nearMe(words)) return listOfNotNull("autour", nearbyMode(words))

        // « vers » / « direction » / « en direction de » : le reste de la phrase est la direction
        var direction = emptyList<String>()
        val marker = words.indexOfFirst { it == "vers" || it == "direction" }
        if (marker >= 0) {
            direction = words.drop(marker + 1).stripLeading(DIRECTION_FILLERS)
            words = words.take(marker)
            if (words.lastOrNull() == "en") words = words.dropLast(1)
        }
        val body = words.stripLeading(FILLERS)
        if (body.isEmpty()) throw ChtiError.NotUnderstood(utterance)
        if (direction.isEmpty()) {
            favoriteNamed(spokenKey(body.joinToString(" ")))?.let { return listOf(it) }
        }

        var rest = body.drop(1)
        val command: List<String> = when (val head = body[0]) {
            "bus", "ligne", "liane" -> {
                if (rest.isEmpty()) throw ChtiError.UtteranceMissingLine()
                var line = NUMBER_WORDS[rest.take(2).joinToString(" ")]
                if (line != null) {
                    rest = rest.drop(2)
                } else {
                    line = NUMBER_WORDS[rest[0]] ?: rest[0]
                    rest = rest.drop(1)
                }
                if (head == "liane" && line.all { it.isDigit() } && line.isNotEmpty()) line = "L$line"
                listOf("bus", line.uppercase())
            }
            "metro", "metros" -> {
                val number = rest.firstOrNull()?.let { (NUMBER_WORDS[it] ?: it).trimStart('m') }
                if (number != "1" && number != "2") throw ChtiError.UtteranceUnknownMetro()
                rest = rest.drop(1)
                listOf("metro", number)
            }
            "tram", "tramway" -> listOf("tram")
            else -> if (head in BIKE_WORDS || body.take(2) == listOf("v", "lille")) {
                return bike(if (head == "v") body.drop(2) else rest)
            } else {
                throw ChtiError.NotUnderstood(utterance)
            }
        }
        val stop = rest.stripLeading(STOP_FILLERS)
        if (stop.isEmpty()) throw ChtiError.UtteranceMissingStop(command.joinToString(" "))
        return command + stop + (if (direction.isNotEmpty()) listOf("-d", direction.joinToString(" ")) else emptyList())
    }

    private fun nearMe(words: List<String>): Boolean =
        words.any { it in NEAR_WORDS } || words.indices.any { i ->
            words[i] in AROUND && (words.subList(i + 1, minOf(i + 3, words.size)) in NEAR_TAILS ||
                words.subList(i + 1, minOf(i + 2, words.size)) in NEAR_TAILS)
        }

    /** « vélo à proximité » : seulement V'Lille ; « bus à proximité » : seulement les bus… ; null : tout */
    private fun nearbyMode(words: List<String>): String? {
        val said = words.map { NEARBY_SYNONYMS[it] ?: it }.toMutableSet()
        if ("v" in words && "lille" in words) said += "velo"
        return listOf("velo", "bus", "tram", "metro").firstOrNull { it in said }
    }

    private fun bike(words: List<String>): List<String> {
        var rest = words
        var mode = emptyList<String>()
        for ((verb, flag) in BIKE_VERBS) {
            if (verb in rest) {
                rest = rest.filter { it != verb }
                mode = listOf(flag)
            }
        }
        rest = rest.filter { it !in BIKE_NOISE }.stripLeading(STOP_FILLERS)
        if (rest.isEmpty()) throw ChtiError.UtteranceMissingStation()
        return listOf("velo") + rest + mode
    }

    /** Favori dont le nom, dit à voix haute, donne [key] ; à nom égal, la dernière ligne l'emporte */
    private fun favoriteNamed(key: String): String? =
        FavoritesFile.names(favorites.read().orEmpty()).lastOrNull { spokenKey(it) == key }

    private companion object {
        /** Mots ignorés en tête de phrase et en tête du nom d'arrêt (jamais au milieu : « Pont de Bois ») */
        val FILLERS = setOf(
            "quand", "passe", "est", "ce", "que", "qu", "il", "y", "a", "le", "la", "les", "l", "au",
            "aux", "de", "du", "des", "d", "prochain", "prochains", "prochaine", "prochaines",
            "passage", "passages", "arret", "station", "horaire", "horaires", "je", "veux", "pour",
            "moi", "donne", "dis", "quel", "quels", "c", "un",
        )
        val STOP_FILLERS = setOf("a", "au", "l", "arret", "station", "de", "du", "d")
        val DIRECTION_FILLERS = setOf("de", "d", "du", "la", "l")
        val NEAR_WORDS = setOf("proximite", "proche", "proches")
        val NEARBY_SYNONYMS = mapOf("velos" to "velo", "vlille" to "velo", "tramway" to "tram", "metros" to "metro")
        val AROUND = setOf("autour", "pres")
        val NEAR_TAILS = listOf(listOf("de", "moi"), listOf("d", "ici"), listOf("moi"), listOf("ici"))
        val BIKE_WORDS = setOf("velo", "velos", "vlille")
        val BIKE_VERBS = listOf(
            "prendre" to "--prendre", "emprunter" to "--prendre",
            "deposer" to "--deposer", "rendre" to "--deposer", "poser" to "--deposer",
        )
        val BIKE_NOISE = setOf("pour", "un", "mon", "velo", "velos")

        /** « Rentrer à vélo » et « rentrer-velo » donnent la même clé */
        fun spokenKey(text: String): String = fold(text).split(' ').filter { it.isNotEmpty() && it !in FILLERS }.joinToString(" ")

        fun List<String>.stripLeading(fillers: Set<String>): List<String> = dropWhile { it in fillers }
    }
}
