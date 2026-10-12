package fr.chtilevia.application

import fr.chtilevia.domain.BikeStation
import fr.chtilevia.domain.Line
import fr.chtilevia.domain.Stop
import fr.chtilevia.domain.StopTime
import fr.chtilevia.domain.Trip
import fr.chtilevia.domain.TripUpdate
import kotlinx.datetime.LocalDate

// Ports sortants : ce dont les cas d'usage ont besoin, sans savoir d'où cela vient.

/** Référentiel théorique (GTFS statique) */
interface TimetableRepository {
    /** Lignes, dans l'ordre du référentiel */
    fun lines(): List<Line>

    /** Courses de la ligne, dans l'ordre du référentiel */
    fun trips(lineId: String): List<Trip>

    fun stops(): Map<String, Stop>

    /** Passages de chaque course de la ligne, dans l'ordre du référentiel */
    fun stopTimes(lineId: String): Map<String, List<StopTime>>

    /** Lignes (identifiants) qui desservent chacun des quais [stopIds] ; un quai sans ligne est absent */
    fun linesServing(stopIds: Set<String>): Map<String, Set<String>>

    /** Pour chaque service, celles des [dates] où il circule */
    fun activeDates(dates: Collection<LocalDate>): Map<String, Set<LocalDate>>
}

/** Mise à jour du référentiel théorique */
interface TimetableUpdater {
    /**
     * Télécharge et prépare le référentiel s'il est absent ou périmé (toujours si [force]).
     * Signale le téléchargement et l'usage d'un cache périmé par [notices].
     * Renvoie l'emplacement du référentiel ; lève [ChtiError.TimetableUnavailable] sans référentiel.
     */
    fun refresh(force: Boolean, notices: Notices): String
}

/** Flux temps réel (GTFS-RT) */
fun interface RealtimeFeed {
    /** Instantané du flux, ou null s'il est injoignable */
    fun fetch(): RealtimeSnapshot?
}

fun interface RealtimeSnapshot {
    /** Mises à jour des courses [tripIds] présentes dans l'instantané */
    fun tripUpdates(tripIds: Set<String>): Map<String, TripUpdate>
}

/** Vélos en libre service (GBFS) */
fun interface BikeShareFeed {
    /** Stations et leur état, dans l'ordre du flux, ou null si le service est injoignable */
    fun stations(): List<BikeStation>?
}

/** Fichier des favoris (format `nom: commande`, partagé avec le shell) */
interface FavoritesStore {
    /** Emplacement montré à l'utilisateur */
    val location: String

    /** Contenu, ou null si le fichier n'existe pas */
    fun read(): String?
}

/** Horloge, en epoch secondes */
fun interface Clock {
    fun now(): Long
}

/** Ce qui se passe pendant une requête et qui mérite d'être signalé, avant même la réponse */
enum class Notice {
    /** Téléchargement du référentiel en cours (long) */
    TIMETABLE_DOWNLOAD,

    /** Référentiel non mis à jour, le cache existant est utilisé */
    TIMETABLE_STALE,

    /** Flux temps réel injoignable : horaires théoriques uniquement */
    REALTIME_UNAVAILABLE,

    /** V'Lille injoignable : pas de stations autour d'une position */
    BIKE_SHARE_UNAVAILABLE,
}

fun interface Notices {
    fun notice(notice: Notice)

    companion object {
        val NONE = Notices {}
    }
}
