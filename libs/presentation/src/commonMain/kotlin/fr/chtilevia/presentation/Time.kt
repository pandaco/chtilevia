package fr.chtilevia.presentation

import fr.chtilevia.domain.localDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.offsetAt
import kotlin.math.abs
import kotlin.time.Instant

private fun Int.pad(length: Int = 2) = toString().padStart(length, '0')

private fun LocalDateTime.isoSeconds() =
    "${year.pad(4)}-${month.number.pad()}-${day.pad()}T${hour.pad()}:${minute.pad()}:${second.pad()}"

/** Heure locale « HH:MM » */
internal fun hhmm(epochSeconds: Long, zone: TimeZone): String =
    localDateTime(epochSeconds, zone).let { "${it.hour.pad()}:${it.minute.pad()}" }

/** ISO 8601 à la seconde avec le décalage du fuseau : 2026-10-10T12:46:00+02:00 */
internal fun isoLocal(epochSeconds: Long, zone: TimeZone): String {
    val offset = zone.offsetAt(Instant.fromEpochSeconds(epochSeconds)).totalSeconds
    val sign = if (offset < 0) '-' else '+'
    val minutes = abs(offset) / 60
    return localDateTime(epochSeconds, zone).isoSeconds() + "$sign${(minutes / 60).pad()}:${(minutes % 60).pad()}"
}

/** ISO 8601 en UTC : 2026-10-10T21:56:18Z */
internal fun isoUtc(epochSeconds: Long): String = localDateTime(epochSeconds, TimeZone.UTC).isoSeconds() + "Z"

/** Minutes jusqu'à [time], arrondies au plus proche (à égalité, vers le pair, comme le shell) */
internal fun minutesUntil(time: Long, now: Long): Long {
    val diff = time - now
    val quotient = diff.floorDiv(60L)
    val remainder = diff - quotient * 60
    return when {
        remainder < 30 -> quotient
        remainder > 30 -> quotient + 1
        quotient % 2 == 0L -> quotient
        else -> quotient + 1
    }
}
