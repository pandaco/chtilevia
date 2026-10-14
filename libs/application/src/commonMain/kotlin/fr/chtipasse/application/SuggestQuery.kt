package fr.chtipasse.application

import fr.chtipasse.domain.BikeStation
import fr.chtipasse.domain.Line
import fr.chtipasse.domain.LineSchedule
import fr.chtipasse.domain.TransportMode
import fr.chtipasse.domain.fold

/** Proposition de saisie : ce qu'on affiche, et le texte complet de la requête si on la choisit */
data class Suggestion(val label: String, val text: String)

/** Étape où en est la requête (« Quelle ligne ? »…) et les propositions pour la compléter */
data class QuerySuggestions(val step: String, val items: List<Suggestion>)

/**
 * Aide à la saisie d'une question, au fil de la frappe : le mode, puis la ligne, l'arrêt, la
 * direction (« vers »), ou la station V'Lille et « pour prendre / déposer ». Les propositions sont
 * des phrases que [InterpretUtterance] comprend, tirées du référentiel, de V'Lille et des favoris.
 */
class SuggestQuery(
    private val timetable: TimetableRepository,
    private val bikeShare: BikeShareFeed,
    private val favorites: FavoritesStore,
    private val clock: Clock,
) {
    private var stations: List<BikeStation>? = null
    private var stationsAt = 0L
    private var withNearby = true

    /**
     * Étape et propositions pour [input] ; sans [nearby], pas de « à proximité » (une question de
     * favori : la position n'est connue qu'au moment de la poser)
     */
    fun execute(input: String, nearby: Boolean = true): QuerySuggestions {
        this.withNearby = nearby
        val words = input.trim().split(' ').filter { it.isNotEmpty() }
        val trailing = input.isEmpty() || input.endsWith(" ")
        // Mot en cours de frappe, et mots déjà écrits
        val partial = if (trailing) "" else words.last()
        val done = if (trailing) words else words.dropLast(1)
        val folded = done.map(::fold)
        return when (folded.firstOrNull()) {
            null -> start(partial)
            "bus", "ligne", "liane" -> bus(done, partial)
            "tram", "tramway" -> line(TransportMode.TRAM)?.let { stopAndDirection(done.take(1), it, done.drop(1), partial) } ?: NOTHING
            "metro" -> metro(done, partial)
            "velo", "velos", "vlille" -> bike(done, partial)
            else -> NOTHING
        }
    }

    /** Premier mot : le mode, « à proximité », ou un favori */
    private fun start(partial: String): QuerySuggestions {
        val names = FavoritesFile.names(favorites.read().orEmpty()).distinct()
        val items = MODES.filter { withNearby || "proximité" !in it.text } + names.map { Suggestion("⭐ $it", it) }
        val needle = fold(partial)
        return QuerySuggestions("Que cherches-tu ?", items.filter { needle.isEmpty() || fold(it.text).startsWith(needle) || needle in fold(it.text) })
    }

    private fun bus(done: List<String>, partial: String): QuerySuggestions {
        if (done.size == 1) {
            val needle = partial.uppercase()
            val lines = timetable.lines().filter { it.mode == TransportMode.BUS && it.name.uppercase().startsWith(needle) }
                .sortedWith(compareBy({ it.name.length }, { it.name }))
            val items = nearby("bus", partial) + lines.take(MAX).map { Suggestion("Bus ${it.name}", "${done[0]} ${it.name} ") }
            return QuerySuggestions("Quelle ligne ?", items)
        }
        val line = timetable.lines().firstOrNull { it.mode == TransportMode.BUS && it.name.equals(done[1], ignoreCase = true) }
            ?: return QuerySuggestions("Ligne inconnue : ${done[1]}", emptyList())
        return stopAndDirection(done.take(2), line, done.drop(2), partial)
    }

    private fun metro(done: List<String>, partial: String): QuerySuggestions {
        if (done.size == 1) {
            val items = listOf(Suggestion("Métro 1", "${done[0]} 1 "), Suggestion("Métro 2", "${done[0]} 2 "))
            return QuerySuggestions("Quel métro ?", items.filter { it.text.trim().endsWith(partial) || partial.isEmpty() })
        }
        val number = when (fold(done[1])) {
            "1", "un", "m1" -> "1"
            "2", "deux", "m2" -> "2"
            else -> return QuerySuggestions("Métro 1 ou 2 ?", emptyList())
        }
        val line = timetable.lines().firstOrNull { it.mode == TransportMode.METRO && it.name.trimStart('M', 'm') == number }
            ?: return NOTHING
        return stopAndDirection(done.take(2), line, done.drop(2), partial)
    }

    /** Après la ligne : l'arrêt, puis « vers » et la direction */
    private fun stopAndDirection(prefix: List<String>, line: Line, rest: List<String>, partial: String): QuerySuggestions {
        val start = prefix.joinToString(" ") + " "
        val schedule = LineSchedule(line, timetable.trips(line.id), timetable.stops(), timetable.stopTimes(line.id))
        val all = rest + listOfNotNull(partial.takeIf { it.isNotEmpty() })
        val vers = all.indexOfFirst { fold(it) == "vers" }
        if (vers >= 0) {
            val stop = all.take(vers).joinToString(" ")
            val needle = fold(all.drop(vers + 1).joinToString(" "))
            val directions = schedule.headsigns().filter { needle in fold(it) }
            return QuerySuggestions("Vers quelle direction ?", directions.map { Suggestion("→ $it", "$start$stop vers $it") })
        }
        val stops = schedule.stopsByDirection(schedule.trips).flatMap { it.stops }.distinct()
        val typed = all.joinToString(" ")
        val needle = fold(typed)
        // Arrêt écrit en entier : la direction, facultative
        if (needle.isNotEmpty() && partial.isEmpty() && stops.any { fold(it) == needle }) {
            val name = stops.first { fold(it) == needle }
            val directions = schedule.headsigns().map { Suggestion("→ $it", "$start$name vers $it") }
            return QuerySuggestions("Direction ? (facultatif)", directions)
        }
        val matching = stops.filter { fold(it).startsWith(needle) } + stops.filter { needle in fold(it) && !fold(it).startsWith(needle) }
        return QuerySuggestions("Quel arrêt ?", matching.take(MAX).map { Suggestion(it, "$start$it ") })
    }

    /** Après « vélo » : la station, puis « pour prendre / déposer » */
    private fun bike(done: List<String>, partial: String): QuerySuggestions {
        val start = done[0] + " "
        val all = done.drop(1) + listOfNotNull(partial.takeIf { it.isNotEmpty() })
        if (all.any { fold(it) == "pour" }) return QuerySuggestions("C'est complet", emptyList())
        val names = stations()?.map { it.name }?.distinct() ?: return QuerySuggestions("V'Lille injoignable", emptyList())
        val needle = fold(all.joinToString(" "))
        if (needle.isNotEmpty() && partial.isEmpty() && names.any { fold(it) == needle }) {
            val name = names.first { fold(it) == needle }
            return QuerySuggestions(
                "Prendre ou déposer ? (facultatif)",
                listOf(Suggestion("🚲 pour prendre", "$start$name pour prendre"), Suggestion("🅿️ pour déposer", "$start$name pour déposer")),
            )
        }
        val matching = names.filter { fold(it).startsWith(needle) } + names.filter { needle in fold(it) && !fold(it).startsWith(needle) }
        val items = nearby("vélo", partial).takeIf { done.size == 1 }.orEmpty() + matching.take(MAX).map { Suggestion(it, "$start$it ") }
        return QuerySuggestions("Quelle station ?", items)
    }

    /** Stations V'Lille, gardées quelques minutes : pas un téléchargement par lettre tapée */
    private fun stations(): List<BikeStation>? {
        val now = clock.now()
        if (stations == null || now - stationsAt > STATIONS_SECONDS) {
            stations = bikeShare.stations() ?: return stations
            stationsAt = now
        }
        return stations
    }

    private fun line(mode: TransportMode): Line? = timetable.lines().firstOrNull { it.mode == mode }

    /** « à… » tapé après le mode : « à proximité », pour ce mode seulement */
    private fun nearby(mode: String, partial: String): List<Suggestion> {
        val needle = fold(partial)
        return if (withNearby && needle.isNotEmpty() && "a proximite".startsWith(needle)) listOf(Suggestion("📍 $mode à proximité", "$mode à proximité")) else emptyList()
    }

    private companion object {
        const val MAX = 12
        const val STATIONS_SECONDS = 600L
        val NOTHING = QuerySuggestions("", emptyList())
        val MODES = listOf(
            Suggestion("🚌 Bus", "bus "),
            Suggestion("🚊 Tram", "tram "),
            Suggestion("🚇 Métro", "métro "),
            Suggestion("🚲 Vélo (V'Lille)", "vélo "),
            Suggestion("📍 À proximité", "à proximité"),
            Suggestion("🚲 Vélos à proximité", "vélo à proximité"),
        )
    }
}
