package fr.chtilevia.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Origine des horaires GTFS d'un jour de service : « midi moins 12 h », en epoch secondes.
 * Ce n'est pas minuit les jours de changement d'heure, ce qui garde les horaires justes.
 */
fun serviceDayOrigin(date: LocalDate, zone: TimeZone): Long =
    LocalDateTime(date, LocalTime(12, 0)).toInstant(zone).epochSeconds - 12 * 3600

/**
 * Jours de service à considérer à l'instant [now] : la veille (ses courses roulent après minuit,
 * horaires > 24:00:00), le jour même, et le lendemain quand il ne reste plus assez de passages ce soir.
 */
fun serviceDates(now: Long, zone: TimeZone): List<LocalDate> {
    val today = localDate(now, zone)
    return listOf(today.minus(1, DateTimeUnit.DAY), today, today.plus(1, DateTimeUnit.DAY))
}

/** Date locale de l'instant [epochSeconds] dans le fuseau du réseau */
fun localDate(epochSeconds: Long, zone: TimeZone): LocalDate = localDateTime(epochSeconds, zone).date

fun localDateTime(epochSeconds: Long, zone: TimeZone): LocalDateTime =
    Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(zone)
