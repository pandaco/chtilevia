package fr.chtilevia.presentation

import fr.chtilevia.application.ChtiError
import fr.chtilevia.application.ChtiError.MissingOptionValue.Expected
import fr.chtilevia.application.ChtiError.UnsupportedCombination.Kind
import fr.chtilevia.application.Notice

/** Aide de la commande, identique à celle du shell */
internal val USAGE = """
    |Usage :
    |  chti bus <ligne> [arrêt] [-d direction] [-n nombre]   ex : chti bus 10 gambetta -d flandres -n 5
    |  chti tram [arrêt] [-d direction] [-n nombre]          ex : chti tram croise laroche -d roubaix
    |  chti metro <1|2> [arrêt] [-d direction] [-n nombre]   ex : chti metro 1 republique -d chu   (théorique)
    |  chti velo <station> [--prendre|--deposer]             ex : chti velo rihour --prendre
    |  chti autour <latitude> <longitude> [-n nombre]        ex : chti autour 50.6366 3.0630
    |  chti <favori>                                         ex : chti travail
    |  chti favoris                                          liste les favoris
    |  chti update                                           re-télécharge le référentiel GTFS
    |  chti --voix <commande>                                réponse en phrases, pour la synthèse vocale
    |  chti --json <commande>                                réponse JSON, pour les programmes (schéma : libs/contract/schema)
    |  chti vocal "<phrase>"                                 ex : chti vocal "prochain tram à croisé laroche vers roubaix"
    |
    |Sans arrêt, la liste des arrêts de la ligne est affichée.
    |-d / --direction / --vers : ne garde que les courses dont la destination contient ce texte.
    |  « vers » s'écrit aussi sans tiret : chti tram croise laroche vers roubaix
    |-n / --nombre : nombre de passages affichés par direction (3 par défaut) ; avec autour, nombre
    |  d'arrêts et de stations V'Lille.
    |--prendre / --deposer : n'affiche que les vélos disponibles / que les places libres.
    |La recherche ignore la casse et les accents, et tolère les fautes (arrêts, directions).
    |
""".trimMargin()

private const val UTTERANCE_EXAMPLE = "Dis par exemple : métro 1 république vers chu, ou : vélo rihour."

/**
 * Message d'une erreur, et la liste de suggestions qui l'accompagne ([hintTitle] : [hint]) ;
 * [hintKey] : clé de cette liste dans les `details` du JSON
 */
internal data class ErrorMessage(
    val text: String,
    val hintTitle: String? = null,
    val hint: List<String> = emptyList(),
    val hintKey: String = "directions",
)

/** Message en français d'une erreur, commun à l'écran, à la voix et au JSON (sauf mention contraire) */
internal fun ChtiError.message(): ErrorMessage = when (this) {
    is ChtiError.TimetableUnavailable -> ErrorMessage("téléchargement du GTFS impossible ($source)")
    is ChtiError.UnknownLine -> ErrorMessage("ligne inconnue : $line")
    is ChtiError.UnknownDirection -> ErrorMessage(
        "aucune direction de la ligne $line ne correspond à « $direction »",
        "directions possibles",
        headsigns,
    )
    is ChtiError.StopNotInDirection -> ErrorMessage(
        "l'arrêt ${stops.joinToString(", ")} n'est pas desservi par la ligne $line en direction de « $direction »",
        "directions qui le desservent",
        headsigns,
    )
    is ChtiError.UnknownStop ->
        ErrorMessage("aucun arrêt de la ligne $line ne correspond à « $query » (relance sans arrêt pour voir la liste)")
    is ChtiError.AmbiguousStop -> ErrorMessage(
        "plusieurs arrêts de la ligne $line ressemblent à « $query »",
        "arrêts approchants",
        suggestions,
        hintKey = "suggestions",
    )
    is ChtiError.MissingStop -> ErrorMessage("Précise un arrêt, par exemple : $example.")
    is ChtiError.MissingStation -> ErrorMessage("précise un nom de station, ex : chti velo rihour")
    is ChtiError.BikeServiceUnreachable -> ErrorMessage("V'Lille injoignable")
    is ChtiError.UnknownStation -> ErrorMessage("Aucune station V'Lille ne correspond à « $query ».")
    is ChtiError.MissingPosition ->
        ErrorMessage("précise une position (latitude longitude), ex : chti autour 50.6366 3.0630")
    is ChtiError.InvalidPosition ->
        ErrorMessage("position invalide : « $text » (latitude et longitude en degrés décimaux, ex : 50.6366 3.0630)")
    is ChtiError.MissingLine -> ErrorMessage("précise une ligne, ex : chti bus 10")
    is ChtiError.UnknownMetro -> ErrorMessage("ligne de métro : 1 ou 2")
    is ChtiError.MissingOptionValue -> when (expected) {
        Expected.DIRECTION -> ErrorMessage("option $option : précise une direction, ex : $option flandres")
        Expected.COUNT -> ErrorMessage("option $option : précise un nombre, ex : $option 5")
    }
    is ChtiError.InvalidCount -> ErrorMessage("nombre de passages invalide : « $value » (entier ≥ 1 attendu)")
    is ChtiError.UnknownCommand -> ErrorMessage("commande ou favori inconnu : $command (voir chti favoris)")
    is ChtiError.UnsupportedCombination -> when (kind) {
        Kind.VOICE_AND_JSON -> ErrorMessage("--voix et --json ne peuvent pas être utilisés ensemble")
        Kind.UTTERANCE_AND_JSON ->
            ErrorMessage("vocal ne s'utilise pas avec --json : l'assistant interprète la phrase lui-même")
    }
    is ChtiError.EmptyUtterance -> ErrorMessage("Je n'ai rien entendu.")
    is ChtiError.NotUnderstood -> ErrorMessage("Je n'ai pas compris « $utterance ». $UTTERANCE_EXAMPLE")
    is ChtiError.UtteranceMissingLine -> ErrorMessage("Quelle ligne ? $UTTERANCE_EXAMPLE")
    is ChtiError.UtteranceUnknownMetro -> ErrorMessage("Quel métro ? Dis par exemple : métro 1 république.")
    is ChtiError.UtteranceMissingStation -> ErrorMessage("Quelle station ? Dis par exemple : vélo rihour.")
    is ChtiError.UtteranceMissingStop -> ErrorMessage("Quel arrêt ? Dis par exemple : $example gambetta.")
}

/** Avertissements du JSON (`warnings`) : code stable et message */
internal fun Notice.warningCode(): String? = when (this) {
    Notice.REALTIME_UNAVAILABLE -> "realtime_unavailable"
    Notice.TIMETABLE_STALE -> "gtfs_stale"
    Notice.BIKE_SHARE_UNAVAILABLE -> "bike_share_unavailable"
    Notice.TIMETABLE_DOWNLOAD -> null
}

internal fun Notice.text(): String = when (this) {
    Notice.REALTIME_UNAVAILABLE -> "flux temps réel indisponible, horaires théoriques uniquement"
    Notice.TIMETABLE_STALE -> "GTFS non mis à jour, utilisation du cache existant"
    Notice.TIMETABLE_DOWNLOAD -> "Mise à jour du référentiel GTFS…"
    Notice.BIKE_SHARE_UNAVAILABLE -> "V'Lille injoignable, stations non affichées"
}
