package fr.chtilevia.adapters

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/**
 * Contenu de [url] (http, https ou file), ou null si la ressource est injoignable ou absente.
 * Délai de 30 s, comme le shell.
 */
fun fetch(url: String): ByteArray? = try {
    val connection = URI(url).toURL().openConnection().apply {
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
    }
    if (connection is HttpURLConnection && connection.responseCode !in 200..299) {
        connection.disconnect()
        null
    } else {
        connection.getInputStream().use { it.readBytes() }
    }
} catch (_: IOException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

private const val TIMEOUT_MS = 30_000
