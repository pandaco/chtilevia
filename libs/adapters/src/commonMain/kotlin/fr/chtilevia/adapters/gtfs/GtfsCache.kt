package fr.chtilevia.adapters.gtfs

import fr.chtilevia.adapters.LocalFiles
import fr.chtilevia.adapters.TextWriter
import fr.chtilevia.adapters.Zip
import fr.chtilevia.adapters.fetch
import fr.chtilevia.application.ChtiError
import fr.chtilevia.application.Notice
import fr.chtilevia.application.Notices
import fr.chtilevia.application.TimetableRepository
import fr.chtilevia.application.TimetableUpdater
import kotlin.time.Clock

/** Fichiers d'un GTFS décompressé dans [directory] */
class DirectoryGtfsFiles(private val directory: String) : GtfsFiles {
    override fun <T> read(name: String, block: (Sequence<String>) -> T): T? = LocalFiles.useLines("$directory/$name", block)
}

/**
 * Référentiel GTFS en cache dans `<cacheDir>/gtfs`, même disposition et même politique que le
 * shell (qui peut partager ce cache) : téléchargé s'il a plus de 24 h ; en cas d'échec, l'ancien
 * reste utilisé et l'on ne retente pas avant une heure. [keepStopTimes] = false supprime
 * `stop_times.txt` une fois l'index construit (plus de la moitié de la place) : à réserver aux caches
 * que le shell ne partage pas, car il en a besoin pour reconstruire son index.
 */
class GtfsCache(
    private val cacheDir: String,
    private val url: String,
    private val download: (String) -> ByteArray? = ::fetch,
    private val currentTimeMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val keepStopTimes: Boolean = true,
) : TimetableUpdater {
    val directory = "$cacheDir/gtfs"

    /** Gardé en mémoire jusqu'à l'installation d'un nouveau GTFS */
    val timetable: TimetableRepository =
        CachedTimetable(GtfsTimetable(DirectoryGtfsFiles(directory))) { LocalFiles.lastModified("$directory/stamp") }

    override fun refresh(force: Boolean, notices: Notices): String {
        val stamp = "$directory/stamp"
        val retry = "$directory/retry"
        val upToDate = isFresh(stamp, MAX_AGE_MINUTES) || isFresh(retry, RETRY_MINUTES)
        if (!force && LocalFiles.isFile(stamp) && upToDate) return directory

        notices.notice(Notice.TIMETABLE_DOWNLOAD)
        LocalFiles.mkdirs(cacheDir)
        val work = LocalFiles.createTempDirectory(cacheDir, "gtfs.")
        try {
            val zip = download(url)
            if (zip == null || !Zip.extract(zip, work)) {
                // Réseau KO : on garde l'ancien référentiel s'il existe
                if (!LocalFiles.isFile(stamp)) throw ChtiError.TimetableUnavailable(url)
                notices.notice(Notice.TIMETABLE_STALE)
                LocalFiles.touch(retry, currentTimeMillis())
                return directory
            }
            index(work)
            if (!keepStopTimes) LocalFiles.deleteRecursively("$work/stop_times.txt")
            val old = "$cacheDir/gtfs.old"
            LocalFiles.deleteRecursively(old)
            if (LocalFiles.exists(directory) && !LocalFiles.rename(directory, old)) error("impossible de remplacer $directory")
            if (!LocalFiles.rename(work, directory)) error("impossible d'installer $directory")
            LocalFiles.deleteRecursively(old)
            LocalFiles.touch(stamp, currentTimeMillis())
            return directory
        } finally {
            LocalFiles.deleteRecursively(work)
        }
    }

    private fun isFresh(file: String, minutes: Int): Boolean {
        val modified = LocalFiles.lastModified(file) ?: return false
        return LocalFiles.isFile(file) && currentTimeMillis() - modified <= minutes * 60_000L
    }

    /** Index par ligne (voir [GtfsTimetable.buildIndex]) */
    private fun index(gtfs: String) {
        val index = "$gtfs/idx"
        LocalFiles.mkdirs(index)
        val writers = HashMap<String, TextWriter>()
        try {
            LocalFiles.useLines("$gtfs/trips.txt") { trips ->
                LocalFiles.useLines("$gtfs/stop_times.txt") { stopTimes ->
                    GtfsTimetable.buildIndex(trips, stopTimes) { lineId, line ->
                        writers.getOrPut(lineId) { LocalFiles.openWriter("$index/$lineId.tsv") }.write(line)
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
