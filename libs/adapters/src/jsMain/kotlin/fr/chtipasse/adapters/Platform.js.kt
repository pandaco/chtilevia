package fr.chtipasse.adapters

import org.khronos.webgl.Int8Array

// Sous Node, les ports sont synchrones comme ailleurs : fichiers par les appels *Sync de node:fs,
// réseau par un processus node enfant attendu (execFileSync), sans curl ni bibliothèque.

@JsModule("node:fs")
@JsNonModule
private external object Fs {
    fun existsSync(path: String): Boolean

    fun statSync(path: String, options: dynamic = definedExternally): dynamic

    fun writeFileSync(path: String, data: dynamic)

    fun utimesSync(path: String, atime: Double, mtime: Double)

    fun mkdirSync(path: String, options: dynamic)

    fun mkdtempSync(prefix: String): String

    fun renameSync(from: String, to: String)

    fun rmSync(path: String, options: dynamic)

    fun readFileSync(path: String, options: dynamic = definedExternally): dynamic

    fun openSync(path: String, flags: String): Int

    fun writeSync(fd: Int, text: String): Int

    fun closeSync(fd: Int)
}

@JsModule("node:os")
@JsNonModule
private external object Os {
    fun tmpdir(): String
}

@JsModule("node:zlib")
@JsNonModule
private external object Zlib {
    fun inflateRawSync(data: dynamic): dynamic
}

@JsModule("node:child_process")
@JsNonModule
private external object ChildProcess {
    fun execFileSync(file: String, args: Array<String>, options: dynamic): dynamic
}

private external val process: dynamic

private external object Buffer {
    fun from(data: dynamic, offset: Int, length: Int): dynamic
}

/** Octets d'un Buffer Node, sans copie */
private fun bytesOf(buffer: dynamic): ByteArray =
    Int8Array(buffer.buffer, buffer.byteOffset as Int, buffer.length as Int).unsafeCast<ByteArray>()

/** Buffer Node sur des octets Kotlin, sans copie */
private fun bufferOf(bytes: ByteArray): dynamic {
    val array = bytes.unsafeCast<Int8Array>()
    return Buffer.from(array.buffer, array.byteOffset, array.length)
}

/** Téléchargement par un node enfant : sortie standard = contenu, code ≠ 0 = échec */
private const val DOWNLOAD_SCRIPT = """
const response = await fetch(process.argv[1], { signal: AbortSignal.timeout(30000) });
if (!response.ok) process.exit(2);
process.stdout.write(Buffer.from(await response.arrayBuffer()));
"""

actual fun fetch(url: String): ByteArray? = try {
    when {
        url.startsWith("file:") -> {
            val path = decodeURIComponent(url.removePrefix("file://").removePrefix("file:"))
            if (Fs.existsSync(path) && Fs.statSync(path).isFile() as Boolean) bytesOf(Fs.readFileSync(path)) else null
        }
        url.startsWith("http://") || url.startsWith("https://") -> bytesOf(
            ChildProcess.execFileSync(
                process.execPath as String,
                arrayOf("--input-type=module", "-e", DOWNLOAD_SCRIPT, url),
                js("({ stdio: ['ignore', 'pipe', 'ignore'], maxBuffer: 1024 * 1024 * 1024 })"),
            ),
        )
        else -> null
    }
} catch (_: Throwable) {
    null
}

private external fun decodeURIComponent(text: String): String

internal actual val LocalFiles: LocalFileSystem = object : LocalFileSystem {
    override fun systemTemporaryDirectory(): String = Os.tmpdir().trimEnd('/')

    private fun stat(path: String): dynamic = Fs.statSync(path, js("({ throwIfNoEntry: false })"))

    override fun isFile(path: String): Boolean = stat(path)?.isFile() == true

    override fun exists(path: String): Boolean = Fs.existsSync(path)

    override fun lastModified(path: String): Long? {
        val stat = stat(path) ?: return null
        return (stat.mtimeMs as Double).toLong()
    }

    override fun touch(path: String, millis: Long) {
        Fs.writeFileSync(path, "")
        val seconds = millis / 1000.0
        Fs.utimesSync(path, seconds, seconds)
    }

    override fun mkdirs(path: String) {
        Fs.mkdirSync(path, js("({ recursive: true })"))
    }

    override fun createTempDirectory(parent: String, prefix: String): String = Fs.mkdtempSync("$parent/$prefix")

    override fun rename(from: String, to: String): Boolean = try {
        Fs.renameSync(from, to)
        true
    } catch (_: Throwable) {
        false
    }

    override fun deleteRecursively(path: String) {
        Fs.rmSync(path, js("({ recursive: true, force: true })"))
    }

    override fun readText(path: String): String? = if (isFile(path)) Fs.readFileSync(path, "utf8") as String else null

    override fun writeBytes(path: String, bytes: ByteArray) {
        Fs.writeFileSync(path, bufferOf(bytes))
    }

    override fun <T> useLines(path: String, block: (Sequence<String>) -> T): T? = readText(path)?.let { text ->
        // Comme BufferedReader : pas de ligne vide après le dernier saut de ligne
        val body = when {
            text.endsWith("\r\n") -> text.dropLast(2)
            text.endsWith("\n") || text.endsWith("\r") -> text.dropLast(1)
            else -> text
        }
        block(if (text.isEmpty()) emptySequence() else body.lineSequence())
    }

    override fun openWriter(path: String): TextWriter {
        val fd = Fs.openSync(path, "w")
        val pending = StringBuilder()
        return object : TextWriter {
            // Écritures regroupées : l'index du GTFS complet compte des millions de petites lignes
            override fun write(text: String) {
                pending.append(text)
                if (pending.length >= 1 shl 16) flush()
            }

            override fun close() {
                flush()
                Fs.closeSync(fd)
            }

            private fun flush() {
                Fs.writeSync(fd, pending.toString())
                pending.clear()
            }
        }
    }
}

internal actual fun inflateRaw(data: ByteArray, size: Int): ByteArray {
    val out = bytesOf(Zlib.inflateRawSync(bufferOf(data)))
    check(out.size == size) { "archive tronquée" }
    return out
}
