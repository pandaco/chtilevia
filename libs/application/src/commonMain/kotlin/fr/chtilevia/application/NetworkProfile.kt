package fr.chtilevia.application

import kotlinx.datetime.TimeZone

/**
 * Ce qui distingue un réseau d'un autre : sources de données, fuseau, particularités. Le code reste
 * générique GTFS ; Lille (Ilévia) n'est qu'un profil.
 */
data class NetworkProfile(
    val timeZone: TimeZone,
    val gtfsUrl: String,
    val realtimeUrl: String,
    /** Racine GBFS (`…/station_information.json`, `…/station_status.json`) */
    val bikeShareUrl: String,
    /** Lignes absentes du flux temps réel : inutile de le télécharger pour elles */
    val linesWithoutRealtime: Set<String>,
) {
    companion object {
        val ILEVIA = NetworkProfile(
            timeZone = TimeZone.of("Europe/Paris"),
            gtfsUrl = "https://media.ilevia.fr/opendata/gtfs.zip",
            realtimeUrl = "https://proxy.transport.data.gouv.fr/resource/ilevia-lille-gtfs-rt",
            bikeShareUrl = "https://media.ilevia.fr/opendata",
            // Pas de temps réel métro en open data
            linesWithoutRealtime = setOf("M1", "M2"),
        )
    }
}
