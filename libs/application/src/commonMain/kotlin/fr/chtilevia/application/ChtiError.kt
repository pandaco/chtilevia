package fr.chtilevia.application

/**
 * Erreurs métier, avec le code stable du contrat (`libs/contract/schema`). Les messages en
 * français sont l'affaire de la présentation : une même erreur se dit autrement à l'écran et à l'oral.
 */
sealed class ChtiError(val code: String) : Exception(code) {
    // Référentiel et lignes
    class TimetableUnavailable(val source: String) : ChtiError("gtfs_unavailable")
    class UnknownLine(val line: String) : ChtiError("unknown_line")
    class UnknownDirection(val line: String, val direction: String, val headsigns: List<String>) :
        ChtiError("unknown_direction")
    class StopNotInDirection(val line: String, val stops: List<String>, val direction: String, val headsigns: List<String>) :
        ChtiError("stop_not_in_direction")
    class UnknownStop(val line: String, val query: String) : ChtiError("unknown_stop")

    /** Plusieurs arrêts ressemblent autant à [query] : [suggestions] à faire choisir */
    class AmbiguousStop(val line: String, val query: String, val suggestions: List<String>) : ChtiError("ambiguous_stop")

    /** Demande à l'oral sans arrêt : [example] = « bus 10 gare lille flandres » */
    class MissingStop(val example: String) : ChtiError("missing_stop")

    // V'Lille
    class MissingStation : ChtiError("missing_station")
    class BikeServiceUnreachable : ChtiError("bike_service_unreachable")
    class UnknownStation(val query: String) : ChtiError("unknown_station")

    // Position (autour)
    class MissingPosition : ChtiError("missing_position")

    /** [text] : les mots reçus pour la position */
    class InvalidPosition(val text: String) : ChtiError("invalid_position")

    // Grammaire des commandes (CommandLine)
    class MissingLine : ChtiError("missing_line")
    class UnknownMetro : ChtiError("unknown_metro")
    class MissingOptionValue(val option: String, val expected: Expected) : ChtiError("missing_option_value") {
        enum class Expected { DIRECTION, COUNT }
    }
    class InvalidCount(val value: String) : ChtiError("invalid_count")
    class UnknownCommand(val command: String) : ChtiError("unknown_command")
    class UnsupportedCombination(val kind: Kind) : ChtiError("unsupported_combination") {
        enum class Kind { VOICE_AND_JSON, UTTERANCE_AND_JSON }
    }

    // Phrase libre (InterpretUtterance)
    class EmptyUtterance : ChtiError("empty_phrase")
    class NotUnderstood(val utterance: String) : ChtiError("not_understood")
    class UtteranceMissingLine : ChtiError("missing_line")
    class UtteranceUnknownMetro : ChtiError("unknown_metro")
    class UtteranceMissingStation : ChtiError("missing_station")

    /** [example] = début de commande à compléter, ex. « bus 10 » */
    class UtteranceMissingStop(val example: String) : ChtiError("missing_stop")
}
