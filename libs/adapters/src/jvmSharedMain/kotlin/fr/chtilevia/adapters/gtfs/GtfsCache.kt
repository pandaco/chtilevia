package fr.chtilevia.adapters.gtfs

import fr.chtilevia.adapters.fetch
import fr.chtilevia.application.ChtiError
import fr.chtilevia.application.Notice
import fr.chtilevia.application.Notices
import fr.chtilevia.application.TimetableRepository
import fr.chtilevia.application.TimetableUpdater
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.Writer
import java.util.zip.ZipInputStream
import kotlin.io.path.createTempDirectory

/** Fichiers d'un GTFS décompressé dans [directory] */
class DirectoryGtfsFiles(private val directory: File) : GtfsFiles {
    override fun <T> read(name: String, block: (Sequence<String>) -> T): T? =
        File(directory, name).takeIf { it.isFile }?.bufferedReader()?.useLines(block)
}

/**
 * Référentiel GTFS en cache dans `<cacheDir>/gtfs`, même disposition et même politique que le
 * shell (qui peut partager ce cache) : téléchargé s'il a plus de 24 h ; en cas d'échec, l'ancien
 * reste utilisé et l'on ne retente pas avant une heure.
 */
class GtfsCache(
    cacheDir: File,
    private val url: String,
    private val download: (String) -> ByteArray? = ::fetch,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : TimetableUpdater {
    private val root = cacheDir
    val directory = File(cacheDir, "gtfs")
    val timetable: TimetableRepository = GtfsTimetable(DirectoryGtfsFiles(directory))

    override fun refresh(force: Boolean, notices: Notices): String {
        val stamp = File(directory, "stamp")
        val retry = File(directory, "retry")
        if (!force && stamp.isFile && (isFresh(stamp, MAX_AGE_MINUTES) || isFresh(retry, RETRY_MINUTES))) return directory.path

        notices.notice(Notice.TIMETABLE_DOWNLOAD)
        root.mkdirs()
        val work = createTempDirectory(root.toPath(), "gtfs.").toFile()
        try {
            val zip = download(url)
            if (zip == null || !unzip(zip, work)) {
                // Réseau KO : on garde l'ancien référentiel s'il existe
                if (!stamp.isFile) throw ChtiError.TimetableUnavailable(url)
                notices.notice(Notice.TIMETABLE_STALE)
                retry.writeText("")
                retry.setLastModified(currentTimeMillis())
                return directory.path
            }
            index(work)
            val old = File(root, "gtfs.old")
            old.deleteRecursively()
            if (directory.exists() && !directory.renameTo(old)) throw IOException("impossible de remplacer $directory")
            if (!work.renameTo(directory)) throw IOException("impossible d'installer $directory")
            old.deleteRecursively()
            stamp.writeText("")
            stamp.setLastModified(currentTimeMillis())
            return directory.path
        } finally {
            work.deleteRecursively()
        }
    }

    private fun isFresh(file: File, minutes: Int) =
        file.isFile && currentTimeMillis() - file.lastModified() <= minutes * 60_000L

    /** Décompresse [zip] dans [target] ; false si l'archive est illisible */
    private fun unzip(zip: ByteArray, target: File): Boolean = try {
        val base = target.canonicalFile
        ZipInputStream(ByteArrayInputStream(zip)).use { input ->
            var found = false
            while (true) {
                val entry = input.nextEntry ?: break
                val file = File(base, entry.name).canonicalFile
                // Rien en dehors du dossier cible (noms d'entrées en « ../ »)
                if (!file.path.startsWith(base.path + File.separator)) continue
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    file.outputStream().use { input.copyTo(it) }
                    found = true
                }
            }
            found
        }
    } catch (_: IOException) {
        false
    }

    /** Index par ligne (voir [GtfsTimetable.buildIndex]) */
    private fun index(gtfs: File) {
        val index = File(gtfs, "idx").apply { mkdirs() }
        val writers = HashMap<String, Writer>()
        try {
            File(gtfs, "trips.txt").bufferedReader().useLines { trips ->
                File(gtfs, "stop_times.txt").bufferedReader().useLines { stopTimes ->
                    GtfsTimetable.buildIndex(trips, stopTimes) { lineId, line ->
                        writers.getOrPut(lineId) { File(index, "$lineId.tsv").bufferedWriter() }.write(line)
                    }
                }
            }
        } finally {
            writers.values.forEach { it.close() }
        }
    }

    private companion object {
        const val MAX_AGE_MINUTES = 24 * 60
        const val RETRY_MINUTES = 60
    }
}
