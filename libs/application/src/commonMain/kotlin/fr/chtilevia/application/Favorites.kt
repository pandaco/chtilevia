package fr.chtilevia.application

import fr.chtilevia.domain.Favorite

/**
 * Fichier des favoris, partagé avec le shell : une ligne `nom: commande` par commande, `#` pour
 * les commentaires. Plusieurs lignes du même nom (casse ignorée) forment un favori à plusieurs
 * commandes.
 */
object FavoritesFile {
    private fun contentLines(text: String): List<String> =
        text.split('\n').let { if (text.endsWith('\n')) it.dropLast(1) else it }.map { it.substringBefore('#') }

    private val entry = Regex("""^\s*([^:]*?)\s*:\s*(.*?)\s*$""")

    /** Lignes utiles du fichier, sans commentaires ni espaces de fin, pour les montrer telles quelles */
    fun displayLines(text: String): List<String> =
        contentLines(text).map { it.trimEnd() }.filter { it.isNotEmpty() }

    /** Favoris, chacun sous le nom de sa première ligne, avec toutes ses commandes */
    fun favorites(text: String): List<Favorite> {
        val byName = LinkedHashMap<String, Favorite>()
        for (line in contentLines(text)) {
            val match = entry.find(line) ?: continue
            val (name, command) = match.destructured
            if (name.isEmpty() || command.isEmpty()) continue
            val key = name.asciiLowercase()
            byName[key] = byName[key]?.let { it.copy(commands = it.commands + command) } ?: Favorite(name, listOf(command))
        }
        return byName.values.toList()
    }

    /** Commandes du favori [name] (casse ignorée), vide s'il n'existe pas */
    fun commands(text: String, name: String): List<String> = contentLines(text).mapNotNull { line ->
        if (':' !in line) return@mapNotNull null
        val key = line.substringBefore(':').trim(' ', '\t').lowercase()
        if (key != name.lowercase()) return@mapNotNull null
        line.substringAfter(':').trimStart(' ', '\t').trimEnd(' ', '\t').ifEmpty { null }
    }

    /** Noms des favoris, tels qu'écrits dans le fichier */
    fun names(text: String): List<String> = contentLines(text)
        .filter { ':' in it }
        .map { it.substringBefore(':').trim() }
        .filter { it.isNotEmpty() }

    /** Mots d'une commande de favori, découpés comme le ferait le shell */
    fun words(command: String): List<String> = command.split(' ', '\t', '\n').filter { it.isNotEmpty() }

    private fun String.asciiLowercase(): String = map { if (it in 'A'..'Z') it + 32 else it }.joinToString("")
}

/** Contenu de la liste des favoris : fichier présent et non vide, lignes à montrer, favoris */
data class FavoritesListing(
    val location: String,
    val isEmpty: Boolean,
    val lines: List<String>,
    val favorites: List<Favorite>,
)

class ListFavorites(private val store: FavoritesStore) {
    fun execute(): FavoritesListing {
        val text = store.read().orEmpty()
        return FavoritesListing(store.location, text.isEmpty(), FavoritesFile.displayLines(text), FavoritesFile.favorites(text))
    }
}

/** Commandes d'un favori ; vide s'il n'existe pas */
class FindFavorite(private val store: FavoritesStore) {
    fun execute(name: String): List<String> = FavoritesFile.commands(store.read().orEmpty(), name)
}
