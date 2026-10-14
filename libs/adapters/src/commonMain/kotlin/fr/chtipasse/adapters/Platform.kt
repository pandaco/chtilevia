package fr.chtipasse.adapters

// Accès au système de chaque plateforme : java.io / java.net sur la JVM et Android (jvmShared),
// modules node:* sous Node (js). Le reste des adaptateurs est commun.

/**
 * Contenu de [url] (http, https ou file), ou null si la ressource est injoignable ou absente.
 * Délai de 30 s, comme le shell.
 */
expect fun fetch(url: String): ByteArray?

/** Fichiers locaux de la plateforme */
internal expect val LocalFiles: LocalFileSystem

/** Accès aux fichiers locaux ; chemins séparés par « / » */
internal interface LocalFileSystem {
    /** Dossier temporaire du système */
    fun systemTemporaryDirectory(): String

    fun isFile(path: String): Boolean

    fun exists(path: String): Boolean

    /** Date de modification (epoch ms), ou null si le fichier n'existe pas */
    fun lastModified(path: String): Long?

    /** Crée [path] vide (ou le vide) et fixe sa date de modification à [millis] */
    fun touch(path: String, millis: Long)

    fun mkdirs(path: String)

    /** Nouveau dossier vide dans [parent], au nom commençant par [prefix] */
    fun createTempDirectory(parent: String, prefix: String): String

    fun rename(from: String, to: String): Boolean

    fun deleteRecursively(path: String)

    fun readText(path: String): String?

    fun writeBytes(path: String, bytes: ByteArray)

    /** Applique [block] aux lignes du fichier, ou renvoie null s'il n'existe pas */
    fun <T> useLines(path: String, block: (Sequence<String>) -> T): T?

    /** Écriture de texte en flux, à fermer */
    fun openWriter(path: String): TextWriter
}

internal interface TextWriter {
    fun write(text: String)

    fun close()
}

/** Données compressées par « deflate » brut (méthode 8 des zip), décompressées */
internal expect fun inflateRaw(data: ByteArray, size: Int): ByteArray
