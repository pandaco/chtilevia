package fr.chtilevia.adapters

/**
 * Lecteur d'archives zip minimal, commun à toutes les plateformes : répertoire central, entrées
 * stockées (méthode 0) ou compressées (méthode 8, [inflateRaw]). Pas de zip64 : le GTFS en est loin.
 */
internal object Zip {
    /**
     * Extrait [zip] dans le dossier [target] ; false si l'archive est illisible ou ne contient aucun
     * fichier. Les entrées qui sortiraient du dossier (« ../ », chemin absolu) sont ignorées.
     */
    fun extract(zip: ByteArray, target: String): Boolean = try {
        var found = false
        for (entry in entries(zip)) {
            val segments = entry.name.split('/')
            if (entry.name.startsWith("/") || '\\' in entry.name || segments.any { it == ".." }) continue
            val path = "$target/${entry.name.trimEnd('/')}"
            if (entry.name.endsWith("/")) {
                LocalFiles.mkdirs(path)
                continue
            }
            if (segments.size > 1) LocalFiles.mkdirs(path.substringBeforeLast('/'))
            LocalFiles.writeBytes(path, entry.content())
            found = true
        }
        found
    } catch (_: Exception) {
        // Archive tronquée, signature inattendue, méthode inconnue, données corrompues…
        false
    }

    private class Entry(val name: String, val content: () -> ByteArray)

    private fun entries(zip: ByteArray): List<Entry> {
        val end = (zip.size - END_SIZE downTo maxOf(0, zip.size - END_SIZE - 0xFFFF)).firstOrNull { zip.u32(it) == END_SIGNATURE }
            ?: error("fin d'archive introuvable")
        val count = zip.u16(end + 10)
        var at = zip.u32(end + 16).toInt()
        return List(count) {
            check(zip.u32(at) == CENTRAL_SIGNATURE) { "répertoire central illisible" }
            val method = zip.u16(at + 10)
            val compressed = zip.u32(at + 20).toInt()
            val size = zip.u32(at + 24).toInt()
            val nameLength = zip.u16(at + 28)
            val local = zip.u32(at + 42).toInt()
            val name = zip.decodeToString(at + 46, at + 46 + nameLength)
            at += 46 + nameLength + zip.u16(at + 30) + zip.u16(at + 32)
            Entry(name) {
                check(zip.u32(local) == LOCAL_SIGNATURE) { "entrée illisible : $name" }
                val start = local + 30 + zip.u16(local + 26) + zip.u16(local + 28)
                val data = zip.copyOfRange(start, start + compressed)
                when (method) {
                    0 -> data
                    8 -> inflateRaw(data, size)
                    else -> error("méthode de compression non prise en charge : $method")
                }
            }
        }
    }

    private fun ByteArray.u16(at: Int): Int = (this[at].toInt() and 0xFF) or ((this[at + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.u32(at: Int): Long = u16(at).toLong() or (u16(at + 2).toLong() shl 16)

    private const val END_SIZE = 22
    private const val END_SIGNATURE = 0x06054b50L
    private const val CENTRAL_SIGNATURE = 0x02014b50L
    private const val LOCAL_SIGNATURE = 0x04034b50L
}
