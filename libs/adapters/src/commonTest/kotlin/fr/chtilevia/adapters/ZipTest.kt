package fr.chtilevia.adapters

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZipTest {
    private val dir = LocalFiles.createTempDirectory(LocalFiles.systemTemporaryDirectory(), "zip-test")

    @AfterTest
    fun cleanUp() {
        LocalFiles.deleteRecursively(dir)
    }

    @Test
    fun extractsDeflatedEntriesAndDirectories() {
        // Produit par zipfile (Python) en ZIP_DEFLATED : un dossier, et un fichier dedans
        val zip = DEFLATED_ZIP.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertTrue(Zip.extract(zip, dir))
        assertEquals("Gare Lille Flandres\n".repeat(20), LocalFiles.readText("$dir/dossier/arrets.txt"))
    }

    @Test
    fun rejectsTruncatedArchives() {
        val zip = DEFLATED_ZIP.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertFalse(Zip.extract(zip.copyOf(zip.size - 30), dir))
        assertFalse(Zip.extract(ByteArray(0), dir))
    }

    private companion object {
        const val DEFLATED_ZIP =
            "504b0304140000000800f9b94c5d00000000020000000000000008000000646f73736965722f0300504b0304140000000800" +
                "f9b94c5de930f2b01b0000009001000012000000646f73736965722f6172726574732e747874734f2c4a55f0c9ccc9495570" +
                "cb49cc4b294a2de6721f151b303100504b01021403140000000800f9b94c5d000000000200000000000000080000000000" +
                "000000001000fd4100000000646f73736965722f504b01021403140000000800f9b94c5de930f2b01b0000009001000012" +
                "0000000000000000000000800128000000646f73736965722f6172726574732e747874504b05060000000002000200760000" +
                "00730000000000"
    }
}
