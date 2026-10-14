package fr.chtipasse.adapters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AppDirectoryTest {
    private fun base() = LocalFiles.createTempDirectory(LocalFiles.systemTemporaryDirectory(), "chti-app-dir")

    private fun write(path: String, text: String) {
        LocalFiles.mkdirs(path.substringBeforeLast('/'))
        LocalFiles.writeBytes(path, text.encodeToByteArray())
    }

    @Test
    fun legacyDirectoryIsTakenOver() {
        val base = base()
        write("$base/chtilevia/favoris", "travail: tram croise laroche\n")
        assertEquals("$base/chtipasse", appDirectory(base))
        assertEquals("travail: tram croise laroche\n", LocalFiles.readText("$base/chtipasse/favoris"))
        assertFalse(LocalFiles.exists("$base/chtilevia"))
        LocalFiles.deleteRecursively(base)
    }

    @Test
    fun newDirectoryWinsOverTheLegacyOne() {
        val base = base()
        write("$base/chtipasse/favoris", "a: bus 10 marais\n")
        write("$base/chtilevia/favoris", "b: velo rihour\n")
        assertEquals("$base/chtipasse", appDirectory(base))
        assertEquals("a: bus 10 marais\n", LocalFiles.readText("$base/chtipasse/favoris"))
        LocalFiles.deleteRecursively(base)
    }

    @Test
    fun nothingToTakeOver() {
        val base = base()
        assertEquals("$base/chtipasse", appDirectory(base))
        assertNull(LocalFiles.readText("$base/chtipasse/favoris"))
        LocalFiles.deleteRecursively(base)
    }
}
