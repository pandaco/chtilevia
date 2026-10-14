package fr.chtilevia.android

import android.util.Log
import fr.chtilevia.adapters.fetch
import fr.chtilevia.adapters.gtfs.GtfsCache
import fr.chtilevia.application.NetworkProfile
import fr.chtilevia.application.Notices
import java.io.File

/**
 * Mesure sur le téléphone de ce que coûte le GTFS complet : téléchargement, décompression et
 * index, place occupée, mémoire, puis temps de réponse de quelques commandes (première et seconde
 * fois). Chaque résultat est écrit dans logcat (étiquette « chtilevia ») et passé à [report].
 */
class Benchmark(private val app: AndroidChti, private val filesDir: File, private val report: (String) -> Unit) {
    fun run() {
        val runtime = Runtime.getRuntime()
        fun heapMb() = (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576
        fun line(text: String) {
            Log.i(TAG, text)
            report(text)
        }
        fun <T> timed(label: String, block: () -> T): T {
            val start = System.nanoTime()
            val result = block()
            line("$label : ${(System.nanoTime() - start) / 1_000_000} ms (tas ${heapMb()} Mo, max ${runtime.maxMemory() / 1_048_576} Mo)")
            return result
        }

        val url = NetworkProfile.ILEVIA.gtfsUrl
        val zip = timed("Téléchargement du GTFS") { fetch(url) }
        if (zip == null) {
            line("Téléchargement impossible : $url")
            return
        }
        line("Archive : ${zip.size / 1024} Ko")

        // Même cache que l'appli, reconstruit avec l'archive déjà téléchargée
        val root = filesDir.resolve("chtilevia")
        val cache = GtfsCache(root.path, url, download = { zip }, keepStopTimes = false)
        timed("Décompression et index") { cache.refresh(force = true, notices = Notices.NONE) }
        line("Place occupée : ${root.resolve("gtfs").walk().filter { it.isFile }.sumOf { it.length() } / 1024} Ko")

        for (command in COMMANDS) {
            for (attempt in 1..2) {
                val reply = timed("chti ${command.joinToString(" ")} (n° $attempt)") { app.run(command) }
                if (attempt == 1) line(reply.text.lines().take(4).joinToString("\n"))
            }
        }
        line("Terminé.")
    }

    private companion object {
        const val TAG = "chtipasse"
        val COMMANDS = listOf(
            listOf("tram", "croise", "laroche", "vers", "roubaix"),
            listOf("bus", "10", "gambetta"),
            listOf("metro", "1", "republique"),
            listOf("autour", "50.6366", "3.0630"),
            listOf("velo", "rihour"),
        )
    }
}
