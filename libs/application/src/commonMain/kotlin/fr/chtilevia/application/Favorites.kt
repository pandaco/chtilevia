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

    /** Fichier qui contient [favorites], dans l'ordre : une ligne `nom: commande` par commande */
    fun format(favorites: List<Favorite>): String =
        favorites.flatMap { favorite -> favorite.commands.map { "${favorite.name}: $it\n" } }.joinToString("")

    /**
     * Commande à enregistrer pour les mots [words] d'une phrase comprise ([InterpretUtterance]) :
     * `-d` devient « vers », qui garde une direction en plusieurs mots une fois la ligne découpée
     */
    fun command(words: List<String>): String = words.joinToString(" ") { if (it == "-d") "vers" else it }

    /**
     * Commande de favori réécrite comme les propositions de saisie ([SuggestQuery]), pour la
     * modifier avec elles : `--prendre` → « pour prendre », `-d` → « vers »
     */
    fun phrase(command: String): String = words(command).joinToString(" ") {
        when (it) {
            "--prendre" -> "pour prendre"
            "--deposer", "--déposer" -> "pour déposer"
            "-d", "--direction", "--vers" -> "vers"
            else -> it
        }
    }

    /** Raison pour laquelle [name] ne peut pas nommer un favori parmi [others], ou null s'il convient */
    fun invalidName(name: String, others: List<String>): String? = when {
        name.isBlank() -> "Donne un nom au favori."
        ':' in name || '#' in name -> "Le nom ne peut contenir ni « : » ni « # »."
        others.any { it.asciiLowercase() == name.trim().asciiLowercase() } -> "Un autre favori porte déjà ce nom."
        else -> null
    }

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
