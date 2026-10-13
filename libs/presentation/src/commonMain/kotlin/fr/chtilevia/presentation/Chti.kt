package fr.chtilevia.presentation

import fr.chtilevia.application.ChtiError
import fr.chtilevia.application.Command
import fr.chtilevia.application.CommandLine
import fr.chtilevia.application.Departures
import fr.chtilevia.application.DeparturesQuery
import fr.chtilevia.application.FavoritesFile
import fr.chtilevia.application.FindFavorite
import fr.chtilevia.application.FindNearby
import fr.chtilevia.application.GetBikeStations
import fr.chtilevia.application.GetDepartures
import fr.chtilevia.application.InterpretUtterance
import fr.chtilevia.application.ListFavorites
import fr.chtilevia.application.Notice
import fr.chtilevia.application.Notices
import fr.chtilevia.application.StopList
import fr.chtilevia.application.UpdateTimetable
import kotlinx.datetime.TimeZone

/** Sorties d'une commande : [out] pour la réponse, [err] pour les messages (textes déjà terminés par \n) */
interface Console {
    fun out(text: String)

    fun err(text: String)
}

/** Cas d'usage dont la commande a besoin, assemblés par la racine de composition */
class ChtiServices(
    val departures: GetDepartures,
    val bikeStations: GetBikeStations,
    val nearby: FindNearby,
    val listFavorites: ListFavorites,
    val findFavorite: FindFavorite,
    val interpretUtterance: InterpretUtterance,
    val updateTimetable: UpdateTimetable,
)

/**
 * La commande `chti` (≡ `apps/shell/chti`, vérifié par `libs/contract`) : mêmes arguments, mêmes
 * sorties à l'écran, à l'oral (`--voix`) et en JSON (`--json`), mêmes codes retour.
 * En plus, propre au cœur Kotlin : `--court`, réponse écrite compacte à pictos pour les notifications
 * et le widget de l'appli Android ([BriefFormatter]).
 */
class Chti(private val services: ChtiServices, zone: TimeZone) {
    private val text = TextFormatter(zone)
    private val voice = VoiceFormatter(zone)
    private val json = JsonFormatter(zone)
    private val brief = BriefFormatter(zone)

    private enum class Mode {
        TEXT, VOICE, JSON, BRIEF;

        /** Réponses en phrases courtes (oral, ou écrit compact) : moins de passages, erreurs en phrases */
        val terse get() = this == VOICE || this == BRIEF
    }

    /** Réponse d'une commande : code retour, et document à imprimer en mode JSON */
    private class Reply(val exit: Int, val document: Json? = null)

    /** Lance la commande [args] ; renvoie le code retour (0 : succès, 1 : erreur) */
    fun run(args: List<String>, console: Console): Int = run(args, console, voice = false, json = false, brief = false, depth = 0)

    private fun run(args: List<String>, console: Console, voice: Boolean, json: Boolean, brief: Boolean, depth: Int): Int {
        val reply = respond(args, console, voice, json, brief, depth)
        reply.document?.let { console.out(it.render()) }
        return reply.exit
    }

    /** `--voix`, `--json` et `--court` peuvent se placer n'importe où ; elles s'ajoutent à celles héritées d'un favori */
    private fun respond(args: List<String>, console: Console, voice: Boolean, json: Boolean, brief: Boolean, depth: Int): Reply {
        val words = args.filter { it != "--voix" && it != "--json" && it != "--court" }
        val mode = when {
            json || "--json" in args -> Mode.JSON
            voice || "--voix" in args -> Mode.VOICE
            brief || "--court" in args -> Mode.BRIEF
            else -> Mode.TEXT
        }
        if (mode == Mode.JSON && (voice || "--voix" in args)) {
            return Reply(1, this.json.error(ChtiError.UnsupportedCombination(ChtiError.UnsupportedCombination.Kind.VOICE_AND_JSON)))
        }
        if (words.isEmpty()) {
            console.out(USAGE)
            return Reply(1)
        }
        return try {
            execute(CommandLine.parse(words), mode, console, depth)
        } catch (error: ChtiError) {
            fail(error, mode, console)
        }
    }

