package fr.chtilevia.application

import fr.chtilevia.domain.BikeMode

/** Commande chti, telle qu'on la tape ou qu'on l'enregistre dans un favori */
sealed interface Command {
    /** [count] null : nombre par défaut, choisi par la présentation (moins de passages à l'oral) */
    data class NextDepartures(val line: String, val stop: String, val direction: String, val count: Int?) : Command
    data class BikeStations(val station: String, val mode: BikeMode) : Command
    data object ListFavorites : Command
    data object Update : Command
    data object Help : Command
    data class Utterance(val text: String) : Command

    /** Un mot seul inconnu : peut-être un favori */
    data class FavoriteOrUnknown(val name: String) : Command
}

/**
 * Grammaire des commandes chti (sans les options globales `--voix` et `--json`, affaire de la
 * présentation) : `bus <ligne> [arrêt] [-d direction] [-n nombre]`, `tram …`, `metro <1|2> …`,
 * `velo <station> [--prendre|--deposer]`, `favoris`, `update`, `vocal <phrase>`, `<favori>`.
 */
object CommandLine {
    fun parse(words: List<String>): Command {
        require(words.isNotEmpty()) { "commande vide" }
        val args = words.drop(1)
        return when (words[0]) {
            "bus" -> {
                if (args.isEmpty()) throw ChtiError.MissingLine()
                transit(args[0], args.drop(1))
            }
            "tram" -> transit("TRAM", args)
            "metro", "métro" -> when (args.firstOrNull()) {
                "1", "m1", "M1" -> transit("M1", args.drop(1))
                "2", "m2", "M2" -> transit("M2", args.drop(1))
                else -> throw ChtiError.UnknownMetro()
            }
            "velo", "vélo", "vlille" -> bike(args)
            "update" -> Command.Update
            "favoris" -> Command.ListFavorites
            "vocal" -> Command.Utterance(args.joinToString(" "))
            "-h", "--help", "help" -> Command.Help
            else -> if (args.isEmpty()) Command.FavoriteOrUnknown(words[0]) else throw ChtiError.UnknownCommand(words[0])
        }
    }

    /** Mots de l'arrêt et options, dans n'importe quel ordre ; « vers » seul fait passer la suite dans la direction */
    private fun transit(line: String, args: List<String>): Command.NextDepartures {
        val stop = mutableListOf<String>()
        var direction = ""
        var count: String? = null
        var afterVers = false
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            when {
                arg == "vers" -> afterVers = true
                arg == "-d" || arg == "--direction" || arg == "--vers" -> {
                    direction = args.getOrNull(++i)
                        ?: throw ChtiError.MissingOptionValue(arg, ChtiError.MissingOptionValue.Expected.DIRECTION)
                }
                arg.startsWith("--direction=") || arg.startsWith("--vers=") -> direction = arg.substringAfter('=')
                arg == "-n" || arg == "--nombre" -> {
                    count = args.getOrNull(++i)
                        ?: throw ChtiError.MissingOptionValue(arg, ChtiError.MissingOptionValue.Expected.COUNT)
                }
                arg.startsWith("--nombre=") -> count = arg.substringAfter('=')
                afterVers -> direction = if (direction.isEmpty()) arg else "$direction $arg"
                else -> stop += arg
            }
            i++
        }
        return Command.NextDepartures(line, stop.joinToString(" "), direction, count?.let(::parseCount))
    }

    /** Entier ≥ 1 ; comme le shell, « 00 » passe et vaut 0 */
    private fun parseCount(value: String): Int {
        if (value.isEmpty() || value.any { it !in '0'..'9' } || value == "0") throw ChtiError.InvalidCount(value)
        return value.trimStart('0').ifEmpty { "0" }.let { if (it.length > 9) Int.MAX_VALUE else it.toInt() }
    }

    private fun bike(args: List<String>): Command.BikeStations {
        var mode = BikeMode.ALL
        val station = mutableListOf<String>()
        for (arg in args) {
            when (arg) {
                "--prendre" -> mode = BikeMode.TAKE
                "--deposer", "--déposer" -> mode = BikeMode.RETURN
                else -> station += arg
            }
        }
        return Command.BikeStations(station.joinToString(" "), mode)
    }
}
