package fr.chtipasse.android

import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import org.json.JSONObject
import java.util.Locale

/**
 * Commandes chti des AppFunctions, construites comme celles du serveur MCP
 * (apps/mcp/src/adapters/command-gateway.ts) : un texte libre de l'agent n'est jamais une option.
 */
object ChtiCommands {
    private val TRANSIT = setOf("bus", "tram", "metro")

    fun departures(mode: String, line: String?, stop: String, direction: String?, count: Int): List<String> {
        // Arrêt et direction passés d'un seul tenant : un mot comme « vers » dans un nom reste un nom
        val args = lineArgs(mode, line) + text("arret", stop)
        return args + directionArgs(direction) + countArgs(count, max = 20)
    }

    fun stops(mode: String, line: String?, direction: String?): List<String> = lineArgs(mode, line) + directionArgs(direction)

    fun bikeStations(station: String, mode: String?): List<String> = listOf("velo", text("station", station)) + when (mode?.trim()?.lowercase(Locale.ROOT)) {
        null, "", "tout" -> emptyList()
        "prendre" -> listOf("--prendre")
        "deposer", "déposer" -> listOf("--deposer")
        else -> throw AppFunctionInvalidArgumentException("mode : « tout », « prendre » ou « deposer », pas « $mode »")
    }

    fun nearby(latitude: Double, longitude: Double, count: Int, mode: String?): List<String> {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            throw AppFunctionInvalidArgumentException("position invalide : latitude entre -90 et 90, longitude entre -180 et 180")
        }
        val filter = mode?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }?.also {
            if (it !in TRANSIT + "velo") throw AppFunctionInvalidArgumentException("mode : « bus », « tram », « metro » ou « velo », pas « $mode »")
        }
        // Notation décimale fixe : jamais d'exposant (1e-7), que chti refuserait
        val position = listOf(String.format(Locale.ROOT, "%.6f", latitude), String.format(Locale.ROOT, "%.6f", longitude))
        return listOf("autour") + listOfNotNull(filter) + position + countArgs(count, max = 10)
    }

    fun favorites(): List<String> = listOf("favoris")

    /** Erreur du contrat (`"type": "error"`) en exception AppFunctions : message, code et détails pour l'agent */
    fun exception(error: JSONObject): AppFunctionException {
        val code = error.optString("code")
        val details = error.optJSONObject("details")?.takeIf { it.length() > 0 }
        val message = error.optString("message") + " [$code]" + (details?.let { " $it" } ?: "")
        return when {
            code.startsWith("unknown_") || code == "ambiguous_stop" || code == "stop_not_in_direction" ->
                AppFunctionElementNotFoundException(message)
            code.startsWith("missing_") || code.startsWith("invalid_") -> AppFunctionInvalidArgumentException(message)
            else -> AppFunctionAppUnknownException(message)
        }
    }

    private fun lineArgs(mode: String, line: String?): List<String> = when (mode.trim().lowercase(Locale.ROOT)) {
        "tram" -> listOf("tram")
        "bus" -> listOf("bus", text("ligne", line ?: throw AppFunctionInvalidArgumentException("précise une ligne, ex : 10")))
        "metro", "métro" -> listOf("metro", text("ligne", line ?: throw AppFunctionInvalidArgumentException("précise la ligne de métro : 1 ou 2")))
        else -> throw AppFunctionInvalidArgumentException("mode : « bus », « tram » ou « metro », pas « $mode »")
    }

    private fun directionArgs(direction: String?): List<String> =
        direction?.trim()?.takeIf { it.isNotEmpty() }?.let { listOf("--vers=${text("direction", it)}") }.orEmpty()

    private fun countArgs(count: Int, max: Int): List<String> = when (count) {
        0 -> emptyList()
        in 1..max -> listOf("--nombre=$count")
        else -> throw AppFunctionInvalidArgumentException("nombre : de 1 à $max (0 pour la valeur par défaut)")
    }

    /** Texte libre non vide, qui ne peut pas passer pour une option de chti */
    private fun text(name: String, value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("-")) throw AppFunctionInvalidArgumentException("$name : un nom, qui ne commence pas par « - »")
        return trimmed
    }
}