    private fun execute(command: Command, mode: Mode, console: Console, depth: Int): Reply = when (command) {
        is Command.NextDepartures -> {
            val count = command.count ?: if (mode.terse) 2 else 3
            val query = DeparturesQuery(command.line, command.stop, command.direction, count)
            when (val result = services.departures.execute(query, notices(mode, console))) {
                is Departures -> output(mode, console, { text.departures(result) }, { voice.departures(result) }, { brief.departures(result) }) {
                    json.departures(result)
                }
                is StopList -> {
                    if (mode.terse) {
                        val example = listOfNotNull(result.line.label, result.exampleStop).joinToString(" ").lowercase()
                        throw ChtiError.MissingStop(example)
                    }
                    output(mode, console, { text.stopList(result) }, { text.stopList(result) }, { text.stopList(result) }) {
                        json.stopList(result)
                    }
                }
            }
        }
        is Command.BikeStations -> {
            val result = services.bikeStations.execute(command.station, command.mode)
            output(mode, console, { text.bikeStations(result) }, { voice.bikeStations(result) }, { brief.bikeStations(result) }) {
                json.bikeStations(result)
            }
        }
        is Command.Nearby -> {
            val count = command.count ?: if (mode.terse) 2 else 3
            val result = services.nearby.execute(command.position, count, command.mode, notices(mode, console))
            output(mode, console, { text.nearby(result) }, { voice.nearby(result) }, { brief.nearby(result) }) { json.nearby(result) }
        }
        Command.ListFavorites -> {
            val listing = services.listFavorites.execute()
            output(mode, console, { text.favorites(listing) }, { text.favorites(listing) }, { text.favorites(listing) }) {
                json.favorites(listing)
            }
        }
        Command.Update -> {
            val location = services.updateTimetable.execute(notices(mode, console))
            val done = { "Référentiel GTFS à jour dans $location\n" }
            output(mode, console, done, done, done) { json.update(location) }
        }
        Command.Help -> {
            console.out(USAGE)
            Reply(0)
        }
        is Command.Utterance -> {
            if (mode == Mode.JSON) throw ChtiError.UnsupportedCombination(ChtiError.UnsupportedCombination.Kind.UTTERANCE_AND_JSON)
            // Phrase reconnue à la voix : réponse à l'oral, erreurs comprises
            val words = try {
                services.interpretUtterance.execute(command.text)
            } catch (error: ChtiError) {
                return fail(error, Mode.VOICE, console)
            }
            Reply(run(words, console, voice = true, json = false, brief = false, depth = depth + 1))
        }
        is Command.FavoriteOrUnknown -> favorite(command.name, mode, console, depth)
    }

    /** Lance chaque commande du favori ; échoue (1) si l'une échoue */
    private fun favorite(name: String, mode: Mode, console: Console, depth: Int): Reply {
        val commands = services.findFavorite.execute(name)
        // Un favori qui se lance lui-même (directement ou non) finirait par épuiser la pile
        if (commands.isEmpty() || depth >= MAX_DEPTH) throw ChtiError.UnknownCommand(name)
        var exit = 0
        if (mode == Mode.JSON) {
            val results = commands.mapNotNull { command ->
                val reply = respond(FavoritesFile.words(command), console, voice = false, json = true, brief = false, depth = depth + 1)
                if (reply.exit != 0) exit = 1
                reply.document
            }
            return Reply(exit, json.favorite(name, results))
        }
        for (command in commands) {
            val words = FavoritesFile.words(command)
            if (run(words, console, mode == Mode.VOICE, json = false, brief = mode == Mode.BRIEF, depth = depth + 1) != 0) exit = 1
        }
        return Reply(exit)
    }

    private fun output(
        mode: Mode,
        console: Console,
        text: () -> String,
        voice: () -> String,
        brief: () -> String,
        json: () -> Json,
    ): Reply = when (mode) {
        Mode.JSON -> Reply(0, json())
        Mode.VOICE -> Reply(0).also { console.out(voice()) }
        Mode.BRIEF -> Reply(0).also { console.out(brief()) }
        Mode.TEXT -> Reply(0).also { console.out(text()) }
    }

    /** Messages pendant le traitement : l'écran dit tout, la voix l'essentiel, le JSON les met dans la réponse */
    private fun notices(mode: Mode, console: Console) = Notices { notice ->
        val shown = when (notice) {
            Notice.TIMETABLE_DOWNLOAD -> true
            Notice.TIMETABLE_STALE -> mode != Mode.JSON
            Notice.REALTIME_UNAVAILABLE, Notice.BIKE_SHARE_UNAVAILABLE -> mode == Mode.TEXT
        }
        if (shown) console.err(if (notice == Notice.TIMETABLE_DOWNLOAD) "${notice.text()}\n" else "chti : ${notice.text()}\n")
    }

    private fun fail(error: ChtiError, mode: Mode, console: Console): Reply {
        if (mode == Mode.JSON) return Reply(1, json.error(error))
        val message = error.message()
        when {
            // Messages déjà rédigés comme des phrases complètes, identiques à l'écran et à l'oral
            error is ChtiError.UnknownStation -> console.err("${message.text}\n")
            error is ChtiError.UnknownCommand && mode.terse ->
                console.err("Je ne connais pas la commande ou le favori « ${error.command} ».\n")
            error is ChtiError.UnknownCommand -> console.err("chti : ${message.text}\n\n$USAGE")
            mode.terse -> {
                val hint = message.hintTitle?.let { ". ${it.capitalized()} : ${message.hint.joinToString(", ")}." }.orEmpty()
                console.err((message.text + hint).capitalized() + "\n")
            }
            else -> {
                val hint = message.hintTitle?.let { title -> "\n$title :" + message.hint.joinToString("") { "\n  • $it" } }.orEmpty()
                console.err("chti : ${message.text}$hint\n")
            }
        }
        return Reply(1)
    }

    private fun String.capitalized() = replaceFirstChar { it.uppercaseChar() }

    private companion object {
        const val MAX_DEPTH = 16
    }
}
