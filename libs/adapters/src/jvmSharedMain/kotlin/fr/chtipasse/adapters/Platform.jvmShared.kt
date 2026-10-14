package fr.chtipasse.adapters

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.DataFormatException
import java.util.zip.Inflater
import kotlin.io.path.createTempDirectory

actual fun fetch(url: String): ByteArray? = try {
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

internal actual val LocalFiles: LocalFileSystem = object : LocalFileSystem {
    override fun systemTemporaryDirectory(): String = System.getProperty("java.io.tmpdir").orEmpty().trimEnd('/')

    override fun isFile(path: String): Boolean = File(path).isFile

    override fun exists(path: String): Boolean = File(path).exists()

    override fun lastModified(path: String): Long? = File(path).takeIf { it.exists() }?.lastModified()

    override fun touch(path: String, millis: Long) {
        File(path).writeText("")
        File(path).setLastModified(millis)
    }

    override fun mkdirs(path: String) {
        File(path).mkdirs()
    }

    override fun createTempDirectory(parent: String, prefix: String): String =
        createTempDirectory(File(parent).toPath(), prefix).toFile().path

    override fun rename(from: String, to: String): Boolean = File(from).renameTo(File(to))

    override fun deleteRecursively(path: String) {
        File(path).deleteRecursively()
    }

    override fun readText(path: String): String? = File(path).takeIf { it.isFile }?.readText()

    override fun writeBytes(path: String, bytes: ByteArray) = File(path).writeBytes(bytes)

    override fun <T> useLines(path: String, block: (Sequence<String>) -> T): T? =
        File(path).takeIf { it.isFile }?.bufferedReader()?.useLines(block)

    override fun openWriter(path: String): TextWriter {
        val writer = File(path).bufferedWriter()
        return object : TextWriter {
            override fun write(text: String) = writer.write(text)

            override fun close() = writer.close()
        }
    }
}

internal actual fun inflateRaw(data: ByteArray, size: Int): ByteArray {
    val inflater = Inflater(true)
    try {
        // Sans en-tête zlib, l'Inflater de Java demande un octet de plus en fin d'entrée
        inflater.setInput(data + 0)
        val out = ByteArray(size)
        var done = 0
        while (done < size) {
            val n = inflater.inflate(out, done, size - done)
            if (n == 0 && (inflater.finished() || inflater.needsInput() || inflater.needsDictionary())) break
            done += n
        }
        if (done != size) throw IOException("archive tronquée")
        return out
    } catch (e: DataFormatException) {
        throw IOException(e)
    } finally {
        inflater.end()
    }
}
