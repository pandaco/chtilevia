package fr.chtipasse.adapters

import fr.chtipasse.adapters.bikeshare.Gbfs
import fr.chtipasse.adapters.gtfs.csvRows
import fr.chtipasse.adapters.realtime.GtfsRealtime
import fr.chtipasse.domain.BikeStation
import fr.chtipasse.domain.StopTimeUpdate
import fr.chtipasse.domain.TripUpdate
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptersTest {
    @Test
    fun csvHandlesBomQuotesAndLineBreaks() {
        val text = "﻿stop_id,stop_name,stop_desc\r\n1,\"Gare, quai \"\"A\"\"\",\"sur\ndeux lignes\"\r\n\r\n2,Marais\r\n"
        assertEquals(
            listOf(
                mapOf("stop_id" to "1", "stop_name" to "Gare, quai \"A\"", "stop_desc" to "sur\ndeux lignes"),
                mapOf("stop_id" to "2", "stop_name" to "Marais", "stop_desc" to ""),
            ),
            csvRows(text.lineSequence()).toList(),
        )
    }

    @Test
    fun realtimeDecoderReadsTripUpdates() {
        // FeedMessage { entity { trip_update { trip { trip_id: "t1" } stop_time_update { stop_id: "S"
        //   departure { delay: -30 time: 1791629460 } } } } }, codé à la main
        val event = field(1, varint(-30L)) + field(2, varint(1791629460L))
        val stop = bytes(3, event) + bytes(4, "S".encodeToByteArray())
        val tripUpdate = bytes(1, bytes(1, "t1".encodeToByteArray())) + bytes(2, stop)
        val feed = bytes(2, bytes(3, tripUpdate))
        assertEquals(
            mapOf("t1" to TripUpdate(false, mapOf("S" to StopTimeUpdate(time = 1791629460L, delay = -30L)))),
            GtfsRealtime.tripUpdates(feed, setOf("t1")),
        )
        assertEquals(emptyMap(), GtfsRealtime.tripUpdates(feed, setOf("autre")))
    }

    @Test
    fun gbfsJoinsInformationAndStatus() {
        val information = """{"data":{"stations":[{"station_id":"1","name":"Rihour","capacity":32},{"station_id":"2","name":"Gare"}]}}"""
        val status = """{"data":{"stations":[{"station_id":"1","num_bikes_available":3,"num_docks_available":29,
            "is_installed":true,"is_renting":true,"is_returning":false,"last_reported":1791669372}]}}"""
        val stations = Gbfs.stations(information, status)
        assertEquals(listOf("Rihour", "Gare"), stations.map { it.name })
        assertEquals(BikeStation.Availability.OK, stations[0].availability)
        assertEquals(3, stations[0].status?.bikes)
        assertEquals(1791669372L, stations[0].status?.lastReported)
        assertEquals(BikeStation.Availability.UNKNOWN, stations[1].availability)
    }

    private fun varint(value: Long): ByteArray {
        val out = mutableListOf<Byte>()
        var v = value
        do {
            val byte = (v and 0x7F).toInt()
            v = v ushr 7
            out += (if (v != 0L) byte or 0x80 else byte).toByte()
        } while (v != 0L)
        return out.toByteArray()
    }

    private fun field(number: Int, varint: ByteArray) = varint((number shl 3).toLong()) + varint

    private fun bytes(number: Int, content: ByteArray) = varint(((number shl 3) or 2).toLong()) + varint(content.size.toLong()) + content
}
