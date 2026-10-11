package fr.chtilevia.adapters.realtime

import fr.chtilevia.application.RealtimeFeed
import fr.chtilevia.application.RealtimeSnapshot
import fr.chtilevia.domain.StopTimeUpdate
import fr.chtilevia.domain.TripUpdate

/** Flux GTFS-RT obtenu par [fetch] (null : injoignable) */
class GtfsRealtimeFeed(private val fetch: () -> ByteArray?) : RealtimeFeed {
    override fun fetch(): RealtimeSnapshot? = fetch.invoke()?.let { bytes ->
        RealtimeSnapshot { tripIds -> GtfsRealtime.tripUpdates(bytes, tripIds) }
    }
}

/**
 * Décodeur protobuf minimal du message GTFS-RT `FeedMessage`, limité aux champs utiles (même
 * logique que le shell) : pas de code généré ni de bibliothèque protobuf.
 */
object GtfsRealtime {
    // Numéros de champs de gtfs-realtime.proto
    private const val FEED_ENTITY = 2
    private const val ENTITY_TRIP_UPDATE = 3
    private const val TRIP_UPDATE_TRIP = 1
    private const val TRIP_UPDATE_STOP_TIME_UPDATE = 2
    private const val TRIP_ID = 1
    private const val TRIP_SCHEDULE_RELATIONSHIP = 4
    private const val TRIP_CANCELED = 3L
    private const val STOP_ARRIVAL = 2
    private const val STOP_DEPARTURE = 3
    private const val STOP_ID = 4
    private const val STOP_SCHEDULE_RELATIONSHIP = 5
    private const val STOP_SKIPPED = 1L
    private const val EVENT_DELAY = 1
    private const val EVENT_TIME = 2

    /** Mises à jour des courses [tripIds] ; pour une course présente plusieurs fois, la dernière l'emporte */
    fun tripUpdates(feed: ByteArray, tripIds: Set<String>): Map<String, TripUpdate> {
        val trips = LinkedHashMap<String, TripUpdate>()
        for (entity in fields(feed)[FEED_ENTITY].orEmpty()) {
            for (tripUpdateBytes in fields(entity.bytes())[ENTITY_TRIP_UPDATE].orEmpty()) {
                val tripUpdate = fields(tripUpdateBytes.bytes())
                val trip = tripUpdate[TRIP_UPDATE_TRIP]?.first()?.let { fields(it.bytes()) }.orEmpty()
                val tripId = trip[TRIP_ID]?.first()?.bytes()?.decodeToString().orEmpty()
                if (tripId !in tripIds) continue
                val stops = LinkedHashMap<String, StopTimeUpdate>()
                for (stopBytes in tripUpdate[TRIP_UPDATE_STOP_TIME_UPDATE].orEmpty()) {
                    val stop = fields(stopBytes.bytes())
                    val stopId = stop[STOP_ID]?.first()?.bytes() ?: continue
                    if ((stop[STOP_SCHEDULE_RELATIONSHIP]?.first()?.long() ?: 0L) == STOP_SKIPPED) continue
                    // Départ de préférence, sinon arrivée
                    val event = (stop[STOP_DEPARTURE] ?: stop[STOP_ARRIVAL])?.first()?.let { fields(it.bytes()) }.orEmpty()
                    stops[stopId.decodeToString()] = StopTimeUpdate(
                        time = event[EVENT_TIME]?.first()?.long(),
                        delay = event[EVENT_DELAY]?.first()?.long(),
                    )
                }
                val cancelled = (trip[TRIP_SCHEDULE_RELATIONSHIP]?.first()?.long() ?: 0L) == TRIP_CANCELED
                trips[tripId] = TripUpdate(cancelled, stops)
            }
        }
        return trips
    }

    /** Valeur d'un champ : entier (varint, fixe) ou octets (length-delimited) */
    private class Value(private val number: Long?, private val data: ByteArray?) {
        fun long(): Long = number ?: error("champ protobuf non entier")
        fun bytes(): ByteArray = data ?: error("champ protobuf non délimité")
    }

    /** Champs d'un message, par numéro, dans l'ordre */
    private fun fields(message: ByteArray): Map<Int, List<Value>> {
        val out = HashMap<Int, MutableList<Value>>()
        var i = 0
        fun varint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                val byte = message[i++].toInt() and 0xFF
                result = result or ((byte and 0x7F).toLong() shl shift)
                shift += 7
                if (byte < 0x80) return result
            }
        }
        while (i < message.size) {
            val key = varint()
            val field = (key ushr 3).toInt()
            val value = when (val wire = (key and 7).toInt()) {
                0 -> Value(varint(), null)
                2 -> {
                    val length = varint().toInt()
                    Value(null, message.copyOfRange(i, i + length)).also { i += length }
                }
                1 -> Value(littleEndian(message, i, 8), null).also { i += 8 }
                5 -> Value(littleEndian(message, i, 4), null).also { i += 4 }
                else -> error("type protobuf $wire non géré")
            }
            out.getOrPut(field) { mutableListOf() }.add(value)
        }
        return out
    }

    private fun littleEndian(bytes: ByteArray, start: Int, size: Int): Long =
        (0 until size).fold(0L) { acc, k -> acc or ((bytes[start + k].toLong() and 0xFF) shl (8 * k)) }
}
